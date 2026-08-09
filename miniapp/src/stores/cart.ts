/**
 * 购物车 Store（对齐 plan.md 第 5.1 节）
 * 职责：cartList / cartCount / selectedItems / addToCart / updateQuantity / removeItem
 * 持久化：不持久化（每次进入页面重新拉取）
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as cartApi from '@/api/cart'
import type { CartItemVO, AddCartRequest } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const cartList = ref<CartItemVO[]>([])
  const loading = ref<boolean>(false)

  /** 选中商品列表 */
  const selectedItems = computed(() => cartList.value.filter(item => item.selected))

  /** 选中商品数量 */
  const selectedCount = computed(() => selectedItems.value.length)

  /** 选中商品总价 */
  const totalAmount = computed(() =>
    selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  /** 购物车总数量 */
  const totalCount = computed(() =>
    cartList.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  /** 是否全选 */
  const isAllSelected = computed(() =>
    cartList.value.length > 0 && cartList.value.every(item => item.selected)
  )

  /** 拉取购物车列表 */
  async function fetchCartList() {
    loading.value = true
    try {
      cartList.value = await cartApi.getCartList()
    } finally {
      loading.value = false
    }
  }

  /** 加入购物车 */
  async function addToCart(data: AddCartRequest) {
    await cartApi.addToCart(data)
    await fetchCartList()
  }

  /** 修改数量 */
  async function updateQuantity(id: string, quantity: number) {
    await cartApi.updateCartQuantity(id, { quantity })
    const item = cartList.value.find(i => i.id === id)
    if (item) item.quantity = quantity
  }

  /** 删除单项 */
  async function removeItem(id: string) {
    await cartApi.removeCartItem(id)
    cartList.value = cartList.value.filter(i => i.id !== id)
  }

  /** 清空购物车 */
  async function clearCart() {
    await cartApi.clearCart()
    cartList.value = []
  }

  /** 切换选中状态 */
  async function toggleSelected(id: string, selected: boolean) {
    await cartApi.updateCartSelected(id, selected)
    const item = cartList.value.find(i => i.id === id)
    if (item) item.selected = selected
  }

  /** 批量选中 */
  async function batchSelected(ids: string[], selected: boolean) {
    await cartApi.batchUpdateSelected({ ids, selected })
    cartList.value.forEach(item => {
      if (ids.includes(item.id)) item.selected = selected
    })
  }

  /** 全选/反选 */
  async function toggleSelectAll(selected: boolean) {
    const ids = cartList.value.map(i => i.id)
    await batchSelected(ids, selected)
  }

  return {
    cartList,
    loading,
    selectedItems,
    selectedCount,
    totalAmount,
    totalCount,
    isAllSelected,
    fetchCartList,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart,
    toggleSelected,
    batchSelected,
    toggleSelectAll
  }
})