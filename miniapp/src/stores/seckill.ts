/**
 * 秒杀 Store（对齐 plan.md 第 5.1 节）
 * 职责：seckillList / activities / currentSession / fetchSeckillList
 * 持久化：不持久化
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as seckillApi from '@/api/seckill'
import type { SeckillGoodsVO, SeckillActivityVO, SeckillResultVO } from '@/types'

export const useSeckillStore = defineStore('seckill', () => {
  const seckillList = ref<SeckillGoodsVO[]>([])
  const activities = ref<SeckillActivityVO[]>([])
  const currentActivity = ref<SeckillActivityVO | null>(null)
  const loading = ref<boolean>(false)

  /** 拉取秒杀列表 */
  async function fetchSeckillList(params?: any) {
    loading.value = true
    try {
      const res = await seckillApi.getSeckillList(params)
      seckillList.value = res.list
      return res
    } finally {
      loading.value = false
    }
  }

  /** 拉取秒杀活动 */
  async function fetchActivities() {
    activities.value = await seckillApi.getSeckillActivities()
    if (activities.value.length > 0 && !currentActivity.value) {
      currentActivity.value = activities.value[0]
    }
    return activities.value
  }

  /** 切换当前活动场次 */
  function setCurrentActivity(activity: SeckillActivityVO) {
    currentActivity.value = activity
  }

  /** 执行秒杀（获取 token → execute） */
  async function executeSeckill(seckillId: string, data?: any): Promise<SeckillResultVO> {
    // 1. 获取一次性 token
    const seckillToken = await seckillApi.getSeckillToken(seckillId)
    // 2. 执行秒杀
    return await seckillApi.executeSeckill(seckillId, seckillToken, data)
  }

  /** 查询秒杀结果 */
  async function getSeckillResult(seckillId: string): Promise<SeckillResultVO> {
    return await seckillApi.getSeckillResult(seckillId)
  }

  return {
    seckillList,
    activities,
    currentActivity,
    loading,
    fetchSeckillList,
    fetchActivities,
    setCurrentActivity,
    executeSeckill,
    getSeckillResult
  }
})