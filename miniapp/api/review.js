// api/review.js — 评论接口封装（usercenter 模块）
//
// 集中封装商品评论相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - GET  /api/v1/reviews/product/{productId}?pageNum=&pageSize=  商品评论分页（公开）
//   - POST /api/v1/reviews/create                                  发表评论
//
// 注意：
//   - 商品 ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//   - create 需登录态；listByProduct 公开
//   - 评论 images 字段为 JSON 字符串（URL 数组序列化），由调用方负责序列化
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 4 节
//   - .codeartsdoer/specs/usercenter/design.md 4 节
//   - 后端 ReviewController.java 端点

const { get, post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
    BY_PRODUCT: '/api/v1/reviews/product/', // GET /{productId} 商品评论分页
    CREATE: '/api/v1/reviews/create'        // POST 发表评论
}

/**
 * 获取商品评论分页
 * @param {string|number} productId 商品 ID（强制 string 拼接）
 * @param {object} [params] { pageNum, pageSize }
 * @returns {Promise<Result<PageResult<ReviewVO>>>}
 */
function listByProduct(productId, params) {
    const url = API.BY_PRODUCT + encodeURIComponent(String(productId))
    const query = params || {}
    // 清理空值字段
    const cleaned = {}
    Object.keys(query).forEach((key) => {
        const v = query[key]
        if (v !== '' && v !== null && v !== undefined) {
            cleaned[key] = v
        }
    })
    return get(url, cleaned)
}

/**
 * 发表评论
 * @param {object} data { productId, skuId?, content, rating, images? }
 *   - images 为 JSON 字符串（URL 数组序列化），由调用方负责 JSON.stringify
 * @returns {Promise<Result<ReviewVO>>}
 */
function create(data) {
    return post(API.CREATE, data)
}

module.exports = {
    listByProduct,
    create
}