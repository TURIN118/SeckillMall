# Seckill Mall Bug 整合与改善方向总报告

> **生成时间**: 2026-08-07  
> **数据来源**: 8 份专项测试/审查报告（API 安全测试、UI E2E 浏览器测试 2 份、代码审查、代码审查标准/流程、前端性能审计、全系统功能测试）  
> **整合范围**: 阻断级 5 项 · 严重 6 项 · 高危 19 项 · 中危 35 项 · 低危 14 项 — 合计 **79 项**  
> **目标读者**: 后端开发、前端开发、QA、PM、架构/TL



---

## 一、总览

### 1.1 问题来源分布

| 来源报告            |  阻断 |  严重 |  高危 |  中危 |  低危 |     合计     |
| --------------- | :-: | :-: | :-: | :-: | :-: | :--------: |
| 代码审查报告          |  5  |  0  |  13 |  16 |  4  |     38     |
| API 安全质量测试报告    |  0  |  2  |  0  |  3  |  4  |      9     |
| UI-E2E 测试问题汇总报告 |  0  |  3  |  1  |  5  |  0  |      9     |
| 前端性能优化审计报告      |  0  |  1  |  1  |  2  |  2  |      6     |
| 全系统功能测试报告       |  0  |  0  |  0  |  4  |  0  |      4     |
| UI 端到端浏览器测试报告   |  —  |  —  |  —  |  —  |  —  | (与E2E汇总重叠) |
| 代码审查标准/流程       |  —  |  —  |  —  |  —  |  —  |   (审查依据)   |

> 注：部分问题跨报告重复出现（如 HTTP 状态码 200 问题同时出现在 API 报告 M3 和代码审查 M-D5），本表只计一次。

### 1.2 按模块分布

| 模块          | 问题数 |     最严重级别     |
| ----------- | :-: | :-----------: |
| 安全与认证       |  14 |   阻断 (密钥泄露)   |
| 秒杀核心        |  12 |   阻断 (双重扣减)   |
| 配置/基础设施/SQL |  14 |   阻断 (PII入库)  |
| 契约与数据访问     |  11 | 阻断 (Entity泄露) |
| 前端          |  13 |  严重 (生产构建失败)  |
| 支付/订单/钱包    |  5  |   高危 (支付双扣)   |
| MQ 可靠性      |  3  |       高危      |
| 性能          |  5  |   严重 (构建失败)   |
| UI/体验       |  4  |       中危      |

---

## 二、🔴 阻断级（BLOCKER — 必须立即修复，否则禁止发布）

### B1. RSA 私钥被提交进 Git 并推送远程，JWT 信任根失效

- **来源**: 代码审查报告 B1
- **位置**: `seckill-mall/src/main/resources/keys/private.pem`
- **根因**: Git 跟踪了 RSA 私钥文件，提交历史含 `6961647` "提交至 Gitee 远程仓库"。私钥既在仓库历史中，又被 `Dockerfile` 烘焙进镜像。
- **影响**: 攻击者可离线签发任意用户/角色（含 ADMIN）的合法 JWT，绕过全部认证鉴权，接管任意账户。
- **改善方向**:
  1. 立即视密钥对已泄露，生成全新 RSA 密钥对，存量 token 全部失效
  2. `git rm --cached` + `.gitignore` 增加 `**/resources/keys/*.pem`
  3. 用 `git filter-repo` 清洗全部历史后强推，通知协作者重 clone
  4. 生产改用容器外挂载 Secret（K8s Secret / Vault），不进 classpath

### B2. 前端 HMAC 防重放签名密钥硬编码并提交，架构上不可成立

- **来源**: 代码审查报告 B2
- **位置**: `frontend/.env.development:8` + `frontend/src/utils/replayProtection.ts`
- **根因**: `.gitignore` 写的是 `*.env` 匹配不到 `.env.development`（应为 `.env.*`），密钥明文进了仓库。且 `VITE_` 变量在构建期内联进 JS bundle，浏览器 SPA 无法"保管"共享密钥。
- **影响**: 攻击者可伪造防重放签名头，秒杀防刷归零。
- **改善方向**:
  1. 轮换 `SECKILL_SIGN_SECRET`，git filter-repo 清洗历史
  2. `.gitignore` 修正为 `.env.*`，实际值走 `.env.development.local`
  3. **架构重构**: 废弃前端签名，改用服务端下发一次性短时效 token（项目已有 `getSeckillToken()` 接口形态正确），`ReplayProtectionFilter` 改为校验后端签发的 token

### B3. Redis 秒杀库存被双重扣减，活动卖一半即报"已售罄"（严重少卖/资损）

- **来源**: 代码审查报告 B3
- **位置**: `mq/consumer/SeckillOrderConsumer.java:86` + `cache/SeckillLuaService.java:58`
- **根因**: 每笔成功订单 `seckillStock` 被减 2 次（Lua 预减 1 + 消费者再减 1），而 `seckillInfo.stock` 哈希被设为 DB 正确值，二者不一致。
- **影响**: 活动卖出一半时前端即报告售罄，造成严重的提前售罄/少卖和直接资损。
- **改善方向**:
  1. 消费者"同步 Redis"应使用 `SET`（校正到 DB 值）而非 `DECR`
  2. 明确"Lua 预减是唯一扣减点"，统一以 DB `available_count` 为真相来源，Redis 仅作闸门并定时校正

### B4. OrderController 直接把 Entity / Enum 序列化给前端（契约泄漏）

- **来源**: 代码审查报告 B4
- **位置**: `controller/OrderController.java:49/59/67/76/83`
- **根因**: 返回值类型为 `Result<PageResult<SeckillOrder>>`、`Result<SeckillOrder>`——`SeckillOrder` 是 entity（含 `isDeleted`/外键/内部金额），`OrderStatus` 是枚举，直接暴露。
- **影响**: 泄漏表结构、无法统一脱敏/格式化、entity 字段变更直接破坏前端契约。
- **改善方向**:
  1. 新增 `SeckillOrderVO`，通过转换层 entity→VO
  2. 接口签名改为 `Result<PageResult<SeckillOrderVO>>` / `Result<SeckillOrderVO>`
  3. `OrderStatus` 仅在 VO 以字符串/中文描述暴露

### B5. 含真实个人信息的数据库转储被提交进 Git（合规风险）

- **来源**: 代码审查报告 B5
- **位置**: `seckill_mall.sql`（173KB）+ `sql/03_migration.sql`
- **根因**: SQL 文件含姓名"吴同学"、手机号、可定位学校的地址、QQ 邮箱、100+ 条登录日志等真实 PII。admin/buyer01 使用同一 BCrypt 哈希，`data.sql` 注释给出明文口令。
- **影响**: 违反《个人信息保护法》，仓库一旦公开即构成个人信息泄露。
- **改善方向**:
  1. 从版本库移除含 PII 的 SQL 并清洗 git 历史
  2. `.gitignore` 增加 `*_dump.sql`、`seckill_mall.sql`
  3. 种子数据只保留结构性/脱敏示例，用户/地址/订单用脚本随机生成
  4. 轮换 admin/buyer01 口令，作废已泄露的充值卡

---

## 三、🔴 严重（CRITICAL — 核心功能阻断，1-2 天内修复）

### C1. 生产构建跑不通（terser 缺失）

- **来源**: 前端性能优化审计报告 CRITICAL
- **位置**: `vite.config.ts:91` + `package.json`
- **根因**: `vite.config.ts` 配置 `minify: 'terser'`，但 `devDependencies` 未声明 terser。`npm run build` 直接失败。
- **影响**: CI/CD 生产构建 100% 失败，发版阻断。
- **改善方向**: 方案 A — 补装 `terser` 依赖；方案 B（推荐）— 改用 Vite 内置 esbuild minify，零额外依赖、构建更快，`drop: ['console','debugger']` 等价替代。

### C2. 限流可被伪造 X-Forwarded-For 头绕过 → 短信/邮件费用泵攻击

- **来源**: API 安全质量测试报告 H1
- **位置**: `aspect/RateLimitAspect.java:101-119` `getClientIp()`
- **根因**: 未登录用户限流 key 基于客户端 IP，IP 无条件取自 `X-Forwarded-For` 请求头。攻击者每次请求携带不同伪造 IP 即可为每次请求分配独立限流桶。
- **影响**: 短信验证码接口无鉴权 + IP 限流失效 → 攻击者可无限触发短信发送（费用泵/短信轰炸），骚扰任意手机号。邮件接口同理。
- **改善方向**:
  1. 只信任来自可信反向代理的 `X-Forwarded-For`，配置代理 IP 白名单（Spring `ForwardedHeaderFilter` + `server.forward-headers-strategy`）
  2. 短信/邮件接口叠加按目标手机号/邮箱的每日总量限制
  3. 引入全局发送总量熔断（单日短信总量阈值告警 + 自动降级）

### C3. @RateLimit 的 seconds 参数为死配置，限流强度比预期弱约 60 倍

- **来源**: API 安全质量测试报告 H2
- **位置**: `annotation/RateLimit.java` + `aspect/RateLimitAspect.java` + `lua/rate_limit.lua`
- **根因**: 注解定义了 `seconds()` 字段，但切面和 Lua 从未使用该参数。`@RateLimit(key="send-sms", capacity=1, rate=1, seconds=60)` 实际效果是每秒 1 次，而非每 60 秒 1 次。
- **影响**: 所有带 `seconds` 的限流点（验证码发送/校验、秒杀）实际防护强度远低于设计预期，暴力枚举/刷单窗口被放大约 60 倍。
- **改善方向**: 修正 Lua 令牌桶，令 `rate = capacity / seconds`（如 60s 1 次 → `rate = 1/60`），或改用"固定窗口计数 + EXPIRE(seconds)"实现。

### C4. 充值卡生成接口无法返回明文卡密 → 新卡永久无法使用

- **来源**: UI-E2E 测试问题汇总报告 #7
- **位置**: `AdminRechargeCardController.java:54` + `RechargeCardVO.java:34`
- **根因**: `RechargeCardVO.cardPassword` 标注了 `@JsonIgnore`（H17 修复用于列表防泄露），但 Controller 生成接口返回类型仍为 `List<RechargeCardVO>`，导致生成时同样屏蔽卡密。虽有 `RechargeCardGenerateVO` 但完全未使用。
- **影响**: 新生成的充值卡明文卡密丢失后无法充值；DB BCrypt 加密存储单向不可逆，无补救机制。
- **改善方向**:
  1. `AdminRechargeCardController.generate` 返回类型改为 `Result<List<RechargeCardGenerateVO>>`
  2. `RechargeCardGenerateVO` 重新声明 `private String cardPassword`（不带 @JsonIgnore）
  3. Service 签名同步修改，验证 Jackson 序列化正确

### C5. 商品 ID 精度丢失导致编辑/详情接口误调

- **来源**: UI-E2E 测试问题汇总报告 #8
- **位置**: `ProductEdit.vue:246-251` + `product.ts:22-24`
- **根因**: MyBatis-Plus 雪花算法生成的 Long ID（如 `2085560004061081601`）超过 JS `Number.MAX_SAFE_INTEGER` (2^53-1)。前端 `Number(id)` 转换丢失精度，实测 `Number('2085560004061081601') === 2085560004061081600`（差 1）。
- **影响**: 所有雪花 ID 实体的编辑页、详情页均无法正常打开（商品、订单、充值卡、分类、SKU 编辑）。
- **改善方向**:
  1. **推荐方案 A**: 所有 ID 在前端全程使用 `string` 类型；axios 路径参数用 `encodeURIComponent`
  2. 方案 B: 修改 MyBatis-Plus ID 策略，使用更短 ID
  3. 修复后需重新测试所有详情/编辑类页面路由跳转

### C6. 创建秒杀场次接口返回 500 系统繁忙

- **来源**: UI-E2E 测试问题汇总报告 #9
- **位置**: `SeckillActivityServiceImpl.java:50-124` + `SeckillController.java:152-156`
- **根因**: `createActivity` 标注 `@Transactional(rollbackFor = Exception.class)`，推测 `registerAfterCommit(() -> seckillGoodsService.preheatSeckill(gid))` 中预热方法在 afterCommit 阶段抛错导致事务回滚。需后端日志调试确认。
- **影响**: 秒杀场次化重构功能不可用，admin 无法通过 UI 创建任何场次；用户侧 `/seckill` 专区长期显示"暂无秒杀活动"。
- **改善方向**:
  1. 后端日志重定向到文件，复现后查看完整堆栈
  2. 检查 `preheatSeckill` 内部是否有 NPE/Redis 序列化异常
  3. 将预热逻辑改为 `@TransactionalEventListener(phase = AFTER_COMMIT)`，异常不影响主流程
  4. 前端 500 时显示更具体的"创建失败，请联系管理员"提示

---

## 四、🟠 高危（HIGH — 本迭代必须修复）

### 4.1 安全类

#### H-S1. Banner 存储型 XSS（缺 XSS 清洗）

- **来源**: 代码审查报告 H-S1
- **位置**: `service/impl/BannerServiceImpl.java:59-70,73-98` + `controller/BannerPublicController.java:33`
- **根因**: 入库前未调项目已有的 `XssCleanUtil`（对比 `ProductReviewServiceImpl`/`ProductServiceImpl` 均已清洗）。
- **影响**: 管理员在 title/linkUrl 写入 `<img onerror=...>` 或 `javascript:` → 全站访客首页被执行恶意脚本。
- **改善方向**: 入库前对 title/linkUrl/imageUrl 调 `XssCleanUtil.clean`，并对 URL 做协议白名单（`http/https`）。

### 4.2 并发与秒杀类

#### H-C1. 取消/超时仅回补 Redis、不回补 DB available_count，且无补偿任务

- **来源**: 代码审查报告 H-C1
- **位置**: `service/impl/OrderServiceImpl.java:366-374` `rollbackStock`
- **根因**: `rollbackStock` 只动 Redis；全项目唯一 scheduler `SeckillStatusScheduler` 只更新状态，声称的"补偿任务兜底"实际不存在。
- **影响**: DB 库存单调递减永不恢复（假售罄/丢单），并触发幽灵单问题。
- **改善方向**: 在 `afterCommit` 中回补 DB 库存，并实现真正的库存对账补偿定时任务。

#### H-C2. 消费者 DB 扣减失败（rows==0）仍写"抢购成功"

- **来源**: 代码审查报告 H-C2
- **位置**: `mq/consumer/SeckillOrderConsumer.java:76-97`
- **根因**: 订单先于库存扣减创建，扣减返回 0 时仅告警不回滚，仍 `writeSuccessResult`。
- **影响**: 幽灵单/超卖（用户可支付、发货但无对应库存）。
- **改善方向**: 订单创建与 DB 扣减同事务，扣减失败必须撤销订单并写失败结果。

#### H-C3. 支付并发双扣

- **来源**: 代码审查报告 H-C3
- **位置**: `OrderServiceImpl.java:155-193 / 666-703`
- **根因**: 先"读状态"再 `updateById`，无 `WHERE status=UNPAID` 乐观条件；两并发请求都通过校验、各扣一次余额、末写覆盖。
- **影响**: 钱包被扣两次（直接资损）。
- **改善方向**: 状态判定下沉到 SQL（`UPDATE ... SET status=PAID WHERE id=? AND status='UNPAID'`），失败抛 `ORDER_ALREADY_PAID`；钱包扣减放到状态变更成功之后。

#### H-C4. MQ 同步降级路径不扣 DB 库存、不发延迟取消消息

- **来源**: 代码审查报告 H-C4
- **位置**: `SeckillOrderProducer.java:50-56`
- **根因**: 捕获 `AmqpException` 后直接 `createSeckillOrder`（仅建单，DB 扣减只在异步消费者做），且同步路径不发送延迟消息。
- **影响**: DB 超卖 + 订单永不自动取消 + Redis 死库存。
- **改善方向**: 降级路径须复刻异步路径关键副作用（事务内扣 DB 库存 + 发延迟消息）。

#### H-C5. RabbitMQ 发布不可靠（未开启 confirm/return）

- **来源**: 代码审查报告 H-C5
- **位置**: `RabbitMQConfig` + `SeckillOrderProducer.java:42`
- **根因**: 未开启 `publisher-confirm`/`publisher-returns`，发送时无 `CorrelationData`。
- **影响**: broker 端丢消息时应用无感知，静默丢失秒杀订单。
- **改善方向**: yml 开启 `publisher-confirm-type=correlated` + `publisher-returns=true`，发送传 `CorrelationData(messageId)`，在 ConfirmCallback 处理 `ack=false`。

#### H-C6. 秒杀下单队列无死信队列，毒消息被丢弃

- **来源**: 代码审查报告 H-C6
- **位置**: `RabbitMQConfig.java:58-61` + `SeckillOrderConsumer.java:113`
- **根因**: `seckillOrderQueue` 无 DLX；`basicNack(requeue=false)` 直接丢弃。对比延迟队列已正确配置 DLX。
- **影响**: 任何非业务异常都会让"待创建订单"彻底消失。
- **改善方向**: 配置 DLX/DLQ，或异常时 `requeue=true` + 有限重试 + 最终进 DLQ，并配补偿消费者。

#### H-C7. 秒杀取消缺乐观锁状态机，并发双回补

- **来源**: 代码审查报告 H-C7
- **位置**: `OrderServiceImpl.java:209-219(cancelOrder)`、`246-262(timeoutCancel)`
- **根因**: 用普通 `updateById` 无 `WHERE status`，而普通订单已用 `CANCELLING` 中间态防并发。
- **影响**: 用户取消与延迟超时并发时 Redis 库存 `+2`（双回补），叠加 B3 进一步失真、可能超卖。
- **改善方向**: 秒杀取消/超时引入 `UNPAID→CANCELLING→CANCELLED/TIMEOUT` 乐观锁状态机。

### 4.3 契约与数据访问类

#### H-D1. 未启用防全表更新拦截器

- **来源**: 代码审查报告 H-D1
- **位置**: `config/MybatisPlusConfig.java:20-27`
- **根因**: 仅配 `PaginationInnerInterceptor` + `OptimisticLockerInnerInterceptor`，缺 `BlockAttackInnerInterceptor`。项目存在 Controller 直调 Mapper 的写法。
- **影响**: 一旦 `UpdateWrapper` 漏写 `eq` 即触发全表更新/删除，无兜底。
- **改善方向**: 追加 `new BlockAttackInnerInterceptor()`，并 review 所有 `update/delete` 确保带主键/唯一条件。

### 4.4 配置与基础设施类

#### H-K1. 生产 profile 缺失全部连接配置，compose 注入变量无人消费

- **来源**: 代码审查报告 H-K1
- **位置**: `application-prod.yml` + `docker-compose.yml:73-87`
- **根因**: `application-prod.yml` 只有连接池参数，无 `url/username/password/host/port`；compose 注入的变量在 yml 中零引用；`JWT_SECRET` 更是死配置，制造"密钥已注入"的虚假安全感。
- **影响**: prod 静默回落 localhost，可能启动失败。
- **改善方向**: 补齐并统一变量名，全部强制外部注入（无默认值，缺失即失败）；删除失效 `JWT_SECRET`，改挂载 RSA 私钥。

#### H-K2. Redis 默认无口令 + 端口全网发布

- **来源**: 代码审查报告 H-K2
- **位置**: `docker-compose.yml:26-33`
- **根因**: `${REDIS_PASSWORD:-}` 默认空串，`ports: "6379:6379"` 绑 `0.0.0.0`。
- **影响**: 攻击者可直接改秒杀库存、清 nonce、刷单，甚至写 SSH key 提权。
- **改善方向**: `${REDIS_PASSWORD:?required}` 必填；删除 ports 或绑 `127.0.0.1`；`--protected-mode yes` + 重命名危险命令。

#### H-K3. MySQL 弱口令默认值 + 端口发布 + root 暴露

- **来源**: 代码审查报告 H-K3
- **位置**: `docker-compose.yml:3-17`
- **根因**: `root123`/`seckill123` 在口令字典前几百条，3306 发布到 `0.0.0.0`。
- **改善方向**: 全部 `${...:?required}`；删除 ports 或绑回环；生产用密钥管理下发。

#### H-K4. RabbitMQ 默认 guest/guest + 15672 硬编码发布

- **来源**: 代码审查报告 H-K4
- **位置**: `docker-compose.yml:42-52`
- **根因**: 管理端口 `15672` 无变量化硬编码发布，配默认 `guest/guest`。
- **影响**: 攻击者可登录控制台篡改/清空秒杀订单队列。
- **改善方向**: 口令必填；`15672` 删或绑 `127.0.0.1`；生产用独立管理账号 + IP 白名单。

#### H-K5. 应用以 MySQL root + 空口令连库（dev）

- **来源**: 代码审查报告 H-K5
- **位置**: `application-dev.yml:1-9`
- **根因**: `${DB_USERNAME:root}`、`${DB_PASSWORD:}`、`useSSL=false`、`allowPublicKeyRetrieval=true`、`characterEncoding=utf8`（与库表 utf8mb4 不一致）。
- **影响**: 违反最小权限，一旦注入即获 DROP/INTO OUTFILE 级能力。
- **改善方向**: 建专用业务账号仅授 CRUD；去掉默认值；`useSSL=true&allowPublicKeyRetrieval=false&characterEncoding=utf8mb4`。

### 4.5 前端类

#### H-F1. 生产构建下秒杀功能必然 401（签名逻辑被 tree-shake）

- **来源**: 代码审查报告 H-F1
- **位置**: `frontend/.env.production:4-5` + `replayProtection.ts:67-71`
- **根因**: `VITE_SIGN_SECRET=` 为空；`if (!SIGN_SECRET) return {}` 在构建期被 tree-shake 消除；`ReplayProtectionFilter` 对 `/api/v1/seckill/**` 所有 POST 强制要求三件套。
- **影响**: 用当前生产配置打出的包，核心秒杀 100% 不可用。
- **改善方向**: 随 B2 重构为服务端 token 方案；若过渡须对 `VITE_SIGN_SECRET` 空值 fail-fast + 把静默降级改为抛错。

#### H-F2. Token 刷新失败时排队请求 Promise 永不 settle

- **来源**: 代码审查报告 H-F2
- **位置**: `api/request.ts:113-152`
- **根因**: 刷新失败的两条分支只 `pendingRequests = []` 清空数组、丢弃 `resolve` 闭包，对应 Promise 永久 pending。
- **影响**: 所有 `await` 调用方（含 `loading=false`）永久挂起，按钮永久 disabled。
- **改善方向**: 队列元素存 `{resolve, reject}` 对，失败分支显式 `reject`。

### 4.6 功能类

#### H-F3. 秒杀数据双轨制不一致

- **来源**: UI-E2E 测试问题汇总报告 #1 + UI端到端浏览器测试报告 #1
- **位置**: 旧版 `/api/v1/seckill/list`（SeckillGoodsService）vs 新版 `/api/v1/seckill/activities`（SeckillActivityService）
- **根因**: 系统存在两套秒杀 API，新版场次化重构后旧数据成"孤儿"。首页用旧 API 有数据，秒杀专区/管理页用新 API 无数据。
- **影响**: 用户在前台秒杀专区看不到活动；管理员无法通过 UI 管理旧活动。
- **改善方向**:
  1. 短期：`/seckill` 同时展示两套数据
  2. 中期：编写迁移脚本将旧 `t_seckill_goods` 关联到新 `t_seckill_activity`
  3. 长期：废弃旧 API，下线 SeckillGoodsService

#### H-F4. 缺少 Brotli 压缩

- **来源**: 前端性能优化审计报告 HIGH
- **位置**: `vite.config.ts` plugins
- **根因**: 仅有 gzip 输出，无 `.br`。大块资源 gzip 后体量仍可观（wangeditor 267KB、echarts 166KB、xlsx 135KB）。
- **影响**: Brotli 通常比 gzip 再小 15-20%，大块单块可再省 30-50KB。
- **改善方向**: 追加 `viteCompression({ algorithm: 'brotliCompress', threshold: 10240, ext: '.br' })`。部署侧 nginx 启用 `brotli_static on`。

---

## 五、🟡 中危（MEDIUM — 应尽早修复）

### 5.1 安全类

#### M-S1. 登录接口缺 @RateLimit 限流

- **来源**: 代码审查报告 M-S1
- **位置**: `AuthController.java:51`
- **改善方向**: 按 IP 维度补 `@RateLimit`，防"不同用户名+同弱口令"横向爆破。

#### M-S2. Banner 管理接口入参缺 @Valid

- **来源**: 代码审查报告 M-S2
- **位置**: `BannerController.java:46-58`
- **改善方向**: `@RequestBody BannerVO` 前加 `@Valid`，`BannerVO` 字段补 jakarta.validation 约束。

#### M-S3. SeckillController 误导性 TODO 注释

- **来源**: 代码审查报告 M-S3
- **位置**: `SeckillController.java:93-94,116-118`
- **改善方向**: 更新/删除注释，避免误导后续开发者改回从请求体取 userId 重引越权。

#### M-S4. 密码喷洒攻击——登录失败锁定按用户名维度可被规避

- **来源**: API 安全质量测试报告 M1
- **位置**: `service/impl/AuthServiceImpl.java:112`
- **改善方向**: 叠加按客户端 IP/设备维度的失败计数与全局风控（如单 IP 每分钟登录失败 > N 次则要求验证码或临时封禁）。

#### M-S5. 用户名枚举——存在与不存在的用户响应可区分

- **来源**: API 安全质量测试报告 M2
- **位置**: `service/impl/AuthServiceImpl.java:126` → `writeLoginLog`
- **根因**: 不存在用户返回 500（`t_login_log.user_id` NOT NULL 约束异常），存在用户返回 1003。
- **改善方向**: 修复 `t_login_log.user_id` 允许为空或不存在用户时不写该表；统一返回相同的"用户名或密码错误"文案与耗时。

#### M-S6. 全部接口 HTTP 状态码恒为 200，破坏 REST 语义与网关监控

- **来源**: API 安全质量测试报告 M3 + 代码审查报告 M-D5
- **位置**: `common/GlobalExceptionHandler.java` + `common/Result`
- **改善方向**: 在 `@RestControllerAdvice` 中为不同异常设置对应 HTTP 状态（400/401/403/404/429/500），body 保留业务 `code`。

### 5.2 并发类

#### M-C1. 幂等键在处理前设置，处理中异常 nack 丢弃致消息丢失

- **来源**: 代码审查报告 M-C1
- **位置**: `SeckillOrderConsumer.java:67-73,110-114`
- **改善方向**: 幂等键在"处理成功并提交后"再设置，或 nack 时删除 dedup 并配 DLQ。

#### M-C2. Lua/Redis 回补非原子

- **来源**: 代码审查报告 M-C2
- **位置**: `SeckillLuaService.java:65-72` + `OrderServiceImpl.rollbackStock`
- **改善方向**: 合并为 Lua 原子执行回补。

#### M-C3. Lua stockTtl 参数未传

- **来源**: 代码审查报告 M-C3
- **位置**: `SeckillLuaService.java:58`
- **改善方向**: 补传 stockTtl 或统一由预热 TTL 管理并惰性延期。

### 5.3 契约与数据访问类

#### M-D1. VO 双向用作请求体与响应体

- **来源**: 代码审查报告 M-D1
- **改善方向**: 拆分 `BannerCreateRequest`/`BannerUpdateRequest` 等放 `dto/` 包。

#### M-D2. Controller 直调 Mapper 绕过 Service

- **来源**: 代码审查报告 M-D2
- **位置**: `WalletController.java:53-54,78-87`、`UserController.java:41,62,64`
- **改善方向**: 下沉到 Service，Controller 仅编排并返回 `Result<VO>`。

#### M-D3. 多处 DTO 已声明校验注解但 Controller 未触发

- **来源**: 代码审查报告 M-D3
- **位置**: `CategoryController.java:57`、`AdminUserController.java:44`、`AdminOrderController.java:33`、`SystemController.java:61`
- **改善方向**: 参数前加 `@Valid`/`@Validated`。

#### M-D4. CategoryUpdateRequest 全字段、LoginRequest 缺格式校验

- **来源**: 代码审查报告 M-D4
- **改善方向**: 补齐 `@NotBlank/@Size/@Min` 与 `@Email`/`@Pattern`。

#### M-D5. 声明 MapStruct 却零使用，全量手工 setXxx

- **来源**: 代码审查报告 M-D6
- **改善方向**: 启用 `@Mapper(componentModel="spring")` 做 entity↔VO 转换，脱敏用 `@AfterMapping`。

#### M-D6. ProductSkuMapper.xml 使用 SELECT *

- **来源**: 代码审查报告 M-D7
- **位置**: `resources/mapper/ProductSkuMapper.xml:6,15`
- **改善方向**: 显式列 `Base_Column_List`。

#### M-D7. PaginationWrapper total 属性类型警告

- **来源**: UI-E2E 测试问题汇总报告 #3
- **根因**: 后端 `PageResult` 的 total 序列化为 String，前端 PaginationWrapper 期望 Number。
- **改善方向**: 后端 `PageResult` 字段改为 `Integer`；或前端 `Number(p.total)` 强转。

### 5.4 前端类

#### M-F1. 路由角色校验 fail-open

- **来源**: 代码审查报告 M-F1
- **位置**: `router/index.ts:313-319`
- **改善方向**: 改为 fail-closed（无 userInfo 即 `next('/403')`）。

#### M-F2. OrderDetail.vue 用 any 擦除已有强类型

- **来源**: 代码审查报告 M-F2
- **位置**: `OrderDetail.vue:339,369`
- **改善方向**: 直接换成已有类型 `NormalOrderDetailVO` 等，零成本。

#### M-F3. 导出订单一次性拉 10000 条撞 10s 全局超时

- **来源**: 代码审查报告 M-F3
- **位置**: `OrderManage.vue:280-289` + `request.ts:52`
- **改善方向**: 该请求单独放宽容 `timeout`，或改后端流式导出。

#### M-F4. 前端持续轮询商品数据

- **来源**: 全系统功能测试报告 问题2
- **改善方向**: 前端使用 `document.visibilitychange` 暂停后台标签页轮询；或改用 WebSocket/SSE 推送。

#### M-F5. build.target: 'es2015' 过旧

- **来源**: 前端性能优化审计报告 MEDIUM-1
- **位置**: `vite.config.ts:83`
- **改善方向**: 改为 `target: 'modules'`（1 行改动），现代浏览器运行时解析/执行更快。

#### M-F6. minify 选型 terser 慢且需额外依赖

- **来源**: 前端性能优化审计报告 MEDIUM-2
- **改善方向**: 改用 esbuild minify（与 C1 联动，二选一）。

### 5.5 配置与 SQL 类

#### M-K1. .gitignore 规则无法覆盖 .env.*

- **来源**: 代码审查报告 M-K1
- **改善方向**: 改为 `.env` / `.env.*` / `*.env` + `!.env.example` / `!.env.*.example`。

#### M-K2. CORS 生产未覆盖 + 默认值含内网 IP + 用 setAllowedOriginPatterns

- **来源**: 代码审查报告 M-K2
- **改善方向**: prod 显式注入、改用 `setAllowedOrigins`、抽成 `@ConfigurationProperties` 共用。

#### M-K3. 容器以 root 运行 + 私钥烘焙 + 无优雅停机/HEALTHCHECK

- **来源**: 代码审查报告 M-K3
- **改善方向**: 建 `app` 用户 + `USER app` + `exec java` + `HEALTHCHECK`；私钥改运行时挂载。

#### M-K4. t_seckill_activity 建表缺 COLLATE

- **来源**: 代码审查报告 M-K4
- **位置**: `sql/seckill_activity_migration.sql:22`
- **改善方向**: 补 `COLLATE=utf8mb4_general_ci`；主键改 `BIGINT NOT NULL`；对齐 status 类型。

#### M-K5. 01_schema.sql 对 21 张表执行 DROP TABLE IF EXISTS 无环境护栏

- **来源**: 代码审查报告 M-K5
- **改善方向**: 拆分破坏性脚本、加环境断言、改用 Flyway/Liquibase 版本化增量。

#### M-K6. t_user_coupon 缺 (user_id, status) 复合索引

- **来源**: 代码审查报告 M-K6
- **改善方向**: 加 `idx_user_status_create (user_id,status,create_time)` 与 `idx_order_id`；`t_product_review` 补 `(product_id,status,create_time)`。

### 5.6 功能/数据类

#### M-F7. 注册页密码 placeholder/校验规则不一致

- **来源**: UI-E2E 测试问题汇总报告 #5
- **位置**: `Register.vue:54,198`
- **改善方向**: 统一 placeholder 与 rules 规则（推荐 6-20 + 大小写字母+数字）。

#### M-F8. 找回密码页切换验证方式时残留错误未清空

- **来源**: UI-E2E 测试问题汇总报告 #6
- **位置**: `ForgotPassword.vue:27-39`
- **改善方向**: Tab 切换时 reset 整个 errors 对象或触发 `form.validate('account')`。

#### M-F9. buyer01 密码与种子数据不一致

- **来源**: 全系统功能测试报告 问题1
- **改善方向**: 重新执行 data.sql 或手动更新 buyer01 密码为 buyer123 对应的 BCrypt 哈希。

#### M-F10. Banner 标题为空

- **来源**: 全系统功能测试报告 问题3
- **改善方向**: 补充 Banner 标题数据，或在后端新增时校验 title 非空。

#### M-F11. 注册接口必填验证码但 SecurityConfig 放行

- **来源**: 全系统功能测试报告 问题4
- **改善方向**: 评估对校验失败返回 400 状态码，区分"未登录"和"校验失败"。

---

## 六、💭 低危/轻微（LOW — 迭代优化）

### 6.1 安全加固类

#### L-S1. /actuator/health 匿名可访问，泄露组件拓扑

- **来源**: API 安全质量测试报告 L1
- **位置**: `SecurityConfig.java:83`
- **改善方向**: 生产环境设 `management.endpoint.health.show-details=never` 或 `when-authorized`。

#### L-S2. 缺少 CSP 与 HSTS 响应头

- **来源**: API 安全质量测试报告 L2
- **改善方向**: 补充 `Content-Security-Policy`、`Strict-Transport-Security`（HTTPS 环境）。

#### L-S3. CORS allowCredentials=true 且允许 6 种方法，白名单靠配置兜底

- **来源**: API 安全质量测试报告 L3
- **改善方向**: 确保生产 `allowed-origins` 不含通配、不误配公网域名。

#### L-S4. 图形验证码为简单字符+干扰线，易被 OCR 识别

- **来源**: API 安全质量测试报告 L4
- **改善方向**: 高风险场景升级为行为验证码（滑块/点选）。

#### L-S5. window.open 缺 noopener

- **来源**: 代码审查报告 L-F1
- **位置**: `Home.vue:291-299`
- **改善方向**: `'_blank','noopener,noreferrer'`。

#### L-S6. 种子数据弱口令明文写注释

- **来源**: 代码审查报告 L-K2
- **位置**: `data.sql:1-17`
- **改善方向**: 删明文注释、不预置固定口令、首次登录强制改密。

#### L-S7. 开发环境信息泄露

- **来源**: 代码审查报告 L-K3
- **位置**: `application.yml` `info.app.*`
- **改善方向**: 收敛或限制 `/actuator/info` 访问。

### 6.2 前端/UI 类

#### L-F2. 系统健康页非堆内存显示"null%"

- **来源**: UI-E2E 测试问题汇总报告 #2
- **改善方向**: 前端 v-if 兜底显示"暂未提供"；后端 metrics 补全 non-heap 指标。

#### L-F3. ElPagination 废弃用法警告

- **来源**: UI-E2E 测试问题汇总报告 #4
- **改善方向**: 按 Element Plus 最新文档迁移到 `v-model:current-page` / `v-model:page-size`。

#### L-F4. element-plus 经主入口引入形成约 150KB 共享块

- **来源**: 前端性能优化审计报告 LOW-1
- **改善方向**: 路由守卫里的 `ElMessage` 改为动态 import；收益有限，按需处理。

#### L-F5. 图片缺少懒加载

- **来源**: 前端性能优化审计报告 LOW-2
- **改善方向**: 商品列表/详情的 `<img>` 加 `loading="lazy"` 与 `sizes`。

### 6.3 可观测性建议

#### L-O1. 秒杀防重放错误码优化

- **来源**: 全系统功能测试报告 改进建议 7.1-1
- **改善方向**: ReplayProtectionFilter 返回 403 + "缺少秒杀签名"，区分"未登录"和"缺少签名"。

#### L-O2. 登录失败日志补全

- **来源**: 全系统功能测试报告 改进建议 7.1-2
- **改善方向**: `t_login_log` 的 username/userAgent/loginLocation 字段在记录时填充。

#### L-O3. 慢查询监控 / 秒杀链路追踪 / 操作日志覆盖率

- **来源**: 全系统功能测试报告 改进建议 7.3
- **改善方向**: 增加慢查询阈值告警（>100ms）；接入 Sleuth/Zipkin 链路追踪；补充商品/订单/秒杀管理的操作日志。

---

## 七、修复优先级路线图

### P0 — 应急响应（立即执行，事关安全与合规）

|  编号 | 问题                               | 类型 |
| :-: | -------------------------------- | -- |
|  B1 | RSA 私钥泄露 — 轮换密钥 + 清洗 git 历史      | 安全 |
|  B2 | HMAC 签名密钥泄露 — 轮换 + 清洗历史 + 架构重构   | 安全 |
|  B5 | 真实 PII 入库 — 移除 PII + 清洗历史 + 轮换口令 | 合规 |

### P1 — 阻断上线（本迭代必修，1-3 天）

|      编号     | 问题                           | 模块    |
| :---------: | ---------------------------- | ----- |
|      B3     | Redis 秒杀库存双重扣减致少卖            | 秒杀    |
|      B4     | OrderController Entity 泄漏    | 契约    |
|      C1     | 生产构建跑不通（terser 缺失）           | 前端构建  |
|      C2     | X-Forwarded-For 限流绕过 → 短信费用泵 | 安全    |
|      C3     | @RateLimit seconds 参数失效      | 安全/限流 |
|      C4     | 充值卡生成不返回明文卡密                 | 充值卡   |
|      C5     | 商品 ID JS 精度丢失                | 前端/数据 |
|      C6     | 创建秒杀场次 500 错误                | 秒杀    |
|     H-S1    | Banner 存储型 XSS               | 安全    |
|     H-C1    | 取消/超时不同步 DB 库存               | 秒杀    |
|     H-C2    | 扣减失败仍写"成功"→幽灵单               | 秒杀    |
|     H-C3    | 支付并发双扣                       | 支付    |
|     H-C4    | MQ 降级路径不扣库存                  | MQ    |
|     H-C5    | MQ 发布无 confirm               | MQ    |
|     H-C6    | 下单队列无死信                      | MQ    |
|     H-C7    | 秒杀取消缺乐观锁                     | 秒杀    |
|     H-D1    | 未启用防全表更新拦截器                  | 数据访问  |
| H-K1 ~ H-K5 | 生产配置缺失 + 中间件弱口令/端口暴露         | 配置    |
|     H-F1    | 生产构建秒杀必 401                  | 前端    |
|     H-F2    | Token 刷新 Promise 挂起          | 前端    |
|     H-F3    | 秒杀数据双轨制不一致                   | 功能    |
|     H-F4    | 缺 Brotli 压缩                  | 性能    |

### P2 — 跟进（1-2 周内）

|      编号     | 问题                                                                    | 模块 |
| :---------: | --------------------------------------------------------------------- | -- |
| M-S1 ~ M-S6 | 6 项安全中危（登录限流、Banner @Valid、HTTP 状态码、密码喷洒、用户名枚举、误导注释）                  | 安全 |
| M-C1 ~ M-C3 | 3 项并发中危（幂等键、回补非原子、stockTtl）                                           | 秒杀 |
| M-D1 ~ M-D7 | 7 项契约/数据中危（VO分层、Controller直调Mapper、@Valid未触发、MapStruct、SELECT *、分页类型） | 契约 |
| M-F1 ~ M-F6 | 6 项前端中危（路由fail-open、any擦类型、导出超时、轮询、es2015、terser选型）                   | 前端 |
| M-K1 ~ M-K6 | 6 项配置/SQL中危（.gitignore、CORS、Dockerfile、COLLATE、破坏性脚本、缺索引）             | 配置 |
| M-F7 ~ M-F8 | 注册页/找回密码 UI 瑕疵                                                        | 前端 |

### P3 — 迭代优化（后续版本）

|      编号     | 问题                                                               | 模块  |
| :---------: | ---------------------------------------------------------------- | --- |
| L-S1 ~ L-S7 | 7 项低危安全加固（actuator、CSP/HSTS、CORS白名单、验证码强度、noopener、弱口令注释、info泄露） | 安全  |
| L-F2 ~ L-F5 | 4 项前端低危（null%、ElPagination废弃、element-plus共享块、图片懒加载）              | 前端  |
| L-O1 ~ L-O3 | 3 项可观测性改进（错误码优化、日志补全、链路追踪）                                       | 可观测 |

---

## 八、附录

### 8.1 已做对的事（不应改动的基线）

从各报告提炼的项目亮点，应作为团队基线保留：

| 维度    | 亮点                                                                              |
| ----- | ------------------------------------------------------------------------------- |
| 认证/鉴权 | JWT RS256 非对称签名、tokenVersion 防复用、黑名单、全量 IDOR 从 Token 取身份、ADMIN 接口 @PreAuthorize |
| 防重放   | ReplayProtectionFilter 三件套校验；后端已有 getSeckillToken() 服务端令牌方案（B2 替代方向）            |
| 缓存一致性 | Cache Aside 正确（afterCommit）、布隆过滤器防穿透、互斥锁+空值+随机TTL防击穿/雪崩                         |
| 幂等    | uk_user_seckill 唯一约束 + Lua SISMEMBER 判重 + MQ messageId 去重；发货校验 status==PAID     |
| 前端工程化 | vue-tsc 0 错误、strict:true、v-html 经 DOMPurify 净化、API 拦截器统一、路由全懒加载、Pinia 按域拆分      |
| SQL   | 热点列索引覆盖、无不带 WHERE 的 UPDATE/DELETE、逻辑删除全局开启、utf8mb4 统一                           |
| 安全加固  | CORS 已从 * 改为显式列表、生产关闭 Swagger/Knife4j、BCrypt(10)、STATELESS 会话                   |

### 8.2 各维度风险热度分布

```
安全与认证    ████████████████  (14项: 2阻断 + 2严重 + 1高危 + 5中危 + 4低危)
秒杀核心      ██████████████    (12项: 1阻断 + 1严重 + 7高危 + 3中危)
配置/基础设施  ████████████████  (14项: 1阻断 + 0严重 + 5高危 + 6中危 + 2低危)
契约与数据访问 ███████████       (11项: 1阻断 + 0严重 + 1高危 + 7中危 + 2低危)
前端          █████████████     (13项: 1阻断 + 2严重 + 2高危 + 6中危 + 2低危)
支付/订单/钱包 █████             (5项:  0阻断 + 0严重 + 3高危 + 2中危)
MQ 可靠性     ███               (3项:  0阻断 + 0严重 + 3高危 + 0中危)
```

### 8.3 参考文档清单

| 文档              | 路径                           |
| --------------- | ---------------------------- |
| API 安全质量测试报告    | `docs/报告/API安全质量测试报告.md`     |
| UI-E2E 测试问题汇总报告 | `docs/报告/UI-E2E-测试问题汇总报告.md` |
| UI 端到端浏览器测试报告   | `docs/报告/UI端到端浏览器测试报告.md`    |
| 代码审查报告          | `docs/报告/代码审查报告.md`          |
| 代码审查标准          | `docs/报告/代码审查标准.md`          |
| 代码审查流程          | `docs/报告/代码审查流程.md`          |
| 前端性能优化审计报告      | `docs/报告/前端性能优化审计报告.md`      |
| 全系统功能测试报告       | `docs/报告/全系统功能测试报告.md`       |

---

> **文档版本**: v1.0  
> **下次更新**: P0/P1 修复完成后更新各 Bug 状态  
> **建议**: 团队过一遍本报告后，将 P0/P1 项导入项目管理工具（TAPD/Jira），逐项跟踪闭环。
