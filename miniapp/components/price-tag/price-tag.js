// components/price-tag/price-tag.js
// 价格标签组件：显示当前价格，可选原价划线
Component({
    properties: {
        // 当前价格（数字或字符串，组件内格式化为两位小数）
        price: {
            type: null,
            value: 0
        },
        // 原价（可选，显示为划线）
        originalPrice: {
            type: null,
            value: null
        },
        // 尺寸：sm | md | lg
        size: {
            type: String,
            value: 'md'
        },
        // 货币符号
        symbol: {
            type: String,
            value: '¥'
        },
        // 是否高亮主色（秒杀价用）
        highlight: {
            type: Boolean,
            value: true
        }
    },
    data: {
        formattedPrice: '0.00',
        formattedOriginal: ''
    },
    observers: {
        'price, originalPrice': function (price, originalPrice) {
            this.setData({
                formattedPrice: this._format(price)
            })
            if (originalPrice != null && originalPrice !== '') {
                this.setData({
                    formattedOriginal: this._format(originalPrice)
                })
            } else {
                this.setData({
                    formattedOriginal: ''
                })
            }
        }
    },
    methods: {
        /** 格式化两位小数 */
        _format(n) {
            const num = typeof n === 'string' ? parseFloat(n) : Number(n)
            if (Number.isNaN(num)) return '0.00'
            return num.toFixed(2)
        }
    }
})