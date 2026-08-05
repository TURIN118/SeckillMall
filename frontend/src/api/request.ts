/**
 * Axios 实例配置 + 拦截器
 * 参照 10-ai-design-spec.md "API Client Layer"
 */
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result, TokenVO } from '@/types'

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

/** 刷新锁：防止并发请求时重复刷新 */
let isRefreshing = false
/** 等待刷新完成的请求队列 */
let pendingRequests: Array<(token: string) => void> = []

/** 清空 Token 并跳转登录页 */
function clearTokensAndRedirect(currentPath: string) {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  if (!currentPath.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
  }
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
  async (error) => {
    const { response, message } = error
    if (response) {
      const status = response.status
      const currentPath = window.location.pathname + window.location.search
      switch (status) {
        case 401: {
          const refreshTokenValue = localStorage.getItem(REFRESH_TOKEN_KEY)
          if (!refreshTokenValue) {
            clearTokensAndRedirect(currentPath)
            break
          }
          if (isRefreshing) {
            // 正在刷新中，将请求加入等待队列
            return new Promise((resolve) => {
              pendingRequests.push((newToken: string) => {
                error.config.headers.Authorization = `Bearer ${newToken}`
                resolve(request(error.config))
              })
            })
          }
          isRefreshing = true
          try {
            // 直接用 axios 发刷新请求，避免走拦截器循环
            const refreshRes = await axios.post<Result<TokenVO>>(
              (import.meta.env.VITE_API_BASE_URL || '') + '/api/v1/auth/refresh',
              { refreshToken: refreshTokenValue },
              { headers: { 'Content-Type': 'application/json' }, timeout: 10000 }
            )
            if (refreshRes.data?.code === 200 && refreshRes.data?.data) {
              const newAccessToken = refreshRes.data.data.accessToken
              const newRefreshToken = refreshRes.data.data.refreshToken
              localStorage.setItem(ACCESS_TOKEN_KEY, newAccessToken)
              localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)
              // 处理等待队列中的请求
              pendingRequests.forEach(cb => cb(newAccessToken))
              pendingRequests = []
              // 重试原请求
              error.config.headers.Authorization = `Bearer ${newAccessToken}`
              return request(error.config)
            } else {
              pendingRequests = []
              clearTokensAndRedirect(currentPath)
              break
            }
          } catch {
            pendingRequests = []
            clearTokensAndRedirect(currentPath)
            break
          } finally {
            isRefreshing = false
          }
        }
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
