/**
 * 用户 Store - 参照 10-ai-design-spec.md "State Management / userStore"
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/api/request'
import type { UserVO, LoginRequest, UserRole } from '@/types'

export const useUserStore = defineStore('user', () => {
  /* === State === */
  const accessToken = ref<string | null>(localStorage.getItem(ACCESS_TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_TOKEN_KEY))
  const userInfo = ref<UserVO | null>(null)

  /* === Getters === */
  const isLoggedIn = computed<boolean>(() => !!accessToken.value)
  const isAdmin = computed<boolean>(() => userInfo.value?.role === 'ADMIN')
  const isSeller = computed<boolean>(() => userInfo.value?.role === 'SELLER')
  const isBuyer = computed<boolean>(() => userInfo.value?.role === 'BUYER')
  const userRole = computed<UserRole | ''>(() => userInfo.value?.role || '')

  /** 判断是否拥有指定角色之一 */
  function hasRole(roles: UserRole[]): boolean {
    return !!userInfo.value && roles.includes(userInfo.value.role)
  }

  /* === Actions === */

  /** 登录 */
  async function login(data: LoginRequest): Promise<void> {
    const res = await authApi.login(data)
    accessToken.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    userInfo.value = res.data.user
    localStorage.setItem(ACCESS_TOKEN_KEY, res.data.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, res.data.refreshToken)
  }

  /** 退出登录 */
  async function logout(): Promise<void> {
    // fire-and-forget, 不阻塞
    try {
      if (accessToken.value) {
        await authApi.logout()
      }
    } catch {
      // 忽略退出接口错误
    } finally {
      accessToken.value = null
      refreshToken.value = null
      userInfo.value = null
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    }
  }

  /** 获取当前用户信息 */
  async function fetchUserInfo(): Promise<void> {
    const res = await authApi.getMe()
    userInfo.value = res.data
  }

  /** 刷新令牌, 成功返回 true */
  async function refreshTokenAction(): Promise<boolean> {
    if (!refreshToken.value) return false
    try {
      const res = await authApi.refreshToken({ refreshToken: refreshToken.value })
      accessToken.value = res.data.accessToken
      refreshToken.value = res.data.refreshToken
      localStorage.setItem(ACCESS_TOKEN_KEY, res.data.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, res.data.refreshToken)
      return true
    } catch {
      await logout()
      return false
    }
  }

  /** 检查 token 是否过期 (提前 5 分钟视为过期) */
  function isTokenExpired(): boolean {
    if (!accessToken.value) return true
    try {
      const parts = accessToken.value.split('.')
      if (parts.length !== 3) return true
      const payload = JSON.parse(atob(parts[1]))
      if (!payload.exp) return true
      // 提前 5 分钟过期缓冲
      return payload.exp * 1000 - 5 * 60 * 1000 < Date.now()
    } catch {
      return true
    }
  }

  /** 从 localStorage 恢复状态 (应用初始化时调用) */
  function restoreFromStorage(): void {
    accessToken.value = localStorage.getItem(ACCESS_TOKEN_KEY)
    refreshToken.value = localStorage.getItem(REFRESH_TOKEN_KEY)
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    isAdmin,
    isSeller,
    isBuyer,
    userRole,
    hasRole,
    login,
    logout,
    fetchUserInfo,
    refreshTokenAction,
    isTokenExpired,
    restoreFromStorage
  }
})