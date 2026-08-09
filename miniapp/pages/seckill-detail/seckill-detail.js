// pages/seckill-detail/seckill-detail.js
// 秒杀详情/抢购页：图片轮播 + 商品信息 + 倒计时 + 实时库存 + 抢购按钮 + 结果轮询
//
// 对齐：
//   - .codeartsdoer/specs/seckill/spec.md 3.2 节
//   - .codeartsdoer/specs/seckill/design.md 6 节
//   - tasks.md S4
//
// 关键点：
//   1. onLoad 并行 getSeckillDetail + getSeckillStock，启动库存轮询 setInterval 3s
//   2. 抢购流程：debounce 防连点 + loading 态 + getSeckillToken → doSeckill → PENDING 轮询 result
//   3. pollResult：最多 15 次 ×2s，SUCCESS/FAILED/超时
//   4. 1011 错误由 request.js 统一 toast，页面不刷新 token
//   5. onCountdownStatusChange 控制按钮禁用
//   6. onHide/onUnload clearInterval(stockTimer/pollTimer)
//   7. 抢购成功引导去订单中心

const {
  getSeckillDetail,
  getSeckillStock,
  getSeckillToken,
  doSeckill,
  getSeckillResult
} = require('../../api/seckill')
const { debounce } = require('../../utils/debounce')
const { isLoggedIn, navigateToLogin } = require('../../utils/auth')

// 倒计时状态 → 按钮文案
const BTN_TEXT_MAP = {
  BEFORE: '即将开抢',
  RUNNING: '立即抢购',
  ENDED: '已结束'
}

// 结果轮询参数
const POLL_MAX_TIMES = 15
const POLL_INTERVAL = 2000

// 库存轮询间隔
const STOCK_INTERVAL = 3000

Page({
  data: {
    // 秒杀 ID（string）
    seckillId: '',
    // 详情对象
    detail: null,
    // 实时库存
    stock: 0,
    // 倒计时状态：BEFORE | RUNNING | ENDED
    countdownStatus: 'BEFORE',
    // 按钮文案
    btnText: '即将开抢',
    // 按钮禁用
    btnDisabled: true,
    // 抢购中
    submitting: false,
    // 加载态
    loading: true,
    // 错误态
    loadError: false,
    // 当前页路径（登录回跳）
    currentUrl: '',
    // 展示字段
    images: [],
    productName: '',
    seckillPrice: 0,
    originalPrice: 0,
    startTime: 0,
    endTime: 0
  },

  onLoad(options) {
    const opts = options || {}
    const id = opts.seckillId ? decodeURIComponent(opts.seckillId) : ''

    if (!id) {
      this.setData({ loading: false, loadError: true })
      wx.showToast({ title: '秒杀 ID 缺失', icon: 'none' })
      return
    }

    this.setData({ seckillId: id })

    // 记录当前页路径（登录回跳）
    const pages = getCurrentPages()
    const cur = pages[pages.length - 1]
    if (cur) {
      this.setData({
        currentUrl: '/' + cur.route + '?seckillId=' + encodeURIComponent(id)
      })
    }

    // 初始化 debounce 抢购（500ms 立即执行版，防连点）
    this._debouncedSeckill = debounce(this._doSeckill.bind(this), 500, true)

    // 并行加载详情 + 库存
    this._loadDetailAndStock(id)
  },

  onShow() {
    // 页面再次显示时恢复库存轮询（若已加载过）
    if (this.data.seckillId && !this.data.loading && !this.data.loadError) {
      this._startStockTimer()
    }
  },

  onHide() {
    this._clearStockTimer()
    this._clearPollTimer()
  },

  onUnload() {
    this._clearStockTimer()
    this._clearPollTimer()
  },

  /**
   * 并行加载详情 + 库存，然后启动库存轮询
   */
  _loadDetailAndStock(id) {
    this.setData({ loading: true, loadError: false })
    Promise.all([
      getSeckillDetail(id).catch((e) => { throw e }),
      getSeckillStock(id).catch(() => null) // 库存失败不阻塞详情
    ])
      .then((results) => {
        const detailRes = results[0]
        const stockRes = results[1]
        const detail = (detailRes && detailRes.data) || null
        if (!detail) {
          this.setData({ loading: false, loadError: true })
          return
        }

        const images = Array.isArray(detail.images) ? detail.images : []
        const stock = stockRes && typeof stockRes.data === 'number'
          ? stockRes.data
          : (detail.stock != null ? detail.stock : (detail.totalStock || 0))

        this.setData({
          detail: detail,
          stock: stock,
          loading: false,
          loadError: false,
          images: images,
          productName: detail.productName || detail.title || '',
          seckillPrice: detail.seckillPrice != null ? detail.seckillPrice : (detail.price || 0),
          originalPrice: detail.originalPrice != null ? detail.originalPrice : 0,
          startTime: detail.startTime != null ? detail.startTime : 0,
          endTime: detail.endTime != null ? detail.endTime : 0
        })

        // 启动库存轮询
        this._startStockTimer()
      })
      .catch(() => {
        this.setData({ loading: false, loadError: true })
        wx.showToast({ title: '加载失败', icon: 'none' })
      })
  },

  // ========== 库存轮询 ==========

  _startStockTimer() {
    this._clearStockTimer()
    const id = this.data.seckillId
    if (!id) return
    this._stockTimer = setInterval(() => {
      getSeckillStock(id)
        .then((res) => {
          if (res && typeof res.data === 'number') {
            this.setData({ stock: res.data })
          }
        })
        .catch(() => {
          // 静默忽略，下次重试
        })
    }, STOCK_INTERVAL)
  },

  _clearStockTimer() {
    if (this._stockTimer) {
      clearInterval(this._stockTimer)
      this._stockTimer = null
    }
  },

  // ========== 结果轮询 ==========

  /**
   * 轮询秒杀结果
   * @param {string} id 秒杀 ID
   * @param {string} requestId 抢购请求 ID
   * @returns {Promise<SeckillResultVO>}
   *   - SUCCESS → resolve
   *   - FAILED → reject（_pollError=true, _pollFail=true）
   *   - 超时 → reject（_pollError=true, _pollTimeout=true）
   */
  _pollResult(id, requestId) {
    return new Promise((resolve, reject) => {
      let count = 0
      const tick = () => {
        count++
        getSeckillResult(id, requestId)
          .then((res) => {
            const data = (res && res.data) || {}
            if (data.status === 'SUCCESS') {
              resolve(data)
            } else if (data.status === 'FAILED') {
              const err = new Error(data.message || '抢购失败')
              err._pollError = true
              err._pollFail = true
              reject(err)
            } else if (count >= POLL_MAX_TIMES) {
              const err = new Error('排队超时，请稍后到订单中心查看')
              err._pollError = true
              err._pollTimeout = true
              reject(err)
            } else {
              // PENDING：继续轮询
              this._pollTimer = setTimeout(tick, POLL_INTERVAL)
            }
          })
          .catch(() => {
            // 网络错误：request.js 已 toast，不中断轮询
            if (count >= POLL_MAX_TIMES) {
              const err = new Error('排队超时，请稍后到订单中心查看')
              err._pollError = true
              err._pollTimeout = true
              reject(err)
            } else {
              this._pollTimer = setTimeout(tick, POLL_INTERVAL)
            }
          })
      }
      tick()
    })
  },

  _clearPollTimer() {
    if (this._pollTimer) {
      clearTimeout(this._pollTimer)
      this._pollTimer = null
    }
  },

  // ========== 抢购流程 ==========

  /**
   * 点击抢购按钮（debounce 防连点入口）
   */
  onSeckillTap() {
    if (!this._debouncedSeckill) return
    this._debouncedSeckill()
  },

  /**
   * 实际抢购流程
   * 1. 登录态 + 状态 + submitting 守卫
   * 2. getSeckillToken → doSeckill → PENDING 轮询 result
   * 3. 成功 modal 引导订单中心
   * 4. 1011 由 request.js 已 toast，不刷新 token
   */
  async _doSeckill() {
    // 守卫
    if (this.data.submitting) return
    if (this.data.countdownStatus !== 'RUNNING') {
      wx.showToast({ title: this.data.btnText || '暂未开抢', icon: 'none' })
      return
    }
    if (!this._checkLogin()) return

    const id = this.data.seckillId
    if (!id) return

    this.setData({ submitting: true })
    try {
      // 1. 获取一次性 token
      const tokenRes = await getSeckillToken(id)
      const token = (tokenRes && tokenRes.data) || ''

      // 2. 执行秒杀
      const res = await doSeckill(id, token)
      const result = (res && res.data) || {}

      // 3. PENDING 则轮询结果
      if (result.status === 'PENDING' && result.requestId) {
        await this._pollResult(id, String(result.requestId))
      }
      // 若 status === 'FAILED' 直接抛错
      if (result.status === 'FAILED') {
        const err = new Error(result.message || '抢购失败')
        err._pollError = true
        err._pollFail = true
        throw err
      }

      // 4. 成功
      this._showSuccessModal()
    } catch (e) {
      this._handleSeckillError(e)
    } finally {
      this.setData({ submitting: false })
    }
  },

  /**
   * 抢购成功引导
   */
  _showSuccessModal() {
    wx.showModal({
      title: '抢购成功',
      content: '已抢到！去订单中心查看？',
      confirmText: '去订单',
      cancelText: '再逛逛',
      success: (modalRes) => {
        if (modalRes.confirm) {
          wx.navigateTo({ url: '/pages/orders/orders' })
        }
      }
    })
  },

  /**
   * 抢购错误处理
   * - 1011 / 业务错误：request.js 已 toast，不重复
   * - pollResult FAILED：modal 提示失败原因
   * - pollResult 超时：modal 提示去订单中心
   */
  _handleSeckillError(e) {
    if (!e) return
    // pollResult 自身 reject 的错误
    if (e._pollError) {
      if (e._pollTimeout) {
        wx.showModal({
          title: '提示',
          content: e.message || '排队超时，请稍后到订单中心查看',
          showCancel: false,
          confirmText: '知道了'
        })
      } else {
        // FAILED
        wx.showModal({
          title: '抢购失败',
          content: e.message || '很抱歉，抢购失败',
          showCancel: false,
          confirmText: '知道了'
        })
      }
      return
    }
    // 其他错误（含 1011 防重放、1002 已由 request.js 处理、网络错误等）
    // request.js 已统一 toast，此处不重复，避免双重提示闪烁
    // 仅在 message 为空时兜底
    if (!e.message) {
      wx.showToast({ title: '抢购失败', icon: 'none' })
    }
  },

  // ========== 倒计时状态 ==========

  /**
   * 倒计时状态变化：更新按钮文案 + 禁用态
   */
  onCountdownStatusChange(e) {
    const status = (e.detail && e.detail.status) || 'BEFORE'
    this.setData({
      countdownStatus: status,
      btnText: BTN_TEXT_MAP[status] || '立即抢购',
      btnDisabled: status !== 'RUNNING'
    })
  },

  // ========== 工具方法 ==========

  /**
   * 登录态检查：未登录跳登录页
   * @returns {boolean}
   */
  _checkLogin() {
    if (isLoggedIn()) return true
    navigateToLogin(this.data.currentUrl)
    return false
  },

  /**
   * 点击重试
   */
  onTapRetry() {
    this._loadDetailAndStock(this.data.seckillId)
  },

  /**
   * 返回上一页
   */
  onGoBack() {
    wx.navigateBack({ delta: 1 })
  }
})