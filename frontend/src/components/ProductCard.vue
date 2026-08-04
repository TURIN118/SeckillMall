<template>
  <!-- 纯 div 结构，严格对照 index.html .p-card 样式 -->
  <div class="p-card" :class="{ disabled }" @click="handleClick">
    <!-- 图片容器 -->
    <div class="p-card-img">
      <el-image v-if="displayImage" :src="displayImage" fit="cover" lazy class="card-image">
        <template #error>
          <div class="img-placeholder">
            <el-icon :size="48">
              <Picture />
            </el-icon>
          </div>
        </template>
      </el-image>
      <div v-else class="img-placeholder">
        <el-icon :size="48">
          <Picture />
        </el-icon>
      </div>
      <!-- 售罄遮罩 -->
      <div v-if="disabled || isSoldOut" class="sold-out-overlay">已售罄</div>
      <!-- 即将开始遮罩 -->
      <div v-if="isPending" class="pending-overlay">
        <span>距开始 {{ pendingCountdownText }}</span>
        <span class="pending-sub">{{ pendingOpenText }} 开抢</span>
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="p-card-body">
      <div class="p-card-name">{{ displayName }}</div>
      <div class="p-card-prices">
        <span v-if="seckillPrice !== null" class="p-card-price">{{ seckillPrice }}</span>
        <span v-if="originalPrice" class="p-card-original">¥{{ originalPrice }}</span>
      </div>

      <!-- 库存进度条 -->
      <div v-if="showStock && stockCount > 0" class="stock-section">
        <div class="stock-bar">
          <div class="stock-bar-fill" :class="stockLevel" :style="{ width: soldPercent + '%' }"></div>
        </div>
        <div class="stock-text">已抢{{ soldPercent }}% · 剩余{{ availableCount }}件</div>
      </div>

      <!-- 倒计时 -->
      <div v-if="showCountdown && countdownTarget" class="card-countdown">
        <SeckillCountdown :target-time="countdownTarget" size="small" @end="$emit('countdown-end')" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 商品卡片组件 C02
 * 严格对照 index.html .p-card 样式（纯 div 结构，不用 el-card）
 * 库存文本格式：已抢 X% · 剩余 Y 件
 */
import { computed } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import SeckillCountdown from './SeckillCountdown.vue'
import dayjs from 'dayjs'
import { formatImageUrl } from '@/utils/image'
import type { ProductVO, SeckillGoodsVO } from '@/types'

interface Props {
  /** 商品数据 (ProductVO 或 SeckillGoodsVO) */
  product: ProductVO | SeckillGoodsVO
  /** 是否显示倒计时 */
  showCountdown?: boolean
  /** 是否显示库存条 */
  showStock?: boolean
  /** 是否禁用 (已结束) */
  disabled?: boolean
  /** 倒计时目标时间 */
  countdownTarget?: string
}

const props = withDefaults(defineProps<Props>(), {
  showCountdown: false,
  showStock: false,
  disabled: false,
  countdownTarget: ''
})

const emit = defineEmits<{
  (e: 'click', product: ProductVO | SeckillGoodsVO): void
  (e: 'countdown-end'): void
}>()

/** 是否为秒杀商品 */
const isSeckill = computed(() => 'seckillName' in props.product)

const displayName = computed(() => {
  if (isSeckill.value) {
    return (props.product as SeckillGoodsVO).seckillName || (props.product as SeckillGoodsVO).productName
  }
  return (props.product as ProductVO).productName
})

const displayImage = computed(() => {
  const images = props.product.images
  return images && images.length > 0 ? formatImageUrl(images[0]) : ''
})

const seckillPrice = computed<number | null>(() => {
  if (isSeckill.value) {
    return (props.product as SeckillGoodsVO).seckillPrice
  }
  return null
})

const originalPrice = computed<number | null>(() => {
  if (!isSeckill.value) {
    return (props.product as ProductVO).originalPrice
  }
  return null
})

const stockCount = computed(() => {
  if (isSeckill.value) {
    return (props.product as SeckillGoodsVO).stockCount
  }
  return (props.product as ProductVO).stock
})

const availableCount = computed(() => {
  if (isSeckill.value) {
    return (props.product as SeckillGoodsVO).availableCount
  }
  return (props.product as ProductVO).stock
})

const isSoldOut = computed(() => availableCount.value <= 0)

const isPending = computed(() => {
  if (isSeckill.value) {
    return (props.product as SeckillGoodsVO).status === 'PENDING'
  }
  return false
})

/** 已抢百分比（库存条填充宽度） */
const soldPercent = computed(() => {
  if (stockCount.value <= 0) return 0
  return Math.round(((stockCount.value - availableCount.value) / stockCount.value) * 100)
})

/** 库存剩余比例对应等级（基于剩余量） */
const stockLevel = computed(() => {
  const remainPercent = stockCount.value <= 0 ? 0 : (availableCount.value / stockCount.value) * 100
  if (remainPercent > 50) return 'high'
  if (remainPercent > 20) return 'mid'
  return 'low'
})

/** 即将开始倒计时文本（距开始 HH:mm） */
const pendingCountdownText = computed(() => {
  if (!isSeckill.value) return ''
  const startTime = (props.product as SeckillGoodsVO).startTime
  if (!startTime) return '--:--'
  const diff = dayjs(startTime).diff(dayjs(), 'minute')
  if (diff <= 0) return '00:00'
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
})

/** 开抢时间文本（HH:mm） */
const pendingOpenText = computed(() => {
  if (!isSeckill.value) return ''
  const startTime = (props.product as SeckillGoodsVO).startTime
  if (!startTime) return '--:--'
  return dayjs(startTime).format('HH:mm')
})

function handleClick(): void {
  if (props.disabled) return
  emit('click', props.product)
}
</script>

<style scoped>
/* 严格对照 index.html .p-card 样式 */
.p-card {
  width: 200px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow 0.2s;
  cursor: pointer;
}

.p-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.p-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.p-card-img {
  width: 100%;
  height: 160px;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

/* 售罄遮罩 */
.sold-out-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

/* 即将开始遮罩 */
.pending-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 600;
}

.pending-sub {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.p-card-body {
  padding: 12px;
}

/* 单行省略 */
.p-card-name {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.p-card-prices {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}

/* 价格 ¥ 前缀通过 ::before content: '\A5' */
.p-card-price {
  font-family: var(--font-price);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.p-card-price::before {
  content: '\A5';
  font-size: 13px;
}

.p-card-original {
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: line-through;
}

.stock-section {
  margin-top: 4px;
}

/* 库存进度条：high 绿 / mid 黄 / low 红 */
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

.stock-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.card-countdown {
  margin-top: 8px;
}
</style>
