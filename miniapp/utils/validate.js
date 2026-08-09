// utils/validate.js — 表单校验工具
//
// 纯函数，无副作用。对齐 spec.md 5.3 节规则 4、design.md 2.2 节

/**
 * 手机号校验：11 位数字，1 开头
 * @param {string} s
 * @returns {boolean}
 */
function isPhone(s) {
  if (!s) return false
  return /^1[3-9]\d{9}$/.test(String(s))
}

/**
 * 邮箱校验
 * @param {string} s
 * @returns {boolean}
 */
function isEmail(s) {
  if (!s) return false
  // 标准 RFC 简化正则，覆盖常见邮箱格式
  return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(String(s))
}

/**
 * 密码强度校验
 * @param {string} s 密码
 * @returns {{valid: boolean, msg: string}} 校验结果与提示
 *
 * 规则（对齐常见账号体系）：
 *   - 长度 6-20 位
 *   - 必须包含字母和数字（允许特殊字符）
 */
function isPassword(s) {
  if (!s) return { valid: false, msg: '密码不能为空' }
  const str = String(s)
  if (str.length < 6 || str.length > 20) {
    return { valid: false, msg: '密码长度为 6-20 位' }
  }
  if (!/[A-Za-z]/.test(str)) {
    return { valid: false, msg: '密码必须包含字母' }
  }
  if (!/\d/.test(str)) {
    return { valid: false, msg: '密码必须包含数字' }
  }
  return { valid: true, msg: '' }
}

/**
 * 简单非空校验
 * @param {any} v
 * @returns {boolean}
 */
function isNotEmpty(v) {
  if (v == null) return false
  if (typeof v === 'string') return v.trim().length > 0
  if (Array.isArray(v)) return v.length > 0
  return true
}

/**
 * URL 校验（http/https）
 * @param {string} s
 * @returns {boolean}
 */
function isHttpUrl(s) {
  if (!s) return false
  return /^https?:\/\/.+/i.test(String(s))
}

module.exports = {
  isPhone,
  isEmail,
  isPassword,
  isNotEmpty,
  isHttpUrl
}