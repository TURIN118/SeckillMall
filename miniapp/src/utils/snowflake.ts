/**
 * 雪花 ID 处理工具（对齐 plan.md 第 4.6 节 / spec.md 1.6）
 * 后端使用雪花算法生成 ID，超过 JS Number.MAX_SAFE_INTEGER（2^53 - 1）
 * 全程使用 string 类型，URL 路径参数用 encodeURIComponent 编码
 */

/**
 * 确保 ID 为 string 类型
 * 防止后端返回的雪花 ID 被隐式转为 number 导致精度丢失
 */
export function ensureStringId(id: string | number): string {
  return String(id)
}

/**
 * URL 路径参数编码
 * 雪花 ID 用于 URL 路径时需 encodeURIComponent
 */
export function encodeId(id: string | number): string {
  return encodeURIComponent(ensureStringId(id))
}

/**
 * 构建带 ID 的 URL 路径
 * @param base 基础路径，如 '/products'
 * @param id 雪花 ID
 * @returns 如 '/products/2085560004061081601'
 */
export function buildPath(base: string, id: string | number): string {
  return `${base}/${encodeId(id)}`
}

/**
 * 批量构建带 ID 的 URL 路径
 * @param base 基础路径
 * @param ids 雪花 ID 数组
 * @returns 如 '/products/1,2,3'（具体拼接规则按需调整）
 */
export function buildPathWithIds(base: string, ids: Array<string | number>): string {
  const encodedIds = ids.map(id => encodeId(id)).join(',')
  return `${base}/${encodedIds}`
}