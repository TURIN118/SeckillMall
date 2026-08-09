# spec.md — uni-app 端微信小程序功能规格

> **项目名称**：秒杀商城（Seckill Mall）— uni-app 端微信小程序
> **文档类型**：功能规格（What to build）
> **版本**：v1.0
> **撰写日期**：2026-08-09
> **目标平台**：微信小程序（uni-app 编译目标 `mp-weixin`）
> **技术栈**：uni-app + Vue 3 + TypeScript + uView Plus + Pinia
> **用途**：定义"做什么"，作为开发验收的依据。不含技术实现细节（实现见 `plan.md`）。
> **内容来源**：《uni-app 端微信小程序开发计划》第 1.3、2、3.1、3.2、7.3、8.3 节

---

## 目录

- [第 1 章：全局约束](#第-1-章全局约束)
- [第 2 章：后端 API 契约](#第-2-章后端-api-契约)
- [第 3 章：功能规格（14 个页面模块）](#第-3-章功能规格14-个页面模块)
- [第 4 章：组件映射规格](#第-4-章组件映射规格)
- [第 5 章：非功能需求](#第-5-章非功能需求)

---

## 第 1 章：全局约束

> 以下约束在整个开发周期内不可违反，源自源文档第 1.3 节硬性约束清单与第 8.3 节关键约束传递。

### 1.1 硬性约束清单（C1~C7）

| 编号 | 约束 | 说明 |
|------|------|------|
| C1 | 仅适配 14 个前台 C 端页面 | 后台管理 14 个页面不纳入小程序，继续用现有 Web 端 |
| C2 | 仅微信小程序目标 | uni-app 编译目标 `mp-weixin`，不考虑其他小程序/App/H5 |
| C3 | 技术栈固定 | uni-app + Vue 3 + TypeScript + uView Plus + Pinia |
| C4 | 登录方式固定 | 账号密码 + 图形验证码，不引入微信授权登录 |
| C5 | 支付方式固定 | 现有模拟支付接口，微信支付作为后续扩展 |
| C6 | 接口复用 | 与现有 Web 前端共用同一套后端 RESTful API |
| C7 | 严格隔离 | 严禁修改 `frontend/` 与 `seckill-mall/` 任何文件 |

### 1.2 隔离原则

- 所有 uni-app 代码限定在 `miniapp/` 目录内。
- `miniapp/` 已存在但初始为空，所有新增文件均在此目录下创建。
- 严禁以任何形式修改 `frontend/`（现有 Web 前端）与 `seckill-mall/`（后端）目录下的任何文件。

### 1.3 API 复用约束

- 与现有 Web 前端共用同一套后端 RESTful API。
- 后端零改动：不新增、不修改、不删除任何后端接口。
- 所有接口前缀 `/api/v1/`。

### 1.4 登录方式约束

- 仅支持账号密码 + 图形验证码登录方式。
- 复用 `/api/v1/auth/login` 接口。
- 不引入 `wx.login` + `code2session` 微信授权登录流程。

### 1.5 支付方式约束

- 走现有模拟支付接口（`/api/v1/orders/{id}/pay` 与 `/api/v1/orders/{id}/pay-normal`）。
- 微信支付（`wx.requestPayment`）作为后续扩展项，本期不实现。

### 1.6 雪花 ID 约束

- 后端使用雪花算法生成 ID（如 `2085560004061081601`），超过 JS `Number.MAX_SAFE_INTEGER`（`2^53 - 1`）。
- 全程使用 **string 类型** 承载 ID，禁止 `Number(id)` 转换。
- URL 路径参数用 `encodeURIComponent` 编码。
- TypeScript 类型定义中所有 ID 字段显式声明为 `string`。

### 1.7 秒杀防重放约束

- 采用后端签发的一次性短时效 token 方案（已废弃前端 HMAC 签名方案）。
- 流程：`GET /api/v1/seckill/{id}/token` 获取一次性 token → `POST /api/v1/seckill/{id}/execute` 执行秒杀，请求头携带 `X-Seckill-Token: <token>`。
- 防重放拦截返回 HTTP 401 + 业务码 `1011`（REPLAY_DETECTED），**不触发 Token 刷新**，提示用户重新发起秒杀。

### 1.8 Token 刷新约束

- 采用 access_token（短时效）+ refresh_token（长时效）双 Token 机制。
- access_token 过期返回 HTTP 401 + 业务码 `1002`（UNAUTHORIZED），触发 refresh_token 刷新流程。
- 刷新接口：`POST /api/v1/auth/refresh`，body `{ refreshToken }`，返回 `{ accessToken, refreshToken }`。
- 并发锁 + 等待队列：多个请求并发触发 401 时，仅第一个请求执行刷新，其余请求入队等待，刷新成功后重试队列，刷新失败则清空 token 跳转登录。对齐 Web 端 H-F2 修复逻辑。

---

## 第 2 章：后端 API 契约

> uni-app 端请求封装必须严格对齐以下契约，否则将出现鉴权失败、秒杀失败、ID 精度丢失等严重问题。

### 2.1 统一响应结构 Result<T>

所有接口前缀 `/api/v1/`。统一响应结构：

```typescript
interface Result<T> {
  code: number;       // 业务码，200 为成功
  message: string;    // 提示信息
  data: T;            // 业务数据
  timestamp: string;  // 服务器时间，用于时间同步
}
```

### 2.2 关键业务码

| 业务码 | 含义 | uni-app 端处理 |
|--------|------|----------------|
| 200 | 成功 | 解包返回 `data` |
| 1002 | UNAUTHORIZED（Token 过期） | 触发 refresh_token 刷新流程 |
| 1011 | REPLAY_DETECTED（防重放拦截） | 不触发刷新，提示用户重新发起秒杀 |
| 其他非 200 | 业务错误 | `uni.showToast` 提示 message |

### 2.3 JWT 双 Token 认证机制

- **access_token**（短时效）：请求头 `Authorization: Bearer <access_token>`。
- **refresh_token**（长时效）：用于刷新 access_token。
- **Token 过期**：返回 HTTP 401 + 业务码 `1002`（UNAUTHORIZED）。
- **刷新接口**：`POST /api/v1/auth/refresh`，body `{ refreshToken }`，返回 `{ accessToken, refreshToken }`。
- **防重放拦截**：返回 HTTP 401 + 业务码 `1011`（REPLAY_DETECTED），**不触发刷新**，提示用户重新获取秒杀 token。

### 2.4 服务器时间同步

每个响应带 `timestamp` 字段，前端记录：

```typescript
timeOffset = serverTime - localTime
```

用于秒杀倒计时与服务端时间对齐，避免客户端时钟偏差导致倒计时错误。

### 2.5 秒杀防重放流程

```
1. GET  /api/v1/seckill/{id}/token    → 获取一次性 token
2. POST /api/v1/seckill/{id}/execute  → 执行秒杀
   请求头携带 X-Seckill-Token: <token>
```

uni-app 端 `uni.request` 的 header 可直接携带自定义头 `X-Seckill-Token`，无需共享密钥，无需 HMAC 计算。

### 2.6 关键 API 端点清单

| 模块 | 端点 | 方法 | 说明 |
|------|------|------|------|
| 认证 | `/api/v1/auth/login` | POST | 登录 |
| 认证 | `/api/v1/auth/register` | POST | 注册 |
| 认证 | `/api/v1/auth/captcha` | GET | 图形验证码 |
| 认证 | `/api/v1/auth/refresh` | POST | 刷新 token |
| 认证 | `/api/v1/auth/me` | GET | 获取当前用户 |
| 认证 | `/api/v1/auth/logout` | POST | 登出 |
| 认证 | `/api/v1/auth/password` | PUT | 修改密码 |
| 认证 | `/api/v1/auth/profile` | PUT | 修改资料 |
| 认证 | `/api/v1/auth/forgot-password/send-code` | POST | 发送重置验证码 |
| 认证 | `/api/v1/auth/forgot-password/reset` | POST | 重置密码 |
| 商品 | `/api/v1/products` | GET | 商品列表 |
| 商品 | `/api/v1/products/{id}` | GET | 商品详情 |
| 分类 | `/api/v1/categories` | GET | 分类列表 |
| 购物车 | `/api/v1/cart/list` | GET | 购物车列表 |
| 购物车 | `/api/v1/cart/add` | POST | 加入购物车 |
| 购物车 | `/api/v1/cart/{id}/quantity` | PUT | 修改数量 |
| 购物车 | `/api/v1/cart/{id}` | DELETE | 删除单项 |
| 购物车 | `/api/v1/cart/clear` | DELETE | 清空购物车 |
| 购物车 | `/api/v1/cart/{id}/selected` | PUT | 选中/取消选中 |
| 购物车 | `/api/v1/cart/batch-selected` | PUT | 批量选中 |
| 购物车 | `/api/v1/cart/count` | GET | 购物车数量 |
| 订单 | `/api/v1/orders` | GET | 订单列表 |
| 订单 | `/api/v1/orders/unified` | GET | 统一订单列表 |
| 订单 | `/api/v1/orders/{id}` | GET | 订单详情 |
| 订单 | `/api/v1/orders/{id}/pay` | POST | 支付 |
| 订单 | `/api/v1/orders/{id}/pay-normal` | POST | 普通订单支付 |
| 订单 | `/api/v1/orders/{id}/cancel` | POST | 取消订单 |
| 订单 | `/api/v1/orders/{id}/cancel-normal` | POST | 取消普通订单 |
| 订单 | `/api/v1/orders/{id}/confirm` | POST | 确认收货 |
| 订单 | `/api/v1/orders/{id}/confirm-normal` | POST | 确认收货（普通） |
| 订单 | `/api/v1/orders` | POST | 创建订单 |
| 订单 | `/api/v1/orders/from-cart` | POST | 从购物车创建 |
| 订单 | `/api/v1/orders/{id}/normal-detail` | GET | 普通订单详情 |
| 秒杀 | `/api/v1/seckill/list` | GET | 秒杀列表 |
| 秒杀 | `/api/v1/seckill/{id}` | GET | 秒杀详情 |
| 秒杀 | `/api/v1/seckill/{id}/stock` | GET | 秒杀库存 |
| 秒杀 | `/api/v1/seckill/{id}/token` | GET | 获取一次性 token |
| 秒杀 | `/api/v1/seckill/{id}/execute` | POST | 执行秒杀 |
| 秒杀 | `/api/v1/seckill/{id}/result` | GET | 秒杀结果 |
| 秒杀 | `/api/v1/seckill/activities` | GET | 秒杀活动 |
| 收藏 | `/api/v1/favorites` | GET/POST/DELETE | 收藏夹 |
| 地址 | `/api/v1/users/addresses` 或 `/api/v1/addresses` | CRUD | 收货地址 |
| 优惠券 | `/api/v1/coupons` | GET | 优惠券 |
| 钱包 | `/api/v1/wallet` | GET | 钱包余额 |
| 轮播 | `/api/v1/banners` | GET | 轮播图（公开） |
| 评价 | `/api/v1/reviews` | GET/POST | 评价 |
| 上传 | `/api/v1/upload` | POST | 文件上传（uni.uploadFile） |
| 验证码 | `/api/v1/verification` | POST | 验证码 |

---

## 第 3 章：功能规格（14 个页面模块）

> 14 个前台页面与 Web 端一一对应，交互体验符合移动端习惯。每个页面作为一个 spec 模块，格式统一。

### 3.1 首页 Home

- **路由**：`pages/home/home`
- **鉴权**：否
- **tabBar**：是（首页 tab）
- **功能点**：
  - Banner 轮播（自动轮播 + 手动滑动 + 点击跳转）
  - 秒杀专区入口（显示进行中场次 + 点击跳转秒杀专区）
  - 商品分类导航（横向滚动 + 点击跳转商品列表）
  - 猜你喜欢（触底加载 + 分类筛选 + 无限滚动）
  - 分类筛选（顶部筛选栏 + 底部弹出 ActionSheet）
- **UI 组件**：u-swiper（mode="dot"）、u-card、u-tabs（scrollable）、u-loadmore、u-action-sheet
- **API 依赖**：`GET /api/v1/banners`、`GET /api/v1/seckill/activities`、`GET /api/v1/categories`、`GET /api/v1/products`
- **验收标准**：
  - Banner 自动轮播、手动滑动、点击跳转均正常
  - 秒杀入口显示进行中场次，点击跳转秒杀专区
  - 分类导航横向滚动，点击跳转商品列表并带分类参数
  - 猜你喜欢触底加载更多，无数据时显示"没有更多了"
  - 分类筛选底部弹出，筛选生效后列表更新
- **适配要点**：
  - el-carousel → u-swiper（自带指示器，无需自定义）
  - 无限滚动 + IntersectionObserver → 触底加载 onReachBottom + u-loadmore
  - 顶部筛选栏 → 顶部筛选栏 + 底部弹出 ActionSheet（移动端空间有限）

### 3.2 商品列表 ProductList

- **路由**：`pages-product/pages/product-list/product-list`（商品分包）
- **鉴权**：否
- **tabBar**：否
- **功能点**：
  - 分类筛选（顶部 u-tabs + 下拉筛选面板）
  - 价格区间筛选（双滑块）
  - 多维度排序（顶部排序栏 + ActionSheet）
  - 分页（触底加载 + u-loadmore）
  - 商品卡片（2 列网格）
- **UI 组件**：u-tabs、u-slider、u-action-sheet、u-loadmore、u-image
- **API 依赖**：`GET /api/v1/products`、`GET /api/v1/categories`
- **验收标准**：
  - 分类筛选生效，结果更新
  - 价格区间筛选生效，结果更新
  - 多维度排序生效，结果更新
  - 触底加载更多，无数据时显示"没有更多了"
  - 商品卡片 2 列网格展示
- **适配要点**：
  - 侧边栏或顶部下拉 → 顶部 u-tabs + 下拉筛选面板（移动端单栏布局）
  - el-slider 双滑块 → u-slider 双滑块（需验证 uView Plus 支持）
  - el-select 排序下拉 → 顶部排序栏 + ActionSheet（改用底部弹出选择）
  - el-pagination → 触底加载 + u-loadmore（移动端无传统分页）
  - 多列网格 → 2 列网格（移动端宽度）

### 3.3 商品详情 ProductDetail

- **路由**：`pages-product/pages/product-detail/product-detail`（商品分包）
- **鉴权**：否
- **tabBar**：否
- **功能点**：
  - 图片轮播（全宽展示 + 指示器 + 预览）
  - SKU 规格选择（底部弹出 + 规格矩阵 + 价格/库存更新）
  - 参数速览（u-cell 列表）
  - 服务保障（u-tag + u-popup 说明）
  - 商品评价（列表 + 触底加载）
  - 售后说明（u-collapse 折叠面板）
  - 富文本详情（rich-text 组件渲染）
- **UI 组件**：u-swiper、u-popup、u-cell、u-tag、u-collapse、u-loadmore、rich-text
- **API 依赖**：`GET /api/v1/products/{id}`、`GET /api/v1/reviews`
- **验收标准**：
  - 图片轮播全宽展示，指示器正常，预览可用
  - SKU 选择底部弹出，规格切换后价格/库存更新
  - 参数速览以列表形式展示
  - 服务保障标签点击弹出说明
  - 商品评价列表触底加载
  - 售后说明折叠面板展开/收起
  - 富文本详情 rich-text 渲染正确，标签过滤生效
- **适配要点**：
  - el-carousel 大图 → u-swiper + 自定义指示器（移动端全宽展示）
  - 弹窗 el-dialog + 规格矩阵 → 底部弹出 u-popup + 规格矩阵（改底部弹出符合移动习惯）
  - 表格展示 → u-cell 列表
  - 标签 + tooltip → u-tag + u-popup 说明（tooltip 改点击弹出）
  - 列表 + 分页 → 列表 + 触底加载
  - 折叠面板 → u-collapse
  - v-html 渲染 → rich-text 组件（见 spec 4.3 富文本适配）

### 3.4 购物车 Cart

- **路由**：`pages/cart/cart`
- **鉴权**：是（requiresAuth）
- **tabBar**：是（购物车 tab）
- **功能点**：
  - 商品列表展示（商品信息 + 选中状态 + 数量）
  - 左滑删除（u-swipe-action）
  - 数量修改（u-number-box 步进器 + 实时更新）
  - 批量选中（全选/反选 + 底部结算栏更新）
  - 选中结算（底部固定结算栏）
- **UI 组件**：u-checkbox、u-swipe-action、u-number-box、u-button
- **API 依赖**：`GET /api/v1/cart/list`、`POST /api/v1/cart/add`、`PUT /api/v1/cart/{id}/quantity`、`DELETE /api/v1/cart/{id}`、`DELETE /api/v1/cart/clear`、`PUT /api/v1/cart/{id}/selected`、`PUT /api/v1/cart/batch-selected`、`GET /api/v1/cart/count`
- **验收标准**：
  - 商品列表展示选中状态与数量
  - 左滑出现删除按钮，删除成功
  - 步进器数量修改实时更新
  - 全选/反选生效，底部结算栏更新
  - 底部固定结算栏显示选中商品总价
- **适配要点**：
  - el-checkbox 全选 → u-checkbox 全选 + 底部结算栏（布局调整）
  - el-table 操作列 → u-swipe-action 左滑删除（改用左滑删除交互）
  - el-pagination → 触底加载（分页方式不同）
  - 顶部结算按钮 → 底部固定结算栏（移动端底部固定）
  - el-input-number → u-number-box（组件替换）

### 3.5 秒杀专区 SeckillZone

- **路由**：`pages-seckill/pages/seckill-zone/seckill-zone`（秒杀分包）
- **鉴权**：否（浏览），是（执行秒杀）
- **tabBar**：否
- **功能点**：
  - 场次切换（横向滚动 u-tabs）
  - 倒计时（服务器时间对齐 + 毫秒级精度）
  - 库存优先排序（顶部排序栏）
  - 网格展示（2 列网格）
  - 立即秒杀按钮（u-button + 跳转确认）
  - 秒杀执行（获取一次性 token → execute → 结果轮询）
  - 防重放拦截提示（业务码 1011）
- **UI 组件**：u-tabs（scrollable）、u-button、u-image、CountdownTimer（自定义）
- **API 依赖**：`GET /api/v1/seckill/list`、`GET /api/v1/seckill/{id}`、`GET /api/v1/seckill/{id}/stock`、`GET /api/v1/seckill/{id}/token`、`POST /api/v1/seckill/{id}/execute`、`GET /api/v1/seckill/{id}/result`、`GET /api/v1/seckill/activities`
- **验收标准**：
  - 场次切换生效，倒计时更新
  - 倒计时与服务器时间对齐，毫秒级精度
  - 库存优先排序生效
  - 立即秒杀：获取 token → execute → 结果，全流程跑通
  - 重复请求被拦截，1011 提示"操作已过期，请重新发起秒杀"
  - 秒杀成功后订单正确生成，跳转订单详情
- **适配要点**：
  - el-tabs 横向 → u-tabs + scrollable（横向滚动）
  - setInterval + 服务器时间对齐 → setInterval + timeOffset（时间同步逻辑需复用）
  - 排序按钮 → 顶部排序栏（布局调整）
  - 多列网格 → 2 列网格（列数减少）
  - 秒杀执行：获取 token + execute → 同左 + X-Seckill-Token 头

### 3.6 结算 Checkout

- **路由**：`pages-order/pages/checkout/checkout`（订单分包）
- **鉴权**：是（requiresAuth）
- **tabBar**：否
- **功能点**：
  - 收货地址选择（u-cell 列表 + 跳转地址管理）
  - 支付方式选择（u-radio-group）
  - 订单备注（u-input + type="textarea"）
  - 商品清单（u-cell 列表）
  - 提交订单（底部固定提交栏）
- **UI 组件**：u-cell、u-radio-group、u-input、u-button
- **API 依赖**：`POST /api/v1/orders`、`POST /api/v1/orders/from-cart`、`GET /api/v1/users/addresses`
- **验收标准**：
  - 地址选择跳转地址列表，选择后回填
  - 支付方式选择生效
  - 订单备注可输入
  - 商品清单以列表形式展示
  - 提交订单创建成功，跳转订单详情
- **适配要点**：
  - el-select 或卡片列表 → u-cell 列表 + 跳转地址管理（改跳转选择）
  - el-radio-group → u-radio-group（组件替换）
  - el-input textarea → u-input + type="textarea"（组件替换）
  - 顶部提交按钮 → 底部固定提交栏（移动端底部固定）
  - 表格 → u-cell 列表（改用列表展示）

### 3.7 订单列表 UserOrders

- **路由**：`pages-order/pages/order-list/order-list`（订单分包）
- **鉴权**：是（requiresAuth）
- **tabBar**：否
- **功能点**：
  - 状态筛选（u-tabs 顶部 scrollable）
  - 订单类型筛选（u-tabs 或 ActionSheet）
  - 订单删除（u-swipe-action 左滑删除）
  - 详情查看（uni.navigateTo 跳转）
  - 分页（触底加载）
- **UI 组件**：u-tabs、u-swipe-action、u-loadmore
- **API 依赖**：`GET /api/v1/orders`、`GET /api/v1/orders/unified`
- **验收标准**：
  - 状态筛选生效，列表更新
  - 订单类型筛选生效
  - 左滑删除成功
  - 点击订单跳转订单详情
  - 触底加载更多
- **适配要点**：
  - el-tabs 顶部 → u-tabs 顶部 scrollable（横向滚动）
  - el-select → u-tabs 或 ActionSheet（改用 tabs）
  - el-button 操作列 → u-swipe-action 左滑删除（改左滑删除）
  - 跳转 → uni.navigateTo（跳转方式不同）
  - el-pagination → 触底加载（分页方式不同）

### 3.8 订单详情 OrderDetail

- **路由**：`pages-order/pages/order-detail/order-detail`（订单分包）
- **鉴权**：是（requiresAuth）
- **tabBar**：否
- **功能点**：
  - 收货地址展示（u-cell）
  - 商品列表（u-cell 列表）
  - 支付（u-button + 跳转支付确认）
  - 确认收货（u-button + uni.showModal 确认弹窗）
  - 取消订单
  - 订单状态（u-steps 步骤展示）
- **UI 组件**：u-cell、u-button、u-steps、u-modal
- **API 依赖**：`GET /api/v1/orders/{id}`、`GET /api/v1/orders/{id}/normal-detail`、`POST /api/v1/orders/{id}/pay`、`POST /api/v1/orders/{id}/pay-normal`、`POST /api/v1/orders/{id}/cancel`、`POST /api/v1/orders/{id}/cancel-normal`、`POST /api/v1/orders/{id}/confirm`、`POST /api/v1/orders/{id}/confirm-normal`
- **验收标准**：
  - 收货地址以 u-cell 形式展示
  - 商品列表以列表形式展示
  - 支付成功，状态更新
  - 确认收货弹窗确认后状态更新
  - 取消订单成功
  - u-steps 展示订单状态流转正确
- **适配要点**：
  - 卡片展示 → u-cell 展示（组件替换）
  - 表格 → u-cell 列表（改用列表展示）
  - el-button → u-button + 跳转支付确认（交互一致）
  - el-button + 确认弹窗 → u-button + uni.showModal（弹窗方式不同）
  - el-steps → u-steps（组件替换）

### 3.9 收藏夹 Favorites

- **路由**：`pages-user/pages/favorites/favorites`（用户分包）
- **鉴权**：是（requiresAuth）
- **tabBar**：否
- **功能点**：
  - 排序（顶部排序栏 + ActionSheet）
  - 管理模式（顶部切换 + 底部批量操作栏）
  - 批量操作（u-checkbox + 底部固定操作栏）
  - 取消收藏（u-swipe-action 左滑）
- **UI 组件**：u-checkbox、u-swipe-action、u-button
- **API 依赖**：`GET /api/v1/favorites`、`POST /api/v1/favorites`、`DELETE /api/v1/favorites`
- **验收标准**：
  - 商品列表展示，排序生效
  - 管理模式切换后底部出现批量操作栏
  - 批量操作生效
  - 左滑取消收藏，列表更新
- **适配要点**：
  - el-select → 顶部排序栏 + ActionSheet（改底部弹出）
  - 切换按钮 → 顶部切换 + 底部批量操作栏（布局调整）
  - el-checkbox + 操作按钮 → u-checkbox + 底部固定操作栏（移动端底部固定）
  - el-button → u-swipe-action 左滑（改左滑交互）

### 3.10 个人中心 UserProfile

- **路由**：`pages-user/pages/user-profile/user-profile`（用户分包）
- **鉴权**：是（requiresAuth）
- **tabBar**：否
- **功能点**：
  - 资料修改（u-form 表单提交）
  - 密码修改（u-form + 图形验证码）
  - 钱包余额展示（u-card）
  - 收货地址管理（uni.navigateTo 跳转地址管理页）
  - 优惠券（u-tabs 切换）
  - 头像上传（uni.chooseImage + uni.uploadFile）
- **UI 组件**：u-form、u-form-item、u-input、u-card、u-tabs、u-image
- **API 依赖**：`GET /api/v1/auth/me`、`PUT /api/v1/auth/profile`、`PUT /api/v1/auth/password`、`GET /api/v1/wallet`、`GET /api/v1/coupons`、`POST /api/v1/upload`
- **验收标准**：
  - 资料修改表单提交成功，更新生效
  - 密码修改需验证码，修改成功
  - 钱包余额展示正确
  - 收货地址跳转地址管理页
  - 优惠券 tab 切换生效
  - 头像上传：选图 + 上传 + 更新成功
- **适配要点**：
  - el-form → u-form（组件替换）
  - el-form + 验证码 → u-form + 图形验证码（验证码适配见 spec 4.4）
  - 卡片展示 → u-card 展示（组件替换）
  - 跳转管理页 → uni.navigateTo 地址管理（跳转方式不同）
  - Tab 切换 → u-tabs 切换（组件替换）
  - el-upload → uni.chooseImage + uni.uploadFile（上传方式不同）

### 3.11 我的优惠券 MyCoupons

- **路由**：`pages-user/pages/my-coupons/my-coupons`（用户分包）
- **鉴权**：是（requiresAuth）
- **tabBar**：否
- **功能点**：
  - 优惠券列表（u-card 列表）
  - 状态筛选（u-tabs）
  - 使用说明（u-collapse 折叠面板）
- **UI 组件**：u-card、u-tabs、u-collapse
- **API 依赖**：`GET /api/v1/coupons`
- **验收标准**：
  - 优惠券列表展示正确
  - 状态筛选生效
  - 使用说明折叠面板展开/收起
- **适配要点**：
  - 卡片列表 → u-card 列表（组件替换）
  - el-tabs → u-tabs（组件替换）
  - 折叠面板 → u-collapse（组件替换）

### 3.12 登录 Login

- **路由**：`pages/login/login`
- **鉴权**：否
- **tabBar**：否
- **功能点**：
  - 账号/邮箱/手机号输入（u-input）
  - 密码输入
  - 图形验证码（image base64 或临时路径 + 点击刷新）
  - 记住我（u-checkbox + uni.setStorageSync）
  - 登录提交（u-button）
  - 登录后跳转（uni.switchTab 跳转首页 tab）
- **UI 组件**：u-form、u-form-item、u-input、u-button、u-checkbox、image
- **API 依赖**：`POST /api/v1/auth/login`、`GET /api/v1/auth/captcha`
- **验收标准**：
  - 账号 + 密码 + 验证码登录成功
  - 图形验证码点击刷新
  - 勾选"记住我"后下次自动填充
  - 登录成功后 Token 持久化，关闭小程序重开仍保持登录态
  - 登录后跳转首页 tab
- **适配要点**：
  - el-input → u-input（组件替换）
  - img src 直接请求 → image base64 或临时路径（见 spec 4.4 验证码适配）
  - el-checkbox → u-checkbox + uni.setStorageSync（存储方式不同）
  - el-button → u-button（组件替换）
  - router.push → uni.switchTab（首页 tab，跳转方式不同）

### 3.13 注册 Register

- **路由**：`pages/register/register`
- **鉴权**：否
- **tabBar**：否
- **功能点**：
  - 用户注册表单（u-form）
  - 图形验证码 + 短信验证码
  - 协议同意（u-checkbox + uni.navigateTo 协议页）
  - 注册提交
- **UI 组件**：u-form、u-form-item、u-input、u-button、u-checkbox、image
- **API 依赖**：`POST /api/v1/auth/register`、`GET /api/v1/auth/captcha`、`POST /api/v1/verification`
- **验收标准**：
  - 注册表单填写完整
  - 图形验证码 + 短信验证码验证通过
  - 协议同意勾选后方可提交
  - 注册成功后跳转登录页
- **适配要点**：
  - el-form → u-form（组件替换）
  - 图形验证码 + 短信验证码 → 同左 + 适配（见 spec 4.4）
  - el-checkbox + 协议链接 → u-checkbox + uni.navigateTo 协议页（协议页需新建）

### 3.14 找回密码 ForgotPassword

- **路由**：`pages/forgot-password/forgot-password`
- **鉴权**：否
- **tabBar**：否
- **功能点**：
  - 邮箱/手机验证码（u-form + 发送按钮 + 倒计时）
  - 重置密码表单（u-form）
  - 提交（u-button）
- **UI 组件**：u-form、u-form-item、u-input、u-button
- **API 依赖**：`POST /api/v1/auth/forgot-password/send-code`、`POST /api/v1/auth/forgot-password/reset`
- **验收标准**：
  - 发送验证码成功，按钮进入倒计时
  - 倒计时结束后可重新发送
  - 重置密码提交成功，跳转登录页
- **适配要点**：
  - el-form + 发送按钮 → u-form + 倒计时按钮（倒计时逻辑复用）
  - el-form → u-form（组件替换）
  - el-button → u-button（组件替换）

---

## 第 4 章：组件映射规格

> Element Plus → uView Plus 完整映射表，开发时严格按此映射替换组件。

### 4.1 组件映射表

| Element Plus 组件 | uView Plus 组件 | 适配说明 | 注意事项 |
|-------------------|-----------------|----------|----------|
| `el-button` | `u-button` | 直接替换 | type/size/plain 参数对应，事件 `@click` 一致 |
| `el-input` | `u-input` | 直接替换 | `v-model` 一致，textarea 用 `type="textarea"` |
| `el-form` | `u-form` | 直接替换 | 校验规则格式需对齐 uView Plus |
| `el-form-item` | `u-form-item` | 直接替换 | label 属性一致 |
| `el-select` | `u-picker` 或 `u-action-sheet` | 选择器替换 | 单选用 u-picker，少量选项用 u-action-sheet |
| `el-option` | u-picker 的 columns 配置 | 数据结构不同 | 需转换 options 为 columns 数组 |
| `el-radio` / `el-radio-group` | `u-radio` / `u-radio-group` | 直接替换 | 事件名一致 |
| `el-checkbox` / `el-checkbox-group` | `u-checkbox` / `u-checkbox-group` | 直接替换 | 事件名一致 |
| `el-switch` | `u-switch` | 直接替换 | v-model 一致 |
| `el-table` | 列表 + `u-cell` + `u-swipe-action` | 表格改列表 | 移动端无表格，改纵向列表 |
| `el-pagination` | `u-loadmore` + 触底加载 | 分页改触底 | 配合 `onReachBottom` 生命周期 |
| `el-dialog` | `u-popup` 或 `u-modal` | 弹窗替换 | 简单确认用 u-modal，复杂内容用 u-popup |
| `el-message` | `uni.showToast` | 全局提示 | 不需组件，调用 API |
| `el-message-box` | `uni.showModal` | 确认弹窗 | 不需组件，调用 API |
| `el-notification` | `uni.showToast`（简化） | 通知简化 | 小程序无复杂通知，简化为 toast |
| `el-carousel` | `u-swiper` | 轮播替换 | 指示器 mode 配置 |
| `el-tabs` | `u-tabs` | 直接替换 | scrollable 属性用于横向滚动 |
| `el-image` | `u-image` | 直接替换 | lazy-load 属性启用懒加载 |
| `el-tag` | `u-tag` | 直接替换 | type/text 属性对应 |
| `el-card` | `u-card` | 直接替换 | slot 用法略有不同 |
| `el-steps` | `u-steps` | 直接替换 | current 属性对应 |
| `el-collapse` | `u-collapse` | 直接替换 | 折叠面板替换 |
| `el-collapse-item` | `u-collapse-item` | 直接替换 | - |
| `el-tooltip` | `u-popup`（点击触发） | tooltip 改弹出 | 移动端无 hover，改点击 |
| `el-dropdown` | `u-action-sheet` | 下拉改底部弹出 | 移动端习惯 |
| `el-slider` | `u-slider` | 直接替换 | 双滑块需验证 uView Plus 支持 |
| `el-input-number` | `u-number-box` | 数量选择器 | 步进器替换 |
| `el-upload` | `uni.chooseImage` + `uni.uploadFile` | 上传改 API | 见 spec 4.5 文件上传适配 |
| `el-backtop` | 自定义 + `uni.pageScrollTo` | 回顶自定义 | 用 API 实现 |
| `el-skeleton` | `u-skeleton` | 骨架屏替换 | 加载态替换 |
| `el-empty` | `u-empty` | 空状态替换 | - |
| `el-divider` | `u-divider` | 分割线替换 | - |
| `el-badge` | `u-badge` | 徽标替换 | - |
| `el-avatar` | `u-avatar` | 头像替换 | - |
| `el-row` / `el-col` | flex 布局 + 自定义 | 栅格改 flex | uni-app 推荐用 flex 布局 |
| `el-breadcrumb` | 自定义或省略 | 面包屑简化 | 移动端通常省略面包屑 |
| `el-date-picker` | `u-datetime-picker` | 日期选择器 | - |
| `el-cascader` | 自定义级联选择 | 级联选择 | uView Plus 无直接对应，需自定义 |

### 4.2 交互差异适配（PC → 移动端）

| 交互场景 | PC 端（鼠标） | 移动端（触控） | 适配方案 |
|----------|---------------|----------------|----------|
| 列表刷新 | 手动刷新按钮 | 下拉刷新 | `onPullDownRefresh` 生命周期 + `uni.stopPullDownRefresh` |
| 列表加载更多 | 分页按钮 | 触底加载 | `onReachBottom` 生命周期 + u-loadmore |
| 删除项 | 操作列按钮 | 左滑删除 | u-swipe-action 组件 |
| 选项选择 | 下拉框 | 底部弹出 ActionSheet | u-action-sheet |
| 弹窗确认 | el-message-box | uni.showModal | 调用 API |
| 全局提示 | el-message | uni.showToast | 调用 API |
| hover 效果 | :hover 伪类 | :active 伪类 | CSS 适配 |
| 右键菜单 | @contextmenu | 长按事件 | @longpress |
| 图片预览 | el-image preview | uni.previewImage | 调用 API |
| 复制文本 | execCommand | uni.setClipboardData | 调用 API |
| 滚动到顶 | el-backtop | uni.pageScrollTo | 调用 API |

### 4.3 富文本商品详情渲染

- **Web 端**：商品详情使用 WangEditor 生成的 HTML，通过 `v-html` 渲染。
- **小程序端**：小程序不支持 `v-html`，需使用 `rich-text` 组件。
- **适配方案**：将后端返回的 HTML 字符串赋值给 `rich-text` 的 `nodes` 属性，需过滤不支持的标签（script、style、link 等），并将 class 选择器转为 inline style。
- **支持的标签**：`rich-text` 组件支持 `a`、`abbr`、`b`、`bdi`、`bdo`、`br`、`cite`、`code`、`col`、`colgroup`、`dd`、`del`、`div`、`dl`、`dt`、`em`、`fieldset`、`font`、`h1`-`h6`、`hr`、`i`、`img`、`ins`、`label`、`legend`、`li`、`ol`、`p`、`q`、`s`、`span`、`strong`、`sub`、`sup`、`table`、`tbody`、`td`、`tfoot`、`th`、`thead`、`tr`、`u`、`ul`。
- **注意**：`rich-text` 不支持 `class` 选择器，所有样式需转为 inline style。若富文本复杂度高，可考虑使用第三方插件 `mp-html`（uni-app 插件市场）。

### 4.4 图形验证码适配

- **Web 端**：`<img :src="captchaUrl" @click="refreshCaptcha" />`，`captchaUrl` 直接指向 `/api/v1/auth/captcha`。
- **小程序端**：小程序 `<image>` 不支持直接请求需要鉴权的接口，且小程序 image src 不支持携带自定义请求头。
- **适配方案**：后端 `/api/v1/auth/captcha` 改为返回 base64 图片字符串（若后端已返回 base64 则直接用；若返回图片流，则需通过 `uni.request` 获取 arraybuffer 转 base64）。
- **方案 A（推荐，后端已返回 base64）**：请求验证码接口，获取 base64 字符串，拼接 `data:image/png;base64,` 前缀后赋值给 image 的 src。
- **方案 B（后端返回图片流）**：用 `uni.request` 获取 arraybuffer，通过 `uni.arrayBufferToBase64` 转 base64，再拼接前缀。
- **注意**：需确认后端 `/api/v1/auth/captcha` 返回格式。若返回 `{ img: base64, key: string }`，用方案 A；若返回图片流，用方案 B。此环节需在阶段 0 联调时确认，不修改后端。

### 4.5 文件上传适配

- **Web 端**：`FormData` + Axios。
- **小程序端**：`uni.uploadFile`。
- **适配方案**：通过 `uni.chooseImage` 选择图片获取临时文件路径，通过 `uni.uploadFile` 上传至后端 `/api/v1/upload` 接口，请求头携带 `Authorization: Bearer <token>`。

---

## 第 5 章：非功能需求

### 5.1 性能指标

| 指标 | 目标值 | 测量方式 |
|------|--------|----------|
| 首屏加载时间 | ≤ 2s | 微信开发者工具 Performance |
| 接口响应时间 | ≤ 1s（常规）/ ≤ 500ms（秒杀） | 接口监控 |
| 秒杀成功率 | ≥ 99%（防重放拦截除外） | 秒杀压测统计 |
| 主包体积 | ≤ 1.5MB（预留 0.5MB 余量） | 微信开发者工具构建产物 |
| 分包体积 | 每个分包 ≤ 1.5MB | 微信开发者工具构建产物 |
| Token 刷新无感知 | 刷新期间用户无感 | 人工验证 |

### 5.2 提审检查清单

| 检查项 | 检查内容 | 通过标准 |
|--------|----------|----------|
| appid | manifest.json 中 appid 为正式 appid | 非测试号 |
| 合法域名 | request/uploadFile/downloadFile 合法域名已配置 | 微信公众平台配置完成 |
| 主包体积 | 主包 ≤ 2MB | 微信开发者工具构建检查 |
| 分包体积 | 每个分包 ≤ 2MB，总包 ≤ 20MB | 微信开发者工具构建检查 |
| 页面路径 | pages.json 中所有页面路径存在对应文件 | 无 404 |
| 接口域名 | 所有接口走 HTTPS | 无 HTTP 接口 |
| 用户隐私 | 隐私政策页面已配置 | 含隐私协议 |
| 权限说明 | 所用权限（如定位）有 desc 说明 | manifest.json permission 配置 |
| 无测试代码 | console.log 已移除 | 代码搜索无 console.log |
| 无硬编码 | API base URL 走环境变量 | 无硬编码地址 |
| 错误处理 | 所有接口有错误处理 | 无未捕获异常 |
| 真机测试 | iOS + Android 真机测试通过 | 多机型验证 |

### 5.3 隔离与复用约束

- 所有代码限定 `miniapp/` 目录，严禁修改 `frontend/` 与 `seckill-mall/`。
- 与现有 Web 前端共用同一套后端 RESTful API，零后端改动。
- 登录方式：账号密码 + 图形验证码，不引入微信授权登录。
- 支付方式：现有模拟支付接口，微信支付作为后续扩展。
- 雪花 ID：全程 string 类型 + encodeURIComponent。
- 秒杀防重放：一次性 token 方案，X-Seckill-Token 头传递。
- Token 刷新：并发锁 + 等待队列，对齐 Web 端 H-F2 修复逻辑。

---

> **文档结束**
> 本 spec.md 定义了 uni-app 端微信小程序的功能规格，含全局约束、API 契约、14 个页面模块、组件映射与非功能需求。技术实现见 `plan.md`，任务执行顺序见 `tasks.md`。