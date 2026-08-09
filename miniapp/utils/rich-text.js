// utils/rich-text.js — 富文本清洗
//
// 过滤 script/style/link 等危险标签与事件属性，
// 供 rich-text 组件渲染前做双重防护（与后端 Jsoup XSS 过滤形成纵深防御）。
// 对齐：spec.md 5.3 节、主计划 3.6 节

/**
 * 过滤富文本 HTML 中的危险标签与属性
 * @param {string} html 原始 HTML
 * @returns {string} 清洗后的 HTML
 *
 * 处理内容：
 *   1. 移除 <script>...</script>（含内容）
 *   2. 移除 <style>...</style>（含内容）
 *   3. 移除 <link .../> 与 <meta .../>
 *   4. 移除 <iframe>...</iframe>
 *   5. 移除 <object>...</object>、<embed .../>
 *   6. 移除所有 onXXX 事件属性（onclick/onerror/onload 等）
 *   7. 移除 javascript: 协议的 href/src
 */
function sanitize(html) {
  if (!html || typeof html !== 'string') return ''

  let result = html

  // 1. 移除 script 标签（含内容），忽略大小写
  result = result.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
  // 2. 移除 style 标签（含内容）
  result = result.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')
  // 3. 移除 link / meta 自闭合标签
  result = result.replace(/<link\b[^>]*\/?>/gi, '')
  result = result.replace(/<meta\b[^>]*\/?>/gi, '')
  // 4. 移除 iframe 标签（含内容）
  result = result.replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
  result = result.replace(/<iframe\b[^>]*\/?>/gi, '')
  // 5. 移除 object / embed
  result = result.replace(/<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi, '')
  result = result.replace(/<embed\b[^>]*\/?>/gi, '')
  // 6. 移除所有 onXXX 事件属性
  result = result.replace(/\son\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi, '')
  // 7. 移除 javascript: 协议的 href/src
  result = result.replace(/(href|src)\s*=\s*("javascript:[^"]*"|'javascript:[^']*'|javascript:[^\s>]*)/gi, '$1=""')

  return result
}

module.exports = {
  sanitize
}