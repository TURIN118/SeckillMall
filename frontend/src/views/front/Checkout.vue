<template>
    <!-- 结算确认页：三栏卡片式 (收货地址 / 商品清单 / 支付方式) + 底部提交栏 -->
    <div class="checkout-page">
        <!-- 页头 -->
        <div class="checkout-header">
            <h2 class="checkout-title">确认订单</h2>
            <button class="btn-sm text" type="button" @click="router.push('/cart')">返回购物车</button>
        </div>

        <!-- 商品为空 (直接访问 /checkout 或 sessionStorage 已被清除) -->
        <div v-if="checkoutItems.length === 0" class="empty-state">
            <el-empty description="没有可结算的商品" :image-size="120" />
            <button class="btn-sm primary" type="button" @click="router.push('/cart')">返回购物车</button>
        </div>

        <template v-else>
            <!-- 1. 商品清单卡 -->
            <div class="section-card">
                <div class="section-title">商品清单</div>
                <div class="goods-table-head">
                    <div class="col-info">商品信息</div>
                    <div class="col-price">单价</div>
                    <div class="col-quantity">数量</div>
                    <div class="col-subtotal">小计</div>
                </div>
                <div v-for="item in checkoutItems" :key="item.cartId" class="goods-row">
                    <div class="col-info">
                        <div class="product-img">
                            <img v-if="item.mainImage" :src="formatImageUrl(item.mainImage)" :alt="item.productName"
                                class="product-img-tag" loading="lazy" />
                            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                                class="product-img-placeholder">
                                <rect x="3" y="3" width="18" height="18" rx="2" />
                                <circle cx="8.5" cy="8.5" r="1.5" />
                                <path d="m21 15-5-5L5 21" />
                            </svg>
                        </div>
                        <div class="product-name">{{ item.productName }}</div>
                    </div>
                    <div class="col-price">
                        <span class="price-text">¥{{ formatPrice(item.price) }}</span>
                    </div>
                    <div class="col-quantity">
                        <span class="qty-text">×{{ item.quantity }}</span>
                    </div>
                    <div class="col-subtotal">
                        <span class="subtotal-text">¥{{ formatPrice(item.subtotal) }}</span>
                    </div>
                </div>
                <div class="goods-total">
                    共 <span class="total-count">{{ totalCount }}</span> 件商品，
                    合计：<span class="total-amount">¥{{ formatPrice(totalAmount) }}</span>
                </div>
            </div>

            <!-- 2. [收货地址 | 支付方式] 两列排列 -->
            <div class="section-row">
                <!-- 收货地址卡 -->
                <div class="section-card">
                    <div class="section-title">收货地址</div>
                    <div v-if="addressLoading" class="loading-state">
                        <el-icon class="is-loading">
                            <Loading />
                        </el-icon>
                        <span class="loading-text">加载中...</span>
                    </div>
                    <template v-else>
                        <div v-if="addressList.length === 0" class="address-empty">
                            <span class="address-empty-text">请先添加收货地址</span>
                            <button class="btn-sm primary" type="button"
                                @click="router.push('/user/profile')">去添加</button>
                        </div>
                        <template v-else>
                            <!-- 默认只展示当前选中地址 + 更换地址按钮 -->
                            <div v-if="currentAddress" class="address-current">
                                <div class="addr-info">
                                    <div class="addr-name-row">
                                        <span class="addr-name">{{ currentAddress.receiverName }}</span>
                                        <span class="addr-phone">{{ currentAddress.receiverPhone }}</span>
                                        <el-tag v-if="currentAddress.isDefault === 1" type="danger" size="small"
                                            effect="dark" class="default-tag">默认</el-tag>
                                    </div>
                                    <div class="addr-detail">
                                        {{ currentAddress.province }}{{ currentAddress.city }}{{
                                            currentAddress.district }} {{ currentAddress.detailAddress }}
                                    </div>
                                </div>
                                <button class="btn-sm text" type="button"
                                    @click="showAddressPicker = true">更换地址</button>
                            </div>
                            <!-- 地址选择弹窗 -->
                            <el-dialog v-model="showAddressPicker" title="选择收货地址" width="560px" append-to-body>
                                <el-radio-group v-model="selectedAddressId" class="address-radio-group">
                                    <div v-for="addr in addressList" :key="addr.id" class="address-item"
                                        :class="{ 'address-default': addr.isDefault === 1 }">
                                        <el-radio :value="addr.id" class="address-radio">
                                            <div class="addr-info">
                                                <div class="addr-name-row">
                                                    <span class="addr-name">{{ addr.receiverName }}</span>
                                                    <span class="addr-phone">{{ addr.receiverPhone }}</span>
                                                    <el-tag v-if="addr.isDefault === 1" type="danger" size="small"
                                                        effect="dark" class="default-tag">默认</el-tag>
                                                </div>
                                                <div class="addr-detail">
                                                    {{ addr.province }}{{ addr.city }}{{ addr.district }}
                                                    {{ addr.detailAddress }}
                                                </div>
                                            </div>
                                        </el-radio>
                                    </div>
                                </el-radio-group>
                                <template #footer>
                                    <button class="btn-sm primary" type="button"
                                        @click="showAddressPicker = false">确定</button>
                                </template>
                            </el-dialog>
                        </template>
                    </template>
                </div>

                <!-- 支付方式卡 -->
                <div class="section-card">
                    <div class="section-title">支付方式</div>
                    <el-radio-group v-model="payMethod" class="pay-radio-group">
                        <el-radio value="ALIPAY" class="pay-radio">
                            <span class="pay-label">支付宝</span>
                            <span class="pay-tag">模拟支付</span>
                        </el-radio>
                        <el-radio value="WECHAT" class="pay-radio">
                            <span class="pay-label">微信支付</span>
                            <span class="pay-tag">模拟支付</span>
                        </el-radio>
                        <el-radio value="WALLET" class="pay-radio" :disabled="walletBalance === null">
                            <span class="pay-label">余额支付</span>
                            <span class="pay-tag" v-if="walletBalance !== null">
                                可用余额 ¥{{ formatPrice(walletBalance) }}
                            </span>
                            <span class="pay-tag" v-else>余额加载中</span>
                            <span v-if="walletBalance !== null && !walletEnough" class="pay-insufficient">（余额不足）</span>
                        </el-radio>
                    </el-radio-group>
                </div>
            </div>

            <!-- 备注 -->
            <div class="section-card">
                <div class="section-title">订单备注</div>
                <el-input v-model="remark" type="textarea" :rows="2" placeholder="选填，给商家留言（最多 200 字）" maxlength="200"
                    show-word-limit />
            </div>

            <!-- 底部提交栏 (固定在视口底部) -->
            <div class="checkout-footer">
                <div class="footer-left">
                    <span class="footer-tip">应付总额：</span>
                    <span class="footer-amount">¥{{ formatPrice(totalAmount) }}</span>
                </div>
                <div class="footer-right">
                    <button class="btn-submit" type="button" :disabled="!canSubmit || submitting" @click="handleSubmit">
                        {{ submitting ? '提交中...' : '提交订单' }}
                    </button>
                </div>
            </div>
        </template>
    </div>
</template>

<script setup lang="ts">
/**
 * 结算确认页 (前台)
 * 从 sessionStorage 读取购物车选中商品信息 (由 Cart.vue handleCheckout 写入),
 * 调用 createOrderFromCart 创建普通订单 → payNormalOrder 模拟支付 → 跳转订单详情.
 * 参照 UserAddress.vue / Cart.vue 卡片风格.
 */
defineOptions({ name: 'Checkout' })
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getAddressList } from '@/api/address'
import { createOrder, createOrderFromCart, payNormalOrder } from '@/api/order'
import { getWalletBalance } from '@/api/wallet'
import { formatImageUrl } from '@/utils/image'
import type { UserAddressVO } from '@/types'

const router = useRouter()
const route = useRoute()

/** 结算商品项 (从 sessionStorage 读取, 结构与 Cart.vue 写入一致) */
interface CheckoutItem {
    cartId: number | string
    productId: number | string
    productName: string
    mainImage: string
    price: number
    quantity: number
    subtotal: number
}

/** sessionStorage 中存储结算商品的 key */
const CHECKOUT_STORAGE_KEY = 'checkout_items'

/** 结算商品列表 */
const checkoutItems = ref<CheckoutItem[]>([])

/** 收货地址列表 */
const addressList = ref<UserAddressVO[]>([])

/** 地址加载中 */
const addressLoading = ref<boolean>(false)

/** 选中的收货地址 ID */
const selectedAddressId = ref<number | string>('')

/** 支付方式 (默认支付宝, 均为模拟支付) */
const payMethod = ref<string>('ALIPAY')

/** 订单备注 */
const remark = ref<string>('')

/** 提交中 */
const submitting = ref<boolean>(false)

/** 钱包余额 (null 表示未加载/加载失败) */
const walletBalance = ref<number | null>(null)

/** 钱包余额加载中 */
const walletLoading = ref<boolean>(false)

/** 地址选择弹窗显示 */
const showAddressPicker = ref<boolean>(false)

/** 结算模式: 'buynow' 立即购买 / 'cart' 购物车结算 (默认) */
const checkoutMode = ref<'buynow' | 'cart'>('cart')

/** 立即购买模式下的商品参数 (从 route.query 读取) */
const buyNowParams = ref<{
  productId: number | string
  skuId: number | string | null
  quantity: number
} | null>(null)

/* === 计算属性 === */

/** 总件数 */
const totalCount = computed<number>(() => {
    return checkoutItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

/** 总金额 */
const totalAmount = computed<number>(() => {
    return checkoutItems.value.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

/** 是否可提交 (有商品 + 已选地址 + 未在提交中) */
const canSubmit = computed<boolean>(() => {
    return checkoutItems.value.length > 0 && selectedAddressId.value !== '' && selectedAddressId.value !== null
})

/** 当前选中的收货地址 */
const currentAddress = computed<UserAddressVO | undefined>(() => {
    return addressList.value.find(a => a.id === selectedAddressId.value)
})

/** 钱包余额是否充足 */
const walletEnough = computed<boolean>(() => {
    return (walletBalance.value ?? 0) >= totalAmount.value
})

/* === 工具函数 === */

/** 格式化价格 (保留两位小数) */
function formatPrice(value: number): string {
    return (value || 0).toFixed(2)
}

/** 从 sessionStorage 读取结算商品 */
function loadCheckoutItems(): void {
    try {
        const raw = sessionStorage.getItem(CHECKOUT_STORAGE_KEY)
        if (!raw) {
            checkoutItems.value = []
            return
        }
        const parsed = JSON.parse(raw) as CheckoutItem[]
        checkoutItems.value = Array.isArray(parsed) ? parsed : []
    } catch {
        checkoutItems.value = []
    }
}

/** 加载收货地址列表, 默认选中 isDefault=1 的地址 */
async function loadAddressList(): Promise<void> {
    addressLoading.value = true
    try {
        const res = await getAddressList()
        addressList.value = res.data ?? []
        // 默认选中 isDefault=1 的地址; 没有默认则选第一个
        const defaultAddr = addressList.value.find(a => a.isDefault === 1)
        selectedAddressId.value = defaultAddr?.id ?? addressList.value[0]?.id ?? ''
    } catch {
        addressList.value = []
    } finally {
        addressLoading.value = false
    }
}

/** 加载钱包余额, 余额充足时默认选余额支付 */
async function loadWalletBalance(): Promise<void> {
    walletLoading.value = true
    try {
        const res = await getWalletBalance()
        walletBalance.value = res.data ?? 0
        // 余额充足时默认选余额支付
        if (walletBalance.value >= totalAmount.value) {
            payMethod.value = 'WALLET'
        }
    } catch {
        walletBalance.value = null
    } finally {
        walletLoading.value = false
    }
}

/** 提交订单: 创建订单 → 模拟支付 → 跳转订单详情 */
async function handleSubmit(): Promise<void> {
    if (!canSubmit.value) {
        if (checkoutItems.value.length === 0) {
            ElMessage.warning('没有可结算的商品')
        } else if (selectedAddressId.value === '' || selectedAddressId.value === null) {
            ElMessage.warning('请选择收货地址')
        }
        return
    }
    // 余额支付校验: 余额不足时提示去充值
    if (payMethod.value === 'WALLET' && walletBalance.value !== null && walletBalance.value < totalAmount.value) {
        ElMessageBox.confirm('余额不足，是否前往充值？', '提示', { type: 'warning' })
            .then(() => router.push('/user/wallet'))
            .catch(() => { })
        return
    }
    submitting.value = true
    try {
        // 1. 创建订单 (根据结算模式调用不同 API)
        let order: { id: number | string } | undefined
        if (checkoutMode.value === 'buynow' && buyNowParams.value) {
            // 立即购买模式: 使用 createOrder
            const createRes = await createOrder({
                productId: buyNowParams.value.productId,
                skuId: buyNowParams.value.skuId,
                quantity: buyNowParams.value.quantity,
                addressId: selectedAddressId.value,
                remark: remark.value.trim() || undefined
            })
            order = createRes.data
        } else {
            // 购物车结算模式: 使用 createOrderFromCart
            const cartIds = checkoutItems.value.map(item => item.cartId)
            const createRes = await createOrderFromCart({
                addressId: selectedAddressId.value,
                cartIds,
                remark: remark.value.trim() || undefined
            })
            order = createRes.data
        }
        if (!order || !order.id) {
            ElMessage.error('创建订单失败，请重试')
            submitting.value = false
            return
        }
        // 2. 模拟支付
        try {
            await payNormalOrder(order.id, payMethod.value)
        } catch (payErr) {
            // 支付失败: 订单已创建, 跳转到订单详情让用户重新支付
            ElMessage.warning('订单已创建，但支付失败，请到订单详情重新支付')
            sessionStorage.removeItem(CHECKOUT_STORAGE_KEY)
            router.push(`/user/orders/${order.id}?type=NORMAL`)
            return
        }
        // 3. 支付成功: 清除 sessionStorage, 跳转订单详情
        sessionStorage.removeItem(CHECKOUT_STORAGE_KEY)
        ElMessage.success('支付成功')
        router.push(`/user/orders/${order.id}?type=NORMAL`)
    } catch {
        // 错误已由请求拦截器统一提示
    } finally {
        submitting.value = false
    }
}

// 页面挂载时加载结算商品 + 地址列表 + 钱包余额
onMounted(() => {
    // 解析结算模式: route.query.mode === 'buynow' 为立即购买模式
    const mode = route.query.mode
    if (mode === 'buynow') {
        checkoutMode.value = 'buynow'
        const productId = route.query.productId
        const skuId = route.query.skuId
        const quantity = route.query.quantity
        if (productId && quantity) {
            buyNowParams.value = {
                productId: String(productId),
                skuId: skuId ? String(skuId) : null,
                quantity: Number(quantity) || 1
            }
        }
    }
    loadCheckoutItems()
    loadAddressList()
    loadWalletBalance()
})
</script>

<style scoped>
/* 参照 UserAddress.vue / Cart.vue 样式 */
.checkout-page {
    padding: 24px;
    padding-bottom: 80px;
}

/* 页头 */
.checkout-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
}

.checkout-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
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

/* 区块卡片 */
.section-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: 16px 20px;
    margin-bottom: 16px;
}

/* 两列排列容器 (收货地址 | 支付方式) */
.section-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
    align-items: start;
}

.section-row .section-card {
    margin-bottom: 0;
}

.section-title {
    font-size: 15px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--color-border-light);
}

/* 加载中 */
.loading-state {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 24px 0;
    color: var(--color-text-muted);
    justify-content: center;
}

.loading-text {
    font-size: 13px;
    color: var(--color-text-secondary);
}

/* === 收货地址 === */
.address-empty {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 0;
}

.address-empty-text {
    font-size: 13px;
    color: var(--color-text-secondary);
}

/* 当前选中地址展示 */
.address-current {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    border: 1px solid var(--color-border);
    border-radius: 6px;
    padding: 12px 16px;
    transition: border-color 0.2s;
}

.address-current:hover {
    border-color: var(--color-primary);
}

.address-radio-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
}

.address-item {
    border: 1px solid var(--color-border);
    border-radius: 6px;
    padding: 12px 16px;
    transition: border-color 0.2s, box-shadow 0.2s;
}

.address-item:hover {
    border-color: var(--color-primary);
}

.address-item.address-default {
    border-color: var(--color-primary);
    box-shadow: 0 2px 8px rgba(229, 57, 53, 0.1);
}

/* el-radio 占满整行 */
.address-radio {
    width: 100%;
    height: auto;
    align-items: flex-start;
}

.addr-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-left: 4px;
}

.addr-name-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}

.addr-name {
    font-size: 14px;
    font-weight: 700;
    color: var(--color-text-primary);
}

.addr-phone {
    font-size: 13px;
    color: var(--color-text-secondary);
}

.default-tag {
    font-weight: 700;
}

.addr-detail {
    font-size: 13px;
    color: var(--color-text-secondary);
    line-height: 1.5;
    word-break: break-all;
}

/* === 商品清单 === */
.goods-table-head,
.goods-row {
    display: grid;
    grid-template-columns: 1fr 120px 100px 120px;
    align-items: center;
}

.goods-table-head {
    padding: 10px 0;
    background: #fafafa;
    border-radius: 4px;
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-secondary);
}

.goods-table-head>div {
    padding: 0 12px;
}

.goods-row {
    padding: 14px 0;
    border-bottom: 1px solid var(--color-border-light);
}

.goods-row:last-child {
    border-bottom: none;
}

.goods-row>div {
    padding: 0 12px;
}

.col-info {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
}

.product-img {
    width: 64px;
    height: 64px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    overflow: hidden;
    flex-shrink: 0;
    background: #f8f8f8;
    display: flex;
    align-items: center;
    justify-content: center;
}

.product-img-tag {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.product-img-placeholder {
    width: 28px;
    height: 28px;
    color: #ccc;
}

.product-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-primary);
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.col-price,
.col-quantity,
.col-subtotal {
    text-align: center;
}

.price-text {
    font-size: 13px;
    color: var(--color-text-secondary);
}

.qty-text {
    font-size: 13px;
    color: var(--color-text-secondary);
}

.subtotal-text {
    font-size: 15px;
    font-weight: 700;
    color: var(--color-primary);
}

.goods-total {
    text-align: right;
    font-size: 13px;
    color: var(--color-text-secondary);
    padding: 14px 12px 0;
}

.total-count {
    font-weight: 700;
    color: var(--color-primary);
}

.total-amount {
    font-size: 18px;
    font-weight: 800;
    color: var(--color-primary);
    margin-left: 4px;
}

/* === 支付方式 === */
.pay-radio-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.pay-radio {
    width: 100%;
    height: auto;
    align-items: flex-start;
}

.pay-label {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-right: 8px;
}

.pay-tag {
    font-size: 11px;
    color: #9ca3af;
    background: #f3f4f6;
    padding: 1px 6px;
    border-radius: 3px;
}

.pay-insufficient {
    color: var(--color-primary);
    font-size: 12px;
    margin-left: 6px;
}

/* === 底部提交栏 === */
.checkout-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    /* padding-bottom 增大到 36px, 使底部栏总高度(约 68px) ≥ FrontLayout 页脚高度(约 67px),
       完全覆盖页脚避免深色页脚露出形成"黑色区域" */
    padding: 12px 24px 36px;
    background: #ffffff;
    border-top: 1px solid var(--color-border);
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
    z-index: 50;
    box-sizing: border-box;
    gap: 24px;
}

.footer-left {
    display: flex;
    align-items: baseline;
    gap: 4px;
}

.footer-tip {
    font-size: 13px;
    color: var(--color-text-secondary);
}

.footer-amount {
    font-size: 22px;
    font-weight: 800;
    color: var(--color-primary);
}

.footer-right {
    display: flex;
    align-items: center;
}

.btn-submit {
    padding: 10px 40px;
    font-size: 15px;
    font-weight: 700;
    border-radius: 4px;
    cursor: pointer;
    border: none;
    background: var(--color-primary);
    color: #ffffff;
    letter-spacing: 0.02em;
    transition: background 0.15s;
}

.btn-submit:hover:not(:disabled) {
    background: var(--btn-hover);
}

.btn-submit:disabled {
    background: #e5e7eb;
    color: #9ca3af;
    cursor: not-allowed;
}

/* === 小按钮 (对照 UserAddress.vue .btn-sm) === */
.btn-sm {
    padding: 5px 14px;
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
    gap: 4px;
    transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.btn-sm.primary {
    background: var(--color-primary);
    color: #fff;
    border-color: var(--color-primary);
}

.btn-sm.primary:hover {
    background: var(--btn-hover);
    border-color: var(--btn-hover);
}

.btn-sm.text {
    border: none;
    background: none;
    color: var(--color-text-secondary);
    padding: 5px 10px;
}

.btn-sm.text:hover {
    color: var(--color-primary);
}

/* === 响应式 === */
@media (max-width: 768px) {
    .checkout-page {
        padding: 16px;
        padding-bottom: 80px;
    }

    .section-card {
        padding: 14px 16px;
    }

    .section-row {
        grid-template-columns: 1fr;
    }

    .section-row .section-card {
        margin-bottom: 16px;
    }

    .goods-table-head,
    .goods-row {
        grid-template-columns: 1fr 80px 70px 90px;
    }

    .goods-table-head>div,
    .goods-row>div {
        padding: 0 6px;
    }

    .product-img {
        width: 56px;
        height: 56px;
    }

    .checkout-footer {
        padding: 10px 12px;
        gap: 12px;
    }

    .footer-amount {
        font-size: 18px;
    }

    .btn-submit {
        padding: 8px 24px;
        font-size: 14px;
    }
}
</style>