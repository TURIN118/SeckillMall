// app.js — 小程序入口
// infra 模块仅负责基础设施，业务模块后续在 onLaunch 中按需扩展。
App({
  globalData: {
    userInfo: null
  },
  onLaunch() {
    // 占位：后续业务模块可在此初始化全局状态、检查登录态等。
    // infra 阶段不引入具体业务逻辑，保持骨架最小化。
  }
})