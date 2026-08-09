/**
 * 用户 Store（对齐 plan.md 第 5.3 节 / Web 端 stores/user.ts）
 * 职责：token / userInfo / login / logout / refreshTokenAction / clearAuth / restoreFromStorage
 * 持久化：token 通过 tokenStorage 持久化，userInfo 不持久化（每次启动拉取）
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tokenStorage } from '@/utils/tokenStorage'
import { isTokenExpired } from '@/utils/jwt'
import * as authApi from '@/api/auth'
import type { UserVO, LoginRequest } from '@/types'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserVO | null>(null)
  const isLoggedIn = ref<boolean>(tokenStorage.hasToken())

  /** 登录 */
  async function login(loginForm: LoginRequest) {
    const res = await authApi.login(loginForm)
    tokenStorage.setAccessToken(res.accessToken)
    tokenStorage.setRefreshToken(res.refreshToken)
    isLoggedIn.value = true
    await fetchUserInfo()
    return res
  }

  /** 拉取用户信息 */
  async function fetchUserInfo() {
    const res = await authApi.me()
    userInfo.value = res
    return res
  }

  /** 刷新 token（由 request.ts 401 拦截器调用） */
  async function refreshTokenAction() {
    const refreshToken = tokenStorage.getRefreshToken()
    if (!refreshToken) {
      throw new Error('refresh_token 不存在')
    }
    const res = await authApi.refresh({ refreshToken })
    tokenStorage.setAccessToken(res.accessToken)
    tokenStorage.setRefreshToken(res.refreshToken)
    return res
  }

  /** 清空认证信息 */
  function clearAuth() {
    tokenStorage.clearAll()
    userInfo.value = null
    isLoggedIn.value = false
  }

  /** 登出 */
  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      console.error('登出接口调用失败', e)
    } finally {
      clearAuth()
    }
  }

  /** 启动时恢复登录态（检查 Token 有效性） */
  function restoreFromStorage() {
    const accessToken = tokenStorage.getAccessToken()
    if (accessToken && !isTokenExpired(accessToken)) {
      isLoggedIn.value = true
      fetchUserInfo().catch(() => {
        clearAuth()
      })
    } else {
      // Token 不存在或已过期，清理
      if (accessToken) {
        clearAuth()
      }
    }
  }

  /** 判断当前 Token 是否过期 */
  function isCurrentTokenExpired(): boolean {
    const accessToken = tokenStorage.getAccessToken()
    if (!accessToken) return true
    return isTokenExpired(accessToken)
  }

  return {
    userInfo,
    isLoggedIn,
    login,
    fetchUserInfo,
    refreshTokenAction,
    clearAuth,
    logout,
    restoreFromStorage,
    isCurrentTokenExpired
  }
})