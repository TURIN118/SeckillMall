/**
 * 轮播图 API（对齐 spec.md 2.6 轮播端点）
 * /api/v1/banners（公开接口，无需鉴权）
 */

import { get } from '@/utils/request'
import type { BannerVO } from '@/types'

/** 轮播图列表（公开接口） */
export function getBannerList(): Promise<BannerVO[]> {
  return get<BannerVO[]>('/banners', undefined, { skipAuth: true })
}