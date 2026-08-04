/**
 * Axios 实例配置 + 拦截器
 * 参照 10-ai-design-spec.md "API Client Layer"
 */
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

/** localStorage token 键名 */
export const ACCESS_TOKEN_KEY = 'access_token'
export const REFRESH_TOKEN_KEY = 'refresh_token'

/** 服务器时间偏移 (ms) */
let timeOffset = 0

/** 同步服务器时间 */
export function syncServerTime(serverTimestamp: string): void {
  const serverTime = new Date(serverTimestamp).getTime()
  if (Number.isNaN(serverTime)) return
  const localTime = Date.now()
  timeOffset = serverTime - localTime
}


/** 获取时间偏移量 */
export function getTimeOffset(): number {
  return timeOffset
}

/** 从 localStorage 读取 token (避免循环依赖, 不引入 store) */
function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

/** 创建 Axios 实例 */
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/** 请求拦截器: 添加 Authorization Bearer token */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器: 处理业务码 + HTTP 错误 */
request.interceptors.response.use(
  (response) => {
    // blob 响应（如 Excel 导出）直接返回原始数据，跳过业务码解包
    if (response.config.responseType === 'blob') {
      return response.data as unknown as typeof response
    }
    const res = response.data as Result
    // 同步服务器时间
    if (res?.timestamp) {
      syncServerTime(res.timestamp)
    }
    // 业务码非 200 视为错误 (后端统一成功码为 200)
    if (res && typeof res.code === 'number' && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || `业务错误: code=${res.code}`))
    }
    // 返回解包后的 Result<T> (而非 AxiosResponse)
    return res as unknown as typeof response
  },
  (error) => {
    const { response, message } = error
    if (response) {
      const status = response.status
      const currentPath = window.location.pathname + window.location.search
      switch (status) {
        case 401:
          // 登录过期, 清除 token, 跳转登录
          localStorage.removeItem(ACCESS_TOKEN_KEY)
          localStorage.removeItem(REFRESH_TOKEN_KEY)
          ElMessage.warning('登录已过期，请重新登录')
          if (!currentPath.startsWith('/login')) {
            // M40 说明: 此处使用 window.location.href 而非 router.push 是有意为之。
            // axios 拦截器中无法安全访问 router 实例 (循环依赖风险)，
            // 且 401 时需彻底重置应用状态 (清空 Pinia store、组件状态等)，
            // 完整刷新比 SPA 跳转更安全可靠。redirect 参数保证登录后可回到原页面。
            window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
          }
          break
        case 403:
          ElMessage.error('您没有权限访问该页面')
          if (!currentPath.startsWith('/403')) {
            // M40 说明: 同 401，使用完整跳转重置应用状态
            window.location.href = '/403'
          }
          break
        case 429:
          ElMessage.warning('请求太频繁，请稍后再试')
          break
        default:
          if (status >= 500) {
            ElMessage.error('服务器异常，请稍后重试')
          } else {
            // L27 修复: 兼容后端返回纯字符串错误体或 { message } 对象两种格式
            const msg = typeof response.data === 'string'
              ? response.data
              : (response.data?.message || message)
            ElMessage.error(msg || `请求错误 (${status})`)
          }
      }
    } else if (message?.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

/** 通用请求方法, 返回 Result<T> */
export function http<T = unknown>(config: AxiosRequestConfig): Promise<Result<T>> {
  return request(config) as Promise<Result<T>>
}

/** GET 请求 */
export function get<T = unknown>(
  url: string,
  params?: Record<string, unknown> | object,
  config?: AxiosRequestConfig
): Promise<Result<T>> {
  return http<T>({ method: 'GET', url, params, ...config })
}

/** POST 请求 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<Result<T>> {
  return http<T>({ method: 'POST', url, data, ...config })
}

/** PUT 请求 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<Result<T>> {
  return http<T>({ method: 'PUT', url, data, ...config })
}

/** DELETE 请求 */
export function del<T = unknown>(
  url: string,
  params?: Record<string, unknown> | object,
  config?: AxiosRequestConfig
): Promise<Result<T>> {
  return http<T>({ method: 'DELETE', url, params, ...config })
}
