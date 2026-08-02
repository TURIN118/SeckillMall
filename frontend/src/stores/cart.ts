/**
 * 购物车 Store - 管理购物车数量(用于导航栏徽标实时更新)
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCartCount } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
    /* === State === */
    /** 购物车商品数量(用于导航栏徽标) */
    const count = ref<number>(0)

    /* === Actions === */

    /** 拉取购物车数量 */
    async function fetchCount(): Promise<void> {
        try {
            const res = await getCartCount()
            count.value = res.data ?? 0
        } catch {
            // 错误已由全局拦截器统一提示, 不阻塞 UI
            count.value = 0
        }
    }

    /** 重置(退出登录时调用) */
    function reset(): void {
        count.value = 0
    }

    /** 增量更新(添加商品后本地 +N, 避免重复请求) */
    function increment(n: number = 1): void {
        count.value += n
    }

    /** 减量更新(删除商品后本地 -N) */
    function decrement(n: number = 1): void {
        count.value = Math.max(0, count.value - n)
    }

    return {
        count,
        fetchCount,
        reset,
        increment,
        decrement
    }
})