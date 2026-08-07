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

dayjs.locale('zh-cn')

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
