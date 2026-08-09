import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app Vite 配置
// 环境变量采用 UNI_ 前缀（对齐 plan.md 第 7 章），通过 define 注入 process.env
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'UNI_')
  return {
    plugins: [uni()],
    define: {
      'process.env.UNI_API_BASE_URL': JSON.stringify(env.UNI_API_BASE_URL || ''),
      'process.env.UNI_API_PREFIX': JSON.stringify(env.UNI_API_PREFIX || '/api/v1'),
      'process.env.UNI_TIMEOUT': JSON.stringify(env.UNI_TIMEOUT || '10000')
    },
    css: {
      preprocessorOptions: {
        scss: {
          // uView Plus 主题变量注入（如需可在此 additionalData）
        }
      }
    }
  }
})