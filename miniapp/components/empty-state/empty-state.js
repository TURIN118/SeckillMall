// components/empty-state/empty-state.js
// 封装 van-empty，统一空状态展示
Component({
    properties: {
        // 描述文案
        description: {
            type: String,
            value: '暂无数据'
        },
        // 图片地址（默认使用 van-empty 内置图标）
        image: {
            type: String,
            value: ''
        },
        // 图标类型（van-empty 内置：default/error/network/search，自定义 image 时无效）
        imageType: {
            type: String,
            value: 'default'
        }
    }
})