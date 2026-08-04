<template>
  <div class="category-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">分类树</div>
        <div class="admin-table-actions">
          <button class="btn-sm primary" @click="openCreateDialog">新增分类</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>分类名称</th>
            <th>排序</th>
            <th>商品数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="node in flatNodes" :key="node.id">
            <tr :class="{ 'parent-row': node.isParent }">
              <td>
                <span v-if="node.isParent" class="tree-toggle" @click="toggleExpand(node.id)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                    :style="{ transform: expandedSet.has(node.id) ? 'rotate(90deg)' : 'none' }">
                    <path d="M9 18l6-6-6-6" />
                  </svg>
                  <strong>{{ node.categoryName }}</strong>
                </span>
                <span v-else class="tree-indent">{{ node.categoryName }}</span>
              </td>
              <td>{{ node.sortOrder }}</td>
              <td>{{ node.productCount || 0 }}</td>
              <td>
                <el-switch :model-value="node.status === 1" :loading="statusLoadingId === node.id"
                  @change="handleStatusChange(node)" />
              </td>
              <td>
                <div class="table-actions">
                  <button class="table-action-btn" @click="openEditDialog(node)">编辑</button>
                  <button class="table-action-btn danger" @click="handleDelete(node)">删除</button>
                </div>
              </td>
            </tr>
          </template>
          <tr v-if="flatNodes.length === 0 && !loading">
            <td colspan="5" class="empty-cell">暂无分类数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑弹窗：保留 el-dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false"
      @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" placeholder="请输入分类名称" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="父分类" prop="parentId">
          <el-select v-model="formData.parentId" placeholder="请选择父分类" style="width: 100%">
            <el-option :value="0" label="作为一级分类" />
            <el-option v-for="cat in rootCategories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
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
 * P12 分类管理 - 严格对照 index.html .page-admin-categories
 * 树形表格 (前端按 parentId 构建树) + 新增/编辑弹窗
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getCategoryTree,
  createCategory,
  updateCategory,
  deleteCategory,
  updateCategoryStatus
} from '@/api/category'
import type { CategoryVO, CategoryTreeNode } from '@/types'

/* === 列表数据 === */
const loading = ref(false)
const categoryList = ref<CategoryVO[]>([])
const categoryTree = ref<CategoryTreeNode[]>([])

/* === 展开状态 === */
const expandedSet = ref<Set<number | string>>(new Set())

/* === 构建树结构 === */
function buildTree(list: CategoryVO[]): CategoryTreeNode[] {
  const map = new Map<number | string, CategoryTreeNode>()
  const roots: CategoryTreeNode[] = []
  list.forEach((item) => {
    map.set(item.id, { ...item, children: [] })
  })
  list.forEach((item) => {
    const node = map.get(item.id)!
    if (item.parentId === 0 || !map.has(item.parentId)) {
      roots.push(node)
    } else {
      const parent = map.get(item.parentId)!
      parent.children!.push(node)
    }
  })
  const sortNodes = (nodes: CategoryTreeNode[]): void => {
    nodes.sort((a, b) => a.sortOrder - b.sortOrder)
    nodes.forEach((node) => {
      if (node.children && node.children.length > 0) {
        sortNodes(node.children)
      }
    })
  }
  sortNodes(roots)
  return roots
}

/* === 将后端返回的树形结构展平为扁平列表 === */
function flattenTree(tree: CategoryTreeNode[]): CategoryVO[] {
  const result: CategoryVO[] = []
  const walk = (nodes: CategoryTreeNode[]): void => {
    nodes.forEach((node) => {
      const { children, ...flat } = node
      result.push(flat)
      if (children && children.length > 0) {
        walk(children)
      }
    })
  }
  walk(tree)
  return result
}

/* === 展平为行 (父 + 展开的子) === */
interface FlatNode extends CategoryVO {
  isParent: boolean
  productCount?: number
}
const flatNodes = computed<FlatNode[]>(() => {
  const result: FlatNode[] = []
  const walk = (nodes: CategoryTreeNode[]): void => {
    nodes.forEach((node) => {
      const hasChildren = !!(node.children && node.children.length > 0)
      result.push({ ...node, isParent: hasChildren })
      if (hasChildren && expandedSet.value.has(node.id)) {
        walk(node.children!)
      }
    })
  }
  walk(categoryTree.value)
  return result
})

/* === 切换展开 === */
function toggleExpand(id: number | string): void {
  if (expandedSet.value.has(id)) {
    expandedSet.value.delete(id)
  } else {
    expandedSet.value.add(id)
  }
  // 触发响应式更新
  expandedSet.value = new Set(expandedSet.value)
}

/* === 拉取分类列表 === */
async function fetchCategoryList(): Promise<void> {
  loading.value = true
  try {
    const res = await getCategoryTree()
    // 后端返回树形结构(嵌套children)，需先展平为扁平列表
    const flatList = flattenTree(res.data as unknown as CategoryTreeNode[])
    categoryList.value = flatList
    categoryTree.value = buildTree(flatList)
    // 默认收起所有节点，用户点击展开箭头才展开
    expandedSet.value = new Set<number>()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 根分类 (用于父分类选择) === */
const rootCategories = computed<CategoryVO[]>(() =>
  categoryList.value.filter((c) => c.parentId === 0)
)

/* === 弹窗 === */
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const editingId = ref<number | string | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

interface CategoryFormData {
  categoryName: string
  parentId: number
  sortOrder: number
  status: number
}

const formData = reactive<CategoryFormData>({
  categoryName: '',
  parentId: 0,
  sortOrder: 0,
  status: 1
})

const formRules: FormRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
  ],
  parentId: [{ required: true, message: '请选择父分类', trigger: 'change' }]
}

/* === 打开新增弹窗 === */
function openCreateDialog(): void {
  editingId.value = null
  dialogTitle.value = '新增分类'
  Object.assign(formData, {
    categoryName: '',
    parentId: 0,
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

/* === 打开编辑弹窗 === */
function openEditDialog(row: CategoryVO): void {
  editingId.value = row.id
  dialogTitle.value = '编辑分类'
  Object.assign(formData, {
    categoryName: row.categoryName,
    parentId: row.parentId,
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

/* === 重置表单 === */
function resetForm(): void {
  formRef.value?.resetFields()
  Object.assign(formData, {
    categoryName: '',
    parentId: 0,
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
  submitting.value = true
  try {
    if (editingId.value === null) {
      await createCategory({
        categoryName: formData.categoryName,
        parentId: formData.parentId,
        sortOrder: formData.sortOrder,
        status: formData.status
      })
      ElMessage.success('新增成功')
    } else {
      await updateCategory(editingId.value, {
        categoryName: formData.categoryName,
        parentId: formData.parentId,
        sortOrder: formData.sortOrder,
        status: formData.status
      })
      ElMessage.success('编辑成功')
    }
    dialogVisible.value = false
    await fetchCategoryList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

/* === 删除分类 === */
async function handleDelete(row: CategoryVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.categoryName}」吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    // 用户取消确认, 静默
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    await fetchCategoryList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === 切换分类状态 === */
const statusLoadingId = ref<number | string | null>(null)
async function handleStatusChange(node: CategoryVO): Promise<void> {
  const nextStatus = node.status === 1 ? 0 : 1
  statusLoadingId.value = node.id
  try {
    await updateCategoryStatus(node.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '已启用' : '已禁用')
    await fetchCategoryList()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    statusLoadingId.value = null
  }
}

onMounted(() => {
  fetchCategoryList()
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

/* 父行背景：对照设计稿 tr style="background:var(--bg-subtle)" */
.admin-table tbody tr.parent-row {
  background: var(--color-bg-subtle);
}

.empty-cell {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 40px 16px;
}

/* === 严格对照 .tree-toggle / .tree-indent === */
.tree-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.tree-toggle svg {
  width: 12px;
  height: 12px;
  transition: transform 0.2s;
}

.tree-indent {
  padding-left: 24px;
}

/* === 严格对照 .status-tag === */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
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
