// api/favorite.js — 收藏接口最小封装
//
// 仅封装 product 模块详情页收藏切换所需的最小接口；
// 完整收藏夹管理（列表/删除/批量操作）由 trade 模块实现。
//
// 对齐：
//   - design.md 2.5 节
//   - spec.md 1.4 节职责边界（product 模块仅发起操作意图与最小 API 调用）

const { post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  TOGGLE: '/api/v1/favorites' // POST 切换收藏状态
}

/**
 * 切换收藏状态（已收藏则取消，未收藏则收藏）
 * @param {object} data { productId } 或后端约定的字段
 * @returns {Promise<Result<void>>}
 */
function toggleFavorite(data) {
  return post(API.TOGGLE, data)
}

module.exports = {
  toggleFavorite
}