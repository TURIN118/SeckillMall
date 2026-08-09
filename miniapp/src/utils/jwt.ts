/**
 * JWT 解析工具（对齐 plan.md 第 4.4 节）
 * 微信小程序不支持 atob，使用 wx.base64ToArrayBuffer 替代
 * 用于 Token 过期检测（isTokenExpired）
 */

/**
 * base64 解码（兼容小程序环境）
 * 微信小程序基础库 2.4.0+ 支持 wx.base64ToArrayBuffer
 */
function base64Decode(str: string): string {
  // 补齐 padding
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  const pad = base64.length % 4
  if (pad) {
    base64 += '===='.slice(0, 4 - pad)
  }

  // #ifdef MP-WEIXIN
  // 微信小程序环境：使用 wx.base64ToArrayBuffer
  const arrayBuffer = wx.base64ToArrayBuffer(base64)
  const bytes = new Uint8Array(arrayBuffer)
  let result = ''
  for (let i = 0; i < bytes.length; i++) {
    result += String.fromCharCode(bytes[i])
  }
  return decodeURIComponent(escape(result))
  // #endif

  // #ifndef MP-WEIXIN
  // 非微信小程序环境，使用 atob
  return decodeURIComponent(escape(atob(base64)))
  // #endif
}

/**
 * 解析 JWT payload
 * @param token JWT 字符串
 * @returns payload 对象，解析失败返回 null
 */
export function parseJwtPayload(token: string): any {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      throw new Error('Invalid JWT format')
    }
    const payloadStr = base64Decode(parts[1])
    return JSON.parse(payloadStr)
  } catch (e) {
    console.error('JWT 解析失败', e)
    return null
  }
}

/**
 * 判断 Token 是否过期
 * @param token JWT 字符串
 * @returns true 已过期 / false 未过期或无法判断
 */
export function isTokenExpired(token: string): boolean {
  const payload = parseJwtPayload(token)
  if (!payload || !payload.exp) {
    return true
  }
  // exp 是秒级时间戳
  const now = Math.floor(Date.now() / 1000)
  return payload.exp < now
}

/**
 * 获取 Token 剩余有效时间（秒）
 * @returns 剩余秒数，无法解析返回 0
 */
export function getTokenRemainingTime(token: string): number {
  const payload = parseJwtPayload(token)
  if (!payload || !payload.exp) {
    return 0
  }
  const now = Math.floor(Date.now() / 1000)
  return Math.max(0, payload.exp - now)
}