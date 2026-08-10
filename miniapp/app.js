// app.js — 小程序入口
// infra 模块仅负责基础设施，业务模块在 onLaunch 中预加载用户信息。
App({
    globalData: {
        userInfo: null
    },
    onLaunch() {
        // 预加载本地缓存的用户信息到 globalData，便于页面快速消费
        // 对齐 spec 5.5：登录态依据本地 access_token 判定，userInfo 仅作展示缓存
        try {
            const { getUserInfo } = require('./utils/auth')
            const info = getUserInfo()
            if (info) {
                this.globalData.userInfo = info
            }
        } catch (e) {
            // require 异常或 storage 异常均静默处理，不阻塞启动
        }
    }
})
