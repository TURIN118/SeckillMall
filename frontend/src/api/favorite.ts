/**
 * 收藏夹 API - 对接后端 /api/v1/favorites
 */
import { get, post, del } from './request'
import type { Result, FavoriteItemVO, FavoriteAddRequest } from '@/types'

/** 获取收藏夹列表 */
export function getFavoriteList(): Promise<Result<FavoriteItemVO[]>> {
    return get<FavoriteItemVO[]>('/api/v1/favorites/list')
}

/** 添加收藏 */
export function addFavorite(data: FavoriteAddRequest): Promise<Result<void>> {
    return post<void>('/api/v1/favorites/add', data)
}

/** 取消收藏 */
export function removeFavorite(productId: number | string): Promise<Result<void>> {
    return del<void>(`/api/v1/favorites/${productId}`)
}

/** 检查商品是否已收藏 */
export function checkFavorite(productId: number | string): Promise<Result<boolean>> {
    return get<boolean>(`/api/v1/favorites/check/${productId}`)
}
