/**
 * 订单 API - 严格匹配 default.md
 */
import { get, post } from './request'
import type { Result, PageResult, SeckillOrder, OrderStatus } from '@/types'

/** 我的订单列表 (分页+状态筛选) */
export function getOrderList(params: {
  status?: OrderStatus
  pageNum?: number
  pageSize?: number
}): Promise<Result<PageResult<SeckillOrder>>> {
  return get<PageResult<SeckillOrder>>('/api/v1/orders', params)
}

/** 订单详情 */
export function getOrderDetail(orderId: number): Promise<Result<SeckillOrder>> {
  return get<SeckillOrder>(`/api/v1/orders/${orderId}`)
}

/** 查询订单状态 */
export function getOrderStatus(orderId: number): Promise<Result<string>> {
  return get<string>(`/api/v1/orders/${orderId}/status`)
}

/** 确认支付 (模拟支付) */
export function payOrder(orderId: number, payMethod?: string): Promise<Result<SeckillOrder>> {
  return post<SeckillOrder>(`/api/v1/orders/${orderId}/pay`, undefined, {
    params: { payMethod }
  })
}

/** 取消订单 (仅待支付) */
export function cancelOrder(orderId: number): Promise<Result<SeckillOrder>> {
  return post<SeckillOrder>(`/api/v1/orders/${orderId}/cancel`)
}