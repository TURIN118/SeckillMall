// utils/debounce.js — 防抖 / 节流
//
// 用于秒杀按钮等高频操作防连点。对齐 spec.md 5.3 节规则 5、design.md 2.2 节

/**
 * 防抖：在指定等待时间内多次调用，仅最后一次生效
 * @param {Function} fn 目标函数
 * @param {number} wait 等待毫秒数，默认 300ms
 * @param {boolean} [immediate=false] 是否立即执行首次
 * @returns {Function} 防抖后的函数
 */
function debounce(fn, wait = 300, immediate = false) {
  let timer = null
  return function (...args) {
    const ctx = this
    if (timer) clearTimeout(timer)
    if (immediate && !timer) {
      fn.apply(ctx, args)
    }
    timer = setTimeout(() => {
      timer = null
      if (!immediate) fn.apply(ctx, args)
    }, wait)
  }
}

/**
 * 节流：在指定间隔内多次调用，仅首次生效
 * @param {Function} fn 目标函数
 * @param {number} wait 间隔毫秒数，默认 300ms
 * @param {boolean} [leading=true] 是否首次立即执行
 * @param {boolean} [trailing=false] 是否尾部追加一次
 * @returns {Function} 节流后的函数
 */
function throttle(fn, wait = 300, leading = true, trailing = false) {
  let lastTime = 0
  let timer = null
  return function (...args) {
    const ctx = this
    const now = Date.now()
    if (!lastTime && !leading) lastTime = now
    const remaining = wait - (now - lastTime)
    if (remaining <= 0 || remaining > wait) {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
      lastTime = now
      fn.apply(ctx, args)
    } else if (trailing && !timer) {
      timer = setTimeout(() => {
        lastTime = leading ? Date.now() : 0
        timer = null
        fn.apply(ctx, args)
      }, remaining)
    }
  }
}

module.exports = {
  debounce,
  throttle
}