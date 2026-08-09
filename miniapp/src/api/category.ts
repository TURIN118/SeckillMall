/**
 * 分类 API（对齐 spec.md 2.6 分类端点）
 * /api/v1/categories
 */

import { get } from '@/utils/request'
import type { CategoryVO } from '@/types'

/** 分类列表（树形） */
export function getCategoryList(): Promise<CategoryVO[]> {
  return get<CategoryVO[]>('/categories')
}

/** 分类详情 */
export function getCategoryDetail(id: string): Promise<CategoryVO> {
  return get<CategoryVO>(`/categories/${id}`)
}