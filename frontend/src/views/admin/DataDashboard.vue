<template>
  <div class="data-dashboard-page">
    <!-- 顶部总览卡片行：7 个核心指标 -->
    <div class="stat-grid" v-loading="overviewLoading">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.display }}</div>
        <div class="stat-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 中部图表区：用户注册趋势折线图 + 订单趋势柱状图 -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-header">
          <h4>用户注册趋势（近 7 天）</h4>
          <span class="chart-tip" v-if="userTrendLoading">加载中...</span>
        </div>
        <div ref="userTrendChartRef" class="chart-canvas"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h4>订单趋势（近 7 天）</h4>
          <span class="chart-tip" v-if="orderTrendLoading">加载中...</span>
        </div>
        <div ref="orderTrendChartRef" class="chart-canvas"></div>
      </div>
    </div>

    <!-- 底部秒杀排行榜表格 Top10 -->
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
            <th style="width: 120px">销量</th>
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
  </div>
</template>

<script setup lang="ts">
/**
 * 数据看台页面 - 全部数据来自后端统计 API，禁止任何模拟数据
 *
 * - 顶部 7 个总览卡片：getStatsOverview()
 * - 中部两个图表：getUserTrend(7) 折线图 / getOrderTrend(7) 柱状图
 * - 底部秒杀排行榜 Top10：getSeckillRanking(10)
 * - 使用 echarts 绘制图表，onMounted 初始化，onUnmounted dispose
 * - 页面风格参考现有 Dashboard.vue（白色卡片、红色主色调）
 */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getStatsOverview,
  getUserTrend,
  getOrderTrend,
  getSeckillRanking,
  type StatsOverviewVO,
  type TrendItemVO,
  type SeckillRankingVO
} from '@/api/stats'

/* === 总览数据 === */
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

/* === 总览卡片配置 === */
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

/* === 拉取总览数据 === */
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

/* === 趋势数据 === */
const userTrend = ref<TrendItemVO[]>([])
const orderTrend = ref<TrendItemVO[]>([])
const userTrendLoading = ref(false)
const orderTrendLoading = ref(false)

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

/* === 排行榜数据 === */
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

/* === ECharts 图表 === */
const userTrendChartRef = ref<HTMLElement | null>(null)
const orderTrendChartRef = ref<HTMLElement | null>(null)
let userTrendChart: echarts.ECharts | null = null
let orderTrendChart: echarts.ECharts | null = null

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

/* === 日期标签格式化 yyyy-MM-dd -> MM-dd === */
function formatDateLabel(date: string): string {
  if (!date) return ''
  // 兼容 "yyyy-MM-dd" 与 "yyyy-MM-dd HH:mm:ss"
  const parts = date.split(' ')[0].split('-')
  if (parts.length < 3) return date
  return `${parts[1]}-${parts[2]}`
}

/* === 渲染/更新图表 === */
function renderUserTrendChart(): void {
  if (!userTrendChart) return
  userTrendChart.setOption(buildUserTrendOption(userTrend.value))
}

function renderOrderTrendChart(): void {
  if (!orderTrendChart) return
  orderTrendChart.setOption(buildOrderTrendOption(orderTrend.value))
}

function initCharts(): void {
  if (userTrendChartRef.value) {
    userTrendChart = echarts.init(userTrendChartRef.value)
    renderUserTrendChart()
  }
  if (orderTrendChartRef.value) {
    orderTrendChart = echarts.init(orderTrendChartRef.value)
    renderOrderTrendChart()
  }
}

/* === 窗口大小变化重绘 === */
function handleResize(): void {
  userTrendChart?.resize()
  orderTrendChart?.resize()
}

/* === 自动刷新定时器（30 秒刷新总览与图表数据） === */
let refreshTimer: ReturnType<typeof setInterval> | null = null

async function refreshAll(): Promise<void> {
  await fetchOverview()
  await fetchUserTrend()
  await fetchOrderTrend()
  await fetchRanking()
  renderUserTrendChart()
  renderOrderTrendChart()
}

onMounted(async () => {
  // 并发拉取首批数据
  await Promise.all([fetchOverview(), fetchUserTrend(), fetchOrderTrend(), fetchRanking()])
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
  refreshTimer = setInterval(() => {
    refreshAll()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  userTrendChart?.dispose()
  orderTrendChart?.dispose()
  userTrendChart = null
  orderTrendChart = null
})
</script>

<style scoped>
/* === 总览卡片网格：7 个指标，自适应列数 === */
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

/* === 总览卡片：白色背景，无左侧色条 === */
.stat-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 18px 16px;
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

/* === 图表区：两列等宽 === */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
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

/* === 排行榜卡片 === */
.ranking-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
}

.ranking-card .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.ranking-card .card-header h4 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

/* === 原生表格：对照 Dashboard.vue .admin-table === */
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
</style>