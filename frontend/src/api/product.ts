/**
 * 商品 API - 严格匹配 default.md
 *
 * C5 修复: 所有雪花 ID 在前端全程使用 string 类型, 避免 Number 精度丢失
 * (雪花 ID 如 2085560004061081601 超过 JS Number.MAX_SAFE_INTEGER = 2^53-1).
 * axios 路径参数统一用 encodeURIComponent 编码, 防止特殊字符破坏 URL.
 */
import { get, post, put, del } from './request'
import type {
  Result,
  PageResult,
  ProductVO,
  ProductQueryRequest,
  ProductCreateRequest,
  ProductUpdateRequest
} from '@/types'

/** 商品列表分页 */
export function getProductList(
  params: ProductQueryRequest
): Promise<Result<PageResult<ProductVO>>> {
  return get<PageResult<ProductVO>>('/api/v1/products', params)
}

/** 商品详情 */
export function getProductDetail(id: number | string): Promise<Result<ProductVO>> {
  // C5 修复: ID 全程 string, encodeURIComponent 防止精度丢失与特殊字符破坏 URL
  return get<ProductVO>(`/api/v1/products/${encodeURIComponent(String(id))}`)
}

/** 新增商品 */
export function createProduct(data: ProductCreateRequest): Promise<Result<ProductVO>> {
  return post<ProductVO>('/api/v1/products', data)
}

/** 编辑商品 */
export function updateProduct(id: number | string, data: ProductUpdateRequest): Promise<Result<ProductVO>> {
  return put<ProductVO>(`/api/v1/products/${encodeURIComponent(String(id))}`, data)
}

/** 删除商品 (逻辑删除) */
export function deleteProduct(id: number | string): Promise<Result<void>> {
  return del<void>(`/api/v1/products/${encodeURIComponent(String(id))}`)
}