# tasks.md — uni-app 端微信小程序任务清单

> **项目名称**：秒杀商城（Seckill Mall）— uni-app 端微信小程序
> **文档类型**：任务清单（What to do，按阶段拆解）
> **版本**：v1.0
> **撰写日期**：2026-08-09
> **目标平台**：微信小程序（uni-app 编译目标 `mp-weixin`）
> **技术栈**：uni-app + Vue 3 + TypeScript + uView Plus + Pinia
> **用途**：定义"做什么（执行顺序）"，作为开发进度跟踪的依据。不含技术实现细节（实现见 `plan.md`）。
> **内容来源**：《uni-app 端微信小程序开发计划》第 5 章阶段 0-6 任务拆解 + 第 5.9 节里程碑节点汇总
> **任务编号规则**：T{阶段号}.{序号}，如 T0.1、T1.3、T4.2
> **依赖标注**：每个任务标注 blocked_by（依赖的任务编号）

---

## 目录

- [阶段总览表](#阶段总览表)
- [阶段 0：项目初始化与公共基建](#阶段-0项目初始化与公共基建)
- [阶段 1：认证模块](#阶段-1认证模块)
- [阶段 2：商品浏览模块](#阶段-2商品浏览模块)
- [阶段 3：交易核心模块](#阶段-3交易核心模块)
- [阶段 4：秒杀模块](#阶段-4秒杀模块)
- [阶段 5：用户中心模块](#阶段-5用户中心模块)
- [阶段 6：联调测试与提审](#阶段-6联调测试与提审)
- [里程碑汇总表](#里程碑汇总表)

---

## 阶段总览表

| 阶段 | 名称 | 内容概述 | 预估工时（人天） | 里程碑 | 依赖阶段 |
|------|------|----------|------------------|--------|----------|
| 阶段 0 | 项目初始化与公共基建 | 项目创建、依赖安装、请求封装、Token 存储、Pinia stores、类型定义、全局样式、uView Plus 配置 | 4 | M1 基建完成 | 无 |
| 阶段 1 | 认证模块 | 登录、注册、找回密码、图形验证码、路由守卫、Token 刷新 | 4 | M2 认证闭环 | 阶段 0 |
| 阶段 2 | 商品浏览模块 | 首页、商品列表、商品详情、分类导航、Banner | 5 | M3 商品浏览可演示 | 阶段 0 |
| 阶段 3 | 交易核心模块 | 购物车、结算、订单列表、订单详情、支付、地址 | 6 | M4 交易闭环 | 阶段 0、阶段 2 |
| 阶段 4 | 秒杀模块 | 秒杀专区、倒计时、一次性 token、执行秒杀、结果轮询 | 4 | M5 秒杀闭环 | 阶段 0、阶段 2 |
| 阶段 5 | 用户中心模块 | 个人中心、收藏、优惠券、钱包、评价 | 4 | M6 用户中心完成 | 阶段 0、阶段 1 |
| 阶段 6 | 联调测试与提审 | 全量联调、真机测试、秒杀压测、性能优化、提审 | 5 | M7 提审完成 | 阶段 0-5 |
| **合计** | - | - | **32 人天** | - | - |

---

## 阶段 0：项目初始化与公共基建

> **阶段目标**：搭建可运行的 uni-app 项目骨架，完成请求封装、Token 存储、Pinia stores、类型定义、全局样式、uView Plus 配置等公共基建。
> **阶段依赖**：无
> **预估总工时**：4 人天
> **里程碑节点**：M1 基建完成

### 任务清单

- [ ] **T0.1** HBuilderX/CLI 创建 uni-app Vue3+TS 项目 (预估: 0.5人天) [依赖: 无]
  - 描述：在 `miniapp/` 目录下创建 uni-app Vue 3 + TypeScript 项目，项目结构包含 `src/`、`pages/`、`static/`、`App.vue`、`main.ts`、`manifest.json`、`pages.json`、`uni.scss`。
  - 交付物：可运行的 uni-app 项目骨架。

- [ ] **T0.2** 安装依赖（uView Plus、pinia、sass） (预估: 0.5人天) [依赖: T0.1]
  - 描述：安装 uview-plus、pinia、sass、sass-loader 等依赖，配置 main.ts 引入 uview-plus。
  - 交付物：package.json 依赖列表，main.ts 引入 uview-plus。

- [ ] **T0.3** 配置 manifest.json（appid、权限等） (预估: 0.5人天) [依赖: T0.1]
  - 描述：配置 manifest.json 微信小程序 appid、urlCheck、lazyCodeLoading、permission 等关键配置项。
  - 交付物：manifest.json 完整配置。

- [ ] **T0.4** 配置 pages.json（主包 + 分包 + tabBar） (预估: 0.5人天) [依赖: T0.1]
  - 描述：配置 pages.json 主包（首页/分类/购物车/我的/登录/注册/找回密码）+ 4 分包（pages-product/pages-order/pages-seckill/pages-user）+ tabBar 4 tab + preloadRule 预加载。
  - 交付物：pages.json 完整配置。

- [ ] **T0.5** 设计 uni.scss 全局样式变量 (预估: 0.5人天) [依赖: T0.2]
  - 描述：定义 uni.scss 主题变量（uView Plus 主题覆盖 + 业务自定义变量 + 间距 + 圆角 + uni-app 内置变量），App.vue 全局样式引入 uview-plus/index.scss。
  - 交付物：uni.scss 与 App.vue 全局样式。

- [ ] **T0.6** 配置环境变量方案（env 文件 + vite.config.ts） (预估: 0.5人天) [依赖: T0.1]
  - 描述：创建 env/.env.development 与 env/.env.production，配置 vite.config.ts 读取 env，封装 utils/env.ts 统一导出 ENV。
  - 交付物：env 文件、vite.config.ts、utils/env.ts。

- [ ] **T0.7** 定义 TypeScript 类型（types/ 全部） (预估: 1人天) [依赖: T0.1]
  - 描述：定义 types/ 下全部类型：api.ts（Result<T> 等）、user.ts、product.ts、cart.ts、order.ts、seckill.ts、address.ts、coupon.ts、common.ts。所有 ID 字段显式声明为 string。
  - 交付物：types/ 目录下 9 个类型文件。

- [ ] **T0.8** 封装 tokenStorage 工具（uni.setStorageSync） (预估: 0.5人天) [依赖: T0.2]
  - 描述：封装 tokenStorage 工具，提供 getAccessToken/setAccessToken/getRefreshToken/setRefreshToken/clearAll/hasToken 方法，基于 uni.setStorageSync/getStorageSync。
  - 交付物：utils/tokenStorage.ts。

- [ ] **T0.9** 封装 JWT 解析工具（base64 解码 + payload 提取） (预估: 0.5人天) [依赖: T0.8]
  - 描述：封装 jwt.ts，提供 parseJwtPayload/isTokenExpired 方法，使用 wx.base64ToArrayBuffer 替代 atob，兼容小程序环境。
  - 交付物：utils/jwt.ts。

- [ ] **T0.10** 封装 timeSync 工具（服务器时间同步） (预估: 0.5人天) [依赖: T0.7]
  - 描述：封装 timeSync.ts，提供 syncServerTime/getServerTime/getTimeOffset 方法，计算 timeOffset = serverTime - localTime。
  - 交付物：utils/timeSync.ts。

- [ ] **T0.11** 封装 request 核心函数（请求/响应拦截器、401 刷新、业务码校验） (预估: 1.5人天) [依赖: T0.8, T0.9, T0.10]
  - 描述：封装 request.ts，对齐 Axios 拦截器全部逻辑：请求拦截加 token、响应拦截解包 Result、业务码校验、服务器时间同步、401 刷新+请求队列、403/429/5xx 处理、防重放 1011 处理。
  - 交付物：api/request.ts。

- [ ] **T0.12** 封装 replayProtection 工具（X-Seckill-Token 头） (预估: 0.5人天) [依赖: T0.7]
  - 描述：封装 replayProtection.ts，提供 buildSeckillHeaders 方法，构建 `{ 'X-Seckill-Token': seckillToken }` 头。
  - 交付物：utils/replayProtection.ts。

- [ ] **T0.13** 封装 snowflakeId 工具（string + encodeURIComponent） (预估: 0.5人天) [依赖: T0.7]
  - 描述：封装 snowflakeId.ts，提供 ensureStringId/encodeId/buildPath 方法，确保 ID 为 string 类型，URL 路径参数用 encodeURIComponent 编码。
  - 交付物：utils/snowflakeId.ts。

- [ ] **T0.14** 封装 navigate 工具（tabBar 与非 tabBar 区分） (预估: 0.5人天) [依赖: T0.4]
  - 描述：封装 navigate.ts，提供 to/redirect/back/toLogin 方法，自动区分 tabBar 与非 tabBar 页面，tabBar 用 uni.switchTab，非 tabBar 用 uni.navigateTo。
  - 交付物：utils/navigate.ts。

- [ ] **T0.15** 封装 toast 工具（uni.showToast/showModal 统一） (预估: 0.5人天) [依赖: T0.2]
  - 描述：封装 toast.ts，提供 showToast/showConfirm/showLoading/hideLoading 方法，统一封装 uni.showToast/uni.showModal。
  - 交付物：utils/toast.ts。

- [ ] **T0.16** 创建 Pinia stores（user/cart/category/seckill/app） (预估: 1人天) [依赖: T0.8, T0.11]
  - 描述：创建 Pinia stores：user.ts（token/userInfo/login/logout/refresh）、cart.ts、category.ts、seckill.ts、app.ts（timeOffset/serverTime 等），对齐 Web 端 stores 逻辑，持久化适配 uni storage。
  - 交付物：stores/ 目录下 6 个文件。

- [ ] **T0.17** 封装全部 API 接口模块（api/ 下 15 个文件） (预估: 1.5人天) [依赖: T0.11]
  - 描述：封装 api/ 下 15 个接口模块：auth/product/category/cart/order/seckill/favorite/address/coupon/wallet/banner/review/upload，基于 request 核心函数。
  - 交付物：api/ 目录下 15 个接口文件。

- [ ] **T0.18** 创建公共组件（NavBar/ProductCard/EmptyState/LoadMore/PriceTag/CountdownTimer/CaptchaInput/RichTextRenderer） (预估: 1.5人天) [依赖: T0.2]
  - 描述：创建公共组件：NavBar（自定义导航栏）、ProductCard（商品卡片）、EmptyState（空状态）、LoadMore（加载更多）、PriceTag（价格标签）、CountdownTimer（倒计时）、CaptchaInput（图形验证码输入）、AddressSelector（地址选择器）、SkuSelector（SKU 规格选择器）、RichTextRenderer（富文本渲染组件，rich-text 封装）。
  - 交付物：components/ 目录下公共组件。

- [ ] **T0.19** 联调验证：请求封装能正常访问后端 `/api/v1/banners` 公开接口 (预估: 0.5人天) [依赖: T0.11, T0.17]
  - 描述：联调验证请求封装能正常访问后端公开接口，解包 Result<T>，Token 存储读写正常，TypeScript 类型无报错。
  - 交付物：联调验证报告。

### 里程碑 M1：基建完成

- **交付物**：可运行的 uni-app 项目骨架，请求封装通过联调验证。
- **验收标准**：能成功调用后端公开接口并解包 `Result<T>`，Token 存储读写正常，TypeScript 类型无报错。

---

## 阶段 1：认证模块

> **阶段目标**：实现登录、注册、找回密码三页面，完成图形验证码、路由守卫、Token 刷新机制。
> **阶段依赖**：阶段 0
> **预估总工时**：4 人天
> **里程碑节点**：M2 认证闭环

### 任务清单

- [ ] **T1.1** 实现图形验证码组件 CaptchaInput（base64 渲染 + 点击刷新） (预估: 0.5人天) [依赖: T0.18]
  - 描述：实现 CaptchaInput 组件，支持 base64 渲染与点击刷新，确认后端 `/api/v1/auth/captcha` 返回格式（方案 A base64 或方案 B 图片流）。
  - 交付物：components/CaptchaInput/CaptchaInput.vue。

- [ ] **T1.2** 实现登录页 login.vue（账号/邮箱/手机号 + 验证码 + 记住我） (预估: 1人天) [依赖: T1.1, T0.16]
  - 描述：实现登录页，含账号/邮箱/手机号输入、密码输入、图形验证码、记住我（uni.setStorageSync）、登录提交、登录后 uni.switchTab 跳转首页 tab。
  - 交付物：pages/login/login.vue。

- [ ] **T1.3** 实现注册页 register.vue（注册表单 + 验证码 + 协议同意） (预估: 1人天) [依赖: T1.1, T0.16]
  - 描述：实现注册页，含注册表单、图形验证码 + 短信验证码、协议同意（u-checkbox + uni.navigateTo 协议页）、注册提交。
  - 交付物：pages/register/register.vue。

- [ ] **T1.4** 实现找回密码页 forgot-password.vue（发送验证码 + 倒计时 + 重置） (预估: 1人天) [依赖: T1.1, T0.16]
  - 描述：实现找回密码页，含邮箱/手机验证码（发送按钮 + 倒计时）、重置密码表单、提交。
  - 交付物：pages/forgot-password/forgot-password.vue。

- [ ] **T1.5** 实现路由守卫（uni-app 拦截器 + requiresAuth 校验 + 跳转登录） (预估: 1人天) [依赖: T0.14, T0.16]
  - 描述：实现路由守卫，uni-app 拦截器 + requiresAuth 校验 + 跳转登录页（携带 redirect 参数）。
  - 交付物：路由守卫逻辑。

- [ ] **T1.6** 实现 Token 刷新并发锁 + 等待队列（对齐 Web 端 H-F2 修复逻辑） (预估: 0.5人天) [依赖: T0.11, T0.16]
  - 描述：实现 Token 刷新并发锁 isRefreshing + 等待队列 pendingQueue，对齐 Web 端 H-F2 修复逻辑，刷新成功后重试队列，刷新失败清空 token 跳转登录。
  - 交付物：request.ts 中 Token 刷新逻辑。

- [ ] **T1.7** 联调验证：登录 → 拉取用户信息 → Token 过期自动刷新 → 登出 (预估: 0.5人天) [依赖: T1.2, T1.5, T1.6]
  - 描述：联调验证完整认证闭环：登录 → 拉取用户信息 → Token 过期自动刷新 → 登出，模拟并发 401 场景验证刷新锁。
  - 交付物：联调验证报告。

### 里程碑 M2：认证闭环

- **交付物**：登录、注册、找回密码三页面可用，Token 刷新机制完整。
- **验收标准**：登录成功后 Token 持久化，关闭小程序重开仍保持登录态；Token 过期自动刷新无感知；refresh_token 失效跳转登录页。

---

## 阶段 2：商品浏览模块

> **阶段目标**：实现首页、分类、商品列表、商品详情四页面，完成 Banner、分类导航、富文本渲染、SKU 选择。
> **阶段依赖**：阶段 0
> **预估总工时**：5 人天
> **里程碑节点**：M3 商品浏览可演示

### 任务清单

- [ ] **T2.1** 实现首页 home.vue（Banner 轮播 + 秒杀入口 + 分类导航 + 猜你喜欢触底加载） (预估: 1.5人天) [依赖: T0.17, T0.18]
  - 描述：实现首页，含 Banner 轮播（u-swiper）、秒杀专区入口（u-card）、分类导航（u-tabs scrollable）、猜你喜欢触底加载（onReachBottom + u-loadmore）、分类筛选（底部弹出 ActionSheet）。
  - 交付物：pages/home/home.vue。

- [ ] **T2.2** 实现分类页 category.vue（分类 tab + 商品列表） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现分类页，含分类 tab + 商品列表展示。
  - 交付物：pages/category/category.vue。

- [ ] **T2.3** 实现商品列表页 product-list.vue（分类筛选 + 价格区间 + 排序 + 触底加载） (预估: 1.5人天) [依赖: T0.17, T0.18]
  - 描述：实现商品列表页，含分类筛选（顶部 u-tabs + 下拉筛选面板）、价格区间筛选（u-slider 双滑块）、多维度排序（顶部排序栏 + ActionSheet）、触底加载、2 列网格展示。
  - 交付物：pages-product/pages/product-list/product-list.vue。

- [ ] **T2.4** 实现富文本渲染组件 RichTextRenderer（rich-text 封装 + 标签过滤） (预估: 0.5人天) [依赖: T0.18]
  - 描述：实现 RichTextRenderer 组件，封装 rich-text，过滤不支持的标签（script、style、link），class 转 inline style。
  - 交付物：components/RichTextRenderer/RichTextRenderer.vue。

- [ ] **T2.5** 实现 SKU 选择器组件 SkuSelector（底部弹出 + 规格矩阵） (预估: 1人天) [依赖: T0.18]
  - 描述：实现 SkuSelector 组件，底部弹出 + 规格矩阵，规格切换后价格/库存更新。
  - 交付物：components/SkuSelector/SkuSelector.vue。

- [ ] **T2.6** 实现商品详情页 product-detail.vue（图片轮播 + SKU + 参数 + 评价 + 富文本详情） (预估: 1.5人天) [依赖: T2.4, T2.5, T0.17]
  - 描述：实现商品详情页，含图片轮播（u-swiper 全宽）、SKU 选择（SkuSelector）、参数速览（u-cell）、服务保障（u-tag + u-popup）、商品评价（列表 + 触底加载）、售后说明（u-collapse）、富文本详情（RichTextRenderer）。
  - 交付物：pages-product/pages/product-detail/product-detail.vue。

- [ ] **T2.7** 联调验证：首页 → 商品列表 → 商品详情 → 加购物车 → 立即购买 (预估: 0.5人天) [依赖: T2.1, T2.3, T2.6]
  - 描述：联调验证商品浏览流程：首页 → 商品列表 → 商品详情 → 加购物车 → 立即购买。
  - 交付物：联调验证报告。

### 里程碑 M3：商品浏览可演示

- **交付物**：首页、分类、商品列表、商品详情四页面可用。
- **验收标准**：Banner 轮播正常，触底加载流畅，商品详情富文本渲染正确，SKU 选择可用。

---

## 阶段 3：交易核心模块

> **阶段目标**：实现购物车、结算、订单列表、订单详情、地址管理全流程，跑通完整购物流程。
> **阶段依赖**：阶段 0、阶段 2
> **预估总工时**：6 人天
> **里程碑节点**：M4 交易闭环

### 任务清单

- [ ] **T3.1** 实现购物车页 cart.vue（列表 + 左滑删除 + 数量修改 + 批量选中 + 底部结算栏） (预估: 1.5人天) [依赖: T0.17, T0.18]
  - 描述：实现购物车页，含商品列表展示、左滑删除（u-swipe-action）、数量修改（u-number-box）、批量选中（u-checkbox 全选/反选）、底部固定结算栏。
  - 交付物：pages/cart/cart.vue。

- [ ] **T3.2** 实现地址列表页 address-list.vue（地址列表 + 设为默认 + 编辑/删除） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现地址列表页，含地址列表、设为默认、编辑/删除。
  - 交付物：pages-user/pages/address-list/address-list.vue。

- [ ] **T3.3** 实现地址编辑页 address-edit.vue（省市区联动 + 表单校验） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现地址编辑页，含省市区联动、表单校验。
  - 交付物：pages-user/pages/address-edit/address-edit.vue。

- [ ] **T3.4** 实现地址选择器组件 AddressSelector (预估: 0.5人天) [依赖: T3.2]
  - 描述：实现地址选择器组件，跳转地址列表选择后回填。
  - 交付物：components/AddressSelector/AddressSelector.vue。

- [ ] **T3.5** 实现结算页 checkout.vue（地址选择 + 支付方式 + 备注 + 商品清单 + 提交） (预估: 1.5人天) [依赖: T3.4, T0.17]
  - 描述：实现结算页，含地址选择（AddressSelector）、支付方式（u-radio-group）、订单备注（u-input textarea）、商品清单（u-cell 列表）、底部固定提交栏。
  - 交付物：pages-order/pages/checkout/checkout.vue。

- [ ] **T3.6** 实现订单列表页 order-list.vue（状态筛选 + 类型筛选 + 左滑删除 + 触底加载） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现订单列表页，含状态筛选（u-tabs scrollable）、订单类型筛选、左滑删除（u-swipe-action）、触底加载。
  - 交付物：pages-order/pages/order-list/order-list.vue。

- [ ] **T3.7** 实现订单详情页 order-detail.vue（状态步骤 + 商品列表 + 支付 + 确认收货 + 取消） (预估: 1.5人天) [依赖: T0.17, T0.18]
  - 描述：实现订单详情页，含收货地址展示（u-cell）、商品列表（u-cell）、支付（u-button）、确认收货（u-button + uni.showModal）、取消订单、订单状态（u-steps）。
  - 交付物：pages-order/pages/order-detail/order-detail.vue。

- [ ] **T3.8** 联调验证：购物车 → 结算 → 创建订单 → 支付 → 订单详情 → 确认收货 (预估: 0.5人天) [依赖: T3.1, T3.5, T3.6, T3.7]
  - 描述：联调验证完整交易流程：购物车 → 结算 → 创建订单 → 支付 → 订单详情 → 确认收货。
  - 交付物：联调验证报告。

### 里程碑 M4：交易闭环

- **交付物**：购物车、结算、订单列表、订单详情、地址管理全流程可用。
- **验收标准**：完整购物流程跑通，订单状态流转正确，支付（模拟）成功。

---

## 阶段 4：秒杀模块

> **阶段目标**：实现秒杀专区页，完成倒计时、一次性 token、执行秒杀、结果轮询全流程。
> **阶段依赖**：阶段 0、阶段 2
> **预估总工时**：4 人天
> **里程碑节点**：M5 秒杀闭环

### 任务清单

- [ ] **T4.1** 实现倒计时组件 CountdownTimer（服务器时间对齐 + 毫秒级精度） (预估: 1人天) [依赖: T0.10, T0.18]
  - 描述：实现 CountdownTimer 组件，服务器时间对齐（timeOffset）+ 毫秒级精度，setInterval 驱动。
  - 交付物：components/CountdownTimer/CountdownTimer.vue。

- [ ] **T4.2** 实现秒杀专区页 seckill-zone.vue（场次切换 + 倒计时 + 库存排序 + 网格展示） (预估: 1.5人天) [依赖: T4.1, T0.17, T0.18]
  - 描述：实现秒杀专区页，含场次切换（u-tabs scrollable）、倒计时（CountdownTimer）、库存优先排序（顶部排序栏）、2 列网格展示、立即秒杀按钮。
  - 交付物：pages-seckill/pages/seckill-zone/seckill-zone.vue。

- [ ] **T4.3** 实现秒杀执行流程（获取一次性 token → execute → 结果轮询） (预估: 1人天) [依赖: T0.12, T0.17]
  - 描述：实现秒杀执行流程：GET /seckill/{id}/token 获取一次性 token → POST /seckill/{id}/execute 携带 X-Seckill-Token 头 → 结果轮询（首次 1s，后续指数退避至 5s，上限 30 次）。
  - 交付物：秒杀执行逻辑。

- [ ] **T4.4** 实现秒杀结果页/弹窗（成功 → 跳转订单，失败 → 提示原因） (预估: 0.5人天) [依赖: T4.3]
  - 描述：实现秒杀结果展示，成功跳转订单详情，失败提示原因（含 1011 防重放拦截提示）。
  - 交付物：秒杀结果页/弹窗。

- [ ] **T4.5** 联调验证：秒杀专区 → 倒计时 → 获取 token → 执行秒杀 → 结果 → 订单 (预估: 0.5人天) [依赖: T4.2, T4.3, T4.4]
  - 描述：联调验证秒杀全流程：秒杀专区 → 倒计时 → 获取 token → 执行秒杀 → 结果 → 订单，验证防重放拦截（1011）正确提示。
  - 交付物：联调验证报告。

### 里程碑 M5：秒杀闭环

- **交付物**：秒杀专区页可用，秒杀全流程跑通。
- **验收标准**：倒计时与服务器时间对齐，秒杀 token 一次性使用，防重放拦截（1011）正确提示，秒杀成功后订单正确生成。

---

## 阶段 5：用户中心模块

> **阶段目标**：实现个人中心、收藏、优惠券、钱包、评价全部页面。
> **阶段依赖**：阶段 0、阶段 1
> **预估总工时**：4 人天
> **里程碑节点**：M6 用户中心完成

### 任务清单

- [ ] **T5.1** 实现我的页 profile.vue（用户信息卡片 + 功能入口列表） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现我的页，含用户信息卡片、功能入口列表（个人资料/收藏/优惠券/钱包/地址/订单）。
  - 交付物：pages/profile/profile.vue。

- [ ] **T5.2** 实现个人资料页 user-profile.vue（资料修改 + 头像上传 + 密码修改） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现个人资料页，含资料修改（u-form）、头像上传（uni.chooseImage + uni.uploadFile）、密码修改（u-form + 图形验证码）。
  - 交付物：pages-user/pages/user-profile/user-profile.vue。

- [ ] **T5.3** 实现收藏夹页 favorites.vue（列表 + 排序 + 管理模式 + 批量操作） (预估: 1人天) [依赖: T0.17, T0.18]
  - 描述：实现收藏夹页，含列表展示、排序（顶部排序栏 + ActionSheet）、管理模式（顶部切换 + 底部批量操作栏）、批量操作（u-checkbox）、取消收藏（u-swipe-action 左滑）。
  - 交付物：pages-user/pages/favorites/favorites.vue。

- [ ] **T5.4** 实现我的优惠券页 my-coupons.vue（列表 + 状态筛选 + 使用说明） (预估: 0.5人天) [依赖: T0.17, T0.18]
  - 描述：实现我的优惠券页，含优惠券列表（u-card）、状态筛选（u-tabs）、使用说明（u-collapse）。
  - 交付物：pages-user/pages/my-coupons/my-coupons.vue。

- [ ] **T5.5** 实现钱包余额展示（嵌入 profile 或独立页） (预估: 0.5人天) [依赖: T0.17]
  - 描述：实现钱包余额展示，嵌入 profile 页或独立页。
  - 交付物：钱包余额展示。

- [ ] **T5.6** 联调验证：个人中心 → 资料修改 → 收藏 → 优惠券 → 钱包 (预估: 0.5人天) [依赖: T5.1, T5.2, T5.3, T5.4, T5.5]
  - 描述：联调验证用户中心全流程：个人中心 → 资料修改 → 收藏 → 优惠券 → 钱包。
  - 交付物：联调验证报告。

### 里程碑 M6：用户中心完成

- **交付物**：用户中心全部页面可用。
- **验收标准**：资料修改成功，头像上传成功，收藏/取消收藏正常，优惠券列表正确。

---

## 阶段 6：联调测试与提审

> **阶段目标**：全量联调、真机测试、秒杀压测、性能优化、提审检查，完成提审。
> **阶段依赖**：阶段 0-5（M1-M6 全部达成）
> **预估总工时**：5 人天
> **里程碑节点**：M7 提审完成

### 任务清单

- [ ] **T6.1** 全量功能联调（14 页面逐项验证） (预估: 1.5人天) [依赖: M1-M6]
  - 描述：全量功能联调，对照 spec.md 第 3 章 14 页面功能点与验收标准逐项验证。
  - 交付物：全量联调报告。

- [ ] **T6.2** 真机测试（iOS + Android 多机型） (预估: 1人天) [依赖: T6.1]
  - 描述：真机测试，覆盖 iOS + Android 多机型，重点关注 rich-text 渲染、base64 图片、uni.request header、storage 行为。
  - 交付物：真机测试报告。

- [ ] **T6.3** 秒杀高并发压测（验证防重放、倒计时、结果轮询） (预估: 0.5人天) [依赖: T6.1]
  - 描述：秒杀高并发压测，验证防重放、倒计时、结果轮询，秒杀成功率 ≥ 99%。
  - 交付物：秒杀压测报告。

- [ ] **T6.4** 性能优化（分包加载、图片懒加载、骨架屏、首屏优化） (预估: 1人天) [依赖: T6.1]
  - 描述：性能优化，含分包加载、图片懒加载、骨架屏、首屏优化，达成首屏加载 ≤ 2s、主包 ≤ 1.5MB、分包 ≤ 1.5MB。
  - 交付物：性能优化报告。

- [ ] **T6.5** 配置 request 合法域名（微信开发者工具 + 公众平台） (预估: 0.5人天) [依赖: T6.1]
  - 描述：配置 request/uploadFile/downloadFile 合法域名，微信公众平台 → 开发设置 → 服务器域名，后端需 HTTPS。
  - 交付物：合法域名配置完成。

- [ ] **T6.6** 提审检查清单逐项核对 (预估: 0.5人天) [依赖: T6.4, T6.5]
  - 描述：对照 spec.md 第 5.2 节提审检查清单逐项核对：appid、合法域名、主包/分包体积、页面路径、接口 HTTPS、用户隐私、权限说明、无 console.log、无硬编码、错误处理、真机测试。
  - 交付物：提审检查报告。

### 里程碑 M7：提审完成

- **交付物**：通过全量测试的小程序包，提审检查清单全部通过。
- **验收标准**：14 页面功能验收清单全部通过，性能指标达标，提审检查清单全部通过。

---

## 里程碑汇总表

| 里程碑 | 名称 | 完成阶段 | 关键交付物 | 验收方式 |
|--------|------|----------|------------|----------|
| M1 | 基建完成 | 阶段 0 | 项目骨架 + 请求封装 | 调用公开接口成功 |
| M2 | 认证闭环 | 阶段 1 | 登录/注册/找回密码 + Token 刷新 | 登录态持久化 + 自动刷新 |
| M3 | 商品浏览可演示 | 阶段 2 | 首页/分类/商品列表/商品详情 | 浏览流程跑通 |
| M4 | 交易闭环 | 阶段 3 | 购物车/结算/订单/地址 | 完整购物流程 |
| M5 | 秒杀闭环 | 阶段 4 | 秒杀专区 + 一次性 token | 秒杀全流程 |
| M6 | 用户中心完成 | 阶段 5 | 个人中心/收藏/优惠券/钱包 | 用户中心全功能 |
| M7 | 提审完成 | 阶段 6 | 通过测试的小程序包 | 提审检查清单 |

### 里程碑依赖关系

```
M1 (阶段 0) ──┬── M2 (阶段 1) ──┬── M6 (阶段 5) ──┐
              ├── M3 (阶段 2) ──┬── M4 (阶段 3) ──┤
              │                 └── M5 (阶段 4) ──┤
              └──────────────────────────────────┴── M7 (阶段 6)
```

### 工时汇总

| 阶段 | 任务数 | 预估工时（人天） |
|------|--------|------------------|
| 阶段 0 | 19 | 4 |
| 阶段 1 | 7 | 4 |
| 阶段 2 | 7 | 5 |
| 阶段 3 | 8 | 6 |
| 阶段 4 | 5 | 4 |
| 阶段 5 | 6 | 4 |
| 阶段 6 | 6 | 5 |
| **合计** | **58** | **32** |

---

> **文档结束**
> 本 tasks.md 定义了 uni-app 端微信小程序的任务清单，按阶段 0-6 拆解 58 个任务，含依赖关系、工时预估、里程碑节点。功能规格见 `spec.md`，技术方案见 `plan.md`。