/**
 * Token 存储封装（对齐 plan.md 第 4.2 节）
 * 基于 uni.setStorageSync / uni.getStorageSync / uni.removeStorageSync
 * 键名与 Web 端保持一致：access_token / refresh_token
 */

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export const tokenStorage = {
  /** 获取 access_token */
  getAccessToken(): string | null {
    try {
      return uni.getStorageSync(ACCESS_TOKEN_KEY) || null
    } catch (e) {
      console.error('读取 access_token 失败', e)
      return null
    }
  },

  /** 设置 access_token */
  setAccessToken(token: string): void {
    try {
      uni.setStorageSync(ACCESS_TOKEN_KEY, token)
    } catch (e) {
      console.error('保存 access_token 失败', e)
    }
  },

  /** 获取 refresh_token */
  getRefreshToken(): string | null {
    try {
      return uni.getStorageSync(REFRESH_TOKEN_KEY) || null
    } catch (e) {
      console.error('读取 refresh_token 失败', e)
      return null
    }
  },

  /** 设置 refresh_token */
  setRefreshToken(token: string): void {
    try {
      uni.setStorageSync(REFRESH_TOKEN_KEY, token)
    } catch (e) {
      console.error('保存 refresh_token 失败', e)
    }
  },

  /** 清空所有 Token */
  clearAll(): void {
    try {
      uni.removeStorageSync(ACCESS_TOKEN_KEY)
      uni.removeStorageSync(REFRESH_TOKEN_KEY)
    } catch (e) {
      console.error('清空 Token 失败', e)
    }
  },

  /** 是否存在有效 Token */
  hasToken(): boolean {
    return !!this.getAccessToken()
  }
}