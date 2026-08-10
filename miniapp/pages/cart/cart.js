// pages/cart/cart.js
// 购物车页：列表 + 数量调整 + 选中 + 左滑删除 + 实时合计 + 结算跳转
//
// 对齐：
//   - design.md 2.5 节 cart data
//   - spec.md 5.1 节（购物车管理全部业务规则）
//   - tasks.md TR3
//
// 关键点：
//   1. onShow 检查登录态，未登录跳登录页（tabBar 页）
//   2. van-swipe-cell 左滑删除，van-stepper 改数量，van-checkbox 选中
//   3. 实时计算合计（仅选中项 price*quantity 求和）
//   4. 全选/全不选 batchUpdateSelected
//   5. 底部 van-submit-bar 结算按钮，携带选中 cartIds 跳 checkout
//   6. 空状态 van-empty + 去逛逛（switchTab 首页）

const {
    getCartList,
    updateQuantity,
    removeCart,
    updateSelected,
    batchUpdateSelected
} = require('../../api/cart')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')
const { formatPrice } = require('../../utils/format')
const { equalId } = require('../../utils/id')

Page({
    data: {
        // 购物车项列表
        items: [],
        // 全选标识
        allSelected: false,
        // 合计金额（仅选中项）
        totalPrice: 0,
        // 选中项数量
        selectedCount: 0,
        // 格式化后的合计
        formattedTotal: '0.00',
        // 加载态
        loading: false,
        // 是否编辑模式（预留，本期不实现编辑态切换）
        editing: false,
        // 是否空状态
        isEmpty: false
    },

    onShow() {
        // 登录拦截：tabBar 页 onShow 检查
        if (!isLoggedIn()) {
            const pages = getCurrentPages()
            const cur = pages[pages.length - 1]
            const redirect = cur ? '/' + cur.route : ''
            navigateToLogin(redirect)
            return
        }
        this._loadCart()
    },

    /**
     * 加载购物车列表
     */
    _loadCart() {
        this.setData({ loading: true })
        getCartList()
            .then((res) => {
                const list = (res && res.data) || []
                const items = Array.isArray(list) ? list : []
                this._hydrate(items)
            })
            .catch(() => {
                // 错误提示已由 request 拦截器处理
                this.setData({ loading: false, isEmpty: true })
            })
    },

    /**
     * 填充数据并计算派生字段
     * @param {Array} items 购物车项
     */
    _hydrate(items) {
        // 计算全选、合计、选中数量
        const selectedItems = items.filter((it) => it.selected)
        const allSelected = items.length > 0 && selectedItems.length === items.length
        let totalPrice = 0
        selectedItems.forEach((it) => {
            const price = Number(it.price) || 0
            const qty = parseInt(it.quantity, 10) || 0
            totalPrice += price * qty
        })
        const selectedCount = selectedItems.length

        this.setData({
            items: items,
            allSelected: allSelected,
            totalPrice: totalPrice,
            selectedCount: selectedCount,
            formattedTotal: formatPrice(totalPrice),
            loading: false,
            isEmpty: items.length === 0
        })
    },

    // ========== 数量调整 ==========

    /**
     * van-stepper 数量变化
     */
    onStepperChange(e) {
        const { index } = e.currentTarget.dataset
        const quantity = e.detail
        const items = this.data.items
        const item = items[index]
        if (!item) return

        // 乐观更新：先改本地，再发请求；失败回滚
        const oldQty = item.quantity
        this._updateItem(index, { quantity: quantity })
        this._recalc()

        updateQuantity(item.id, quantity)
            .catch(() => {
                // 回滚
                this._updateItem(index, { quantity: oldQty })
                this._recalc()
            })
    },

    // ========== 选中 ==========

    /**
     * 单项选中/取消
     */
    onItemSelect(e) {
        const { index } = e.currentTarget.dataset
        const selected = e.detail
        const items = this.data.items
        const item = items[index]
        if (!item) return

        // 乐观更新
        this._updateItem(index, { selected: selected })
        this._recalc()

        updateSelected(item.id, selected).catch(() => {
            // 回滚
            this._updateItem(index, { selected: !selected })
            this._recalc()
        })
    },

    /**
     * 全选/全不选
     */
    onToggleAll(e) {
        const allSelected = e.detail
        const items = this.data.items
        if (items.length === 0) return

        const cartIds = items.map((it) => String(it.id))

        // 乐观更新
        const newItems = items.map((it) => Object.assign({}, it, { selected: allSelected }))
        this.setData({ items: newItems, allSelected: allSelected })
        this._recalc()

        batchUpdateSelected(cartIds, allSelected).catch(() => {
            // 回滚：恢复原选中态
            const restored = items.map((it) => Object.assign({}, it))
            this.setData({ items: restored })
            this._recalc()
        })
    },

    // ========== 删除 ==========

    /**
     * 左滑删除单项
     */
    onRemoveItem(e) {
        const { index } = e.currentTarget.dataset
        const items = this.data.items
        const item = items[index]
        if (!item) return

        wx.showModal({
            title: '提示',
            content: '确认删除该购物车项？',
            success: (res) => {
                if (!res.confirm) return
                // 乐观删除
                const newItems = items.slice(0, index).concat(items.slice(index + 1))
                this._hydrate(newItems)

                removeCart(item.id).catch(() => {
                    // 失败回滚：重新加载
                    this._loadCart()
                })
            }
        })
    },

    // ========== 结算 ==========

    /**
     * 点击结算：携带选中 cartIds 跳 checkout
     */
    onCheckout() {
        const selectedItems = this.data.items.filter((it) => it.selected)
        if (selectedItems.length === 0) {
            wx.showToast({ title: '请选择要结算的商品', icon: 'none' })
            return
        }
        const cartIds = selectedItems.map((it) => String(it.id))
        const url = '/pages/checkout/checkout?fromCart=1&cartIds=' +
            encodeURIComponent(cartIds.join(','))
        wx.navigateTo({ url })
    },

    /**
     * 去逛逛：跳首页（tabBar 页用 switchTab）
     */
    onGoShopping() {
        wx.switchTab({ url: '/pages/home/home' })
    },

    // ========== 工具方法 ==========

    /**
     * 更新指定 index 的购物车项字段
     */
    _updateItem(index, patch) {
        const items = this.data.items
        const newItem = Object.assign({}, items[index], patch)
        const newItems = items.slice(0, index).concat([newItem], items.slice(index + 1))
        this.setData({ items: newItems })
    },

    /**
     * 重新计算派生字段（合计、全选、选中数量）
     */
    _recalc() {
        const items = this.data.items
        const selectedItems = items.filter((it) => it.selected)
        const allSelected = items.length > 0 && selectedItems.length === items.length
        let totalPrice = 0
        selectedItems.forEach((it) => {
            const price = Number(it.price) || 0
            const qty = parseInt(it.quantity, 10) || 0
            totalPrice += price * qty
        })
        this.setData({
            allSelected: allSelected,
            totalPrice: totalPrice,
            selectedCount: selectedItems.length,
            formattedTotal: formatPrice(totalPrice),
            isEmpty: items.length === 0
        })
    }
})