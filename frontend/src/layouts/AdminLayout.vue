<template>
  <div class="admin-layout">
    <!-- 侧边栏：对照 index.html .admin-sidebar -->
    <aside class="admin-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="sidebar-logo">
        <template v-if="!appStore.sidebarCollapsed">Seckill<span>Mall</span></template>
        <template v-else>S</template>
      </div>

      <!-- 主要功能 -->
      <div class="admin-menu-group" v-if="!appStore.sidebarCollapsed">主要功能</div>
      <div class="admin-menu-item" :class="{ active: activeMenu === '/admin' }" @click="goTo('/admin')">
        <el-icon>
          <Odometer />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">仪表盘</span>
      </div>

      <!-- 业务管理 -->
      <div class="admin-menu-group" v-if="!appStore.sidebarCollapsed">业务管理</div>
      <div class="admin-menu-item" :class="{ active: activeMenu === '/admin/products' }"
        @click="goTo('/admin/products')">
        <el-icon>
          <GoodsFilled />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">商品管理</span>
      </div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/categories' }"
        @click="goTo('/admin/categories')">
        <el-icon>
          <Menu />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">分类管理</span>
      </div>
      <div class="admin-menu-item" :class="{ active: activeMenu === '/admin/seckills' }"
        @click="goTo('/admin/seckills')">
        <el-icon>
          <Timer />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">秒杀管理</span>
      </div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/orders' }"
        @click="goTo('/admin/orders')">
        <el-icon>
          <Document />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">订单管理</span>
      </div>

      <!-- 运营管理 -->
      <div class="admin-menu-group" v-if="!appStore.sidebarCollapsed">运营管理</div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/banners' }"
        @click="goTo('/admin/banners')">
        <el-icon>
          <Picture />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">轮播图管理</span>
      </div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/reviews' }"
        @click="goTo('/admin/reviews')">
        <el-icon>
          <ChatDotSquare />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">评论管理</span>
      </div>

      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/coupons' }"
        @click="goTo('/admin/coupons')">
        <el-icon>
          <Ticket />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">优惠券管理</span>
      </div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/recharge-cards' }"
        @click="goTo('/admin/recharge-cards')">
        <el-icon>
          <CreditCard />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">充值卡管理</span>
      </div>

      <!-- 系统管理 -->
      <div class="admin-menu-group" v-if="!appStore.sidebarCollapsed">系统管理</div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/users' }"
        @click="goTo('/admin/users')">
        <el-icon>
          <User />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">用户管理</span>
      </div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/logs' }"
        @click="goTo('/admin/logs')">
        <el-icon>
          <Notebook />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">操作日志</span>
      </div>
      <div v-if="userStore.isAdmin" class="admin-menu-item" :class="{ active: activeMenu === '/admin/system' }"
        @click="goTo('/admin/system')">
        <el-icon>
          <Monitor />
        </el-icon>
        <span class="menu-text" v-if="!appStore.sidebarCollapsed">系统健康</span>
      </div>
    </aside>

    <!-- 主区域：对照 index.html .admin-main -->
    <div class="admin-main">
      <!-- 顶栏：对照 index.html .admin-topbar -->
      <header class="admin-topbar">
        <div class="admin-topbar-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <div class="admin-breadcrumb">
            首页 / <strong>{{ currentTitle }}</strong>
          </div>
        </div>
        <div class="admin-topbar-right">
          <span class="topbar-username">管理员：{{ userStore.userInfo?.username || 'admin' }}</span>
          <el-dropdown trigger="hover" @command="handleUserCommand">
            <div class="mock-avatar">{{ avatarText }}</div>
            <template #dropdown>
              <el-dropdown-menu>

                <el-dropdown-item command="home">前台首页</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区：对照 index.html .admin-content -->
      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="cachedViewNames">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台布局 - 严格对照 index.html .admin-layout / .admin-sidebar / .admin-topbar 样式
 * 侧边栏使用原生 HTML + CSS 还原设计稿，不使用 el-menu
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  GoodsFilled,
  Menu,
  Timer,
  Document,
  User,
  Notebook,
  Monitor,
  Fold,
  Expand,
  Picture,
  ChatDotSquare,

  Ticket,
  CreditCard
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)

/**
 * keep-alive 缓存的组件名列表 (后台).
 * 后台管理页面通常需要每次刷新数据, 因此默认不缓存.
 * 如需缓存某页面 (如仪表盘), 在此数组添加其组件名.
 */
const cachedViewNames = computed<string[]>(() => {
  return []
})

const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase() || 'A'
})

/* === 当前页面标题（用于面包屑） === */
const currentTitle = computed(() => {
  const title = route.meta?.title as string | undefined
  return title || '仪表盘'
})

/* === 菜单跳转 === */
function goTo(path: string): void {
  router.push(path)
}

async function handleUserCommand(command: string): Promise<void> {
  switch (command) {

    case 'home':
      router.push('/')
      break
    case 'logout':
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
      break
  }
}
</script>

<style scoped>
/* === 严格对照 index.html .admin-layout === */
.admin-layout {
  display: flex;
  min-height: 100vh;
}

/* === 侧边栏：对照 .admin-sidebar === */
.admin-sidebar {
  width: 200px;
  background: var(--nav-bg);
  color: var(--nav-fg);
  padding: 16px 0;
  flex-shrink: 0;
  transition: width 0.25s ease;
  overflow-y: auto;
  position: sticky;  /* 固定侧边栏，不随内容区域滚动 */
  top: 0;
  height: 100vh;     /* 撑满视口高度 */
}

.admin-sidebar.collapsed {
  width: 64px;
}

/* === sidebar-logo：对照 .admin-sidebar .sidebar-logo === */
.sidebar-logo {
  padding: 0 16px 16px;
  font-size: 15px;
  font-weight: 800;
  color: #fff;
  border-bottom: 1px solid var(--nav-border);
  margin-bottom: 12px;
}

.sidebar-logo span {
  color: var(--color-accent);
}

/* === 菜单分组标题 === */
.admin-menu-group {
  padding: 8px 16px 4px;
  font-size: 13px;
  font-weight: 700;
  color: var(--nav-highlight);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

/* === 菜单项：对照 .admin-menu-item === */
.admin-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: var(--nav-link);
  cursor: pointer;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}

.admin-menu-item:hover {
  background: var(--nav-border);
  color: #fff;
}

.admin-menu-item.active {
  background: var(--nav-border);
  color: var(--nav-highlight);
  border-left-color: var(--nav-highlight);
  font-weight: 600;
}

.admin-menu-item .el-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  font-size: 16px;
}

.menu-text {
  white-space: nowrap;
}

/* === 主区域：对照 .admin-main === */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* === 顶栏：对照 .admin-topbar === */
.admin-topbar {
  height: 52px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.admin-topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 18px;
  cursor: pointer;
  color: var(--color-text-primary);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: var(--color-primary);
}

/* === 面包屑：对照 .admin-breadcrumb === */
.admin-breadcrumb {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.admin-breadcrumb strong {
  color: var(--color-text-primary);
  font-weight: 600;
}

/* === 顶栏右侧：对照 .admin-topbar-right === */
.admin-topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-username {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* === 头像：对照 .mock-avatar === */
.mock-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  cursor: pointer;
}

/* === 内容区：对照 .admin-content === */
.admin-content {
  padding: 20px;
  flex: 1;
  background: var(--color-bg);
  overflow-y: auto;
  font-size: 14px;
}

/* === 路由切换过渡: 缩短到 0.15s 减少白屏感, will-change 提示 GPU 加速 === */
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
