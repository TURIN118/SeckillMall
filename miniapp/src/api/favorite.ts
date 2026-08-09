/**
 * 收藏 API（对齐 spec.md 2.6 收藏端点）
 * /api/v1/favorites
 */

import { get, post, del } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import type { FavoriteVO, FavoriteQuery, PageResult } from '@/types'

/** 收藏列表 */
export function getFavoriteList(params?: FavoriteQuery): Promise<PageResult<FavoriteVO>> {
  return get<PageResult<FavoriteVO>>('/favorites', params)
}

/** 添加收藏 */
export function addFavorite(productId: string): Promise<any> {
  return post('/favorites', { productId })
}

/** 取消收藏 */
export function removeFavorite(id: string): Promise<any> {
  return del(`/favorites/${encodeId(id)}`)
}

/** 批量取消收藏 */
export function batchRemoveFavorites(ids: string[]): Promise<any> {
  return del('/favorites', { ids: ids.join(',') })
}

/** 检查是否已收藏 */
export function checkFavorite(productId: string): Promise<boolean> {
  return get<boolean>('/favorites/check', { productId })
}