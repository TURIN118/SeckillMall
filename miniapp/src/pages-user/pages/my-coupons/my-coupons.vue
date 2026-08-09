<!--
  我的优惠券页（对齐 spec.md 3.11 我的优惠券 / tasks.md T5.4）
  - 调用 couponApi.getCouponList()
  - 状态筛选 u-tabs：未使用 / 已使用 / 已过期
  - 优惠券卡片展示：金额 / 门槛 / 有效期 / 使用说明
  - u-collapse 使用说明折叠
-->
<template>
  <view class="coupons-page">
    <!-- 状态筛选 tabs -->
    <u-tabs
      :list="tabList"
      :current="currentTab"
      @click="onTabChange"
      :scrollable="false"
      barWidth="60"
    />

    <!-- 优惠券列表 -->
    <view v-if="couponList.length > 0" class="coupon-list">
      <view
        v-for="item in couponList"
        :key="item.id"
        class="coupon-card"
        :class="couponCardClass(item)"
      >
        <!-- 左侧金额 -->
        <view class="coupon-left">
          <view class="amount-row">
            <text class="amount-symbol">¥</text>
            <text class="amount-value">{{ formatAmount(item) }}</text>
          </view>
          <text class="amount-desc">{{ amountDesc(item) }}</text>
        </view>

        <!-- 右侧信息 -->
        <view class="coupon-right">
          <view class="info-top">
            <text class="coupon-name">{{ item.name }}</text>
            <u-tag
              :text="statusTagText(item)"
              :type="statusTagType(item)"
              size="mini"
              plain
            />
          </view>
          <text class="validity">有效期：{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</text>
          <text class="threshold">{{ thresholdText(item) }}</text>

          <!-- 使用说明折叠 -->
          <u-collapse v-if="item.description" :accordion="true">
            <u-collapse-item title="使用说明">
              <text class="desc-text">{{ item.description }}</text>
            </u-collapse-item>
          </u-collapse>
        </view>
      </view>

      <!-- 加载更多 -->
      <u-loadmore
        :status="loadMoreStatus"
        :contentText="loadMoreText"
      />
    </view>

    <!-- 空状态 -->
    <u-empty
      v-else-if="!loading"
      :mode="emptyMode"
      :text="emptyText"
      marginTop="120"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import * as couponApi from '@/api/coupon'
import { requireAuthAsync } from '@/utils/authGuard'
import { showToast } from '@/utils/toast'
import type { CouponVO } from '@/types'

/** Tab 选项 */
const tabList = [
  { name: '未使用', value: 0 },
  { name: '已使用', value: 1 },
  { name: '已过期', value: 2 }
]

const currentTab = ref<number>(0)
const couponList = ref<CouponVO[]>([])
const loading = ref<boolean>(false)

/** 分页参数 */
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
  hasMore: true
})

/** 加载更多状态 */
const loadMoreStatus = computed<'loadmore' | 'loading' | 'nomore'>(() => {
  if (loading.value) return 'loading'
  if (!pagination.hasMore) return 'nomore'
  return 'loadmore'
})

const loadMoreText = {
  loadmore: '上拉加载更多',
  loading: '加载中...',
  nomore: '没有更多了'
}

/** 空状态 */
const emptyMode = computed(() => {
  if (currentTab.value === 0) return 'coupon'
  if (currentTab.value === 1) return 'gift'
  return 'expired'
})

const emptyText = computed(() => {
  if (currentTab.value === 0) return '暂无可用优惠券'
  if (currentTab.value === 1) return '暂无已使用优惠券'
  return '暂无已过期优惠券'
})

onLoad(() => {
  if (!requireAuthAsync()) return
  resetAndFetch()
})

/** 重置并拉取 */
async function resetAndFetch() {
  pagination.page = 1
  pagination.hasMore = true
  couponList.value = []
  await fetchList()
}

/** 拉取优惠券列表 */
async function fetchList() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await couponApi.getCouponList({
      status: tabList[currentTab.value].value,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    const list = res?.list || []
    if (pagination.page === 1) {
      couponList.value = list
    } else {
      couponList.value = [...couponList.value, ...list]
    }
    pagination.total = res?.total || 0
    pagination.hasMore = !!res?.hasMore
  } catch (e) {
    console.error('拉取优惠券列表失败', e)
    showToast('优惠券列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

/** Tab 切换 */
function onTabChange(item: any) {
  const idx = tabList.findIndex(t => t.name === item.name)
  if (idx === -1 || idx === currentTab.value) return
  currentTab.value = idx
  resetAndFetch()
}

/** 触底加载 */
onReachBottom(() => {
  if (!pagination.hasMore || loading.value) return
  pagination.page++
  fetchList()
})

/** 下拉刷新 */
onPullDownRefresh(() => {
  resetAndFetch().finally(() => {
    uni.stopPullDownRefresh()
  })
})

/** 格式化金额 */
function formatAmount(item: CouponVO): string {
  // type: 1满减 2折扣 3无门槛
  if (item.type === 2) {
    // 折扣：value 表示折扣率（如 8.5 表示 8.5 折）
    return `${item.value}`
  }
  // 满减 / 无门槛：discountAmount 或 value
  const amount = item.discountAmount || item.value || 0
  return amount.toFixed(2)
}

/** 金额描述 */
function amountDesc(item: CouponVO): string {
  if (item.type === 2) return '折'
  return '元'
}

/** 门槛文本 */
function thresholdText(item: CouponVO): string {
  if (item.type === 3) return '无门槛'
  if (item.minAmount > 0) return `满 ${item.minAmount.toFixed(2)} 元可用`
  return '无门槛'
}

/** 卡片样式（已使用/已过期置灰） */
function couponCardClass(item: CouponVO): string {
  if (item.status === 1) return 'used'
  if (item.status === 2) return 'expired'
  return ''
}

/** 状态标签文本 */
function statusTagText(item: CouponVO): string {
  if (item.status === 0) return '可使用'
  if (item.status === 1) return '已使用'
  return '已过期'
}

/** 状态标签类型 */
function statusTagType(item: CouponVO): 'success' | 'info' | 'error' | 'warning' {
  if (item.status === 0) return 'success'
  if (item.status === 1) return 'info'
  return 'warning'
}

/** 格式化日期 */
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.slice(0, 10)
}
</script>

<style lang="scss" scoped>
.coupons-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}

/* 优惠券列表 */
.coupon-list {
  padding: 24rpx 24rpx 0;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

/* 优惠券卡片 */
.coupon-card {
  display: flex;
  background-color: #ffffff;
  border-radius: 12rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  /* 左侧金额区 */
  .coupon-left {
    flex-shrink: 0;
    width: 200rpx;
    background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24rpx 0;

    .amount-row {
      display: flex;
      align-items: baseline;

      .amount-symbol {
        font-size: 26rpx;
        color: #ffffff;
        font-weight: bold;
      }

      .amount-value {
        font-size: 56rpx;
        color: #ffffff;
        font-weight: bold;
      }
    }

    .amount-desc {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.9);
      margin-top: 4rpx;
    }
  }

  /* 右侧信息区 */
  .coupon-right {
    flex: 1;
    padding: 24rpx 28rpx;
    display: flex;
    flex-direction: column;
    gap: 8rpx;

    .info-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12rpx;

      .coupon-name {
        font-size: 30rpx;
        font-weight: bold;
        color: #303133;
        flex: 1;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
    }

    .validity {
      font-size: 24rpx;
      color: #909399;
    }

    .threshold {
      font-size: 26rpx;
      color: #ff4d4f;
    }

    .desc-text {
      font-size: 24rpx;
      color: #606266;
      line-height: 1.6;
    }
  }

  /* 已使用 / 已过期 置灰 */
  &.used .coupon-left,
  &.expired .coupon-left {
    background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%);
  }

  &.used .coupon-right .threshold,
  &.expired .coupon-right .threshold {
    color: #909399;
  }
}
</style>