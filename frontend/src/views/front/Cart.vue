<template>
  <!-- 购物车页面：左右分栏布局（左侧商品列表 + 右侧结算明细面板） -->
  <div class="cart-page">
    <!-- 页头：左侧大标题 + 右侧操作按钮与商品数量提示 -->
    <div class="cart-header">
      <h2 class="cart-title">购物车</h2>
      <div class="cart-header-right">
        <div v-if="!loading && cartList.length > 0" class="cart-count-tip">
          共 <span class="cart-count-num">{{ cartList.length }}</span> 件商品
        </div>
        <template v-if="!loading && cartList.length > 0">
          <button class="btn-sm" type="button" :disabled="selectedCount === 0"
            @click="handleBatchDelete">删除选中</button>
          <button class="btn-sm" type="button" @click="handleClear">清空购物车</button>
        </template>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span class="loading-text">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="cartList.length === 0" class="empty-state">
      <el-empty description="购物车空空如也，快去挑选心仪的商品吧！" :image-size="120" />
      <button class="btn-sm primary" type="button" @click="router.push('/products')">去逛逛</button>
    </div>

    <!-- 主体: 左右分栏布局 -->
    <div v-else class="cart-body">
      <!-- 左侧: 商品列表主内容区 -->
      <div class="cart-main">
        <!-- 商品列表卡片 -->
        <div class="cart-content">
          <!-- 表头: 全选 -->
          <div class="cart-table-head">
            <div class="col-check">
              <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate"
                @change="handleToggleAll">全选</el-checkbox>
            </div>
            <div class="col-info">商品信息</div>
            <div class="col-price">单价</div>
            <div class="col-quantity">数量</div>
            <div class="col-subtotal">小计</div>
            <div class="col-action">操作</div>
          </div>

          <!-- 商品行 -->
          <div v-for="item in pagedCartList" :key="item.id" class="cart-row"
            :class="{ disabled: item.productStatus !== 'ON_SALE' }">
            <!-- 复选框 -->
            <div class="col-check">
              <el-checkbox :model-value="item.selected" :disabled="item.productStatus !== 'ON_SALE'"
                @change="(val: boolean | string | number) => handleToggleSelect(item, Boolean(val))" />
            </div>

            <!-- 商品信息: 图片 + 名称 + SKU属性 -->
            <div class="col-info">
              <div class="product-img" @click="goProductDetail(item.productId)">
                <img v-if="item.skuMainImage || item.mainImage"
                  :src="formatImageUrl(item.skuMainImage || item.mainImage)" :alt="item.productName"
                  class="product-img-tag" loading="lazy" />
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                  class="product-img-placeholder">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m21 15-5-5L5 21" />
                </svg>
              </div>
              <div class="product-info">
                <div class="product-name" @click="goProductDetail(item.productId)">{{ item.productName }}</div>
                <div v-if="item.skuAttributes" class="product-sku-attrs">
                  <svg class="product-sku-attrs-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2">
                    <path d="M3 6h18M3 12h18M3 18h18" />
                  </svg>
                  {{ item.skuAttributes }}
                </div>
                <div v-if="item.productStatus !== 'ON_SALE'" class="product-status-tag">已下架</div>
              </div>
            </div>

            <!-- 单价 -->
            <div class="col-price">
              <span class="price-text">¥{{ formatPrice(item.originalPrice) }}</span>
            </div>

            <!-- 数量加减控件 -->
            <div class="col-quantity">
              <el-input-number v-model="item.quantity" :min="1" :max="Math.max(1, item.stock)"
                :disabled="item.productStatus !== 'ON_SALE'" size="small"
                @change="(val: number | undefined) => handleQuantityChange(item, val)" />
              <div v-if="item.stock <= 5 && item.productStatus === 'ON_SALE'" class="stock-warn">
                仅剩 {{ item.stock }} 件
              </div>
            </div>

            <!-- 小计 -->
            <div class="col-subtotal">
              <span class="subtotal-text">¥{{ formatPrice(item.subtotal) }}</span>
            </div>

            <!-- 操作 -->
            <div class="col-action">
              <button class="btn-sm text danger" type="button" @click="handleDelete(item)">删除</button>
            </div>
          </div>
        </div>

        <!-- 分页组件 (商品数量超过 pageSize 时显示) -->
        <div v-if="cartList.length > pageSize" class="cart-pagination">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="cartList.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="handlePageChange"
          />
        </div>
      </div>

      <!-- 右侧: 结算明细面板 (sticky 定位) -->
      <div class="cart-sidebar">
        <!-- 面板标题 -->
        <div class="sidebar-title">结算明细</div>
        <!-- 面板内容 -->
        <div class="sidebar-body">
          <!-- 选中商品列表 (最多显示5个，超过显示"等N件商品") -->
          <div v-if="selectedCount > 0" class="selected-items">
            <div v-for="item in cartList.filter(i => i.selected && i.productStatus === 'ON_SALE').slice(0, 5)"
              :key="item.id" class="selected-item">
              <div class="selected-item-img">
                <img v-if="item.skuMainImage || item.mainImage"
                  :src="formatImageUrl(item.skuMainImage || item.mainImage)" :alt="item.productName"
                  class="selected-item-img-tag" loading="lazy" />
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                  class="selected-item-img-placeholder">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m21 15-5-5L5 21" />
                </svg>
              </div>
              <div class="selected-item-info">
                <div class="selected-item-name">{{ item.productName }}</div>
                <div class="selected-item-meta">{{ item.quantity }} × ¥{{ formatPrice(item.originalPrice) }}</div>
              </div>
            </div>
            <div v-if="selectedCount > 5" class="more-items">等 {{ selectedCount }} 件商品</div>
          </div>
          <div v-else class="selected-empty">暂未选中商品</div>

          <div class="summary-divider"></div>

          <div class="summary-row">
            <span class="summary-label">商品总价</span>
            <span class="summary-value">¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">已选商品</span>
            <span class="summary-value">{{ selectedCount }} 件</span>
          </div>
          <div class="summary-divider"></div>
        </div>
        <!-- 面板底部 -->
        <div class="sidebar-footer">
          <div class="summary-total">
            <span class="summary-total-label">合计</span>
            <span class="summary-total-value">¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <button class="btn-checkout" type="button" :disabled="selectedCount === 0"
            @click="handleCheckout">去结算</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 购物车页面 (前台)
 * 对接后端 /api/v1/cart 接口，无模拟数据。
 * 操作后刷新列表和购物车数量徽标 (Pinia store)。
 */
defineOptions({ name: 'Cart' })
import { ref, computed, onMounted, onActivated, onUnmounted, onDeactivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
  getCartList,
  updateCartQuantity,
  deleteCartItem,
  clearCart,
  updateCartSelected,
  batchUpdateCartSelected
} from '@/api/cart'
import { useCartStore } from '@/stores/cart'
import { formatImageUrl } from '@/utils/image'
import type { CartItemVO } from '@/types'

const router = useRouter()
const cartStore = useCartStore()

/** 购物车列表 (后端返回) */
const cartList = ref<CartItemVO[]>([])

/** 列表加载中 */
const loading = ref<boolean>(false)

/** 分页相关变量 (前端假分页，因后端 /api/v1/cart/list 不支持分页参数) */
const pageNum = ref<number>(1)
const pageSize = ref<number>(10)

/** 数量更新防抖映射 (避免频繁请求) */
const quantityTimers = new Map<number | string, ReturnType<typeof setTimeout>>()

/**
 * H19 修复: 清理所有数量更新防抖定时器
 * 在组件卸载或 keep-alive 缓存失活时调用，避免内存泄漏与卸载后仍触发请求
 */
function clearAllQuantityTimers(): void {
  quantityTimers.forEach((t) => clearTimeout(t))
  quantityTimers.clear()
}

/* === 计算属性 === */

/** 选中商品数量 */
const selectedCount = computed<number>(() => {
  return cartList.value.filter(item => item.selected && item.productStatus === 'ON_SALE').length
})

/** 合计金额 (选中且上架商品的小计之和) */
const totalAmount = computed<number>(() => {
  return cartList.value
    .filter(item => item.selected && item.productStatus === 'ON_SALE')
    .reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

/** 可选商品 (上架商品) */
const selectableItems = computed<CartItemVO[]>(() => {
  return cartList.value.filter(item => item.productStatus === 'ON_SALE')
})

/** 是否全选 */
const isAllSelected = computed<boolean>(() => {
  if (selectableItems.value.length === 0) return false
  return selectableItems.value.every(item => item.selected)
})

/** 是否半选 (indeterminate) */
const isIndeterminate = computed<boolean>(() => {
  const selected = selectableItems.value.filter(item => item.selected).length
  return selected > 0 && selected < selectableItems.value.length
})

/** 当前页显示的购物车列表 (前端假分页: slice 数组) */
const pagedCartList = computed<CartItemVO[]>(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return cartList.value.slice(start, start + pageSize.value)
})

/* === 工具函数 === */

/** 格式化价格 (保留两位小数) */
function formatPrice(value: number): string {
  return (value || 0).toFixed(2)
}

/** 加载购物车列表 */
async function loadCartList(): Promise<void> {
  loading.value = true
  try {
    const res = await getCartList()
    // 规范化：后端 selected 可能是 Integer(0/1)，统一转为 boolean
    cartList.value = (res.data ?? []).map(item => ({
      ...item,
      selected: Boolean(item.selected)
    }))
    // 修正越界页码: 删除商品后当前页可能无数据，回退到最后一页
    const maxPage = Math.max(1, Math.ceil(cartList.value.length / pageSize.value))
    if (pageNum.value > maxPage) {
      pageNum.value = maxPage
    }
  } catch {
    cartList.value = []
  } finally {
    loading.value = false
  }
}

/** 刷新列表 + 购物车数量徽标 */
async function refreshAll(): Promise<void> {
  await loadCartList()
  await cartStore.fetchCount()
}

/* === 分页事件处理 === */

/** 分页变化 (页码或每页条数变化时触发) */
function handlePageChange(): void {
  // 前端假分页: 仅需更新视图，pagedCartList 计算属性会自动响应
  // 滚动到商品列表顶部，提升用户体验
  const tableHead = document.querySelector('.cart-table-head')
  if (tableHead) {
    tableHead.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }
}

/* === 事件处理 === */

/** 跳转商品详情 */
function goProductDetail(id: number | string): void {
  router.push(`/products/${id}`)
}

/** 切换单个商品选中状态 */
async function handleToggleSelect(item: CartItemVO, selected: boolean): Promise<void> {
  try {
    await updateCartSelected(item.id, selected)
    item.selected = selected
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

/** 切换全选/全不选 */
async function handleToggleAll(val: boolean | string | number): Promise<void> {
  const selected = Boolean(val)
  // 仅对上架商品进行批量选中
  const ids = selectableItems.value.map(item => item.id)
  if (ids.length === 0) return
  try {
    await batchUpdateCartSelected(ids, selected)
    selectableItems.value.forEach(item => { item.selected = selected })
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

/** 数量变化 (防抖 300ms 后提交, 避免连续点击发送多次请求) */
function handleQuantityChange(item: CartItemVO, val: number | undefined): void {
  const newQty = typeof val === 'number' ? val : 1
  // 清除已有定时器
  const existing = quantityTimers.get(item.id)
  if (existing) clearTimeout(existing)
  // 设置新定时器
  const timer = setTimeout(async () => {
    try {
      await updateCartQuantity(item.id, newQty)
      // 更新本地小计
      item.subtotal = item.originalPrice * newQty
      quantityTimers.delete(item.id)
    } catch {
      // 失败时回滚到服务器状态
      await loadCartList()
    }
  }, 300)
  quantityTimers.set(item.id, timer)
}

/** 删除单个商品 */
async function handleDelete(item: CartItemVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定从购物车删除"${item.productName}"吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  try {
    await deleteCartItem(item.id)
    ElMessage.success('已删除')
    await refreshAll()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

/** 批量删除选中商品 */
async function handleBatchDelete(): Promise<void> {
  const selectedItems = cartList.value.filter(item => item.selected)
  if (selectedItems.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedItems.length} 件商品吗？`, '批量删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  try {
    // 串行删除 (后端未提供批量删除接口)
    for (const item of selectedItems) {
      await deleteCartItem(item.id)
    }
    ElMessage.success('已删除选中商品')
    await refreshAll()
  } catch {
    // 错误已由请求拦截器统一提示
    await loadCartList()
  }
}

/** 清空购物车 */
async function handleClear(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定清空购物车吗？此操作不可恢复！', '清空确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  try {
    await clearCart()
    ElMessage.success('购物车已清空')
    await refreshAll()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

/** 去结算: 校验选中 → 存 sessionStorage → 跳转 /checkout 结算确认页 */
function handleCheckout(): void {
  if (selectedCount.value === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  // 仅取选中且上架的商品 (与合计金额口径一致)
  const selectedItems = cartList.value
    .filter(item => item.selected && item.productStatus === 'ON_SALE')
    .map(item => ({
      cartId: item.id,
      productId: item.productId,
      productName: item.productName,
      mainImage: item.mainImage,
      price: item.originalPrice,
      quantity: item.quantity,
      subtotal: item.subtotal
    }))
  if (selectedItems.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  try {
    sessionStorage.setItem('checkout_items', JSON.stringify(selectedItems))
  } catch {
    ElMessage.error('结算数据保存失败，请重试')
    return
  }
  router.push('/checkout')
}

// 页面挂载时加载购物车列表
onMounted(() => {
  loadCartList()
})

// keep-alive 缓存后, 再次激活时刷新购物车列表 (保证从其他页面返回时数据新鲜)
onActivated(() => {
  loadCartList()
})

// H19 修复: 组件卸载时清理所有数量更新防抖定时器，避免内存泄漏
onUnmounted(() => {
  clearAllQuantityTimers()
})

// H19 修复: keep-alive 缓存失活时也清理定时器，避免失活期间仍触发请求
onDeactivated(() => {
  clearAllQuantityTimers()
})
</script>

<style scoped>
/* ===== 购物车页面 - 左右分栏布局 ===== */

/* 页面容器 */
.cart-page {
  padding: 24px;
}

/* ===== 页头 ===== */
.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 页头右侧容器: 容纳商品数量提示 + 操作按钮 */
.cart-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: 0.02em;
}

.cart-count-tip {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.cart-count-num {
  font-weight: 600;
  color: var(--color-primary);
  margin: 0 2px;
}

/* ===== 加载中状态 ===== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px 60px;
  text-align: center;
  gap: 20px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

/* ===== 主体: 左右分栏布局 ===== */
.cart-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* 左侧: 商品列表主内容区 */
.cart-main {
  flex: 1;
  min-width: 0;
}

/* ===== 购物车内容卡片 ===== */
.cart-content {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* ===== 分页组件 ===== */
.cart-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

/* ===== 表头 (含全选) ===== */
.cart-table-head {
  display: grid;
  grid-template-columns: 50px 1fr 100px 140px 100px 70px;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid var(--color-border);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* ===== 商品行 ===== */
.cart-row {
  display: grid;
  grid-template-columns: 50px 1fr 100px 140px 100px 70px;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--color-border-light);
  transition: background 0.15s;
}

.cart-row:last-child {
  border-bottom: none;
}

.cart-row:hover {
  background: #fafafa;
}

/* 下架商品降低不透明度 */
.cart-row.disabled {
  opacity: 0.55;
}

/* ===== 复选框列 ===== */
.col-check {
  display: flex;
  align-items: center;
  justify-content: center;
}


/* ===== 商品信息列 ===== */
.col-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

/* 商品图片 */
.product-img {
  width: 72px;
  height: 72px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s;
}

.product-img:hover {
  border-color: var(--color-primary);
}

.product-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-img-placeholder {
  width: 32px;
  height: 32px;
  color: #ccc;
}

/* 商品信息文本区 */
.product-info {
  flex: 1;
  min-width: 0;
}

/* 商品名称 (2行截断) */
.product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  cursor: pointer;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.15s;
}

.product-name:hover {
  color: var(--color-primary);
}

/* 已下架标记 */
.product-status-tag {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 8px;
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

/* SKU 属性标签 */
.product-sku-attrs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border-light);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-sku-attrs-icon {
  width: 12px;
  height: 12px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

/* ===== 单价列 ===== */
.col-price,
.col-subtotal {
  text-align: center;
}

.price-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* ===== 小计列 (红色醒目) ===== */
.subtotal-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

/* ===== 数量列 ===== */
.col-quantity {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

/* 库存警告 (橙色) */
.stock-warn {
  font-size: 12px;
  color: #ff9800;
  font-weight: 500;
}

/* ===== 操作列 ===== */
.col-action {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 右侧结算面板 (sticky 定位) ===== */
.cart-sidebar {
  width: 300px;
  flex-shrink: 0;
  align-self: flex-start;
  position: sticky;
  top: 80px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* 面板标题 */
.sidebar-title {
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-light);
}

/* 面板内容 */
.sidebar-body {
  padding: 20px;
}

/* ===== 选中商品列表 (结算明细面板内) ===== */
.selected-items {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 4px;
}

.selected-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--color-border-light);
}

.selected-item:last-child {
  border-bottom: none;
}

.selected-item-img {
  width: 40px;
  height: 40px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
}

.selected-item-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selected-item-img-placeholder {
  width: 20px;
  height: 20px;
  color: #ccc;
}

.selected-item-info {
  flex: 1;
  min-width: 0;
}

/* 商品名称 (1行截断) */
.selected-item-name {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-item-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

/* 超过5个商品的提示 */
.more-items {
  padding: 8px 0 4px;
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
}

/* 暂无选中商品占位 */
.selected-empty {
  padding: 16px 0;
  font-size: 13px;
  color: var(--color-text-muted);
  text-align: center;
}

/* 摘要行 */
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.summary-label {
  color: var(--color-text-secondary);
}

.summary-value {
  color: var(--color-text-primary);
  font-weight: 500;
}

/* 分隔线 */
.summary-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 16px 0;
}

/* 面板底部 */
.sidebar-footer {
  padding: 16px 20px 20px;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-subtle);
}

/* 合计行 */
.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.summary-total-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.summary-total-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-primary);
  letter-spacing: 0.01em;
}

/* ===== 去结算按钮 (全宽) ===== */
.btn-checkout {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
  font-weight: 700;
  border-radius: var(--radius-md);
  cursor: pointer;
  border: none;
  background: var(--color-primary);
  color: #ffffff;
  letter-spacing: 0.05em;
  transition: background 0.15s;
}

.btn-checkout:hover:not(:disabled) {
  background: var(--btn-hover);
}

.btn-checkout:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

/* ===== 小按钮样式系统 ===== */
.btn-sm {
  padding: 5px 14px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}

.btn-sm:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-sm:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
  color: #fff;
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  padding: 5px 8px;
}

.btn-sm.text:hover {
  color: var(--color-primary);
}

.btn-sm.text.danger:hover {
  color: var(--color-primary);
}

/* ===== 响应式设计 ===== */

/* 中等屏幕：右侧面板宽度缩小到 260px */
@media (max-width: 1024px) {
  .cart-sidebar {
    width: 260px;
  }
}

/* 小屏幕：改为上下布局（右侧面板移到下方） */
@media (max-width: 768px) {
  .cart-page {
    padding: 16px;
  }

  .cart-title {
    font-size: 20px;
  }

  .cart-count-tip {
    font-size: 12px;
  }

  /* 改为上下布局 */
  .cart-body {
    flex-direction: column;
    gap: 16px;
  }

  .cart-sidebar {
    width: 100%;
    position: static;
    align-self: stretch;
  }

  .cart-table-head,
  .cart-row {
    grid-template-columns: 40px 1fr 80px 110px 80px 60px;
    padding: 10px 8px;
    font-size: 12px;
  }

  .product-img {
    width: 60px;
    height: 60px;
  }

  .col-info {
    gap: 8px;
  }

  .product-name {
    font-size: 13px;
  }

  .subtotal-text {
    font-size: 14px;
  }

  .summary-total-value {
    font-size: 20px;
  }

  .btn-sm {
    padding: 4px 10px;
    font-size: 11px;
  }

  /* 小屏幕下页头右侧按钮间距收紧 */
  .cart-header-right {
    gap: 8px;
  }
}
</style>
