<template>
  <div class="seckill-zone-page">

    <!-- === 顶部 Hero 区（左右分栏：标题+倒计时 / 场次标签） === -->
    <div class="seckill-hero">
      <!-- 左侧：标题 + 倒计时 -->
      <div class="hero-left">
        <div class="hero-title-wrap">
          <svg class="hero-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13 2L3 14h7l-1 8 10-12h-7l1-8z" />
          </svg>
          <h1 class="hero-title">限时秒杀</h1>
        </div>
        <div v-if="currentActivity && currentActivity.status !== 2" class="hero-countdown">
          <span class="hero-cd-label">{{ currentActivity.status === 1 ? '距结束' : '距开始' }}</span>
          <span class="hero-cd-block">{{ heroCountdown.hours }}</span>
          <span class="hero-cd-sep">:</span>
          <span class="hero-cd-block">{{ heroCountdown.minutes }}</span>
          <span class="hero-cd-sep">:</span>
          <span class="hero-cd-block">{{ heroCountdown.seconds }}</span>
        </div>
        <div v-else class="hero-countdown hero-countdown-static">
          <span class="hero-cd-label">敬请期待</span>
        </div>
      </div>

      <!-- 右侧：场次标签横向滚动 -->
      <div class="hero-tabs" v-if="sortedActivities.length > 0">
        <div v-for="activity in sortedActivities" :key="activity.id" class="session-tab"
          :class="{ active: selectedActivityId === activity.id, [statusClass(activity)]: true }"
          @click="selectActivity(activity.id)">
          <span class="tab-status">{{ statusText(activity) }}</span>
          <span class="tab-name">{{ activity.name }}</span>
          <span class="tab-time">{{ formatTime(activity.startTime) }}</span>
        </div>
      </div>
      <!-- 无场次时右侧占位提示 -->
      <div class="hero-tabs hero-tabs-empty" v-else>
        <span class="tabs-placeholder">暂无场次</span>
      </div>
    </div>

    <!-- === 内容区 === -->
    <div class="seckill-content">
      <!-- 骨架屏 -->
      <div v-if="loading" class="skeleton-grid">
        <div v-for="i in 12" :key="i" class="skeleton-card"></div>
      </div>

      <template v-else>
        <!-- 有场次数据 -->
        <template v-if="sortedActivities.length > 0">
          <!-- 选中场次的商品 -->
          <div v-if="currentGoods.length > 0" class="seckill-grid">
            <div v-for="item in currentGoods" :key="item.id" class="seckill-card" @click="goDetail(item)">
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
                    <div class="stock-bar-fill" :class="stockLevel(item)"
                      :style="{ width: soldPercent(item) + '%' }">
                    </div>
                  </div>
                  <div class="stock-text" :class="{ danger: isLowStock(item) }">
                    <template v-if="isLowStock(item)">仅剩 {{ item.availableCount }} 件！手慢无</template>
                    <template v-else>已抢 {{ soldPercent(item) }}% · 剩余 {{ item.availableCount }} 件</template>
                  </div>
                </template>
                <template v-else-if="item.status === 'PENDING'">
                  <div class="stock-text pending-stock">限量 {{ item.stockCount }} 件 · 每人限购 {{ item.perLimit }} 件
                  </div>
                </template>
                <template v-else>
                  <div class="stock-text ended-stock">已结束</div>
                </template>

                <!-- 操作按钮 -->
                <div class="card-action">
                  <button v-if="item.status === 'ACTIVE' && Number(item.availableCount) > 0" class="btn-seckill"
                    @click.stop="goDetail(item)">立即抢购</button>
                  <button
                    v-else-if="item.status === 'ACTIVE' && (!item.availableCount || Number(item.availableCount) <= 0)"
                    class="btn-seckill btn-ended" disabled>已抢完</button>
                  <button v-else-if="item.status === 'PENDING'" class="btn-seckill btn-pending"
                    @click.stop="goDetail(item)">即将开始</button>
                  <button v-else class="btn-seckill btn-ended" disabled>已结束</button>
                </div>
              </div>
            </div>
          </div>
          <!-- 场次下无商品 -->
          <div v-else class="empty-in-section">
            <el-empty :image-size="80" description="该场次下暂无商品" />
          </div>
        </template>

        <!-- H-F3 修复: 旧版秒杀数据展示区 (双轨制合并) -->
        <!-- 当新版场次化 API 无数据, 但旧版 /seckill/list 有数据时, 展示旧版数据 -->
        <template v-else-if="legacySeckillList.length > 0">
          <div class="legacy-header">
            <h2 class="legacy-title">秒杀商品</h2>
            <span class="activity-status status-active">进行中</span>
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
                    <div class="stock-bar-fill" :class="stockLevel(item)"
                      :style="{ width: soldPercent(item) + '%' }">
                    </div>
                  </div>
                  <div class="stock-text" :class="{ danger: isLowStock(item) }">
                    <template v-if="isLowStock(item)">仅剩 {{ item.availableCount }} 件！手慢无</template>
                    <template v-else>已抢 {{ soldPercent(item) }}% · 剩余 {{ item.availableCount }} 件</template>
                  </div>
                </template>
                <template v-else-if="item.status === 'PENDING'">
                  <div class="stock-text pending-stock">限量 {{ item.stockCount }} 件 · 每人限购 {{ item.perLimit }} 件
                  </div>
                </template>
                <template v-else>
                  <div class="stock-text ended-stock">已结束</div>
                </template>
                <div class="card-action">
                  <button v-if="item.status === 'ACTIVE' && Number(item.availableCount) > 0" class="btn-seckill"
                    @click.stop="goDetail(item)">立即抢购</button>
                  <button
                    v-else-if="item.status === 'ACTIVE' && (!item.availableCount || Number(item.availableCount) <= 0)"
                    class="btn-seckill btn-ended" disabled>已抢完</button>
                  <button v-else-if="item.status === 'PENDING'" class="btn-seckill btn-pending"
                    @click.stop="goDetail(item)">即将开始</button>
                  <button v-else class="btn-seckill btn-ended" disabled>已结束</button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty :image-size="120" description="暂无秒杀活动" />
        </div>
      </template>
    </div>

    <!-- === 秒杀规则提示（底部小卡片，填充空白） === -->
    <div v-if="!loading && (sortedActivities.length > 0 || legacySeckillList.length > 0)" class="seckill-rules">
      <div class="rules-card">
        <h3 class="rules-title">秒杀规则</h3>
        <div class="rules-list">
          <div class="rule-item"><span class="rule-num">1</span>秒杀商品数量有限，先到先得</div>
          <div class="rule-item"><span class="rule-num">2</span>每位用户每场秒杀限购一件，不可重复参与</div>
          <div class="rule-item"><span class="rule-num">3</span>秒杀订单需在15分钟内完成支付，超时自动取消</div>
          <div class="rule-item"><span class="rule-num">4</span>秒杀商品不支持退换货，请谨慎购买</div>
        </div>
      </div>
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
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
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
  let result = legacySeckillList.value
  if (selectedCategoryName.value) {
    result = legacySeckillList.value.filter((g) => g.productName?.includes(selectedCategoryName.value!))
  }
  // 库存优先排序: 有库存的在前，无库存的在后（保持原相对顺序）
  return [...result].sort((a, b) => {
    const aStock = Number(a.availableCount) || 0
    const bStock = Number(b.availableCount) || 0
    if (aStock > 0 && bStock <= 0) return -1
    if (aStock <= 0 && bStock > 0) return 1
    return 0 // 都有库存或都无库存时保持原顺序
  })
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
  let result = goods
  if (selectedCategoryName.value) {
    result = goods.filter((g) => g.productName?.includes(selectedCategoryName.value!))
  }
  // 库存优先排序: 有库存的在前，无库存的在后（保持原相对顺序）
  return [...result].sort((a, b) => {
    const aStock = Number(a.availableCount) || 0
    const bStock = Number(b.availableCount) || 0
    if (aStock > 0 && bStock <= 0) return -1
    if (aStock <= 0 && bStock > 0) return 1
    return 0 // 都有库存或都无库存时保持原顺序
  })
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

  // 2.1 库存校验: 已抢完时阻止抢购 (增强校验: 处理 undefined/null/字符串"0" 等异常值)
  if (!item.availableCount || Number(item.availableCount) <= 0) {
    ElMessage.warning('手慢了，商品已抢完')
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

/* === 重构新增: 场次标签切换逻辑 === */
/** 当前选中的场次ID */
const selectedActivityId = ref<number | string | null>(null)

/** 选中场次 */
function selectActivity(id: number | string): void {
  selectedActivityId.value = id
}

/** 当前选中的场次对象 */
const currentActivity = computed<SeckillActivityVO | null>(() => {
  if (!selectedActivityId.value) return sortedActivities.value[0] || null
  return sortedActivities.value.find((a) => a.id === selectedActivityId.value) || null
})

/** 当前场次的商品列表 */
const currentGoods = computed<SeckillGoodsVO[]>(() => {
  if (!currentActivity.value) return []
  return filteredGoods(currentActivity.value)
})

/** Hero区域倒计时（当前场次的倒计时） */
const heroCountdown = computed(() => {
  if (!currentActivity.value || currentActivity.value.status === 2) {
    return { hours: '00', minutes: '00', seconds: '00' }
  }
  const target = currentActivity.value.status === 1 ? currentActivity.value.endTime : currentActivity.value.startTime
  return countdown(target)
})

/** watch sortedActivities 变化时自动选中第一个场次 */
watch(sortedActivities, (list) => {
  if (list.length > 0 && !list.find((a) => a.id === selectedActivityId.value)) {
    selectedActivityId.value = list[0].id
  }
}, { immediate: true })

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
/*
 * 秒杀专区现代化UI样式（场次标签切换+6列网格版）
 * 设计参考: Material Design 3 + Apple HIG
 * 主色调: #FF4B2B → #FF416C 渐变 (秒杀主题)
 * 强调色: #E94560 (秒杀价)
 * 间距系统: 8pt 网格
 */

/* === 页面根 - 与首页宽度一致，不内收 === */
.seckill-zone-page {
  padding: 24px;
  padding-bottom: 32px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  color: var(--color-text-primary);
  box-sizing: border-box;
}

/* === 顶部 Hero 区（左右分栏） === */
.seckill-hero {
  display: flex;
  gap: 20px;
  align-items: stretch;
  margin-bottom: 20px;
}

/* 左侧：标题+倒计时（渐变背景，限制宽度） */
.hero-left {
  width: 320px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #FF4B2B, #FF416C);
  border-radius: 12px;
  padding: 20px 24px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(255, 75, 43, 0.25);
  box-sizing: border-box;
}

.hero-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-icon {
  width: 28px;
  height: 28px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.hero-title {
  font-size: 24px;
  font-weight: 800;
  margin: 0;
  letter-spacing: 0.02em;
  line-height: 1.2;
}

.hero-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
}

.hero-countdown-static {
  gap: 0;
}

.hero-cd-label {
  font-size: 13px;
  opacity: 0.9;
  margin-right: 8px;
  font-weight: 500;
}

.hero-cd-block {
  min-width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 6px;
  font-size: 16px;
  font-weight: 700;
  font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  font-variant-numeric: tabular-nums;
  padding: 0 4px;
  box-sizing: border-box;
}

.hero-cd-sep {
  font-size: 16px;
  font-weight: 700;
}

/* 右侧：场次标签横向滚动 */
.hero-tabs {
  flex: 1;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px;
  align-items: center;
  min-width: 0;
}

.hero-tabs-empty {
  justify-content: center;
  background: #fff;
  border-radius: 12px;
  border: 1px dashed var(--color-border, #e5e7eb);
  padding: 24px;
}

.tabs-placeholder {
  color: var(--color-text-muted, #9CA3AF);
  font-size: 14px;
}

/* 自定义滚动条 */
.hero-tabs::-webkit-scrollbar {
  height: 6px;
}

.hero-tabs::-webkit-scrollbar-track {
  background: transparent;
}

.hero-tabs::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 99px;
}

.hero-tabs::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

.session-tab {
  flex-shrink: 0;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
  transition: all 0.15s;
  box-sizing: border-box;
}

.session-tab:hover {
  border-color: #FF4B2B;
  box-shadow: 0 2px 8px rgba(255, 75, 43, 0.1);
  transform: translateY(-1px);
}

.session-tab.active {
  border-color: #FF4B2B;
  background: linear-gradient(135deg, rgba(255, 75, 43, 0.06), rgba(255, 65, 108, 0.06));
  box-shadow: 0 2px 8px rgba(255, 75, 43, 0.15);
}

.tab-status {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 99px;
  display: inline-block;
  width: fit-content;
  line-height: 14px;
}

.session-tab.status-active .tab-status {
  background: linear-gradient(135deg, #FF4B2B, #FF416C);
  color: #fff;
}

.session-tab.status-pending .tab-status {
  background: #FA8B17;
  color: #fff;
}

.session-tab.status-ended .tab-status {
  background: #9CA3AF;
  color: #fff;
}

.tab-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #1a1a1a);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-time {
  font-size: 11px;
  color: var(--color-text-muted, #9CA3AF);
  font-weight: 500;
}

/* === 内容区 === */
.seckill-content {
  width: 100%;
  box-sizing: border-box;
}

/* === 骨架屏（6列网格） === */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.skeleton-card {
  height: 280px;
  background: #fff;
  border-radius: 12px;
  background-image: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
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

/* === 旧版秒杀数据头部 === */
.legacy-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.legacy-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #1a1a1a);
  margin: 0;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

/* 场次状态标签（用于 legacy-header） */
.activity-status {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  border-radius: 99px;
  letter-spacing: 0.02em;
  line-height: 16px;
}

.activity-status.status-active {
  background: linear-gradient(135deg, #FF4B2B, #FF416C);
}

.activity-status.status-pending {
  background: #FA8B17;
}

.activity-status.status-ended {
  background: #9CA3AF;
}

/* === 秒杀卡片网格（6列桌面） === */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

/* === 单个秒杀卡片 === */
.seckill-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.seckill-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* === 卡片图片（缩小到140px） === */
.card-img {
  position: relative;
  width: 100%;
  height: 140px;
  background: #f5f5f5;
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
  color: #D1D5DB;
  background: #f9f9f9;
}

/* === 状态标签（左上角绝对定位，圆角全圆） === */
.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  border-radius: 99px;
  letter-spacing: 0.02em;
  line-height: 14px;
  backdrop-filter: blur(4px);
}

.tag-active {
  background: linear-gradient(135deg, #FF4B2B, #FF416C);
  box-shadow: 0 2px 6px rgba(255, 75, 43, 0.4);
}

.tag-pending {
  background: rgba(250, 139, 23, 0.95);
}

.tag-ended {
  background: rgba(156, 163, 175, 0.95);
}

/* === 卡片主体（紧凑布局） === */
.card-body {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 5px;
}

/* 商品名称（12px, 600, 单行省略） */
.card-name {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* === 价格行 === */
.card-prices {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.price-seckill {
  font-size: 15px;
  font-weight: 700;
  color: #E94560;
  font-family: 'SF Pro Display', 'PingFang SC', sans-serif;
  line-height: 1;
  letter-spacing: -0.02em;
}

.price-original {
  font-size: 11px;
  color: #9CA3AF;
  text-decoration: line-through;
  font-weight: 400;
}

/* === 库存进度条（圆角全圆，高度6px，渐变填充） === */
.stock-bar {
  width: 100%;
  height: 6px;
  background: #f0f0f0;
  border-radius: 99px;
  overflow: hidden;
}

.stock-bar-fill {
  height: 100%;
  border-radius: 99px;
  transition: width 0.3s ease;
}

.stock-bar-fill.high {
  background: linear-gradient(90deg, #FF4B2B, #FF416C);
}

.stock-bar-fill.mid {
  background: linear-gradient(90deg, #FF4B2B, #FF416C);
}

.stock-bar-fill.low {
  background: linear-gradient(90deg, #FF4B2B, #FF416C);
}

/* === 库存文字 === */
.stock-text {
  font-size: 11px;
  color: #6B7280;
  font-weight: 500;
}

.stock-text.danger {
  color: #E94560;
  font-weight: 600;
}

.stock-text.pending-stock {
  color: #6B7280;
}

.stock-text.ended-stock {
  color: #9CA3AF;
}

/* === 操作区 === */
.card-action {
  margin-top: auto;
  padding-top: 4px;
}

/* === 抢购按钮（全宽，32px高，8px圆角） === */
.btn-seckill {
  width: 100%;
  height: 32px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #FF4B2B, #FF416C);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  letter-spacing: 0.04em;
  transition: opacity 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
  box-shadow: 0 2px 8px rgba(255, 75, 43, 0.3);
}

.btn-seckill:hover {
  opacity: 0.92;
  box-shadow: 0 4px 12px rgba(255, 75, 43, 0.4);
}

.btn-seckill:active {
  transform: scale(0.98);
  box-shadow: 0 1px 4px rgba(255, 75, 43, 0.3);
}

/* Pending 状态：浅灰 */
.btn-seckill.btn-pending {
  background: #F3F4F6;
  color: #6B7280;
  box-shadow: none;
}

.btn-seckill.btn-pending:hover {
  background: #E5E7EB;
  box-shadow: none;
}

/* Ended 状态：禁用 */
.btn-seckill.btn-ended {
  background: #F3F4F6;
  color: #D1D5DB;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-seckill.btn-ended:hover {
  opacity: 1;
  background: #F3F4F6;
  box-shadow: none;
}

/* === 场次内空状态 === */
.empty-in-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

/* === 空状态 === */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

/* === 秒杀规则卡片 === */
.seckill-rules {
  margin-top: 24px;
}

.rules-card {
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

.rules-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary, #1a1a1a);
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rules-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #FF4B2B, #FF416C);
  border-radius: 2px;
}

.rules-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary, #6B7280);
  line-height: 1.5;
}

.rule-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-primary-light, rgba(229, 57, 53, 0.08));
  color: var(--color-primary, #E94560);
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* === 响应式 === */
/* 5列: <=1200px */
@media (max-width: 1200px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

/* 4列: <=900px, Hero 改纵向布局 */
@media (max-width: 900px) {
  .seckill-hero {
    flex-direction: column;
  }

  .hero-left {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .rules-list {
    grid-template-columns: 1fr;
  }
}

/* 3列: <=768px */
@media (max-width: 768px) {
  .seckill-zone-page {
    padding: 16px 16px 24px;
  }

  .hero-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .skeleton-card {
    height: 240px;
  }
}

/* 2列: <=480px */
@media (max-width: 480px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
