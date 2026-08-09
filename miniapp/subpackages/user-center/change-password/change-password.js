// subpackages/user-center/change-password/change-password.js
// 修改密码：原密码 + 新密码 + 确认密码
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.4 节
//   - .codeartsdoer/specs/usercenter/design.md 8 节
//   - .codeartsdoer/specs/usercenter/tasks.md U5
//
// 关键点：
//   1. 表单：原密码/新密码/确认密码
//   2. 前端校验：非空、新密码 ≥8 位含字母+数字、两次一致
//   3. changePassword 提交，成功提示重新登录

const authApi = require('../../../api/auth')
const { isLoggedIn, navigateToLogin } = require('../../../utils/auth')

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    submitting: false
  },

  onLoad() {
    if (!isLoggedIn()) {
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      const redirect = cur ? '/' + cur.route : ''
      navigateToLogin(redirect)
    }
  },

  /**
   * 输入绑定
   */
  onOldPasswordInput(e) {
    this.setData({ oldPassword: e.detail })
  },

  onNewPasswordInput(e) {
    this.setData({ newPassword: e.detail })
  },

  onConfirmPasswordInput(e) {
    this.setData({ confirmPassword: e.detail })
  },

  /**
   * 前端校验
   * @returns {{valid: boolean, msg: string}}
   */
  _validate() {
    const { oldPassword, newPassword, confirmPassword } = this.data

    if (!oldPassword) {
      return { valid: false, msg: '请输入原密码' }
    }
    if (!newPassword) {
      return { valid: false, msg: '请输入新密码' }
    }
    // 新密码 ≥8 位含字母+数字
    if (newPassword.length < 8) {
      return { valid: false, msg: '新密码至少 8 位' }
    }
    if (!/[a-zA-Z]/.test(newPassword) || !/\d/.test(newPassword)) {
      return { valid: false, msg: '新密码需同时包含字母和数字' }
    }
    // 两次一致
    if (newPassword !== confirmPassword) {
      return { valid: false, msg: '两次输入的密码不一致' }
    }
    // 新旧不能相同
    if (newPassword === oldPassword) {
      return { valid: false, msg: '新密码不能与原密码相同' }
    }
    return { valid: true, msg: '' }
  },

  /**
   * 提交修改密码
   */
  onSubmit() {
    const check = this._validate()
    if (!check.valid) {
      wx.showToast({ title: check.msg, icon: 'none' })
      return
    }

    const { oldPassword, newPassword } = this.data
    this.setData({ submitting: true })
    authApi.changePassword({ oldPassword: oldPassword, newPassword: newPassword })
      .then(() => {
        this.setData({ submitting: false })
        wx.showToast({ title: '修改成功', icon: 'success' })
        // 提示建议重新登录
        setTimeout(() => {
          wx.showModal({
            title: '提示',
            content: '密码已修改，建议重新登录以确保安全',
            showCancel: false,
            confirmText: '重新登录',
            success: () => {
              // 清 token 并跳登录页
              const { clearToken } = require('../../../utils/auth')
              clearToken()
              wx.reLaunch({ url: '/pages/login/login' })
            }
          })
        }, 600)
      })
      .catch(() => {
        this.setData({ submitting: false })
        // request 拦截器已 toast 错误
      })
  }
})