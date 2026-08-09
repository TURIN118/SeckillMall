/**
 * 商品 API（对齐 spec.md 2.6 商品端点）
 * /api/v1/products
 */

import { get } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import type { ProductVO, ProductQuery, PageResult } from '@/types'

/** 商品列表 */
export function getProductList(params?: ProductQuery): Promise<PageResult<ProductVO>> {
  return get<PageResult<ProductVO>>('/products', params)
}

/** 商品详情 */
export function getProductDetail(id: string): Promise<ProductVO> {
  return get<ProductVO>(`/products/${encodeId(id)}`)
}

/** 猜你喜欢（首页） */
export function getRecommendProducts(params?: { page?: number; pageSize?: number }): Promise<PageResult<ProductVO>> {
  return get<PageResult<ProductVO>>('/products/recommend', params)
}

/** 搜索商品 */
export function searchProducts(keyword: string, params?: ProductQuery): Promise<PageResult<ProductVO>> {
  return get<PageResult<ProductVO>>('/products', { ...params, keyword })
}