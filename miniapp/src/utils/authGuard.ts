/**
 * 路由守卫（对齐 spec.md 3.4/3.6/3.7/3.8/3.9/3.10/3.11 鉴权页 + tasks.md T1.5）
 *
 * 用法：在需要鉴权的页面 onLoad / onShow 中调用 requireAuth()，
 *      返回 false 表示未登录并已跳转登录页（携带 redirect），页面应中止后续逻辑。
 *
 * 示例：
 *   import { requireAuth } from '@/utils/authGuard'
 *   onLoad(() => {
 *     if (!requireAuth()) return
 *     // 已登录，继续加载页面数据
 *   })
 *
 * 说明：
 *   - uni-app 无全局路由拦截器（不像 Vue Router beforeEach），
 *     采用"页面级守卫"模式，在需要鉴权的页面 onLoad/onShow 主动调用 requireAuth。
 *   - 未登录时跳转登录页并携带 redirect 参数（当前页路径），
 *     登录成功后由 login.vue 读取 redirect 跳回。
 *   - isLoggedIn 基于 tokenStorage.hasToken()，启动时由 App.vue 调用
 *     userStore.restoreFromStorage() 校验 Token 有效性。
 */

import { useUserStore } from '@/stores/user'
import { navigate } from '@/utils/navigate'

/**
 * 获取当前页面路径（不含查询参数）
 * 兼容主包与分包页面
 */
function getCurrentPagePath(): string {
  try {
    const pages = getCurrentPages()
    if (!pages || pages.length === 0) return ''
    const current = pages[pages.length - 1]
    // uni-app 页面对象：route（小程序）或 __route__（兼容）
    const route = (current as any).route || (current as any).__route__ || ''
    return route
  } catch (e) {
    console.error('获取当前页面路径失败', e)
    return ''
  }
}

/**
 * 鉴权守卫：检查登录态，未登录则跳转登录页
 *
 * @param redirect 自定义重定向路径，默认取当前页路径
 * @returns true 已登录可继续；false 未登录已跳转登录页（调用方应中止后续逻辑）
 */
export function requireAuth(redirect?: string): boolean {
  const userStore = useUserStore()

  if (userStore.isLoggedIn) {
    return true
  }

  // 未登录，跳转登录页
  const redirectPath = redirect || getCurrentPagePath()
  navigate.toLogin(redirectPath)
  return false
}

/**
 * 异步鉴权守卫：先尝试从本地存储恢复登录态，再校验
 *
 * 适用场景：页面 onShow 时 Token 可能尚未恢复（冷启动）
 *
 * @param redirect 自定义重定向路径
 * @returns true 已登录；false 未登录已跳转登录页
 */
export function requireAuthAsync(redirect?: string): boolean {
  const userStore = useUserStore()

  // 若 isLoggedIn 为 false 但本地有有效 Token，尝试恢复
  if (!userStore.isLoggedIn) {
    userStore.restoreFromStorage()
  }

  return requireAuth(redirect)
}

/**
 * 登录态断言：仅校验不跳转
 *
 * 适用场景：操作前检查登录态（如点击"加入购物车"），未登录则手动跳转
 *
 * @returns true 已登录；false 未登录
 */
export function isLoggedIn(): boolean {
  const userStore = useUserStore()
  return userStore.isLoggedIn
}

/**
 * 未登录时跳转登录页（携带 redirect）
 *
 * @param redirect 自定义重定向路径，默认取当前页路径
 */
export function redirectToLogin(redirect?: string): void {
  const redirectPath = redirect || getCurrentPagePath()
  navigate.toLogin(redirectPath)
}