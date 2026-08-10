// components/captcha-image/captcha-image.js — 图形验证码组件
//
// 职责：
//   1. 渲染 base64 验证码图片
//   2. 点击图片调用 api/auth.js getCaptcha 刷新
//   3. 通过 triggerEvent('update', { captchaKey, image }) 通知父组件
//
// 对齐：
//   - design.md 2.2 节 components/captcha-image/
//   - spec.md 4.1 节规则 1（点击刷新，不阻塞表单其他操作）
//   - spec.md 6.5 节（CaptchaVO: { captchaKey, image }）

const { getCaptcha } = require('../../api/auth.js')

Component({
    options: {
        multipleSlots: false
    },

    properties: {
        // 验证码标识（与父组件双向绑定）
        captchaKey: {
            type: String,
            value: ''
        },
        // base64 图片
        image: {
            type: String,
            value: ''
        }
    },

    lifetimes: {
        attached() {
            // 组件挂载时若无图片，自动拉取一次
            if (!this.data.image) {
                this.refresh()
            }
        }
    },

    methods: {
        /**
         * 刷新验证码
         * 调用 getCaptcha → 更新本地 image → triggerEvent 通知父组件
         */
        refresh() {
            getCaptcha()
                .then((res) => {
                    const data = (res && res.data) || {}
                    const captchaKey = data.captchaKey || ''
                    const image = data.image || ''
                    // 更新组件自身 image 显示
                    this.setData({ image })
                    // 通知父组件更新 captchaKey 与 image
                    this.triggerEvent('update', { captchaKey, image })
                })
                .catch(() => {
                    // 网络异常时 request.js 已统一提示，此处不重复 toast
                    // 仅更新占位提示
                    this.setData({ image: '' })
                })
        }
    }
})