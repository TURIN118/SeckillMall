<template>
  <div class="operation-log-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">操作日志</div>
        <div class="admin-table-actions">
          <select v-model="moduleFilter" class="admin-filter-select" @change="handleSearch">
            <option value="">全部模块</option>
            <option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</option>
          </select>
          <button class="btn-sm primary" :disabled="exportLoading" @click="handleExport">
            {{ exportLoading ? '导出中...' : '导出Excel' }}
          </button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>操作人</th>
            <th>模块</th>
            <th>操作</th>
            <th>目标</th>
            <th>IP</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in logList" :key="idx">
            <td>{{ row.operatorName }}</td>
            <td>{{ row.module }}</td>
            <td>{{ row.action }}</td>
            <td>{{ formatTarget(row) }}</td>
            <td>{{ row.ipAddress }}</td>
            <td>{{ formatDateTime(row.operationTime) }}</td>
          </tr>
          <tr v-if="logList.length === 0 && !loading">
            <td colspan="6" class="empty-cell">暂无操作日志</td>
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
  </div>
</template>

<script setup lang="ts">
/**
 * P16 操作日志 - 严格对照 index.html .page-admin-logs
 * 筛选栏 + 原生 table + 分页
 */
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { getOperationLogs, exportLogs } from '@/api/system'
import type { OperationLogVO } from '@/types'

/* === 列表数据 === */
const loading = ref(false)
const logList = ref<OperationLogVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/* === 筛选条件 === */
const moduleFilter = ref('')

/* === 导出loading === */
const exportLoading = ref(false)

/* === 模块选项 (从已加载数据中提取) === */
const moduleOptions = computed<string[]>(() => {
  const set = new Set<string>()
  logList.value.forEach((log) => {
    if (log.module) set.add(log.module)
  })
  return Array.from(set).sort()
})

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
function formatDateTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/* === 格式化目标列：对照设计稿 "iPhone 15 Pro (ID:1001)" === */
function formatTarget(row: OperationLogVO): string {
  if (row.detail) return row.detail
  if (row.targetId && row.targetType) return `${row.targetType} (ID:${row.targetId})`
  if (row.targetId) return `ID:${row.targetId}`
  return '—'
}

/* === 拉取日志列表 === */
async function fetchLogList(): Promise<void> {
  loading.value = true
  try {
    const res = await getOperationLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      module: moduleFilter.value || undefined
    })
    logList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 搜索 === */
function handleSearch(): void {
  pageNum.value = 1
  fetchLogList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchLogList()
}

/* === 导出 Excel（后端生成，blob 下载） === */
async function handleExport(): Promise<void> {
  exportLoading.value = true
  try {
    const blob = await exportLogs(moduleFilter.value || undefined)
    // 空文件保护：blob 大小为 0 时提示
    if (blob.size === 0) {
      ElMessage.warning('没有可导出的操作日志')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  fetchLogList()
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
.btn-sm:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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
</style>
