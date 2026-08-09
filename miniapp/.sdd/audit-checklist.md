# 提审检查清单 — uni-app 微信小程序端

> 阶段 6：联调测试与提审（T6.6 提审检查）
>
> 生成时间：2026-08-09
> 对齐文档：`spec.md` 第 5.2 节提审检查清单，`plan.md` 第 3.4 节关键配置说明 / 第 8.1 节风险矩阵
> 适用范围：`miniapp/` 目录（严禁修改 `frontend/` 与 `seckill-mall/`）

---

## 0. 提审前必做事项总览

| 序号 | 检查项 | 当前状态 | 操作指引 | 优先级 |
|------|--------|----------|----------|--------|
| 1 | appid 配置 | ⚠️ 占位符 | 替换为正式 appid | 高 |
| 2 | request 合法域名 | ⚠️ 未配置 | 微信公众平台配置 | 高 |
| 3 | 主包/分包体积 | ⏳ 待构建 | 微信开发者工具构建检查 | 高 |
| 4 | 页面路径完整性 | ✅ 通过 | 已验证（见 test-report.md 第 3 节） | 高 |
| 5 | 接口 HTTPS | ✅ 生产已配置 | 验证后端 HTTPS 证书 | 高 |
| 6 | 用户隐私政策 | ⚠️ 未配置 | 配置隐私政策页面 | 高 |
| 7 | 权限说明 | ✅ 已配置 | manifest.json permission 已配 desc | 中 |
| 8 | console.log 清理 | ⚠️ 3 处 | App.vue 移除 console.log | 中 |
| 9 | 硬编码检查 | ✅ 通过 | API base URL 走环境变量 | 中 |
| 10 | 错误处理检查 | ✅ 通过 | 所有接口有 try-catch | 中 |
| 11 | 真机测试 | ⏳ 待执行 | iOS + Android 多机型 | 高 |

---

## 1. appid 配置检查

### 1.1 检查内容

`manifest.json` 中 `mp-weixin.appid` 必须为正式 appid（非测试号、非占位符）。

### 1.2 当前状态

```json
// src/manifest.json 第 9-10 行
"mp-weixin": {
  "appid": "wxXXXXXXXXXXXXXXX",  // ⚠️ 占位符，需替换
```

### 1.3 操作指引

1. 登录 [微信公众平台](https://mp.weixin.qq.com/) → 开发 → 开发管理 → 开发设置，获取 AppID
2. 替换 `src/manifest.json` 第 10 行 `wxXXXXXXXXXXXXXXX` 为正式 AppID
3. 同步替换根 `appid` 字段（第 3 行，目前为空字符串）

### 1.4 通过标准

- `mp-weixin.appid` 为正式 AppID（格式 `wx` + 16 位字符）
- 非 `touristappid`、非占位符、非空

---

## 2. request 合法域名配置

### 2.1 检查内容

微信公众平台 → 开发设置 → 服务器域名，需配置以下合法域名：

| 域名类型 | 用途 | 配置值 |
|----------|------|--------|
| request 合法域名 | uni.request 网络请求 | `https://api.seckill-mall.com` |
| uploadFile 合法域名 | uni.uploadFile 文件上传 | `https://api.seckill-mall.com` |
| downloadFile 合法域名 | uni.downloadFile 文件下载（若有） | `https://api.seckill-mall.com` |
| socket 合法域名 | WebSocket（本项目未用） | 不配置 |

### 2.2 当前状态

- `src/manifest.json` 第 12 行 `"urlCheck": false`（开发期关闭域名校验）⚠️
- 生产环境 API 地址：`https://api.seckill-mall.com`（`env/.env.production`）✅

### 2.3 操作指引

1. 登录微信公众平台 → 开发 → 开发管理 → 开发设置 → 服务器域名
2. request 合法域名添加：`https://api.seckill-mall.com`
3. uploadFile 合法域名添加：`https://api.seckill-mall.com`
4. downloadFile 合法域名添加：`https://api.seckill-mall.com`（若有下载需求）
5. 提审前将 `manifest.json` 第 12 行 `urlCheck` 改为 `true`

### 2.4 通过标准

- 微信公众平台服务器域名配置完成
- `urlCheck: true`（提审前）
- 后端域名必须为 HTTPS，证书有效

### 2.5 风险提示（对齐 plan.md R2）

> 开发期关闭 urlCheck；上线前在微信公众平台配置 request/uploadFile/downloadFile 合法域名；后端需 HTTPS。否则真机请求会被拦截。

---

## 3. 主包/分包体积检查

### 3.1 检查内容

| 包类型 | 微信限制 | spec 目标（预留余量） |
|--------|----------|----------------------|
| 主包 | ≤ 2MB | ≤ 1.5MB |
| 单个分包 | ≤ 2MB | ≤ 1.5MB |
| 总包体积 | ≤ 20MB | ≤ 16MB |

### 3.2 当前状态

⏳ 需运行 `npm run build:mp-weixin` 后，在微信开发者工具中查看构建产物体积。

### 3.3 操作指引

1. 执行构建：`npm run build:mp-weixin`（在 `miniapp/` 目录）
2. 用微信开发者工具打开 `miniapp/dist/build/mp-weixin`
3. 点击工具菜单 → 构建 → 上传，查看体积分布
4. 若主包超 1.5MB，检查 `static/` 目录资源，必要时移至分包或 CDN
5. 若分包超 1.5MB，检查分包内资源，必要时拆分

### 3.4 分包配置回顾

| 分包 | root | 页面数 | 预估体积 |
|------|------|--------|----------|
| 商品分包 | `pages-product` | 2 | 中（含商品详情富文本） |
| 订单分包 | `pages-order` | 3 | 中 |
| 秒杀分包 | `pages-seckill` | 1 | 低 |
| 用户分包 | `pages-user` | 7 | 中 |

### 3.5 预加载配置（已配置）

```json
// pages.json 第 176-185 行
"preloadRule": {
  "pages/home/home": { "network": "all", "packages": ["pages-product", "pages-seckill"] },
  "pages/profile/profile": { "network": "all", "packages": ["pages-user", "pages-order"] }
}
```

### 3.6 通过标准

- 主包 ≤ 1.5MB（spec 目标）/ ≤ 2MB（微信硬限制）
- 每个分包 ≤ 1.5MB（spec 目标）/ ≤ 2MB（微信硬限制）
- 总包 ≤ 20MB

---

## 4. 页面路径完整性

### 4.1 检查内容

`pages.json` 中所有 pages 和 subPackages 声明的页面路径必须有对应 .vue 文件。

### 4.2 当前状态

✅ **已通过**。详见 `test-report.md` 第 3 节。

- 主包 7 个页面：全部匹配
- 4 个分包 13 个页面：全部匹配
- tabBar 4 个 tab：全部匹配
- preloadRule 2 个触发页：全部匹配

### 4.3 通过标准

- pages.json 中所有路径存在对应 .vue 文件
- 无 404 风险

---

## 5. 接口 HTTPS 检查

### 5.1 检查内容

所有接口请求必须走 HTTPS（微信小程序强制要求）。

### 5.2 当前状态

| 环境 | API_BASE_URL | 协议 | 状态 |
|------|--------------|------|------|
| 开发 | `http://localhost:8080` | HTTP | ℹ️ 开发期可接受（urlCheck: false） |
| 生产 | `https://api.seckill-mall.com` | HTTPS | ✅ |

### 5.3 操作指引

1. 确认后端生产环境已部署 HTTPS 证书
2. 确认 `env/.env.production` 中 `UNI_API_BASE_URL=https://api.seckill-mall.com`
3. 验证证书有效性：浏览器访问 `https://api.seckill-mall.com` 无证书警告
4. 检查代码中无硬编码 HTTP 接口地址（已检查，仅 `home.vue` 第 332 行 `'http://placeholder'` 用作 URL 解析 base，非实际请求）

### 5.4 通过标准

- 生产环境所有接口走 HTTPS
- 证书有效且未过期
- 代码中无硬编码 HTTP 接口地址

---

## 6. 用户隐私政策

### 6.1 检查内容

微信小程序提审要求配置用户隐私政策。

### 6.2 当前状态

⚠️ **未配置**。`manifest.json` 中无 `__usePrivacyCheck__` 配置，项目中无隐私政策页面。

### 6.3 操作指引

1. 在微信公众平台 → 设置 → 服务内容声明 → 用户隐私保护，配置隐私政策
2. 或在 `manifest.json` 中添加 `"__usePrivacyCheck__": true`
3. 创建隐私政策页面（如 `pages/privacy/privacy.vue`），或在 `pages.json` 中配置
4. 隐私政策需说明收集的信息：账号、头像、收货地址、定位（chooseAddress）

### 6.4 涉及的用户信息

| 信息类型 | 使用场景 | 对应权限 |
|----------|----------|----------|
| 账号密码 | 登录注册 | 无需特殊权限 |
| 用户头像昵称 | 个人资料 | `chooseImage`（已用） |
| 收货地址 | 下单 | `chooseAddress`（已声明 requiredPrivateInfos） |
| 地理位置 | 地址定位 | `scope.userLocation`（已声明 permission） |

### 6.5 通过标准

- 微信公众平台隐私政策配置完成
- 或项目内含隐私政策页面
- 所收集信息在政策中明确说明

---

## 7. 权限说明

### 7.1 检查内容

`manifest.json` 中所用权限（如定位）必须有 `desc` 说明。

### 7.2 当前状态

✅ **已通过**。

```json
// src/manifest.json 第 47-52 行
"permission": {
  "scope.userLocation": {
    "desc": "你的位置信息将用于收货地址定位"
  }
},
"requiredPrivateInfos": ["chooseAddress"]
```

### 7.3 涉及权限清单

| 权限 | desc 说明 | 使用场景 | 状态 |
|------|-----------|----------|------|
| `scope.userLocation` | 你的位置信息将用于收货地址定位 | 地址编辑页 chooseLocation | ✅ |
| `chooseAddress`（requiredPrivateInfos） | - | 地址选择 | ✅ |
| `chooseImage`（隐式） | - | 头像上传、评价图片 | ✅（无需 desc） |

### 7.4 通过标准

- 所有 `scope.*` 权限有 desc 说明
- `requiredPrivateInfos` 声明所有使用的私有 API

---

## 8. console.log 清理

### 8.1 检查内容

提审前移除所有 `console.log`（`console.error`/`console.warn` 可保留用于错误监控）。

### 8.2 当前状态

⚠️ **3 处 console.log 需清理**。

| 位置 | 代码 | 处理建议 |
|------|------|----------|
| `src/App.vue` 第 7 行 | `console.log('App Launch')` | 移除或改为条件编译 |
| `src/App.vue` 第 18 行 | `console.log('App Show')` | 移除或改为条件编译 |
| `src/App.vue` 第 22 行 | `console.log('App Hide')` | 移除或改为条件编译 |

### 8.3 console.error/console.warn 统计

- `console.error`：约 60 处（保留，用于错误监控）
- `console.warn`：3 处（保留，用于警告）

### 8.4 操作指引

**方案 A（推荐）**：直接移除 3 处 console.log

**方案 B**：改为条件编译

```vue
// #ifdef MP-WEIXIN-DEBUG
console.log('App Launch')
// #endif
```

### 8.5 通过标准

- 代码搜索无 `console.log(`（`console.error`/`console.warn` 除外）

---

## 9. 硬编码检查

### 9.1 检查内容

API base URL 必须走环境变量，无硬编码地址。

### 9.2 当前状态

✅ **已通过**。

| 检查项 | 位置 | 状态 |
|--------|------|------|
| API base URL | `utils/env.ts` 第 8 行 `process.env.UNI_API_BASE_URL \|\| 'http://localhost:8080'` | ✅ 走环境变量（fallback 为开发默认值） |
| API 前缀 | `utils/env.ts` 第 10 行 `process.env.UNI_API_PREFIX \|\| '/api/v1'` | ✅ 走环境变量 |
| 超时时间 | `utils/env.ts` 第 12 行 `Number(process.env.UNI_TIMEOUT) \|\| 10000` | ✅ 走环境变量 |
| 环境文件 | `env/.env.development` + `env/.env.production` | ✅ 已配置 |

### 9.3 硬编码扫描结果

| 位置 | 内容 | 性质 | 处理 |
|------|------|------|------|
| `utils/env.ts` 第 8 行 | `'http://localhost:8080'` | fallback 默认值 | ✅ 保留（环境变量优先） |
| `pages/home/home.vue` 第 332 行 | `'http://placeholder'` | URL 解析 base | ✅ 保留（非实际请求） |

### 9.4 通过标准

- API base URL 走环境变量
- 无硬编码生产地址
- fallback 默认值仅用于开发环境

---

## 10. 错误处理检查

### 10.1 检查内容

所有接口调用必须有错误处理（try-catch 或 .catch），无未捕获异常。

### 10.2 当前状态

✅ **已通过**。

| 检查项 | 状态 | 说明 |
|--------|------|------|
| request.ts 全局错误处理 | ✅ | HTTP 401/403/429/5xx 全部处理 + showToast |
| 业务码校验 | ✅ | `code !== 200` 报错 reject |
| 网络异常 | ✅ | `fail` 回调 showToast + reject |
| 页面级 try-catch | ✅ | 约 60 处 console.error 覆盖所有接口调用 |
| Token 刷新失败处理 | ✅ | 清空 token + 跳转登录 |
| 防重放拦截处理 | ✅ | 1011 提示"操作已过期" |

### 10.3 错误处理模式示例

```typescript
// 页面级错误处理模式（所有页面一致）
try {
  const res = await api.someRequest()
  // 处理成功
} catch (e) {
  console.error('操作失败', e)
  // showToast 已由 request.ts 全局处理
}
```

### 10.4 通过标准

- 所有接口有 try-catch
- 无未捕获 Promise rejection
- 错误提示用户友好

---

## 11. 真机测试要求

### 11.1 检查内容

iOS + Android 真机测试通过，多机型验证。

### 11.2 测试机型建议

| 平台 | 机型 | 系统版本 | 优先级 |
|------|------|----------|--------|
| iOS | iPhone 8 | iOS 16+ | 高 |
| iOS | iPhone 12 | iOS 17+ | 高 |
| iOS | iPhone 14/15 | iOS 18+ | 高 |
| Android | 华为 Mate 系列 | HarmonyOS / EMUI | 高 |
| Android | 小米 13/14 | MIUI | 高 |
| Android | OPPO/vivo | ColorOS / OriginOS | 中 |

### 11.3 重点验证项（对齐 plan.md R15）

| 验证项 | 风险点 | 验证方法 |
|--------|--------|----------|
| rich-text 富文本渲染 | 各端渲染差异 | 商品详情页富文本 |
| base64 图片渲染 | 验证码图片 | 登录页图形验证码 |
| uni.request header 传递 | X-Seckill-Token 头 | 秒杀执行流程 |
| storage 行为 | Token 持久化 | 冷启动登录态恢复 |
| 左滑删除 | u-swipe-action 兼容性 | 购物车页 |
| 底部弹出层 | u-popup 兼容性 | 商品详情 SKU 选择 |
| 分包预加载 | preloadRule 生效 | 首页→商品详情跳转 |
| 时间校准 | getTimeOffset 精度 | 秒杀倒计时 |

### 11.4 操作指引

1. 执行 `npm run build:mp-weixin` 构建产物
2. 微信开发者工具打开 `dist/build/mp-weixin`
3. 点击"预览"生成二维码，手机扫码体验
4. 逐机型验证第 11.3 节所有项目
5. 记录问题并修复，重新验证

### 11.5 通过标准

- iOS + Android 多机型测试通过
- 第 11.3 节所有重点验证项无异常
- 无白屏、无崩溃、无功能缺失

---

## 12. 提审前最终检查流程

### 12.1 提审前 Checklist（按顺序执行）

- [ ] **1. appid 替换**：`manifest.json` 中 `mp-weixin.appid` 替换为正式 AppID
- [ ] **2. 合法域名配置**：微信公众平台配置 request/uploadFile/downloadFile 合法域名
- [ ] **3. urlCheck 开启**：`manifest.json` 中 `urlCheck` 改为 `true`
- [ ] **4. 隐私政策配置**：微信公众平台配置用户隐私保护
- [ ] **5. console.log 清理**：移除 `App.vue` 中 3 处 console.log
- [ ] **6. 构建产物**：`npm run build:mp-weixin` 构建成功
- [ ] **7. 体积检查**：主包 ≤ 1.5MB，分包 ≤ 1.5MB
- [ ] **8. 真机测试**：iOS + Android 多机型通过
- [ ] **9. 功能联调**：所有业务流程跑通（见 test-report.md 第 8 节）
- [ ] **10. 性能验证**：首屏 ≤ 2s，接口 ≤ 1s
- [ ] **11. 上传提审**：微信开发者工具 → 上传 → 微信公众平台 → 提审

### 12.2 提审被拒常见原因规避

| 风险 | 规避措施 |
|------|----------|
| appid 为测试号 | 使用正式 AppID |
| 合法域名未配置 | 微信公众平台配置完成 |
| 接口 HTTP | 后端部署 HTTPS |
| 无隐私政策 | 配置用户隐私保护 |
| 权限无说明 | manifest.json permission desc 配置 |
| 体积超限 | 优化资源，分包加载 |
| 功能不完整 | 真机全流程验证 |
| 测试代码残留 | 移除 console.log |

---

## 13. 相关文档索引

| 文档 | 路径 | 用途 |
|------|------|------|
| 联调测试报告 | `miniapp/.sdd/test-report.md` | 静态检查结果 + 待验证项 |
| 产品规格说明书 | `miniapp/.sdd/spec.md` | 第 5.2 节提审检查清单 |
| 开发计划 | `miniapp/.sdd/plan.md` | 第 3.4 节配置说明 / 第 8.1 节风险矩阵 |
| 任务分解 | `miniapp/.sdd/tasks.md` | T6.1~T6.6 任务定义 |
| manifest.json | `miniapp/src/manifest.json` | 微信小程序配置 |
| pages.json | `miniapp/src/pages.json` | 路由与分包配置 |
| env 文件 | `miniapp/env/.env.*` | 环境变量配置 |

---

## 14. 风险提示（对齐 plan.md 第 8 章）

| 风险编号 | 风险 | 严重度 | 应对策略 |
|----------|------|--------|----------|
| R2 | request 合法域名配置问题 | 高 | 开发期关闭 urlCheck；上线前在微信公众平台配置合法域名；后端需 HTTPS |
| R10 | 后端 CORS 与小程序不在 CORS 范围 | 中 | 小程序请求不受 CORS 限制，但需配置合法域名 |
| R15 | 微信开发者工具与真机表现差异 | 中 | 真机测试覆盖 iOS + Android 多机型；重点关注 rich-text/base64/header/storage |

---

*提审检查清单生成完毕。请按第 12.1 节 Checklist 顺序执行提审前检查。*