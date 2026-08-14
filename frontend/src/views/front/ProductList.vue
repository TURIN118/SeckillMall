<template>
  <!-- 商品分类/搜索页 (参考淘宝: 左侧分类树 + 筛选区 + 商品网格 + 分页) -->
  <div class="category-page">
    <!-- 双栏布局: 左侧分类树 + 右侧商品区 -->
    <div class="main-layout">
      <!-- 左侧分类树 (参考首页 Home.vue category-sidebar 风格) -->
      <aside class="category-tree" @mouseleave="handleTreeLeaveAll">
        <!-- 滚动容器：包裹分类项，超出可滚动 -->
        <div class="tree-scroll" ref="treeScrollRef">
          <div class="tree-item all" :class="{ active: !categoryId }" @click="handleCategoryClick(undefined)">
            <span class="tree-name">全部分类</span>
          </div>

          <div v-for="cat in categoryList" :key="cat.id" class="tree-item" :class="{ active: isCategoryActive(cat.id) }"
            @click="handleCategoryClick(cat.id)" @mouseenter="handleTreeEnter(cat.id, $event)"
            @mouseleave="handleTreeLeave(cat.id)">
            <span class="tree-name">{{ cat.categoryName }}</span>
            <span v-if="cat.children && cat.children.length > 0" class="tree-arrow" aria-hidden="true">&#8250;</span>
          </div>

          <!-- 空状态 -->
          <div v-if="categoryList.length === 0" class="tree-empty">暂无分类</div>
        </div>

        <!-- 二级分类浮层提取到外层，不受滚动容器 overflow 裁剪 -->
        <div v-if="hoverPanelData" class="tree-panel" :style="{ top: panelTop + 'px' }" @mouseenter="handlePanelEnter"
          @mouseleave="handlePanelLeave">
          <div class="panel-header">
            <span class="panel-title">{{ hoverPanelData.categoryName }}</span>
            <span class="panel-hint">全部分类</span>
          </div>
          <div class="panel-content">
            <div v-for="child in hoverPanelData.children" :key="child.id" class="panel-item"
              :class="{ active: String(child.id) === categoryId }" @click="handleCategoryClick(child.id)">{{
                child.categoryName
              }}
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧商品区 -->
      <div class="product-area">

        <!-- T14: AI 导购助手 (可折叠, 默认展开) -->
        <AIShoppingAssistant />

        <!-- 筛选排序栏 -->
        <div class="filter-bar">
          <!-- 排序 Tab -->
          <div class="sort-tabs">
            <span v-for="opt in sortOptions" :key="opt.value" class="sort-tab"
              :class="{ active: sortType === opt.value }" @click="handleSortChange(opt.value)">{{ opt.label }}</span>
          </div>

          <!-- 价格区间筛选 -->
          <div class="price-filter">
            <span class="price-label">价格</span>
            <input v-model="minPriceInput" type="number" class="price-input" placeholder="最低价" min="0"
              @keyup.enter="handlePriceFilter" />
            <span class="price-sep">~</span>
            <input v-model="maxPriceInput" type="number" class="price-input" placeholder="最高价" min="0"
              @keyup.enter="handlePriceFilter" />
            <button class="price-btn" type="button" @click="handlePriceFilter">确定</button>
            <button v-if="minPrice !== undefined || maxPrice !== undefined" class="price-btn text" type="button"
              @click="clearPriceFilter">清除</button>
          </div>
        </div>

        <!-- 商品主体区域：独立滚动容器，分页器固定底部 -->
        <div class="product-main">
          <!-- 滚动内容区：只有此区域滚动 -->
          <div class="product-scroll">
            <!-- 加载骨架屏 -->
            <div v-if="loading" class="skeleton-grid">
              <div v-for="i in 8" :key="i" class="skeleton-card"></div>
            </div>

            <template v-else>
              <!-- 商品网格 -->
              <div v-if="productList.length > 0" class="product-grid">
                <div v-for="product in productList" :key="product.id" class="product-card"
                  @click="goProductDetail(product.id)">
                  <!-- 图片 -->
                  <div class="card-img">
                    <el-image v-if="product.images && product.images.length > 0"
                      :src="formatImageUrl(product.images[0])" fit="cover" lazy class="card-image">
                      <template #error>
                        <div class="img-placeholder">
                          <el-icon :size="48">
                            <Picture />
                          </el-icon>
                        </div>
                      </template>
                    </el-image>
                    <div v-else class="img-placeholder">
                      <el-icon :size="48">
                        <Picture />
                      </el-icon>
                    </div>
                    <!-- 售罄遮罩 -->
                    <div v-if="product.stock <= 0" class="sold-out-overlay">已售罄</div>
                  </div>

                  <!-- 卡片内容 -->
                  <div class="card-body">
                    <!-- 名称 (2行省略) -->
                    <div class="card-name">{{ product.productName }}</div>
                    <!-- 价格 -->
                    <div class="card-prices">
                      <span class="card-price">¥{{ formatPrice(product.originalPrice) }}</span>
                    </div>
                    <!-- 销量 -->
                    <div class="card-sales">已售 {{ product.salesCount || 0 }} 件</div>
                    <!-- 加入购物车 -->
                    <div class="card-actions">
                      <button class="btn-cart" type="button" :disabled="addingId === product.id || product.stock <= 0"
                        @click.stop="handleAddToCart(product)">
                        <span v-if="addingId === product.id">加入中...</span>
                        <span v-else-if="product.stock <= 0">已售罄</span>
                        <span v-else>加入购物车</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-else class="empty-state">
                <div class="empty-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" />
                    <path d="m21 15-5-5L5 21" />
                  </svg>
                </div>
                <p class="empty-text">没有找到符合条件的商品</p>
                <button class="btn-sm primary" @click="resetFilters">重置筛选</button>
              </div>
            </template>
          </div>

          <!-- 分页器固定底部（不随商品列表滚动） -->
          <div v-if="!loading && total > 0" class="pagination-bar">
            <PaginationWrapper :total="total" :page-num="pageNum" :page-size="pageSize" :page-sizes="[20, 40, 60]"
              @change="handlePageChange" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P02 商品列表/分类页 (参考淘宝重构)
 * 布局: 顶部面包屑 + 左侧分类树 + 右侧商品区(筛选排序+价格区间+商品网格+分页)
 * 搜索和分类浏览共用此页面 (通过 URL query 的 keyword 或 categoryId 区分)
 */
import { ref, computed, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { addCart } from '@/api/cart'
import { useCartStore } from '@/stores/cart'
import { useCategoryStore } from '@/stores/category'
import { formatImageUrl } from '@/utils/image'
import PaginationWrapper from '@/components/PaginationWrapper.vue'
import AIShoppingAssistant from '@/components/AIShoppingAssistant.vue'
import type { ProductVO, CategoryTreeNode, ProductQueryRequest } from '@/types'

// 显式声明组件名, 使 keep-alive 的 include 匹配生效
defineOptions({ name: 'ProductList' })

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const categoryStore = useCategoryStore()

/* === 状态 === */
const loading = ref<boolean>(false)
const productList = ref<ProductVO[]>([])
// 分类列表改为从 categoryStore 获取 (全站缓存, 避免重复请求)
const categoryList = computed<CategoryTreeNode[]>(() => categoryStore.tree)
const total = ref<number>(0)

// 统一使用 string 类型: 后端 Long 字段经 JSON 序列化为 string,
// 与 cat.id 比较时需保持类型一致, 避免 "701" === 701 为 false 导致面包屑匹配失败
const categoryId = ref<string | undefined>(undefined)
const keyword = ref<string | undefined>(undefined)
const sortType = ref<string>('default')
const pageNum = ref<number>(1)
const pageSize = ref<number>(20)

// 价格区间筛选: input 为输入框绑定(字符串), 实际值为应用后的数字
const minPriceInput = ref<string>('')
const maxPriceInput = ref<string>('')
const minPrice = ref<number | undefined>(undefined)
const maxPrice = ref<number | undefined>(undefined)

// 加入购物车 loading
const addingId = ref<number | string | null>(null)

// 分类树滚动容器引用
const treeScrollRef = ref<HTMLElement | null>(null)
// 浮层 top 位置
const panelTop = ref<number>(0)
// hover 的分类 ID
const hoverCategoryId = ref<number | string | null>(null)
// hover 延迟定时器（互斥：enter/leave 不会同时存在等待中的定时器）
let hoverEnterTimer: ReturnType<typeof setTimeout> | null = null
let hoverLeaveTimer: ReturnType<typeof setTimeout> | null = null
const HOVER_DELAY = 200 // ms
// hover 的分类数据（含 children）
const hoverPanelData = computed<CategoryTreeNode | null>(() => {
  if (hoverCategoryId.value === null) return null
  return categoryList.value.find(c => c.id === hoverCategoryId.value) || null
})

/* === 排序选项 (参考淘宝: 综合/价格升/价格降/销量/最新) === */
const sortOptions = [
  { label: '综合排序', value: 'default' },
  { label: '价格升序', value: 'priceAsc' },
  { label: '价格降序', value: 'priceDesc' },
  { label: '销量优先', value: 'sales' },
  { label: '最新上架', value: 'newest' }
]

/** 计算排序参数 (映射到后端 sortBy/sortOrder) */
function getSortParams(): { sortBy?: string; sortOrder?: string } {
  switch (sortType.value) {
    case 'priceAsc':
      return { sortBy: 'originalPrice', sortOrder: 'asc' }
    case 'priceDesc':
      return { sortBy: 'originalPrice', sortOrder: 'desc' }
    case 'sales':
      return { sortBy: 'salesCount', sortOrder: 'desc' }
    case 'newest':
      return { sortBy: 'createTime', sortOrder: 'desc' }
    default:
      // 综合: 默认按创建时间降序
      return { sortBy: 'createTime', sortOrder: 'desc' }
  }
}


/* === 数据拉取 === */

/** 拉取商品列表 */
async function fetchProducts(): Promise<void> {
  loading.value = true
  try {
    const params: ProductQueryRequest = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      // 前台只展示上架商品
      status: 'ON_SALE',
      ...getSortParams()
    }
    if (categoryId.value !== undefined) {
      params.categoryId = categoryId.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (minPrice.value !== undefined) {
      params.minPrice = minPrice.value
    }
    if (maxPrice.value !== undefined) {
      params.maxPrice = maxPrice.value
    }
    const res = await getProductList(params)
    productList.value = res.data.list || []
    total.value = res.data.total
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

/** 拉取分类树 (通过 categoryStore 缓存, 避免重复请求) */
async function fetchCategories(): Promise<void> {
  await categoryStore.fetchTree()
}

/* === 工具函数 === */

/** 格式化价格 (保留两位小数) */
function formatPrice(value: number): string {
  return (value || 0).toFixed(2)
}

/**
 * 判断一级分类是否高亮:
 * 当 categoryId 等于该一级分类 id，或等于其某个二级分类 id 时高亮
 * 注意: catId 形参可能为 number 或 string, 统一转 string 后与 categoryId.value(string) 比较
 */
function isCategoryActive(catId: number | string): boolean {
  const idStr = String(catId)
  if (categoryId.value === idStr) return true
  const cat = categoryList.value.find(c => String(c.id) === idStr)
  if (cat && cat.children && cat.children.some(child => String(child.id) === categoryId.value)) {
    return true
  }
  return false
}

// 鼠标进入分类项：延迟显示浮层（避免快速划过时闪烁）
function handleTreeEnter(categoryId: number | string, event: MouseEvent): void {
  // 取消任何正在等待的隐藏
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
  // 若已显示同一项，直接返回
  if (hoverCategoryId.value === categoryId) return
  // 延迟 200ms 显示，避免快速划过时闪烁
  hoverEnterTimer = setTimeout(() => {
    // 计算浮层 top：hover 项相对于滚动容器的 offsetTop - 滚动容器的 scrollTop
    const item = event.currentTarget as HTMLElement
    const scrollContainer = treeScrollRef.value
    if (item && scrollContainer) {
      panelTop.value = item.offsetTop - scrollContainer.scrollTop
    }
    hoverCategoryId.value = categoryId
  }, HOVER_DELAY)
}

// 鼠标离开分类项：延迟隐藏浮层（给用户时间移到浮层上）
function handleTreeLeave(categoryId: number | string): void {
  // 取消任何正在等待的显示
  if (hoverEnterTimer) {
    clearTimeout(hoverEnterTimer)
    hoverEnterTimer = null
  }
  // 若不是当前显示项，直接返回
  if (hoverCategoryId.value !== categoryId) return
  // 延迟 200ms 隐藏，给用户时间移到浮层上
  hoverLeaveTimer = setTimeout(() => {
    hoverCategoryId.value = null
  }, HOVER_DELAY)
}

// 浮层鼠标进入：取消隐藏定时器，保持显示
function handlePanelEnter(): void {
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
}

// 浮层鼠标离开：启动隐藏定时器
function handlePanelLeave(): void {
  // 取消任何正在等待的显示
  if (hoverEnterTimer) {
    clearTimeout(hoverEnterTimer)
    hoverEnterTimer = null
  }
  // 延迟 200ms 隐藏
  hoverLeaveTimer = setTimeout(() => {
    hoverCategoryId.value = null
  }, HOVER_DELAY)
}

// 鼠标离开整个分类树区域：立即隐藏浮层（取消所有待执行的显示/隐藏定时器）
function handleTreeLeaveAll(): void {
  if (hoverEnterTimer) {
    clearTimeout(hoverEnterTimer)
    hoverEnterTimer = null
  }
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
  hoverCategoryId.value = null
}

/* === 事件处理 === */

/** 分类点击: 通过路由跳转驱动状态 (传 undefined 表示点击"全部分类") */
function handleCategoryClick(id: number | string | undefined): void {
  if (id === undefined) {
    router.push({ path: '/products' })
  } else {
    router.push({ path: '/products', query: { categoryId: String(id) } })
  }
}

/** 排序变化 */
function handleSortChange(value: string): void {
  sortType.value = value
  pageNum.value = 1
  fetchProducts()
}

/** 价格区间筛选确定 */
function handlePriceFilter(): void {
  const min = minPriceInput.value.trim()
  const max = maxPriceInput.value.trim()
  const minNum = min === '' ? undefined : Number(min)
  const maxNum = max === '' ? undefined : Number(max)

  // 校验: 非法数字
  if (minNum !== undefined && (isNaN(minNum) || minNum < 0)) {
    ElMessage.warning('请输入有效的最低价')
    return
  }
  if (maxNum !== undefined && (isNaN(maxNum) || maxNum < 0)) {
    ElMessage.warning('请输入有效的最高价')
    return
  }
  // 校验: 最低价不能大于最高价
  if (minNum !== undefined && maxNum !== undefined && minNum > maxNum) {
    ElMessage.warning('最低价不能大于最高价')
    return
  }

  minPrice.value = minNum
  maxPrice.value = maxNum
  pageNum.value = 1
  fetchProducts()
}

/** 清除价格筛选 */
function clearPriceFilter(): void {
  minPriceInput.value = ''
  maxPriceInput.value = ''
  minPrice.value = undefined
  maxPrice.value = undefined
  pageNum.value = 1
  fetchProducts()
}

/** 重置全部筛选 */
function resetFilters(): void {
  sortType.value = 'default'
  minPriceInput.value = ''
  maxPriceInput.value = ''
  minPrice.value = undefined
  maxPrice.value = undefined
  pageNum.value = 1
  // 如果当前有分类或关键词筛选, 通过路由跳转清除 (会触发 watch 重新 fetch)
  if (categoryId.value !== undefined || keyword.value !== undefined) {
    router.push({ path: '/products' })
  } else {
    // 已在无分类/关键词状态, 直接 fetch
    fetchProducts()
  }
}

/** 加入购物车 */
async function handleAddToCart(product: ProductVO): Promise<void> {
  if (product.stock <= 0) {
    ElMessage.warning('商品已售罄，无法加入购物车')
    return
  }
  addingId.value = product.id
  try {
    await addCart({ productId: product.id, quantity: 1 })
    ElMessage.success('已加入购物车')
    // 刷新顶部导航购物车数量徽标
    await cartStore.fetchCount()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    addingId.value = null
  }
}

/** 跳转商品详情 */
function goProductDetail(id: number | string): void {
  router.push(`/products/${id}`)
}


/** 分页变化 */
function handlePageChange(payload: { pageNum: number; pageSize: number }): void {
  pageNum.value = payload.pageNum
  pageSize.value = payload.pageSize
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* === 路由同步 === */

/** 从 URL query 读取分类 ID 和搜索关键词 */
function syncFromRoute(): void {
  // 分类 ID: 优先读 query.categoryId (首页分类侧边栏跳转使用)
  // 保持 string 类型, 不转 Number, 与 cat.id (后端 Long→String 序列化) 类型一致
  const queryCid = route.query.categoryId as string | undefined
  categoryId.value = queryCid || undefined
  // 搜索关键词
  const queryKw = route.query.keyword as string | undefined
  keyword.value = queryKw || undefined
}

onMounted(() => {
  syncFromRoute()
  fetchCategories()
  fetchProducts()
})

/**
 * keep-alive 激活时 (从其他页面返回商品列表):
 *   - 确保分类树已加载 (categoryStore.fetchTree 走缓存, 无重复请求)
 *   - 商品数据不主动重新拉取, 由下方 watch route.query 处理:
 *     若路由 query 变化 (如从首页点击不同分类), watch 会触发重新拉取;
 *     若 query 未变 (如从详情页返回), 保持缓存数据, 减少 loading 闪烁.
 */
onActivated(() => {
  fetchCategories()
})

// 组件卸载时清理 hover 定时器，避免内存泄漏与卸载后状态变更
onUnmounted(() => {
  if (hoverEnterTimer) clearTimeout(hoverEnterTimer)
  if (hoverLeaveTimer) clearTimeout(hoverLeaveTimer)
})

/**
 * 监听路由 query 变化 (从首页分类侧边栏或搜索框跳转时, 同一组件复用需重新拉取)
 * 同时监听 categoryId 和 keyword
 */
watch(
  () => [route.query.categoryId, route.query.keyword],
  () => {
    syncFromRoute()
    pageNum.value = 1
    fetchProducts()
  }
)
</script>

<style scoped>
/* === 页面容器 === */
.category-page {
  padding: 16px 24px 24px;
  box-sizing: border-box;
  /* 最小占满视口减去顶部导航栏 60px，允许内容撑高，页面整体滚动 */
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

/* === 双栏布局 === */
.main-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  /* 占满剩余高度，允许子项收缩 */
  flex: 1;
  min-height: 0;
}

/* === 左侧分类树 (参考首页 Home.vue category-sidebar 风格) === */
.category-tree {
  width: 200px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-sizing: border-box;
  position: sticky;
  top: 80px;
  /* 浮层提取到外层，需要 visible 以便浮层溢出显示 */
  overflow: visible;
  /* 高于 .filter-bar 的 z-index:10, 使 .category-tree 的 stacking context
     高于右侧 .filter-bar, 二级分类浮窗(.tree-panel z-index:60)才能覆盖右侧内容,
     否则浮窗会被 .filter-bar 遮挡, 导致鼠标移入浮窗时事件被拦截而浮窗消失 */
  z-index: 20;
}

/* 滚动容器：包裹分类项，超出可滚动 */
.tree-scroll {
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 6px 0;
  box-sizing: border-box;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.tree-scroll::-webkit-scrollbar {
  display: none;
}

/* 一级分类项 */
.tree-item {
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background 0.15s, color 0.15s;
  box-sizing: border-box;
}

.tree-item:hover {
  background: var(--color-primary);
  color: #ffffff;
}

.tree-item.active {
  background: var(--price-bg);
  color: var(--color-primary);
  font-weight: 700;
}

.tree-item.all {
  border-bottom: 1px solid var(--color-border-light);
  margin-bottom: 4px;
}

.tree-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.tree-arrow {
  color: var(--color-text-muted);
  font-size: 14px;
  margin-left: 6px;
  flex-shrink: 0;
  transition: color 0.15s;
}

.tree-item:hover>.tree-arrow {
  color: #ffffff;
}

/* 二级分类浮层: 悬停一级分类时右侧弹出大面板 (与首页风格一致) */
.tree-panel {
  position: absolute;
  left: 100%;
  width: 460px;
  min-height: 200px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 16px 20px;
  z-index: 60;
  box-sizing: border-box;
}

/* 浮层头部 */
.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border-light);
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.panel-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 二级分类流式布局 */
.panel-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.panel-item {
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.15s;
  white-space: nowrap;
  line-height: 1.4;
}

.panel-item:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.panel-item.active {
  color: var(--color-primary);
  background: var(--price-bg);
  font-weight: 600;
}

.tree-empty {
  padding: 24px 16px;
  font-size: 13px;
  color: var(--color-text-muted);
  text-align: center;
}

/* === 右侧商品区 === */
.product-area {
  flex: 1;
  min-width: 0;
  /* flex column 让排序栏、商品主体区域垂直排列 */
  display: flex;
  flex-direction: column;
  min-height: 0;
  /* 高度填满 main-layout，使内部滚动容器可生效 */
  align-self: stretch;
}


/* 筛选排序栏 */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 10px 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  /* sticky 固定不滑动（防御性：内部已通过 flex 限制滚动） */
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
}

/* === 商品主体区域：独立滚动容器 === */
.product-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 滚动内容区：独立滚动容器，避免溢出覆盖分页器 */
.product-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

/* 分页器固定底部（不随商品列表滚动） */
.pagination-bar {
  flex-shrink: 0;
  padding: 12px 0;
  display: flex;
  justify-content: center;
  background: transparent;
}

/* 排序 Tab */
.sort-tabs {
  display: flex;
  gap: 4px;
  align-items: center;
}

.sort-tab {
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  white-space: nowrap;
}

.sort-tab:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.sort-tab.active {
  color: #ffffff;
  background: var(--color-primary);
  font-weight: 600;
}

/* 价格区间筛选 */
.price-filter {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-right: 4px;
}

.price-input {
  width: 80px;
  padding: 5px 8px;
  font-size: 13px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.price-input:focus {
  border-color: var(--color-primary);
}

/* 隐藏 number input 的旋钮按钮 */
.price-input::-webkit-outer-spin-button,
.price-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.price-input[type='number'] {
  -moz-appearance: textfield;
}

.price-sep {
  color: var(--color-text-muted);
  font-size: 13px;
}

.price-btn {
  padding: 5px 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
  letter-spacing: 0.02em;
}

.price-btn:hover {
  background: var(--btn-hover);
  border-color: var(--btn-hover);
}

.price-btn.text {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: var(--color-border);
}

.price-btn.text:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

/* === 骨架屏 === */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.skeleton-card {
  height: 340px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-muted) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

/* === 商品网格 (4列响应式) === */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

/* 商品卡片 */
.product-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

/* 图片 */
.card-img {
  width: 100%;
  height: 200px;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

/* 售罄遮罩 */
.sold-out-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

/* 卡片内容 */
.card-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  box-sizing: border-box;
}

/* 名称 (2行省略) */
.card-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.4;
  margin-bottom: 8px;
  /* 2行省略 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
  min-height: 2.8em;
}

/* 价格 */
.card-prices {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
}

.card-price {
  font-family: var(--font-price);
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
}

/* 销量 */
.card-sales {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
}

/* 加入购物车按钮 */
.card-actions {
  margin-top: auto;
}

.btn-cart {
  width: 100%;
  padding: 7px 0;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  transition: background 0.15s, opacity 0.15s;
  letter-spacing: 0.02em;
}

.btn-cart:hover:not(:disabled) {
  background: var(--btn-hover);
  border-color: var(--btn-hover);
}

.btn-cart:disabled {
  background: var(--btn-disabled-bg);
  border-color: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
  cursor: not-allowed;
}

/* === 空状态 === */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
}

.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

/* 小按钮 */
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
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
}

/* === 响应式 === */
/* 中屏: 商品网格 3 列 */
@media (max-width: 1200px) {

  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .tree-panel {
    width: 380px;
  }
}

/* 小屏: 隐藏左侧分类树, 商品网格 2 列 */
@media (max-width: 768px) {
  .category-page {
    padding: 12px 12px 24px;
  }

  .main-layout {
    gap: 0;
  }

  .category-tree {
    display: none;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .sort-tabs {
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }

  .sort-tabs::-webkit-scrollbar {
    display: none;
  }

  .price-filter {
    justify-content: flex-start;
  }

  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .card-img {
    height: 160px;
  }

  .card-price {
    font-size: 18px;
  }
}

/* 超小屏: 商品网格 2 列, 紧凑布局 */
@media (max-width: 480px) {

  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .card-body {
    padding: 8px;
  }

  .card-name {
    font-size: 12px;
  }

  .card-price {
    font-size: 16px;
  }
}
</style>
