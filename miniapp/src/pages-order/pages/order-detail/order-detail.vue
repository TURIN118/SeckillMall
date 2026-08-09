<!--
  订单详情页 OrderDetail（对齐 spec.md 3.8 / tasks.md T3.7）
  功能点：
    - 收货地址展示（u-cell）
    - 商品列表（u-cell 列表）
    - 支付（u-button + 模拟支付）
    - 确认收货（u-button + uni.showModal 确认弹窗）
    - 取消订单
    - 订单状态（u-steps 步骤展示：下单→付款→发货→收货→完成）
  入参：id（string，雪花 ID）
  鉴权：是（requiresAuth）
-->
<template>
  <view class="order-detail-page">
    <!-- 加载中骨架 -->
    <view v-if="loading" class="loading-state">
      <u-skeleton rows="8" :loading="true" />
    </view>

    <template v-else-if="order">
      <!-- 状态步骤展示 -->
      <view class="section status-section">
        <view class="status-header">
          <text class="status-text" :class="getStatusClass(order.status)">
            {{ getStatusText(order.status) }}
          </text>
          <text class="status-desc">{{ getStatusDesc(order.status) }}</text>
        </view>
        <u-steps
          :current="currentStep"
          :list="stepList"
          active-color="#FF4D4F"
          inactive-color="#909399"
        />
      </view>

      <!-- 收货地址 -->
      <view class="section address-section">
        <view class="section-title">
          <u-icon name="map" size="32rpx" color="#FF4D4F" />
          <text>收货地址</text>
        </view>
        <view v-if="addressInfo" class="address-card">
          <view class="receiver-row">
            <text class="receiver">{{ addressInfo.receiver }}</text>
            <text class="phone">{{ formatPhone(addressInfo.phone) }}</text>
            <u-tag
              v-if="addressInfo.isDefault"
              text="默认"
              type="error"
              size="mini"
              plain
            />
          </view>
          <text class="address-text">
            {{ addressInfo.province }}{{ addressInfo.city }}{{ addressInfo.district }}{{ addressInfo.detail }}
          </text>
        </view>
        <view v-else class="address-empty">
          <text>地址信息缺失</text>
        </view>
      </view>

      <!-- 商品列表 -->
      <view class="section goods-section">
        <view class="section-title">
          <text>商品信息</text>
        </view>
        <view class="goods-list">
          <view
            v-for="item in order.items"
            :key="item.id"
            class="goods-item"
            @click="goProductDetail(item)"
          >
            <u-image
              :src="item.productImage || ''"
              mode="aspectFill"
              width="140rpx"
              height="140rpx"
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
      </view>

      <!-- 订单信息 -->
      <view class="section info-section">
        <view class="section-title">
          <text>订单信息</text>
        </view>
        <view class="info-row">
          <text class="info-label">订单编号</text>
          <text class="info-value">{{ order.orderNo }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">订单类型</text>
          <text class="info-value">{{ order.type === 0 ? '普通订单' : '秒杀订单' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间</text>
          <text class="info-value">{{ formatTime(order.createdAt) }}</text>
        </view>
        <view v-if="order.payTime" class="info-row">
          <text class="info-label">付款时间</text>
          <text class="info-value">{{ formatTime(order.payTime) }}</text>
        </view>
        <view v-if="order.deliverTime" class="info-row">
          <text class="info-label">发货时间</text>
          <text class="info-value">{{ formatTime(order.deliverTime) }}</text>
        </view>
        <view v-if="order.confirmTime" class="info-row">
          <text class="info-label">收货时间</text>
          <text class="info-value">{{ formatTime(order.confirmTime) }}</text>
        </view>
        <view v-if="order.remark" class="info-row remark-row">
          <text class="info-label">订单备注</text>
          <text class="info-value">{{ order.remark }}</text>
        </view>
      </view>

      <!-- 金额明细 -->
      <view class="section amount-section">
        <view class="section-title">
          <text>金额明细</text>
        </view>
        <view class="amount-row">
          <text class="amount-label">商品金额</text>
          <text class="amount-value">¥{{ formatPrice(order.totalAmount) }}</text>
        </view>
        <view class="amount-row">
          <text class="amount-label">运费</text>
          <text class="amount-value">¥{{ formatPrice(order.freightAmount) }}</text>
        </view>
        <view v-if="order.couponAmount > 0" class="amount-row">
          <text class="amount-label">优惠</text>
          <text class="amount-value discount">-¥{{ formatPrice(order.couponAmount) }}</text>
        </view>
        <view class="amount-row total-row">
          <text class="amount-label">实付金额</text>
          <view class="total-amount-wrap">
            <text class="total-symbol">¥</text>
            <text class="total-amount">{{ formatPrice(order.payAmount) }}</text>
          </view>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view
        v-if="hasActions"
        class="bottom-bar"
        :style="{ paddingBottom: safeBottom + 'px' }"
      >
        <view class="bottom-actions">
          <u-button
            v-if="order.status === OrderStatus.PENDING_PAY"
            type="default"
            shape="circle"
            text="取消订单"
            @click="handleCancel"
          />
          <u-button
            v-if="order.status === OrderStatus.PENDING_PAY"
            type="error"
            shape="circle"
            text="去支付"
            @click="handlePay"
          />
          <u-button
            v-if="order.status === OrderStatus.PENDING_RECEIVE"
            type="error"
            shape="circle"
            text="确认收货"
            @click="handleConfirm"
          />
        </view>
      </view>
    </template>

    <!-- 订单不存在 -->
    <view v-else class="empty-state">
      <u-empty
        mode="data"
        text="订单不存在或加载失败"
        icon-size="180"
        margin-top="200"
      />
      <u-button
        type="default"
        shape="circle"
        text="返回订单列表"
        custom-style="margin-top: 40rpx; width: 280rpx;"
        @click="goOrderList"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showConfirm, showLoading, hideLoading } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import * as orderApi from '@/api/order'
import { OrderStatus, type OrderVO, type AddressVO, type OrderItemVO } from '@/types'

/** 订单 ID（雪花 ID，string） */
const orderId = ref<string>('')
/** 订单详情 */
const order = ref<OrderVO | null>(null)
/** 加载中 */
const loading = ref<boolean>(true)
/** 操作中 */
const acting = ref<boolean>(false)
/** 安全区域底部高度 */
const safeBottom = ref<number>(0)

/** 步骤列表 */
const stepList = [
  { name: '下单' },
  { name: '付款' },
  { name: '发货' },
  { name: '收货' },
  { name: '完成' }
]

/** 当前步骤索引（根据订单状态计算） */
const currentStep = computed<number>(() => {
  if (!order.value) return 0
  const status = order.value.status
  switch (status) {
    case OrderStatus.PENDING_PAY:
      return 0  // 已下单，待付款
    case OrderStatus.PENDING_DELIVER:
      return 1  // 已付款，待发货
    case OrderStatus.PENDING_RECEIVE:
      return 2  // 已发货，待收货
    case OrderStatus.COMPLETED:
      return 4  // 已完成
    case OrderStatus.CANCELLED:
      return 0  // 已取消，回退到下单
    default:
      return 0
  }
})

/** 收货地址（优先用快照） */
const addressInfo = computed<AddressVO | null>(() => {
  if (!order.value) return null
  return order.value.addressSnapshot || null
})

/** 是否有可执行的操作 */
const hasActions = computed<boolean>(() => {
  if (!order.value) return false
  const status = order.value.status
  return status === OrderStatus.PENDING_PAY || status === OrderStatus.PENDING_RECEIVE
})

/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 格式化手机号（脱敏） */
function formatPhone(phone: string): string {
  if (!phone || phone.length < 11) return phone || ''
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

/** 格式化时间 */
function formatTime(time: string): string {
  if (!time) return ''
  // 兼容后端返回的 ISO 字符串或时间戳
  try {
    const d = new Date(time)
    if (isNaN(d.getTime())) return time
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch (e) {
    return time
  }
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

/** 获取状态描述 */
function getStatusDesc(status: number): string {
  const map: Record<number, string> = {
    [OrderStatus.PENDING_PAY]: '请尽快完成支付',
    [OrderStatus.PENDING_DELIVER]: '商家正在备货，请耐心等待',
    [OrderStatus.PENDING_RECEIVE]: '商品已发出，请确认收货',
    [OrderStatus.COMPLETED]: '订单已完成，感谢您的购买',
    [OrderStatus.CANCELLED]: '订单已取消'
  }
  return map[status] || ''
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

/** 读取系统信息 */
function loadSystemInfo() {
  try {
    const sysInfo = uni.getSystemInfoSync()
    // #ifdef MP-WEIXIN
    const winInfo = (uni.getWindowInfo ? uni.getWindowInfo() : null)
    if (winInfo && winInfo.safeAreaInsets) {
      safeBottom.value = winInfo.safeAreaInsets.bottom || 0
    }
    // #endif
    if (safeBottom.value === 0 && sysInfo.safeArea) {
      safeBottom.value = sysInfo.screenHeight - sysInfo.safeArea.bottom
    }
  } catch (e) {
    console.error('读取系统信息失败', e)
  }
}

/** 加载订单详情 */
async function loadOrderDetail() {
  if (!orderId.value) {
    showToast('订单 ID 缺失', 'error')
    loading.value = false
    return
  }
  loading.value = true
  try {
    const id = ensureStringId(orderId.value)
    // 优先尝试普通订单详情接口（含地址快照），失败回退到通用详情
    try {
      order.value = await orderApi.getNormalOrderDetail(id)
    } catch (e) {
      // 回退到通用详情接口
      order.value = await orderApi.getOrderDetail(id)
    }
  } catch (e) {
    console.error('加载订单详情失败', e)
    order.value = null
    showToast('加载订单详情失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 跳转商品详情 */
function goProductDetail(item: OrderItemVO) {
  navigate.to('pages-product/pages/product-detail/product-detail', {
    id: ensureStringId(item.productId)
  })
}

/** 跳转订单列表 */
function goOrderList() {
  navigate.to('pages-order/pages/order-list/order-list')
}

/** 去支付 */
async function handlePay() {
  if (!order.value || acting.value) return
  const ok = await showConfirm(`确认支付订单 ${order.value.orderNo}？`, '支付确认')
  if (!ok) return
  acting.value = true
  showLoading('支付中...')
  try {
    const id = ensureStringId(order.value.id)
    // 根据订单类型选择支付接口（0 普通 / 1 秒杀）
    if (order.value.type === 0) {
      await orderApi.payNormalOrder(id)
    } else {
      await orderApi.payOrder(id)
    }
    hideLoading()
    showToast('支付成功', 'success')
    // 刷新订单详情
    await loadOrderDetail()
  } catch (e) {
    hideLoading()
    console.error('支付失败', e)
    showToast('支付失败', 'error')
  } finally {
    acting.value = false
  }
}

/** 取消订单 */
async function handleCancel() {
  if (!order.value || acting.value) return
  const ok = await showConfirm(`确认取消订单 ${order.value.orderNo}？`, '取消订单')
  if (!ok) return
  acting.value = true
  showLoading('取消中...')
  try {
    const id = ensureStringId(order.value.id)
    if (order.value.type === 0) {
      await orderApi.cancelNormalOrder(id)
    } else {
      await orderApi.cancelOrder(id)
    }
    hideLoading()
    showToast('取消成功', 'success')
    await loadOrderDetail()
  } catch (e) {
    hideLoading()
    console.error('取消订单失败', e)
    showToast('取消失败', 'error')
  } finally {
    acting.value = false
  }
}

/** 确认收货 */
async function handleConfirm() {
  if (!order.value || acting.value) return
  const ok = await showConfirm('确认已收到商品？确认后订单将完成', '确认收货')
  if (!ok) return
  acting.value = true
  showLoading('处理中...')
  try {
    const id = ensureStringId(order.value.id)
    if (order.value.type === 0) {
      await orderApi.confirmNormalOrder(id)
    } else {
      await orderApi.confirmOrder(id)
    }
    hideLoading()
    showToast('确认收货成功', 'success')
    await loadOrderDetail()
  } catch (e) {
    hideLoading()
    console.error('确认收货失败', e)
    showToast('操作失败', 'error')
  } finally {
    acting.value = false
  }
}

/** 页面加载 */
onLoad((options: Record<string, any>) => {
  if (options && options.id) {
    orderId.value = ensureStringId(options.id)
  }
})

/** 页面显示 */
onShow(() => {
  if (!requireAuthAsync()) return
  if (orderId.value) {
    loadOrderDetail()
  }
})

onMounted(() => {
  loadSystemInfo()
})
</script>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 140rpx;
}

/* 加载状态 */
.loading-state {
  padding: 32rpx;
  background-color: #ffffff;
}

/* 通用 section */
.section {
  background-color: #ffffff;
  margin: 16rpx 0;
  padding: 24rpx 32rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16rpx;
    display: flex;
    align-items: center;
    gap: 8rpx;
  }
}

/* 状态 section */
.status-section {
  margin-top: 16rpx;

  .status-header {
    display: flex;
    align-items: baseline;
    gap: 16rpx;
    margin-bottom: 24rpx;

    .status-text {
      font-size: 36rpx;
      font-weight: 600;

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

    .status-desc {
      font-size: 26rpx;
      color: #909399;
    }
  }
}

/* 地址 section */
.address-section {
  .address-card {
    .receiver-row {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-bottom: 8rpx;

      .receiver {
        font-size: 30rpx;
        font-weight: 600;
        color: #303133;
      }

      .phone {
        font-size: 28rpx;
        color: #606266;
      }
    }

    .address-text {
      font-size: 26rpx;
      color: #606266;
      line-height: 1.5;
    }
  }

  .address-empty {
    text-align: center;
    padding: 16rpx 0;
    color: #909399;
    font-size: 26rpx;
  }
}

/* 商品列表 */
.goods-section {
  .goods-list {
    display: flex;
    flex-direction: column;
    gap: 24rpx;
  }

  .goods-item {
    display: flex;
    gap: 20rpx;

    .goods-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8rpx;
      min-width: 0;

      .goods-name {
        font-size: 28rpx;
        color: #303133;
        line-height: 1.4;
      }

      .goods-sku {
        font-size: 24rpx;
        color: #909399;
        background-color: #f5f5f5;
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
        align-self: flex-start;
      }

      .goods-price-row {
        display: flex;
        align-items: baseline;
        gap: 4rpx;
        margin-top: auto;

        .price-symbol {
          font-size: 24rpx;
          color: #FF4D4F;
          font-weight: bold;
        }

        .price-value {
          font-size: 30rpx;
          color: #FF4D4F;
          font-weight: bold;
        }

        .goods-qty {
          font-size: 26rpx;
          color: #909399;
          margin-left: auto;
        }
      }
    }
  }
}

/* 订单信息 */
.info-section {
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 8rpx 0;

    .info-label {
      font-size: 26rpx;
      color: #909399;
      flex-shrink: 0;
      margin-right: 16rpx;
    }

    .info-value {
      font-size: 26rpx;
      color: #303133;
      text-align: right;
      word-break: break-all;
    }

    &.remark-row {
      .info-value {
        max-width: 480rpx;
      }
    }
  }
}

/* 金额明细 */
.amount-section {
  .amount-row {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding: 8rpx 0;

    .amount-label {
      font-size: 28rpx;
      color: #606266;
    }

    .amount-value {
      font-size: 28rpx;
      color: #303133;

      &.discount {
        color: #FF4D4F;
      }
    }

    &.total-row {
      padding-top: 16rpx;
      border-top: 2rpx solid #f0f0f0;
      margin-top: 8rpx;

      .amount-label {
        font-size: 30rpx;
        font-weight: 600;
        color: #303133;
      }

      .total-amount-wrap {
        display: flex;
        align-items: baseline;

        .total-symbol {
          font-size: 26rpx;
          color: #FF4D4F;
          font-weight: bold;
        }

        .total-amount {
          font-size: 36rpx;
          color: #FF4D4F;
          font-weight: bold;
        }
      }
    }
  }
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 100rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 32rpx;
  box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.06);

  .bottom-actions {
    display: flex;
    gap: 16rpx;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
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