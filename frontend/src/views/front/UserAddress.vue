<template>
  <!-- 收货地址管理页面：卡片网格布局 (el-row + el-col 响应式) -->
  <div class="address-page">
    <!-- 页头：标题 + 新增按钮 -->
    <div class="address-header">
      <h2 class="address-title">收货地址管理</h2>
      <button class="btn-sm primary" type="button" @click="openAddDialog">
        <span class="btn-plus">+</span> 新增地址
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span class="loading-text">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="addressList.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
          <circle cx="12" cy="10" r="3" />
        </svg>
      </div>
      <p class="empty-text">还没有收货地址，快去添加吧！</p>
      <button class="btn-sm primary" type="button" @click="openAddDialog">新增地址</button>
    </div>

    <!-- 地址列表：响应式卡片网格 -->
    <el-row v-else :gutter="16" class="address-grid">
      <el-col
        v-for="addr in addressList"
        :key="addr.id"
        :xs="24"
        :sm="24"
        :md="12"
        :lg="8"
        :xl="8"
        class="address-col"
      >
        <div
          class="address-card"
          :class="{ 'address-default': addr.isDefault === 1 }"
        >
          <!-- 顶部：姓名 + 手机号 + 默认标签 -->
          <div class="card-top">
            <div class="card-name-row">
              <span class="address-name">{{ addr.receiverName }}</span>
              <span class="address-phone">{{ addr.receiverPhone }}</span>
              <el-tag
                v-if="addr.isDefault === 1"
                type="danger"
                size="small"
                effect="dark"
                class="default-tag"
              >默认</el-tag>
            </div>
          </div>

          <!-- 中部：详细地址 -->
          <div class="card-detail">
            <el-icon class="detail-icon"><Location /></el-icon>
            <span class="detail-text">
              {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detailAddress }}
            </span>
          </div>

          <!-- 底部：操作按钮 -->
          <div class="card-actions">
            <button
              v-if="addr.isDefault !== 1"
              class="btn-sm text"
              type="button"
              @click="handleSetDefault(addr)"
            >设为默认</button>
            <button class="btn-sm text" type="button" @click="openEditDialog(addr)">编辑</button>
            <button class="btn-sm text danger" type="button" @click="handleDelete(addr)">删除</button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="姓名" prop="receiverName">
          <el-input v-model="formData.receiverName" placeholder="请输入收货人姓名" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="formData.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="formData.province" placeholder="如：北京市" maxlength="30" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="formData.city" placeholder="如：北京市" maxlength="30" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="formData.district" placeholder="如：海淀区" maxlength="30" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="formData.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址（街道、门牌号等）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="formData.isDefault" />
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
 * 收货地址管理页面 (前台)
 * 对接后端 /api/v1/addresses 接口，无模拟数据。
 * 布局：响应式卡片网格 (el-row + el-col)，大屏一行3个、中屏一行2个、小屏一行1个。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Loading, Location } from '@element-plus/icons-vue'
import {
  getAddressList,
  createAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '@/api/address'
import type { UserAddressVO, UserAddressRequest } from '@/types'

/** 地址列表 (后端返回) */
const addressList = ref<UserAddressVO[]>([])

/** 列表加载中 */
const loading = ref<boolean>(false)

/* === 弹窗状态 === */
const dialogVisible = ref<boolean>(false)
const dialogTitle = ref<string>('新增地址')
const submitting = ref<boolean>(false)
const editingId = ref<number | null>(null)

const formRef = ref<FormInstance | null>(null)

/** 表单数据 (isDefault 在表单中用 boolean, 提交时转 0/1) */
const formData = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const formRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

/** 加载地址列表 */
async function loadAddressList(): Promise<void> {
  loading.value = true
  try {
    const res = await getAddressList()
    addressList.value = res.data ?? []
  } catch {
    // 错误信息已由请求拦截器统一提示
    addressList.value = []
  } finally {
    loading.value = false
  }
}

/** 重置表单 */
function resetForm(): void {
  formData.receiverName = ''
  formData.receiverPhone = ''
  formData.province = ''
  formData.city = ''
  formData.district = ''
  formData.detailAddress = ''
  formData.isDefault = false
  editingId.value = null
  formRef.value?.clearValidate()
}

/** 打开新增弹窗 */
function openAddDialog(): void {
  dialogTitle.value = '新增地址'
  resetForm()
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEditDialog(addr: UserAddressVO): void {
  dialogTitle.value = '编辑地址'
  editingId.value = addr.id
  formData.receiverName = addr.receiverName
  formData.receiverPhone = addr.receiverPhone
  formData.province = addr.province
  formData.city = addr.city
  formData.district = addr.district
  formData.detailAddress = addr.detailAddress
  formData.isDefault = addr.isDefault === 1
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 构造提交载荷 (isDefault: boolean → number 0/1) */
function buildPayload(): UserAddressRequest {
  return {
    receiverName: formData.receiverName.trim(),
    receiverPhone: formData.receiverPhone.trim(),
    province: formData.province.trim(),
    city: formData.city.trim(),
    district: formData.district.trim(),
    detailAddress: formData.detailAddress.trim(),
    isDefault: formData.isDefault ? 1 : 0
  }
}

/** 提交表单 (新增 / 编辑) */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    if (editingId.value === null) {
      // 新增
      await createAddress(payload)
      ElMessage.success('地址添加成功')
    } else {
      // 编辑
      await updateAddress(editingId.value, payload)
      ElMessage.success('地址修改成功')
    }
    dialogVisible.value = false
    resetForm()
    // 重新拉取列表，保证与后端一致 (默认地址互斥逻辑由后端处理)
    await loadAddressList()
  } catch {
    // 错误信息已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

/** 删除地址 */
async function handleDelete(addr: UserAddressVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该收货地址吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }

  try {
    await deleteAddress(addr.id)
    ElMessage.success('地址已删除')
    await loadAddressList()
  } catch {
    // 错误信息已由请求拦截器统一提示
  }
}

/** 设为默认地址 */
async function handleSetDefault(addr: UserAddressVO): Promise<void> {
  try {
    await setDefaultAddress(addr.id)
    ElMessage.success('已设为默认地址')
    await loadAddressList()
  } catch {
    // 错误信息已由请求拦截器统一提示
  }
}

// 页面挂载时加载地址列表
onMounted(() => {
  loadAddressList()
})
</script>

<style scoped>
/* 页面容器 */
.address-page {
  padding: 24px;
}

/* 页头 */
.address-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.address-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

/* 加载中状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
}

.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

/* === 卡片网格布局 === */
.address-grid {
  /* el-row 默认有 margin-left/right: -8px (gutter/2)，这里消除两侧负边距对齐 */
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.address-col {
  /* 每个卡片下方留出间距，配合 gutter 控制水平间距 */
  margin-bottom: 16px;
}

/* 地址卡片：白色底色、圆角、边框、阴影、hover 上移效果 */
.address-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  transition: box-shadow 0.25s ease, transform 0.25s ease, border-color 0.25s ease;
  box-sizing: border-box;
}

/* hover 效果：阴影加深 + 轻微上移 */
.address-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

/* 默认地址卡片：红色边框高亮 */
.address-card.address-default {
  border-color: var(--color-primary);
  box-shadow: 0 2px 12px rgba(229, 57, 53, 0.15);
}

.address-card.address-default:hover {
  box-shadow: 0 8px 24px rgba(229, 57, 53, 0.25);
}

/* 卡片顶部：姓名行 */
.card-top {
  display: flex;
  flex-direction: column;
}

.card-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.address-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.address-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 默认标签 (el-tag 已自带样式，这里微调) */
.default-tag {
  margin-left: auto;
  font-weight: 700;
}

/* 卡片中部：详细地址 */
.card-detail {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  min-height: 42px;
}

.detail-icon {
  font-size: 15px;
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.detail-text {
  flex: 1;
  word-break: break-all;
  /* 限制两行显示，超出省略 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 卡片底部：操作按钮 */
.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-light);
  margin-top: auto;
}

/* === 小按钮样式 === */
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
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
  border-color: var(--btn-hover);
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  padding: 5px 10px;
}

.btn-sm.text:hover {
  color: var(--color-primary);
}

.btn-sm.text.danger:hover {
  color: var(--color-primary);
}

.btn-plus {
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

/* === 响应式：小屏下调整间距 === */
@media (max-width: 768px) {
  .address-page {
    padding: 16px;
  }

  .address-col {
    margin-bottom: 12px;
  }

  .address-card {
    padding: 14px;
  }
}
</style>
