<!--
  SeckillCard 秒杀商品卡片组件
  用途：秒杀专区页 2 列网格展示秒杀商品
  对齐 spec.md 3.5 秒杀专区 / plan.md 4.5 防重放头传递
  复用 ProductCard 视觉风格，增加秒杀价、库存进度条、抢购按钮
-->
<template>
  <view class="seckill-card">
    <!-- 商品主图 -->
    <view class="image-wrap" @click="handleCardClick">
      <u-image
        :src="goods.productImage || ''"
        mode="aspectFill"
        width="100%"
        height="320rpx"
        :lazy-load="true"
        :show-loading="true"
        :show-error="true"
        radius="8rpx 8rpx 0 0"
      />
      <!-- 秒杀角标 -->
      <view class="seckill-badge">
        <text class="badge-text">秒杀</text>
      </view>
      <!-- 售罄遮罩 -->
      <view v-if="isSoldOut" class="sold-out-mask">
        <text class="sold-out-text">已抢光</text>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="info">
      <!-- 标题 -->
      <text class="title ellipsis-2">{{ goods.productName }}</text>

      <!-- 价格行 -->
      <view class="price-row">
        <view class="price-left">
          <text class="price-symbol">¥</text>
          <text class="price-current">{{ formatPrice(goods.seckillPrice) }}</text>
          <text class="price-original">¥{{ formatPrice(goods.originalPrice) }}</text>
        </view>
      </view>

      <!-- 库存进度条 -->
      <view class="stock-bar">
        <view class="stock-track">
          <view class="stock-fill" :style="{ width: `${soldPercent}%` }" />
        </view>
        <text class="stock-text">已抢{{ soldPercent }}%</text>
      </view>

      <!-- 抢购按钮行 -->
      <view class="action-row">
        <!-- 限购标签 -->
        <text v-if="goods.limitPerUser > 0" class="limit-tag">限购{{ goods.limitPerUser }}件</text>

        <!-- 抢购按钮：根据状态切换 -->
        <button
          class="seckill-btn"
          :class="btnClass"
          :disabled="btnDisabled"
          @click.stop="handleSeckill"
        >
          <text class="btn-text">{{ btnText }}</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SeckillGoodsVO } from '@/types'

const props = withDefaults(
  defineProps<{
    /** 秒杀商品 VO */
    goods: SeckillGoodsVO
    /** 是否正在抢购中（防止重复点击） */
    purchasing?: boolean
  }>(),
  {
    purchasing: false
  }
)

const emit = defineEmits<{
  /** 点击抢购按钮 */
  (e: 'seckill', goods: SeckillGoodsVO): void
  /** 点击卡片（跳转详情） */
  (e: 'click', goods: SeckillGoodsVO): void
}>()

/** 是否售罄（可用库存为 0） */
const isSoldOut = computed(() => props.goods.availableStock <= 0)

/** 已抢百分比（向下取整，0~100） */
const soldPercent = computed(() => {
  const total = props.goods.totalStock
  if (!total || total <= 0) return 0
  const sold = total - props.goods.availableStock
  const percent = Math.floor((sold / total) * 100)
  return Math.min(100, Math.max(0, percent))
})

/** 按钮文案 */
const btnText = computed(() => {
  if (props.purchasing) return '抢购中'
  switch (props.goods.status) {
    case 0:
      return '未开始'
    case 1:
      return isSoldOut.value ? '已抢光' : '立即抢购'
    case 2:
      return '已结束'
    default:
      return '立即抢购'
  }
})

/** 按钮是否禁用 */
const btnDisabled = computed(() => {
  if (props.purchasing) return true
  // 未开始(0) / 已结束(2) / 已抢光 禁用
  return props.goods.status !== 1 || isSoldOut.value
})

/** 按钮样式类 */
const btnClass = computed(() => {
  if (props.purchasing) return 'btn-loading'
  switch (props.goods.status) {
    case 0:
      return 'btn-not-started'
    case 1:
      return isSoldOut.value ? 'btn-sold-out' : 'btn-active'
    case 2:
      return 'btn-ended'
    default:
      return 'btn-active'
  }
})

/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 点击抢购按钮 */
function handleSeckill() {
  if (btnDisabled.value) return
  emit('seckill', props.goods)
}

/** 点击卡片 */
function handleCardClick() {
  emit('click', props.goods)
}
</script>

<style lang="scss" scoped>
.seckill-card {
  position: relative;
  width: 100%;
  background-color: #ffffff;
  border-radius: 8rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .image-wrap {
    position: relative;
    width: 100%;
    height: 320rpx;
    background-color: #f5f5f5;

    .seckill-badge {
      position: absolute;
      top: 0;
      left: 0;
      background: linear-gradient(135deg, #ff4d4f, #ff7a45);
      color: #ffffff;
      padding: 4rpx 16rpx;
      border-bottom-right-radius: 12rpx;
      z-index: 2;

      .badge-text {
        font-size: 22rpx;
        font-weight: bold;
        color: #ffffff;
      }
    }

    .sold-out-mask {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.55);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 3;

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
    gap: 10rpx;

    .title {
      font-size: 26rpx;
      color: #303133;
      line-height: 1.4;
      font-weight: 500;
      min-height: 72rpx;
    }

    .price-row {
      display: flex;
      align-items: baseline;

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
        font-size: 34rpx;
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

    .stock-bar {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .stock-track {
        flex: 1;
        height: 12rpx;
        background-color: #ffe1e1;
        border-radius: 6rpx;
        overflow: hidden;

        .stock-fill {
          height: 100%;
          background: linear-gradient(90deg, #ff4d4f, #ff7a45);
          border-radius: 6rpx;
          transition: width 0.3s ease;
        }
      }

      .stock-text {
        font-size: 20rpx;
        color: #ff4d4f;
        min-width: 100rpx;
        text-align: right;
      }
    }

    .action-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12rpx;
      margin-top: 4rpx;

      .limit-tag {
        font-size: 20rpx;
        color: #909399;
        background-color: #f4f4f5;
        padding: 2rpx 8rpx;
        border-radius: 4rpx;
      }

      .seckill-btn {
        flex: 1;
        height: 56rpx;
        line-height: 56rpx;
        padding: 0 20rpx;
        border-radius: 28rpx;
        font-size: 24rpx;
        font-weight: 600;
        border: none;
        text-align: center;

        .btn-text {
          color: #ffffff;
          font-size: 24rpx;
          font-weight: 600;
        }

        &.btn-active {
          background: linear-gradient(135deg, #ff4d4f, #ff7a45);
          color: #ffffff;
        }

        &.btn-not-started,
        &.btn-ended,
        &.btn-sold-out,
        &.btn-loading {
          background-color: #c0c4cc;
          color: #ffffff;
        }

        &[disabled] {
          opacity: 0.7;
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

/* 两行省略 */
.ellipsis-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>