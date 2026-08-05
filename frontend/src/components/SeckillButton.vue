<template>
  <button class="seckill-btn" :class="stateClass" :disabled="isDisabled" @click="handleClick">
    <el-icon v-if="isLoading" class="loading-icon">
      <Loading />
    </el-icon>
    <span class="btn-text">{{ buttonText }}</span>
  </button>
</template>

<script setup lang="ts">
/**
 * 秒杀按钮组件 C03 (核心)
 * 参照 10-ai-design-spec.md C03 规范 + index.html .seckill-btn 样式
 * 内部状态机: disabled/active/loading/polling/success/fail
 */
import { computed, ref } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { SeckillStatus } from '@/types'

interface Props {
  /** 秒杀活动状态 */
  seckillStatus: SeckillStatus
  /** 可用库存 */
  availableCount: number
  /** 外部 loading 状态 */
  loading?: boolean
  /** 距开始倒计时秒数 */
  countdown?: number
  /** 按钮内部状态 (覆盖自动计算) */
  state?: 'active' | 'loading' | 'polling' | 'success' | 'fail' | 'disabled'
  /** 失败时的自定义文本 */
  failText?: string
  /** 轮询进度 (N/10) */
  pollProgress?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  countdown: 0,
  state: undefined,
  failText: '',
  pollProgress: 0
})

const emit = defineEmits<{
  (e: 'seckill'): void
}>()

/** 按钮状态 */
type ButtonState = 'disabled' | 'active' | 'loading' | 'polling' | 'success' | 'fail'

/** 计算默认状态 (无外部 state 覆盖时) */
function computeDefaultState(): ButtonState {
  // 已结束
  if (props.seckillStatus === 'ENDED') return 'disabled'
  // 已取消
  if (props.seckillStatus === 'CANCELLED') return 'disabled'
  // 售罄
  if (props.availableCount <= 0) return 'disabled'
  // 待开始
  if (props.seckillStatus === 'PENDING') {
    if (props.countdown <= 0) return 'active'
    return 'disabled'
  }
  // 进行中且有库存
  if (props.seckillStatus === 'ACTIVE' && props.availableCount > 0) return 'active'
  return 'disabled'
}

const currentState = computed<ButtonState>(() => {
  // 外部强制状态优先
  if (props.state) return props.state
  // loading 优先
  if (props.loading) return 'loading'
  return computeDefaultState()
})

const stateClass = computed(() => `btn-${currentState.value}`)

const isDisabled = computed(() => {
  const s = currentState.value
  return (
    s === 'disabled' ||
    s === 'loading' ||
    s === 'polling' ||
    s === 'success' ||
    s === 'fail'
  )
})

const isLoading = computed(() => currentState.value === 'loading')

const buttonText = computed(() => {
  switch (currentState.value) {
    case 'active':
      return '立即抢购'
    case 'loading':
      return '抢购中...'
    case 'polling':
      return props.pollProgress > 0
        ? `排队中... (${props.pollProgress}/10)`
        : '排队中...'
    case 'success':
      return '抢购成功'
    case 'fail':
      return props.failText || '已售罄'
    case 'disabled':
    default:
      if (props.seckillStatus === 'ENDED') return '秒杀已结束'
      if (props.seckillStatus === 'CANCELLED') return '活动已取消'
      if (props.availableCount <= 0) return '已售罄'
      if (props.seckillStatus === 'PENDING' && props.countdown > 0) {
        return `距开始 ${props.countdown}s`
      }
      return '立即抢购'
  }
})

/**
 * L26 修复: 防重复点击标志
 * emit('seckill') 后短暂禁用 300ms，防止用户连点触发多次抢购
 */
const justEmitted = ref<boolean>(false)
let emitResetTimer: ReturnType<typeof setTimeout> | null = null

function handleClick(): void {
  if (isDisabled.value) return
  // L26 修复: 防重复点击，刚刚 emit 过则忽略
  if (justEmitted.value) return
  if (currentState.value === 'active') {
    justEmitted.value = true
    emit('seckill')
    if (emitResetTimer) clearTimeout(emitResetTimer)
    emitResetTimer = setTimeout(() => {
      justEmitted.value = false
      emitResetTimer = null
    }, 300)
  }
}
</script>

<style scoped>
.seckill-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 0.02em;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.seckill-btn:disabled {
  cursor: not-allowed;
}

.btn-disabled {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
}

.btn-active {
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(229, 57, 53, 0.3);
}

.btn-active:hover {
  background: var(--btn-hover);
  transform: translateY(-1px);
}

.btn-loading {
  background: var(--btn-loading-bg);
  color: #fff;
  cursor: wait;
}

.btn-polling {
  background: var(--btn-polling-bg);
  color: var(--btn-polling-fg);
  cursor: wait;
}

.btn-success {
  background: var(--color-success);
  color: #fff;
}

.btn-fail {
  background: var(--btn-disabled-bg);
  color: var(--btn-fail-fg);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
