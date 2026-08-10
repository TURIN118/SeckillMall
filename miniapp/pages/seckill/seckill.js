// pages/seckill/seckill.js
// 秒杀专区页：场次列表 + 当前场次商品 + 倒计时 + 抢购入口
//
// 对齐：
//   - .codeartsdoer/specs/seckill/spec.md 3.1 节
//   - .codeartsdoer/specs/seckill/design.md 5 节
//   - tasks.md S3
//
// 关键点：
//   1. onLoad 调 listActivities() 获取场次；无场次回退 getSeckillList() 全部秒杀商品
//   2. 场次 tab 横向滚动切换，更新商品列表
//   3. 商品卡片：van-card + seckill-countdown + 状态按钮
//   4. bind:statuschange 控制按钮文字（即将开抢/立即抢购/已结束）
//   5. 点击商品跳转 /pages/seckill-detail/seckill-detail?seckillId=xxx
//   6. onPullDownRefresh 重新拉取场次与商品

const {
    listActivities,
    getSeckillList
} = require('../../api/seckill')
const { formatImageUrl } = require('../../utils/image')

// 倒计时状态 → 按钮文案
const BTN_TEXT_MAP = {
    BEFORE: '即将开抢',
    RUNNING: '立即抢购',
    ENDED: '已结束'
}

Page({
    data: {
        // 场次列表
        activities: [],
        // 当前选中场次索引
        currentActivityIndex: 0,
        // 当前场次秒杀商品列表（每项含 _status/_btnText 用于渲染）
        goodsList: [],
        // 是否无场次（回退模式：直接展示全部秒杀商品）
        fallbackMode: false,
        // 加载态
        loading: true,
        // 错误态
        loadError: false,
        // 空态
        isEmpty: false
    },

    onLoad() {
        this._initPage()
    },

    onPullDownRefresh() {
        this._initPage().finally(() => {
            wx.stopPullDownRefresh()
        })
    },

    /**
     * 初始化：拉场次 → 选首个 → 拉商品
     */
    _initPage() {
        this.setData({ loading: true, loadError: false, isEmpty: false })
        return listActivities()
            .then((res) => {
                const list = (res && res.data) || []
                const activities = Array.isArray(list) ? list : []

                if (activities.length === 0) {
                    // 无场次：回退拉全部秒杀商品
                    return this._fallbackLoadAll()
                }

                this.setData({
                    activities: activities,
                    currentActivityIndex: 0,
                    fallbackMode: false,
                    loading: false
                })
                // 加载首个场次商品
                return this._loadActivityGoods(0)
            })
            .catch(() => {
                this.setData({
                    loading: false,
                    loadError: true,
                    activities: [],
                    goodsList: []
                })
            })
    },

    /**
     * 回退模式：无场次时直接拉全部秒杀商品
     */
    _fallbackLoadAll() {
        return getSeckillList({ pageNum: 1, pageSize: 20 })
            .then((res) => {
                const data = (res && res.data) || {}
                const list = Array.isArray(data.list) ? data.list : []
                const goods = this._normalizeGoods(list)
                this.setData({
                    fallbackMode: true,
                    activities: [],
                    goodsList: goods,
                    loading: false,
                    loadError: false,
                    isEmpty: goods.length === 0
                })
            })
            .catch(() => {
                this.setData({
                    fallbackMode: true,
                    activities: [],
                    goodsList: [],
                    loading: false,
                    loadError: true
                })
            })
    },

    /**
     * 加载指定场次商品
     * 优先用场次 VO 内的 goods 字段；否则回退 getSeckillList({ activityId })
     * @param {number} index 场次索引
     */
    _loadActivityGoods(index) {
        const activity = this.data.activities[index]
        if (!activity) {
            this.setData({ goodsList: [], isEmpty: true })
            return Promise.resolve()
        }

        // 1. 场次 VO 内含 goods 列表
        if (Array.isArray(activity.goods) && activity.goods.length > 0) {
            const goods = this._normalizeGoods(activity.goods, activity)
            this.setData({
                goodsList: goods,
                isEmpty: goods.length === 0
            })
            return Promise.resolve()
        }

        // 2. 回退：按 activityId 拉商品
        this.setData({ loading: true })
        const params = {
            pageNum: 1,
            pageSize: 50
        }
        if (activity.id != null) {
            params.activityId = String(activity.id)
        }
        return getSeckillList(params)
            .then((res) => {
                const data = (res && res.data) || {}
                const list = Array.isArray(data.list) ? data.list : []
                const goods = this._normalizeGoods(list, activity)
                this.setData({
                    goodsList: goods,
                    loading: false,
                    isEmpty: goods.length === 0
                })
            })
            .catch(() => {
                this.setData({
                    goodsList: [],
                    loading: false,
                    isEmpty: true
                })
                wx.showToast({ title: '商品加载失败', icon: 'none' })
            })
    },

    /**
     * 规范化商品列表，附加渲染字段
     * @param {Array} list 后端秒杀商品 VO 列表
     * @param {object} [activity] 所属场次（提供 startTime/endTime）
     * @returns {Array} 渲染用列表
     */
    _normalizeGoods(list, activity) {
        if (!Array.isArray(list)) return []
        return list.map((item) => {
            const images = Array.isArray(item.images) ? item.images : []
            const coverRaw = images.length > 0 ? images[0] : (item.coverImage || item.thumb || '')
            const cover = formatImageUrl(coverRaw)
            // 场次时间优先；商品自带 startTime/endTime 次之
            const startTime = activity && activity.startTime != null
                ? activity.startTime
                : item.startTime
            const endTime = activity && activity.endTime != null
                ? activity.endTime
                : item.endTime
            return {
                seckillId: String(item.id != null ? item.id : (item.seckillId || '')),
                productName: item.productName || item.title || '',
                coverImage: cover,
                seckillPrice: item.seckillPrice != null ? item.seckillPrice : (item.price || 0),
                originalPrice: item.originalPrice != null ? item.originalPrice : 0,
                stock: item.stock != null ? item.stock : (item.totalStock || 0),
                startTime: startTime,
                endTime: endTime,
                // 渲染态（由倒计时组件 statuschange 回填）
                _status: 'BEFORE',
                _btnText: BTN_TEXT_MAP.BEFORE,
                _btnDisabled: true
            }
        })
    },

    // ========== 事件处理 ==========

    /**
     * 场次 tab 切换
     */
    onActivityTap(e) {
        const index = Number(e.currentTarget.dataset.index)
        if (Number.isNaN(index) || index === this.data.currentActivityIndex) return
        this.setData({ currentActivityIndex: index, goodsList: [], isEmpty: false })
        this._loadActivityGoods(index)
    },

    /**
     * 倒计时状态变化：更新对应商品的按钮文案 + 禁用态
     */
    onCountdownStatusChange(e) {
        const index = Number(e.currentTarget.dataset.index)
        const status = (e.detail && e.detail.status) || 'BEFORE'
        if (Number.isNaN(index) || index < 0 || index >= this.data.goodsList.length) return

        const key = 'goodsList[' + index + ']'
        const patch = {}
        patch[key + '._status'] = status
        patch[key + '._btnText'] = BTN_TEXT_MAP[status] || '立即抢购'
        // RUNNING 可抢；BEFORE/ENDED 禁用
        patch[key + '._btnDisabled'] = status !== 'RUNNING'
        this.setData(patch)
    },

    /**
     * 点击商品卡片：跳转秒杀详情
     */
    onGoodsTap(e) {
        const seckillId = e.currentTarget.dataset.seckillId
        if (!seckillId) return
        const url = '/pages/seckill-detail/seckill-detail?seckillId=' +
            encodeURIComponent(String(seckillId))
        wx.navigateTo({ url })
    },

    /**
     * 点击抢购按钮：等同点击卡片（详情页处理抢购流程）
     */
    onBuyTap(e) {
        const seckillId = e.currentTarget.dataset.seckillId
        if (!seckillId) return
        const url = '/pages/seckill-detail/seckill-detail?seckillId=' +
            encodeURIComponent(String(seckillId))
        wx.navigateTo({ url })
    },

    /**
     * 点击重试
     */
    onTapRetry() {
        this._initPage()
    }
})