/**
 * 秒杀 API（对齐 spec.md 2.6 秒杀端点 / 2.5 防重放流程）
 * /api/v1/seckill/*
 * 防重放：execute 请求需携带 X-Seckill-Token 头
 */

import { get, post } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import { buildSeckillHeaders } from '@/utils/replayProtection'
import type {
  SeckillGoodsVO,
  SeckillActivityVO,
  SeckillStockVO,
  SeckillExecuteRequest,
  SeckillResultVO,
  SeckillQuery,
  PageResult
} from '@/types'

/** 秒杀列表 */
export function getSeckillList(params?: SeckillQuery): Promise<PageResult<SeckillGoodsVO>> {
  return get<PageResult<SeckillGoodsVO>>('/seckill/list', params)
}

/** 秒杀详情 */
export function getSeckillDetail(id: string): Promise<SeckillGoodsVO> {
  return get<SeckillGoodsVO>(`/seckill/${encodeId(id)}`)
}

/** 秒杀库存 */
export function getSeckillStock(id: string): Promise<SeckillStockVO> {
  return get<SeckillStockVO>(`/seckill/${encodeId(id)}/stock`)
}

/**
 * 获取一次性秒杀 token（防重放）
 * @returns 一次性 token 字符串
 */
export function getSeckillToken(id: string): Promise<string> {
  return get<string>(`/seckill/${encodeId(id)}/token`)
}

/**
 * 执行秒杀（携带 X-Seckill-Token 头防重放）
 * @param id 秒杀商品 ID
 * @param seckillToken 一次性 token（由 getSeckillToken 获取）
 * @param data 执行参数（数量、地址）
 */
export function executeSeckill(
  id: string,
  seckillToken: string,
  data?: SeckillExecuteRequest
): Promise<SeckillResultVO> {
  return post<SeckillResultVO>(
    `/seckill/${encodeId(id)}/execute`,
    data,
    { header: buildSeckillHeaders(seckillToken) }
  )
}

/** 查询秒杀结果 */
export function getSeckillResult(id: string): Promise<SeckillResultVO> {
  return get<SeckillResultVO>(`/seckill/${encodeId(id)}/result`)
}

/** 秒杀活动列表 */
export function getSeckillActivities(): Promise<SeckillActivityVO[]> {
  return get<SeckillActivityVO[]>('/seckill/activities')
}