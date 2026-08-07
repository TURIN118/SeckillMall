<template>
  <div class="seckill-zone-page">
    <!-- === 紧凑页面标题栏 === -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">秒杀专区</h1>
        <p class="page-subtitle">限时抢购 · 手慢无 · 限量开抢</p>
      </div>
      <div class="header-right">
        <span class="stat-label">当前场次</span>
        <span class="stat-value">{{ activityList.length }}</span>
      </div>
    </div>

    <!-- === 分类筛选栏（仅一级分类） === -->
    <div class="zone-category">
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
      <!-- 骨架屏 -->
      <div v-if="loading" class="skeleton-section-list">
        <div v-for="i in 2" :key="i" class="skeleton-section">
          <div class="skeleton-section-header"></div>
          <div class="skeleton-grid">
            <div v-for="j in 4" :key="j" class="skeleton-card"></div>
          </div>
        </div>
      </div>

      <!-- 场次列表 -->
      <template v-else>
        <div v-if="sortedActivities.length > 0">
          <div v-for="activity in sortedActivities" :key="activity.id" class="activity-section">
            <!-- 场次头部 -->
            <div class="activity-header">
              <div class="activity-title-wrap">
                <h2 class="activity-name">{{ activity.name }}</h2>
                <span class="activity-status" :class="statusClass(activity)">{{ statusText(activity) }}</span>
                <span class="activity-time">{{ formatTime(activity.startTime) }} - {{ formatTime(activity.endTime)
                  }}</span>
              </div>
              <div v-if="activity.status !== 2" class="activity-countdown">
                <span class="cd-label">{{ activity.status === 1 ? '距结束' : '距开始' }}</span>
                <span class="cd-block">{{
                  countdown(activity.status === 1 ? activity.endTime : activity.startTime).hours }}</span>
                <span class="cd-sep">:</span>
                <span class="cd-block">{{
                  countdown(activity.status === 1 ? activity.endTime : activity.startTime).minutes }}</span>
                <span class="cd-sep">:</span>
                <span class="cd-block">{{
                  countdown(activity.status === 1 ? activity.endTime : activity.startTime).seconds }}</span>
              </div>
            </div>

            <!-- 场次描述 -->
            <div v-if="activity.description" class="activity-desc">{{ activity.description }}</div>

            <!-- 商品网格 -->
            <div v-if="filteredGoods(activity).length > 0" class="seckill-grid">
              <div v-for="item in filteredGoods(activity)" :key="item.id" class="seckill-card" @click="goDetail(item)">
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
                  <span class="status-tag" :class="goodsStatusClass(item)">{{ goodsStatusText(item) }}</span>
                </div>

                <!-- 卡片主体 -->
                <div class="card-body">
                  <!-- Bug 11 修复: 移除重复的 seckillName (活动名称已在场次标题显示), 只显示商品名称 -->
                  <div class="card-name" :title="item.productName">{{ item.productName }}</div>

                  <!-- 价格行 -->
                  <div class="card-prices">
                    <span class="price-seckill">{{ formatPrice(item.seckillPrice) }}</span>
                    <span v-if="getOriginalPrice(item) && getOriginalPrice(item)! > item.seckillPrice"
                      class="price-original">¥{{
                        formatNumber(getOriginalPrice(item)!) }}</span>
                  </div>

                  <!-- 库存信息 -->
                  <template v-if="item.status === 'ACTIVE'">
                    <div class="stock-bar">
                      <div class="stock-bar-fill" :class="stockLevel(item)" :style="{ width: soldPercent(item) + '%' }">
                      </div>
                    </div>
                    <div class="stock-text" :class="{ danger: isLowStock(item) }">
                      <template v-if="isLowStock(item)">仅剩 {{ item.availableCount }} 件！手慢无</template>
                      <template v-else>已抢 {{ soldPercent(item) }}% · 剩余 {{ item.availableCount }} 件</template>
                    </div>
                  </template>
                  <template v-else-if="item.status === 'PENDING'">
                    <div class="stock-text pending-stock">限量 {{ item.stockCount }} 件 · 每人限购 {{ item.perLimit }} 件</div>
                  </template>
                  <template v-else>
                    <div class="stock-text ended-stock">已结束</div>
                  </template>

                  <!-- 操作按钮 -->
                  <div class="card-action">
                    <button v-if="item.status === 'ACTIVE'" class="btn-seckill"
                      @click.stop="goDetail(item)">立即抢购</button>
                    <button v-else-if="item.status === 'PENDING'" class="btn-seckill btn-pending"
                      @click.stop="goDetail(item)">即将开始</button>
                    <button v-else class="btn-seckill btn-ended" disabled>已结束</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 场次下无匹配商品（被分类筛选过滤） -->
            <div v-else class="empty-in-section">
              <el-empty :image-size="80" description="该场次下暂无匹配商品" />
            </div>
          </div>
        </div>

        <!-- H-F3 修复: 旧版秒杀数据展示区 (双轨制合并) -->
        <!-- 当新版场次化 API 无数据, 但旧版 /seckill/list 有数据时, 展示旧版数据 -->
        <div v-if="sortedActivities.length === 0 && legacySeckillList.length > 0" class="legacy-section">
          <div class="activity-header">
            <div class="activity-title-wrap">
              <h2 class="activity-name">秒杀商品</h2>
              <span class="activity-status status-active">进行中</span>
            </div>
          </div>
          <div class="seckill-grid">
            <div v-for="item in filteredLegacyGoods" :key="item.id" class="seckill-card" @click="goDetail(item)">
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
                <span class="status-tag" :class="goodsStatusClass(item)">{{ goodsStatusText(item) }}</span>
              </div>
              <div class="card-body">
                <!-- Bug 11 修复: 移除重复的 seckillName, 只显示商品名称 -->
                <div class="card-name" :title="item.productName">{{ item.productName }}</div>
                <div class="card-prices">
                  <span class="price-seckill">{{ formatPrice(item.seckillPrice) }}</span>
                  <span v-if="getOriginalPrice(item) && getOriginalPrice(item)! > item.seckillPrice"
                    class="price-original">¥{{
                      formatNumber(getOriginalPrice(item)!) }}</span>
                </div>
                <template v-if="item.status === 'ACTIVE'">
                  <div class="stock-bar">
                    <div class="stock-bar-fill" :class="stockLevel(item)" :style="{ width: soldPercent(item) + '%' }">
                    </div>
                  </div>
                  <div class="stock-text" :class="{ danger: isLowStock(item) }">
                    <template v-if="isLowStock(item)">仅剩 {{ item.availableCount }} 件！手慢无</template>
                    <template v-else>已抢 {{ soldPercent(item) }}% · 剩余 {{ item.availableCount }} 件</template>
                  </div>
                </template>
                <template v-else-if="item.status === 'PENDING'">
                  <div class="stock-text pending-stock">限量 {{ item.stockCount }} 件 · 每人限购 {{ item.perLimit }} 件</div>
                </template>
                <template v-else>
                  <div class="stock-text ended-stock">已结束</div>
                </template>
                <div class="card-action">
                  <button v-if="item.status === 'ACTIVE'" class="btn-seckill" @click.stop="goDetail(item)">立即抢购</button>
                  <button v-else-if="item.status === 'PENDING'" class="btn-seckill btn-pending"
                    @click.stop="goDetail(item)">即将开始</button>
                  <button v-else class="btn-seckill btn-ended" disabled>已结束</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="sortedActivities.length === 0 && legacySeckillList.length === 0" class="empty-state">
          <el-empty :image-size="120" description="暂无秒杀活动" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 秒杀专区页面（场次化重构版）
 * - 调用 listSeckillActivities() 获取场次列表，每场次包含多个商品
 * - 按场次分组展示：进行中 → 待开始 → 已结束
 * - 紧凑头部替代原 160px 大横幅
 * - 分类筛选改为前端过滤（API 不支持 categoryId 参数）
 * - 每 8 秒静默刷新数据，每秒驱动倒计时
 * - 使用 getTimeOffset() 处理服务器时间偏移
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { listSeckillActivities, getSeckillList, executeSeckill } from '@/api/seckill'
import { getProductDetail } from '@/api/product'
import { getCategoryTree } from '@/api/category'
import { formatImageUrl } from '@/utils/image'
import { getTimeOffset } from '@/api/request'
import { useVisibilityPolling } from '@/composables/useVisibilityPolling'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'
import type { SeckillActivityVO, SeckillGoodsVO, CategoryVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

/* === 列表数据 === */
const activityList = ref<SeckillActivityVO[]>([])
// H-F3 修复: 旧版秒杀数据 (双轨制合并展示)
// 当新版场次化 API 无数据时, 回退展示旧版 /seckill/list 数据, 避免孤儿数据
const legacySeckillList = ref<SeckillGoodsVO[]>([])
const loading = ref<boolean>(false)

/* === 原价缓存（通过商品详情 API 获取，key 为 productId） === */
const originalPriceMap = reactive<Record<number | string, number>>({})

/* === 分类筛选（仅一级分类） === */
const allCategories = ref<CategoryVO[]>([])
const selectedFirstId = ref<number | string | null>(null)

/* 一级分类：parentId === 0 */
const firstLevelCategories = computed<CategoryVO[]>(() =>
  allCategories.value.filter((c) => c.parentId === 0)
)

/* 选中的分类名称（用于前端过滤，因 listSeckillActivities 不支持 categoryId 参数） */
const selectedCategoryName = computed<string | null>(() => {
  if (selectedFirstId.value === null) return null
  const cat = allCategories.value.find((c) => c.id === selectedFirstId.value)
  return cat?.categoryName || null
})

/* === 当前时间戳（每秒更新，驱动倒计时 computed） === */
const now = ref<number>(Date.now())
let tickTimer: ReturnType<typeof setInterval> | null = null
const refreshing = ref<boolean>(false)

/* === 自动刷新冷却机制（防止场次状态变化导致无限刷新循环） === */
let lastAutoRefreshTime = 0
const AUTO_REFRESH_COOLDOWN = 30_000 // 30秒冷却期

/* === 拉取场次列表 ===
 * H-F3 修复: 同时拉取新版 (activities) 与旧版 (list) 数据, 合并展示
 * - 新版有数据: 优先展示场次化数据
 * - 新版无数据但旧版有: 回退展示旧版秒杀商品列表
 * - 两套都无: 显示空状态
 */
async function fetchActivities(silent = false): Promise<void> {
  if (!silent) loading.value = true
  try {
    // 并发拉取两套数据, 互不阻塞
    const [activitiesRes, legacyRes] = await Promise.allSettled([
      listSeckillActivities(),
      getSeckillList({ pageNum: 1, pageSize: 50 })
    ])
    // 新版场次化数据
    if (activitiesRes.status === 'fulfilled') {
      activityList.value = activitiesRes.value.data || []
    } else {
      activityList.value = []
    }
    // 旧版秒杀商品列表 (双轨制合并)
    if (legacyRes.status === 'fulfilled') {
      legacySeckillList.value = legacyRes.value.data?.list || []
    } else {
      legacySeckillList.value = []
    }
    // 收集所有商品, 异步填充原价, 不阻塞渲染
    const allGoods: SeckillGoodsVO[] = []
    activityList.value.forEach((a) => {
      allGoods.push(...(a.goodsList || []))
    })
    // 旧版数据也加入原价获取队列
    allGoods.push(...legacySeckillList.value)
    void fetchOriginalPrices(allGoods)
  } catch {
    // 错误已由请求拦截器处理
  } finally {
    if (!silent) loading.value = false
  }
}

/* === H-F3 修复: 旧版秒杀商品前端过滤 (按分类名称匹配) === */
const filteredLegacyGoods = computed<SeckillGoodsVO[]>(() => {
  if (!selectedCategoryName.value) return legacySeckillList.value
  return legacySeckillList.value.filter((g) => g.productName?.includes(selectedCategoryName.value!))
})

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

/* === 选择一级分类（前端过滤，无需重新请求） === */
function selectFirst(id: number | string | null): void {
  if (selectedFirstId.value === id) return
  selectedFirstId.value = id
}

/* === 场次排序：进行中(1) → 待开始(0) → 已结束(2)，同状态按开始时间升序 === */
const sortedActivities = computed<SeckillActivityVO[]>(() => {
  return [...activityList.value].sort((a, b) => {
    const order = (s: number): number => (s === 1 ? 0 : s === 0 ? 1 : 2)
    const diff = order(a.status) - order(b.status)
    if (diff !== 0) return diff
    return dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf()
  })
})

/* === 前端过滤场次下商品（按分类名称匹配 productName） === */
function filteredGoods(activity: SeckillActivityVO): SeckillGoodsVO[] {
  const goods = activity.goodsList || []
  if (!selectedCategoryName.value) return goods
  return goods.filter((g) => g.productName?.includes(selectedCategoryName.value!))
}

/* === Bug 10 修复: 点击商品直接执行秒杀, 不跳转商品详情 === */
async function goDetail(item: SeckillGoodsVO): Promise<void> {
  // 1. 登录校验
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再参与秒杀')
    router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
    return
  }

  // 2. 活动状态校验
  if (item.status !== 'ACTIVE') {
    ElMessage.info('活动未开始或已结束')
    return
  }

  // 3. 弹窗确认抢购
  try {
    await ElMessageBox.confirm(
      `确认以 ¥${formatNumber(item.seckillPrice)} 抢购「${item.productName}」？`,
      '秒杀确认',
      {
        confirmButtonText: '立即抢购',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    // 用户取消
    return
  }

  // 4. 调用秒杀下单 API
  try {
    const res = await executeSeckill(item.id)
    const result = res.data
    // status: 1=成功, 0=排队中, -1=库存不足, -2=重复购买
    if (result.status === 1) {
      ElMessage.success(`抢购成功！订单号：${result.orderNo}`)
    } else if (result.status === 0) {
      ElMessage.info('抢购请求已提交，正在排队中，请稍后查看订单')
    } else if (result.status === -1) {
      ElMessage.error('手慢了，商品已抢完')
    } else if (result.status === -2) {
      ElMessage.warning('您已抢购过该商品，不能重复购买')
    } else {
      ElMessage.error('抢购失败，请重试')
    }
    // 刷新秒杀列表, 让前端展示的 availableCount / 已抢 / 剩余 与后端保持同步
    await fetchActivities(true)
  } catch {
    // 错误已由全局拦截器统一提示
  }
}

/* === 场次状态相关 === */
function statusText(activity: SeckillActivityVO): string {
  if (activity.status === 1) return '进行中'
  if (activity.status === 0) return '待开始'
  return '已结束'
}

function statusClass(activity: SeckillActivityVO): string {
  if (activity.status === 1) return 'status-active'
  if (activity.status === 0) return 'status-pending'
  return 'status-ended'
}

/* === 商品状态相关 === */
function goodsStatusText(item: SeckillGoodsVO): string {
  if (item.status === 'ACTIVE') return '抢购中'
  if (item.status === 'PENDING') return '即将开始'
  return '已结束'
}

function goodsStatusClass(item: SeckillGoodsVO): string {
  if (item.status === 'ACTIVE') return 'tag-active'
  if (item.status === 'PENDING') return 'tag-pending'
  return 'tag-ended'
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
  // 加上服务器时间偏移，避免本地时钟与服务器不一致导致倒计时偏差
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

/* === 检查场次状态是否变化，变化则静默刷新（带冷却机制） === */
function checkActivityExpired(): void {
  if (activityList.value.length === 0 || refreshing.value) return
  if (Date.now() - lastAutoRefreshTime < AUTO_REFRESH_COOLDOWN) return
  const offset = getTimeOffset()
  const hasChanged = activityList.value.some((a) => {
    if (a.status === 0) {
      // 待开始 → 检查是否已到开始时间
      return dayjs(a.startTime).valueOf() <= (now.value + offset)
    }
    if (a.status === 1) {
      // 进行中 → 检查是否已到结束时间
      return dayjs(a.endTime).valueOf() <= (now.value + offset)
    }
    return false
  })
  if (hasChanged) {
    refreshing.value = true
    lastAutoRefreshTime = Date.now()
    fetchActivities(true).finally(() => {
      refreshing.value = false
    })
  }
}

/* === 生命周期 === */
// 数据刷新定时器, 每 8 秒静默刷新秒杀场次列表,
// 让前端展示的 availableCount / 已抢 / 剩余 与后端保持同步 (与倒计时 tickTimer 分开)
// M-F4 修复: 使用 useVisibilityPolling 替代裸 setInterval, 后台标签页暂停轮询
const { start: startDataPolling, stop: stopDataPolling } = useVisibilityPolling(
  () => fetchActivities(true),
  8000
)

onMounted(() => {
  fetchCategories()
  fetchActivities()
  tickTimer = setInterval(() => {
    now.value = Date.now()
    checkActivityExpired()
  }, 1000)
  // 启动数据定时轮询（静默刷新，不触发 loading 闪烁）
  startDataPolling()
})

onUnmounted(() => {
  if (tickTimer) clearInterval(tickTimer)
  stopDataPolling()
})
</script>

<style scoped>
/* === 页面根 === */
.seckill-zone-page {
  padding-bottom: 24px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  color: var(--color-text-primary);
}

/* === 紧凑页面标题栏（不超过 60px 高） === */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  margin: 16px 24px 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: -0.01em;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.stat-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--color-primary);
  font-family: var(--font-price);
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
.skeleton-section-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.skeleton-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.skeleton-section-header {
  height: 32px;
  width: 240px;
  margin-bottom: 16px;
  border-radius: var(--radius-sm);
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-muted) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.skeleton-card {
  height: 320px;
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

/* === 场次区块 === */
.activity-section {
  margin-bottom: 20px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-sizing: border-box;
}

/* H-F3 修复: 旧版秒杀数据区块样式 (复用场次区块样式) */
.legacy-section {
  margin-bottom: 20px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-sizing: border-box;
}

.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.activity-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.activity-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  line-height: 1.4;
}

.activity-status {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  border-radius: 10px;
  letter-spacing: 0.02em;
  line-height: 18px;
}

.activity-status.status-active {
  background: var(--color-primary);
}

.activity-status.status-pending {
  background: var(--color-accent);
}

.activity-status.status-ended {
  background: var(--color-text-muted);
}

.activity-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 场次倒计时 */
.activity-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 700;
}

.activity-countdown .cd-label {
  font-size: 12px;
  margin-right: 2px;
}

.activity-countdown .cd-block {
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

.activity-countdown .cd-sep {
  font-weight: 700;
}

.activity-desc {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

/* === 秒杀卡片网格（3列桌面 / 2列平板 / 1列手机） === */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 16px;
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
  height: 180px;
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

.tag-ended {
  background: var(--color-text-muted);
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

.stock-text.ended-stock {
  margin: 4px 0 12px;
  color: var(--color-text-muted);
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

.btn-seckill.btn-pending {
  background: var(--color-accent);
}

.btn-seckill.btn-ended {
  background: var(--color-text-muted);
  cursor: not-allowed;
}

.btn-seckill.btn-ended:hover {
  opacity: 1;
  transform: none;
}

/* === 场次内空状态 === */
.empty-in-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  margin-top: 16px;
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
  .page-header {
    margin: 12px 12px 0;
    padding: 12px 16px;
  }

  .page-title {
    font-size: 18px;
  }

  .page-subtitle {
    display: none;
  }

  .zone-category {
    margin: 12px 12px 0;
  }

  .zone-content {
    padding: 16px 12px 0;
  }

  .activity-section {
    padding: 12px 14px;
  }

  .activity-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .skeleton-card {
    height: 280px;
  }

  .card-img {
    height: 160px;
  }
}

@media (max-width: 480px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: 1fr;
  }
}
</style>
