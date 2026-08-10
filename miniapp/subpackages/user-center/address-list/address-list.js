// subpackages/user-center/address-list/address-list.js
// 地址列表：展示地址 + 设默认 + 左滑删除 + 新增/编辑跳转 + 可被 checkout 选择
//
// 对齐：
//   - design.md 2.5 节 address-list data
//   - spec.md 5.5 节（地址管理全部业务规则）
//   - tasks.md TR8
//
// 关键点：
//   1. onLoad 解析 selectMode（来自 checkout 选择模式）
//   2. getAddressList，address-card 列表
//   3. 设默认(setDefaultAddress)，左滑删除(deleteAddress)
//   4. 新增按钮跳 address-edit，编辑跳 address-edit?id=xxx
//   5. 可被 checkout 选择：selectMode=1 时点击地址 navigateBack 携带地址（通过全局数据通道）

const {
    getAddressList,
    deleteAddress,
    setDefaultAddress
} = require('../../../api/address')
const { isLoggedIn, navigateToLogin } = require('../../../utils/auth')

// 全局事件通道：选择地址后通过 app.globalData 传递给 checkout
const ADDRESS_SELECT_KEY = '__checkoutSelectedAddress__'

Page({
    data: {
        // 地址列表
        addresses: [],
        // 是否选择模式（来自 checkout）
        selectMode: false,
        // 当前选中地址 ID（选择模式下高亮）
        selectedId: '',
        // 加载态
        loading: false,
        // 空状态
        isEmpty: false
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
        this.setData({
            selectMode: opts.selectMode === '1' || opts.selectMode === 1
        })
    },

    onShow() {
        // 每次显示刷新（编辑/新增返回后需刷新）
        if (isLoggedIn()) {
            this._loadAddresses()
        }
    },

    /**
     * 加载地址列表
     */
    _loadAddresses() {
        this.setData({ loading: true })
        getAddressList()
            .then((res) => {
                const list = (res && res.data) || []
                this.setData({
                    addresses: Array.isArray(list) ? list : [],
                    loading: false,
                    isEmpty: !list || list.length === 0
                })
            })
            .catch(() => {
                this.setData({ loading: false, isEmpty: true })
            })
    },

    /**
     * 点击地址卡片
     *   - selectMode=1：选择并返回（携带地址）
     *   - 否则：跳编辑
     */
    onTapAddress(e) {
        const { id } = e.detail || {}
        if (!id) return
        if (this.data.selectMode) {
            this._selectAndBack(id)
        } else {
            this._goEdit(id)
        }
    },

    /**
     * 选择地址并返回（checkout 选择模式）
     */
    _selectAndBack(id) {
        const address = this.data.addresses.find((a) => String(a.id) === String(id))
        if (!address) return
        // 通过全局数据通道传递
        const app = getApp()
        if (app && app.globalData) {
            app.globalData[ADDRESS_SELECT_KEY] = address
        }
        wx.navigateBack({ delta: 1 })
    },

    /**
     * 设默认地址
     */
    onSetDefault(e) {
        const { id } = e.detail || {}
        if (!id) return
        setDefaultAddress(id)
            .then(() => {
                wx.showToast({ title: '已设为默认', icon: 'success' })
                this._loadAddresses()
            })
            .catch(() => { })
    },

    /**
     * 编辑地址
     */
    onEditAddress(e) {
        const { id } = e.detail || {}
        if (!id) return
        this._goEdit(id)
    },

    /**
     * 跳编辑页
     */
    _goEdit(id) {
        const url = '/subpackages/user-center/address-edit/address-edit?id=' +
            encodeURIComponent(id)
        wx.navigateTo({ url })
    },

    /**
     * 新增地址
     */
    onAddNew() {
        wx.navigateTo({
            url: '/subpackages/user-center/address-edit/address-edit'
        })
    },

    /**
     * 左滑删除地址
     */
    onDeleteAddress(e) {
        const { index } = e.currentTarget.dataset
        const addresses = this.data.addresses
        const address = addresses[index]
        if (!address) return

        wx.showModal({
            title: '提示',
            content: '确认删除该地址？',
            success: (res) => {
                if (!res.confirm) return
                // 乐观删除
                const newList = addresses.slice(0, index).concat(addresses.slice(index + 1))
                this.setData({
                    addresses: newList,
                    isEmpty: newList.length === 0
                })

                deleteAddress(address.id).catch(() => {
                    // 失败回滚
                    this._loadAddresses()
                })
            }
        })
    }
})