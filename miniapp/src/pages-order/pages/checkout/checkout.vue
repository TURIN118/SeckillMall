<!--
  结算页 Checkout（对齐 spec.md 3.6 / tasks.md T3.5）
  功能点：
    - 收货地址选择（u-cell 列表 + 跳转地址管理选择）
    - 支付方式选择（u-radio-group：在线支付/钱包支付/货到付款）
    - 订单备注（u-input textarea）
    - 商品清单（u-cell 列表）
    - 提交订单（底部固定提交栏）
  入参：
    - cartIds（逗号分隔字符串，从购物车结算）
    - 或 productId + skuId? + quantity（立即购买）
  鉴权：是（requiresAuth）
-->
<template>
  <view class="checkout-page">
    <!-- 收货地址 -->
    <view class="section address-section">
      <view
        v-if="selectedAddress"
        class="address-card"
        @click="goSelectAddress"
      >
        <view class="address-info">
          <view class="receiver-row">
            <text class="receiver">{{ selectedAddress.receiver }}</text>
            <text class="phone">{{ formatPhone(selectedAddress.phone) }}</text>
            <u-tag
              v-if="selectedAddress.isDefault"
              text="默认"
              type="error"
              size="mini"
              plain
            />
          </view>
          <text class="address-text ellipsis-2">
            {{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detail }}
          </text>
        </view>
        <u-icon name="arrow-right" size="40rpx" color="#909399" />
      </view>
      <view
        v-else
        class="address-empty"
        @click="goSelectAddress"
      >
        <u-icon name="map" size="40rpx" color="#FF4D4F" />
        <text class="empty-text">请选择收货地址</text>
        <u-icon name="arrow-right" size="40rpx" color="#909399" />
      </view>
    </view>

    <!-- 商品清单 -->
    <view class="section goods-section">
      <view class="section-title">
        <text>商品清单</text>
      </view>
      <view v-if="goodsList.length > 0" class="goods-list">
        <view
          v-for="item in goodsList"
          :key="item.id || item.productId"
          class="goods-item"
        >
          <u-image
            :src="item.productImage || ''"
            mode="aspectFill"
            width="140rpx"
            height="140rpx"
            radius="8rpx"
            :lazy-load="true"
          />
          <view class="goods-info">
            <text class="goods-name ellipsis-2">{{ item.productName }}</text>
            <text v-if="item.skuSpec" class="goods-sku ellipsis-1">{{ item.skuSpec }}</text>
            <view class="goods-price-row">
              <text class="price-symbol">¥</text>
              <text class="price-value">{{ formatPrice(item.price) }}</text>
              <text class="goods-qty">×{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="goods-empty">
        <text>暂无商品信息</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="section pay-section">
      <view class="section-title">
        <text>支付方式</text>
      </view>
      <u-radio-group v-model="payMethod" placement="column" @change="onPayMethodChange">
        <view
          v-for="opt in payMethodOptions"
          :key="opt.value"
          class="pay-option"
        >
          <u-icon :name="opt.icon" size="40rpx" :color="opt.color" />
          <text class="pay-label">{{ opt.label }}</text>
          <u-radio
            :name="opt.value"
            shape="circle"
            active-color="#FF4D4F"
          />
        </view>
      </u-radio-group>
    </view>

    <!-- 订单备注 -->
    <view class="section remark-section">
      <view class="section-title">
        <text>订单备注</text>
      </view>
      <u-input
        v-model="remark"
        type="textarea"
        placeholder="选填：给商家留言（最多 200 字）"
        :maxlength="200"
        :auto-height="true"
        border="none"
        custom-style="padding: 16rpx 0;"
      />
    </view>

    <!-- 金额合计 -->
    <view class="section amount-section">
      <view class="amount-row">
        <text class="amount-label">商品金额</text>
        <text class="amount-value">¥{{ formatPrice(goodsAmount) }}</text>
      </view>
      <view class="amount-row">
        <text class="amount-label">运费</text>
        <text class="amount-value">¥{{ formatPrice(freightAmount) }}</text>
      </view>
      <view v-if="couponAmount > 0" class="amount-row">
        <text class="amount-label">优惠</text>
        <text class="amount-value discount">-¥{{ formatPrice(couponAmount) }}</text>
      </view>
      <view class="amount-row total-row">
        <text class="amount-label">实付</text>
        <view class="total-amount-wrap">
          <text class="total-symbol">¥</text>
          <text class="total-amount">{{ formatPrice(payAmount) }}</text>
        </view>
      </view>
    </view>

    <!-- 底部提交栏 -->
    <view class="bottom-bar" :style="{ paddingBottom: safeBottom + 'px' }">
      <view class="bottom-left">
        <text class="total-label">合计：</text>
        <text class="total-symbol">¥</text>
        <text class="total-amount">{{ formatPrice(payAmount) }}</text>
      </view>
      <view class="bottom-right">
        <u-button
          type="error"
          shape="circle"
          :loading="submitting"
          :disabled="!canSubmit"
          :custom-style="`width: 240rpx; ${!canSubmit ? 'opacity: 0.5;' : ''}`"
          text="提交订单"
          @click="handleSubmit"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showLoading, hideLoading } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import * as cartApi from '@/api/cart'
import * as orderApi from '@/api/order'
import * as addressApi from '@/api/address'
import type { CartItemVO, AddressVO, OrderVO, OrderItemVO } from '@/types'

/** 支付方式选项 */
interface PayMethodOption {
  label: string
  value: string
  icon: string
  color: string
}

/** 来源类型：cart（购物车结算）/ buyNow（立即购买） */
type SourceType = 'cart' | 'buyNow'

/** 入参解析结果 */
const sourceType = ref<SourceType>('cart')
const cartIds = ref<string[]>([])
const buyNowProductId = ref<string>('')
const buyNowSkuId = ref<string>('')
const buyNowQuantity = ref<number>(1)

/** 商品清单（购物车项或立即购买项） */
const goodsList = ref<Array<CartItemVO | OrderItemVO>>([])
/** 选中的收货地址 */
const selectedAddress = ref<AddressVO | null>(null)
/** 地址列表（用于默认选择） */
const addressList = ref<AddressVO[]>([])
/** 支付方式 */
const payMethod = ref<string>('online')
/** 订单备注 */
const remark = ref<string>('')
/** 提交中 */
const submitting = ref<boolean>(false)
/** 安全区域底部高度 */
const safeBottom = ref<number>(0)
/** 是否已鉴权通过 */
const authPassed = ref<boolean>(false)

/** 支付方式选项列表 */
const payMethodOptions: PayMethodOption[] = [
  { label: '在线支付', value: 'online', icon: 'rmb-circle', color: '#FF4D4F' },
  { label: '钱包支付', value: 'wallet', icon: 'integral', color: '#FF9900' },
  { label: '货到付款', value: 'cod', icon: 'car', color: '#909399' }
]

/** 商品金额 */
const goodsAmount = computed<number>(() =>
  goodsList.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)

/** 运费（模拟：满 99 包邮） */
const freightAmount = computed<number>(() => {
  if (goodsAmount.value === 0) return 0
  return goodsAmount.value >= 99 ? 0 : 10
})

/** 优惠金额（本期不接入优惠券，预留 0） */
const couponAmount = computed<number>(() => 0)

/** 实付金额 */
const payAmount = computed<number>(() =>
  Math.max(0, goodsAmount.value + freightAmount.value - couponAmount.value)
)

/** 是否可提交（有地址 + 有商品 + 未在提交中） */
const canSubmit = computed<boolean>(() =>
  !!selectedAddress.value && goodsList.value.length > 0 && !submitting.value
)

/** 格式化价格 */
function formatPrice(price: number): string {
  if (typeof price !== 'number' || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

/** 格式化手机号（脱敏中间 4 位） */
function formatPhone(phone: string): string {
  if (!phone || phone.length < 11) return phone || ''
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

/** 读取系统信息 */
function loadSystemInfo() {
  try {
    const sysInfo = uni.getSystemInfoSync()
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

/** 加载收货地址列表，自动选中默认地址 */
async function loadAddressList() {
  try {
    const list = await addressApi.getAddressList()
    addressList.value = list || []
    // 优先选默认地址，否则选第一个
    if (addressList.value.length > 0) {
      const def = addressList.value.find(a => a.isDefault) || addressList.value[0]
      selectedAddress.value = def
    }
  } catch (e) {
    console.error('加载地址列表失败', e)
  }
}

/** 加载购物车选中商品（按 cartIds 过滤） */
async function loadCartItems() {
  if (cartIds.value.length === 0) {
    showToast('未选择结算商品', 'none')
    return
  }
  try {
    const all = await cartApi.getCartList()
    const idSet = new Set(cartIds.value.map(ensureStringId))
    goodsList.value = all.filter(item => idSet.has(ensureStringId(item.id)))
    if (goodsList.value.length === 0) {
      showToast('结算商品已失效，请重新选择', 'none')
    }
  } catch (e) {
    console.error('加载购物车商品失败', e)
    showToast('加载商品失败', 'error')
  }
}

/** 构造立即购买的商品清单（无接口拉取，使用入参构造展示项） */
function buildBuyNowItems() {
  // 立即购买场景：商品详情页传 productId + skuId + quantity + 名称/图片/价格
  // 由于入参有限，这里构造一个最小展示项；实际商品信息由商品详情页通过事件总线或全局 store 传递
  // 此处使用 onLoad 解析的额外参数（productName/productImage/price）构造
  const item: any = {
    id: '',
    productId: buyNowProductId.value,
    productName: buyNowProductName.value || '商品信息',
    productImage: buyNowProductImage.value,
    price: buyNowProductPrice.value,
    quantity: buyNowQuantity.value,
    skuId: buyNowSkuId.value || undefined,
    skuSpec: buyNowSkuSpec.value || undefined
  }
  goodsList.value = [item]
}

/** 立即购买补充参数（由商品详情页透传） */
const buyNowProductName = ref<string>('')
const buyNowProductImage = ref<string>('')
const buyNowProductPrice = ref<number>(0)
const buyNowSkuSpec = ref<string>('')

/** 跳转地址选择 */
function goSelectAddress() {
  navigate.to('pages-user/pages/address-list/address-list', {
    from: 'checkout',
    mode: 'select'
  })
}

/** 支付方式切换 */
function onPayMethodChange(name: string) {
  payMethod.value = name
}

/** 提交订单 */
async function handleSubmit() {
  if (!canSubmit.value) return
  if (!selectedAddress.value) {
    showToast('请选择收货地址', 'none')
    return
  }
  if (goodsList.value.length === 0) {
    showToast('暂无结算商品', 'none')
    return
  }

  submitting.value = true
  showLoading('提交订单中...')
  try {
    const addressId = ensureStringId(selectedAddress.value.id)
    let order: OrderVO

    if (sourceType.value === 'cart') {
      // 从购物车创建订单
      order = await orderApi.createOrderFromCart({
        addressId,
        cartIds: cartIds.value.map(ensureStringId),
        remark: remark.value || undefined
      })
    } else {
      // 立即购买：创建订单
      order = await orderApi.createOrder({
        addressId,
        remark: remark.value || undefined,
        items: [{
          productId: ensureStringId(buyNowProductId.value),
          skuId: buyNowSkuId.value || undefined,
          quantity: buyNowQuantity.value
        }]
      })
    }

    hideLoading()
    showToast('订单创建成功', 'success')

    // 跳转订单详情（雪花 ID 用 string 传递）
    const orderId = ensureStringId(order.id)
    // 使用 redirectTo 替换当前页，避免返回时回到结算页
    setTimeout(() => {
      navigate.redirect('pages-order/pages/order-detail/order-detail', { id: orderId })
    }, 800)
  } catch (e) {
    hideLoading()
    console.error('提交订单失败', e)
    showToast('提交订单失败', 'error')
  } finally {
    submitting.value = false
  }
}

/** 处理页面入参 */
function parseOptions(options: Record<string, any>) {
  // 购物车结算：cartIds=1,2,3
  if (options.cartIds) {
    sourceType.value = 'cart'
    cartIds.value = String(options.cartIds)
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
      .map(ensureStringId)
    return
  }
  // 立即购买：productId + skuId? + quantity
  if (options.productId) {
    sourceType.value = 'buyNow'
    buyNowProductId.value = ensureStringId(options.productId)
    buyNowSkuId.value = options.skuId ? ensureStringId(options.skuId) : ''
    buyNowQuantity.value = Number(options.quantity) || 1
    // 透传商品展示信息（由商品详情页传入）
    buyNowProductName.value = options.productName ? decodeURIComponent(String(options.productName)) : ''
    buyNowProductImage.value = options.productImage ? decodeURIComponent(String(options.productImage)) : ''
    buyNowProductPrice.value = Number(options.price) || 0
    buyNowSkuSpec.value = options.skuSpec ? decodeURIComponent(String(options.skuSpec)) : ''
    return
  }
  // 无入参，默认空购物车结算
  sourceType.value = 'cart'
  cartIds.value = []
}

/** 页面加载 */
onLoad((options: Record<string, any>) => {
  parseOptions(options || {})
})

/** 页面显示 */
onShow(() => {
  // 鉴权
  if (!requireAuthAsync()) return
  authPassed.value = true

  // 加载地址列表
  loadAddressList()

  // 加载商品清单
  if (sourceType.value === 'cart') {
    loadCartItems()
  } else {
    buildBuyNowItems()
  }
})

/** 挂载 */
onMounted(() => {
  loadSystemInfo()
})
</script>

<style lang="scss" scoped>
.checkout-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 140rpx;
}

/* 通用 section */
.section {
  background-color: #ffffff;
  margin: 16rpx 0;
  padding: 24rpx 32rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16rpx;
    display: flex;
    align-items: center;
  }
}

/* 地址 section */
.address-section {
  margin-top: 16rpx;

  .address-card {
    display: flex;
    align-items: center;
    gap: 16rpx;

    .address-info {
      flex: 1;
      min-width: 0;

      .receiver-row {
        display: flex;
        align-items: center;
        gap: 16rpx;
        margin-bottom: 8rpx;

        .receiver {
          font-size: 30rpx;
          font-weight: 600;
          color: #303133;
        }

        .phone {
          font-size: 28rpx;
          color: #606266;
        }
      }

      .address-text {
        font-size: 26rpx;
        color: #606266;
        line-height: 1.4;
      }
    }
  }

  .address-empty {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 16rpx 0;

    .empty-text {
      flex: 1;
      font-size: 30rpx;
      color: #909399;
    }
  }
}

/* 商品清单 */
.goods-section {
  .goods-list {
    display: flex;
    flex-direction: column;
    gap: 24rpx;
  }

  .goods-item {
    display: flex;
    gap: 20rpx;

    .goods-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8rpx;
      min-width: 0;

      .goods-name {
        font-size: 28rpx;
        color: #303133;
        line-height: 1.4;
      }

      .goods-sku {
        font-size: 24rpx;
        color: #909399;
        background-color: #f5f5f5;
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
        align-self: flex-start;
      }

      .goods-price-row {
        display: flex;
        align-items: baseline;
        gap: 4rpx;
        margin-top: auto;

        .price-symbol {
          font-size: 24rpx;
          color: #FF4D4F;
          font-weight: bold;
        }

        .price-value {
          font-size: 30rpx;
          color: #FF4D4F;
          font-weight: bold;
        }

        .goods-qty {
          font-size: 26rpx;
          color: #909399;
          margin-left: auto;
        }
      }
    }
  }

  .goods-empty {
    text-align: center;
    padding: 40rpx 0;
    color: #909399;
    font-size: 26rpx;
  }
}

/* 支付方式 */
.pay-section {
  .pay-option {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 16rpx 0;

    .pay-label {
      flex: 1;
      font-size: 28rpx;
      color: #303133;
    }
  }
}

/* 备注 */
.remark-section {
  u-input {
    width: 100%;
  }
}

/* 金额合计 */
.amount-section {
  .amount-row {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding: 8rpx 0;

    .amount-label {
      font-size: 28rpx;
      color: #606266;
    }

    .amount-value {
      font-size: 28rpx;
      color: #303133;

      &.discount {
        color: #FF4D4F;
      }
    }

    &.total-row {
      padding-top: 16rpx;
      border-top: 2rpx solid #f0f0f0;
      margin-top: 8rpx;

      .amount-label {
        font-size: 30rpx;
        font-weight: 600;
        color: #303133;
      }

      .total-amount-wrap {
        display: flex;
        align-items: baseline;

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
    }
  }
}

/* 底部提交栏 */
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
  justify-content: space-between;
  padding: 0 32rpx;
  box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.06);

  .bottom-left {
    display: flex;
    align-items: baseline;

    .total-label {
      font-size: 28rpx;
      color: #303133;
    }

    .total-symbol {
      font-size: 26rpx;
      color: #FF4D4F;
      font-weight: bold;
    }

    .total-amount {
      font-size: 38rpx;
      color: #FF4D4F;
      font-weight: bold;
    }
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