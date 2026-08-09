// utils/id.js — 雪花 ID 安全处理
//
// 后端使用雪花算法生成 ID（18-19 位长整型），超过 JS Number.MAX_SAFE_INTEGER，
// 必须全程以 string 类型承载，禁止 Number(id) 转换。
// 对齐：spec.md 5.3 节规则 1、4.3 节安全性、design.md 4.2 节、主计划 2.5 节

/**
 * 安全拼接 URL，对 ID 进行 encodeURIComponent 编码
 * @param {string} path 路径前缀，如 '/api/v1/products/'
 * @param {string|number} id 雪花 ID（强制转 string 后编码）
 * @returns {string} 拼接后的 URL，如 '/api/v1/products/2085560004061081601'
 *
 * 示例：
 *   buildUrl('/api/v1/products/', '2085560004061081601')
 *   => '/api/v1/products/2085560004061081601'
 *   buildUrl('/api/v1/seckill/', 123)  // 数字也会被安全转 string
 *   => '/api/v1/seckill/123'
 */
function buildUrl(path, id) {
  // 强制 String 转换，禁止 Number 化；encodeURIComponent 防止特殊字符注入
  return path + encodeURIComponent(String(id))
}

/**
 * 字符串比较两个 ID 是否相等
 * @param {any} a ID a
 * @param {any} b ID b
 * @returns {boolean} String(a) === String(b)
 *
 * 说明：避免 === 直接比较时 number 与 string 的隐式差异，
 * 同时禁止 Number 转换（精度丢失风险）。
 */
function equalId(a, b) {
  if (a == null || b == null) return false
  return String(a) === String(b)
}

/**
 * 安全将 ID 转为 string（禁止 Number 化的统一入口）
 * @param {any} id
 * @returns {string}
 */
function toString(id) {
  if (id == null) return ''
  return String(id)
}

module.exports = {
  buildUrl,
  equalId,
  toString
}