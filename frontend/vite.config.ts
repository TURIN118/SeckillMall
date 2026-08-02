import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts',
        eslintrc: { enabled: false }
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts'
      })
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    css: {
      preprocessorOptions: {}
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      open: false,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      target: 'es2015',
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      // CSS 代码分割: 每个异步 chunk 的 CSS 单独提取, 减少首屏 CSS 体积
      cssCodeSplit: true,
      rollupOptions: {
        output: {
          // 代码分割: 将第三方大库分离为独立 chunk, 减少业务代码 chunk 体积, 提升缓存命中率
          manualChunks: {
            // Vue 核心 (vue + vue-router + pinia)
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            // Element Plus UI 库 + 图标
            'element-plus': ['element-plus', '@element-plus/icons-vue'],
            // ECharts 图表库 (仅数据看台使用, 按需加载)
            'echarts': ['echarts'],
            // wangEditor 富文本编辑器 (仅商品编辑使用, 按需加载)
            'wangeditor': ['@wangeditor/editor', '@wangeditor/editor-for-vue']
          }
        }
      }
    }
  }
})