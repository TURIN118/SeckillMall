<template>
  <div class="seckill-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">秒杀场次列表</div>
        <div class="admin-table-actions">
          <button class="btn-sm primary" @click="openCreateDialog">创建场次</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>场次名称</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>商品数量</th>
            <th>限购/人</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in activityList" :key="row.id">
            <td>{{ row.name }}</td>
            <td>{{ formatDateTime(row.startTime) }}</td>
            <td>{{ formatDateTime(row.endTime) }}</td>
            <td>{{ row.goodsList ? row.goodsList.length : 0 }}</td>
            <td>{{ row.perLimit }}</td>
            <td>
              <span class="status-tag" :class="getActivityStatusTagClass(row.status)">
                {{ getActivityStatusLabel(row.status) }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action-btn" @click="openDetailDialog(row)">详情</button>
                <button v-if="row.status !== 2" class="table-action-btn danger"
                  @click="handleDelete(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="activityList.length === 0 && !loading">
            <td colspan="7" class="empty-cell">暂无秒杀场次</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 创建场次弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建秒杀场次" width="800px" :close-on-click-modal="false"
      @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="场次名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入场次名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="formData.startTime" type="datetime" placeholder="请选择开始时间"
            format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="formData.endTime" type="datetime" placeholder="请选择结束时间" format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="限购数量" prop="perLimit">
          <el-input-number v-model="formData.perLimit" :min="1" :step="1" controls-position="right"
            style="width: 200px" />
          <span class="form-unit">件/人</span>
        </el-form-item>
        <el-form-item label="场次描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入场次描述" maxlength="200"
            show-word-limit />
        </el-form-item>

        <!-- 动态商品列表 -->
        <el-divider content-position="left">场次商品列表</el-divider>
        <div v-for="(item, idx) in formData.goodsItems" :key="idx" class="goods-item-row">
          <el-form-item :label="`商品${idx + 1}`" :prop="`goodsItems.${idx}.productId`"
            :rules="[{ required: true, message: '请选择商品', trigger: 'change' }]">
            <el-select v-model="item.productId" placeholder="搜索商品名称" filterable remote
              :remote-method="(q: string) => searchProducts(q, idx)" :loading="productLoadingIdx === idx"
              style="width: 260px" @change="(val: number) => handleProductChange(val, idx)">
              <el-option v-for="p in productOptionsMap[idx] || []" :key="p.id"
                :label="`${p.productName} (¥${formatPrice(p.originalPrice)})`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="`活动名称${idx + 1}`" :prop="`goodsItems.${idx}.seckillName`"
            :rules="[{ required: true, message: '请输入活动名称', trigger: 'blur' }]">
            <el-input v-model="item.seckillName" placeholder="秒杀活动名称" maxlength="50" />
          </el-form-item>
          <el-form-item :label="`秒杀价${idx + 1}`" :prop="`goodsItems.${idx}.seckillPrice`"
            :rules="[{ required: true, message: '请输入秒杀价格', trigger: 'blur' }]">
            <el-input-number v-model="item.seckillPrice" :min="0.01" :precision="2" :step="1"
              controls-position="right" style="width: 160px" />
            <span class="form-unit">元</span>
          </el-form-item>
          <el-form-item :label="`库存${idx + 1}`" :prop="`goodsItems.${idx}.stockCount`"
            :rules="[{ required: true, message: '请输入库存', trigger: 'blur' }]">
            <el-input-number v-model="item.stockCount" :min="1" :step="1" controls-position="right"
              style="width: 160px" />
            <span class="form-unit">件</span>
          </el-form-item>
          <div class="goods-item-actions">
            <button v-if="formData.goodsItems.length > 1" class="btn-sm" @click="removeGoodsItem(idx)">移除</button>
          </div>
        </div>
        <el-form-item>
          <el-button @click="addGoodsItem">+ 添加商品</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 场次详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="场次详情" width="800px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="场次名称">{{ detailData.name }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="status-tag" :class="getActivityStatusTagClass(detailData.status)">
              {{ getActivityStatusLabel(detailData.status) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDateTime(detailData.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDateTime(detailData.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="限购/人">{{ detailData.perLimit }}</el-descriptions-item>
          <el-descriptions-item label="商品数量">{{ detailData.goodsList?.length || 0 }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailData.description || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">场次下商品</el-divider>
        <table class="admin-table">
          <thead>
            <tr>
              <th>活动名称</th>
              <th>商品</th>
              <th>秒杀价</th>
              <th>库存(剩余/总量)</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="g in detailData.goodsList" :key="g.id">
              <td>{{ g.seckillName }}</td>
              <td>{{ g.productName }}</td>
              <td class="price-cell">¥{{ formatPrice(g.seckillPrice) }}</td>
              <td>{{ g.availableCount }} / {{ g.stockCount }}</td>
              <td>
                <span class="status-tag" :class="getGoodsStatusTagClass(g.status)">
                  {{ getGoodsStatusLabel(g.status) }}
                </span>
              </td>
            </tr>
            <tr v-if="!detailData.goodsList || detailData.goodsList.length === 0">
              <td colspan="5" class="empty-cell">暂无商品</td>
            </tr>
          </tbody>
        </table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * P13 秒杀场次管理（场次化重构后）
 * - 场次列表表格
 * - 创建场次弹窗（含动态商品列表）
 * - 场次详情弹窗
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import {
  listSeckillActivities,
  createSeckillActivity,
  deleteSeckillActivity
} from '@/api/seckill'
import { getProductList } from '@/api/product'
import type { SeckillActivityVO, SeckillGoodsVO, SeckillStatus, ProductVO, ActivityGoodsItem } from '@/types'

/* === 列表数据 === */
const loading = ref(false)
const activityList = ref<SeckillActivityVO[]>([])

/* === 格式化 === */
function formatPrice(price: number): string {
  return Number(price || 0).toFixed(2)
}
function formatDateTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

/* === 场次状态映射 (0=待开始 1=进行中 2=已结束) === */
function getActivityStatusLabel(status: number): string {
  const map: Record<number, string> = { 0: '待开始', 1: '进行中', 2: '已结束' }
  return map[status] ?? '未知'
}
function getActivityStatusTagClass(status: number): string {
  const map: Record<number, string> = { 0: 'unpaid', 1: 'timeout', 2: 'cancelled' }
  return map[status] ?? 'cancelled'
}

/* === 商品状态映射 === */
function getGoodsStatusLabel(status: SeckillStatus): string {
  const map: Record<SeckillStatus, string> = {
    PENDING: '待开始',
    ACTIVE: '进行中',
    ENDED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] || status
}
function getGoodsStatusTagClass(status: SeckillStatus): string {
  const map: Record<SeckillStatus, string> = {
    PENDING: 'unpaid',
    ACTIVE: 'timeout',
    ENDED: 'cancelled',
    CANCELLED: 'cancelled'
  }
  return map[status] || 'cancelled'
}

/* === 拉取场次列表 === */
async function fetchActivityList(): Promise<void> {
  loading.value = true
  try {
    const res = await listSeckillActivities()
    activityList.value = res.data || []
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 创建场次弹窗 === */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

interface ActivityFormData {
  name: string
  startTime: string
  endTime: string
  perLimit: number
  description: string
  goodsItems: ActivityGoodsItem[]
}

const formData = reactive<ActivityFormData>({
  name: '',
  startTime: '',
  endTime: '',
  perLimit: 1,
  description: '',
  goodsItems: [createEmptyGoodsItem()]
})

function createEmptyGoodsItem(): ActivityGoodsItem {
  return {
    productId: undefined as unknown as number,
    seckillName: '',
    seckillPrice: 0.01,
    stockCount: 1
  }
}

/* === 商品远程搜索（按 idx 维护独立选项列表） === */
const productOptionsMap = reactive<Record<number, ProductVO[]>>({})
const productLoadingIdx = ref<number>(-1)

async function searchProducts(query: string, idx: number): Promise<void> {
  if (!query) {
    productOptionsMap[idx] = []
    return
  }
  productLoadingIdx.value = idx
  try {
    const res = await getProductList({ keyword: query, pageNum: 1, pageSize: 20, status: 'ON_SALE' })
    productOptionsMap[idx] = res.data.list
  } catch {
    productOptionsMap[idx] = []
  } finally {
    productLoadingIdx.value = -1
  }
}

function handleProductChange(_productId: number, _idx: number): void {
  // 预留：可在此根据所选商品原价校验秒杀价
}

function addGoodsItem(): void {
  formData.goodsItems.push(createEmptyGoodsItem())
}

function removeGoodsItem(idx: number): void {
  if (formData.goodsItems.length > 1) {
    formData.goodsItems.splice(idx, 1)
    delete productOptionsMap[idx]
  }
}

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

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入场次名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, validator: validateEndTime, trigger: 'change' }],
  perLimit: [{ required: true, message: '请输入限购数量', trigger: 'blur' }]
}

/* === 打开创建弹窗 === */
function openCreateDialog(): void {
  resetForm()
  dialogVisible.value = true
}

/* === 重置表单 === */
function resetForm(): void {
  formRef.value?.resetFields()
  Object.assign(formData, {
    name: '',
    startTime: '',
    endTime: '',
    perLimit: 1,
    description: '',
    goodsItems: [createEmptyGoodsItem()]
  })
  // 清空商品选项缓存
  for (const key of Object.keys(productOptionsMap)) {
    delete productOptionsMap[Number(key)]
  }
}

/* === 提交表单 === */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    ElMessage.warning('表单未就绪，请稍后重试')
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    // 校验失败：提示用户完善必填项，避免点击"确定"后无任何反馈
    ElMessage.warning('请完善表单必填项后再提交')
    return
  }
  if (formData.goodsItems.length === 0) {
    ElMessage.warning('请至少添加一个秒杀商品')
    return
  }
  submitting.value = true
  try {
    await createSeckillActivity({
      name: formData.name,
      startTime: formData.startTime,
      endTime: formData.endTime,
      perLimit: formData.perLimit,
      description: formData.description,
      goodsItems: formData.goodsItems
    })
    ElMessage.success('场次创建成功')
    dialogVisible.value = false
    fetchActivityList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

/* === 删除场次 === */
async function handleDelete(row: SeckillActivityVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除秒杀场次「${row.name}」吗？此操作不可恢复`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '返回' }
    )
  } catch {
    return
  }
  try {
    await deleteSeckillActivity(row.id)
    ElMessage.success('删除成功')
    fetchActivityList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === 场次详情弹窗 === */
const detailDialogVisible = ref(false)
const detailData = ref<SeckillActivityVO | null>(null)

function openDetailDialog(row: SeckillActivityVO): void {
  detailData.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchActivityList()
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

/* === 秒杀价单元格 === */
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

/* === 表单单位 === */
.form-unit {
  margin-left: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

/* === 动态商品项行 === */
.goods-item-row {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  margin-bottom: 12px;
  background: var(--color-bg-subtle);
}

.goods-item-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}
</style>
