<template>
  <div class="product-manage-page">
    <!-- 表格容器：对照 index.html .admin-table-wrap -->
    <div class="admin-table-wrap">
      <!-- 表头工具栏：对照 .admin-table-header -->
      <div class="admin-table-header">
        <div class="admin-table-title">商品列表</div>
        <div class="admin-table-actions">
          <input v-model="keyword" class="admin-search-input" placeholder="搜索商品名称..." @keyup.enter="handleSearch" />
          <select v-model="categoryId" class="admin-filter-select" @change="handleSearch">
            <option :value="undefined">全部分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.categoryName }}
            </option>
          </select>
          <select v-model="statusFilter" class="admin-filter-select" @change="handleSearch">
            <option value="">全部状态</option>
            <option value="ON_SALE">上架中</option>
            <option value="OFF_SHELF">已下架</option>
          </select>
          <button class="btn-sm primary" @click="openCreatePage">新增商品</button>
        </div>
      </div>

      <!-- 表格：对照 .admin-table -->
      <table class="admin-table" v-loading="loading">
        <thead>
          <tr>
            <th>图片</th>
            <th>商品名称</th>
            <th>分类</th>
            <th>原价</th>
            <th>库存</th>
            <th>销量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in productList" :key="row.id">
            <td>
              <div class="table-avatar img">
                <img v-if="row.images && row.images.length > 0" :src="formatImageUrl(row.images[0])" alt="" />
                <span v-else>无图</span>
              </div>
            </td>
            <td>{{ row.productName }}</td>
            <td>{{ row.categoryName }}</td>
            <td>¥{{ formatPrice(row.originalPrice) }}</td>
            <td>{{ row.stock }}</td>
            <td>{{ row.salesCount }}</td>
            <td>
              <span class="status-tag" :class="row.status === 'ON_SALE' ? 'paid' : 'cancelled'">
                {{ row.status === 'ON_SALE' ? '上架中' : '已下架' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action-btn" @click="openEditPage(row as ProductVO)">编辑</button>
                <button class="table-action-btn danger" @click="handleDelete(row as ProductVO)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="productList.length === 0 && !loading">
            <td colspan="8" class="empty-cell">暂无商品数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 表尾分页：对照 .admin-table-footer / .pagination -->
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
  </div>
</template>

<script setup lang="ts">
/**
 * P11 商品管理 - 严格对照 index.html .page-admin-products
 * 工具栏 + 原生 table + 分页
 * 新增/编辑已迁移至独立页面 ProductEdit.vue（路由 /admin/products/create 和 /admin/products/edit/:id）
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductList, deleteProduct } from '@/api/product'
import { getCategoryTree } from '@/api/category'
import { formatImageUrl } from '@/utils/image'
import type { ProductVO, CategoryVO, ProductStatus } from '@/types'

const router = useRouter()

/* === 列表数据 === */
const loading = ref(false)
const productList = ref<ProductVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const categoryId = ref<number | undefined>(undefined)
const categories = ref<CategoryVO[]>([])
/* === 状态筛选: '' 表示全部，'ON_SALE' 上架中，'OFF_SHELF' 已下架 === */
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


/* === 格式化价格 === */
function formatPrice(price: number): string {
  return price.toFixed(2)
}

/* === 拉取商品列表 === */
async function fetchProductList(): Promise<void> {
  loading.value = true
  try {
    const res = await getProductList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value,
      // 后台默认显示所有商品(含下架)，仅当指定状态时才筛选
      // statusFilter 只能为 '' | 'ON_SALE' | 'OFF_SHELF'，安全断言为 ProductStatus
      status: (statusFilter.value || undefined) as ProductStatus | undefined
    })
    productList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 拉取分类列表 === */
async function fetchCategories(): Promise<void> {
  try {
    const res = await getCategoryTree()
    categories.value = res.data
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === 搜索/筛选 === */
function handleSearch(): void {
  pageNum.value = 1
  fetchProductList()
}

/* === 分页 === */
function handlePageChange(page: number): void {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  fetchProductList()
}

/* === 跳转到新增商品独立页面 === */
function openCreatePage(): void {
  router.push('/admin/products/create')
}

/* === 跳转到编辑商品独立页面 === */
function openEditPage(row: ProductVO): void {
  router.push(`/admin/products/edit/${row.id}`)
}

/* === 删除商品 === */
async function handleDelete(row: ProductVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除商品「${row.productName}」吗？此操作不可恢复`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    if (productList.value.length === 1 && pageNum.value > 1) {
      pageNum.value -= 1
    }
    fetchProductList()
  } catch {
    // 错误已由全局拦截器提示
  }
}

onMounted(() => {
  // 分类树和商品列表并行请求, 互不依赖, 用 Promise.all 明确并行
  Promise.all([fetchCategories(), fetchProductList()])
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

/* === 严格对照 .admin-search-input === */
.admin-search-input {
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 10px;
  font-size: 12px;
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
  font-size: 12px;
  background: #fff;
  outline: none;
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

/* === 严格对照 .table-avatar === */
.table-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-bg-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-secondary);
  overflow: hidden;
}

.table-avatar.img {
  border-radius: 6px;
  width: 40px;
  height: 40px;
}

.table-avatar.img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

/* === 严格对照 .admin-table-footer / .pagination === */
.admin-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}

.page-info {
  font-size: 12px;
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
  font-size: 12px;
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
