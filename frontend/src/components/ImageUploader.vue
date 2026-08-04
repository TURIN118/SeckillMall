<template>
  <div class="image-uploader">
    <el-upload :file-list="fileList" list-type="picture-card" :limit="maxCount" accept="image/*"
      :before-upload="handleBeforeUpload" :http-request="handleHttpRequest" :on-remove="handleRemove"
      :on-exceed="handleExceed" :on-preview="handlePreview" action="#">
      <el-icon>
        <Plus />
      </el-icon>
    </el-upload>

    <!-- 图片预览 -->
    <el-image-viewer v-if="previewVisible" :url-list="previewList" :initial-index="previewIndex"
      @close="previewVisible = false" />
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
import { uploadImage } from '@/api/upload'
import { formatImageUrl } from '@/utils/image'

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

/**
 * 预览列表：将后端返回的相对路径拼接 baseURL 得到完整 URL，供 el-image-viewer 显示。
 * 注意：props.modelValue 始终保存后端原样返回的相对路径（如 /images/products/xxx），
 * 不被 formatImageUrl 污染，避免提交给后端时携带 host。
 */
const previewList = computed(() => props.modelValue.map((url) => formatImageUrl(url)))

/**
 * 同步 modelValue 到 fileList。
 * el-upload 缩略图需要完整 URL 才能正确加载图片，故此处用 formatImageUrl 拼接 baseURL；
 * 而 modelValue 仍保持后端返回的相对路径，确保提交给后端的数据纯净。
 */
watch(
  () => props.modelValue,
  (urls) => {
    fileList.value = urls.map((url, index) => ({
      name: `image-${index}`,
      url: formatImageUrl(url),
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

/**
 * 自定义上传：调用后端 /api/v1/upload/image 接口，存储文件并返回 URL 路径。
 * - emit 给父组件的 modelValue 保持后端原样返回的相对路径（如 /images/products/xxx），保证数据纯净；
 * - 返回给 el-upload 的响应体 url 用 formatImageUrl 拼接完整 URL，供缩略图显示。
 */
async function handleHttpRequest(options: UploadRequestOptions): Promise<unknown> {
  const file = options.file as File
  try {
    const res = await uploadImage(file, 'products')
    const url = res.data.url
    // modelValue 保持相对路径
    const newUrls = [...props.modelValue, url]
    emit('update:modelValue', newUrls)
    // 返回完整 URL 供 el-upload 显示缩略图
    return { url: formatImageUrl(url) }
  } catch (error) {
    ElMessage.error('图片上传失败')
    throw error
  }
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
    // file.url 已是完整 URL，故在 previewList（同样为完整 URL）中查找索引
    const index = previewList.value.indexOf(url)
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