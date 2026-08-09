// api/user.js — 用户接口封装（usercenter 模块）
//
// 集中封装用户资料相关 HTTP 调用（手机号/邮箱修改），页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - PhoneUpdateRequest { phone, code }
//   - EmailUpdateRequest { email, code }
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 4 节
//   - .codeartsdoer/specs/usercenter/design.md 4 节
//   - 后端 UsersController.java 端点

const { put } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
  PHONE: '/api/v1/users/profile/phone', // PUT 修改手机号
  EMAIL: '/api/v1/users/profile/email'  // PUT 修改邮箱
}

/**
 * 修改手机号
 * @param {string} phone 新手机号
 * @param {string} code 验证码
 * @returns {Promise<Result<void>>}
 */
function updatePhone(phone, code) {
  return put(API.PHONE, { phone: phone, code: code })
}

/**
 * 修改邮箱
 * @param {string} email 新邮箱
 * @param {string} code 验证码
 * @returns {Promise<Result<void>>}
 */
function updateEmail(email, code) {
  return put(API.EMAIL, { email: email, code: code })
}

module.exports = {
  updatePhone,
  updateEmail
}