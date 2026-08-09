// subpackages/user-center/change-contact/change-contact.js
// 修改手机号/邮箱（query.type 区分）
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.5 节
//   - .codeartsdoer/specs/usercenter/design.md 9 节
//   - .codeartsdoer/specs/usercenter/tasks.md U6
//
// 关键点：
//   1. query.type 区分 phone / email
//   2. 输入新手机号/邮箱 + 验证码
//   3. 验证码：调 auth.sendForgotCode（type=PHONE/EMAIL, account=value）+ 60s 倒计时
//   4. 提交：phone 调 updatePhone(value, code)；email 调 updateEmail(value, code)
//   5. 成功后更新本地 userInfo 并返回

const authApi = require('../../../api/auth')
const userApi = require('../../../api/user')
const { isLoggedIn, navigateToLogin, getUserInfo, setUserInfo } = require('../../../utils/auth')

// 倒计时总秒数
const COUNTDOWN_TOTAL = 60

// 手机号正则（11 位 1 开头）
const PHONE_REGEX = /^1\d{10}$/
// 邮箱正则（基础校验）
const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

Page({
  data: {
    // 类型：phone / email
    type: 'phone',
    // 类型展示文案
    typeText: '手机号',
    // 输入值
    value: '',
    // 验证码
    code: '',
    // 倒计时剩余秒数（0 表示可发送）
    countdown: 0,
    // 发送中
    sendingCode: false,
    // 提交中
    submitting: false
  },

  onLoad(options) {
    if (!isLoggedIn()) {
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      const redirect = cur ? '/' + cur.route : ''
      navigateToLogin(redirect)
      return
    }

    const opts = options || {}
    const type = opts.type === 'email' ? 'email' : 'phone'
    this.setData({
      type: type,
      typeText: type === 'email' ? '邮箱' : '手机号'
    })
  },

  onUnload() {
    // 清理倒计时定时器
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },

  /**
   * 输入绑定
   */
  onValueInput(e) {
    this.setData({ value: e.detail })
  },

  onCodeInput(e) {
    this.setData({ code: e.detail })
  },

  /**
   * 校验 value 格式
   * @returns {{valid: boolean, msg: string}}
   */
  _validateValue() {
    const { type, value } = this.data
    if (!value) {
      return { valid: false, msg: '请输入' + this.data.typeText }
    }
    if (type === 'phone' && !PHONE_REGEX.test(value)) {
      return { valid: false, msg: '手机号格式不正确' }
    }
    if (type === 'email' && !EMAIL_REGEX.test(value)) {
      return { valid: false, msg: '邮箱格式不正确' }
    }
    return { valid: true, msg: '' }
  },

  /**
   * 发送验证码
   */
  onSendCode() {
    if (this.data.countdown > 0 || this.data.sendingCode) return

    const check = this._validateValue()
    if (!check.valid) {
      wx.showToast({ title: check.msg, icon: 'none' })
      return
    }

    const { type, value } = this.data
    // 复用 auth 模块的 sendForgotCode（type=PHONE/EMAIL, account=value）
    const reqType = type === 'phone' ? 'PHONE' : 'EMAIL'
    this.setData({ sendingCode: true })
    authApi.sendForgotCode({ type: reqType, account: value })
      .then(() => {
        this.setData({ sendingCode: false })
        wx.showToast({ title: '验证码已发送', icon: 'success' })
        this._startCountdown()
      })
      .catch(() => {
        this.setData({ sendingCode: false })
        // request 拦截器已 toast 错误
      })
  },

  /**
   * 启动 60s 倒计时
   */
  _startCountdown() {
    this.setData({ countdown: COUNTDOWN_TOTAL })
    if (this._timer) clearInterval(this._timer)
    this._timer = setInterval(() => {
      const left = this.data.countdown - 1
      if (left <= 0) {
        clearInterval(this._timer)
        this._timer = null
        this.setData({ countdown: 0 })
      } else {
        this.setData({ countdown: left })
      }
    }, 1000)
  },

  /**
   * 提交修改
   */
  onSubmit() {
    const check = this._validateValue()
    if (!check.valid) {
      wx.showToast({ title: check.msg, icon: 'none' })
      return
    }
    if (!this.data.code) {
      wx.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }

    const { type, value, code } = this.data
    this.setData({ submitting: true })

    const promise = type === 'phone'
      ? userApi.updatePhone(value, code)
      : userApi.updateEmail(value, code)

    promise
      .then(() => {
        // 更新本地 userInfo
        const info = Object.assign({}, getUserInfo() || {})
        if (type === 'phone') info.phone = value
        else info.email = value
        setUserInfo(info)

        this.setData({ submitting: false })
        wx.showToast({ title: '修改成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack({ delta: 1 })
        }, 600)
      })
      .catch(() => {
        this.setData({ submitting: false })
        // request 拦截器已 toast 错误
      })
  }
})