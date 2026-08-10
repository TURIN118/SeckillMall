// api/product.js — 商品接口封装
//
// 集中封装商品相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - ProductQueryRequest  { pageNum, pageSize, categoryId, minPrice, maxPrice, keyword, sortBy, sortOrder, status }
//   - 商品 ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//
// 对齐：
//   - design.md 2.1 节
//   - spec.md 4.4 节规则 1（商品接口调用集中在 api/product.js）
//   - spec.md 4.3 节规则 2（商品 ID 全程字符串承载，禁止 Number 转换）

const { get } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
    LIST: '/api/v1/products',       // GET 商品分页
    DETAIL: '/api/v1/products/'    // GET /api/v1/products/{id} 商品详情
}

/**
 * 获取商品分页列表
 * @param {object} [params] 查询参数
 *   { pageNum, pageSize, categoryId, minPrice, maxPrice, keyword, sortBy, sortOrder, status }
 * @returns {Promise<Result<PageResult<ProductVO>>>}
 *
 * 说明：
 *   - 前台固定 status='ON_SALE'，调用方若未传则在此强制注入
 *   - 过滤掉值为空字符串/undefined/null 的字段，避免发送无效 query
 */
function getProductList(params) {
    const query = params || {}
    // 前台固定只查上架商品
    if (!query.status) query.status = 'ON_SALE'

    // 清理空值字段
    const cleaned = {}
    Object.keys(query).forEach((key) => {
        const v = query[key]
        if (v !== '' && v !== null && v !== undefined) {
            cleaned[key] = v
        }
    })

    return get(API.LIST, cleaned)
}

/**
 * 获取商品详情
 * @param {string} id 商品 ID（string，雪花 ID）
 * @returns {Promise<Result<ProductVO>>}
 *
 * 安全：id 强制 string + encodeURIComponent，避免 URL 注入
 */
function getProductDetail(id) {
    const safeId = encodeURIComponent(String(id))
    return get(API.DETAIL + safeId)
}

module.exports = {
    getProductList,
    getProductDetail
}