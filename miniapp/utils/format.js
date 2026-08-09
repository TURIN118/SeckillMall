// utils/format.js — 格式化工具
//
// 纯函数，无副作用。对齐 spec.md 5.3 节规则 3、design.md 2.2 节

/**
 * 格式化价格为两位小数字符串
 * @param {number|string} n 价格数值
 * @returns {string} 两位小数，如 formatPrice(99.5) => '99.50'
 *
 * 说明：
 *   - 输入为 string 时先转 number
 *   - NaN 或非法值返回 '0.00'
 *   - 负数按原值格式化（业务层负责校验）
 */
function formatPrice(n) {
  const num = typeof n === 'string' ? parseFloat(n) : Number(n)
  if (Number.isNaN(num)) return '0.00'
  // toFixed 可能因浮点精度产生四舍五入偏差，对金额场景可接受；
  // 若需精确可改用整数分运算，此处保持简单对齐 spec 验收条件。
  return num.toFixed(2)
}

/**
 * 手机号脱敏：中间 4 位替换为星号
 * @param {string} phone 11 位手机号
 * @returns {string} 如 maskPhone('13812345678') => '138****5678'
 *
 * 说明：非 11 位手机号原样返回（不脱敏），避免误处理短号
 */
function maskPhone(phone) {
  if (!phone) return ''
  const s = String(phone)
  if (s.length !== 11) return s
  return s.slice(0, 3) + '****' + s.slice(7)
}

/**
 * 简单日期格式化
 * @param {string|number|Date} date 日期
 * @param {string} fmt 格式串，默认 'YYYY-MM-DD HH:mm:ss'
 *   支持占位符：YYYY MM DD HH mm ss
 * @returns {string} 格式化后的日期字符串
 *
 * 示例：
 *   formatDate('2026-08-09T10:00:00', 'YYYY-MM-DD') => '2026-08-09'
 *   formatDate(1723180800000, 'YYYY/MM/DD HH:mm') => '2026/08/09 12:00'
 */
function formatDate(date, fmt) {
  if (!date) return ''
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  const map = {
    YYYY: d.getFullYear(),
    MM: pad(d.getMonth() + 1),
    DD: pad(d.getDate()),
    HH: pad(d.getHours()),
    mm: pad(d.getMinutes()),
    ss: pad(d.getSeconds())
  }
  let result = fmt || 'YYYY-MM-DD HH:mm:ss'
  // 按 YYYY → MM → DD → HH → mm → ss 顺序替换
  Object.keys(map).forEach((key) => {
    result = result.replace(key, map[key])
  })
  return result
}

/**
 * 金额分转元（后端金额若以分为单位）
 * @param {number} cents 分
 * @returns {string} 元，两位小数
 */
function centsToYuan(cents) {
  const num = Number(cents)
  if (Number.isNaN(num)) return '0.00'
  return (num / 100).toFixed(2)
}

/**
 * 元转分（提交给后端时若需以分为单位）
 * @param {number|string} yuan 元
 * @returns {number} 分（整数）
 */
function yuanToCents(yuan) {
  const num = typeof yuan === 'string' ? parseFloat(yuan) : Number(yuan)
  if (Number.isNaN(num)) return 0
  return Math.round(num * 100)
}

module.exports = {
  formatPrice,
  maskPhone,
  formatDate,
  centsToYuan,
  yuanToCents
}