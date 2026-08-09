/**
 * 路由跳转封装（对齐 plan.md 第 4.9 节）
 * 自动区分 tabBar 与非 tabBar 页面
 * tabBar 用 uni.switchTab，非 tabBar 用 uni.navigateTo
 */

// tabBar 页面路径集合（对齐 pages.json tabBar.list）
const TAB_BAR_PAGES = [
  'pages/home/home',
  'pages/category/category',
  'pages/cart/cart',
  'pages/profile/profile'
]

/** 判断是否 tabBar 页面 */
function isTabBarPage(path: string): boolean {
  return TAB_BAR_PAGES.some(p => path.startsWith(p))
}

/** 构建查询字符串 */
function buildQueryString(params: Record<string, any>): string {
  return Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
}

export const navigate = {
  /**
   * 跳转（自动区分 tabBar 与非 tabBar）
   * tabBar 用 uni.switchTab，非 tabBar 用 uni.navigateTo
   */
  to(path: string, params?: Record<string, any>): void {
    const queryString = params ? buildQueryString(params) : ''
    const fullPath = `${path}${queryString ? `?${queryString}` : ''}`

    if (isTabBarPage(path)) {
      uni.switchTab({ url: `/${fullPath}` })
    } else {
      uni.navigateTo({ url: `/${fullPath}` })
    }
  },

  /** 重定向 */
  redirect(path: string, params?: Record<string, any>): void {
    const queryString = params ? buildQueryString(params) : ''
    const fullPath = `${path}${queryString ? `?${queryString}` : ''}`
    uni.redirectTo({ url: `/${fullPath}` })
  },

  /** 返回 */
  back(delta = 1): void {
    uni.navigateBack({ delta })
  },

  /** 跳转登录页（携带 redirect 参数） */
  toLogin(redirect?: string): void {
    const params = redirect ? { redirect } : undefined
    this.to('pages/login/login', params)
  }
}