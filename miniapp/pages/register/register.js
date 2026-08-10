// pages/register/register.js — 注册页
//
// 对齐 spec 5.2：
//   5.2.1 业务规则：
//     1. username 4-20
//     2. password 6-20
//     3. phone ^1[3-9]\d{9}$
//     4. captchaKey/captchaCode 必填
//     5. 注册成功提示并跳登录页
//     6. 禁止自动登录
//
// 对齐 design 2.2 页面 data 结构。

const { register } = require('../../api/auth.js')
const { isPhone, isNotEmpty } = require('../../utils/validate')

Page({
    data: {
        username: '',
        password: '',
        phone: '',
        captchaKey: '',
        captchaCode: '',
        captchaImage: '',
        agreed: false,
        submitting: false
    },

    // ========== 表单输入绑定 ==========
    onUsernameInput(e) {
        this.setData({ username: e.detail })
    },

    onPasswordInput(e) {
        this.setData({ password: e.detail })
    },

    onPhoneInput(e) {
        this.setData({ phone: e.detail })
    },

    onCaptchaCodeInput(e) {
        this.setData({ captchaCode: e.detail })
    },

    onCaptchaUpdate(e) {
        const { captchaKey, image } = e.detail || {}
        this.setData({
            captchaKey: captchaKey || '',
            captchaImage: image || ''
        })
    },

    onAgreedChange(e) {
        const value = e.detail || []
        this.setData({ agreed: Array.isArray(value) && value.length > 0 })
    },

    // ========== 前端校验 ==========
    /**
     * 注册前前端校验
     * @returns {{valid: boolean, msg: string}}
     */
    validate() {
        const { username, password, phone, captchaKey, captchaCode, agreed } = this.data

        if (!isNotEmpty(username)) {
            return { valid: false, msg: '用户名不能为空' }
        }
        if (username.length < 4 || username.length > 20) {
            return { valid: false, msg: '用户名长度需在 4-20 之间' }
        }

        if (!isNotEmpty(password)) {
            return { valid: false, msg: '密码不能为空' }
        }
        if (password.length < 6 || password.length > 20) {
            return { valid: false, msg: '密码长度需在 6-20 之间' }
        }

        if (!isNotEmpty(phone)) {
            return { valid: false, msg: '手机号不能为空' }
        }
        if (!isPhone(phone)) {
            return { valid: false, msg: '手机号格式不正确' }
        }

        if (!isNotEmpty(captchaKey)) {
            return { valid: false, msg: '请先获取图形验证码' }
        }
        if (!isNotEmpty(captchaCode)) {
            return { valid: false, msg: '请输入图形验证码' }
        }

        if (!agreed) {
            return { valid: false, msg: '请先同意用户协议' }
        }

        return { valid: true, msg: '' }
    },

    // ========== 提交注册 ==========
    handleRegister() {
        const check = this.validate()
        if (!check.valid) {
            wx.showToast({ title: check.msg, icon: 'none' })
            return
        }

        const { username, password, phone, captchaKey, captchaCode } = this.data
        const reqBody = { username, password, phone, captchaKey, captchaCode }

        this.setData({ submitting: true })
        register(reqBody)
            .then(() => {
                // 规则 6：注册成功不自动登录，跳登录页
                wx.showToast({
                    title: '注册成功',
                    icon: 'success',
                    duration: 1500
                })
                setTimeout(() => {
                    wx.redirectTo({ url: '/pages/login/login' })
                }, 1500)
            })
            .catch(() => {
                // 失败后刷新验证码，避免验证码失效
                this.setData({ submitting: false, captchaCode: '' })
                this.refreshCaptcha()
            })
    },

    /**
     * 刷新验证码图片
     */
    refreshCaptcha() {
        const captchaComp = this.selectComponent('.register-page__captcha captcha-image')
        if (captchaComp && typeof captchaComp.refresh === 'function') {
            captchaComp.refresh()
        }
    }
})