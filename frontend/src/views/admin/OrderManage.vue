<template>
  <div class="order-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">全部订单</div>
        <div class="admin-table-actions">
          <input
            v-model="orderNo"
            class="admin-search-input"
            placeholder="搜索订单号..."
            @keyup.enter="handleQuery"
          />
          <select v-model="statusFilter" class="admin-filter-select" @change="handleQuery">
            <option value="">全部状态</option>
            <option value="UNPAID">待支付</option>
            <option value="PAID">已支付</option>
            <option value="CANCELLED">已取消</option>
            <option value="TIMEOUT">已超时</option>
            <option value="COMPLETED">已完成</option>
          </select>
          <input
            v-model="dateSingle"
            type="date"
            class="admin-search-input date-input"
          />
          <button class="btn-sm" @click="handleQuery">查询</button>
          <button class="btn-sm" @click="handleReset">重置</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户</th>
            <th>商品</th>
            <th>金额</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in orderList" :key="row.orderNo">
            <td class="order-no-cell">{{ row.orderNo }}</td>
            <td>{{ row.userId }}</td>
            <td>{{ row.productId }}</td>
            <td class="amount-cell">¥{{ formatPrice(row.totalAmount) }}</td>
            <td>
              <span class="status-tag" :class="getStatusTagClass(row.status)">
                {{ getStatusLabel(row.status) }}
              </span>
            </td>
            <td>{{ formatTime(row.createTime) }}</td>
            <td>
              <div class="table-actions">
                <button class="table-action-btn" @click="openDetail(row as SeckillOrder)">详情</button>
              </div>
            </td>
          </tr>
          <tr v-if="orderList.length === 0 && !loading">
            <td colspan="7" class="empty-cell">暂无订单数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 表尾分页：对照 .admin-table-footer / .pagination -->
      <div class="admin-table-footer">
        <span class="page-info">共 {{ total }} 条记录</span>
        <div class="pagination">
          <div
            class="page-btn"
            :class="{ disabled: pageNum <= 1 }"
            @click="handlePageChange(pageNum - 1)"
          >&lt;</div>
          <div
            v-for="p in displayPages"
            :key="p"
            class="page-btn"
            :class="{ active: p === pageNum }"
            @click="handlePageChange(p)"
          >{{ p }}</div>
          <div
            class="page-btn"
            :class="{ disabled: pageNum >= totalPages }"
            @click="handlePageChange(pageNum + 1)"
          >&gt;</div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="detailRow" class="detail-list">
        <div class="detail-row"><span class="detail-label">订单号</span><span class="detail-value">{{ detailRow.orderNo }}</span></div>
        <div class="detail-row"><span class="detail-label">用户 ID</span><span class="detail-value">{{ detailRow.userId }}</span></div>
        <div class="detail-row"><span class="detail-label">商品 ID</span><span class="detail-value">{{ detailRow.productId }}</span></div>
        <div class="detail-row"><span class="detail-label">秒杀价</span><span class="detail-value">¥{{ formatPrice(detailRow.seckillPrice) }}</span></div>
        <div class="detail-row"><span class="detail-label">总金额</span><span class="detail-value">¥{{ formatPrice(detailRow.totalAmount) }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value">{{ getStatusLabel(detailRow.status) }}</span></div>
        <div class="detail-row"><span class="detail-label">创建时间</span><span class="detail-value">{{ formatDateTime(detailRow.createTime) }}</span></div>
        <div class="detail-row"><span class="detail-label">支付时间</span><span class="detail-value">{{ detailRow.payTime ? formatDateTime(detailRow.payTime) : '—' }}</span></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * P14 后台订单管理 - 严格对照 index.html .page-admin-orders
 * 筛选栏 + 原生 table + 分页 + 详情弹窗
 */
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { getOrderList } from '@/api/order'
import type { SeckillOrder, OrderStatus } from '@/types'

/* === 列表数据 === */
const loading = ref(false)
const orderList = ref<SeckillOrder[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/* === 筛选条件 === */
const orderNo = ref('')
const statusFilter = ref<OrderStatus | ''>('')
const dateSingle = ref('')

/* === 总页数 === */
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* === 显示页码 (最多 5 个) === */
const displayPages = computed<number[]>(() => {
  const pages: number[] = []
  const total = totalPages.value
  let start = Math.max(1, pageNum.value - 2)
  let end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

/* === 格式化 === */
function formatPrice(price: number): string {
  return price.toFixed(2)
}
function formatDateTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}
/* === 下单时间只显示时分秒 (对照设计稿) === */
function formatTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('HH:mm:ss')
}

/* === 状态映射 === */
function getStatusLabel(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    UNPAID: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
    TIMEOUT: '已超时',
    COMPLETED: '已完成'
  }
  return map[status] || status
}
/* === 状态 tag class：对照设计稿 status-tag.unpaid/paid/timeout === */
function getStatusTagClass(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    UNPAID: 'unpaid',
    PAID: 'paid',
    CANCELLED: 'cancelled',
    TIMEOUT: 'timeout',
    COMPLETED: 'completed'
  }
  return map[status] || 'cancelled'
}

/* === 拉取订单列表 === */
async function fetchOrderList(): Promise<void> {
  loading.value = true
  try {
    const res = await getOrderList({
      status: statusFilter.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    let list = res.data.list
    if (orderNo.value) {
      list = list.filter((o) => o.orderNo.includes(orderNo.value))
    }
    if (dateSingle.value) {
      const startTs = dayjs(dateSingle.value).startOf('day').valueOf()
      const endTs = dayjs(dateSingle.value).endOf('day').valueOf()
      list = list.filter((o) => {
        const ts = dayjs(o.createTime).valueOf()
        return ts >= startTs && ts <= endTs
      })
    }
    orderList.value = list
    total.value = orderNo.value || dateSingle.value ? list.length : res.data.total
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 查询 === */
function handleQuery(): void {
  pageNum.value = 1
  fetchOrderList()
}

/* === 重置 === */
function handleReset(): void {
  orderNo.value = ''
  statusFilter.value = ''
  dateSingle.value = ''
  pageNum.value = 1
  fetchOrderList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchOrderList()
}

/* === 详情弹窗 === */
const detailVisible = ref(false)
const detailRow = ref<SeckillOrder | null>(null)
function openDetail(row: SeckillOrder): void {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchOrderList()
})
</script>

<style scoped>
/* === 严格对照 index.html .admin-table-wrap === */
.admin-table-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

/* === 严格对照 .admin-table-header === */
.admin-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
  gap: 8px;
}
.admin-table-title {
  font-size: 15px;
  font-weight: 700;
}
.admin-table-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* === 严格对照 .admin-search-input === */
.admin-search-input {
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 10px;
  font-size: 13px;
  width: 180px;
  outline: none;
}
.admin-search-input:focus {
  border-color: var(--color-primary);
}
.date-input {
  width: 140px;
}

/* === 严格对照 .admin-filter-select === */
.admin-filter-select {
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 8px;
  font-size: 13px;
  background: #fff;
  outline: none;
}

/* === 严格对照 .btn-sm === */
.btn-sm {
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 13px;
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
.btn-sm:hover {
  opacity: 0.9;
}

/* === 严格对照 .admin-table === */
.admin-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.admin-table thead th {
  background: var(--color-bg-subtle);
  padding: 10px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 0.02em;
}
.admin-table tbody td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}
.admin-table tbody tr:hover {
  background: var(--color-bg-subtle);
}
.admin-table tbody tr:last-child td {
  border-bottom: none;
}
.empty-cell {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 40px 16px;
}

/* === 订单号单元格：对照设计稿 style="font-family:var(--font-price);font-size:12px" === */
.order-no-cell {
  font-family: var(--font-price);
  font-size: 13px;
}

/* === 金额单元格：对照设计稿 style="font-weight:700" === */
.amount-cell {
  font-weight: 700;
}

/* === 严格对照 .status-tag === */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 13px;
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

/* === 严格对照 .table-actions / .table-action-btn === */
.table-actions {
  display: flex;
  gap: 8px;
}
.table-action-btn {
  font-size: 13px;
  color: var(--color-primary-blue);
  cursor: pointer;
  background: none;
  border: none;
  font-weight: 600;
  padding: 0;
}
.table-action-btn:hover {
  text-decoration: underline;
}

/* === 严格对照 .admin-table-footer / .pagination === */
.admin-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}
.page-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
}
.page-btn {
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  background: #fff;
  color: var(--color-text-primary);
  user-select: none;
}
.page-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.page-btn.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.page-btn.disabled {
  color: #ccc;
  cursor: not-allowed;
}
.page-btn.disabled:hover {
  border-color: var(--color-border);
  color: #ccc;
}

/* === 详情弹窗 === */
.detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
  font-size: 13px;
}
.detail-row:last-child {
  border-bottom: none;
}
.detail-label {
  color: var(--color-text-secondary);
}
.detail-value {
  font-weight: 600;
  color: var(--color-text-primary);
}
</style>
