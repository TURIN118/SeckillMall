/**
 * AI 相关 API
 *
 * T11 前端埋点 SDK - 批量上报埋点事件接口
 * 后端接口: POST /api/v1/track/event (批量上报)
 */
import { post } from './request'
import type { Result } from '@/types'

/** 单条埋点事件 */
export interface TrackItem {
  /** 事件类型 (VIEW / CLICK / ADD_CART / FAVORITE / ORDER / SEARCH ...) */
  eventType: string
  /** 目标类型 (如 product / category / banner) */
  targetType?: string
  /** 目标 ID (商品 ID / 分类 ID 等) */
  targetId?: string | number
  /** 扩展字段 (JSON 字符串, 业务自定义) */
  ext?: string
}

/** 批量上报埋点事件请求体 */
export interface TrackEventRequest {
  events: TrackItem[]
}

/**
 * 批量上报埋点事件
 * POST /api/v1/track/event
 */
export function trackEvent(data: TrackEventRequest): Promise<Result<void>> {
  return post<void>('/api/v1/track/event', data)
}