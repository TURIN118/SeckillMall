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
        <p>正品秒杀，手快有手慢无。<br />每日精选大牌好物，限时低价抢购。</p>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form">
        <h3>登录</h3>
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="form-label">用户名 / 手机号</label>
            <input
              v-model.trim="form.username"
              class="form-input"
              :class="{ error: errors.username }"
              type="text"
              placeholder="请输入用户名"
              autocomplete="username"
            />
            <div v-if="errors.username" class="form-error">{{ errors.username }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input
              v-model.trim="form.password"
              class="form-input"
              :class="{ error: errors.password }"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
            <div v-if="errors.password" class="form-error">{{ errors.password }}</div>
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
                @keyup.enter="handleLogin"
              />
              <div class="captcha-img" @click="refreshCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else>点击获取</span>
              </div>
            </div>
            <div v-if="errors.captchaCode" class="form-error">{{ errors.captchaCode }}</div>
          </div>

          <button class="form-btn" type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="form-link">没有账号？<router-link to="/register">立即注册</router-link></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P05 登录页
 * 严格对照 index.html .auth-page / .auth-card 样式（原生 HTML + CSS 双列布局）
 */
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref<boolean>(false)
const captchaImage = ref<string>('')
const captchaKey = ref<string>('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const errors = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

/** 表单校验 */
function validate(): boolean {
  errors.username = ''
  errors.password = ''
  errors.captchaCode = ''
  let valid = true
  if (!form.username) {
    errors.username = '请输入用户名'
    valid = false
  } else if (form.username.length < 4 || form.username.length > 16) {
    errors.username = '用户名长度为 4-16 位'
    valid = false
  }
  if (!form.password) {
    errors.password = '请输入密码'
    valid = false
  } else if (form.password.length < 6 || form.password.length > 20) {
    errors.password = '密码长度为 6-20 位'
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

/** 处理登录 */
async function handleLogin(): Promise<void> {
  if (!validate()) return
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode
    })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch {
    ElMessage.error('用户名或密码错误')
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

/* 验证码图片：100x40 灰色背景 */
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
