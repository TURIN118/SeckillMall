/**
 * 秒杀 API - 严格匹配 default.md
 */
import { get, post, put, del } from './request'
import { generateReplayHeaders } from '@/utils/replayProtection'
import type {
  Result,
  PageResult,
  SeckillGoodsVO,
  SeckillResultVO,
  SeckillCreateRequest,
  SeckillStatus,
  SeckillActivityVO,
  SeckillActivityCreateRequest
} from '@/types'

/** 秒杀活动列表 */
export function getSeckillList(params: {
  status?: SeckillStatus
  categoryId?: number | string
  pageNum?: number
  pageSize?: number
}): Promise<Result<PageResult<SeckillGoodsVO>>> {
  return get<PageResult<SeckillGoodsVO>>('/api/v1/seckill/list', params)
}

/** 秒杀活动详情 */
export function getSeckillDetail(seckillId: number | string): Promise<Result<SeckillGoodsVO>> {
  return get<SeckillGoodsVO>(`/api/v1/seckill/${seckillId}`)
}

/** 查询实时库存 */
export function getSeckillStock(seckillId: number | string): Promise<Result<number>> {
  return get<number>(`/api/v1/seckill/${seckillId}/stock`)
}

/** 获取秒杀令牌 */
export function getSeckillToken(seckillId: number | string): Promise<Result<string>> {
  return get<string>(`/api/v1/seckill/${seckillId}/token`)
}

/** 执行秒杀（带防重放签名） */
export async function doSeckill(
  seckillId: number | string,
  seckillToken: string
): Promise<Result<SeckillResultVO>> {
  const uri = `/api/v1/seckill/${seckillId}`
  const replayHeaders = await generateReplayHeaders(uri)
  return post<SeckillResultVO>(uri, undefined, {
    params: { seckillToken },
    headers: {
      'X-Seckill-Token': seckillToken,
      ...replayHeaders
    }
  })
}

/** 一键执行秒杀下单（无需预取 token，后端在 /execute 端点内部处理资格校验，带防重放签名） */
export async function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
  const uri = `/api/v1/seckill/${seckillId}/execute`
  const replayHeaders = await generateReplayHeaders(uri)
  return post<SeckillResultVO>(uri, undefined, {
    headers: replayHeaders
  })
}

/** 查询秒杀结果 */
export function getSeckillResult(
  seckillId: number | string,
  requestId: string
): Promise<Result<SeckillResultVO>> {
  return get<SeckillResultVO>(`/api/v1/seckill/${seckillId}/result`, { requestId })
}

/** 创建秒杀活动 */
export function createSeckill(data: SeckillCreateRequest): Promise<Result<SeckillGoodsVO>> {
  return post<SeckillGoodsVO>('/api/v1/seckill/admin', data)
}

/** 编辑秒杀活动 */
export function updateSeckill(
  seckillId: number | string,
  data: SeckillCreateRequest
): Promise<Result<SeckillGoodsVO>> {
  return put<SeckillGoodsVO>(`/api/v1/seckill/admin/${seckillId}`, data)
}

/** 取消秒杀活动 */
export function cancelSeckill(seckillId: number | string): Promise<Result<void>> {
  return put<void>(`/api/v1/seckill/admin/${seckillId}/cancel`)
}

/* ==================== 秒杀场次管理 API（场次化重构） ==================== */

/** 创建秒杀场次（含商品列表） */
export function createSeckillActivity(
  data: SeckillActivityCreateRequest
): Promise<Result<SeckillActivityVO>> {
  return post<SeckillActivityVO>('/api/v1/seckill/activities', data)
}

/** 查询所有秒杀场次列表 */
export function listSeckillActivities(): Promise<Result<SeckillActivityVO[]>> {
  return get<SeckillActivityVO[]>('/api/v1/seckill/activities')
}

/** 查询秒杀场次详情 */
export function getSeckillActivityDetail(
  activityId: number | string
): Promise<Result<SeckillActivityVO>> {
  return get<SeckillActivityVO>(`/api/v1/seckill/activities/${activityId}`)
}

/** 删除秒杀场次 */
export function deleteSeckillActivity(activityId: number | string): Promise<Result<void>> {
  return del<void>(`/api/v1/seckill/activities/${activityId}`)
}