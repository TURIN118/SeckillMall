/**
 * 应用入口
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'dayjs/locale/zh-cn'
// Element Plus 中文 locale，确保 el-pagination 等组件显示中文
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/global.css'
import dayjs from 'dayjs'

dayjs.locale('zh-cn')

const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
// 使用中文 locale，使 Element Plus 组件（如分页的 "Go to"、每页条数等）显示中文
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')