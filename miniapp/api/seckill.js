// api/seckill.js — 秒杀接口封装
//
// 集中封装秒杀相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端 /api/v1/seckill 前缀：
//   - GET  /list                    秒杀活动列表（分页 PageResult）
//   - GET  /{seckillId}             秒杀活动详情
//   - GET  /{seckillId}/token       一次性 token（返回 string）
//   - GET  /{seckillId}/stock       实时库存（返回 number）
//   - POST /{seckillId}             执行秒杀（X-Seckill-Token 头；@RateLimit 1/s）
//   - POST /{seckillId}/execute     一键执行秒杀（备用，无需 token）
//   - GET  /{seckillId}/result      查询秒杀结果
//   - GET  /activities              场次列表（List<SeckillActivityVO>）
//   - GET  /activities/{activityId} 场次详情
//
// 对齐：
//   - .codeartsdoer/specs/seckill/spec.md 3.4 节、4 节
//   - .codeartsdoer/specs/seckill/design.md 3 节
//   - spec 4 节数据约束：雪花 ID 全程 string，禁止 Number 转换
//
// 注意：
//   - utils/request.js 的 post 签名为 post(url, data, header)，第三参为 header 对象
//   - 所有 id 参数强制 String() 拼接，避免雪花 ID 精度丢失
//   - URL 路径用 encodeURIComponent 防特殊字符

const { get, post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
    BASE: '/api/v1/seckill',           // 秒杀根路径
    LIST: '/api/v1/seckill/list',      // GET 秒杀活动列表（分页）
    ACTIVITIES: '/api/v1/seckill/activities' // GET 场次列表
}

// ========== 工具函数 ==========

/**
 * 安全拼接秒杀详情子路径：/api/v1/seckill/{id}
 * @param {string|number} id 秒杀 ID（雪花 ID，强制 string）
 * @returns {string} 已 encodeURIComponent 的路径
 */
function buildSeckillPath(id) {
    const safeId = encodeURIComponent(String(id))
    return API.BASE + '/' + safeId
}

/**
 * 安全拼接场次子路径：/api/v1/seckill/activities/{id}
 * @param {string|number} id 场次 ID
 * @returns {string}
 */
function buildActivityPath(id) {
    const safeId = encodeURIComponent(String(id))
    return API.ACTIVITIES + '/' + safeId
}

// ========== 9 个接口方法 ==========

/**
 * 1. 获取秒杀活动列表（分页）
 * @param {object} [params] 查询参数
 *   { status, categoryId, pageNum, pageSize }
 * @returns {Promise<Result<PageResult<SeckillActivityVO>>>}
 *
 * 说明：过滤空值字段，避免发送无效 query
 */
function getSeckillList(params) {
    const query = params || {}
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
 * 2. 获取秒杀活动详情
 * @param {string|number} id 秒杀 ID
 * @returns {Promise<Result<SeckillActivityVO>>}
 */
function getSeckillDetail(id) {
    return get(buildSeckillPath(id))
}

/**
 * 3. 获取一次性秒杀 token（防重放）
 * @param {string|number} id 秒杀 ID
 * @returns {Promise<Result<string>>} data 为 token 字符串
 */
function getSeckillToken(id) {
    return get(buildSeckillPath(id) + '/token')
}

/**
 * 4. 获取实时库存
 * @param {string|number} id 秒杀 ID
 * @returns {Promise<Result<number>>} data 为库存数量
 */
function getSeckillStock(id) {
    return get(buildSeckillPath(id) + '/stock')
}

/**
 * 5. 执行秒杀（携带 X-Seckill-Token 头）
 * @param {string|number} id 秒杀 ID
 * @param {string} token 一次性 token（由 getSeckillToken 获取）
 * @returns {Promise<Result<SeckillResultVO>>}
 *   SeckillResultVO { requestId, status: 'PENDING'|'SUCCESS'|'FAILED', ... }
 *
 * 安全：token 通过自定义头传递，后端 @RateLimit 1/s 限流
 */
function doSeckill(id, token) {
    const header = {}
    if (token != null && token !== '') {
        header['X-Seckill-Token'] = token
    }
    // post(url, data, header) — request.js 第三参为 header 对象
    return post(buildSeckillPath(id), {}, header)
}

/**
 * 6. 一键执行秒杀（备用，无需 token）
 * @param {string|number} id 秒杀 ID
 * @returns {Promise<Result<SeckillResultVO>>}
 */
function executeSeckill(id) {
    return post(buildSeckillPath(id) + '/execute', {})
}

/**
 * 7. 查询秒杀结果
 * @param {string|number} id 秒杀 ID
 * @param {string} requestId 抢购请求 ID（doSeckill 返回的 requestId）
 * @returns {Promise<Result<SeckillResultVO>>}
 */
function getSeckillResult(id, requestId) {
    const query = {}
    if (requestId != null && requestId !== '') {
        query.requestId = String(requestId)
    }
    return get(buildSeckillPath(id) + '/result', query)
}

/**
 * 8. 获取场次列表
 * @returns {Promise<Result<List<SeckillActivityVO>>>}
 */
function listActivities() {
    return get(API.ACTIVITIES)
}

/**
 * 9. 获取场次详情
 * @param {string|number} activityId 场次 ID
 * @returns {Promise<Result<SeckillActivityVO>>}
 */
function getActivityDetail(activityId) {
    return get(buildActivityPath(activityId))
}

module.exports = {
    getSeckillList,
    getSeckillDetail,
    getSeckillToken,
    getSeckillStock,
    doSeckill,
    executeSeckill,
    getSeckillResult,
    listActivities,
    getActivityDetail
}