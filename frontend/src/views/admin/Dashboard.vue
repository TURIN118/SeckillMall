<template>
  <div class="dashboard-page">
    <!-- 第一行：7 个核心指标卡片（取自 DataDashboard，数据来自 getStatsOverview） -->
    <div class="stat-grid" v-loading="overviewLoading">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.display }}</div>
        <div class="stat-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 第二行：3 个图表（全部真实 API 数据） -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-header">
          <h4>订单趋势（近 7 天）</h4>
          <span class="chart-tip" v-if="orderTrendLoading">加载中...</span>
        </div>
        <div ref="orderTrendChartRef" class="chart-canvas"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h4>用户注册趋势（近 7 天）</h4>
          <span class="chart-tip" v-if="userTrendLoading">加载中...</span>
        </div>
        <div ref="userTrendChartRef" class="chart-canvas"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h4>订单状态分布</h4>
          <span class="chart-tip" v-if="statusDistLoading">加载中...</span>
        </div>
        <div ref="pieChartRef" class="chart-canvas"></div>
      </div>
    </div>

    <!-- 第三行：双栏布局 - 秒杀排行榜 Top10 + 最近订单前 5 条 -->
    <div class="dual-grid">
      <div class="ranking-card">
        <div class="card-header">
          <h4>秒杀排行榜 Top 10</h4>
          <span class="chart-tip" v-if="rankingLoading">加载中...</span>
        </div>
        <table class="admin-table" v-loading="rankingLoading">
          <thead>
            <tr>
              <th style="width: 60px">排名</th>
              <th>商品名称</th>
              <th style="width: 120px">秒杀价</th>
              <th style="width: 100px">销量</th>
              <th style="width: 140px">销售额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in ranking" :key="item.seckillId ?? idx">
              <td class="rank-cell">
                <span class="rank-badge" :class="rankClass(idx)">{{ idx + 1 }}</span>
              </td>
              <td class="product-name-cell">{{ item.productName || '—' }}</td>
              <td class="amount-cell">{{ formatMoney(item.seckillPrice) }}</td>
              <td class="count-cell">{{ formatNumber(item.salesCount) }}</td>
              <td class="amount-cell strong">{{ formatMoney(item.totalAmount) }}</td>
            </tr>
            <tr v-if="ranking.length === 0 && !rankingLoading">
              <td colspan="5" class="empty-cell">暂无排行榜数据</td>
            </tr>
          </tbody>
        </table>
      </div>
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
    </div>

    <!-- 第四行：快捷操作 + 系统信息 -->
    <div class="info-grid">
      <div class="info-card">
        <h4>快捷操作</h4>
        <div class="quick-links">
          <button class="quick-link-btn primary" @click="goTo('/admin/seckills')">创建秒杀活动</button>
          <button class="quick-link-btn" @click="goTo('/admin/products')">新增商品</button>
          <button class="quick-link-btn" @click="goTo('/admin/users')">用户管理</button>
          <button class="quick-link-btn" @click="goTo('/admin/orders')">订单管理</button>
          <button class="quick-link-btn" @click="goTo('/admin/products')">商品管理</button>
        </div>
      </div>
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
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台仪表盘 - 合并自原 Dashboard.vue 与 DataDashboard.vue
 *
 * 布局：
 * - 第一行：7 个核心指标卡片（getStatsOverview）
 * - 第二行：3 个图表（订单趋势柱状图 / 用户注册趋势折线图 / 订单状态分布饼图）
 * - 第三行：双栏 - 秒杀排行榜 Top10 + 最近订单前 5 条
 * - 第四行：快捷操作 + 系统信息
 *
 * 数据来源：全部从后端实时 API 获取，禁止任何 mock/随机数据。
 * 30 秒自动刷新总览与图表数据。
 */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import {
  getStatsOverview,
  getUserTrend,
  getOrderTrend,
  getSeckillRanking,
  getOrderStatusDistribution,
  type StatsOverviewVO,
  type TrendItemVO,
  type SeckillRankingVO,
  type OrderStatusItemVO
} from '@/api/stats'
import { getOrderList } from '@/api/order'
import type { SeckillOrder, OrderStatus } from '@/types'

const router = useRouter()

/* === 格式化数字 (千分位) === */
function formatNumber(num: number | undefined | null): string {
  if (num == null) return '0'
  return num.toLocaleString('zh-CN')
}

/* === 格式化金额 === */
function formatMoney(num: number | undefined | null): string {
  if (num == null) return '¥0.00'
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/* === 路由跳转 === */
function goTo(path: string): void {
  router.push(path)
}

/* ==================== 第一行：总览指标 ==================== */
const overview = reactive<StatsOverviewVO>({
  userCount: 0,
  orderCount: 0,
  seckillCount: 0,
  totalSales: 0,
  productCount: 0,
  todayOrderCount: 0,
  todayUserCount: 0
})
const overviewLoading = ref(false)

/* === 总览卡片配置：7 个核心指标 === */
const statCards = computed(() => [
  {
    key: 'user',
    label: '用户总数',
    display: formatNumber(overview.userCount),
    sub: `今日新增 ${formatNumber(overview.todayUserCount)}`
  },
  {
    key: 'order',
    label: '订单总数',
    display: formatNumber(overview.orderCount),
    sub: `今日订单 ${formatNumber(overview.todayOrderCount)}`
  },
  {
    key: 'seckill',
    label: '秒杀活动数',
    display: formatNumber(overview.seckillCount),
    sub: '活动总量'
  },
  {
    key: 'sales',
    label: '销售总额',
    display: formatMoney(overview.totalSales),
    sub: '已支付/已完成'
  },
  {
    key: 'product',
    label: '商品总数',
    display: formatNumber(overview.productCount),
    sub: '在售商品'
  },
  {
    key: 'todayOrder',
    label: '今日订单',
    display: formatNumber(overview.todayOrderCount),
    sub: '当日下单量'
  },
  {
    key: 'todayUser',
    label: '今日注册',
    display: formatNumber(overview.todayUserCount),
    sub: '当日新增用户'
  }
])

async function fetchOverview(): Promise<void> {
  overviewLoading.value = true
  try {
    const res = await getStatsOverview()
    if (res?.data) {
      Object.assign(overview, res.data)
    }
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    overviewLoading.value = false
  }
}

/* ==================== 第二行：图表数据 ==================== */
const userTrend = ref<TrendItemVO[]>([])
const orderTrend = ref<TrendItemVO[]>([])
const statusDist = ref<OrderStatusItemVO[]>([])
const userTrendLoading = ref(false)
const orderTrendLoading = ref(false)
const statusDistLoading = ref(false)

async function fetchUserTrend(): Promise<void> {
  userTrendLoading.value = true
  try {
    const res = await getUserTrend(7)
    userTrend.value = res?.data || []
  } catch {
    userTrend.value = []
  } finally {
    userTrendLoading.value = false
  }
}

async function fetchOrderTrend(): Promise<void> {
  orderTrendLoading.value = true
  try {
    const res = await getOrderTrend(7)
    orderTrend.value = res?.data || []
  } catch {
    orderTrend.value = []
  } finally {
    orderTrendLoading.value = false
  }
}

async function fetchStatusDist(): Promise<void> {
  statusDistLoading.value = true
  try {
    const res = await getOrderStatusDistribution()
    statusDist.value = res?.data || []
  } catch {
    statusDist.value = []
  } finally {
    statusDistLoading.value = false
  }
}

/* === 日期标签格式化 yyyy-MM-dd -> MM-dd === */
function formatDateLabel(date: string): string {
  if (!date) return ''
  // 兼容 "yyyy-MM-dd" 与 "yyyy-MM-dd HH:mm:ss"
  const parts = date.split(' ')[0].split('-')
  if (parts.length < 3) return date
  return `${parts[1]}-${parts[2]}`
}

/* === ECharts 图表实例与 ref === */
const orderTrendChartRef = ref<HTMLElement | null>(null)
const userTrendChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let orderTrendChart: echarts.ECharts | null = null
let userTrendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

/* === 订单趋势柱状图 === */
function buildOrderTrendOption(data: TrendItemVO[]): echarts.EChartsOption {
  const days = data.map((d) => formatDateLabel(d.date))
  const counts = data.map((d) => d.count)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: days
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '订单数',
        type: 'bar',
        barWidth: '50%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff7043' },
            { offset: 1, color: '#e53935' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        data: counts
      }
    ]
  }
}

/* === 用户注册趋势折线图 === */
function buildUserTrendOption(data: TrendItemVO[]): echarts.EChartsOption {
  const days = data.map((d) => formatDateLabel(d.date))
  const counts = data.map((d) => d.count)
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
        name: '注册数',
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
        data: counts
      }
    ]
  }
}

/* === 订单状态码到中文标签的映射 === */
const STATUS_LABEL_MAP: Record<string, string> = {
  UNPAID: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  TIMEOUT: '已超时',
  COMPLETED: '已完成'
}

/* === 订单状态码到饼图颜色的映射 === */
const STATUS_COLOR_MAP: Record<string, string> = {
  UNPAID: '#ff9800',
  PAID: '#1976d2',
  CANCELLED: '#9e9e9e',
  TIMEOUT: '#f44336',
  COMPLETED: '#4caf50'
}

/* === 订单状态分布饼图 === */
function buildPieOption(data: OrderStatusItemVO[]): echarts.EChartsOption {
  const pieData = data.map((item) => ({
    value: item.count,
    name: STATUS_LABEL_MAP[item.status] || item.status,
    itemStyle: { color: STATUS_COLOR_MAP[item.status] || '#9e9e9e' }
  }))
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
        data: pieData
      }
    ]
  }
}

/* === 渲染/更新图表 === */
function renderOrderTrendChart(): void {
  if (!orderTrendChart) return
  orderTrendChart.setOption(buildOrderTrendOption(orderTrend.value))
}

function renderUserTrendChart(): void {
  if (!userTrendChart) return
  userTrendChart.setOption(buildUserTrendOption(userTrend.value))
}

function renderPieChart(): void {
  if (!pieChart) return
  pieChart.setOption(buildPieOption(statusDist.value))
}

function initCharts(): void {
  if (orderTrendChartRef.value) {
    orderTrendChart = echarts.init(orderTrendChartRef.value)
    renderOrderTrendChart()
  }
  if (userTrendChartRef.value) {
    userTrendChart = echarts.init(userTrendChartRef.value)
    renderUserTrendChart()
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    renderPieChart()
  }
}

/* === 窗口大小变化重绘 === */
function handleResize(): void {
  orderTrendChart?.resize()
  userTrendChart?.resize()
  pieChart?.resize()
}

/* ==================== 第三行：排行榜 + 最近订单 ==================== */
/* === 秒杀排行榜 Top10 === */
const ranking = ref<SeckillRankingVO[]>([])
const rankingLoading = ref(false)

async function fetchRanking(): Promise<void> {
  rankingLoading.value = true
  try {
    const res = await getSeckillRanking(10)
    ranking.value = res?.data || []
  } catch {
    ranking.value = []
  } finally {
    rankingLoading.value = false
  }
}

/* === 排名徽章样式 === */
function rankClass(idx: number): string {
  if (idx === 0) return 'gold'
  if (idx === 1) return 'silver'
  if (idx === 2) return 'bronze'
  return 'normal'
}

/* === 最近订单前 5 条 === */
const recentOrders = ref<SeckillOrder[]>([])
const orderLoading = ref(false)

async function fetchRecentOrders(): Promise<void> {
  orderLoading.value = true
  try {
    const res = await getOrderList({ pageNum: 1, pageSize: 5 })
    recentOrders.value = res.data.list || []
  } catch {
    // 加载失败时保留空列表，不阻塞页面
    recentOrders.value = []
  } finally {
    orderLoading.value = false
  }
}

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

/* ==================== 第四行：系统信息 ==================== */
const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let clockTimer: ReturnType<typeof setInterval> | null = null

/* ==================== 生命周期与定时器 ==================== */
let refreshTimer: ReturnType<typeof setInterval> | null = null

async function refreshAll(): Promise<void> {
  await Promise.all([
    fetchOverview(),
    fetchUserTrend(),
    fetchOrderTrend(),
    fetchStatusDist(),
    fetchRanking(),
    fetchRecentOrders()
  ])
  renderOrderTrendChart()
  renderUserTrendChart()
  renderPieChart()
}

onMounted(async () => {
  // 并发拉取首批数据
  await Promise.all([
    fetchOverview(),
    fetchUserTrend(),
    fetchOrderTrend(),
    fetchStatusDist(),
    fetchRanking(),
    fetchRecentOrders()
  ])
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
  // 30 秒自动刷新总览与图表数据
  refreshTimer = setInterval(() => {
    refreshAll()
  }, 30000)
  // 每秒更新当前时间
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
  orderTrendChart?.dispose()
  userTrendChart?.dispose()
  pieChart?.dispose()
  orderTrendChart = null
  userTrendChart = null
  pieChart = null
})
</script>

<style scoped>
/* === 第一行：7 个核心指标卡片网格，自适应列数 === */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

@media (max-width: 1400px) {
  .stat-grid {
    grid-template-columns: repeat(4, 1fr);
  }
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

/* === 总览卡片：白色背景、圆角、阴影 === */
.stat-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 18px 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card .stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.stat-card .stat-value {
  font-family: var(--font-price);
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.1;
  word-break: break-all;
}

.stat-card .stat-sub {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 6px;
}

/* === 第二行：3 个图表等宽布局 === */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .chart-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.chart-card h4 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

.chart-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.chart-canvas {
  width: 100%;
  height: 260px;
}

/* === 第三行：双栏布局 - 排行榜 + 最近订单 === */
.dual-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .dual-grid {
    grid-template-columns: 1fr;
  }
}

.ranking-card,
.recent-orders-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.ranking-card .card-header,
.recent-orders-card .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.ranking-card .card-header h4,
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

/* === 排名徽章 === */
.rank-cell {
  text-align: center;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #9e9e9e;
}

.rank-badge.gold {
  background: #ffc107;
  color: #5d4037;
}

.rank-badge.silver {
  background: #9e9e9e;
}

.rank-badge.bronze {
  background: #cd7f32;
}

.rank-badge.normal {
  background: #bdbdbd;
}

.product-name-cell {
  font-weight: 600;
  color: var(--color-text-primary);
}

.order-no-cell {
  font-family: var(--font-price);
  font-weight: 600;
}

.amount-cell {
  font-family: var(--font-price);
  color: var(--color-danger);
}

.amount-cell.strong {
  font-weight: 700;
}

.count-cell {
  font-family: var(--font-price);
  font-weight: 600;
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

/* === 第四行：快捷操作 + 系统信息双栏 === */
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
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

.quick-link-btn.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.quick-link-btn.primary:hover {
  opacity: 0.9;
  color: #fff;
}
</style>
