<template>
  <div class="order-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">全部订单</div>
        <div class="admin-table-actions">
          <input v-model="orderNo" class="admin-search-input" placeholder="搜索订单号..." @keyup.enter="handleQuery" />
          <select v-model="statusFilter" class="admin-filter-select" @change="handleQuery">
            <option value="">全部状态</option>
            <option value="UNPAID">待支付</option>
            <option value="PAID">已支付</option>
            <option value="CANCELLED">已取消</option>
            <option value="TIMEOUT">已超时</option>
            <option value="COMPLETED">已完成</option>
          </select>
          <input v-model="dateSingle" type="date" class="admin-search-input date-input" />
          <button class="btn-sm" @click="handleQuery">查询</button>
          <button class="btn-sm" @click="handleReset">重置</button>
          <button class="btn-sm primary" :disabled="exportLoading" @click="handleExport">
            {{ exportLoading ? '导出中...' : '导出Excel' }}
          </button>
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
                <button class="table-action-btn" @click="openDetail(row as AdminOrderVO)">详情</button>
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
          <div class="page-btn" :class="{ disabled: pageNum <= 1 }" @click="handlePageChange(pageNum - 1)">&lt;</div>
          <div v-for="p in displayPages" :key="p" class="page-btn" :class="{ active: p === pageNum }"
            @click="handlePageChange(p)">{{ p }}</div>
          <div class="page-btn" :class="{ disabled: pageNum >= totalPages }" @click="handlePageChange(pageNum + 1)">&gt;
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="detailRow" class="detail-list">
        <div class="detail-row"><span class="detail-label">订单号</span><span class="detail-value">{{ detailRow.orderNo
            }}</span></div>
        <div class="detail-row"><span class="detail-label">用户 ID</span><span class="detail-value">{{ detailRow.userId
            }}</span></div>
        <div class="detail-row"><span class="detail-label">商品 ID</span><span class="detail-value">{{ detailRow.productId
            }}</span></div>
        <div class="detail-row"><span class="detail-label">秒杀价</span><span class="detail-value">¥{{
          formatPrice(detailRow.seckillPrice) }}</span></div>
        <div class="detail-row"><span class="detail-label">总金额</span><span class="detail-value">¥{{
          formatPrice(detailRow.totalAmount) }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value">{{
          getStatusLabel(detailRow.status) }}</span></div>
        <div class="detail-row"><span class="detail-label">创建时间</span><span class="detail-value">{{
          formatDateTime(detailRow.createTime) }}</span></div>
        <div class="detail-row"><span class="detail-label">支付时间</span><span class="detail-value">{{ detailRow.payTime ?
          formatDateTime(detailRow.payTime) : '—' }}</span></div>
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
import * as XLSX from 'xlsx'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrderList } from '@/api/order'
import type { AdminOrderVO, OrderStatus } from '@/types'

/* === 列表数据 === */
const loading = ref(false)
const exportLoading = ref(false)
const orderList = ref<AdminOrderVO[]>([])
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
    // 后端搜索：将 orderNo / date / status 传给后端 /api/v1/admin/orders
    // 后端在 SQL 中进行模糊查询和按天筛选，避免前端仅过滤当前页 10 条的 BUG-005
    const res = await getAdminOrderList({
      status: statusFilter.value || undefined,
      orderNo: orderNo.value || undefined,
      date: dateSingle.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orderList.value = res.data.list
    total.value = res.data.total
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

/* === 导出 Excel（多 Sheet） === */
async function handleExport(): Promise<void> {
  // BUG-009: 未选择日期范围时硬编码 pageSize=10000 可能导致导出不全或内存溢出
  // 强制提示用户先选择日期范围，未选择时需用户二次确认才继续
  if (!dateSingle.value) {
    try {
      await ElMessageBox.confirm(
        '未选择日期范围，仅导出最近10000条订单，建议先选择日期范围后再导出。是否继续？',
        '导出提示',
        { confirmButtonText: '继续导出', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      // 用户取消导出
      return
    }
  }

  exportLoading.value = true
  try {
    // 1. 查询当前筛选条件下所有订单（传大 pageSize 获取全量）
    //    后端搜索：orderNo / date / status 由后端 SQL 过滤，避免前端仅过滤当前页的 BUG-005
    const res = await getAdminOrderList({
      status: statusFilter.value || undefined,
      orderNo: orderNo.value || undefined,
      date: dateSingle.value || undefined,
      pageNum: 1,
      pageSize: 10000
    })
    const orders: AdminOrderVO[] = res.data.list || []

    if (orders.length === 0) {
      ElMessage.warning('没有可导出的订单数据')
      return
    }

    // 2. 创建工作簿
    const wb = XLSX.utils.book_new()

    // === Sheet1: 订单明细 ===
    const detailData = orders.map((o) => ({
      订单号: o.orderNo,
      用户ID: o.userId,
      商品ID: o.productId,
      秒杀价: o.seckillPrice || 0,
      购买数量: o.quantity || 0,
      金额: o.totalAmount || 0,
      状态: getStatusLabel(o.status),
      下单时间: formatDateTime(o.createTime)
    }))
    const ws1 = XLSX.utils.json_to_sheet(detailData)
    ws1['!cols'] = [
      { wch: 25 }, // 订单号
      { wch: 12 }, // 用户ID
      { wch: 12 }, // 商品ID
      { wch: 12 }, // 秒杀价
      { wch: 10 }, // 购买数量
      { wch: 12 }, // 金额
      { wch: 10 }, // 状态
      { wch: 22 }  // 下单时间
    ]
    XLSX.utils.book_append_sheet(wb, ws1, '订单明细')

    // === Sheet2: 统计汇总 ===
    const totalAmount = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0)
    const statusCount = {
      UNPAID: orders.filter((o) => o.status === 'UNPAID').length,
      PAID: orders.filter((o) => o.status === 'PAID').length,
      CANCELLED: orders.filter((o) => o.status === 'CANCELLED').length,
      TIMEOUT: orders.filter((o) => o.status === 'TIMEOUT').length,
      COMPLETED: orders.filter((o) => o.status === 'COMPLETED').length
    }
    const orderCount = orders.length
    const summaryData = [
      { 指标: '总订单数', 值: orderCount, 说明: '当前筛选条件下' },
      { 指标: '总金额(元)', 值: totalAmount.toFixed(2), 说明: '所有订单金额合计' },
      { 指标: '待支付', 值: statusCount.UNPAID, 说明: `${(statusCount.UNPAID / orderCount * 100).toFixed(1)}%` },
      { 指标: '已支付', 值: statusCount.PAID, 说明: `${(statusCount.PAID / orderCount * 100).toFixed(1)}%` },
      { 指标: '已取消', 值: statusCount.CANCELLED, 说明: `${(statusCount.CANCELLED / orderCount * 100).toFixed(1)}%` },
      { 指标: '已超时', 值: statusCount.TIMEOUT, 说明: `${(statusCount.TIMEOUT / orderCount * 100).toFixed(1)}%` },
      { 指标: '已完成', 值: statusCount.COMPLETED, 说明: `${(statusCount.COMPLETED / orderCount * 100).toFixed(1)}%` }
    ]
    const ws2 = XLSX.utils.json_to_sheet(summaryData)
    ws2['!cols'] = [{ wch: 15 }, { wch: 15 }, { wch: 25 }]
    XLSX.utils.book_append_sheet(wb, ws2, '统计汇总')

    // === Sheet3: 状态分布 ===
    const distributionData = [
      { 状态: '待支付', 订单数: statusCount.UNPAID, '占比(%)': (statusCount.UNPAID / orderCount * 100).toFixed(1) },
      { 状态: '已支付', 订单数: statusCount.PAID, '占比(%)': (statusCount.PAID / orderCount * 100).toFixed(1) },
      { 状态: '已取消', 订单数: statusCount.CANCELLED, '占比(%)': (statusCount.CANCELLED / orderCount * 100).toFixed(1) },
      { 状态: '已超时', 订单数: statusCount.TIMEOUT, '占比(%)': (statusCount.TIMEOUT / orderCount * 100).toFixed(1) },
      { 状态: '已完成', 订单数: statusCount.COMPLETED, '占比(%)': (statusCount.COMPLETED / orderCount * 100).toFixed(1) }
    ]
    const ws3 = XLSX.utils.json_to_sheet(distributionData)
    ws3['!cols'] = [{ wch: 12 }, { wch: 10 }, { wch: 12 }]
    XLSX.utils.book_append_sheet(wb, ws3, '状态分布')

    // 3. 生成文件并下载
    const fileName = `订单数据_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    XLSX.writeFile(wb, fileName)
    ElMessage.success(`导出成功，共 ${orderCount} 条订单`)
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchOrderList()
}

/* === 详情弹窗 === */
const detailVisible = ref(false)
const detailRow = ref<AdminOrderVO | null>(null)
function openDetail(row: AdminOrderVO): void {
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
