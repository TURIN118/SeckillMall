<template>
  <div class="seckill-zone-page">
    <!-- === 页头横幅 === -->
    <div class="zone-banner">
      <div class="banner-inner">
        <div class="banner-eyebrow">SECKILL ZONE</div>
        <h1 class="banner-title">秒杀专区</h1>
        <p class="banner-subtitle">限时抢购 · 手慢无 · 限量开抢</p>
      </div>
      <div class="banner-deco">
        <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.5)" stroke-width="1.2">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
        </svg>
      </div>
    </div>

    <!-- === Tab 切换 === -->
    <div class="zone-tabs">
      <div v-for="tab in tabs" :key="tab.key" class="zone-tab" :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key">
        <span class="tab-label">{{ tab.label }}</span>
        <span class="tab-count">{{ tab.key === 'ACTIVE' ? activeList.length : pendingList.length }}</span>
      </div>
    </div>

    <!-- === 分类筛选栏（仅一级分类） === -->
    <div class="zone-category">
      <!-- 一级分类 -->
      <div class="category-row">
        <span class="cat-row-label">分类：</span>
        <div class="cat-tags">
          <span class="cat-tag" :class="{ active: selectedFirstId === null }" @click="selectFirst(null)">全部</span>
          <span v-for="cat in firstLevelCategories" :key="cat.id" class="cat-tag"
            :class="{ active: selectedFirstId === cat.id }" @click="selectFirst(cat.id)">{{ cat.categoryName }}</span>
        </div>
      </div>
    </div>

    <!-- === 内容区 === -->
    <div class="zone-content">
      <!-- 进行中 -->
      <template v-if="activeTab === 'ACTIVE'">
        <div v-if="activeLoading" class="skeleton-grid">
          <div v-for="i in 8" :key="i" class="skeleton-card"></div>
        </div>
        <template v-else>
          <div v-if="activeList.length > 0" class="seckill-grid">
            <div v-for="item in activeList" :key="item.id" class="seckill-card" @click="goDetail(item.id)">
              <!-- 商品图片 -->
              <div class="card-img">
                <el-image v-if="cardImage(item)" :src="cardImage(item)" fit="cover" class="card-img-tag" lazy>
                  <template #error>
                    <div class="img-placeholder">
                      <el-icon :size="40">
                        <Picture />
                      </el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="img-placeholder">
                  <el-icon :size="40">
                    <Picture />
                  </el-icon>
                </div>
                <!-- 状态标签 -->
                <span class="status-tag tag-active">抢购中</span>
              </div>

              <!-- 卡片主体 -->
              <div class="card-body">
                <div class="card-name" :title="item.seckillName">{{ item.seckillName }}</div>
                <div class="card-sub" :title="item.productName">{{ item.productName }}</div>

                <!-- 价格行 -->
                <div class="card-prices">
                  <span class="price-seckill">{{ formatPrice(item.seckillPrice) }}</span>
                  <span v-if="getOriginalPrice(item) && getOriginalPrice(item)! > item.seckillPrice"
                    class="price-original">¥{{
                      formatNumber(getOriginalPrice(item)!) }}</span>
                </div>

                <!-- 库存进度条 -->
                <div class="stock-bar">
                  <div class="stock-bar-fill" :class="stockLevel(item)" :style="{ width: soldPercent(item) + '%' }">
                  </div>
                </div>
                <div class="stock-text" :class="{ danger: isLowStock(item) }">
                  <template v-if="isLowStock(item)">仅剩 {{ item.availableCount }} 件！手慢无</template>
                  <template v-else>已抢 {{ soldPercent(item) }}% · 剩余 {{ item.availableCount }} 件</template>
                </div>

                <!-- 立即抢购按钮 -->
                <div class="card-action">
                  <button class="btn-seckill" @click.stop="goDetail(item.id)">立即抢购</button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty :image-size="120" description="暂无进行中的秒杀活动" />
          </div>
        </template>
      </template>

      <!-- 待开始 -->
      <template v-else>
        <div v-if="pendingLoading" class="skeleton-grid">
          <div v-for="i in 8" :key="i" class="skeleton-card"></div>
        </div>
        <template v-else>
          <div v-if="pendingList.length > 0" class="seckill-grid">
            <div v-for="item in pendingList" :key="item.id" class="seckill-card" @click="goDetail(item.id)">
              <!-- 商品图片 -->
              <div class="card-img">
                <el-image v-if="cardImage(item)" :src="cardImage(item)" fit="cover" class="card-img-tag" lazy>
                  <template #error>
                    <div class="img-placeholder">
                      <el-icon :size="40">
                        <Picture />
                      </el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="img-placeholder">
                  <el-icon :size="40">
                    <Picture />
                  </el-icon>
                </div>
                <!-- 状态标签 -->
                <span class="status-tag tag-pending">即将开始</span>
              </div>

              <!-- 卡片主体 -->
              <div class="card-body">
                <div class="card-name" :title="item.seckillName">{{ item.seckillName }}</div>
                <div class="card-sub" :title="item.productName">{{ item.productName }}</div>

                <!-- 价格行 -->
                <div class="card-prices">
                  <span class="price-seckill">{{ formatPrice(item.seckillPrice) }}</span>
                  <span v-if="getOriginalPrice(item) && getOriginalPrice(item)! > item.seckillPrice"
                    class="price-original">¥{{
                      formatNumber(getOriginalPrice(item)!) }}</span>
                </div>

                <!-- 库存信息（待开始不显示已抢进度，显示限量） -->
                <div class="stock-text pending-stock">限量 {{ item.stockCount }} 件 · 每人限购 {{ item.perLimit }} 件</div>

                <!-- 倒计时 / 开抢按钮 -->
                <div class="card-action">
                  <template v-if="remainMs(item.startTime) > 0">
                    <div class="countdown-wrap">
                      <span class="cd-label">距开始</span>
                      <span class="cd-block">{{ countdown(item.startTime).hours }}</span>
                      <span class="cd-sep">:</span>
                      <span class="cd-block">{{ countdown(item.startTime).minutes }}</span>
                      <span class="cd-sep">:</span>
                      <span class="cd-block">{{ countdown(item.startTime).seconds }}</span>
                    </div>
                  </template>
                  <button v-else class="btn-seckill" @click.stop="goDetail(item.id)">立即抢购</button>
                </div>
                <div class="start-time-text">{{ formatTime(item.startTime) }} 开抢</div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty :image-size="120" description="暂无待开始的秒杀活动" />
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 秒杀专区页面
 * - 两个区域：进行中(ACTIVE) / 待开始(PENDING)
 * - 数据全部来自后端 API (getSeckillList)，无任何模拟数据
 * - 原价通过 getProductDetail 异步获取（SeckillGoodsVO 无原价字段）
 * - 待开始卡片显示距 startTime 倒计时，到期后自动刷新列表
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { getSeckillList } from '@/api/seckill'
import { getProductDetail } from '@/api/product'
import { getCategoryTree } from '@/api/category'
import { formatImageUrl } from '@/utils/image'
import { getTimeOffset } from '@/api/request'
import dayjs from 'dayjs'
import type { SeckillGoodsVO, CategoryVO } from '@/types'

const router = useRouter()

/* === Tab 定义 === */
const tabs = [
  { key: 'ACTIVE' as const, label: '进行中' },
  { key: 'PENDING' as const, label: '待开始' }
]
const activeTab = ref<'ACTIVE' | 'PENDING'>('ACTIVE')

/* === 列表数据 === */
const activeList = ref<SeckillGoodsVO[]>([])
const pendingList = ref<SeckillGoodsVO[]>([])
const activeLoading = ref<boolean>(false)
const pendingLoading = ref<boolean>(false)

/* === 原价缓存（通过商品详情 API 获取，key 为 productId） === */
const originalPriceMap = reactive<Record<number | string, number>>({})

/* === 分类筛选（仅一级分类） === */
const allCategories = ref<CategoryVO[]>([])
const selectedFirstId = ref<number | string | null>(null)

/* 一级分类：parentId === 0 */
const firstLevelCategories = computed<CategoryVO[]>(() =>
  allCategories.value.filter((c) => c.parentId === 0)
)

/* 当前生效的分类 id：直接使用一级分类 id，未选则为 undefined（不筛选） */
const currentCategoryId = computed<number | string | undefined>(() => {
  return selectedFirstId.value === null ? undefined : selectedFirstId.value
})

/* === 当前时间戳（每秒更新，驱动倒计时 computed） === */
const now = ref<number>(Date.now())
let tickTimer: ReturnType<typeof setInterval> | null = null
// M43 修复: refreshing 改为 ref，使其在模板中可响应式使用，并保持类型安全
const refreshing = ref<boolean>(false)

/* === 自动刷新冷却机制（防止后端PENDING列表包含已到期商品导致无限刷新循环） === */
let lastAutoRefreshTime = 0
const AUTO_REFRESH_COOLDOWN = 30_000 // 30秒冷却期

/* === 拉取进行中列表 === */
async function fetchActive(silent = false): Promise<void> {
  if (!silent) activeLoading.value = true
  try {
    const res = await getSeckillList({ status: 'ACTIVE', categoryId: currentCategoryId.value, pageNum: 1, pageSize: 10 })
    activeList.value = res.data?.list || []
    // 异步填充原价，不阻塞渲染
    void fetchOriginalPrices(activeList.value)
  } catch {
    // 错误已由请求拦截器处理
  } finally {
    if (!silent) activeLoading.value = false
  }
}

/* === 拉取待开始列表 === */
async function fetchPending(silent = false): Promise<void> {
  if (!silent) pendingLoading.value = true
  try {
    const res = await getSeckillList({ status: 'PENDING', categoryId: currentCategoryId.value, pageNum: 1, pageSize: 10 })
    pendingList.value = res.data?.list || []
    void fetchOriginalPrices(pendingList.value)
  } catch {
    // 错误已由请求拦截器处理
  } finally {
    if (!silent) pendingLoading.value = false
  }
}

/* === 异步获取原价（并发，失败忽略） === */
async function fetchOriginalPrices(list: SeckillGoodsVO[]): Promise<void> {
  await Promise.all(
    list.map(async (item) => {
      if (originalPriceMap[item.productId] !== undefined) return
      try {
        const res = await getProductDetail(item.productId)
        if (res.data?.originalPrice) {
          originalPriceMap[item.productId] = res.data.originalPrice
        }
      } catch {
        // 忽略单个商品原价获取失败
      }
    })
  )
}

/* === 获取分类树 === */
async function fetchCategories(): Promise<void> {
  try {
    const res = await getCategoryTree()
    allCategories.value = res.data || []
  } catch {
    // 错误已由请求拦截器处理
  }
}

/* === 选择一级分类（直接刷新列表） === */
function selectFirst(id: number | string | null): void {
  if (selectedFirstId.value === id) return
  selectedFirstId.value = id
  lastAutoRefreshTime = 0 // 用户手动切换分类时重置冷却时间
  fetchActive()
  fetchPending()
}

/* === 跳转秒杀详情 === */
function goDetail(id: number | string): void {
  router.push(`/seckill/${id}`)
}

/* === 工具函数 === */
/** 卡片首图 */
function cardImage(item: SeckillGoodsVO): string {
  return formatImageUrl(item.images?.[0])
}

/** 价格格式化（带 ¥ 符号，保留两位小数） */
function formatPrice(price: number): string {
  return `¥${formatNumber(price)}`
}

/** 数字格式化（保留两位小数） */
function formatNumber(num: number): string {
  return Number(num || 0).toFixed(2)
}

/** 获取原价（未加载返回 undefined） */
function getOriginalPrice(item: SeckillGoodsVO): number | undefined {
  return originalPriceMap[item.productId]
}

/** 已抢百分比 */
function soldPercent(item: SeckillGoodsVO): number {
  const total = item.stockCount || 0
  if (total <= 0) return 0
  const sold = total - (item.availableCount || 0)
  return Math.min(100, Math.max(0, Math.round((sold / total) * 100)))
}

/** 库存进度条颜色等级 */
function stockLevel(item: SeckillGoodsVO): 'high' | 'mid' | 'low' {
  const percent = soldPercent(item)
  if (percent >= 60) return 'high'
  if (percent >= 25) return 'mid'
  return 'low'
}

/** 是否低库存（剩余 <= 5） */
function isLowStock(item: SeckillGoodsVO): boolean {
  return (item.availableCount || 0) <= 5 && (item.availableCount || 0) > 0
}

/** 距目标时间的剩余毫秒 */
function remainMs(targetTime: string): number {
  // M39 修复: 加上服务器时间偏移，避免本地时钟与服务器不一致导致倒计时偏差
  const offset = getTimeOffset()
  return dayjs(targetTime).valueOf() - (now.value + offset)
}

/** 倒计时时分秒 */
function countdown(targetTime: string): { hours: string; minutes: string; seconds: string } {
  const total = Math.max(0, Math.floor(remainMs(targetTime) / 1000))
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  return {
    hours: String(hours).padStart(2, '0'),
    minutes: String(minutes).padStart(2, '0'),
    seconds: String(seconds).padStart(2, '0')
  }
}

/** 时间格式化（显示） */
function formatTime(time: string): string {
  return dayjs(time).format('MM-DD HH:mm')
}

/* === 检查待开始商品是否到期，到期则刷新列表（带冷却机制防止无限刷新） === */
function checkPendingExpired(): void {
  if (pendingList.value.length === 0 || refreshing.value) return
  // 冷却检查：30秒内不重复触发自动刷新
  if (Date.now() - lastAutoRefreshTime < AUTO_REFRESH_COOLDOWN) return
  const offset = getTimeOffset()
  const hasExpired = pendingList.value.some(
    (item) => dayjs(item.startTime).valueOf() <= (now.value + offset)
  )
  if (hasExpired) {
    refreshing.value = true
    lastAutoRefreshTime = Date.now() // 记录自动刷新时间
    Promise.all([fetchActive(true), fetchPending(true)]).finally(() => {
      refreshing.value = false
    })
  }
}

/* === 生命周期 === */
onMounted(() => {
  fetchCategories()
  fetchActive()
  fetchPending()
  tickTimer = setInterval(() => {
    now.value = Date.now()
    checkPendingExpired()
  }, 1000)
})

onUnmounted(() => {
  if (tickTimer) clearInterval(tickTimer)
})
</script>

<style scoped>
/* === 页面根 === */
.seckill-zone-page {
  padding-bottom: 24px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  color: var(--color-text-primary);
}

/* === 页头横幅 === */
.zone-banner {
  position: relative;
  margin: 16px 24px 0;
  height: 160px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 0 40px;
  box-sizing: border-box;
}

.banner-inner {
  position: relative;
  z-index: 2;
}

.banner-eyebrow {
  font-size: 11px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}

.banner-title {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.banner-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.banner-deco {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.8;
}

/* === Tab 切换 === */
.zone-tabs {
  display: flex;
  gap: 8px;
  margin: 20px 24px 0;
  border-bottom: 2px solid var(--color-border);
}

.zone-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.zone-tab:hover {
  color: var(--color-text-primary);
}

.zone-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: var(--color-text-muted);
  border-radius: 10px;
}

.zone-tab.active .tab-count {
  background: var(--color-primary);
}

/* === 分类筛选栏 === */
.zone-category {
  margin: 16px 24px 0;
  padding: 12px 16px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.category-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.category-row+.category-row {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-border);
}

.cat-row-label {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  line-height: 28px;
}

.cat-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.cat-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-sm);
  cursor: pointer;
  line-height: 20px;
  transition: all 0.2s;
  user-select: none;
}

.cat-tag:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.cat-tag.active {
  color: #fff;
  background: var(--color-primary);
  font-weight: 600;
}

/* === 内容区 === */
.zone-content {
  padding: 20px 24px 0;
}

/* === 骨架屏 === */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.skeleton-card {
  height: 360px;
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

/* === 秒杀卡片网格 === */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* === 单个秒杀卡片 === */
.seckill-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
}

.seckill-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

/* 卡片图片 */
.card-img {
  position: relative;
  width: 100%;
  height: 200px;
  background: var(--color-bg-subtle);
  overflow: hidden;
}

.card-img-tag {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

/* 状态标签 */
.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  border-radius: var(--radius-sm);
  letter-spacing: 0.02em;
}

.tag-active {
  background: var(--color-primary);
}

.tag-pending {
  background: var(--color-accent);
}

/* 卡片主体 */
.card-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.card-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.card-sub {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 10px;
}

/* 价格行 */
.card-prices {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.price-seckill {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-seckill-price);
  font-family: var(--font-price);
  line-height: 1;
}

.price-original {
  font-size: 13px;
  color: var(--color-original-price);
  text-decoration: line-through;
}

/* 库存进度条 */
.stock-bar {
  width: 100%;
  height: 8px;
  background: var(--color-bg-muted);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 6px;
}

.stock-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.stock-bar-fill.high {
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
}

.stock-bar-fill.mid {
  background: linear-gradient(90deg, var(--color-accent), var(--color-warning));
}

.stock-bar-fill.low {
  background: var(--color-success);
}

/* 库存文字 */
.stock-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.stock-text.danger {
  color: var(--color-primary);
  font-weight: 700;
}

.stock-text.pending-stock {
  margin: 4px 0 12px;
}

/* 操作区 */
.card-action {
  margin-top: auto;
}

/* 立即抢购按钮 */
.btn-seckill {
  width: 100%;
  padding: 9px 0;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  letter-spacing: 0.04em;
  transition: opacity 0.15s, transform 0.15s;
}

.btn-seckill:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.btn-seckill:active {
  transform: translateY(0);
}

/* 倒计时 */
.countdown-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 7px 0;
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 700;
}

.cd-label {
  font-size: 12px;
  margin-right: 2px;
}

.cd-block {
  display: inline-block;
  min-width: 26px;
  padding: 2px 4px;
  text-align: center;
  background: var(--color-primary);
  color: #fff;
  border-radius: 3px;
  font-family: var(--font-mono);
  font-size: 13px;
}

.cd-sep {
  font-weight: 700;
}

/* 开抢时间 */
.start-time-text {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
}

/* === 空状态 === */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
}

/* === 响应式 === */
@media (max-width: 1024px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .zone-banner {
    margin: 12px 12px 0;
    padding: 0 20px;
    height: 130px;
  }

  .banner-title {
    font-size: 22px;
  }

  .banner-deco {
    display: none;
  }

  .zone-tabs {
    margin: 16px 12px 0;
  }

  .zone-category {
    margin: 12px 12px 0;
  }

  .zone-content {
    padding: 16px 12px 0;
  }

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .skeleton-card {
    height: 300px;
  }
}

@media (max-width: 480px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: 1fr;
  }
}
</style>