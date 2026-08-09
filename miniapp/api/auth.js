// api/auth.js — 认证接口封装
//
// 集中封装所有认证相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端 DTO 字段名与端点：
//   - LoginRequest            { username, password, captchaKey?, captchaCode? }
//   - RegisterRequest         { username, password, phone, captchaKey, captchaCode }
//   - RefreshTokenRequest     { refreshToken }
//   - ForgotPasswordSendRequest  { type, account }
//   - ForgotPasswordResetRequest { type, account, code, newPassword }
//
// 对齐：
//   - design.md 2.2 节接口清单
//   - spec.md 4.4 节规则 1（认证接口调用集中在 api/auth.js）
//   - 后端 AuthController.java 端点
//   - Web 端 frontend/src/api/auth.ts 字段对齐

const { get, post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  LOGIN: '/api/v1/auth/login',
  REGISTER: '/api/v1/auth/register',
  CAPTCHA: '/api/v1/auth/captcha',
  REFRESH: '/api/v1/auth/refresh',
  ME: '/api/v1/auth/me',
  LOGOUT: '/api/v1/auth/logout',
  FORGOT_SEND: '/api/v1/auth/forgot-password/send-code',
  FORGOT_RESET: '/api/v1/auth/forgot-password/reset'
}

/**
 * 用户登录
 * @param {object} data { username, password, captchaKey?, captchaCode? }
 * @returns {Promise<Result<{accessToken, refreshToken, user}>>}
 */
function login(data) {
  return post(API.LOGIN, data)
}

/**
 * 用户注册
 * @param {object} data { username, password, phone, captchaKey, captchaCode }
 * @returns {Promise<Result<UserVO>>}
 */
function register(data) {
  return post(API.REGISTER, data)
}

/**
 * 获取图形验证码
 * @returns {Promise<Result<{captchaKey, image}>>}
 */
function getCaptcha() {
  return get(API.CAPTCHA)
}

/**
 * 刷新令牌
 * @param {object} data { refreshToken }
 * @returns {Promise<Result<{accessToken, refreshToken}>>}
 */
function refreshToken(data) {
  return post(API.REFRESH, data)
}

/**
 * 获取当前登录用户信息
 * @returns {Promise<Result<UserVO>>}
 */
function getMe() {
  return get(API.ME)
}

/**
 * 退出登录
 * @returns {Promise<Result<void>>}
 */
function logout() {
  return post(API.LOGOUT)
}

/**
 * 找回密码 - 发送验证码（手机短信或邮箱）
 * @param {object} data { type: 'PHONE'|'EMAIL', account }
 * @returns {Promise<Result<void>>}
 */
function sendForgotCode(data) {
  return post(API.FORGOT_SEND, data)
}

/**
 * 找回密码 - 校验验证码并重置密码
 * @param {object} data { type, account, code, newPassword }
 * @returns {Promise<Result<void>>}
 */
function resetPassword(data) {
  return post(API.FORGOT_RESET, data)
}

module.exports = {
  login,
  register,
  getCaptcha,
  refreshToken,
  getMe,
  logout,
  sendForgotCode,
  resetPassword
}