/**
 * v-track 埋点指令 (T11)
 *
 * 用法:
 *   <!-- 点击事件 (默认) -->
 *   <button v-track="{ targetType: 'product', targetId: 123 }">立即购买</button>
 *
 *   <!-- 点击事件显式指定 -->
 *   <button v-track="{ eventType: 'CLICK', targetType: 'product', targetId: 123 }">...</button>
 *
 *   <!-- 曝光事件: mounted 时立即触发一次 -->
 *   <div v-track="{ eventType: 'VIEW', targetType: 'banner', targetId: 'home-1' }">...</div>
 *
 *   <!-- 加入购物车 -->
 *   <button v-track="{ eventType: 'ADD_CART', targetType: 'sku', targetId: 456 }">加入购物车</button>
 *
 * 设计说明:
 *   - VIEW 事件在 mounted 钩子立即触发一次 (适合曝光统计)
 *   - 其他事件 (CLICK/ADD_CART/...) 绑定原生 click 监听
 *   - targetId 统一转 string 上报, 与后端字段约定一致
 */
import type { Directive, DirectiveBinding } from 'vue'
import { track, EventType, type EventTypeValue } from '@/utils/tracker'

/** v-track 指令绑定的值结构 */
export interface TrackBindingValue {
  /** 事件类型, 默认 CLICK */
  eventType?: EventTypeValue | string
  /** 目标类型 */
  targetType?: string
  /** 目标 ID */
  targetId?: string | number
  /** 扩展字段 (JSON 字符串) */
  ext?: string
}

/**
 * 构造一条 TrackItem
 * targetId 统一转 string, undefined / null 转空串
 */
function buildItem(value: TrackBindingValue) {
  const {
    eventType = EventType.CLICK,
    targetType,
    targetId,
    ext
  } = value || {}
  return {
    eventType,
    targetType,
    targetId: String(targetId ?? ''),
    ext
  }
}

export const vTrack: Directive<HTMLElement, TrackBindingValue> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<TrackBindingValue>) {
    const value = binding.value || {}
    const { eventType = EventType.CLICK } = value

    if (eventType === EventType.VIEW) {
      // VIEW 事件在 mounted 时立即触发一次 (曝光统计)
      track(buildItem(value))
    } else {
      // CLICK / ADD_CART / FAVORITE / ORDER / SEARCH 等事件绑定 click 监听
      el.addEventListener('click', () => {
        track(buildItem(value))
      })
    }
  }
}