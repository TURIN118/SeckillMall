/**
 * 评价 API（对齐 spec.md 2.6 评价端点）
 * /api/v1/reviews
 */

import { get, post } from '@/utils/request'
import type { ReviewVO, ReviewRequest, ReviewQuery, PageResult } from '@/types'

/** 评价列表 */
export function getReviewList(params?: ReviewQuery): Promise<PageResult<ReviewVO>> {
  return get<PageResult<ReviewVO>>('/reviews', params)
}

/** 商品评价列表 */
export function getProductReviews(productId: string, params?: ReviewQuery): Promise<PageResult<ReviewVO>> {
  return get<PageResult<ReviewVO>>('/reviews', { ...params, productId })
}

/** 提交评价 */
export function submitReview(data: ReviewRequest): Promise<ReviewVO> {
  return post<ReviewVO>('/reviews', data)
}