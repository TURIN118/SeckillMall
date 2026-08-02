<template>
  <div class="review-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">评论管理</div>
        <div class="admin-table-actions">
          <select v-model="statusFilter" class="admin-filter-select" @change="handleQuery">
            <option value="">全部状态</option>
            <option :value="1">显示中</option>
            <option :value="0">已隐藏</option>
          </select>
          <button class="btn-sm" @click="handleQuery">查询</button>
          <button class="btn-sm" @click="handleReset">重置</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>评论 ID</th>
            <th>商品 ID</th>
            <th>用户</th>
            <th>评分</th>
            <th>评论内容</th>
            <th>状态</th>
            <th>评论时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in reviewList" :key="row.id">
            <td class="review-id-cell">{{ row.id }}</td>
            <td>{{ row.productId }}</td>
            <td>{{ row.userName || '匿名用户' }}</td>
            <td>
              <span class="review-rating">
                <span
                  v-for="star in 5"
                  :key="star"
                  class="star"
                  :class="{ filled: star <= row.rating }"
                >★</span>
              </span>
            </td>
            <td class="review-content-cell">
              <div class="content-text">{{ row.content }}</div>
              <div v-if="row.replyContent" class="reply-text">
                <span class="reply-tag">回复：</span>{{ row.replyContent }}
              </div>
            </td>
            <td>
              <span class="status-tag" :class="row.status === 1 ? 'paid' : 'cancelled'">
                {{ row.status === 1 ? '显示中' : '已隐藏' }}
              </span>
            </td>
            <td>{{ formatTime(row.createTime) }}</td>
            <td>
              <div class="table-actions">
                <button class="table-action-btn" @click="openReply(row)">回复</button>
                <button
                  v-if="row.status === 1"
                  class="table-action-btn"
                  @click="handleToggleStatus(row, 0)"
                >隐藏</button>
                <button
                  v-else
                  class="table-action-btn"
                  @click="handleToggleStatus(row, 1)"
                >显示</button>
              </div>
            </td>
          </tr>
          <tr v-if="reviewList.length === 0 && !loading">
            <td colspan="8" class="empty-cell">暂无评论数据</td>
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

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复评论" width="600px">
      <div v-if="replyRow" class="reply-dialog-content">
        <div class="reply-original">
          <div class="reply-original-header">
            <span class="reply-original-user">{{ replyRow.userName || '匿名用户' }}</span>
            <span class="reply-original-rating">
              <span
                v-for="star in 5"
                :key="star"
                class="star"
                :class="{ filled: star <= replyRow.rating }"
              >★</span>
            </span>
            <span class="reply-original-time">{{ formatTime(replyRow.createTime) }}</span>
          </div>
          <div class="reply-original-text">{{ replyRow.content }}</div>
        </div>
        <div class="reply-form">
          <textarea
            v-model="replyContent"
            class="reply-textarea"
            placeholder="请输入回复内容..."
            rows="4"
            maxlength="1000"
          ></textarea>
        </div>
      </div>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replySubmitting" @click="submitReply">确认回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台评论管理 - 严格对照 index.html .admin-table-wrap 样式
 * 列表 + 状态筛选 + 回复 + 隐藏/显示
 */
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewList, replyReview, updateReviewStatus } from '@/api/review'
import type { ProductReviewVO } from '@/types'

/* === 列表数据 === */
const loading = ref(false)
const reviewList = ref<ProductReviewVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/* === 筛选条件 === */
const statusFilter = ref<number | ''>('')

/* === 总页数 === */
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* === 显示页码 (最多 5 个) === */
const displayPages = computed<number[]>(() => {
  const pages: number[] = []
  const t = totalPages.value
  let start = Math.max(1, pageNum.value - 2)
  let end = Math.min(t, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

/* === 格式化 === */
function formatTime(time: string | null | undefined): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/* === 拉取评论列表 === */
async function fetchReviewList(): Promise<void> {
  loading.value = true
  try {
    const res = await getReviewList({
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    reviewList.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 查询 === */
function handleQuery(): void {
  pageNum.value = 1
  fetchReviewList()
}

/* === 重置 === */
function handleReset(): void {
  statusFilter.value = ''
  pageNum.value = 1
  fetchReviewList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchReviewList()
}

/* === 隐藏/显示 === */
async function handleToggleStatus(row: ProductReviewVO, newStatus: number): Promise<void> {
  const action = newStatus === 0 ? '隐藏' : '显示'
  try {
    await ElMessageBox.confirm(`确认${action}该评论吗？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateReviewStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    await fetchReviewList()
  } catch (err) {
    // 用户取消或操作失败
    if (err !== 'cancel') {
      // 错误已由全局拦截器提示
    }
  }
}

/* === 回复弹窗 === */
const replyVisible = ref(false)
const replyRow = ref<ProductReviewVO | null>(null)
const replyContent = ref('')
const replySubmitting = ref(false)

function openReply(row: ProductReviewVO): void {
  replyRow.value = row
  replyContent.value = row.replyContent || ''
  replyVisible.value = true
}

async function submitReply(): Promise<void> {
  if (!replyRow.value) return
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replySubmitting.value = true
  try {
    await replyReview(replyRow.value.id, replyContent.value.trim())
    ElMessage.success('回复成功')
    replyVisible.value = false
    await fetchReviewList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    replySubmitting.value = false
  }
}

onMounted(() => {
  fetchReviewList()
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

/* === 评论 ID 单元格 === */
.review-id-cell {
  font-family: var(--font-price);
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* === 评分星星 === */
.review-rating {
  display: inline-flex;
  gap: 1px;
}
.star {
  font-size: 14px;
  color: var(--color-text-muted);
  user-select: none;
}
.star.filled {
  color: #f5a623;
}

/* === 评论内容单元格 === */
.review-content-cell {
  max-width: 300px;
}
.content-text {
  color: var(--color-text-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.reply-text {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  padding: 4px 8px;
  border-radius: 3px;
}
.reply-tag {
  color: var(--color-primary);
  font-weight: 600;
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
.status-tag.paid {
  background: var(--tag-paid-bg);
  color: var(--tag-paid-fg);
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
.table-action-btn:hover {
  text-decoration: underline;
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

/* === 回复弹窗 === */
.reply-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.reply-original {
  background: var(--color-bg-subtle);
  padding: 12px;
  border-radius: 4px;
}
.reply-original-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.reply-original-user {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.reply-original-rating {
  display: inline-flex;
  gap: 1px;
}
.reply-original-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: auto;
}
.reply-original-text {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.6;
}
.reply-form {
  width: 100%;
}
.reply-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 13px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}
.reply-textarea:focus {
  border-color: var(--color-primary);
}
</style>