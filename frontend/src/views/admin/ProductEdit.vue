<template>
  <div class="product-edit-page">
    <!-- 页面头部：返回按钮 + 标题 -->
    <div class="page-header">
      <button class="back-btn" @click="handleBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
        返回
      </button>
      <h2 class="page-title">{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card" v-loading="loading">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px" class="product-form">
        <!-- 双列布局：左列基本信息 + 右列媒体上传 -->
        <div class="form-grid">
          <!-- 左列：基本信息 -->
          <div class="form-col form-col-left">
            <el-form-item label="商品名称" prop="productName">
              <el-input v-model="formData.productName" placeholder="请输入商品名称" maxlength="100" show-word-limit />
            </el-form-item>

            <el-form-item label="分类" prop="categoryId">
              <el-cascader v-model="cascaderValue" :options="categoryOptions" :props="cascaderProps" placeholder="请选择分类"
                clearable style="width: 100%" @change="handleCategoryChange" />
            </el-form-item>

            <el-form-item label="原价" prop="originalPrice">
              <el-input-number v-model="formData.originalPrice" :min="0.01" :precision="2" :step="1"
                controls-position="right" style="width: 200px" />
              <span class="form-unit">元</span>
            </el-form-item>

            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="formData.stock" :min="0" :step="1" controls-position="right"
                style="width: 200px" />
              <span class="form-unit">件</span>
            </el-form-item>

            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio value="ON_SALE">上架</el-radio>
                <el-radio value="OFF_SHELF">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>

          <!-- 右列：媒体上传 -->
          <div class="form-col form-col-right">
            <el-form-item label="主图" prop="mainImage">
              <ImageUploader v-model="mainImageList" :max-count="1" />
              <span class="form-unit">商品主图，仅支持 1 张</span>
            </el-form-item>

            <el-form-item label="图片" prop="images">
              <ImageUploader v-model="formData.images" :max-count="5" />
              <span class="form-unit">商品多图，最多 5 张</span>
            </el-form-item>

            <el-form-item label="商品简介" prop="description">
              <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入商品简介/描述"
                maxlength="500" show-word-limit />
            </el-form-item>
          </div>
        </div>

        <!-- 全宽：富文本编辑器 -->
        <el-form-item label="商品详情" prop="detailHtml" class="form-item-full">
          <div class="wang-editor-wrap">
            <Toolbar :editor="editorRef" :mode="mode" style="border-bottom: 1px solid #ccc" />
            <Editor v-model="formData.detailHtml" :defaultConfig="editorConfig" :mode="mode"
              style="height: 360px; overflow-y: hidden;" @onCreated="(editor: any) => editorRef = editor" />
          </div>
        </el-form-item>

        <!-- 全宽：操作按钮 -->
        <el-form-item class="form-item-full">
          <div class="form-actions">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
            <el-button @click="handleCancel">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 商品编辑独立页面 (新增/编辑)
 * 从 ProductManage.vue 弹窗迁移而来，使用独立路由页面替代弹窗
 * 分类选择器使用 el-cascader，支持选择一级分类或二级分类
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getProductDetail,
  createProduct,
  updateProduct
} from '@/api/product'
import { getCategoryTree } from '@/api/category'
import type { ProductVO, CategoryVO, CategoryTreeNode, ProductStatus } from '@/types'
import ImageUploader from '@/components/ImageUploader.vue'
// wangEditor 富文本编辑器（Vue3 版本）
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()

/* === 路由判断：编辑 or 新增 === */
const isEdit = computed(() => route.name === 'ProductEdit')
const editingId = computed<number | null>(() => {
  const id = route.params.id
  if (!id) return null
  const n = Number(id)
  return Number.isFinite(n) && n > 0 ? n : null
})

/* === 加载状态 === */
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

/* === 表单数据 === */
interface ProductFormData {
  productName: string
  categoryId: number | string | undefined
  originalPrice: number
  stock: number
  description: string
  detailHtml: string
  images: string[]
  status: ProductStatus
}

const formData = reactive<ProductFormData>({
  productName: '',
  categoryId: undefined,
  originalPrice: 0.01,
  stock: 0,
  description: '',
  detailHtml: '',
  images: [],
  status: 'ON_SALE'
})

/* === 主图（单图，独立于多图 images） ===
 * 后端 ProductCreateRequest 只有 images 数组，这里将主图作为 images[0]，
 * 多图作为 images[1..n]。提交时合并；编辑时拆分。
 */
const mainImageList = ref<string[]>([])

/* === 表单校验规则 === */
const formRules: FormRules = {
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  originalPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

/* === 分类级联选择器 === */
interface CascaderOption {
  value: number | string
  label: string
  children?: CascaderOption[]
}

const categoryOptions = ref<CascaderOption[]>([])
// check-strictly: 允许选择任意层级（一级或二级）
// emitPath: false: 只返回选中节点 value，不返回路径数组
const cascaderProps = {
  checkStrictly: true,
  emitPath: false,
  value: 'value',
  label: 'label',
  children: 'children'
}
// cascaderValue 与 formData.categoryId 同步
const cascaderValue = ref<number | string | undefined>(undefined)

function handleCategoryChange(value: unknown): void {
  // emitPath=false 时 value 为单值(number | string | null | undefined)
  if (value == null) {
    formData.categoryId = undefined
    return
  }
  if (Array.isArray(value)) {
    const last = value[value.length - 1]
    formData.categoryId = typeof last === 'number' ? last : Number(last)
  } else {
    formData.categoryId = typeof value === 'number' ? value : Number(value)
  }
}

/* === 将后端分类树转换为 el-cascader 所需格式 === */
function buildCascaderOptions(tree: CategoryTreeNode[]): CascaderOption[] {
  const walk = (nodes: CategoryTreeNode[]): CascaderOption[] => {
    return nodes.map((node) => {
      const opt: CascaderOption = {
        value: node.id,
        label: node.categoryName
      }
      if (node.children && node.children.length > 0) {
        opt.children = walk(node.children)
      }
      return opt
    })
  }
  return walk(tree)
}

/* === 拉取分类树 === */
async function fetchCategoryTree(): Promise<void> {
  try {
    const res = await getCategoryTree()
    const tree = res.data as unknown as CategoryTreeNode[]
    categoryOptions.value = buildCascaderOptions(tree)
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* === wangEditor 富文本编辑器实例 === */
// 使用 shallowRef 避免对编辑器实例做递归响应式化（官方推荐）
const editorRef = shallowRef<any>(null)
const editorConfig = { placeholder: '请输入商品详情内容...' }
const mode = 'default'

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

/* === 拉取商品详情（编辑模式） === */
async function fetchProductDetail(id: number): Promise<void> {
  loading.value = true
  try {
    const res = await getProductDetail(id)
    const p: ProductVO = res.data
    formData.productName = p.productName
    formData.categoryId = p.categoryId
    formData.originalPrice = p.originalPrice
    formData.stock = p.stock
    formData.description = p.description || ''
    formData.detailHtml = p.detailHtml || ''
    formData.status = p.status
    // 拆分主图与多图：第一张为主图，其余为多图
    const imgs = p.images ? [...p.images] : []
    if (imgs.length > 0) {
      mainImageList.value = [imgs[0]]
      formData.images = imgs.slice(1)
    } else {
      mainImageList.value = []
      formData.images = []
    }
    // 同步 cascader 选中值
    cascaderValue.value = p.categoryId
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    loading.value = false
  }
}

/* === 监听 mainImageList 变化（仅用于响应式，实际合并发生在提交时） === */
watch(mainImageList, () => {
  // 占位：保持响应式依赖
})

/* === 返回商品管理列表 === */
function handleBack(): void {
  router.push('/admin/products')
}

/* === 取消 === */
function handleCancel(): void {
  handleBack()
}

/* === 提交表单 === */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 校验分类
  if (!formData.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  submitting.value = true
  try {
    // 合并主图与多图：主图在前，多图在后
    const mergedImages: string[] = [
      ...mainImageList.value,
      ...formData.images
    ]
    const payload = {
      productName: formData.productName,
      categoryId: formData.categoryId as number,
      originalPrice: formData.originalPrice,
      stock: formData.stock,
      description: formData.description,
      detailHtml: formData.detailHtml,
      images: mergedImages,
      status: formData.status
    }
    if (isEdit.value && editingId.value !== null) {
      await updateProduct(editingId.value, payload)
      ElMessage.success('编辑成功')
    } else {
      await createProduct(payload)
      ElMessage.success('新增成功')
    }
    router.push('/admin/products')
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

/* === 初始化 === */
onMounted(async () => {
  await fetchCategoryTree()
  if (isEdit.value && editingId.value !== null) {
    await fetchProductDetail(editingId.value)
  }
})
</script>

<style scoped>
/* === 页面容器 === */
.product-edit-page {
  padding: 20px;

  margin: 0 auto;
}

/* === 页面头部 === */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--color-text-primary);
}

/* === 表单卡片（白色底色、圆角、边框、阴影） === */
.form-card {
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 32px 40px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.product-form {
  width: 100%;
}

/* === 双列布局网格 === */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 40px;
}

.form-col {
  /* 每列内的表单项垂直排列 */
  display: flex;
  flex-direction: column;
}

/* === 全宽表单项（富文本编辑器、操作按钮） === */
.form-item-full {
  width: 100%;
  margin-top: 8px;
}

/* === 表单单位提示 === */
.form-unit {
  margin-left: 8px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

/* === 表单底部操作按钮 === */
.form-actions {
  display: flex;
  gap: 12px;
}

/* === wangEditor 富文本编辑器容器 === */
.wang-editor-wrap {
  border: 1px solid #ccc;
  /* 创建层叠上下文，确保工具栏下拉菜单正确显示 */
  z-index: 100;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
}

/* === 响应式：小屏改为单列 === */
@media (max-width: 768px) {
  .product-edit-page {
    padding: 12px;
  }

  .form-card {
    padding: 20px 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>