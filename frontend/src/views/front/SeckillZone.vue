<template>
  <div class="seckill-zone-page">

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
/*
 * 秒杀专区现代化UI样式
 * 设计参考: Material Design 3 + Apple HIG
 * 主色调: #FF4B2B → #FF416C 渐变 (秒杀主题)
 * 强调色: #E94560 (秒杀价)
 * 间距系统: 8pt 网格
 */

/* === 页面根 === */
.seckill-zone-page {
  padding: 24px 24px 32px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  color: var(--color-text-primary);
  box-sizing: border-box;
}

/* === 内容区 === */
.zone-content {
  width: 100%;
  box-sizing: border-box;
}

/* === 骨架屏 === */
.skeleton-section-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.skeleton-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.skeleton-section-header {
  height: 28px;
  width: 220px;
  margin-bottom: 16px;
  border-radius: 8px;
  background-image: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.skeleton-card {
  height: 360px;
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

/* === 场次区块（白色圆角卡片） === */
.activity-section {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
  transition: box-shadow 0.2s ease;
}

/* H-F3 修复: 旧版秒杀数据区块样式 (复用场次区块样式) */
.legacy-section {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
}

/* === 场次头部 === */
.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.activity-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.activity-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

/* 场次状态标签 */
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

.activity-time {
  font-size: 12px;
  color: #9CA3AF;
  font-weight: 500;
}

/* === 场次倒计时（紧凑数字块设计） === */
.activity-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
}

.activity-countdown .cd-label {
  font-size: 12px;
  color: #6B7280;
  margin-right: 6px;
  font-weight: 500;
}

.activity-countdown .cd-block {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 4px;
  text-align: center;
  background: #1a1a1a;
  color: #fff;
  border-radius: 6px;
  font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.activity-countdown .cd-sep {
  color: #1a1a1a;
  font-weight: 700;
  font-size: 13px;
}

.activity-desc {
  margin: 12px 0 0;
  font-size: 13px;
  color: #6B7280;
  line-height: 1.5;
}

/* === 秒杀卡片网格（4列桌面 / 3列平板 / 2列手机 / 1列小手机） === */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 16px;
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

/* === 卡片图片（固定高度200px） === */
.card-img {
  position: relative;
  width: 100%;
  height: 200px;
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
  padding: 4px 10px;
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

/* === 卡片主体 === */
.card-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 8px;
}

/* 商品名称（14px, 600, 单行省略） */
.card-name {
  font-size: 14px;
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
  gap: 8px;
}

.price-seckill {
  font-size: 18px;
  font-weight: 700;
  color: #E94560;
  font-family: 'SF Pro Display', 'PingFang SC', sans-serif;
  line-height: 1;
  letter-spacing: -0.02em;
}

.price-original {
  font-size: 12px;
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

/* === 抢购按钮（全宽，40px高，8px圆角） === */
.btn-seckill {
  width: 100%;
  height: 40px;
  font-size: 14px;
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
  padding: 32px;
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
/* 3列: 768-1024px */
@media (max-width: 1024px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 2列: 480-768px */
@media (max-width: 768px) {
  .seckill-zone-page {
    padding: 16px 16px 24px;
  }

  .activity-section,
  .legacy-section {
    padding: 16px 18px;
  }

  .activity-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .skeleton-card {
    height: 320px;
  }
}

/* 1列: <480px */
@media (max-width: 480px) {

  .skeleton-grid,
  .seckill-grid {
    grid-template-columns: 1fr;
  }
}
</style>
