/**
 * 分类 Store（对齐 plan.md 第 5.1 节）
 * 职责：categoryList / fetchCategories
 * 持久化：不持久化（每次进入页面重新拉取）
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as categoryApi from '@/api/category'
import type { CategoryVO } from '@/types'

export const useCategoryStore = defineStore('category', () => {
  const categoryList = ref<CategoryVO[]>([])
  const currentCategory = ref<CategoryVO | null>(null)
  const loading = ref<boolean>(false)

  /** 拉取分类列表 */
  async function fetchCategories() {
    loading.value = true
    try {
      categoryList.value = await categoryApi.getCategoryList()
    } finally {
      loading.value = false
    }
  }

  /** 设置当前分类 */
  function setCurrentCategory(category: CategoryVO | null) {
    currentCategory.value = category
  }

  /** 根据 ID 查找分类 */
  function findCategoryById(id: string): CategoryVO | undefined {
    return categoryList.value.find(c => c.id === id)
  }

  return {
    categoryList,
    currentCategory,
    loading,
    fetchCategories,
    setCurrentCategory,
    findCategoryById
  }
})