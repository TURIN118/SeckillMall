<!--
  ProductCard 商品卡片公共组件
  用途：首页猜你喜欢 / 商品列表 / 收藏夹 复用
  对齐 spec.md 3.2 商品卡片 2 列网格 + 4.1 组件映射（el-image → u-image）
-->
<template>
  <view class="product-card" @click="handleClick">
    <!-- 商品主图 -->
    <view class="image-wrap">
      <u-image
        :src="product.mainImage || ''"
        mode="aspectFill"
        width="100%"
        height="340rpx"
        :lazy-load="true"
        :show-loading="true"
        :show-error="true"
        radius="8rpx 8rpx 0 0"
      />
      <!-- 库存售罊遮罩 -->
      <view v-if="isSoldOut" class="sold-out-mask">
        <text class="sold-out-text">已售罄</text>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="info">
      <!-- 标题 -->
      <text class="title ellipsis-2">{{ product.name }}</text>

      <!-- 副标题（如有） -->
      <text v-if="product.subtitle" class="subtitle ellipsis-1">{{ product.subtitle }}</text>

      <!-- 价格行 -->
      <view class="price-row">
        <view class="price-left">
          <text class="price-symbol">¥</text>
          <text class="price-current">{{ formatPrice(product.price) }}</text>
          <template v-if="product.originalPrice && product.originalPrice > product.price">
            <text class="price-original">¥{{ formatPrice(product.originalPrice) }}</text>
          </template>
        </view>
      </view>

      <!-- 底部标签行：销量 + 标签 -->
      <view class="meta-row">
        <text v-if="product.sales > 0" class="sales">已售{{ formatSales(product.sales) }}</text>
        <view class="tags">
          <u-tag
            v-if="isDiscount"
            text="促销"
            type="error"
            size="mini"
            plain
          />
          <u-tag
            v-if="isHot"
            text="热销"
            type="warning"
            size="mini"
            plain
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { navigate } from '@/utils/navigate'
import { ensureStringId } from '@/utils/snowflake'
import type { ProductVO } from '@/types'

const props = defineProps<{
  /** 商品 VO */
  product: ProductVO
  /** 是否禁用点击跳转（收藏管理模式用） */
  disableClick?: boolean
}>()

const emit = defineEmits<{
  (e: 'click', product: ProductVO): void
}>()

/** 是否售罄 */
const isSoldOut = computed(() => props.product.stock <= 0)

/** 是否促销（原价大于现价） */
const isDiscount = computed(() =>
  props.product.originalPrice > 0 && props.product.originalPrice > props.product.price
)

/** 是否热销（销量 > 100） */
const isHot = computed(() => props.product.sales >= 100)

/** 格式化价格（保留 2 位小数） */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 格式化销量（>10000 显示万） */
function formatSales(sales: number): string {
  if (sales >= 10000) {
    return `${(sales / 10000).toFixed(1)}万`
  }
  return String(sales)
}

/** 点击跳转商品详情 */
function handleClick() {
  if (props.disableClick) {
    emit('click', props.product)
    return
  }
  emit('click', props.product)
  const id = ensureStringId(props.product.id)
  // 跳转商品分包详情页，雪花 ID 用 string 传递（navigate 内部 encodeURIComponent）
  navigate.to('pages-product/pages/product-detail/product-detail', { id })
}
</script>

<style lang="scss" scoped>
.product-card {
  position: relative;
  width: 100%;
  background-color: #ffffff;
  border-radius: 8rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .image-wrap {
    position: relative;
    width: 100%;
    height: 340rpx;
    background-color: #f5f5f5;

    .sold-out-mask {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;

      .sold-out-text {
        color: #ffffff;
        font-size: 28rpx;
        font-weight: bold;
        padding: 8rpx 24rpx;
        border: 2rpx solid #ffffff;
        border-radius: 4rpx;
      }
    }
  }

  .info {
    padding: 16rpx 20rpx 20rpx;
    display: flex;
    flex-direction: column;
    gap: 8rpx;

    .title {
      font-size: 26rpx;
      color: #303133;
      line-height: 1.4;
      font-weight: 500;
    }

    .subtitle {
      font-size: 22rpx;
      color: #909399;
      line-height: 1.3;
    }

    .price-row {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      margin-top: 4rpx;

      .price-left {
        display: flex;
        align-items: baseline;
      }

      .price-symbol {
        font-size: 22rpx;
        color: #ff4d4f;
        font-weight: bold;
      }

      .price-current {
        font-size: 32rpx;
        color: #ff4d4f;
        font-weight: bold;
        margin-left: 2rpx;
      }

      .price-original {
        font-size: 22rpx;
        color: #c0c4cc;
        text-decoration: line-through;
        margin-left: 12rpx;
      }
    }

    .meta-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 4rpx;

      .sales {
        font-size: 22rpx;
        color: #909399;
      }

      .tags {
        display: flex;
        gap: 8rpx;
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

/* 两行省略 */
.ellipsis-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>