/**
 * 订单 API - 严格匹配 default.md
 */
import { get, post } from './request'
import type {
  Result,
  PageResult,
  SeckillOrder,
  OrderStatus,
  NormalOrder,
  NormalOrderDetailVO,
  OrderListItemVO,
  AdminOrderVO,
  AdminOrderQueryRequest
} from '@/types'

/** 我的订单列表 (分页+状态筛选) */
export function getOrderList(params: {
  status?: OrderStatus
  pageNum?: number
  pageSize?: number
}): Promise<Result<PageResult<SeckillOrder>>> {
  return get<PageResult<SeckillOrder>>('/api/v1/orders', params)
}

/**
 * 后台订单列表（高级筛选+分页+排序）
 * GET /api/v1/admin/orders
 * 支持订单号模糊查询、按天筛选日期、状态筛选
 */
export function getAdminOrderList(
  params: AdminOrderQueryRequest
): Promise<Result<PageResult<AdminOrderVO>>> {
  return get<PageResult<AdminOrderVO>>('/api/v1/admin/orders', params)
}

/** 订单详情 */
export function getOrderDetail(orderId: number | string): Promise<Result<SeckillOrder>> {
  return get<SeckillOrder>(`/api/v1/orders/${orderId}`)
}


/** 确认支付 (模拟支付) */
export function payOrder(orderId: number | string, payMethod?: string): Promise<Result<SeckillOrder>> {
  return post<SeckillOrder>(`/api/v1/orders/${orderId}/pay`, undefined, {
    params: { payMethod }
  })
}

/** 取消订单 (仅待支付) */
export function cancelOrder(orderId: number | string): Promise<Result<SeckillOrder>> {
  return post<SeckillOrder>(`/api/v1/orders/${orderId}/cancel`)
}

/* ==================== 普通订单 API (任务11后端接口) ==================== */

/**
 * 立即购买创建订单
 * POST /api/v1/orders  body: { productId, skuId?, quantity, addressId, remark? }
 */
export function createOrder(data: {
  productId: number | string
  /** SKU ID（可选，null/不传 表示无规格商品立即购买） */
  skuId?: number | string | null
  quantity: number
  /** 收货地址 ID */
  addressId?: number | string
  /** 备注（可选） */
  remark?: string
}): Promise<Result<NormalOrder>> {
  return post<NormalOrder>('/api/v1/orders', data)
}

/**
 * 从购物车创建订单
 * POST /api/v1/orders/from-cart  body: { addressId, cartIds, remark? }
 */
export function createOrderFromCart(data: {
  addressId: number | string
  cartIds: (number | string)[]
  remark?: string
}): Promise<Result<NormalOrder>> {
  return post<NormalOrder>('/api/v1/orders/from-cart', data)
}

/**
 * 普通订单详情 (含订单明细 items)
 * GET /api/v1/orders/{orderId}/normal-detail
 * 返回嵌套结构 { order, items }
 */
export function getNormalOrderDetail(
  orderId: number | string
): Promise<Result<NormalOrderDetailVO>> {
  return get<NormalOrderDetailVO>(`/api/v1/orders/${orderId}/normal-detail`)
}

/**
 * 普通订单支付
 * POST /api/v1/orders/{orderId}/pay-normal  body: { payMethod }
 * payMethod="WALLET" 时钱包扣款
 */
export function payNormalOrder(
  orderId: number | string,
  payMethod: string
): Promise<Result<NormalOrder>> {
  return post<NormalOrder>(`/api/v1/orders/${orderId}/pay-normal`, { payMethod })
}

/**
 * 取消普通订单（BUG-002 修复）
 * POST /api/v1/orders/{orderId}/cancel-normal
 * 仅允许 UNPAID 状态的普通订单取消，取消后回补商品库存
 */
export function cancelNormalOrder(orderId: number | string): Promise<Result<NormalOrder>> {
  return post<NormalOrder>(`/api/v1/orders/${orderId}/cancel-normal`)
}

/**
 * 确认收货（秒杀订单）
 * POST /api/v1/orders/{orderId}/confirm
 * 仅允许 SHIPPED 状态的秒杀订单确认收货
 */
export function confirmOrder(orderId: number | string): Promise<Result<SeckillOrder>> {
  return post<SeckillOrder>(`/api/v1/orders/${orderId}/confirm`)
}

/**
 * 确认收货（普通订单）
 * POST /api/v1/orders/{orderId}/confirm-normal
 * 仅允许 SHIPPED 状态的普通订单确认收货
 */
export function confirmNormalOrder(orderId: number | string): Promise<Result<NormalOrder>> {
  return post<NormalOrder>(`/api/v1/orders/${orderId}/confirm-normal`)
}

/* ==================== 统一订单列表 API (需求1 合并秒杀+普通) ==================== */

/**
 * 统一订单列表（秒杀+普通）
 * GET /api/v1/orders/unified?status=&pageNum=&pageSize=
 * 后端合并查询两套订单表，按 createTime 降序返回
 */
export function getUnifiedOrderList(params: {
  status?: string
  pageNum?: number
  pageSize?: number
}): Promise<Result<PageResult<OrderListItemVO>>> {
  return get<PageResult<OrderListItemVO>>('/api/v1/orders/unified', params)
}

/* ==================== 发货 API ==================== */

/** 管理员发货 - 秒杀订单 */
export function shipOrder(orderId: string | number, data: { shippingCompany: string; shippingNo: string }) {
  return post(`/api/v1/orders/${orderId}/ship`, data)
}

/** 管理员发货 - 普通订单 */
export function shipNormalOrder(orderId: string | number, data: { shippingCompany: string; shippingNo: string }) {
  return post(`/api/v1/orders/${orderId}/normal-ship`, data)
}
