// api/coupon.js — 优惠券接口封装（usercenter 模块）
//
// 集中封装优惠券相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - GET  /api/v1/coupons/available?productId=  可领取列表
//   - POST /api/v1/coupons/{id}/receive          领取
//   - GET  /api/v1/coupons/mine?status=          我的优惠券
//
// 注意：
//   - 优惠券 ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//   - 均需登录态（请求拦截器自动注入 Authorization）
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 4 节
//   - .codeartsdoer/specs/usercenter/design.md 4 节
//   - 后端 CouponController.java 端点

const { get, post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  AVAILABLE: '/api/v1/coupons/available', // GET 可领取列表
  RECEIVE: '/api/v1/coupons/',            // POST /{id}/receive 领取
  MINE: '/api/v1/coupons/mine'            // GET 我的优惠券
}

/**
 * 获取可领取优惠券列表
 * @param {string} [productId] 商品 ID（可选，按商品筛选可领券）
 * @returns {Promise<Result<Array<CouponVO>>>}
 */
function listAvailable(productId) {
  const query = productId ? { productId: productId } : {}
  return get(API.AVAILABLE, query)
}

/**
 * 领取优惠券
 * @param {string|number} id 优惠券 ID（强制 string 拼接）
 * @returns {Promise<Result<void>>}
 */
function receive(id) {
  const url = API.RECEIVE + encodeURIComponent(String(id)) + '/receive'
  return post(url)
}

/**
 * 获取我的优惠券列表
 * @param {string} [status] 状态筛选（UNUSED/USED/EXPIRED），空=全部
 * @returns {Promise<Result<Array<UserCouponVO>>>}
 */
function listMine(status) {
  const query = status ? { status: status } : {}
  return get(API.MINE, query)
}

module.exports = {
  listAvailable,
  receive,
  listMine
}