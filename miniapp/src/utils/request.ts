/**
 * 请求核心封装（对齐 plan.md 第 4.1 节 / spec.md 第 2 章）
 * 基于 uni.request，对齐 Web 端 Axios 拦截器全部逻辑：
 * 1. 请求拦截：自动添加 Authorization: Bearer <token>
 * 2. 响应拦截：解包 Result<T>，同步服务器时间
 * 3. 业务码校验：code !== 200 报错 reject
 * 4. HTTP 401 处理：区分 1002（Token 过期，触发刷新）与 1011（防重放，不刷新）
 * 5. Token 刷新：并发锁 isRefreshing + 等待队列 pendingQueue（对齐 Web 端 H-F2 修复逻辑）
 * 6. HTTP 403/429/5xx 处理
 */

import { ENV } from '@/utils/env'
import { tokenStorage } from '@/utils/tokenStorage'
import { syncServerTime } from '@/utils/timeSync'
import { showToast } from '@/utils/toast'
import { navigate } from '@/utils/navigate'
import type { Result } from '@/types'

// ============ 刷新锁与等待队列（模块级变量，确保全局唯一） ============
let isRefreshing = false
// 等待队列：存刷新期间并发 401 请求的重试回调与 reject 引用
interface PendingItem {
  retry: () => void
  reject: (e: Error) => void
}
let pendingQueue: PendingItem[] = []

// ============ 请求选项 ============
interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  /** 是否跳过自动添加 token（如登录接口） */
  skipAuth?: boolean
  /** 是否跳过业务码校验（如特殊场景） */
  skipResultCheck?: boolean
}

// ============ 核心 request 函数 ============
/**
 * 发起请求并解包 Result<T>.data
 * @returns Promise<T>，T 为业务数据类型
 */
export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const accessToken = tokenStorage.getAccessToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.header
    }
    // 请求拦截：自动添加 Authorization
    if (!options.skipAuth && accessToken) {
      header['Authorization'] = `Bearer ${accessToken}`
    }

    uni.request({
      url: `${ENV.API_BASE_URL}${ENV.API_PREFIX}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: ENV.TIMEOUT,
      success: (res) => {
        const statusCode = res.statusCode
        const resData = res.data as Result<T>

        // 响应拦截：同步服务器时间
        if (resData?.timestamp) {
          syncServerTime(resData.timestamp)
        }

        // ===== HTTP 401 处理 =====
        if (statusCode === 401) {
          const code = resData?.code
          // Token 过期（1002）→ 触发刷新
          if (code === 1002 && !options.skipAuth) {
            // 当前请求入队等待刷新完成
            pendingQueue.push({
              retry: () => {
                request<T>(options).then(resolve).catch(reject)
              },
              reject
            })

            if (!isRefreshing) {
              isRefreshing = true
              // 动态导入避免循环依赖（request → user → auth → request）
              import('@/stores/user').then(({ useUserStore }) => {
                const userStore = useUserStore()
                userStore.refreshTokenAction()
                  .then(() => {
                    // 刷新成功，重试队列
                    isRefreshing = false
                    const queue = pendingQueue
                    pendingQueue = []
                    queue.forEach(item => item.retry())
                  })
                  .catch((err) => {
                    // 刷新失败，reject 队列所有请求，清空 token，跳转登录
                    isRefreshing = false
                    const queue = pendingQueue
                    pendingQueue = []
                    const error = new Error('登录已过期，请重新登录')
                    queue.forEach(item => item.reject(error))
                    userStore.clearAuth()
                    navigate.toLogin()
                  })
              }).catch(() => {
                isRefreshing = false
                const queue = pendingQueue
                pendingQueue = []
                const error = new Error('登录状态异常')
                queue.forEach(item => item.reject(error))
              })
            }
            return
          }
          // 防重放拦截（1011）→ 不刷新，提示用户重新发起秒杀
          if (code === 1011) {
            showToast('操作已过期，请重新发起秒杀', 'none')
            reject(new Error(resData?.message || '防重放拦截'))
            return
          }
          // 其他 401 → 业务拒绝（不触发刷新）
          showToast(resData?.message || '无权限', 'none')
          reject(new Error(resData?.message || '无权限'))
          return
        }

        // ===== HTTP 403 =====
        if (statusCode === 403) {
          showToast('无权限访问', 'none')
          reject(new Error('403 Forbidden'))
          return
        }

        // ===== HTTP 429 =====
        if (statusCode === 429) {
          showToast('请求太频繁，请稍后再试', 'none')
          reject(new Error('429 Too Many Requests'))
          return
        }

        // ===== HTTP 5xx =====
        if (statusCode >= 500) {
          showToast('服务器异常，请稍后再试', 'none')
          reject(new Error('服务器异常'))
          return
        }

        // ===== 业务码校验 =====
        if (!options.skipResultCheck && resData?.code !== 200) {
          showToast(resData?.message || '请求失败', 'none')
          reject(new Error(resData?.message || '请求失败'))
          return
        }

        // ===== 成功，返回 data =====
        resolve(resData?.data)
      },
      fail: (err) => {
        showToast('网络异常，请检查网络连接', 'none')
        reject(err)
      }
    })
  })
}

// ============ 便捷方法 ============

/** GET 请求 */
export function get<T = any>(
  url: string,
  params?: Record<string, any>,
  options?: Partial<RequestOptions>
): Promise<T> {
  // GET 请求参数拼接到 URL
  let finalUrl = url
  if (params) {
    const query = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
    if (query) {
      finalUrl = `${url}${url.includes('?') ? '&' : '?'}${query}`
    }
  }
  return request<T>({ ...options, url: finalUrl, method: 'GET' })
}

/** POST 请求 */
export function post<T = any>(
  url: string,
  data?: any,
  options?: Partial<RequestOptions>
): Promise<T> {
  return request<T>({ ...options, url, method: 'POST', data })
}

/** PUT 请求 */
export function put<T = any>(
  url: string,
  data?: any,
  options?: Partial<RequestOptions>
): Promise<T> {
  return request<T>({ ...options, url, method: 'PUT', data })
}

/** DELETE 请求 */
export function del<T = any>(
  url: string,
  params?: Record<string, any>,
  options?: Partial<RequestOptions>
): Promise<T> {
  // DELETE 请求参数拼接到 URL
  let finalUrl = url
  if (params) {
    const query = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
    if (query) {
      finalUrl = `${url}${url.includes('?') ? '&' : '?'}${query}`
    }
  }
  return request<T>({ ...options, url: finalUrl, method: 'DELETE' })
}

// ============ 重试队列手动清理（测试/登出场景用） ============
export function clearPendingQueue(): void {
  pendingQueue = []
  isRefreshing = false
}