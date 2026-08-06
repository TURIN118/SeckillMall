<template>
  <!-- 严格对照 index.html .order-page / .order-tabs / .order-card 样式 -->
  <div class="order-page">
    <!-- 标签页 -->
    <div class="order-tabs">
      <div v-for="tab in tabs" :key="tab.name" class="order-tab" :class="{ active: activeTab === tab.name }"
        @click="handleTabChange(tab.name)">{{ tab.label }}</div>
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="skeleton-list">
      <div v-for="i in 3" :key="i" class="skeleton-item"></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="orderList.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <path d="m21 15-5-5L5 21" />
        </svg>
      </div>
      <p class="empty-text">还没有订单，快去秒杀好物吧！</p>
      <button class="btn-sm primary" @click="router.push('/')">去逛逛</button>
    </div>

    <!-- 订单列表 -->
    <div v-else>
      <div v-for="order in orderList" :key="order.id" class="order-card"
        :class="{ 'order-disabled': isDisabledStatus(order.status) }" @click="goDetail(order)">
        <!-- 商品图：显示第一个商品图片，无图则显示 SVG 占位 -->
        <div class="order-card-img">
          <img v-if="order.items && order.items.length > 0 && order.items[0].productImage"
            :src="formatImageUrl(order.items[0].productImage)" :alt="order.items[0].productName"
            class="order-card-img-tag" loading="lazy" />
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="m21 15-5-5L5 21" />
          </svg>
        </div>

        <!-- 信息 -->
        <div class="order-card-info">
          <div class="order-card-name">
            {{ order.items && order.items.length > 0 ? order.items[0].productName : '—' }}
            <span v-if="order.items && order.items.length > 1" class="order-item-count">等{{ order.items.length
            }}件商品</span>
          </div>
          <div class="order-card-time">
            下单时间：{{ formatTime(order.createTime) }} &nbsp;|&nbsp; 订单号：{{ order.orderNo }}
          </div>
          <div class="order-card-status">
            <span class="order-type-tag" :class="order.orderType === 'SECKILL' ? 'seckill' : 'normal'">
              {{ order.orderType === 'SECKILL' ? '秒杀订单' : '普通订单' }}
            </span>
            <span class="status-tag" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
          </div>
        </div>

        <!-- 右侧金额和操作 -->
        <div class="order-card-right">
          <div class="order-card-price">{{ formatPrice(order.totalAmount) }}</div>
          <div class="order-card-actions">
            <template v-if="order.status === 'UNPAID'">
              <button class="btn-sm text" @click.stop="handleCancel(order)">取消订单</button>
              <button class="btn-sm primary" @click.stop="goPay(order)">去支付</button>
            </template>
            <template v-else-if="order.status === 'SHIPPED'">
              <button class="btn-sm primary" :disabled="confirmLoadingId === order.id" @click.stop="handleConfirm(order)">
                {{ confirmLoadingId === order.id ? '确认中...' : '确认收货' }}
              </button>
              <button class="btn-sm" @click.stop="goDetail(order)">查看订单</button>
            </template>
            <button v-else class="btn-sm" @click.stop="goDetail(order)">查看订单</button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <PaginationWrapper v-if="total > 0" :total="total" :page-num="pageNum" :page-size="pageSize"
        :page-sizes="[10, 20, 50]" @change="handlePageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P07 我的订单
 * 严格对照 index.html .order-page / .order-tabs / .order-card 样式
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUnifiedOrderList, cancelOrder, cancelNormalOrder, confirmOrder, confirmNormalOrder } from '@/api/order'

import PaginationWrapper from '@/components/PaginationWrapper.vue'
import { formatImageUrl } from '@/utils/image'
import dayjs from 'dayjs'
import type { OrderListItemVO } from '@/types'

const router = useRouter()

const loading = ref<boolean>(false)
const orderList = ref<OrderListItemVO[]>([])
const total = ref<number>(0)
const activeTab = ref<string>('all')
const pageNum = ref<number>(1)
const pageSize = ref<number>(10)
const confirmLoadingId = ref<number | string | null>(null)

const tabs = [
  { label: '全部', name: 'all' },
  { label: '待支付', name: 'UNPAID' },
  { label: '已支付', name: 'PAID' },
  { label: '已发货', name: 'SHIPPED' },
  { label: '已取消', name: 'CANCELLED' },
  { label: '已完成', name: 'COMPLETED' }
]

/** 状态标签 class 映射：unpaid 橙 / paid 绿 / shipped 蓝 / cancelled 灰 / timeout 红 / completed 蓝 */
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

/** 是否为禁用状态 (取消/超时) */
function isDisabledStatus(status: string): boolean {
  return status === 'CANCELLED' || status === 'TIMEOUT'
}

/** 拉取订单列表 */
async function fetchOrders(): Promise<void> {
  loading.value = true
  try {
    const res = await getUnifiedOrderList({
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orderList.value = res.data.list || []
    total.value = res.data.total
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

/** 标签切换 */
function handleTabChange(name: string): void {
  activeTab.value = name
  pageNum.value = 1
  fetchOrders()
}

/** 分页变化 */
function handlePageChange(payload: { pageNum: number; pageSize: number }): void {
  pageNum.value = payload.pageNum
  pageSize.value = payload.pageSize
  fetchOrders()
}

/** 跳转订单详情/去支付 */
function goPay(order: OrderListItemVO): void {
  router.push(`/user/orders/${order.id}?type=${order.orderType}`)
}

function goDetail(order: OrderListItemVO): void {
  // M4 修复: 秒杀订单跳转到普通商品详情页（场次化重构后不再有独立秒杀详情页），
  // 通过订单快照 items[0].productId 获取商品 ID
  if (order.orderType === 'SECKILL' && order.items && order.items.length > 0) {
    router.push(`/products/${order.items[0].productId}`)
  } else {
    router.push(`/user/orders/${order.id}?type=${order.orderType}`)
  }
}

/** 取消订单：根据 orderType 调用不同取消接口（BUG-002 修复） */
async function handleCancel(order: OrderListItemVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
    // 普通订单走 cancel-normal 接口（操作 t_normal_order），秒杀订单走原 cancel 接口
    if (order.orderType === 'NORMAL') {
      await cancelNormalOrder(order.id)
    } else {
      await cancelOrder(order.id)
    }
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch {
    // 取消操作
  }
}

/** 确认收货：根据 orderType 调用不同确认收货接口 */
async function handleConfirm(order: OrderListItemVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', {
      type: 'info',
      confirmButtonText: '确认收货',
      cancelButtonText: '再想想'
    })
    confirmLoadingId.value = order.id
    if (order.orderType === 'NORMAL') {
      await confirmNormalOrder(order.id)
    } else {
      await confirmOrder(order.id)
    }
    ElMessage.success('已确认收货')
    fetchOrders()
  } catch {
    // 取消操作或请求错误
  } finally {
    confirmLoadingId.value = null
  }
}


/** 格式化时间 */
function formatTime(time: string): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/** 格式化价格（不带 ¥ 符号，由 ::before 提供） */
function formatPrice(price: number): string {
  return Number(price || 0).toFixed(2)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
/* 严格对照 index.html .order-page 样式 */
.order-page {
  padding: 24px;
}

/* 标签页 */
.order-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 20px;
}

.order-tab {
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.order-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* 骨架屏 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-item {
  height: 104px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
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

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
}

.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

/* 订单卡片：对照 .order-card 样式 */
.order-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  gap: 16px;
  align-items: center;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.order-disabled {
  opacity: 0.65;
}

/* 商品图 72x72 */
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

.order-card-img svg {
  width: 28px;
  height: 28px;
  color: var(--color-text-muted);
}

.order-card-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.order-item-count {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin-left: 6px;
}

/* 订单类型标签：seckill 红 / normal 蓝 */
.order-type-tag {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.02em;
  margin-right: 6px;
}

.order-type-tag.seckill {
  background: #fff1f0;
  color: #cf1322;
}

.order-type-tag.normal {
  background: #e6f4ff;
  color: #1677ff;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card-time {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.order-card-status {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-countdown-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-primary);
}

.countdown-suffix {
  color: var(--color-text-secondary);
}

/* 状态标签：unpaid 橙 / paid 绿 / cancelled 灰 / timeout 红 / completed 蓝 */
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

.status-tag.shipped {
  background: #e6f4ff;
  color: #1677ff;
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

/* 右侧 */
.order-card-right {
  text-align: right;
  flex-shrink: 0;
}

.order-card-price {
  font-family: var(--font-price);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.order-card-price::before {
  content: '\A5';
  font-size: 12px;
}

.order-card-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* 小按钮：primary 红底白字 */
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

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
}

.btn-sm.text:hover {
  color: var(--color-primary);
}
</style>
