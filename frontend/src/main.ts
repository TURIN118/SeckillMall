/**
 * 应用入口
 *
 * Element Plus 按需引入说明:
 *   - 组件由 unplugin-vue-components 的 ElementPlusResolver 自动按需引入 (见 vite.config.ts)
 *   - 组件样式同样由 ElementPlusResolver 自动按需引入, 无需手动 import 'element-plus/dist/index.css'
 *   - 图标由各组件单独 import (如 `import { Picture } from '@element-plus/icons-vue'`)
 *   - 中文 locale 通过 App.vue 根组件的 <el-config-provider> 注入
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'dayjs/locale/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/global.css'
import dayjs from 'dayjs'

dayjs.locale('zh-cn')

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
