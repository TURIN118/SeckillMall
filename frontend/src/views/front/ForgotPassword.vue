<template>
  <!-- 找回密码页：对照 Login.vue 的 .auth-page / .auth-card 双列布局 -->
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左侧品牌区（与 Login.vue 一致） -->
      <div class="auth-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
          </svg>
        </div>
        <h2>SeckillMall</h2>
        <p>正品秒杀，手快有手慢无。<br />每日精选大牌好物，限时低价抢购。</p>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form">
        <h3>找回密码</h3>

        <!-- 步骤 1：选择验证方式 + 输入账号 + 发送验证码 -->
        <div v-if="step === 1">
          <div class="form-group">
            <label class="form-label">验证方式</label>
            <div class="type-tabs">
              <div
                class="type-tab"
                :class="{ active: form.type === 'PHONE' }"
                @click="form.type = 'PHONE'"
              >
                手机短信
              </div>
              <div
                class="type-tab"
                :class="{ active: form.type === 'EMAIL' }"
                @click="form.type = 'EMAIL'"
              >
                邮箱验证码
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">{{ form.type === 'PHONE' ? '手机号' : '邮箱' }}</label>
            <input
              v-model.trim="form.account"
              class="form-input"
              :class="{ error: errors.account }"
              type="text"
              :placeholder="form.type === 'PHONE' ? '请输入手机号' : '请输入邮箱'"
              autocomplete="off"
            />
            <div v-if="errors.account" class="form-error">{{ errors.account }}</div>
          </div>

          <button class="form-btn" type="button" :disabled="sending" @click="handleSendCode">
            {{ sending ? '发送中...' : '发送验证码' }}
          </button>
        </div>

        <!-- 步骤 2：输入验证码 + 新密码 + 确认密码 -->
        <div v-else>
          <div class="step-tip">
            验证码已发送至 <span class="step-account">{{ form.account }}</span>
            <span class="step-change" @click="backToStep1">修改</span>
          </div>

          <div class="form-group">
            <label class="form-label">验证码</label>
            <div class="code-row">
              <input
                v-model.trim="form.code"
                class="form-input"
                :class="{ error: errors.code }"
                type="text"
                placeholder="请输入验证码"
                maxlength="6"
                autocomplete="off"
              />
              <button
                class="code-btn"
                type="button"
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '重新发送' }}
              </button>
            </div>
            <div v-if="errors.code" class="form-error">{{ errors.code }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">新密码</label>
            <input
              v-model.trim="form.newPassword"
              class="form-input"
              :class="{ error: errors.newPassword }"
              type="password"
              placeholder="请输入新密码 (6-20位)"
              autocomplete="new-password"
            />
            <!-- 密码强度指示器（参考 UserProfile.vue） -->
            <div class="strength-bars">
              <div
                class="strength-bar"
                :class="{ active: passwordStrength >= 1, weak: passwordStrength === 1 }"
              ></div>
              <div
                class="strength-bar"
                :class="{ active: passwordStrength >= 2, mid: passwordStrength === 2 }"
              ></div>
              <div
                class="strength-bar"
                :class="{ active: passwordStrength >= 3, strong: passwordStrength === 3 }"
              ></div>
            </div>
            <div class="strength-text" :class="strengthClass">密码强度：{{ strengthLabel }}</div>
            <div v-if="errors.newPassword" class="form-error">{{ errors.newPassword }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">确认新密码</label>
            <input
              v-model.trim="form.confirmPassword"
              class="form-input"
              :class="{ error: errors.confirmPassword }"
              type="password"
              placeholder="请再次输入新密码"
              autocomplete="new-password"
              @keyup.enter="handleReset"
            />
            <div v-if="errors.confirmPassword" class="form-error">{{ errors.confirmPassword }}</div>
          </div>

          <button class="form-btn" type="button" :disabled="loading" @click="handleReset">
            {{ loading ? '重置中...' : '重置密码' }}
          </button>
        </div>

        <div class="form-link">
          <span class="back-link" @click="router.push('/login')">返回登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 找回密码页
 * 支持手机短信与邮箱两种验证方式，双步骤：
 *  1. 选择验证方式 + 输入账号 + 发送验证码
 *  2. 输入验证码 + 新密码 + 确认密码 + 提交重置
 */
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendForgotPasswordCode, resetPassword } from '@/api/auth'

const router = useRouter()

/** 当前步骤：1=发送验证码，2=重置密码 */
const step = ref<number>(1)
const sending = ref<boolean>(false)
const loading = ref<boolean>(false)
/** 倒计时秒数，>0 时禁用重新发送按钮 */
const countdown = ref<number>(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  type: 'PHONE' as 'PHONE' | 'EMAIL',
  account: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const errors = reactive({
  account: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

/* === 密码强度（参考 UserProfile.vue 实现） === */
const passwordStrength = computed<number>(() => {
  const pwd = form.newPassword
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  const hasLower = /[a-z]/.test(pwd)
  const hasUpper = /[A-Z]/.test(pwd)
  const hasNumber = /\d/.test(pwd)
  const hasSpecial = /[^a-zA-Z0-9]/.test(pwd)
  const complexity = [hasLower, hasUpper, hasNumber, hasSpecial].filter(Boolean).length
  if (complexity >= 2) score++
  if (complexity >= 3) score++
  return Math.min(3, score)
})

const strengthLabel = computed<string>(() => {
  const labels = ['', '弱', '中', '强']
  return labels[passwordStrength.value] || ''
})

const strengthClass = computed<string>(() => {
  const classes = ['', 'weak', 'mid', 'strong']
  return classes[passwordStrength.value] || ''
})

/** 启动 60 秒倒计时 */
function startCountdown(): void {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }
  }, 1000)
}

/** 校验账号格式 */
function validateAccount(): boolean {
  errors.account = ''
  if (!form.account) {
    errors.account = form.type === 'PHONE' ? '请输入手机号' : '请输入邮箱'
    return false
  }
  if (form.type === 'PHONE') {
    if (!/^1[3-9]\d{9}$/.test(form.account)) {
      errors.account = '手机号格式不正确'
      return false
    }
  } else {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.account)) {
      errors.account = '邮箱格式不正确'
      return false
    }
  }
  return true
}

/** 校验重置表单 */
function validateResetForm(): boolean {
  errors.code = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
  let valid = true
  if (!form.code) {
    errors.code = '请输入验证码'
    valid = false
  } else if (form.code.length !== 6) {
    errors.code = '验证码为 6 位'
    valid = false
  }
  if (!form.newPassword) {
    errors.newPassword = '请输入新密码'
    valid = false
  } else if (form.newPassword.length < 6 || form.newPassword.length > 20) {
    errors.newPassword = '密码长度为 6-20 位'
    valid = false
  }
  if (!form.confirmPassword) {
    errors.confirmPassword = '请再次输入新密码'
    valid = false
  } else if (form.confirmPassword !== form.newPassword) {
    errors.confirmPassword = '两次密码不一致'
    valid = false
  }
  return valid
}

/** 发送验证码 */
async function handleSendCode(): Promise<void> {
  if (!validateAccount()) return
  sending.value = true
  try {
    await sendForgotPasswordCode({ account: form.account, type: form.type })
    ElMessage.success('验证码已发送')
    step.value = 2
    startCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    sending.value = false
  }
}

/** 返回步骤 1，并清空验证码 */
function backToStep1(): void {
  step.value = 1
  form.code = ''
  errors.code = ''
}

/** 提交重置密码 */
async function handleReset(): Promise<void> {
  if (!validateResetForm()) return
  loading.value = true
  try {
    await resetPassword({
      account: form.account,
      type: form.type,
      code: form.code,
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功')
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
/* 严格对照 Login.vue 的 .auth-page 样式 */
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 108px);
  background: linear-gradient(135deg, var(--price-bg) 0%, var(--tag-unpaid-bg) 100%);
  padding: 40px;
}

/* auth-card 双列布局 */
.auth-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 800px;
  background: var(--color-bg-card);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 品牌区：红色渐变背景 */
.auth-brand {
  background: linear-gradient(160deg, var(--color-primary), var(--color-accent));
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.auth-brand h2 {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 10px;
  letter-spacing: -0.01em;
}

.auth-brand p {
  font-size: 15px;
  opacity: 0.85;
  line-height: 1.7;
}

.auth-brand .brand-icon {
  margin-bottom: 24px;
}

.auth-brand .brand-icon svg {
  width: 56px;
  height: 56px;
}

/* 表单区 */
.auth-form {
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-form h3 {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 24px;
  letter-spacing: -0.01em;
  color: var(--color-text-primary);
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

/* 原生输入框对照 .form-input 样式 */
.form-input {
  width: 100%;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 12px;
  font-size: 13px;
  transition: border-color 0.2s;
  outline: none;
  box-sizing: border-box;
  background: #fff;
  color: var(--color-text-primary);
}

.form-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(229, 57, 53, 0.1);
}

.form-input.error {
  border-color: var(--color-danger);
}

.form-error {
  font-size: 11px;
  color: var(--color-danger);
  margin-top: 4px;
}

/* 提交按钮：红底白字 100% 宽 */
.form-btn {
  width: 100%;
  height: 42px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  letter-spacing: 0.02em;
  transition: background 0.2s;
}

.form-btn:hover {
  background: var(--btn-hover);
}

.form-btn:disabled {
  background: var(--btn-loading-bg);
  cursor: not-allowed;
}

.form-link {
  text-align: center;
  margin-top: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.back-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
  cursor: pointer;
}

.back-link:hover {
  text-decoration: underline;
}

/* 验证方式切换 Tab */
.type-tabs {
  display: flex;
  gap: 8px;
}

.type-tab {
  flex: 1;
  height: 40px;
  line-height: 40px;
  text-align: center;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  box-sizing: border-box;
  background: #fff;
}

.type-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.type-tab.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(229, 57, 53, 0.06);
}

/* 步骤 2 顶部提示 */
.step-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  padding: 8px 12px;
  background: var(--color-bg-muted);
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.step-account {
  color: var(--color-text-primary);
  font-weight: 600;
}

.step-change {
  margin-left: auto;
  color: var(--color-primary);
  cursor: pointer;
  font-weight: 600;
}

.step-change:hover {
  text-decoration: underline;
}

/* 验证码行 */
.code-row {
  display: flex;
  gap: 8px;
}

.code-row .form-input {
  flex: 1;
}

.code-btn {
  width: 110px;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: #fff;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
  box-sizing: border-box;
}

.code-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  background: rgba(229, 57, 53, 0.06);
}

.code-btn:disabled {
  color: var(--color-text-secondary);
  cursor: not-allowed;
  background: var(--color-bg-muted);
}

/* 密码强度条（对照 UserProfile.vue） */
.strength-bars {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.strength-bar {
  flex: 1;
  height: 3px;
  background: var(--btn-disabled-bg);
  border-radius: 2px;
  transition: background 0.2s;
}

.strength-bar.active.weak {
  background: var(--color-danger);
}

.strength-bar.active.mid {
  background: var(--color-warning);
}

.strength-bar.active.strong {
  background: var(--color-success);
}

.strength-text {
  font-size: 10px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.strength-text.weak {
  color: var(--color-danger);
}

.strength-text.mid {
  color: var(--color-warning);
}

.strength-text.strong {
  color: var(--color-success);
}

/* 响应式 */
@media (max-width: 768px) {
  .auth-card {
    grid-template-columns: 1fr;
    width: 100%;
    max-width: 420px;
  }
  .auth-brand {
    padding: 32px 24px;
  }
  .auth-form {
    padding: 32px 24px;
  }
}
</style>