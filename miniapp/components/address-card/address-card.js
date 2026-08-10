// components/address-card/address-card.js
// 地址卡片组件：展示收货人/电话/地址，isDefault 标记，可选中
//
// 对齐：
//   - design.md 2.6 节 address-card
//   - spec.md 5.5 节（地址管理）
//   - tasks.md TR2

const { maskPhone } = require('../../utils/format')

Component({
    properties: {
        // 地址对象 UserAddressVO
        // { id, receiver, phone, province, city, district, detailAddress, isDefault }
        address: {
            type: Object,
            value: null
        },
        // 是否可选择（结算页/地址列表选择模式）
        selectable: {
            type: Boolean,
            value: false
        },
        // 是否处于选中态（selectable=true 时生效）
        selected: {
            type: Boolean,
            value: false
        },
        // 是否展示设默认按钮（地址列表中展示，结算页不展示）
        showSetDefault: {
            type: Boolean,
            value: false
        }
    },
    data: {
        // 脱敏后的电话
        maskedPhone: '',
        // 完整地址串
        fullAddress: ''
    },
    observers: {
        'address': function (address) {
            if (!address) {
                this.setData({ maskedPhone: '', fullAddress: '' })
                return
            }
            const province = address.province || ''
            const city = address.city || ''
            const district = address.district || ''
            const detail = address.detailAddress || ''
            this.setData({
                maskedPhone: maskPhone(address.phone || ''),
                fullAddress: province + city + district + detail
            })
        }
    },
    methods: {
        /** 点击卡片：triggerEvent('tap', { id }) */
        onTap() {
            const address = this.data.address
            if (!address || address.id == null) return
            this.triggerEvent('tap', { id: String(address.id) })
        },
        /** 选择地址：triggerEvent('select', { address })，仅在 selectable=true 时触发 */
        onSelect() {
            if (!this.data.selectable) return
            const address = this.data.address
            if (!address) return
            this.triggerEvent('select', { address: address })
        },
        /** 设默认：triggerEvent('setdefault', { id }) */
        onSetDefault() {
            const address = this.data.address
            if (!address || address.id == null) return
            this.triggerEvent('setdefault', { id: String(address.id) })
        },
        /** 编辑：triggerEvent('edit', { id }) */
        onEdit() {
            const address = this.data.address
            if (!address || address.id == null) return
            this.triggerEvent('edit', { id: String(address.id) })
        }
    }
})