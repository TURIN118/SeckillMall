/**
 * 格式化图片URL
 * 后端返回 /images/products/... 相对路径，需拼接后端 baseURL
 */
export function formatImageUrl(url: string | undefined | null): string {
    if (!url) return ''
    // L28 修复: 严格校验 URL 协议，仅放行 http://、https://、data:image/
    // 原实现 startsWith('http') 会误匹配 'httpfoo'，startsWith('data:') 会放行非图片 data URL (如 data:text/html)
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:image/')) return url
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const normalizedUrl = url.startsWith('/') ? url : `/${url}`
    return `${baseUrl}${normalizedUrl}`
}