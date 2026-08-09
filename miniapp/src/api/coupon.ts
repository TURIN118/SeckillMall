/**
 * 优惠券 API（对齐 spec.md 2.6 优惠券端点）
 * /api/v1/coupons
 */

import { get } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import type { CouponVO, CouponQuery, PageResult } from '@/types'

/** 优惠券列表 */
export function getCouponList(params?: CouponQuery): Promise<PageResult<CouponVO>> {
  return get<PageResult<CouponVO>>('/coupons', params)
}

/** 可用优惠券（下单时） */
export function getAvailableCoupons(amount: number): Promise<CouponVO[]> {
  return get<CouponVO[]>('/coupons/available', { amount })
}

/** 优惠券详情 */
export function getCouponDetail(id: string): Promise<CouponVO> {
  return get<CouponVO>(`/coupons/${encodeId(id)}`)
}