<template>
  <div v-if="showPagination" class="pagination-wrapper">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="currentSize"
      :page-sizes="pageSizes"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 分页包装组件 C06
 * 参照 10-ai-design-spec.md C06 规范
 * 使用 el-pagination
 */
import { ref, computed, watch } from 'vue'

interface Props {
  /** 总记录数 */
  total: number
  /** 当前页 */
  pageNum?: number
  /** 每页条数 */
  pageSize?: number
  /** 可选每页条数 */
  pageSizes?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  pageNum: 1,
  pageSize: 10,
  pageSizes: () => [10, 20, 50]
})

const emit = defineEmits<{
  (e: 'update:pageNum', value: number): void
  (e: 'update:pageSize', value: number): void
  (e: 'change', payload: { pageNum: number; pageSize: number }): void
}>()

const currentPage = ref(props.pageNum)
const currentSize = ref(props.pageSize)

watch(
  () => props.pageNum,
  (val) => {
    currentPage.value = val
  }
)

watch(
  () => props.pageSize,
  (val) => {
    currentSize.value = val
  }
)

const showPagination = computed(() => props.total > 0)

function handleSizeChange(size: number): void {
  emit('update:pageSize', size)
  emit('change', { pageNum: 1, pageSize: size })
}

function handlePageChange(page: number): void {
  emit('update:pageNum', page)
  emit('change', { pageNum: page, pageSize: currentSize.value })
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>