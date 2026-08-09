<!--
  图形验证码组件（对齐 spec.md 3.12/3.13/3.14 + 4.4 验证码适配）
  - 调用 authApi.getCaptcha() 获取 { img: base64, key: string }
  - <image :src="base64"> 渲染，点击重新获取
  - emit 'update:captchaKey' 给父组件同步验证码 key
  - emit 'refresh' 刷新完成事件
-->
<template>
  <view class="captcha-image" @tap="refresh">
    <image
      v-if="captchaImg"
      class="captcha-img"
      :src="captchaImg"
      mode="scaleToFill"
      :style="{ width: width, height: height }"
    />
    <view
      v-else
      class="captcha-placeholder"
      :style="{ width: width, height: height }"
    >
      <text class="placeholder-text">点击加载</text>
    </view>
    <view v-if="loading" class="captcha-loading" :style="{ width: width, height: height }">
      <text class="loading-text">加载中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as authApi from '@/api/auth'
import { showToast } from '@/utils/toast'

interface Props {
  /** 图片宽度（rpx 或 px 字符串） */
  width?: string
  /** 图片高度（rpx 或 px 字符串） */
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  width: '200rpx',
  height: '80rpx'
})

const emit = defineEmits<{
  (e: 'update:captchaKey', key: string): void
  (e: 'refresh', key: string): void
}>()

const captchaImg = ref<string>('')
const captchaKey = ref<string>('')
const loading = ref<boolean>(false)

/** 拼接 base64 前缀（后端返回纯 base64 字符串，需拼接 data:image/png;base64, 前缀） */
function buildBase64Src(base64: string): string {
  if (!base64) return ''
  // 已带前缀直接返回
  if (base64.startsWith('data:image')) return base64
  return `data:image/png;base64,${base64}`
}

/** 刷新图形验证码 */
async function refresh(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const res = await authApi.getCaptcha()
    captchaImg.value = buildBase64Src(res.img)
    captchaKey.value = res.key
    // 同步 key 给父组件（支持 v-model:captchaKey 双向绑定）
    emit('update:captchaKey', res.key)
    emit('refresh', res.key)
  } catch (e) {
    console.error('获取图形验证码失败', e)
    showToast('验证码获取失败，请点击重试', 'none')
    captchaImg.value = ''
    captchaKey.value = ''
    emit('update:captchaKey', '')
  } finally {
    loading.value = false
  }
}

/** 暴露给父组件：手动刷新 */
defineExpose({ refresh, captchaKey })

onMounted(() => {
  refresh()
})
</script>

<style lang="scss" scoped>
.captcha-image {
  position: relative;
  display: inline-block;
  border-radius: $radius-sm;
  overflow: hidden;
  background-color: #f5f5f5;
}

.captcha-img {
  display: block;
  border-radius: $radius-sm;
}

.captcha-placeholder,
.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border: 1rpx solid $border-color;
  border-radius: $radius-sm;
}

.placeholder-text,
.loading-text {
  font-size: 24rpx;
  color: $text-color-secondary;
}

.captcha-loading {
  position: absolute;
  top: 0;
  left: 0;
  background-color: rgba(255, 255, 255, 0.8);
}
</style>