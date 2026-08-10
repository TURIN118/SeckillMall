// pages/checkout/checkout.js
// 结算页：地址 + 商品明细 + 备注 + 支付方式 + 提交下单
//
// 对齐：
//   - design.md 2.5 节 checkout data
//   - spec.md 5.2 节（结算下单全部业务规则）
//   - tasks.md TR4
//
// 关键点：
//   1. onLoad 解析参数：fromCart=1&cartIds=xxx 或 buyNow=1&productId=xxx&skuId=xxx&quantity=xxx
//   2. 地址区：getAddressList 取默认地址展示，点击跳 address-list 选择
//      选择后 navigateBack 携带地址，本页 onShow 通过全局事件通道接收
//   3. 商品明细：fromCart 时从购物车选中项展示，buyNow 时从 product 详情传入
//   4. 备注 van-field，支付方式 van-radio-group(ALIPAY/WALLET)
//   5. 提交：fromCart 走 checkoutFromCart，否则走 buyNow；成功 wx.redirectTo 订单详情
//   6. 无地址禁用提交并提示

const { getCartList } = require('../../api/cart')
const { getProductDetail } = require('../../api/product')
const { getAddressList } = require('../../api/address')
const { buyNow, checkoutFromCart } = require('../../api/order')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')
const { formatPrice } = require('../../utils/format')

// 全局事件通道：address-list 选择地址后通过 app.globalData 传递
const ADDRESS_SELECT_KEY = '__checkoutSelectedAddress__'

Page({
    data: {
        // 入参模式
        fromCart: false,
        buyNow: false,
        // 立即购买入参
        buyNowData: {
            productId: '',
            skuId: '',
            quantity: 1
        },
        // 购物车结算入参
        cartIds: [],
        // 收货地址
        address: null,
        // 商品明细列表（统一格式：{productId, productName, productImage, price, quantity, skuName}）
        items: [],
        // 备注
        remark: '',
        // 支付方式
        payMethod: 'ALIPAY',
        // 派生字段
        totalQuantity: 0,
        totalPrice: 0,
        formattedTotal: '0.00',
        // 状态
        loading: false,
        submitting: false,
        // 是否可提交（有地址 + 有商品 + 未提交中）
        canSubmit: false
    },

    onLoad(options) {
        // 登录拦截
        if (!isLoggedIn()) {
            const pages = getCurrentPages()
            const cur = pages[pages.length - 1]
            const redirect = cur ? '/' + cur.route : ''
            navigateToLogin(redirect)
            return
        }

        const opts = options || {}
        this._parseOptions(opts)

        // 并发加载地址 + 商品明细
        this._loadAddress()
        this._loadItems()
    },

    onShow() {
        // 接收 address-list 选择回传的地址（通过全局数据通道）
        const app = getApp()
        if (app && app.globalData && app.globalData[ADDRESS_SELECT_KEY]) {
            const selected = app.globalData[ADDRESS_SELECT_KEY]
            app.globalData[ADDRESS_SELECT_KEY] = null
            this.setData({ address: selected })
            this._recalcCanSubmit()
        }
    },

    /**
     * 解析 onLoad 参数
     */
    _parseOptions(opts) {
        if (opts.fromCart === '1' || opts.fromCart === 1) {
            // 购物车结算：cartIds=xxx,xxx
            const cartIdsStr = opts.cartIds ? decodeURIComponent(opts.cartIds) : ''
            const cartIds = cartIdsStr
                ? cartIdsStr.split(',').filter((s) => !!s).map((s) => String(s))
                : []
            this.setData({ fromCart: true, cartIds: cartIds })
        } else if (opts.buyNow === '1' || opts.buyNow === 1) {
            // 立即购买
            const productId = opts.productId ? decodeURIComponent(opts.productId) : ''
            const skuId = opts.skuId ? decodeURIComponent(opts.skuId) : ''
            const quantity = parseInt(opts.quantity, 10) || 1
            this.setData({
                buyNow: true,
                buyNowData: { productId: productId, skuId: skuId, quantity: quantity }
            })
        }
    },

    /**
     * 加载地址列表，取默认地址（或第一个）
     */
    _loadAddress() {
        getAddressList()
            .then((res) => {
                const list = (res && res.data) || []
                if (!Array.isArray(list) || list.length === 0) {
                    this.setData({ address: null })
                    this._recalcCanSubmit()
                    return
                }
                // 优先取默认地址，否则取第一个
                const def = list.find((a) => a.isDefault) || list[0]
                this.setData({ address: def })
                this._recalcCanSubmit()
            })
            .catch(() => {
                this.setData({ address: null })
                this._recalcCanSubmit()
            })
    },

    /**
     * 加载商品明细
     *   - fromCart: 重新查购物车，过滤 cartIds
     *   - buyNow:   查商品详情，构造单项
     */
    _loadItems() {
        this.setData({ loading: true })
        if (this.data.fromCart) {
            this._loadItemsFromCart()
        } else if (this.data.buyNow) {
            this._loadItemsBuyNow()
        } else {
            this.setData({ loading: false })
        }
    },

    /**
     * 购物车结算：从购物车列表过滤选中项
     */
    _loadItemsFromCart() {
        const cartIds = this.data.cartIds
        if (cartIds.length === 0) {
            this.setData({ loading: false, items: [] })
            this._recalcTotal()
            return
        }
        getCartList()
            .then((res) => {
                const list = (res && res.data) || []
                const allItems = Array.isArray(list) ? list : []
                // 过滤出选中的 cartIds
                const items = allItems
                    .filter((it) => cartIds.indexOf(String(it.id)) !== -1)
                    .map((it) => ({
                        cartId: String(it.id),
                        productId: String(it.productId),
                        productName: it.productName,
                        productImage: it.productImage,
                        price: it.price,
                        quantity: parseInt(it.quantity, 10) || 1,
                        skuName: it.skuName || ''
                    }))
                this.setData({ items: items, loading: false })
                this._recalcTotal()
                this._recalcCanSubmit()
            })
            .catch(() => {
                this.setData({ loading: false, items: [] })
                this._recalcTotal()
            })
    },

    /**
     * 立即购买：查商品详情构造单项
     */
    _loadItemsBuyNow() {
        const { productId, skuId, quantity } = this.data.buyNowData
        if (!productId) {
            this.setData({ loading: false, items: [] })
            return
        }
        getProductDetail(productId)
            .then((res) => {
                const product = (res && res.data) || null
                if (!product) {
                    this.setData({ loading: false, items: [] })
                    return
                }
                // 取 SKU 价格（若有 skuId），否则取 originalPrice
                let price = product.originalPrice || 0
                let skuName = ''
                if (skuId && Array.isArray(product.skuList)) {
                    const sku = product.skuList.find((s) => String(s.id) === String(skuId))
                    if (sku) {
                        price = sku.price || price
                        skuName = sku.skuName || ''
                    }
                }
                const images = Array.isArray(product.images) ? product.images : []
                const items = [{
                    cartId: '',
                    productId: String(product.id),
                    productName: product.productName,
                    productImage: images[0] || '',
                    price: price,
                    quantity: quantity,
                    skuName: skuName
                }]
                this.setData({ items: items, loading: false })
                this._recalcTotal()
                this._recalcCanSubmit()
            })
            .catch(() => {
                this.setData({ loading: false, items: [] })
                this._recalcTotal()
            })
    },

    // ========== 事件处理 ==========

    /**
     * 点击地址区：跳 address-list 选择
     */
    onTapAddress() {
        wx.navigateTo({
            url: '/subpackages/user-center/address-list/address-list?selectMode=1'
        })
    },

    /**
     * 备注输入
     */
    onRemarkInput(e) {
        this.setData({ remark: e.detail.value || '' })
    },

    /**
     * 支付方式切换（van-radio-group change）
     */
    onPayMethodChange(e) {
        this.setData({ payMethod: e.detail })
    },

    /**
     * 点击 cell 选中支付方式
     */
    onSelectPayMethod(e) {
        const { name } = e.currentTarget.dataset
        if (!name) return
        this.setData({ payMethod: name })
    },

    /**
     * 提交订单
     */
    onSubmit() {
        if (this.data.submitting) return
        if (!this.data.address) {
            wx.showToast({ title: '请选择收货地址', icon: 'none' })
            return
        }
        if (this.data.items.length === 0) {
            wx.showToast({ title: '无可结算商品', icon: 'none' })
            return
        }

        this.setData({ submitting: true })
        const addressId = String(this.data.address.id)
        const remark = this.data.remark

        if (this.data.fromCart) {
            // 购物车结算
            const payload = {
                addressId: addressId,
                cartIds: this.data.cartIds,
                remark: remark
            }
            checkoutFromCart(payload)
                .then((res) => this._onCreateSuccess(res))
                .catch(() => { })
                .finally(() => {
                    this.setData({ submitting: false })
                })
        } else {
            // 立即购买
            const { productId, skuId, quantity } = this.data.buyNowData
            const payload = {
                productId: productId,
                quantity: quantity,
                addressId: addressId,
                remark: remark
            }
            if (skuId) payload.skuId = skuId
            buyNow(payload)
                .then((res) => this._onCreateSuccess(res))
                .catch(() => { })
                .finally(() => {
                    this.setData({ submitting: false })
                })
        }
    },

    /**
     * 下单成功：跳订单详情
     * 后端返回 { orderId, orderType }
     */
    _onCreateSuccess(res) {
        const data = (res && res.data) || {}
        const orderId = data.orderId ? String(data.orderId) : ''
        const orderType = data.orderType || 'NORMAL'
        if (!orderId) {
            wx.showToast({ title: '下单成功', icon: 'success' })
            // 跳订单中心
            wx.switchTab({ url: '/pages/orders/orders' })
            return
        }
        wx.showToast({ title: '下单成功', icon: 'success', duration: 1000 })
        // redirectTo 替换当前页，避免返回回到结算页
        const url = '/pages/order-detail/order-detail?id=' +
            encodeURIComponent(orderId) + '&orderType=' + encodeURIComponent(orderType)
        setTimeout(() => {
            wx.redirectTo({ url })
        }, 800)
    },

    // ========== 工具方法 ==========

    /**
     * 重新计算合计
     */
    _recalcTotal() {
        const items = this.data.items
        let totalPrice = 0
        let totalQuantity = 0
        items.forEach((it) => {
            const price = Number(it.price) || 0
            const qty = parseInt(it.quantity, 10) || 0
            totalPrice += price * qty
            totalQuantity += qty
        })
        this.setData({
            totalPrice: totalPrice,
            totalQuantity: totalQuantity,
            formattedTotal: formatPrice(totalPrice)
        })
    },

    /**
     * 重新计算可提交状态
     */
    _recalcCanSubmit() {
        const canSubmit = !!this.data.address && this.data.items.length > 0
        this.setData({ canSubmit: canSubmit })
    }
})