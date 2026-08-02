<template>
  <div class="user-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">用户列表</div>
        <div class="admin-table-actions">
          <input
            v-model="keyword"
            class="admin-search-input"
            placeholder="搜索用户名/手机号..."
            @keyup.enter="handleSearch"
          />
          <select v-model="roleFilter" class="admin-filter-select" @change="handleSearch">
            <option value="">全部角色</option>
            <option value="BUYER">买家</option>
            <option value="SELLER">卖家</option>
            <option value="ADMIN">管理员</option>
          </select>
          <select v-model="statusFilter" class="admin-filter-select" @change="handleSearch">
            <option value="">全部状态</option>
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">禁用</option>
          </select>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>头像</th>
            <th>用户名</th>
            <th>手机号</th>
            <th>角色</th>
            <th>注册时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in userList" :key="row.id">
            <td>
              <div class="table-avatar">{{ (row.username || '').charAt(0).toUpperCase() }}</div>
            </td>
            <td>{{ row.username }}</td>
            <td>{{ maskPhone(row.phone) }}</td>
            <td>
              <span class="role-badge" :class="getRoleClass(row.role)">
                {{ getRoleLabel(row.role) }}
              </span>
            </td>
            <td>{{ formatDate(row.createTime) }}</td>
            <td>
              <span
                class="switch-toggle"
                :class="{ off: row.status !== 'ACTIVE' }"
                @click="handleStatusToggle(row as UserVO)"
              ></span>
            </td>
            <td>
              <div class="table-actions">
                <template v-if="row.id === currentUserId">
                  <button class="table-action-btn muted" disabled>当前账号</button>
                </template>
                <template v-else>
                  <button class="table-action-btn" @click="openRoleDialog(row as UserVO)">改角色</button>
                  <button class="table-action-btn" @click="openLogsDialog(row as UserVO)">日志</button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="userList.length === 0 && !loading">
            <td colspan="7" class="empty-cell">暂无用户数据</td>
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

    <!-- 改角色弹窗：保留 el-dialog -->
    <el-dialog
      v-model="roleDialogVisible"
      title="修改用户角色"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <span>{{ roleDialogUser?.username }}</span>
        </el-form-item>
        <el-form-item label="当前角色">
          <span class="role-badge" :class="getRoleClass(roleDialogUser?.role || 'BUYER')">
            {{ getRoleLabel(roleDialogUser?.role || 'BUYER') }}
          </span>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="newRole" placeholder="请选择角色" style="width: 100%">
            <el-option label="买家" value="BUYER" />
            <el-option label="商家" value="SELLER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="handleRoleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 登录日志弹窗：保留 el-dialog -->
    <el-dialog
      v-model="logsDialogVisible"
      title="登录日志"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-table v-loading="logsLoading" :data="loginLogs" stripe border max-height="400">
        <el-table-column prop="loginIp" label="登录 IP" width="140" />
        <el-table-column prop="loginLocation" label="登录地点" width="140" show-overflow-tooltip />
        <el-table-column label="User Agent" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.userAgent || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.loginResult === 'SUCCESS' ? 'success' : 'danger'" effect="light">
              {{ row.loginResult === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.failReason || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="登录时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.loginTime) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无登录记录" />
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * P15 用户管理 - 严格对照 index.html .page-admin-users
 * 筛选栏 + 原生 table + 角色修改弹窗 + 登录日志弹窗
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  getUserList,
  updateUserStatus,
  updateUserRole,
  getUserLoginLogs
} from '@/api/admin'
import { useUserStore } from '@/stores/user'
import type { UserVO, UserRole, LoginLogVO } from '@/types'

const userStore = useUserStore()
const currentUserId = computed<number | null>(() => userStore.userInfo?.id ?? null)

/* === 列表数据 === */
const loading = ref(false)
const userList = ref<UserVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/* === 筛选条件 === */
const keyword = ref('')
const roleFilter = ref<UserRole | ''>('')
const statusFilter = ref<string>('')

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
function formatDate(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD')
}

/* === 手机号脱敏 === */
function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone || '—'
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

/* === 角色映射 === */
function getRoleLabel(role: UserRole): string {
  const map: Record<UserRole, string> = {
    BUYER: '买家',
    SELLER: '卖家',
    ADMIN: '管理员'
  }
  return map[role] || role
}
/* === 角色 badge class：对照设计稿 role-badge.buyer/seller/admin === */
function getRoleClass(role: UserRole): string {
  const map: Record<UserRole, string> = {
    BUYER: 'buyer',
    SELLER: 'seller',
    ADMIN: 'admin'
  }
  return map[role] || 'buyer'
}

/* === 拉取用户列表 === */
async function fetchUserList(): Promise<void> {
  loading.value = true
  try {
    const res = await getUserList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      role: roleFilter.value || undefined,
      status: statusFilter.value || undefined
    })
    userList.value = res.data.list
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
  fetchUserList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchUserList()
}

/* === 状态切换 === */
async function handleStatusToggle(row: UserVO): Promise<void> {
  if (row.id === currentUserId.value) return
  const newVal = row.status !== 'ACTIVE'
  const action = newVal ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定${action}用户「${row.username}」吗？`,
      '操作确认',
      { type: 'warning', confirmButtonText: `确定${action}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id, { status: newVal ? 'ACTIVE' : 'DISABLED' })
    ElMessage.success(`${action}成功`)
    fetchUserList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === 改角色弹窗 === */
const roleDialogVisible = ref(false)
const roleDialogUser = ref<UserVO | null>(null)
const newRole = ref<UserRole>('BUYER')
const roleSubmitting = ref(false)

function openRoleDialog(row: UserVO): void {
  roleDialogUser.value = row
  newRole.value = row.role
  roleDialogVisible.value = true
}

async function handleRoleSubmit(): Promise<void> {
  if (!roleDialogUser.value) return
  if (newRole.value === roleDialogUser.value.role) {
    ElMessage.info('角色未变更')
    roleDialogVisible.value = false
    return
  }
  roleSubmitting.value = true
  try {
    await updateUserRole(roleDialogUser.value.id, { role: newRole.value })
    ElMessage.success('角色修改成功')
    roleDialogVisible.value = false
    fetchUserList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    roleSubmitting.value = false
  }
}

/* === 登录日志弹窗 === */
const logsDialogVisible = ref(false)
const logsLoading = ref(false)
const loginLogs = ref<LoginLogVO[]>([])

async function openLogsDialog(row: UserVO): Promise<void> {
  logsDialogVisible.value = true
  logsLoading.value = true
  loginLogs.value = []
  try {
    const res = await getUserLoginLogs(row.id, { pageNum: 1, pageSize: 50 })
    loginLogs.value = res.data.list
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    logsLoading.value = false
  }
}

onMounted(() => {
  fetchUserList()
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

/* === 严格对照 .admin-search-input === */
.admin-search-input {
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 10px;
  font-size: 13px;
  width: 180px;
  outline: none;
}
.admin-search-input:focus {
  border-color: var(--color-primary);
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

/* === 严格对照 .table-avatar === */
.table-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-bg-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-secondary);
}

/* === 严格对照 .role-badge === */
.role-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 13px;
  font-weight: 700;
}
.role-badge.admin {
  background: var(--tag-timeout-bg);
  color: var(--tag-timeout-fg);
}
.role-badge.seller {
  background: var(--tag-unpaid-bg);
  color: var(--tag-unpaid-fg);
}
.role-badge.buyer {
  background: var(--tag-completed-bg);
  color: var(--tag-completed-fg);
}

/* === 严格对照 .switch-toggle === */
.switch-toggle {
  width: 36px;
  height: 20px;
  border-radius: 10px;
  background: var(--color-success);
  position: relative;
  cursor: pointer;
  display: inline-block;
  transition: background 0.2s;
}
.switch-toggle::after {
  content: '';
  position: absolute;
  top: 2px;
  right: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: all 0.2s;
}
.switch-toggle.off {
  background: var(--btn-disabled-bg);
}
.switch-toggle.off::after {
  right: auto;
  left: 2px;
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
</style>
