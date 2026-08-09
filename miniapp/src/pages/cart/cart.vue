<!--
  购物车页 Cart（对齐 spec.md 3.4 / tasks.md T3.1）
  功能点：
    - 商品列表展示（商品信息 + 选中状态 + 数量）
    - 左滑删除（u-swipe-action）
    - 数量修改（u-number-box 步进器 + 实时更新）
    - 批量选中（u-checkbox 全选/反选 + 底部结算栏更新）
    - 选中结算（底部固定结算栏，跳转 checkout 传 cartIds）
    - 空购物车状态展示
  鉴权：是（requiresAuth）
  tabBar：是（购物车 tab）
-->
<template>
  <view class="cart-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">购物车</text>
        <text
          v-if="cartStore.cartList.length > 0"
          class="nav-action"
          @click="handleToggleManage"
        >{{ isManageMode ? '完成' : '管理' }}</text>
      </view>
    </view>

    <!-- 占位高度（与导航栏同高） -->
    <view class="nav-placeholder" :style="{ height: statusBarHeight + 44 + 'px' }"></view>

    <!-- 主体内容 -->
    <view class="content">
      <!-- 空购物车 -->
      <view v-if="!cartStore.loading && cartStore.cartList.length === 0" class="empty-state">
        <u-empty
          mode="car"
          text="购物车空空如也"
          icon-size="180"
          margin-top="120"
        />
        <u-button
          type="error"
          shape="circle"
          text="去逛逛"
          custom-style="margin-top: 40rpx; width: 280rpx;"
          @click="goHome"
        />
      </view>

      <!-- 购物车列表 -->
      <view v-else class="cart-list">
        <u-swipe-action
          v-for="item in cartStore.cartList"
          :key="item.id"
          :show="false"
          :options="swipeOptions"
          @click="(index: number) => handleSwipeAction(item, index)"
        >
          <view class="cart-item">
            <!-- 选中框 -->
            <view class="check-col" @click.stop="handleToggleItem(item)">
              <u-checkbox-group>
                <u-checkbox
                  :modelValue="item.selected"
                  shape="circle"
                  active-color="#FF4D4F"
                  size="40rpx"
                  @update:modelValue="(val: boolean) => handleToggleItem(item, val)"
                />
              </u-checkbox-group>
            </view>

            <!-- 商品图 -->
            <view class="image-col">
              <u-image
                :src="item.productImage || ''"
                mode="aspectFill"
                width="160rpx"
                height="160rpx"
                radius="8rpx"
                :lazy-load="true"
              />
            </view>

            <!-- 商品信息 -->
            <view class="info-col">
              <text class="product-name ellipsis-2">{{ item.productName }}</text>
              <text v-if="item.skuSpec" class="sku-spec ellipsis-1">{{ item.skuSpec }}</text>
              <view class="price-row">
                <text class="price-symbol">¥</text>
                <text class="price-value">{{ formatPrice(item.price) }}</text>
                <view class="number-box-wrap">
                  <u-number-box
                    :modelValue="item.quantity"
                    :min="1"
                    :max="Math.max(1, item.stock)"
                    :step="1"
                    integer
                    @change="(val: number) => handleQuantityChange(item, val)"
                  />
                </view>
              </view>
            </view>
          </view>
        </u-swipe-action>
      </view>
    </view>

    <!-- 底部结算栏 -->
    <view
      v-if="cartStore.cartList.length > 0"
      class="bottom-bar"
      :style="{ paddingBottom: safeBottom + 'px' }"
    >
      <view class="bottom-left" @click="handleToggleAll">
        <u-checkbox-group>
          <u-checkbox
            :modelValue="cartStore.isAllSelected"
            shape="circle"
            active-color="#FF4D4F"
            size="40rpx"
            @update:modelValue="(val: boolean) => handleToggleAll(val)"
          />
        </u-checkbox-group>
        <text class="all-label">全选</text>
      </view>

      <view class="bottom-center">
        <text class="total-label">合计：</text>
        <text class="total-symbol">¥</text>
        <text class="total-amount">{{ formatPrice(cartStore.totalAmount) }}</text>
      </view>

      <view class="bottom-right">
        <u-button
          v-if="!isManageMode"
          type="error"
          shape="circle"
          :disabled="cartStore.selectedCount === 0"
          :custom-style="`width: 200rpx; ${cartStore.selectedCount === 0 ? 'opacity: 0.5;' : ''}`"
          :text="`去结算(${cartStore.selectedCount})`"
          @click="handleCheckout"
        />
        <u-button
          v-else
          type="error"
          shape="circle"
          :disabled="cartStore.selectedCount === 0"
          :custom-style="`width: 200rpx; ${cartStore.selectedCount === 0 ? 'opacity: 0.5;' : ''}`"
          text="删除"
          @click="handleBatchDelete"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { useCartStore } from '@/stores/cart'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showConfirm } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import type { CartItemVO } from '@/types'

const cartStore = useCartStore()

/** 状态栏高度（自定义导航栏适配） */
const statusBarHeight = ref<number>(20)
/** 安全区域底部高度（适配 iPhone X 系列） */
const safeBottom = ref<number>(0)
/** 管理模式（用于批量删除） */
const isManageMode = ref<boolean>(false)

/** 左滑操作按钮配置 */
const swipeOptions = [
  { text: '删除', style: { backgroundColor: '#FF4D4F' } }
]

/** 格式化价格（保留 2 位小数） */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 读取系统信息（状态栏高度 + 安全区域） */
function loadSystemInfo() {
  try {
    const sysInfo = uni.getSystemInfoSync()
    statusBarHeight.value = sysInfo.statusBarHeight || 20
    // #ifdef MP-WEIXIN
    const winInfo = (uni.getWindowInfo ? uni.getWindowInfo() : null)
    if (winInfo && winInfo.safeAreaInsets) {
      safeBottom.value = winInfo.safeAreaInsets.bottom || 0
    }
    // #endif
    if (safeBottom.value === 0 && sysInfo.safeArea) {
      safeBottom.value = sysInfo.screenHeight - sysInfo.safeArea.bottom
    }
  } catch (e) {
    console.error('读取系统信息失败', e)
  }
}

/** 拉取购物车列表 */
async function loadCartList() {
  try {
    await cartStore.fetchCartList()
  } catch (e) {
    console.error('拉取购物车列表失败', e)
    showToast('加载失败，请下拉刷新', 'error')
  }
}

/** 切换管理模式 */
function handleToggleManage() {
  isManageMode.value = !isManageMode.value
}

/** 跳转首页 */
function goHome() {
  navigate.to('pages/home/home')
}

/** 切换单项选中状态 */
async function handleToggleItem(item: CartItemVO, val?: boolean) {
  const next = typeof val === 'boolean' ? val : !item.selected
  try {
    await cartStore.toggleSelected(ensureStringId(item.id), next)
  } catch (e) {
    console.error('切换选中失败', e)
    showToast('操作失败', 'error')
  }
}

/** 全选/反选 */
async function handleToggleAll(val?: boolean) {
  const next = typeof val === 'boolean' ? val : !cartStore.isAllSelected
  try {
    await cartStore.toggleSelectAll(next)
  } catch (e) {
    console.error('全选失败', e)
    showToast('操作失败', 'error')
  }
}

/** 数量修改 */
async function handleQuantityChange(item: CartItemVO, val: number) {
  // 防御：val 为 0 或负数时跳过（u-number-box min=1 已限制）
  if (!val || val < 1) return
  // 数量未变化直接返回
  if (val === item.quantity) return
  // 超出库存提示
  if (val > item.stock) {
    showToast(`库存仅剩 ${item.stock} 件`, 'none')
    return
  }
  try {
    await cartStore.updateQuantity(ensureStringId(item.id), val)
  } catch (e) {
    console.error('修改数量失败', e)
    showToast('修改数量失败', 'error')
    // 失败时重新拉取列表以同步状态
    await loadCartList()
  }
}

/** 左滑操作（删除） */
async function handleSwipeAction(item: CartItemVO, index: number) {
  if (index === 0) {
    await handleDeleteItem(item)
  }
}

/** 删除单项 */
async function handleDeleteItem(item: CartItemVO) {
  const ok = await showConfirm(`确认删除「${item.productName}」？`, '删除商品')
  if (!ok) return
  try {
    await cartStore.removeItem(ensureStringId(item.id))
    showToast('删除成功', 'success')
  } catch (e) {
    console.error('删除购物车项失败', e)
    showToast('删除失败', 'error')
  }
}

/** 批量删除（管理模式） */
async function handleBatchDelete() {
  if (cartStore.selectedCount === 0) {
    showToast('请先选择商品', 'none')
    return
  }
  const ok = await showConfirm(`确认删除选中的 ${cartStore.selectedCount} 件商品？`, '批量删除')
  if (!ok) return
  try {
    const selectedIds = cartStore.selectedItems.map(i => ensureStringId(i.id))
    // 逐项删除（后端无批量删除接口，复用 removeItem）
    await Promise.all(selectedIds.map(id => cartStore.removeItem(id)))
    showToast('删除成功', 'success')
    isManageMode.value = false
  } catch (e) {
    console.error('批量删除失败', e)
    showToast('删除失败', 'error')
    await loadCartList()
  }
}

/** 去结算 */
function handleCheckout() {
  if (cartStore.selectedCount === 0) {
    showToast('请先选择商品', 'none')
    return
  }
  const cartIds = cartStore.selectedItems.map(i => ensureStringId(i.id))
  // 跳转结算页，传 cartIds（逗号分隔字符串，避免 URL 过长）
  navigate.to('pages-order/pages/checkout/checkout', {
    cartIds: cartIds.join(',')
  })
}

/** 页面显示（tabBar 切换、返回此页均触发） */
onShow(() => {
  // 鉴权：未登录跳转登录页
  if (!requireAuthAsync()) return
  loadCartList()
})

/** 下拉刷新 */
onPullDownRefresh(() => {
  loadCartList().finally(() => {
    uni.stopPullDownRefresh()
  })
})

/** 挂载时读取系统信息 */
onMounted(() => {
  loadSystemInfo()
})
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 自定义导航栏 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background-color: #ffffff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .nav-content {
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 32rpx;

    .nav-title {
      font-size: 34rpx;
      font-weight: 600;
      color: #303133;
    }

    .nav-action {
      font-size: 28rpx;
      color: #FF4D4F;
      padding: 8rpx 16rpx;
    }
  }
}

.nav-placeholder {
  width: 100%;
}

/* 主体内容 */
.content {
  flex: 1;
  padding-bottom: 120rpx;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80rpx;
}

/* 购物车列表 */
.cart-list {
  padding: 16rpx 16rpx 0;
}

/* 购物车项 */
.cart-item {
  display: flex;
  align-items: center;
  padding: 24rpx 24rpx;
  background-color: #ffffff;
  border-radius: 12rpx;
  margin-bottom: 16rpx;

  .check-col {
    width: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .image-col {
    flex-shrink: 0;
    margin-right: 20rpx;
  }

  .info-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8rpx;
    min-width: 0;

    .product-name {
      font-size: 28rpx;
      color: #303133;
      line-height: 1.4;
      font-weight: 500;
    }

    .sku-spec {
      font-size: 24rpx;
      color: #909399;
      line-height: 1.3;
      background-color: #f5f5f5;
      padding: 4rpx 12rpx;
      border-radius: 4rpx;
      align-self: flex-start;
    }

    .price-row {
      display: flex;
      align-items: center;
      margin-top: 8rpx;

      .price-symbol {
        font-size: 24rpx;
        color: #FF4D4F;
        font-weight: bold;
      }

      .price-value {
        font-size: 32rpx;
        color: #FF4D4F;
        font-weight: bold;
        margin-right: auto;
      }

      .number-box-wrap {
        margin-left: auto;
      }
    }
  }
}

/* 底部结算栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 100rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.06);

  .bottom-left {
    display: flex;
    align-items: center;
    gap: 12rpx;
    flex-shrink: 0;

    .all-label {
      font-size: 28rpx;
      color: #303133;
    }
  }

  .bottom-center {
    flex: 1;
    display: flex;
    align-items: baseline;
    justify-content: flex-end;
    margin-right: 24rpx;

    .total-label {
      font-size: 26rpx;
      color: #303133;
    }

    .total-symbol {
      font-size: 26rpx;
      color: #FF4D4F;
      font-weight: bold;
    }

    .total-amount {
      font-size: 36rpx;
      color: #FF4D4F;
      font-weight: bold;
    }
  }

  .bottom-right {
    flex-shrink: 0;
  }
}

/* 单行省略 */
.ellipsis-1 {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 两行省略 */
.ellipsis-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
