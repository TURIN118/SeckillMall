<template>
  <div class="coupon-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏 -->
      <div class="admin-table-header">
        <div class="admin-table-title">优惠券管理</div>
        <div class="admin-table-actions">
          <button class="btn-sm primary" @click="openCreateDialog">新增优惠券</button>
        </div>
      </div>

      <!-- 表格 -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>优惠券名称</th>
            <th>类型</th>
            <th>面额</th>
            <th>最低消费</th>
            <th>已领/总数</th>
            <th>已用</th>
            <th>状态</th>
            <th>有效期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in couponList" :key="row.id">
            <td class="coupon-name-cell">{{ row.name }}</td>
            <td>
              <span class="status-tag" :class="row.type === 'AMOUNT' ? 'paid' : 'completed'">
                {{ row.type === 'AMOUNT' ? '满减' : '折扣' }}
              </span>
            </td>
            <td>
              <template v-if="row.type === 'AMOUNT'">¥ {{ formatMoney(row.amount) }}</template>
              <template v-else>{{ formatDiscount(row.amount) }} 折</template>
            </td>
            <td>¥ {{ formatMoney(row.minAmount) }}</td>
            <td>{{ row.receivedCount }} / {{ row.totalCount }}</td>
            <td>{{ row.usedCount }}</td>
            <td>
              <el-switch :model-value="row.status === 1" :loading="statusLoadingId === row.id"
                @change="handleStatusChange(row)" />
            </td>
            <td class="time-cell">
              {{ formatDate(row.startTime) }}<br />~ {{ formatDate(row.endTime) }}
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action-btn" @click="openEditDialog(row)">编辑</button>
                <button class="table-action-btn" @click="openDistributeDialog(row)">发放</button>
                <button class="table-action-btn" @click="openRecordsDialog(row)">领取记录</button>
                <button class="table-action-btn danger" @click="handleDelete(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="couponList.length === 0 && !loading">
            <td colspan="9" class="empty-cell">暂无优惠券数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 表尾分页 -->
      <div class="admin-table-footer">
        <span class="page-info">共 {{ total }} 条记录</span>
        <div class="pagination">
          <div class="page-btn" :class="{ disabled: pageNum <= 1 }" @click="handlePageChange(pageNum - 1)">&lt;</div>
          <div v-for="p in displayPages" :key="p" class="page-btn" :class="{ active: p === pageNum }"
            @click="handlePageChange(p)">{{ p }}</div>
          <div class="page-btn" :class="{ disabled: pageNum >= totalPages }" @click="handlePageChange(pageNum + 1)">&gt;
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false" destroy-on-close
      @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model.trim="formData.name" placeholder="请输入优惠券名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio value="AMOUNT">满减券</el-radio>
            <el-radio value="DISCOUNT">折扣券</el-radio>
          </el-radio-group>
          <div class="form-tip">
            <template v-if="formData.type === 'AMOUNT'">满减券：满【最低消费金额】元减【减免金额】元</template>
            <template v-else>折扣券：【折扣率】表示折扣率，如 8.5 表示 8.5 折</template>
          </div>
        </el-form-item>
        <el-form-item :label="formData.type === 'AMOUNT' ? '减免金额' : '折扣率'" prop="amount">
          <el-input-number v-model="formData.amount" :min="formData.type === 'AMOUNT' ? 0.01 : 0.1"
            :max="formData.type === 'AMOUNT' ? 99999 : 9.9" :step="formData.type === 'AMOUNT' ? 1 : 0.1"
            :precision="formData.type === 'AMOUNT' ? 2 : 1" controls-position="right" style="width: 200px" />
          <span class="form-unit">{{ formData.type === 'AMOUNT' ? '元' : '折' }}</span>
        </el-form-item>
        <el-form-item label="最低消费" prop="minAmount">
          <el-input-number v-model="formData.minAmount" :min="0" :step="1" :precision="2" controls-position="right"
            style="width: 200px" />
          <span class="form-unit">元 (0 表示无门槛)</span>
        </el-form-item>
        <el-form-item label="发放总数" prop="totalCount">
          <el-input-number v-model="formData.totalCount" :min="1" :step="1" :precision="0" controls-position="right"
            style="width: 200px" />
          <span class="form-unit">张</span>
        </el-form-item>
        <el-form-item label="生效时间" prop="startTime">
          <el-date-picker v-model="formData.startTime" type="datetime" placeholder="选择开始时间" format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效时间" prop="endTime">
          <el-date-picker v-model="formData.endTime" type="datetime" placeholder="选择结束时间" format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发放弹窗 -->
    <el-dialog v-model="distributeVisible" title="发放优惠券" width="420px" :close-on-click-modal="false" destroy-on-close>
      <div v-if="distributeRow" class="distribute-content">
        <div class="distribute-info">
          <span class="info-label">优惠券：</span>
          <span class="info-value">{{ distributeRow.name }}</span>
        </div>
        <div class="distribute-info">
          <span class="info-label">已领：</span>
          <span class="info-value">{{ distributeRow.receivedCount }} / {{ distributeRow.totalCount }}</span>
        </div>
        <el-form ref="distributeFormRef" :model="distributeForm" :rules="distributeRules" label-width="80px"
          style="margin-top: 16px">
          <el-form-item label="用户" prop="userId">
            <el-select v-model="distributeForm.userId" filterable remote :remote-method="searchUser"
              :loading="userLoading" placeholder="请输入用户名搜索" style="width: 100%">
              <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username} (ID: ${u.id})`" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="distributeVisible = false">取消</el-button>
        <el-button type="primary" :loading="distributeSubmitting" @click="handleDistribute">确认发放</el-button>
      </template>
    </el-dialog>

    <!-- 领取记录弹窗 -->
    <el-dialog v-model="recordsVisible" :title="recordsTitle" width="780px" :close-on-click-modal="false"
      destroy-on-close @closed="resetRecords">
      <div v-if="recordsRow" class="records-info">
        <span class="info-label">优惠券：</span>
        <span class="info-value">{{ recordsRow.name }}</span>
        <span class="info-divider">|</span>
        <span class="info-label">已领：</span>
        <span class="info-value">{{ recordsRow.receivedCount }} / {{ recordsRow.totalCount }}</span>
      </div>
      <table class="admin-table" v-loading="recordsLoading" style="margin-top: 12px">
        <thead>
          <tr>
            <th>用户名</th>
            <th>状态</th>
            <th>领取时间</th>
            <th>使用时间</th>
            <th>订单ID</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in recordsList" :key="r.id">
            <td class="coupon-name-cell">{{ r.username || `用户 ${r.userId}` }}</td>
            <td>
              <span class="status-tag" :class="recordsStatusClass(r.status)">
                {{ recordsStatusText(r.status) }}
              </span>
            </td>
            <td class="time-cell">{{ formatDate(r.receiveTime) }}</td>
            <td class="time-cell">{{ r.useTime ? formatDate(r.useTime) : '—' }}</td>
            <td>{{ r.orderId || '—' }}</td>
          </tr>
          <tr v-if="recordsList.length === 0 && !recordsLoading">
            <td colspan="5" class="empty-cell">暂无领取记录</td>
          </tr>
        </tbody>
      </table>
      <div class="admin-table-footer">
        <span class="page-info">共 {{ recordsTotal }} 条记录</span>
        <div class="pagination">
          <div class="page-btn" :class="{ disabled: recordsPageNum <= 1 }"
            @click="handleRecordsPageChange(recordsPageNum - 1)">&lt;</div>
          <div v-for="p in recordsDisplayPages" :key="p" class="page-btn"
            :class="{ active: p === recordsPageNum }" @click="handleRecordsPageChange(p)">{{ p }}</div>
          <div class="page-btn" :class="{ disabled: recordsPageNum >= recordsTotalPages }"
            @click="handleRecordsPageChange(recordsPageNum + 1)">&gt;</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台优惠券管理
 * 表格列表 + 新增/编辑弹窗 + 启停 + 发放 + 删除
 * 严格对照 index.html .admin-table-wrap 样式
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import {
  adminGetCouponList,
  adminCreateCoupon,
  adminUpdateCoupon,
  adminDeleteCoupon,
  adminUpdateCouponStatus,
  adminDistributeCoupon,
  adminGetCouponRecords
} from '@/api/coupon'
import { getUserList } from '@/api/admin'
import type { CouponVO, CouponType, CouponRequest, AdminCouponRecordVO, UserVO } from '@/types'

/* === 列表数据 === */
const loading = ref<boolean>(false)
const couponList = ref<CouponVO[]>([])
const total = ref<number>(0)
const pageNum = ref<number>(1)
const pageSize = ref<number>(10)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const displayPages = computed<number[]>(() => {
  const pages: number[] = []
  const t = totalPages.value
  let start = Math.max(1, pageNum.value - 2)
  const end = Math.min(t, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

/* === 拉取优惠券列表 === */
async function fetchCouponList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminGetCouponList(pageNum.value, pageSize.value)
    couponList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchCouponList()
}

/* === 工具函数 === */
function formatMoney(value: number): string {
  return Number(value || 0).toFixed(2)
}

function formatDiscount(value: number): string {
  const num = Number(value || 0)
  return num % 1 === 0 ? String(num) : num.toFixed(1)
}

function formatDate(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

/* === 新增/编辑弹窗 === */
const dialogVisible = ref<boolean>(false)
const dialogTitle = ref<string>('新增优惠券')
const editingId = ref<number | string | null>(null)
const submitting = ref<boolean>(false)
const formRef = ref<FormInstance | null>(null)

interface CouponFormData {
  name: string
  type: CouponType
  amount: number
  minAmount: number
  totalCount: number
  startTime: string
  endTime: string
  status: number
}

const formData = reactive<CouponFormData>({
  name: '',
  type: 'AMOUNT',
  amount: 10,
  minAmount: 100,
  totalCount: 100,
  startTime: '',
  endTime: '',
  status: 1
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入面额', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发放总数', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

function openCreateDialog(): void {
  editingId.value = null
  dialogTitle.value = '新增优惠券'
  Object.assign(formData, {
    name: '',
    type: 'AMOUNT',
    amount: 10,
    minAmount: 100,
    totalCount: 100,
    startTime: '',
    endTime: '',
    status: 1
  })
  dialogVisible.value = true
}

function openEditDialog(row: CouponVO): void {
  editingId.value = row.id
  dialogTitle.value = '编辑优惠券'
  Object.assign(formData, {
    name: row.name,
    type: row.type,
    amount: row.amount,
    minAmount: row.minAmount,
    totalCount: row.totalCount,
    startTime: row.startTime,
    endTime: row.endTime,
    status: row.status
  })
  dialogVisible.value = true
}

function resetForm(): void {
  formRef.value?.resetFields()
  Object.assign(formData, {
    name: '',
    type: 'AMOUNT',
    amount: 10,
    minAmount: 100,
    totalCount: 100,
    startTime: '',
    endTime: '',
    status: 1
  })
  editingId.value = null
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 校验时间区间
  if (formData.startTime && formData.endTime) {
    if (dayjs(formData.startTime).isAfter(dayjs(formData.endTime))) {
      ElMessage.warning('开始时间不能晚于结束时间')
      return
    }
  }
  submitting.value = true
  try {
    const payload: CouponRequest = {
      name: formData.name.trim(),
      type: formData.type,
      amount: formData.amount,
      minAmount: formData.minAmount,
      totalCount: formData.totalCount,
      startTime: formData.startTime,
      endTime: formData.endTime,
      status: formData.status
    }
    if (editingId.value === null) {
      await adminCreateCoupon(payload)
      ElMessage.success('新增成功')
    } else {
      await adminUpdateCoupon(editingId.value, payload)
      ElMessage.success('编辑成功')
    }
    dialogVisible.value = false
    await fetchCouponList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

/* === 删除 === */
async function handleDelete(row: CouponVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除优惠券「${row.name}」吗？此操作不可恢复`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await adminDeleteCoupon(row.id)
    ElMessage.success('删除成功')
    await fetchCouponList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === 启停 === */
const statusLoadingId = ref<number | string | null>(null)
async function handleStatusChange(row: CouponVO): Promise<void> {
  const nextStatus = row.status === 1 ? 0 : 1
  statusLoadingId.value = row.id
  try {
    await adminUpdateCouponStatus(row.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '已启用' : '已停用')
    await fetchCouponList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    statusLoadingId.value = null
  }
}

/* === 发放弹窗 === */
const distributeVisible = ref<boolean>(false)
const distributeRow = ref<CouponVO | null>(null)
const distributeSubmitting = ref<boolean>(false)
const distributeFormRef = ref<FormInstance | null>(null)

const distributeForm = reactive({
  userId: null as number | null
})

const distributeRules: FormRules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }]
}

/* === 用户远程搜索 === */
const userOptions = ref<UserVO[]>([])
const userLoading = ref<boolean>(false)

async function searchUser(query: string): Promise<void> {
  if (!query || !query.trim()) {
    userOptions.value = []
    return
  }
  userLoading.value = true
  try {
    const res = await getUserList({ keyword: query.trim(), pageNum: 1, pageSize: 20 })
    userOptions.value = res.data?.list || []
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    userLoading.value = false
  }
}

function openDistributeDialog(row: CouponVO): void {
  distributeRow.value = row
  distributeForm.userId = null
  userOptions.value = []
  distributeVisible.value = true
}

async function handleDistribute(): Promise<void> {
  if (!distributeFormRef.value || !distributeRow.value) return
  try {
    await distributeFormRef.value.validate()
  } catch {
    return
  }
  distributeSubmitting.value = true
  try {
    const res = await adminDistributeCoupon(distributeRow.value.id, distributeForm.userId as number)
    // 优先使用后端返回的 username，回退到 userOptions 中的 username
    const username = res.data
      || userOptions.value.find(u => u.id === distributeForm.userId)?.username
      || `用户 ${distributeForm.userId}`
    ElMessage.success(`已发放给用户 ${username}`)
    distributeVisible.value = false
    await fetchCouponList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    distributeSubmitting.value = false
  }
}

/* === 领取记录弹窗 === */
const recordsVisible = ref<boolean>(false)
const recordsRow = ref<CouponVO | null>(null)
const recordsLoading = ref<boolean>(false)
const recordsList = ref<AdminCouponRecordVO[]>([])
const recordsTotal = ref<number>(0)
const recordsPageNum = ref<number>(1)
const recordsPageSize = ref<number>(10)

const recordsTitle = computed<string>(() =>
  recordsRow.value ? `领取记录 - ${recordsRow.value.name}` : '领取记录'
)

const recordsTotalPages = computed(() => Math.max(1, Math.ceil(recordsTotal.value / recordsPageSize.value)))

const recordsDisplayPages = computed<number[]>(() => {
  const pages: number[] = []
  const t = recordsTotalPages.value
  let start = Math.max(1, recordsPageNum.value - 2)
  const end = Math.min(t, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function recordsStatusText(status: string): string {
  switch (status) {
    case 'UNUSED': return '未使用'
    case 'USED': return '已使用'
    case 'EXPIRED': return '已过期'
    default: return status || '—'
  }
}

function recordsStatusClass(status: string): string {
  switch (status) {
    case 'UNUSED': return 'completed'
    case 'USED': return 'paid'
    default: return ''
  }
}

function openRecordsDialog(row: CouponVO): void {
  recordsRow.value = row
  recordsPageNum.value = 1
  recordsList.value = []
  recordsTotal.value = 0
  recordsVisible.value = true
  fetchRecordsList()
}

async function fetchRecordsList(): Promise<void> {
  if (!recordsRow.value) return
  recordsLoading.value = true
  try {
    const res = await adminGetCouponRecords(
      recordsRow.value.id,
      recordsPageNum.value,
      recordsPageSize.value
    )
    recordsList.value = res.data?.list || []
    recordsTotal.value = res.data?.total || 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    recordsLoading.value = false
  }
}

function handleRecordsPageChange(page: number): void {
  if (page < 1 || page > recordsTotalPages.value) return
  recordsPageNum.value = page
  fetchRecordsList()
}

function resetRecords(): void {
  recordsRow.value = null
  recordsList.value = []
  recordsTotal.value = 0
  recordsPageNum.value = 1
}

onMounted(() => {
  fetchCouponList()
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

/* === .btn-sm === */
.btn-sm {
  padding: 5px 14px;
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

.btn-sm:hover {
  opacity: 0.9;
}

/* === .admin-table === */
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
  font-size: 13px;
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

/* === 名称单元格 === */
.coupon-name-cell {
  font-weight: 600;
  color: var(--color-text-primary);
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* === 时间单元格 === */
.time-cell {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  line-height: 1.6;
}

/* === 状态标签 === */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-tag.paid {
  background: var(--tag-paid-bg);
  color: var(--tag-paid-fg);
}

.status-tag.completed {
  background: var(--tag-completed-bg);
  color: var(--tag-completed-fg);
}

/* === 操作按钮 === */
.table-actions {
  display: flex;
  gap: 8px;
}

.table-action-btn {
  font-size: 13px;
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

/* === 分页 === */
.admin-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  background: #fff;
  color: var(--color-text-primary);
  user-select: none;
}

.page-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-btn.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.page-btn.disabled {
  color: #ccc;
  cursor: not-allowed;
}

.page-btn.disabled:hover {
  border-color: var(--color-border);
  color: #ccc;
}

/* === 表单辅助 === */
.form-unit {
  margin-left: 8px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.form-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
  line-height: 1.5;
}

/* === 发放弹窗 === */
.distribute-content {
  display: flex;
  flex-direction: column;
}

.distribute-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  padding: 4px 0;
}

.distribute-info .info-label {
  color: var(--color-text-secondary);
}

.distribute-info .info-value {
  color: var(--color-text-primary);
  font-weight: 600;
}

/* === 领取记录弹窗 === */
.records-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  padding: 4px 0;
}

.records-info .info-label {
  color: var(--color-text-secondary);
}

.records-info .info-value {
  color: var(--color-text-primary);
  font-weight: 600;
}

.records-info .info-divider {
  color: var(--color-border);
  margin: 0 8px;
}
</style>