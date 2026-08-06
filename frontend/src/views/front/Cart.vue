<template>
  <!-- 购物车页面：参照 UserAddress.vue / UserOrders.vue 卡片风格 -->
  <div class="cart-page">
    <!-- 页头 -->
    <div class="cart-header">
      <h2 class="cart-title">我的购物车</h2>
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

    <!-- 购物车列表 -->
    <div v-else class="cart-content">
      <!-- 表头 -->
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
      <div v-for="item in cartList" :key="item.id" class="cart-row"
        :class="{ disabled: item.productStatus !== 'ON_SALE' }">
        <!-- 复选框 -->
        <div class="col-check">
          <el-checkbox :model-value="item.selected" :disabled="item.productStatus !== 'ON_SALE'"
            @change="(val: boolean | string | number) => handleToggleSelect(item, Boolean(val))" />
        </div>

        <!-- 商品信息: 图片 + 名称 + SKU属性 -->
        <div class="col-info">
          <div class="product-img" @click="goProductDetail(item.productId)">
            <img v-if="item.skuMainImage || item.mainImage" :src="formatImageUrl(item.skuMainImage || item.mainImage)"
              :alt="item.productName" class="product-img-tag" loading="lazy" />
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
              <svg class="product-sku-attrs-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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

      <!-- 底部操作栏 (固定在视口底部) -->
      <div class="cart-footer">
        <div class="footer-left">
          <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate"
            @change="handleToggleAll">全选</el-checkbox>
          <button class="btn-sm" type="button" :disabled="selectedCount === 0" @click="handleBatchDelete">删除选中</button>
          <button class="btn-sm" type="button" @click="handleClear">清空购物车</button>
        </div>
        <div class="footer-right">
          <div class="total-info">
            已选 <span class="total-count">{{ selectedCount }}</span> 件商品
            合计：<span class="total-amount">¥{{ formatPrice(totalAmount) }}</span>
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
/* 参照 UserAddress.vue .address-page 样式 */
.cart-page {
  padding: 24px;
  padding-bottom: 80px;
}

/* 页头 */
.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.cart-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

/* 加载中状态 */
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

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px 40px;
  text-align: center;
  gap: 16px;
}

/* 购物车内容 */
.cart-content {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* 表头 */
.cart-table-head {
  display: grid;
  grid-template-columns: 80px 1fr 120px 160px 120px 80px;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid var(--color-border);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 商品行 */
.cart-row {
  display: grid;
  grid-template-columns: 80px 1fr 120px 160px 120px 80px;
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

.cart-row.disabled {
  opacity: 0.6;
}

/* 复选框列 */
.col-check {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 商品信息列 */
.col-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.product-img {
  width: 80px;
  height: 80px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
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

.product-info {
  flex: 1;
  min-width: 0;
}

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
}

.product-name:hover {
  color: var(--color-primary);
}

.product-status-tag {
  display: inline-block;
  margin-top: 4px;
  padding: 1px 6px;
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
  border-radius: 3px;
}

/* 7.2 购物车展示 SKU 属性 */
.product-sku-attrs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle, #f5f5f5);
  border-radius: 4px;
  border: 1px solid var(--color-border-light, #eee);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-sku-attrs-icon {
  width: 12px;
  height: 12px;
  color: var(--color-text-muted, #999);
  flex-shrink: 0;
}

/* 单价列 */
.col-price,
.col-subtotal {
  text-align: center;
}

.price-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.subtotal-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

/* 数量列 */
.col-quantity {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stock-warn {
  font-size: 11px;
  color: #ff9800;
}

/* 操作列 */
.col-action {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 底部操作栏 */
.cart-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #ffffff;
  border-top: 1px solid var(--color-border);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 50;
  box-sizing: border-box;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.total-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.total-count {
  font-weight: 700;
  color: var(--color-primary);
}

.total-amount {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-primary);
  margin-left: 4px;
}

/* 结算按钮 */
.btn-checkout {
  padding: 8px 32px;
  font-size: 14px;
  font-weight: 700;
  border-radius: 4px;
  cursor: pointer;
  border: none;
  background: var(--color-primary);
  color: #ffffff;
  letter-spacing: 0.02em;
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

/* 小按钮 (对照 UserAddress.vue .btn-sm) */
.btn-sm {
  padding: 5px 14px;
  border-radius: 4px;
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

/* 响应式 */
@media (max-width: 1024px) {

  .cart-table-head,
  .cart-row {
    grid-template-columns: 60px 1fr 100px 140px 100px 70px;
  }
}

@media (max-width: 768px) {

  .cart-table-head,
  .cart-row {
    grid-template-columns: 40px 1fr 80px 120px 80px 60px;
    padding: 12px 8px;
    font-size: 12px;
  }

  .product-img {
    width: 60px;
    height: 60px;
  }

  .cart-footer {
    padding: 10px 12px;
  }

  .footer-left {
    gap: 8px;
  }

  .footer-right {
    gap: 12px;
  }
}
</style>