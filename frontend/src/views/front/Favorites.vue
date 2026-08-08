<template>
  <!-- 收藏夹页面：参考京东 + 淘宝收藏夹设计 -->
  <div class="favorites-page">
    <!-- 顶部工具栏 (参考淘宝) -->
    <div class="fav-toolbar">
      <!-- 左侧：标题 + 数量 -->
      <div class="toolbar-left">
        <h2 class="fav-title">我的收藏</h2>
        <span class="fav-count">共 {{ filteredList.length }} 件商品</span>
      </div>
      <!-- 右侧：筛选标签 + 管理按钮 -->
      <div class="toolbar-right">
        <div class="filter-tabs">
          <span class="filter-tab" :class="{ active: filterType === 'all' }" @click="filterType = 'all'">全部</span>
          <span class="filter-tab" :class="{ active: filterType === 'ON_SALE' }"
            @click="filterType = 'ON_SALE'">在售</span>
          <span class="filter-tab" :class="{ active: filterType === 'OFF_SHELF' }"
            @click="filterType = 'OFF_SHELF'">已下架</span>
        </div>
        <button v-if="!manageMode && favoriteList.length > 0" class="btn-manage" @click="manageMode = true">管理</button>
        <button v-else-if="manageMode" class="btn-manage" @click="exitManage">退出管理</button>
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
    <div v-else-if="favoriteList.length === 0" class="empty-state">
      <el-empty description="还没有收藏任何商品，快去发现心仪好物吧！" :image-size="120" />
      <button class="btn-sm primary" type="button" @click="router.push('/products')">去逛逛</button>
    </div>

    <!-- 主体: 卡片网格 + 分页 + 批量操作栏 -->
    <template v-else>
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
      <div v-if="filteredList.length > 20" class="fav-pagination">
        <PaginationWrapper :total="filteredList.length" :page-num="pageNum" :page-size="pageSize"
          :page-sizes="[20, 40, 60]" @change="handlePageChange" />
      </div>

      <!-- 管理模式底部批量操作栏 (参考京东) -->
      <div v-if="manageMode" class="batch-bar">
        <div class="batch-left">
          <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate"
            @change="handleToggleAll">全选</el-checkbox>
          <span class="batch-count">已选 {{ selectedIds.length }} 件</span>
        </div>
        <div class="batch-right">
          <button class="btn-batch-remove" type="button" :disabled="selectedIds.length === 0 || batchRemoving"
            @click="handleBatchRemove">
            {{ batchRemoving ? '取消中...' : '取消收藏' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * 收藏夹页面 (前台)
 * 对接后端 /api/v1/favorites 接口，无模拟数据。
 * 参考 京东 + 淘宝收藏夹设计：筛选 / 管理 / 批量取消 / 分页。
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

/** 管理模式 */
const manageMode = ref<boolean>(false)

/** 选中的商品 ID 列表 */
const selectedIds = ref<(number | string)[]>([])

/** 批量取消中 */
const batchRemoving = ref<boolean>(false)

/** 分页参数 */
const pageNum = ref<number>(1)
const pageSize = ref<number>(20)

/* === 计算属性 === */

/** 筛选后的列表 */
const filteredList = computed<FavoriteItemVO[]>(() => {
  if (filterType.value === 'all') return favoriteList.value
  return favoriteList.value.filter(item => item.productStatus === filterType.value)
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

/** 分页变化 */
function handlePageChange(payload: { pageNum: number; pageSize: number }): void {
  pageNum.value = payload.pageNum
  pageSize.value = payload.pageSize
}

/* === 监听 === */

// 筛选类型变化时重置页码
watch(filterType, () => {
  pageNum.value = 1
})

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

/* === 顶部工具栏 === */
.fav-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.fav-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.fav-count {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 筛选标签 (胶囊样式) */
.filter-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--color-bg-subtle);
  border-radius: 20px;
  padding: 4px;
}

.filter-tab {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  white-space: nowrap;
}

.filter-tab:hover {
  color: var(--color-text-primary);
}

.filter-tab.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

/* 管理按钮 */
.btn-manage {
  padding: 6px 16px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.15s;
}

.btn-manage:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
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
}

/* === 收藏商品卡片网格 (大屏 5 列, 参考淘宝) === */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
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
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.fav-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
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
  font-size: 18px;
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
  margin-top: 16px;
}

/* === 管理模式底部批量操作栏 (参考京东, sticky) === */
.batch-bar {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid var(--color-border);
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.batch-count {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.batch-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-batch-remove {
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background 0.15s;
  letter-spacing: 0.02em;
}

.btn-batch-remove:hover:not(:disabled) {
  background: var(--btn-hover);
}

.btn-batch-remove:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
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
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .fav-price {
    font-size: 16px;
  }

  .fav-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left,
  .toolbar-right {
    justify-content: space-between;
  }

  .batch-bar {
    padding: 12px 16px;
  }

  .btn-batch-remove {
    padding: 8px 16px;
  }
}

@media (max-width: 480px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
}
</style>
