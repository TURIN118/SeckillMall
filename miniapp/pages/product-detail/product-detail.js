// pages/product-detail/product-detail.js
// 商品详情页：图片轮播 + 商品信息 + SKU 选择 + 参数 + 富文本 + 底部操作栏
//
// 对齐：
//   - design.md 2.6 节 product-detail data
//   - spec.md 5.3 节（商品详情全部业务规则）
//   - tasks.md P7
//
// 关键点：
//   1. 商品 ID 全程 string，URL 用 encodeURIComponent
//   2. SKU 选择：hasSku 时展示 sku-selector，选中更新价格库存
//   3. 富文本渲染前必须 sanitize（rich-text-viewer 组件内完成）
//   4. 底部操作登录态拦截：未登录跳登录页
//   5. 加购调 api/cart.js addCart，收藏调 api/favorite.js toggleFavorite
//   6. 立即购买跳 checkout 页（携带商品+SKU+quantity）

const { getProductDetail } = require('../../api/product')
const { addCart } = require('../../api/cart')
const { toggleFavorite } = require('../../api/favorite')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')
const { formatImageUrl } = require('../../utils/image')

Page({
    data: {
        // 商品 ID（string）
        productId: '',
        // 商品对象 ProductVO
        product: null,
        // 当前选中 SKU（无 SKU 商品为 null，使用 originalPrice）
        currentSku: null,
        // 各属性已选值
        selectedAttrs: {},
        // 是否显示 SKU 面板
        showSkuPanel: false,
        // 收藏加载中
        favLoading: false,
        // 提交中（加购/购买）
        submitting: false,
        // 加载态
        loading: true,
        // 错误态
        loadError: false,
        // 展示价（currentSku?.price 或 product.originalPrice）
        displayPrice: 0,
        // 展示库存
        displayStock: 0,
        // 是否多规格
        hasSku: false,
        // 是否库存充足
        canBuy: false,
        // 当前页路径（用于登录回跳）
        currentUrl: ''
    },

    onLoad(options) {
        const opts = options || {}
        const id = opts.id ? decodeURIComponent(opts.id) : ''

        if (!id) {
            this.setData({ loading: false, loadError: true })
            wx.showToast({ title: '商品 ID 缺失', icon: 'none' })
            return
        }

        this.setData({ productId: id })

        // 记录当前页路径（用于登录回跳）
        const pages = getCurrentPages()
        const cur = pages[pages.length - 1]
        if (cur) {
            this.setData({ currentUrl: '/' + cur.route + '?id=' + encodeURIComponent(id) })
        }

        this._loadDetail(id)
    },

    /**
     * 加载商品详情
     */
    _loadDetail(id) {
        this.setData({ loading: true, loadError: false })
        getProductDetail(id)
            .then((res) => {
                let product = (res && res.data) || null
                if (!product) {
                    this.setData({ loading: false, loadError: true })
                    return
                }
                // 拼接图片 BASE_URL（images 为相对路径数组）
                product = Object.assign({}, product, {
                    images: Array.isArray(product.images) ? product.images.map(formatImageUrl) : []
                })
                // 预处理商品属性：把 values 列表拼接成展示字符串
                if (Array.isArray(product.attributes)) {
                    product.attributes = product.attributes.map((attr) => {
                        const displayValue = (Array.isArray(attr.values) ? attr.values : [])
                            .map((v) => v.value || '')
                            .filter(Boolean)
                            .join(' / ')
                        return Object.assign({}, attr, { _displayValue: displayValue })
                    })
                }
                const hasSku = !!product.hasSku
                const originalPrice = product.originalPrice != null ? product.originalPrice : 0
                const stock = product.totalStock != null ? product.totalStock : (product.stock || 0)
                this.setData({
                    product: product,
                    loading: false,
                    loadError: false,
                    hasSku: hasSku,
                    displayPrice: originalPrice,
                    displayStock: stock,
                    canBuy: stock > 0
                })
            })
            .catch(() => {
                this.setData({ loading: false, loadError: true })
                wx.showToast({ title: '加载失败', icon: 'none' })
            })
    },

    // ========== SKU 选择 ==========

    /**
     * 打开 SKU 面板
     */
    onOpenSkuPanel() {
        if (!this.data.hasSku) return
        this.setData({ showSkuPanel: true })
    },

    /**
     * 关闭 SKU 面板
     */
    onCloseSkuPanel() {
        this.setData({ showSkuPanel: false })
    },

    /**
     * SKU 选择确认：更新当前 SKU、价格、库存
     */
    onSkuConfirm(e) {
        const { sku } = e.detail || {}
        if (!sku) return
        const price = sku.price != null ? sku.price : 0
        const stock = sku.stock != null ? sku.stock : 0
        this.setData({
            currentSku: sku,
            showSkuPanel: false,
            displayPrice: price,
            displayStock: stock,
            canBuy: stock > 0
        })
    },

    // ========== 底部操作 ==========

    /**
     * 加入购物车
     */
    onAddCart() {
        if (!this._checkLogin()) return
        if (!this._checkSkuSelected()) return
        if (!this.data.canBuy) {
            wx.showToast({ title: '库存不足', icon: 'none' })
            return
        }

        const product = this.data.product
        const sku = this.data.currentSku
        const data = {
            productId: String(product.id),
            quantity: 1
        }
        if (sku && sku.id) data.skuId = String(sku.id)

        this.setData({ submitting: true })
        addCart(data)
            .then(() => {
                wx.showToast({ title: '已加入购物车', icon: 'success' })
            })
            .catch(() => {
                // 错误提示已由 request 拦截器处理
            })
            .finally(() => {
                this.setData({ submitting: false })
            })
    },

    /**
     * 立即购买：跳转结算页（checkout 由 trade 模块实现）
     */
    onBuyNow() {
        if (!this._checkLogin()) return
        if (!this._checkSkuSelected()) return
        if (!this.data.canBuy) {
            wx.showToast({ title: '库存不足', icon: 'none' })
            return
        }

        const product = this.data.product
        const sku = this.data.currentSku
        const params = [
            'productId=' + encodeURIComponent(String(product.id)),
            'quantity=1'
        ]
        if (sku && sku.id) {
            params.push('skuId=' + encodeURIComponent(String(sku.id)))
        }
        const url = '/pages/checkout/checkout?' + params.join('&')
        wx.navigateTo({ url })
    },

    /**
     * 切换收藏
     */
    onToggleFavorite() {
        if (!this._checkLogin()) return
        if (this.data.favLoading) return

        const product = this.data.product
        const data = { productId: String(product.id) }

        this.setData({ favLoading: true })
        toggleFavorite(data)
            .then(() => {
                wx.showToast({ title: '操作成功', icon: 'success' })
            })
            .catch(() => {
                // 错误提示已由 request 拦截器处理
            })
            .finally(() => {
                this.setData({ favLoading: false })
            })
    },

    // ========== 工具方法 ==========

    /**
     * 登录态检查：未登录跳登录页
     * @returns {boolean} 是否已登录
     */
    _checkLogin() {
        if (isLoggedIn()) return true
        navigateToLogin(this.data.currentUrl)
        return false
    },

    /**
     * 检查 SKU 是否已选（多规格商品必须先选规格）
     * @returns {boolean} 是否可继续操作
     */
    _checkSkuSelected() {
        if (this.data.hasSku && !this.data.currentSku) {
            wx.showToast({ title: '请选择规格', icon: 'none' })
            // 自动打开 SKU 面板
            this.setData({ showSkuPanel: true })
            return false
        }
        return true
    },

    /**
     * 点击重试
     */
    onTapRetry() {
        this._loadDetail(this.data.productId)
    },

    /**
     * 返回上一页
     */
    onGoBack() {
        wx.navigateBack({ delta: 1 })
    }
})