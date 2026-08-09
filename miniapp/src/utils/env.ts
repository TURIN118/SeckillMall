/**
 * 环境变量统一导出（对齐 plan.md 第 7.3 节）
 * UNI_ 前缀变量通过 vite.config.ts define 注入 process.env
 */

export const ENV = {
  /** API 基础地址（开发期 http://localhost:8080，生产期 https://api.seckill-mall.com） */
  API_BASE_URL: process.env.UNI_API_BASE_URL || 'http://localhost:8080',
  /** API 前缀（统一 /api/v1） */
  API_PREFIX: process.env.UNI_API_PREFIX || '/api/v1',
  /** 请求超时时间（毫秒） */
  TIMEOUT: Number(process.env.UNI_TIMEOUT) || 10000
}

/** 是否开发环境 */
export const isDev = process.env.NODE_ENV === 'development'

/** 是否生产环境 */
export const isProd = process.env.NODE_ENV === 'production'