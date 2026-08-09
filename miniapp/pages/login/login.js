// pages/login/login.js — 登录页
//
// 对齐 spec 5.1：
//   5.1.1 业务规则：
//     1. username 3-32
//     2. password 6-64 可打印 ASCII
//     3. 失败≥3 次强制验证码
//     4. 成功：存双令牌 + 用户信息 + 跳转 redirect 或首页
//     5. 失败：failCount++，≥3 显示验证码
//     6. 跳转前必须先存令牌
//
// 对齐 design 2.2 页面 data 结构。

const { login } = require('../../api/auth.js')
const { setToken, setUserInfo } = require('../../utils/auth')
const { isNotEmpty } = require('../../utils/validate')

// 失败次数阈值：达到该值后强制显示图形验证码
const FAIL_COUNT_THRESHOLD = 3

Page({
  data: {
    username: '',
    password: '',
    captchaKey: '',
    captchaCode: '',
    captchaImage: '',
    failCount: 0,
    showCaptcha: false,
    redirect: '',
    submitting: false
  },

  onLoad(options) {
    // 解析回跳目标
    const redirect = (options && options.redirect) || ''
    this.setData({ redirect })
  },

  // ========== 表单输入绑定 ==========
  onUsernameInput(e) {
    this.setData({ username: e.detail })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail })
  },

  onCaptchaCodeInput(e) {
    this.setData({ captchaCode: e.detail })
  },

  // 验证码组件更新回调
  onCaptchaUpdate(e) {
    const { captchaKey, image } = e.detail || {}
    this.setData({
      captchaKey: captchaKey || '',
      captchaImage: image || ''
    })
  },

  // ========== 前端校验 ==========
  /**
   * 登录前前端校验
   * @returns {{valid: boolean, msg: string}}
   */
  validate() {
    const { username, password, showCaptcha, captchaCode } = this.data

    if (!isNotEmpty(username)) {
      return { valid: false, msg: '用户名不能为空' }
    }
    if (username.length < 3 || username.length > 32) {
      return { valid: false, msg: '用户名长度需在 3-32 之间' }
    }

    if (!isNotEmpty(password)) {
      return { valid: false, msg: '密码不能为空' }
    }
    if (password.length < 6 || password.length > 64) {
      return { valid: false, msg: '密码长度需在 6-64 之间' }
    }
    // 仅允许可打印 ASCII（对齐后端 LoginRequest @Pattern）
    if (!/^[\x20-\x7E]+$/.test(password)) {
      return { valid: false, msg: '密码仅允许可打印 ASCII 字符' }
    }

    // 失败≥3 次时验证码必填
    if (showCaptcha && !isNotEmpty(captchaCode)) {
      return { valid: false, msg: '请输入图形验证码' }
    }

    return { valid: true, msg: '' }
  },

  // ========== 提交登录 ==========
  handleLogin() {
    const check = this.validate()
    if (!check.valid) {
      wx.showToast({ title: check.msg, icon: 'none' })
      return
    }

    const { username, password, showCaptcha, captchaKey, captchaCode } = this.data

    // 组装请求体：showCaptcha 时带验证码字段
    const reqBody = { username, password }
    if (showCaptcha) {
      reqBody.captchaKey = captchaKey
      reqBody.captchaCode = captchaCode
    }

    this.setData({ submitting: true })
    login(reqBody)
      .then((res) => {
        const data = (res && res.data) || {}
        const accessToken = data.accessToken
        const refreshToken = data.refreshToken
        const user = data.user || {}

        // 规则 6：跳转前必须先持久化令牌与用户信息
        if (accessToken && refreshToken) {
          setToken(accessToken, refreshToken)
          setUserInfo(user)
        }

        wx.showToast({ title: '登录成功', icon: 'success' })

        // 跳转：有 redirect 用 redirectTo 回原页，否则 switchTab 首页
        const redirect = this.data.redirect
        if (redirect) {
          wx.redirectTo({
            url: redirect,
            fail: () => {
              // redirect 失败回退到首页 tab
              wx.switchTab({ url: '/pages/home/home' })
            }
          })
        } else {
          wx.switchTab({ url: '/pages/home/home' })
        }
      })
      .catch(() => {
        // 登录失败：累加 failCount，≥3 显示验证码
        const failCount = this.data.failCount + 1
        const shouldShowCaptcha = failCount >= FAIL_COUNT_THRESHOLD
        const patch = { failCount, submitting: false }

        if (shouldShowCaptcha && !this.data.showCaptcha) {
          patch.showCaptcha = true
        }
        this.setData(patch)

        // 验证码已显示时，失败后刷新验证码图片
        if (shouldShowCaptcha) {
          this.refreshCaptcha()
        }
      })
  },

  /**
   * 刷新验证码图片
   * 通过选择组件实例调用其 refresh 方法
   */
  refreshCaptcha() {
    const captchaComp = this.selectComponent('.login-page__captcha captcha-image')
    if (captchaComp && typeof captchaComp.refresh === 'function') {
      captchaComp.refresh()
    }
  }
})