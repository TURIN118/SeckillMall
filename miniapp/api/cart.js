// api/cart.js — 购物车接口封装
//
// 集中封装购物车相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - CartItemVO { id, productId, skuId, quantity, selected, productName, productImage, price, skuName }
//   - 购物车 ID / 商品 ID / SKU ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//
// 对齐：
//   - design.md 2.1 节
//   - spec.md 4.4 节规则 1（购物车接口调用集中在 api/cart.js）
//   - spec.md 4.3 节规则 2（ID 全程字符串承载，禁止 Number 转换）

const { get, post, put, del } = require('../utils/request')
const { buildUrl } = require('../utils/id')

// ========== 接口端点常量 ==========
const API = {
  LIST: '/api/v1/cart/list',                 // GET 购物车列表
  ADD: '/api/v1/cart/add',                   // POST 加入购物车
  QUANTITY: '/api/v1/cart/',                 // PUT /{cartId}/quantity
  REMOVE: '/api/v1/cart/',                   // DELETE /{cartId}
  CLEAR: '/api/v1/cart/clear',               // DELETE 清空
  SELECTED: '/api/v1/cart/',                 // PUT /{cartId}/selected
  BATCH_SELECTED: '/api/v1/cart/batch-selected', // PUT 批量选中
  COUNT: '/api/v1/cart/count'                // GET 数量
}

/**
 * 获取购物车列表
 * @returns {Promise<Result<Array<CartItemVO>>>}
 */
function getCartList() {
  return get(API.LIST)
}

/**
 * 加入购物车
 * @param {object} data { productId, skuId?, quantity }
 *   - productId: string 商品 ID
 *   - skuId:     string 可选，多规格商品的 SKU ID
 *   - quantity:  number 数量
 * @returns {Promise<Result<void>>}
 */
function addCart(data) {
  return post(API.ADD, data)
}

/**
 * 修改购物车项数量
 * @param {string} cartId 购物车项 ID
 * @param {number} quantity 数量（≥1）
 * @returns {Promise<Result<void>>}
 */
function updateQuantity(cartId, quantity) {
  const url = buildUrl(API.QUANTITY, cartId) + '/quantity'
  return put(url, { quantity: quantity })
}

/**
 * 删除购物车单项
 * @param {string} cartId 购物车项 ID
 * @returns {Promise<Result<void>>}
 */
function removeCart(cartId) {
  const url = buildUrl(API.REMOVE, cartId)
  return del(url)
}

/**
 * 清空购物车
 * @returns {Promise<Result<void>>}
 */
function clearCart() {
  return del(API.CLEAR)
}

/**
 * 修改购物车项选中状态
 * @param {string} cartId 购物车项 ID
 * @param {boolean} selected 选中状态
 * @returns {Promise<Result<void>>}
 */
function updateSelected(cartId, selected) {
  const url = buildUrl(API.SELECTED, cartId) + '/selected'
  return put(url, { selected: !!selected })
}

/**
 * 批量修改选中状态
 * @param {Array<string>} cartIds 购物车项 ID 列表
 * @param {boolean} selected 选中状态
 * @returns {Promise<Result<void>>}
 */
function batchUpdateSelected(cartIds, selected) {
  return put(API.BATCH_SELECTED, {
    cartIds: Array.isArray(cartIds) ? cartIds : [],
    selected: !!selected
  })
}

/**
 * 获取购物车数量（角标）
 * @returns {Promise<Result<number>>}
 */
function getCartCount() {
  return get(API.COUNT)
}

module.exports = {
  getCartList,
  addCart,
  updateQuantity,
  removeCart,
  clearCart,
  updateSelected,
  batchUpdateSelected,
  getCartCount
}
