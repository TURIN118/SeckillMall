// utils/storage.js — wx.storage 同步 API 封装
//
// 提供统一的本地存储读写入口，便于后续替换为异步或加密存储方案。
// 对齐：design.md 2.2 节

/**
 * 同步读取本地存储
 * @param {string} key 键名
 * @returns {any} 值，不存在时返回空字符串 ''（wx.getStorageSync 行为）
 */
function get(key) {
    try {
        return wx.getStorageSync(key)
    } catch (e) {
        // 读取异常时返回空字符串，避免抛出导致程序崩溃
        return ''
    }
}

/**
 * 同步写入本地存储
 * @param {string} key 键名
 * @param {any} value 值
 */
function set(key, value) {
    try {
        wx.setStorageSync(key, value)
    } catch (e) {
        // 写入异常静默处理（如存储空间不足），上层可按需扩展提示
    }
}

/**
 * 同步删除本地存储
 * @param {string} key 键名
 */
function remove(key) {
    try {
        wx.removeStorageSync(key)
    } catch (e) {
        // 删除异常静默处理
    }
}

/**
 * 清空所有本地存储（谨慎使用）
 */
function clear() {
    try {
        wx.clearStorageSync()
    } catch (e) {
        // 静默处理
    }
}

module.exports = {
    get,
    set,
    remove,
    clear
}