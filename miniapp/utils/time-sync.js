// utils/time-sync.js — 服务器时间同步
//
// 维护模块级 timeOffset = serverTime - localTime（毫秒），
// 供秒杀倒计时与服务端时间对齐，避免客户端时钟偏差。
// 对齐：spec.md 5.3 节规则 2、design.md 2.2 节、主计划 2.3 节

const storage = require('./storage')
const { TIME_OFFSET_KEY } = require('../config/constants')

/** 模块级时间偏移（ms），默认从本地存储恢复，避免冷启动丢失 */
let timeOffset = 0

// 初始化时尝试从本地存储恢复 timeOffset
try {
    const stored = storage.get(TIME_OFFSET_KEY)
    if (typeof stored === 'number' && !Number.isNaN(stored)) {
        timeOffset = stored
    }
} catch (e) {
    // 静默处理
}

/**
 * 同步服务器时间，计算并更新 timeOffset
 * @param {string|number} timestamp ISO 字符串或毫秒时间戳
 *   - 非法 timestamp（NaN/无法解析）跳过，保持原值不变
 *   - 对齐 spec 5.3.3 异常场景 1
 */
function syncServerTime(timestamp) {
    if (timestamp == null) return
    const serverTime = typeof timestamp === 'number'
        ? timestamp
        : new Date(timestamp).getTime()
    if (Number.isNaN(serverTime)) return
    const localTime = Date.now()
    timeOffset = serverTime - localTime
    // 持久化，避免冷启动丢失（秒杀倒计时依赖）
    storage.set(TIME_OFFSET_KEY, timeOffset)
}

/**
 * 获取时间偏移量
 * @returns {number} timeOffset（ms），可为负数
 */
function getTimeOffset() {
    return timeOffset
}

/**
 * 获取对齐后的当前服务器时间（毫秒）
 * @returns {number} localTime + timeOffset
 */
function getServerNow() {
    return Date.now() + timeOffset
}

/**
 * 重置时间偏移（主要用于登出/测试）
 */
function resetTimeOffset() {
    timeOffset = 0
    storage.remove(TIME_OFFSET_KEY)
}

module.exports = {
    syncServerTime,
    getTimeOffset,
    getServerNow,
    resetTimeOffset
}