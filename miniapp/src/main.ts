import { createSSRApp } from 'vue'
import App from './App.vue'
import uviewPlus from 'uview-plus'
import pinia from './stores'

// uni-app Vue3 入口（对齐 plan.md 第 1.3 节）
// createSSRApp 适配 uni-app 跨端，注册 uView Plus 与 Pinia
export function createApp() {
  const app = createSSRApp(App)
  app.use(uviewPlus)
  app.use(pinia)
  return { app }
}