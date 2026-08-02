/**
 * 商品评论 API
 */
import { get, post, put } from './request'
import type { Result, PageResult, ProductReviewVO, ReviewCreateRequest } from '@/types'

/** 查商品评论分页（公开接口） */
export function getProductReviews(
    productId: number,
    pageNum: number = 1,
    pageSize: number = 10
): Promise<Result<PageResult<ProductReviewVO>>> {
    return get<PageResult<ProductReviewVO>>(`/api/v1/reviews/product/${productId}`, {
        pageNum,
        pageSize
    })
}

/** 发表评论（需登录） */
export function createReview(
    data: ReviewCreateRequest
): Promise<Result<ProductReviewVO>> {
    return post<ProductReviewVO>('/api/v1/reviews/create', data)
}

/** 后台查所有评论（可按 status 筛选） */
export function getReviewList(params: {
    status?: number
    pageNum?: number
    pageSize?: number
}): Promise<Result<PageResult<ProductReviewVO>>> {
    return get<PageResult<ProductReviewVO>>('/api/v1/admin/reviews/list', params)
}

/** 后台回复评论 */
export function replyReview(
    id: number,
    replyContent: string
): Promise<Result<void>> {
    return put<void>(`/api/v1/admin/reviews/${id}/reply`, { replyContent })
}

/** 后台隐藏/显示评论 */
export function updateReviewStatus(
    id: number,
    status: number
): Promise<Result<void>> {
    return put<void>(`/api/v1/admin/reviews/${id}/status`, { status })
}