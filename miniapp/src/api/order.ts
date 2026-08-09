/**
 * 订单 API（对齐 spec.md 2.6 订单端点）
 * /api/v1/orders/*
 */

import { get, post } from '@/utils/request'
import { encodeId } from '@/utils/snowflake'
import type {
  OrderVO,
  OrderQuery,
  PageResult,
  CreateOrderRequest,
  CreateOrderFromCartRequest
} from '@/types'

/** 订单列表 */
export function getOrderList(params?: OrderQuery): Promise<PageResult<OrderVO>> {
  return get<PageResult<OrderVO>>('/orders', params)
}

/** 统一订单列表 */
export function getUnifiedOrderList(params?: OrderQuery): Promise<PageResult<OrderVO>> {
  return get<PageResult<OrderVO>>('/orders/unified', params)
}

/** 订单详情 */
export function getOrderDetail(id: string): Promise<OrderVO> {
  return get<OrderVO>(`/orders/${encodeId(id)}`)
}

/** 普通订单详情 */
export function getNormalOrderDetail(id: string): Promise<OrderVO> {
  return get<OrderVO>(`/orders/${encodeId(id)}/normal-detail`)
}

/** 创建订单 */
export function createOrder(data: CreateOrderRequest): Promise<OrderVO> {
  return post<OrderVO>('/orders', data)
}

/** 从购物车创建订单 */
export function createOrderFromCart(data: CreateOrderFromCartRequest): Promise<OrderVO> {
  return post<OrderVO>('/orders/from-cart', data)
}

/** 支付（秒杀订单） */
export function payOrder(id: string): Promise<any> {
  return post(`/orders/${encodeId(id)}/pay`)
}

/** 普通订单支付 */
export function payNormalOrder(id: string): Promise<any> {
  return post(`/orders/${encodeId(id)}/pay-normal`)
}

/** 取消订单（秒杀订单） */
export function cancelOrder(id: string): Promise<any> {
  return post(`/orders/${encodeId(id)}/cancel`)
}

/** 取消普通订单 */
export function cancelNormalOrder(id: string): Promise<any> {
  return post(`/orders/${encodeId(id)}/cancel-normal`)
}

/** 确认收货（秒杀订单） */
export function confirmOrder(id: string): Promise<any> {
  return post(`/orders/${encodeId(id)}/confirm`)
}

/** 确认收货（普通订单） */
export function confirmNormalOrder(id: string): Promise<any> {
  return post(`/orders/${encodeId(id)}/confirm-normal`)
}