/**
 * 格式化图片URL
 * 后端返回 /images/products/... 相对路径，需拼接后端 baseURL
 */
export function formatImageUrl(url: string | undefined | null): string {
    if (!url) return ''
    // 完整URL或base64数据直接返回
    if (url.startsWith('http') || url.startsWith('data:')) return url
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const normalizedUrl = url.startsWith('/') ? url : `/${url}`
    return `${baseUrl}${normalizedUrl}`
}