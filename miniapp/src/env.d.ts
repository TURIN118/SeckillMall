/// <reference types="@dcloudio/types" />

// 环境变量类型声明（对齐 plan.md 第 7 章，UNI_ 前缀通过 vite define 注入 process.env）
declare namespace NodeJS {
  interface ProcessEnv {
    UNI_API_BASE_URL: string
    UNI_API_PREFIX: string
    UNI_TIMEOUT: string
    NODE_ENV: 'development' | 'production'
  }
}

// Vue SFC 类型声明
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}