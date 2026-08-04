/**
 * 秒杀 Store - 参照 10-ai-design-spec.md "State Management / seckillStore"
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import dayjs from 'dayjs'
import * as seckillApi from '@/api/seckill'
import { getTimeOffset } from '@/api/request'
import type { SeckillGoodsVO, SeckillResultVO, SeckillStatus } from '@/types'

export const useSeckillStore = defineStore('seckill', () => {
  /* === State === */
  // M45 修复: countdownTimers 从模块级移入 store 工厂函数内部，
  // 避免多个 store 实例 (如 SSR 或测试场景) 共享同一 Map 导致定时器冲突与内存泄漏
  /** 倒计时定时器映射 */
  const countdownTimers = new Map<number, ReturnType<typeof setInterval>>()

  const activeList = ref<SeckillGoodsVO[]>([])
  const pendingList = ref<SeckillGoodsVO[]>([])
  const endedList = ref<SeckillGoodsVO[]>([])
  const cancelledList = ref<SeckillGoodsVO[]>([])
  const countdownMap = ref<Record<number, number>>({})
  const pollingMap = ref<Record<string, SeckillResultVO | null>>({})
  const timeOffset = ref<number>(0)
  const total = ref<number>(0)
  const pageNum = ref<number>(1)
  const pageSize = ref<number>(20)

  /* === Actions === */

  /** 拉取秒杀列表并按状态分组 */
  async function fetchSeckillList(params?: {
    status?: SeckillStatus
    pageNum?: number
    pageSize?: number
  }): Promise<void> {
    const res = await seckillApi.getSeckillList({
      pageNum: params?.pageNum ?? pageNum.value,
      pageSize: params?.pageSize ?? pageSize.value,
      status: params?.status
    })
    const list = res.data.list || []
    total.value = res.data.total
    pageNum.value = res.data.pageNum
    pageSize.value = res.data.pageSize
    // 同步时间偏移
    timeOffset.value = getTimeOffset()
    // 按状态分组
    activeList.value = list.filter((item) => item.status === 'ACTIVE')
    pendingList.value = list.filter((item) => item.status === 'PENDING')
    endedList.value = list.filter((item) => item.status === 'ENDED')
    cancelledList.value = list.filter((item) => item.status === 'CANCELLED')
  }

  /** 启动倒计时 */
  function startCountdown(id: number, targetTime: string): void {
    // 已存在则先清除
    stopCountdown(id)
    const update = () => {
      const remaining = dayjs(targetTime).diff(dayjs(Date.now() + timeOffset.value), 'second')
      if (remaining <= 0) {
        countdownMap.value[id] = 0
        stopCountdown(id)
      } else {
        countdownMap.value[id] = remaining
      }
    }
    update()
    const timer = setInterval(update, 1000)
    countdownTimers.set(id, timer)
  }

  /** 停止倒计时 */
  function stopCountdown(id: number): void {
    const timer = countdownTimers.get(id)
    if (timer) {
      clearInterval(timer)
      countdownTimers.delete(id)
    }
  }

  /** 停止所有倒计时 */
  function stopAllCountdowns(): void {
    countdownTimers.forEach((timer) => clearInterval(timer))
    countdownTimers.clear()
    countdownMap.value = {}
  }

  /** 轮询秒杀结果 (最多 10 次, 每次间隔 1s) */
  async function pollResult(
    seckillId: number,
    requestId: string
  ): Promise<SeckillResultVO | null> {
    for (let i = 0; i < 10; i++) {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      try {
        const res = await seckillApi.getSeckillResult(seckillId, requestId)
        if (res.data.status !== 0) {
          pollingMap.value[requestId] = res.data
          return res.data
        }
      } catch {
        // 单次轮询失败, 继续下一次
      }
    }
    pollingMap.value[requestId] = null
    return null
  }

  /** 重置 store */
  function reset(): void {
    stopAllCountdowns()
    activeList.value = []
    pendingList.value = []
    endedList.value = []
    cancelledList.value = []
    countdownMap.value = {}
    pollingMap.value = {}
    total.value = 0
  }

  return {
    activeList,
    pendingList,
    endedList,
    cancelledList,
    countdownMap,
    pollingMap,
    timeOffset,
    total,
    pageNum,
    pageSize,
    fetchSeckillList,
    startCountdown,
    stopCountdown,
    stopAllCountdowns,
    pollResult,
    reset
  }
})