<!--
  收货地址列表页（对齐 spec.md 3.10 跳转地址管理 / tasks.md T3.2 + 阶段5复用）
  - 调用 addressApi.getAddressList()
  - u-swipe-action 左滑删除
  - 设默认地址 addressApi.setDefaultAddress()
  - 编辑跳转 address-edit（携带 id），新增跳转 address-edit（无 id）
  - 选择模式：从结算页跳转携带 selectMode=1，点击地址回传并返回
-->
<template>
  <view class="address-list-page">
    <!-- 加载中骨架 -->
    <view v-if="loading && addressList.length === 0" class="loading-wrap">
      <u-skeleton rows="4" :loading="true" />
    </view>

    <!-- 空状态 -->
    <u-empty
      v-else-if="addressList.length === 0"
      mode="address"
      text="暂无收货地址"
      marginTop="120"
    >
      <u-button
        slot="bottom"
        type="error"
        shape="circle"
        @click="goAdd"
      >新增地址</u-button>
    </u-empty>

    <!-- 地址列表 -->
    <view v-else class="address-list">
      <u-swipe-action
        v-for="item in addressList"
        :key="item.id"
        :options="swipeOptions"
        @click="(index) => onSwipeAction(index, item)"
      >
        <view class="address-item" @tap="onItemTap(item)">
          <view class="item-header">
            <text class="receiver">{{ item.receiver }}</text>
            <text class="phone">{{ formatPhone(item.phone) }}</text>
            <u-tag
              v-if="item.isDefault"
              text="默认"
              type="error"
              size="mini"
              plain
            />
            <u-tag
              v-if="item.tag"
              :text="item.tag"
              type="info"
              size="mini"
              plain
            />
          </view>
          <view class="item-detail">
            <text class="detail-text">
              {{ item.province }}{{ item.city }}{{ item.district }}{{ item.detail }}
            </text>
          </view>
          <view class="item-footer">
            <view
              class="default-btn"
              @tap.stop="onSetDefault(item)"
            >
              <u-icon
                :name="item.isDefault ? 'checkmark-circle-fill' : 'radio'"
                :color="item.isDefault ? '#ff4d4f' : '#c0c4cc'"
                size="36"
              />
              <text
                class="default-text"
                :class="{ active: item.isDefault }"
              >设为默认</text>
            </view>
            <view class="op-btns">
              <view class="op-btn" @tap.stop="goEdit(item)">
                <u-icon name="edit-pen" size="32" />
                <text class="op-text">编辑</text>
              </view>
              <view class="op-btn danger" @tap.stop="onDelete(item)">
                <u-icon name="trash" size="32" />
                <text class="op-text">删除</text>
              </view>
            </view>
          </view>
        </view>
      </u-swipe-action>
    </view>

    <!-- 底部新增按钮 -->
    <view v-if="addressList.length > 0" class="footer-bar">
      <u-button
        type="error"
        shape="circle"
        @click="goAdd"
      >+ 新增收货地址</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import * as addressApi from '@/api/address'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showConfirm, showToast, showLoading, hideLoading } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import type { AddressVO } from '@/types'

const addressList = ref<AddressVO[]>([])
const loading = ref<boolean>(false)
const selectMode = ref<boolean>(false)

/** u-swipe-action 操作按钮 */
const swipeOptions = [
  { text: '删除', style: { backgroundColor: '#ff4d4f' } }
]

onLoad((options) => {
  if (!requireAuthAsync()) return
  // 从结算页跳转携带 selectMode=1，点击地址回传
  if (options && options.selectMode === '1') {
    selectMode.value = true
    uni.setNavigationBarTitle({ title: '选择收货地址' })
  }
})

onShow(() => {
  // 每次显示刷新列表（编辑/新增后返回能及时更新）
  fetchList()
})

/** 拉取地址列表 */
async function fetchList() {
  loading.value = true
  try {
    const list = await addressApi.getAddressList()
    addressList.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.error('拉取地址列表失败', e)
    showToast('地址列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

/** 手机号脱敏（保留前 3 后 4） */
function formatPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone || ''
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

/** 点击地址项：选择模式下回传并返回 */
function onItemTap(item: AddressVO) {
  if (selectMode.value) {
    // 通过 eventChannel 回传给上一页
    const pages = getCurrentPages()
    const current = pages[pages.length - 1]
    const eventChannel = (current as any).getOpenerEventChannel?.()
    if (eventChannel && typeof eventChannel.emit === 'function') {
      eventChannel.emit('selectAddress', item)
    }
    uni.navigateBack()
  }
}

/** 设为默认地址 */
async function onSetDefault(item: AddressVO) {
  if (item.isDefault) {
    showToast('已是默认地址', 'none')
    return
  }
  try {
    showLoading('设置中...')
    await addressApi.setDefaultAddress(ensureStringId(item.id))
    showToast('已设为默认', 'success')
    await fetchList()
  } catch (e) {
    console.error('设默认地址失败', e)
    showToast('设置失败', 'error')
  } finally {
    hideLoading()
  }
}

/** 编辑地址 */
function goEdit(item: AddressVO) {
  navigate.to('pages-user/pages/address-edit/address-edit', {
    id: ensureStringId(item.id)
  })
}

/** 新增地址 */
function goAdd() {
  navigate.to('pages-user/pages/address-edit/address-edit')
}

/** 删除地址 */
async function onDelete(item: AddressVO) {
  const confirmed = await showConfirm(`确定删除该地址吗？`, '删除地址')
  if (!confirmed) return
  try {
    showLoading('删除中...')
    await addressApi.removeAddress(ensureStringId(item.id))
    showToast('删除成功', 'success')
    await fetchList()
  } catch (e) {
    console.error('删除地址失败', e)
    showToast('删除失败', 'error')
  } finally {
    hideLoading()
  }
}

/** u-swipe-action 滑动操作 */
async function onSwipeAction(index: number, item: AddressVO) {
  // index 0 = 删除
  if (index === 0) {
    await onDelete(item)
  }
}
</script>

<style lang="scss" scoped>
.address-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 24rpx 24rpx 160rpx;
}

.loading-wrap {
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 32rpx;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.address-item {
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 28rpx 32rpx;

  .item-header {
    display: flex;
    align-items: center;
    gap: 16rpx;

    .receiver {
      font-size: 32rpx;
      font-weight: bold;
      color: #303133;
    }

    .phone {
      font-size: 28rpx;
      color: #606266;
    }
  }

  .item-detail {
    margin-top: 16rpx;

    .detail-text {
      font-size: 26rpx;
      color: #606266;
      line-height: 1.5;
    }
  }

  .item-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 20rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #f0f0f0;

    .default-btn {
      display: flex;
      align-items: center;
      gap: 8rpx;

      .default-text {
        font-size: 26rpx;
        color: #909399;

        &.active {
          color: #ff4d4f;
        }
      }
    }

    .op-btns {
      display: flex;
      gap: 32rpx;

      .op-btn {
        display: flex;
        align-items: center;
        gap: 6rpx;

        .op-text {
          font-size: 26rpx;
          color: #606266;
        }

        &.danger .op-text {
          color: #ff4d4f;
        }
      }
    }
  }
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);
}
</style>