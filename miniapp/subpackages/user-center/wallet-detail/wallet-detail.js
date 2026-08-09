// subpackages/user-center/wallet-detail/wallet-detail.js
// 钱包详情：余额 + 充值 + 交易记录
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.6 节
//   - .codeartsdoer/specs/usercenter/design.md 10 节
//   - .codeartsdoer/specs/usercenter/tasks.md U7
//
// 关键点：
//   1. onLoad 并行 getBalance + listRecords
//   2. 余额卡片 + 充值按钮
//   3. 充值弹窗（卡号+密码）recharge
//   4. 交易记录列表
//   5. 充值成功后刷新余额与记录

const walletApi = require('../../../api/wallet')
const { isLoggedIn, navigateToLogin } = require('../../../utils/auth')
const { formatPrice, formatDate } = require('../../../utils/format')

// 交易类型文案映射
const TYPE_TEXT = {
  RECHARGE: '充值',
  CONSUME: '消费',
  REFUND: '退款',
  WITHDRAW: '提现'
}

Page({
  data: {
    // 余额（格式化字符串）
    balance: '0.00',
    // 余额加载中
    balanceLoading: false,
    // 交易记录
    records: [],
    recordsLoading: false,
    recordsEmpty: false,
    // 充值弹窗
    rechargeVisible: false,
    cardNo: '',
    cardPassword: '',
    recharging: false
  },

  onLoad() {
    if (!isLoggedIn()) {
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      const redirect = cur ? '/' + cur.route : ''
      navigateToLogin(redirect)
      return
    }
    this._loadBalance()
    this._loadRecords()
  },

  /**
   * 加载余额
   */
  _loadBalance() {
    this.setData({ balanceLoading: true })
    walletApi.getBalance()
      .then((res) => {
        const bal = (res && res.data) || 0
        this.setData({
          balance: formatPrice(bal),
          balanceLoading: false
        })
      })
      .catch(() => {
        this.setData({ balanceLoading: false })
      })
  },

  /**
   * 加载交易记录
   */
  _loadRecords() {
    this.setData({ recordsLoading: true, recordsEmpty: false })
    walletApi.listRecords()
      .then((res) => {
        const list = (res && res.data) || []
        const arr = Array.isArray(list) ? list : []
        const formatted = arr.map((item) => {
          if (!item) return item
          return Object.assign({}, item, {
            _amount: formatPrice(item.amount || 0),
            _time: formatDate(item.createTime || item.time, 'YYYY-MM-DD HH:mm'),
            _typeText: TYPE_TEXT[item.type] || item.type || '变动'
          })
        })
        this.setData({
          records: formatted,
          recordsLoading: false,
          recordsEmpty: formatted.length === 0
        })
      })
      .catch(() => {
        this.setData({
          recordsLoading: false,
          recordsEmpty: true
        })
      })
  },

  /**
   * 打开充值弹窗
   */
  onOpenRecharge() {
    this.setData({
      rechargeVisible: true,
      cardNo: '',
      cardPassword: ''
    })
  },

  /**
   * 关闭充值弹窗
   */
  onCloseRecharge() {
    if (this.data.recharging) return
    this.setData({ rechargeVisible: false })
  },

  /**
   * 充值卡号输入
   */
  onCardNoInput(e) {
    this.setData({ cardNo: e.detail })
  },

  /**
   * 充值卡密码输入
   */
  onCardPasswordInput(e) {
    this.setData({ cardPassword: e.detail })
  },

  /**
   * 确认充值
   */
  onConfirmRecharge() {
    const cardNo = (this.data.cardNo || '').trim()
    const cardPassword = (this.data.cardPassword || '').trim()
    if (!cardNo) {
      wx.showToast({ title: '请输入卡号', icon: 'none' })
      return
    }
    if (!cardPassword) {
      wx.showToast({ title: '请输入卡密', icon: 'none' })
      return
    }

    this.setData({ recharging: true })
    walletApi.recharge(cardNo, cardPassword)
      .then((res) => {
        // 后端可能返回新余额
        const newBal = (res && res.data) || null
        this.setData({
          recharging: false,
          rechargeVisible: false
        })
        if (newBal !== null && newBal !== undefined) {
          this.setData({ balance: formatPrice(newBal) })
        } else {
          this._loadBalance()
        }
        wx.showToast({ title: '充值成功', icon: 'success' })
        // 刷新交易记录
        this._loadRecords()
      })
      .catch(() => {
        this.setData({ recharging: false })
        // request 拦截器已 toast 错误
      })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    Promise.all([
      this._loadBalance(),
      this._loadRecords()
    ]).finally(() => {
      wx.stopPullDownRefresh()
    })
  }
})