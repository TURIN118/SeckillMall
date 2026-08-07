/**
 * 秒杀 API - 严格匹配 default.md
 *
 * B2 重构: 防重放改用服务端签发的一次性 token, 不再使用前端 HMAC 签名.
 * H-F1 修复: 移除对 VITE_SIGN_SECRET 的依赖, 避免生产构建 tree-shake 导致 401.
 */
import { get, post, put, del } from './request'
import { buildSeckillHeaders } from '@/utils/replayProtection'
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

/** 执行秒杀（B2 重构: 携带后端签发的一次性 token, 不再使用前端 HMAC 签名） */
export async function doSeckill(
  seckillId: number | string,
  seckillToken: string
): Promise<Result<SeckillResultVO>> {
  const uri = `/api/v1/seckill/${seckillId}`
  // B2 重构: 使用后端签发的 token, 由 buildSeckillHeaders 构建请求头
  const replayHeaders = buildSeckillHeaders(seckillToken)
  return post<SeckillResultVO>(uri, undefined, {
    params: { seckillToken },
    headers: replayHeaders
  })
}

/** 一键执行秒杀下单（无需预取 token，后端在 /execute 端点内部处理资格校验）
 *  B2 重构: 该端点由后端内部校验资格, 前端无需携带防重放头 */
export async function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
  const uri = `/api/v1/seckill/${seckillId}/execute`
  return post<SeckillResultVO>(uri)
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