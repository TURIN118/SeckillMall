// config/env.js — 环境变量配置
//
// 原生小程序无 .env 机制，通过本文件集中管理环境相关变量。
// 切换环境：将 currentEnv 改为 'prod'（发布前务必改为 prod 并配置 HTTPS 域名）。
//
// 对齐：主计划 4.6 节、design.md 2.2 节

const ENV = {
  // 开发环境：本地后端
  dev: {
    BASE_URL: 'http://localhost:8080',
    TIMEOUT: 10000,
    LOG_ENABLED: true
  },
  // 生产环境：必须 HTTPS，需在微信公众平台配置 request 合法域名
  prod: {
    BASE_URL: 'https://api.yourdomain.com',
    TIMEOUT: 10000,
    LOG_ENABLED: false
  }
}

// 当前环境：'dev' | 'prod'
// 发布前改为 'prod'，并替换 prod.BASE_URL 为实际域名
const currentEnv = 'dev'

module.exports = ENV[currentEnv]