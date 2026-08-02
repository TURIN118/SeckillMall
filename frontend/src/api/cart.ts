/**
 * 购物车 API - 对接后端 /api/v1/cart
 */
import { get, post, put, del } from './request'
import type { Result, CartItemVO, CartAddRequest } from '@/types'

/** 获取购物车列表 */
export function getCartList(): Promise<Result<CartItemVO[]>> {
    return get<CartItemVO[]>('/api/v1/cart/list')
}

/** 添加商品到购物车 */
export function addCart(data: CartAddRequest): Promise<Result<void>> {
    return post<void>('/api/v1/cart/add', data)
}

/** 修改购物车项数量 */
export function updateCartQuantity(cartId: number, quantity: number): Promise<Result<void>> {
    return put<void>(`/api/v1/cart/${cartId}/quantity`, { quantity })
}

/** 删除购物车项 */
export function deleteCartItem(cartId: number): Promise<Result<void>> {
    return del<void>(`/api/v1/cart/${cartId}`)
}

/** 清空购物车 */
export function clearCart(): Promise<Result<void>> {
    return del<void>('/api/v1/cart/clear')
}

/** 更新购物车项选中状态 */
export function updateCartSelected(cartId: number, selected: boolean): Promise<Result<void>> {
    return put<void>(`/api/v1/cart/${cartId}/selected`, { selected })
}

/** 批量更新购物车项选中状态 */
export function batchUpdateCartSelected(cartIds: number[], selected: boolean): Promise<Result<void>> {
    return put<void>('/api/v1/cart/batch-selected', { cartIds, selected })
}

/** 获取购物车商品数量 */
export function getCartCount(): Promise<Result<number>> {
    return get<number>('/api/v1/cart/count')
}