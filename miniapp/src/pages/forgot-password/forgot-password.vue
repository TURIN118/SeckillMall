<!--
  找回密码页（对齐 spec.md 3.14）
  - 方式切换（手机/邮箱，u-tabs）
  - 账号输入
  - 发送验证码按钮（authApi.sendResetCode()，60s 倒计时禁用）
  - 验证码输入
  - 新密码/确认密码输入
  - 重置按钮：authApi.resetPassword()，成功后跳转登录
-->
<template>
  <view class="forgot-page">
    <!-- 顶部标题 -->
    <view class="forgot-header">
      <text class="title-text">找回密码</text>
      <text class="subtitle-text">通过手机号或邮箱重置您的密码</text>
    </view>

    <!-- 重置表单 -->
    <view class="forgot-form">
      <!-- 方式切换 -->
      <view class="form-item">
        <u-tabs
          :list="methodTabs"
          :current="currentMethod"
          @click="onMethodChange"
          :scrollable="false"
          line-color="#ff4d4f"
          :active-style="{ color: '#ff4d4f' }"
        />
      </view>

      <!-- 账号输入（手机号或邮箱） -->
      <view class="form-item">
        <view class="item-label">{{ currentMethod === 0 ? '手机号' : '邮箱' }}</view>
        <u-input
          v-model="form.account"
          :placeholder="currentMethod === 0 ? '请输入手机号' : '请输入邮箱'"
          border="surround"
          clearable
          :maxlength="currentMethod === 0 ? 11 : 50"
          :type="currentMethod === 0 ? 'number' : 'text'"
          adjust-position
        />
      </view>

      <!-- 验证码 + 发送按钮 -->
      <view class="form-item">
        <view class="item-label">验证码</view>
        <view class="code-row">
          <u-input
            v-model="form.code"
            placeholder="请输入验证码"
            border="surround"
            clearable
            :maxlength="6"
            type="number"
            adjust-position
          />
          <view
            class="send-btn"
            :class="{ 'send-btn-disabled': countdown > 0 || sending }"
            @tap="handleSendCode"
          >
            <text class="send-btn-text">{{ sendBtnText }}</text>
          </view>
        </view>
      </view>

      <!-- 新密码 -->
      <view class="form-item">
        <view class="item-label">新密码</view>
        <u-input
          v-model="form.newPassword"
          placeholder="请输入新密码（6-32位）"
          border="surround"
          :password="true"
          clearable
          :maxlength="32"
          adjust-position
        />
      </view>

      <!-- 确认密码 -->
      <view class="form-item">
        <view class="item-label">确认新密码</view>
        <u-input
          v-model="confirmPassword"
          placeholder="请再次输入新密码"
          border="surround"
          :password="true"
          clearable
          :maxlength="32"
          adjust-position
        />
      </view>

      <!-- 重置按钮 -->
      <view class="form-actions">
        <u-button
          type="error"
          shape="circle"
          text="重置密码"
          :loading="loading"
          :custom-style="btnStyle"
          @click="handleReset"
        />
      </view>

      <!-- 底部登录链接 -->
      <view class="form-footer">
        <text class="footer-text">想起密码了？</text>
        <text class="login-link" @tap="goLogin">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import * as authApi from '@/api/auth'
import { navigate } from '@/utils/navigate'
import { showToast } from '@/utils/toast'

// ============ 方式切换 ============
const methodTabs = [
  { name: '手机号' },
  { name: '邮箱' }
]
// 0 = 手机号，1 = 邮箱
const currentMethod = ref<number>(0)

// ============ 表单数据 ============
const form = reactive({
  account: '',
  code: '',
  newPassword: ''
})

const confirmPassword = ref<string>('')
const loading = ref<boolean>(false)
const sending = ref<boolean>(false)

// ============ 倒计时 ============
const COUNTDOWN_TOTAL = 60
const countdown = ref<number>(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const sendBtnText = computed<string>(() => {
  if (sending.value) return '发送中'
  if (countdown.value > 0) return `${countdown.value}s 后重发`
  return '发送验证码'
})

function startCountdown(): void {
  countdown.value = COUNTDOWN_TOTAL
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdownTimer()
    }
  }, 1000)
}

function clearCountdownTimer(): void {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onUnmounted(() => {
  clearCountdownTimer()
})

// ============ 按钮样式 ============
const btnStyle = {
  width: '100%',
  height: '88rpx',
  fontSize: '32rpx'
}

// ============ 方式切换 ============
function onMethodChange(tab: any): void {
  const idx = tab.index ?? 0
  if (idx === currentMethod.value) return
  currentMethod.value = idx
  // 切换方式时清空账号与验证码
  form.account = ''
  form.code = ''
}

// ============ 校验工具 ============
function isEmail(s: string): boolean {
  return /^[\w.-]+@[\w-]+(\.[\w-]+)+$/.test(s)
}

function isPhone(s: string): boolean {
  return /^1[3-9]\d{9}$/.test(s)
}

// ============ 发送验证码 ============
async function handleSendCode(): Promise<void> {
  if (countdown.value > 0 || sending.value) return

  // 校验账号
  if (!form.account.trim()) {
    showToast(currentMethod.value === 0 ? '请输入手机号' : '请输入邮箱', 'none')
    return
  }
  if (currentMethod.value === 0 && !isPhone(form.account.trim())) {
    showToast('手机号格式不正确', 'none')
    return
  }
  if (currentMethod.value === 1 && !isEmail(form.account.trim())) {
    showToast('邮箱格式不正确', 'none')
    return
  }

  sending.value = true
  try {
    const payload =
      currentMethod.value === 0
        ? { phone: form.account.trim() }
        : { email: form.account.trim() }
    await authApi.sendResetCode(payload)
    showToast('验证码已发送，请注意查收', 'success', 1500)
    startCountdown()
  } catch (e: any) {
    console.error('发送验证码失败', e)
    const msg = e?.message || '发送验证码失败，请重试'
    if (!msg.includes('网络')) {
      showToast(msg, 'none')
    }
  } finally {
    sending.value = false
  }
}

// ============ 重置密码校验 ============
function validateForm(): boolean {
  if (!form.account.trim()) {
    showToast(currentMethod.value === 0 ? '请输入手机号' : '请输入邮箱', 'none')
    return false
  }
  if (currentMethod.value === 0 && !isPhone(form.account.trim())) {
    showToast('手机号格式不正确', 'none')
    return false
  }
  if (currentMethod.value === 1 && !isEmail(form.account.trim())) {
    showToast('邮箱格式不正确', 'none')
    return false
  }
  if (!form.code.trim()) {
    showToast('请输入验证码', 'none')
    return false
  }
  if (!form.newPassword) {
    showToast('请输入新密码', 'none')
    return false
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 32) {
    showToast('新密码长度需6-32位', 'none')
    return false
  }
  if (!confirmPassword.value) {
    showToast('请再次输入新密码', 'none')
    return false
  }
  if (form.newPassword !== confirmPassword.value) {
    showToast('两次输入的密码不一致', 'none')
    return false
  }
  return true
}

// ============ 重置密码提交 ============
async function handleReset(): Promise<void> {
  if (loading.value) return
  if (!validateForm()) return

  loading.value = true
  try {
    const payload = {
      code: form.code.trim(),
      newPassword: form.newPassword,
      ...(currentMethod.value === 0
        ? { phone: form.account.trim() }
        : { email: form.account.trim() })
    }
    await authApi.resetPassword(payload)

    showToast('密码重置成功，请登录', 'success', 1500)

    // 重置成功后跳转登录页
    setTimeout(() => {
      navigate.redirect('pages/login/login')
    }, 1000)
  } catch (e: any) {
    console.error('重置密码失败', e)
    const msg = e?.message || '重置密码失败，请重试'
    if (!msg.includes('网络')) {
      showToast(msg, 'none')
    }
  } finally {
    loading.value = false
  }
}

// ============ 跳转登录 ============
function goLogin(): void {
  navigate.redirect('pages/login/login')
}
</script>

<style lang="scss" scoped>
.forgot-page {
  min-height: 100vh;
  background-color: $bg-color;
  padding: 0 $spacing-lg;
  box-sizing: border-box;
}

/* 顶部标题 */
.forgot-header {
  display: flex;
  flex-direction: column;
  padding-top: 60rpx;
  padding-bottom: 40rpx;
}

.title-text {
  font-size: 40rpx;
  color: $text-color-primary;
  font-weight: bold;
  margin-bottom: $spacing-xs;
}

.subtitle-text {
  font-size: 26rpx;
  color: $text-color-secondary;
}

/* 表单 */
.forgot-form {
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
.code-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

/* 发送按钮 */
.send-btn {
  flex-shrink: 0;
  min-width: 200rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid $brand-color;
  border-radius: $radius-sm;
  background-color: #fff;
  padding: 0 $spacing-sm;
  box-sizing: border-box;
}

.send-btn-text {
  font-size: 24rpx;
  color: $brand-color;
  white-space: nowrap;
}

.send-btn-disabled {
  border-color: $text-color-secondary;
  background-color: #f5f5f5;
}

.send-btn-disabled .send-btn-text {
  color: $text-color-secondary;
}

/* 操作按钮 */
.form-actions {
  margin-bottom: $spacing-lg;
}

/* 底部登录链接 */
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

.login-link {
  font-size: 26rpx;
  color: $brand-color;
  margin-left: $spacing-xs;
}
</style>