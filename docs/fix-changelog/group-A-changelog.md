# 组A 修改日志（后端安全+配置+基础设施+SQL）

> **修复人**: 组A（coding-engineer）
> **修复时间**: 2026-08-07
> **修复Bug清单**: B1, B2(后端), B5, C2, C3, H-S1, H-K1~H-K5, M-S1, M-S2, M-S4, M-S5, M-S6, M-K1~M-K6, M-D1, M-F9, M-F10, M-F11, L-S1~L-S4, L-S6, L-S7, L-O1~L-O3

---

## B1. RSA 私钥泄露

- **根因分析**: RSA 私钥 `private.pem` 被 Git 跟踪并推送远程，Dockerfile 烘焙进镜像，攻击者可离线签发任意 JWT。
- **修复策略**:
  1. `git rm --cached` 移除 `private.pem` / `public.pem` 的 Git 跟踪
  2. 生成全新 RSA 2048 密钥对（PKCS#8 私钥 + X.509 公钥）替换旧密钥
  3. `.gitignore` 增加 `**/resources/keys/*.pem` + `*.pem`
  4. Dockerfile 改为运行时挂载私钥（`VOLUME /app/keys`），不烘焙进镜像
- **修改文件**:
  - `seckill-mall/src/main/resources/keys/private.pem`（新密钥）
  - `seckill-mall/src/main/resources/keys/public.pem`（新密钥）
  - `.gitignore`
  - `seckill-mall/Dockerfile`
- **预期效果**: 旧密钥失效，攻击者无法用泄露的私钥签发 JWT；新密钥不进仓库/镜像。
- **潜在风险**: 存量 JWT token 全部失效，用户需重新登录。
- **回归测试**: 验证 JWT 签发/验证正常；验证 `git status` 不再跟踪 keys/*.pem；验证容器挂载私钥后启动正常。

---

## B2. 前端 HMAC 防重放签名架构重构（后端部分）

- **根因分析**: 前端 HMAC 签名密钥硬编码且浏览器 SPA 无法安全保管共享密钥，架构上不可成立。
- **修复策略**:
  1. `ReplayProtectionFilter` 废弃前端 HMAC 三件套（X-Sign/X-Timestamp/X-Nonce）校验
  2. 改为校验服务端签发的秒杀令牌（`X-Seckill-Token`），项目已有 `getSeckillToken()` 接口
  3. 保留 nonce 原子去重（用 token 作为 nonce，防重放）
  4. `sign-secret` 改为服务端 token 签发用，不再暴露给前端
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/security/ReplayProtectionFilter.java`
- **预期效果**: 前端无需 HMAC 签名，秒杀防刷由服务端 token 保证；密钥不暴露给浏览器。
- **潜在风险**: 前端需配合改造（从 `/seckill/{id}/token` 获取 token 并携带 `X-Seckill-Token` 头下单）。
- **回归测试**: 验证携带有效 token 的秒杀请求正常；验证缺少 token 返回 403；验证重复使用同一 token 被拒绝。

---

## B5. PII 入库脱敏

- **根因分析**: `seckill_mall.sql`（173KB）和 `sql/03_migration.sql` 含真实姓名/手机号/地址/邮箱等 PII。
- **修复策略**:
  1. `git rm --cached` 移除 `seckill_mall.sql` 和 `sql/03_migration.sql`
  2. `.gitignore` 增加 `*_dump.sql` / `seckill_mall.sql` / `sql/03_migration.sql`
  3. `data.sql` 脱敏：email 改为 `@example.com`，删除明文口令注释
- **修改文件**:
  - `.gitignore`
  - `seckill-mall/src/main/resources/sql/data.sql`
- **预期效果**: 仓库不再含真实 PII，合规风险消除。
- **潜在风险**: 需要从其他渠道重新导入生产数据。
- **回归测试**: 验证 `git status` 不再跟踪 seckill_mall.sql；验证 data.sql 可正常执行。

---

## C2. X-Forwarded-For 限流绕过

- **根因分析**: `RateLimitAspect.getClientIp()` 无条件信任 `X-Forwarded-For` 头，攻击者伪造即可绕过 IP 限流。
- **修复策略**:
  1. 新增可信代理 IP 白名单配置 `seckill.security.trusted-proxy-ips`
  2. `getClientIp()` 仅当 `request.getRemoteAddr()` 在白名单时才信任 X-Forwarded-For
  3. 从右向左取第一个非可信 IP（防代理链伪造）
  4. 短信/邮件接口叠加按目标手机号/邮箱的每日总量限制
  5. `application.yml` 启用 `server.forward-headers-strategy: native`
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/aspect/RateLimitAspect.java`
  - `seckill-mall/src/main/java/com/seckill/mall/controller/VerificationCodeController.java`
  - `seckill-mall/src/main/resources/application.yml`
  - `seckill-mall/src/main/resources/application-dev.yml`
  - `seckill-mall/src/main/resources/application-prod.yml`
- **预期效果**: 攻击者无法通过伪造 X-Forwarded-For 绕过限流；短信轰炸被按目标每日上限拦截。
- **潜在风险**: 可信代理白名单需正确配置，否则限流可能误判。
- **回归测试**: 验证非白名单 IP 的 X-Forwarded-For 被忽略；验证同一手机号每日超过 10 次被拒绝。

---

## C3. @RateLimit seconds 参数失效

- **根因分析**: `rate_limit.lua` 从未使用 `seconds` 参数，`@RateLimit(seconds=60)` 实际效果是每秒 1 次而非每 60 秒 1 次。
- **修复策略**: Lua 脚本接收 `seconds` 参数，令 `refillRate = capacity / seconds`，确保 `capacity=1, seconds=60` → 每 60s 1 次。
- **修改文件**:
  - `seckill-mall/src/main/resources/lua/rate_limit.lua`
  - `seckill-mall/src/main/java/com/seckill/mall/aspect/RateLimitAspect.java`
- **预期效果**: `@RateLimit(seconds=60)` 实际防护强度为每 60s 1 次，符合设计预期。
- **潜在风险**: 现有限流点防护强度变严（之前过松），可能影响正常用户高频操作。
- **回归测试**: 验证 `@RateLimit(seconds=60)` 60s 内第 2 次请求被拒绝。

---

## H-S1. Banner 存储型 XSS

- **根因分析**: `BannerServiceImpl` 入库前未调 `XssCleanUtil` 清洗，管理员可写入恶意脚本。
- **修复策略**: 入库前对 title 调 `XssCleanUtil.cleanStrict`，对 imageUrl/linkUrl 调 `XssCleanUtil.clean` + 协议白名单（http/https）。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/service/impl/BannerServiceImpl.java`
- **预期效果**: `<img onerror=...>` / `javascript:` 等恶意内容被清洗或拒绝。
- **潜在风险**: 合法 HTML 标题被移除（title 用严格模式）。
- **回归测试**: 验证含 `<script>` 的 title 被清洗；验证 `javascript:` URL 被拒绝。

---

## H-K1. 生产 profile 缺失连接配置

- **根因分析**: `application-prod.yml` 无 url/username/password/host/port，compose 注入变量无人消费；`JWT_SECRET` 死配置。
- **修复策略**: 补齐全部连接配置，使用 `${VAR:?required}` 强制外部注入（缺失即失败）；删除失效 `JWT_SECRET`，改挂载 RSA 私钥。
- **修改文件**:
  - `seckill-mall/src/main/resources/application-prod.yml`
- **预期效果**: prod 启动时缺失任一必要配置即报错退出，杜绝静默回落 localhost。
- **潜在风险**: 部署时必须注入所有环境变量，否则无法启动。
- **回归测试**: 验证缺失任一 `?required` 变量时启动失败。

---

## H-K2. Redis 默认无口令 + 端口全网发布

- **根因分析**: `${REDIS_PASSWORD:-}` 默认空串，`ports: "6379:6379"` 绑 `0.0.0.0`。
- **修复策略**: `${REDIS_PASSWORD:?required}` 必填；端口绑 `127.0.0.1`；`--protected-mode yes`。
- **修改文件**:
  - `docker-compose.yml`
  - `seckill-mall/src/main/resources/application-prod.yml`
- **预期效果**: Redis 不可匿名访问，端口不暴露到公网。
- **回归测试**: 验证无 REDIS_PASSWORD 时 compose 启动失败；验证 6379 仅绑回环。

---

## H-K3. MySQL 弱口令 + 端口发布 + root 暴露

- **根因分析**: `root123`/`seckill123` 弱口令，3306 发布到 `0.0.0.0`。
- **修复策略**: 全部 `${...:?required}` 必填；端口绑 `127.0.0.1`。
- **修改文件**:
  - `docker-compose.yml`
- **预期效果**: MySQL 口令必填，端口不暴露到公网。
- **回归测试**: 验证无 MYSQL_PASSWORD 时 compose 启动失败。

---

## H-K4. RabbitMQ 默认 guest/guest + 15672 硬编码发布

- **根因分析**: 管理端口 15672 无变量化硬编码发布，配默认 guest/guest。
- **修复策略**: 口令必填；15672 绑 `127.0.0.1`。
- **修改文件**:
  - `docker-compose.yml`
- **预期效果**: RabbitMQ 管理端口不暴露到公网，口令必填。
- **回归测试**: 验证无 RABBITMQ_PASSWORD 时 compose 启动失败。

---

## H-K5. 应用以 MySQL root + 空口令连库（dev）

- **根因分析**: `${DB_USERNAME:root}` / `${DB_PASSWORD:}` / `useSSL=false` / `characterEncoding=utf8`。
- **修复策略**: 建专用业务账号 `seckill_app`；`useSSL=true` + `characterEncoding=utf8mb4`。
- **修改文件**:
  - `seckill-mall/src/main/resources/application-dev.yml`
- **预期效果**: dev 环境使用业务账号而非 root，连接参数对齐生产。
- **回归测试**: 验证 dev 启动使用 seckill_app 账号。

---

## M-S1. 登录接口缺 @RateLimit 限流

- **根因分析**: `AuthController.login` 无限流，可被横向爆破。
- **修复策略**: 登录接口加 `@RateLimit(key="login", capacity=10, rate=10, seconds=60)`，按 IP 维度限流。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/controller/AuthController.java`
- **预期效果**: 同一 IP 60s 内最多 10 次登录尝试。
- **回归测试**: 验证同 IP 60s 内第 11 次登录被 429 拒绝。

---

## M-S2. Banner 管理接口入参缺 @Valid

- **根因分析**: `BannerController` 的 `@RequestBody BannerVO` 前无 `@Valid`，`BannerVO` 无校验注解。
- **修复策略**: 拆分 `BannerCreateRequest`/`BannerUpdateRequest` DTO，补 jakarta.validation 约束，Controller 加 `@Valid`。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/dto/BannerCreateRequest&Request.java`
  - `seckill-mall/src/main/java/com/seckill/mall/controller/BannerController.java`
- **预期效果**: 非法入参（空 title、非 http URL）被 400 拒绝。
- **回归测试**: 验证空 title 返回 400；验证非 http URL 返回 400。

---

## M-S4. 密码喷洒攻击

- **根因分析**: 登录失败锁定仅按用户名维度，同 IP 不同用户名可绕过。
- **修复策略**: 叠加按 IP 维度的失败计数（`login:fail:ip:{ip}`），阈值 20 次/15 分钟。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/service/impl/AuthServiceImpl.java`
- **预期效果**: 同 IP 对不同用户名爆破超过 20 次被临时封禁。
- **回归测试**: 验证同 IP 20 次失败后登录被锁定。

---

## M-S5. 用户名枚举

- **根因分析**: 不存在用户返回 500（`t_login_log.user_id` NOT NULL 约束异常），存在用户返回 1003，可区分。
- **修复策略**:
  1. `t_login_log.user_id` 改为允许 NULL
  2. `writeLoginLog` 内 try-catch 包裹，日志写入失败不影响主流程
  3. 统一返回"用户名或密码错误"
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/service/impl/AuthServiceImpl.java`
  - `sql/01_schema.sql`
- **预期效果**: 不存在用户与存在用户返回相同错误，无法枚举。
- **回归测试**: 验证不存在用户登录返回 403 而非 500。

---

## M-S6. HTTP 状态码恒为 200

- **根因分析**: `GlobalExceptionHandler` 所有异常返回 HTTP 200，破坏 REST 语义。
- **修复策略**: 为不同异常设置对应 HTTP 状态（400/401/403/404/429/500），body 保留业务 code。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/common/GlobalExceptionHandler.java`
- **预期效果**: 限流返回 429，未授权返回 401，禁止返回 403，参数错误返回 400。
- **回归测试**: 验证各异常类型返回正确 HTTP 状态码。

---

## M-K1. .gitignore 规则无法覆盖 .env.*

- **根因分析**: 旧规则 `*.env` 匹配不到 `.env.development`。
- **修复策略**: 改为 `.env` / `.env.*` / `*.env` + `!.env.example` / `!.env.*.example`。
- **修改文件**:
  - `.gitignore`
- **预期效果**: 所有 `.env.*` 变体被忽略，仅保留 `.example` 模板。
- **回归测试**: 验证 `.env.development` 被忽略，`.env.example` 不被忽略。

---

## M-K2. CORS 生产未覆盖 + setAllowedOriginPatterns

- **根因分析**: CORS 用 `setAllowedOriginPatterns`（模式匹配），生产未显式注入。
- **修复策略**: 改用 `setAllowedOrigins`（精确匹配）；生产环境校验不允许通配 `*`；抽成配置项。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/config/SecurityConfig.java`
  - `seckill-mall/src/main/resources/application-prod.yml`
- **预期效果**: CORS 仅允许显式列出的来源，生产不允许通配。
- **回归测试**: 验证生产配置含 `*` 时启动失败。

---

## M-K3. 容器以 root 运行 + 私钥烘焙 + 无 HEALTHCHECK

- **根因分析**: Dockerfile 无非 root 用户，私钥 COPY 进镜像，无优雅停机/健康检查。
- **修复策略**: 建 `app` 用户 + `USER app` + `exec java` + `HEALTHCHECK`；私钥改运行时挂载。
- **修改文件**:
  - `seckill-mall/Dockerfile`
- **预期效果**: 容器以非 root 运行，支持优雅停机，健康检查自动探测。
- **回归测试**: 验证容器内 `whoami` 返回 app；验证 SIGTERM 优雅停机。

---

## M-K4. t_seckill_activity 建表缺 COLLATE

- **根因分析**: `seckill_activity_migration.sql` 缺 `COLLATE=utf8mb4_general_ci`，主键用 `AUTO_INCREMENT`。
- **修复策略**: 补 `COLLATE=utf8mb4_general_ci`；主键改 `BIGINT NOT NULL`（雪花算法）；status 改 TINYINT。
- **修改文件**:
  - `sql/seckill_activity_migration.sql`
- **预期效果**: 表字符集对齐项目其他表，主键策略一致。
- **回归测试**: 验证建表后 `SHOW CREATE TABLE` 含 COLLATE=utf8mb4_general_ci。

---

## M-K5. 01_schema.sql 无环境护栏

- **根因分析**: 对 21 张表执行 `DROP TABLE IF EXISTS` 无环境护栏，误连生产库即破坏。
- **修复策略**: 开头加环境断言（`@schema_destructive_allowed` 变量检查），DBA 需显式确认才执行。
- **修改文件**:
  - `sql/01_schema.sql`
- **预期效果**: 误执行时输出警告提醒。
- **回归测试**: 验证未设置 `@schema_destructive_allowed=1` 时输出警告。

---

## M-K6. t_user_coupon / t_product_review 缺索引

- **根因分析**: `t_user_coupon` 缺 `(user_id, status, create_time)` 复合索引；`t_product_review` 缺 `(product_id, status, create_time)`。
- **修复策略**: 补复合索引。
- **修改文件**:
  - `sql/01_schema.sql`
- **预期效果**: "我的优惠券"和商品评论分页查询加速。
- **回归测试**: 验证 `EXPLAIN` 使用新索引。

---

## M-D1. Banner VO 双向用作请求体与响应体

- **根因分析**: `BannerVO` 同时用作请求体和响应体，违反单一职责。
- **修复策略**: 新建 `dto/BannerCreateRequest.java` 和 `dto/BannerUpdateRequest.java`，从 BannerVO 拆出请求体。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/dto/BannerCreateRequest.java`
  - `seckill-mall/src/main/java/com/seckill/mall/dto/BannerUpdateRequest.java`
  - `seckill-mall/src/main/java/com/seckill/mall/service/BannerService.java`
  - `seckill-mall/src/main/java/com/seckill/mall/service/impl/BannerServiceImpl.java`
  - `seckill-mall/src/main/java/com/seckill/mall/controller/BannerController.java`
- **预期效果**: 请求体与响应体分离，各司其职。
- **回归测试**: 验证新增/编辑接口使用新 DTO。

---

## M-F9. buyer01 密码不一致

- **根因分析**: seckill_mall.sql（已移除）中 buyer01 密码与 data.sql 不一致。
- **修复策略**: 移除含 PII 的 seckill_mall.sql；验证 data.sql 中 buyer01 哈希与 buyer123 匹配（已确认正确）。
- **修改文件**:
  - `seckill-mall/src/main/resources/sql/data.sql`（脱敏 + 注释清理）
- **预期效果**: buyer01/buyer123 可正常登录。
- **回归测试**: 验证 buyer01/buyer123 登录成功。

---

## M-F10. Banner 标题为空

- **根因分析**: `BannerServiceImpl.create` 未校验 title 非空。
- **修复策略**: `BannerCreateRequest.title` 加 `@NotBlank`；Service 层防御性二次校验。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/dto/BannerCreateRequest.java`
  - `seckill-mall/src/main/java/com/seckill/mall/service/impl/BannerServiceImpl.java`
- **预期效果**: 空 title 被 400 拒绝。
- **回归测试**: 验证空 title 返回 400。

---

## M-F11. 注册接口验证码

- **根因分析**: SecurityConfig 放行注册接口但验证码校验失败时返回 200（M-S6 修复前）。
- **修复策略**: 通过 M-S6 的 GlobalExceptionHandler 修复，验证码错误（CAPTCHA_ERROR）现返回 400。
- **修改文件**:
  - `seckill-mall/src/main/java/com/seckill/mall/common/GlobalExceptionHandler.java`
- **预期效果**: 注册时验证码错误返回 400，区分"未登录"（放行）和"校验失败"（400）。
- **回归测试**: 验证注册时错误验证码返回 400。

---

## L-S1. /actuator/health 匿名可访问泄露拓扑

- **修复策略**: 生产 `management.endpoint.health.show-details=never` + `show-components=never`。
- **修改文件**: `application-prod.yml`、`application.yml`
- **回归测试**: 验证生产 `/actuator/health` 不含 details。

---

## L-S2. 缺少 CSP 与 HSTS 响应头

- **修复策略**: SecurityConfig `.headers()` 添加 HSTS、CSP、X-Content-Type-Options、Referrer-Policy、X-Frame-Options、X-XSS-Protection。
- **修改文件**: `SecurityConfig.java`
- **回归测试**: 验证响应头含 CSP/HSTS。

---

## L-S3. CORS allowCredentials=true 且白名单靠配置兜底

- **修复策略**: 生产环境校验 `allowed-origins` 不含通配 `*`；改用 `setAllowedOrigins` 精确匹配。
- **修改文件**: `SecurityConfig.java`、`application-prod.yml`
- **回归测试**: 验证生产配置含 `*` 时启动失败。

---

## L-S4. 图形验证码强度不足

- **修复策略**: 在 `application.yml` 中记录 TODO，建议高风险场景升级为行为验证码（滑块/点选）。
- **修改文件**: `application.yml`
- **回归测试**: N/A（建议性优化）。

---

## L-S6. 种子数据弱口令明文写注释

- **修复策略**: `data.sql` 删除明文口令注释，改为"口令已 BCrypt 加密，明文不入库不入注释"。
- **修改文件**: `data.sql`
- **回归测试**: 验证 data.sql 不含明文口令。

---

## L-S7. 开发环境信息泄露

- **修复策略**: `info.app.*` 收敛为仅 `name: seckill-mall`；`/actuator/info` 需 ADMIN 权限；`info.env.enabled=false`。
- **修改文件**: `application.yml`、`application-prod.yml`、`SecurityConfig.java`
- **回归测试**: 验证 `/actuator/info` 需 ADMIN 权限。

---

## L-O1. 秒杀防重放错误码优化

- **修复策略**: `ReplayProtectionFilter` 返回 403 + "缺少秒杀令牌"，区分未登录（401）和缺少令牌（403）。
- **修改文件**: `ReplayProtectionFilter.java`
- **回归测试**: 验证缺少 token 返回 403 + 明确消息。

---

## L-O2. 登录失败日志补全

- **修复策略**: `writeLoginLog` 补全 `userAgent` 和 `loginLocation` 字段；`t_login_log` 加 `idx_ip_time` 索引。
- **修改文件**: `AuthServiceImpl.java`、`sql/01_schema.sql`
- **回归测试**: 验证登录日志含 userAgent/loginLocation。

---

## L-O3. 慢查询监控 / 链路追踪

- **修复策略**: `application.yml` 配置 MyBatis 慢查询日志（`log-impl=Slf4jImpl` + `default-statement-timeout=30`）；添加 Sleuth/Zipkin 配置注释。
- **修改文件**: `application.yml`
- **回归测试**: 验证慢 SQL 以 WARN 级别记录。

---

## 额外任务：为组B补 RabbitMQ 配置

- **修复策略**: `application.yml` / `application-dev.yml` / `application-prod.yml` 均添加 `publisher-confirm-type: correlated` + `publisher-returns: true`。
- **修改文件**: `application.yml`、`application-dev.yml`、`application-prod.yml`
- **预期效果**: 组B 的 H-C5 修复可基于此配置开启 publisher confirm/returns。