<!--
  个人中心页（对齐 spec.md 3.10 个人中心 / tasks.md T5.1）
  - 顶部用户信息卡片：头像 / 昵称 / 角色标签；未登录显示"请登录"按钮
  - 功能入口列表（u-cell-group）：订单 / 地址 / 收藏 / 优惠券 / 钱包 / 修改资料 / 修改密码 / 退出登录
  - authGuard 检查登录态；onShow 时调用 userStore.fetchUserInfo() 刷新
-->
<template>
  <view class="profile-page">
    <!-- 顶部用户信息卡片 -->
    <view class="user-card">
      <view class="user-info" @tap="handleAvatarClick">
        <u-image
          :src="avatarUrl"
          mode="aspectFill"
          width="120rpx"
          height="120rpx"
          shape="circle"
          :lazy-load="true"
        />
        <view class="user-meta">
          <view class="nickname-row">
            <text class="nickname">{{ displayName }}</text>
            <u-tag
              v-if="isLoggedIn && userInfo"
              :text="roleTagText"
              :type="roleTagType"
              size="mini"
              plain
            />
          </view>
          <text class="sub-text">{{ subText }}</text>
        </view>
        <view v-if="!isLoggedIn" class="login-btn">
          <u-button type="error" size="mini" shape="circle" @click="goLogin">请登录</u-button>
        </view>
      </view>
    </view>

    <!-- 功能入口列表 -->
    <view class="menu-group">
      <u-cell-group :border="true">
        <u-cell
          title="我的订单"
          icon="order"
          isLink
          @click="goOrderList"
        />
        <u-cell
          title="收货地址"
          icon="map"
          isLink
          @click="goAddressList"
        />
        <u-cell
          title="我的收藏"
          icon="heart"
          isLink
          @click="goFavorites"
        />
        <u-cell
          title="我的优惠券"
          icon="coupon"
          isLink
          @click="goCoupons"
        />
        <u-cell
          title="我的钱包"
          icon="rmb"
          isLink
          @click="goWallet"
        />
      </u-cell-group>
    </view>

    <view class="menu-group">
      <u-cell-group :border="true">
        <u-cell
          title="修改资料"
          icon="edit-pen"
          isLink
          @click="goEditProfile"
        />
        <u-cell
          title="修改密码"
          icon="lock"
          isLink
          @click="goEditPassword"
        />
        <u-cell
          v-if="isLoggedIn"
          title="退出登录"
          icon="close"
          title-style="color: #ff4d4f;"
          @click="handleLogout"
        />
      </u-cell-group>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { requireAuthAsync, redirectToLogin } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showConfirm, showToast } from '@/utils/toast'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

/** 头像 URL（未登录用占位图） */
const avatarUrl = computed(() => {
  if (isLoggedIn.value && userInfo.value?.avatar) {
    return userInfo.value.avatar
  }
  // 默认头像（static 目录下需放置 default-avatar.png，缺失时 u-image 显示 error 占位）
  return '/static/default-avatar.png'
})

/** 显示名称 */
const displayName = computed(() => {
  if (!isLoggedIn.value) return '未登录'
  return userInfo.value?.nickname || userInfo.value?.username || '用户'
})

/** 副文本（邮箱或手机号） */
const subText = computed(() => {
  if (!isLoggedIn.value) return '登录后享受更多服务'
  const info = userInfo.value
  if (!info) return ''
  return info.email || info.phone || `用户ID: ${info.id}`
})

/** 角色标签文本 */
const roleTagText = computed(() => {
  // 简化：根据 username 推断（实际可由后端返回 role 字段）
  return '普通用户'
})

/** 角色标签类型 */
const roleTagType = computed<'primary' | 'success' | 'warning' | 'error'>(() => {
  return 'primary'
})

/** onShow：尝试恢复登录态并刷新用户信息 */
onShow(() => {
  // 冷启动后 Token 可能尚未恢复，先尝试恢复
  if (!userStore.isLoggedIn) {
    userStore.restoreFromStorage()
  }
  // 已登录则拉取最新用户信息（失败不阻塞页面）
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo().catch((e) => {
      console.error('拉取用户信息失败', e)
    })
  }
})

/** 头像点击：已登录跳转修改资料，未登录跳转登录 */
function handleAvatarClick() {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  goEditProfile()
}

/** 跳转登录页 */
function goLogin() {
  redirectToLogin('pages/profile/profile')
}

/** 跳转订单列表 */
function goOrderList() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-order/pages/order-list/order-list')
}

/** 跳转地址管理 */
function goAddressList() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-user/pages/address-list/address-list')
}

/** 跳转收藏夹 */
function goFavorites() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-user/pages/favorites/favorites')
}

/** 跳转优惠券 */
function goCoupons() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-user/pages/my-coupons/my-coupons')
}

/** 跳转钱包 */
function goWallet() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-user/pages/wallet/wallet')
}

/** 跳转修改资料 */
function goEditProfile() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-user/pages/user-profile/user-profile')
}

/** 跳转修改密码 */
function goEditPassword() {
  if (!requireAuthAsync('pages/profile/profile')) return
  navigate.to('pages-user/pages/edit-password/edit-password')
}

/** 退出登录 */
async function handleLogout() {
  const confirmed = await showConfirm('确定要退出登录吗？', '退出登录')
  if (!confirmed) return
  try {
    await userStore.logout()
    showToast('已退出登录', 'success')
  } catch (e) {
    console.error('退出登录失败', e)
    showToast('退出失败，请重试', 'error')
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}

/* 用户信息卡片 */
.user-card {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%);
  padding: 60rpx 32rpx 48rpx;

  .user-info {
    display: flex;
    align-items: center;
    gap: 24rpx;

    .user-meta {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8rpx;

      .nickname-row {
        display: flex;
        align-items: center;
        gap: 12rpx;

        .nickname {
          font-size: 36rpx;
          font-weight: bold;
          color: #ffffff;
        }
      }

      .sub-text {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.85);
      }
    }

    .login-btn {
      flex-shrink: 0;
    }
  }
}

/* 菜单分组 */
.menu-group {
  margin-top: 24rpx;
  background-color: #ffffff;
}
</style>
