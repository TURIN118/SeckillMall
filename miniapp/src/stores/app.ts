/**
 * 应用 Store（对齐 plan.md 第 5.4 节）
 * 职责：timeOffset / serverTime / syncServerTime / getServerTime
 * 持久化：不持久化（timeOffset 内存态）
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  syncServerTime as syncTime,
  getServerTime as getServer,
  getTimeOffset
} from '@/utils/timeSync'

export const useAppStore = defineStore('app', () => {
  const timeOffset = ref<number>(0)
  const serverTime = ref<number>(0)

  /** 同步服务器时间 */
  function syncServerTime(timestamp: string) {
    timeOffset.value = syncTime(timestamp)
    serverTime.value = getServer()
  }

  /** 获取服务器当前时间（毫秒） */
  function getServerTime(): number {
    return getServer()
  }

  /** 获取时间偏移量 */
  function getTimeOffsetValue(): number {
    return getTimeOffset()
  }

  return {
    timeOffset,
    serverTime,
    syncServerTime,
    getServerTime,
    getTimeOffsetValue
  }
})