// pages/forgot-password/forgot-password.js — 找回密码页
//
// 对齐 spec 5.3：
//   5.3.1 业务规则：
//     1. type ∈ {PHONE, EMAIL}
//     2. account 格式按 type 校验
//     3. 发送验证码 60s 倒计时，倒计时中按钮禁用
//     4. newPassword 6-20
//     5. 重置成功提示并跳登录页
//     6. 验证码为空不可提交重置
//
// 对齐 design 2.2 页面 data 结构。

const { sendForgotCode, resetPassword } = require('../../api/auth.js')
const { isPhone, isEmail, isNotEmpty } = require('../../utils/validate')

// 倒计时总时长（秒）
const COUNTDOWN_TOTAL = 60

Page({
    data: {
        type: 'PHONE',
        account: '',
        code: '',
        newPassword: '',
        countdown: 0,
        sending: false,
        submitting: false
    },

    // 倒计时定时器引用
    _timer: null,

    onUnload() {
        // 页面卸载时清除倒计时，避免内存泄漏
        this.clearTimer()
    },

    // ========== 表单输入绑定 ==========
    onTypeChange(e) {
        const type = e.detail || 'PHONE'
        this.setData({ type, account: '' })
    },

    onAccountInput(e) {
        this.setData({ account: e.detail })
    },

    onCodeInput(e) {
        this.setData({ code: e.detail })
    },

    onNewPasswordInput(e) {
        this.setData({ newPassword: e.detail })
    },

    // ========== 倒计时 ==========
    startCountdown() {
        this.clearTimer()
        this.setData({ countdown: COUNTDOWN_TOTAL })
        this._timer = setInterval(() => {
            const next = this.data.countdown - 1
            if (next <= 0) {
                this.clearTimer()
                this.setData({ countdown: 0 })
            } else {
                this.setData({ countdown: next })
            }
        }, 1000)
    },

    clearTimer() {
        if (this._timer) {
            clearInterval(this._timer)
            this._timer = null
        }
    },

    // ========== 账号格式校验 ==========
    /**
     * 校验账号格式是否符合当前验证方式
     * @returns {{valid: boolean, msg: string}}
     */
    validateAccount() {
        const { type, account } = this.data
        if (!isNotEmpty(account)) {
            return { valid: false, msg: '账号不能为空' }
        }
        if (type === 'PHONE') {
            if (!isPhone(account)) {
                return { valid: false, msg: '手机号格式不正确' }
            }
        } else if (type === 'EMAIL') {
            if (!isEmail(account)) {
                return { valid: false, msg: '邮箱格式不正确' }
            }
        } else {
            return { valid: false, msg: '验证方式只能为 PHONE 或 EMAIL' }
        }
        return { valid: true, msg: '' }
    },

    // ========== 发送验证码 ==========
    handleSendCode() {
        // 倒计时中或发送中直接忽略
        if (this.data.countdown > 0 || this.data.sending) return

        const check = this.validateAccount()
        if (!check.valid) {
            wx.showToast({ title: check.msg, icon: 'none' })
            return
        }

        const { type, account } = this.data
        this.setData({ sending: true })
        sendForgotCode({ type, account })
            .then(() => {
                this.setData({ sending: false })
                wx.showToast({
                    title: '验证码已发送',
                    icon: 'success',
                    duration: 1500
                })
                // 启动 60s 倒计时
                this.startCountdown()
            })
            .catch(() => {
                this.setData({ sending: false })
            })
    },

    // ========== 提交重置 ==========
    handleReset() {
        const accountCheck = this.validateAccount()
        if (!accountCheck.valid) {
            wx.showToast({ title: accountCheck.msg, icon: 'none' })
            return
        }

        const { code, newPassword } = this.data

        if (!isNotEmpty(code)) {
            wx.showToast({ title: '请输入验证码', icon: 'none' })
            return
        }

        if (!isNotEmpty(newPassword)) {
            wx.showToast({ title: '新密码不能为空', icon: 'none' })
            return
        }
        if (newPassword.length < 6 || newPassword.length > 20) {
            wx.showToast({ title: '新密码长度需在 6-20 之间', icon: 'none' })
            return
        }

        const { type, account } = this.data
        const reqBody = { type, account, code, newPassword }

        this.setData({ submitting: true })
        resetPassword(reqBody)
            .then(() => {
                this.clearTimer()
                wx.showToast({
                    title: '密码重置成功',
                    icon: 'success',
                    duration: 1500
                })
                setTimeout(() => {
                    wx.redirectTo({ url: '/pages/login/login' })
                }, 1500)
            })
            .catch(() => {
                this.setData({ submitting: false })
            })
    }
})