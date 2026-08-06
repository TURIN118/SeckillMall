/**
 * 商品 SKU API - 对接后端 /api/v1/products/{productId}/skus
 */
import { get, post } from './request'
import type { Result, ProductSkuVO, ProductSkuDTO } from '@/types'

/** 查询商品所有启用 SKU（前台商品详情页用） */
export function getProductSkus(
  productId: number | string
): Promise<Result<ProductSkuVO[]>> {
  return get<ProductSkuVO[]>(`/api/v1/products/${productId}/skus`)
}

/* === 建议10 已落实：后端生成 SKU 笛卡尔积 === */
/**
 * 生成 SKU 笛卡尔积（后台商品编辑用，传入属性定义，后端返回笛卡尔积 SKU 列表）
 * 后端接口：POST /api/v1/admin/product/skus/generate
 */
export function generateSkuCombinations(data: {
  productId?: number | string | null
  attributes: Array<{
    name: string
    type: string
    values: Array<{ value: string; imageUrl?: string }>
  }>
  defaultPrice: number
}): Promise<Result<ProductSkuDTO[]>> {
  return post<ProductSkuDTO[]>('/api/v1/admin/product/skus/generate', data)
}