<!--
  我的收藏页（对齐 spec.md 3.9 收藏夹 / tasks.md T5.3）
  - 调用 favoriteApi.getFavoriteList()
  - 2 列网格商品卡片
  - 排序：综合 / 时间 / 价格（顶部 ActionSheet）
  - 管理模式：u-checkbox 批量选择 + 底部固定操作栏 + 取消收藏
  - u-swipe-action 左滑取消收藏
  - 触底加载 + 下拉刷新
-->
<template>
  <view class="favorites-page">
    <!-- 顶部操作栏 -->
    <view class="top-bar">
      <view class="sort-btn" @tap="showSortSheet = true">
        <text class="sort-text">{{ currentSortText }}</text>
        <u-icon name="arrow-down" size="24" color="#606266" />
      </view>
      <view class="manage-btn" @tap="toggleManage">
        <text class="manage-text">{{ manageMode ? '完成' : '管理' }}</text>
      </view>
    </view>

    <!-- 收藏列表 -->
    <view v-if="favoriteList.length > 0" class="list-wrap">
      <view class="grid">
        <view
          v-for="item in favoriteList"
          :key="item.id"
          class="grid-item"
        >
          <u-swipe-action
            :options="manageMode ? [] : swipeOptions"
            @click="() => onSwipeCancel(item)"
          >
            <view class="card-wrapper">
              <!-- 管理模式下显示复选框 -->
              <view v-if="manageMode" class="check-box" @tap.stop="toggleSelect(item)">
                <u-icon
                  :name="isSelected(item) ? 'checkmark-circle-fill' : 'circle'"
                  :color="isSelected(item) ? '#ff4d4f' : '#c0c4cc'"
                  size="40"
                />
              </view>

              <!-- 商品卡片（简化版，基于 FavoriteVO） -->
              <view class="favorite-card" @tap="goProductDetail(item)">
                <view class="image-wrap">
                  <u-image
                    :src="item.productImage || ''"
                    mode="aspectFill"
                    width="100%"
                    height="340rpx"
                    :lazy-load="true"
                    radius="8rpx 8rpx 0 0"
                  />
                </view>
                <view class="info">
                  <text class="title ellipsis-2">{{ item.productName }}</text>
                  <view class="price-row">
                    <text class="price-symbol">¥</text>
                    <text class="price-current">{{ formatPrice(item.price) }}</text>
                  </view>
                  <view class="time-row">
                    <text class="time-text">{{ formatDate(item.createdAt) }}</text>
                  </view>
                </view>
              </view>
            </view>
          </u-swipe-action>
        </view>
      </view>

      <!-- 加载更多 -->
      <u-loadmore
        :status="loadMoreStatus"
        :contentText="loadMoreText"
      />
    </view>

    <!-- 空状态 -->
    <u-empty
      v-else-if="!loading"
      mode="heart"
      text="暂无收藏商品"
      marginTop="120"
    >
      <u-button
        slot="bottom"
        type="error"
        shape="circle"
        plain
        @click="goHome"
      >去逛逛</u-button>
    </u-empty>

    <!-- 管理模式底部操作栏 -->
    <view v-if="manageMode && favoriteList.length > 0" class="footer-bar">
      <view class="select-all" @tap="toggleSelectAll">
        <u-icon
          :name="isAllSelected ? 'checkmark-circle-fill' : 'circle'"
          :color="isAllSelected ? '#ff4d4f' : '#c0c4cc'"
          size="40"
        />
        <text class="select-all-text">全选</text>
      </view>
      <view class="op-btns">
        <u-button
          type="error"
          shape="circle"
          :disabled="selectedIds.length === 0"
          @click="onBatchCancel"
        >取消收藏 ({{ selectedIds.length }})</u-button>
      </view>
    </view>

    <!-- 排序 ActionSheet -->
    <u-action-sheet
      :show="showSortSheet"
      :actions="sortActions"
      @close="showSortSheet = false"
      @select="onSortSelect"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad, onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import * as favoriteApi from '@/api/favorite'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showConfirm, showToast, showLoading, hideLoading } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import type { FavoriteVO } from '@/types'

const favoriteList = ref<FavoriteVO[]>([])
const loading = ref<boolean>(false)
const manageMode = ref<boolean>(false)
const selectedIds = ref<Set<string>>(new Set())
const showSortSheet = ref<boolean>(false)

/** 分页参数 */
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
  hasMore: true
})

/** 排序参数 */
const sortState = reactive<{ sortBy: string; sortOrder: 'asc' | 'desc' }>({
  sortBy: 'time',
  sortOrder: 'desc'
})

/** u-swipe-action 操作按钮 */
const swipeOptions = [
  { text: '取消收藏', style: { backgroundColor: '#ff4d4f' } }
]

/** 排序选项 */
const sortActions = [
  { name: '综合排序', value: { sortBy: 'time', sortOrder: 'desc' } },
  { name: '收藏时间 newest', value: { sortBy: 'time', sortOrder: 'desc' } },
  { name: '收藏时间最早', value: { sortBy: 'time', sortOrder: 'asc' } },
  { name: '价格从低到高', value: { sortBy: 'price', sortOrder: 'asc' } },
  { name: '价格从高到低', value: { sortBy: 'price', sortOrder: 'desc' } }
]

/** 当前排序显示文本 */
const currentSortText = computed(() => {
  if (sortState.sortBy === 'time' && sortState.sortOrder === 'desc') return '最新收藏'
  if (sortState.sortBy === 'time' && sortState.sortOrder === 'asc') return '最早收藏'
  if (sortState.sortBy === 'price' && sortState.sortOrder === 'asc') return '价格升序'
  if (sortState.sortBy === 'price' && sortState.sortOrder === 'desc') return '价格降序'
  return '综合排序'
})

/** 加载更多状态 */
const loadMoreStatus = computed<'loadmore' | 'loading' | 'nomore'>(() => {
  if (loading.value) return 'loading'
  if (!pagination.hasMore) return 'nomore'
  return 'loadmore'
})

const loadMoreText = {
  loadmore: '上拉加载更多',
  loading: '加载中...',
  nomore: '没有更多了'
}

/** 是否全选 */
const isAllSelected = computed(() => {
  if (favoriteList.value.length === 0) return false
  return favoriteList.value.every(item => selectedIds.value.has(ensureStringId(item.id)))
})

onLoad(() => {
  if (!requireAuthAsync()) return
})

onShow(() => {
  // 非管理模式下刷新列表
  if (!manageMode.value) {
    resetAndFetch()
  }
})

/** 重置并拉取第一页 */
async function resetAndFetch() {
  pagination.page = 1
  pagination.hasMore = true
  favoriteList.value = []
  await fetchList()
}

/** 拉取收藏列表 */
async function fetchList() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await favoriteApi.getFavoriteList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      sortBy: sortState.sortBy,
      sortOrder: sortState.sortOrder
    })
    const list = res?.list || []
    if (pagination.page === 1) {
      favoriteList.value = list
    } else {
      favoriteList.value = [...favoriteList.value, ...list]
    }
    pagination.total = res?.total || 0
    pagination.hasMore = !!res?.hasMore
  } catch (e) {
    console.error('拉取收藏列表失败', e)
    showToast('收藏列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 触底加载 */
onReachBottom(() => {
  if (!pagination.hasMore || loading.value) return
  pagination.page++
  fetchList()
})

/** 下拉刷新 */
onPullDownRefresh(() => {
  resetAndFetch().finally(() => {
    uni.stopPullDownRefresh()
  })
})

/** 切换管理模式 */
function toggleManage() {
  manageMode.value = !manageMode.value
  if (!manageMode.value) {
    // 退出管理模式清空选择
    selectedIds.value.clear()
  }
}

/** 切换选中 */
function toggleSelect(item: FavoriteVO) {
  const id = ensureStringId(item.id)
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
  // 触发响应式
  selectedIds.value = new Set(selectedIds.value)
}

/** 是否选中 */
function isSelected(item: FavoriteVO): boolean {
  return selectedIds.value.has(ensureStringId(item.id))
}

/** 全选/反选 */
function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value.clear()
  } else {
    favoriteList.value.forEach(item => {
      selectedIds.value.add(ensureStringId(item.id))
    })
  }
  selectedIds.value = new Set(selectedIds.value)
}

/** 排序选择 */
function onSortSelect(action: any) {
  if (action?.value) {
    sortState.sortBy = action.value.sortBy
    sortState.sortOrder = action.value.sortOrder
    showSortSheet.value = false
    resetAndFetch()
  }
}

/** 左滑取消收藏 */
async function onSwipeCancel(item: FavoriteVO) {
  const confirmed = await showConfirm(`确定取消收藏"${item.productName}"吗？`, '取消收藏')
  if (!confirmed) return
  try {
    showLoading('取消中...')
    await favoriteApi.removeFavorite(ensureStringId(item.id))
    showToast('已取消收藏', 'success')
    // 本地移除
    favoriteList.value = favoriteList.value.filter(f => f.id !== item.id)
    pagination.total = Math.max(0, pagination.total - 1)
  } catch (e) {
    console.error('取消收藏失败', e)
    showToast('取消失败', 'error')
  } finally {
    hideLoading()
  }
}

/** 批量取消收藏 */
async function onBatchCancel() {
  if (selectedIds.value.size === 0) {
    showToast('请先选择要取消的收藏', 'none')
    return
  }
  const confirmed = await showConfirm(
    `确定取消选中的 ${selectedIds.value.size} 项收藏吗？`,
    '批量取消收藏'
  )
  if (!confirmed) return
  try {
    showLoading('取消中...')
    const ids = Array.from(selectedIds.value)
    await favoriteApi.batchRemoveFavorites(ids)
    showToast('批量取消成功', 'success')
    // 本地移除
    favoriteList.value = favoriteList.value.filter(f => !selectedIds.value.has(ensureStringId(f.id)))
    pagination.total = Math.max(0, pagination.total - ids.length)
    selectedIds.value.clear()
    // 若全部取消完，退出管理模式
    if (favoriteList.value.length === 0) {
      manageMode.value = false
    }
  } catch (e) {
    console.error('批量取消收藏失败', e)
    showToast('批量取消失败', 'error')
  } finally {
    hideLoading()
  }
}

/** 跳转商品详情 */
function goProductDetail(item: FavoriteVO) {
  if (manageMode.value) {
    toggleSelect(item)
    return
  }
  navigate.to('pages-product/pages/product-detail/product-detail', {
    id: ensureStringId(item.productId)
  })
}

/** 跳转首页 */
function goHome() {
  navigate.to('pages/home/home')
}

/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 格式化日期 */
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  // 简化：取 YYYY-MM-DD
  return dateStr.slice(0, 10)
}
</script>

<style lang="scss" scoped>
.favorites-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
}

/* 顶部操作栏 */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #f0f0f0;

  .sort-btn {
    display: flex;
    align-items: center;
    gap: 6rpx;

    .sort-text {
      font-size: 28rpx;
      color: #606266;
    }
  }

  .manage-btn {
    .manage-text {
      font-size: 28rpx;
      color: #ff4d4f;
    }
  }
}

/* 列表 */
.list-wrap {
  padding: 20rpx 20rpx 0;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 20rpx;

  .grid-item {
    width: calc((100% - 20rpx) / 2);
    box-sizing: border-box;

    .card-wrapper {
      position: relative;
      background-color: #ffffff;
      border-radius: 8rpx;
      overflow: hidden;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

      .check-box {
        position: absolute;
        top: 12rpx;
        left: 12rpx;
        z-index: 10;
        background-color: rgba(255, 255, 255, 0.9);
        border-radius: 50%;
        padding: 4rpx;
      }
    }
  }
}

/* 收藏卡片 */
.favorite-card {
  background-color: #ffffff;

  .image-wrap {
    width: 100%;
    height: 340rpx;
    background-color: #f5f5f5;
  }

  .info {
    padding: 16rpx 20rpx 20rpx;
    display: flex;
    flex-direction: column;
    gap: 8rpx;

    .title {
      font-size: 26rpx;
      color: #303133;
      line-height: 1.4;
      font-weight: 500;
    }

    .price-row {
      display: flex;
      align-items: baseline;

      .price-symbol {
        font-size: 22rpx;
        color: #ff4d4f;
        font-weight: bold;
      }

      .price-current {
        font-size: 32rpx;
        color: #ff4d4f;
        font-weight: bold;
        margin-left: 2rpx;
      }
    }

    .time-row {
      .time-text {
        font-size: 22rpx;
        color: #c0c4cc;
      }
    }
  }
}

/* 底部操作栏 */
.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);

  .select-all {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .select-all-text {
      font-size: 28rpx;
      color: #303133;
    }
  }
}

/* 省略 */
.ellipsis-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>