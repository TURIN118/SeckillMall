<!--
  商品详情 ProductDetail（T2.4，商品分包）
  对齐 spec.md 3.3：
  - 接收商品 id（onLoad query.id，string 类型，雪花 ID）
  - 图片轮播（u-swiper 全宽 + 指示器）
  - SKU 规格选择（底部弹出 u-popup，规格矩阵，选择后更新价格/库存）
  - 参数速览（u-cell 列表）
  - 服务保障（u-tag + u-popup 说明）
  - 商品评价（列表 + 触底加载，调用 reviewApi）
  - 售后说明（u-collapse 折叠面板）
  - 富文本详情（rich-text 组件渲染，过滤不支持标签）
  - 底部操作栏：加入购物车 + 立即购买
  - 调用 productApi.detail(id)，id 用 string + encodeURIComponent
-->
<template>
  <view class="product-detail-page">
    <!-- 顶部自定义导航栏 -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="navbar-content">
        <view class="navbar-back" @click="handleBack">
          <u-icon name="arrow-left" size="40rpx" color="#303133" />
        </view>
        <text class="navbar-title">商品详情</text>
        <view class="navbar-right" />
      </view>
    </view>
    <view class="navbar-placeholder" :style="{ height: (statusBarHeight + 44) + 'px' }" />

    <!-- 加载骨架 -->
    <view v-if="loading && !product" class="loading-wrap">
      <u-skeleton
        rows="10"
        :loading="true"
        animate
      />
    </view>

    <template v-if="product">
      <!-- 图片轮播 -->
      <view class="image-swiper">
        <u-swiper
          :list="productImages"
          mode="number"
          :height="750"
          indicator-pos="bottomRight"
          :autoplay="false"
          :circular="true"
          :name="'url'"
          @click="handleImagePreview"
        />
      </view>

      <!-- 价格信息 -->
      <view class="price-section">
        <view class="price-row">
          <text class="price-symbol">¥</text>
          <text class="price-current">{{ formatPrice(currentPrice) }}</text>
          <template v-if="product.originalPrice && product.originalPrice > product.price">
            <text class="price-original">¥{{ formatPrice(product.originalPrice) }}</text>
            <u-tag
              :text="discountText"
              type="error"
              size="mini"
              plain
            />
          </template>
        </view>
        <view class="sales-row">
          <text class="sales-text">已售 {{ product.sales }} 件</text>
          <text class="stock-text">库存 {{ currentStock }} 件</text>
        </view>
      </view>

      <!-- 商品标题 -->
      <view class="title-section">
        <text class="title">{{ product.name }}</text>
        <text class="subtitle" v-if="product.subtitle">{{ product.subtitle }}</text>
      </view>

      <!-- 服务保障 -->
      <view class="service-section">
        <view class="service-tags">
          <u-tag text="正品保证" type="success" size="mini" plain />
          <u-tag text="七天无理由" type="success" size="mini" plain />
          <u-tag text="极速发货" type="success" size="mini" plain />
          <view class="service-more" @click="showServicePopup = true">
            <u-icon name="arrow-right" size="24rpx" color="#909399" />
          </view>
        </view>
      </view>

      <!-- 参数速览 -->
      <view class="params-section">
        <view class="section-title">
          <text>参数速览</text>
        </view>
        <u-cell-group>
          <u-cell title="商品分类" :value="product.categoryName || '-'" />
          <u-cell title="商品编号" :value="product.id" />
          <u-cell title="库存" :value="`${product.stock} 件`" />
          <u-cell title="销量" :value="`${product.sales} 件`" />
        </u-cell-group>
      </view>

      <!-- 商品评价 -->
      <view class="review-section">
        <view class="section-title">
          <text>商品评价（{{ reviewTotal }}）</text>
        </view>
        <view class="review-list" v-if="reviews.length > 0">
          <view class="review-item" v-for="review in reviews" :key="review.id">
            <view class="review-header">
              <u-avatar :src="review.avatar" size="60rpx" />
              <text class="review-user">{{ review.username }}</text>
              <u-rate :value="review.rating" readonly size="24rpx" />
            </view>
            <text class="review-content">{{ review.content }}</text>
            <view class="review-images" v-if="review.images && review.images.length > 0">
              <u-image
                v-for="(img, idx) in review.images"
                :key="idx"
                :src="img"
                mode="aspectFill"
                width="160rpx"
                height="160rpx"
                radius="8rpx"
                @click="previewReviewImage(review.images, idx)"
              />
            </view>
            <text class="review-time">{{ formatTime(review.createdAt) }}</text>
            <view class="review-reply" v-if="review.reply">
              <text class="reply-label">商家回复：</text>
              <text class="reply-content">{{ review.reply }}</text>
            </view>
          </view>
        </view>
        <u-empty
          v-else
          text="暂无评价"
          mode="data"
          margin-top="60"
        />
        <u-loadmore
          v-if="reviews.length > 0"
          :status="reviewLoadMoreStatus"
          :content-text="reviewLoadMoreText"
        />
      </view>

      <!-- 售后说明（折叠面板） -->
      <view class="after-sale-section">
        <view class="section-title">
          <text>售后说明</text>
        </view>
        <u-collapse>
          <u-collapse-item title="七天无理由退换货">
            <text class="collapse-content">商品自签收之日起 7 天内，可申请无理由退换货（商品需保持原状）。</text>
          </u-collapse-item>
          <u-collapse-item title="正品保证">
            <text class="collapse-content">本店所有商品均为正品，假一赔十。</text>
          </u-collapse-item>
          <u-collapse-item title="发货时间">
            <text class="collapse-content">下单后 48 小时内发货，节假日顺延。</text>
          </u-collapse-item>
        </u-collapse>
      </view>

      <!-- 富文本详情 -->
      <view class="rich-section">
        <view class="section-title">
          <text>商品详情</text>
        </view>
        <rich-text :nodes="richNodes" class="rich-content" />
      </view>

      <!-- 底部占位（避免被操作栏遮挡） -->
      <view class="bottom-placeholder" />
    </template>

    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="product">
      <view class="bar-icon" @click="handleContactCs">
        <u-icon name="kefu-ermai" size="40rpx" color="#606266" />
        <text class="bar-icon-text">客服</text>
      </view>
      <view class="bar-icon" @click="handleAddToCart">
        <u-icon name="shopping-cart" size="40rpx" color="#606266" />
        <text class="bar-icon-text">购物车</text>
      </view>
      <view class="bar-actions">
        <u-button
          type="warning"
          text="加入购物车"
          @click="handleAddToCart"
          :customStyle="{ marginRight: '16rpx', borderRadius: '40rpx' }"
        />
        <u-button
          type="error"
          text="立即购买"
          @click="handleBuyNow"
          :customStyle="{ borderRadius: '40rpx' }"
        />
      </view>
    </view>

    <!-- SKU 选择弹窗 -->
    <u-popup
      :show="showSkuPopup"
      mode="bottom"
      :safe-area-inset-bottom="true"
      round="16"
      @close="showSkuPopup = false"
    >
      <view class="sku-popup">
        <view class="sku-header">
          <u-image
            :src="product?.mainImage || ''"
            mode="aspectFill"
            width="180rpx"
            height="180rpx"
            radius="8rpx"
          />
          <view class="sku-info">
            <text class="sku-price">¥{{ formatPrice(currentPrice) }}</text>
            <text class="sku-stock">库存：{{ currentStock }} 件</text>
            <text class="sku-selected" v-if="selectedSpecText">已选：{{ selectedSpecText }}</text>
          </view>
          <u-icon name="close" size="40rpx" @click="showSkuPopup = false" />
        </view>

        <scroll-view scroll-y class="sku-content">
          <!-- 规格矩阵（简化版：单规格维度演示，实际可扩展多维度） -->
          <view class="spec-group">
            <text class="spec-label">规格</text>
            <view class="spec-options">
              <view
                class="spec-option"
                :class="{ active: selectedSpecIndex === idx }"
                v-for="(spec, idx) in specOptions"
                :key="idx"
                @click="handleSelectSpec(idx)"
              >
                <text>{{ spec }}</text>
              </view>
            </view>
          </view>

          <!-- 数量 -->
          <view class="quantity-group">
            <text class="quantity-label">数量</text>
            <u-number-box
              v-model="quantity"
              :min="1"
              :max="currentStock > 0 ? currentStock : 1"
              :disabled="currentStock <= 0"
            />
          </view>
        </scroll-view>

        <view class="sku-footer">
          <u-button
            type="error"
            :text="skuConfirmText"
            @click="handleSkuConfirm"
            :disabled="currentStock <= 0"
            :customStyle="{ borderRadius: '40rpx' }"
          />
        </view>
      </view>
    </u-popup>

    <!-- 服务保障说明弹窗 -->
    <u-popup
      :show="showServicePopup"
      mode="bottom"
      :safe-area-inset-bottom="true"
      round="16"
      @close="showServicePopup = false"
    >
      <view class="service-popup">
        <view class="popup-header">
          <text class="popup-title">服务保障</text>
          <u-icon name="close" size="40rpx" @click="showServicePopup = false" />
        </view>
        <view class="service-list">
          <view class="service-item">
            <u-icon name="checkmark-circle" size="40rpx" color="#67c23a" />
            <view class="service-text">
              <text class="service-name">正品保证</text>
              <text class="service-desc">商品均为正品，假一赔十</text>
            </view>
          </view>
          <view class="service-item">
            <u-icon name="checkmark-circle" size="40rpx" color="#67c23a" />
            <view class="service-text">
              <text class="service-name">七天无理由</text>
              <text class="service-desc">签收 7 天内可无理由退换货</text>
            </view>
          </view>
          <view class="service-item">
            <u-icon name="checkmark-circle" size="40rpx" color="#67c23a" />
            <view class="service-text">
              <text class="service-name">极速发货</text>
              <text class="service-desc">下单 48 小时内发货</text>
            </view>
          </view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import * as productApi from '@/api/product'
import * as reviewApi from '@/api/review'
import * as cartApi from '@/api/cart'
import { showToast, showConfirm } from '@/utils/toast'
import { navigate } from '@/utils/navigate'
import { ensureStringId } from '@/utils/snowflake'
import type { ProductVO, ReviewVO } from '@/types'

// ============ 状态栏高度 ============
const statusBarHeight = ref<number>(20)

// ============ 商品数据 ============
const productId = ref<string>('')
const product = ref<ProductVO | null>(null)
const loading = ref<boolean>(false)

// ============ 图片轮播 ============
const productImages = computed(() => {
  if (!product.value) return []
  const imgs = product.value.images && product.value.images.length > 0
    ? product.value.images
    : [product.value.mainImage]
  // u-swiper list 期望 { url } 形式
  return imgs.map(url => ({ url }))
})

// ============ SKU 状态 ============
const showSkuPopup = ref<boolean>(false)
const skuAction = ref<'cart' | 'buy'>('cart')
const selectedSpecIndex = ref<number>(0)
const quantity = ref<number>(1)
// 简化版规格选项（实际应从 product SKUs 接口获取）
const specOptions = ref<string[]>(['默认'])

const selectedSpecText = computed(() => {
  if (specOptions.value.length === 0) return ''
  return specOptions.value[selectedSpecIndex.value] || ''
})

const currentPrice = computed(() => {
  if (!product.value) return 0
  return product.value.price
})

const currentStock = computed(() => {
  if (!product.value) return 0
  return product.value.stock
})

const skuConfirmText = computed(() => {
  if (currentStock.value <= 0) return '已售罄'
  return skuAction.value === 'cart' ? '加入购物车' : '立即购买'
})

// ============ 评价数据 ============
const reviews = ref<ReviewVO[]>([])
const reviewTotal = ref<number>(0)
const reviewPage = ref<number>(1)
const reviewPageSize = ref<number>(10)
const reviewLoading = ref<boolean>(false)
const reviewHasMore = ref<boolean>(true)

const reviewLoadMoreStatus = computed<'loadmore' | 'loading' | 'nomore'>(() => {
  if (reviewLoading.value) return 'loading'
  if (!reviewHasMore.value) return 'nomore'
  return 'loadmore'
})
const reviewLoadMoreText = computed(() => ({
  loadmore: '上拉加载更多',
  loading: '正在加载...',
  nomore: '没有更多了'
}))

// ============ 服务保障弹窗 ============
const showServicePopup = ref<boolean>(false)

// ============ 富文本 nodes ============
const richNodes = computed(() => {
  if (!product.value?.description) return ''
  return filterRichTextHtml(product.value.description)
})

// ============ 折扣文字 ============
const discountText = computed(() => {
  if (!product.value) return ''
  const { price, originalPrice } = product.value
  if (!originalPrice || originalPrice <= price) return ''
  const discount = (price / originalPrice * 10).toFixed(1)
  return `${discount}折`
})

// ============ onLoad 接收 id ============
onLoad((query) => {
  if (query?.id) {
    // 雪花 ID 用 string 传递
    productId.value = ensureStringId(query.id)
  } else {
    showToast('商品 ID 缺失', 'error')
    setTimeout(() => uni.navigateBack({ delta: 1 }), 1500)
  }
})

// ============ 初始化 ============
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20

  if (productId.value) {
    loadProductDetail()
    loadReviews(true)
  }
})

/** 加载商品详情 */
async function loadProductDetail() {
  loading.value = true
  try {
    // 调用 productApi.detail(id)，id 用 string + encodeURIComponent（在 api 内已处理）
    product.value = await productApi.getProductDetail(productId.value)
  } catch (e) {
    console.error('[product-detail] loadProductDetail error:', e)
    showToast('加载商品详情失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 加载评价 */
async function loadReviews(reset = false) {
  if (reviewLoading.value) return
  if (reset) {
    reviewPage.value = 1
    reviewHasMore.value = true
    reviews.value = []
  }
  if (!reviewHasMore.value || !productId.value) return

  reviewLoading.value = true
  try {
    const res = await reviewApi.getProductReviews(productId.value, {
      page: reviewPage.value,
      pageSize: reviewPageSize.value
    })
    const list = res.list || []
    if (reset) {
      reviews.value = list
    } else {
      reviews.value = [...reviews.value, ...list]
    }
    reviewTotal.value = res.total
    reviewHasMore.value = res.hasMore
    if (list.length > 0) {
      reviewPage.value += 1
    }
  } catch (e) {
    console.error('[product-detail] loadReviews error:', e)
  } finally {
    reviewLoading.value = false
  }
}

// ============ 事件处理 ============

/** 返回 */
function handleBack() {
  uni.navigateBack({ delta: 1 })
}

/** 图片预览 */
function handleImagePreview(index: number) {
  const urls = productImages.value.map(item => item.url).filter(Boolean)
  if (urls.length === 0) return
  uni.previewImage({
    current: urls[index] || urls[0],
    urls
  })
}

/** 评价图片预览 */
function previewReviewImage(images: string[], index: number) {
  uni.previewImage({
    current: images[index],
    urls: images
  })
}

/** 选择规格 */
function handleSelectSpec(idx: number) {
  selectedSpecIndex.value = idx
}

/** 客服 */
function handleContactCs() {
  showToast('客服功能开发中', 'none')
}

/** 加入购物车 */
async function handleAddToCart() {
  if (!product.value) return
  if (currentStock.value <= 0) {
    showToast('商品已售罄', 'error')
    return
  }
  // 打开 SKU 弹窗
  skuAction.value = 'cart'
  showSkuPopup.value = true
}

/** 立即购买 */
function handleBuyNow() {
  if (!product.value) return
  if (currentStock.value <= 0) {
    showToast('商品已售罄', 'error')
    return
  }
  // 打开 SKU 弹窗
  skuAction.value = 'buy'
  showSkuPopup.value = true
}

/** SKU 确认 */
async function handleSkuConfirm() {
  if (!product.value) return
  if (currentStock.value <= 0) {
    showToast('商品已售罄', 'error')
    return
  }
  if (quantity.value < 1) {
    showToast('请选择购买数量', 'error')
    return
  }

  if (skuAction.value === 'cart') {
    await doAddToCart()
  } else {
    doBuyNow()
  }
}

/** 执行加入购物车 */
async function doAddToCart() {
  if (!product.value) return
  try {
    await cartApi.addToCart({
      productId: ensureStringId(product.value.id),
      quantity: quantity.value
    })
    showToast('已加入购物车', 'success')
    showSkuPopup.value = false
  } catch (e) {
    console.error('[product-detail] addToCart error:', e)
  }
}

/** 执行立即购买 → 跳转结算 */
function doBuyNow() {
  if (!product.value) return
  showSkuPopup.value = false
  // 跳转结算页（订单分包），通过参数传递立即购买的商品信息
  // 立即购买场景：使用 buyNow=1 + productId + quantity
  navigate.to('pages-order/pages/checkout/checkout', {
    buyNow: '1',
    productId: ensureStringId(product.value.id),
    quantity: String(quantity.value)
  })
}

// ============ 触底加载（评价） ============
onReachBottom(() => {
  if (reviewHasMore.value && !reviewLoading.value) {
    loadReviews(false)
  }
})

// ============ 工具函数 ============

/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 格式化时间 */
function formatTime(time: string): string {
  if (!time) return ''
  // 简单处理：截取到分钟
  return time.replace('T', ' ').substring(0, 16)
}

/**
 * 过滤富文本 HTML（spec.md 4.3）
 * - 移除不支持的标签：script、style、link、meta
 * - rich-text 不支持 class 选择器，移除 class 属性（保留 inline style）
 */
function filterRichTextHtml(html: string): string {
  if (!html) return ''
  let result = html
  // 移除 script/style/link/meta 标签及其内容
  result = result.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
  result = result.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')
  result = result.replace(/<link\b[^>]*\/?>/gi, '')
  result = result.replace(/<meta\b[^>]*\/?>/gi, '')
  // 移除 class 属性（rich-text 不支持 class 选择器）
  result = result.replace(/\sclass\s*=\s*"[^"]*"/gi, '')
  result = result.replace(/\sclass\s*=\s*'[^']*'/gi, '')
  // 移除 id 属性（避免冲突）
  result = result.replace(/\sid\s*=\s*"[^"]*"/gi, '')
  result = result.replace(/\sid\s*=\s*'[^']*'/gi, '')
  return result
}
</script>

<style lang="scss" scoped>
.product-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 0;
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
    }

    .navbar-right {
      padding: 8rpx;
      width: 56rpx;
    }
  }
}

.navbar-placeholder {
  width: 100%;
}

.loading-wrap {
  padding: 48rpx 24rpx;
  background-color: #ffffff;
}

/* 图片轮播 */
.image-swiper {
  width: 100%;
  background-color: #ffffff;
}

/* 价格信息 */
.price-section {
  background-color: #ffffff;
  padding: 24rpx;

  .price-row {
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    .price-symbol {
      font-size: 28rpx;
      color: #ff4d4f;
      font-weight: bold;
    }

    .price-current {
      font-size: 48rpx;
      color: #ff4d4f;
      font-weight: bold;
    }

    .price-original {
      font-size: 26rpx;
      color: #c0c4cc;
      text-decoration: line-through;
      margin-left: 16rpx;
    }
  }

  .sales-row {
    display: flex;
    justify-content: space-between;
    margin-top: 16rpx;

    .sales-text,
    .stock-text {
      font-size: 24rpx;
      color: #909399;
    }
  }
}

/* 标题 */
.title-section {
  background-color: #ffffff;
  padding: 24rpx;
  margin-top: 2rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;

  .title {
    font-size: 32rpx;
    font-weight: bold;
    color: #303133;
    line-height: 1.4;
  }

  .subtitle {
    font-size: 26rpx;
    color: #909399;
    line-height: 1.4;
  }
}

/* 服务保障 */
.service-section {
  background-color: #ffffff;
  padding: 20rpx 24rpx;
  margin-top: 2rpx;

  .service-tags {
    display: flex;
    align-items: center;
    gap: 16rpx;

    .service-more {
      margin-left: auto;
      padding: 8rpx;
    }
  }
}

/* 通用 section title */
.section-title {
  padding: 24rpx 24rpx 16rpx;
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  background-color: #ffffff;

  text {
    border-left: 6rpx solid #ff4d4f;
    padding-left: 16rpx;
  }
}

/* 参数速览 */
.params-section {
  margin-top: 16rpx;
  background-color: #ffffff;
}

/* 评价 */
.review-section {
  margin-top: 16rpx;
  background-color: #ffffff;
  padding-bottom: 24rpx;

  .review-list {
    padding: 0 24rpx;

    .review-item {
      padding: 24rpx 0;
      border-bottom: 2rpx solid #f5f5f5;
      display: flex;
      flex-direction: column;
      gap: 12rpx;

      .review-header {
        display: flex;
        align-items: center;
        gap: 16rpx;

        .review-user {
          font-size: 26rpx;
          color: #303133;
          flex: 1;
        }
      }

      .review-content {
        font-size: 28rpx;
        color: #303133;
        line-height: 1.5;
      }

      .review-images {
        display: flex;
        gap: 16rpx;
        flex-wrap: wrap;
      }

      .review-time {
        font-size: 22rpx;
        color: #c0c4cc;
      }

      .review-reply {
        background-color: #f5f5f5;
        padding: 16rpx;
        border-radius: 8rpx;

        .reply-label {
          font-size: 24rpx;
          color: #ff4d4f;
          font-weight: bold;
        }

        .reply-content {
          font-size: 24rpx;
          color: #606266;
        }
      }
    }
  }
}

/* 售后说明 */
.after-sale-section {
  margin-top: 16rpx;
  background-color: #ffffff;
  padding-bottom: 24rpx;

  .collapse-content {
    font-size: 26rpx;
    color: #606266;
    line-height: 1.6;
    padding: 16rpx 0;
  }
}

/* 富文本详情 */
.rich-section {
  margin-top: 16rpx;
  background-color: #ffffff;
  padding-bottom: 24rpx;

  .rich-content {
    padding: 24rpx;
    width: 100%;
  }
}

/* 底部占位 */
.bottom-placeholder {
  height: 120rpx;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.04);
  z-index: 998;
  /* 兼容安全区 */
  padding-bottom: env(safe-area-inset-bottom);

  .bar-icon {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4rpx;
    padding: 0 16rpx;

    .bar-icon-text {
      font-size: 20rpx;
      color: #606266;
    }
  }

  .bar-actions {
    flex: 1;
    display: flex;
    align-items: center;
    margin-left: 24rpx;
  }
}

/* SKU 弹窗 */
.sku-popup {
  padding: 32rpx 24rpx;

  .sku-header {
    display: flex;
    align-items: center;
    gap: 24rpx;
    padding-bottom: 24rpx;
    border-bottom: 2rpx solid #f5f5f5;

    .sku-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8rpx;

      .sku-price {
        font-size: 36rpx;
        color: #ff4d4f;
        font-weight: bold;
      }

      .sku-stock,
      .sku-selected {
        font-size: 24rpx;
        color: #909399;
      }
    }
  }

  .sku-content {
    max-height: 600rpx;
    padding: 24rpx 0;

    .spec-group {
      margin-bottom: 32rpx;

      .spec-label {
        font-size: 28rpx;
        font-weight: bold;
        color: #303133;
        margin-bottom: 16rpx;
        display: block;
      }

      .spec-options {
        display: flex;
        flex-wrap: wrap;
        gap: 16rpx;

        .spec-option {
          padding: 16rpx 32rpx;
          background-color: #f5f5f5;
          border-radius: 8rpx;
          font-size: 26rpx;
          color: #606266;

          &.active {
            background-color: #fff5f5;
            color: #ff4d4f;
            border: 2rpx solid #ff4d4f;
          }
        }
      }
    }

    .quantity-group {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .quantity-label {
        font-size: 28rpx;
        font-weight: bold;
        color: #303133;
      }
    }
  }

  .sku-footer {
    margin-top: 24rpx;
  }
}

/* 服务保障弹窗 */
.service-popup {
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

  .service-list {
    .service-item {
      display: flex;
      align-items: flex-start;
      gap: 16rpx;
      padding: 24rpx 0;
      border-bottom: 2rpx solid #f5f5f5;

      .service-text {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .service-name {
          font-size: 28rpx;
          font-weight: bold;
          color: #303133;
        }

        .service-desc {
          font-size: 24rpx;
          color: #909399;
        }
      }
    }
  }
}
</style>