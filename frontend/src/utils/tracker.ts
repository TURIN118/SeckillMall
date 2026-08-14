/**
 * 埋点核心 SDK (T11)
 *
 * 设计要点:
 *   - 缓冲区 + 定时 flush: 默认 5 秒或满 20 条触发批量上报, 降低请求频次
 *   - 失败静默: 埋点不影响主流程, 上报失败仅 console.debug, 不弹 ElMessage
 *   - beforeunload 兜底: 页面关闭时尽力触发一次 flush (已知限制: 异步请求可能被浏览器取消)
 *   - 单例定时器: initTracker/destroyTracker 配对使用, 避免重复定时器
 */
import { trackEvent, type TrackItem } from '@/api/ai'

/** 埋点缓冲区 */
const BUFFER: TrackItem[] = []

/** 定时上报间隔 (ms) */
const FLUSH_INTERVAL = 5000

/** 缓冲区最大条数, 达到立即 flush */
const MAX_BUFFER_SIZE = 20

/** 定时器句柄 */
let timer: ReturnType<typeof setInterval> | null = null

/**
 * 计入埋点缓冲区
 * @param item 单条埋点事件
 */
export function track(item: TrackItem): void {
  BUFFER.push(item)
  if (BUFFER.length >= MAX_BUFFER_SIZE) {
    flush()
  }
}

/**
 * 批量上报: 取出缓冲区全部事件, 调用 trackEvent 批量接口
 * 失败静默, 不影响主流程
 */
function flush(): void {
  if (BUFFER.length === 0) return
  // 取出当前缓冲区全部事件并清空, 避免并发 flush 重复上报
  const events = BUFFER.splice(0, BUFFER.length)
  trackEvent({ events }).catch(() => {
    // 失败静默, 埋点不影响主流程
    console.debug('[tracker] flush failed, events dropped')
  })
}

/**
 * 初始化定时上报
 * 应在应用入口 (main.ts) 调用一次, 重复调用会先销毁旧定时器
 */
export function initTracker(): void {
  if (timer) {
    clearInterval(timer)
  }
  timer = setInterval(flush, FLUSH_INTERVAL)
  window.addEventListener('beforeunload', flush)
}

/**
 * 销毁 tracker (测试用 / 模块卸载用)
 * 清理定时器并移除 beforeunload 监听, 销毁前尽力 flush 一次
 */
export function destroyTracker(): void {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  window.removeEventListener('beforeunload', flush)
  flush()
}

/**
 * 事件类型常量
 * 与后端 track 模块约定的事件类型字符串保持一致
 */
export const EventType = {
  /** 曝光/浏览 */
  VIEW: 'VIEW',
  /** 点击 */
  CLICK: 'CLICK',
  /** 加入购物车 */
  ADD_TO_CART: 'ADD_CART',
  /** 收藏 */
  FAVORITE: 'FAVORITE',
  /** 下单/购买 */
  PURCHASE: 'ORDER',
  /** 搜索 */
  SEARCH: 'SEARCH'
} as const

/** 事件类型字面量联合 (便于强类型约束) */
export type EventTypeValue = (typeof EventType)[keyof typeof EventType]
