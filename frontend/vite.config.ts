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
        // API 接口代理：将 /api 开头的请求转发到后端
        '/api': {
          // 后端地址从 VITE_PROXY_TARGET 读取，默认 http://127.0.0.1:8080（避免 IPv6 优先解析延迟）
          target: env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,   // 修改请求头中的 Host 为后端地址，避免后端 Host 校验失败
          secure: false,        // 后端为 HTTP 时设为 false；HTTPS 自签名证书时也设为 false
          ws: true,             // 支持 WebSocket 代理（为后续秒杀结果推送预留）
          configure: (proxy) => {
            // 设置代理超时，避免连接异常时长时间阻塞
            proxy.on('proxyReq', (proxyReq, req, res) => {
              // 设置代理请求超时为30秒
              proxyReq.setTimeout(30000, () => {
                proxyReq.destroy()
              })
            })
          }
        },
        // 图片/上传文件代理：将 /images 和 /upload 开头的请求也转发到后端
        '/images': {
          target: env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,
          secure: false,
          ws: false             // 静态资源不需要 WebSocket
        },
        '/upload': {
          target: env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,
          secure: false,
          ws: false             // 上传路径不需要 WebSocket
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