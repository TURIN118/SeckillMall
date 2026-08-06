/**
 * 分类树 Store - 缓存全站分类树, 避免多个页面重复请求 getCategoryTree
 *
 * 使用场景:
 *   - ProductList.vue 左侧分类树
 *   - Home.vue 首页分类侧边栏
 *   - 其他需要分类数据的组件
 *
 * 缓存策略:
 *   - 首次调用 fetchTree() 拉取并缓存, 后续调用直接返回 (loaded 标记)
 *   - 传 force=true 可强制刷新 (分类变更后调用)
 *   - reset() 用于退出登录等需要清空缓存的场景
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCategoryTree } from '@/api/category'
import type { CategoryTreeNode } from '@/types'

export const useCategoryStore = defineStore('category', () => {
  /** 分类树数据 (一级分类, children 含二级分类) */
  const tree = ref<CategoryTreeNode[]>([])
  /** 是否已加载 (避免重复请求) */
  const loaded = ref(false)

  /**
   * 拉取分类树
   * @param force 是否强制刷新 (忽略缓存)
   */
  async function fetchTree(force = false): Promise<void> {
    if (loaded.value && !force) return
    try {
      const res = await getCategoryTree()
      // 后端返回树形结构, 一级分类对象中包含 children 数组存放二级分类
      // 接口签名是 CategoryVO[], 但实际数据满足 CategoryTreeNode 结构, 做类型断言
      tree.value = (res.data as CategoryTreeNode[]) || []
      loaded.value = true
    } catch {
      tree.value = []
    }
  }

  /** 重置缓存 (退出登录或分类数据失效时调用) */
  function reset(): void {
    tree.value = []
    loaded.value = false
  }

  return { tree, loaded, fetchTree, reset }
})