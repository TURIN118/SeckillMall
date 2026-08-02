/**
 * 轮播图 API
 */
import { get, post, put, del } from './request'
import type { Result, BannerVO } from '@/types'

/** 查所有轮播图（后台） */
export function getBannerList(): Promise<Result<BannerVO[]>> {
    return get<BannerVO[]>('/api/v1/admin/banners/list')
}

/** 新增轮播图 */
export function createBanner(data: Partial<BannerVO>): Promise<Result<BannerVO>> {
    return post<BannerVO>('/api/v1/admin/banners/create', data)
}

/** 编辑轮播图 */
export function updateBanner(id: number, data: Partial<BannerVO>): Promise<Result<BannerVO>> {
    return put<BannerVO>(`/api/v1/admin/banners/${id}`, data)
}

/** 删除轮播图 (逻辑删除) */
export function deleteBanner(id: number): Promise<Result<void>> {
    return del<void>(`/api/v1/admin/banners/${id}`)
}

/** 切换轮播图状态 */
export function updateBannerStatus(id: number, status: number): Promise<Result<void>> {
    return put<void>(`/api/v1/admin/banners/${id}/status`, { status })
}

/** 查启用轮播图（前台首页，无需登录） */
export function getActiveBanners(): Promise<Result<BannerVO[]>> {
    return get<BannerVO[]>('/api/v1/banners/active')
}