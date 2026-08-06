/**
 * 分类规格模板 API - 对接后端 /api/v1/admin/category
 */
import { get, post, put, del } from './request'
import type { Result, CategoryAttribute, CategoryAttributeValue } from '@/types'

/** 获取分类的规格模板（含预设值） */
export function getCategoryAttributes(
  categoryId: number | string
): Promise<Result<CategoryAttribute[]>> {
  return get<CategoryAttribute[]>(`/api/v1/admin/category/${categoryId}/attributes`)
}

/** 创建分类规格属性 */
export function createCategoryAttribute(
  data: CategoryAttribute
): Promise<Result<CategoryAttribute>> {
  return post<CategoryAttribute>('/api/v1/admin/category/attributes', data)
}

/** 更新分类规格属性 */
export function updateCategoryAttribute(
  id: number | string,
  data: CategoryAttribute
): Promise<Result<CategoryAttribute>> {
  return put<CategoryAttribute>(`/api/v1/admin/category/attributes/${id}`, data)
}

/** 删除分类规格属性 */
export function deleteCategoryAttribute(
  id: number | string
): Promise<Result<void>> {
  return del<void>(`/api/v1/admin/category/attributes/${id}`)
}

/** 为分类规格属性添加预设值 */
export function addCategoryAttributeValue(
  attributeId: number | string,
  data: CategoryAttributeValue
): Promise<Result<CategoryAttribute>> {
  return post<CategoryAttribute>(
    `/api/v1/admin/category/attributes/${attributeId}/values`,
    data
  )
}