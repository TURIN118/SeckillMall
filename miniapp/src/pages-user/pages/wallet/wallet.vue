<!--
  我的钱包页（对齐 spec.md 3.10 钱包余额展示 / tasks.md T5.5）
  - 调用 walletApi.getWallet() 拉取余额
  - 调用 walletApi.getWalletTransactions() 拉取交易记录，触底加载
  - 充值入口：showToast 提示"充值功能待扩展"
-->
<template>
  <view class="wallet-page">
    <!-- 钱包余额卡片 -->
    <view class="balance-card">
      <view class="balance-row">
        <text class="balance-label">可用余额（元）</text>
        <view class="recharge-btn" @tap="onRecharge">
          <u-icon name="rmb" size="28" color="#ff4d4f" />
          <text class="recharge-text">充值</text>
        </view>
      </view>
      <view class="balance-value-row">
        <text class="balance-symbol">¥</text>
        <text class="balance-value">{{ formatBalance(wallet?.balance) }}</text>
      </view>
      <view class="balance-meta">
        <view class="meta-item">
          <text class="meta-label">冻结金额</text>
          <text class="meta-value">¥{{ formatBalance(wallet?.frozenBalance) }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">累计充值</text>
          <text class="meta-value">¥{{ formatBalance(wallet?.totalRecharge) }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">累计消费</text>
          <text class="meta-value">¥{{ formatBalance(wallet?.totalConsume) }}</text>
        </view>
      </view>
    </view>

    <!-- 交易记录 -->
    <view class="transactions-section">
      <view class="section-title">
        <text class="title-text">交易记录</text>
        <text class="title-count">共 {{ pagination.total }} 条</text>
      </view>

      <view v-if="transactions.length > 0" class="transaction-list">
        <view
          v-for="item in transactions"
          :key="item.id"
          class="transaction-item"
        >
          <view class="tx-left">
            <view
              class="tx-icon"
              :class="txIconClass(item)"
            >
              <u-icon
                :name="txIconName(item)"
                size="32"
                :color="txIconColor(item)"
              />
            </view>
            <view class="tx-meta">
              <text class="tx-title">{{ txTitle(item) }}</text>
              <text class="tx-time">{{ formatDate(item.createdAt) }}</text>
            </view>
          </view>
          <view class="tx-right">
            <text
              class="tx-amount"
              :class="txAmountClass(item)"
            >{{ txAmountText(item) }}</text>
            <text class="tx-status">{{ txStatusText(item) }}</text>
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
        v-else-if="!txLoading"
        mode="order"
        text="暂无交易记录"
        marginTop="80"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import * as walletApi from '@/api/wallet'
import { requireAuthAsync } from '@/utils/authGuard'
import { showToast, showLoading, hideLoading } from '@/utils/toast'
import type { WalletVO } from '@/types'

/** 交易记录项（后端返回 any，这里定义本地类型） */
interface TransactionVO {
  id: string
  type: number          // 1充值 2消费 3退款 4提现
  amount: number
  balanceAfter: number
  description: string
  status: number        // 0处理中 1成功 2失败
  createdAt: string
}

const wallet = ref<WalletVO | null>(null)
const transactions = ref<TransactionVO[]>([])
const walletLoading = ref<boolean>(false)
const txLoading = ref<boolean>(false)

/** 分页参数 */
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
  hasMore: true
})

/** 加载更多状态 */
const loadMoreStatus = computed<'loadmore' | 'loading' | 'nomore'>(() => {
  if (txLoading.value) return 'loading'
  if (!pagination.hasMore) return 'nomore'
  return 'loadmore'
})

const loadMoreText = {
  loadmore: '上拉加载更多',
  loading: '加载中...',
  nomore: '没有更多了'
}

onLoad(() => {
  if (!requireAuthAsync()) return
  fetchWallet()
  fetchTransactions()
})

/** 拉取钱包余额 */
async function fetchWallet() {
  walletLoading.value = true
  try {
    const res = await walletApi.getWallet()
    wallet.value = res
  } catch (e) {
    console.error('拉取钱包余额失败', e)
    showToast('余额加载失败', 'error')
  } finally {
    walletLoading.value = false
  }
}

/** 拉取交易记录 */
async function fetchTransactions() {
  if (txLoading.value) return
  txLoading.value = true
  try {
    const res = await walletApi.getWalletTransactions({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    // 兼容 PageResult 与裸数组
    const list = (res?.list || res || []) as TransactionVO[]
    if (pagination.page === 1) {
      transactions.value = list
    } else {
      transactions.value = [...transactions.value, ...list]
    }
    pagination.total = res?.total || list.length
    pagination.hasMore = res?.hasMore !== undefined
      ? !!res.hasMore
      : list.length >= pagination.pageSize
  } catch (e) {
    console.error('拉取交易记录失败', e)
    showToast('交易记录加载失败', 'error')
  } finally {
    txLoading.value = false
  }
}

/** 触底加载 */
onReachBottom(() => {
  if (!pagination.hasMore || txLoading.value) return
  pagination.page++
  fetchTransactions()
})

/** 下拉刷新 */
onPullDownRefresh(() => {
  pagination.page = 1
  pagination.hasMore = true
  Promise.all([fetchWallet(), fetchTransactions()]).finally(() => {
    uni.stopPullDownRefresh()
  })
})

/** 充值入口 */
function onRecharge() {
  // 充值功能作为后续扩展项
  showToast('充值功能待扩展', 'none')
}

/** 格式化余额 */
function formatBalance(value: number | undefined | null): string {
  if (value === undefined || value === null || typeof value !== 'number' || isNaN(value)) {
    return '0.00'
  }
  return value.toFixed(2)
}

/** 格式化日期 */
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  // 取 YYYY-MM-DD HH:mm
  return dateStr.slice(0, 16).replace('T', ' ')
}

/** 交易图标名称 */
function txIconName(item: TransactionVO): string {
  if (item.type === 1) return 'rmb-circle'
  if (item.type === 2) return 'shopping-cart'
  if (item.type === 3) return 'redo'
  if (item.type === 4) return 'rmb-circle-fill'
  return 'info-circle'
}

/** 交易图标颜色 */
function txIconColor(item: TransactionVO): string {
  if (item.type === 1) return '#67c23a'   // 充值绿
  if (item.type === 2) return '#ff4d4f'   // 消费红
  if (item.type === 3) return '#409eff'   // 退款蓝
  if (item.type === 4) return '#e6a23c'   // 提现橙
  return '#909399'
}

/** 交易图标 class */
function txIconClass(item: TransactionVO): string {
  return `tx-type-${item.type}`
}

/** 交易标题 */
function txTitle(item: TransactionVO): string {
  if (item.description) return item.description
  if (item.type === 1) return '账户充值'
  if (item.type === 2) return '订单消费'
  if (item.type === 3) return '订单退款'
  if (item.type === 4) return '账户提现'
  return '交易记录'
}

/** 金额文本（带正负号） */
function txAmountText(item: TransactionVO): string {
  const abs = Math.abs(item.amount || 0).toFixed(2)
  // 充值/退款为正，消费/提现为负
  if (item.type === 1 || item.type === 3) {
    return `+¥${abs}`
  }
  return `-¥${abs}`
}

/** 金额 class */
function txAmountClass(item: TransactionVO): string {
  if (item.type === 1 || item.type === 3) return 'positive'
  return 'negative'
}

/** 状态文本 */
function txStatusText(item: TransactionVO): string {
  if (item.status === 0) return '处理中'
  if (item.status === 1) return '成功'
  return '失败'
}
</script>

<style lang="scss" scoped>
.wallet-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}

/* 余额卡片 */
.balance-card {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%);
  padding: 40rpx 32rpx 32rpx;
  color: #ffffff;

  .balance-row {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .balance-label {
      font-size: 26rpx;
      color: rgba(255, 255, 255, 0.9);
    }

    .recharge-btn {
      display: flex;
      align-items: center;
      gap: 4rpx;
      padding: 8rpx 24rpx;
      background-color: rgba(255, 255, 255, 0.95);
      border-radius: 32rpx;

      .recharge-text {
        font-size: 26rpx;
        color: #ff4d4f;
        font-weight: bold;
      }
    }
  }

  .balance-value-row {
    display: flex;
    align-items: baseline;
    margin-top: 16rpx;

    .balance-symbol {
      font-size: 36rpx;
      color: #ffffff;
      font-weight: bold;
    }

    .balance-value {
      font-size: 72rpx;
      color: #ffffff;
      font-weight: bold;
      margin-left: 4rpx;
    }
  }

  .balance-meta {
    display: flex;
    justify-content: space-between;
    margin-top: 32rpx;
    padding-top: 24rpx;
    border-top: 1rpx solid rgba(255, 255, 255, 0.2);

    .meta-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8rpx;

      .meta-label {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.85);
      }

      .meta-value {
        font-size: 28rpx;
        color: #ffffff;
        font-weight: bold;
      }
    }
  }
}

/* 交易记录区 */
.transactions-section {
  margin-top: 24rpx;
  background-color: #ffffff;
  padding: 24rpx 32rpx;

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 16rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .title-text {
      font-size: 32rpx;
      font-weight: bold;
      color: #303133;
    }

    .title-count {
      font-size: 24rpx;
      color: #909399;
    }
  }
}

.transaction-list {
  margin-top: 16rpx;
}

.transaction-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  .tx-left {
    display: flex;
    align-items: center;
    gap: 20rpx;

    .tx-icon {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f5f5f5;
    }

    .tx-meta {
      display: flex;
      flex-direction: column;
      gap: 6rpx;

      .tx-title {
        font-size: 28rpx;
        color: #303133;
        font-weight: 500;
      }

      .tx-time {
        font-size: 24rpx;
        color: #909399;
      }
    }
  }

  .tx-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 6rpx;

    .tx-amount {
      font-size: 30rpx;
      font-weight: bold;

      &.positive {
        color: #67c23a;
      }

      &.negative {
        color: #ff4d4f;
      }
    }

    .tx-status {
      font-size: 22rpx;
      color: #909399;
    }
  }
}
</style>