// pages/favorites/favorites.js
// 收藏夹：2 列网格 product-card + 取消收藏 + 触底加载 + 空状态
//
// 对齐：
//   - design.md 2.5 节 favorites data
//   - spec.md 5.4 节（收藏管理全部业务规则）
//   - tasks.md TR7
//
// 关键点：
//   1. onShow 检查登录态，未登录跳登录页
//   2. getFavoriteList，2 列网格 product-card
//   3. 长按或按钮取消收藏(removeFavorite)，列表移除
//   4. 点击跳商品详情
//   5. 触底加载，空状态

const { getFavoriteList, removeFavorite } = require('../../api/favorite')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')
const { formatImageUrl } = require('../../utils/image')

Page({
    data: {
        // 收藏列表（每项为 FavoriteItemVO，需映射为 product-card 接受的 product）
        list: [],
        // 分页
        pageNum: 1,
        pageSize: 20,
        hasMore: true,
        // 加载态
        loading: false,
        loadMoreStatus: 'loading',
        // 空状态
        isEmpty: false
    },

    onShow() {
        // 登录拦截
        if (!isLoggedIn()) {
            const pages = getCurrentPages()
            const cur = pages[pages.length - 1]
            const redirect = cur ? '/' + cur.route : ''
            navigateToLogin(redirect)
            return
        }
        this._loadFavorites(true)
    },

    onReachBottom() {
        this._loadMore()
    },

    /**
     * 加载收藏列表
     * @param {boolean} reset true 时重置分页
     */
    _loadFavorites(reset) {
        if (this.data.loading) return Promise.resolve()
        if (reset) {
            this.setData({
                pageNum: 1,
                hasMore: true,
                list: [],
                loadMoreStatus: 'loading',
                isEmpty: false
            })
        } else {
            if (!this.data.hasMore) return Promise.resolve()
        }

        this.setData({ loading: true })

        // getFavoriteList 后端返回数组（非分页），本地模拟分页
        return getFavoriteList()
            .then((res) => {
                const data = (res && res.data) || []
                const all = Array.isArray(data) ? data : []
                // 映射为 product-card 接受的 product 对象
                const mapped = all.map((it) => ({
                    id: it.productId,
                    productId: it.productId,
                    productName: it.productName,
                    images: it.productImage ? [formatImageUrl(it.productImage)] : [],
                    originalPrice: it.price,
                    minPrice: it.price,
                    favoriteTime: it.favoriteTime,
                    __raw: it
                }))

                // 本地分页（后端若返回分页结构则直接使用）
                const start = 0
                const end = this.data.pageNum * this.data.pageSize
                const paged = mapped.slice(start, end)
                const hasMore = mapped.length > paged.length

                this.setData({
                    list: paged,
                    hasMore: hasMore,
                    loading: false,
                    isEmpty: paged.length === 0,
                    loadMoreStatus: hasMore ? 'loading' : 'nomore'
                })
            })
            .catch(() => {
                this.setData({
                    loading: false,
                    loadMoreStatus: this.data.list.length > 0 ? 'nomore' : 'error',
                    isEmpty: this.data.list.length === 0
                })
                if (reset) {
                    wx.showToast({ title: '加载失败', icon: 'none' })
                }
            })
    },

    /**
     * 触底加载更多
     */
    _loadMore() {
        if (!this.data.hasMore || this.data.loading) return
        this.setData({ pageNum: this.data.pageNum + 1 })
        this._loadFavorites(false)
    },

    /**
     * 点击收藏项：跳商品详情
     */
    onTapProduct(e) {
        const { id } = e.detail || {}
        if (!id) return
        const url = '/pages/product-detail/product-detail?id=' + encodeURIComponent(id)
        wx.navigateTo({ url })
    },

    /**
     * 长按取消收藏
     */
    onLongPressItem(e) {
        const { id } = e.currentTarget.dataset
        if (!id) return
        this._confirmRemove(id)
    },

    /**
     * 点击取消收藏按钮
     */
    onRemoveFavorite(e) {
        const { id } = e.currentTarget.dataset
        if (!id) return
        this._confirmRemove(id)
    },

    /**
     * 确认取消收藏
     */
    _confirmRemove(productId) {
        wx.showModal({
            title: '提示',
            content: '确认取消收藏？',
            success: (res) => {
                if (!res.confirm) return
                // 乐观移除
                const newList = this.data.list.filter((it) => String(it.productId) !== String(productId))
                this.setData({
                    list: newList,
                    isEmpty: newList.length === 0
                })

                removeFavorite(productId).catch(() => {
                    // 失败回滚：重新加载
                    this._loadFavorites(true)
                })
            }
        })
    },

    /**
     * 点击重试
     */
    onTapRetry() {
        this._loadFavorites(true)
    },

    /**
     * 去逛逛
     */
    onGoShopping() {
        wx.switchTab({ url: '/pages/home/home' })
    }
})