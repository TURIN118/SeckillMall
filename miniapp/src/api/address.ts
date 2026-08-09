/**
 * 收货地址 API（对齐 spec.md 2.6 地址端点）
 * /api/v1/users/addresses 或 /api/v1/addresses
 */

import { get, post, put, del } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import type { AddressVO, AddressRequest } from '@/types'

/** 地址列表 */
export function getAddressList(): Promise<AddressVO[]> {
  return get<AddressVO[]>('/users/addresses')
}

/** 地址详情 */
export function getAddressDetail(id: string): Promise<AddressVO> {
  return get<AddressVO>(`/users/addresses/${encodeId(id)}`)
}

/** 新增地址 */
export function addAddress(data: AddressRequest): Promise<AddressVO> {
  return post<AddressVO>('/users/addresses', data)
}

/** 修改地址 */
export function updateAddress(id: string, data: AddressRequest): Promise<AddressVO> {
  return put<AddressVO>(`/users/addresses/${encodeId(id)}`, data)
}

/** 删除地址 */
export function removeAddress(id: string): Promise<any> {
  return del(`/users/addresses/${encodeId(id)}`)
}

/** 设为默认地址 */
export function setDefaultAddress(id: string): Promise<any> {
  return put(`/users/addresses/${encodeId(id)}/default`)
}