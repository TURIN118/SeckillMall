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

const { get, post, put } = require('../utils/request')
const { BASE_URL } = require('../config/env')
const { getAccessToken } = require('../utils/auth')

// ========== 接口端点常量 ==========
const API = {
  LOGIN: '/api/v1/auth/login',
  REGISTER: '/api/v1/auth/register',
  CAPTCHA: '/api/v1/auth/captcha',
  REFRESH: '/api/v1/auth/refresh',
  ME: '/api/v1/auth/me',
  LOGOUT: '/api/v1/auth/logout',
  FORGOT_SEND: '/api/v1/auth/forgot-password/send-code',
  FORGOT_RESET: '/api/v1/auth/forgot-password/reset',
  // U1: usercenter 模块新增
  PROFILE: '/api/v1/auth/profile',
  AVATAR: '/api/v1/auth/avatar',
  PASSWORD: '/api/v1/auth/password'
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

// ========== U1: usercenter 模块新增方法 ==========

/**
 * 更新个人信息（昵称等）
 * 对齐后端 PUT /api/v1/auth/profile，ProfileUpdateRequest
 * @param {object} data { nickname, ... }
 * @returns {Promise<Result<UserVO>>}
 */
function updateProfile(data) {
  return put(API.PROFILE, data)
}

/**
 * 修改密码
 * 对齐后端 PUT /api/v1/auth/password，ChangePasswordRequest
 * @param {object} data { oldPassword, newPassword }
 * @returns {Promise<Result<void>>}
 */
function changePassword(data) {
  return put(API.PASSWORD, data)
}

/**
 * 上传头像
 * 对齐后端 POST /api/v1/auth/avatar（MultipartFile file）
 *
 * 用 wx.uploadFile（非 wx.request）Promise 化，header 携带 Authorization Bearer。
 * 后端返回 Result<Map<String,String>>，取 data.avatarUrl 作为新头像地址。
 *
 * @param {string} filePath 微信本地临时文件路径（wx.chooseImage 返回的 tempFilePaths[i]）
 * @returns {Promise<string>} 头像 URL（res.data.data.avatarUrl）
 *
 * 异常：
 *   - 未登录（无 token）→ reject Error('未登录')
 *   - HTTP/业务失败 → reject Error(message)
 *   - 网络层失败 → reject Error(errMsg)
 */
function uploadAvatar(filePath) {
  const token = getAccessToken()
  if (!token) {
    return Promise.reject(new Error('未登录'))
  }
  if (!filePath) {
    return Promise.reject(new Error('缺少文件路径'))
  }

  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: BASE_URL + API.AVATAR,
      filePath: filePath,
      name: 'file',
      header: {
        Authorization: 'Bearer ' + token
      },
      success: (res) => {
        // res.data 是字符串，需 JSON.parse
        let body = null
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        } catch (e) {
          reject(new Error('上传响应解析失败'))
          return
        }

        const statusCode = res.statusCode
        if (statusCode >= 200 && statusCode < 300 && body && body.code === 200) {
          const data = body.data || {}
          const avatarUrl = data.avatarUrl || ''
          resolve(avatarUrl)
        } else {
          const msg = (body && body.message) || '上传失败'
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        const errMsg = (err && err.errMsg) || '上传失败'
        reject(new Error(errMsg))
      }
    })
  })
}

module.exports = {
  login,
  register,
  getCaptcha,
  refreshToken,
  getMe,
  logout,
  sendForgotCode,
  resetPassword,
  // U1: usercenter 模块新增
  updateProfile,
  uploadAvatar,
  changePassword
}