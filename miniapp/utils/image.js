// utils/image.js — 图片 URL 拼接工具
//
// 后端返回的图片路径多为相对路径（如 /images/products/2026/08/09/xxx.png），
// 微信小程序原生 image 组件会将其解析为小程序包内本地资源，导致加载失败（500）。
// 因此在展示前需拼接后端 BASE_URL。
//
// 对齐：frontend/src/utils/image.ts 的 formatImageUrl

const { BASE_URL } = require('../config/env')

/**
 * 格式化图片 URL：
 *  - 空值返回空字符串
 *  - 已是 http(s):// 或 data:image/ 直接返回
 *  - 否则在前缀补 '/' 后拼接 BASE_URL
 * @param {string | undefined | null} url
 * @returns {string}
 */
function formatImageUrl(url) {
    if (!url) return ''
    const s = String(url)
    if (s.startsWith('http://') || s.startsWith('https://') || s.startsWith('data:image/')) return s
    const normalized = s.startsWith('/') ? s : '/' + s
    return BASE_URL + normalized
}

module.exports = { formatImageUrl }