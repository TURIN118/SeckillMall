<template>
  <div class="page-home">
    <!-- === 1. Banner 轮播区域 (对照 index.html 第766-803行) === -->
    <div class="banner-wrap">
      <div class="banner-row">
        <!-- 左侧分类侧边栏 (淘宝风格: 竖向一级分类, 悬停弹出二级分类大面板) -->
        <aside class="category-sidebar">
          <div
            v-for="cat in categoryTree"
            :key="cat.id"
            class="sidebar-item"
            @click="goCategory(cat.id)"
          >
            <span class="sidebar-name">{{ cat.categoryName }}</span>
            <span v-if="cat.children && cat.children.length > 0" class="sidebar-arrow">&#8250;</span>

            <!-- 二级分类浮层 (悬停时右侧弹出大面板) -->
            <div
              v-if="cat.children && cat.children.length > 0"
              class="sidebar-panel"
            >
              <div class="panel-header">
                <span class="panel-title">{{ cat.categoryName }}</span>
                <span class="panel-hint">全部分类</span>
              </div>
              <div class="panel-content">
                <div
                  v-for="child in cat.children"
                  :key="child.id"
                  class="panel-item"
                  @click.stop="goCategory(child.id)"
                >{{ child.categoryName }}</div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="categoryTree.length === 0" class="sidebar-empty">暂无分类</div>
        </aside>

        <!-- 主 Banner：从 API 获取启用轮播图 -->
        <div class="banner-main">
          <el-carousel
            v-if="bannerList.length > 0"
            height="280px"
            :interval="4000"
            arrow="hover"
            indicator-position="outside"
            class="banner-carousel"
          >
            <el-carousel-item v-for="banner in bannerList" :key="banner.id">
              <div class="banner-slide" @click="handleBannerClick(banner)">
                <img
                  :src="banner.imageUrl"
                  :alt="banner.title || ''"
                  class="banner-slide-img"
                  loading="lazy"
                />
                <div v-if="banner.title" class="banner-slide-title">{{ banner.title }}</div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <!-- 无数据占位（不使用模拟数据） -->
          <div v-else class="banner-empty">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="rgba(255,255,255,0.4)"
              stroke-width="1"
              class="banner-empty-svg"
            >
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

    <!-- === 2. 分类快捷导航 (对照 index.html 第805-833行) === -->
    <div class="category-wrap">
      <div class="category-row">
        <div
          v-for="(cat, idx) in categories"
          :key="cat.name"
          class="category-item"
          :class="{ 'no-border': idx === categories.length - 1 }"
          @click="router.push('/products')"
        >
          <svg viewBox="0 0 24 24" fill="none" :stroke="cat.color" stroke-width="1.8" class="category-icon">
            <component :is="cat.svg" />
          </svg>
          <div class="category-name">{{ cat.name }}</div>
        </div>
      </div>
    </div>

    <!-- === 3. 限时秒杀区域 (对照 index.html 第835-924行) === -->
    <section ref="seckillSectionRef" class="seckill-zone">
      <div class="zone-header">
        <!-- 闪电图标 -->
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#e53935" stroke-width="2" class="zone-lightning">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
        </svg>
        <span class="zone-title">限时秒杀</span>
        <span class="zone-badge">限时抢购</span>
        <!-- 倒计时方块 -->
        <div class="zone-countdown">
          距结束
          <span class="cd-block">{{ cd.hours }}</span>
          <span class="cd-sep">:</span>
          <span class="cd-block">{{ cd.minutes }}</span>
          <span class="cd-sep">:</span>
          <span class="cd-block">{{ cd.seconds }}</span>
        </div>
        <!-- 右侧更多链接 -->
        <span class="zone-more" @click="router.push('/products')">更多秒杀 &gt;</span>
      </div>
      <!-- 商品横向滚动 -->
      <div v-loading="seckillLoading" class="product-scroll">
        <div
          v-for="item in seckillItems"
          :key="item.id"
          class="p-card"
          @click="goSeckillDetail(item.id)"
        >
          <div class="p-card-img">
            <img v-if="item.image" :src="item.image" :alt="item.name" class="p-card-img-tag" loading="lazy" />
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="p-card-placeholder">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="m21 15-5-5L5 21" />
            </svg>
            <!-- 待开始遮罩 -->
            <div v-if="item.pending" class="pending-overlay">
              <span>距开始 {{ item.pendingStart }}</span>
              <span class="pending-sub">{{ item.pendingTime }} 开抢</span>
            </div>
          </div>
          <div class="p-card-body">
            <div class="p-card-name">{{ item.name }}</div>
            <div class="p-card-prices">
              <span class="p-card-price">{{ item.price }}</span>
              <span v-if="item.original" class="p-card-original">¥{{ item.original }}</span>
            </div>
            <div v-if="item.stockPercent !== undefined" class="stock-bar">
              <div class="stock-bar-fill" :class="item.stockLevel" :style="{ width: item.stockPercent + '%' }"></div>
            </div>
            <div class="stock-text" :class="{ danger: item.danger }">{{ item.stockText }}</div>
          </div>
        </div>
        <!-- 空状态：后端无数据时不使用模拟数据填充 -->
        <el-empty
          v-if="!seckillLoading && seckillItems.length === 0"
          description="暂无秒杀活动，敬请期待"
          :image-size="100"
          class="scroll-empty"
        />
      </div>
    </section>

    <!-- === 4. 猜你喜欢 (对照 index.html 第926-1035行) === -->
    <div class="recommend-wrap">
      <div class="recommend-header">
        <div class="recommend-title">猜你喜欢</div>
        <span class="recommend-refresh" @click="shuffleRecommend">换一换</span>
      </div>
      <div v-loading="recommendLoading" class="recommend-grid">
        <div
          v-for="item in recommendItems"
          :key="item.id"
          class="p-card"
          @click="goProductDetail(item.id)"
        >
          <div class="p-card-img recommend-img">
            <img v-if="item.image" :src="item.image" :alt="item.name" class="p-card-img-tag" loading="lazy" />
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="p-card-placeholder">
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
        <el-empty
          v-if="!recommendLoading && recommendItems.length === 0"
          description="暂无推荐商品"
          :image-size="100"
          class="grid-empty"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P01 首页 / 秒杀大厅
 * 严格对照 index.html 第766-1035行 page-home 结构 + 第240-294行 CSS
 */
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useSeckillStore } from '@/stores/seckill'
import { getActiveBanners } from '@/api/banner'
import { getCategoryTree } from '@/api/category'
import { getProductList } from '@/api/product'
import type { SeckillGoodsVO, BannerVO, CategoryTreeNode, ProductVO } from '@/types'

const router = useRouter()
const seckillStore = useSeckillStore()

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
function goCategory(categoryId: number): void {
  router.push({ path: '/products', query: { categoryId: String(categoryId) } })
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
      window.open(banner.linkUrl, '_blank')
    } else {
      router.push(banner.linkUrl)
    }
  }
}

/* === 分类快捷导航 SVG 内容 === */
const svgPhone = () => [h('rect', { x: 5, y: 2, width: 14, height: 20, rx: 2 }), h('path', { d: 'M12 18h.01' })]
const svgComputer = () => [h('rect', { x: 2, y: 3, width: 20, height: 14, rx: 2 }), h('path', { d: 'M8 21h8M12 17v4' })]
const svgHome = () => [h('path', { d: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z' }), h('path', { d: 'M9 22V12h6v10' })]
const svgGame = () => [h('circle', { cx: 12, cy: 12, r: 10 }), h('path', { d: 'M8 14s1.5 2 4 2 4-2 4-2' }), h('path', { d: 'M9 9h.01M15 9h.01' })]
const svgBeauty = () => [h('path', { d: 'M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z' })]
const svgBag = () => [h('path', { d: 'M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z' }), h('path', { d: 'M3 6h18' }), h('path', { d: 'M16 10a4 4 0 01-8 0' })]

interface CategoryItem {
  name: string
  color: string
  svg: () => ReturnType<typeof h>[]
}

const categories: CategoryItem[] = [
  { name: '手机数码', color: '#e53935', svg: svgPhone },
  { name: '电脑办公', color: '#ff6d00', svg: svgComputer },
  { name: '家用电器', color: '#4caf50', svg: svgHome },
  { name: '游戏娱乐', color: '#1976d2', svg: svgGame },
  { name: '美妆个护', color: '#e53935', svg: svgBeauty },
  { name: '服饰鞋包', color: '#ff6d00', svg: svgBag }
]

/* === 倒计时 === */
interface Countdown {
  hours: string
  minutes: string
  seconds: string
}

const cd = ref<Countdown>({ hours: '02', minutes: '34', seconds: '56' })
let cdTimer: ReturnType<typeof setInterval> | null = null
// 默认倒计时目标：当前时间 + 2小时34分56秒
let cdTarget = Date.now() + (2 * 3600 + 34 * 60 + 56) * 1000

function updateCountdown(): void {
  // 若 store 有 ACTIVE 数据，使用第一个活动的 endTime
  const firstActive = seckillStore.activeList[0]
  if (firstActive) {
    cdTarget = new Date(firstActive.endTime).getTime()
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
  id: number
  name: string
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
    price,
    original: price > 0 ? Math.round(price / 0.7) : undefined, // 原价近似（无字段时）
    image: item.images?.[0],
    stockPercent: isPending ? undefined : soldPercent,
    stockLevel: isPending ? undefined : level,
    stockText: isPending
      ? `限量${total}件`
      : isLowStock
        ? `仅剩${available}件！手慢无`
        : `已抢${soldPercent}% · 剩余${available}件`,
    danger: isLowStock,
    pending: isPending,
    pendingStart: '15:30',
    pendingTime: '20:00'
  }
}

/* === 秒杀列表 loading 状态 === */
const seckillLoading = ref<boolean>(false)

const seckillItems = computed<SeckillCardItem[]>(() => {
  // 全部从后端 store 获取，禁止使用模拟数据
  const list = [...seckillStore.activeList, ...seckillStore.pendingList]
  return list.map(toCardItem)
})

/* === 猜你喜欢（从后端 /api/v1/products 获取，按销量排序） === */
interface RecommendItem {
  id: number
  name: string
  price: number
  sold: number
  image?: string
}

const recommendItems = ref<RecommendItem[]>([])
const recommendLoading = ref<boolean>(false)

/** 从后端 API 获取推荐商品列表（按销量降序排列，取前 12 个） */
async function fetchRecommendProducts(): Promise<void> {
  recommendLoading.value = true
  try {
    const res = await getProductList({
      pageNum: 1,
      pageSize: 12,
      status: 'ON_SALE',
      sortBy: 'salesCount',
      sortOrder: 'desc'
    })
    const list = res.data.list || []
    recommendItems.value = list.map((p: ProductVO) => ({
      id: p.id,
      name: p.productName,
      price: p.originalPrice || 0,
      sold: p.salesCount || 0,
      image: p.images?.[0]
    }))
  } catch {
    // 错误已由全局拦截器统一提示
    recommendItems.value = []
  } finally {
    recommendLoading.value = false
  }
}

/** 换一换：简单旋转数组 */
function shuffleRecommend(): void {
  if (recommendItems.value.length > 1) {
    const first = recommendItems.value.shift()
    if (first) recommendItems.value.push(first)
  }
}

/* === 跳转秒杀详情 === */
function goSeckillDetail(id: number): void {
  router.push(`/seckill/${id}`)
}

/* === 跳转商品详情 === */
function goProductDetail(id: number): void {
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

onMounted(() => {
  fetchBanners()
  fetchCategoryTree()
  fetchList()
  fetchRecommendProducts()
  updateCountdown()
  cdTimer = setInterval(updateCountdown, 1000)
})

onUnmounted(() => {
  if (cdTimer) clearInterval(cdTimer)
  seckillStore.stopAllCountdowns()
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
  padding: 8px 0;
  box-sizing: border-box;
  /* 高度与 banner 主图对齐 (280px) */
  height: 280px;
  overflow-y: auto;
  position: relative;
  /* 隐藏滚动条但保留滚动能力 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.category-sidebar::-webkit-scrollbar {
  display: none;
}

/* 一级分类项 */
.sidebar-item {
  position: relative;
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

.sidebar-item:hover {
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

.sidebar-item:hover .sidebar-arrow {
  color: #ffffff;
}

/* 二级分类浮层: 悬停一级分类时右侧弹出大面板 */
.sidebar-panel {
  display: none;
  position: absolute;
  top: 0;
  left: 100%;
  width: 480px;
  min-height: 280px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 16px 20px;
  z-index: 60;
  box-sizing: border-box;
}

.sidebar-item:hover > .sidebar-panel {
  display: block;
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

/* 二级分类按列排列 (淘宝风格: 多列布局, 每列3-4个, 自动平衡列高) */
.panel-content {
  column-count: 3;
  column-gap: 16px;
  /* 兼容性前缀 */
  -webkit-column-count: 3;
  -moz-column-count: 3;
  -webkit-column-gap: 16px;
  -moz-column-gap: 16px;
}

.panel-item {
  display: block;
  font-size: 13px;
  color: #4b5563;
  cursor: pointer;
  padding: 5px 12px;
  border-radius: 4px;
  transition: all 0.15s;
  white-space: nowrap;
  line-height: 1.4;
  margin-bottom: 6px;
  /* 防止 item 被列分割断开 */
  break-inside: avoid;
  -webkit-column-break-inside: avoid;
  page-break-inside: avoid;
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

/* === 2. 分类快捷导航 (对照 index.html 第805-833行) === */
.category-wrap {
  padding: 20px 24px 0;
}

.category-row {
  display: flex;
  gap: 0;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.category-item {
  flex: 1;
  padding: 14px 0;
  text-align: center;
  cursor: pointer;
  border-right: 1px solid #e5e7eb;
  transition: background 0.15s;
  box-sizing: border-box;
}

.category-item.no-border {
  border-right: none;
}

.category-item:hover {
  background: #f8f8f8;
}

.category-icon {
  width: 22px;
  height: 22px;
  margin: 0 auto 6px;
  display: block;
}

.category-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

/* === 3. 限时秒杀区域 (对照 index.html 第835-924行 + CSS 第240-294行) === */
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

/* 商品横向滚动 */
.product-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.product-scroll::-webkit-scrollbar {
  height: 4px;
}

.product-scroll::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

/* 空状态在横向滚动容器中居中显示 */
.scroll-empty {
  width: 100%;
  margin: 32px auto;
  flex-shrink: 0;
}

/* === 商品卡片 p-card (对照 index.html CSS 第261-294行) === */
.p-card {
  width: 200px;
  flex-shrink: 0;
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

/* 待开始遮罩 */
.pending-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  font-size: 12px;
  color: #1a1a2e;
  font-weight: 600;
}

.pending-sub {
  font-size: 11px;
  color: #6b7280;
  margin-top: 4px;
  font-weight: 400;
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

.p-card-original {
  font-size: 12px;
  color: #6b7280;
  text-decoration: line-through;
}

.stock-bar {
  height: 6px;
  background: #eee;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.stock-bar-fill {
  height: 100%;
  border-radius: 3px;
}

.stock-bar-fill.high {
  background: #4caf50;
}

.stock-bar-fill.mid {
  background: #ff9800;
}

.stock-bar-fill.low {
  background: #e53935;
}

.stock-text {
  font-size: 11px;
  color: #6b7280;
}

.stock-text.danger {
  color: #e53935;
  font-weight: 700;
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

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

/* 空状态在网格容器中占满整行居中显示 */
.grid-empty {
  grid-column: 1 / -1;
  margin: 32px auto;
}

/* 猜你喜欢卡片图片高度 150px */
.recommend-img {
  height: 150px;
}

/* 猜你喜欢价格用深色 */
.recommend-price {
  color: #1a1a2e;
}

.recommend-price::before {
  content: '\A5';
  font-size: 12px;
}
</style>
