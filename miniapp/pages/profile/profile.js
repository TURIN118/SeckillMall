// pages/profile/profile.js
// 个人中心主页：用户卡 + 订单状态宫格 + 功能入口 + 退出登录
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.1 节
//   - .codeartsdoer/specs/usercenter/design.md 5 节
//   - .codeartsdoer/specs/usercenter/tasks.md U2
//
// 关键点：
//   1. onShow 检查登录态，已登录 auth.getMe 刷新 userInfo
//   2. 用户卡：头像 + 昵称 + 手机号脱敏；未登录点击跳登录页
//   3. 订单状态宫格：待付款/待发货/待收货/已完成 → orders?status=xxx
//   4. 功能入口列表：优惠券/收藏/钱包/地址 + 客服/帮助/关于
//   5. 退出登录：wx.showModal 确认 → auth.logout → reLaunch 登录页

const authApi = require('../../api/auth')
const { isLoggedIn, navigateToLogin, clearToken, getUserInfo, setUserInfo } = require('../../utils/auth')
const { maskPhone } = require('../../utils/format')
const { formatImageUrl } = require('../../utils/image')

// 订单状态宫格配置（对齐后端枚举：UNPAID/PAID/SHIPPED/COMPLETED）
const ORDER_STATUS_GRID = [
    { key: 'UNPAID', name: '待付款', icon: '💰' },
    { key: 'PAID', name: '待发货', icon: '📦' },
    { key: 'SHIPPED', name: '待收货', icon: '🚚' },
    { key: 'COMPLETED', name: '已完成', icon: '✅' }
]

Page({
    data: {
        // 是否已登录
        isLoggedIn: false,
        // 用户信息 { id, nickname, avatar, phone, email, ... }
        userInfo: null,
        // 手机号脱敏展示
        phoneMasked: '',
        // 订单状态宫格
        orderStatusGrid: ORDER_STATUS_GRID
    },

    onShow() {
        this._refreshLoginState()
    },

    /**
     * 刷新登录态与用户信息
     * - 未登录：清空 userInfo，展示"请登录"
     * - 已登录：先展示本地缓存，再异步 getMe 刷新
     */
    _refreshLoginState() {
        const logged = isLoggedIn()
        if (!logged) {
            this.setData({
                isLoggedIn: false,
                userInfo: null,
                phoneMasked: ''
            })
            return
        }

        // 先用本地缓存快速展示
        const cached = getUserInfo()
        this._applyUserInfo(cached, true)

        // 异步刷新（不阻塞 UI）
        authApi.getMe()
            .then((res) => {
                const info = (res && res.data) || null
                if (info) {
                    // 更新本地缓存
                    setUserInfo(info)
                    this._applyUserInfo(info, true)
                }
            })
            .catch(() => {
                // getMe 失败（如 token 过期已被 request 拦截器处理）不阻塞页面
            })
    },

    /**
     * 应用用户信息到 data
     * @param {object|null} info
     * @param {boolean} logged
     */
    _applyUserInfo(info, logged) {
        // 拼接后端 BASE_URL（avatar 为相对路径）
        const safeInfo = info ? Object.assign({}, info, { avatar: formatImageUrl(info.avatar) }) : null
        this.setData({
            isLoggedIn: !!logged,
            userInfo: safeInfo,
            phoneMasked: info && info.phone ? maskPhone(info.phone) : ''
        })
    },

    // ========== 事件处理 ==========

    /**
     * 点击用户卡：未登录跳登录页，已登录跳编辑资料
     */
    onTapUserCard() {
        if (!this.data.isLoggedIn) {
            const pages = getCurrentPages()
            const cur = pages[pages.length - 1]
            const redirect = cur ? '/' + cur.route : ''
            navigateToLogin(redirect)
            return
        }
        wx.navigateTo({
            url: '/subpackages/user-center/profile-edit/profile-edit'
        })
    },

    /**
     * 点击订单状态宫格 → 跳订单页带 status
     */
    onTapOrderStatus(e) {
        const { status } = e.currentTarget.dataset
        if (!status) return
        if (!this.data.isLoggedIn) {
            navigateToLogin('/pages/orders/orders?status=' + status)
            return
        }
        wx.switchTab({
            url: '/pages/orders/orders',
            success: () => {
                // switchTab 不能带 query，通过全局数据通道传递 status
                const app = getApp()
                if (app && app.globalData) {
                    app.globalData.__ordersStatusFilter__ = status
                }
            }
        })
    },

    /**
     * 点击功能入口
     */
    onTapFeature(e) {
        const { url, needLogin } = e.currentTarget.dataset
        if (!url) return
        if (needLogin && !this.data.isLoggedIn) {
            navigateToLogin(url)
            return
        }
        wx.navigateTo({ url })
    },

    /**
     * 客服中心：拨打电话
     */
    onTapCustomerService() {
        wx.showModal({
            title: '客服热线',
            content: '400-888-8888\n工作时间：9:00-22:00',
            confirmText: '拨打',
            success: (res) => {
                if (res.confirm) {
                    wx.makePhoneCall({ phoneNumber: '4008888888' }).catch(() => {})
                }
            }
        })
    },

    /**
     * 帮助中心
     */
    onTapHelp() {
        wx.showModal({
            title: '帮助中心',
            content: '如需帮助，请拨打客服热线 400-888-8888\n或发送邮件至 support@example.com',
            showCancel: false,
            confirmText: '知道了'
        })
    },

    /**
     * 关于我们
     */
    onTapAbout() {
        wx.showModal({
            title: '关于我们',
            content: '秒杀商城 v1.0.0\n\n基于 Spring Boot + 微信小程序原生开发\n\n© 2026 WNJ. All rights reserved.',
            showCancel: false,
            confirmText: '知道了'
        })
    },

    /**
     * 退出登录
     */
    onLogout() {
        if (!this.data.isLoggedIn) return
        wx.showModal({
            title: '提示',
            content: '确认退出登录？',
            success: (res) => {
                if (!res.confirm) return
                // 先调后端 logout（best-effort），再清本地 token，最后 reLaunch 登录页
                authApi.logout()
                    .catch(() => { })
                    .finally(() => {
                        clearToken()
                        wx.reLaunch({ url: '/pages/login/login' })
                    })
            }
        })
    }
})