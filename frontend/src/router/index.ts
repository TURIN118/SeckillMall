/**
 * Vue Router 配置 - 参照 10-ai-design-spec.md "Route Table"
 * 17 个页面路由 + 路由守卫
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// L-F4 修复: 路由守卫里的 ElMessage 改为动态 import, 避免 element-plus 主入口被引入
// 到路由模块的初始 chunk, 减少约 150KB 共享块体积.
// ElMessage 仅在登录失效/过期等少数场景使用, 动态 import 不影响用户体验.
async function showElMessage(type: 'warning' | 'error' | 'success', message: string): Promise<void> {
  const { ElMessage } = await import('element-plus')
  ElMessage[type](message)
}

const FrontLayout = () => import('@/layouts/FrontLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const BlankLayout = () => import('@/layouts/BlankLayout.vue')

const routes: RouteRecordRaw[] = [
  /* === 前台路由 (FrontLayout) === */
  {
    path: '/',
    component: FrontLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/front/Home.vue'),
        meta: { title: '秒杀大厅' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/front/ProductList.vue'),
        meta: { title: '商品列表' }
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/front/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {

        path: 'user/orders',
        name: 'UserOrders',
        component: () => import('@/views/front/UserOrders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'user/orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/front/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('@/views/front/UserProfile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'user/address',
        redirect: '/user/profile'
      },
      {
        path: 'user/wallet',
        redirect: '/user/profile'
      },
      {
        path: 'user/coupons',
        redirect: '/user/profile'
      },
      {
        path: 'seckill',
        name: 'SeckillZone',
        component: () => import('@/views/front/SeckillZone.vue'),
        meta: { title: '秒杀专区' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/front/Cart.vue'),
        meta: { title: '购物车', requiresAuth: true }
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/front/Checkout.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/front/Favorites.vue'),
        meta: { title: '收藏夹', requiresAuth: true }
      }
    ]
  },
  /* === 空白布局路由 (BlankLayout) — 拆分为独立顶级路由，避免与 FrontLayout 的 path:'/' 冲突 === */
  {
    path: '/login',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/front/Login.vue'),
        meta: { title: '登录' }
      }
    ]
  },
  {
    path: '/register',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Register',
        component: () => import('@/views/front/Register.vue'),
        meta: { title: '注册' }
      }
    ]
  },
  {
    path: '/forgot-password',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'ForgotPassword',
        component: () => import('@/views/front/ForgotPassword.vue'),
        meta: { title: '找回密码' }
      }
    ]
  },
  /* === 后台路由 (AdminLayout) === */
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ADMIN', 'SELLER'] },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '仪表盘', requiresAuth: true, roles: ['ADMIN', 'SELLER'] }
      },
      {
        path: 'products',
        name: 'ProductManage',
        component: () => import('@/views/admin/ProductManage.vue'),
        meta: { title: '商品管理', requiresAuth: true, roles: ['ADMIN', 'SELLER'] }
      },
      {
        path: 'products/create',
        name: 'ProductCreate',
        component: () => import('@/views/admin/ProductEdit.vue'),
        meta: { title: '新增商品', requiresAuth: true, roles: ['ADMIN', 'SELLER'] }
      },
      {
        path: 'products/edit/:id',
        name: 'ProductEdit',
        component: () => import('@/views/admin/ProductEdit.vue'),
        meta: { title: '编辑商品', requiresAuth: true, roles: ['ADMIN', 'SELLER'] }
      },
      {
        path: 'categories',
        name: 'CategoryManage',
        component: () => import('@/views/admin/CategoryManage.vue'),
        meta: { title: '分类管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'category-attribute',
        name: 'CategoryAttributeManage',
        component: () => import('@/views/admin/CategoryAttributeManage.vue'),
        meta: { title: '分类规格模板', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'seckills',
        name: 'SeckillManage',
        component: () => import('@/views/admin/SeckillManage.vue'),
        meta: { title: '秒杀管理', requiresAuth: true, roles: ['ADMIN', 'SELLER'] }
      },
      {
        path: 'orders',
        name: 'OrderManage',
        component: () => import('@/views/admin/OrderManage.vue'),
        meta: { title: '订单管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'logs',
        name: 'OperationLog',
        component: () => import('@/views/admin/OperationLog.vue'),
        meta: { title: '操作日志', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'system',
        name: 'SystemHealth',
        component: () => import('@/views/admin/SystemHealth.vue'),
        meta: { title: '系统健康', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'banners',
        name: 'BannerManage',
        component: () => import('@/views/admin/BannerManage.vue'),
        meta: { title: '轮播图管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'reviews',
        name: 'ReviewManage',
        component: () => import('@/views/admin/ReviewManage.vue'),
        meta: { title: '评论管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'dashboard-data',
        redirect: '/admin'
      },
      {
        path: 'coupons',
        name: 'CouponManage',
        component: () => import('@/views/admin/CouponManage.vue'),
        meta: { title: '优惠券管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'recharge-cards',
        name: 'RechargeCardManage',
        component: () => import('@/views/admin/RechargeCardManage.vue'),
        meta: { title: '充值卡管理', requiresAuth: true, roles: ['ADMIN'] }
      }
    ]
  },
  /* === 错误页 === */
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  }
})

/**
 * 角色归一化: 兼容后端 "ROLE_ADMIN" / "ADMIN" 两种格式
 * 后端 UserRole.getCode() 当前返回 "ADMIN"/"SELLER"/"BUYER" (不带 ROLE_ 前缀),
 * 但 SecurityUserDetails 内部使用 "ROLE_" 前缀, 为防御性兼容, 统一去除前缀再比较.
 */
function normalizeRole(role: string | undefined | null): string {
  if (!role) return ''
  return role.startsWith('ROLE_') ? role.slice(5) : role
}

/* === 路由守卫 === */
router.beforeEach(async (to, _from, next) => {
  // 设置文档标题
  const title = to.meta.title
  document.title = title ? `${title} - SeckillMall` : 'SeckillMall 秒杀商城'

  const userStore = useUserStore()

  // 已登录访问登录/注册页 -> 跳转首页
  if (['/login', '/register'].includes(to.path) && userStore.isLoggedIn) {
    next('/')
    return
  }

  // 需要鉴权但未登录 -> 跳转登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  // 已登录但用户信息为空 -> 拉取用户信息
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // H24 修复: 拉取用户信息失败 (token 无效/被踢出等)，跳转登录页而非继续放行
      // 继续放行会让用户以"半登录"状态访问页面，导致后续 API 401 雪崩式报错
      await userStore.logout()
      await showElMessage('warning', '登录信息已失效，请重新登录')
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      return
    }
  }

  // token 过期 -> 尝试刷新
  if (userStore.isLoggedIn && userStore.isTokenExpired()) {
    const refreshed = await userStore.refreshTokenAction()
    if (!refreshed) {
      await showElMessage('warning', '登录已过期，请重新登录')
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      return
    }
  }

  // 角色权限校验 (使用 normalizeRole 兼容 "ROLE_ADMIN" / "ADMIN" 两种格式)
  // M-F1 修复: 改为 fail-closed (无 userInfo 即拒绝访问), 避免未登录或用户信息缺失时放行敏感路由
  if (to.meta.roles && to.meta.roles.length > 0) {
    if (!userStore.userInfo) {
      // fail-closed: 无用户信息时不放行, 跳转 403
      // 此场景理论上已被上方 requiresAuth 校验拦截, 但防御性处理避免漏网
      next('/403')
      return
    }
    const userRole = normalizeRole(userStore.userInfo.role)
    if (!to.meta.roles.some((r) => r === userRole)) {
      next('/403')
      return
    }
  }

  // 后台管理路由预取: 当用户角色为 ADMIN/SELLER 时, 空闲时预加载后台关键 chunk
  // 在路由守卫中触发 (而非全局预取), 因为只有管理员才需要后台 chunk
  if (userStore.userInfo) {
    const userRole = normalizeRole(userStore.userInfo.role)
    if (userRole === 'ADMIN' || userRole === 'SELLER') {
      if ('requestIdleCallback' in window) {
        ; (window as unknown as { requestIdleCallback: (cb: () => void) => void }).requestIdleCallback(
          prefetchAdminRoutes
        )
      } else {
        setTimeout(prefetchAdminRoutes, 500)
      }
    }
  }

  next()
})

/* === 高频路由懒加载预取 (减少首次切换卡顿) ===
 * 在浏览器空闲时预加载前台高频页面组件,
 * 避免点击顶部菜单时新组件仍在加载导致的卡顿.
 * 分两批次预取:
 *   批次1(首屏空闲立即): 首页 + 商品列表 (最高频)
 *   批次2(延迟 3s):     商品详情 + 购物车 + 收藏夹 + 个人中心 + 秒杀专区
 * 注意: 不预取所有页面, 避免首屏加载过多 chunk; 低频页面(订单/钱包/优惠券/后台等)按需加载.
 * 批次2延迟 3s 确保首屏完全加载后再预取, 避免抢占首屏带宽.
 */
if (typeof window !== 'undefined') {
  /** 预取指定路径列表对应的路由组件 chunk (错误静默) */
  const prefetchRoutes = (paths: string[]): void => {
    paths.forEach((path) => {
      const resolved = router.resolve(path)
      resolved.matched.forEach((record) => {
        const comp = record.components?.default
        if (typeof comp === 'function') {
          // 触发动态 import, 预加载 chunk (错误静默, 不影响主流程)
          ; (comp as () => Promise<unknown>)().catch(() => { })
        }
      })
    })
  }

  /** 批次1: 首屏最高频路由 (首页 + 商品列表 + 登录/注册/找回密码)
   *  登录/注册/找回密码预取: 未登录用户点击切换时 chunk 已加载, 避免首次切换卡顿白屏. */
  const prefetchBatch1 = (): void => {
    prefetchRoutes(['/', '/products', '/login', '/register', '/forgot-password'])
  }

  /** 批次2: 次高频路由 (商品详情/购物车/收藏夹/个人中心/秒杀专区) */
  const prefetchBatch2 = (): void => {
    prefetchRoutes([
      '/products/1',    // 商品详情 (用占位 id 触发 chunk 加载, 实际访问时复用)
      '/cart',          // 购物车
      '/favorites',     // 收藏夹
      '/user/profile',  // 个人中心
      '/seckill'        // 秒杀专区
    ])
  }

  // 批次1: 立即在空闲时预取
  if ('requestIdleCallback' in window) {
    ; (window as unknown as { requestIdleCallback: (cb: () => void) => void }).requestIdleCallback(
      prefetchBatch1
    )
  } else {
    setTimeout(prefetchBatch1, 200)
  }

  // 批次2: 延迟 3000ms 后预取 (确保首屏完全加载后再预取, 避免抢占首屏带宽)
  setTimeout(() => {
    if ('requestIdleCallback' in window) {
      ; (window as unknown as { requestIdleCallback: (cb: () => void) => void }).requestIdleCallback(
        prefetchBatch2
      )
    } else {
      prefetchBatch2()
    }
  }, 3000)
}

/* === 后台管理路由预取 (仅管理员/卖家触发, 首次进入后台时减少卡顿) ===
 * 当路由守卫确认用户角色为 ADMIN/SELLER 后, 在浏览器空闲时预取后台关键页面 chunk:
 *   - AdminLayout + Dashboard (必经路径, 优先预取)
 *   - ProductManage + OrderManage (高频后台页面)
 * 使用 adminPrefetched 标志确保只触发一次, 避免重复预取.
 */
let adminPrefetched = false
function prefetchAdminRoutes(): void {
  if (adminPrefetched) return
  adminPrefetched = true
  /** 后台关键路由路径列表 */
  const adminPaths = ['/admin', '/admin/products', '/admin/orders']
  /** 预取指定路径列表对应的路由组件 chunk (错误静默) */
  adminPaths.forEach((path) => {
    const resolved = router.resolve(path)
    resolved.matched.forEach((record) => {
      const comp = record.components?.default
      if (typeof comp === 'function') {
        ; (comp as () => Promise<unknown>)().catch(() => { })
      }
    })
  })
}

export default router