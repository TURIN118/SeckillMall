import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

import viteCompression from 'vite-plugin-compression'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [
      vue(),
      AutoImport({
        // ElementPlusResolver 自动按需引入 Element Plus 的 API (如 ElMessage, ElMessageBox)
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts',
        eslintrc: { enabled: false }
      }),
      Components({
        // ElementPlusResolver 自动按需引入 Element Plus 组件及其样式
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts'
      }),
      // gzip 压缩: 对 > 10KB 的资源生成 .gz 文件, 配合 nginx 静态 gzip 进一步减小传输体积
      viteCompression({
        verbose: true,
        disable: false,
        threshold: 10240,
        algorithm: 'gzip',
        ext: '.gz'
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
      // terser 压缩: 移除 console 和 debugger, 减小生产包体积
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true
        }
      },
      // 不计算 gzip 压缩大小报告, 加快构建速度 (由 vite-plugin-compression 生成实际 .gz 文件)
      reportCompressedSize: false,
      rollupOptions: {
        output: {
          // 代码分割: 将第三方大库分离为独立 chunk, 减少业务代码 chunk 体积, 提升缓存命中率
          manualChunks: {
            // Vue 核心 (vue + vue-router + pinia)
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            // wangEditor 富文本编辑器 (仅商品编辑使用, 按需加载)
            'wangeditor': ['@wangeditor/editor', '@wangeditor/editor-for-vue'],
            // ECharts 图表库 (仅后台 Dashboard 使用, 按需引入后分离为独立 chunk)
            // - 减小 Dashboard chunk 体积, 首次进入后台时 Dashboard 加载更快
            // - echarts chunk 独立缓存, 其他页面若使用图表可直接命中缓存
            'echarts': ['echarts']
          }
          // 注意: element-plus 不再配置 manualChunks,
          // 因为已改为按需引入, 强制合并会抵消按需引入的体积优化效果.
          // - element-plus: 各组件由 ElementPlusResolver 自动按需引入, rollup 自动分割
        }
      }
    }
  }
})