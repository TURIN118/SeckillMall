/**
 * 优惠券 API
 * 包含前台(领取/我的优惠券)和后台(管理)接口
 */
import { get, post, put, del } from './request'
import type {
    Result,
    PageResult,
    CouponVO,
    UserCouponVO,
    CouponRequest,
    UserCouponStatus
} from '@/types'

/* ==================== 前台接口 (需登录) ==================== */


/** 查询我的优惠券列表 (按状态筛选) */
export function getMyCoupons(status?: UserCouponStatus): Promise<Result<UserCouponVO[]>> {
    const params: Record<string, unknown> = {}
    if (status) params.status = status
    return get<UserCouponVO[]>('/api/v1/coupons/mine', params)
}

/* ==================== 后台接口 (ADMIN) ==================== */

/** 后台分页查询优惠券列表 */
export function adminGetCouponList(
    pageNum = 1,
    pageSize = 10
): Promise<Result<PageResult<CouponVO>>> {
    return get<PageResult<CouponVO>>('/api/v1/admin/coupons/list', { pageNum, pageSize })
}

/** 后台新增优惠券 */
export function adminCreateCoupon(data: CouponRequest): Promise<Result<CouponVO>> {
    return post<CouponVO>('/api/v1/admin/coupons/create', data)
}

/** 后台编辑优惠券 */
export function adminUpdateCoupon(id: number | string, data: CouponRequest): Promise<Result<CouponVO>> {
    return put<CouponVO>(`/api/v1/admin/coupons/${id}/update`, data)
}

/** 后台删除优惠券 */
export function adminDeleteCoupon(id: number | string): Promise<Result<void>> {
    return del<void>(`/api/v1/admin/coupons/${id}`)
}

/** 后台启停优惠券 (status: 1-启用 / 0-停用) */
export function adminUpdateCouponStatus(id: number | string, status: number): Promise<Result<void>> {
    return put<void>(`/api/v1/admin/coupons/${id}/status`, { status })
}

/** 后台发放优惠券给指定用户 */
export function adminDistributeCoupon(id: number | string, userId: number): Promise<Result<void>> {
    return post<void>(`/api/v1/admin/coupons/${id}/distribute`, { userId })
}