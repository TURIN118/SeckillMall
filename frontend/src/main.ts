/**
 * 应用入口
 *
 * Element Plus 全量引入说明:
 *   - 全量引入所有组件及样式, 避免按需引入时部分样式缺失
 *   - 图标由各组件单独 import (如 `import { Picture } from '@element-plus/icons-vue'`)
 *   - 中文 locale 通过 App.vue 根组件的 <el-config-provider> 注入
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'dayjs/locale/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/global.css'
import dayjs from 'dayjs'

// T11 前端埋点 SDK: v-track 指令 + tracker 初始化
import { vTrack } from './directives/track'
import { initTracker } from './utils/tracker'

dayjs.locale('zh-cn')

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册全局埋点指令 v-track
app.directive('track', vTrack)
// 初始化埋点定时上报 (5s 批量 + beforeunload 兜底)
initTracker()

app.mount('#app')
