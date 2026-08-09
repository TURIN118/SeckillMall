// api/banner.js — 轮播图接口封装
//
// 集中封装轮播图相关 HTTP 调用。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 对齐：
//   - design.md 2.3 节
//   - spec.md 6.3 节 BannerVO

const { get } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  ACTIVE: '/api/v1/banners/active' // GET 启用轮播图
}

/**
 * 获取启用状态的轮播图列表
 * @returns {Promise<Result<Array<BannerVO>>>}
 *
 * BannerVO: { id, title, imageUrl, linkUrl }
 */
function getActiveBanners() {
  return get(API.ACTIVE)
}

module.exports = {
  getActiveBanners
}