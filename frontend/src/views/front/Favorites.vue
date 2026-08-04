<template>
  <!-- 收藏夹页面：参照 UserAddress.vue / Home.vue 卡片风格 -->
  <div class="favorites-page">
    <!-- 页头 -->
    <div class="favorites-header">
      <h2 class="favorites-title">我的收藏</h2>
      <span v-if="favoriteList.length > 0" class="favorites-count">共 {{ favoriteList.length }} 件商品</span>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span class="loading-text">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="favoriteList.length === 0" class="empty-state">
      <el-empty description="还没有收藏任何商品，快去发现心仪好物吧！" :image-size="120" />
      <button class="btn-sm primary" type="button" @click="router.push('/products')">去逛逛</button>
    </div>

    <!-- 收藏商品卡片网格 -->
    <div v-else class="favorites-grid">
      <div v-for="item in favoriteList" :key="item.id" class="fav-card"
        :class="{ disabled: item.productStatus !== 'ON_SALE' }">
        <!-- 商品图片 (点击跳转详情) -->
        <div class="fav-card-img" @click="goProductDetail(item.productId)">
          <img v-if="item.mainImage" :src="formatImageUrl(item.mainImage)" :alt="item.productName" class="fav-img-tag"
            loading="lazy" />
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="fav-img-placeholder">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="m21 15-5-5L5 21" />
          </svg>
          <!-- 下架遮罩 -->
          <div v-if="item.productStatus !== 'ON_SALE'" class="off-shelf-mask">
            <span>已下架</span>
          </div>
        </div>

        <!-- 卡片内容 -->
        <div class="fav-card-body">
          <div class="fav-name" @click="goProductDetail(item.productId)" :title="item.productName">
            {{ item.productName }}
          </div>
          <div class="fav-meta">
            <span class="fav-price">¥{{ formatPrice(item.originalPrice) }}</span>
            <span class="fav-sales">已售 {{ item.salesCount || 0 }} 件</span>
          </div>
          <div class="fav-actions">
            <button class="btn-sm primary" type="button"
              :disabled="item.productStatus !== 'ON_SALE' || addingId === item.productId"
              @click="handleAddToCart(item)">
              <span v-if="addingId === item.productId">加入中...</span>
              <span v-else>加入购物车</span>
            </button>
            <button class="btn-sm text danger" type="button" :disabled="removingId === item.productId"
              @click="handleRemoveFavorite(item)">
              <span v-if="removingId === item.productId">取消中...</span>
              <span v-else>取消收藏</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 收藏夹页面 (前台)
 * 对接后端 /api/v1/favorites 接口，无模拟数据。
 */
defineOptions({ name: 'Favorites' })
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { addCart } from '@/api/cart'
import { useCartStore } from '@/stores/cart'
import { formatImageUrl } from '@/utils/image'
import type { FavoriteItemVO } from '@/types'

const router = useRouter()
const cartStore = useCartStore()

/** 收藏夹列表 (后端返回) */
const favoriteList = ref<FavoriteItemVO[]>([])

/** 列表加载中 */
const loading = ref<boolean>(false)

/** 正在加入购物车的商品 ID (按钮 loading 态) */
const addingId = ref<number | string | null>(null)

/** 正在取消收藏的商品 ID (按钮 loading 态) */
const removingId = ref<number | string | null>(null)

/* === 工具函数 === */

/** 格式化价格 (保留两位小数) */
function formatPrice(value: number): string {
  return (value || 0).toFixed(2)
}

/** 加载收藏夹列表 */
async function loadFavoriteList(): Promise<void> {
  loading.value = true
  try {
    const res = await getFavoriteList()
    favoriteList.value = res.data ?? []
  } catch {
    favoriteList.value = []
  } finally {
    loading.value = false
  }
}

/* === 事件处理 === */

/** 跳转商品详情 */
function goProductDetail(id: number | string): void {
  router.push(`/products/${id}`)
}

/** 加入购物车 */
async function handleAddToCart(item: FavoriteItemVO): Promise<void> {
  if (item.productStatus !== 'ON_SALE') {
    ElMessage.warning('该商品已下架，无法加入购物车')
    return
  }
  addingId.value = item.productId
  try {
    await addCart({ productId: item.productId, quantity: 1 })
    ElMessage.success('已加入购物车')
    // 刷新顶部导航购物车数量徽标
    await cartStore.fetchCount()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    addingId.value = null
  }
}

/** 取消收藏 */
async function handleRemoveFavorite(item: FavoriteItemVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定取消收藏"${item.productName}"吗？`, '取消收藏确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  removingId.value = item.productId
  try {
    await removeFavorite(item.productId)
    ElMessage.success('已取消收藏')
    // 从列表中移除 (乐观更新, 避免整页刷新)
    favoriteList.value = favoriteList.value.filter(f => f.productId !== item.productId)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    removingId.value = null
  }
}

// 页面挂载时加载收藏夹列表
onMounted(() => {
  loadFavoriteList()
})

// keep-alive 缓存后, 再次激活时刷新收藏夹列表 (保证从其他页面返回时数据新鲜)
onActivated(() => {
  loadFavoriteList()
})
</script>

<style scoped>
/* 参照 UserAddress.vue .address-page 样式 */
.favorites-page {
  padding: 24px;
}

/* 页头 */
.favorites-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.favorites-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.favorites-count {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 加载中状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px 40px;
  text-align: center;
  gap: 16px;
}

/* 收藏商品卡片网格 (每行 4 个) */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* 收藏卡片 */
.fav-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
}

.fav-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.fav-card.disabled {
  opacity: 0.75;
}

/* 卡片图片 */
.fav-card-img {
  width: 100%;
  height: 200px;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.fav-img-tag {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.fav-card:hover .fav-img-tag {
  transform: scale(1.05);
}

.fav-img-placeholder {
  width: 48px;
  height: 48px;
  color: #ccc;
}

/* 下架遮罩 */
.off-shelf-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

/* 卡片内容 */
.fav-card-body {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.fav-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.fav-name:hover {
  color: var(--color-primary);
}

.fav-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.fav-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'DIN Alternate', 'Roboto', 'Arial', sans-serif;
}

.fav-sales {
  font-size: 12px;
  color: var(--color-text-muted);
}

.fav-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.fav-actions .btn-sm {
  flex: 1;
  justify-content: center;
}

/* 小按钮 (对照 UserAddress.vue .btn-sm) */
.btn-sm {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.15s;
}

.btn-sm:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover:not(:disabled) {
  background: var(--btn-hover);
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  padding: 6px 8px;
}

.btn-sm.text:hover:not(:disabled) {
  color: var(--color-primary);
}

.btn-sm.text.danger:hover:not(:disabled) {
  color: var(--color-primary);
}

/* 响应式 */
@media (max-width: 1200px) {
  .favorites-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .fav-card-img {
    height: 160px;
  }

  .fav-price {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
}
</style>