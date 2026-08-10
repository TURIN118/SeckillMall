// utils/request.js — HTTP 请求封装（核心）
//
// Promise 化 wx.request，内置请求/响应拦截器，对齐 Web 端 Axios 拦截器逻辑
// （frontend/src/api/request.ts）。
//
// 拦截器管线：
//   请求拦截：注入 Authorization Bearer
//   wx.request
//   响应拦截：
//     ├─ 同步 timeOffset（timestamp）
//     ├─ HTTP 401 + code 1002 → 刷新令牌 → 重试（刷新锁 + 等待队列）
//     ├─ HTTP 401 + code 1011（或其他非 1002）→ 提示，不刷新
//     ├─ HTTP 403 → 提示无权限
//     ├─ HTTP 429 → 提示频繁
//     ├─ HTTP 5xx → 提示服务器异常
//     ├─ 业务码非 200 → 提示 message，拒绝
//     └─ 业务码 200 → 返回完整 Result<T>（调用方取 .data）
//
// 对齐：
//   - spec.md 5.1 节（统一请求处理）、5.2 节（鉴权状态管理）
//   - design.md 1.2 节、2.2 节
//   - Web 端 frontend/src/api/request.ts（H-F2 修复：刷新失败显式 reject 队列）
//   - 主计划 6.1 节代码骨架

const { BASE_URL, TIMEOUT, LOG_ENABLED } = require('../config/env')
const {
    CODE_SUCCESS,
    CODE_UNAUTHORIZED,
    REFRESH_TOKEN_URL
} = require('../config/constants')
const {
    getAccessToken,
    getRefreshToken,
    setToken,
    clearToken,
    isRefreshing,
    setRefreshing,
    pushPending,
    getPending,
    clearPending
} = require('./auth')
const { syncServerTime } = require('./time-sync')

// ========== 工具函数 ==========

/**
 * 显示 toast 提示（统一入口）
 * @param {string} msg 提示文案
 */
function showToast(msg) {
    if (!msg) return
    wx.showToast({
        title: String(msg),
        icon: 'none',
        duration: 2000
    })
}

/**
 * 跳转登录页（避免重复跳转）
 * 对齐 spec 5.2.3 异常场景 3：重复跳转登录
 */
function redirectToLogin() {
    try {
        const pages = getCurrentPages()
        const current = pages[pages.length - 1]
        const route = current ? current.route : ''
        if (route && route.indexOf('pages/login/login') !== -1) return
        // 使用 reLaunch 清空导航栈，避免刷新失败后残留失效页面
        wx.reLaunch({ url: '/pages/login/login' })
    } catch (e) {
        // getCurrentPages 在某些场景（如 App 未初始化完成）可能异常，静默处理
    }
}

/**
 * 简易日志（受 LOG_ENABLED 控制，禁止输出 token 明文）
 */
function log(...args) {
    if (LOG_ENABLED) console.log('[request]', ...args)
}

// ========== 核心：Promise 化 wx.request ==========

/**
 * Promise 化 wx.request（裸请求，不含拦截器）
 * @param {object} options { url, method, data, header, timeout }
 * @returns {Promise<{statusCode: number, data: any, header: object}>}
 *
 * 说明：success 回调只要收到 HTTP 响应就触发（含 401/403/429/5xx），
 *       fail 仅在网络层失败（无网络/超时/连接失败）时触发。
 */
function rawRequest(options) {
    return new Promise((resolve, reject) => {
        wx.request({
            url: BASE_URL + options.url,
            method: options.method || 'GET',
            data: options.data,
            header: Object.assign({ 'Content-Type': 'application/json' }, options.header || {}),
            timeout: options.timeout || TIMEOUT,
            success: (res) => resolve(res),
            fail: (err) => reject(err)
        })
    })
}

// ========== 请求拦截：注入 Authorization ==========

/**
 * 请求拦截：注入 Authorization Bearer
 * 对齐 spec 5.1.1 规则 3：本地有 token 且请求未显式设置 Authorization 时注入
 * @param {object} options
 * @returns {object} 处理后的 options
 */
function withAuth(options) {
    options.header = options.header || {}
    const token = getAccessToken()
    if (token && !options.header.Authorization) {
        options.header.Authorization = 'Bearer ' + token
    }
    return options
}

// ========== 主请求函数（含完整响应拦截） ==========

/**
 * 核心请求函数，含完整拦截器管线
 * @param {object} options { url, method, data, header, timeout }
 * @returns {Promise<Result<T>>} 成功 resolve 完整 Result（调用方取 .data），失败 reject Error
 */
async function request(options) {
    options = withAuth(options)
    try {
        const res = await rawRequest(options)
        const statusCode = res.statusCode
        const body = res.data

        // 1. 同步服务器时间（任何携带合法 timestamp 的响应都同步）
        if (body && body.timestamp) {
            syncServerTime(body.timestamp)
        }

        // 2. HTTP 401 处理：区分 Token 过期(1002) 与防重放(1011) 等
        if (statusCode === 401) {
            const code = body && typeof body.code === 'number' ? body.code : null
            if (code !== CODE_UNAUTHORIZED) {
                // 非 Token 过期（如 1011 防重放、签名校验失败等），直接提示，不触发刷新
                // 对齐 spec 5.2.1 规则 3、Web 端 request.ts 第 104-115 行
                const msg = (body && body.message) || '请求失败，请重新操作'
                showToast(msg)
                return Promise.reject(new Error(msg))
            }
            // Token 过期（1002），走刷新流程
            return refreshTokenAndRetry(options)
        }

        // 3. HTTP 403
        if (statusCode === 403) {
            showToast('您没有权限访问该页面')
            return Promise.reject(new Error('您没有权限访问该页面'))
        }

        // 4. HTTP 429
        if (statusCode === 429) {
            showToast('请求太频繁，请稍后再试')
            return Promise.reject(new Error('请求太频繁，请稍后再试'))
        }

        // 5. HTTP 5xx
        if (statusCode >= 500) {
            showToast('服务器异常，请稍后重试')
            return Promise.reject(new Error('服务器异常，请稍后重试'))
        }

        // 6. 业务码非 200（HTTP 2xx 但业务失败）
        if (body && typeof body.code === 'number' && body.code !== CODE_SUCCESS) {
            const msg = (body && body.message) || '请求失败'
            showToast(msg)
            return Promise.reject(new Error(msg))
        }

        // 7. 成功：返回完整 Result<T>（调用方取 .data）
        return body
    } catch (err) {
        // 网络层失败（无网络/超时/连接失败）
        // 对齐 spec 5.1.3 异常场景 1、2
        const errMsg = (err && err.errMsg) || ''
        if (errMsg.indexOf('timeout') !== -1) {
            showToast('请求超时，请稍后重试')
        } else {
            showToast('网络异常，请检查连接')
        }
        log('network error:', errMsg)
        return Promise.reject(err)
    }
}

// ========== Token 刷新 + 重试（并发锁 + 等待队列） ==========

/**
 * Token 刷新 + 重试原请求
 *
 * 流程（对齐 spec 5.2.2、Web 端 request.ts 第 116-174 行）：
 *   1. 无 refresh_token → 清空 token + 跳登录
 *   2. 刷新中 → 入等待队列
 *   3. 获取刷新锁 → 发刷新请求
 *      - 成功：存新 token → 批量重试队列 + 重试原请求
 *      - 失败：显式 reject 队列（H-F2 修复）→ 清空 token → 跳登录
 *
 * @param {object} options 原请求配置
 * @returns {Promise<Result<T>>}
 */
async function refreshTokenAndRetry(options) {
    const refreshToken = getRefreshToken()

    // 规则 6：无 refresh_token → 清空 token + 跳登录
    if (!refreshToken) {
        clearToken()
        redirectToLogin()
        return Promise.reject(new Error('无 refresh_token，跳转登录'))
    }

    // 规则 4：刷新进行中 → 入等待队列
    if (isRefreshing()) {
        return new Promise((resolve, reject) => {
            pushPending({ options, resolve, reject })
        })
    }

    // 获取刷新锁
    setRefreshing(true)
    try {
        // 直接用 rawRequest 发刷新请求，避免走 request 拦截器循环
        const refreshRes = await rawRequest({
            url: REFRESH_TOKEN_URL,
            method: 'POST',
            data: { refreshToken },
            header: { 'Content-Type': 'application/json' }
        })
        const refreshBody = refreshRes.data

        if (
            refreshBody &&
            refreshBody.code === CODE_SUCCESS &&
            refreshBody.data &&
            refreshBody.data.accessToken
        ) {
            // 刷新成功
            const newAccessToken = refreshBody.data.accessToken
            const newRefreshToken = refreshBody.data.refreshToken
            setToken(newAccessToken, newRefreshToken)

            // 批量重试等待队列：用新 token 重试
            // 对齐 spec 5.2.1 规则 4、Web 端 request.ts 第 147-152 行
            const queue = getPending()
            clearPending()
            queue.forEach((item) => {
                item.options.header = item.options.header || {}
                item.options.header.Authorization = 'Bearer ' + newAccessToken
                // item.resolve 接收的是 Promise，调用方 await 后得到 Result
                item.resolve(request(item.options))
            })

            // 重试原请求
            options.header = options.header || {}
            options.header.Authorization = 'Bearer ' + newAccessToken
            return request(options)
        } else {
            // 刷新失败：服务端返回非 200 业务码
            // H-F2 修复：显式 reject 队列中所有 Promise，避免永久 pending
            const queue = getPending()
            clearPending()
            const refreshError = new Error(
                (refreshBody && refreshBody.message) || 'Token 刷新失败'
            )
            queue.forEach((item) => item.reject(refreshError))
            clearToken()
            redirectToLogin()
            return Promise.reject(refreshError)
        }
    } catch (e) {
        // 刷新异常：网络异常/超时
        // H-F2 修复：显式 reject 队列中所有 Promise，避免永久 pending
        const queue = getPending()
        clearPending()
        const refreshError = new Error('Token 刷新异常')
        queue.forEach((item) => item.reject(refreshError))
        clearToken()
        redirectToLogin()
        return Promise.reject(refreshError)
    } finally {
        // 释放刷新锁
        setRefreshing(false)
    }
}

// ========== 对外暴露的便捷方法 ==========

/**
 * GET 请求
 * @param {string} url 路径（不含 BASE_URL）
 * @param {object} [data] 查询参数（wx.request GET 时作为 query）
 * @param {object} [header] 自定义头
 * @returns {Promise<Result<T>>}
 */
function get(url, data, header) {
    return request({ url, method: 'GET', data, header })
}

/**
 * POST 请求
 * @param {string} url
 * @param {object} [data] 请求体
 * @param {object} [header]
 * @returns {Promise<Result<T>>}
 */
function post(url, data, header) {
    return request({ url, method: 'POST', data, header })
}

/**
 * PUT 请求
 * @param {string} url
 * @param {object} [data]
 * @param {object} [header]
 * @returns {Promise<Result<T>>}
 */
function put(url, data, header) {
    return request({ url, method: 'PUT', data, header })
}

/**
 * DELETE 请求
 * @param {string} url
 * @param {object} [data]
 * @param {object} [header]
 * @returns {Promise<Result<T>>}
 */
function del(url, data, header) {
    return request({ url, method: 'DELETE', data, header })
}

module.exports = {
    request,
    get,
    post,
    put,
    del
}