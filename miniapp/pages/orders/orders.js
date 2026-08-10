// pages/orders/orders.js
// 订单中心：状态筛选 + 统一订单列表 + 触底加载 + 左滑删除
//
// 对齐：
//   - design.md 2.5 节 orders data
//   - spec.md 5.3 节（订单管理全部业务规则）
//   - tasks.md TR5
//
// 关键点：
//   1. onShow 检查登录态，未登录跳登录页（tabBar 页）
//   2. 顶部 van-tabs 状态筛选（全部''/PENDING_PAY/PENDING_SHIP/PENDING_RECEIVE/COMPLETED）
//   3. order-card 列表，onReachBottom 触底加载
//   4. van-swipe-cell 左滑删除（仅 COMPLETED/CANCELLED 可删除）
//   5. 切换状态重置 pageNum=1

const { getUnifiedOrders, deleteOrder } = require('../../api/order')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')

// 状态 tab 配置
const STATUS_TABS = [
    { name: '全部', value: '' },
    { name: '待付款', value: 'PENDING_PAY' },
    { name: '待发货', value: 'PENDING_SHIP' },
    { name: '待收货', value: 'PENDING_RECEIVE' },
    { name: '已完成', value: 'COMPLETED' }
]

// 可删除状态
const DELETABLE_STATUS = ['COMPLETED', 'CANCELLED']

Page({
    data: {
        // 状态 tab 配置
        tabs: STATUS_TABS,
        // 当前激活 tab index
        activeTab: 0,
        // 当前状态筛选
        status: '',
        // 订单列表
        orders: [],
        // 分页
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        // 加载态
        loading: false,
        // 加载更多状态
        loadMoreStatus: 'loading',
        // 是否空列表
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
        // 每次进入刷新当前 tab（支付/确认收货后状态变化）
        this._loadOrders(true)
    },

    onReachBottom() {
        this._loadMore()
    },

    /**
     * 切换状态 tab
     */
    onTabChange(e) {
        const index = e.detail.index
        const tab = this.data.tabs[index] || { value: '' }
        this.setData({
            activeTab: index,
            status: tab.value
        })
        // 切换状态重置 pageNum=1
        this._loadOrders(true)
    },

    /**
     * 加载订单列表
     * @param {boolean} reset true 时重置分页
     */
    _loadOrders(reset) {
        if (this.data.loading) return Promise.resolve()
        if (reset) {
            this.setData({
                pageNum: 1,
                hasMore: true,
                orders: [],
                loadMoreStatus: 'loading',
                isEmpty: false
            })
        } else {
            if (!this.data.hasMore) return Promise.resolve()
        }

        this.setData({ loading: true })

        const params = {
            pageNum: this.data.pageNum,
            pageSize: this.data.pageSize
        }
        if (this.data.status) params.status = this.data.status

        return getUnifiedOrders(params)
            .then((res) => {
                const data = (res && res.data) || {}
                const list = Array.isArray(data.list) ? data.list : []
                const total = typeof data.total === 'number' ? data.total : 0
                const pageNum = typeof data.pageNum === 'number' ? data.pageNum : this.data.pageNum

                const merged = reset ? list : this.data.orders.concat(list)
                const hasMore = merged.length < total

                this.setData({
                    orders: merged,
                    hasMore: hasMore,
                    loading: false,
                    isEmpty: merged.length === 0,
                    loadMoreStatus: hasMore ? 'loading' : 'nomore'
                })

                if (hasMore) {
                    this.setData({ pageNum: pageNum + 1 })
                }
            })
            .catch(() => {
                this.setData({
                    loading: false,
                    loadMoreStatus: this.data.orders.length > 0 ? 'nomore' : 'error',
                    isEmpty: this.data.orders.length === 0
                })
                if (reset) {
                    wx.showToast({ title: '加载失败，请下拉刷新', icon: 'none' })
                }
            })
    },

    /**
     * 触底加载更多
     */
    _loadMore() {
        if (!this.data.hasMore || this.data.loading) return
        this._loadOrders(false)
    },

    /**
     * 点击订单卡片：跳详情
     */
    onTapOrder(e) {
        const { orderId, orderType } = e.detail || {}
        if (!orderId) return
        const url = '/pages/order-detail/order-detail?id=' +
            encodeURIComponent(orderId) + '&orderType=' + encodeURIComponent(orderType || 'NORMAL')
        wx.navigateTo({ url })
    },

    /**
     * 左滑删除订单（仅 COMPLETED/CANCELLED 可删除）
     */
    onDeleteOrder(e) {
        const { index } = e.currentTarget.dataset
        const orders = this.data.orders
        const order = orders[index]
        if (!order) return

        if (DELETABLE_STATUS.indexOf(order.status) === -1) {
            wx.showToast({ title: '当前状态不可删除', icon: 'none' })
            return
        }

        wx.showModal({
            title: '提示',
            content: '确认删除该订单？',
            success: (res) => {
                if (!res.confirm) return
                // 乐观删除
                const newOrders = orders.slice(0, index).concat(orders.slice(index + 1))
                this.setData({
                    orders: newOrders,
                    isEmpty: newOrders.length === 0
                })

                deleteOrder(order.orderId).catch(() => {
                    // 失败回滚：重新加载
                    this._loadOrders(true)
                })
            }
        })
    },

    /**
     * 点击重试
     */
    onTapRetry() {
        this._loadOrders(true)
    }
})