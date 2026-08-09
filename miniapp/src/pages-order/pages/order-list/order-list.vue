<!--
  订单列表页 OrderList（对齐 spec.md 3.7 / tasks.md T3.6）
  功能点：
    - 状态筛选（u-tabs 顶部 scrollable：全部/待付款/待发货/待收货/已完成/已取消）
    - 订单类型筛选（u-tabs：全部/普通订单/秒杀订单）
    - 订单卡片：订单号、状态、商品列表、金额、操作按钮
    - 左滑删除（仅已完成/已取消状态）
    - 触底加载 onReachBottom + u-loadmore
    - 下拉刷新
    - 点击跳转订单详情
  鉴权：是（requiresAuth）
-->
<template>
  <view class="order-list-page">
    <!-- 状态筛选 tabs -->
    <view class="status-tabs">
      <u-tabs
        :list="statusTabs"
        :current="currentStatusIndex"
        scrollable
        @click="onStatusTabClick"
        :active-style="{ color: '#FF4D4F' }"
        :inactive-style="{ color: '#606266' }"
        bar-width="40rpx"
      />
    </view>

    <!-- 订单类型筛选 tabs -->
    <view class="type-tabs">
      <u-tabs
        :list="typeTabs"
        :current="currentTypeIndex"
        @click="onTypeTabClick"
        :active-style="{ color: '#FF4D4F' }"
        :inactive-style="{ color: '#606266' }"
      />
    </view>

    <!-- 订单列表 -->
    <view class="order-list">
      <!-- 空状态 -->
      <view v-if="!loading && orderList.length === 0" class="empty-state">
        <u-empty
          mode="order"
          text="暂无订单"
          icon-size="180"
          margin-top="120"
        />
      </view>

      <!-- 订单卡片 -->
      <u-swipe-action
        v-for="order in orderList"
        :key="order.id"
        :show="false"
        :options="canDelete(order.status) ? swipeOptions : []"
        @click="(index: number) => handleSwipeAction(order, index)"
      >
        <view class="order-card" @click="goOrderDetail(order)">
          <!-- 订单头部 -->
          <view class="order-header">
            <text class="order-no">订单号：{{ order.orderNo }}</text>
            <text class="order-status" :class="getStatusClass(order.status)">
              {{ getStatusText(order.status) }}
            </text>
          </view>

          <!-- 商品列表 -->
          <view class="order-goods">
            <view
              v-for="item in order.items"
              :key="item.id"
              class="goods-item"
            >
              <u-image
                :src="item.productImage || ''"
                mode="aspectFill"
                width="120rpx"
                height="120rpx"
                radius="8rpx"
                :lazy-load="true"
              />
              <view class="goods-info">
                <text class="goods-name ellipsis-2">{{ item.productName }}</text>
                <text v-if="item.skuSpec" class="goods-sku ellipsis-1">{{ item.skuSpec }}</text>
                <view class="goods-price-row">
                  <text class="price-symbol">¥</text>
                  <text class="price-value">{{ formatPrice(item.price) }}</text>
                  <text class="goods-qty">×{{ item.quantity }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 订单底部 -->
          <view class="order-footer">
            <view class="order-amount">
              <text class="amount-label">共{{ getTotalQuantity(order) }}件 合计</text>
              <text class="amount-symbol">¥</text>
              <text class="amount-value">{{ formatPrice(order.payAmount) }}</text>
            </view>
            <view class="order-actions">
              <u-button
                v-if="order.status === OrderStatus.PENDING_PAY"
                type="error"
                shape="circle"
                size="mini"
                text="去支付"
                @click.stop="handlePay(order)"
              />
              <u-button
                v-if="order.status === OrderStatus.PENDING_PAY"
                type="default"
                shape="circle"
                size="mini"
                text="取消"
                @click.stop="handleCancel(order)"
              />
              <u-button
                v-if="order.status === OrderStatus.PENDING_RECEIVE"
                type="error"
                shape="circle"
                size="mini"
                text="确认收货"
                @click.stop="handleConfirm(order)"
              />
              <u-button
                v-if="canDelete(order.status)"
                type="default"
                shape="circle"
                size="mini"
                text="删除"
                @click.stop="handleDelete(order)"
              />
            </view>
          </view>
        </view>
      </u-swipe-action>

      <!-- 加载更多 -->
      <u-loadmore
        v-if="orderList.length > 0"
        :status="loadMoreStatus"
        :content-text="loadMoreText"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showConfirm, showLoading, hideLoading } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import * as orderApi from '@/api/order'
import { OrderStatus, type OrderVO, type OrderQuery, type PageResult } from '@/types'

/** 状态 tabs 配置 */
interface TabItem {
  name: string
  value: number | undefined
}

const statusTabs: TabItem[] = [
  { name: '全部', value: undefined },
  { name: '待付款', value: OrderStatus.PENDING_PAY },
  { name: '待发货', value: OrderStatus.PENDING_DELIVER },
  { name: '待收货', value: OrderStatus.PENDING_RECEIVE },
  { name: '已完成', value: OrderStatus.COMPLETED },
  { name: '已取消', value: OrderStatus.CANCELLED }
]

const typeTabs: TabItem[] = [
  { name: '全部', value: undefined },
  { name: '普通订单', value: 0 },
  { name: '秒杀订单', value: 1 }
]

/** 当前选中的状态/类型索引 */
const currentStatusIndex = ref<number>(0)
const currentTypeIndex = ref<number>(0)

/** 订单列表 */
const orderList = ref<OrderVO[]>([])
/** 加载中 */
const loading = ref<boolean>(false)
/** 分页参数 */
const pageNum = ref<number>(1)
const pageSize = ref<number>(10)
const hasMore = ref<boolean>(true)
/** 总数 */
const total = ref<number>(0)

/** 左滑操作按钮 */
const swipeOptions = [
  { text: '删除', style: { backgroundColor: '#FF4D4F' } }
]

/** loadmore 状态 */
const loadMoreStatus = computed<'loading' | 'nomore' | 'loadmore'>(() => {
  if (loading.value) return 'loading'
  if (!hasMore.value) return 'nomore'
  return 'loadmore'
})

const loadMoreText = computed(() => ({
  loadmore: '上拉加载更多',
  loading: '正在加载...',
  nomore: '没有更多订单了'
}))

/** 当前筛选的状态值 */
const currentStatus = computed<number | undefined>(() => statusTabs[currentStatusIndex.value]?.value)
/** 当前筛选的类型值 */
const currentType = computed<number | undefined>(() => typeTabs[currentTypeIndex.value]?.value)

/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 获取状态文本 */
function getStatusText(status: number): string {
  const map: Record<number, string> = {
    [OrderStatus.PENDING_PAY]: '待付款',
    [OrderStatus.PENDING_DELIVER]: '待发货',
    [OrderStatus.PENDING_RECEIVE]: '待收货',
    [OrderStatus.COMPLETED]: '已完成',
    [OrderStatus.CANCELLED]: '已取消',
    [OrderStatus.REFUNDING]: '退款中',
    [OrderStatus.REFUNDED]: '已退款'
  }
  return map[status] || '未知'
}

/** 获取状态样式类 */
function getStatusClass(status: number): string {
  const map: Record<number, string> = {
    [OrderStatus.PENDING_PAY]: 'status-pending-pay',
    [OrderStatus.PENDING_DELIVER]: 'status-pending',
    [OrderStatus.PENDING_RECEIVE]: 'status-pending',
    [OrderStatus.COMPLETED]: 'status-completed',
    [OrderStatus.CANCELLED]: 'status-cancelled'
  }
  return map[status] || ''
}

/** 是否可删除（仅已完成/已取消） */
function canDelete(status: number): boolean {
  return status === OrderStatus.COMPLETED || status === OrderStatus.CANCELLED
}

/** 获取订单总件数 */
function getTotalQuantity(order: OrderVO): number {
  return order.items.reduce((sum, item) => sum + item.quantity, 0)
}

/** 加载订单列表 */
async function loadOrderList(reset = false) {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
    orderList.value = []
  }
  if (!hasMore.value && !reset) return

  loading.value = true
  try {
    const params: OrderQuery = {
      page: pageNum.value,
      pageSize: pageSize.value,
      status: currentStatus.value,
      type: currentType.value
    }
    const res: PageResult<OrderVO> = await orderApi.getUnifiedOrderList(params)
    const list = res.list || []
    if (reset || pageNum.value === 1) {
      orderList.value = list
    } else {
      orderList.value = orderList.value.concat(list)
    }
    total.value = res.total || 0
    hasMore.value = !!res.hasMore && orderList.value.length < res.total
  } catch (e) {
    console.error('加载订单列表失败', e)
    showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 状态 tab 切换 */
function onStatusTabClick(item: TabItem) {
  const idx = statusTabs.findIndex(t => t.name === item.name)
  if (idx === currentStatusIndex.value) return
  currentStatusIndex.value = idx
  loadOrderList(true)
}

/** 类型 tab 切换 */
function onTypeTabClick(item: TabItem) {
  const idx = typeTabs.findIndex(t => t.name === item.name)
  if (idx === currentTypeIndex.value) return
  currentTypeIndex.value = idx
  loadOrderList(true)
}

/** 跳转订单详情 */
function goOrderDetail(order: OrderVO) {
  navigate.to('pages-order/pages/order-detail/order-detail', {
    id: ensureStringId(order.id)
  })
}

/** 去支付 */
async function handlePay(order: OrderVO) {
  const ok = await showConfirm(`确认支付订单 ${order.orderNo}？`, '支付确认')
  if (!ok) return
  showLoading('支付中...')
  try {
    const id = ensureStringId(order.id)
    // 根据订单类型选择支付接口（0 普通 / 1 秒杀）
    if (order.type === 0) {
      await orderApi.payNormalOrder(id)
    } else {
      await orderApi.payOrder(id)
    }
    hideLoading()
    showToast('支付成功', 'success')
    // 刷新列表
    loadOrderList(true)
  } catch (e) {
    hideLoading()
    console.error('支付失败', e)
    showToast('支付失败', 'error')
  }
}

/** 取消订单 */
async function handleCancel(order: OrderVO) {
  const ok = await showConfirm(`确认取消订单 ${order.orderNo}？`, '取消订单')
  if (!ok) return
  showLoading('取消中...')
  try {
    const id = ensureStringId(order.id)
    if (order.type === 0) {
      await orderApi.cancelNormalOrder(id)
    } else {
      await orderApi.cancelOrder(id)
    }
    hideLoading()
    showToast('取消成功', 'success')
    loadOrderList(true)
  } catch (e) {
    hideLoading()
    console.error('取消订单失败', e)
    showToast('取消失败', 'error')
  }
}

/** 确认收货 */
async function handleConfirm(order: OrderVO) {
  const ok = await showConfirm(`确认已收到商品？`, '确认收货')
  if (!ok) return
  showLoading('处理中...')
  try {
    const id = ensureStringId(order.id)
    if (order.type === 0) {
      await orderApi.confirmNormalOrder(id)
    } else {
      await orderApi.confirmOrder(id)
    }
    hideLoading()
    showToast('确认收货成功', 'success')
    loadOrderList(true)
  } catch (e) {
    hideLoading()
    console.error('确认收货失败', e)
    showToast('操作失败', 'error')
  }
}

/** 删除订单 */
async function handleDelete(order: OrderVO) {
  const ok = await showConfirm(`确认删除订单 ${order.orderNo}？`, '删除订单')
  if (!ok) return
  // 注意：当前 order.ts 未封装 deleteOrder 接口，spec.md 2.6 也未列出删除端点
  // 此处提示用户该功能暂未开放，避免调用不存在的接口
  showToast('删除订单功能暂未开放', 'none')
}

/** 左滑操作 */
async function handleSwipeAction(order: OrderVO, index: number) {
  if (index === 0) {
    await handleDelete(order)
  }
}

/** 触底加载 */
onReachBottom(() => {
  if (!hasMore.value || loading.value) return
  pageNum.value++
  loadOrderList()
})

/** 下拉刷新 */
onPullDownRefresh(() => {
  loadOrderList(true).finally(() => {
    uni.stopPullDownRefresh()
  })
})

/** 页面加载（支持从外部带初始状态参数进入，如 profile 页"待付款"入口） */
onLoad((options: Record<string, any>) => {
  if (options.status !== undefined && options.status !== null) {
    const statusVal = Number(options.status)
    const idx = statusTabs.findIndex(t => t.value === statusVal)
    if (idx >= 0) currentStatusIndex.value = idx
  }
  if (options.type !== undefined && options.type !== null) {
    const typeVal = Number(options.type)
    const idx = typeTabs.findIndex(t => t.value === typeVal)
    if (idx >= 0) currentTypeIndex.value = idx
  }
})

/** 页面显示 */
onShow(() => {
  if (!requireAuthAsync()) return
  // 每次显示刷新列表（订单状态可能在外部变更）
  loadOrderList(true)
})

onMounted(() => {
  // onShow 已处理首次加载
})
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 状态 tabs */
.status-tabs {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #ffffff;
  box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.04);
}

.type-tabs {
  background-color: #ffffff;
  border-bottom: 2rpx solid #f0f0f0;
}

/* 订单列表 */
.order-list {
  padding: 16rpx;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80rpx;
}

/* 订单卡片 */
.order-card {
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;

  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16rpx;
    border-bottom: 2rpx solid #f5f5f5;

    .order-no {
      font-size: 26rpx;
      color: #909399;
    }

    .order-status {
      font-size: 28rpx;
      font-weight: 500;

      &.status-pending-pay {
        color: #FF4D4F;
      }

      &.status-pending {
        color: #FF9900;
      }

      &.status-completed {
        color: #67C23A;
      }

      &.status-cancelled {
        color: #909399;
      }
    }
  }

  .order-goods {
    padding: 16rpx 0;

    .goods-item {
      display: flex;
      gap: 16rpx;
      padding: 12rpx 0;

      .goods-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 6rpx;
        min-width: 0;

        .goods-name {
          font-size: 28rpx;
          color: #303133;
          line-height: 1.4;
        }

        .goods-sku {
          font-size: 24rpx;
          color: #909399;
        }

        .goods-price-row {
          display: flex;
          align-items: baseline;
          gap: 4rpx;
          margin-top: auto;

          .price-symbol {
            font-size: 22rpx;
            color: #FF4D4F;
            font-weight: bold;
          }

          .price-value {
            font-size: 28rpx;
            color: #FF4D4F;
            font-weight: bold;
          }

          .goods-qty {
            font-size: 24rpx;
            color: #909399;
            margin-left: auto;
          }
        }
      }
    }
  }

  .order-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16rpx;
    border-top: 2rpx solid #f5f5f5;

    .order-amount {
      display: flex;
      align-items: baseline;

      .amount-label {
        font-size: 24rpx;
        color: #606266;
        margin-right: 8rpx;
      }

      .amount-symbol {
        font-size: 24rpx;
        color: #FF4D4F;
        font-weight: bold;
      }

      .amount-value {
        font-size: 32rpx;
        color: #FF4D4F;
        font-weight: bold;
      }
    }

    .order-actions {
      display: flex;
      gap: 12rpx;
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