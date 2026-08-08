<template>
  <!-- 两列布局：左侧 sticky 状态导航栏 + 右侧订单卡片内容区 -->
  <div class="order-page">
    <div class="order-body">
      <!-- 左侧状态导航栏 -->
      <aside class="order-sidebar">
        <div class="sidebar-title">我的订单</div>
        <nav class="sidebar-nav">
          <div v-for="tab in tabs" :key="tab.name" class="nav-item"
            :class="{ active: activeTab === tab.name }" @click="handleTabChange(tab.name)">
            <span class="nav-label">{{ tab.label }}</span>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <div class="order-main">
        <!-- 加载骨架屏 -->
        <div v-if="loading" class="skeleton-list">
          <div v-for="i in 3" :key="i" class="skeleton-item"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="orderList.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="80" height="80">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="m21 15-5-5L5 21" />
            </svg>
          </div>
          <p class="empty-text">还没有订单，快去秒杀好物吧！</p>
          <button class="btn-sm primary" @click="router.push('/')">去逛逛</button>
        </div>

        <!-- 订单列表 -->
        <div v-else class="order-list">
          <div v-for="order in orderList" :key="order.id" class="order-card"
            :class="{ 'order-disabled': isDisabledStatus(order.status) }">
            <!-- 卡片头部：订单类型 + 订单号 + 下单时间 + 状态 -->
            <div class="order-card-header">
              <div class="order-card-header-left">
                <span class="order-type-tag" :class="order.orderType === 'SECKILL' ? 'seckill' : 'normal'">
                  {{ order.orderType === 'SECKILL' ? '秒杀订单' : '普通订单' }}
                </span>
                <span class="order-card-no">订单号：{{ order.orderNo }}</span>
                <span class="order-card-time">{{ formatTime(order.createTime) }}</span>
              </div>
              <div class="order-card-header-right">
                <span class="status-tag" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
              </div>
            </div>

            <!-- 卡片主体：商品列表 + 汇总操作 -->
            <div class="order-card-body" @click="goDetail(order)">
              <!-- 商品列表 -->
              <div class="order-goods-list">
                <div v-for="(item, idx) in (order.items || [])" :key="idx" class="order-goods-item">
                  <div class="order-goods-img">
                    <img v-if="item.productImage" :src="formatImageUrl(item.productImage)" :alt="item.productName"
                      loading="lazy" />
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="3" width="18" height="18" rx="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <path d="m21 15-5-5L5 21" />
                    </svg>
                  </div>
                  <div class="order-goods-info">
                    <div class="order-goods-name">{{ item.productName || '—' }}</div>
                    <div class="order-goods-spec">
                      <span class="goods-quantity">x{{ item.quantity }}</span>
                    </div>
                  </div>
                  <div class="order-goods-price">
                    <span class="price-symbol">¥</span>{{ formatPrice(item.price) }}
                  </div>
                </div>
                <!-- 无商品占位 -->
                <div v-if="!order.items || order.items.length === 0" class="order-goods-empty">
                  暂无商品信息
                </div>
              </div>

              <!-- 右侧汇总和操作 -->
              <div class="order-card-summary">
                <div class="order-card-amount">
                  <span class="amount-label">实付款</span>
                  <span class="amount-value">{{ formatPrice(order.totalAmount) }}</span>
                </div>
                <div class="order-card-actions">
                  <template v-if="order.status === 'UNPAID'">
                    <button class="btn-sm" @click.stop="goDetail(order)">查看详情</button>
                    <button class="btn-sm text" @click.stop="handleCancel(order)">取消订单</button>
                    <button class="btn-sm primary" @click.stop="goPay(order)">去支付</button>
                  </template>
                  <template v-else-if="order.status === 'SHIPPED'">
                    <button class="btn-sm" @click.stop="goDetail(order)">查看详情</button>
                    <button class="btn-sm primary" :disabled="confirmLoadingId === order.id"
                      @click.stop="handleConfirm(order)">
                      {{ confirmLoadingId === order.id ? '确认中...' : '确认收货' }}
                    </button>
                  </template>
                  <template v-else>
                    <button class="btn-sm" @click.stop="goDetail(order)">查看详情</button>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="order-pagination" v-if="total > 0">
            <PaginationWrapper :total="total" :page-num="pageNum" :page-size="pageSize" :page-sizes="[10, 20, 50]"
              @change="handlePageChange" />
          </div>
        </div>
      </div>
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
/* ============ 页面容器 ============ */
.order-page {
  padding: 24px;
}

/* ============ 两列布局主体 ============ */
.order-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* ============ 左侧状态导航栏 ============ */
.order-sidebar {
  width: 200px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
  align-self: flex-start;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.sidebar-title {
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-light, var(--color-border));
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 1px solid var(--color-border-light, var(--color-border));
  user-select: none;
}

.nav-item:last-child {
  border-bottom: none;
}

.nav-item:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-primary);
}

.nav-item.active {
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-light, rgba(229, 57, 53, 0.08));
  border-left: 3px solid var(--color-primary);
  padding-left: 17px;
}

.nav-label {
  line-height: 1.4;
}

/* ============ 右侧内容区 ============ */
.order-main {
  flex: 1;
  min-width: 0;
}

/* ============ 骨架屏 ============ */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-item {
  height: 220px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, #f5f5f5 50%, var(--color-bg-subtle) 75%);
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

/* ============ 空状态 ============ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 24px;
  text-align: center;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 20px;
  opacity: 0.6;
}

.empty-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

/* ============ 订单列表 ============ */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ============ 订单卡片 ============ */
.order-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow 0.25s, transform 0.25s;
}

.order-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.order-disabled {
  opacity: 0.65;
}

/* 卡片头部 */
.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.order-card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.order-card-no {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.order-card-time {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.order-card-header-right {
  display: flex;
  align-items: center;
}

/* 订单类型标签 */
.order-type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.order-type-tag.seckill {
  background: #fff1f0;
  color: #cf1322;
}

.order-type-tag.normal {
  background: #e6f4ff;
  color: #1677ff;
}

/* 状态标签：unpaid 橙 / paid 绿 / shipped 蓝 / cancelled 灰 / timeout 红 / completed 蓝 */
.status-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
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

/* ============ 卡片主体 ============ */
.order-card-body {
  display: flex;
  align-items: stretch;
  cursor: pointer;
}

/* 商品列表 */
.order-goods-list {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.order-goods-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--color-border);
}

.order-goods-item:last-child {
  border-bottom: none;
}

.order-goods-img {
  width: 64px;
  height: 64px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-text-muted);
  overflow: hidden;
}

.order-goods-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.order-goods-item:hover .order-goods-img img {
  transform: scale(1.06);
}

.order-goods-img svg {
  width: 24px;
  height: 24px;
  color: var(--color-text-muted);
}

.order-goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-goods-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-goods-spec {
  font-size: 12px;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.goods-quantity {
  display: inline-block;
  padding: 1px 6px;
  background: var(--color-bg-subtle);
  border-radius: 3px;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.order-goods-price {
  font-family: var(--font-price);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  flex-shrink: 0;
}

.price-symbol {
  color: var(--color-text-secondary);
  margin-right: 1px;
  font-size: 12px;
}

.order-goods-empty {
  padding: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

/* ============ 右侧汇总和操作 ============ */
.order-card-summary {
  width: 220px;
  padding: 16px 20px;
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 16px;
  flex-shrink: 0;
  background: linear-gradient(to bottom, #fff, var(--color-bg-subtle));
}

.order-card-amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
  width: 100%;
  justify-content: flex-end;
}

.amount-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.amount-value {
  font-family: var(--font-price);
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;
}

.amount-value::before {
  content: '\A5';
  font-size: 14px;
  margin-right: 2px;
}

.order-card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  align-items: stretch;
}

/* ============ 小按钮系统 ============ */
.btn-sm {
  padding: 7px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  transition: all 0.2s;
  text-align: center;
  line-height: 1.4;
}

.btn-sm:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
  color: #fff;
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  padding: 7px 8px;
}

.btn-sm.text:hover {
  color: var(--color-primary);
  background: none;
}

.btn-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ============ 分页 ============ */
.order-pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;
  margin-top: 8px;
}

/* ============ 响应式 768px 以下 ============ */
@media (max-width: 768px) {
  .order-page {
    padding: 12px;
  }

  .order-body {
    flex-direction: column;
    gap: 16px;
  }

  .order-sidebar {
    width: 100%;
    position: static;
    align-self: stretch;
    display: flex;
    flex-direction: column;
  }

  .sidebar-nav {
    flex-direction: row;
    overflow-x: auto;
  }

  .nav-item {
    white-space: nowrap;
    border-bottom: none;
    border-right: 1px solid var(--color-border-light, var(--color-border));
    padding: 12px 16px;
    font-size: 13px;
  }

  .nav-item:last-child {
    border-right: none;
  }

  .nav-item.active {
    border-left: none;
    border-bottom: 2px solid var(--color-primary);
    padding-left: 16px;
  }

  .order-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 10px 12px;
  }

  .order-card-header-left {
    gap: 8px;
  }

  .order-card-body {
    flex-direction: column;
  }

  .order-goods-list {
    padding: 12px;
  }

  .order-goods-img {
    width: 48px;
    height: 48px;
  }

  .order-goods-img svg {
    width: 20px;
    height: 20px;
  }

  .order-goods-name {
    font-size: 13px;
  }

  .order-card-summary {
    width: 100%;
    border-left: none;
    border-top: 1px solid var(--color-border);
    padding: 12px;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    background: var(--color-bg-subtle);
  }

  .order-card-actions {
    flex-direction: row;
    width: auto;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .amount-value {
    font-size: 18px;
  }

  .order-pagination {
    padding: 16px 0 4px;
  }
}
</style>
