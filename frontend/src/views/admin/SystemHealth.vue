<template>
  <div class="system-health-page">
    <!-- 状态告警横幅：对照 index.html 设计稿顶部状态条 -->
    <div class="health-banner" :class="health?.allHealthy ? 'ok' : 'err'">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
        <path d="M22 4L12 14.01l-3-3" />
      </svg>
      <span>{{ bannerText }}</span>
      <button class="refresh-btn" @click="fetchHealth" :disabled="loading">刷新</button>
    </div>

    <!-- 服务卡片：对照 index.html .health-grid / .health-card -->
    <div class="health-grid">
      <div v-for="svc in serviceCards" :key="svc.key" class="health-card"
        :class="svc.status === 'UP' ? 'healthy' : 'unhealthy'">
        <div class="health-icon" :class="svc.status === 'UP' ? 'ok' : 'err'">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path v-if="svc.key === 'redis'" d="M22 12h-4l-3 9L9 3l-3 9H2" />
            <template v-else-if="svc.key === 'database'">
              <ellipse cx="12" cy="5" rx="9" ry="3" />
              <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
              <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
            </template>
            <template v-else>
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
              <path d="M7 10l5 5 5-5" />
              <path d="M12 15V3" />
            </template>
          </svg>
        </div>
        <div class="health-name">{{ svc.name }}</div>
        <div class="health-status" :class="svc.status === 'UP' ? '' : 'err'">
          {{ svc.status === 'UP' ? `UP · ${svc.meta}` : 'DOWN · 服务异常' }}
        </div>
      </div>
    </div>

    <!-- 资源监控图表：对照 index.html .chart-grid -->
    <div class="chart-grid">
      <div class="chart-card">
        <h4>系统资源监控</h4>
        <div class="resource-list">
          <div class="resource-item">
            <div class="resource-label">
              <span>CPU 使用率</span>
              <span class="resource-value">{{ cpuUsageText }}</span>
            </div>
            <div class="stock-bar">
              <div class="stock-bar-fill" :class="getBarClass(cpuUsage)" :style="{ width: cpuUsage + '%' }"></div>
            </div>
          </div>
          <div class="resource-item">
            <div class="resource-label">
              <span>内存使用率</span>
              <span class="resource-value">{{ memUsageText }}</span>
            </div>
            <div class="stock-bar">
              <div class="stock-bar-fill" :class="getBarClass(memUsage)" :style="{ width: memUsage + '%' }"></div>
            </div>
          </div>
          <div class="resource-item">
            <div class="resource-label">
              <span>磁盘使用率</span>
              <span class="resource-value">{{ diskUsageText }}</span>
            </div>
            <div class="stock-bar">
              <div class="stock-bar-fill" :class="getBarClass(diskUsage)" :style="{ width: diskUsage + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card">
        <h4>Redis 缓存命中率</h4>
        <div class="hit-rate">
          <div class="hit-rate-value">{{ hitRateText }}</div>
          <div class="hit-rate-desc">过去 1 小时平均命中率</div>
        </div>
      </div>
    </div>

    <!-- JVM 内存监控 + 数据库连接池详情 -->
    <div class="chart-grid" style="margin-top: 16px;">
      <div class="chart-card">
        <h4>JVM 内存监控</h4>
        <div class="resource-list">
          <div class="resource-item">
            <div class="resource-label">
              <span>堆内存使用率</span>
              <span class="resource-value">{{ jvmHeapUsageText }}</span>
            </div>
            <div class="stock-bar">
              <div class="stock-bar-fill" :class="getBarClass(jvmHeapUsage)" :style="{ width: jvmHeapUsage + '%' }">
              </div>
            </div>
          </div>
          <div class="resource-item">
            <div class="resource-label">
              <span>非堆内存使用率</span>
              <span class="resource-value">{{ jvmNonHeapUsageText }}</span>
            </div>
            <!-- L-F2 修复: 非堆内存为 null 时不显示进度条, 避免显示 0% 误导 -->
            <div v-if="jvmNonHeapUsageText !== '暂未提供'" class="stock-bar">
              <div class="stock-bar-fill" :class="getBarClass(jvmNonHeapUsage)"
                :style="{ width: jvmNonHeapUsage + '%' }">
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-card">
        <h4>数据库连接池详情</h4>
        <div class="db-pool-list">
          <div class="db-pool-item">
            <div class="db-pool-label">活跃连接</div>
            <div class="db-pool-value">{{ dbActiveText }}</div>
          </div>
          <div class="db-pool-item">
            <div class="db-pool-label">空闲连接</div>
            <div class="db-pool-value">{{ dbIdleText }}</div>
          </div>
          <div class="db-pool-item">
            <div class="db-pool-label">最大连接</div>
            <div class="db-pool-value">{{ dbMaxText }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统信息 -->
    <div class="chart-grid" style="margin-top: 16px;">
      <div class="chart-card">
        <h4>系统信息</h4>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">操作系统</span>
            <span class="info-value">{{ osNameText }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">JDK 版本</span>
            <span class="info-value">{{ jdkVersionText }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">启动时间</span>
            <span class="info-value">{{ appStartTimeText }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">运行时长</span>
            <span class="info-value">{{ appUptimeText }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P17 系统健康 - 严格对照 index.html .page-admin-health
 * 状态告警横幅 + 3 个服务卡片 + 资源监控 + Redis 命中率
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getSystemHealth } from '@/api/system'
import type { SystemResourceVO } from '@/api/system'

/* === 数据 === */
const loading = ref(false)
const health = ref<SystemResourceVO | null>(null)

/* === 拉取健康状态 === */
async function fetchHealth(): Promise<void> {
  loading.value = true
  try {
    const res = await getSystemHealth()
    health.value = res.data
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 横幅文本 === */
const bannerText = computed(() => {
  if (!health.value) return '正在获取系统健康状态...'
  if (health.value.allHealthy) {
    return '系统运行正常 · 所有服务健康 · 最后检查：30秒前'
  }
  return '系统异常 · 部分服务不可用 · 请及时排查'
})

/* === 服务卡片：对照设计稿 Redis/MySQL/RabbitMQ === */
/* meta 使用接口返回的服务元信息，未返回时显示 '—' 而非写死的 mock */
const serviceCards = computed(() => {
  if (!health.value) return []
  return [
    {
      key: 'redis',
      name: 'Redis',
      status: health.value.redis,
      meta: health.value.redisResponseTime ?? '—'
    },
    {
      key: 'database',
      name: 'MySQL',
      status: health.value.database,
      meta: health.value.dbPoolUsage ?? '—'
    },
    {
      key: 'mq',
      name: 'RabbitMQ',
      status: health.value.mq,
      meta: health.value.mqQueueBacklog ?? '—'
    }
  ]
})

/* === 资源使用率 === */
/* 取接口返回值；后端未扩展时字段为 undefined，进度条按 0 渲染（空条），
   文本显示 '—' 占位，避免展示写死的 mock 假数据。 */
const cpuUsage = computed<number>(() => health.value?.cpuUsage ?? 0)
const memUsage = computed<number>(() => health.value?.memoryUsage ?? 0)
const diskUsage = computed<number>(() => health.value?.diskUsage ?? 0)
const cpuUsageText = computed<string>(() =>
  health.value && health.value.cpuUsage !== undefined ? `${health.value.cpuUsage}%` : '—'
)
const memUsageText = computed<string>(() =>
  health.value && health.value.memoryUsage !== undefined ? `${health.value.memoryUsage}%` : '—'
)
const diskUsageText = computed<string>(() =>
  health.value && health.value.diskUsage !== undefined ? `${health.value.diskUsage}%` : '—'
)
/* Redis 命中率：未返回时显示 '—' */
const hitRate = computed<number>(() => health.value?.redisHitRate ?? 0)
const hitRateText = computed<string>(() =>
  health.value && health.value.redisHitRate !== undefined
    ? `${health.value.redisHitRate}%`
    : '—'
)

/* === JVM 内存监控 ===
 * L-F2 修复: 非堆内存指标可能为 null (后端 metrics 未提供), 需兜底显示"暂未提供"
 * 旧代码在 jvmNonHeapUsage === null 时显示 "null%", 现改为 "暂未提供"
 */
const jvmHeapUsage = computed<number>(() => health.value?.jvmHeapUsage ?? 0)
const jvmNonHeapUsage = computed<number>(() => health.value?.jvmNonHeapUsage ?? 0)
const jvmHeapUsageText = computed<string>(() =>
  health.value && health.value.jvmHeapUsage !== undefined && health.value.jvmHeapUsage !== null
    ? `${health.value.jvmHeapUsage}%`
    : '暂未提供'
)
const jvmNonHeapUsageText = computed<string>(() =>
  health.value && health.value.jvmNonHeapUsage !== undefined && health.value.jvmNonHeapUsage !== null
    ? `${health.value.jvmNonHeapUsage}%`
    : '暂未提供'
)

/* === 数据库连接池详情 === */
const dbActiveText = computed<string>(() =>
  health.value && health.value.dbActiveConnections !== undefined
    ? String(health.value.dbActiveConnections)
    : '—'
)
const dbIdleText = computed<string>(() =>
  health.value && health.value.dbIdleConnections !== undefined
    ? String(health.value.dbIdleConnections)
    : '—'
)
const dbMaxText = computed<string>(() =>
  health.value && health.value.dbMaxConnections !== undefined
    ? String(health.value.dbMaxConnections)
    : '—'
)

/* === 系统信息 === */
const osNameText = computed<string>(() => health.value?.osName ?? '—')
const jdkVersionText = computed<string>(() => health.value?.jdkVersion ?? '—')
const appStartTimeText = computed<string>(() => health.value?.appStartTime ?? '—')
const appUptimeText = computed<string>(() => health.value?.appUptime ?? '—')


/* === 进度条颜色 class：对照设计稿 stock-bar-fill.high/mid === */
function getBarClass(value: number): string {
  if (value < 50) return 'high'
  if (value < 80) return 'mid'
  return 'low'
}

/* === 自动刷新定时器 === */
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchHealth()
  refreshTimer = setInterval(() => {
    fetchHealth()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
/* === 状态告警横幅：对照设计稿 === */
.health-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
}

.health-banner.ok {
  background: var(--tag-paid-bg);
  color: var(--color-success);
}

.health-banner.err {
  background: var(--tag-timeout-bg);
  color: var(--color-danger);
}

.refresh-btn {
  margin-left: auto;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid currentColor;
  background: transparent;
  color: inherit;
}

.refresh-btn:hover:not(:disabled) {
  opacity: 0.8;
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* === 严格对照 index.html .health-grid === */
.health-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}

/* === 严格对照 .health-card === */
.health-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.health-card.healthy {
  border-color: var(--color-success);
}

.health-card.unhealthy {
  border-color: var(--color-danger);
}

/* === 严格对照 .health-icon === */
.health-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.health-icon.ok {
  background: var(--tag-paid-bg);
  color: var(--color-success);
}

.health-icon.err {
  background: var(--tag-timeout-bg);
  color: var(--color-danger);
}

/* === 严格对照 .health-name / .health-status === */
.health-name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
}

.health-status {
  font-size: 13px;
  color: var(--color-success);
  font-weight: 600;
}

.health-status.err {
  color: var(--color-danger);
}

/* === 严格对照 .chart-grid / .chart-card === */
.chart-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
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

/* === 资源监控列表 === */
.resource-list {
  display: grid;
  gap: 16px;
}

.resource-item {
  /* 每个资源项 */
}

.resource-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.resource-value {
  font-weight: 700;
}

/* === 严格对照 .stock-bar / .stock-bar-fill === */
.stock-bar {
  height: 8px;
  background: #eee;
  border-radius: 3px;
  overflow: hidden;
}

.stock-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.stock-bar-fill.high {
  background: var(--color-success);
}

.stock-bar-fill.mid {
  background: var(--color-warning);
}

.stock-bar-fill.low {
  background: var(--color-primary);
}

/* === Redis 命中率：对照设计稿 === */
.hit-rate {
  text-align: center;
  padding: 20px 0;
}

.hit-rate-value {
  font-family: var(--font-price);
  font-size: 48px;
  font-weight: 700;
  color: var(--color-success);
}

.hit-rate-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}

/* === 数据库连接池详情 === */
.db-pool-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.db-pool-item {
  text-align: center;
  padding: 16px 8px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.db-pool-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.db-pool-value {
  font-family: var(--font-price);
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

/* === 系统信息 === */
.info-list {
  display: grid;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--color-border);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--color-text-secondary);
}

.info-value {
  font-weight: 600;
  color: var(--color-text);
}

</style>
