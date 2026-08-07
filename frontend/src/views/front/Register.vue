<template>
  <!-- 严格对照 index.html .auth-page / .auth-card 双列布局 -->
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左侧品牌区 -->
      <div class="auth-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
          </svg>
        </div>
        <h2>SeckillMall</h2>
        <p>注册即享新人专属秒杀资格。<br />每日 10:00 / 14:00 / 20:00 三场秒杀等你来抢。</p>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form">
        <h3>注册新账号</h3>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
              v-model.trim="form.username"
              class="form-input"
              :class="{ error: errors.username }"
              type="text"
              placeholder="4-16位字母或数字"
              autocomplete="username"
            />
            <div v-if="errors.username" class="form-error">{{ errors.username }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">手机号</label>
            <input
              v-model.trim="form.phone"
              class="form-input"
              :class="{ error: errors.phone }"
              type="text"
              placeholder="请输入手机号"
              maxlength="11"
              autocomplete="tel"
            />
            <div v-if="errors.phone" class="form-error">{{ errors.phone }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input
              v-model.trim="form.password"
              class="form-input"
              :class="{ error: errors.password }"
              type="password"
              placeholder="6-20位，需含大小写字母和数字"
              autocomplete="new-password"
            />
            <!-- 密码强度指示器 -->
            <div class="strength-bars">
              <div class="strength-bar" :class="{ active: passwordStrength >= 1, weak: passwordStrength === 1 }"></div>
              <div class="strength-bar" :class="{ active: passwordStrength >= 2, mid: passwordStrength === 2 }"></div>
              <div class="strength-bar" :class="{ active: passwordStrength >= 3, strong: passwordStrength === 3 }"></div>
            </div>
            <div class="strength-text" :class="strengthClass">密码强度：{{ strengthLabel }}</div>
            <div v-if="errors.password" class="form-error">{{ errors.password }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">确认密码</label>
            <input
              v-model.trim="form.confirmPassword"
              class="form-input"
              :class="{ error: errors.confirmPassword }"
              type="password"
              placeholder="再次输入密码"
              autocomplete="new-password"
              @keyup.enter="handleRegister"
            />
            <div v-if="errors.confirmPassword" class="form-error">{{ errors.confirmPassword }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">验证码</label>
            <div class="captcha-row">
              <input
                v-model.trim="form.captchaCode"
                class="form-input"
                :class="{ error: errors.captchaCode }"
                type="text"
                placeholder="请输入验证码"
                maxlength="4"
                @keyup.enter="handleRegister"
              />
              <div class="captcha-img" @click="refreshCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else>点击获取</span>
              </div>
            </div>
            <div v-if="errors.captchaCode" class="form-error">{{ errors.captchaCode }}</div>
          </div>

          <button class="form-btn" type="submit" :disabled="loading">
            {{ loading ? '注册中...' : '注 册' }}
          </button>
        </form>

        <div class="form-link">已有账号？<router-link to="/login">立即登录</router-link></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P06 注册页
 * 严格对照 index.html .auth-page / .auth-card 样式（原生 HTML + CSS 双列布局）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref<boolean>(false)
const captchaImage = ref<string>('')
const captchaKey = ref<string>('')

const form = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  captchaCode: ''
})

const errors = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  captchaCode: ''
})

/** 密码强度: 0=空, 1=弱, 2=中, 3=强 */
const passwordStrength = computed<number>(() => {
  const pwd = form.password
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

/** 表单校验 */
function validate(): boolean {
  errors.username = ''
  errors.phone = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.captchaCode = ''
  let valid = true
  if (!form.username) {
    errors.username = '请输入用户名'
    valid = false
  } else if (form.username.length < 4 || form.username.length > 16) {
    errors.username = '用户名长度为 4-16 位'
    valid = false
  }
  if (!form.phone) {
    errors.phone = '请输入手机号'
    valid = false
  } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '手机号格式不正确'
    valid = false
  }
  if (!form.password) {
    errors.password = '请输入密码'
    valid = false
  } else if (form.password.length < 6 || form.password.length > 20) {
    errors.password = '密码长度为 6-20 位'
    valid = false
  } else if (!/[a-z]/.test(form.password) || !/[A-Z]/.test(form.password) || !/\d/.test(form.password)) {
    // M-F7 修复: 统一密码规则为 6-20 位 + 必须包含大小写字母和数字
    errors.password = '密码必须包含大小写字母和数字'
    valid = false
  }
  if (!form.confirmPassword) {
    errors.confirmPassword = '请再次输入密码'
    valid = false
  } else if (form.confirmPassword !== form.password) {
    errors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  if (!form.captchaCode) {
    errors.captchaCode = '请输入验证码'
    valid = false
  } else if (form.captchaCode.length !== 4) {
    errors.captchaCode = '验证码为 4 位'
    valid = false
  }
  return valid
}

/** 获取验证码 */
async function refreshCaptcha(): Promise<void> {
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaId
    captchaImage.value = res.data.captchaImage
  } catch {
    // 错误已由拦截器处理
  }
}

/** 处理注册 */
async function handleRegister(): Promise<void> {
  if (!validate()) return
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode
    })
    ElMessage.success('注册成功，正在自动登录...')
    try {
      await userStore.login({
        username: form.username,
        password: form.password
      })
      router.push('/')
    } catch {
      router.push('/login')
    }
  } catch {
    refreshCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
/* 严格对照 index.html .auth-page 样式 */
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

/* 密码强度条 */
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

/* 提交按钮 */
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

.form-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}

.form-link a:hover {
  text-decoration: underline;
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 8px;
}

.captcha-row .form-input {
  flex: 1;
}

.captcha-img {
  width: 100px;
  height: 40px;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #333;
  font-style: italic;
  letter-spacing: 2px;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: var(--color-primary);
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
