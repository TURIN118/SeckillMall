/**
 * 收货地址 API - 对接后端 /api/v1/addresses
 */
import { get, post, put, del } from './request'
import type { Result, UserAddressVO, UserAddressRequest } from '@/types'

/** 查询当前用户地址列表 */
export function getAddressList(): Promise<Result<UserAddressVO[]>> {
    return get<UserAddressVO[]>('/api/v1/addresses/list')
}

/** 新增收货地址 */
export function createAddress(data: UserAddressRequest): Promise<Result<UserAddressVO>> {
    return post<UserAddressVO>('/api/v1/addresses/create', data)
}

/** 编辑收货地址 */
export function updateAddress(id: number, data: UserAddressRequest): Promise<Result<UserAddressVO>> {
    return put<UserAddressVO>(`/api/v1/addresses/${id}`, data)
}

/** 删除收货地址 (逻辑删除) */
export function deleteAddress(id: number): Promise<Result<void>> {
    return del<void>(`/api/v1/addresses/${id}`)
}

/** 设置默认收货地址 */
export function setDefaultAddress(id: number): Promise<Result<void>> {
    return put<void>(`/api/v1/addresses/${id}/default`)
}