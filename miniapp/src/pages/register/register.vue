<!--
  注册页（对齐 spec.md 3.13）
  - 用户名/邮箱/手机号/密码/确认密码输入
  - 图形验证码（CaptchaImage 组件）
  - 协议同意勾选（用户协议 + 隐私协议，u-checkbox）
  - 注册按钮：authApi.register()，成功后 toast 提示并跳转登录
  - 表单校验（空值、格式、密码一致、协议勾选）
-->
<template>
  <view class="register-page">
    <!-- 顶部标题 -->
    <view class="register-header">
      <text class="title-text">创建账号</text>
      <text class="subtitle-text">注册后即可享受秒杀购物体验</text>
    </view>

    <!-- 注册表单 -->
    <view class="register-form">
      <!-- 用户名 -->
      <view class="form-item">
        <view class="item-label">用户名</view>
        <u-input
          v-model="form.username"
          placeholder="请输入用户名"
          border="surround"
          clearable
          :maxlength="20"
          adjust-position
        />
      </view>

      <!-- 邮箱 -->
      <view class="form-item">
        <view class="item-label">邮箱</view>
        <u-input
          v-model="form.email"
          placeholder="请输入邮箱"
          border="surround"
          clearable
          :maxlength="50"
          adjust-position
        />
      </view>

      <!-- 手机号 -->
      <view class="form-item">
        <view class="item-label">手机号</view>
        <u-input
          v-model="form.phone"
          placeholder="请输入手机号"
          border="surround"
          clearable
          :maxlength="11"
          type="number"
          adjust-position
        />
      </view>

      <!-- 密码 -->
      <view class="form-item">
        <view class="item-label">密码</view>
        <u-input
          v-model="form.password"
          placeholder="请输入密码（6-32位）"
          border="surround"
          :password="true"
          clearable
          :maxlength="32"
          adjust-position
        />
      </view>

      <!-- 确认密码 -->
      <view class="form-item">
        <view class="item-label">确认密码</view>
        <u-input
          v-model="confirmPassword"
          placeholder="请再次输入密码"
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

      <!-- 协议同意 -->
      <view class="agreement-row">
        <u-checkbox-group v-model="agreementChecked">
          <u-checkbox shape="circle" name="agree" />
        </u-checkbox-group>
        <view class="agreement-text">
          <text class="agreement-label">我已阅读并同意</text>
          <text class="agreement-link" @tap.stop="showAgreement('user')">《用户协议》</text>
          <text class="agreement-label">和</text>
          <text class="agreement-link" @tap.stop="showAgreement('privacy')">《隐私协议》</text>
        </view>
      </view>

      <!-- 注册按钮 -->
      <view class="form-actions">
        <u-button
          type="error"
          shape="circle"
          text="注 册"
          :loading="loading"
          :custom-style="btnStyle"
          @click="handleRegister"
        />
      </view>

      <!-- 底部登录链接 -->
      <view class="form-footer">
        <text class="footer-text">已有账号？</text>
        <text class="login-link" @tap="goLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import * as authApi from '@/api/auth'
import { navigate } from '@/utils/navigate'
import { showToast, showConfirm } from '@/utils/toast'
import CaptchaImage from '@/components/CaptchaImage.vue'

// ============ 表单数据 ============
const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  captchaCode: '',
  captchaKey: ''
})

const confirmPassword = ref<string>('')
const agreementChecked = ref<string[]>([])
const loading = ref<boolean>(false)

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

// ============ 校验工具 ============
function isEmail(s: string): boolean {
  return /^[\w.-]+@[\w-]+(\.[\w-]+)+$/.test(s)
}

function isPhone(s: string): boolean {
  return /^1[3-9]\d{9}$/.test(s)
}

// ============ 表单校验 ============
function validateForm(): boolean {
  if (!form.username.trim()) {
    showToast('请输入用户名', 'none')
    return false
  }
  if (form.username.trim().length < 2) {
    showToast('用户名至少2个字符', 'none')
    return false
  }
  if (!form.email.trim()) {
    showToast('请输入邮箱', 'none')
    return false
  }
  if (!isEmail(form.email.trim())) {
    showToast('邮箱格式不正确', 'none')
    return false
  }
  if (!form.phone.trim()) {
    showToast('请输入手机号', 'none')
    return false
  }
  if (!isPhone(form.phone.trim())) {
    showToast('手机号格式不正确', 'none')
    return false
  }
  if (!form.password) {
    showToast('请输入密码', 'none')
    return false
  }
  if (form.password.length < 6 || form.password.length > 32) {
    showToast('密码长度需6-32位', 'none')
    return false
  }
  if (!confirmPassword.value) {
    showToast('请再次输入密码', 'none')
    return false
  }
  if (form.password !== confirmPassword.value) {
    showToast('两次输入的密码不一致', 'none')
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
  if (!agreementChecked.value.includes('agree')) {
    showToast('请先阅读并同意用户协议和隐私协议', 'none')
    return false
  }
  return true
}

// ============ 注册提交 ============
async function handleRegister(): Promise<void> {
  if (loading.value) return
  if (!validateForm()) return

  loading.value = true
  try {
    await authApi.register({
      username: form.username.trim(),
      password: form.password,
      email: form.email.trim(),
      phone: form.phone.trim(),
      captchaCode: form.captchaCode.trim(),
      captchaKey: form.captchaKey,
      // smsCode 可选，本页未启用短信验证码流程
      agreement: true
    })

    showToast('注册成功，请登录', 'success', 1500)

    // 注册成功后跳转登录页
    setTimeout(() => {
      // redirectTo 替换当前页，避免返回回到注册页
      navigate.redirect('pages/login/login')
    }, 1000)
  } catch (e: any) {
    console.error('注册失败', e)
    const msg = e?.message || '注册失败，请重试'
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

// ============ 显示协议（协议页待后续开发，暂用弹窗展示摘要） ============
async function showAgreement(type: 'user' | 'privacy'): Promise<void> {
  const title = type === 'user' ? '用户协议' : '隐私协议'
  const content =
    type === 'user'
      ? '欢迎使用秒杀商城。注册即代表您同意本平台用户协议，您可在本平台浏览商品、参与秒杀、下单购买等。平台将保障您的合法权益。'
      : '本平台重视您的隐私保护。我们仅收集注册所需的信息（用户名、邮箱、手机号），用于身份识别与订单通知，不会向第三方泄露您的个人信息。'
  await showConfirm(content, title)
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background-color: $bg-color;
  padding: 0 $spacing-lg;
  box-sizing: border-box;
}

/* 顶部标题 */
.register-header {
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
.register-form {
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

/* 协议行 */
.agreement-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: $spacing-lg;
}

.agreement-text {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-left: $spacing-xs;
  font-size: 26rpx;
  line-height: 40rpx;
}

.agreement-label {
  color: $text-color-regular;
}

.agreement-link {
  color: $brand-color;
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