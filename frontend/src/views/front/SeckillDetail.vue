<template>
  <div class="seckill-detail-page">
    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 错误状态 -->
    <el-result v-else-if="error" icon="warning" title="活动不存在" sub-title="您访问的秒杀活动可能已结束或被删除">
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <!-- 秒杀详情主体 (严格对照 index.html page-seckill-detail 结构) -->
    <template v-else-if="seckill">
      <div class="seckill-detail">
        <!-- === 状态横幅 === -->
        <div class="seckill-status-bar" :class="statusBarClass">
          <span class="status-label">{{ statusLabel }}</span>
          <div class="status-cd">
            <template v-if="seckill.status === 'PENDING'">
              距开始
              <span class="cd-block">{{ cdHours }}</span>
              <span class="cd-sep">:</span>
              <span class="cd-block">{{ cdMinutes }}</span>
              <span class="cd-sep">:</span>
              <span class="cd-block">{{ cdSeconds }}</span>
            </template>
            <template v-else-if="seckill.status === 'ACTIVE'">
              距结束
              <span class="cd-block">{{ cdHours }}</span>
              <span class="cd-sep">:</span>
              <span class="cd-block">{{ cdMinutes }}</span>
              <span class="cd-sep">:</span>
              <span class="cd-block">{{ cdSeconds }}</span>
            </template>
            <template v-else>
              <span class="status-text">{{ statusText }}</span>
            </template>
          </div>
        </div>

        <!-- === 详情主体 grid === -->
        <div class="detail-grid">
          <!-- 左列: 图片轮播 + 缩略图条 -->
          <div class="detail-left">
            <el-carousel v-if="displayImages.length > 0" height="360px" indicator-position="none" :autoplay="true"
              :interval="4000" arrow="hover" class="detail-carousel" @change="onCarouselChange">
              <el-carousel-item v-for="(img, idx) in displayImages" :key="idx">
                <el-image :src="formatImageUrl(img)" fit="cover" class="carousel-image">
                  <template #error>
                    <div class="img-placeholder">
                      <el-icon :size="64">
                        <Picture />
                      </el-icon>
                    </div>
                  </template>
                </el-image>
              </el-carousel-item>
            </el-carousel>
            <div v-else class="detail-carousel empty-carousel">
              <el-icon :size="64">
                <Picture />
              </el-icon>
            </div>

            <!-- 缩略图条 -->
            <div class="thumb-strip">
              <div v-for="(img, idx) in thumbImages" :key="idx" class="thumb-item"
                :class="{ active: idx === currentCarouselIdx }" @click="switchThumb(idx)">
                <el-image :src="formatImageUrl(img)" fit="cover" class="thumb-img" lazy>
                  <template #error>
                    <el-icon :size="20">
                      <Picture />
                    </el-icon>
                  </template>
                </el-image>
              </div>
            </div>
          </div>

          <!-- 右列: 商品信息 (detail-info) -->
          <div class="detail-info">
            <!-- 卖点标签 -->
            <div class="selling-tags">
              <span class="sell-tag tag-red">限时秒杀</span>
              <span class="sell-tag tag-orange">官方正品</span>
              <span class="sell-tag tag-green">全国联保</span>
            </div>

            <!-- 标题 + 副标题 -->
            <h2>{{ seckill.seckillName }}</h2>
            <p class="sub-name">{{ seckill.productName }}</p>

            <!-- 价格块 (price-block) -->
            <div class="price-block">
              <div class="price-row">
                <span class="price-label">秒杀价</span>
                <span class="price-seckill">{{ formatPrice(seckill.seckillPrice) }}</span>
                <span v-if="originalPrice" class="price-original">¥{{ formatPrice(originalPrice) }}</span>
                <span v-if="discountAmount > 0" class="price-discount">
                  省{{ formatPrice(discountAmount) }}
                </span>
              </div>
              <div class="price-stats">
                <span>累计销量 {{ soldCount }}</span>
                <span>好评率 98%</span>
                <span>已有 {{ participantCount }} 人参与</span>
              </div>
            </div>

            <!-- 规格选择: 颜色 -->
            <div class="spec-row">
              <div class="spec-label">
                颜色 <span class="spec-value">{{ selectedColor }}</span>
              </div>
              <div class="spec-options">
                <span v-for="color in colorOptions" :key="color" class="spec-option"
                  :class="{ active: selectedColor === color }" @click="selectedColor = color">{{ color }}</span>
              </div>
            </div>

            <!-- 规格选择: 存储 -->
            <div class="spec-row">
              <div class="spec-label">
                存储 <span class="spec-value">{{ selectedStorage }}</span>
              </div>
              <div class="spec-options">
                <span v-for="storage in storageOptions" :key="storage.value" class="spec-option" :class="{
                  active: selectedStorage === storage.value,
                  disabled: storage.disabled
                }" @click="!storage.disabled && (selectedStorage = storage.value)">{{ storage.label }}</span>
              </div>
            </div>

            <!-- 服务保障 -->
            <div class="service-row">
              <span class="service-item">
                <el-icon class="service-icon">
                  <CircleCheckFilled />
                </el-icon>正品保障
              </span>
              <span class="service-item">
                <el-icon class="service-icon">
                  <CircleCheckFilled />
                </el-icon>7天无理由
              </span>
              <span class="service-item">
                <el-icon class="service-icon">
                  <CircleCheckFilled />
                </el-icon>运费险
              </span>
              <span class="service-item">
                <el-icon class="service-icon">
                  <CircleCheckFilled />
                </el-icon>极速退款
              </span>
            </div>

            <!-- 商品参数 (detail-meta) -->
            <dl class="detail-meta">
              <div>
                <dt>限购数量</dt>
                <dd>每人限购 {{ seckill.perLimit }} 件</dd>
              </div>
              <div>
                <dt>发货时间</dt>
                <dd>付款后 24h 内</dd>
              </div>
              <div>
                <dt>开始时间</dt>
                <dd>{{ formatTimeShort(seckill.startTime) }}</dd>
              </div>
              <div>
                <dt>结束时间</dt>
                <dd>{{ formatTimeShort(seckill.endTime) }}</dd>
              </div>
            </dl>

            <!-- 库存进度条 (stock-inline) -->
            <div class="stock-inline">
              <span class="stock-inline-label">库存</span>
              <div class="stock-bar">
                <div class="stock-bar-fill" :class="stockBarClass" :style="{ width: stockPercent + '%' }"></div>
              </div>
              <span class="stock-num">剩余 {{ availableCount }} / {{ seckill.stockCount }} 件</span>
            </div>

            <!-- 秒杀按钮 (seckill-btn) -->
            <SeckillButton :seckill-status="seckill.status" :available-count="availableCount" :loading="btnLoading"
              :state="btnState" :countdown="pendingCountdown" :fail-text="failText" :poll-progress="pollProgress"
              class="seckill-action" @seckill="handleSeckill" />

            <p class="action-tip">
              已有 {{ participantCount }} 人参与抢购 · 限购{{ seckill.perLimit }}件
            </p>
          </div>
        </div>

        <!-- === 下方标签页 === -->
        <div class="tab-section">
          <div class="tab-header">
            <div v-for="tab in tabs" :key="tab.key" class="tab-item" :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key">{{ tab.label }}</div>
          </div>

          <div class="tab-content">
            <!-- 商品详情 -->
            <template v-if="activeTab === 'detail'">
              <!-- 高亮 banner -->
              <div class="highlight-banner">
                <div class="banner-title">{{ bannerTitle }}</div>
                <div class="banner-subtitle">钛金属。强得很。</div>
              </div>

              <!-- 特性网格 -->
              <div class="feature-grid">
                <div class="feature-card">
                  <div class="feature-title">A17 Pro</div>
                  <div class="feature-desc">3nm 制程芯片，性能怪兽</div>
                </div>
                <div class="feature-card">
                  <div class="feature-title">4800万</div>
                  <div class="feature-desc">主摄像素，超视网膜级画质</div>
                </div>
                <div class="feature-card">
                  <div class="feature-title">钛金属</div>
                  <div class="feature-desc">航空级材质，轻至 187g</div>
                </div>
              </div>

              <!-- 详情图占位 -->
              <div class="detail-image-placeholder">
                <el-icon :size="48">
                  <Picture />
                </el-icon>
                <span>商品详情图 1 — 产品正面展示</span>
              </div>
              <div class="detail-image-placeholder">
                <el-icon :size="48">
                  <Picture />
                </el-icon>
                <span>商品详情图 2 — 摄像头模组特写</span>
              </div>
              <div class="detail-image-placeholder">
                <el-icon :size="48">
                  <Picture />
                </el-icon>
                <span>商品详情图 3 — 钛金属边框细节</span>
              </div>
            </template>

            <!-- 规格参数 -->
            <template v-else-if="activeTab === 'spec'">
              <h3 class="spec-table-title">规格参数</h3>
              <table class="spec-table">
                <tbody>
                  <tr>
                    <td class="spec-key">品牌</td>
                    <td class="spec-val">Apple</td>
                    <td class="spec-key">型号</td>
                    <td class="spec-val">iPhone 15 Pro</td>
                  </tr>
                  <tr>
                    <td class="spec-key">处理器</td>
                    <td class="spec-val">A17 Pro (3nm)</td>
                    <td class="spec-key">存储容量</td>
                    <td class="spec-val">{{ selectedStorage }}</td>
                  </tr>
                  <tr>
                    <td class="spec-key">屏幕尺寸</td>
                    <td class="spec-val">6.1 英寸</td>
                    <td class="spec-key">屏幕材质</td>
                    <td class="spec-val">OLED 超视网膜 XDR</td>
                  </tr>
                  <tr>
                    <td class="spec-key">后置摄像头</td>
                    <td class="spec-val">4800万 + 1200万 + 1200万</td>
                    <td class="spec-key">前置摄像头</td>
                    <td class="spec-val">1200万像素</td>
                  </tr>
                  <tr>
                    <td class="spec-key">电池容量</td>
                    <td class="spec-val">3274mAh</td>
                    <td class="spec-key">充电接口</td>
                    <td class="spec-val">USB-C</td>
                  </tr>
                  <tr>
                    <td class="spec-key">机身材质</td>
                    <td class="spec-val">钛金属 + 超瓷晶面板</td>
                    <td class="spec-key">重量</td>
                    <td class="spec-val">187g</td>
                  </tr>
                </tbody>
              </table>
            </template>

            <!-- 用户评价 -->
            <template v-else-if="activeTab === 'review'">
              <div class="review-summary">
                <h3 class="review-title">用户评价 <span class="review-count">(326条)</span></h3>
                <div class="review-rate">
                  <span class="rate-percent">98%</span>
                  <span class="rate-label">好评率</span>
                </div>
              </div>

              <!-- 评价标签 -->
              <div class="review-tags">
                <span class="review-tag active">正品保障 (128)</span>
                <span class="review-tag">物流很快 (96)</span>
                <span class="review-tag">手感一流 (87)</span>
                <span class="review-tag">拍照清晰 (74)</span>
                <span class="review-tag">性价比高 (65)</span>
              </div>

              <!-- 评价列表 -->
              <div class="review-list">
                <div v-for="review in reviewList" :key="review.id" class="review-item">
                  <div class="review-avatar">{{ review.avatar }}</div>
                  <div class="review-body">
                    <div class="review-head">
                      <span class="review-user">{{ review.user }}</span>
                      <span class="review-stars">
                        <span class="star-filled">★★★★★</span>
                      </span>
                    </div>
                    <p class="review-text">{{ review.text }}</p>
                    <div class="review-meta">{{ review.time }} · {{ review.spec }}</div>
                  </div>
                </div>
              </div>
            </template>

            <!-- 秒杀须知 -->
            <template v-else-if="activeTab === 'notice'">
              <h4 class="notice-title">秒杀须知</h4>
              <div class="notice-content">
                <p>1. 本商品为限时秒杀活动，每人限购 {{ seckill.perLimit }} 件，不可重复下单。</p>
                <p>2. 秒杀成功后请在 15 分钟内完成支付，超时订单将自动取消并释放库存。</p>
                <p>3. 秒杀商品享受与正价商品相同的售后保障（7天无理由退换、全国联保）。</p>
                <p>4. 秒杀活动最终解释权归平台所有，如遇不可抗力因素，平台有权调整活动规则。</p>
              </div>
            </template>
          </div>
        </div>

        <!-- === 规格参数表格 (常驻区域, 对照设计稿第 1213-1300 行) === -->
        <div class="spec-section">
          <h3 class="spec-section-title">规格参数</h3>
          <table class="spec-table">
            <tbody>
              <tr>
                <td class="spec-key">品牌</td>
                <td class="spec-val">Apple</td>
                <td class="spec-key">型号</td>
                <td class="spec-val">iPhone 15 Pro</td>
              </tr>
              <tr>
                <td class="spec-key">处理器</td>
                <td class="spec-val">A17 Pro (3nm)</td>
                <td class="spec-key">存储容量</td>
                <td class="spec-val">{{ selectedStorage }}</td>
              </tr>
              <tr>
                <td class="spec-key">屏幕尺寸</td>
                <td class="spec-val">6.1 英寸</td>
                <td class="spec-key">屏幕材质</td>
                <td class="spec-val">OLED 超视网膜 XDR</td>
              </tr>
              <tr>
                <td class="spec-key">后置摄像头</td>
                <td class="spec-val">4800万 + 1200万 + 1200万</td>
                <td class="spec-key">前置摄像头</td>
                <td class="spec-val">1200万像素</td>
              </tr>
              <tr>
                <td class="spec-key">电池容量</td>
                <td class="spec-val">3274mAh</td>
                <td class="spec-key">充电接口</td>
                <td class="spec-val">USB-C</td>
              </tr>
              <tr>
                <td class="spec-key">机身材质</td>
                <td class="spec-val">钛金属 + 超瓷晶面板</td>
                <td class="spec-key">重量</td>
                <td class="spec-val">187g</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * P04 秒杀详情 (核心页面)
 * 严格对照 index.html page-seckill-detail 结构 1:1 还原
 * 完整状态机: IDLE → PENDING_COUNTDOWN → TOKEN_PREFETCH → ACTIVE_READY
 *           → CLICK_SECKILL → LOADING → POLLING → SUCCESS/FAIL
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled, Picture } from '@element-plus/icons-vue'
import {
  getSeckillDetail,
  getSeckillStock,
  getSeckillToken,
  doSeckill,
  getSeckillResult
} from '@/api/seckill'
import { getProductDetail } from '@/api/product'
import { useUserStore } from '@/stores/user'
import SeckillButton from '@/components/SeckillButton.vue'
import dayjs from 'dayjs'
import { formatImageUrl } from '@/utils/image'
import type { SeckillGoodsVO, SeckillResultVO } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/* === 状态 === */
const loading = ref<boolean>(false)
const error = ref<boolean>(false)
const seckill = ref<SeckillGoodsVO | null>(null)
const originalPrice = ref<number>(0)
const availableCount = ref<number>(0)

/* === 按钮状态机 === */
type BtnState = 'active' | 'loading' | 'polling' | 'success' | 'fail' | 'disabled' | undefined
const btnLoading = ref<boolean>(false)
const btnState = ref<BtnState>(undefined)
const failText = ref<string>('')
const pollProgress = ref<number>(0)
const pendingCountdown = ref<number>(0)
const seckillToken = ref<string>('')
const tokenPrefetched = ref<boolean>(false)

/* === UI 状态 === */
const currentCarouselIdx = ref<number>(0)
const activeTab = ref<'detail' | 'spec' | 'review' | 'notice'>('detail')
const selectedColor = ref<string>('原色钛金属')
const selectedStorage = ref<string>('256GB')

/* === 定时器 === */
let stockTimer: ReturnType<typeof setInterval> | null = null
let pendingCountdownTimer: ReturnType<typeof setInterval> | null = null

/* === 静态数据 (对照设计稿) === */
const colorOptions = ['原色钛金属', '蓝色钛金属', '白色钛金属', '黑色钛金属']
const storageOptions = [
  { value: '128GB', label: '128GB', disabled: false },
  { value: '256GB', label: '256GB', disabled: false },
  { value: '512GB', label: '512GB', disabled: false },
  { value: '1TB', label: '1TB (售罄)', disabled: true }
]

const tabs = [
  { key: 'detail', label: '商品详情' },
  { key: 'spec', label: '规格参数' },
  { key: 'review', label: '用户评价 (326)' },
  { key: 'notice', label: '秒杀须知' }
] as const

const reviewList = [
  {
    id: 1,
    avatar: '张',
    user: '张***8',
    text: '秒杀价太香了！比官网便宜了2000块，正品无疑，序列号可查。钛金属手感确实比上一代好很多，轻了不少。物流也很快，第二天就到了。',
    time: '2026-08-01 19:23',
    spec: '原色钛金属 · 256GB'
  },
  {
    id: 2,
    avatar: '王',
    user: '王***3',
    text: 'A17 Pro 芯片确实猛，打游戏全程满帧不发热。拍照提升很大，夜景模式终于能用了。USB-C 接口方便多了，出门不用多带一根线。',
    time: '2026-08-01 20:05',
    spec: '蓝色钛金属 · 256GB'
  },
  {
    id: 3,
    avatar: '刘',
    user: '刘***6',
    text: '整体满意，就是电池续航一般，重度使用一天还是得充。不过这个价格没什么好挑剔的了，秒杀抢到就是赚到。',
    time: '2026-08-01 21:12',
    spec: '原色钛金属 · 256GB'
  }
]

/* === 计算属性 === */
const displayImages = computed<string[]>(() => {
  if (!seckill.value) return []
  return seckill.value.images || []
})

/** 缩略图列表 (最多4个, 不足4个补占位) */
const thumbImages = computed<string[]>(() => {
  const imgs = displayImages.value.slice(0, 4)
  // 补足4个 (用空字符串占位, el-image 会触发 error 插槽)
  while (imgs.length < 4) {
    imgs.push('')
  }
  return imgs
})

const discountAmount = computed<number>(() => {
  if (!seckill.value || !originalPrice.value) return 0
  return Math.max(0, originalPrice.value - seckill.value.seckillPrice)
})

const stockPercent = computed<number>(() => {
  if (!seckill.value || seckill.value.stockCount <= 0) return 0
  return Math.round((availableCount.value / seckill.value.stockCount) * 100)
})

/** 库存进度条样式类 (对照设计稿 stock-bar-fill high/mid/low) */
const stockBarClass = computed<string>(() => {
  const percent = stockPercent.value
  if (percent > 50) return 'high'
  if (percent > 20) return 'mid'
  return 'low'
})

/** 状态横幅样式类 (对照设计稿 seckill-status-bar active/pending) */
const statusBarClass = computed<string>(() => {
  if (!seckill.value) return ''
  const status = seckill.value.status
  if (status === 'ACTIVE') return 'active'
  if (status === 'PENDING') return 'pending'
  return 'ended'
})

const statusLabel = computed<string>(() => {
  if (!seckill.value) return ''
  const map: Record<string, string> = {
    PENDING: '即将开始',
    ACTIVE: '秒杀进行中',
    ENDED: '秒杀已结束',
    CANCELLED: '活动已取消'
  }
  return map[seckill.value.status] || ''
})

const statusText = computed<string>(() => {
  if (!seckill.value) return ''
  const map: Record<string, string> = {
    PENDING: '即将开抢',
    ACTIVE: '正在进行',
    ENDED: '活动已结束',
    CANCELLED: '活动已取消'
  }
  return map[seckill.value.status] || ''
})

/** 倒计时目标时间 */
const countdownTarget = computed<string>(() => {
  if (!seckill.value) return ''
  if (seckill.value.status === 'PENDING') return seckill.value.startTime
  if (seckill.value.status === 'ACTIVE') return seckill.value.endTime
  return ''
})

/** 倒计时剩余秒数 */
const countdownRemaining = computed<number>(() => {
  if (!countdownTarget.value) return 0
  if (seckill.value?.status === 'PENDING') return pendingCountdown.value
  // ACTIVE: 实时计算
  const remaining = dayjs(countdownTarget.value).diff(dayjs(), 'second')
  return Math.max(0, remaining)
})

const cdHours = computed<string>(() => {
  const s = countdownRemaining.value
  return String(Math.floor(s / 3600)).padStart(2, '0')
})

const cdMinutes = computed<string>(() => {
  const s = countdownRemaining.value
  return String(Math.floor((s % 3600) / 60)).padStart(2, '0')
})

const cdSeconds = computed<string>(() => {
  const s = countdownRemaining.value
  return String(s % 60).padStart(2, '0')
})

/** 累计销量 */
const soldCount = computed<string>(() => {
  if (!seckill.value) return '0'
  const sold = seckill.value.stockCount - availableCount.value
  return sold.toLocaleString()
})

/** 参与人数 (与销量一致) */
const participantCount = computed<string>(() => {
  if (!seckill.value) return '0'
  const sold = seckill.value.stockCount - availableCount.value
  return sold.toLocaleString()
})

/** 高亮 banner 标题 */
const bannerTitle = computed<string>(() => {
  if (!seckill.value) return ''
  // 取商品名前缀 (如 "Apple iPhone 15 Pro 256GB 原色钛金属" -> "iPhone 15 Pro")
  const name = seckill.value.productName || seckill.value.seckillName
  const match = name.match(/iPhone\s*\d+\s*Pro/i)
  return match ? match[0] : name.slice(0, 20)
})

/* === 工具函数 === */
function getSeckillId(): number {
  return Number(route.params.id)
}

function formatPrice(price: number): string {
  return Number(price || 0).toFixed(2)
}

function formatTimeShort(time: string): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

/* === 数据拉取 === */
async function fetchSeckillDetail(): Promise<void> {
  const id = getSeckillId()
  if (!id || Number.isNaN(id)) {
    error.value = true
    return
  }
  loading.value = true
  error.value = false
  try {
    const res = await getSeckillDetail(id)
    seckill.value = res.data
    availableCount.value = res.data.availableCount
    // 拉取原价 (从商品详情)
    if (res.data.productId) {
      try {
        const productRes = await getProductDetail(res.data.productId)
        originalPrice.value = productRes.data.originalPrice
      } catch {
        // 忽略原价拉取失败
      }
    }
    // 根据状态初始化
    initState()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

/** 拉取实时库存 */
async function fetchStock(): Promise<void> {
  if (!seckill.value) return
  try {
    const res = await getSeckillStock(seckill.value.id)
    availableCount.value = res.data
  } catch {
    // 忽略
  }
}

/* === 状态机初始化 === */
function initState(): void {
  if (!seckill.value) return
  const status = seckill.value.status
  if (status === 'PENDING') {
    // 启动距开始倒计时
    startPendingCountdown()
  } else if (status === 'ACTIVE') {
    // 启动库存轮询 (每3秒)
    startStockPolling()
  }
}

/** 启动距开始倒计时 (用于按钮显示) */
function startPendingCountdown(): void {
  if (!seckill.value) return
  stopPendingCountdown()
  const update = () => {
    if (!seckill.value) return
    const remaining = dayjs(seckill.value.startTime).diff(dayjs(), 'second')
    pendingCountdown.value = Math.max(0, remaining)
    // 最后5秒预取 token
    if (remaining <= 5 && remaining > 0 && !tokenPrefetched.value) {
      prefetchToken()
    }
    if (remaining <= 0) {
      stopPendingCountdown()
      // 转为 ACTIVE 状态
      handleStartEnd()
    }
  }
  update()
  pendingCountdownTimer = setInterval(update, 1000)
}

function stopPendingCountdown(): void {
  if (pendingCountdownTimer) {
    clearInterval(pendingCountdownTimer)
    pendingCountdownTimer = null
  }
}

/** 启动库存轮询 */
function startStockPolling(): void {
  stopStockPolling()
  stockTimer = setInterval(fetchStock, 3000)
}

function stopStockPolling(): void {
  if (stockTimer) {
    clearInterval(stockTimer)
    stockTimer = null
  }
}

/** 预取秒杀 token */
async function prefetchToken(): Promise<void> {
  if (!seckill.value || tokenPrefetched.value) return
  tokenPrefetched.value = true
  try {
    const res = await getSeckillToken(seckill.value.id)
    seckillToken.value = res.data
  } catch {
    // 预取失败, 抢购时再尝试
    tokenPrefetched.value = false
  }
}

/* === 轮播交互 === */
function onCarouselChange(idx: number): void {
  currentCarouselIdx.value = idx
}

function switchThumb(idx: number): void {
  currentCarouselIdx.value = idx
  // el-carousel 不支持外部直接切换, 这里仅更新缩略图高亮状态
}

/* === 倒计时事件处理 === */
/** 距开始倒计时结束 -> 转为 ACTIVE */
async function handleStartEnd(): Promise<void> {
  if (!seckill.value) return
  // 更新活动状态
  seckill.value.status = 'ACTIVE'
  pendingCountdown.value = 0
  // 拉取最新库存
  await fetchStock()
  // 启动库存轮询
  startStockPolling()
  ElMessage.success('秒杀已开始，立即抢购！')
}

/** 距结束倒计时结束 -> 转为 ENDED */
function handleActiveEnd(): void {
  if (!seckill.value) return
  seckill.value.status = 'ENDED'
  stopStockPolling()
}

/* === 抢购核心逻辑 === */
async function handleSeckill(): Promise<void> {
  if (!seckill.value) return

  // 1. 登录检查
  if (!userStore.isLoggedIn) {
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    ElMessage.warning('请先登录后再参与秒杀')
    return
  }

  // 2. 确保 token 已获取
  if (!seckillToken.value) {
    try {
      const tokenRes = await getSeckillToken(seckill.value.id)
      seckillToken.value = tokenRes.data
    } catch {
      ElMessage.error('获取秒杀资格失败，请重试')
      return
    }
  }

  // 3. 调用秒杀接口
  btnLoading.value = true
  btnState.value = 'loading'
  try {
    const res = await doSeckill(seckill.value.id, seckillToken.value)
    const result = res.data
    btnLoading.value = false

    // 4. 处理结果
    handleSeckillResult(result)
  } catch {
    btnLoading.value = false
    btnState.value = 'fail'
    failText.value = '抢购失败'
    setTimeout(() => {
      btnState.value = undefined
    }, 2000)
  }
}

/** 处理秒杀结果 */
function handleSeckillResult(result: SeckillResultVO): void {
  if (!seckill.value) return
  const status = result.status

  if (status === 0) {
    // 排队中, 进入轮询
    btnState.value = 'polling'
    pollResult(result.requestId)
  } else if (status === 1) {
    // 成功
    handleSuccess(result)
  } else if (status === -1) {
    // 售罄
    handleFailStock()
  } else if (status === -2) {
    // 重复购买
    handleFailDuplicate()
  } else {
    // 未知状态
    ElMessage.error('抢购异常，请重试')
    btnState.value = undefined
  }
}

/** 轮询秒杀结果 (最多10次, 每次间隔1秒) */
async function pollResult(requestId: string): Promise<void> {
  if (!seckill.value) return
  for (let i = 1; i <= 10; i++) {
    pollProgress.value = i
    await new Promise((resolve) => setTimeout(resolve, 1000))
    try {
      const res = await getSeckillResult(seckill.value.id, requestId)
      const result = res.data
      if (result.status !== 0) {
        // 状态已变更, 处理结果
        if (result.status === 1) {
          handleSuccess(result)
        } else if (result.status === -1) {
          handleFailStock()
        } else if (result.status === -2) {
          handleFailDuplicate()
        }
        return
      }
    } catch {
      // 单次失败, 继续下一次
    }
  }
  // 轮询超时
  ElMessage.warning('排队超时，请重试')
  btnState.value = undefined
  pollProgress.value = 0
}

/** 抢购成功 */
function handleSuccess(result: SeckillResultVO): void {
  btnState.value = 'success'
  ElMessageBox.alert(
    `恭喜您，抢购成功！\n订单号：${result.orderNo}\n请在 15 分钟内完成支付`,
    '抢购成功',
    {
      confirmButtonText: '去支付',
      type: 'success',
      callback: () => {
        router.push(`/user/orders/${result.orderId}?type=SECKILL`)
      }
    }
  )
}

/** 售罄 */
function handleFailStock(): void {
  btnState.value = 'fail'
  failText.value = '已售罄'
  availableCount.value = 0
  ElMessage.error('手慢了，商品已被抢光')
}

/** 重复购买 */
function handleFailDuplicate(): void {
  btnState.value = 'fail'
  failText.value = '已抢购'
  ElMessage.warning('您已抢购过该商品，请勿重复操作')
}

/* === 生命周期 === */
watch(
  () => route.params.id,
  () => {
    if (route.name === 'SeckillDetail') {
      cleanup()
      fetchSeckillDetail()
    }
  }
)

// 监听 ACTIVE 状态结束
watch(
  () => countdownRemaining.value,
  (val) => {
    if (val === 0 && seckill.value?.status === 'ACTIVE') {
      handleActiveEnd()
    }
  }
)

function cleanup(): void {
  stopStockPolling()
  stopPendingCountdown()
  // 重置状态
  btnLoading.value = false
  btnState.value = undefined
  failText.value = ''
  pollProgress.value = 0
  pendingCountdown.value = 0
  seckillToken.value = ''
  tokenPrefetched.value = false
  currentCarouselIdx.value = 0
  activeTab.value = 'detail'
}

onMounted(() => {
  fetchSeckillDetail()
})

onUnmounted(() => {
  cleanup()
})
</script>

<style scoped>
/* === 容器 === */
.seckill-detail-page {
  padding-bottom: 24px;
}

.seckill-detail {
  padding: 24px;
}

.loading-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

/* === 状态横幅 (对照 .seckill-status-bar) === */
.seckill-status-bar {
  padding: 10px 20px;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.seckill-status-bar.active {
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  color: #fff;
}

.seckill-status-bar.pending {
  background: var(--tag-unpaid-bg);
  color: var(--tag-unpaid-fg);
  border: 1px solid var(--pending-border);
}

.seckill-status-bar.ended {
  background: var(--color-info-light);
  color: var(--color-text-secondary);
}

.status-label {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.status-cd {
  display: flex;
  gap: 4px;
  align-items: center;
  font-size: 13px;
}

/* 倒计时方块 (对照 .cd-block) */
.cd-block {
  background: var(--cd-bg);
  color: #fff;
  font-family: var(--font-price);
  font-size: 14px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 3px;
  min-width: 28px;
  text-align: center;
  display: inline-block;
}

/* active 状态下倒计时方块为半透明白色背景 (对照设计稿) */
.seckill-status-bar.active .cd-block {
  background: rgba(255, 255, 255, 0.2);
}

.cd-sep {
  color: inherit;
  font-weight: 700;
}

.seckill-status-bar.active .cd-sep {
  color: #fff;
}

.status-text {
  font-size: 13px;
  font-weight: 600;
}

/* === 详情主体 grid (对照 .detail-grid) === */
.detail-grid {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 24px;
}

.detail-left {
  width: 400px;
}

/* === 图片轮播 (对照 .detail-carousel) === */
.detail-carousel {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg);
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.empty-carousel {
  color: var(--color-text-muted);
}

.carousel-image {
  width: 100%;
  height: 360px;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
}

/* === 缩略图条 (对照设计稿第 1075-1080 行) === */
.thumb-strip {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.thumb-item {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}

.thumb-item.active {
  border: 2px solid var(--color-primary);
}

.thumb-img {
  width: 100%;
  height: 100%;
}

/* === 右列信息 (对照 .detail-info) === */
.detail-info h2 {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 8px;
  letter-spacing: -0.01em;
  color: var(--color-text-primary);
  line-height: 1.4;
}

.detail-info .sub-name {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

/* === 卖点标签 (对照设计稿第 1086-1090 行) === */
.selling-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.sell-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 3px;
}

.tag-red {
  color: var(--color-primary);
  background: var(--price-bg);
}

.tag-orange {
  color: var(--color-accent);
  background: var(--tag-unpaid-bg);
}

.tag-green {
  color: var(--color-success);
  background: var(--tag-paid-bg);
}

/* === 价格块 (对照 .price-block) === */
.price-block {
  background: var(--price-bg);
  padding: 16px;
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  width: 60px;
}

.price-seckill {
  font-family: var(--font-price);
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
}

.price-seckill::before {
  content: '\A5';
  font-size: 18px;
}

.price-original {
  font-size: 14px;
  color: var(--color-text-secondary);
  text-decoration: line-through;
}

.price-discount {
  background: var(--color-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 2px;
  margin-left: 8px;
}

.price-stats {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 11px;
  color: var(--color-text-secondary);
}

/* === 规格选择 (对照设计稿第 1109-1129 行) === */
.spec-row {
  margin-bottom: 14px;
}

.spec-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.spec-value {
  color: var(--color-text-primary);
  font-weight: 600;
}

.spec-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.spec-option {
  padding: 5px 14px;
  font-size: 12px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.spec-option:hover:not(.disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.spec-option.active {
  border: 2px solid var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
  background: var(--price-bg);
  padding: 4px 13px;
}

.spec-option.disabled {
  color: var(--color-text-secondary);
  text-decoration: line-through;
  cursor: not-allowed;
}

/* === 服务保障 (对照设计稿第 1132-1137 行) === */
.service-row {
  display: flex;
  gap: 16px;
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-bottom: 14px;
  padding: 10px 12px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.service-icon {
  color: var(--color-success);
  font-size: 12px;
}

/* === 商品参数 (对照 .detail-meta) === */
.detail-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
  font-size: 13px;
}

.detail-meta dt {
  color: var(--color-text-secondary);
}

.detail-meta dd {
  font-weight: 600;
  color: var(--color-text-primary);
}

/* === 库存进度条 (对照 .stock-inline) === */
.stock-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.stock-inline-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.stock-bar {
  flex: 1;
  height: 8px;
  background: var(--color-bg-muted);
  border-radius: 3px;
  overflow: hidden;
}

.stock-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
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

.stock-num {
  font-size: 12px;
  color: var(--color-primary);
  font-weight: 700;
}

/* === 秒杀按钮外层 === */
.seckill-action {
  margin-bottom: 8px;
}

.action-tip {
  text-align: center;
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}

/* === 下方标签页 (对照设计稿第 1158-1210 行) === */
.tab-section {
  margin-top: 32px;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  border: 1px solid var(--color-border);
  border-bottom: none;
}

.tab-item {
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item.active {
  font-weight: 700;
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-content {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-top: none;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  padding: 24px;
}

/* === 高亮 banner (对照设计稿第 1170-1173 行) === */
.highlight-banner {
  background: linear-gradient(135deg, var(--nav-bg), var(--nav-border));
  border-radius: var(--radius-lg);
  padding: 40px;
  text-align: center;
  margin-bottom: 24px;
}

.banner-title {
  font-size: 24px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.banner-subtitle {
  font-size: 14px;
  color: var(--nav-link);
}

/* === 特性网格 (对照设计稿第 1176-1189 行) === */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.feature-card {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
}

.feature-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* === 详情图占位 (对照设计稿第 1192-1209 行) === */
.detail-image-placeholder {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg);
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: var(--color-text-secondary);
  font-size: 13px;
  gap: 8px;
}

/* === 规格参数表格 (对照设计稿第 1213-1226 行) === */
.spec-section {
  margin-top: 24px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.spec-section-title,
.spec-table-title {
  font-size: 15px;
  font-weight: 800;
  margin-bottom: 16px;
  color: var(--color-text-primary);
}

.spec-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.spec-table tr {
  border-bottom: 1px solid var(--color-border);
}

.spec-table tr:last-child {
  border-bottom: none;
}

.spec-key {
  padding: 10px 12px;
  color: var(--color-text-secondary);
  width: 120px;
  background: var(--color-bg-subtle);
}

.spec-val {
  padding: 10px 12px;
  color: var(--color-text-primary);
}

/* === 用户评价 (对照设计稿第 1228-1281 行) === */
.review-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.review-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--color-text-primary);
}

.review-count {
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-secondary);
}

.review-rate {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.rate-percent {
  color: var(--color-accent);
  font-weight: 700;
  font-size: 20px;
}

.rate-label {
  color: var(--color-text-secondary);
}

.review-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.review-tag {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.review-tag.active {
  background: var(--price-bg);
  color: var(--color-primary);
  font-weight: 600;
}

.review-list {
  border-top: 1px solid var(--color-border);
  padding-top: 16px;
}

.review-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.review-item:last-child {
  margin-bottom: 0;
}

.review-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-bg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.review-body {
  flex: 1;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.review-user {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.review-stars {
  font-size: 11px;
}

.star-filled {
  color: var(--color-accent);
}

.review-text {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: 6px;
}

.review-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
}

/* === 秒杀须知 (对照设计稿第 1283-1292 行) === */
.notice-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--tag-unpaid-fg);
  margin-bottom: 12px;
}

.notice-content {
  font-size: 12px;
  color: var(--tag-unpaid-fg);
  line-height: 2;
  opacity: 0.85;
}

.notice-content p {
  margin: 0;
}

/* === 响应式 === */
@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-left {
    width: 100%;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .tab-item {
    padding: 12px 16px;
    font-size: 13px;
  }
}
</style>
