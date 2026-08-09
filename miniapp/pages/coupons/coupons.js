// pages/coupons/coupons.js
// 优惠券页：可领取 + 我的（tab 切换）
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.2 节
//   - .codeartsdoer/specs/usercenter/design.md 6 节
//   - .codeartsdoer/specs/usercenter/tasks.md U3
//
// 关键点：
//   1. onLoad 检查登录态，加载可领取列表
//   2. van-tabs 两个 tab：可领取 / 我的
//   3. 可领取：listAvailable + 领取按钮 receive
//   4. 我的：listMine + 状态筛选（UNUSED/USED/EXPIRED）
//   5. 领取成功 toast，失败 toast 错误

const couponApi = require('../../api/coupon')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')
const { formatPrice, formatDate } = require('../../utils/format')

// 我的优惠券状态筛选 tab
const MINE_STATUS_TABS = [
  { name: '未使用', value: 'UNUSED' },
  { name: '已使用', value: 'USED' },
  { name: '已过期', value: 'EXPIRED' }
]

Page({
  data: {
    // 当前激活 tab：0=可领取, 1=我的
    activeTab: 0,
    // 可领取列表
    availableList: [],
    availableLoading: false,
    availableEmpty: false,
    // 我的优惠券
    mineList: [],
    mineLoading: false,
    mineEmpty: false,
    // 我的状态筛选 tab
    mineStatusTabs: MINE_STATUS_TABS,
    mineStatusActive: 0,
    mineStatus: 'UNUSED',
    // 是否已加载过"我的"（首次切换时加载）
    mineLoaded: false
  },

  onLoad() {
    if (!isLoggedIn()) {
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      const redirect = cur ? '/' + cur.route : ''
      navigateToLogin(redirect)
      return
    }
    this._loadAvailable()
  },

  /**
   * 切换 tab
   */
  onTabChange(e) {
    const index = e.detail.index
    this.setData({ activeTab: index })
    if (index === 1 && !this.data.mineLoaded) {
      this._loadMine()
    }
  },

  /**
   * 切换"我的"状态筛选
   */
  onMineStatusChange(e) {
    const { index } = e.currentTarget.dataset
    const tab = this.data.mineStatusTabs[index] || { value: 'UNUSED' }
    this.setData({
      mineStatusActive: Number(index),
      mineStatus: tab.value
    })
    this._loadMine()
  },

  /**
   * 加载可领取列表
   */
  _loadAvailable() {
    this.setData({ availableLoading: true, availableEmpty: false })
    couponApi.listAvailable()
      .then((res) => {
        const list = (res && res.data) || []
        const arr = Array.isArray(list) ? list : []
        this.setData({
          availableList: arr.map(this._formatAvailable),
          availableLoading: false,
          availableEmpty: arr.length === 0
        })
      })
      .catch(() => {
        this.setData({
          availableLoading: false,
          availableEmpty: true
        })
      })
  },

  /**
   * 加载我的优惠券
   */
  _loadMine() {
    this.setData({ mineLoading: true, mineEmpty: false })
    couponApi.listMine(this.data.mineStatus)
      .then((res) => {
        const list = (res && res.data) || []
        const arr = Array.isArray(list) ? list : []
        this.setData({
          mineList: arr.map(this._formatMine),
          mineLoading: false,
          mineEmpty: arr.length === 0,
          mineLoaded: true
        })
      })
      .catch(() => {
        this.setData({
          mineLoading: false,
          mineEmpty: true,
          mineLoaded: true
        })
      })
  },

  /**
   * 格式化可领取优惠券展示字段
   */
  _formatAvailable(item) {
    if (!item) return item
    return Object.assign({}, item, {
      _faceValue: formatPrice(item.faceValue || item.amount || 0),
      _minSpend: formatPrice(item.minSpend || item.threshold || 0),
      _validStart: formatDate(item.validStart || item.startTime, 'YYYY-MM-DD'),
      _validEnd: formatDate(item.validEnd || item.endTime, 'YYYY-MM-DD')
    })
  },

  /**
   * 格式化我的优惠券展示字段
   */
  _formatMine(item) {
    if (!item) return item
    return Object.assign({}, item, {
      _faceValue: formatPrice(item.faceValue || item.amount || 0),
      _minSpend: formatPrice(item.minSpend || item.threshold || 0),
      _validStart: formatDate(item.validStart || item.startTime, 'YYYY-MM-DD'),
      _validEnd: formatDate(item.validEnd || item.endTime, 'YYYY-MM-DD')
    })
  },

  /**
   * 领取优惠券
   */
  onReceive(e) {
    const { id } = e.currentTarget.dataset
    if (!id) return
    couponApi.receive(id)
      .then(() => {
        wx.showToast({ title: '领取成功', icon: 'success' })
        // 从可领取列表移除
        const list = this.data.availableList.filter((it) => String(it.id) !== String(id))
        this.setData({
          availableList: list,
          availableEmpty: list.length === 0
        })
      })
      .catch(() => {
        // request 拦截器已 toast 错误信息
      })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    const tasks = [this._loadAvailable()]
    if (this.data.activeTab === 1 || this.data.mineLoaded) {
      tasks.push(this._loadMine())
    }
    Promise.all(tasks).finally(() => {
      wx.stopPullDownRefresh()
    })
  }
})