<!--
  CountDown 倒计时公共组件
  用途：秒杀专区页场次倒计时（距开始 / 距结束）
  关键：使用 timeSync.getTimeOffset() 校准服务器时间，避免本地时钟偏差
  对齐 spec.md 2.3 服务器时间同步 / 2.4 秒杀防重放机制 / plan.md 4.5
-->
<template>
  <view class="count-down">
    <!-- 前缀文案（如"距开始"、"距结束"） -->
    <text v-if="prefix" class="prefix">{{ prefix }}</text>
    <!-- 倒计时数字 -->
    <view class="time-box">
      <text v-if="days > 0" class="time-unit">{{ pad(days) }}</text>
      <text v-if="days > 0" class="time-sep">天</text>
      <text class="time-unit">{{ hours }}</text>
      <text class="time-sep">:</text>
      <text class="time-unit">{{ minutes }}</text>
      <text class="time-sep">:</text>
      <text class="time-unit">{{ seconds }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, onMounted } from 'vue'
import { getTimeOffset } from '@/utils/timeSync'

const props = withDefaults(
  defineProps<{
    /** 目标时间戳（毫秒，服务器时间） */
    targetTime: number
    /** 前缀文案（如"距开始"、"距结束"） */
    prefix?: string
    /** 是否自动启动（默认 true） */
    autoStart?: boolean
  }>(),
  {
    prefix: '',
    autoStart: true
  }
)

const emit = defineEmits<{
  /** 倒计时结束 */
  (e: 'end'): void
  /** 倒计时变化（剩余毫秒） */
  (e: 'change', remainMs: number): void
}>()

/** 剩余毫秒数 */
const remainMs = ref<number>(0)
/** 定时器句柄 */
let timerId: ReturnType<typeof setInterval> | null = null

/** 天 */
const days = computed(() => Math.floor(remainMs.value / 86400000))
/** 时 */
const hours = computed(() => Math.floor((remainMs.value % 86400000) / 3600000))
/** 分 */
const minutes = computed(() => Math.floor((remainMs.value % 3600000) / 60000))
/** 秒 */
const seconds = computed(() => Math.floor((remainMs.value % 60000) / 1000))

/** 两位补零 */
function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/**
 * 计算剩余时间
 * 关键：用 Date.now() + getTimeOffset() 校准服务器时间
 * timeOffset = serverTime - localTime，故 serverNow = localNow + timeOffset
 */
function calcRemain(): number {
  const serverNow = Date.now() + getTimeOffset()
  return Math.max(0, props.targetTime - serverNow)
}

/** 启动倒计时 */
function start(): void {
  stop()
  remainMs.value = calcRemain()
  emit('change', remainMs.value)
  if (remainMs.value <= 0) {
    emit('end')
    return
  }
  timerId = setInterval(() => {
    remainMs.value = calcRemain()
    emit('change', remainMs.value)
    if (remainMs.value <= 0) {
      stop()
      emit('end')
    }
  }, 1000)
}

/** 停止倒计时 */
function stop(): void {
  if (timerId !== null) {
    clearInterval(timerId)
    timerId = null
  }
}

/** 监听目标时间变化，重启倒计时 */
watch(
  () => props.targetTime,
  () => {
    if (props.autoStart) {
      start()
    }
  }
)

onMounted(() => {
  if (props.autoStart) {
    start()
  }
})

onUnmounted(() => {
  stop()
})

/** 暴露方法供父组件调用 */
defineExpose({
  start,
  stop,
  /** 获取当前剩余毫秒 */
  getRemain: () => remainMs.value
})
</script>

<style lang="scss" scoped>
.count-down {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;

  .prefix {
    font-size: 24rpx;
    color: #ffffff;
    margin-right: 4rpx;
  }

  .time-box {
    display: inline-flex;
    align-items: center;
    gap: 2rpx;
  }

  .time-unit {
    display: inline-block;
    min-width: 36rpx;
    padding: 2rpx 6rpx;
    background-color: rgba(0, 0, 0, 0.55);
    color: #ffffff;
    font-size: 24rpx;
    font-weight: 600;
    text-align: center;
    border-radius: 4rpx;
    line-height: 1.4;
  }

  .time-sep {
    color: #ffffff;
    font-size: 24rpx;
    font-weight: 600;
  }
}
</style>