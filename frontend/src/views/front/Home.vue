<template>
  <div class="page-home">
    <!-- === 1. Banner 轮播区域 (对照 index.html 第766-803行) === -->
    <div class="banner-wrap">
      <div class="banner-row">
        <!-- 左侧分类侧边栏 (淘宝风格: 竖向一级分类, 悬停弹出二级分类大面板) -->
        <aside class="category-sidebar">
          <!-- 滚动容器：包裹分类项，超出可滚动（隐藏滚动条但保留滚动能力） -->
          <div ref="sidebarScrollRef" class="sidebar-scroll">
            <div v-for="cat in categoryTree" :key="cat.id" class="sidebar-item"
              :class="{ 'is-hover': hoverCategoryId === cat.id }" @click="goCategory(cat.id)"
              @mouseenter="handleSidebarEnter(cat.id, $event)" @mouseleave="handleSidebarLeave(cat.id)">
              <span class="sidebar-name">{{ cat.categoryName }}</span>
              <span v-if="cat.children && cat.children.length > 0" class="sidebar-arrow">&#8250;</span>
            </div>

            <!-- 空状态 -->
            <div v-if="categoryTree.length === 0" class="sidebar-empty">暂无分类</div>
          </div>

          <!-- 二级分类浮层提取到外层，不受滚动容器 overflow 裁剪 -->
          <div v-if="hoverPanelData" class="sidebar-panel" :style="{ top: panelTop + 'px' }"
            @mouseenter="handlePanelEnter" @mouseleave="handlePanelLeave">
            <div class="panel-header">
              <span class="panel-title">{{ hoverPanelData.categoryName }}</span>
              <span class="panel-hint">全部分类</span>
            </div>
            <div class="panel-content">
              <div v-for="child in hoverPanelData.children" :key="child.id" class="panel-item"
                @click.stop="goCategory(child.id)">{{ child.categoryName }}</div>
            </div>
          </div>
        </aside>

        <!-- 主 Banner：从 API 获取启用轮播图 -->
        <div class="banner-main">
          <el-carousel v-if="bannerList.length > 0" height="280px" :interval="4000" arrow="hover"
            indicator-position="outside" class="banner-carousel">
            <el-carousel-item v-for="banner in bannerList" :key="banner.id">
              <div class="banner-slide" @click="handleBannerClick(banner)">
                <img :src="formatImageUrl(banner.imageUrl)" :alt="banner.title || ''" class="banner-slide-img"
                  loading="lazy" />
                <div v-if="banner.title" class="banner-slide-title">{{ banner.title }}</div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <!-- 无数据占位（不使用模拟数据） -->
          <div v-else class="banner-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.4)" stroke-width="1"
              class="banner-empty-svg">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="m21 15-5-5L5 21" />
            </svg>
            <div class="banner-empty-text">暂无轮播图</div>
          </div>
        </div>
        <!-- 侧边 Banner：2个小卡片 -->
        <div class="banner-side">
          <div class="side-card dark">
            <div>
              <div class="side-title white">新人专享</div>
              <div class="side-sub muted">首单立减50元</div>
            </div>
          </div>
          <div class="side-card light">
            <div>
              <div class="side-title dark">品牌特卖</div>
              <div class="side-sub gray">Apple 专场 5折起</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- === 2. 限时秒杀区域 (对照 index.html 第835-924行) === -->
    <section ref="seckillSectionRef" class="seckill-zone">
      <div class="zone-header">
        <!-- 闪电图标 -->
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#e53935" stroke-width="2"
          class="zone-lightning">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
        </svg>
        <span class="zone-title">限时秒杀</span>
        <span class="zone-badge">限时抢购</span>
        <!-- 倒计时方块 -->
        <!-- M44 修复: 仅在有进行中秒杀活动时显示倒计时，无数据时不显示硬编码的虚假倒计时 -->
        <div v-if="hasActiveSeckill" class="zone-countdown">
          距结束
          <span class="cd-block">{{ cd.hours }}</span>
          <span class="cd-sep">:</span>
          <span class="cd-block">{{ cd.minutes }}</span>
          <span class="cd-sep">:</span>
          <span class="cd-block">{{ cd.seconds }}</span>
        </div>
        <!-- 右侧更多链接 -->
        <span class="zone-more" @click="router.push('/seckill')">更多秒杀 &gt;</span>
      </div>
      <!-- 2×3 网格纵向紧凑卡片布局，最多展示6个秒杀商品 -->
      <div v-loading="seckillLoading" class="seckill-grid">
        <div v-for="item in seckillItems" :key="item.id" class="sk-card">
          <!-- 顶部：商品图片 -->
          <div class="sk-card-img">
            <img v-if="item.image" :src="formatImageUrl(item.image)" :alt="item.name" class="sk-card-img-tag"
              loading="lazy" />
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
              class="sk-card-placeholder">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="m21 15-5-5L5 21" />
            </svg>
            <!-- 待开始遮罩 -->
            <div v-if="item.pending" class="sk-pending-overlay">
              <span class="sk-pending-badge">即将开始</span>
              <span class="sk-pending-time">{{ item.pendingTime }} 开抢</span>
            </div>
          </div>
          <!-- 底部：商品信息 -->
          <div class="sk-card-body">
            <div class="sk-card-name" :title="item.name">{{ item.name }}</div>
            <div class="sk-card-desc">{{ item.desc }}</div>
            <div class="sk-card-prices">
              <span class="sk-card-price">{{ item.price }}</span>
              <span v-if="item.original" class="sk-card-original">¥{{ item.original }}</span>
            </div>
            <!-- 秒杀进度条 -->
            <div v-if="item.stockPercent !== undefined" class="sk-stock-bar">
              <div class="sk-stock-bar-fill" :class="item.stockLevel" :style="{ width: item.stockPercent + '%' }"></div>
            </div>
            <div class="sk-stock-text" :class="{ danger: item.danger }">{{ item.stockText }}</div>
            <!-- 立即抢购按钮 -->
            <button class="sk-buy-btn" :disabled="item.pending || buyingId === item.id"
              @click.stop="handleSeckillBuy(item)">
              <span v-if="buyingId === item.id" class="sk-btn-loading">下单中...</span>
              <template v-else-if="item.pending">即将开始</template>
              <template v-else>立即抢购</template>
            </button>
          </div>
        </div>
        <!-- 空状态：后端无数据时不使用模拟数据填充 -->
        <el-empty v-if="!seckillLoading && seckillItems.length === 0" description="暂无秒杀活动，敬请期待" :image-size="100"
          class="sk-grid-empty" />
      </div>
    </section>

    <!-- === 4. 猜你喜欢 (对照 index.html 第926-1035行) === -->
    <div class="recommend-wrap">
      <div class="recommend-header">
        <div class="recommend-title">猜你喜欢</div>
        <span class="recommend-refresh" @click="shuffleRecommend">换一换</span>
      </div>
      <!-- 改进3: 热门分类快捷筛选标签 (取一级分类前8个) -->
      <div class="recommend-tags">
        <span class="recommend-tag" :class="{ active: recommendCategoryId === undefined }"
          @click="handleRecommendCategoryClick(undefined)">全部</span>
        <span v-for="cat in recommendCategories" :key="cat.id" class="recommend-tag"
          :class="{ active: recommendCategoryId === cat.id }"
          @click="handleRecommendCategoryClick(cat.id)">{{ cat.categoryName }}</span>
      </div>
      <div v-loading="recommendLoading" class="recommend-grid">
        <div v-for="item in recommendItems" :key="item.id" class="p-card recommend-card" @click="goProductDetail(item.id)">
          <div class="p-card-img recommend-img">
            <img v-if="item.image" :src="formatImageUrl(item.image)" :alt="item.name" class="p-card-img-tag"
              loading="lazy" />
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
              class="p-card-placeholder">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="m21 15-5-5L5 21" />
            </svg>
          </div>
          <div class="p-card-body">
            <div class="p-card-name">{{ item.name }}</div>
            <div class="p-card-prices">
              <span class="p-card-price recommend-price">{{ item.price }}</span>
            </div>
            <div class="stock-text">已售 {{ item.sold }} 件</div>
          </div>
        </div>
        <!-- 空状态：后端无数据时不使用模拟数据填充 -->
        <el-empty v-if="!recommendLoading && recommendItems.length === 0" description="暂无推荐商品" :image-size="100"
          class="grid-empty" />
      </div>
      <!-- 改进5: 无限滚动哨兵元素 + 加载更多 / 没有更多了 提示 -->
      <div v-if="recommendItems.length > 0 && recommendHasMore" ref="recommendSentinel" class="recommend-sentinel">
        <el-icon v-if="recommendLoadingMore" class="is-loading"><Loading /></el-icon>
        <span v-else>滚动加载更多...</span>
      </div>
      <div v-if="!recommendHasMore && recommendItems.length > 0" class="recommend-end">
        <span>没有更多了</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P01 首页 / 秒杀大厅
 * 严格对照 index.html 第766-1035行 page-home 结构 + 第240-294行 CSS
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useSeckillStore } from '@/stores/seckill'
import { useUserStore } from '@/stores/user'
import { getActiveBanners } from '@/api/banner'
import { getCategoryTree } from '@/api/category'
import { getProductList } from '@/api/product'
import { executeSeckill } from '@/api/seckill'
import { formatImageUrl } from '@/utils/image'
import type { SeckillGoodsVO, BannerVO, CategoryTreeNode, ProductVO } from '@/types'

// 显式声明组件名, 使 keep-alive 的 include 匹配生效 (FrontLayout 已将 Home 加入缓存列表)
defineOptions({ name: 'Home' })

const router = useRouter()
const seckillStore = useSeckillStore()
const userStore = useUserStore()

const seckillSectionRef = ref<HTMLElement | null>(null)

/* === 轮播图数据（从 API 获取，不使用模拟数据） === */
const bannerList = ref<BannerVO[]>([])

/* === 分类树数据 (从后端 API 获取, 用于左侧分类侧边栏) === */
const categoryTree = ref<CategoryTreeNode[]>([])

async function fetchCategoryTree(): Promise<void> {
  try {
    const res = await getCategoryTree()
    // 后端返回树形结构, 一级分类对象中包含 children 数组存放二级分类
    categoryTree.value = (res.data as CategoryTreeNode[]) || []
  } catch {
    // 错误已由全局拦截器统一提示
    categoryTree.value = []
  }
}

/** 点击分类跳转商品列表 (URL query 携带 categoryId) */
function goCategory(categoryId: number | string): void {
  router.push({ path: '/products', query: { categoryId: String(categoryId) } })
}

/* === 左侧分类侧边栏 hover 交互（带 200ms 延迟避免快速划过时闪烁） === */
const hoverCategoryId = ref<number | string | null>(null)
let hoverEnterTimer: ReturnType<typeof setTimeout> | null = null
let hoverLeaveTimer: ReturnType<typeof setTimeout> | null = null
const HOVER_DELAY = 200 // ms

/* 浮层提取到外层后，需要滚动容器引用与浮层 top 位置 */
const sidebarScrollRef = ref<HTMLElement | null>(null)
const panelTop = ref<number>(0)

/** 当前 hover 的一级分类对象（含 children），用于外层浮层渲染 */
const hoverPanelData = computed<CategoryTreeNode | null>(() => {
  if (hoverCategoryId.value === null) return null
  const cat = categoryTree.value.find(c => c.id === hoverCategoryId.value)
  if (!cat || !cat.children || cat.children.length === 0) return null
  return cat
})

/** 鼠标进入一级分类项：延迟显示浮层 */
function handleSidebarEnter(categoryId: number | string, event: MouseEvent): void {
  // 取消任何正在等待的隐藏
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
  // 若已显示同一项，直接返回
  if (hoverCategoryId.value === categoryId) return
  // 延迟 200ms 显示，避免快速划过时闪烁
  hoverEnterTimer = setTimeout(() => {
    // 计算浮层 top：hover 项相对于滚动容器的 offsetTop - 滚动容器的 scrollTop
    const item = event.currentTarget as HTMLElement
    const scrollContainer = sidebarScrollRef.value
    if (item && scrollContainer) {
      panelTop.value = item.offsetTop - scrollContainer.scrollTop
    }
    hoverCategoryId.value = categoryId
  }, HOVER_DELAY)
}

/** 鼠标离开一级分类项：延迟隐藏浮层 */
function handleSidebarLeave(categoryId: number | string): void {
  // 取消任何正在等待的显示
  if (hoverEnterTimer) {
    clearTimeout(hoverEnterTimer)
    hoverEnterTimer = null
  }
  // 若不是当前显示项，直接返回
  if (hoverCategoryId.value !== categoryId) return
  // 延迟 200ms 隐藏，给用户时间移到浮层上
  hoverLeaveTimer = setTimeout(() => {
    hoverCategoryId.value = null
  }, HOVER_DELAY)
}

/** 浮层鼠标进入：取消隐藏定时器，保持显示 */
function handlePanelEnter(): void {
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
}

/** 浮层鼠标离开：启动隐藏定时器 */
function handlePanelLeave(): void {
  // 取消任何正在等待的显示
  if (hoverEnterTimer) {
    clearTimeout(hoverEnterTimer)
    hoverEnterTimer = null
  }
  // 延迟 200ms 隐藏
  hoverLeaveTimer = setTimeout(() => {
    hoverCategoryId.value = null
  }, HOVER_DELAY)
}

async function fetchBanners(): Promise<void> {
  try {
    const res = await getActiveBanners()
    bannerList.value = res.data || []
  } catch {
    // 错误已由全局拦截器提示
  }
}

/** 点击轮播图跳转 */
function handleBannerClick(banner: BannerVO): void {
  if (banner.linkUrl) {
    // 外链直接打开，内链走 router
    if (banner.linkUrl.startsWith('http://') || banner.linkUrl.startsWith('https://')) {
      // L-S5 修复: 加 noopener,noreferrer 防止新打开的页面通过 window.opener 访问原页面
      window.open(banner.linkUrl, '_blank', 'noopener,noreferrer')
    } else {
      router.push(banner.linkUrl)
    }
  }
}


/* === 倒计时 === */
interface Countdown {
  hours: string
  minutes: string
  seconds: string
}

const cd = ref<Countdown>({ hours: '00', minutes: '00', seconds: '00' })
let cdTimer: ReturnType<typeof setInterval> | null = null
// M44 修复: 移除硬编码的虚假倒计时目标，改为仅在有 ACTIVE 活动时计算
let cdTarget = 0

/**
 * M44 修复: 是否存在进行中的秒杀活动
 * 仅在有 ACTIVE 活动时才显示倒计时，避免无数据时显示虚假倒计时误导用户
 */
const hasActiveSeckill = computed<boolean>(() => {
  return seckillStore.activeList.length > 0
})

function updateCountdown(): void {
  // 若 store 有 ACTIVE 数据，使用第一个活动的 endTime
  const firstActive = seckillStore.activeList[0]
  if (firstActive) {
    cdTarget = new Date(firstActive.endTime).getTime()
  } else {
    // M44 修复: 无 ACTIVE 活动时重置倒计时为 00:00:00，不显示虚假倒计时
    cd.value = { hours: '00', minutes: '00', seconds: '00' }
    return
  }
  let remain = Math.max(0, Math.floor((cdTarget - Date.now()) / 1000))
  const hours = Math.floor(remain / 3600)
  remain -= hours * 3600
  const minutes = Math.floor(remain / 60)
  const seconds = remain - minutes * 60
  cd.value = {
    hours: String(hours).padStart(2, '0'),
    minutes: String(minutes).padStart(2, '0'),
    seconds: String(seconds).padStart(2, '0')
  }
}

/* === 秒杀商品卡片数据 === */
interface SeckillCardItem {
  id: number | string
  name: string
  desc: string
  price: number
  original?: number
  image?: string
  stockPercent?: number
  stockLevel?: 'high' | 'mid' | 'low'
  stockText: string
  danger?: boolean
  pending?: boolean
  pendingStart?: string
  pendingTime?: string
}

/** 格式化时间为 MM-DD HH:mm */
function formatStartTime(time: string): string {
  // 简单格式化，避免引入 dayjs 依赖
  const d = new Date(time)
  if (isNaN(d.getTime())) return ''
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

/** 把 store 中的 SeckillGoodsVO 转为卡片数据 */
function toCardItem(item: SeckillGoodsVO): SeckillCardItem {
  const total = item.stockCount || 0
  const available = item.availableCount || 0
  const sold = total - available
  const soldPercent = total > 0 ? Math.round((sold / total) * 100) : 0
  // 库存等级：>60 high, >25 mid, else low
  let level: 'high' | 'mid' | 'low' = 'low'
  if (soldPercent >= 60) level = 'high'
  else if (soldPercent >= 25) level = 'mid'
  const isPending = item.status === 'PENDING'
  const isLowStock = available <= 5 && item.status === 'ACTIVE'
  const price = item.seckillPrice || 0
  return {
    id: item.id,
    name: item.productName || item.seckillName || '未命名商品',
    desc: item.seckillName || item.description || '限时秒杀 · 手慢无',
    price,
    // H25 修复: 不再伪造原价 (原价近似 price/0.7 误导消费者，违反价格法)
    // 原价应来自后端 originalPrice 字段，无字段时不显示
    original: undefined,
    image: item.images?.[0],
    stockPercent: isPending ? undefined : soldPercent,
    stockLevel: isPending ? undefined : level,
    stockText: isPending
      ? `限量${total}件 · 每人限购${item.perLimit}件`
      : isLowStock
        ? `仅剩${available}件！手慢无`
        : `已抢${soldPercent}% · 剩余${available}件`,
    danger: isLowStock,
    pending: isPending,
    pendingStart: formatStartTime(item.startTime),
    pendingTime: formatStartTime(item.startTime)
  }
}

/* === 秒杀列表 loading 状态 === */
const seckillLoading = ref<boolean>(false)

const seckillItems = computed<SeckillCardItem[]>(() => {
  // 全部从后端 store 获取，禁止使用模拟数据
  // 优先展示进行中(ACTIVE)的秒杀，再补充待开始(PENDING)，最多取6个用于2×3网格
  const list = [...seckillStore.activeList, ...seckillStore.pendingList]
  return list.slice(0, 6).map(toCardItem)
})

/* === 秒杀下单：弹出确认框 → 调用 executeSeckill === */
const buyingId = ref<number | string | null>(null)

async function handleSeckillBuy(item: SeckillCardItem): Promise<void> {
  // 1. 登录检查
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再参与秒杀')
    router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
    return
  }

  // 2. 弹出确认框
  try {
    await ElMessageBox.confirm(
      `确认以 ¥${item.price} 抢购「${item.name}」？`,
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

  // 3. 调用秒杀下单 API
  buyingId.value = item.id
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
    // M6 修复: 无论何种状态（包括排队中 status===0），都刷新秒杀列表，
    // 让前端展示的 availableCount / 已抢 / 剩余 与后端保持同步
    await fetchList()
  } catch {
    // 错误已由全局拦截器统一提示
  } finally {
    buyingId.value = null
  }
}

/* === 猜你喜欢（从后端 /api/v1/products 获取，按销量排序） === */
interface RecommendItem {
  id: number | string
  name: string
  price: number
  sold: number
  image?: string
}

const recommendItems = ref<RecommendItem[]>([])
const recommendLoading = ref<boolean>(false)

/* 改进3: 分类筛选 - 当前选中的分类 id (undefined 表示全部) */
const recommendCategoryId = ref<number | string | undefined>(undefined)
/* 取前 8 个一级分类作为快捷标签 */
const recommendCategories = computed<CategoryTreeNode[]>(() => (categoryTree.value || []).slice(0, 8))

/* 改进5: 无限滚动分页状态 */
const recommendPageNum = ref<number>(1)
const recommendHasMore = ref<boolean>(true)
const recommendLoadingMore = ref<boolean>(false)
/* 哨兵元素引用，用于 IntersectionObserver 监听 */
const recommendSentinel = ref<HTMLElement | null>(null)
let recommendObserver: IntersectionObserver | null = null

/** 改进1+3+5: 从后端 API 获取推荐商品列表（按销量降序，pageSize=30，支持分类筛选，重置分页） */
async function fetchRecommendProducts(): Promise<void> {
  recommendLoading.value = true
  // 重置分页状态（首次加载或切换分类时调用）
  recommendPageNum.value = 1
  recommendHasMore.value = true
  try {
    const params: any = {
      pageNum: 1,
      pageSize: 30,
      status: 'ON_SALE',
      sortBy: 'salesCount',
      sortOrder: 'desc'
    }
    if (recommendCategoryId.value !== undefined) {
      params.categoryId = recommendCategoryId.value
    }
    const res = await getProductList(params)
    const list = res.data.list || []
    recommendItems.value = list.map((p: ProductVO) => ({
      id: p.id,
      name: p.productName,
      price: p.originalPrice || 0,
      sold: p.salesCount || 0,
      image: p.images?.[0]
    }))
    // 不足 30 个说明没有更多了
    if (list.length < 30) recommendHasMore.value = false
  } catch {
    // 错误已由全局拦截器统一提示
    recommendItems.value = []
    recommendHasMore.value = false
  } finally {
    recommendLoading.value = false
  }
}

/** 改进5: 滚动到底部加载更多 */
async function loadMoreRecommend(): Promise<void> {
  if (recommendLoadingMore.value || !recommendHasMore.value || recommendLoading.value) return
  recommendLoadingMore.value = true
  recommendPageNum.value++
  try {
    const params: any = {
      pageNum: recommendPageNum.value,
      pageSize: 30,
      status: 'ON_SALE',
      sortBy: 'salesCount',
      sortOrder: 'desc'
    }
    if (recommendCategoryId.value !== undefined) params.categoryId = recommendCategoryId.value
    const res = await getProductList(params)
    const list = res.data.list || []
    if (list.length === 0) {
      recommendHasMore.value = false
    } else {
      const newItems = list.map((p: ProductVO) => ({
        id: p.id,
        name: p.productName,
        price: p.originalPrice || 0,
        sold: p.salesCount || 0,
        image: p.images?.[0]
      }))
      recommendItems.value.push(...newItems)
      if (list.length < 30) recommendHasMore.value = false
    }
  } catch {
    // 失败时回退页码，下次可重试
    recommendPageNum.value--
  } finally {
    recommendLoadingMore.value = false
  }
}

/** 改进3: 点击分类标签切换分类并重新拉取 */
function handleRecommendCategoryClick(catId: number | string | undefined): void {
  if (recommendCategoryId.value === catId) return
  recommendCategoryId.value = catId
  fetchRecommendProducts()
}

/** 改进2: 换一换 - Fisher-Yates 随机洗牌算法 */
function shuffleRecommend(): void {
  if (recommendItems.value.length <= 1) return
  const arr = [...recommendItems.value]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  recommendItems.value = arr
}


/* === 跳转商品详情 === */
function goProductDetail(id: number | string): void {
  router.push(`/products/${id}`)
}


/* === 拉取秒杀列表（从后端 /api/v1/seckill/list 获取，不使用模拟数据） === */
async function fetchList(): Promise<void> {
  seckillLoading.value = true
  try {
    await seckillStore.fetchSeckillList({ pageNum: 1, pageSize: 20 })
  } catch {
    // 错误已由拦截器处理
  } finally {
    seckillLoading.value = false
  }
}

/* === 静默刷新秒杀列表：不触发 loading 状态，避免定时轮询导致界面闪烁 === */
async function silentRefreshSeckill(): Promise<void> {
  try {
    await seckillStore.fetchSeckillList({ pageNum: 1, pageSize: 20 })
  } catch {
    // 错误已由拦截器处理
  }
}

// M6 修复: 秒杀列表定时轮询定时器，每 8 秒刷新一次，
// 让前端展示的 availableCount / 已抢 / 剩余 与后端保持同步
// 使用静默刷新避免 loading 遮罩反复闪烁
let seckillRefreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchBanners()
  fetchCategoryTree()
  fetchList()
  fetchRecommendProducts()
  updateCountdown()
  cdTimer = setInterval(updateCountdown, 1000)
  // 启动秒杀列表定时轮询（静默刷新，不闪烁）
  seckillRefreshTimer = setInterval(() => {
    silentRefreshSeckill()
  }, 8000)
  // 改进5: 初始化无限滚动 IntersectionObserver
  // 哨兵元素带 v-if 条件，初始可能未渲染，使用 watch 监听其出现/消失动态 observe/unobserve
  recommendObserver = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && recommendItems.value.length > 0) {
      loadMoreRecommend()
    }
  }, { rootMargin: '200px' })
  watch(recommendSentinel, (el, oldEl) => {
    if (oldEl && recommendObserver) recommendObserver.unobserve(oldEl)
    if (el && recommendObserver) recommendObserver.observe(el)
  })
})

onUnmounted(() => {
  if (cdTimer) clearInterval(cdTimer)
  if (seckillRefreshTimer) {
    clearInterval(seckillRefreshTimer)
    seckillRefreshTimer = null
  }
  if (hoverEnterTimer) clearTimeout(hoverEnterTimer)
  if (hoverLeaveTimer) clearTimeout(hoverLeaveTimer)
  seckillStore.stopAllCountdowns()
  // 改进5: 清理无限滚动 Observer
  if (recommendObserver) {
    recommendObserver.disconnect()
    recommendObserver = null
  }
})
</script>

<style scoped>
/* === 页面根 === */
.page-home {
  padding-bottom: 24px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  color: #1a1a2e;
}

/* === 1. Banner 轮播区域 (对照 index.html 第766-803行) === */
.banner-wrap {
  padding: 16px 24px 0;
}

.banner-row {
  display: flex;
  gap: 12px;
}

/* === 左侧分类侧边栏 (淘宝风格) === */
.category-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-sizing: border-box;
  /* 高度与 banner 主图对齐 (280px) */
  height: 280px;
  /* overflow: visible 保证 sidebar-panel 二级分类浮层不被父容器裁剪。
     分类项滚动由内部 .sidebar-scroll 容器承担，浮层置于该滚动容器外。 */
  overflow: visible;
  position: relative;
}

/* 滚动容器：包裹分类项，超出可滚动；隐藏滚动条但保留滚动能力 */
.sidebar-scroll {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
  box-sizing: border-box;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.sidebar-scroll::-webkit-scrollbar {
  display: none;
}

/* 一级分类项 */
.sidebar-item {
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background 0.15s, color 0.15s;
  box-sizing: border-box;
}

/* hover 状态由 JS 控制 is-hover 类，避免纯 CSS :hover 与延迟逻辑冲突 */
.sidebar-item:hover,
.sidebar-item.is-hover {
  background: #e53935;
  color: #ffffff;
}

.sidebar-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.sidebar-arrow {
  color: #9ca3af;
  font-size: 14px;
  margin-left: 6px;
  flex-shrink: 0;
  transition: color 0.15s;
}

.sidebar-item:hover .sidebar-arrow,
.sidebar-item.is-hover .sidebar-arrow {
  color: #ffffff;
}

/* 二级分类浮层: 悬停一级分类时右侧弹出大面板 (从左侧滑出动画)。
   浮层已提取到 .category-sidebar 直接子元素，top 由 JS 动态计算 */
.sidebar-panel {
  position: absolute;
  left: 100%;
  width: 400px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
  padding: 16px 20px;
  /* z-index 跳够高，不被轮播图遮挡 */
  z-index: 100;
  box-sizing: border-box;
  /* 从左侧滑出 + 淡入动画 */
  transform: translateX(-10px);
  opacity: 0;
  animation: sidebar-panel-slide-in 0.2s ease forwards;
}

@keyframes sidebar-panel-slide-in {
  from {
    transform: translateX(-10px);
    opacity: 0;
  }

  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 浮层头部 */
.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
}

.panel-hint {
  font-size: 12px;
  color: #9ca3af;
}

/* 二级分类项: 用 flex-wrap 自动换行排列 */
.panel-content {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 8px;
}

.panel-item {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: #4b5563;
  cursor: pointer;
  padding: 5px 12px;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
  white-space: nowrap;
  line-height: 1.4;
  /* hover 高亮效果 */
  background: transparent;
}

.panel-item:hover {
  color: #e53935;
  background: #fce8e8;
}

.sidebar-empty {
  padding: 24px 16px;
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
}

/* 主 Banner：轮播图容器 */
.banner-main {
  flex: 1;
  height: 280px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e53935, #ff6d00);
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
}

/* el-carousel 填满 banner-main */
.banner-carousel {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

/* 轮播图每一项 */
.banner-slide {
  width: 100%;
  height: 100%;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.banner-slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 轮播图标题覆盖层 */
.banner-slide-title {
  position: absolute;
  left: 24px;
  bottom: 24px;
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  z-index: 2;
}

/* 无数据占位 */
.banner-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.banner-empty-svg {
  width: 64px;
  height: 64px;
}

.banner-empty-text {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  font-weight: 600;
}

/* 侧边 Banner */
.banner-side {
  width: 220px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex-shrink: 0;
}

.side-card {
  flex: 1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  text-align: center;
  box-sizing: border-box;
}

.side-card.dark {
  background: #1a1a2e;
}

.side-card.light {
  background: #f8f8f8;
  border: 1px solid #e5e7eb;
}

.side-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
}

.side-title.white {
  color: #ffffff;
}

.side-title.dark {
  color: #1a1a2e;
}

.side-sub {
  font-size: 13px;
}

.side-sub.muted {
  color: #a6adc8;
}

.side-sub.gray {
  color: #6b7280;
}

/* === 2. 限时秒杀区域 (对照 index.html 第835-924行 + CSS 第240-294行) === */
.seckill-zone {
  margin-top: 20px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-left: 24px;
  margin-right: 24px;
  padding: 16px 20px;
  box-sizing: border-box;
}

.zone-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.zone-lightning {
  flex-shrink: 0;
}

.zone-title {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: #1a1a2e;
}

.zone-badge {
  background: #e53935;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 3px;
  letter-spacing: 0.02em;
}

.zone-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.cd-block {
  background: #1a1a2e;
  color: #ffffff;
  font-family: 'DIN Alternate', 'Roboto', 'Arial', sans-serif;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 14px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
  box-sizing: border-box;
}

.cd-sep {
  font-weight: 700;
  color: #1a1a2e;
}

.zone-more {
  margin-left: auto;
  font-size: 12px;
  color: #e53935;
  cursor: pointer;
  font-weight: 600;
}

.zone-more:hover {
  text-decoration: underline;
}

/* === 2×3 秒杀网格 + 纵向紧凑卡片 === */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

/* 空状态在网格容器中占满整行居中显示 */
.sk-grid-empty {
  grid-column: 1 / -1;
  margin: 32px auto;
}

/* 单个秒杀卡片：左图右信息水平布局 */
.sk-card {
  display: flex;
  flex-direction: row;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
  box-sizing: border-box;
}

.sk-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

/* 左侧图片：水平布局固定宽度160px，固定高度160px避免拉伸 */
.sk-card-img {
  width: 160px;
  height: 160px;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
  align-self: center;
}

.sk-card-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sk-card-placeholder {
  width: 48px;
  height: 48px;
  color: #ccc;
}

/* 待开始遮罩 */
.sk-pending-overlay {
  position: absolute;
  inset: 0;
  background: rgba(26, 26, 46, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
}

.sk-pending-badge {
  background: #ff6d00;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 3px;
}

.sk-pending-time {
  font-size: 12px;
  color: #ffffff;
  font-weight: 600;
}

/* 右侧信息区：水平布局 flex:1 占满剩余空间，纵向排列内容 */
.sk-card-body {
  flex: 1;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  min-width: 0;
}

.sk-card-name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #1a1a2e;
}

.sk-card-desc {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sk-card-prices {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}

.sk-card-price {
  font-family: 'DIN Alternate', 'Roboto', 'Arial', sans-serif;
  font-size: 20px;
  font-weight: 800;
  color: #e53935;
  line-height: 1;
}

/* ¥ 前缀 */
.sk-card-price::before {
  content: '\A5';
  font-size: 14px;
}

.sk-card-original {
  font-size: 13px;
  color: #9ca3af;
  text-decoration: line-through;
}

/* 秒杀进度条 */
.sk-stock-bar {
  height: 6px;
  background: #eee;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.sk-stock-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.sk-stock-bar-fill.high {
  background: #4caf50;
}

.sk-stock-bar-fill.mid {
  background: #ff9800;
}

.sk-stock-bar-fill.low {
  background: #e53935;
}

.sk-stock-text {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.sk-stock-text.danger {
  color: #e53935;
  font-weight: 700;
}

/* 立即抢购按钮：自适应宽度按钮 */
.sk-buy-btn {
  margin-top: auto;
  width: auto;
  align-self: flex-start;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #e53935, #ff6d00);
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.1s;
  box-sizing: border-box;
}

.sk-buy-btn:hover:not(:disabled) {
  opacity: 0.92;
}

.sk-buy-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.sk-buy-btn:disabled {
  background: #9ca3af;
  cursor: not-allowed;
  opacity: 0.7;
}

.sk-btn-loading {
  display: inline-block;
}

/* === 商品卡片 p-card 通用样式 (猜你喜欢区域使用) === */
.p-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
  cursor: pointer;
  box-sizing: border-box;
}

.p-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.p-card-img {
  width: 100%;
  height: 160px;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.p-card-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.p-card-placeholder {
  width: 48px;
  height: 48px;
  color: #ccc;
}

.p-card-body {
  padding: 12px;
  box-sizing: border-box;
}

.p-card-name {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #1a1a2e;
}

.p-card-prices {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}

.p-card-price {
  font-family: 'DIN Alternate', 'Roboto', 'Arial', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #e53935;
}

/* ¥ 前缀 */
.p-card-price::before {
  content: '\A5';
  font-size: 12px;
}

.stock-text {
  font-size: 11px;
  color: #6b7280;
}

/* === 4. 猜你喜欢 (对照 index.html 第926-1035行) === */
.recommend-wrap {
  margin-top: 20px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-left: 24px;
  margin-right: 24px;
  padding: 16px 20px;
  box-sizing: border-box;
}

.recommend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.recommend-title {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: #1a1a2e;
}

.recommend-refresh {
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
}

.recommend-refresh:hover {
  color: #e53935;
}

/* 改进3: 分类筛选标签区域 */
.recommend-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.recommend-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  font-size: 12px;
  color: #4b5563;
  background: #f3f4f6;
  border-radius: 12px;
  cursor: pointer;
  transition: color 0.15s, background 0.15s, border-color 0.15s;
  border: 1px solid transparent;
  user-select: none;
}

.recommend-tag:hover {
  color: #e53935;
  background: #fce8e8;
}

.recommend-tag.active {
  color: #ffffff;
  background: #e53935;
  border-color: #e53935;
}

/* 改进4: 瀑布流多列布局 (CSS columns) */
.recommend-grid {
  column-count: 5;
  column-gap: 12px;
}

/* 瀑布流中的卡片：避免被列分割，宽度占满单列 */
.recommend-grid .recommend-card {
  break-inside: avoid;
  display: inline-block;
  width: 100%;
  margin-bottom: 12px;
  /* 换一换时的淡入动画 */
  animation: recommend-fade-in 0.3s ease;
}

@keyframes recommend-fade-in {
  from {
    opacity: 0.4;
    transform: translateY(4px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 空状态在网格容器中占满整行居中显示 */
.grid-empty {
  grid-column: 1 / -1;
  margin: 32px auto;
}

/* 改进1: 猜你喜欢卡片图片高度 160px (瀑布流下仍保持图片高度一致) */
.recommend-img {
  height: 160px;
}

/* 猜你喜欢价格用深色 */
.recommend-price {
  color: #1a1a2e;
}

.recommend-price::before {
  content: '\A5';
  font-size: 12px;
}

/* 改进5: 无限滚动哨兵元素 + 没有更多了 提示 */
.recommend-sentinel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0 8px;
  font-size: 12px;
  color: #9ca3af;
  gap: 6px;
}

.recommend-sentinel .is-loading {
  font-size: 14px;
  color: #e53935;
}

.recommend-end {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0 8px;
  font-size: 12px;
  color: #9ca3af;
}

.recommend-end span {
  position: relative;
  padding: 0 12px;
}

.recommend-end span::before,
.recommend-end span::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40px;
  height: 1px;
  background: #e5e7eb;
}

.recommend-end span::before {
  right: 100%;
}

.recommend-end span::after {
  left: 100%;
}

/* === 响应式：秒杀网格在小屏下退化为 2 列，超小屏退化为 1 列 === */
@media (max-width: 1024px) {
  /* 改进4: 瀑布流在中等屏幕下 4 列 */
  .recommend-grid {
    column-count: 4;
  }
}

@media (max-width: 768px) {
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  /* 小屏下图片宽度减小到 120px */
  .sk-card-img {
    width: 120px;
    height: 120px;
  }

  /* 改进4: 瀑布流在小屏下 3 列 */
  .recommend-grid {
    column-count: 3;
  }
}

@media (max-width: 480px) {
  .seckill-grid {
    grid-template-columns: 1fr;
  }

  /* 超小屏下图片宽度进一步减小到 120px */
  .sk-card-img {
    width: 120px;
    height: 120px;
  }

  /* 改进4: 瀑布流在超小屏下 2 列 */
  .recommend-grid {
    column-count: 2;
  }
}
</style>
