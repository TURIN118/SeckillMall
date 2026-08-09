// pages/order-detail/order-detail.js
// 订单详情：展示地址/商品/金额/状态/操作按钮
//
// 对齐：
//   - design.md 2.5 节 order-detail data
//   - spec.md 5.3 节（订单管理全部业务规则）
//   - tasks.md TR6
//
// 关键点：
//   1. onLoad 解析 id + orderType
//   2. orderType=SECKILL 走 getOrderDetail，NORMAL 走 getNormalOrderDetail
//   3. 操作按钮：
//      - PENDING_PAY: 支付(秒杀 payOrder/普通 payNormalOrder) + 取消(秒杀 cancelOrder/普通 cancelNormalOrder)
//      - PENDING_RECEIVE: 确认收货(秒杀 confirmOrder/普通 confirmNormalOrder)
//   4. 操作后刷新详情

const {
  getOrderDetail,
  getNormalOrderDetail,
  payOrder,
  payNormalOrder,
  cancelOrder,
  cancelNormalOrder,
  confirmOrder,
  confirmNormalOrder
} = require('../../api/order')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')
const { formatPrice, formatDate, maskPhone } = require('../../utils/format')

// 状态文案
const STATUS_TEXT = {
  PENDING_PAY: '待付款',
  PENDING_SHIP: '待发货',
  PENDING_RECEIVE: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

Page({
  data: {
    // 订单 ID
    orderId: '',
    // 订单类型 SECKILL/NORMAL
    orderType: 'NORMAL',
    // 是否普通订单
    isNormal: false,
    // 订单对象
    order: null,
    // 派生展示字段
    statusText: '',
    formattedAmount: '0.00',
    formattedTime: '',
    maskedPhone: '',
    fullAddress: '',
    // 商品明细
    items: [],
    // 状态
    loading: false,
    submitting: false,
    loadError: false,
    // 操作按钮可见性
    canPay: false,
    canCancel: false,
    canConfirm: false
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
    const id = opts.id ? decodeURIComponent(opts.id) : ''
    const orderType = opts.orderType ? decodeURIComponent(opts.orderType) : 'NORMAL'

    if (!id) {
      this.setData({ loadError: true })
      wx.showToast({ title: '订单 ID 缺失', icon: 'none' })
      return
    }

    this.setData({
      orderId: id,
      orderType: orderType,
      isNormal: orderType === 'NORMAL'
    })

    this._loadDetail()
  },

  /**
   * 加载订单详情（按 orderType 分发）
   */
  _loadDetail() {
    this.setData({ loading: true, loadError: false })
    const id = this.data.orderId
    const fetcher = this.data.isNormal ? getNormalOrderDetail : getOrderDetail

    fetcher(id)
      .then((res) => {
        const order = (res && res.data) || null
        if (!order) {
          this.setData({ loading: false, loadError: true })
          return
        }
        this._hydrate(order)
      })
      .catch(() => {
        this.setData({ loading: false, loadError: true })
        wx.showToast({ title: '加载失败', icon: 'none' })
      })
  },

  /**
   * 填充派生字段
   */
  _hydrate(order) {
    const status = order.status || ''
    const items = Array.isArray(order.items) ? order.items : []
    const address = order.address || order.userAddress || null

    let fullAddress = ''
    let maskedPhone = ''
    if (address) {
      const province = address.province || ''
      const city = address.city || ''
      const district = address.district || ''
      const detail = address.detailAddress || ''
      fullAddress = province + city + district + detail
      maskedPhone = maskPhone(address.phone || '')
    }

    this.setData({
      order: order,
      statusText: STATUS_TEXT[status] || status,
      formattedAmount: formatPrice(order.totalAmount || order.payAmount || 0),
      formattedTime: formatDate(order.createTime, 'YYYY-MM-DD HH:mm'),
      maskedPhone: maskedPhone,
      fullAddress: fullAddress,
      items: items,
      loading: false,
      // 操作按钮可见性
      canPay: status === 'PENDING_PAY',
      canCancel: status === 'PENDING_PAY',
      canConfirm: status === 'PENDING_RECEIVE'
    })
  },

  // ========== 操作 ==========

  /**
   * 支付
   */
  onPay() {
    if (this.data.submitting) return
    this.setData({ submitting: true })
    const id = this.data.orderId
    const fetcher = this.data.isNormal ? payNormalOrder : payOrder

    fetcher(id, 'ALIPAY')
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' })
        this._loadDetail()
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ submitting: false })
      })
  },

  /**
   * 取消订单
   */
  onCancel() {
    if (this.data.submitting) return
    wx.showModal({
      title: '提示',
      content: '确认取消该订单？',
      success: (res) => {
        if (!res.confirm) return
        this._doCancel()
      }
    })
  },

  _doCancel() {
    this.setData({ submitting: true })
    const id = this.data.orderId
    const fetcher = this.data.isNormal ? cancelNormalOrder : cancelOrder

    fetcher(id)
      .then(() => {
        wx.showToast({ title: '已取消', icon: 'success' })
        this._loadDetail()
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ submitting: false })
      })
  },

  /**
   * 确认收货
   */
  onConfirm() {
    if (this.data.submitting) return
    wx.showModal({
      title: '提示',
      content: '确认已收到商品？',
      success: (res) => {
        if (!res.confirm) return
        this._doConfirm()
      }
    })
  },

  _doConfirm() {
    this.setData({ submitting: true })
    const id = this.data.orderId
    const fetcher = this.data.isNormal ? confirmNormalOrder : confirmOrder

    fetcher(id)
      .then(() => {
        wx.showToast({ title: '已确认收货', icon: 'success' })
        this._loadDetail()
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ submitting: false })
      })
  },

  /**
   * 点击重试
   */
  onTapRetry() {
    this._loadDetail()
  },

  /**
   * 返回
   */
  onGoBack() {
    wx.navigateBack({ delta: 1 })
  }
})