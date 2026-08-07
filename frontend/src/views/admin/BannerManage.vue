<template>
  <div class="banner-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">轮播图列表</div>
        <div class="admin-table-actions">
          <button class="btn-sm primary" @click="openCreateDialog">新增轮播图</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>缩略图</th>
            <th>标题</th>
            <th>跳转链接</th>
            <th>排序</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in bannerList" :key="row.id">
            <td>
              <div class="table-avatar img">
                <img v-if="row.imageUrl" :src="formatImageUrl(row.imageUrl)" alt=""
                  loading="lazy" sizes="60px" />
                <span v-else>无图</span>
              </div>
            </td>
            <td>{{ row.title || '—' }}</td>
            <td class="link-cell">
              <span class="link-text" :title="row.linkUrl || ''">{{ row.linkUrl || '—' }}</span>
            </td>
            <td>{{ row.sortOrder }}</td>
            <td>
              <el-switch :model-value="row.status === 1" :loading="statusLoadingId === row.id"
                @change="handleStatusChange(row)" />
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action-btn" @click="openEditDialog(row)">编辑</button>
                <button class="table-action-btn danger" @click="handleDelete(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="bannerList.length === 0 && !loading">
            <td colspan="6" class="empty-cell">暂无轮播图数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :close-on-click-modal="false" destroy-on-close
      @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入轮播图标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="图片" prop="images">
          <ImageUploader v-model="formData.images" :max-count="1" />
          <span class="form-unit">建议尺寸 1200×400，仅支持 1 张</span>
        </el-form-item>
        <el-form-item label="跳转链接" prop="linkUrl">
          <el-input v-model="formData.linkUrl" placeholder="请输入跳转链接（可留空）" maxlength="500" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :step="1" controls-position="right"
            style="width: 200px" />
          <span class="form-unit">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台轮播图管理
 * 表格 + 新增/编辑弹窗 + 图片上传 + 排序 + 状态切换
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getBannerList,
  createBanner,
  updateBanner,
  deleteBanner,
  updateBannerStatus
} from '@/api/banner'
import type { BannerVO } from '@/types'
import { formatImageUrl } from '@/utils/image'
import ImageUploader from '@/components/ImageUploader.vue'

/* === 列表数据 === */
const loading = ref(false)
const bannerList = ref<BannerVO[]>([])

/* === 拉取轮播图列表 === */
async function fetchBannerList(): Promise<void> {
  loading.value = true
  try {
    const res = await getBannerList()
    bannerList.value = res.data || []
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 弹窗 === */
const dialogVisible = ref(false)
const dialogTitle = ref('新增轮播图')
const editingId = ref<number | string | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

interface BannerFormData {
  title: string
  images: string[]
  linkUrl: string
  sortOrder: number
  status: number
}

const formData = reactive<BannerFormData>({
  title: '',
  images: [],
  linkUrl: '',
  sortOrder: 0,
  status: 1
})

const formRules: FormRules = {
  images: [{ required: true, message: '请上传轮播图图片', trigger: 'change' }]
}

/* === 打开新增弹窗 === */
function openCreateDialog(): void {
  editingId.value = null
  dialogTitle.value = '新增轮播图'
  Object.assign(formData, {
    title: '',
    images: [],
    linkUrl: '',
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

/* === 打开编辑弹窗 === */
function openEditDialog(row: BannerVO): void {
  editingId.value = row.id
  dialogTitle.value = '编辑轮播图'
  Object.assign(formData, {
    title: row.title || '',
    images: row.imageUrl ? [row.imageUrl] : [],
    linkUrl: row.linkUrl || '',
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

/* === 重置表单 === */
function resetForm(): void {
  formRef.value?.resetFields()
  Object.assign(formData, {
    title: '',
    images: [],
    linkUrl: '',
    sortOrder: 0,
    status: 1
  })
  editingId.value = null
}

/* === 提交表单 (新增/编辑) === */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 校验图片必填（images 数组至少 1 张）
  if (!formData.images || formData.images.length === 0) {
    ElMessage.warning('请上传轮播图图片')
    return
  }
  submitting.value = true
  try {
    const payload = {
      title: formData.title,
      imageUrl: formData.images[0],
      linkUrl: formData.linkUrl,
      sortOrder: formData.sortOrder,
      status: formData.status
    }
    if (editingId.value === null) {
      await createBanner(payload)
      ElMessage.success('新增成功')
    } else {
      await updateBanner(editingId.value, payload)
      ElMessage.success('编辑成功')
    }
    dialogVisible.value = false
    await fetchBannerList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

/* === 删除轮播图 === */
async function handleDelete(row: BannerVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除轮播图「${row.title || '未命名'}」吗？此操作不可恢复`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    // 用户取消确认, 静默
    return
  }
  try {
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    await fetchBannerList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === 切换轮播图状态 === */
const statusLoadingId = ref<number | string | null>(null)
async function handleStatusChange(row: BannerVO): Promise<void> {
  const nextStatus = row.status === 1 ? 0 : 1
  statusLoadingId.value = row.id
  try {
    await updateBannerStatus(row.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '已启用' : '已禁用')
    await fetchBannerList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    statusLoadingId.value = null
  }
}

onMounted(() => {
  fetchBannerList()
})
</script>

<style scoped>
/* === 严格对照 index.html .admin-table-wrap === */
.admin-table-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

/* === 严格对照 .admin-table-header === */
.admin-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.admin-table-title {
  font-size: 15px;
  font-weight: 700;
}

.admin-table-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* === 严格对照 .btn-sm === */
.btn-sm {
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 12px;
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

.btn-sm:hover {
  opacity: 0.9;
}

/* === 严格对照 .admin-table === */
.admin-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.admin-table thead th {
  background: var(--color-bg-subtle);
  padding: 10px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 12px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 0.02em;
}

.admin-table tbody td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}

.admin-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.admin-table tbody tr:last-child td {
  border-bottom: none;
}

.empty-cell {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 40px 16px;
}

/* === 缩略图 === */
.table-avatar.img {
  width: 80px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.table-avatar.img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* === 跳转链接单元格 === */
.link-cell {
  max-width: 240px;
}

.link-text {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

/* === 严格对照 .table-actions / .table-action-btn === */
.table-actions {
  display: flex;
  gap: 8px;
}

.table-action-btn {
  font-size: 12px;
  color: var(--color-primary-blue);
  cursor: pointer;
  background: none;
  border: none;
  font-weight: 600;
  padding: 0;
}

.table-action-btn.danger {
  color: var(--color-danger);
}

.table-action-btn:hover {
  text-decoration: underline;
}

/* === 表单单位 === */
.form-unit {
  margin-left: 8px;
  color: var(--color-text-secondary);
  font-size: 12px;
}
</style>