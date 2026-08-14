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
              <div class="input-with-ai">
                <el-input v-model="formData.productName" placeholder="请输入商品名称" maxlength="100" show-word-limit />
                <el-button type="primary" plain size="small" class="ai-gen-btn" :loading="generatingTitle"
                  @click="generateTitle">
                  <el-icon><MagicStick /></el-icon>
                  AI 生成
                </el-button>
              </div>
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

            <!-- 商品规格区域：模板驱动 + 自定义扩展 -->
            <el-form-item label="商品规格" class="form-item-full">
              <div class="sku-attribute-area">
                <el-alert v-if="templateAttributes.length > 0" type="info" :closable="false"
                  title="以下属性来自分类规格模板，可在此基础上增删" style="margin-bottom: 8px" />
                <el-button type="primary" plain size="small" @click="addAttribute">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"
                    style="margin-right: 2px">
                    <path d="M12 5v14M5 12h14" />
                  </svg>
                  添加自定义属性
                </el-button>
                <div v-for="(attr, idx) in formData.attributes" :key="idx" class="attribute-row">
                  <el-tag v-if="attr.categoryAttributeId" type="success" size="small">模板</el-tag>
                  <el-tag v-else type="info" size="small">自定义</el-tag>
                  <el-input v-model="attr.name" placeholder="属性名（如：颜色）" style="width: 120px" />
                  <el-select v-model="attr.type" placeholder="类型" style="width: 100px">
                    <el-option label="图片型" value="IMAGE" />
                    <el-option label="文字型" value="TEXT" />
                  </el-select>
                  <div class="attribute-values">
                    <el-tag v-for="(val, vIdx) in attr.values" :key="vIdx" closable @close="removeAttrValue(idx, vIdx)">
                      {{ val.value }}
                    </el-tag>
                    <el-input v-if="attr.valueInputVisible" v-model="attr.valueInput" size="small" style="width: 100px"
                      @keyup.enter="confirmAttrValue(idx)" @blur="confirmAttrValue(idx)" />
                    <el-button v-else size="small" @click="showAttrValueInput(idx)">+ 添加值</el-button>
                  </div>
                  <el-button type="danger" plain size="small" @click="removeAttribute(idx)">删除属性</el-button>
                </div>
                <el-button v-if="formData.attributes.length > 0" type="success" plain size="small"
                  :loading="generatingSkus" @click="generateSkus">
                  生成 SKU 组合
                </el-button>
              </div>
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
              <div class="input-with-ai input-with-ai-column">
                <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入商品简介/描述"
                  maxlength="500" show-word-limit />
                <el-button type="primary" plain size="small" class="ai-gen-btn" :loading="generatingDescription"
                  @click="generateDescription">
                  <el-icon><MagicStick /></el-icon>
                  AI 生成简介
                </el-button>
              </div>
            </el-form-item>
          </div>
        </div>

        <!-- 全宽：SKU 组合表格 -->
        <el-form-item v-if="formData.skus.length > 0" label="SKU组合" class="form-item-full">
          <div class="sku-table-wrap">
            <el-table :data="paginatedSkus" border style="width: 100%">
              <el-table-column label="组合" min-width="200">
                <template #default="{ row }">{{ formatSkuAttributes(row.attributes) }}</template>
              </el-table-column>
              <el-table-column label="价格" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :min="0.01" :precision="2" :controls="false" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="100">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="0" :controls="false" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="主图" width="120">
                <template #default="{ row }">
                  <ImageUploader v-model="row.mainImageList" :max-count="1" />
                </template>
              </el-table-column>
              <el-table-column label="编码" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.skuCode" size="small" placeholder="自动生成" />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button type="danger" plain size="small" @click="removeSkuByRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination v-if="formData.skus.length > 20" v-model:current-page="skuCurrentPage" :page-size="20"
              :total="formData.skus.length" layout="prev, pager, next, total"
              style="margin-top: 12px; justify-content: flex-end" />
          </div>
        </el-form-item>

        <!-- 全宽：富文本编辑器 -->
        <el-form-item label="商品详情" prop="detailHtml" class="form-item-full">
          <div class="wang-editor-wrap">
            <div class="wang-editor-toolbar-wrap">
              <Toolbar :editor="editorRef" :mode="mode" style="border-bottom: 1px solid #ccc; flex: 1" />
              <el-button size="small" class="ai-gen-detail-btn" :loading="generatingDetail"
                @click="generateDetail">
                <el-icon><MagicStick /></el-icon>
                AI 生成详情
              </el-button>
            </div>
            <Editor v-model="formData.detailHtml" :defaultConfig="editorConfig" :mode="mode"
              style="height: 360px; overflow-y: hidden;" @onCreated="(editor: IDomEditor) => editorRef = editor" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import {
  getProductDetail,
  createProduct,
  updateProduct
} from '@/api/product'
import { getCategoryTree } from '@/api/category'
import { getCategoryAttributes } from '@/api/categoryAttribute'
import { generateSkuCombinations } from '@/api/sku'
import { uploadImage } from '@/api/upload'
// T17: AIGC 文案生成接口
import { generateAigc } from '@/api/ai'
import type {
  ProductVO,
  CategoryVO,
  CategoryTreeNode,
  ProductStatus,
  ProductAttributeDTO,
  ProductSkuDTO,
  CategoryAttribute
} from '@/types'
import ImageUploader from '@/components/ImageUploader.vue'
// wangEditor 富文本编辑器（Vue3 版本）
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
// M41 修复: 引入 IDomEditor 类型替代 any
import type { IDomEditor } from '@wangeditor/editor'

const route = useRoute()
const router = useRouter()

/* === 路由判断：编辑 or 新增 === */
const isEdit = computed(() => route.name === 'ProductEdit')
// C5 修复: 雪花 ID 全程使用 string 类型, 避免 Number 精度丢失
// (雪花 ID 如 2085560004061081601 超过 JS Number.MAX_SAFE_INTEGER = 2^53-1)
const editingId = computed<string | null>(() => {
  const id = route.params.id
  if (!id) return null
  const idStr = String(id)
  // 仅校验非空字符串, 不做 Number 转换 (避免精度丢失)
  return idStr.length > 0 ? idStr : null
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
  // 新增 SKU 相关字段
  attributes: ProductAttributeDTO[]
  skus: ProductSkuDTO[]
}

const formData = reactive<ProductFormData>({
  productName: '',
  categoryId: undefined,
  originalPrice: 0.01,
  stock: 0,
  description: '',
  detailHtml: '',
  images: [],
  status: 'ON_SALE',
  attributes: [],
  skus: []
})

/* === 主图（单图，独立于多图 images） ===
 * 后端 ProductCreateRequest 只有 images 数组，这里将主图作为 images[0]，
 * 多图作为 images[1..n]。提交时合并；编辑时拆分。
 */
const mainImageList = ref<string[]>([])

/* === SKU 相关状态 === */
/** 分类规格模板（用于区分模板属性 / 自定义属性） */
const templateAttributes = ref<CategoryAttribute[]>([])
/** SKU 生成中加载态 */
const generatingSkus = ref(false)
/** SKU 表格分页 */
const skuCurrentPage = ref(1)
const SKU_PAGE_SIZE = 20
/** 分页后的 SKU 列表 */
const paginatedSkus = computed(() => {
  const start = (skuCurrentPage.value - 1) * SKU_PAGE_SIZE
  return formData.skus.slice(start, start + SKU_PAGE_SIZE)
})

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

/* === T17: 根据分类 ID 查找分类名称（遍历分类树） ===
 *  AIGC 接口需要 categoryName 参数，从已加载的 categoryOptions 树中查找
 */
function getCategoryName(categoryId: string | number | undefined): string {
  if (categoryId == null) return ''
  const walk = (nodes: CascaderOption[]): string => {
    for (const node of nodes) {
      if (node.value === categoryId) return node.label
      if (node.children && node.children.length > 0) {
        const found = walk(node.children)
        if (found) return found
      }
    }
    return ''
  }
  return walk(categoryOptions.value)
}

/* === 选择分类后自动带出规格模板 ===
 *  仅在 attributes 为空（首次选择分类）时自动带出，避免覆盖用户已编辑的属性
 */
watch(
  () => formData.categoryId,
  async (newCategoryId) => {
    if (!newCategoryId) {
      templateAttributes.value = []
      return
    }
    try {
      const res = await getCategoryAttributes(newCategoryId as number)
      templateAttributes.value = res.data || []
      // 仅在 attributes 为空（首次选择分类）时自动带出
      if (formData.attributes.length === 0 && templateAttributes.value.length > 0) {
        formData.attributes = templateAttributes.value.map((t) => ({
          categoryAttributeId: t.id,
          name: t.name,
          type: t.type,
          sortOrder: t.sortOrder,
          values: t.values.map((v) => ({
            value: v.value,
            imageUrl: v.imageUrl || undefined,
            sortOrder: v.sortOrder
          })),
          valueInputVisible: false,
          valueInput: ''
        }))
        ElMessage.info(`已带出分类模板 ${templateAttributes.value.length} 个属性`)
      }
    } catch {
      templateAttributes.value = []
    }
  }
)

/* === 添加自定义属性 === */
function addAttribute(): void {
  formData.attributes.push({
    categoryAttributeId: undefined,  // 自定义属性
    name: '',
    type: 'TEXT',
    sortOrder: formData.attributes.length,
    values: [],
    valueInputVisible: false,
    valueInput: ''
  })
}

/* === 删除属性 === */
function removeAttribute(idx: number): void {
  formData.attributes.splice(idx, 1)
}

/* === 显示属性值输入框 === */
function showAttrValueInput(idx: number): void {
  formData.attributes[idx].valueInputVisible = true
  formData.attributes[idx].valueInput = ''
}

/* === 确认添加属性值 === */
function confirmAttrValue(idx: number): void {
  const attr = formData.attributes[idx]
  const val = (attr.valueInput || '').trim()
  if (val && !attr.values.some((v) => v.value === val)) {
    attr.values.push({
      value: val,
      imageUrl: attr.type === 'IMAGE' ? '' : undefined,
      sortOrder: attr.values.length
    })
  }
  attr.valueInputVisible = false
  attr.valueInput = ''
}

/* === 删除属性值 === */
function removeAttrValue(idx: number, vIdx: number): void {
  formData.attributes[idx].values.splice(vIdx, 1)
}

/* === 建议10 已落实：SKU 笛卡尔积后端生成，前端仅传属性定义 ===
 *  SKU 数量可能爆炸（3 属性 × 10 值 = 1000 个 SKU），前端生成会导致：
 *  1. 大量计算阻塞 UI
 *  2. 一次性渲染过多 DOM 导致页面卡顿
 *  3. 前端笛卡尔积逻辑与后端重复，维护成本高
 *  改为：前端传属性定义，后端返回笛卡尔积 SKU 列表，前端表格分页展示
 */
async function generateSkus(): Promise<void> {
  if (formData.attributes.length === 0) {
    ElMessage.warning('请先添加属性')
    return
  }
  for (const attr of formData.attributes) {
    if (!attr.name || attr.values.length === 0) {
      ElMessage.warning('属性名和属性值不能为空')
      return
    }
  }
  generatingSkus.value = true
  try {
    // 调用后端接口生成笛卡尔积 SKU
    const res = await generateSkuCombinations({
      productId: editingId.value,  // 编辑时传，新增时为 null
      attributes: formData.attributes.map((a) => ({
        name: a.name,
        type: a.type,
        values: a.values.map((v) => ({ value: v.value, imageUrl: v.imageUrl }))
      })),
      defaultPrice: formData.originalPrice  // 默认价格
    })
    formData.skus = res.data.map((sku) => ({
      ...sku,
      mainImageList: sku.mainImage ? [sku.mainImage] : []
    }))
    skuCurrentPage.value = 1
    ElMessage.success(`已生成 ${res.data.length} 个 SKU 组合`)
  } catch {
    ElMessage.error('生成 SKU 失败')
  } finally {
    generatingSkus.value = false
  }
}

/* === 笛卡尔积工具函数（保留供前端小规模预览用，大规模生成走后端） === */
function cartesianProduct<T>(arrays: T[][]): T[][] {
  if (arrays.length === 0) return [[]]
  const [first, ...rest] = arrays
  const restProduct = cartesianProduct(rest)
  return first.flatMap((x) => restProduct.map((r) => [x, ...r]))
}

/* === 格式化 SKU 属性为可读字符串 === */
function formatSkuAttributes(attributesJson: string): string {
  try {
    const obj = JSON.parse(attributesJson) as Record<string, unknown>
    return Object.entries(obj)
      .map(([k, v]) => `${k}: ${v}`)
      .join(' / ')
  } catch {
    return attributesJson
  }
}

/* === 删除 SKU 行（通过行引用找到全局索引，兼容分页） === */
function removeSkuByRow(row: unknown): void {
  const idx = formData.skus.indexOf(row as ProductSkuDTO)
  if (idx >= 0) {
    formData.skus.splice(idx, 1)
  }
}

/* === 删除 SKU 行（按全局索引） === */
function removeSku(idx: number): void {
  formData.skus.splice(idx, 1)
}

/* === wangEditor 富文本编辑器实例 === */
// 使用 shallowRef 避免对编辑器实例做递归响应式化（官方推荐）
// M41 修复: 使用 IDomEditor 类型替代 any，保证类型安全
const editorRef = shallowRef<IDomEditor | null>(null)
// 配置图片自定义上传：调用后端 /api/v1/upload/image 接口，避免 base64 嵌入导致大图失败
const editorConfig = {
  placeholder: '请输入商品详情内容...',
  MENU_CONF: {
    uploadImage: {
      // customUpload(file, insertFn): 自定义上传，上传完成后调用 insertFn(url, alt, href) 将图片插入编辑器
      async customUpload(
        file: File,
        insertFn: (url: string, alt?: string, href?: string) => void
      ): Promise<void> {
        try {
          const res = await uploadImage(file, 'product-detail')
          // res 为 Result<UploadResultVO>，res.data.url 即后端返回的完整可访问 URL
          insertFn(res.data.url, file.name, res.data.url)
        } catch {
          ElMessage.error('图片上传失败')
        }
      }
    }
  }
}
const mode = 'default'

/* ==================== T17: AIGC 文案生成 ==================== */
/** AI 生成加载状态 */
const generatingTitle = ref(false)
const generatingDescription = ref(false)
const generatingDetail = ref(false)

/** 构造 AIGC 请求公共参数 */
function buildAigcBaseParams(generateType: 'TITLE' | 'DESCRIPTION' | 'DETAIL' | 'SEO') {
  return {
    productId: editingId.value ?? undefined,
    categoryId: formData.categoryId as string | number,
    categoryName: getCategoryName(formData.categoryId),
    skuAttributes: JSON.stringify(
      formData.attributes.map((a) => ({ name: a.name, values: a.values.map((v) => v.value) }))
    ),
    price: formData.originalPrice || 0,
    generateType
  }
}

/** AI 生成标题：调用 AIGC 接口 → 预览弹窗 → 用户确认后采纳 */
async function generateTitle(): Promise<void> {
  if (!formData.categoryId) {
    ElMessage.warning('请先选择商品分类')
    return
  }
  generatingTitle.value = true
  try {
    const res = await generateAigc(buildAigcBaseParams('TITLE'))
    if (res.code === 200 && res.data) {
      try {
        await ElMessageBox.confirm(res.data, 'AI 生成标题预览', {
          confirmButtonText: '采纳',
          cancelButtonText: '取消',
          type: 'info',
          dangerouslyUseHTMLString: false
        })
        formData.productName = res.data
        ElMessage.success('已采纳 AI 生成的标题')
      } catch (action) {
        // 用户点取消/关闭不算错误，不提示
        if (action !== 'cancel' && action !== 'close') {
          ElMessage.info('已取消采纳')
        }
      }
    } else {
      ElMessage.error(res.message || 'AI 生成失败')
    }
  } catch {
    ElMessage.error('AI 生成失败，请稍后重试')
  } finally {
    generatingTitle.value = false
  }
}

/** AI 生成简介：调用 AIGC 接口 → 预览弹窗 → 用户确认后采纳 */
async function generateDescription(): Promise<void> {
  if (!formData.categoryId) {
    ElMessage.warning('请先选择商品分类')
    return
  }
  generatingDescription.value = true
  try {
    const res = await generateAigc(buildAigcBaseParams('DESCRIPTION'))
    if (res.code === 200 && res.data) {
      try {
        await ElMessageBox.confirm(res.data, 'AI 生成简介预览', {
          confirmButtonText: '采纳',
          cancelButtonText: '取消',
          type: 'info',
          dangerouslyUseHTMLString: false
        })
        formData.description = res.data
        ElMessage.success('已采纳 AI 生成的简介')
      } catch (action) {
        if (action !== 'cancel' && action !== 'close') {
          ElMessage.info('已取消采纳')
        }
      }
    } else {
      ElMessage.error(res.message || 'AI 生成失败')
    }
  } catch {
    ElMessage.error('AI 生成失败，请稍后重试')
  } finally {
    generatingDescription.value = false
  }
}

/** AI 生成详情：调用 AIGC 接口 → 预览弹窗 → 用户确认后采纳写入 wangEditor */
async function generateDetail(): Promise<void> {
  if (!formData.categoryId) {
    ElMessage.warning('请先选择商品分类')
    return
  }
  generatingDetail.value = true
  try {
    const res = await generateAigc(buildAigcBaseParams('DETAIL'))
    if (res.code === 200 && res.data) {
      try {
        // 详情内容可能含 HTML，使用 dangerouslyUseHTMLString 渲染预览
        await ElMessageBox.confirm(res.data, 'AI 生成详情预览', {
          confirmButtonText: '采纳',
          cancelButtonText: '取消',
          type: 'info',
          dangerouslyUseHTMLString: true
        })
        // 采纳：写入 wangEditor 内容
        const editor = editorRef.value
        if (editor) {
          editor.setHtml(res.data)
        } else {
          // 编辑器未就绪时直接赋值 v-model
          formData.detailHtml = res.data
        }
        ElMessage.success('已采纳 AI 生成的详情')
      } catch (action) {
        if (action !== 'cancel' && action !== 'close') {
          ElMessage.info('已取消采纳')
        }
      }
    } else {
      ElMessage.error(res.message || 'AI 生成失败')
    }
  } catch {
    ElMessage.error('AI 生成失败，请稍后重试')
  } finally {
    generatingDetail.value = false
  }
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

/* === 拉取商品详情（编辑模式） ===
 * C5 修复: id 参数改为 string 类型, 避免雪花 ID 精度丢失
 */
async function fetchProductDetail(id: number | string): Promise<void> {
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
    // 回填 SKU 相关字段
    //  使用类型断言：ProductAttributeVO.type 为 string，ProductAttributeDTO.type 为 AttributeType
    //  后端返回的 type 实际为 'IMAGE' | 'TEXT'，运行时安全
    formData.attributes = p.attributes
      ? (p.attributes.map((a) => ({
        ...a,
        values: a.values.map((v) => ({ ...v })),
        valueInputVisible: false,
        valueInput: ''
      })) as unknown as ProductAttributeDTO[])
      : []
    //  ProductSkuVO.skuCode 为可选，ProductSkuDTO.skuCode 为必选；后端总会返回 skuCode
    formData.skus = p.skus
      ? (p.skus.map((s) => ({
        ...s,
        mainImageList: s.mainImage ? [s.mainImage] : []
      })) as unknown as ProductSkuDTO[])
      : []
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
      status: formData.status,
      // 新增：仅当有属性时才提交 SKU 数据
      attributes: formData.attributes.length > 0
        ? formData.attributes.map((a) => ({
          categoryAttributeId: a.categoryAttributeId || null,  // 关联模板
          name: a.name,
          type: a.type,
          sortOrder: a.sortOrder,
          values: a.values.map((v) => ({
            value: v.value,
            imageUrl: v.imageUrl,
            sortOrder: v.sortOrder
          }))
        }))
        : undefined,
      skus: formData.skus.length > 0
        ? formData.skus.map((s) => ({
          skuCode: s.skuCode,
          price: s.price,
          stock: s.stock,
          mainImage:
            s.mainImageList && s.mainImageList.length > 0
              ? s.mainImageList[0]
              : null,
          attributes: s.attributes,
          status: s.status
        }))
        : undefined
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

/* === T17: wangEditor 工具栏容器（含 AI 生成详情按钮） === */
.wang-editor-toolbar-wrap {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ccc;
}

.ai-gen-detail-btn {
  margin: 0 8px;
  flex-shrink: 0;
}

/* === T17: 输入框 + AI 生成按钮布局 === */
.input-with-ai {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.input-with-ai .el-input {
  flex: 1;
}

/* 简介区域：textarea 与按钮纵向排列 */
.input-with-ai-column {
  flex-direction: column;
  align-items: flex-end;
}

.input-with-ai-column .el-textarea {
  width: 100%;
}

.ai-gen-btn {
  flex-shrink: 0;
}

/* === 商品规格区域 === */
.sku-attribute-area {
  width: 100%;
}

.attribute-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.attribute-values {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

/* === SKU 表格容器 === */
.sku-table-wrap {
  width: 100%;
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