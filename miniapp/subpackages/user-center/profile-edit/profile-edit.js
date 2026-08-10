// subpackages/user-center/profile-edit/profile-edit.js
// 编辑资料：头像 + 昵称 + 手机号/邮箱只读 cell
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.3 节
//   - .codeartsdoer/specs/usercenter/design.md 7 节
//   - .codeartsdoer/specs/usercenter/tasks.md U4
//
// 关键点：
//   1. onLoad 加载 auth.getMe 填充昵称/头像/手机号/邮箱
//   2. 头像：wx.chooseImage + uploadAvatar
//   3. 昵称：van-field + updateProfile 保存
//   4. 手机号/邮箱只读 cell，点击跳 change-contact?type=phone/email
//   5. 保存成功更新本地 userInfo 并返回

const authApi = require('../../../api/auth')
const { isLoggedIn, navigateToLogin, getUserInfo, setUserInfo } = require('../../../utils/auth')
const { maskPhone } = require('../../../utils/format')
const { formatImageUrl } = require('../../../utils/image')

Page({
    data: {
        // 用户信息
        userInfo: null,
        // 昵称（可编辑）
        nickname: '',
        // 头像 URL
        avatarUrl: '',
        // 手机号脱敏
        phoneMasked: '',
        // 邮箱
        email: '',
        // 头像上传中
        uploading: false,
        // 保存中
        saving: false
    },

    onLoad() {
        if (!isLoggedIn()) {
            const pages = getCurrentPages()
            const cur = pages[pages.length - 1]
            const redirect = cur ? '/' + cur.route : ''
            navigateToLogin(redirect)
            return
        }
        this._loadUserInfo()
    },

    /**
     * 加载用户信息
     */
    _loadUserInfo() {
        // 先用本地缓存
        const cached = getUserInfo()
        if (cached) this._applyUserInfo(cached)

        authApi.getMe()
            .then((res) => {
                const info = (res && res.data) || null
                if (info) {
                    setUserInfo(info)
                    this._applyUserInfo(info)
                }
            })
            .catch(() => { })
    },

    /**
     * 应用用户信息到 data
     */
    _applyUserInfo(info) {
        this.setData({
            userInfo: info,
            nickname: info.nickname || '',
            avatarUrl: formatImageUrl(info.avatar || ''),
            phoneMasked: info.phone ? maskPhone(info.phone) : '',
            email: info.email || ''
        })
    },

    /**
     * 昵称输入
     */
    onNicknameInput(e) {
        this.setData({ nickname: e.detail })
    },

    /**
     * 选择并上传头像
     */
    onChooseAvatar() {
        if (this.data.uploading) return
        wx.chooseImage({
            count: 1,
            sizeType: ['compressed'],
            sourceType: ['album', 'camera'],
            success: (res) => {
                const filePath = (res.tempFilePaths && res.tempFilePaths[0]) || ''
                if (!filePath) return
                this._uploadAvatar(filePath)
            },
            fail: () => {
                // 用户取消，静默处理
            }
        })
    },

    /**
     * 上传头像
     */
    _uploadAvatar(filePath) {
        this.setData({ uploading: true })
        authApi.uploadAvatar(filePath)
            .then((avatarUrl) => {
                this.setData({
                    avatarUrl: formatImageUrl(avatarUrl || this.data.avatarUrl),
                    uploading: false
                })
                wx.showToast({ title: '头像已更新', icon: 'success' })
                // 同步更新本地缓存的 userInfo
                const info = Object.assign({}, this.data.userInfo || {}, { avatar: avatarUrl })
                setUserInfo(info)
                this.setData({ userInfo: info })
            })
            .catch(() => {
                this.setData({ uploading: false })
                // request 拦截器已 toast 错误
            })
    },

    /**
     * 保存昵称
     */
    onSave() {
        const nickname = (this.data.nickname || '').trim()
        if (!nickname) {
            wx.showToast({ title: '昵称不能为空', icon: 'none' })
            return
        }
        if (nickname.length > 32) {
            wx.showToast({ title: '昵称最多 32 个字符', icon: 'none' })
            return
        }

        // 昵称未变化直接返回
        if (this.data.userInfo && this.data.userInfo.nickname === nickname) {
            wx.navigateBack({ delta: 1 })
            return
        }

        this.setData({ saving: true })
        authApi.updateProfile({ nickname: nickname })
            .then((res) => {
                // 后端可能返回更新后的 UserVO，优先用；否则本地拼
                const newInfo = (res && res.data) || Object.assign({}, this.data.userInfo || {}, { nickname: nickname })
                setUserInfo(newInfo)
                this.setData({ userInfo: newInfo, saving: false })
                wx.showToast({ title: '保存成功', icon: 'success' })
                setTimeout(() => {
                    wx.navigateBack({ delta: 1 })
                }, 600)
            })
            .catch(() => {
                this.setData({ saving: false })
            })
    },

    /**
     * 跳转修改手机号
     */
    onTapPhone() {
        wx.navigateTo({
            url: '/subpackages/user-center/change-contact/change-contact?type=phone'
        })
    },

    /**
     * 跳转修改邮箱
     */
    onTapEmail() {
        wx.navigateTo({
            url: '/subpackages/user-center/change-contact/change-contact?type=email'
        })
    }
})