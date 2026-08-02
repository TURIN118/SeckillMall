/**
 * 分类 API - 严格匹配 default.md
 */
import { get, post, put, del } from './request'
import type { Result, CategoryVO } from '@/types'

/** 分类树 */
export function getCategoryTree(): Promise<Result<CategoryVO[]>> {
  return get<CategoryVO[]>('/api/v1/categories')
}

/** 新增分类请求体 */
export interface CategoryCreateRequest {
  categoryName: string
  parentId: number
  sortOrder?: number
  status?: number
}

/** 编辑分类请求体 */
export interface CategoryUpdateRequest {
  categoryName?: string
  parentId?: number
  sortOrder?: number
  status?: number
}

/** 新增分类 */
export function createCategory(data: CategoryCreateRequest): Promise<Result<CategoryVO>> {
  return post<CategoryVO>('/api/v1/categories', data)
}

/** 编辑分类 */
export function updateCategory(id: number, data: CategoryUpdateRequest): Promise<Result<CategoryVO>> {
  return put<CategoryVO>(`/api/v1/categories/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id: number): Promise<Result<void>> {
  return del<void>(`/api/v1/categories/${id}`)
}

/** 切换分类状态 */
export function updateCategoryStatus(id: number, status: number): Promise<Result<void>> {
  return put<void>(`/api/v1/categories/${id}/status`, { status })
}
