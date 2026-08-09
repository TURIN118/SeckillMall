<!--
  修改密码页（对齐 spec.md 3.10 密码修改 / tasks.md T5.2）
  - 旧密码 / 新密码 / 确认密码
  - 图形验证码（复用 CaptchaImage 组件）
  - 调用 authApi.changePassword()
-->
<template>
  <view class="edit-password-page">
    <u-form
      ref="formRef"
      :model="form"
      :rules="rules"
      labelPosition="top"
      :labelWidth="100"
      errorType="toast"
    >
      <u-form-item label="旧密码" prop="oldPassword" required>
        <u-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入旧密码"
          maxlength="32"
          :password-icon="true"
          clearable
        />
      </u-form-item>

      <u-form-item label="新密码" prop="newPassword" required>
        <u-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码（6-32位）"
          maxlength="32"
          :password-icon="true"
          clearable
        />
      </u-form-item>

      <u-form-item label="确认新密码" prop="confirmPassword" required>
        <u-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          maxlength="32"
          :password-icon="true"
          clearable
        />
      </u-form-item>

      <u-form-item label="图形验证码" prop="captchaCode" required>
        <view class="captcha-row">
          <u-input
            v-model="form.captchaCode"
            placeholder="请输入图形验证码"
            maxlength="6"
            clearable
          />
          <CaptchaImage
            ref="captchaRef"
            width="200rpx"
            height="72rpx"
            @update:captchaKey="onCaptchaKeyUpdate"
          />
        </view>
      </u-form-item>
    </u-form>

    <!-- 密码强度提示 -->
    <view class="tips">
      <text class="tips-title">密码要求：</text>
      <text class="tips-content">• 长度 6-32 位</text>
      <text class="tips-content">• 建议包含字母、数字、特殊字符</text>
      <text class="tips-content">• 不可与旧密码相同</text>
    </view>

    <!-- 底部提交按钮 -->
    <view class="footer-bar">
      <u-button
        type="error"
        shape="circle"
        :loading="submitting"
        @click="onSubmit"
      >确认修改</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import * as authApi from '@/api/auth'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showLoading, hideLoading } from '@/utils/toast'
import CaptchaImage from '@/components/CaptchaImage.vue'

const formRef = ref<any>(null)
const captchaRef = ref<InstanceType<typeof CaptchaImage> | null>(null)
const submitting = ref<boolean>(false)

/** 表单数据 */
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  captchaCode: '',
  captchaKey: ''
})

/** 校验规则 */
const rules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: ['blur', 'change'] }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: ['blur', 'change'] },
    {
      validator: (val: string) => val.length >= 6 && val.length <= 32,
      message: '密码长度需 6-32 位',
      trigger: ['blur']
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: ['blur', 'change'] },
    {
      validator: (val: string) => val === form.newPassword,
      message: '两次输入的密码不一致',
      trigger: ['blur']
    }
  ],
  captchaCode: [
    { required: true, message: '请输入图形验证码', trigger: ['blur', 'change'] }
  ]
}

onLoad(() => {
  if (!requireAuthAsync()) return
})

/** 验证码 key 更新回调 */
function onCaptchaKeyUpdate(key: string) {
  form.captchaKey = key
}

/** 提交修改密码 */
async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }

  // 二次校验验证码 key
  if (!form.captchaKey) {
    showToast('请先获取图形验证码', 'none')
    return
  }

  // 二次校验新旧密码不同
  if (form.oldPassword === form.newPassword) {
    showToast('新密码不能与旧密码相同', 'none')
    return
  }

  submitting.value = true
  try {
    showLoading('提交中...')
    await authApi.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      captchaCode: form.captchaCode,
      captchaKey: form.captchaKey
    })
    showToast('密码修改成功', 'success')
    // 修改成功后返回上一页
    setTimeout(() => {
      navigate.back()
    }, 1000)
  } catch (e: any) {
    console.error('修改密码失败', e)
    // 验证码错误时刷新验证码并清空验证码输入
    const msg = e?.message || '修改失败，请重试'
    showToast(msg, 'error')
    form.captchaCode = ''
    // 刷新图形验证码
    captchaRef.value?.refresh?.()
  } finally {
    submitting.value = false
    hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.edit-password-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 24rpx 32rpx 160rpx;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  width: 100%;

  /* u-input 占满剩余空间 */
  > :first-child {
    flex: 1;
  }
}

.tips {
  margin-top: 32rpx;
  padding: 24rpx;
  background-color: #fffbe6;
  border: 1rpx solid #ffe58f;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;

  .tips-title {
    font-size: 26rpx;
    color: #d48806;
    font-weight: bold;
  }

  .tips-content {
    font-size: 24rpx;
    color: #8c8c8c;
    line-height: 1.5;
  }
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);
}
</style>