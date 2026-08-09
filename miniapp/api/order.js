// api/order.js — 订单接口封装
//
// 集中封装订单相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - 统一订单 OrderListItemVO { orderId, orderType, status, totalAmount, createTime, items }
//   - BuyNowRequest    { productId, skuId?, quantity, addressId, remark?, userCouponId? }
//   - CartCheckoutRequest { addressId, cartIds[], remark?, userCouponId? }
//   - 订单 ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//   - 秒杀订单(SECKILL)与普通订单(NORMAL)接口路径不同，需按 orderType 分发
//
// 对齐：
//   - design.md 2.2 节
//   - spec.md 5.3 节（订单管理全部业务规则）

const { get, post, del } = require('../utils/request')
const { buildUrl } = require('../utils/id')

// ========== 接口端点常量 ==========
const API = {
  UNIFIED: '/api/v1/orders/unified',   // GET 统一订单列表
  DETAIL: '/api/v1/orders/',           // GET /{id} 秒杀订单详情
  NORMAL_DETAIL: '/api/v1/orders/',    // GET /{id}/detail 普通订单详情
  CREATE_BUY_NOW: '/api/v1/orders',    // POST 立即购买
  CREATE_FROM_CART: '/api/v1/orders/from-cart', // POST 购物车结算
  PAY: '/api/v1/orders/',              // POST /{id}/pay 秒杀支付
  PAY_NORMAL: '/api/v1/orders/',       // POST /{id}/pay-normal 普通支付
  CANCEL: '/api/v1/orders/',           // POST /{id}/cancel 取消秒杀
  CANCEL_NORMAL: '/api/v1/orders/',    // POST /{id}/cancel-normal 取消普通
  CONFIRM: '/api/v1/orders/',          // POST /{id}/confirm 确认收货秒杀
  CONFIRM_NORMAL: '/api/v1/orders/',   // POST /{id}/normal-confirm 确认收货普通
  DELETE: '/api/v1/orders/'            // DELETE /{id} 逻辑删除
}

/**
 * 获取统一订单列表（秒杀+普通）
 * @param {object} [params] { status, orderType, pageNum, pageSize }
 *   - status:     string 订单状态 PENDING_PAY/PENDING_SHIP/PENDING_RECEIVE/COMPLETED/CANCELLED
 *   - orderType:  string 订单类型 SECKILL/NORMAL
 *   - pageNum:    number 页码
 *   - pageSize:   number 每页数量
 * @returns {Promise<Result<PageResult<OrderListItemVO>>>}
 */
function getUnifiedOrders(params) {
  const query = params || {}
  // 清理空值字段
  const cleaned = {}
  Object.keys(query).forEach((key) => {
    const v = query[key]
    if (v !== '' && v !== null && v !== undefined) {
      cleaned[key] = v
    }
  })
  return get(API.UNIFIED, cleaned)
}

/**
 * 获取秒杀订单详情
 * @param {string} id 订单 ID
 * @returns {Promise<Result<OrderDetailVO>>}
 */
function getOrderDetail(id) {
  const url = buildUrl(API.DETAIL, id)
  return get(url)
}

/**
 * 获取普通订单详情
 * @param {string} id 订单 ID
 * @returns {Promise<Result<OrderDetailVO>>}
 */
function getNormalOrderDetail(id) {
  const url = buildUrl(API.NORMAL_DETAIL, id) + '/detail'
  return get(url)
}

/**
 * 立即购买（创建订单）
 * @param {object} data BuyNowRequest { productId, skuId?, quantity, addressId, remark?, userCouponId? }
 * @returns {Promise<Result<{orderId:string, orderType:string}>>}
 */
function buyNow(data) {
  return post(API.CREATE_BUY_NOW, data)
}

/**
 * 购物车结算（创建订单）
 * @param {object} data CartCheckoutRequest { addressId, cartIds[], remark?, userCouponId? }
 * @returns {Promise<Result<{orderId:string, orderType:string}>>}
 */
function checkoutFromCart(data) {
  return post(API.CREATE_FROM_CART, data)
}

/**
 * 秒杀订单支付
 * @param {string} id 订单 ID
 * @param {string} [payMethod] 支付方式 ALIPAY/WALLET
 * @returns {Promise<Result<void>>}
 */
function payOrder(id, payMethod) {
  const url = buildUrl(API.PAY, id) + '/pay'
  const body = payMethod ? { payMethod: payMethod } : {}
  return post(url, body)
}

/**
 * 普通订单支付
 * @param {string} id 订单 ID
 * @param {string} [payMethod] 支付方式 ALIPAY/WALLET
 * @returns {Promise<Result<void>>}
 */
function payNormalOrder(id, payMethod) {
  const url = buildUrl(API.PAY_NORMAL, id) + '/pay-normal'
  const body = payMethod ? { payMethod: payMethod } : {}
  return post(url, body)
}

/**
 * 取消秒杀订单
 * @param {string} id 订单 ID
 * @returns {Promise<Result<void>>}
 */
function cancelOrder(id) {
  const url = buildUrl(API.CANCEL, id) + '/cancel'
  return post(url)
}

/**
 * 取消普通订单
 * @param {string} id 订单 ID
 * @returns {Promise<Result<void>>}
 */
function cancelNormalOrder(id) {
  const url = buildUrl(API.CANCEL_NORMAL, id) + '/cancel-normal'
  return post(url)
}

/**
 * 确认收货（秒杀订单）
 * @param {string} id 订单 ID
 * @returns {Promise<Result<void>>}
 */
function confirmOrder(id) {
  const url = buildUrl(API.CONFIRM, id) + '/confirm'
  return post(url)
}

/**
 * 确认收货（普通订单）
 * @param {string} id 订单 ID
 * @returns {Promise<Result<void>>}
 */
function confirmNormalOrder(id) {
  const url = buildUrl(API.CONFIRM_NORMAL, id) + '/normal-confirm'
  return post(url)
}

/**
 * 删除订单（逻辑删除）
 * @param {string} id 订单 ID
 * @returns {Promise<Result<void>>}
 */
function deleteOrder(id) {
  const url = buildUrl(API.DELETE, id)
  return del(url)
}

module.exports = {
  getUnifiedOrders,
  getOrderDetail,
  getNormalOrderDetail,
  buyNow,
  checkoutFromCart,
  payOrder,
  payNormalOrder,
  cancelOrder,
  cancelNormalOrder,
  confirmOrder,
  confirmNormalOrder,
  deleteOrder
}