// pages/home/home.js
// 首页：轮播图 + 秒杀入口 + 分类导航 + 猜你喜欢瀑布流
//
// 对齐：
//   - design.md 2.6 节 home data
//   - spec.md 5.1 节（首页展示全部业务规则）
//   - tasks.md P5
//
// 关键点：
//   1. onLoad 并发请求 banners + categories + products（Promise.all）
//   2. 触底加载：onReachBottom 中判断 hasMore && !loading 防重复
//   3. 末页显示"没有更多了"，不再发起请求
//   4. 轮播图加载失败不阻塞其余内容

const { getActiveBanners } = require('../../api/banner')
const { getCategoryTree } = require('../../api/category')
const { getProductList } = require('../../api/product')

Page({
    data: {
        // 轮播图
        banners: [],
        // 分类树（一级）
        categories: [],
        // 商品列表
        products: [],
        // 分页
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        loading: false,
        // 末页标识
        loadMoreStatus: 'loading' // loading | nomore | error
    },

    onLoad() {
        this._initPage()
    },

    onShow() {
        // 可在此处刷新数据（如需）
    },

    onReachBottom() {
        this._loadMoreProducts()
    },

    onPullDownRefresh() {
        this._initPage().finally(() => {
            wx.stopPullDownRefresh()
        })
    },

    /**
     * 并发初始化：banners + categories + products
     */
    _initPage() {
        // 三并发：banners / categories 失败不阻塞 products
        return Promise.all([
            this._loadBanners(),
            this._loadCategories(),
            this._loadProducts(true)
        ])
    },

    /**
     * 加载轮播图（失败不阻塞）
     */
    _loadBanners() {
        return getActiveBanners()
            .then((res) => {
                const list = (res && res.data) || []
                this.setData({ banners: Array.isArray(list) ? list : [] })
            })
            .catch(() => {
                // 轮播图加载失败：隐藏轮播区域，不阻塞其余内容
                this.setData({ banners: [] })
            })
    },

    /**
     * 加载分类树（失败不阻塞）
     */
    _loadCategories() {
        return getCategoryTree()
            .then((res) => {
                const list = (res && res.data) || []
                this.setData({ categories: Array.isArray(list) ? list : [] })
            })
            .catch(() => {
                this.setData({ categories: [] })
            })
    },

    /**
     * 加载商品列表
     * @param {boolean} reset true 时重置 pageNum=1 清空 products
     */
    _loadProducts(reset) {
        if (this.data.loading) return Promise.resolve()
        if (reset) {
            this.setData({
                pageNum: 1,
                hasMore: true,
                products: [],
                loadMoreStatus: 'loading'
            })
        } else {
            if (!this.data.hasMore) return Promise.resolve()
        }

        this.setData({ loading: true })

        const params = {
            pageNum: this.data.pageNum,
            pageSize: this.data.pageSize
        }

        return getProductList(params)
            .then((res) => {
                const data = (res && res.data) || {}
                const list = Array.isArray(data.list) ? data.list : []
                const total = typeof data.total === 'number' ? data.total : 0
                const pageNum = typeof data.pageNum === 'number' ? data.pageNum : this.data.pageNum
                const pageSize = typeof data.pageSize === 'number' ? data.pageSize : this.data.pageSize

                const merged = reset ? list : this.data.products.concat(list)
                const hasMore = merged.length < total

                this.setData({
                    products: merged,
                    hasMore: hasMore,
                    loading: false,
                    loadMoreStatus: hasMore ? 'loading' : 'nomore'
                })

                // 自动推进 pageNum（用于触底加载下一页）
                if (hasMore) {
                    this.setData({ pageNum: pageNum + 1 })
                }
            })
            .catch(() => {
                this.setData({
                    loading: false,
                    loadMoreStatus: this.data.products.length > 0 ? 'nomore' : 'error'
                })
                if (reset) {
                    wx.showToast({ title: '加载失败，请下拉刷新', icon: 'none' })
                }
            })
    },

    /**
     * 触底加载更多
     */
    _loadMoreProducts() {
        if (!this.data.hasMore || this.data.loading) return
        this._loadProducts(false)
    },

    // ========== 事件处理 ==========

    /**
     * 点击轮播图：跳转 linkUrl
     */
    onTapBanner(e) {
        const { linkUrl } = e.currentTarget.dataset
        if (!linkUrl) return
        // linkUrl 可能是小程序内部页面路径或 webview URL
        if (linkUrl.indexOf('/pages/') === 0) {
            wx.navigateTo({ url: linkUrl })
        } else {
            // 外链暂不处理，可后续接入 webview
            wx.showToast({ title: '暂不支持该链接', icon: 'none' })
        }
    },

    /**
     * 点击秒杀入口：跳转秒杀专区（tabBar 页用 switchTab）
     */
    onTapSeckill() {
        wx.switchTab({ url: '/pages/seckill/seckill' })
    },

    /**
     * 点击分类：跳转商品列表携带 categoryId
     */
    onTapCategory(e) {
        const { id, name } = e.currentTarget.dataset
        if (!id) return
        const url = '/pages/product-list/product-list?categoryId=' +
            encodeURIComponent(id) + '&title=' + encodeURIComponent(name || '')
        wx.navigateTo({ url })
    },

    /**
     * 点击商品卡片：跳转商品详情
     */
    onTapProduct(e) {
        const { id } = e.detail || {}
        if (!id) return
        const url = '/pages/product-detail/product-detail?id=' + encodeURIComponent(id)
        wx.navigateTo({ url })
    },

    /**
     * 点击重试（load-more error 状态）
     */
    onTapRetry() {
        this._loadProducts(true)
    }
})