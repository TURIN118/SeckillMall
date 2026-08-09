<!--
  修改资料页（对齐 spec.md 3.10 个人中心 / tasks.md T5.2）
  - 昵称 / 邮箱 / 手机 / 性别 / 生日 / 头像
  - 头像上传：uni.chooseImage + uploadApi.uploadAvatar
  - 调用 authApi.updateProfile() 提交
  - 提交成功后更新 userStore.userInfo
-->
<template>
  <view class="edit-profile-page">
    <u-form
      ref="formRef"
      :model="form"
      :rules="rules"
      labelPosition="left"
      :labelWidth="160"
      errorType="toast"
    >
      <!-- 头像 -->
      <u-form-item label="头像" prop="avatar">
        <view class="avatar-row" @tap="onChooseAvatar">
          <u-image
            :src="avatarDisplay"
            mode="aspectFill"
            width="120rpx"
            height="120rpx"
            shape="circle"
            :lazy-load="true"
          />
          <u-icon name="camera-fill" size="40" color="#909399" />
        </view>
      </u-form-item>

      <!-- 昵称 -->
      <u-form-item label="昵称" prop="nickname" required>
        <u-input
          v-model="form.nickname"
          placeholder="请输入昵称"
          maxlength="20"
          clearable
        />
      </u-form-item>

      <!-- 邮箱 -->
      <u-form-item label="邮箱" prop="email">
        <u-input
          v-model="form.email"
          placeholder="请输入邮箱"
          maxlength="50"
          clearable
        />
      </u-form-item>

      <!-- 手机号 -->
      <u-form-item label="手机号" prop="phone">
        <u-input
          v-model="form.phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
          clearable
        />
      </u-form-item>

      <!-- 性别 -->
      <u-form-item label="性别" prop="gender">
        <u-radio-group v-model="form.gender">
          <u-radio :name="1" label="男" shape="circle" />
          <u-radio :name="2" label="女" shape="circle" />
          <u-radio :name="0" label="保密" shape="circle" />
        </u-radio-group>
      </u-form-item>

      <!-- 生日 -->
      <u-form-item label="生日" prop="birthday">
        <view class="birthday-row" @tap="showBirthdayPicker = true">
          <text
            class="birthday-text"
            :class="{ placeholder: !form.birthday }"
          >{{ form.birthday || '请选择生日' }}</text>
          <u-icon name="arrow-right" size="28" color="#909399" />
        </view>
      </u-form-item>
    </u-form>

    <!-- 底部保存按钮 -->
    <view class="footer-bar">
      <u-button
        type="error"
        shape="circle"
        :loading="submitting"
        @click="onSubmit"
      >保存修改</u-button>
    </view>

    <!-- 生日选择器 -->
    <u-datetime-picker
      :show="showBirthdayPicker"
      mode="date"
      :value="birthdayTimestamp"
      @confirm="onBirthdayConfirm"
      @cancel="showBirthdayPicker = false"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'
import * as uploadApi from '@/api/upload'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showLoading, hideLoading } from '@/utils/toast'
import type { UpdateProfileRequest } from '@/types'

const userStore = useUserStore()
const formRef = ref<any>(null)
const submitting = ref<boolean>(false)
const showBirthdayPicker = ref<boolean>(false)
const avatarUploading = ref<boolean>(false)

/** 表单数据 */
const form = reactive<{
  nickname: string
  email: string
  phone: string
  gender: 0 | 1 | 2
  birthday: string
  avatar: string
}>({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: '',
  avatar: ''
})

/** 校验规则 */
const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: ['blur', 'change'] }
  ],
  email: [
    {
      validator: (val: string) => !val || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val),
      message: '邮箱格式不正确',
      trigger: ['blur']
    }
  ],
  phone: [
    {
      validator: (val: string) => !val || /^1[3-9]\d{9}$/.test(val),
      message: '手机号格式不正确',
      trigger: ['blur']
    }
  ]
}

/** 头像显示（上传中显示 loading 文案） */
const avatarDisplay = computed(() => {
  if (avatarUploading.value) return ''
  return form.avatar || '/static/default-avatar.png'
})

/** 生日时间戳（传给 u-datetime-picker） */
const birthdayTimestamp = computed(() => {
  if (!form.birthday) return Date.now()
  return new Date(form.birthday).getTime()
})

onLoad(async () => {
  if (!requireAuthAsync()) return
  // 拉取最新用户信息填充表单
  try {
    showLoading('加载中...')
    await userStore.fetchUserInfo()
    const info = userStore.userInfo
    if (info) {
      form.nickname = info.nickname || ''
      form.email = info.email || ''
      form.phone = info.phone || ''
      form.gender = info.gender ?? 0
      form.birthday = info.birthday ? info.birthday.slice(0, 10) : ''
      form.avatar = info.avatar || ''
    }
  } catch (e) {
    console.error('拉取用户信息失败', e)
    showToast('用户信息加载失败', 'error')
  } finally {
    hideLoading()
  }
})

/** 选择头像 */
function onChooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePath = res.tempFilePaths[0]
      if (!tempFilePath) return
      await uploadAvatar(tempFilePath)
    },
    fail: (err) => {
      // 用户取消选择不提示
      if (err.errMsg && err.errMsg.indexOf('cancel') === -1) {
        console.warn('chooseImage 失败', err)
      }
    }
  })
}

/** 上传头像 */
async function uploadAvatar(filePath: string) {
  if (avatarUploading.value) return
  avatarUploading.value = true
  try {
    showLoading('上传中...')
    const res = await uploadApi.uploadAvatar(filePath)
    if (res?.url) {
      form.avatar = res.url
      showToast('头像上传成功', 'success')
    } else {
      showToast('头像上传失败', 'error')
    }
  } catch (e) {
    console.error('头像上传失败', e)
    showToast('头像上传失败', 'error')
  } finally {
    avatarUploading.value = false
    hideLoading()
  }
}

/** 生日确认 */
function onBirthdayConfirm(e: any) {
  // e.value 为时间戳
  const date = new Date(e.value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  form.birthday = `${year}-${month}-${day}`
  showBirthdayPicker.value = false
}

/** 提交保存 */
async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    showLoading('保存中...')
    const payload: UpdateProfileRequest = {
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
      gender: form.gender,
      birthday: form.birthday,
      avatar: form.avatar
    }
    const updated = await authApi.updateProfile(payload)
    // 更新 store 中的用户信息
    if (updated && userStore.userInfo) {
      userStore.userInfo.nickname = updated.nickname || form.nickname
      userStore.userInfo.email = updated.email || form.email
      userStore.userInfo.phone = updated.phone || form.phone
      userStore.userInfo.gender = updated.gender ?? form.gender
      userStore.userInfo.birthday = updated.birthday || form.birthday
      userStore.userInfo.avatar = updated.avatar || form.avatar
    }
    showToast('保存成功', 'success')
    setTimeout(() => {
      navigate.back()
    }, 800)
  } catch (e) {
    console.error('保存资料失败', e)
    showToast('保存失败，请重试', 'error')
  } finally {
    submitting.value = false
    hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.edit-profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 24rpx 32rpx 160rpx;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 0;
}

.birthday-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 64rpx;

  .birthday-text {
    font-size: 30rpx;
    color: #303133;

    &.placeholder {
      color: #c0c4cc;
    }
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