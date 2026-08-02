<template>
  <div class="seckill-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">秒杀活动列表</div>
        <div class="admin-table-actions">
          <select v-model="statusFilter" class="admin-filter-select" @change="handleStatusChange">
            <option value="">全部状态</option>
            <option value="PENDING">待开始</option>
            <option value="ACTIVE">进行中</option>
            <option value="ENDED">已结束</option>
            <option value="CANCELLED">已取消</option>
          </select>
          <button class="btn-sm primary" @click="openCreateDialog">创建活动</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>活动名称</th>
            <th>商品</th>
            <th>秒杀价</th>
            <th>库存(剩余/总量)</th>
            <th>开始时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in seckillList" :key="row.id">
            <td>{{ row.seckillName }}</td>
            <td>{{ row.productName }}</td>
            <td class="price-cell">¥{{ formatPrice(row.seckillPrice) }}</td>
            <td>{{ row.availableCount }} / {{ row.stockCount }}</td>
            <td>{{ formatDateTime(row.startTime) }}</td>
            <td>
              <span class="status-tag" :class="getStatusTagClass(row.status)">
                {{ getStatusLabel(row.status) }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button
                  v-if="row.status === 'PENDING'"
                  class="table-action-btn"
                  @click="openEditDialog(row as SeckillGoodsVO)"
                >编辑</button>
                <button
                  v-if="row.status === 'PENDING' || row.status === 'ACTIVE'"
                  class="table-action-btn muted"
                  @click="handleCancel(row as SeckillGoodsVO)"
                >取消</button>
                <button
                  v-if="row.status === 'ENDED' || row.status === 'CANCELLED'"
                  class="table-action-btn muted"
                  disabled
                >—</button>
              </div>
            </td>
          </tr>
          <tr v-if="seckillList.length === 0 && !loading">
            <td colspan="7" class="empty-cell">暂无秒杀活动</td>
          </tr>
        </tbody>
      </table>

      <!-- 表尾分页：对照 .admin-table-footer / .pagination -->
      <div class="admin-table-footer">
        <span class="page-info">共 {{ total }} 条记录</span>
        <div class="pagination">
          <div
            class="page-btn"
            :class="{ disabled: pageNum <= 1 }"
            @click="handlePageChange(pageNum - 1)"
          >&lt;</div>
          <div
            v-for="p in displayPages"
            :key="p"
            class="page-btn"
            :class="{ active: p === pageNum }"
            @click="handlePageChange(p)"
          >{{ p }}</div>
          <div
            class="page-btn"
            :class="{ disabled: pageNum >= totalPages }"
            @click="handlePageChange(pageNum + 1)"
          >&gt;</div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑弹窗：保留 el-dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="650px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="商品" prop="productId">
          <el-select
            v-model="formData.productId"
            placeholder="请输入商品名称搜索"
            filterable
            remote
            :remote-method="searchProducts"
            :loading="productLoading"
            style="width: 100%"
            @change="handleProductChange"
          >
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="`${p.productName} (¥${formatPrice(p.originalPrice)})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称" prop="seckillName">
          <el-input v-model="formData.seckillName" placeholder="请输入活动名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="秒杀价格" prop="seckillPrice">
          <el-input-number
            v-model="formData.seckillPrice"
            :min="0.01"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-unit">元</span>
          <span v-if="selectedProduct" class="form-hint">
            原价 ¥{{ formatPrice(selectedProduct.originalPrice) }}
          </span>
        </el-form-item>
        <el-form-item label="秒杀库存" prop="stockCount">
          <el-input-number
            v-model="formData.stockCount"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-unit">件</span>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="formData.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="限购数量" prop="perLimit">
          <el-input-number
            v-model="formData.perLimit"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-unit">件/人</span>
        </el-form-item>
        <el-form-item label="活动图片" prop="images">
          <ImageUploader v-model="formData.images" :max-count="3" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入活动描述"
            maxlength="200"
            show-word-limit
          />
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
 * P13 秒杀活动管理 - 严格对照 index.html .page-admin-seckills
 * 状态筛选 + 原生 table + 分页 + 创建/编辑弹窗
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import {
  getSeckillList,
  createSeckill,
  updateSeckill,
  cancelSeckill
} from '@/api/seckill'
import { getProductList } from '@/api/product'
import type { SeckillGoodsVO, SeckillStatus, ProductVO } from '@/types'
import ImageUploader from '@/components/ImageUploader.vue'

/* === 列表数据 === */
const loading = ref(false)
const seckillList = ref<SeckillGoodsVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref<SeckillStatus | ''>('')

/* === 总页数 === */
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* === 显示页码 (最多 5 个) === */
const displayPages = computed<number[]>(() => {
  const pages: number[] = []
  const total = totalPages.value
  let start = Math.max(1, pageNum.value - 2)
  let end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

/* === 格式化 === */
function formatPrice(price: number): string {
  return price.toFixed(2)
}
function formatDateTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

/* === 状态映射 === */
function getStatusLabel(status: SeckillStatus): string {
  const map: Record<SeckillStatus, string> = {
    PENDING: '待开始',
    ACTIVE: '进行中',
    ENDED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] || status
}
/* === 状态 tag class：对照设计稿 status-tag.timeout/cancelled/unpaid === */
function getStatusTagClass(status: SeckillStatus): string {
  const map: Record<SeckillStatus, string> = {
    PENDING: 'unpaid',
    ACTIVE: 'timeout',
    ENDED: 'cancelled',
    CANCELLED: 'cancelled'
  }
  return map[status] || 'cancelled'
}

/* === 拉取秒杀列表 === */
async function fetchSeckillList(): Promise<void> {
  loading.value = true
  try {
    const res = await getSeckillList({
      status: statusFilter.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    seckillList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 状态筛选 === */
function handleStatusChange(): void {
  pageNum.value = 1
  fetchSeckillList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchSeckillList()
}

/* === 弹窗 === */
const dialogVisible = ref(false)
const dialogTitle = ref('创建秒杀活动')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

/* === 商品远程搜索 === */
const productOptions = ref<ProductVO[]>([])
const productLoading = ref(false)
const selectedProduct = ref<ProductVO | null>(null)

async function searchProducts(query: string): Promise<void> {
  if (!query) {
    productOptions.value = []
    return
  }
  productLoading.value = true
  try {
    const res = await getProductList({ keyword: query, pageNum: 1, pageSize: 20, status: 'ON_SALE' })
    productOptions.value = res.data.list
  } catch {
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

function handleProductChange(productId: number): void {
  selectedProduct.value = productOptions.value.find((p) => p.id === productId) || null
}

interface SeckillFormData {
  productId: number | undefined
  seckillName: string
  seckillPrice: number
  stockCount: number
  startTime: string
  endTime: string
  perLimit: number
  images: string[]
  description: string
}

const formData = reactive<SeckillFormData>({
  productId: undefined,
  seckillName: '',
  seckillPrice: 0.01,
  stockCount: 1,
  startTime: '',
  endTime: '',
  perLimit: 1,
  images: [],
  description: ''
})

/* === 自定义校验: 结束时间必须大于开始时间 === */
const validateEndTime = (_rule: unknown, value: string, callback: (err?: Error) => void): void => {
  if (!value) {
    callback(new Error('请选择结束时间'))
    return
  }
  if (formData.startTime && dayjs(value).valueOf() <= dayjs(formData.startTime).valueOf()) {
    callback(new Error('结束时间必须大于开始时间'))
    return
  }
  callback()
}

/* === 自定义校验: 秒杀价应低于原价 === */
const validateSeckillPrice = (_rule: unknown, value: number, callback: (err?: Error) => void): void => {
  if (!value || value <= 0) {
    callback(new Error('秒杀价格必须大于 0'))
    return
  }
  if (selectedProduct.value && value >= selectedProduct.value.originalPrice) {
    callback(new Error('秒杀价应低于商品原价'))
    return
  }
  callback()
}

const formRules: FormRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  seckillName: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  seckillPrice: [
    { required: true, message: '请输入秒杀价格', trigger: 'blur' },
    { validator: validateSeckillPrice, trigger: 'blur' }
  ],
  stockCount: [{ required: true, message: '请输入秒杀库存', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, validator: validateEndTime, trigger: 'change' }],
  perLimit: [{ required: true, message: '请输入限购数量', trigger: 'blur' }]
}

/* === 打开创建弹窗 === */
function openCreateDialog(): void {
  editingId.value = null
  dialogTitle.value = '创建秒杀活动'
  selectedProduct.value = null
  productOptions.value = []
  Object.assign(formData, {
    productId: undefined,
    seckillName: '',
    seckillPrice: 0.01,
    stockCount: 1,
    startTime: '',
    endTime: '',
    perLimit: 1,
    images: [],
    description: ''
  })
  dialogVisible.value = true
}

/* === 打开编辑弹窗 === */
function openEditDialog(row: SeckillGoodsVO): void {
  editingId.value = row.id
  dialogTitle.value = '编辑秒杀活动'
  selectedProduct.value = {
    id: row.productId,
    productName: row.productName,
    originalPrice: row.seckillPrice,
    categoryId: 0,
    categoryName: '',
    description: '',
    images: [],
    stock: 0,
    salesCount: 0,
    status: 'ON_SALE',
    createTime: ''
  }
  productOptions.value = [selectedProduct.value]
  Object.assign(formData, {
    productId: row.productId,
    seckillName: row.seckillName,
    seckillPrice: row.seckillPrice,
    stockCount: row.stockCount,
    startTime: dayjs(row.startTime).format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs(row.endTime).format('YYYY-MM-DD HH:mm:ss'),
    perLimit: row.perLimit || 1,
    images: row.images ? [...row.images] : [],
    description: row.description || ''
  })
  dialogVisible.value = true
}

/* === 重置表单 === */
function resetForm(): void {
  formRef.value?.resetFields()
  Object.assign(formData, {
    productId: undefined,
    seckillName: '',
    seckillPrice: 0.01,
    stockCount: 1,
    startTime: '',
    endTime: '',
    perLimit: 1,
    images: [],
    description: ''
  })
  editingId.value = null
  selectedProduct.value = null
  productOptions.value = []
}

/* === 提交表单 === */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = {
      productId: formData.productId as number,
      seckillName: formData.seckillName,
      seckillPrice: formData.seckillPrice,
      stockCount: formData.stockCount,
      startTime: formData.startTime,
      endTime: formData.endTime,
      perLimit: formData.perLimit,
      images: formData.images,
      description: formData.description
    }
    if (editingId.value !== null) {
      await updateSeckill(editingId.value, payload)
      ElMessage.success('编辑成功')
    } else {
      await createSeckill(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchSeckillList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

/* === 取消活动 === */
async function handleCancel(row: SeckillGoodsVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定取消秒杀活动「${row.seckillName}」吗？此操作不可恢复`,
      '取消确认',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '返回' }
    )
  } catch {
    return
  }
  try {
    await cancelSeckill(row.id)
    ElMessage.success('取消成功')
    fetchSeckillList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

onMounted(() => {
  fetchSeckillList()
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

/* === 严格对照 .admin-filter-select === */
.admin-filter-select {
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 8px;
  font-size: 13px;
  background: #fff;
  outline: none;
}

/* === 严格对照 .btn-sm === */
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

/* === 秒杀价单元格：对照设计稿 style="color:var(--seed-primary);font-weight:700" === */
.price-cell {
  color: var(--color-primary);
  font-weight: 700;
}

/* === 严格对照 .status-tag === */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.status-tag.unpaid {
  background: var(--tag-unpaid-bg);
  color: var(--tag-unpaid-fg);
}
.status-tag.timeout {
  background: var(--tag-timeout-bg);
  color: var(--tag-timeout-fg);
}
.status-tag.cancelled {
  background: var(--tag-cancelled-bg);
  color: var(--tag-cancelled-fg);
}

/* === 严格对照 .table-actions / .table-action-btn === */
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
.table-action-btn.muted {
  color: var(--color-text-secondary);
}
.table-action-btn:hover:not(:disabled) {
  text-decoration: underline;
}
.table-action-btn:disabled {
  cursor: default;
}

/* === 严格对照 .admin-table-footer / .pagination === */
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

/* === 表单单位 === */
.form-unit {
  margin-left: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
}
.form-hint {
  margin-left: 12px;
  color: var(--color-text-muted);
  font-size: 13px;
}
</style>
