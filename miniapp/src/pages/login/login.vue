<!--
  登录页（对齐 spec.md 3.12）
  - 账号/邮箱/手机号输入
  - 密码输入（type="password"）
  - 图形验证码（CaptchaImage 组件）
  - 记住我（u-checkbox + uni.setStorageSync 存账号）
  - 登录提交：userStore.login()，成功后根据 redirect 跳转或 switchTab 首页
  - 底部"去注册""忘记密码"链接
-->
<template>
  <view class="login-page">
    <!-- 顶部 Logo / 标题 -->
    <view class="login-header">
      <view class="logo-box">
        <text class="logo-text">秒杀商城</text>
      </view>
      <text class="welcome-text">欢迎登录</text>
    </view>

    <!-- 登录表单 -->
    <view class="login-form">
      <!-- 账号 -->
      <view class="form-item">
        <view class="item-label">账号</view>
        <u-input
          v-model="form.account"
          placeholder="请输入账号/邮箱/手机号"
          border="surround"
          clearable
          :maxlength="50"
          adjust-position
        />
      </view>

      <!-- 密码 -->
      <view class="form-item">
        <view class="item-label">密码</view>
        <u-input
          v-model="form.password"
          placeholder="请输入密码"
          border="surround"
          :password="true"
          clearable
          :maxlength="32"
          adjust-position
        />
      </view>

      <!-- 图形验证码 -->
      <view class="form-item">
        <view class="item-label">图形验证码</view>
        <view class="captcha-row">
          <u-input
            v-model="form.captchaCode"
            placeholder="请输入验证码"
            border="surround"
            clearable
            :maxlength="6"
            adjust-position
          />
          <captcha-image
            class="captcha-img"
            width="200rpx"
            height="80rpx"
            @update:captcha-key="onCaptchaKeyUpdate"
          />
        </view>
      </view>

      <!-- 记住我 + 忘记密码 -->
      <view class="form-options">
        <view class="remember-me" @tap="toggleRemember">
          <u-checkbox-group v-model="rememberChecked">
            <u-checkbox shape="circle" name="remember" />
          </u-checkbox-group>
          <text class="remember-text">记住我</text>
        </view>
        <text class="forgot-link" @tap="goForgotPassword">忘记密码？</text>
      </view>

      <!-- 登录按钮 -->
      <view class="form-actions">
        <u-button
          type="error"
          shape="circle"
          text="登 录"
          :loading="loading"
          :custom-style="btnStyle"
          @click="handleLogin"
        />
      </view>

      <!-- 底部注册链接 -->
      <view class="form-footer">
        <text class="footer-text">还没有账号？</text>
        <text class="register-link" @tap="goRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { navigate } from '@/utils/navigate'
import { showToast } from '@/utils/toast'
import CaptchaImage from '@/components/CaptchaImage.vue'

// ============ Store ============
const userStore = useUserStore()

// ============ 记住我存储键 ============
const REMEMBER_ACCOUNT_KEY = 'remember_account'

// ============ 表单数据 ============
const form = reactive({
  account: '',
  password: '',
  captchaCode: '',
  captchaKey: ''
})

const rememberChecked = ref<string[]>([])
const loading = ref<boolean>(false)
const redirectUrl = ref<string>('')

// ============ 按钮样式 ============
const btnStyle = {
  width: '100%',
  height: '88rpx',
  fontSize: '32rpx'
}

// ============ 验证码 key 同步 ============
function onCaptchaKeyUpdate(key: string): void {
  form.captchaKey = key
}

// ============ 记住我切换 ============
function toggleRemember(): void {
  if (rememberChecked.value.length > 0) {
    rememberChecked.value = []
  } else {
    rememberChecked.value = ['remember']
  }
}

function isRememberChecked(): boolean {
  return rememberChecked.value.includes('remember')
}

// ============ 表单校验 ============
function validateForm(): boolean {
  if (!form.account.trim()) {
    showToast('请输入账号', 'none')
    return false
  }
  if (!form.password) {
    showToast('请输入密码', 'none')
    return false
  }
  if (form.password.length < 6) {
    showToast('密码长度不能少于6位', 'none')
    return false
  }
  if (!form.captchaCode.trim()) {
    showToast('请输入图形验证码', 'none')
    return false
  }
  if (!form.captchaKey) {
    showToast('验证码未就绪，请点击验证码图片刷新', 'none')
    return false
  }
  return true
}

// ============ 保存/读取记住的账号 ============
function saveRememberedAccount(): void {
  if (isRememberChecked()) {
    uni.setStorageSync(REMEMBER_ACCOUNT_KEY, {
      account: form.account,
      remember: true
    })
  } else {
    uni.removeStorageSync(REMEMBER_ACCOUNT_KEY)
  }
}

function loadRememberedAccount(): void {
  try {
    const saved = uni.getStorageSync(REMEMBER_ACCOUNT_KEY)
    if (saved && saved.remember) {
      form.account = saved.account || ''
      rememberChecked.value = ['remember']
    }
  } catch (e) {
    console.error('读取记住账号失败', e)
  }
}

// ============ 登录提交 ============
async function handleLogin(): Promise<void> {
  if (loading.value) return
  if (!validateForm()) return

  loading.value = true
  try {
    await userStore.login({
      account: form.account.trim(),
      password: form.password,
      captchaCode: form.captchaCode.trim(),
      captchaKey: form.captchaKey,
      rememberMe: isRememberChecked()
    })

    // 登录成功，保存记住我
    saveRememberedAccount()

    showToast('登录成功', 'success', 1500)

    // 跳转：有 redirect 参数则跳转 redirect，否则跳首页 tab
    setTimeout(() => {
      if (redirectUrl.value) {
        // 重定向到来源页（可能是 tabBar 或普通页）
        navigate.to(redirectUrl.value)
      } else {
        // 默认跳首页 tab
        navigate.to('pages/home/home')
      }
    }, 800)
  } catch (e: any) {
    console.error('登录失败', e)
    // 错误提示已由 request.ts 拦截器统一处理，此处兜底
    const msg = e?.message || '登录失败，请重试'
    if (!msg.includes('网络')) {
      showToast(msg, 'none')
    }
  } finally {
    loading.value = false
  }
}

// ============ 跳转注册 ============
function goRegister(): void {
  navigate.to('pages/register/register')
}

// ============ 跳转找回密码 ============
function goForgotPassword(): void {
  navigate.to('pages/forgot-password/forgot-password')
}

// ============ 页面加载：读取 redirect 参数 + 记住的账号 ============
onLoad((options: any) => {
  if (options && options.redirect) {
    redirectUrl.value = decodeURIComponent(options.redirect)
  }
  loadRememberedAccount()
})

onMounted(() => {
  // 兜底：若 onLoad 未触发（理论上不会），也尝试读取记住账号
  if (!form.account) {
    loadRememberedAccount()
  }
})
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background-color: $bg-color;
  padding: 0 $spacing-lg;
  box-sizing: border-box;
}

/* 顶部头部 */
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
  padding-bottom: 60rpx;
}

.logo-box {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, $brand-color, #ff7875);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-md;
}

.logo-text {
  color: #ffffff;
  font-size: 32rpx;
  font-weight: bold;
}

.welcome-text {
  font-size: 36rpx;
  color: $text-color-primary;
  font-weight: 500;
}

/* 表单 */
.login-form {
  background-color: $card-bg;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.04);
}

.form-item {
  margin-bottom: $spacing-lg;
}

.item-label {
  font-size: 28rpx;
  color: $text-color-regular;
  margin-bottom: $spacing-xs;
}

/* 验证码行 */
.captcha-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.captcha-img {
  flex-shrink: 0;
}

/* 选项行 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
}

.remember-me {
  display: flex;
  align-items: center;
}

.remember-text {
  font-size: 26rpx;
  color: $text-color-regular;
  margin-left: $spacing-xs;
}

.forgot-link {
  font-size: 26rpx;
  color: $brand-color;
}

/* 操作按钮 */
.form-actions {
  margin-bottom: $spacing-lg;
}

/* 底部注册链接 */
.form-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: $spacing-sm;
}

.footer-text {
  font-size: 26rpx;
  color: $text-color-secondary;
}

.register-link {
  font-size: 26rpx;
  color: $brand-color;
  margin-left: $spacing-xs;
}
</style>