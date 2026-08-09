# plan.md — uni-app 端微信小程序技术方案

> **项目名称**：秒杀商城（Seckill Mall）— uni-app 端微信小程序
> **文档类型**：技术方案（How to build）
> **版本**：v1.0
> **撰写日期**：2026-08-09
> **目标平台**：微信小程序（uni-app 编译目标 `mp-weixin`）
> **技术栈**：uni-app + Vue 3 + TypeScript + uView Plus + Pinia
> **用途**：定义"怎么做"，作为架构决策与技术实现的依据。不含功能验收清单（验收见 `spec.md`）。
> **内容来源**：《uni-app 端微信小程序开发计划》第 4 章目录架构 + 第 6 章请求封装 + 第 7.1 节风险矩阵

---

## 目录

- [第 1 章：技术栈与版本](#第-1-章技术栈与版本)
- [第 2 章：目录架构设计](#第-2-章目录架构设计)
- [第 3 章：路由与分包规划](#第-3-章路由与分包规划)
- [第 4 章：请求封装技术方案](#第-4-章请求封装技术方案)
- [第 5 章：状态管理方案](#第-5-章状态管理方案)
- [第 6 章：全局样式与主题](#第-6-章全局样式与主题)
- [第 7 章：环境变量方案](#第-7-章环境变量方案)
- [第 8 章：风险预案](#第-8-章风险预案)

---

## 第 1 章：技术栈与版本

### 1.1 技术栈锁定

| 技术 | 用途 | 版本建议 | 说明 |
|------|------|----------|------|
| uni-app | 跨端框架 | 最新稳定版（Vue 3 模板） | 编译目标 `mp-weixin` |
| Vue 3 | 视图框架 | 3.4.x+ | Composition API + `<script setup>` |
| TypeScript | 类型系统 | 5.x | 全程类型安全，ID 显式 string |
| uView Plus | 组件库 | 锁定精确版本号 | 替代 Element Plus |
| Pinia | 状态管理 | 2.x+ | stores 设计见第 5 章 |
| sass | CSS 预处理器 | 最新稳定版 | uni.scss 主题变量 |
| Vite | 构建工具 | 5.x+ | uni-app Vue3 模板默认 |

### 1.2 依赖安装命令

```bash
# uView Plus 组件库
npm install uview-plus

# Pinia 状态管理
npm install pinia

# sass 预处理器
npm install -D sass sass-loader

# uni-app 核心（CLI 方式需要）
npm install @dcloudio/uni-app @dcloudio/uni-components
npm install @dcloudio/uni-mp-weixin --save-dev
```

### 1.3 main.ts 配置

```typescript
import { createSSRApp } from 'vue'
import App from './App.vue'
import uviewPlus from 'uview-plus'

export function createApp() {
  const app = createSSRApp(App)
  app.use(uviewPlus)
  return { app }
}
```

### 1.4 App.vue 入口

```vue
<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
})
onShow(() => {
  console.log('App Show')
})
onHide(() => {
  console.log('App Hide')
})
</script>
```

---

## 第 2 章：目录架构设计

### 2.1 完整目录树

```
miniapp/
├── src/
│   ├── api/                          # API 接口封装
│   │   ├── request.ts                # 请求核心封装（uni.request + 拦截器）
│   │   ├── auth.ts                   # 认证接口
│   │   ├── product.ts                # 商品接口
│   │   ├── category.ts               # 分类接口
│   │   ├── cart.ts                   # 购物车接口
│   │   ├── order.ts                  # 订单接口
│   │   ├── seckill.ts                # 秒杀接口
│   │   ├── favorite.ts               # 收藏接口
│   │   ├── address.ts                # 收货地址接口
│   │   ├── coupon.ts                 # 优惠券接口
│   │   ├── wallet.ts                 # 钱包接口
│   │   ├── banner.ts                 # 轮播图接口
│   │   ├── review.ts                 # 评价接口
│   │   └── upload.ts                 # 上传接口
│   ├── stores/                       # Pinia 状态管理
│   │   ├── index.ts                  # Pinia 实例
│   │   ├── user.ts                   # 用户状态（token/userInfo/login/logout/refresh）
│   │   ├── cart.ts                   # 购物车状态
│   │   ├── category.ts              # 分类状态
│   │   ├── seckill.ts               # 秒杀状态
│   │   └── app.ts                    # 全局应用状态（timeOffset/serverTime 等）
│   ├── utils/                        # 工具函数
│   │   ├── tokenStorage.ts           # Token 存储封装（uni.setStorageSync）
│   │   ├── jwt.ts                    # JWT 解析（base64 解码 + payload 提取）
│   │   ├── timeSync.ts              # 服务器时间同步（timeOffset 计算）
│   │   ├── replayProtection.ts       # 秒杀防重放头构建
│   │   ├── snowflakeId.ts           # 雪花 ID 处理（string + encodeURIComponent）
│   │   ├── navigate.ts              # 路由跳转封装（tabBar 与非 tabBar 区分）
│   │   ├── validator.ts             # 表单校验工具
│   │   ├── format.ts                # 格式化工具（价格/时间/数字）
│   │   ├── env.ts                   # 环境变量统一导出
│   │   └── toast.ts                  # 提示封装（uni.showToast/showModal 统一）
│   ├── types/                        # TypeScript 类型定义
│   │   ├── api.ts                    # API 响应类型（Result<T> 等）
│   │   ├── user.ts                   # 用户类型
│   │   ├── product.ts                # 商品类型
│   │   ├── cart.ts                   # 购物车类型
│   │   ├── order.ts                  # 订单类型
│   │   ├── seckill.ts               # 秒杀类型
│   │   ├── address.ts               # 地址类型
│   │   ├── coupon.ts                # 优惠券类型
│   │   └── common.ts                # 通用类型（分页/枚举等）
│   ├── components/                   # 公共组件
│   │   ├── NavBar/                   # 自定义导航栏
│   │   ├── ProductCard/             # 商品卡片
│   │   ├── EmptyState/              # 空状态
│   │   ├── LoadMore/                # 加载更多
│   │   ├── PriceTag/                # 价格标签
│   │   ├── CountdownTimer/          # 倒计时组件
│   │   ├── CaptchaInput/            # 图形验证码输入
│   │   ├── AddressSelector/         # 地址选择器
│   │   ├── SkuSelector/             # SKU 规格选择器
│   │   └── RichTextRenderer/        # 富文本渲染组件（rich-text 封装）
│   ├── pages/                        # 主包页面
│   │   ├── home/                     # 首页
│   │   │   └── home.vue
│   │   ├── category/                 # 分类 tab
│   │   │   └── category.vue
│   │   ├── cart/                     # 购物车 tab
│   │   │   └── cart.vue
│   │   ├── profile/                  # 我的 tab
│   │   │   └── profile.vue
│   │   ├── login/                    # 登录
│   │   │   └── login.vue
│   │   ├── register/                 # 注册
│   │   │   └── register.vue
│   │   └── forgot-password/         # 找回密码
│   │       └── forgot-password.vue
│   ├── pages-product/               # 商品分包
│   │   └── pages/
│   │       ├── product-list/
│   │       │   └── product-list.vue
│   │       └── product-detail/
│   │           └── product-detail.vue
│   ├── pages-order/                 # 订单分包
│   │   └── pages/
│   │       ├── checkout/
│   │       │   └── checkout.vue
│   │       ├── order-list/
│   │       │   └── order-list.vue
│   │       └── order-detail/
│   │           └── order-detail.vue
│   ├── pages-seckill/              # 秒杀分包
│   │   └── pages/
│   │       └── seckill-zone/
│   │           └── seckill-zone.vue
│   ├── pages-user/                 # 用户分包
│   │   └── pages/
│   │       ├── favorites/
│   │       │   └── favorites.vue
│   │       ├── user-profile/
│   │       │   └── user-profile.vue
│   │       ├── my-coupons/
│   │       │   └── my-coupons.vue
│   │       ├── address-list/
│   │       │   └── address-list.vue
│   │       └── address-edit/
│   │           └── address-edit.vue
│   ├── static/                      # 静态资源
│   │   ├── images/                  # 图片
│   │   ├── icons/                   # 图标
│   │   └── tabbar/                  # tabBar 图标
│   ├── App.vue                      # 应用入口
│   ├── main.ts                      # 主入口
│   ├── manifest.json                # 应用配置
│   ├── pages.json                   # 页面路由配置
│   └── uni.scss                     # 全局样式变量
├── package.json
├── tsconfig.json
├── vite.config.ts
└── env/                             # 环境变量
    ├── .env.development
    └── .env.production
```

### 2.2 目录用途说明

| 目录 | 用途 |
|------|------|
| `api/` | API 接口封装，request.ts 为核心，其余按业务模块拆分 |
| `stores/` | Pinia 状态管理，按业务域拆分（user/cart/category/seckill/app） |
| `utils/` | 工具函数，含 tokenStorage/jwt/timeSync/replayProtection/snowflakeId/navigate/toast 等 |
| `types/` | TypeScript 类型定义，所有 ID 字段显式 string |
| `components/` | 公共组件，含 NavBar/ProductCard/CountdownTimer/CaptchaInput/SkuSelector/RichTextRenderer 等 |
| `pages/` | 主包页面（首页/分类/购物车/我的/登录/注册/找回密码） |
| `pages-product/` | 商品分包（商品列表/商品详情） |
| `pages-order/` | 订单分包（结算/订单列表/订单详情） |
| `pages-seckill/` | 秒杀分包（秒杀专区） |
| `pages-user/` | 用户分包（收藏/个人资料/优惠券/地址列表/地址编辑） |
| `static/` | 静态资源（图片/图标/tabBar 图标） |
| `env/` | 环境变量文件 |

---

## 第 3 章：路由与分包规划

### 3.1 设计原则

1. **主包控制 2MB 以内**：主包仅放高频访问页面（首页、分类、购物车、我的、登录、注册、找回密码）。
2. **分包按业务域划分**：商品、订单、秒杀、用户中心各为独立分包。
3. **tabBar 4 个入口**：首页、分类、购物车、我的。
4. **预加载策略**：首页加载时预下载商品和秒杀分包，"我的"页加载时预下载用户和订单分包。

### 3.2 pages.json 完整配置

```json
{
  "pages": [
    {
      "path": "pages/home/home",
      "style": {
        "navigationBarTitleText": "秒杀商城",
        "enablePullDownRefresh": true,
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/category/category",
      "style": {
        "navigationBarTitleText": "分类",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/cart/cart",
      "style": {
        "navigationBarTitleText": "购物车",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/profile/profile",
      "style": {
        "navigationBarTitleText": "我的",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/login/login",
      "style": {
        "navigationBarTitleText": "登录"
      }
    },
    {
      "path": "pages/register/register",
      "style": {
        "navigationBarTitleText": "注册"
      }
    },
    {
      "path": "pages/forgot-password/forgot-password",
      "style": {
        "navigationBarTitleText": "找回密码"
      }
    }
  ],
  "subPackages": [
    {
      "root": "pages-product",
      "pages": [
        {
          "path": "pages/product-list/product-list",
          "style": {
            "navigationBarTitleText": "商品列表",
            "enablePullDownRefresh": true,
            "navigationStyle": "custom"
          }
        },
        {
          "path": "pages/product-detail/product-detail",
          "style": {
            "navigationBarTitleText": "商品详情",
            "navigationStyle": "custom"
          }
        }
      ]
    },
    {
      "root": "pages-order",
      "pages": [
        {
          "path": "pages/checkout/checkout",
          "style": { "navigationBarTitleText": "结算" }
        },
        {
          "path": "pages/order-list/order-list",
          "style": {
            "navigationBarTitleText": "我的订单",
            "enablePullDownRefresh": true
          }
        },
        {
          "path": "pages/order-detail/order-detail",
          "style": { "navigationBarTitleText": "订单详情" }
        }
      ]
    },
    {
      "root": "pages-seckill",
      "pages": [
        {
          "path": "pages/seckill-zone/seckill-zone",
          "style": {
            "navigationBarTitleText": "秒杀专区",
            "navigationStyle": "custom"
          }
        }
      ]
    },
    {
      "root": "pages-user",
      "pages": [
        {
          "path": "pages/favorites/favorites",
          "style": { "navigationBarTitleText": "我的收藏" }
        },
        {
          "path": "pages/user-profile/user-profile",
          "style": { "navigationBarTitleText": "个人资料" }
        },
        {
          "path": "pages/my-coupons/my-coupons",
          "style": { "navigationBarTitleText": "我的优惠券" }
        },
        {
          "path": "pages/address-list/address-list",
          "style": { "navigationBarTitleText": "收货地址" }
        },
        {
          "path": "pages/address-edit/address-edit",
          "style": { "navigationBarTitleText": "编辑地址" }
        }
      ]
    }
  ],
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#FF4D4F",
    "backgroundColor": "#FFFFFF",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/home/home",
        "text": "首页",
        "iconPath": "static/tabbar/home.png",
        "selectedIconPath": "static/tabbar/home-active.png"
      },
      {
        "pagePath": "pages/category/category",
        "text": "分类",
        "iconPath": "static/tabbar/category.png",
        "selectedIconPath": "static/tabbar/category-active.png"
      },
      {
        "pagePath": "pages/cart/cart",
        "text": "购物车",
        "iconPath": "static/tabbar/cart.png",
        "selectedIconPath": "static/tabbar/cart-active.png"
      },
      {
        "pagePath": "pages/profile/profile",
        "text": "我的",
        "iconPath": "static/tabbar/profile.png",
        "selectedIconPath": "static/tabbar/profile-active.png"
      }
    ]
  },
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "秒杀商城",
    "navigationBarBackgroundColor": "#FFFFFF",
    "backgroundColor": "#F8F8F8"
  },
  "preloadRule": {
    "pages/home/home": {
      "network": "all",
      "packages": ["pages-product", "pages-seckill"]
    },
    "pages/profile/profile": {
      "network": "all",
      "packages": ["pages-user", "pages-order"]
    }
  }
}
```

### 3.3 manifest.json 微信小程序配置

```json
{
  "name": "seckill-miniapp",
  "appid": "",
  "description": "秒杀商城微信小程序",
  "versionName": "1.0.0",
  "versionCode": "100",
  "transformPx": false,
  "app-plus": {},
  "mp-weixin": {
    "appid": "wxXXXXXXXXXXXXXXX",
    "setting": {
      "urlCheck": false,
      "es6": true,
      "enhance": true,
      "postcss": true,
      "preloadBackgroundData": false,
      "minified": true,
      "newFeature": false,
      "coverView": true,
      "nodeModules": false,
      "autoAudits": false,
      "showShadowRootDuringWSSPreview": false,
      "scopeDataUGC": false,
      "uglifyFileName": false,
      "checkInvalidKey": true,
      "checkSiteMap": true,
      "uploadWithSourceMap": true,
      "compileWorklet": false,
      "useMultiFrameRuntime": true,
      "useApiHook": true,
      "useApiHostProcess": true,
      "babelSetting": {
        "ignore": [],
        "disablePlugins": [],
        "disablePresets": [],
        "addPlugins": []
      },
      "useIsolateContext": true,
      "userConfirmedUseIsolateContext": false,
      "ignoreUploadUnusedFiles": false
    },
    "usingComponents": true,
    "betterShadow": false,
    "darkmode": false,
    "themeLocation": "theme.json",
    "lazyCodeLoading": "requiredComponents",
    "permission": {
      "scope.userLocation": {
        "desc": "你的位置信息将用于收货地址定位"
      }
    },
    "requiredPrivateInfos": ["chooseAddress"]
  },
  "vueVersion": "3"
}
```

### 3.4 关键配置说明

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `appid` | `wxXXXXXXXXXXXXXXX` | 需替换为真实 appid（开发期可用测试号） |
| `urlCheck` | `false` | 开发期关闭域名校验，上线前改为 `true` 并配置合法域名 |
| `lazyCodeLoading` | `requiredComponents` | 按需注入组件，减小主包体积 |
| `usingComponents` | `true` | 启用自定义组件 |
| `permission.scope.userLocation` | - | 收货地址定位权限说明 |

---

## 第 4 章：请求封装技术方案

> 本章是核心技术方案，基于 `uni.request` 封装 `request` 函数，对齐现有 Axios 拦截器全部逻辑。

### 4.1 uni.request 封装设计

**设计目标**：基于 `uni.request` 封装 `request` 函数，对齐现有 Axios 拦截器全部逻辑。

**核心逻辑清单**（对齐 `frontend/src/api/request.ts`）：

1. **请求拦截**：自动添加 `Authorization: Bearer <token>`。
2. **响应拦截**：
   - 同步服务器时间（`res.timestamp`）。
   - 业务码非 200 → 报错 reject。
   - HTTP 401：区分"Token 过期(1002)"与"业务拒绝(其他码)"；Token 过期则用 refresh_token 刷新，刷新期间并发请求入队等待，刷新成功后重试队列，刷新失败则清空 token 跳转登录。
   - HTTP 403 → 跳转 403 提示；429 → "请求太频繁"；5xx → "服务器异常"。
3. **blob 响应**：Web 端有 Excel 导出场景，小程序端忽略（小程序无 Excel 导出需求）。

**request.ts 完整实现**：

```typescript
import { ENV } from '@/utils/env'
import { tokenStorage } from '@/utils/tokenStorage'
import { syncServerTime } from '@/utils/timeSync'
import { useUserStore } from '@/stores/user'
import { showToast } from '@/utils/toast'
import { navigate } from '@/utils/navigate'
import type { Result } from '@/types/api'

// 刷新锁与等待队列（模块级变量，确保全局唯一）
let isRefreshing = false
let pendingQueue: Array<() => void> = []

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  // 是否跳过自动添加 token（如登录接口）
  skipAuth?: boolean
  // 是否跳过业务码校验（如特殊场景）
  skipResultCheck?: boolean
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const accessToken = tokenStorage.getAccessToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (!options.skipAuth && accessToken) {
      header['Authorization'] = `Bearer ${accessToken}`
    }

    uni.request({
      url: `${ENV.API_BASE_URL}${ENV.API_PREFIX}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: ENV.TIMEOUT,
      success: (res) => {
        const statusCode = res.statusCode
        const resData = res.data as Result<T>

        // 同步服务器时间
        if (resData?.timestamp) {
          syncServerTime(resData.timestamp)
        }

        // HTTP 401 处理
        if (statusCode === 401) {
          const code = resData?.code
          // Token 过期（1002）→ 刷新
          if (code === 1002 && !options.skipAuth) {
            if (!isRefreshing) {
              isRefreshing = true
              const userStore = useUserStore()
              userStore.refreshTokenAction()
                .then(() => {
                  // 刷新成功，重试队列
                  isRefreshing = false
                  pendingQueue.forEach(cb => cb())
                  pendingQueue = []
                })
                .catch(() => {
                  // 刷新失败，清空 token，跳转登录
                  isRefreshing = false
                  pendingQueue = []
                  userStore.clearAuth()
                  navigate.toLogin()
                  reject(new Error('登录已过期，请重新登录'))
                })
            }
            // 入队等待刷新完成
            return new Promise<void>((resolveQueue) => {
              pendingQueue.push(() => {
                request<T>(options).then(resolve).catch(reject)
                resolveQueue()
              })
            })
          }
          // 防重放拦截（1011）→ 不刷新，提示
          if (code === 1011) {
            showToast('操作已过期，请重新发起秒杀', 'none')
            reject(new Error(resData?.message || '防重放拦截'))
            return
          }
          // 其他 401 → 业务拒绝
          showToast(resData?.message || '无权限', 'none')
          reject(new Error(resData?.message || '无权限'))
          return
        }

        // HTTP 403
        if (statusCode === 403) {
          showToast('无权限访问', 'none')
          reject(new Error('403 Forbidden'))
          return
        }

        // HTTP 429
        if (statusCode === 429) {
          showToast('请求太频繁，请稍后再试', 'none')
          reject(new Error('429 Too Many Requests'))
          return
        }

        // HTTP 5xx
        if (statusCode >= 500) {
          showToast('服务器异常，请稍后再试', 'none')
          reject(new Error('服务器异常'))
          return
        }

        // 业务码校验
        if (!options.skipResultCheck && resData?.code !== 200) {
          showToast(resData?.message || '请求失败', 'none')
          reject(new Error(resData?.message || '请求失败'))
          return
        }

        // 成功，返回 data
        resolve(resData?.data)
      },
      fail: (err) => {
        showToast('网络异常，请检查网络连接', 'none')
        reject(err)
      }
    })
  })
}
```

### 4.2 Token 存储适配

**Web 端**：`localStorage`，键名 `access_token` / `refresh_token`。
**小程序端**：`uni.setStorageSync` / `uni.getStorageSync`，键名保持一致。

**tokenStorage.ts 完整实现**：

```typescript
const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export const tokenStorage = {
  getAccessToken(): string | null {
    return uni.getStorageSync(ACCESS_TOKEN_KEY) || null
  },

  setAccessToken(token: string): void {
    uni.setStorageSync(ACCESS_TOKEN_KEY, token)
  },

  getRefreshToken(): string | null {
    return uni.getStorageSync(REFRESH_TOKEN_KEY) || null
  },

  setRefreshToken(token: string): void {
    uni.setStorageSync(REFRESH_TOKEN_KEY, token)
  },

  clearAll(): void {
    uni.removeStorageSync(ACCESS_TOKEN_KEY)
    uni.removeStorageSync(REFRESH_TOKEN_KEY)
  },

  hasToken(): boolean {
    return !!this.getAccessToken()
  }
}
```

> **注意**：`uni.setStorageSync` 是同步 API，与 `localStorage` 行为一致，无需异步处理。小程序 storage 上限 10MB，Token 字符串远小于上限。

### 4.3 Token 刷新机制（并发锁 + 等待队列）

**对齐 Web 端 H-F2 修复逻辑**：当多个请求并发触发 401 时，仅第一个请求执行刷新，其余请求入队等待，刷新成功后重试队列，刷新失败则清空 token 跳转登录。

**核心实现**（已在 4.1 request.ts 中嵌入，此处单独说明关键点）：

```typescript
// 模块级变量（非组件内，确保全局唯一）
let isRefreshing = false
let pendingQueue: Array<() => void> = []

// 401 触发刷新时
if (!isRefreshing) {
  isRefreshing = true
  userStore.refreshTokenAction()
    .then(() => {
      isRefreshing = false
      pendingQueue.forEach(cb => cb())
      pendingQueue = []
    })
    .catch(() => {
      isRefreshing = false
      pendingQueue = []
      userStore.clearAuth()
      navigate.toLogin()
    })
}
// 当前请求入队
pendingQueue.push(() => {
  request<T>(options).then(resolve).catch(reject)
})
```

**关键点**：

1. `isRefreshing` 为模块级变量，确保全局唯一锁。
2. `pendingQueue` 为模块级变量，确保全局唯一队列。
3. 刷新成功后清空队列并依次重试。
4. 刷新失败后清空队列、清空 token、跳转登录。
5. 防重放拦截（1011）不入队，直接 reject。

### 4.4 JWT 过期检测（小程序端 base64 解码）

**Web 端**：使用 `atob` 解码 JWT payload。
**小程序端**：微信小程序不支持 `atob`，需手写 base64 解码或使用 `wx.base64ToArrayBuffer` 替代。

**jwt.ts 完整实现**：

```typescript
// 手写 base64 解码（兼容小程序环境）
function base64Decode(str: string): string {
  // 补齐 padding
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  const pad = base64.length % 4
  if (pad) {
    base64 += '===='.slice(0, 4 - pad)
  }
  // 使用 uni.arrayBufferToBase64 的逆操作
  // 方案：借助小程序内置 Base64 解码
  // 微信小程序基础库 2.4.0+ 支持 wx.base64ToArrayBuffer
  // #ifdef MP-WEIXIN
  const arrayBuffer = wx.base64ToArrayBuffer(base64)
  // 将 ArrayBuffer 转字符串
  const bytes = new Uint8Array(arrayBuffer)
  let result = ''
  for (let i = 0; i < bytes.length; i++) {
    result += String.fromCharCode(bytes[i])
  }
  return decodeURIComponent(escape(result))
  // #endif

  // #ifndef MP-WEIXIN
  // 非微信小程序环境，使用 atob
  return decodeURIComponent(escape(atob(base64)))
  // #endif
}

export function parseJwtPayload(token: string): any {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      throw new Error('Invalid JWT format')
    }
    const payloadStr = base64Decode(parts[1])
    return JSON.parse(payloadStr)
  } catch (e) {
    console.error('JWT 解析失败', e)
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = parseJwtPayload(token)
  if (!payload || !payload.exp) {
    return true
  }
  // exp 是秒级时间戳
  const now = Math.floor(Date.now() / 1000)
  return payload.exp < now
}
```

### 4.5 秒杀防重放头传递

**Web 端**：`buildSeckillHeaders(seckillToken)` → `{ 'X-Seckill-Token': seckillToken }`。
**小程序端**：`uni.request` 的 header 可直接携带自定义头，无需特殊处理。

**replayProtection.ts 完整实现**：

```typescript
export function buildSeckillHeaders(seckillToken: string): Record<string, string> {
  if (!seckillToken) {
    throw new Error('秒杀 token 缺失，请先获取秒杀资格')
  }
  return {
    'X-Seckill-Token': seckillToken
  }
}
```

**秒杀执行调用示例**：

```typescript
async function executeSeckill(seckillId: string): Promise<void> {
  // 1. 获取一次性 token
  const seckillToken = await request<string>({
    url: `/seckill/${encodeURIComponent(seckillId)}/token`,
    method: 'GET'
  })

  // 2. 执行秒杀，携带 X-Seckill-Token 头
  const result = await request({
    url: `/seckill/${encodeURIComponent(seckillId)}/execute`,
    method: 'POST',
    header: buildSeckillHeaders(seckillToken)
  })

  // 3. 处理结果
  console.log('秒杀结果', result)
}
```

### 4.6 雪花 ID 与 URL 编码

**snowflakeId.ts 完整实现**：

```typescript
/**
 * 雪花 ID 处理工具
 * 后端使用雪花算法生成 ID，超过 JS Number.MAX_SAFE_INTEGER
 * 全程使用 string 类型，URL 路径参数用 encodeURIComponent 编码
 */

// 确保 ID 为 string 类型
export function ensureStringId(id: string | number): string {
  return String(id)
}

// URL 路径参数编码
export function encodeId(id: string | number): string {
  return encodeURIComponent(ensureStringId(id))
}

// 构建带 ID 的 URL 路径
export function buildPath(base: string, id: string | number): string {
  return `${base}/${encodeId(id)}`
}
```

**使用示例**：

```typescript
// ❌ 错误：直接拼接数字
request({ url: `/products/${productId}` })

// ✅ 正确：使用 encodeId
request({ url: `/products/${encodeId(productId)}` })

// ✅ 正确：使用 buildPath
request({ url: buildPath('/products', productId) })
```

### 4.7 文件上传适配

**Web 端**：`FormData` + Axios。
**小程序端**：`uni.uploadFile`。

**upload.ts 完整实现**：

```typescript
import { ENV } from '@/utils/env'
import { tokenStorage } from '@/utils/tokenStorage'
import type { Result } from '@/types/api'

interface UploadOptions {
  filePath: string       // 临时文件路径（uni.chooseImage 返回）
  url?: string           // 上传地址，默认 /upload
  name?: string          // 文件字段名，默认 file
  formData?: Record<string, any>  // 额外表单数据
}

export function uploadFile<T = any>(options: UploadOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const accessToken = tokenStorage.getAccessToken()
    uni.uploadFile({
      url: `${ENV.API_BASE_URL}${ENV.API_PREFIX}${options.url || '/upload'}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header: {
        'Authorization': accessToken ? `Bearer ${accessToken}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = JSON.parse(res.data) as Result<T>
          if (data.code === 200) {
            resolve(data.data)
          } else {
            reject(new Error(data.message))
          }
        } else {
          reject(new Error(`上传失败，状态码：${res.statusCode}`))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}
```

**头像上传使用示例**：

```typescript
async function uploadAvatar() {
  // 1. 选择图片
  const chooseRes = await uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera']
  })
  const tempFilePath = chooseRes.tempFilePaths[0]

  // 2. 上传
  const result = await uploadFile<{ url: string }>({
    filePath: tempFilePath,
    url: '/upload'
  })

  // 3. 更新头像
  await request({
    url: '/auth/profile',
    method: 'PUT',
    data: { avatar: result.url }
  })
}
```

### 4.8 错误提示适配

**Web 端**：`ElMessage.success/error/warning`、`ElMessageBox.confirm`。
**小程序端**：`uni.showToast`、`uni.showModal`。

**toast.ts 完整实现**：

```typescript
type ToastType = 'success' | 'error' | 'loading' | 'none'

export function showToast(title: string, type: ToastType = 'none', duration = 2000): void {
  let icon: 'success' | 'error' | 'loading' | 'none' = 'none'
  switch (type) {
    case 'success': icon = 'success'; break
    case 'error': icon = 'error'; break
    case 'loading': icon = 'loading'; break
    default: icon = 'none'
  }
  uni.showToast({ title, icon, duration })
}

export function showConfirm(
  content: string,
  title = '提示'
): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm)
      },
      fail: () => {
        resolve(false)
      }
    })
  })
}

export function showLoading(title = '加载中...'): void {
  uni.showLoading({ title, mask: true })
}

export function hideLoading(): void {
  uni.hideLoading()
}
```

### 4.9 路由跳转适配

**Web 端**：`router.push` / `router.replace`。
**小程序端**：`uni.navigateTo` / `uni.switchTab` / `uni.redirectTo` / `uni.navigateBack`。

**navigate.ts 完整实现**：

```typescript
// tabBar 页面路径集合
const TAB_BAR_PAGES = [
  'pages/home/home',
  'pages/category/category',
  'pages/cart/cart',
  'pages/profile/profile'
]

function isTabBarPage(path: string): boolean {
  return TAB_BAR_PAGES.some(p => path.startsWith(p))
}

function buildQueryString(params: Record<string, any>): string {
  return Object.entries(params)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
}

export const navigate = {
  // 跳转（自动区分 tabBar 与非 tabBar）
  to(path: string, params?: Record<string, any>): void {
    const queryString = params ? buildQueryString(params) : ''
    const fullPath = `${path}${queryString ? `?${queryString}` : ''}`

    if (isTabBarPage(path)) {
      uni.switchTab({ url: `/${fullPath}` })
    } else {
      uni.navigateTo({ url: `/${fullPath}` })
    }
  },

  // 重定向
  redirect(path: string, params?: Record<string, any>): void {
    const queryString = params ? buildQueryString(params) : ''
    const fullPath = `${path}${queryString ? `?${queryString}` : ''}`
    uni.redirectTo({ url: `/${fullPath}` })
  },

  // 返回
  back(delta = 1): void {
    uni.navigateBack({ delta })
  },

  // 跳转登录页（携带 redirect 参数）
  toLogin(redirect?: string): void {
    const params = redirect ? { redirect } : undefined
    this.to('pages/login/login', params)
  }
}
```

### 4.10 跨端条件编译与域名配置

**条件编译**：

```typescript
// #ifdef MP-WEIXIN
// 仅微信小程序执行的代码
console.log('微信小程序环境')
// #endif

// #ifndef MP-WEIXIN
// 非微信小程序环境执行的代码
// #endif

// #ifdef H5
// 仅 H5 环境执行的代码
// #endif
```

**网络域名配置**：

| 环境 | 配置方式 | 说明 |
|------|----------|------|
| 开发期 | `manifest.json` 中 `urlCheck: false` | 关闭域名校验，可请求任意域名 |
| 上线前 | 微信公众平台 → 开发设置 → 服务器域名 → request 合法域名 | 添加后端域名（如 `https://api.seckill-mall.com`） |
| 上线前 | uploadFile 合法域名 | 添加后端域名（文件上传） |
| 上线前 | downloadFile 合法域名 | 添加后端域名（文件下载，若有） |

**storage 差异**：

| 维度 | Web 端 localStorage | 小程序 uni.setStorageSync |
|------|---------------------|---------------------------|
| 同步/异步 | 同步 | 同步（StorageSync）/异步（Storage） |
| 容量限制 | 5-10MB | 10MB |
| 清理时机 | 持久化 | 用户主动清理或小程序被删除 |
| API | getItem/setItem | getStorageSync/setStorageSync |

> **结论**：行为一致，封装层屏蔽差异即可（见 4.2 tokenStorage）。

---

## 第 5 章：状态管理方案

### 5.1 Pinia stores 设计

按业务域拆分 5 个 store：user/cart/category/seckill/app，对齐 Web 端 stores 逻辑，持久化适配 uni storage。

| Store | 职责 | 持久化 |
|-------|------|--------|
| `user.ts` | token/userInfo/login/logout/refreshTokenAction/clearAuth | token 通过 tokenStorage 持久化，userInfo 不持久化（每次启动拉取） |
| `cart.ts` | cartList/cartCount/selectedItems/addToCart/updateQuantity/removeItem | 不持久化（每次进入页面重新拉取） |
| `category.ts` | categoryList/fetchCategories | 不持久化（每次进入页面重新拉取） |
| `seckill.ts` | seckillList/activities/currentSession/fetchSeckillList | 不持久化 |
| `app.ts` | timeOffset/serverTime/syncServerTime/getServerTime | 不持久化（timeOffset 内存态） |

### 5.2 stores/index.ts 实现示例

```typescript
import { createPinia } from 'pinia'

const pinia = createPinia()

export default pinia
```

### 5.3 stores/user.ts 实现示例

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tokenStorage } from '@/utils/tokenStorage'
import { isTokenExpired } from '@/utils/jwt'
import * as authApi from '@/api/auth'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref<boolean>(tokenStorage.hasToken())

  // 登录
  async function login(loginForm: { account: string; password: string; captchaCode: string; captchaKey: string }) {
    const res = await authApi.login(loginForm)
    tokenStorage.setAccessToken(res.accessToken)
    tokenStorage.setRefreshToken(res.refreshToken)
    isLoggedIn.value = true
    await fetchUserInfo()
    return res
  }

  // 拉取用户信息
  async function fetchUserInfo() {
    const res = await authApi.me()
    userInfo.value = res
    return res
  }

  // 刷新 token
  async function refreshTokenAction() {
    const refreshToken = tokenStorage.getRefreshToken()
    if (!refreshToken) {
      throw new Error('refresh_token 不存在')
    }
    const res = await authApi.refresh({ refreshToken })
    tokenStorage.setAccessToken(res.accessToken)
    tokenStorage.setRefreshToken(res.refreshToken)
    return res
  }

  // 清空认证信息
  function clearAuth() {
    tokenStorage.clearAll()
    userInfo.value = null
    isLoggedIn.value = false
  }

  // 登出
  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }

  // 启动时恢复登录态
  function restoreFromStorage() {
    const accessToken = tokenStorage.getAccessToken()
    if (accessToken && !isTokenExpired(accessToken)) {
      isLoggedIn.value = true
      fetchUserInfo().catch(() => {
        clearAuth()
      })
    } else {
      clearAuth()
    }
  }

  return {
    userInfo,
    isLoggedIn,
    login,
    fetchUserInfo,
    refreshTokenAction,
    clearAuth,
    logout,
    restoreFromStorage
  }
})
```

### 5.4 stores/app.ts 实现示例

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { syncServerTime as syncTime, getServerTime as getServer } from '@/utils/timeSync'

export const useAppStore = defineStore('app', () => {
  const timeOffset = ref<number>(0)
  const serverTime = ref<number>(0)

  function syncServerTime(timestamp: string) {
    syncTime(timestamp)
    timeOffset.value = syncTime(timestamp)
    serverTime.value = getServer()
  }

  function getServerTime(): number {
    return getServer()
  }

  return {
    timeOffset,
    serverTime,
    syncServerTime,
    getServerTime
  }
})
```

---

## 第 6 章：全局样式与主题

### 6.1 uni.scss 设计

```scss
/* uView Plus 主题变量 */
$u-primary: #2979ff;
$u-success: #19be6b;
$u-warning: #ff9900;
$u-error: #ff4d4f;
$u-info: #909399;

/* 业务自定义变量 */
$brand-color: #ff4d4f;          /* 品牌色（秒杀红） */
$brand-color-light: #fff1f0;    /* 品牌色浅 */
$text-color-primary: #303133;   /* 主文本 */
$text-color-regular: #606266;   /* 常规文本 */
$text-color-secondary: #909399; /* 次要文本 */
$border-color: #dcdfe6;         /* 边框色 */
$bg-color: #f8f8f8;             /* 背景色 */
$card-bg: #ffffff;              /* 卡片背景 */

/* 间距 */
$spacing-xs: 8rpx;
$spacing-sm: 16rpx;
$spacing-md: 24rpx;
$spacing-lg: 32rpx;
$spacing-xl: 48rpx;

/* 圆角 */
$radius-sm: 8rpx;
$radius-md: 12rpx;
$radius-lg: 16rpx;

/* uni-app 内置变量 */
$uni-bg-color: #f8f8f8;
$uni-text-color: #303133;
$uni-font-size-base: 28rpx;
```

### 6.2 App.vue 全局样式

```vue
<style lang="scss">
/* 注意：App.vue 的 style 不带 scoped，全局生效 */
@import 'uview-plus/index.scss';

page {
  background-color: #f8f8f8;
  font-size: 28rpx;
  color: #303133;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, sans-serif;
}

/* 全局工具类 */
.flex { display: flex; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.text-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text-ellipsis-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
```

### 6.3 rpx 适配说明

- 全部使用 rpx 单位，以 750rpx 为基准（对应设计稿 750px 宽度）。
- 移动端单栏布局，内容宽度 100%。
- 关键操作底部固定，使用 `position: fixed; bottom: 0;` + safe-area-inset-bottom 适配。

---

## 第 7 章：环境变量方案

uni-app 无 Vite 的 `VITE_` 前缀约定，采用自定义 env 文件 + 条件编译方案。

### 7.1 env 文件

**env/.env.development**：

```
UNI_API_BASE_URL=http://localhost:8080
UNI_API_PREFIX=/api/v1
UNI_TIMEOUT=10000
```

**env/.env.production**：

```
UNI_API_BASE_URL=https://api.seckill-mall.com
UNI_API_PREFIX=/api/v1
UNI_TIMEOUT=10000
```

### 7.2 vite.config.ts 读取 env

```typescript
import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'UNI_')
  return {
    plugins: [uni()],
    define: {
      'process.env.UNI_API_BASE_URL': JSON.stringify(env.UNI_API_BASE_URL),
      'process.env.UNI_API_PREFIX': JSON.stringify(env.UNI_API_PREFIX),
      'process.env.UNI_TIMEOUT': JSON.stringify(env.UNI_TIMEOUT)
    }
  }
})
```

### 7.3 utils/env.ts 统一导出

```typescript
export const ENV = {
  API_BASE_URL: process.env.UNI_API_BASE_URL as string,
  API_PREFIX: process.env.UNI_API_PREFIX as string,
  TIMEOUT: Number(process.env.UNI_TIMEOUT) || 10000
}
```

---

## 第 8 章：风险预案

> 源自源文档第 7.1 节风险矩阵，列出 Top 风险及应对策略。

### 8.1 风险矩阵

| 编号 | 风险描述 | 影响程度 | 发生概率 | 应对策略 | 责任阶段 |
|------|----------|----------|----------|----------|----------|
| R1 | 微信小程序 2MB 主包体积限制 | 高 | 高 | 分包策略（主包仅放高频页 + 4 分包按业务域划分）+ 按需注入组件（lazyCodeLoading: requiredComponents）+ 图片压缩 + 静态资源走 CDN | 阶段 0/6 |
| R2 | request 合法域名配置问题 | 高 | 中 | 开发期关闭 urlCheck；上线前在微信公众平台配置 request/uploadFile/downloadFile 合法域名；后端需 HTTPS | 阶段 6 |
| R3 | uni-app Vue3 + Pinia 持久化兼容 | 中 | 中 | Pinia 状态不直接持久化，仅 Token 通过 tokenStorage（uni.setStorageSync）持久化；其他状态每次进入页面重新拉取 | 阶段 0 |
| R4 | uView Plus 与 uni-app 版本兼容 | 中 | 中 | 锁定 uView Plus 版本（package.json 用精确版本号）；阶段 0 联调时验证核心组件（u-button/u-input/u-form/u-tabs/u-swiper）正常 | 阶段 0 |
| R5 | 秒杀高并发下小程序端轮询性能 | 高 | 中 | 秒杀结果轮询间隔动态调整（首次 1s，后续指数退避至 5s）；轮询上限 30 次；超时提示用户主动查询 | 阶段 4 |
| R6 | 富文本渲染兼容（rich-text 限制） | 中 | 高 | 标签过滤 + class 转 inline style；若复杂度高，引入 mp-html 插件；阶段 2 联调时验证商品详情富文本渲染正确 | 阶段 2 |
| R7 | 图形验证码 base64 渲染 | 中 | 中 | 确认后端 `/api/v1/auth/captcha` 返回格式（base64 或流）；方案 A 直接渲染，方案 B 用 arraybuffer 转 base64；阶段 0 联调时确认 | 阶段 0/1 |
| R8 | Token 刷新并发竞态 | 高 | 中 | 模块级 isRefreshing 锁 + pendingQueue 等待队列；阶段 1 联调时模拟并发 401 场景验证 | 阶段 1 |
| R9 | 雪花 ID 精度丢失 | 高 | 高 | 全程 string 类型 + encodeURIComponent；TypeScript 类型定义显式 string；代码 review 检查所有 ID 使用点 | 阶段 0/6 |
| R10 | 后端 CORS 与小程序不在 CORS 范围 | 中 | 低 | 小程序请求不受 CORS 限制（CORS 是浏览器机制），但需配置合法域名；后端无需为小程序额外配置 CORS | 阶段 6 |
| R11 | 小程序登录态丢失（用户清理缓存） | 低 | 中 | 启动时 restoreFromStorage 检查 Token 有效性；无效则跳转登录；关键操作前再次校验 | 阶段 1 |
| R12 | 移动端键盘弹起遮挡输入框 | 中 | 高 | 使用 u-input 的 adjust-position 属性自动调整；关键表单用 cursor-spacing 配置 | 阶段 1/3 |
| R13 | 图片懒加载与内存占用 | 中 | 中 | 使用 u-image 的 lazy-load 属性；长列表图片用 IntersectionObserver（小程序内置）按需加载；图片压缩至合理尺寸 | 阶段 2/6 |
| R14 | 分包加载延迟影响体验 | 中 | 中 | 配置 preloadRule 预加载分包；首页加载时预下载商品和秒杀分包；"我的"页加载时预下载用户和订单分包 | 阶段 0/6 |
| R15 | 微信开发者工具与真机表现差异 | 中 | 高 | 真机测试覆盖 iOS + Android 多机型；重点关注：rich-text 渲染、base64 图片、uni.request header、storage 行为 | 阶段 6 |

### 8.2 Top 风险应对策略详解

#### R1：主包体积限制

- **策略**：分包 + 按需注入 + 图片压缩 + CDN
- **实施**：
  - 主包仅放 7 个高频页面（首页/分类/购物车/我的/登录/注册/找回密码）
  - 4 分包按业务域划分（pages-product/pages-order/pages-seckill/pages-user）
  - `lazyCodeLoading: requiredComponents` 按需注入组件
  - 静态资源走 CDN，图片压缩至合理尺寸
- **目标**：主包 ≤ 1.5MB（预留 0.5MB 余量）

#### R8：Token 刷新并发竞态

- **策略**：模块级 isRefreshing 锁 + pendingQueue 等待队列
- **实施**：见第 4.3 节 Token 刷新机制
- **验证**：阶段 1 联调时模拟并发 401 场景验证

#### R9：雪花 ID 精度丢失

- **策略**：全程 string 类型 + encodeURIComponent
- **实施**：见第 4.6 节 雪花 ID 与 URL 编码
- **验证**：代码 review 检查所有 ID 使用点

#### R5：秒杀轮询性能

- **策略**：动态间隔 + 指数退避 + 上限
- **实施**：
  - 首次轮询间隔 1s
  - 后续指数退避至 5s
  - 轮询上限 30 次
  - 超时提示用户主动查询

---

> **文档结束**
> 本 plan.md 定义了 uni-app 端微信小程序的技术方案，含技术栈、目录架构、路由分包、请求封装、状态管理、全局样式、环境变量、风险预案。功能规格见 `spec.md`，任务执行顺序见 `tasks.md`。