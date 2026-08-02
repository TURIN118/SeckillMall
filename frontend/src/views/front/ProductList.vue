<template>
  <!-- 顶部横向分类导航栏 + 下方商品区 (参考淘宝/京东) -->
  <div class="category-page">
    <!-- 顶部横向分类栏 -->
    <div class="category-topbar">
      <div class="category-nav">
        <!-- "全部" 选项: 不筛选分类 -->
        <div
          class="nav-item"
          :class="{ active: !categoryId }"
          @click="handleCategoryClick(undefined)"
        >
          <span class="nav-name">全部</span>
        </div>

        <!-- 一级分类横向排列 -->
        <div
          v-for="cat in categoryList"
          :key="cat.id"
          class="nav-item-wrapper"
        >
          <div
            class="nav-item"
            :class="{ active: isCategoryActive(cat.id) }"
            @click="handleCategoryClick(cat.id)"
          >
            <span class="nav-name">{{ cat.categoryName }}</span>
            <span
              v-if="cat.children && cat.children.length > 0"
              class="nav-caret"
              aria-hidden="true"
            >&#8250;</span>
          </div>

          <!-- 二级分类下拉面板 (纯 CSS hover 显示) -->
          <div
            v-if="cat.children && cat.children.length > 0"
            class="nav-dropdown"
          >
            <div class="dropdown-header">
              <span class="dropdown-header-title">{{ cat.categoryName }}</span>
              <span class="dropdown-header-hint">全部分类</span>
            </div>
            <div class="dropdown-content">
              <div
                v-for="child in cat.children"
                :key="child.id"
                class="dropdown-item"
                :class="{ active: child.id === categoryId }"
                @click="handleCategoryClick(child.id)"
              >{{ child.categoryName }}</div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="categoryList.length === 0" class="nav-empty">暂无分类</div>
      </div>
    </div>

    <!-- 下方商品区 -->
    <div class="category-main">
      <!-- 筛选排序栏 -->
      <div class="sort-bar">
        <span
          v-for="opt in sortOptions"
          :key="opt.value"
          class="sort-item"
          :class="{ active: sortType === opt.value }"
          @click="handleSortChange(opt.value)"
        >{{ opt.label }}</span>
      </div>

      <!-- 加载骨架屏 -->
      <div v-if="loading" class="skeleton-grid">
        <div v-for="i in 8" :key="i" class="skeleton-card"></div>
      </div>

      <template v-else>
        <!-- 商品网格 -->
        <div v-if="productList.length > 0" class="product-grid">
          <ProductCard
            v-for="product in productList"
            :key="product.id"
            :product="product"
            class="grid-card"
            @click="goProductDetail(product.id)"
          />
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

        <!-- 分页 -->
        <PaginationWrapper
          v-if="total > 0"
          :total="total"
          :page-num="pageNum"
          :page-size="pageSize"
          :page-sizes="[20, 40, 60]"
          @change="handlePageChange"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P02 商品列表/分类页
 * 顶部横向分类导航栏 (参考淘宝/京东) + 下方商品区
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList } from '@/api/product'
import { getCategoryTree } from '@/api/category'
import ProductCard from '@/components/ProductCard.vue'
import PaginationWrapper from '@/components/PaginationWrapper.vue'
import type { ProductVO, CategoryTreeNode } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref<boolean>(false)
const productList = ref<ProductVO[]>([])
// 后端 getCategoryTree 返回树形结构(一级分类对象含 children 数组)
// 接口签名是 CategoryVO[]，但实际数据满足 CategoryTreeNode 结构，做类型断言
const categoryList = ref<CategoryTreeNode[]>([])
const total = ref<number>(0)

const categoryId = ref<number | undefined>(undefined)
const sortType = ref<string>('default')
const pageNum = ref<number>(1)
const pageSize = ref<number>(20)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

const sortOptions = [
  { label: '综合排序', value: 'default' },
  { label: '价格升序', value: 'priceAsc' },
  { label: '价格降序', value: 'priceDesc' },
  { label: '销量排序', value: 'sales' }
]

/** 计算排序参数 */
function getSortParams(): { sortBy?: string; sortOrder?: string } {
  switch (sortType.value) {
    case 'priceAsc':
      return { sortBy: 'price', sortOrder: 'asc' }
    case 'priceDesc':
      return { sortBy: 'price', sortOrder: 'desc' }
    case 'sales':
      return { sortBy: 'sales', sortOrder: 'desc' }
    default:
      return {}
  }
}

/** 拉取商品列表 */
async function fetchProducts(): Promise<void> {
  loading.value = true
  try {
    const res = await getProductList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      categoryId: categoryId.value,
      // 前台只展示上架商品，显式传 status='ON_SALE'
      status: 'ON_SALE',
      ...getSortParams()
    })
    productList.value = res.data.list || []
    total.value = res.data.total
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

/** 拉取分类列表 */
async function fetchCategories(): Promise<void> {
  try {
    const res = await getCategoryTree()
    // 后端返回的是树形结构，一级分类对象中包含 children 数组存放二级分类
    categoryList.value = (res.data as CategoryTreeNode[]) || []
  } catch {
    // 忽略
  }
}

/**
 * 判断一级分类是否高亮:
 * 当 categoryId 等于该一级分类 id，或等于其某个二级分类 id 时高亮
 */
function isCategoryActive(catId: number): boolean {
  if (categoryId.value === catId) return true
  const cat = categoryList.value.find(c => c.id === catId)
  if (cat && cat.children && cat.children.some(child => child.id === categoryId.value)) {
    return true
  }
  return false
}

/** 分类点击: 传 undefined 表示点击"全部"; 再次点击同一分类则取消筛选 */
function handleCategoryClick(id: number | undefined): void {
  if (id === undefined) {
    categoryId.value = undefined
  } else {
    categoryId.value = categoryId.value === id ? undefined : id
  }
  pageNum.value = 1
  fetchProducts()
}

/** 排序变化 */
function handleSortChange(value: string): void {
  sortType.value = value
  pageNum.value = 1
  fetchProducts()
}

/** 重置筛选 */
function resetFilters(): void {
  categoryId.value = undefined
  sortType.value = 'default'
  pageNum.value = 1
  fetchProducts()
}

/** 跳转商品详情 */
function goProductDetail(id: number): void {
  router.push(`/products/${id}`)
}

/** 分页变化 */
function handlePageChange(payload: { pageNum: number; pageSize: number }): void {
  pageNum.value = payload.pageNum
  pageSize.value = payload.pageSize
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 从 URL query 读取分类 ID (支持 /products?categoryId=xxx 跳转) */
function syncCategoryFromRoute(): void {
  // 优先读 query.categoryId (首页分类侧边栏跳转使用)
  const queryCid = route.query.categoryId as string | undefined
  if (queryCid !== undefined) {
    // 有 query.categoryId (含空字符串) → 按 query 处理
    categoryId.value = queryCid ? Number(queryCid) : undefined
    return
  }
  // 兼容旧路由 params.id
  const paramCid = route.params.id as string | undefined
  if (paramCid) {
    categoryId.value = Number(paramCid)
  }
}

onMounted(() => {
  syncCategoryFromRoute()
  fetchCategories()
  fetchProducts()
})

/** 监听路由 query 变化 (从首页分类侧边栏点击跳转时, 同一组件复用需重新拉取) */
watch(
  () => route.query.categoryId,
  () => {
    syncCategoryFromRoute()
    pageNum.value = 1
    fetchProducts()
  }
)

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<style scoped>
/* 页面容器: 顶部分类栏 + 下方商品区 纵向布局 */
.category-page {
  padding-bottom: 24px;
}

/* 顶部横向分类栏 */
.category-topbar {
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 16px;
}

/* 一级分类横向导航容器 */
.category-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 20px;
  max-width: 1400px;
  margin: 0 auto;
  overflow-x: auto;
  /* 隐藏横向滚动条但保留滚动能力 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.category-nav::-webkit-scrollbar {
  display: none;
}

/* 一级分类项外层 (下拉面板定位基准) */
.nav-item-wrapper {
  position: relative;
  flex-shrink: 0;
}

/* 一级分类项 */
.nav-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 18px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.nav-item:hover {
  color: var(--color-primary);
}

.nav-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.nav-name {
  line-height: 1.2;
}

/* 一级分类右侧小箭头 (表示有子分类) */
.nav-caret {
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 400;
  line-height: 1;
  transition: color 0.2s, transform 0.2s;
}

.nav-item:hover .nav-caret {
  color: var(--color-primary);
  transform: rotate(90deg);
}

/* 二级分类下拉面板 (悬停一级分类时下方显示) */
.nav-dropdown {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 360px;
  max-width: 520px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 16px 20px 18px 20px;
  z-index: 50;
}

/* hover 一级分类项外层时显示下拉面板 */
.nav-item-wrapper:hover > .nav-dropdown {
  display: block;
}

/* 下拉面板头部 */
.dropdown-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border-light);
}

.dropdown-header-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.dropdown-header-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 二级分类流式布局 */
.dropdown-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.dropdown-item {
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.15s;
  white-space: nowrap;
  line-height: 1.4;
}

.dropdown-item:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.dropdown-item.active {
  color: var(--color-primary);
  background: var(--price-bg);
  font-weight: 600;
}

.nav-empty {
  padding: 14px 18px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 下方商品区 */
.category-main {
  padding: 0 20px;
  min-width: 0;
}

/* 排序栏 */
.sort-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  font-size: 14px;
  align-items: center;
  background: var(--color-bg-card);
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
}

.sort-item {
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.sort-item:hover {
  color: var(--color-text-primary);
}

.sort-item.active {
  color: var(--color-primary);
  font-weight: 700;
  border-bottom-color: var(--color-primary);
}

/* 骨架屏 */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.skeleton-card {
  height: 240px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-muted) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 商品网格 4 列 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.grid-card {
  width: 100%;
}

/* 空状态 */
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

/* 响应式 */
@media (max-width: 1024px) {
  .nav-dropdown {
    min-width: 320px;
  }
}

@media (max-width: 768px) {
  /* 移动端: 顶部分类栏横向滚动 */
  .category-nav {
    padding: 0 12px;
    gap: 2px;
  }
  .nav-item {
    padding: 12px 14px;
    font-size: 13px;
  }
  /* 移动端下拉面板靠左对齐并适当缩窄 */
  .nav-dropdown {
    min-width: 260px;
    max-width: 90vw;
  }
  .category-main {
    padding: 0 12px;
  }
  .skeleton-grid,
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
