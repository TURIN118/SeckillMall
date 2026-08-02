<template>
  <div class="image-uploader">
    <el-upload
      :file-list="fileList"
      list-type="picture-card"
      :limit="maxCount"
      accept="image/*"
      :before-upload="handleBeforeUpload"
      :http-request="handleHttpRequest"
      :on-remove="handleRemove"
      :on-exceed="handleExceed"
      :on-preview="handlePreview"
      action="#"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 图片上传组件 C05
 * 参照 10-ai-design-spec.md C05 规范
 * 使用 el-upload list-type="picture-card"
 */
import { ref, computed, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadFiles, UploadRequestOptions } from 'element-plus'

interface Props {
  /** 图片 URL 数组 (v-model) */
  modelValue?: string[]
  /** 最大图片数 */
  maxCount?: number
  /** 最大文件大小 (MB) */
  maxSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxCount: 5,
  maxSize: 5
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
}>()

const fileList = ref<UploadFile[]>([])
const previewVisible = ref(false)
const previewIndex = ref(0)

const previewList = computed(() => props.modelValue)

/** 同步 modelValue 到 fileList */
watch(
  () => props.modelValue,
  (urls) => {
    fileList.value = urls.map((url, index) => ({
      name: `image-${index}`,
      url,
      uid: index
    } as UploadFile))
  },
  { immediate: true }
)

/** 上传前校验 */
function handleBeforeUpload(file: File): boolean {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

/** 自定义上传 (占位: 实际项目应上传到 OSS/文件服务) */
function handleHttpRequest(_options: UploadRequestOptions): Promise<unknown> {
  // 占位实现: 实际项目中这里应该上传到 OSS/文件服务并返回 URL
  // 这里使用 FileReader 生成 base64 预览
  const file = _options.file
  return new Promise<unknown>((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const url = e.target?.result as string
      const newUrls = [...props.modelValue, url]
      emit('update:modelValue', newUrls)
      resolve(url)
    }
    reader.readAsDataURL(file)
  })
}

/** 移除图片 */
function handleRemove(file: UploadFile): void {
  const index = fileList.value.findIndex((f) => f.uid === file.uid)
  if (index >= 0) {
    const newUrls = [...props.modelValue]
    newUrls.splice(index, 1)
    emit('update:modelValue', newUrls)
  }
}

/** 超出数量限制 */
function handleExceed(): void {
  ElMessage.warning(`最多上传 ${props.maxCount} 张图片`)
}

/** 预览图片 */
function handlePreview(file: UploadFile): void {
  const url = file.url
  if (url) {
    const index = props.modelValue.indexOf(url)
    previewIndex.value = index >= 0 ? index : 0
    previewVisible.value = true
  }
}
</script>

<style scoped>
.image-uploader :deep(.el-upload--picture-card) {
  width: 80px;
  height: 80px;
}

.image-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 80px;
  height: 80px;
}
</style>