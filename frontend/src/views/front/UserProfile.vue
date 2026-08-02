<template>
  <!-- 严格对照 index.html .profile-layout 样式 -->
  <div class="profile-page">
    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-wrap">
      <div v-for="i in 6" :key="i" class="skeleton-line"></div>
    </div>

    <div v-else-if="user" class="profile-layout">
      <!-- 左列: 用户信息卡片 -->
      <div class="profile-card">
        <div class="profile-avatar">{{ avatarText }}</div>
        <div class="profile-name">{{ user.nickname || user.username }}</div>
        <div class="profile-phone">{{ maskedPhone }}</div>
        <span class="role-badge" :class="roleClass">{{ roleLabel }}</span>
        <dl class="profile-meta">
          <div class="profile-meta-row">
            <dt>用户名</dt>
            <dd>{{ user.username }}</dd>
          </div>
          <div class="profile-meta-row">
            <dt>邮箱</dt>
            <dd>{{ user.email || '—' }}</dd>
          </div>
          <div class="profile-meta-row">
            <dt>注册时间</dt>
            <dd>{{ formatDate(user.createTime) }}</dd>
          </div>
          <div class="profile-meta-row">
            <dt>账号状态</dt>
            <dd>{{ user.status === 'ACTIVE' ? '正常' : '已禁用' }}</dd>
          </div>
        </dl>
        <!-- 快捷入口 -->
        <div class="profile-shortcuts">
          <div class="shortcut-item" @click="goTo('/user/wallet')">
            <el-icon><Wallet /></el-icon>
            <span>我的钱包</span>
          </div>
          <div class="shortcut-item" @click="goTo('/user/coupons')">
            <el-icon><Ticket /></el-icon>
            <span>我的优惠券</span>
          </div>
          <div class="shortcut-item" @click="goTo('/user/address')">
            <el-icon><Location /></el-icon>
            <span>收货地址</span>
          </div>
          <div class="shortcut-item" @click="goTo('/user/orders')">
            <el-icon><List /></el-icon>
            <span>我的订单</span>
          </div>
        </div>
      </div>

      <!-- 右列: 标签页内容 -->
      <div class="profile-main">
        <!-- 标签页 -->
        <div class="profile-tabs">
          <div class="profile-tab" :class="{ active: activeTab === 'info' }" @click="activeTab = 'info'">基本信息</div>
          <div class="profile-tab" :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">修改密码
          </div>
        </div>

        <!-- Tab 1: 基本信息 -->
        <div v-if="activeTab === 'info'" class="profile-form">
          <div class="info-card">
            <!-- 只读展示模式 -->
            <template v-if="!editing">
              <div class="info-row">
                <div class="info-label">用户名</div>
                <div class="info-value">{{ user.username }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">昵称</div>
                <div class="info-value">{{ user.nickname || '—' }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">手机号</div>
                <div class="info-value">{{ maskedPhone }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">邮箱</div>
                <div class="info-value">{{ user.email || '—' }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">注册时间</div>
                <div class="info-value">{{ formatTime(user.createTime) }}</div>
              </div>
              <div class="info-actions">
                <button class="btn-sm primary" type="button" @click="enterEdit">修改信息</button>
              </div>
            </template>

            <!-- 编辑模式 -->
            <template v-else>
              <div class="info-row">
                <div class="info-label">用户名</div>
                <div class="info-value readonly">{{ user.username }}</div>
              </div>
              <div class="info-row">
                <label class="info-label" for="edit-nickname">昵称</label>
                <input id="edit-nickname" v-model.trim="editForm.nickname" class="form-input" type="text"
                  placeholder="请输入昵称" maxlength="20" />
              </div>
              <div class="info-row">
                <label class="info-label" for="edit-phone">手机号</label>
                <div class="info-edit-cell">
                  <input id="edit-phone" v-model.trim="editForm.phone" class="form-input"
                    :class="{ error: editErrors.phone }" type="text" placeholder="请输入 11 位手机号" maxlength="11" />
                  <div v-if="editErrors.phone" class="form-error">{{ editErrors.phone }}</div>
                  <!-- 验证码输入 + 发送按钮 (仅手机号变更时需要) -->
                  <div v-if="isPhoneChanged" class="verify-row">
                    <input
                      v-model.trim="editForm.phoneCode"
                      class="form-input verify-input"
                      :class="{ error: editErrors.phoneCode }"
                      type="text"
                      placeholder="请输入短信验证码"
                      maxlength="6"
                    />
                    <button
                      class="btn-sm primary verify-btn"
                      type="button"
                      :disabled="phoneCountdown > 0 || sendingPhoneCode"
                      @click="handleSendPhoneCode"
                    >
                      {{ phoneCountdown > 0 ? `${phoneCountdown}s` : (sendingPhoneCode ? '发送中' : '发送验证码') }}
                    </button>
                  </div>
                  <div v-if="editErrors.phoneCode" class="form-error">{{ editErrors.phoneCode }}</div>
                </div>
              </div>
              <div class="info-row">
                <label class="info-label" for="edit-email">邮箱</label>
                <div class="info-edit-cell">
                  <input id="edit-email" v-model.trim="editForm.email" class="form-input"
                    :class="{ error: editErrors.email }" type="text" placeholder="请输入邮箱 (可选)" />
                  <div v-if="editErrors.email" class="form-error">{{ editErrors.email }}</div>
                  <!-- 验证码输入 + 发送按钮 (仅邮箱变更时需要) -->
                  <div v-if="isEmailChanged" class="verify-row">
                    <input
                      v-model.trim="editForm.emailCode"
                      class="form-input verify-input"
                      :class="{ error: editErrors.emailCode }"
                      type="text"
                      placeholder="请输入邮箱验证码"
                      maxlength="6"
                    />
                    <button
                      class="btn-sm primary verify-btn"
                      type="button"
                      :disabled="emailCountdown > 0 || sendingEmailCode"
                      @click="handleSendEmailCode"
                    >
                      {{ emailCountdown > 0 ? `${emailCountdown}s` : (sendingEmailCode ? '发送中' : '发送验证码') }}
                    </button>
                  </div>
                  <div v-if="editErrors.emailCode" class="form-error">{{ editErrors.emailCode }}</div>
                </div>
              </div>
              <div class="info-row">
                <div class="info-label">注册时间</div>
                <div class="info-value readonly">{{ formatTime(user.createTime) }}</div>
              </div>
              <div class="info-actions">
                <button class="btn-sm primary" type="button" :disabled="editLoading" @click="handleSaveProfile">
                  {{ editLoading ? '保存中...' : '保存' }}
                </button>
                <button class="btn-sm" type="button" :disabled="editLoading" @click="cancelEdit">取消</button>
              </div>
            </template>
          </div>
        </div>

        <!-- Tab 2: 修改密码 -->
        <div v-else class="profile-form">
          <div class="pwd-card">
            <form @submit.prevent="handleChangePassword">
              <div class="form-group">
                <label class="form-label">旧密码</label>
                <input v-model.trim="pwdForm.oldPassword" class="form-input" :class="{ error: pwdErrors.oldPassword }"
                  type="password" placeholder="请输入当前密码" autocomplete="current-password" />
                <div v-if="pwdErrors.oldPassword" class="form-error">{{ pwdErrors.oldPassword }}</div>
              </div>

              <div class="form-group">
                <label class="form-label">新密码</label>
                <input v-model.trim="pwdForm.newPassword" class="form-input" :class="{ error: pwdErrors.newPassword }"
                  type="password" placeholder="请输入新密码 (6-20位)" autocomplete="new-password" />
                <!-- 密码强度指示器 -->
                <div class="strength-bars">
                  <div class="strength-bar" :class="{ active: passwordStrength >= 1, weak: passwordStrength === 1 }">
                  </div>
                  <div class="strength-bar" :class="{ active: passwordStrength >= 2, mid: passwordStrength === 2 }"></div>
                  <div class="strength-bar" :class="{ active: passwordStrength >= 3, strong: passwordStrength === 3 }">
                  </div>
                </div>
                <div class="strength-text" :class="strengthClass">密码强度：{{ strengthLabel }}</div>
                <div v-if="pwdErrors.newPassword" class="form-error">{{ pwdErrors.newPassword }}</div>
              </div>

              <div class="form-group">
                <label class="form-label">确认新密码</label>
                <input v-model.trim="pwdForm.confirmPassword" class="form-input"
                  :class="{ error: pwdErrors.confirmPassword }" type="password" placeholder="请再次输入新密码"
                  autocomplete="new-password" />
                <div v-if="pwdErrors.confirmPassword" class="form-error">{{ pwdErrors.confirmPassword }}</div>
              </div>

              <div class="form-actions">
                <button class="btn-sm primary" type="submit" :disabled="pwdLoading">
                  {{ pwdLoading ? '修改中...' : '修改密码' }}
                </button>
                <button class="btn-sm" type="button" @click="resetPwdForm">重置</button>
              </div>
              <div class="form-tip">修改成功后将自动退出登录，需重新登录</div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P09 个人中心
 * 严格对照 index.html .profile-layout / .profile-card / .profile-tabs 样式
 */
defineOptions({ name: 'UserProfile' })
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Wallet, Ticket, Location, List } from '@element-plus/icons-vue'
import { changePassword, updateProfile, updatePhone, updateEmail } from '@/api/auth'
import { sendSmsCode, sendEmailCode } from '@/api/verification'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

/** 快捷入口跳转 */
function goTo(path: string): void {
  router.push(path)
}

const loading = ref<boolean>(false)
const activeTab = ref<string>('info')
const pwdLoading = ref<boolean>(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdErrors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/* === 基本信息: 编辑模式 === */
const editing = ref<boolean>(false)
const editLoading = ref<boolean>(false)
const editForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  phoneCode: '',
  emailCode: ''
})
const editErrors = reactive({
  nickname: '',
  phone: '',
  email: '',
  phoneCode: '',
  emailCode: ''
})

/* === 验证码倒计时 (60s) === */
const phoneCountdown = ref<number>(0)
const emailCountdown = ref<number>(0)
const sendingPhoneCode = ref<boolean>(false)
const sendingEmailCode = ref<boolean>(false)
let phoneTimer: ReturnType<typeof setInterval> | null = null
let emailTimer: ReturnType<typeof setInterval> | null = null

/** 手机号是否变更 (变更才需要验证码) */
const isPhoneChanged = computed<boolean>(() => {
  return editForm.phone.trim() !== (user.value?.phone || '')
})

/** 邮箱是否变更 (变更才需要验证码) */
const isEmailChanged = computed<boolean>(() => {
  return editForm.email.trim() !== (user.value?.email || '')
})

const user = computed(() => userStore.userInfo)

const avatarText = computed<string>(() => {
  const name = user.value?.nickname || user.value?.username || ''
  return name.charAt(0).toUpperCase()
})

const maskedPhone = computed<string>(() => {
  const phone = user.value?.phone || ''
  if (phone.length === 11) {
    return phone.substring(0, 3) + '****' + phone.substring(7)
  }
  return phone || '—'
})

const roleLabel = computed<string>(() => {
  const map: Record<string, string> = {
    BUYER: '普通买家',
    SELLER: '卖家',
    ADMIN: '管理员'
  }
  return map[user.value?.role || ''] || '用户'
})

/** 角色徽章 class：admin 红 / seller 橙 / buyer 蓝 */
const roleClass = computed<string>(() => {
  const map: Record<string, string> = {
    BUYER: 'buyer',
    SELLER: 'seller',
    ADMIN: 'admin'
  }
  return map[user.value?.role || ''] || 'buyer'
})

/* === 密码强度 === */
const passwordStrength = computed<number>(() => {
  const pwd = pwdForm.newPassword
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
function validatePwdForm(): boolean {
  pwdErrors.oldPassword = ''
  pwdErrors.newPassword = ''
  pwdErrors.confirmPassword = ''
  let valid = true
  if (!pwdForm.oldPassword) {
    pwdErrors.oldPassword = '请输入当前密码'
    valid = false
  }
  if (!pwdForm.newPassword) {
    pwdErrors.newPassword = '请输入新密码'
    valid = false
  } else if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    pwdErrors.newPassword = '密码长度为 6-20 位'
    valid = false
  }
  if (!pwdForm.confirmPassword) {
    pwdErrors.confirmPassword = '请再次输入新密码'
    valid = false
  } else if (pwdForm.confirmPassword !== pwdForm.newPassword) {
    pwdErrors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  return valid
}

/* === 工具函数 === */
function formatDate(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD')
}

function formatTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/* === 修改密码 === */
async function handleChangePassword(): Promise<void> {
  if (!validatePwdForm()) return
  pwdLoading.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    pwdLoading.value = false
  }
}

/** 重置密码表单 */
function resetPwdForm(): void {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdErrors.oldPassword = ''
  pwdErrors.newPassword = ''
  pwdErrors.confirmPassword = ''
}

/* === 基本信息: 编辑模式 === */
/** 进入编辑模式: 用当前 user 值初始化表单 */
function enterEdit(): void {
  if (!user.value) return
  editForm.nickname = user.value.nickname || ''
  editForm.phone = user.value.phone || ''
  editForm.email = user.value.email || ''
  editForm.phoneCode = ''
  editForm.emailCode = ''
  editErrors.nickname = ''
  editErrors.phone = ''
  editErrors.email = ''
  editErrors.phoneCode = ''
  editErrors.emailCode = ''
  editing.value = true
}

/** 取消编辑: 退出编辑模式, 清理验证码与倒计时 */
function cancelEdit(): void {
  editing.value = false
  editErrors.nickname = ''
  editErrors.phone = ''
  editErrors.email = ''
  editErrors.phoneCode = ''
  editErrors.emailCode = ''
  editForm.phoneCode = ''
  editForm.emailCode = ''
  stopPhoneCountdown()
  stopEmailCountdown()
}

/** 启动手机号倒计时 */
function startPhoneCountdown(): void {
  phoneCountdown.value = 60
  stopPhoneCountdown()
  phoneTimer = setInterval(() => {
    phoneCountdown.value--
    if (phoneCountdown.value <= 0) {
      stopPhoneCountdown()
    }
  }, 1000)
}

/** 停止手机号倒计时 */
function stopPhoneCountdown(): void {
  if (phoneTimer) {
    clearInterval(phoneTimer)
    phoneTimer = null
  }
  phoneCountdown.value = 0
}

/** 启动邮箱倒计时 */
function startEmailCountdown(): void {
  emailCountdown.value = 60
  stopEmailCountdown()
  emailTimer = setInterval(() => {
    emailCountdown.value--
    if (emailCountdown.value <= 0) {
      stopEmailCountdown()
    }
  }, 1000)
}

/** 停止邮箱倒计时 */
function stopEmailCountdown(): void {
  if (emailTimer) {
    clearInterval(emailTimer)
    emailTimer = null
  }
  emailCountdown.value = 0
}

/** 发送短信验证码 */
async function handleSendPhoneCode(): Promise<void> {
  const phone = editForm.phone.trim()
  if (!phone) {
    editErrors.phone = '请先输入手机号'
    return
  }
  if (!/^1\d{10}$/.test(phone)) {
    editErrors.phone = '请输入正确的 11 位手机号'
    return
  }
  editErrors.phone = ''
  sendingPhoneCode.value = true
  try {
    await sendSmsCode({ target: phone })
    ElMessage.success('短信验证码已发送')
    startPhoneCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    sendingPhoneCode.value = false
  }
}

/** 发送邮箱验证码 */
async function handleSendEmailCode(): Promise<void> {
  const email = editForm.email.trim()
  if (!email) {
    editErrors.email = '请先输入邮箱'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    editErrors.email = '邮箱格式不正确'
    return
  }
  editErrors.email = ''
  sendingEmailCode.value = true
  try {
    await sendEmailCode({ target: email })
    ElMessage.success('邮箱验证码已发送')
    startEmailCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    sendingEmailCode.value = false
  }
}

/** 校验编辑表单: 手机号 11 位数字, 邮箱格式可选, 变更字段需验证码 */
function validateEditForm(): boolean {
  editErrors.nickname = ''
  editErrors.phone = ''
  editErrors.email = ''
  editErrors.phoneCode = ''
  editErrors.emailCode = ''
  let valid = true

  const phone = editForm.phone.trim()
  if (!phone) {
    editErrors.phone = '请输入手机号'
    valid = false
  } else if (!/^1\d{10}$/.test(phone)) {
    editErrors.phone = '请输入正确的 11 位手机号'
    valid = false
  }

  const email = editForm.email.trim()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    editErrors.email = '邮箱格式不正确'
    valid = false
  }

  // 手机号变更必须填写验证码
  if (isPhoneChanged.value && !editForm.phoneCode.trim()) {
    editErrors.phoneCode = '手机号已变更，请输入验证码'
    valid = false
  }

  // 邮箱变更必须填写验证码
  if (isEmailChanged.value && !editForm.emailCode.trim()) {
    editErrors.emailCode = '邮箱已变更，请输入验证码'
    valid = false
  }

  return valid
}

/** 保存个人信息 (区分变更字段, 走对应验证码接口) */
async function handleSaveProfile(): Promise<void> {
  if (!validateEditForm()) return
  editLoading.value = true
  try {
    const nickname = editForm.nickname.trim()
    const phone = editForm.phone.trim()
    const email = editForm.email.trim()
    const phoneChanged = isPhoneChanged.value
    const emailChanged = isEmailChanged.value

    // 1. 若手机号变更 -> 走验证码接口
    if (phoneChanged) {
      await updatePhone({ phone, code: editForm.phoneCode.trim() })
    }
    // 2. 若邮箱变更 -> 走验证码接口
    if (emailChanged) {
      await updateEmail({ email, code: editForm.emailCode.trim() })
    }
    // 3. 昵称变更 (或未变更但需刷新) -> 走通用 profile 接口 (只传昵称, 避免覆盖手机号/邮箱)
    //    注意: 若手机号/邮箱已通过上面接口更新, 这里只更新昵称即可
    if (nickname !== (user.value?.nickname || '')) {
      await updateProfile({ nickname })
    }
    // 拉取最新用户信息
    await userStore.fetchUserInfo()
    ElMessage.success('修改成功')
    editing.value = false
    stopPhoneCountdown()
    stopEmailCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    editLoading.value = false
  }
}

/* === 拉取用户信息 === */
async function fetchUserInfo(): Promise<void> {
  loading.value = true
  try {
    await userStore.fetchUserInfo()
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})

/* === 组件卸载时清理倒计时定时器 === */
onUnmounted(() => {
  stopPhoneCountdown()
  stopEmailCountdown()
})
</script>

<style scoped>
.profile-page {
  padding-bottom: 24px;
}

.loading-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 20px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-muted) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

/* 严格对照 index.html .profile-layout 样式 */
.profile-layout {
  display: flex;
  gap: 24px;
  padding: 24px;
}

/* 左列用户卡片 */
.profile-card {
  width: 260px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px 24px;
  text-align: center;
}

/* 头像 80px 圆形渐变 */
.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  cursor: pointer;
  position: relative;
}

.profile-name {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 4px;
  color: var(--color-text-primary);
}

.profile-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

/* 角色徽章 */
.role-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
}

.role-badge.admin {
  background: var(--tag-timeout-bg);
  color: var(--tag-timeout-fg);
}

.role-badge.seller {
  background: var(--tag-unpaid-bg);
  color: var(--tag-unpaid-fg);
}

.role-badge.buyer {
  background: var(--tag-completed-bg);
  color: var(--tag-completed-fg);
}

.profile-meta {
  font-size: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  text-align: left;
}

.profile-meta-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.profile-meta-row dt {
  color: var(--color-text-secondary);
}

.profile-meta-row dd {
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 右列主内容 */
.profile-main {
  flex: 1;
  min-width: 0;
}

/* 标签页 */
.profile-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 24px;
}

.profile-tab {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.profile-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* 表单：max-width 420px */
.profile-form {
  max-width: 420px;
}

/* 基本信息: 白底卡片 */
.info-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 24px;
}

/* 修改密码: 白底卡片 (与 .info-card 风格协调, 加轻微阴影提升层次感) */
.pwd-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 字段行: label-value 横向布局, 行间分隔线 */
.info-row {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.info-row:last-of-type {
  border-bottom: none;
}

.info-label {
  flex-shrink: 0;
  width: 88px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  letter-spacing: 0.02em;
  line-height: 24px;
}

.info-value {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  color: var(--color-text-primary);
  line-height: 24px;
  word-break: break-all;
}

.info-value.readonly {
  color: var(--color-text-secondary);
}

/* 编辑模式: input 占满 value 区 */
.info-row .form-input {
  width: 100%;
  height: 36px;
}

.info-edit-cell {
  flex: 1;
  min-width: 0;
}

/* 操作区 */
.info-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

.form-value {
  font-size: 14px;
  padding: 8px 0;
  color: var(--color-text-primary);
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

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.form-tip {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}

/* 小按钮 */
.btn-sm {
  padding: 10px 28px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
}

.btn-sm.primary:disabled {
  background: var(--btn-loading-bg);
  cursor: not-allowed;
}

/* === 左侧快捷入口 === */
.profile-shortcuts {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.shortcut-item:hover {
  color: var(--color-primary);
  background: rgba(229, 57, 53, 0.08);
}

.shortcut-item .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

/* === 验证码行 (输入框 + 发送按钮) === */
.verify-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: flex-start;
}

.verify-input {
  flex: 1;
  min-width: 0;
}

.verify-btn {
  flex-shrink: 0;
  padding: 0 14px;
  height: 36px;
  white-space: nowrap;
  min-width: 110px;
  justify-content: center;
  display: inline-flex;
  align-items: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }

  .profile-card {
    width: 100%;
  }

  .profile-shortcuts {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
