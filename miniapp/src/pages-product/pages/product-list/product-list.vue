<!--
  商品列表 ProductList（T2.3，商品分包）
  对齐 spec.md 3.2：
  - 接收分类参数（onLoad query.categoryId）
  - 分类筛选（顶部 u-tabs + 下拉筛选面板）
  - 价格区间筛选（u-slider 双滑块）
  - 多维度排序（顶部排序栏 + ActionSheet：综合/价格升序/价格降序/销量）
  - 分页：触底加载 onReachBottom + u-loadmore
  - 商品卡片 2 列网格（ProductCard 公共组件）
  - 调用 productApi.list() 传参
-->
<template>
  <view class="product-list-page">
    <!-- 顶部自定义导航栏 -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="navbar-content">
        <view class="navbar-back" @click="handleBack">
          <u-icon name="arrow-left" size="40rpx" color="#303133" />
        </view>
        <text class="navbar-title">{{ pageTitle }}</text>
        <view class="navbar-right" @click="showFilterPopup = true">
          <u-icon name="setting" size="40rpx" color="#303133" />
        </view>
      </view>
    </view>
    <view class="navbar-placeholder" :style="{ height: (statusBarHeight + 44) + 'px' }" />

    <!-- 分类筛选 tabs（横向滚动） -->
    <view class="category-tabs" v-if="categoryTabs.length > 0">
      <u-tabs
        :list="categoryTabs"
        :current="currentCategoryIndex"
        @click="handleCategoryTabClick"
        :scrollable="true"
        keyName="name"
        lineWidth="40"
        lineHeight="4"
        :activeStyle="{ color: '#ff4d4f', fontWeight: 'bold' }"
        :inactiveStyle="{ color: '#606266' }"
      />
    </view>

    <!-- 排序栏 -->
    <view class="sort-bar">
      <view
        class="sort-item"
        :class="{ active: sortType === 'comprehensive' }"
        @click="handleSortClick('comprehensive')"
      >
        <text>综合</text>
      </view>
      <view
        class="sort-item"
        :class="{ active: sortType === 'price' }"
        @click="handleSortClick('price')"
      >
        <text>价格</text>
        <view class="sort-arrow">
          <u-icon
            :name="sortOrder === 'asc' ? 'arrow-up' : 'arrow-down'"
            size="20rpx"
            :color="sortType === 'price' ? '#ff4d4f' : '#909399'"
          />
        </view>
      </view>
      <view
        class="sort-item"
        :class="{ active: sortType === 'sales' }"
        @click="handleSortClick('sales')"
      >
        <text>销量</text>
      </view>
      <view class="sort-item" @click="showFilterPopup = true">
        <u-icon name="search" size="28rpx" color="#606266" />
        <text>筛选</text>
      </view>
    </view>

    <!-- 商品网格 -->
    <view class="product-grid" v-if="products.length > 0">
      <view class="grid-item" v-for="product in products" :key="product.id">
        <ProductCard :product="product" />
      </view>
    </view>

    <!-- 空状态 -->
    <u-empty
      v-else-if="!loading"
      text="暂无符合条件的商品"
      mode="data"
      margin-top="120"
    />

    <!-- 加载更多 -->
    <u-loadmore
      v-if="products.length > 0"
      :status="loadMoreStatus"
      :content-text="loadMoreText"
    />

    <!-- 筛选弹窗（价格区间 + 分类） -->
    <u-popup
      :show="showFilterPopup"
      mode="bottom"
      :safe-area-inset-bottom="true"
      round="16"
      @close="showFilterPopup = false"
    >
      <view class="filter-popup">
        <view class="popup-header">
          <text class="popup-title">筛选</text>
          <u-icon name="close" size="40rpx" @click="showFilterPopup = false" />
        </view>

        <scroll-view scroll-y class="popup-content">
          <!-- 价格区间 -->
          <view class="filter-section">
            <text class="section-label">价格区间</text>
            <view class="price-inputs">
              <view class="price-input-wrap">
                <text class="price-prefix">¥</text>
                <u-input
                  v-model="tempMinPrice"
                  type="number"
                  placeholder="最低价"
                  border="surround"
                  :clearable="true"
                />
              </view>
              <text class="price-divider">-</text>
              <view class="price-input-wrap">
                <text class="price-prefix">¥</text>
                <u-input
                  v-model="tempMaxPrice"
                  type="number"
                  placeholder="最高价"
                  border="surround"
                  :clearable="true"
                />
              </view>
            </view>
          </view>

          <!-- 分类筛选 -->
          <view class="filter-section">
            <text class="section-label">分类</text>
            <view class="filter-tags">
              <view
                class="filter-tag"
                :class="{ active: tempCategoryId === '' }"
                @click="tempCategoryId = ''"
              >
                <text>全部</text>
              </view>
              <view
                class="filter-tag"
                :class="{ active: tempCategoryId === category.id }"
                v-for="category in categories"
                :key="category.id"
                @click="tempCategoryId = category.id"
              >
                <text>{{ category.name }}</text>
              </view>
            </view>
          </view>
        </scroll-view>

        <!-- 底部操作 -->
        <view class="popup-footer">
          <u-button
            text="重置"
            @click="handleResetFilter"
            :customStyle="{ marginRight: '24rpx' }"
          />
          <u-button
            type="error"
            text="确定"
            @click="handleConfirmFilter"
          />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import * as productApi from '@/api/product'
import { useCategoryStore } from '@/stores/category'
import { showToast } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import type { ProductVO, CategoryVO } from '@/types'
import ProductCard from '@/components/ProductCard/ProductCard.vue'

// ============ 状态栏高度 ============
const statusBarHeight = ref<number>(20)

// ============ 路由参数 ============
const initialCategoryId = ref<string>('')
const initialCategoryName = ref<string>('')

// ============ 页面标题 ============
const pageTitle = computed(() => {
  if (initialCategoryName.value) return initialCategoryName.value
  return '商品列表'
})

// ============ 分类 store ============
const categoryStore = useCategoryStore()
const categories = computed<CategoryVO[]>(() => categoryStore.categoryList)

// ============ 分类 tabs（含"全部"） ============
const categoryTabs = computed(() => [
  { id: '', name: '全部' },
  ...categories.value.map(c => ({ id: c.id, name: c.name }))
])
const currentCategoryIndex = ref<number>(0)

// ============ 商品数据 ============
const products = ref<ProductVO[]>([])
const page = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)
const loading = ref<boolean>(false)
const hasMore = ref<boolean>(true)

// ============ 筛选/排序状态 ============
type SortType = 'comprehensive' | 'price' | 'sales'
const sortType = ref<SortType>('comprehensive')
const sortOrder = ref<'asc' | 'desc'>('asc')

const selectedCategoryId = ref<string>('')
const minPrice = ref<number | undefined>(undefined)
const maxPrice = ref<number | undefined>(undefined)

// ============ 筛选弹窗临时状态 ============
const showFilterPopup = ref<boolean>(false)
const tempCategoryId = ref<string>('')
const tempMinPrice = ref<string>('')
const tempMaxPrice = ref<string>('')

// ============ 加载更多状态 ============
const loadMoreStatus = computed<'loadmore' | 'loading' | 'nomore'>(() => {
  if (loading.value) return 'loading'
  if (!hasMore.value) return 'nomore'
  return 'loadmore'
})
const loadMoreText = computed(() => ({
  loadmore: '上拉加载更多',
  loading: '正在加载...',
  nomore: '没有更多了'
}))

// ============ onLoad 接收参数 ============
onLoad((query) => {
  // 接收分类参数（雪花 ID 用 string 传递）
  if (query?.categoryId) {
    initialCategoryId.value = ensureStringId(query.categoryId)
    selectedCategoryId.value = initialCategoryId.value
  }
  if (query?.categoryName) {
    initialCategoryName.value = decodeURIComponent(query.categoryName)
  }
})

// ============ 初始化 ============
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20

  // 拉取分类（缓存优先）
  if (categoryStore.categoryList.length === 0) {
    categoryStore.fetchCategories().catch((e) => {
      console.error('[product-list] fetchCategories error:', e)
    })
  }

  // 同步分类 tab 选中
  syncCategoryTabIndex()

  // 加载商品
  loadProducts(true)
})

/** 同步分类 tab 选中索引 */
function syncCategoryTabIndex() {
  if (!selectedCategoryId.value) {
    currentCategoryIndex.value = 0
    return
  }
  const idx = categoryTabs.value.findIndex(t => t.id === selectedCategoryId.value)
  if (idx >= 0) {
    currentCategoryIndex.value = idx
  }
}

/** 加载商品 */
async function loadProducts(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    hasMore.value = true
    products.value = []
  }
  if (!hasMore.value) return

  loading.value = true
  try {
    const params: any = {
      page: page.value,
      pageSize: pageSize.value
    }
    // 分类
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }
    // 价格区间
    if (minPrice.value !== undefined && minPrice.value !== null) {
      params.minPrice = minPrice.value
    }
    if (maxPrice.value !== undefined && maxPrice.value !== null) {
      params.maxPrice = maxPrice.value
    }
    // 排序
    if (sortType.value === 'price') {
      params.sortBy = 'price'
      params.sortOrder = sortOrder.value
    } else if (sortType.value === 'sales') {
      params.sortBy = 'sales'
      params.sortOrder = 'desc'
    } else {
      // 综合：默认按创建时间倒序
      params.sortBy = 'createdAt'
      params.sortOrder = 'desc'
    }

    const res = await productApi.getProductList(params)
    const list = res.list || []
    if (reset) {
      products.value = list
    } else {
      products.value = [...products.value, ...list]
    }
    total.value = res.total
    hasMore.value = res.hasMore
    if (list.length > 0) {
      page.value += 1
    }
  } catch (e) {
    console.error('[product-list] loadProducts error:', e)
    showToast('加载商品失败', 'error')
  } finally {
    loading.value = false
  }
}

// ============ 事件处理 ============

/** 返回 */
function handleBack() {
  uni.navigateBack({ delta: 1 })
}

/** 分类 tab 点击 */
function handleCategoryTabClick(item: any) {
  // uView Plus u-tabs @click 回调参数为 item 对象（含 index）
  // 兼容处理：item 可能是 { id, name } 或 index
  let idx = 0
  let categoryId = ''
  if (typeof item === 'number') {
    idx = item
    categoryId = categoryTabs.value[idx]?.id || ''
  } else if (item && typeof item === 'object') {
    if (item.index !== undefined) {
      idx = item.index
    } else if (item.id !== undefined) {
      idx = categoryTabs.value.findIndex(t => t.id === item.id)
    }
    categoryId = categoryTabs.value[idx]?.id || ''
  }
  currentCategoryIndex.value = idx
  selectedCategoryId.value = categoryId
  loadProducts(true)
}

/** 排序点击 */
function handleSortClick(type: SortType) {
  if (type === 'price' && sortType.value === 'price') {
    // 切换升降序
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortType.value = type
    if (type === 'price') {
      sortOrder.value = 'asc'
    } else if (type === 'sales') {
      sortOrder.value = 'desc'
    }
  }
  loadProducts(true)
}

/** 重置筛选 */
function handleResetFilter() {
  tempCategoryId.value = ''
  tempMinPrice.value = ''
  tempMaxPrice.value = ''
}

/** 确认筛选 */
function handleConfirmFilter() {
  selectedCategoryId.value = tempCategoryId.value
  // 价格区间校验
  const min = tempMinPrice.value ? Number(tempMinPrice.value) : undefined
  const max = tempMaxPrice.value ? Number(tempMaxPrice.value) : undefined
  if (min !== undefined && max !== undefined && min > max) {
    showToast('最低价不能大于最高价', 'error')
    return
  }
  minPrice.value = min
  maxPrice.value = max

  // 同步分类 tab 选中
  syncCategoryTabIndex()

  showFilterPopup.value = false
  loadProducts(true)
}

// ============ 下拉刷新 ============
onPullDownRefresh(() => {
  loadProducts(true).finally(() => {
    uni.stopPullDownRefresh()
  })
})

// ============ 触底加载 ============
onReachBottom(() => {
  if (hasMore.value && !loading.value) {
    loadProducts(false)
  }
})
</script>

<style lang="scss" scoped>
.product-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}

/* 自定义导航栏 */
.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: #ffffff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .navbar-content {
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24rpx;

    .navbar-back {
      padding: 8rpx;
    }

    .navbar-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #303133;
      flex: 1;
      text-align: center;
      padding: 0 16rpx;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .navbar-right {
      padding: 8rpx;
    }
  }
}

.navbar-placeholder {
  width: 100%;
}

/* 分类 tabs */
.category-tabs {
  background-color: #ffffff;
  padding: 0 24rpx;
  position: sticky;
  top: 0;
  z-index: 99;
}

/* 排序栏 */
.sort-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background-color: #ffffff;
  padding: 20rpx 24rpx;
  margin-top: 2rpx;
  position: sticky;
  top: 80rpx;
  z-index: 98;

  .sort-item {
    display: flex;
    align-items: center;
    gap: 4rpx;
    font-size: 28rpx;
    color: #606266;
    padding: 8rpx 16rpx;

    &.active {
      color: #ff4d4f;
      font-weight: bold;
    }

    .sort-arrow {
      display: flex;
      align-items: center;
    }
  }
}

/* 商品网格 */
.product-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 24rpx;

  .grid-item {
    width: 48.5%;
    margin-bottom: 24rpx;
  }
}

/* 筛选弹窗 */
.filter-popup {
  padding: 32rpx 24rpx;

  .popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24rpx;

    .popup-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #303133;
    }
  }

  .popup-content {
    max-height: 600rpx;

    .filter-section {
      margin-bottom: 32rpx;

      .section-label {
        font-size: 28rpx;
        font-weight: bold;
        color: #303133;
        margin-bottom: 16rpx;
        display: block;
      }

      .price-inputs {
        display: flex;
        align-items: center;
        gap: 16rpx;

        .price-input-wrap {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 8rpx;

          .price-prefix {
            font-size: 26rpx;
            color: #909399;
          }
        }

        .price-divider {
          font-size: 28rpx;
          color: #909399;
        }
      }

      .filter-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 16rpx;

        .filter-tag {
          padding: 16rpx 32rpx;
          background-color: #f5f5f5;
          border-radius: 8rpx;
          font-size: 26rpx;
          color: #606266;

          &.active {
            background-color: #ff4d4f;
            color: #ffffff;
          }
        }
      }
    }
  }

  .popup-footer {
    display: flex;
    align-items: center;
    margin-top: 24rpx;
  }
}
</style>