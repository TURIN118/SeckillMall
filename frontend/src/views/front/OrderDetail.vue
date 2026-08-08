<template>
  <!-- 订单详情: 左右分栏布局,参考京东+淘宝订单详情设计 -->
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
      <button class="btn-action primary" @click="router.push('/user/orders')">返回订单列表</button>
    </div>

    <!-- 订单详情内容 -->
    <template v-else-if="order">
      <!-- 1. 顶部状态横幅 (占满宽度,参考京东) -->
      <div class="order-status-banner">
        <div class="banner-left">
          <button class="btn-action outline" @click="router.push('/user/orders')">返回订单列表</button>
          <h2 class="status-title" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</h2>
          <div class="order-meta">
            <span class="meta-item">订单号: {{ order.orderNo }}</span>
            <span class="meta-item">下单时间: {{ formatTime(order.createTime) }}</span>
          </div>
          <span v-if="order.status === 'PAID'" class="waiting-hint">商家备货中</span>
        </div>
        <div class="banner-right">
          <!-- 状态相关操作按钮 -->
          <template v-if="order.status === 'UNPAID'">
            <button class="btn-action outline" :disabled="cancelLoading" @click="handleCancel">
              {{ cancelLoading ? '取消中...' : '取消订单' }}
            </button>
            <button class="btn-action primary" :disabled="payLoading" @click="handlePay">
              {{ payLoading ? '支付中...' : '立即支付' }}
            </button>
          </template>
          <template v-else-if="order.status === 'SHIPPED'">
            <button class="btn-action primary" :disabled="confirmLoading" @click="handleConfirm">
              {{ confirmLoading ? '确认中...' : '确认收货' }}
            </button>
          </template>
        </div>
      </div>

      <!-- 2. 进度步骤条 (占满宽度,4步: 下单→支付→发货→完成) -->
      <div class="order-steps">
        <div class="step">
          <div class="step-dot done">&#10003;</div>
          <span class="step-label">下单成功</span>
          <span class="step-time active">{{ formatTimeShort(order.createTime) }}</span>
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

      <!-- 3. 左右分栏内容 -->
      <div class="order-content">
        <!-- 左栏: 商品信息 + 金额汇总 -->
        <div class="order-left">
          <div class="info-card">
            <div class="card-header-bar">
              <h3 class="card-title">商品信息</h3>
              <span class="order-type-tag" :class="order.orderType === 'SECKILL' ? 'seckill' : 'normal'">
                {{ order.orderType === 'SECKILL' ? '秒杀订单' : '普通订单' }}
              </span>
            </div>
            <!-- 表头 -->
            <div class="goods-table-head">
              <div class="col-product">商品</div>
              <div class="col-price">单价</div>
              <div class="col-qty">数量</div>
              <div class="col-subtotal">小计</div>
            </div>
            <!-- 商品行(限制最大高度,超出滚动) -->
            <div class="goods-table-body">
              <div v-for="item in order.items" :key="item.productId" class="goods-table-row">
                <div class="col-product">
                  <div class="goods-img">
                    <img v-if="item.productImage" :src="formatImageUrl(item.productImage)" :alt="item.productName"
                      loading="lazy" />
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="3" width="18" height="18" rx="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <path d="m21 15-5-5L5 21" />
                    </svg>
                  </div>
                  <div class="goods-info">
                    <div class="goods-name">{{ item.productName }}</div>
                    <div v-if="item.skuAttributes" class="goods-sku">{{ item.skuAttributes }}</div>
                  </div>
                </div>
                <div class="col-price">¥{{ formatPrice(item.unitPrice) }}</div>
                <div class="col-qty">{{ item.quantity }}</div>
                <div class="col-subtotal">¥{{ formatPrice(item.unitPrice * item.quantity) }}</div>
              </div>
            </div>
            <!-- 金额汇总 -->
            <div class="goods-summary">
              <div class="summary-line">
                <span>商品总额</span>
                <span>¥{{ formatPrice(order.totalAmount) }}</span>
              </div>
              <div class="summary-line total">
                <span>实付金额</span>
                <span class="total-amount">¥{{ formatPrice(order.totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右栏: 收货地址 + 订单信息 + 物流信息 -->
        <div class="order-right">
          <!-- 收货地址 -->
          <div v-if="order.receiverName" class="info-card">
            <div class="card-header-bar">
              <h3 class="card-title">收货地址</h3>
            </div>
            <div class="address-content">
              <div class="address-name-phone">
                <span class="address-name">{{ order.receiverName }}</span>
                <span class="address-phone">{{ order.receiverPhone }}</span>
              </div>
              <div class="address-detail">{{ fullAddress }}</div>
            </div>
          </div>

          <!-- 订单信息 -->
          <div class="info-card">
            <div class="card-header-bar">
              <h3 class="card-title">订单信息</h3>
            </div>
            <div class="info-row">
              <span class="info-label">订单编号</span>
              <span class="info-value order-no" @click="copyOrderNo">
                {{ order.orderNo }} <span class="copy-hint">复制</span>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">支付方式</span>
              <span class="info-value">{{ formatPayMethod(order.payMethod) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">下单时间</span>
              <span class="info-value">{{ formatTime(order.createTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">支付时间</span>
              <span class="info-value">{{ order.payTime ? formatTime(order.payTime) : '—' }}</span>
            </div>
            <div v-if="order.cancelTime" class="info-row">
              <span class="info-label">取消时间</span>
              <span class="info-value">{{ formatTime(order.cancelTime) }}</span>
            </div>
            <div v-if="order.cancelReason" class="info-row">
              <span class="info-label">取消原因</span>
              <span class="info-value">{{ order.cancelReason }}</span>
            </div>
            <div v-if="order.remark" class="info-row">
              <span class="info-label">备注</span>
              <span class="info-value remark">{{ order.remark }}</span>
            </div>
          </div>

          <!-- 物流信息 (仅在有物流信息时显示) -->
          <div v-if="order.shippingCompany || order.shippingNo" class="info-card">
            <div class="card-header-bar">
              <h3 class="card-title">物流信息</h3>
            </div>
            <div class="info-row">
              <span class="info-label">物流公司</span>
              <span class="info-value">{{ order.shippingCompany }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">快递单号</span>
              <span class="info-value shipping-no" @click="copyShippingNo">
                {{ order.shippingNo }} <span class="copy-hint">复制</span>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">发货时间</span>
              <span class="info-value">{{ formatTime(order.shipTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. 支付方式选择弹窗 (保留原样) -->
      <el-dialog v-model="showPayDialog" title="选择支付方式" width="420px" append-to-body>
        <div class="pay-method-list">
          <div v-for="method in payMethodOptions" :key="method.value" class="pay-method-item"
            :class="{ active: selectedPayMethod === method.value }" @click="selectedPayMethod = method.value">
            <span class="pay-method-icon">{{ method.icon }}</span>
            <span class="pay-method-label">{{ method.label }}</span>
            <span class="pay-method-desc">{{ method.desc }}</span>
          </div>
        </div>
        <template #footer>
          <button class="btn-action outline" @click="showPayDialog = false">取消</button>
          <button class="btn-action primary" :disabled="payLoading" @click="confirmPay">
            {{ payLoading ? '支付中...' : '确认支付' }}
          </button>
        </template>
      </el-dialog>
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
import type { OrderItemSnapshot, NormalOrder, NormalOrderDetailVO, SeckillOrder } from '@/types'

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
  shippingCompany?: string
  shippingNo?: string
  cancelTime?: string
  cancelReason?: string
  /** 订单备注（后端 NormalOrder 实体 remark 字段，秒杀订单无此字段） */
  remark?: string
  items: OrderItemSnapshot[]
  /** 收货地址-收件人 */
  receiverName?: string
  /** 收货地址-手机号 */
  receiverPhone?: string
  /** 收货地址-省 */
  province?: string
  /** 收货地址-市 */
  city?: string
  /** 收货地址-区 */
  district?: string
  /** 收货地址-详细地址 */
  detailAddress?: string
}

/** 支付方式中文映射 */
const PAY_METHOD_MAP: Record<string, string> = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  WALLET: '钱包支付',
  BANK_CARD: '银行卡'
}

/** 格式化支付方式为中文 */
function formatPayMethod(payMethod: string): string {
  if (!payMethod) return '—'
  return PAY_METHOD_MAP[payMethod] || payMethod
}

const route = useRoute()
const router = useRouter()

const loading = ref<boolean>(false)
const error = ref<boolean>(false)
const order = ref<UnifiedOrderDetail | null>(null)
const payLoading = ref<boolean>(false)
const cancelLoading = ref<boolean>(false)
const confirmLoading = ref<boolean>(false)

/** 支付方式选择弹窗显示 */
const showPayDialog = ref<boolean>(false)

/** 选中的支付方式 */
const selectedPayMethod = ref<string>('ALIPAY')

/** 支付方式选项列表 */
const payMethodOptions = [
  { value: 'ALIPAY', label: '支付宝', icon: '💰', desc: '模拟支付' },
  { value: 'WECHAT', label: '微信支付', icon: '💬', desc: '模拟支付' },
  { value: 'WALLET', label: '钱包支付', icon: '👛', desc: '余额扣款' }
]

/** 完整收货地址文本 */
const fullAddress = computed<string>(() => {
  if (!order.value) return ''
  const o = order.value
  if (!o.receiverName) return ''
  return `${o.province || ''}${o.city || ''}${o.district || ''}${o.detailAddress || ''}`
})

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

/** 构建普通订单的统一详情对象
 *  M-F2 修复: 用已有强类型 NormalOrderDetailVO 替代 any, 保证类型安全
 */
function buildNormalOrder(detail: NormalOrderDetailVO): UnifiedOrderDetail {
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
    shipTime: detail.order.shipTime,
    shippingCompany: detail.order.shippingCompany,
    shippingNo: detail.order.shippingNo,
    cancelTime: detail.order.cancelTime,
    cancelReason: detail.order.cancelReason,
    // NormalOrder 类型未声明 remark, 但后端实体可能返回, 用类型断言安全读取
    remark: (detail.order as NormalOrder & { remark?: string }).remark,
    items: (detail.items || []).map(item => ({
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage,
      unitPrice: item.unitPrice,
      quantity: item.quantity,
      // 7.3 订单展示 SKU 信息（NormalOrderItem 实体新增字段自动序列化）
      skuId: item.skuId ?? null,
      skuAttributes: item.skuAttributes ?? null
    })),
    // 收货地址字段透传
    receiverName: detail.receiverName,
    receiverPhone: detail.receiverPhone,
    province: detail.province,
    city: detail.city,
    district: detail.district,
    detailAddress: detail.detailAddress
  }
}

/** 构建秒杀订单的统一详情对象（额外查询商品信息）
 *  M-F2 修复: 用已有强类型 SeckillOrder 替代 any, 保证类型安全
 */
async function buildSeckillOrder(seckill: SeckillOrder): Promise<UnifiedOrderDetail> {
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
    shipTime: seckill.shipTime,
    shippingCompany: seckill.shippingCompany,
    shippingNo: seckill.shippingNo,
    cancelTime: seckill.cancelTime,
    cancelReason: seckill.cancelReason,
    items: [{
      productId: seckill.productId,
      productName,
      productImage,
      unitPrice: seckill.seckillPrice,
      quantity: seckill.quantity
    }],
    // 秒杀订单无收货地址
    receiverName: undefined,
    receiverPhone: undefined,
    province: undefined,
    city: undefined,
    district: undefined,
    detailAddress: undefined
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
function formatTime(time: string | undefined | null): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

function formatTimeShort(time: string | undefined | null): string {
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

/** 复制快递单号 */
async function copyShippingNo(): Promise<void> {
  if (!order.value?.shippingNo) return
  try {
    await navigator.clipboard.writeText(order.value.shippingNo)
    ElMessage.success('快递单号已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

/** 去支付: 打开支付方式选择弹窗 (Bug 7 修复: 不直接扣费, 让用户选择支付方式) */
async function handlePay(): Promise<void> {
  if (!order.value) return
  // 重置选中支付方式为默认值, 打开弹窗
  selectedPayMethod.value = 'ALIPAY'
  showPayDialog.value = true
}

/** 确认支付: 根据选中的支付方式调用对应支付接口 */
async function confirmPay(): Promise<void> {
  if (!order.value) return
  payLoading.value = true
  try {
    if (order.value.orderType === 'NORMAL') {
      await payNormalOrder(order.value.id, selectedPayMethod.value)
    } else {
      await payOrder(order.value.id, selectedPayMethod.value)
    }
    ElMessage.success('支付成功')
    showPayDialog.value = false
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
/* === 页面容器 === */
.order-page {
  padding: 24px;
}

/* === 加载骨架屏 === */
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

/* === 错误状态 === */
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

/* === 顶部状态横幅 (占满宽度,参考京东) === */
.order-status-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}

.banner-left {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text-primary);
}

.status-title.unpaid {
  color: #ff9800;
}

.status-title.paid {
  color: #4caf50;
}

.status-title.shipped {
  color: #1677ff;
}

.status-title.completed {
  color: #1677ff;
}

.status-title.cancelled,
.status-title.timeout {
  color: var(--color-text-muted);
}

.order-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  flex-wrap: wrap;
}

.meta-item {
  white-space: nowrap;
}

.banner-right {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-shrink: 0;
}

/* === 等待发货提示 === */
.waiting-hint {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 8px 16px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
}

/* === 操作按钮 === */
.btn-action {
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  letter-spacing: 0.02em;
}

.btn-action.primary {
  background: var(--color-primary);
  color: #fff;
  border: none;
}

.btn-action.primary:hover:not(:disabled) {
  background: var(--btn-hover);
}

.btn-action.primary:disabled {
  background: var(--btn-loading-bg);
  cursor: not-allowed;
}

.btn-action.outline {
  background: #fff;
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.btn-action.outline:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.btn-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* === 进度步骤条 (占满宽度,美化, 32px圆点) === */
.order-steps {
  display: flex;
  align-items: flex-start;
  padding: 24px 20px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 8px;
  z-index: 1;
}

.step-dot.done {
  background: #4caf50;
  color: #fff;
}

.step-dot.current {
  background: var(--color-primary);
  color: #fff;
}

.step-dot.pending {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 2px solid var(--color-border);
}

.step-label {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.step-time {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.step-time.active {
  color: var(--color-primary);
}

.step-line {
  position: absolute;
  top: 16px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--color-border);
  z-index: 0;
}

.step-line.done {
  background: #4caf50;
}

/* === 左右分栏布局 === */
.order-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.order-left {
  flex: 1.4;
  min-width: 0;
}

.order-right {
  flex: 1;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* === 信息卡片通用 === */
.info-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

/* 右栏卡片间距由 gap 控制 */
.order-right .info-card {
  margin-bottom: 0;
}

.card-header-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.order-type-tag {
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
}

.order-type-tag.seckill {
  background: var(--color-primary);
  color: #fff;
}

.order-type-tag.normal {
  background: #1677ff;
  color: #fff;
}

/* === 商品表格 === */
.goods-table-head {
  display: grid;
  grid-template-columns: 1fr 100px 70px 100px;
  padding: 12px 20px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
}

/* 商品行容器 - 限制最大高度,超出可滚动 */
.goods-table-body {
  max-height: 400px;
  overflow-y: auto;
}

.goods-table-row {
  display: grid;
  grid-template-columns: 1fr 100px 70px 100px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-bg-subtle);
  align-items: center;
}

.goods-table-row:last-child {
  border-bottom: none;
}

.col-product {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.goods-img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  background: var(--color-bg-subtle);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.goods-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-img svg {
  width: 28px;
  height: 28px;
}

.goods-info {
  min-width: 0;
  flex: 1;
}

.goods-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-sku {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
  padding: 2px 8px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  display: inline-block;
}

.col-price,
.col-qty,
.col-subtotal {
  font-size: 14px;
  color: var(--color-text-primary);
  text-align: center;
}

.col-subtotal {
  color: var(--color-primary);
  font-weight: 600;
}

/* === 金额汇总 === */
.goods-summary {
  padding: 16px 20px;
  background: var(--color-bg-subtle);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.summary-line {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.summary-line.total {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.total-amount {
  color: var(--color-primary);
  font-size: 20px;
  font-family: var(--font-price);
  font-weight: 800;
}

/* === 收货地址 === */
.address-content {
  padding: 16px 20px;
}

.address-name-phone {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  align-items: baseline;
}

.address-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.address-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.address-detail {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

/* === 订单信息行 === */
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 20px;
  font-size: 13px;
  border-bottom: 1px solid var(--color-bg-subtle);
  gap: 12px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.info-value {
  color: var(--color-text-primary);
  text-align: right;
  min-width: 0;
}

.info-value.order-no,
.info-value.shipping-no {
  cursor: pointer;
  font-family: var(--font-mono);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.copy-hint {
  font-size: 11px;
  color: var(--color-text-secondary);
  font-family: var(--font-family);
}

.info-value.order-no:hover .copy-hint,
.info-value.shipping-no:hover .copy-hint {
  color: var(--color-primary);
}

.info-value.remark {
  word-break: break-all;
  max-width: 70%;
}

/* === 支付方式选择弹窗 (保留原样) === */
.pay-method-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pay-method-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.pay-method-item:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.pay-method-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.pay-method-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.pay-method-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  flex: 1;
}

.pay-method-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* === 响应式 === */
@media (max-width: 900px) {

  /* 中等屏幕: 左右分栏改为上下堆叠 */
  .order-content {
    flex-direction: column;
  }

  .order-right {
    max-width: none;
  }
}

@media (max-width: 768px) {
  .order-page {
    padding: 16px;
  }

  .order-status-banner {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
    padding: 16px;
  }

  .banner-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .order-steps {
    padding: 20px 12px;
  }

  .step-label {
    font-size: 12px;
  }

  .step-time {
    font-size: 10px;
  }

  .goods-table-head {
    display: none;
  }

  .goods-table-row {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 16px;
  }

  .col-price,
  .col-qty,
  .col-subtotal {
    text-align: left;
  }

  .col-price::before {
    content: "单价: ";
    color: var(--color-text-secondary);
  }

  .col-qty::before {
    content: "数量: ";
    color: var(--color-text-secondary);
  }

  .col-subtotal::before {
    content: "小计: ";
    color: var(--color-text-secondary);
  }

  .goods-summary {
    align-items: stretch;
  }

  .summary-line {
    justify-content: space-between;
  }
}
</style>
