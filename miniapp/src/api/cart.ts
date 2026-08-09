/**
 * 购物车 API（对齐 spec.md 2.6 购物车端点）
 * /api/v1/cart/*
 */

import { get, post, put, del } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import type {
  CartItemVO,
  AddCartRequest,
  UpdateCartQuantityRequest,
  BatchSelectedRequest,
  CartCountVO
} from '@/types'

/** 购物车列表 */
export function getCartList(): Promise<CartItemVO[]> {
  return get<CartItemVO[]>('/cart/list')
}

/** 加入购物车 */
export function addToCart(data: AddCartRequest): Promise<CartItemVO> {
  return post<CartItemVO>('/cart/add', data)
}

/** 修改数量 */
export function updateCartQuantity(id: string, data: UpdateCartQuantityRequest): Promise<any> {
  return put(`/cart/${encodeId(id)}/quantity`, data)
}

/** 删除单项 */
export function removeCartItem(id: string): Promise<any> {
  return del(`/cart/${encodeId(id)}`)
}

/** 清空购物车 */
export function clearCart(): Promise<any> {
  return del('/cart/clear')
}

/** 选中/取消选中 */
export function updateCartSelected(id: string, selected: boolean): Promise<any> {
  return put(`/cart/${encodeId(id)}/selected`, { selected })
}

/** 批量选中 */
export function batchUpdateSelected(data: BatchSelectedRequest): Promise<any> {
  return put('/cart/batch-selected', data)
}

/** 购物车数量 */
export function getCartCount(): Promise<CartCountVO> {
  return get<CartCountVO>('/cart/count')
}