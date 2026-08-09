// api/category.js — 分类接口封装
//
// 集中封装分类相关 HTTP 调用。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 对齐：
//   - design.md 2.2 节
//   - spec.md 4.4 节规则 1（分类接口调用集中在 api/category.js）
//   - spec.md 6.2 节 CategoryVO

const { get } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  TREE: '/api/v1/categories' // GET 分类树
}

/**
 * 获取分类树
 * @returns {Promise<Result<Array<CategoryVO>>>}
 *
 * 返回结构：一级分类数组，每个分类含 children（子分类数组），递归
 */
function getCategoryTree() {
  return get(API.TREE)
}

module.exports = {
  getCategoryTree
}