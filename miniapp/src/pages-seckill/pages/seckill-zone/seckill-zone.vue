<!--
  秒杀专区页（阶段 4 核心）
  路径：pages-seckill/pages/seckill-zone/seckill-zone
  对齐 spec.md 3.5 秒杀专区 / 2.4 秒杀防重放 / 2.3 服务器时间同步
  对齐 plan.md 4.5 防重放头传递 / tasks.md T4.1~T4.5

  功能：
  - T4.1 场次切换 + 商品列表（u-tabs 横向切换场次，2 列网格）
  - T4.2 倒计时（timeSync.getTimeOffset() 校准服务器时间）
  - T4.3 库存展示（实时查询 + 进度条）
  - T4.4 一次性 token + 执行秒杀（X-Seckill-Token 头防重放）
  - T4.5 结果轮询（排队中 → 成功/失败）

  关键约束：
  - 雪花 ID 全程 string
  - 倒计时用 Date.now() + getTimeOffset() 校准
  - 防重放：getSeckillToken → executeSeckill 携带 X-Seckill-Token 头
  - 1011 REPLAY_DETECTED 不触发 Token 刷新，提示重试
-->
<template>
  <view class="seckill-zone">
    <!-- 自定义导航栏（pages.json navigationStyle:custom） -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="navbar-content">
        <view class="navbar-back" @click="handleBack">
          <u-icon name="arrow-left" color="#ffffff" size="20" />
        </view>
        <text class="navbar-title">秒杀专区</text>
        <view class="navbar-placeholder" />
      </view>
    </view>

    <!-- 顶部倒计时横幅 -->
    <view class="hero-banner" :style="{ marginTop: statusBarHeight + 'px' }">
      <view class="hero-bg" />
      <view class="hero-content">
        <view class="hero-left">
          <text class="hero-title">⚡ 限时秒杀</text>
          <text class="hero-subtitle">手快有 手慢无</text>
        </view>
        <view class="hero-right">
          <!-- 当前场次倒计时 -->
          <view v-if="currentActivity" class="hero-countdown">
            <text class="countdown-label">{{ countdownLabel }}</text>
            <CountDown
              v-if="countdownTarget > 0"
              :target-time="countdownTarget"
              :prefix="countdownPrefix"
              @end="handleCountdownEnd"
            />
          </view>
        </view>
      </view>
    </view>

    <!-- 场次切换 tabs -->
    <view class="session-tabs">
      <u-tabs
        v-if="activityTabs.length > 0"
        :list="activityTabs"
        :current="currentTabIndex"
        :scrollable="true"
        :bar-width="40"
        bar-height="4"
        active-color="#ff4d4f"
        inactive-color="#606266"
        @click="handleTabClick"
      />
      <view v-else class="empty-tabs">
        <text class="empty-text">{{ loading ? '加载中...' : '暂无秒杀场次' }}</text>
      </view>
    </view>

    <!-- 商品列表区域 -->
    <view class="goods-section">
      <!-- 加载中 -->
      <view v-if="loading && currentGoodsList.length === 0" class="loading-wrap">
        <u-loading mode="circle" color="#ff4d4f" />
        <text class="loading-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="currentGoodsList.length === 0" class="empty-wrap">
        <u-empty text="本场次暂无秒杀商品" mode="data" />
      </view>

      <!-- 2 列网格商品列表 -->
      <view v-else class="goods-grid">
        <view
          v-for="goods in currentGoodsList"
          :key="goods.id"
          class="grid-item"
        >
          <SeckillCard
            :goods="goods"
            :purchasing="purchasingId === goods.id"
            @seckill="handleSeckill"
            @click="handleCardClick"
          />
        </view>
      </view>
    </view>

    <!-- 抢购结果弹窗 -->
    <u-modal
      :show="resultModal.show"
      :title="resultModal.title"
      :content="resultModal.content"
      :show-cancel-button="false"
      confirm-text="知道了"
      @confirm="closeResultModal"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useSeckillStore } from '@/stores/seckill'
import { useUserStore } from '@/stores/user'
import * as seckillApi from '@/api/seckill'
import { navigate } from '@/utils/navigate'
import { showToast, showLoading, hideLoading } from '@/utils/toast'
import { getTimeOffset, syncServerTime } from '@/utils/timeSync'
import { BizCode, type SeckillActivityVO, type SeckillGoodsVO, type SeckillResultVO } from '@/types'
import CountDown from '@/components/CountDown/CountDown.vue'
import SeckillCard from '@/components/SeckillCard/SeckillCard.vue'

// ============ Store ============
const seckillStore = useSeckillStore()
const userStore = useUserStore()

// ============ 状态 ============
/** 状态栏高度（自定义导航栏用） */
const statusBarHeight = ref<number>(20)
/** 加载中 */
const loading = ref<boolean>(false)
/** 场次列表 */
const activities = ref<SeckillActivityVO[]>([])
/** 当前场次索引 */
const currentTabIndex = ref<number>(0)
/** 当前场次商品列表（按库存优先排序） */
const currentGoodsList = ref<SeckillGoodsVO[]>([])
/** 正在抢购的商品 ID（防重复点击） */
const purchasingId = ref<string>('')
/** 倒计时目标时间戳（毫秒，服务器时间） */
const countdownTarget = ref<number>(0)
/** 倒计时刷新 key（强制 CountDown 重启） */
const countdownKey = ref<number>(0)
/** 库存轮询定时器 */
let stockTimerId: ReturnType<typeof setInterval> | null = null

/** 抢购结果弹窗 */
const resultModal = ref<{
  show: boolean
  title: string
  content: string
}>({
  show: false,
  title: '',
  content: ''
})

// ============ 计算属性 ============
/** 当前场次 */
const currentActivity = computed<SeckillActivityVO | null>(() => {
  if (activities.value.length === 0 || currentTabIndex.value >= activities.value.length) {
    return null
  }
  return activities.value[currentTabIndex.value]
})

/** u-tabs 数据 */
const activityTabs = computed(() => {
  return activities.value.map((a) => ({
    name: a.name || formatSessionName(a)
  }))
})

/** 倒计时前缀（距开始 / 距结束） */
const countdownPrefix = computed(() => {
  if (!currentActivity.value) return ''
  const act = currentActivity.value
  const serverNow = Date.now() + getTimeOffset()
  const startTime = parseTime(act.startTime)
  if (serverNow < startTime) return '距开始'
  return '距结束'
})

/** 倒计时标签 */
const countdownLabel = computed(() => {
  if (!currentActivity.value) return ''
  return '限时抢购'
})

// ============ 生命周期 ============
onMounted(async () => {
  // 获取状态栏高度
  try {
    const sysInfo = uni.getSystemInfoSync()
    statusBarHeight.value = sysInfo.statusBarHeight || 20
  } catch (e) {
    statusBarHeight.value = 20
  }

  // 加载场次列表
  await loadActivities()

  // 启动库存轮询（每 5s 刷新当前场次商品库存）
  startStockPolling()
})

onUnmounted(() => {
  stopStockPolling()
})

// ============ 数据加载 ============
/** 加载秒杀场次列表 */
async function loadActivities(): Promise<void> {
  loading.value = true
  try {
    const list = await seckillStore.fetchActivities()
    // 同步服务器时间（取列表中第一个 startTime 作为参考，更精确应从响应头取）
    // 注：request.ts 拦截器已自动从响应头 X-Server-Time 同步，此处兜底
    activities.value = list || []
    if (activities.value.length > 0) {
      currentTabIndex.value = 0
      await loadCurrentGoods()
    }
  } catch (e: any) {
    console.error('加载秒杀场次失败', e)
    showToast('加载秒杀场次失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 加载当前场次商品列表（库存优先排序） */
async function loadCurrentGoods(): Promise<void> {
  if (!currentActivity.value) {
    currentGoodsList.value = []
    return
  }
  const act = currentActivity.value
  // 优先使用场次内嵌的 goodsList
  if (act.goodsList && act.goodsList.length > 0) {
    currentGoodsList.value = sortGoodsByStock(act.goodsList)
  } else {
    // 兜底：调用列表接口按 activityId 查询
    try {
      const res = await seckillApi.getSeckillList({ activityId: act.id })
      currentGoodsList.value = sortGoodsByStock(res.list || [])
    } catch (e) {
      currentGoodsList.value = []
    }
  }
  // 更新倒计时目标
  updateCountdownTarget()
}

/** 库存优先排序（可用库存多的在前，售罄沉底） */
function sortGoodsByStock(list: SeckillGoodsVO[]): SeckillGoodsVO[] {
  return [...list].sort((a, b) => {
    // 售罄沉底
    const aSoldOut = a.availableStock <= 0 ? 1 : 0
    const bSoldOut = b.availableStock <= 0 ? 1 : 0
    if (aSoldOut !== bSoldOut) return aSoldOut - bSoldOut
    // 库存多在前
    return b.availableStock - a.availableStock
  })
}

/** 更新倒计时目标时间 */
function updateCountdownTarget(): void {
  if (!currentActivity.value) {
    countdownTarget.value = 0
    return
  }
  const act = currentActivity.value
  const serverNow = Date.now() + getTimeOffset()
  const startTime = parseTime(act.startTime)
  const endTime = parseTime(act.endTime)
  if (serverNow < startTime) {
    countdownTarget.value = startTime
  } else if (serverNow < endTime) {
    countdownTarget.value = endTime
  } else {
    countdownTarget.value = 0
  }
  countdownKey.value++
}

// ============ 库存轮询 ============
/** 启动库存轮询（每 5s 刷新当前场次商品库存） */
function startStockPolling(): void {
  stopStockPolling()
  stockTimerId = setInterval(async () => {
    await refreshCurrentStock()
  }, 5000)
}

/** 停止库存轮询 */
function stopStockPolling(): void {
  if (stockTimerId !== null) {
    clearInterval(stockTimerId)
    stockTimerId = null
  }
}

/** 刷新当前场次商品库存 */
async function refreshCurrentStock(): Promise<void> {
  if (currentGoodsList.value.length === 0) return
  // 并发查询每个商品的实时库存
  const tasks = currentGoodsList.value.map(async (goods) => {
    try {
      const stock = await seckillApi.getSeckillStock(goods.id)
      goods.availableStock = stock.availableStock
      goods.totalStock = stock.totalStock
    } catch (e) {
      // 静默失败，不影响展示
    }
  })
  await Promise.all(tasks)
  // 触发响应式更新
  currentGoodsList.value = [...currentGoodsList.value]
  // 同步更新倒计时目标
  updateCountdownTarget()
}

// ============ 场次切换 ============
/** u-tabs 点击切换 */
function handleTabClick(item: any): void {
  const index = activityTabs.value.findIndex((t) => t.name === item.name)
  if (index < 0 || index === currentTabIndex.value) return
  currentTabIndex.value = index
  if (currentActivity.value) {
    seckillStore.setCurrentActivity(currentActivity.value)
  }
  loadCurrentGoods()
}

// ============ 倒计时结束 ============
/** 倒计时结束回调（场次开始或结束） */
function handleCountdownEnd(): void {
  // 重新加载场次列表与商品，刷新状态
  loadActivities()
}

// ============ 抢购流程（核心 T4.4 + T4.5） ============
/**
 * 处理抢购点击
 * 流程：检查登录 → 获取一次性 token → 执行秒杀 → 处理结果 → 轮询（如排队中）
 */
async function handleSeckill(goods: SeckillGoodsVO): Promise<void> {
  // 1. 防重复点击
  if (purchasingId.value === goods.id) {
    showToast('正在抢购中，请稍候', 'none')
    return
  }

  // 2. 检查登录态
  if (!userStore.isLoggedIn) {
    navigate.toLogin('pages-seckill/pages/seckill-zone/seckill-zone')
    return
  }

  // 3. 校验活动状态
  if (goods.status === 0) {
    showToast('活动尚未开始', 'none')
    return
  }
  if (goods.status === 2) {
    showToast('活动已结束', 'none')
    return
  }
  if (goods.availableStock <= 0) {
    showToast('库存不足，已抢光', 'none')
    return
  }

  purchasingId.value = goods.id
  showLoading('抢购中...')

  try {
    // 4. 执行秒杀（store 内部：getSeckillToken → executeSeckill 携带 X-Seckill-Token 头）
    const result = await seckillStore.executeSeckill(goods.id)
    hideLoading()

    // 5. 处理结果
    await handleSeckillResult(goods, result)
  } catch (e: any) {
    hideLoading()
    handleSeckillError(e)
  } finally {
    purchasingId.value = ''
  }
}

/**
 * 处理秒杀结果
 * - status 0 排队中：轮询结果
 * - status 1 成功：跳转订单详情
 * - status 2 失败：提示
 * - status 3 已售罄：提示
 */
async function handleSeckillResult(goods: SeckillGoodsVO, result: SeckillResultVO): Promise<void> {
  // 兜底：success 字段优先
  if (result.success === true && result.orderId) {
    showToast('抢购成功！', 'success')
    // 跳转订单详情（雪花 ID 用 string）
    setTimeout(() => {
      navigate.to('pages-order/pages/order-detail/order-detail', { id: result.orderId })
    }, 800)
    return
  }

  switch (result.status) {
    case 0:
      // 排队中：轮询结果
      await pollSeckillResult(goods)
      break
    case 1:
      // 成功
      showToast('抢购成功！', 'success')
      if (result.orderId) {
        setTimeout(() => {
          navigate.to('pages-order/pages/order-detail/order-detail', { id: result.orderId })
        }, 800)
      }
      break
    case 2:
      // 失败
      showToast(result.message || '抢购失败', 'error')
      break
    case 3:
      // 已售罄
      showToast('手慢了，已抢光', 'none')
      // 刷新库存
      refreshCurrentStock()
      break
    default:
      showToast(result.message || '抢购结果未知，请稍后查看订单', 'none')
  }
}

/**
 * 轮询秒杀结果（T4.5）
 * 每 1.5s 轮询一次，最多轮询 30s（20 次）
 */
async function pollSeckillResult(goods: SeckillGoodsVO): Promise<void> {
  const POLL_INTERVAL = 1500 // 1.5s
  const MAX_POLL_COUNT = 20 // 最多 20 次（30s）
  showLoading('排队中，请稍候...')

  try {
    for (let i = 0; i < MAX_POLL_COUNT; i++) {
      // 等待间隔
      await sleep(POLL_INTERVAL)
      // 查询结果
      const result = await seckillStore.getSeckillResult(goods.id)
      // status 0 仍排队中，继续轮询
      if (result.status === 0) continue
      // 已有最终结果
      hideLoading()
      await handleSeckillResult(goods, result)
      return
    }
    // 超时仍未出结果
    hideLoading()
    showToast('排队人数较多，请稍后在订单列表查看结果', 'none')
    // 跳转订单列表
    setTimeout(() => {
      navigate.to('pages-order/pages/order-list/order-list')
    }, 1000)
  } catch (e: any) {
    hideLoading()
    handleSeckillError(e)
  }
}

/**
 * 处理秒杀错误（含防重放 1011）
 */
function handleSeckillError(e: any): void {
  const code = e?.code ?? e?.data?.code
  const message = e?.message ?? e?.data?.message ?? '抢购失败'

  if (code === BizCode.REPLAY_DETECTED) {
    // 防重放拦截（1011）：提示重试，允许用户重新点击
    showToast('抢购请求已失效，请重试', 'none')
    return
  }

  if (code === BizCode.UNAUTHORIZED) {
    // 401 未登录（理论上 store 已处理，兜底）
    showToast('请先登录', 'none')
    navigate.toLogin('pages-seckill/pages/seckill-zone/seckill-zone')
    return
  }

  // 其他错误：库存不足 / 活动结束 / 重复抢购等
  showToast(message || '抢购失败，请重试', 'error')
}

// ============ 卡片点击 ============
/** 点击卡片跳转商品详情 */
function handleCardClick(goods: SeckillGoodsVO): void {
  // 跳转商品详情（使用 productId）
  navigate.to('pages-product/pages/product-detail/product-detail', { id: goods.productId })
}

// ============ 返回 ============
/** 返回上一页 */
function handleBack(): void {
  navigate.back(1)
}

// ============ 结果弹窗 ============
/** 关闭结果弹窗 */
function closeResultModal(): void {
  resultModal.value.show = false
}

// ============ 工具函数 ============
/** 解析时间字符串为毫秒时间戳 */
function parseTime(time: string | number): number {
  if (typeof time === 'number') return time
  if (!time) return 0
  // 数字字符串
  if (/^\d+$/.test(time)) {
    return time.length <= 10 ? Number(time) * 1000 : Number(time)
  }
  // ISO 字符串
  const ts = new Date(time).getTime()
  return isNaN(ts) ? 0 : ts
}

/** 格式化场次名（如未提供 name，根据 startTime 生成"HH:00 场"） */
function formatSessionName(act: SeckillActivityVO): string {
  const start = parseTime(act.startTime)
  if (!start) return '秒杀专场'
  const d = new Date(start)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm} 场`
}

/** sleep */
function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
</script>

<style lang="scss" scoped>
.seckill-zone {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}

/* 自定义导航栏 */
.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: linear-gradient(135deg, #ff4d4f, #ff7a45);

  .navbar-content {
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24rpx;

    .navbar-back {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .navbar-title {
      font-size: 32rpx;
      font-weight: 600;
      color: #ffffff;
    }

    .navbar-placeholder {
      width: 64rpx;
      height: 64rpx;
    }
  }
}

/* 顶部横幅 */
.hero-banner {
  position: relative;
  width: 100%;
  height: 200rpx;
  overflow: hidden;

  .hero-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, #ff4d4f, #ff7a45);
  }

  .hero-content {
    position: relative;
    z-index: 2;
    height: 100%;
    padding: 0 32rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .hero-left {
      display: flex;
      flex-direction: column;
      gap: 8rpx;

      .hero-title {
        font-size: 40rpx;
        font-weight: bold;
        color: #ffffff;
      }

      .hero-subtitle {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.85);
      }
    }

    .hero-right {
      .hero-countdown {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 8rpx;

        .countdown-label {
          font-size: 22rpx;
          color: rgba(255, 255, 255, 0.85);
        }
      }
    }
  }
}

/* 场次切换 tabs */
.session-tabs {
  background-color: #ffffff;
  border-radius: 16rpx 16rpx 0 0;
  margin-top: -16rpx;
  position: relative;
  z-index: 3;
  padding: 16rpx 0;

  .empty-tabs {
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    .empty-text {
      font-size: 26rpx;
      color: #909399;
    }
  }
}

/* 商品列表区域 */
.goods-section {
  padding: 16rpx;

  .loading-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 120rpx 0;
    gap: 16rpx;

    .loading-text {
      font-size: 26rpx;
      color: #909399;
    }
  }

  .empty-wrap {
    padding: 120rpx 0;
  }

  /* 2 列网格 */
  .goods-grid {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 16rpx;

    .grid-item {
      width: calc((100% - 16rpx) / 2);
    }
  }
}
</style>