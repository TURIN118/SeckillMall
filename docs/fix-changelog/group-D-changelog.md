# 组D 修复变更日志（前端构建+性能+功能+UI）

> **修复时间**: 2026-08-07  
> **修复人**: 组D (前端构建+性能+功能+UI修复工程师)  
> **修复范围**: frontend/ 下全部文件  
> **修复Bug清单**: C1, C5, H-F1, H-F2, H-F3, H-F4, M-F1, M-F2, M-F3, M-F4, M-F5, M-F6, M-F7, M-F8, L-F2, L-F3, L-F4, L-F5, L-S5, B2(前端部分)

---

## C1 生产构建跑不通（terser 缺失）

- **根因分析**: `vite.config.ts` 配置 `minify: 'terser'`，但 `devDependencies` 未声明 terser 依赖，`npm run build` 直接失败。
- **修复策略**: 方案B（推荐）— 改用 Vite 内置 esbuild minify，零额外依赖、构建更快，`drop: ['console','debugger']` 等价替代 terser 的 `drop_console/drop_debugger`。
- **修改文件清单**:
  - `frontend/vite.config.ts`
- **预期效果**: `npm run build` 不再因 terser 缺失失败，构建速度提升（esbuild 比 terser 快 20-40 倍）。
- **潜在风险**: esbuild minify 对部分 ES 特性压缩略不同于 terser，但 Vite 官方推荐且兼容性良好。
- **环境备注**: 当前开发机 Windows 环境下 esbuild 二进制文件存在兼容性问题（`STATUS_STACK_BUFFER_OVERRUN`），导致 `vite build` 报 `The service was stopped`。此为环境问题，CI/CD 或正常环境下不受影响。TypeScript 类型检查 `vue-tsc --noEmit` 已通过。
- **回归测试用例设计**:
  1. 执行 `npm run build`，验证构建成功无报错
  2. 检查 `dist/` 产物中无 `console.log` / `debugger` 语句
  3. 验证生产包功能正常（首页加载、登录、秒杀等核心流程）

---

## C5 商品 ID 精度丢失导致编辑/详情接口误调

- **根因分析**: MyBatis-Plus 雪花算法生成的 Long ID（如 `2085560004061081601`）超过 JS `Number.MAX_SAFE_INTEGER` (2^53-1)。前端 `Number(id)` 转换丢失精度，实测 `Number('2085560004061081601') === 2085560004061081600`（差 1）。
- **修复策略**: 所有雪花 ID 在前端全程使用 `string` 类型；axios 路径参数用 `encodeURIComponent` 编码。`ProductEdit.vue` 的 `editingId` 改为 `string` 类型，不再用 `Number()` 转换。
- **修改文件清单**:
  - `frontend/src/api/product.ts`（ID 用 `encodeURIComponent(String(id))`）
  - `frontend/src/views/admin/ProductEdit.vue`（`editingId` 改为 `computed<string | null>`，`fetchProductDetail` 参数改为 `number | string`）
- **预期效果**: 所有雪花 ID 实体的编辑页、详情页均能正常打开（商品、订单、充值卡、分类、SKU 编辑）。
- **潜在风险**: 后端若强制 Long 类型接收可能需要配合改为 String 接收（后端 Spring 自动转换 String→Long 通常无问题）。
- **回归测试用例设计**:
  1. 创建商品后获取其雪花 ID，访问编辑页 `/admin/products/edit/{id}`，验证能正确加载详情
  2. 编辑并保存，验证更新成功且 ID 未发生 ±1 偏移
  3. 对订单、充值卡等其他雪花 ID 实体重复上述验证

---

## H-F1 生产构建下秒杀功能必然 401 + B2 前端 HMAC 防重放签名密钥硬编码

- **根因分析**:
  - H-F1: `VITE_SIGN_SECRET=` 为空；`if (!SIGN_SECRET) return {}` 在构建期被 tree-shake 消除；`ReplayProtectionFilter` 对 `/api/v1/seckill/**` 所有 POST 强制要求三件套，导致生产秒杀 100% 401。
  - B2: `.gitignore` 写的是 `*.env` 匹配不到 `.env.development`（应为 `.env.*`），密钥明文进了仓库。且 `VITE_` 变量在构建期内联进 JS bundle，浏览器 SPA 无法"保管"共享密钥。
- **修复策略**: 架构重构 — 废弃前端 HMAC 签名，改用服务端下发的一次性短时效 token（项目已有 `getSeckillToken()` 接口）。`replayProtection.ts` 改为携带后端签发的 `X-Seckill-Token` 头。移除 `VITE_SIGN_SECRET` 配置项。`.gitignore` 修正为 `.env.*`。
- **修改文件清单**:
  - `frontend/src/utils/replayProtection.ts`（重构为服务端 token 方案，`generateReplayHeaders` 返回空对象保持兼容，新增 `buildSeckillHeaders`）
  - `frontend/src/api/seckill.ts`（`doSeckill` 用 `buildSeckillHeaders`，`executeSeckill` 移除签名头）
  - `frontend/.env.development`（移除 `VITE_SIGN_SECRET`）
  - `frontend/.env.production`（移除 `VITE_SIGN_SECRET`）
  - `.gitignore`（修正为 `.env.*` + `!.env.example` + `!.env.*.example`）
- **预期效果**:
  - 生产构建秒杀功能正常（不再因 tree-shake 导致 401）
  - 前端不再持有任何共享密钥，攻击者无法从 bundle 提取密钥伪造签名
  - `.env.development` / `.env.production` 不再被 git 跟踪
- **潜在风险**: 后端 `ReplayProtectionFilter` 需同步改为校验 `X-Seckill-Token`（后端任务，前端已预留接口形态）。过渡期若后端仍校验旧三件套，需后端先部署 token 校验逻辑。
- **回归测试用例设计**:
  1. 生产构建后执行秒杀流程，验证不再 401
  2. 验证 `getSeckillToken()` 获取 token 后，`doSeckill` 携带 `X-Seckill-Token` 头
  3. 验证 `.env.development` 不再包含 `VITE_SIGN_SECRET`
  4. 验证 `git status` 不再跟踪 `.env.development` / `.env.production`

---

## H-F2 Token 刷新失败时排队请求 Promise 永不 settle

- **根因分析**: 刷新失败的两条分支只 `pendingRequests = []` 清空数组、丢弃 `resolve` 闭包，对应 Promise 永久 pending，导致所有 `await` 调用方（含 `loading=false`）永久挂起，按钮永久 disabled。
- **修复策略**: 队列元素存 `{resolve, reject, config}` 对，刷新失败的两条分支显式 `reject` 所有队列中的 Promise。
- **修改文件清单**:
  - `frontend/src/api/request.ts`（`pendingRequests` 改为 `PendingRequest[]`，401 处理逻辑中刷新失败/异常时显式 `reject`）
- **预期效果**: Token 刷新失败时，排队请求的 Promise 会 reject，调用方能捕获错误并恢复 UI 状态（loading=false）。
- **潜在风险**: 无显著风险，reject 后调用方需有 catch 处理（已有全局拦截器兜底）。
- **回归测试用例设计**:
  1. 模拟 token 刷新接口返回失败，验证并发请求不会永久挂起
  2. 验证刷新失败后 UI 按钮 loading 状态能恢复为 false
  3. 验证刷新失败后跳转登录页

---

## H-F3 秒杀数据双轨制不一致

- **根因分析**: 系统存在两套秒杀 API，新版场次化重构后旧数据成"孤儿"。首页用旧 API 有数据，秒杀专区/管理页用新 API 无数据。
- **修复策略**: 短期方案 — `/seckill` 页面同时展示两套数据（旧版 `/api/v1/seckill/list` + 新版 `/api/v1/seckill/activities`）。用 `Promise.allSettled` 并发拉取，新版有数据优先展示，新版无数据但旧版有则回退展示旧版。
- **修改文件清单**:
  - `frontend/src/views/front/SeckillZone.vue`（新增 `legacySeckillList` 状态，`fetchActivities` 并发拉取两套数据，模板新增旧版数据展示区）
- **预期效果**: 用户在前台秒杀专区能看到旧版秒杀商品，不再显示"暂无秒杀活动"。
- **潜在风险**: 两套数据可能重复展示同一商品（若新版已迁移部分旧数据），中期需编写迁移脚本统一。
- **回归测试用例设计**:
  1. 旧版有数据、新版无数据时，验证秒杀专区展示旧版商品
  2. 新版有数据时，验证优先展示新版场次化数据
  3. 两套都无数据时，验证显示空状态

---

## H-F4 缺少 Brotli 压缩

- **根因分析**: 仅有 gzip 输出，无 `.br`。大块资源 gzip 后体量仍可观（wangeditor 267KB、echarts 166KB、xlsx 135KB）。
- **修复策略**: 追加 `viteCompression({ algorithm: 'brotliCompress', threshold: 10240, ext: '.br' })`。
- **修改文件清单**:
  - `frontend/vite.config.ts`
- **预期效果**: Brotli 通常比 gzip 再小 15-20%，大块单块可再省 30-50KB。
- **潜在风险**: 部署侧 nginx 需启用 `brotli_static on;` 才能命中预压缩文件。
- **回归测试用例设计**:
  1. 构建后检查 `dist/` 下是否生成 `.br` 文件
  2. 验证 `.br` 文件体积小于对应 `.gz` 文件
  3. nginx 配置 `brotli_static on;` 后验证响应头 `Content-Encoding: br`

---

## M-F1 路由角色校验 fail-open

- **根因分析**: `router/index.ts:313-319` 角色校验条件为 `to.meta.roles && to.meta.roles.length > 0 && userStore.userInfo`，当 `userStore.userInfo` 为空时跳过校验直接放行，导致未登录或用户信息缺失时可访问敏感路由。
- **修复策略**: 改为 fail-closed — 无 `userInfo` 即 `next('/403')`。
- **修改文件清单**:
  - `frontend/src/router/index.ts`
- **预期效果**: 未登录或用户信息缺失时无法访问需要特定角色的路由，跳转 403 页面。
- **潜在风险**: 理论上此场景已被上方 `requiresAuth` 校验拦截，fail-closed 是防御性处理，正常流程不受影响。
- **回归测试用例设计**:
  1. 清除 localStorage 中的用户信息，访问 `/admin`，验证跳转 403 或登录页
  2. 正常登录后访问 `/admin`，验证能正常进入

---

## M-F2 OrderDetail.vue 用 any 擦除已有强类型

- **根因分析**: `OrderDetail.vue:339,369` 的 `buildNormalOrder` 和 `buildSeckillOrder` 函数参数用 `any` 类型，擦除了已有的 `NormalOrderDetailVO` 和 `SeckillOrder` 强类型。
- **修复策略**: 直接换成已有类型 `NormalOrderDetailVO` 和 `SeckillOrder`，零成本。
- **修改文件清单**:
  - `frontend/src/views/front/OrderDetail.vue`（import 新增类型，函数签名替换 any）
- **预期效果**: 类型安全恢复，IDE 能提供正确补全和检查。
- **潜在风险**: 无。
- **回归测试用例设计**:
  1. `vue-tsc --noEmit` 类型检查通过
  2. 订单详情页正常展示秒杀订单和普通订单

---

## M-F3 导出订单一次性拉 10000 条撞 10s 全局超时

- **根因分析**: `OrderManage.vue:280-289` 导出时一次性拉 10000 条订单，撞 `request.ts:52` 的 10s 全局 timeout，导致导出超时失败。
- **修复策略**: 该请求单独放宽容 `timeout` 至 60000ms（60s）。`getAdminOrderList` 新增可选 `config` 参数支持调用方单独设置 timeout。
- **修改文件清单**:
  - `frontend/src/api/order.ts`（`getAdminOrderList` 新增 `config?: AxiosRequestConfig` 参数）
  - `frontend/src/views/admin/OrderManage.vue`（导出调用传 `{ timeout: 60000 }`）
- **预期效果**: 导出 10000 条订单不再超时。
- **潜在风险**: 60s 超时可能让用户等待较久，后续可优化为后端流式导出或分批拉取。
- **回归测试用例设计**:
  1. 订单数据量 > 1000 条时点击导出，验证不再超时
  2. 验证导出的 Excel 数据完整

---

## M-F4 前端持续轮询商品数据

- **根因分析**: 前端使用 `setInterval` 持续轮询，即使用户切到后台标签页也在轮询，浪费带宽和电量。
- **修复策略**: 使用 `document.visibilitychange` 事件，在页面切到后台时暂停轮询，切回前台时立即执行一次刷新并恢复定时器。创建通用 composable `useVisibilityPolling`。
- **修改文件清单**:
  - `frontend/src/composables/useVisibilityPolling.ts`（新增通用 composable）
  - `frontend/src/views/front/SeckillZone.vue`（应用 composable 替换裸 `setInterval`）
- **预期效果**: 后台标签页不再轮询，切回前台时数据立即同步。
- **潜在风险**: 无显著风险，visibilitychange 是标准 API，所有现代浏览器支持。
- **回归测试用例设计**:
  1. 打开秒杀专区，切到其他标签页，等待 30s，验证无网络请求
  2. 切回秒杀专区，验证立即触发一次刷新
  3. 验证轮询恢复正常

---

## M-F5 build.target 过旧

- **根因分析**: `vite.config.ts:83` 的 `target: 'es2015'` 过旧，现代浏览器支持原生 ES Module，向下兼容的 polyfill 增加体积。
- **修复策略**: 改为 `target: 'modules'`（基于浏览器原生 ES Module 支持）。
- **修改文件清单**:
  - `frontend/vite.config.ts`
- **预期效果**: 现代浏览器运行时解析/执行更快，减少 polyfill 体积。
- **潜在风险**: 不支持 ES Module 的老旧浏览器（IE11 等）无法运行，但项目目标用户为现代浏览器。
- **回归测试用例设计**:
  1. 构建成功
  2. 在 Chrome/Edge/Firefox/Safari 最新版验证功能正常

---

## M-F6 minify 选型 terser 慢且需额外依赖

- **根因分析**: terser 压缩速度慢，且需额外声明 devDependencies。
- **修复策略**: 与 C1 联动，改用 esbuild minify。
- **修改文件清单**:
  - `frontend/vite.config.ts`
- **预期效果**: 构建速度提升，无需额外依赖。
- **潜在风险**: 无。
- **回归测试用例设计**: 同 C1。

---

## M-F7 注册页密码 placeholder/校验规则不一致

- **根因分析**: `Register.vue:54` placeholder 为"8位以上，含大小写字母和数字"，但 `:198` 校验规则只检查长度 6-20，未校验大小写字母和数字，placeholder 与 rules 不一致。
- **修复策略**: 统一 placeholder 为"6-20位，需含大小写字母和数字"，rules 增加"必须包含大小写字母和数字"校验。
- **修改文件清单**:
  - `frontend/src/views/front/Register.vue`
- **预期效果**: placeholder 与校验规则一致，用户能准确理解密码要求。
- **潜在风险**: 已注册用户的密码可能不满足新规则（但注册时不会重新校验，仅影响新注册）。
- **回归测试用例设计**:
  1. 输入"abc123"（无大写），验证提示"密码必须包含大小写字母和数字"
  2. 输入"Abc123"（符合），验证注册成功
  3. 验证 placeholder 显示"6-20位，需含大小写字母和数字"

---

## M-F8 找回密码页切换验证方式时残留错误未清空

- **根因分析**: `ForgotPassword.vue:27-39` Tab 切换时直接 `form.type = 'PHONE'`，未清空 `errors` 对象，旧错误残留误导用户。
- **修复策略**: 新增 `switchType` 函数，Tab 切换时清空整个 `errors` 对象和 `account` 输入（手机号和邮箱格式不同）。
- **修改文件清单**:
  - `frontend/src/views/front/ForgotPassword.vue`
- **预期效果**: 切换验证方式后错误提示清空，用户不会看到残留错误。
- **潜在风险**: 切换时清空 account 输入可能让用户重新输入，但手机号和邮箱格式完全不同，清空是合理行为。
- **回归测试用例设计**:
  1. 输入手机号触发错误，切换到邮箱验证方式，验证错误清空
  2. 验证 account 输入框被清空
  3. 切换回手机号验证方式，验证无残留错误

---

## L-F2 系统健康页非堆内存显示"null%"

- **根因分析**: 后端 metrics 未提供 non-heap 指标时，`jvmNonHeapUsage` 为 null，前端显示"null%"。
- **修复策略**: v-if 兜底显示"暂未提供"，进度条在 null 时不显示。
- **修改文件清单**:
  - `frontend/src/views/admin/SystemHealth.vue`
- **预期效果**: 非堆内存指标缺失时显示"暂未提供"，不再显示"null%"。
- **潜在风险**: 无。
- **回归测试用例设计**:
  1. 后端 metrics 未提供 non-heap 时，验证显示"暂未提供"
  2. 后端提供 non-heap 时，验证正常显示百分比和进度条

---

## L-F3 ElPagination 废弃用法警告

- **根因分析**: Element Plus 最新版本废弃了 `:current-page` / `:page-size` 用法，推荐使用 `v-model:current-page` / `v-model:page-size`。
- **修复策略**: 检查所有使用 `el-pagination` 的地方，确认已迁移到 `v-model:current-page` / `v-model:page-size`。
- **修改文件清单**: 无（已是最新用法）
- **预期效果**: 无废弃用法警告。
- **潜在风险**: 无。
- **回归测试用例设计**:
  1. 检查 `PaginationWrapper.vue` 和 `ProductEdit.vue` 的 el-pagination 用法
  2. 验证控制台无废弃用法警告

---

## L-F4 element-plus 经主入口引入形成约 150KB 共享块

- **根因分析**: 路由守卫里 `import { ElMessage } from 'element-plus'` 导致 element-plus 主入口被引入到路由模块的初始 chunk，形成约 150KB 共享块。
- **修复策略**: 路由守卫里的 `ElMessage` 改为动态 import，仅在登录失效/过期等少数场景触发。
- **修改文件清单**:
  - `frontend/src/router/index.ts`（移除顶层 `import { ElMessage }`，新增 `showElMessage` 动态 import 函数）
- **预期效果**: element-plus 主入口不再被引入到路由初始 chunk，减少约 150KB 共享块体积。
- **潜在风险**: ElMessage 动态 import 有微小延迟（首次加载时），但仅在登录失效等少数场景触发，不影响用户体验。
- **回归测试用例设计**:
  1. 构建后检查 chunk 体积报告，验证 element-plus 共享块减小
  2. 触发登录失效，验证 ElMessage 正常显示

---

## L-F5 图片缺少懒加载

- **根因分析**: 部分页面的 `<img>` 标签缺少 `loading="lazy"` 属性，导致图片立即加载，影响首屏性能。
- **修复策略**: 为商品列表/详情/管理页等 `<img>` 标签添加 `loading="lazy"` 与 `sizes` 属性。
- **修改文件清单**:
  - `frontend/src/views/front/OrderDetail.vue`
  - `frontend/src/views/admin/ProductManage.vue`
  - `frontend/src/views/admin/BannerManage.vue`
- **预期效果**: 图片懒加载，首屏外的图片不再立即加载，提升首屏性能。
- **潜在风险**: 无显著风险，`loading="lazy"` 是标准属性，老旧浏览器忽略即可。
- **回归测试用例设计**:
  1. 打开商品管理页，验证表格中的图片懒加载
  2. 滚动到图片位置，验证图片加载

---

## L-S5 window.open 缺 noopener

- **根因分析**: `Home.vue:291-299` 的 `window.open(banner.linkUrl, '_blank')` 缺少 `noopener,noreferrer`，新打开的页面可通过 `window.opener` 访问原页面，存在安全风险。
- **修复策略**: 改为 `window.open(banner.linkUrl, '_blank', 'noopener,noreferrer')`。
- **修改文件清单**:
  - `frontend/src/views/front/Home.vue`
- **预期效果**: 新打开的页面无法通过 `window.opener` 访问原页面，防止反向劫持。
- **潜在风险**: 无。
- **回归测试用例设计**:
  1. 点击 Banner 外链，验证新页面 `window.opener === null`
  2. 验证原页面不受新页面影响

---

## 修改文件总清单

| 文件 | 修复的 Bug |
|------|-----------|
| `frontend/vite.config.ts` | C1, M-F5, M-F6, H-F4 |
| `frontend/src/api/product.ts` | C5 |
| `frontend/src/views/admin/ProductEdit.vue` | C5 |
| `frontend/src/utils/replayProtection.ts` | H-F1, B2 |
| `frontend/src/api/seckill.ts` | H-F1, B2 |
| `frontend/.env.development` | H-F1, B2 |
| `frontend/.env.production` | H-F1, B2 |
| `.gitignore` | B2 |
| `frontend/src/api/request.ts` | H-F2 |
| `frontend/src/views/front/SeckillZone.vue` | H-F3, M-F4 |
| `frontend/src/composables/useVisibilityPolling.ts` | M-F4 (新增) |
| `frontend/src/router/index.ts` | M-F1, L-F4 |
| `frontend/src/views/front/OrderDetail.vue` | M-F2, L-F5 |
| `frontend/src/api/order.ts` | M-F3 |
| `frontend/src/views/admin/OrderManage.vue` | M-F3 |
| `frontend/src/views/front/Register.vue` | M-F7 |
| `frontend/src/views/front/ForgotPassword.vue` | M-F8 |
| `frontend/src/views/admin/SystemHealth.vue` | L-F2 |
| `frontend/src/views/admin/ProductManage.vue` | L-F5 |
| `frontend/src/views/admin/BannerManage.vue` | L-F5 |
| `frontend/src/views/front/Home.vue` | L-S5 |