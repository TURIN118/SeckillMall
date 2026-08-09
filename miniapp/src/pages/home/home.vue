<!--
  首页 Home（T2.1）
  对齐 spec.md 3.1：
  - Banner 轮播（u-swiper mode="dot"）
  - 秒杀专区入口（显示进行中场次 + 点击跳转秒杀专区）
  - 商品分类导航（横向滚动 + 点击跳转商品列表带分类参数）
  - 猜你喜欢（触底加载 + 分类筛选 + 无限滚动）
  - 下拉刷新 onPullDownRefresh
  - 顶部自定义导航栏（navigationStyle:"custom"）
  - API：GET /banners、GET /seckill/activities、GET /categories、GET /products
-->
<template>
  <view class="home-page">
    <!-- 顶部自定义导航栏 -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="navbar-content">
        <text class="navbar-title">秒杀商城</text>
        <view class="navbar-right" @click="handleSearchClick">
          <u-icon name="search" size="40rpx" color="#303133" />
        </view>
      </view>
    </view>
    <!-- 占位（与自定义导航栏等高） -->
    <view class="navbar-placeholder" :style="{ height: (statusBarHeight + 44) + 'px' }" />

    <!-- Banner 轮播 -->
    <view class="banner-section" v-if="banners.length > 0">
      <u-swiper
        :list="banners"
        mode="dot"
        :height="320"
        :autoplay="true"
        :interval="4000"
        :circular="true"
        indicator-pos="bottomRight"
        border-radius="16"
        :name="'image'"
        @click="handleBannerClick"
      />
    </view>

    <!-- 秒杀专区入口 -->
    <view class="seckill-entry" v-if="inProgressActivities.length > 0">
      <view class="section-header" @click="handleSeckillMore">
        <view class="section-title-wrap">
          <u-icon name="hourglass" size="32rpx" color="#ff4d4f" />
          <text class="section-title">限时秒杀</text>
        </view>
        <view class="section-more">
          <text class="more-text">更多</text>
          <u-icon name="arrow-right" size="24rpx" color="#909399" />
        </view>
      </view>
      <view class="seckill-activities">
        <view
          class="activity-item"
          v-for="activity in inProgressActivities.slice(0, 3)"
          :key="activity.id"
          @click="handleSeckillClick(activity)"
        >
          <view class="activity-info">
            <text class="activity-name">{{ activity.name }}</text>
            <text class="activity-status">进行中</text>
          </view>
          <view class="activity-goods" v-if="activity.goodsList && activity.goodsList.length">
            <u-image
              :src="activity.goodsList[0].productImage"
              mode="aspectFill"
              width="120rpx"
              height="120rpx"
              radius="8rpx"
              :lazy-load="true"
            />
            <view class="goods-info">
              <text class="goods-name ellipsis-1">{{ activity.goodsList[0].productName }}</text>
              <view class="goods-price">
                <text class="seckill-price">¥{{ formatPrice(activity.goodsList[0].seckillPrice) }}</text>
                <text class="original-price">¥{{ formatPrice(activity.goodsList[0].originalPrice) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 商品分类导航（横向滚动） -->
    <view class="category-nav" v-if="categories.length > 0">
      <scroll-view scroll-x class="category-scroll" :show-scrollbar="false">
        <view class="category-list">
          <view
            class="category-item"
            v-for="category in categories"
            :key="category.id"
            @click="handleCategoryClick(category)"
          >
            <view class="category-icon-wrap">
              <u-image
                v-if="category.icon"
                :src="category.icon"
                mode="aspectFill"
                width="80rpx"
                height="80rpx"
                radius="50%"
                :lazy-load="true"
              />
              <view v-else class="category-icon-default">
                <u-icon name="grid" size="40rpx" color="#ff4d4f" />
              </view>
            </view>
            <text class="category-name ellipsis-1">{{ category.name }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 猜你喜欢 -->
    <view class="guess-section">
      <view class="section-header">
        <view class="section-title-wrap">
          <text class="section-title">猜你喜欢</text>
        </view>
        <view class="filter-btn" @click="showCategoryFilter = true">
          <u-icon name="list" size="28rpx" color="#606266" />
          <text class="filter-text">筛选</text>
        </view>
      </view>

      <!-- 商品网格（2 列） -->
      <view class="product-grid" v-if="products.length > 0">
        <view class="grid-item" v-for="product in products" :key="product.id">
          <ProductCard :product="product" />
        </view>
      </view>

      <!-- 空状态 -->
      <u-empty
        v-else-if="!loading"
        text="暂无商品"
        mode="data"
        margin-top="120"
      />

      <!-- 加载更多 -->
      <u-loadmore
        v-if="products.length > 0"
        :status="loadMoreStatus"
        :content-text="loadMoreText"
      />
    </view>

    <!-- 分类筛选底部弹出 -->
    <u-popup
      :show="showCategoryFilter"
      mode="bottom"
      :safe-area-inset-bottom="true"
      round="16"
      @close="showCategoryFilter = false"
    >
      <view class="filter-popup">
        <view class="popup-header">
          <text class="popup-title">分类筛选</text>
          <u-icon name="close" size="40rpx" @click="showCategoryFilter = false" />
        </view>
        <scroll-view scroll-y class="popup-content">
          <view class="filter-tags">
            <view
              class="filter-tag"
              :class="{ active: selectedCategoryId === '' }"
              @click="handleFilterSelect('')"
            >
              <text>全部</text>
            </view>
            <view
              class="filter-tag"
              :class="{ active: selectedCategoryId === category.id }"
              v-for="category in categories"
              :key="category.id"
              @click="handleFilterSelect(category.id)"
            >
              <text>{{ category.name }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import * as bannerApi from '@/api/banner'
import * as seckillApi from '@/api/seckill'
import * as productApi from '@/api/product'
import { useCategoryStore } from '@/stores/category'
import { navigate } from '@/utils/navigate'
import { showToast } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import type { BannerVO, SeckillActivityVO, CategoryVO, ProductVO } from '@/types'
import ProductCard from '@/components/ProductCard/ProductCard.vue'

// ============ 状态栏高度（自定义导航栏） ============
const statusBarHeight = ref<number>(20)

// ============ 数据 ============
const banners = ref<BannerVO[]>([])
const activities = ref<SeckillActivityVO[]>([])
const products = ref<ProductVO[]>([])

// 分类使用 store 缓存
const categoryStore = useCategoryStore()
const categories = computed<CategoryVO[]>(() => categoryStore.categoryList)

// 进行中的秒杀场次
const inProgressActivities = computed(() =>
  activities.value.filter(a => a.status === 1)
)

// ============ 分页状态 ============
const page = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)
const loading = ref<boolean>(false)
const hasMore = ref<boolean>(true)
const selectedCategoryId = ref<string>('')

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

// ============ 初始化 ============
onMounted(() => {
  // 获取状态栏高度
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20

  initPageData()
})

/** 初始化页面数据 */
async function initPageData() {
  // 分类 store 缓存优先
  if (categoryStore.categoryList.length === 0) {
    try {
      await categoryStore.fetchCategories()
    } catch (e) {
      console.error('[home] fetchCategories error:', e)
    }
  }
  // 并行加载 Banner、秒杀活动、商品
  await Promise.all([
    loadBanners(),
    loadSeckillActivities(),
    loadProducts(true)
  ])
}

/** 加载 Banner */
async function loadBanners() {
  try {
    banners.value = await bannerApi.getBannerList()
  } catch (e) {
    console.error('[home] loadBanners error:', e)
  }
}

/** 加载秒杀活动 */
async function loadSeckillActivities() {
  try {
    activities.value = await seckillApi.getSeckillActivities()
  } catch (e) {
    console.error('[home] loadSeckillActivities error:', e)
  }
}

/** 加载商品（猜你喜欢） */
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
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
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
    console.error('[home] loadProducts error:', e)
    showToast('加载商品失败', 'error')
  } finally {
    loading.value = false
  }
}

// ============ 事件处理 ============

/** Banner 点击跳转 */
function handleBannerClick(index: number) {
  const banner = banners.value[index]
  if (!banner || !banner.link) return
  // 简单跳转：若 link 是商品详情 URL，跳商品详情；否则 toast 提示
  // 这里采用通用处理：尝试解析 link 中的 id 参数
  try {
    const url = new URL(banner.link, 'http://placeholder')
    const id = url.searchParams.get('id')
    if (id) {
      navigate.to('pages-product/pages/product-detail/product-detail', { id: ensureStringId(id) })
      return
    }
  } catch {
    // ignore
  }
  showToast(banner.title || 'Banner 跳转', 'none')
}

/** 秒杀入口点击 → 跳转秒杀专区 */
function handleSeckillClick(activity: SeckillActivityVO) {
  navigate.to('pages-seckill/pages/seckill-zone/seckill-zone', {
    activityId: ensureStringId(activity.id)
  })
}

/** 秒杀更多 */
function handleSeckillMore() {
  navigate.to('pages-seckill/pages/seckill-zone/seckill-zone')
}

/** 分类导航点击 → 跳转商品列表带分类参数 */
function handleCategoryClick(category: CategoryVO) {
  navigate.to('pages-product/pages/product-list/product-list', {
    categoryId: ensureStringId(category.id),
    categoryName: category.name
  })
}

/** 顶部搜索点击 */
function handleSearchClick() {
  navigate.to('pages-product/pages/product-list/product-list')
}

/** 分类筛选选择 */
function handleFilterSelect(categoryId: string) {
  selectedCategoryId.value = categoryId
  showCategoryFilter.value = false
  loadProducts(true)
}

// ============ 分类筛选弹窗 ============
const showCategoryFilter = ref<boolean>(false)

// ============ 下拉刷新 ============
onPullDownRefresh(() => {
  initPageData().finally(() => {
    uni.stopPullDownRefresh()
  })
})

// ============ 触底加载 ============
onReachBottom(() => {
  if (hasMore.value && !loading.value) {
    loadProducts(false)
  }
})

// ============ 工具函数 ============
/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}
</script>

<style lang="scss" scoped>
.home-page {
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
    justify-content: center;
    padding: 0 24rpx;
    position: relative;

    .navbar-title {
      font-size: 36rpx;
      font-weight: bold;
      color: #303133;
    }

    .navbar-right {
      position: absolute;
      right: 24rpx;
      top: 50%;
      transform: translateY(-50%);
      padding: 8rpx;
    }
  }
}

.navbar-placeholder {
  width: 100%;
}

/* Banner */
.banner-section {
  padding: 16rpx 24rpx 0;
}

/* 秒杀入口 */
.seckill-entry {
  margin: 24rpx 24rpx 0;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .seckill-activities {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
    margin-top: 16rpx;

    .activity-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16rpx;
      background-color: #fff5f5;
      border-radius: 12rpx;

      .activity-info {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .activity-name {
          font-size: 28rpx;
          font-weight: bold;
          color: #ff4d4f;
        }

        .activity-status {
          font-size: 22rpx;
          color: #ff4d4f;
          padding: 2rpx 12rpx;
          background-color: #ffffff;
          border-radius: 4rpx;
        }
      }

      .activity-goods {
        display: flex;
        align-items: center;
        gap: 16rpx;

        .goods-info {
          display: flex;
          flex-direction: column;
          gap: 8rpx;
          width: 200rpx;

          .goods-name {
            font-size: 24rpx;
            color: #303133;
          }

          .goods-price {
            display: flex;
            align-items: baseline;
            gap: 8rpx;

            .seckill-price {
              font-size: 28rpx;
              color: #ff4d4f;
              font-weight: bold;
            }

            .original-price {
              font-size: 22rpx;
              color: #c0c4cc;
              text-decoration: line-through;
            }
          }
        }
      }
    }
  }
}

/* 通用 section header */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .section-title-wrap {
    display: flex;
    align-items: center;
    gap: 8rpx;

    .section-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #303133;
    }
  }

  .section-more {
    display: flex;
    align-items: center;
    gap: 4rpx;

    .more-text {
      font-size: 24rpx;
      color: #909399;
    }
  }

  .filter-btn {
    display: flex;
    align-items: center;
    gap: 4rpx;

    .filter-text {
      font-size: 24rpx;
      color: #606266;
    }
  }
}

/* 分类导航 */
.category-nav {
  margin: 24rpx 0 0;
  background-color: #ffffff;
  padding: 24rpx 0;

  .category-scroll {
    width: 100%;

    .category-list {
      display: flex;
      gap: 24rpx;
      padding: 0 24rpx;
      white-space: nowrap;

      .category-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12rpx;
        width: 120rpx;
        flex-shrink: 0;

        .category-icon-wrap {
          width: 80rpx;
          height: 80rpx;
          display: flex;
          align-items: center;
          justify-content: center;

          .category-icon-default {
            width: 80rpx;
            height: 80rpx;
            border-radius: 50%;
            background-color: #fff5f5;
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }

        .category-name {
          font-size: 24rpx;
          color: #606266;
          max-width: 120rpx;
        }
      }
    }
  }
}

/* 猜你喜欢 */
.guess-section {
  margin: 24rpx 24rpx 0;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .product-grid {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    margin-top: 16rpx;

    .grid-item {
      width: 48.5%;
      margin-bottom: 16rpx;
    }
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

/* 单行省略 */
.ellipsis-1 {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
