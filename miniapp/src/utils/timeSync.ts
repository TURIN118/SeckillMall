/**
 * 服务器时间同步工具（对齐 plan.md 第 4 章 / spec.md 2.4）
 * timeOffset = serverTime - localTime
 * 用于秒杀倒计时与服务端时间对齐，避免客户端时钟偏差
 */

// 模块级时间偏移量（毫秒）
let timeOffset = 0

/**
 * 同步服务器时间
 * @param timestamp 服务器返回的时间戳（string，毫秒或秒级）
 * @returns 当前 timeOffset
 */
export function syncServerTime(timestamp: string): number {
  try {
    // 兼容秒级/毫秒级时间戳
    let serverTime = Number(timestamp)
    if (!isNaN(serverTime)) {
      // 若为 10 位秒级时间戳，转为毫秒
      if (timestamp.length <= 10) {
        serverTime = serverTime * 1000
      }
      const localTime = Date.now()
      timeOffset = serverTime - localTime
    }
  } catch (e) {
    console.error('同步服务器时间失败', e)
  }
  return timeOffset
}

/**
 * 获取服务器当前时间（毫秒）
 */
export function getServerTime(): number {
  return Date.now() + timeOffset
}

/**
 * 获取时间偏移量（毫秒）
 */
export function getTimeOffset(): number {
  return timeOffset
}

/**
 * 重置时间偏移（测试用）
 */
export function resetTimeOffset(): void {
  timeOffset = 0
}