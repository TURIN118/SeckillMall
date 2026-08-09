// api/cart.js — 购物车接口最小封装
//
// 仅封装 product 模块详情页加购所需的最小接口；
// 完整购物车管理（列表/删除/数量更新/选中）由 trade 模块实现。
//
// 对齐：
//   - design.md 2.4 节
//   - spec.md 1.4 节职责边界（product 模块仅发起操作意图与最小 API 调用）

const { post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  ADD: '/api/v1/cart/add' // POST 加入购物车
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

module.exports = {
  addCart
}