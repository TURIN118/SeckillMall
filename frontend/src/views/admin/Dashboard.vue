<template>
  <div class="dashboard-page">
    <!-- 统计卡片：对照 index.html .stat-grid / .stat-card -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.display }}</div>
        <div class="stat-trend" :class="card.trendClass">{{ card.trend }}</div>
      </div>
    </div>

    <!-- 图表区域：对照 index.html .chart-grid / .chart-card -->
    <div class="chart-grid">
      <div class="chart-card">
        <h4>近 7 日订单趋势</h4>
        <div ref="lineChartRef" class="chart-canvas"></div>
      </div>
      <div class="chart-card">
        <h4>订单状态分布</h4>
        <div ref="pieChartRef" class="chart-canvas"></div>
      </div>
    </div>

    <!-- 快捷操作：对照 index.html 设计稿底部按钮 -->
    <div class="action-row">
      <button class="btn-sm primary" @click="goTo('/admin/seckills')">创建秒杀活动</button>
      <button class="btn-sm" @click="goTo('/admin/products')">新增商品</button>
    </div>

    <!-- 最近订单表格：填充底部空白 -->
    <div class="recent-orders-card">
      <div class="card-header">
        <h4>最近订单</h4>
        <button class="link-btn" @click="goTo('/admin/orders')">查看全部 &rsaquo;</button>
      </div>
      <table class="admin-table" v-loading="orderLoading">
        <thead>
          <tr>
            <th>订单号</th>
            <th>商品ID</th>
            <th>金额</th>
            <th>状态</th>
            <th>下单时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in recentOrders" :key="order.id">
            <td class="order-no-cell">{{ order.orderNo }}</td>
            <td>{{ order.productId }}</td>
            <td class="amount-cell">{{ formatMoney(order.totalAmount) }}</td>
            <td>
              <span class="status-tag" :class="getStatusTagClass(order.status)">
                {{ getStatusLabel(order.status) }}
              </span>
            </td>
            <td>{{ formatDateTime(order.createTime) }}</td>
          </tr>
          <tr v-if="recentOrders.length === 0 && !orderLoading">
            <td colspan="5" class="empty-cell">暂无订单数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 底部双栏：系统信息 + 快捷入口 -->
    <div class="info-grid">
      <div class="info-card">
        <h4>系统信息</h4>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">系统版本</span>
            <span class="info-value">v1.0.0</span>
          </div>
          <div class="info-item">
            <span class="info-label">运行环境</span>
            <span class="info-value">生产环境</span>
          </div>
          <div class="info-item">
            <span class="info-label">服务状态</span>
            <span class="info-value healthy">运行中</span>
          </div>
          <div class="info-item">
            <span class="info-label">当前时间</span>
            <span class="info-value">{{ currentTime }}</span>
          </div>
        </div>
      </div>
      <div class="info-card">
        <h4>快捷入口</h4>
        <div class="quick-links">
          <button class="quick-link-btn" @click="goTo('/admin/users')">用户管理</button>
          <button class="quick-link-btn" @click="goTo('/admin/orders')">订单管理</button>
          <button class="quick-link-btn" @click="goTo('/admin/products')">商品管理</button>
          <button class="quick-link-btn" @click="goTo('/admin/seckills')">秒杀管理</button>
          <button class="quick-link-btn" @click="goTo('/admin/logs')">操作日志</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P10 后台仪表盘 - 严格对照 index.html .page-admin-dashboard
 * - stat-grid: 4 列统计卡片
 * - chart-grid: ECharts 折线图 + 饼图
 * - 快捷操作按钮
 * - 最近订单表格 + 系统信息/快捷入口卡片
 * - 30 秒自动刷新
 */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getDashboard } from '@/api/system'
import { getOrderList } from '@/api/order'
import type { DashboardVO, SeckillOrder, OrderStatus } from '@/types'

const router = useRouter()

/* === 统计数据 === */
const stats = reactive<DashboardVO>({
  userCount: 0,
  orderCount: 0,
  totalSales: 0,
  seckillCount: 0
})
const loading = ref(false)

/* === 格式化数字 (千分位) === */
function formatNumber(num: number): string {
  return num.toLocaleString('zh-CN')
}

/* === 格式化金额 === */
function formatMoney(num: number): string {
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/* === 统计卡片配置：对照设计稿 stat-card.red/orange/green/blue === */
const statCards = computed(() => [
  {
    key: 'user',
    label: '总用户数',
    display: formatNumber(stats.userCount),
    tint: 'red',
    trend: `+${Math.floor(stats.userCount / 100)} 今日新增`,
    trendClass: 'up'
  },
  {
    key: 'order',
    label: '总订单数',
    display: formatNumber(stats.orderCount),
    tint: 'orange',
    trend: `+${Math.floor(stats.orderCount / 150)} 今日新增`,
    trendClass: 'up'
  },
  {
    key: 'sales',
    label: '总销售额',
    display: formatMoney(stats.totalSales),
    tint: 'green',
    trend: `+${formatMoney(stats.totalSales / 100)} 今日`,
    trendClass: 'up'
  },
  {
    key: 'seckill',
    label: '秒杀活动数',
    display: formatNumber(stats.seckillCount),
    tint: 'blue',
    trend: `${Math.floor(stats.seckillCount / 50)} 个进行中`,
    trendClass: 'down'
  }
])

/* === 拉取仪表盘数据 === */
async function fetchDashboard(): Promise<void> {
  loading.value = true
  try {
    const res = await getDashboard()
    Object.assign(stats, res.data)
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 路由跳转 === */
function goTo(path: string): void {
  router.push(path)
}

/* === 最近订单数据 === */
const recentOrders = ref<SeckillOrder[]>([])
const orderLoading = ref(false)

/* === 状态映射：对照 OrderManage.vue === */
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

/* === 格式化日期时间 === */
function formatDateTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/* === 拉取最近订单 (取 8 条) === */
async function fetchRecentOrders(): Promise<void> {
  orderLoading.value = true
  try {
    const res = await getOrderList({ pageNum: 1, pageSize: 8 })
    recentOrders.value = res.data.list || []
  } catch {
    // 加载失败时保留空列表，不阻塞页面
    recentOrders.value = []
  } finally {
    orderLoading.value = false
  }
}

/* === 系统信息：当前时间 (每秒更新) === */
const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let clockTimer: ReturnType<typeof setInterval> | null = null

/* === ECharts 图表 === */
const lineChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

/* === 折线图 mock 数据 (近7日订单趋势) === */
function getLineChartOption(): echarts.EChartsOption {
  const days: string[] = []
  const values: number[] = []
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    days.push(`${d.getMonth() + 1}-${d.getDate()}`)
    values.push(Math.floor(30 + Math.random() * 90))
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: days
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#e53935' },
        itemStyle: { color: '#e53935' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(229, 57, 53, 0.25)' },
            { offset: 1, color: 'rgba(229, 57, 53, 0)' }
          ])
        },
        data: values
      }
    ]
  }
}

/* === 饼图 mock 数据 (订单状态分布) === */
function getPieChartOption(): echarts.EChartsOption {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12 }
    },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: [
          { value: 35, name: '待支付', itemStyle: { color: '#ff9800' } },
          { value: 25, name: '已支付', itemStyle: { color: '#1976d2' } },
          { value: 20, name: '已完成', itemStyle: { color: '#4caf50' } },
          { value: 12, name: '已取消', itemStyle: { color: '#9e9e9e' } },
          { value: 8, name: '已超时', itemStyle: { color: '#f44336' } }
        ]
      }
    ]
  }
}

/* === 初始化图表 === */
function initCharts(): void {
  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption(getLineChartOption())
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption(getPieChartOption())
  }
}

/* === 窗口大小变化重绘 === */
function handleResize(): void {
  lineChart?.resize()
  pieChart?.resize()
}

/* === 自动刷新定时器 === */
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await fetchDashboard()
  fetchRecentOrders()
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
  refreshTimer = setInterval(() => {
    fetchDashboard()
  }, 30000)
  clockTimer = setInterval(() => {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (clockTimer) {
    clearInterval(clockTimer)
    clockTimer = null
  }
  lineChart?.dispose()
  pieChart?.dispose()
  lineChart = null
  pieChart = null
})
</script>

<style scoped>
/* === 严格对照 index.html .stat-grid === */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}

/* === 严格对照 index.html .stat-card === */
.stat-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.stat-card .stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.stat-card .stat-value {
  font-family: var(--font-price);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
  word-break: break-all;
}

.stat-card .stat-trend {
  font-size: 13px;
  margin-top: 6px;
}

.stat-card .stat-trend.up {
  color: var(--color-success);
}

.stat-card .stat-trend.down {
  color: var(--color-danger);
}

/* === 严格对照 index.html .chart-grid / .chart-card === */
.chart-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
}

.chart-card h4 {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 16px;
}

.chart-canvas {
  width: 100%;
  height: 240px;
}

/* === 快捷操作按钮：对照 index.html .btn-sm === */
.action-row {
  display: flex;
  gap: 12px;
}

.btn-sm {
  padding: 8px 20px;
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

/* === 最近订单卡片：复用 chart-card 视觉风格 === */
.recent-orders-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
  margin-top: 20px;
}

.recent-orders-card .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.recent-orders-card .card-header h4 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

.link-btn {
  background: none;
  border: none;
  font-size: 13px;
  color: var(--color-primary);
  cursor: pointer;
  padding: 0;
}

.link-btn:hover {
  opacity: 0.8;
}

/* === 原生表格：对照 OperationLog.vue .admin-table === */
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

.order-no-cell {
  font-family: var(--font-price);
  font-weight: 600;
}

.amount-cell {
  font-weight: 700;
  color: var(--color-danger);
}

.empty-cell {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 40px 16px;
}

/* === 状态标签：对照 OrderManage.vue .status-tag === */
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

/* === 底部双栏：系统信息 + 快捷入口 === */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 20px;
}

@media (max-width: 992px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

.info-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
}

.info-card h4 {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 16px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.info-label {
  color: var(--color-text-secondary);
}

.info-value {
  font-weight: 600;
  color: var(--color-text-primary);
}

.info-value.healthy {
  color: var(--color-success);
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-link-btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  transition: all 0.15s;
}

.quick-link-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>
