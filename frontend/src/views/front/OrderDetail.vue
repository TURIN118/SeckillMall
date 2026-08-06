<template>
  <!-- 严格对照 index.html .order-page / .order-steps / .pay-summary 样式 -->
  <div class="order-page">
    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-wrap">
      <div v-for="i in 6" :key="i" class="skeleton-line"></div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">!</div>
      <h3 class="error-title">订单不存在</h3>
      <p class="error-desc">您访问的订单可能已被删除</p>
      <button class="btn-sm primary" @click="router.push('/user/orders')">返回订单列表</button>
    </div>

    <!-- 订单详情内容 -->
    <template v-else-if="order">
      <!-- 面包屑 -->
      <nav class="breadcrumb">
        <router-link to="/user/orders" class="breadcrumb-link">我的订单</router-link>
        <span class="breadcrumb-sep">&gt;</span>
        <span class="breadcrumb-current">订单详情</span>
      </nav>

      <!-- 进度步骤条（4步：下单→支付→发货→完成） -->
      <div class="order-steps">
        <div class="step">
          <div class="step-dot done">&#10003;</div>
          <span class="step-label">下单成功</span>
          <span class="step-time">{{ formatTimeShort(order.createTime) }}</span>
          <div class="step-line done"></div>
        </div>
        <div class="step">
          <div class="step-dot" :class="step2DotClass">{{ step2DotContent }}</div>
          <span class="step-label">{{ step2Label }}</span>
          <span class="step-time" :class="{ active: order.status === 'UNPAID' }">{{ step2Time }}</span>
          <div class="step-line" :class="{ done: step2LineDone }"></div>
        </div>
        <div class="step">
          <div class="step-dot" :class="step3DotClass">{{ step3DotContent }}</div>
          <span class="step-label">{{ step3Label }}</span>
          <span class="step-time">{{ step3Time }}</span>
          <div class="step-line" :class="{ done: step3LineDone }"></div>
        </div>
        <div class="step">
          <div class="step-dot" :class="step4DotClass">{{ step4DotContent }}</div>
          <span class="step-label">{{ step4Label }}</span>
        </div>
      </div>

      <!-- 商品信息卡片（对照 .order-card，支持多商品遍历） -->
      <div class="order-card product-card" v-for="item in order.items" :key="item.productId">
        <div class="order-card-img large">
          <img v-if="item.productImage" :src="formatImageUrl(item.productImage)" :alt="item.productName"
            class="order-card-img-tag" loading="lazy" />
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="m21 15-5-5L5 21" />
          </svg>
        </div>
        <div class="order-card-info">
          <div class="order-card-name large">{{ item.productName }}</div>
          <div class="order-card-time">单价 ¥{{ formatPrice(item.unitPrice) }} | 数量 {{ item.quantity }} 件</div>
          <div class="product-meta">
            <span>单价：<strong class="seckill-price">¥{{ formatPrice(item.unitPrice) }}</strong></span>
            <span>数量：{{ item.quantity }} 件</span>
          </div>
        </div>
      </div>

      <!-- 订单信息汇总（对照 .pay-summary） -->
      <div class="pay-summary">
        <div class="pay-summary-row">
          <span>订单编号</span>
          <span class="order-no-value" @click="copyOrderNo">
            {{ order.orderNo }}
            <span class="copy-hint">复制</span>
          </span>
        </div>
        <div class="pay-summary-row">
          <span>下单时间</span>
          <span>{{ formatTime(order.createTime) }}</span>
        </div>
        <div class="pay-summary-row">
          <span>支付方式</span>
          <span>{{ order.payMethod || '—' }}</span>
        </div>
        <div class="pay-summary-row">
          <span>支付时间</span>
          <span>{{ order.payTime ? formatTime(order.payTime) : '—' }}</span>
        </div>
        <div class="pay-summary-row">
          <span>订单状态</span>
          <span class="status-tag" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
        </div>
        <div v-if="order.cancelTime" class="pay-summary-row">
          <span>取消时间</span>
          <span>{{ formatTime(order.cancelTime) }}</span>
        </div>
        <div v-if="order.cancelReason" class="pay-summary-row">
          <span>取消原因</span>
          <span>{{ order.cancelReason }}</span>
        </div>
        <div class="pay-summary-row total-row">
          <span class="total-label">应付金额</span>
          <span class="pay-summary-total">¥{{ formatPrice(order.totalAmount) }}</span>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <template v-if="order.status === 'UNPAID'">
          <button class="btn-sm text large" :disabled="cancelLoading" @click="handleCancel">
            {{ cancelLoading ? '取消中...' : '取消订单' }}
          </button>
          <button class="btn-sm primary large" :disabled="payLoading" @click="handlePay">
            {{ payLoading ? '支付中...' : '去支付' }}
          </button>
        </template>
        <template v-else-if="order.status === 'PAID'">
          <span class="waiting-hint">等待发货</span>
        </template>
        <template v-else-if="order.status === 'SHIPPED'">
          <button class="btn-sm primary large" :disabled="confirmLoading" @click="handleConfirm">
            {{ confirmLoading ? '确认中...' : '确认收货' }}
          </button>
        </template>
        <button class="btn-sm large" @click="router.push('/user/orders')">返回订单列表</button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * P08 订单详情
 * 严格对照 index.html .order-steps / .pay-summary 样式
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, getNormalOrderDetail, payOrder, payNormalOrder, cancelOrder, cancelNormalOrder, confirmOrder, confirmNormalOrder } from '@/api/order'
import { getProductDetail } from '@/api/product'
import { formatImageUrl } from '@/utils/image'
import dayjs from 'dayjs'
import type { OrderItemSnapshot } from '@/types'

/** 统一订单详情（适配秒杀+普通两种接口返回） */
interface UnifiedOrderDetail {
  id: number | string
  orderNo: string
  /** 订单类型：SECKILL-秒杀 / NORMAL-普通 */
  orderType: 'SECKILL' | 'NORMAL'
  status: string
  totalAmount: number
  payMethod: string
  createTime: string
  payTime: string
  payExpireTime?: string
  shipTime?: string
  cancelTime?: string
  cancelReason?: string
  items: OrderItemSnapshot[]
}

const route = useRoute()
const router = useRouter()

const loading = ref<boolean>(false)
const error = ref<boolean>(false)
const order = ref<UnifiedOrderDetail | null>(null)
const payLoading = ref<boolean>(false)
const cancelLoading = ref<boolean>(false)
const confirmLoading = ref<boolean>(false)

/** 步骤 2 状态（支付完成） */
const step2DotClass = computed<string>(() => {
  if (!order.value) return 'pending'
  const s = order.value.status
  if (s === 'UNPAID') return 'current'
  if (s === 'PAID' || s === 'SHIPPED' || s === 'COMPLETED') return 'done'
  return 'pending'
})

const step2DotContent = computed<string>(() => {
  if (!order.value) return '2'
  const s = order.value.status
  if (s === 'PAID' || s === 'SHIPPED' || s === 'COMPLETED') return '✓'
  return '2'
})

const step2Label = computed<string>(() => {
  if (!order.value) return '等待支付'
  return order.value.status === 'UNPAID' ? '等待支付' : '支付完成'
})

const step2Time = computed<string>(() => {
  if (!order.value) return ''
  if (order.value.status === 'UNPAID' && order.value.payExpireTime) {
    const diff = dayjs(order.value.payExpireTime).diff(dayjs(), 'second')
    if (diff <= 0) return '已超时'
    const m = Math.floor(diff / 60)
    const s = diff % 60
    return `剩余 ${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  if (order.value.payTime) return formatTimeShort(order.value.payTime)
  return '—'
})

const step2LineDone = computed<boolean>(() => {
  if (!order.value) return false
  const s = order.value.status
  return s === 'PAID' || s === 'SHIPPED' || s === 'COMPLETED'
})

/** 步骤 3 状态（已发货） */
const step3DotClass = computed<string>(() => {
  if (!order.value) return 'pending'
  const s = order.value.status
  if (s === 'SHIPPED' || s === 'COMPLETED') return 'done'
  if (s === 'PAID') return 'current'
  return 'pending'
})

const step3DotContent = computed<string>(() => {
  if (!order.value) return '3'
  const s = order.value.status
  if (s === 'SHIPPED' || s === 'COMPLETED') return '✓'
  return '3'
})

const step3Label = computed<string>(() => {
  if (!order.value) return '待发货'
  const s = order.value.status
  if (s === 'SHIPPED') return '已发货'
  if (s === 'COMPLETED') return '已发货'
  if (s === 'PAID') return '待发货'
  return '待发货'
})

const step3Time = computed<string>(() => {
  if (!order.value) return ''
  const s = order.value.status
  if (s === 'SHIPPED' || s === 'COMPLETED') {
    // 如果有发货时间可以展示，暂用短横线
    return order.value.shipTime ? formatTimeShort(order.value.shipTime) : '—'
  }
  return ''
})

const step3LineDone = computed<boolean>(() => {
  if (!order.value) return false
  return order.value.status === 'COMPLETED'
})

/** 步骤 4 状态（订单完成） */
const step4DotClass = computed<string>(() => {
  if (!order.value) return 'pending'
  return order.value.status === 'COMPLETED' ? 'done' : 'pending'
})

const step4DotContent = computed<string>(() => {
  if (!order.value) return '4'
  return order.value.status === 'COMPLETED' ? '✓' : '4'
})

const step4Label = computed<string>(() => {
  if (!order.value) return '订单完成'
  return order.value.status === 'COMPLETED' ? '订单完成' : '待完成'
})

/** 状态标签 class 映射 */
function statusClass(status: string): string {
  const map: Record<string, string> = {
    UNPAID: 'unpaid',
    PAID: 'paid',
    SHIPPED: 'shipped',
    CANCELLED: 'cancelled',
    TIMEOUT: 'timeout',
    COMPLETED: 'completed'
  }
  return map[status] || 'cancelled'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    UNPAID: '待支付',
    PAID: '已支付',
    SHIPPED: '已发货',
    CANCELLED: '已取消',
    TIMEOUT: '已超时',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

/** 获取订单ID（使用字符串保留雪花算法ID精度，避免Number超出MAX_SAFE_INTEGER） */
function getOrderId(): string {
  return String(route.params.id)
}

/** 获取订单类型 (从 query 参数，可能为空表示未知) */
function getOrderType(): 'SECKILL' | 'NORMAL' | null {
  if (route.query.type === 'NORMAL') return 'NORMAL'
  if (route.query.type === 'SECKILL') return 'SECKILL'
  return null
}

/** 构建普通订单的统一详情对象 */
function buildNormalOrder(detail: { order: any; items: any[] }): UnifiedOrderDetail {
  return {
    id: detail.order.id,
    orderNo: detail.order.orderNo,
    orderType: 'NORMAL',
    status: detail.order.status,
    totalAmount: detail.order.totalAmount,
    payMethod: detail.order.payMethod || '',
    createTime: detail.order.createTime,
    payTime: detail.order.payTime || '',
    payExpireTime: detail.order.payExpireTime,
    cancelTime: detail.order.cancelTime,
    cancelReason: detail.order.cancelReason,
    items: (detail.items || []).map(item => ({
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage,
      unitPrice: item.unitPrice,
      quantity: item.quantity
    }))
  }
}

/** 构建秒杀订单的统一详情对象（额外查询商品信息） */
async function buildSeckillOrder(seckill: any): Promise<UnifiedOrderDetail> {
  // 查询商品详情获取真实的商品名称和图片
  let productName = `秒杀商品 #${seckill.productId}`
  let productImage = ''
  try {
    const productRes = await getProductDetail(seckill.productId)
    const product = productRes.data
    if (product) {
      productName = product.productName || productName
      productImage = (product.images && product.images.length > 0) ? product.images[0] : productImage
    }
  } catch {
    // 商品信息查询失败时保留默认值，不影响订单展示
  }

  return {
    id: seckill.id,
    orderNo: seckill.orderNo,
    orderType: 'SECKILL',
    status: seckill.status,
    totalAmount: seckill.totalAmount,
    payMethod: seckill.payMethod || '',
    createTime: seckill.createTime,
    payTime: seckill.payTime || '',
    payExpireTime: seckill.payExpireTime,
    cancelTime: seckill.cancelTime,
    cancelReason: seckill.cancelReason,
    items: [{
      productId: seckill.productId,
      productName,
      productImage,
      unitPrice: seckill.seckillPrice,
      quantity: seckill.quantity
    }]
  }
}

/** 拉取订单详情 (根据订单类型调用不同接口，无type参数时自动fallback) */
async function fetchOrderDetail(): Promise<void> {
  const id = getOrderId()
  if (!id) {
    error.value = true
    return
  }
  loading.value = true
  error.value = false
  try {
    const orderType = getOrderType()

    if (orderType === 'NORMAL') {
      // 明确指定为普通订单：直接调普通订单接口
      const res = await getNormalOrderDetail(id)
      order.value = buildNormalOrder(res.data)
    } else if (orderType === 'SECKILL') {
      // 明确指定为秒杀订单：直接调秒杀订单接口
      const res = await getOrderDetail(id)
      order.value = await buildSeckillOrder(res.data)
    } else {
      // 无type参数：先尝试秒杀接口，失败后fallback到普通订单接口
      try {
        const res = await getOrderDetail(id)
        order.value = await buildSeckillOrder(res.data)
      } catch {
        // 秒杀接口查询失败，尝试普通订单接口
        const res = await getNormalOrderDetail(id)
        order.value = buildNormalOrder(res.data)
      }
    }
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

/** 格式化时间 */
function formatTime(time: string): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

function formatTimeShort(time: string): string {
  if (!time) return '-'
  return dayjs(time).format('HH:mm:ss')
}

/** 格式化价格 */
function formatPrice(price: number): string {
  return Number(price || 0).toFixed(2)
}

/** 复制订单号 */
async function copyOrderNo(): Promise<void> {
  if (!order.value) return
  try {
    await navigator.clipboard.writeText(order.value.orderNo)
    ElMessage.success('订单号已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

/** 立即支付 (根据订单类型调用不同支付接口) */
async function handlePay(): Promise<void> {
  if (!order.value) return
  payLoading.value = true
  try {
    if (order.value.orderType === 'NORMAL') {
      await payNormalOrder(order.value.id, 'ALIPAY')
    } else {
      await payOrder(order.value.id, 'ALIPAY')
    }
    ElMessage.success('支付成功')
    await fetchOrderDetail()
  } catch {
    // 错误已由拦截器处理
  } finally {
    payLoading.value = false
  }
}

/** 取消订单 */
async function handleCancel(): Promise<void> {
  if (!order.value) return
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
    cancelLoading.value = true
    if (order.value.orderType === 'NORMAL') {
      await cancelNormalOrder(order.value.id)
    } else {
      await cancelOrder(order.value.id)
    }
    ElMessage.success('订单已取消')
    await fetchOrderDetail()
  } catch {
    // 取消操作
  } finally {
    cancelLoading.value = false
  }
}

/** 确认收货 */
async function handleConfirm(): Promise<void> {
  if (!order.value) return
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', {
      type: 'info',
      confirmButtonText: '确认收货',
      cancelButtonText: '再想想'
    })
    confirmLoading.value = true
    if (order.value.orderType === 'NORMAL') {
      await confirmNormalOrder(order.value.id)
    } else {
      await confirmOrder(order.value.id)
    }
    ElMessage.success('已确认收货')
    await fetchOrderDetail()
  } catch {
    // 取消操作或请求错误
  } finally {
    confirmLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'OrderDetail') {
      fetchOrderDetail()
    }
  }
)

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
/* 严格对照 index.html .order-page 样式 */
.order-page {
  padding: 24px;
}

.loading-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 20px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
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

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px;
  text-align: center;
}

.error-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--tag-timeout-bg);
  color: var(--color-danger);
  font-size: 36px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.error-title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 8px;
}

.error-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

/* 面包屑 */
.breadcrumb {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.breadcrumb-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s;
}
.breadcrumb-link:hover {
  color: var(--color-primary);
}
.breadcrumb-sep {
  color: var(--color-text-muted);
}
.breadcrumb-current {
  color: var(--color-text-primary);
  font-weight: 600;
}

/* 进度步骤条：对照 .order-steps 样式 */
.order-steps {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

/* step-dot: done 绿 / current 红 / pending 灰 */
.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
  z-index: 1;
}

.step-dot.done {
  background: var(--color-success);
  color: #fff;
}

.step-dot.current {
  background: var(--color-primary);
  color: #fff;
}

.step-dot.pending {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
}

.step-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.step-time {
  font-size: 10px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.step-time.active {
  color: var(--color-primary);
}

.step-line {
  position: absolute;
  top: 14px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--btn-disabled-bg);
  z-index: 0;
}

.step-line.done {
  background: var(--color-success);
}

/* 商品信息卡片：对照 .order-card 样式 */
.order-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.order-card-img {
  width: 72px;
  height: 72px;
  background: var(--color-bg-subtle);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-text-muted);
}

.order-card-img.large {
  width: 96px;
  height: 96px;
}

.order-card-img svg {
  width: 28px;
  height: 28px;
  color: var(--color-text-muted);
}

.order-card-img.large svg {
  width: 36px;
  height: 36px;
}

.order-card-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.order-card-info {
  flex: 1;
  min-width: 0;
}

.order-card-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--color-text-primary);
}

.order-card-name.large {
  font-size: 15px;
}

.order-card-time {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.product-meta {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.seckill-price {
  color: var(--color-primary);
  font-family: var(--font-price);
  font-size: 16px;
}

/* 订单信息汇总：对照 .pay-summary 样式 */
.pay-summary {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.pay-summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--color-bg-subtle);
}

.pay-summary-row:last-child {
  border-bottom: none;
}

.pay-summary-row span:first-child {
  color: var(--color-text-secondary);
}

.pay-summary-row .order-no-value {
  font-family: var(--font-mono);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.copy-hint {
  font-size: 11px;
  color: var(--color-text-secondary);
  font-family: var(--font-family);
}

.order-no-value:hover .copy-hint {
  color: var(--color-primary);
}

.total-row {
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
  margin-top: 4px;
}

.total-label {
  font-weight: 700;
  color: var(--color-text-primary) !important;
}

.pay-summary-total {
  font-family: var(--font-price);
  font-size: 16px;
  font-weight: 800;
  color: var(--color-primary);
}

/* 状态标签 */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-tag.unpaid {
  background: var(--tag-unpaid-bg);
  color: var(--tag-unpaid-fg);
}

.status-tag.paid {
  background: var(--tag-paid-bg);
  color: var(--tag-paid-fg);
}

.status-tag.cancelled {
  background: var(--tag-cancelled-bg);
  color: var(--tag-cancelled-fg);
}

.status-tag.timeout {
  background: var(--tag-timeout-bg);
  color: var(--tag-timeout-fg);
}

.status-tag.completed {
  background: var(--tag-completed-bg);
  color: var(--tag-completed-fg);
}

.status-tag.shipped {
  background: #e6f4ff;
  color: #1677ff;
}

/* 操作栏 */
.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  align-items: center;
}

/* 等待发货提示 */
.waiting-hint {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 8px 16px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
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

.btn-sm.large {
  padding: 8px 28px;
  font-size: 13px;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
}

.btn-sm.primary:disabled {
  background: var(--btn-loading-bg);
  cursor: not-allowed;
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
}

.btn-sm.text:hover {
  color: var(--color-primary);
}

.btn-sm.text:disabled {
  color: var(--color-text-muted);
  cursor: not-allowed;
}
</style>
