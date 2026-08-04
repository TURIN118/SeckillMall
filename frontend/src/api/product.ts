/**
 * 商品 API - 严格匹配 default.md
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
  return get<ProductVO>(`/api/v1/products/${id}`)
}

/** 新增商品 */
export function createProduct(data: ProductCreateRequest): Promise<Result<ProductVO>> {
  return post<ProductVO>('/api/v1/products', data)
}

/** 编辑商品 */
export function updateProduct(id: number | string, data: ProductUpdateRequest): Promise<Result<ProductVO>> {
  return put<ProductVO>(`/api/v1/products/${id}`, data)
}

/** 删除商品 (逻辑删除) */
export function deleteProduct(id: number | string): Promise<Result<void>> {
  return del<void>(`/api/v1/products/${id}`)
}