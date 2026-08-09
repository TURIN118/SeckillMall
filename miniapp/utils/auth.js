// utils/auth.js — 鉴权状态管理
//
// 职责：
//   1. Token 存取（access_token / refresh_token / user_info）
//   2. 刷新并发控制：模块级刷新锁 isRefreshing + 等待队列 pendingQueue
//
// 对齐：
//   - spec.md 5.2 节（鉴权状态管理全部业务规则）
//   - design.md 2.2 节、4.2 节（刷新并发控制模型）
//   - Web 端 frontend/src/api/request.ts 第 36-47 行（H-F2 修复：刷新失败显式 reject 队列）
//   - 主计划 6.2 节、6.3 节

const storage = require('./storage')
const {
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  USER_INFO_KEY
} = require('../config/constants')

// ========== 模块级刷新并发控制状态（闭包变量） ==========
/** 刷新锁：true 表示正在刷新 access_token，期间新的过期请求入队等待 */
let refreshing = false
/**
 * 等待队列：刷新锁持有期间到达的需刷新请求暂存于此
 * @type {Array<{options: object, resolve: Function, reject: Function}>}
 */
let pendingQueue = []

// ========== Token 存取 ==========

/**
 * 读取 access_token
 * @returns {string|null}
 */
function getAccessToken() {
  const token = storage.get(ACCESS_TOKEN_KEY)
  return token || null
}

/**
 * 读取 refresh_token
 * @returns {string|null}
 */
function getRefreshToken() {
  const token = storage.get(REFRESH_TOKEN_KEY)
  return token || null
}

/**
 * 写入双令牌
 * @param {string} access access_token
 * @param {string} refresh refresh_token
 */
function setToken(access, refresh) {
  storage.set(ACCESS_TOKEN_KEY, access)
  storage.set(REFRESH_TOKEN_KEY, refresh)
}

/**
 * 清空所有令牌与用户信息（登出 / 刷新失败时调用）
 * 对齐 spec 5.2.1 规则 7：禁止刷新失败后残留失效令牌
 */
function clearToken() {
  storage.remove(ACCESS_TOKEN_KEY)
  storage.remove(REFRESH_TOKEN_KEY)
  storage.remove(USER_INFO_KEY)
}

// ========== 登录态查询与登录引导 ==========

/**
 * 判断当前是否已登录
 * 依据本地是否存在 access_token
 * 对齐 spec 5.5.1 规则 1：登录态判定规则
 * @returns {boolean}
 */
function isLoggedIn() {
  return !!getAccessToken()
}

/**
 * 跳转登录页（携带 redirect 参数）
 * 对齐 spec 5.5.1 规则 2：未登录访问受保护页 → 跳登录页 + redirect=原页面
 * @param {string} [redirect] 登录后回跳目标页路径（带 query）
 */
function navigateToLogin(redirect) {
  let url = '/pages/login/login'
  if (redirect && typeof redirect === 'string') {
    // 对 redirect 做 encodeURIComponent，避免 query 中 & 污染登录页 onLoad options
    url += '?redirect=' + encodeURIComponent(redirect)
  }
  wx.navigateTo({ url })
}

// ========== 用户信息存取 ==========

/**
 * 读取用户信息
 * @returns {object|null}
 */
function getUserInfo() {
  const info = storage.get(USER_INFO_KEY)
  return info || null
}

/**
 * 写入用户信息
 * @param {object} info 用户信息对象
 */
function setUserInfo(info) {
  storage.set(USER_INFO_KEY, info)
}

// ========== 刷新锁 ==========

/**
 * 查询刷新锁状态
 * @returns {boolean}
 */
function isRefreshing() {
  return refreshing
}

/**
 * 设置刷新锁状态
 * @param {boolean} v
 */
function setRefreshing(v) {
  refreshing = !!v
}

// ========== 等待队列 ==========

/**
 * 入等待队列
 * @param {{options: object, resolve: Function, reject: Function}} item
 */
function pushPending(item) {
  pendingQueue.push(item)
}

/**
 * 取等待队列（返回引用，调用方遍历后应配合 clearPending 清空）
 * @returns {Array}
 */
function getPending() {
  return pendingQueue
}

/**
 * 清空等待队列
 */
function clearPending() {
  pendingQueue = []
}

module.exports = {
  // Token 存取
  getAccessToken,
  getRefreshToken,
  setToken,
  clearToken,
  // 用户信息
  getUserInfo,
  setUserInfo,
  // 登录态查询与登录引导
  isLoggedIn,
  navigateToLogin,
  // 刷新锁
  isRefreshing,
  setRefreshing,
  // 等待队列
  pushPending,
  getPending,
  clearPending
}