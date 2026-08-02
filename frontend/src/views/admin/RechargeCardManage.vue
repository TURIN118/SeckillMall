<template>
  <div class="recharge-card-manage-page">
    <!-- 表格容器 -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏 -->
      <div class="admin-table-header">
        <div class="admin-table-title">充值卡管理</div>
        <div class="admin-table-actions">
          <input
            v-model.trim="batchNoFilter"
            class="admin-search-input"
            type="text"
            placeholder="按批次号筛选"
            @keyup.enter="handleQuery"
          />
          <button class="btn-sm" @click="handleQuery">查询</button>
          <button class="btn-sm" @click="handleReset">重置</button>
          <button class="btn-sm primary" @click="openGenerateDialog">批量生成</button>
        </div>
      </div>

      <!-- 表格 -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>卡号</th>
            <th>面额</th>
            <th>状态</th>
            <th>使用者</th>
            <th>使用时间</th>
            <th>批次号</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in cardList" :key="row.id">
            <td class="card-no-cell">{{ row.cardNo }}</td>
            <td class="amount-cell">¥ {{ formatMoney(row.faceValue) }}</td>
            <td>
              <span class="status-tag" :class="cardStatusClass(row.status)">
                {{ cardStatusLabel(row.status) }}
              </span>
            </td>
            <td>{{ row.usedBy != null ? row.usedBy : '—' }}</td>
            <td class="time-cell">{{ formatTime(row.usedTime) }}</td>
            <td class="batch-cell">{{ row.batchNo || '—' }}</td>
            <td class="time-cell">{{ formatTime(row.createTime) }}</td>
            <td>
              <div class="table-actions">
                <button
                  v-if="row.status === 'UNUSED'"
                  class="table-action-btn danger"
                  @click="handleDisable(row)"
                >禁用</button>
                <span v-else class="action-placeholder">—</span>
              </div>
            </td>
          </tr>
          <tr v-if="cardList.length === 0 && !loading">
            <td colspan="8" class="empty-cell">暂无充值卡数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 表尾分页 -->
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

    <!-- 批量生成弹窗 -->
    <el-dialog
      v-model="generateVisible"
      title="批量生成充值卡"
      width="460px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="resetGenerateForm"
    >
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="80px">
        <el-form-item label="面额" prop="faceValue">
          <el-input-number
            v-model="generateForm.faceValue"
            :min="1"
            :max="99999"
            :step="10"
            :precision="2"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-unit">元</span>
          <div class="quick-amounts">
            <button
              v-for="amt in quickAmounts"
              :key="amt"
              type="button"
              class="quick-amount-btn"
              :class="{ active: generateForm.faceValue === amt }"
              @click="generateForm.faceValue = amt"
            >{{ amt }}</button>
          </div>
        </el-form-item>
        <el-form-item label="数量" prop="count">
          <el-input-number
            v-model="generateForm.count"
            :min="1"
            :max="1000"
            :step="1"
            :precision="0"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-unit">张 (最多 1000 张)</span>
        </el-form-item>
        <div class="generate-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>生成后卡号和卡密由系统随机产生，卡密仅显示一次，请妥善保管</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateSubmitting" @click="handleGenerate">
          确认生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 生成结果弹窗 (显示卡号 + 卡密) -->
    <el-dialog
      v-model="resultVisible"
      title="充值卡生成结果"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="result-warning">
        <el-icon><WarningFilled /></el-icon>
        <span>卡密仅显示一次，关闭后无法再次查看，请妥善保存！</span>
      </div>
      <div class="result-meta">
        <span>批次号：<b>{{ resultBatchNo }}</b></span>
        <span>共 <b>{{ resultCards.length }}</b> 张</span>
        <span v-if="resultCards[0]">面额：<b>¥ {{ formatMoney(resultCards[0].faceValue) }}</b></span>
      </div>
      <div class="result-actions">
        <el-button size="small" @click="copyAllCards">
          <el-icon><DocumentCopy /></el-icon>&nbsp;复制全部
        </el-button>
        <el-button size="small" @click="exportCsv">
          <el-icon><Download /></el-icon>&nbsp;导出 CSV
        </el-button>
        <el-button size="small" @click="printCards">
          <el-icon><Printer /></el-icon>&nbsp;打印
        </el-button>
      </div>
      <div class="result-table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th style="width: 50px">序号</th>
              <th>卡号</th>
              <th>卡密</th>
              <th style="width: 80px">面额</th>
              <th style="width: 120px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in resultCards" :key="row.id">
              <td>{{ idx + 1 }}</td>
              <td class="mono-cell">{{ row.cardNo }}</td>
              <td class="mono-cell pwd-cell">{{ row.cardPassword || '—' }}</td>
              <td class="amount-cell">¥ {{ formatMoney(row.faceValue) }}</td>
              <td>
                <div class="row-actions">
                  <button class="mini-btn" @click="copyCardNo(row)">复制卡号</button>
                  <button class="mini-btn" @click="copyCardPassword(row)">复制卡密</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">我已保存，关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台充值卡管理
 * 批量生成 + 批次号筛选 + 列表 + 禁用
 * 严格对照 index.html .admin-table-wrap 样式
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { InfoFilled, WarningFilled, DocumentCopy, Download, Printer } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  adminGetRechargeCardList,
  adminGenerateRechargeCards,
  adminDisableRechargeCard
} from '@/api/rechargeCard'
import type { RechargeCardVO, RechargeCardStatus } from '@/types'

/* === 列表数据 === */
const loading = ref<boolean>(false)
const cardList = ref<RechargeCardVO[]>([])
const total = ref<number>(0)
const pageNum = ref<number>(1)
const pageSize = ref<number>(10)

/* === 筛选条件 === */
const batchNoFilter = ref<string>('')

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

/* === 拉取充值卡列表 === */
async function fetchCardList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminGetRechargeCardList(
      pageNum.value,
      pageSize.value,
      batchNoFilter.value || undefined
    )
    cardList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 查询/重置 === */
function handleQuery(): void {
  pageNum.value = 1
  fetchCardList()
}

function handleReset(): void {
  batchNoFilter.value = ''
  pageNum.value = 1
  fetchCardList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchCardList()
}

/* === 工具函数 === */
function formatMoney(value: number): string {
  return Number(value || 0).toFixed(2)
}

function formatTime(time: string | null | undefined): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

function cardStatusLabel(status: RechargeCardStatus): string {
  const map: Record<RechargeCardStatus, string> = {
    UNUSED: '未使用',
    USED: '已使用',
    DISABLED: '已禁用'
  }
  return map[status] || status
}

function cardStatusClass(status: RechargeCardStatus): string {
  const map: Record<RechargeCardStatus, string> = {
    UNUSED: 'paid',
    USED: 'completed',
    DISABLED: 'cancelled'
  }
  return map[status] || 'cancelled'
}

/* === 批量生成弹窗 === */
const generateVisible = ref<boolean>(false)
const generateSubmitting = ref<boolean>(false)
const generateFormRef = ref<FormInstance | null>(null)

/** 常用面额快捷选项 */
const quickAmounts: number[] = [50, 100, 200, 500]

const generateForm = reactive({
  faceValue: 100,
  count: 10
})

const generateRules: FormRules = {
  faceValue: [{ required: true, message: '请输入面额', trigger: 'change' }],
  count: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

function openGenerateDialog(): void {
  resetGenerateForm()
  generateVisible.value = true
}

function resetGenerateForm(): void {
  generateForm.faceValue = 100
  generateForm.count = 10
  generateFormRef.value?.clearValidate()
}

/* === 生成结果弹窗 (显示卡号+卡密) === */
const resultVisible = ref<boolean>(false)
const resultCards = ref<RechargeCardVO[]>([])
const resultBatchNo = ref<string>('')

async function handleGenerate(): Promise<void> {
  if (!generateFormRef.value) return
  try {
    await generateFormRef.value.validate()
  } catch {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认为面额 ${generateForm.faceValue} 元生成 ${generateForm.count} 张充值卡吗？`,
      '生成确认',
      { type: 'warning', confirmButtonText: '确定生成', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  generateSubmitting.value = true
  try {
    const res = await adminGenerateRechargeCards({
      faceValue: generateForm.faceValue,
      count: generateForm.count
    })
    const list = res.data || []
    ElMessage.success(`成功生成 ${list.length} 张充值卡`)
    generateVisible.value = false
    // 显示生成结果（卡号 + 卡密）
    resultCards.value = list
    resultBatchNo.value = list[0]?.batchNo || ''
    resultVisible.value = true
    // 刷新列表
    pageNum.value = 1
    await fetchCardList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    generateSubmitting.value = false
  }
}

/* === 复制到剪贴板 === */
async function copyToClipboard(text: string): Promise<void> {
  if (!text) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      // 兼容兜底方案
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

function copyCardNo(row: RechargeCardVO): void {
  copyToClipboard(row.cardNo)
}

function copyCardPassword(row: RechargeCardVO): void {
  copyToClipboard(row.cardPassword || '')
}

/** 复制当前批次全部卡号+卡密（用于一次性发给用户） */
function copyAllCards(): void {
  const lines = resultCards.value.map(
    (c) => `卡号:${c.cardNo}\t卡密:${c.cardPassword || ''}\t面额:${c.faceValue}`
  )
  copyToClipboard(lines.join('\n'))
}

/** 导出 CSV */
function exportCsv(): void {
  const rows = resultCards.value
  if (rows.length === 0) return
  const header = ['卡号', '卡密', '面额', '批次号']
  const body = rows.map((r) => [
    r.cardNo,
    r.cardPassword || '',
    String(r.faceValue),
    r.batchNo || resultBatchNo.value
  ])
  const csvContent = [header, ...body]
    .map((line) => line.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  // 加 BOM 防止 Excel 中文乱码
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `充值卡_${resultBatchNo.value || 'batch'}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出 CSV')
}

/** 打印 */
function printCards(): void {
  const rows = resultCards.value
  if (rows.length === 0) return
  const html = `
    <html>
    <head>
      <meta charset="utf-8" />
      <title>充值卡_${resultBatchNo.value}</title>
      <style>
        body { font-family: -apple-system, "Microsoft YaHei", sans-serif; padding: 20px; }
        h2 { margin: 0 0 12px; }
        .meta { color: #666; margin-bottom: 12px; font-size: 13px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; font-size: 13px; }
        th { background: #f5f5f5; }
        .mono { font-family: "Courier New", monospace; }
      </style>
    </head>
    <body>
      <h2>充值卡批次：${resultBatchNo.value}</h2>
      <div class="meta">共 ${rows.length} 张 · 面额 ${rows[0]?.faceValue || ''} 元 · 生成时间 ${formatTime(new Date().toISOString())}</div>
      <table>
        <thead><tr><th>序号</th><th>卡号</th><th>卡密</th><th>面额</th></tr></thead>
        <tbody>
          ${rows
            .map(
              (r, i) =>
                `<tr><td>${i + 1}</td><td class="mono">${r.cardNo}</td><td class="mono">${r.cardPassword || ''}</td><td>${r.faceValue}</td></tr>`
            )
            .join('')}
        </tbody>
      </table>
    </body>
    </html>`
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.error('无法打开打印窗口，请检查浏览器弹窗拦截')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

/* === 禁用 === */
async function handleDisable(row: RechargeCardVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定禁用充值卡「${row.cardNo}」吗？禁用后无法再用于充值`,
      '禁用确认',
      { type: 'warning', confirmButtonText: '确定禁用', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await adminDisableRechargeCard(row.id)
    ElMessage.success('已禁用')
    await fetchCardList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

onMounted(() => {
  fetchCardList()
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
  flex-wrap: wrap;
  gap: 8px;
}

.admin-table-title {
  font-size: 15px;
  font-weight: 700;
}

.admin-table-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* === 搜索输入框 === */
.admin-search-input {
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 10px;
  font-size: 13px;
  background: #fff;
  outline: none;
  width: 180px;
  transition: border-color 0.2s;
}

.admin-search-input:focus {
  border-color: var(--color-primary);
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

/* === 卡号单元格 (等宽字体) === */
.card-no-cell {
  font-family: var(--font-price, 'Courier New', monospace);
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.amount-cell {
  font-weight: 700;
  color: var(--color-primary);
}

.time-cell {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.batch-cell {
  font-family: var(--font-price, 'Courier New', monospace);
  font-size: 12px;
  color: var(--color-text-secondary);
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

.status-tag.cancelled {
  background: var(--tag-cancelled-bg);
  color: var(--tag-cancelled-fg);
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

.action-placeholder {
  color: var(--color-text-muted);
  font-size: 13px;
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

.generate-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 8px 12px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.generate-tip .el-icon {
  color: var(--color-primary);
  font-size: 14px;
  flex-shrink: 0;
}

/* === 面额快捷按钮 === */
.quick-amounts {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.quick-amount-btn {
  padding: 3px 10px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-amount-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.quick-amount-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

/* === 生成结果弹窗 === */
.result-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  color: #d46b08;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}

.result-warning .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.result-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.result-meta b {
  color: var(--color-text-primary);
  font-weight: 700;
}

.result-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.result-table-wrap {
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 4px;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.result-table thead th {
  background: var(--color-bg-subtle);
  padding: 8px 12px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 1;
}

.result-table tbody td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}

.result-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.result-table tbody tr:last-child td {
  border-bottom: none;
}

.mono-cell {
  font-family: var(--font-price, 'Courier New', monospace);
  font-size: 13px;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  word-break: break-all;
}

.pwd-cell {
  color: var(--color-danger);
  font-weight: 700;
}

.row-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mini-btn {
  padding: 2px 8px;
  border: 1px solid var(--color-border);
  border-radius: 3px;
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.mini-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>