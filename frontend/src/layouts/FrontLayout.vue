<template>
  <div class="front-layout">
    <!-- 顶部导航栏：严格对照 index.html .mock-navbar (第755-764行) -->
    <header class="mock-navbar">
      <!-- Logo：Seckill(红) + Mall(橙) -->
      <div class="mock-logo" @click="router.push('/')">Seckill<span>Mall</span></div>

      <!-- 搜索框：居中绝对定位 + 红色边框 + 红色按钮 -->
      <div class="mock-search">
        <input v-model="searchKeyword" class="mock-search-input" type="text" placeholder="搜索商品、品牌..."
          @input="debouncedSearch" @keyup.enter="handleSearch" />
        <button class="mock-search-btn" @click="handleSearch">搜索</button>
      </div>

      <!-- 导航链接：秒杀专区 / 我的订单 / 购物车(带徽标) / 收藏夹 -->
      <nav class="mock-nav-links">
        <a href="javascript:void(0)" :class="{ active: route.path === '/seckill' }"
          @click="router.push('/seckill')">秒杀专区</a>
        <a href="javascript:void(0)" :class="{ active: route.path.startsWith('/user/orders') }" @click="goOrders">我的订单</a>
        <!-- 购物车链接 + 数量徽标 -->
        <a href="javascript:void(0)" class="nav-cart-link" :class="{ active: route.path === '/cart' }" @click="goCart">
          购物车
          <span v-if="cartStore.count > 0" class="cart-badge">{{ cartBadgeText }}</span>
        </a>
        <!-- 收藏夹链接 -->
        <a href="javascript:void(0)" :class="{ active: route.path === '/favorites' }" @click="goFavorites">收藏夹</a>
      </nav>

      <!-- 用户头像 + 自定义悬浮下拉菜单 -->
      <template v-if="userStore.isLoggedIn">
        <div class="avatar-wrap">
          <div class="mock-avatar">{{ avatarText }}</div>
          <!-- 头像下拉菜单：严格对照 index.html .avatar-dropdown -->
          <div class="avatar-dropdown">
            <div class="dd-header">
              <div class="dd-name">{{ displayName }}</div>
              <div class="dd-role">{{ roleLabel }}</div>
            </div>
            <div class="dd-item" @click="router.push('/user/profile')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              个人中心
            </div>
            <div class="dd-item" @click="goOrders">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              我的订单
            </div>
            <div class="dd-item" @click="router.push('/user/address')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              收货地址
            </div>
            <div class="dd-divider"></div>
            <div class="dd-item danger" @click="handleLogout">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <path d="M16 17l5-5-5-5" />
                <path d="M21 12H9" />
              </svg>
              退出登录
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="guest-actions">
          <button class="guest-btn primary" @click="router.push('/login')">登录</button>
          <button class="guest-btn" @click="router.push('/register')">注册</button>
        </div>
      </template>
    </header>

    <!-- 主内容 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="default">
          <keep-alive :include="cachedViewNames">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </transition>
      </router-view>
    </main>

    <!-- 页脚：严格对照 index.html .mock-footer -->
    <footer class="mock-footer">
      SeckillMall 秒杀商城 — 正品秒杀，手快有手慢无
    </footer>
  </div>
</template>

<script setup lang="ts">
/**
 * 前台布局 - 严格对照 index.html 第755-764行 mock-navbar 结构 + 第151-238行 CSS
 */
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchKeyword = ref('')

/**
 * keep-alive 缓存的组件名列表.
 * 缓存前台高频页面以减少切换卡顿与重复请求:
 *   首页(Home) / 商品列表(ProductList) / 购物车(Cart) / 收藏夹(Favorites) / 个人中心(UserProfile) / 钱包(Wallet) / 我的优惠券(MyCoupons)
 * 注意: 被缓存组件需通过 defineOptions({ name: 'XXX' }) 显式声明组件名, 否则 keep-alive 无法匹配.
 *   - Home.vue / ProductList.vue 已显式声明组件名
 *   - 各组件在 onActivated 中按需判断是否刷新数据
 * 未登录时不缓存需鉴权页面 (避免缓存重定向前的空实例).
 */
const cachedViewNames = computed<string[]>(() => {
  if (!userStore.isLoggedIn) return []
  return ['Home', 'ProductList', 'Cart', 'Favorites', 'UserProfile', 'Wallet', 'MyCoupons']
})

/** 购物车数量徽标文本：超过 99 显示 99+ */
const cartBadgeText = computed<string>(() => {
  const n = cartStore.count
  return n > 99 ? '99+' : String(n)
})

/** 头像文字：取用户名首字 */
const avatarText = computed<string>(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase() || 'U'
})

/** 下拉头部显示名 */
const displayName = computed<string>(() => {
  const u = userStore.userInfo
  if (!u) return ''
  const name = u.nickname || u.username
  return `${name} (${u.username})`
})

/** 角色标签 */
const roleLabel = computed<string>(() => {
  const roleMap: Record<string, string> = {
    BUYER: '普通买家',
    SELLER: '卖家',
    ADMIN: '管理员'
  }
  return roleMap[userStore.userInfo?.role || ''] || '用户'
})

/** 跳转订单 */
function goOrders(): void {
  router.push('/user/orders')
}

/** 跳转购物车(未登录跳登录页) */
function goCart(): void {
  if (!userStore.isLoggedIn) {
    router.push('/login?redirect=' + encodeURIComponent('/cart'))
    return
  }
  router.push('/cart')
}

/** 跳转收藏夹(未登录跳登录页) */
function goFavorites(): void {
  if (!userStore.isLoggedIn) {
    router.push('/login?redirect=' + encodeURIComponent('/favorites'))
    return
  }
  router.push('/favorites')
}

/** 登录态变化时拉取/重置购物车数量 */
watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      // 登录后拉取购物车数量(用于徽标)
      cartStore.fetchCount()
    } else {
      cartStore.reset()
    }
  },
  { immediate: true }
)

/**
 * 购物车数量刷新说明:
 *   原实现 watch route.path 每次路由变化都调用 cartStore.fetchCount(),
 *   导致频繁切换页面时产生大量冗余请求. 现改为仅在关键操作后手动刷新:
 *     - 用户登录时 (上方 watch isLoggedIn 已处理)
 *     - 添加商品到购物车后 (Cart.vue / ProductDetail.vue 加购操作后调用 cartStore.increment / fetchCount)
 *     - 购物车页面操作后 (Cart.vue 内部已调用 cartStore.fetchCount)
 */

/** 搜索 (立即触发, 用于回车和按钮点击) */
function handleSearch(): void {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: searchKeyword.value.trim() } })
  } else {
    router.push('/products')
  }
}

/**
 * 防抖搜索 (300ms): 用于 input 事件, 避免频繁触发搜索请求.
 * 仅在输入非空且长度 >= 2 时触发, 减少单字符搜索请求.
 * 自定义实现, 避免引入 lodash 增加包体积.
 */
let searchTimer: ReturnType<typeof setTimeout> | null = null
function debouncedSearch(): void {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const kw = searchKeyword.value.trim()
    // 空输入跳转商品列表, 非空且长度>=2才触发搜索
    if (kw && kw.length >= 2) {
      router.push({ path: '/products', query: { keyword: kw } })
    } else if (!kw) {
      router.push('/products')
    }
  }, 300)
}

/** 退出登录 */
async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '退出确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 取消
  }
}

</script>

<style scoped>
/* === 布局骨架 === */
.front-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

/* === 导航栏：严格对照 index.html .mock-navbar === */
.mock-navbar {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 20px;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-sizing: border-box;
}

/* Logo：Seckill(红) + Mall(橙) */
.mock-logo {
  font-size: 18px;
  font-weight: 800;
  color: #e53935;
  letter-spacing: -0.02em;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.mock-logo span {
  color: #ff6d00;
}

/* 搜索框：居中绝对定位 + 红色2px边框 */
.mock-search {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 400px;
  max-width: 40%;
  height: 36px;
  border: 2px solid #e53935;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 0 12px;
  font-size: 14px;
  color: #6b7280;
  background: #ffffff;
  box-sizing: border-box;
  overflow: hidden;
}

.mock-search-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  color: #1a1a2e;
  background: transparent;
  padding: 0;
}

.mock-search-input::placeholder {
  color: #6b7280;
  font-size: 14px;
}

/* 红色搜索按钮 */
.mock-search-btn {
  background: #e53935;
  color: #ffffff;
  border: none;
  padding: 0 16px;
  height: 100%;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 0 2px 2px 0;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}

.mock-search-btn:hover {
  background: #c62828;
}

/* 导航链接 */
.mock-nav-links {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #1a1a2e;
  margin-left: auto;
  align-items: center;
}

.mock-nav-links a {
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: color 0.15s;
}

.mock-nav-links a:hover {
  color: #e53935;
}

/* 导航链接 active 高亮: 当前路由对应菜单显示红色 + 加粗 */
.mock-nav-links a.active {
  color: #e53935;
  font-weight: 700;
}

/* 购物车链接: 相对定位以承载徽标 */
.mock-nav-links a.nav-cart-link {
  position: relative;
  display: inline-flex;
  align-items: center;
}

/* 购物车数量徽标: 红色圆形, 显示在文字右上 */
.cart-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-left: 4px;
  background: #e53935;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 9px;
  line-height: 1;
  box-sizing: border-box;
}

/* 用户头像：32px 圆形渐变 */
.mock-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e53935, #ff6d00);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  cursor: pointer;
}

/* === 头像下拉菜单：严格对照 index.html .avatar-dropdown === */
.avatar-wrap {
  position: relative;
  margin-left: 16px;
  flex-shrink: 0;
}

.avatar-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 180px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 6px 0;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-4px);
  transition: all 0.2s;
  z-index: 100;
}

.avatar-wrap:hover .avatar-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dd-header {
  padding: 10px 14px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 4px;
}

.dd-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
}

.dd-role {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

.dd-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 14px;
  color: #1a1a2e;
  cursor: pointer;
  transition: background 0.15s;
}

.dd-item:hover {
  background: #f8f8f8;
}

.dd-item svg {
  width: 16px;
  height: 16px;
  color: #6b7280;
  flex-shrink: 0;
}

.dd-item.danger {
  color: #f44336;
}

.dd-item.danger svg {
  color: #f44336;
}

.dd-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 4px 0;
}

/* 未登录操作 */
.guest-actions {
  display: flex;
  gap: 8px;
  margin-left: 16px;
  flex-shrink: 0;
}

.guest-btn {
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #1a1a2e;
  transition: all 0.15s;
}

.guest-btn:hover {
  border-color: #e53935;
  color: #e53935;
}

.guest-btn.primary {
  background: #e53935;
  border-color: #e53935;
  color: #ffffff;
}

.guest-btn.primary:hover {
  background: #c62828;
  border-color: #c62828;
  color: #ffffff;
}

/* === 主内容 === */
.main-content {
  flex: 1;
  width: 100%;
}

/* === 页脚：严格对照 index.html .mock-footer === */
.mock-footer {
  background: #2d2d3a;
  color: #9ca3af;
  padding: 24px;
  text-align: center;
  font-size: 12px;
  margin-top: auto;
}

/* 路由过渡: 缩短到 0.15s 减少白屏感, 使用 will-change 提示 GPU 加速 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
  will-change: opacity;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
