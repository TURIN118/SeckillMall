// api/favorite.js — 收藏接口封装
//
// 集中封装收藏相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - FavoriteItemVO { productId, productName, productImage, price, favoriteTime }
//   - 商品 ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//
// 对齐：
//   - design.md 2.4 节
//   - spec.md 4.4 节规则 1（收藏接口调用集中在 api/favorite.js）
//
// 兼容说明：
//   - 保留 toggleFavorite（product 模块详情页已使用），等价于 addFavorite
//   - trade 模块收藏夹页使用 add/remove 显式语义

const { get, post, del } = require('../utils/request')
const { buildUrl } = require('../utils/id')

// ========== 接口端点常量 ==========
const API = {
  LIST: '/api/v1/favorites/list',    // GET 收藏列表
  ADD: '/api/v1/favorites/add',      // POST 新增收藏
  REMOVE: '/api/v1/favorites/',      // DELETE /{productId} 取消收藏
  CHECK: '/api/v1/favorites/check/', // GET /check/{productId} 检查
  COUNT: '/api/v1/favorites/count',  // GET 数量
  TOGGLE: '/api/v1/favorites'        // POST 切换收藏状态（兼容 product 模块）
}

/**
 * 获取收藏列表
 * @returns {Promise<Result<Array<FavoriteItemVO>>>}
 */
function getFavoriteList() {
  return get(API.LIST)
}

/**
 * 新增收藏
 * @param {string} productId 商品 ID
 * @returns {Promise<Result<void>>}
 */
function addFavorite(productId) {
  return post(API.ADD, { productId: String(productId) })
}

/**
 * 取消收藏
 * @param {string} productId 商品 ID
 * @returns {Promise<Result<void>>}
 */
function removeFavorite(productId) {
  const url = buildUrl(API.REMOVE, productId)
  return del(url)
}

/**
 * 检查商品是否已收藏
 * @param {string} productId 商品 ID
 * @returns {Promise<Result<boolean>>}
 */
function checkFavorite(productId) {
  const url = buildUrl(API.CHECK, productId)
  return get(url)
}

/**
 * 获取收藏数量
 * @returns {Promise<Result<number>>}
 */
function getFavoriteCount() {
  return get(API.COUNT)
}

/**
 * 切换收藏状态（兼容 product 模块详情页调用）
 * @param {object} data { productId }
 * @returns {Promise<Result<void>>}
 */
function toggleFavorite(data) {
  return post(API.TOGGLE, data)
}

module.exports = {
  getFavoriteList,
  addFavorite,
  removeFavorite,
  checkFavorite,
  getFavoriteCount,
  // 兼容保留
  toggleFavorite
}
