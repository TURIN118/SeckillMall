<template>
  <div class="category-attribute-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">分类规格模板管理</h2>
      <div class="header-actions">
        <el-cascader
          v-model="cascaderValue"
          :options="categoryOptions"
          :props="cascaderProps"
          placeholder="请选择分类"
          clearable
          style="width: 280px"
          @change="handleCategoryChange"
        />
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card" v-loading="loading">
      <!-- 未选择分类提示 -->
      <div v-if="!selectedCategoryId" class="empty-tip">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
          <path d="M3 7h18M3 12h18M3 17h18" />
        </svg>
        <p>请先在上方选择一个分类，以管理其规格模板</p>
      </div>

      <!-- 已选择分类：属性列表 -->
      <div v-else class="attribute-list">
        <el-alert
          type="info"
          :closable="false"
          title="为分类维护规格属性模板，新增商品选择该分类时将自动带出这些属性"
          style="margin-bottom: 16px"
        />

        <div v-for="(attr, idx) in attributeList" :key="idx" class="attribute-card">
          <div class="attribute-card-header">
            <span class="attribute-index">#{{ idx + 1 }}</span>
            <el-tag v-if="attr.id" type="success" size="small">已保存</el-tag>
            <el-tag v-else type="warning" size="small">未保存</el-tag>
          </div>

          <div class="attribute-card-body">
            <el-input
              v-model="attr.name"
              placeholder="属性名（如：颜色）"
              style="width: 180px"
            />
            <el-select v-model="attr.type" style="width: 120px" placeholder="类型">
              <el-option label="图片型" value="IMAGE" />
              <el-option label="文字型" value="TEXT" />
            </el-select>
            <el-select v-model="attr.inputType" style="width: 140px" placeholder="录入方式">
              <el-option label="预设值选择" value="SELECT" />
              <el-option label="自由输入" value="INPUT" />
            </el-select>
            <div class="switch-wrap">
              <el-switch
                v-model="attr.isRequired"
                :active-value="1"
                :inactive-value="0"
                active-text="必选"
              />
            </div>
            <el-input-number
              v-model="attr.sortOrder"
              :min="0"
              size="small"
              style="width: 110px"
            />
          </div>

          <!-- 预设值列表（仅 SELECT 类型显示） -->
          <div v-if="attr.inputType === 'SELECT'" class="preset-values">
            <div class="preset-values-label">预设值：</div>
            <div class="preset-values-tags">
              <el-tag
                v-for="(v, i) in attr.values"
                :key="i"
                closable
                @close="removePresetValue(attr, i)"
                style="margin-right: 6px; margin-bottom: 4px"
              >
                <span
                  v-if="attr.type === 'IMAGE' && v.imageUrl"
                  class="color-dot"
                  :style="{ background: `url(${v.imageUrl}) center/cover` }"
                />
                {{ v.value }}
              </el-tag>
              <el-input
                v-if="presetInputVisible[idx]"
                v-model="presetInputValue[idx]"
                size="small"
                style="width: 140px"
                placeholder="输入值后回车"
                @keyup.enter="confirmPresetValue(idx)"
                @blur="confirmPresetValue(idx)"
              />
              <el-button v-else size="small" @click="showPresetInput(idx)">
                + 添加预设值
              </el-button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="attribute-card-actions">
            <el-button type="primary" size="small" :loading="savingId === idx" @click="saveAttribute(attr, idx)">
              保存
            </el-button>
            <el-button type="danger" plain size="small" @click="deleteAttribute(attr, idx)">
              删除
            </el-button>
          </div>
        </div>

        <!-- 新增属性按钮 -->
        <div class="add-attribute-area">
          <el-button type="primary" plain @click="addNewAttribute">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px">
              <path d="M12 5v14M5 12h14" />
            </svg>
            新增属性
          </el-button>
        </div>

        <!-- 空列表提示 -->
        <div v-if="attributeList.length === 0" class="empty-attr">
          <p>该分类暂无规格属性，点击上方「新增属性」开始配置</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 分类规格模板管理页面
 * 选择分类 → 加载该分类的规格属性列表 → 增删改属性及预设值
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCategoryAttributes,
  createCategoryAttribute,
  updateCategoryAttribute,
  deleteCategoryAttribute
} from '@/api/categoryAttribute'
import { getCategoryTree } from '@/api/category'
import type { CategoryAttribute, CategoryTreeNode, CategoryVO } from '@/types'

/* === 分类级联选择器 === */
interface CascaderOption {
  value: number | string
  label: string
  children?: CascaderOption[]
}

const categoryOptions = ref<CascaderOption[]>([])
const cascaderProps = {
  checkStrictly: true,
  emitPath: false,
  value: 'value',
  label: 'label',
  children: 'children'
}
const cascaderValue = ref<number | string | undefined>(undefined)
const selectedCategoryId = ref<number | undefined>(undefined)

/* === 属性列表 === */
const attributeList = ref<CategoryAttribute[]>([])
const loading = ref(false)
const savingId = ref<number | null>(null)

/* === 预设值输入框状态（按属性下标） === */
const presetInputVisible = reactive<Record<number, boolean>>({})
const presetInputValue = reactive<Record<number, string>>({})

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

/* === 分类选择变化 === */
function handleCategoryChange(value: unknown): void {
  if (value == null) {
    selectedCategoryId.value = undefined
    attributeList.value = []
    return
  }
  let catId: number | undefined
  if (Array.isArray(value)) {
    const last = value[value.length - 1]
    catId = typeof last === 'number' ? last : Number(last)
  } else {
    catId = typeof value === 'number' ? value : Number(value)
  }
  selectedCategoryId.value = catId
  if (catId) {
    loadAttributes()
  }
}

/* === 加载分类的规格属性 === */
async function loadAttributes(): Promise<void> {
  if (!selectedCategoryId.value) return
  loading.value = true
  try {
    const res = await getCategoryAttributes(selectedCategoryId.value)
    attributeList.value = res.data || []
  } catch {
    attributeList.value = []
  } finally {
    loading.value = false
  }
}

/* === 保存属性（新增 or 更新） === */
async function saveAttribute(attr: CategoryAttribute, idx: number): Promise<void> {
  if (!attr.name || !attr.name.trim()) {
    ElMessage.warning('请填写属性名')
    return
  }
  if (!selectedCategoryId.value) {
    ElMessage.warning('请先选择分类')
    return
  }
  savingId.value = idx
  try {
    if (attr.id) {
      await updateCategoryAttribute(attr.id, attr)
    } else {
      attr.categoryId = selectedCategoryId.value
      const res = await createCategoryAttribute(attr)
      attr.id = res.data.id
    }
    ElMessage.success('保存成功')
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    savingId.value = null
  }
}

/* === 删除属性 === */
async function deleteAttribute(attr: CategoryAttribute, idx: number): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除属性「${attr.name || '未命名'}」及其预设值？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return // 用户取消
  }
  if (attr.id) {
    try {
      await deleteCategoryAttribute(attr.id)
      ElMessage.success('删除成功')
    } catch {
      return
    }
  }
  attributeList.value.splice(idx, 1)
}

/* === 新增属性（前端临时对象，未保存） === */
function addNewAttribute(): void {
  if (!selectedCategoryId.value) {
    ElMessage.warning('请先选择分类')
    return
  }
  attributeList.value.push({
    categoryId: selectedCategoryId.value,
    name: '',
    type: 'TEXT',
    inputType: 'SELECT',
    isRequired: 0,
    sortOrder: attributeList.value.length,
    values: []
  })
}

/* === 显示预设值输入框 === */
function showPresetInput(idx: number): void {
  presetInputVisible[idx] = true
  presetInputValue[idx] = ''
}

/* === 确认添加预设值 === */
function confirmPresetValue(idx: number): void {
  const attr = attributeList.value[idx]
  const val = (presetInputValue[idx] || '').trim()
  if (val && !attr.values.some((v) => v.value === val)) {
    attr.values.push({
      value: val,
      imageUrl: attr.type === 'IMAGE' ? '' : undefined,
      sortOrder: attr.values.length
    })
  }
  presetInputVisible[idx] = false
  presetInputValue[idx] = ''
}

/* === 删除预设值 === */
function removePresetValue(attr: CategoryAttribute, i: number): void {
  attr.values.splice(i, 1)
}

/* === 初始化 ===
 *  使用 onMounted 钩子（auto-imports 已配置自动导入）
 */

onMounted(() => {
  fetchCategoryTree()
})
</script>

<style scoped>
/* === 页面容器 === */
.category-attribute-manage {
  padding: 20px;
}

/* === 页面头部 === */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* === 内容卡片 === */
.content-card {
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  min-height: 400px;
}

/* === 空提示 === */
.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--color-text-secondary);
}

.empty-tip p {
  margin-top: 12px;
  font-size: 14px;
}

.empty-attr {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 40px 0;
  font-size: 13px;
}

/* === 属性卡片 === */
.attribute-card {
  background: var(--color-bg-subtle, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
}

.attribute-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.attribute-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-secondary);
}

.attribute-card-body {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.switch-wrap {
  display: inline-flex;
  align-items: center;
}

/* === 预设值区域 === */
.preset-values {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--color-border, #e5e7eb);
}

.preset-values-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.preset-values-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.color-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
  border: 1px solid var(--color-border, #e5e7eb);
  vertical-align: middle;
}

/* === 卡片操作按钮 === */
.attribute-card-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

/* === 新增属性区域 === */
.add-attribute-area {
  margin-top: 8px;
}

/* === 响应式 === */
@media (max-width: 768px) {
  .category-attribute-manage {
    padding: 12px;
  }

  .content-card {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .attribute-card-body {
    gap: 8px;
  }
}
</style>