<template>
  <!-- 收藏夹页面：两列布局，参考 UserOrders.vue -->
  <div class="favorites-page">
    <div class="fav-body">
      <!-- 左侧 sidebar：标题 + 商品状态筛选 + 排序方式 + 管理按钮 -->
      <aside class="fav-sidebar">
        <div class="sidebar-title">我的收藏</div>
        <nav class="sidebar-nav">
          <!-- 商品状态筛选 -->
          <div class="nav-group-title nav-group-title--first">商品状态</div>
          <div class="nav-item" :class="{ active: filterType === 'all' }" @click="handleFilterChange('all')">
            <span class="nav-label">全部</span>
          </div>
          <div class="nav-item" :class="{ active: filterType === 'ON_SALE' }" @click="handleFilterChange('ON_SALE')">
            <span class="nav-label">在售</span>
          </div>
          <div class="nav-item" :class="{ active: filterType === 'OFF_SHELF' }"
            @click="handleFilterChange('OFF_SHELF')">
            <span class="nav-label">已下架</span>
          </div>

          <!-- 排序方式 -->
          <div class="nav-group-title">排序方式</div>
          <div class="nav-item" :class="{ active: sortType === 'default' }" @click="handleSortChange('default')">
            <span class="nav-label">综合排序</span>
          </div>
          <div class="nav-item" :class="{ active: sortType === 'priceAsc' }" @click="handleSortChange('priceAsc')">
            <span class="nav-label">价格升序</span>
          </div>
          <div class="nav-item" :class="{ active: sortType === 'priceDesc' }" @click="handleSortChange('priceDesc')">
            <span class="nav-label">价格降序</span>
          </div>
          <div class="nav-item" :class="{ active: sortType === 'sales' }" @click="handleSortChange('sales')">
            <span class="nav-label">销量优先</span>
          </div>
        </nav>
        <!-- sidebar 底部：管理按钮 / 管理模式下批量操作 -->
        <div class="sidebar-footer">
          <!-- 非管理模式：管理按钮 -->
          <button v-if="!manageMode && favoriteList.length > 0" class="btn-manage" type="button"
            @click="manageMode = true">
            管理收藏
          </button>
          <!-- 管理模式：全选 + 批量操作 + 退出管理 -->
          <template v-else-if="manageMode">
            <div class="sidebar-batch-info">
              <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate"
                @change="handleToggleAll">全选</el-checkbox>
              <span class="sidebar-batch-count">已选 {{ selectedIds.length }} 件</span>
            </div>
            <button class="btn-batch-cart" type="button" :disabled="selectedIds.length === 0 || batchAdding"
              @click="handleBatchAddCart">
              {{ batchAdding ? '加入中...' : '批量加入购物车' }}
            </button>
            <button class="btn-batch-remove" type="button" :disabled="selectedIds.length === 0 || batchRemoving"
              @click="handleBatchRemove">
              {{ batchRemoving ? '取消中...' : '批量取消收藏' }}
            </button>
            <button class="btn-manage active" type="button" @click="exitManage">退出管理</button>
          </template>
        </div>
      </aside>

      <!-- 右侧主区域 -->
      <div class="fav-main">
        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          <span class="loading-text">加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="favoriteList.length === 0" class="empty-state">
          <el-empty description="还没有收藏任何商品，快去发现心仪好物吧！" :image-size="120" />
          <button class="btn-sm primary" type="button" @click="router.push('/products')">去逛逛</button>
        </div>

        <!-- 主体: 商品数量提示 + 卡片网格 + 分页 + 批量操作栏 -->
        <template v-else>
          <div class="fav-count-bar">共 {{ filteredList.length }} 件商品</div>

          <!-- 卡片网格 (4列) -->
          <div class="favorites-grid">
            <div v-for="item in pagedList" :key="item.id" class="fav-card"
              :class="{ disabled: item.productStatus !== 'ON_SALE', selected: selectedIds.includes(item.productId) }">
              <!-- 图片区域 -->
              <div class="fav-card-img" @click="!manageMode && goProductDetail(item.productId)">
                <img v-if="item.mainImage" :src="formatImageUrl(item.mainImage)" :alt="item.productName"
                  class="fav-img-tag" loading="lazy" />
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                  class="fav-img-placeholder">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m21 15-5-5L5 21" />
                </svg>
                <!-- 管理模式复选框 -->
                <div v-if="manageMode" class="fav-checkbox" @click.stop="toggleSelect(item.productId)">
                  <el-checkbox :model-value="selectedIds.includes(item.productId)" />
                </div>
                <!-- 下架遮罩 -->
                <div v-if="item.productStatus !== 'ON_SALE'" class="off-shelf-mask">
                  <span>已下架</span>
                </div>
                <!-- 非管理模式悬浮加入购物车按钮 -->
                <div v-if="!manageMode && item.productStatus === 'ON_SALE'" class="fav-hover-actions">
                  <button class="btn-cart" type="button" :disabled="addingId === item.productId"
                    @click.stop="handleAddToCart(item)">
                    {{ addingId === item.productId ? '加入中...' : '加入购物车' }}
                  </button>
                </div>
              </div>

              <!-- 卡片内容 -->
              <div class="fav-card-body">
                <div class="fav-name" @click="!manageMode && goProductDetail(item.productId)" :title="item.productName">
                  {{ item.productName }}
                </div>
                <div class="fav-meta">
                  <span class="fav-price">¥{{ formatPrice(item.originalPrice) }}</span>
                  <span class="fav-sales">已售 {{ item.salesCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="filteredList.length > pageSize" class="fav-pagination">
            <PaginationWrapper :total="filteredList.length" :page-num="pageNum" :page-size="pageSize"
              :page-sizes="[20, 40, 60]" @change="handlePageChange" />
          </div>

        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 收藏夹页面 (前台)
 * 对接后端 /api/v1/favorites 接口，无模拟数据。
 * 两列布局：左侧 sidebar(状态筛选+排序+管理) + 右侧卡片网格+批量操作。
 */
defineOptions({ name: 'Favorites' })
import { ref, computed, watch, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { addCart } from '@/api/cart'
import { useCartStore } from '@/stores/cart'
import { formatImageUrl } from '@/utils/image'
import type { FavoriteItemVO } from '@/types'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const router = useRouter()
const cartStore = useCartStore()

/** 收藏夹列表 (后端返回) */
const favoriteList = ref<FavoriteItemVO[]>([])

/** 列表加载中 */
const loading = ref<boolean>(false)

/** 正在加入购物车的商品 ID (按钮 loading 态) */
const addingId = ref<number | string | null>(null)

/** 正在取消收藏的商品 ID (按钮 loading 态) */
const removingId = ref<number | string | null>(null)

/** 筛选类型: all-全部 / ON_SALE-在售 / OFF_SHELF-已下架 */
const filterType = ref<'all' | 'ON_SALE' | 'OFF_SHELF'>('all')

/** 排序类型: default-综合 / priceAsc-价格升序 / priceDesc-价格降序 / sales-销量优先 */
const sortType = ref<'default' | 'priceAsc' | 'priceDesc' | 'sales'>('default')

/** 管理模式 */
const manageMode = ref<boolean>(false)

/** 选中的商品 ID 列表 */
const selectedIds = ref<(number | string)[]>([])

/** 批量取消中 */
const batchRemoving = ref<boolean>(false)

/** 批量加入购物车中 */
const batchAdding = ref<boolean>(false)

/** 分页参数 */
const pageNum = ref<number>(1)
const pageSize = ref<number>(20)

/* === 计算属性 === */

/** 筛选 + 排序后的列表 */
const filteredList = computed<FavoriteItemVO[]>(() => {
  // 状态筛选
  let list = favoriteList.value
  if (filterType.value !== 'all') {
    list = list.filter(item => item.productStatus === filterType.value)
  }
  // 排序
  const sorted = [...list]
  switch (sortType.value) {
    case 'priceAsc':
      sorted.sort((a, b) => (a.originalPrice || 0) - (b.originalPrice || 0))
      break
    case 'priceDesc':
      sorted.sort((a, b) => (b.originalPrice || 0) - (a.originalPrice || 0))
      break
    case 'sales':
      sorted.sort((a, b) => (b.salesCount || 0) - (a.salesCount || 0))
      break
    case 'default':
    default:
      break
  }
  return sorted
})

/** 分页后的列表 (前端假分页) */
const pagedList = computed<FavoriteItemVO[]>(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

/** 是否全选 (基于当前筛选列表) */
const isAllSelected = computed<boolean>(() => {
  if (filteredList.value.length === 0) return false
  return filteredList.value.every(item => selectedIds.value.includes(item.productId))
})

/** 是否半选状态 */
const isIndeterminate = computed<boolean>(() => {
  const selected = filteredList.value.filter(item => selectedIds.value.includes(item.productId)).length
  return selected > 0 && selected < filteredList.value.length
})

/* === 工具函数 === */

/** 格式化价格 (保留两位小数) */
function formatPrice(value: number): string {
  return (value || 0).toFixed(2)
}

/** 加载收藏夹列表 */
async function loadFavoriteList(): Promise<void> {
  loading.value = true
  try {
    const res = await getFavoriteList()
    favoriteList.value = res.data ?? []
  } catch {
    favoriteList.value = []
  } finally {
    loading.value = false
  }
}

/* === 事件处理 === */

/** 跳转商品详情 */
function goProductDetail(id: number | string): void {
  router.push(`/products/${id}`)
}

/** 加入购物车 */
async function handleAddToCart(item: FavoriteItemVO): Promise<void> {
  if (item.productStatus !== 'ON_SALE') {
    ElMessage.warning('该商品已下架，无法加入购物车')
    return
  }
  addingId.value = item.productId
  try {
    await addCart({ productId: item.productId, quantity: 1 })
    ElMessage.success('已加入购物车')
    // 刷新顶部导航购物车数量徽标
    await cartStore.fetchCount()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    addingId.value = null
  }
}

/** 取消收藏 (单个, 保留原入口以兼容外部调用) */
async function handleRemoveFavorite(item: FavoriteItemVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定取消收藏"${item.productName}"吗？`, '取消收藏确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  removingId.value = item.productId
  try {
    await removeFavorite(item.productId)
    ElMessage.success('已取消收藏')
    // 从列表中移除 (乐观更新, 避免整页刷新)
    favoriteList.value = favoriteList.value.filter(f => f.productId !== item.productId)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    removingId.value = null
  }
}

/** 切换选中 */
function toggleSelect(productId: number | string): void {
  const idx = selectedIds.value.indexOf(productId)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(productId)
}

/** 全选 / 取消全选 */
function handleToggleAll(val: boolean | string | number): void {
  const selected = Boolean(val)
  if (selected) {
    selectedIds.value = filteredList.value.map(item => item.productId)
  } else {
    selectedIds.value = []
  }
}

/** 退出管理模式 */
function exitManage(): void {
  manageMode.value = false
  selectedIds.value = []
}

/** 批量取消收藏 */
async function handleBatchRemove(): Promise<void> {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定取消收藏选中的 ${selectedIds.value.length} 件商品吗？`, '批量取消收藏', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  batchRemoving.value = true
  try {
    for (const productId of selectedIds.value) {
      await removeFavorite(productId)
    }
    ElMessage.success('已取消收藏')
    // 从列表中移除 (乐观更新)
    favoriteList.value = favoriteList.value.filter(f => !selectedIds.value.includes(f.productId))
    selectedIds.value = []
    manageMode.value = false
  } catch {
    // 失败时重新拉取列表, 保证数据一致
    await loadFavoriteList()
  } finally {
    batchRemoving.value = false
  }
}

/** 批量加入购物车 */
async function handleBatchAddCart(): Promise<void> {
  if (selectedIds.value.length === 0) return
  batchAdding.value = true
  let successCount = 0
  let failCount = 0
  try {
    for (const productId of selectedIds.value) {
      const item = favoriteList.value.find(f => f.productId === productId)
      if (item && item.productStatus === 'ON_SALE') {
        try {
          await addCart({ productId, quantity: 1 })
          successCount++
        } catch {
          failCount++
        }
      } else {
        failCount++
      }
    }
    if (successCount > 0) {
      ElMessage.success(`已将 ${successCount} 件商品加入购物车${failCount > 0 ? `，${failCount} 件失败` : ''}`)
      await cartStore.fetchCount()
    } else if (failCount > 0) {
      ElMessage.error('加入购物车失败')
    }
    selectedIds.value = []
    manageMode.value = false
  } finally {
    batchAdding.value = false
  }
}

/** 筛选变化处理 */
function handleFilterChange(type: 'all' | 'ON_SALE' | 'OFF_SHELF'): void {
  filterType.value = type
  pageNum.value = 1
}

/** 排序变化处理 */
function handleSortChange(type: 'default' | 'priceAsc' | 'priceDesc' | 'sales'): void {
  sortType.value = type
  pageNum.value = 1
}

/** 分页变化 */
function handlePageChange(payload: { pageNum: number; pageSize: number }): void {
  pageNum.value = payload.pageNum
  pageSize.value = payload.pageSize
}

/* === 监听 === */

// 页面挂载时加载收藏夹列表
onMounted(() => {
  loadFavoriteList()
})

// keep-alive 缓存后, 再次激活时刷新收藏夹列表 (保证从其他页面返回时数据新鲜)
onActivated(() => {
  loadFavoriteList()
})
</script>

<style scoped>
/* === 页面容器 === */
.favorites-page {
  padding: 24px;
  padding-bottom: 80px;
  position: relative;
}

/* === 两列布局主体 === */
.fav-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* === 左侧 sidebar === */
.fav-sidebar {
  width: 200px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
  align-self: flex-start;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-light, var(--color-border));
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* 分组标题 */
.nav-group-title {
  padding: 12px 20px 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
  border-top: 1px solid var(--color-border-light, var(--color-border));
}

/* 第一个分组标题紧挨 sidebar-title(已有 border-bottom), 去掉 border-top 避免双线 */
.nav-group-title--first {
  border-top: none;
}

/* 导航项 */
.nav-item {
  padding: 12px 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 1px solid var(--color-border-light, var(--color-border));
  user-select: none;
}

.nav-item:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-primary);
}

.nav-item.active {
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-light, rgba(229, 57, 53, 0.08));
  border-left: 3px solid var(--color-primary);
  padding-left: 17px;
}

.nav-label {
  line-height: 1.4;
}

/* sidebar 底部管理按钮 */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-manage {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}

.btn-manage:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.btn-manage.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

/* sidebar 批量操作区域 */
.sidebar-batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 4px 0;
}

.sidebar-batch-count {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* sidebar 内批量按钮 (宽度100%) */
.sidebar-footer .btn-batch-cart,
.sidebar-footer .btn-batch-remove {
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  letter-spacing: 0.02em;
  text-align: center;
}

.sidebar-footer .btn-batch-cart {
  color: var(--color-primary);
  background: #fff;
  border: 1px solid var(--color-primary);
}

.sidebar-footer .btn-batch-cart:hover:not(:disabled) {
  background: var(--color-primary-light, rgba(229, 57, 53, 0.08));
}

.sidebar-footer .btn-batch-cart:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.sidebar-footer .btn-batch-remove {
  color: #fff;
  background: var(--color-primary);
  border: none;
}

.sidebar-footer .btn-batch-remove:hover:not(:disabled) {
  background: var(--btn-hover);
}

.sidebar-footer .btn-batch-remove:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: var(--btn-disabled-bg, #ccc);
}

/* === 右侧主区域 === */
.fav-main {
  flex: 1;
  min-width: 0;
}

/* === 加载中状态 === */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--color-text-muted);
  gap: 12px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* === 空状态 === */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px 40px;
  text-align: center;
  gap: 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

/* === 商品数量提示 === */
.fav-count-bar {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

/* === 收藏商品卡片网格 (6列) === */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

/* === 收藏卡片 === */
.fav-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
}

.fav-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
}

.fav-card.disabled {
  opacity: 0.85;
}

.fav-card.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

/* === 卡片图片区域 (正方形) === */
.fav-card-img {
  width: 100%;
  aspect-ratio: 1;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.fav-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.fav-card:hover .fav-img-tag {
  transform: scale(1.05);
}

.fav-img-placeholder {
  width: 48px;
  height: 48px;
  color: #ccc;
}

/* 管理模式复选框 (左上角) */
.fav-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 3;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  padding: 2px;
  cursor: pointer;
}

/* 下架遮罩 */
.off-shelf-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.05em;
  z-index: 2;
}

/* 非管理模式悬浮加入购物车按钮 (渐变遮罩从下往上) */
.fav-hover-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px 12px 10px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  display: flex;
  justify-content: center;
  opacity: 0;
  transform: translateY(8px);
  transition: opacity 0.2s, transform 0.2s;
  z-index: 2;
}

.fav-card:hover .fav-hover-actions {
  opacity: 1;
  transform: translateY(0);
}

.btn-cart {
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background 0.15s;
  letter-spacing: 0.02em;
}

.btn-cart:hover:not(:disabled) {
  background: var(--btn-hover);
}

.btn-cart:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

/* === 卡片内容 === */
.fav-card-body {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.fav-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
  word-break: break-all;
}

.fav-name:hover {
  color: var(--color-primary);
}

.fav-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.fav-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'DIN Alternate', 'Roboto', 'Arial', sans-serif;
  line-height: 1.2;
}

.fav-sales {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* === 分页 === */
.fav-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}


/* === 空状态去逛逛按钮 === */
.btn-sm {
  padding: 6px 12px;
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
  justify-content: center;
  gap: 4px;
  transition: all 0.15s;
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

.btn-sm.primary:hover:not(:disabled) {
  background: var(--btn-hover);
}

/* === 响应式 === */
@media (max-width: 1200px) {
  .favorites-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .fav-body {
    flex-direction: column;
  }

  .fav-sidebar {
    width: 100%;
    position: static;
    align-self: stretch;
  }

  .favorites-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  .fav-price {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
}
</style>
