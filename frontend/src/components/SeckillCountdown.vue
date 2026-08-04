<template>
  <div class="countdown" :class="[sizeClass, { urgent: isUrgent }]">
    <template v-if="expired">
      <span class="cd-block expired">已结束</span>
    </template>
    <template v-else>
      <span v-if="showDays && days > 0" class="cd-block">{{ pad(days) }}</span>
      <span v-if="showDays && days > 0" class="cd-sep">天</span>
      <span class="cd-block">{{ pad(hours) }}</span>
      <span class="cd-sep">:</span>
      <span class="cd-block">{{ pad(minutes) }}</span>
      <span class="cd-sep">:</span>
      <span class="cd-block">{{ secondsDisplay }}</span>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * 倒计时组件 C01
 * 参照 10-ai-design-spec.md C01 规范 + index.html .countdown 样式
 * 使用 dayjs, 最后10秒红色脉冲动画
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import dayjs from 'dayjs'
import { getTimeOffset } from '@/api/request'

interface Props {
  /** 目标时间 (ISO 8601) */
  targetTime: string
  /** 服务器时间 (用于偏移校准) */
  serverTime?: string
  /** 显示尺寸 */
  size?: 'small' | 'medium' | 'large'
  /** 是否显示天数 */
  showDays?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  showDays: false
})

const emit = defineEmits<{
  (e: 'end'): void
  (e: 'tick', remaining: number): void
}>()

const remaining = ref<number>(0)
const remainingMs = ref<number>(0)
const expired = ref<boolean>(false)
// C8 修复: timer 类型改为 setTimeout 的返回类型，使用递归 setTimeout 替代 setInterval
let timer: ReturnType<typeof setTimeout> | null = null

const sizeClass = computed(() => `countdown-${props.size}`)

/** 是否紧急 (最后10秒) */
const isUrgent = computed(() => !expired.value && remaining.value <= 10 && remaining.value > 0)

const days = computed(() => Math.floor(remaining.value / 86400))
const hours = computed(() => Math.floor((remaining.value % 86400) / 3600))
const minutes = computed(() => Math.floor((remaining.value % 3600) / 60))
const seconds = computed(() => remaining.value % 60)

/** 秒数显示: 紧急时显示毫秒 */
const secondsDisplay = computed(() => {
  if (isUrgent.value) {
    const ms = Math.floor(remainingMs.value % 1000 / 100)
    return `${pad(seconds.value)}.${ms}`
  }
  return pad(seconds.value)
})

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function computeRemaining(): void {
  const offset = getTimeOffset()
  const target = dayjs(props.targetTime)
  const now = dayjs(Date.now() + offset)
  const diffMs = target.diff(now, 'millisecond')
  remainingMs.value = diffMs
  remaining.value = Math.max(0, Math.floor(diffMs / 1000))
  if (diffMs <= 0) {
    expired.value = true
    remaining.value = 0
    remainingMs.value = 0
    clearTimer()
    emit('end')
  } else {
    emit('tick', remaining.value)
  }
}

function startTimer(): void {
  clearTimer()
  computeRemaining()
  if (expired.value) return
  // C8 修复: 使用 setTimeout 递归调用替代 setInterval，
  // 每次回调后根据当前剩余时间动态决定下次延迟 (<=60s 用 100ms，否则 1000ms)，
  // 避免原实现中 interval 闭包变量不更新导致频繁重建定时器的缺陷
  const tick = (): void => {
    computeRemaining()
    if (expired.value) return
    const delay = remaining.value <= 60 ? 100 : 1000
    timer = setTimeout(tick, delay)
  }
  const delay = remaining.value <= 60 ? 100 : 1000
  timer = setTimeout(tick, delay)
}

function clearTimer(): void {
  if (timer) {
    // C8 修复: 配合 setTimeout 使用 clearTimeout
    clearTimeout(timer)
    timer = null
  }
}

watch(
  () => props.targetTime,
  () => {
    expired.value = false
    startTimer()
  }
)

onMounted(() => {
  startTimer()
})

onUnmounted(() => {
  clearTimer()
})
</script>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-price);
}

.cd-block {
  background: var(--cd-bg);
  color: #fff;
  border-radius: 4px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cd-sep {
  color: var(--color-text-secondary);
  font-weight: 700;
}

.countdown-small .cd-block {
  font-size: 12px;
  padding: 1px 4px;
  min-width: 22px;
}

.countdown-medium .cd-block {
  font-size: 16px;
  padding: 3px 7px;
}

.countdown-large .cd-block {
  font-size: 22px;
  padding: 5px 10px;
  min-width: 36px;
}

/* 紧急状态: 红色脉冲动画 */
.countdown.urgent .cd-block {
  background: var(--color-primary);
  animation: pulse-red 1s infinite;
}

.cd-block.expired {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
}

@keyframes pulse-red {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.15);
  }
}
</style>