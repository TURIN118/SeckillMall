// subpackages/user-center/address-edit/address-edit.js
// 地址编辑/新增：表单 + 校验 + 提交
//
// 对齐：
//   - design.md 2.5 节 address-edit data
//   - spec.md 5.5 节（地址管理全部业务规则）
//   - tasks.md TR8
//
// 关键点：
//   1. 表单：收货人 van-field / 手机号 van-field / 省市区 van-area / 详细地址 van-field / 设默认 van-switch
//   2. 校验：收货人非空 / 手机号 isPhone / 省市区非空 / 详细地址非空
//   3. 新增走 createAddress，编辑回填走 updateAddress(onLoad 解析 id)
//   4. 提交成功 navigateBack

const { getAddressList, createAddress, updateAddress } = require('../../../api/address')
const { isLoggedIn, navigateToLogin } = require('../../../utils/auth')
const { isPhone, isNotEmpty } = require('../../../utils/validate')
const { areaList } = require('../../../utils/area-data')

Page({
    data: {
        // 是否编辑模式
        editing: false,
        // 地址 ID（编辑模式）
        id: '',
        // 表单数据
        form: {
            receiver: '',
            phone: '',
            province: '',
            city: '',
            district: '',
            detailAddress: '',
            isDefault: false
        },
        // 省市区显示串
        areaText: '',
        // van-area 弹层显示
        showArea: false,
        // 省市区数据（van-area area-list 属性）
        areaList: areaList,
        // 提交中
        submitting: false,
        // 校验错误信息
        errors: {
            receiver: '',
            phone: '',
            area: '',
            detailAddress: ''
        }
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
        const id = opts.id ? decodeURIComponent(opts.id) : ''
        if (id) {
            this.setData({ editing: true, id: id })
            this._loadAddressForEdit(id)
        }
    },

    /**
     * 加载地址详情用于回填
     * 后端未提供单条地址查询接口，从 list 中过滤
     */
    _loadAddressForEdit(id) {
        getAddressList()
            .then((res) => {
                const list = (res && res.data) || []
                const address = (Array.isArray(list) ? list : []).find(
                    (a) => String(a.id) === String(id)
                )
                if (!address) {
                    wx.showToast({ title: '地址不存在', icon: 'none' })
                    return
                }
                this.setData({
                    form: {
                        receiver: address.receiver || '',
                        phone: address.phone || '',
                        province: address.province || '',
                        city: address.city || '',
                        district: address.district || '',
                        detailAddress: address.detailAddress || '',
                        isDefault: !!address.isDefault
                    },
                    areaText: (address.province || '') + (address.city || '') + (address.district || '')
                })
            })
            .catch(() => { })
    },

    // ========== 表单事件 ==========

    /**
     * 收货人输入
     */
    onReceiverInput(e) {
        this.setData({ 'form.receiver': e.detail.value || '' })
    },

    /**
     * 手机号输入
     */
    onPhoneInput(e) {
        this.setData({ 'form.phone': e.detail.value || '' })
    },

    /**
     * 详细地址输入
     */
    onDetailInput(e) {
        this.setData({ 'form.detailAddress': e.detail.value || '' })
    },

    /**
     * 设默认切换
     */
    onDefaultChange(e) {
        this.setData({ 'form.isDefault': !!e.detail })
    },

    /**
     * 显示省市区选择
     */
    onShowArea() {
        this.setData({ showArea: true })
    },

    /**
     * 省市区确认
     * van-area confirm 事件返回 { values: [{code,name}, ...] }
     */
    onAreaConfirm(e) {
        const values = (e.detail && e.detail.values) || []
        const province = values[0] ? values[0].name : ''
        const city = values[1] ? values[1].name : ''
        const district = values[2] ? values[2].name : ''
        this.setData({
            'form.province': province,
            'form.city': city,
            'form.district': district,
            areaText: province + city + district,
            showArea: false
        })
    },

    /**
     * 省市区取消
     */
    onAreaCancel() {
        this.setData({ showArea: false })
    },

    // ========== 校验 ==========

    /**
     * 校验表单
     * @returns {boolean} 是否通过
     */
    _validate() {
        const form = this.data.form
        const errors = {
            receiver: '',
            phone: '',
            area: '',
            detailAddress: ''
        }
        let valid = true

        if (!isNotEmpty(form.receiver)) {
            errors.receiver = '请输入收货人'
            valid = false
        }
        if (!isPhone(form.phone)) {
            errors.phone = '请输入正确的手机号'
            valid = false
        }
        if (!isNotEmpty(form.province) || !isNotEmpty(form.city)) {
            errors.area = '请选择省市区'
            valid = false
        }
        if (!isNotEmpty(form.detailAddress)) {
            errors.detailAddress = '请输入详细地址'
            valid = false
        }

        this.setData({ errors: errors })
        return valid
    },

    // ========== 提交 ==========

    /**
     * 保存地址
     */
    onSave() {
        if (this.data.submitting) return
        if (!this._validate()) {
            // 显示第一个错误
            const errs = this.data.errors
            const firstErr = errs.receiver || errs.phone || errs.area || errs.detailAddress
            if (firstErr) {
                wx.showToast({ title: firstErr, icon: 'none' })
            }
            return
        }

        this.setData({ submitting: true })
        const payload = {
            receiver: this.data.form.receiver,
            phone: this.data.form.phone,
            province: this.data.form.province,
            city: this.data.form.city,
            district: this.data.form.district,
            detailAddress: this.data.form.detailAddress,
            isDefault: this.data.form.isDefault
        }

        const action = this.data.editing
            ? updateAddress(this.data.id, payload)
            : createAddress(payload)

        action
            .then(() => {
                wx.showToast({ title: '保存成功', icon: 'success' })
                setTimeout(() => {
                    wx.navigateBack({ delta: 1 })
                }, 800)
            })
            .catch(() => { })
            .finally(() => {
                this.setData({ submitting: false })
            })
    },

    /**
     * 取消
     */
    onCancel() {
        wx.navigateBack({ delta: 1 })
    }
})