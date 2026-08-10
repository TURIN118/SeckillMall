// pages/product-list/product-list.js
// 商品列表页：分类筛选 + 排序 + 价格区间 + 2 列网格 + 触底分页
//
// 对齐：
//   - design.md 2.6 节 product-list data
//   - spec.md 5.2 节（商品列表全部业务规则）
//   - tasks.md P6
//
// 关键点：
//   1. status 前台固定 'ON_SALE'
//   2. 筛选/排序变更重置 pageNum=1 重新加载
//   3. 触底加载：onReachBottom 中判断 hasMore && !loading 防重复
//   4. 加载状态：loading / 空状态 / 没有更多 / 错误重试

const { getProductList } = require('../../api/product')
const { getCategoryTree } = require('../../api/category')

// 排序选项（对齐 ProductQueryRequest.sortBy / sortOrder）
const SORT_OPTIONS = [
    { name: '综合排序', sortBy: '', sortOrder: '' },
    { name: '价格从低到高', sortBy: 'price', sortOrder: 'asc' },
    { name: '价格从高到低', sortBy: 'price', sortOrder: 'desc' },
    { name: '销量从高到低', sortBy: 'sales', sortOrder: 'desc' },
    { name: '销量从低到高', sortBy: 'sales', sortOrder: 'asc' },
    { name: '最新上架', sortBy: 'createTime', sortOrder: 'desc' }
]

Page({
    data: {
        // 商品列表
        products: [],
        // 分页
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        loading: false,
        loadMoreStatus: 'loading', // loading | nomore | error
        // 筛选条件
        categoryId: '',
        minPrice: '',
        maxPrice: '',
        sortBy: '',
        sortOrder: '',
        keyword: '',
        // 分类
        categories: [],
        activeCategoryIndex: 0, // 0 表示全部
        // 排序
        showSort: false,
        sortOptions: SORT_OPTIONS,
        activeSortIndex: 0,
        activeSortName: '综合排序',
        // 价格区间
        showPriceFilter: false,
        tempMinPrice: '',
        tempMaxPrice: '',
        // 错误态
        loadError: false
    },

    onLoad(options) {
        // 解析参数：categoryId / keyword / title
        const opts = options || {}
        const categoryId = opts.categoryId ? decodeURIComponent(opts.categoryId) : ''
        const keyword = opts.keyword ? decodeURIComponent(opts.keyword) : ''
        const title = opts.title ? decodeURIComponent(opts.title) : ''

        this.setData({
            categoryId: categoryId,
            keyword: keyword
        })

        if (title) {
            wx.setNavigationBarTitle({ title: title })
        } else if (keyword) {
            wx.setNavigationBarTitle({ title: '搜索：' + keyword })
        } else {
            wx.setNavigationBarTitle({ title: '商品列表' })
        }

        // 并发：分类树 + 商品列表
        this._loadCategories()
        this._loadProducts(true)
    },

    onReachBottom() {
        this._loadMoreProducts()
    },

    /**
     * 加载分类树，并标记当前选中的分类
     */
    _loadCategories() {
        getCategoryTree()
            .then((res) => {
                const list = (res && res.data) || []
                const categories = Array.isArray(list) ? list : []
                // 在头部插入"全部"项
                const withAll = [{ id: '', categoryName: '全部' }].concat(categories)
                let activeIndex = 0
                if (this.data.categoryId) {
                    activeIndex = withAll.findIndex((c) => c.id === this.data.categoryId)
                    if (activeIndex < 0) activeIndex = 0
                }
                this.setData({
                    categories: withAll,
                    activeCategoryIndex: activeIndex
                })
            })
            .catch(() => {
                this.setData({ categories: [{ id: '', categoryName: '全部' }] })
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
                loadMoreStatus: 'loading',
                loadError: false
            })
        } else {
            if (!this.data.hasMore) return Promise.resolve()
        }

        this.setData({ loading: true })

        // 组装请求参数（status 固定 ON_SALE，由 api/product.js 注入）
        const params = {
            pageNum: this.data.pageNum,
            pageSize: this.data.pageSize,
            categoryId: this.data.categoryId,
            keyword: this.data.keyword,
            sortBy: this.data.sortBy,
            sortOrder: this.data.sortOrder
        }
        if (this.data.minPrice !== '') params.minPrice = this.data.minPrice
        if (this.data.maxPrice !== '') params.maxPrice = this.data.maxPrice

        return getProductList(params)
            .then((res) => {
                const data = (res && res.data) || {}
                const list = Array.isArray(data.list) ? data.list : []
                const total = typeof data.total === 'number' ? data.total : 0
                const pageNum = typeof data.pageNum === 'number' ? data.pageNum : this.data.pageNum

                const merged = reset ? list : this.data.products.concat(list)
                const hasMore = merged.length < total

                this.setData({
                    products: merged,
                    hasMore: hasMore,
                    loading: false,
                    loadError: false,
                    loadMoreStatus: hasMore ? 'loading' : 'nomore'
                })

                if (hasMore) {
                    this.setData({ pageNum: pageNum + 1 })
                }
            })
            .catch(() => {
                this.setData({
                    loading: false,
                    loadError: reset && this.data.products.length === 0,
                    loadMoreStatus: this.data.products.length > 0 ? 'nomore' : 'error'
                })
            })
    },

    /**
     * 触底加载更多
     */
    _loadMoreProducts() {
        if (!this.data.hasMore || this.data.loading) return
        this._loadProducts(false)
    },

    // ========== 筛选事件 ==========

    /**
     * 切换分类 tab
     */
    onCategoryChange(e) {
        const index = e.detail.index
        const cat = this.data.categories[index] || {}
        this.setData({
            activeCategoryIndex: index,
            categoryId: cat.id || ''
        })
        this._loadProducts(true)
    },

    /**
     * 打开排序面板
     */
    onOpenSort() {
        this.setData({ showSort: true })
    },

    /**
     * 关闭排序面板
     */
    onCloseSort() {
        this.setData({ showSort: false })
    },

    /**
     * 选择排序项
     */
    onSelectSort(e) {
        const index = e.detail.index
        const option = this.data.sortOptions[index] || {}
        this.setData({
            activeSortIndex: index,
            activeSortName: option.name,
            sortBy: option.sortBy || '',
            sortOrder: option.sortOrder || '',
            showSort: false
        })
        this._loadProducts(true)
    },

    /**
     * 打开价格筛选
     */
    onOpenPriceFilter() {
        this.setData({
            showPriceFilter: true,
            tempMinPrice: this.data.minPrice,
            tempMaxPrice: this.data.maxPrice
        })
    },

    /**
     * 关闭价格筛选
     */
    onClosePriceFilter() {
        this.setData({ showPriceFilter: false })
    },

    /**
     * 输入最低价
     */
    onInputMinPrice(e) {
        this.setData({ tempMinPrice: e.detail.value || '' })
    },

    /**
     * 输入最高价
     */
    onInputMaxPrice(e) {
        this.setData({ tempMaxPrice: e.detail.value || '' })
    },

    /**
     * 确认价格区间
     */
    onConfirmPriceFilter() {
        const min = this.data.tempMinPrice
        const max = this.data.tempMaxPrice
        // 校验：min <= max
        if (min !== '' && max !== '' && Number(min) > Number(max)) {
            wx.showToast({ title: '最低价不能大于最高价', icon: 'none' })
            return
        }
        this.setData({
            minPrice: min,
            maxPrice: max,
            showPriceFilter: false
        })
        this._loadProducts(true)
    },

    /**
     * 重置价格区间
     */
    onResetPriceFilter() {
        this.setData({
            tempMinPrice: '',
            tempMaxPrice: ''
        })
    },

    /**
     * 点击商品卡片：跳转详情
     */
    onTapProduct(e) {
        const { id } = e.detail || {}
        if (!id) return
        const url = '/pages/product-detail/product-detail?id=' + encodeURIComponent(id)
        wx.navigateTo({ url })
    },

    /**
     * 点击重试
     */
    onTapRetry() {
        this._loadProducts(true)
    }
})