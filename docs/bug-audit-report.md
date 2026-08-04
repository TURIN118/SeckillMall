# 🔍 SpringBoot 秒杀电商项目 — 全量 Bug 排查报告

> **项目名称**：seckill-mall（高并发秒杀电商平台）
> **技术栈**：Spring Boot 3.2.5 + MyBatis-Plus + Redis + RabbitMQ + Vue3 + TypeScript
> **审查日期**：2026-08-04
> **审查范围**：后端 230 个 Java 文件 + 前端 71 个 TS/Vue 文件
> **审查方式**：4 个专项审查子代理并行审查（安全 / 业务逻辑 / 数据层 / 前端）

---

## 📊 总体统计

| 严重等级 | 数量 | 占比 | 说明 |
|---------|------|------|------|
| 🔴 **Critical（严重）** | **8** | 7.4% | 必须立即修复，可导致系统被接管或核心功能不可用 |
| 🟠 **High（高）** | **25** | 23.1% | 本周修复，涉及安全漏洞、数据一致性、超卖等 |
| 🟡 **Medium（中）** | **47** | 43.5% | 迭代修复，输入校验、性能、信息泄露等 |
| 🟢 **Low（低）** | **28** | 25.9% | 优化项，编码规范、日志、脱敏等 |
| **合计** | **108** | 100% | |

### 按模块分布

| 审查模块 | Critical | High | Medium | Low | 小计 |
|---------|----------|------|--------|-----|------|
| 后端安全（Controller/Security/Config） | 3 | 7 | 11 | 6 | 27 |
| 后端业务逻辑（Service） | 1 | 6 | 8 | 14 | 29 |
| 后端数据层（Mapper/Entity/DTO/Cache） | 1 | 5 | 19 | 5 | 30 |
| 前端（Vue/TypeScript） | 3 | 7 | 9 | 3 | 22 |
| **合计** | **8** | **25** | **47** | **28** | **108** |

---

## 🔴 一、Critical（严重）问题 — 共 8 个

> ⚠️ **这些问题可导致系统被完全接管、核心功能不可用或造成直接经济损失，必须立即修复。**

### C1. 被禁用用户仍可访问系统（认证绕过）

| 项目 | 内容 |
|------|------|
| **模块** | 后端安全 |
| **文件** | `seckill-mall/src/main/java/com/seckill/mall/security/JwtAuthenticationFilter.java` |
| **行号** | E:82-83 |
| **问题类型** | 认证绕过 / 账户状态未校验 |

**问题描述**：
```java
// Token 通过校验即视为有效用户，状态默认启用
user.setStatus(UserStatus.ACTIVE);
```
JWT Filter 在构建 `SecurityUserDetails` 时，将用户状态硬编码为 `ACTIVE`，完全忽略数据库中用户的真实状态。

**风险分析**：管理员禁用用户后，该用户已签发的 JWT 仍有效且被视为 ACTIVE，**被禁用用户仍可正常访问所有接口**（下单、支付、修改资料等）。用户禁用功能形同虚设。

**解决方案**：从数据库实时查询用户状态（建议加 Redis 缓存，TTL 30s）：
```java
User dbUser = userMapper.selectById(userId);
if (dbUser == null || dbUser.getStatus() != UserStatus.ACTIVE) {
    SecurityContextHolder.clearContext();
    return;
}
return new SecurityUserDetails(dbUser);
```

---

### C2. 重放保护签名密钥硬编码默认值

| 项目 | 内容 |
|------|------|
| **模块** | 后端安全 |
| **文件** | `seckill-mall/src/main/java/com/seckill/mall/security/ReplayProtectionFilter.java` |
| **行号** |E:48-49 |
| **问题类型** | 硬编码密钥 / 签名伪造 |

**问题描述**：
```java
@Value("${seckill.security.sign-secret:wnj-seckill-sign-secret-2024}")
private String signSecret;
```
当配置文件未配置时，使用代码中硬编码的默认值 `wnj-seckill-sign-secret-2024`。

**风险分析**：该默认值已写入源码（可能已提交 Git），攻击者可利用该密钥**伪造合法签名**，绕过秒杀接口的重放保护，实现批量秒杀、刷单、恶意抢购。

**解决方案**：去掉默认值强制要求配置，并校验密钥长度：
```java
@Value("${seckill.security.sign-secret}")
private String signSecret;

@PostConstruct
public void validate() {
    if (signSecret == null || signSecret.length() < 32) {
        throw new IllegalStateException("seckill.security.sign-secret 必须配置且长度不少于32字符");
    }
}
```

---

### C3. Actuator 端点未授权暴露

| 项目 | 内容 |
|------|------|
| **模块** | 后端安全 |
| **文件** | `seckill-mall/src/main/java/com/seckill/mall/config/SecurityConfig.java` |
| **行号** |E:68 |
| **问题类型** | 未授权访问 / 信息泄露 / 潜在 RCE |

**问题描述**：`"/actuator/**"` 全部 `permitAll()`，Spring Boot Actuator 所有端点无需认证即可访问。

**风险分析**：
- `/actuator/env`：泄露数据库连接串、JWT 密钥等敏感配置
- `/actuator/heapdump`：可下载 JVM 堆内存快照，提取内存中的密码、token
- `/actuator/shutdown`：可导致远程服务关闭（DoS）

**解决方案**：
```java
.requestMatchers("/actuator/health").permitAll()  // 仅健康检查放行
.requestMatchers("/actuator/**").hasRole("ADMIN") // 其余端点需 ADMIN
```

---

### C4. 秒杀活动状态永不更新，秒杀永远无法进行

| 项目 | 内容 |
|------|------|
| **模块** | 后端业务逻辑 |
| **文件** | `seckill-mall/src/main/java/com/seckill/mall/service/impl/SeckillServiceImpl.java` |
| **行号** |E:96 |
| **问题类型** | 业务逻辑错误 / 状态机错误 |

**问题描述**：`createSeckill` 创建活动时将 `status` 设为 `PENDING`，`cancelSeckill` 设为 `CANCELLED`，但**全代码库没有任何定时任务或逻辑将 status 更新为 `ACTIVE` 或 `ENDED`**（已确认无 `@Scheduled`）。

`preheatSeckill` 将 DB 的 `status`（即 `"PENDING"`）写入 Redis Hash。`doSeckill` 判断：
```java
if (now.isBefore(startTime) || SeckillStatus.PENDING.getCode().equals(status)) {
    throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
}
```
即使活动到达 `startTime`，第二个条件 `SeckillStatus.PENDING.getCode().equals(status)` 恒为 true，**永远抛 SECKILL_NOT_STARTED**。

**风险分析**：秒杀活动创建后永远无法进入"进行中"状态，**核心秒杀功能完全不可用**，属于阻断性缺陷。

**解决方案**（推荐方案一）：`doSeckill` 中移除对 DB `status` 的 PENDING/ENDED 判断，仅依赖时间窗口：
```java
if (SeckillStatus.CANCELLED.getCode().equals(status)) {
    throw new BusinessException(ErrorCode.SECKILL_CANCELLED);
}
if (now.isBefore(startTime)) {
    throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
}
if (now.isAfter(endTime)) {
    throw new BusinessException(ErrorCode.SECKILL_ENDED);
}
```

---

### C5. SQL 注入风险 — ORDER BY 使用 ${} 拼接

| 项目 | 内容 |
|------|------|
| **模块** | 后端数据层 |
| **文件** | `seckill-mall/src/main/resources/mapper/SeckillOrderMapper.xml` |
| **行号** |E:138 |
| **问题类型** | SQL 注入 |

**问题描述**：
```xml
ORDER BY ${req.sortByColumn} ${req.sortOrder}
```
使用 `${}` 进行字符串拼接而非 `#{}` 参数绑定，直接将参数值替换到 SQL 语句中。虽然注释声称"调用方进行白名单校验"，但 XML 层无防御，属于"假设安全"的反模式。

**风险分析**：一旦 Service 层白名单逻辑存在缺陷，攻击者可注入 `sortByColumn = "1; DROP TABLE t_seckill_order; --"`，执行任意 SQL。

**解决方案**：使用 `<choose>` 标签做白名单映射：
```xml
ORDER BY
<choose>
    <when test="req.sortByColumn == 'create_time'">create_time</when>
    <when test="req.sortByColumn == 'pay_time'">pay_time</when>
    <when test="req.sortByColumn == 'total_amount'">total_amount</when>
    <otherwise>create_time</otherwise>
</choose>
<choose>
    <when test="req.sortOrder == 'asc'">ASC</when>
    <otherwise>DESC</otherwise>
</choose>
```

---

### C6. 前端 XSS 漏洞 — v-html 渲染未净化的富文本

| 项目 | 内容 |
|------|------|
| **模块** | 前端 |
| **文件** | `frontend/src/views/front/ProductDetail.vue` |
| **行号** |E:188 |
| **问题类型** | XSS（跨站脚本攻击） |

**问题描述**：
```html
<div v-if="product.detailHtml" class="desc-content" v-html="product.detailHtml"></div>
```
直接使用 `v-html` 渲染后端返回的 `product.detailHtml` 字段，未做任何 HTML 净化/转义处理。攻击者可绕过前端编辑器直接调用 API 注入 `<script>` 脚本。

**风险分析**：盗取用户 token、劫持秒杀下单、篡改页面、XSS 蠕虫传播。秒杀场景下可构造恶意商品详情诱导管理员/买家访问，窃取管理员凭证后横向渗透后台。

**解决方案**：引入 DOMPurify 在渲染前净化：
```ts
import DOMPurify from 'dompurify'
const safeDetailHtml = computed(() =>
  product.value?.detailHtml ? DOMPurify.sanitize(product.value.detailHtml) : ''
)
```

---

### C7. 秒杀详情页 ACTIVE 状态倒计时不会实时更新（响应式失效）

| 项目 | 内容 |
|------|------|
| **模块** | 前端 |
| **文件** | `frontend/src/views/front/SeckillDetail.vue` |
| **行号** |E:571-577, 911-919 |
| **问题类型** | 响应式问题 / 秒杀逻辑缺陷 |

**问题描述**：`countdownRemaining` 在 `ACTIVE` 分支中调用 `dayjs(countdownTarget.value).diff(dayjs(), 'second')`，但 `dayjs()` 不是响应式数据，computed 不会随时间推移重新计算。ACTIVE 状态下倒计时数字永远停留在首次计算的值，不会逐秒递减。

**风险分析**：秒杀活动结束后用户仍可点击"立即抢购"，产生无效请求；`handleActiveEnd` 不触发，库存轮询 `setInterval` 持续运行造成资源浪费。

**解决方案**：引入每秒更新的 `now` ref 驱动 computed：
```ts
const now = ref(Date.now())
let nowTimer: ReturnType<typeof setInterval> | null = null
onMounted(() => { nowTimer = setInterval(() => { now.value = Date.now() }, 1000) })
onUnmounted(() => { if (nowTimer) clearInterval(nowTimer) })
const countdownRemaining = computed(() => {
  if (!countdownTarget.value) return 0
  if (seckill.value?.status === 'PENDING') return pendingCountdown.value
  const remaining = dayjs(countdownTarget.value).diff(dayjs(now.value), 'second')
  return Math.max(0, remaining)
})
```

---

### C8. 倒计时组件定时器间隔切换逻辑缺陷

| 项目 | 内容 |
|------|------|
| **模块** | 前端 |
| **文件** | `frontend/src/components/SeckillCountdown.vue` |
| **行号** |E:95-109 |
| **问题类型** | 内存泄漏 / 性能问题 / 秒杀逻辑 |

**问题描述**：`startTimer` 中 `interval` 是闭包变量，在 setInterval 回调中判断 `interval !== 100` 来决定是否切换到 100ms，但 `interval` 闭包值永不改变，导致每秒重建定时器，产生大量定时器抖动。

**风险分析**：秒杀最后 60 秒（最关键阶段）倒计时组件性能异常，可能导致浏览器卡顿；多个定时器并发更新 `remaining` 产生竞态，倒计时数字乱跳。

**解决方案**：重构为单一定时器 + 动态间隔：
```ts
function startTimer(): void {
  clearTimer()
  computeRemaining()
  if (expired.value) return
  const tick = () => {
    computeRemaining()
    if (expired.value) return
    const delay = remaining.value <= 60 ? 100 : 1000
    timer = setTimeout(tick, delay)
  }
  const delay = remaining.value <= 60 ? 100 : 1000
  timer = setTimeout(tick, delay)
}
```

---

## 🟠 二、High（高）问题 — 共 25 个

> ⚠️ **这些问题涉及安全漏洞、数据一致性、超卖、内存泄漏等，建议本周修复。**

### 后端安全（7 个）

| 编号 | 问题 | 文件 | 行号 | 解决方案概要 |
|------|------|------|------|-------------|
| H1 | CORS 允许任意来源携带凭证 | `SecurityConfig.java` | E:95,98 | 明确列出允许的前端域名，禁止 `*` + credentials |
| H2 | CORS 反射 Origin（3 处） | `ReplayProtectionFilter.java` / `JwtAuthenticationEntryPoint.java` / `JwtAccessDeniedHandler.java` | E:114-118 / E:37-39 / E:37-39 | 统一由 `SecurityConfig.corsConfigurationSource()` 管理 CORS |
| H3 | 日志泄露签名密钥计算结果 | `ReplayProtectionFilter.java` | E:104 | 只记录签名不匹配事实，不输出 expected 值 |
| H4 | Token 黑名单校验 Fail-Open | `TokenBlacklistService.java` | E:45-53 | Redis 异常时采用 Fail-Closed 策略返回 true |
| H5 | 越权查询秒杀结果 | `SeckillController.java` | E:94-100 | 显式获取当前用户 userId 并传入 Service 校验归属 |
| H6 | 文件上传接口权限与校验不足 | `UploadController.java` | E:29-35 | 加 `@PreAuthorize` 角色限制 + 文件类型/大小校验 + 限流 |
| H7 | 验证码发送接口无防护（短信轰炸） | `VerificationCodeController.java` | E:37-49 | 加 `@Valid` + `@RateLimit` + 图形验证码保护 |

### 后端业务逻辑（6 个）

| 编号 | 问题 | 文件 | 行号 | 解决方案概要 |
|------|------|------|------|-------------|
| H8 | 优惠券领取/发放并发超卖 | `CouponServiceImpl.java` | E:202-225, 145-165 | 使用乐观锁或原子更新 `WHERE received_count < total_count` |
| H9 | 事务内操作 Redis 导致缓存与 DB 不一致 | `SeckillGoodsServiceImpl.java` | E:119, 168-169 | 缓存操作移到事务提交后执行（`TransactionSynchronization`） |
| H10 | 订单取消事务内回补 Redis 库存导致超卖 | `OrderServiceImpl.java` | E:188, 220 | `rollbackStock` 移到事务提交后执行 |
| H11 | 统一订单列表全量查询内存分页 | `OrderServiceImpl.java` | E:668-711 | 改为 DB 层分页或两路归并，避免全量加载 |
| H12 | MQ 发送异常导致库存泄漏 | `SeckillServiceImpl.java` | E:139 | try-catch 回补 Lua 预减库存与已购标记 |
| H13 | 分类树商品计数全表扫描 | `CategoryServiceImpl.java` | E:240-242 | 改为 DB 聚合查询 `GROUP BY` |

### 后端数据层（5 个）

| 编号 | 问题 | 文件 | 行号 | 解决方案概要 |
|------|------|------|------|-------------|
| H14 | 布隆过滤器初始化全表加载 OOM | `SeckillBloomInitializer.java` | E:38 | 分页加载 + 仅查询 ID 字段 + 异常容错 |
| H15 | Redis 连接泄漏 | `CacheDegradeService.java` | E:62 | finally 中显式关闭 connection |
| H16 | User 实体 password 字段序列化暴露 | `User.java` | E:31 | 添加 `@JsonIgnore` 注解 |
| H17 | RechargeCardVO 卡密明文泄露风险 | `RechargeCardVO.java` | E:28 | 拆分为查询 VO 和生成 VO，隔离敏感字段 |
| H18 | 商品详情 HTML 存储型 XSS | `ProductCreateRequest.java` | E:34 | 使用 jsoup/OWASP Sanitizer 白名单清洗 |

### 前端（7 个）

| 编号 | 问题 | 文件 | 行号 | 解决方案概要 |
|------|------|------|------|-------------|
| H19 | 购物车 quantityTimers 未清理（内存泄漏） | `Cart.vue` | E:145 | 增加 `onDeactivated`/`onUnmounted` 清理 |
| H20 | SeckillDetail setTimeout 未清理 | `SeckillDetail.vue` | E:808-810 | 保存 timer 引用并在 cleanup 中清理 |
| H21 | pollResult 轮询无取消机制 | `SeckillDetail.vue` / `stores/seckill.ts` | E:840-867 / E:88-106 | 引入取消标志位，组件卸载时取消轮询 |
| H22 | 路由参数 ID 使用 Number() 大整数精度丢失 | `SeckillDetail.vue` / `ProductDetail.vue` | E:618-620 / E:361-363 | 直接使用字符串，API 已支持 `number \| string` |
| H23 | 登录页 redirect 参数未校验（开放重定向） | `Login.vue` | E:166-167 | 校验 redirect 必须以 `/` 开头且非 `//` |
| H24 | 路由守卫 fetchUserInfo 失败后继续放行 | `router/index.ts` | E:288-294 | 失败时跳转登录，不放行 |
| H25 | 首页秒杀卡片原价估算（虚假数据） | `Home.vue` | E:376 | 移除 `price/0.7` 估算，无原价时不显示划线价 |

---

## 🟡 三、Medium（中）问题 — 共 47 个

> 这些问题涉及输入校验缺失、性能、信息泄露、响应式问题等，建议迭代修复。

### 后端安全（11 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| M1 | updateProfile 缺少 @Valid 校验 | `AuthController.java` | E:88 |
| M2 | 信任 X-Forwarded-For 导致 IP 伪造 | `AuthController.java` | E:124-136 |
| M3 | 购物车接口缺少输入校验 | `CartController.java` | E:53, 60-61 |
| M4 | Admin 接口使用 Map 接收参数缺少校验 | `AdminCouponController.java` | E:74-77, 81-88 |
| M5 | 评论内容未过滤 XSS | `ProductReviewController.java` | E:51-55 |
| M6 | CSRF 防护全局禁用 | `SecurityConfig.java` | E:45 |
| M7 | API 文档生产环境暴露 | `SecurityConfig.java` | E:63-67 |
| M8 | 静态资源映射潜在目录穿越 | `WebMvcConfig.java` | E:25-26 |
| M9 | 账户锁定状态检查不完整 | `SecurityUserDetails.java` | E:57-69 |
| M10 | Excel 导出潜在 DoS | `SystemController.java` | E:73-75 |
| M11 | 验证码校验接口无防暴力破解 | `VerificationCodeController.java` | E:53-61 |

### 后端业务逻辑（8 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| M12 | 秒杀订单支付不支持钱包扣款 | `OrderServiceImpl.java` | E:141-172 |
| M13 | doSeckill 时间解析无异常处理 | `SeckillServiceImpl.java` | E:90-91 |
| M14 | ProductServiceImpl RLock unlock 风险 | `ProductServiceImpl.java` | E:212-214 |
| M15 | 加购数量无上限校验 | `CartServiceImpl.java` | E:102 |
| M16 | 优惠券领取重复检查无原子保证 | `CouponServiceImpl.java` | E:207-213 |
| M17 | 系统统计秒杀活动状态不准 | `SystemServiceImpl.java` | E:598-635 |
| M18 | @Async + @Retryable 组合可能重试失效 | `EmailServiceImpl.java` | E:55-57 |
| M19 | cancelSeckill 中 evictCache 在事务内 | `SeckillGoodsServiceImpl.java` | E:183 |

### 后端数据层（19 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| M20 | AdminOrderQueryRequest 分页参数无校验 | `AdminOrderQueryRequest.java` | E:19-24 |
| M21 | ProfileUpdateRequest email/phone 无格式校验 | `ProfileUpdateRequest.java` | E:24, 29 |
| M22 | PhoneUpdateRequest phone 无格式校验 | `PhoneUpdateRequest.java` | E:19 |
| M23 | EmailUpdateRequest email 无格式校验 | `EmailUpdateRequest.java` | E:19 |
| M24 | UserListRequest 分页和枚举无校验 | `UserListRequest.java` | E:14-22 |
| M25 | OperationLogQueryRequest 分页无校验 | `OperationLogQueryRequest.java` | E:14-15 |
| M26 | UserRoleUpdateRequest 角色无枚举校验 | `UserRoleUpdateRequest.java` | E:16 |
| M27 | UserStatusUpdateRequest 状态无枚举校验 | `UserStatusUpdateRequest.java` | E:16 |
| M28 | CategoryStatusUpdateRequest 状态无范围校验 | `CategoryStatusUpdateRequest.java` | E:16 |
| M29 | WalletRechargeRequest 无长度限制 | `WalletRechargeRequest.java` | E:19, 23 |
| M30 | CouponCreateRequest type 无枚举校验 | `CouponCreateRequest.java` | E:31, 50, 54 |
| M31 | UserVO 手机号邮箱未脱敏 | `UserVO.java` | E:20, 22 |
| M32 | NormalOrderDetailVO 直接暴露实体 | `NormalOrderDetailVO.java` | E:23, 26 |
| M33 | RedisService.scanKeys 无数量限制 | `RedisService.java` | E:101-110 |
| M34 | Lua 脚本库存 key 无过期时间 | `seckill_deduct.lua` | E:19 |
| M35 | selectOrderTrend DATE() 函数导致索引失效 | `SeckillOrderMapper.xml` | E:59 |
| M36 | ProductMapper LIKE 前缀通配符索引失效 | `ProductMapper.xml` | E:22 |
| M37 | UserFavoriteMapper/CartMapper 使用 SELECT * | `UserFavoriteMapper.java` / `CartMapper.java` | E:34 / E:35 |
| M38 | SeckillGoodsMapper 子查询效率低 | `SeckillGoodsMapper.xml` | E:17-26 |

### 前端（9 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| M39 | SeckillZone 倒计时未使用服务器时间偏移 | `SeckillZone.vue` | E:343-345 |
| M40 | 401 响应使用 window.location.href 导致页面刷新 | `api/request.ts` | E:86-89 |
| M41 | ProductEdit wangEditor 使用 any 类型 | `ProductEdit.vue` | E:74, 232 |
| M42 | SeckillDetail 直接修改 seckill.value.status 绕过状态机 | `SeckillDetail.vue` | E:756, 768 |
| M43 | SeckillZone refreshing 为普通变量非 ref | `SeckillZone.vue` | E:229 |
| M44 | Home.vue 倒计时目标硬编码 | `Home.vue` | E:306-309 |
| M45 | stores/seckill.ts countdownTimers 为模块级变量 | `stores/seckill.ts` | E:12 |
| M46 | SeckillDetail handleSeckill 未做客户端防重复点击 | `SeckillDetail.vue` | E:773-812 |
| M47 | 前端商品详情 HTML 未净化（配合 C6） | `ProductDetail.vue` | E:188 |

Bash 函数校验注解批量修复示例：
```java
// 所有分页参数统一添加
@Min(value = 1, message = "页码不能小于1") private Integer pageNum;
@Min(value = 1) @Max(value = 100, message = "每页大小不能超过100") private Integer pageSize;

// 枚举字段统一添加
@Pattern(regexp = "^(BUYER|SELLER|ADMIN)$", message = "角色非法") private String role;
@Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") private String phone;
@Email(message = "邮箱格式不正确") private String email;
```

---

## 🟢 四、Low（低）问题 — 共 28 个

> 这些问题涉及编码规范、日志、脱敏等，可在日常迭代中逐步优化。

### 后端安全（6 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| L1 | doSeckill 未显式传入 userId | `SeckillController.java` | E:76-82 |
| L2 | logout 强制要求 Authorization 头 | `AuthController.java` | E:68 |
| L3 | 交易记录泄露完整充值卡号 | `WalletController.java` | E:103 |
| L4 | 用户名枚举风险 | `SecurityUserDetailsService.java` | E:27 |
| L5 | JWT 使用 HS256 算法 | `JwtUtils.java` | E:71 |
| L6 | 日志线程池丢弃策略 | `AsyncConfig.java` | E:50 |

### 后端业务逻辑（14 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| L7 | preheatSeckill 对已结束活动 TTL 兜底 | `SeckillGoodsServiceImpl.java` | E:212 |
| L8 | payOrder 邮件发送无 try-catch | `OrderServiceImpl.java` | E:166-170 |
| L9 | createOrderFromCart 重复 cartIds 误报 | `OrderServiceImpl.java` | E:334 |
| L10 | rollbackProductStock 回补失败仅日志 | `OrderServiceImpl.java` | E:576-580 |
| L11 | ProductStatus.valueOf 无异常处理 | `ProductServiceImpl.java` | E:97 |
| L12 | clearCart 循环更新 cart_count | `CartServiceImpl.java` | E:167 |
| L13 | updateProfile 手机号空字符串处理 | `AuthServiceImpl.java` | E:233 |
| L14 | 评论未校验购买 | `ProductReviewServiceImpl.java` | E:74 |
| L15 | sendSmsCode System.out.println | `VerificationCodeServiceImpl.java` | E:90-94 |
| L16 | verifyCode 非原子 | `VerificationCodeServiceImpl.java` | E:98-116 |
| L17 | BannerServiceImpl updateStatus 无校验 | `BannerServiceImpl.java` | E:112-123 |
| L18 | RechargeCard generate 循环 insert | `RechargeCardServiceImpl.java` | E:72 |
| L19 | isDescendant 循环查询 DB | `CategoryServiceImpl.java` | E:200-219 |
| L20 | distribute 全量更新 coupon | `CouponServiceImpl.java` | E:164-165 |

### 后端数据层（5 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| L21 | RedisKeyConstants key 风格不一致 | `RedisKeyConstants.java` | E:21 |
| L22 | 枚举缺少 @JsonCreator 反序列化支持 | `entity/enums/*.java` | - |
| L23 | CacheDegradeService 健康检测并发竞态 | `CacheDegradeService.java` | E:34-43 |
| L24 | SeckillCreateRequest @JsonFormat 与全局配置冲突 | `SeckillCreateRequest.java` | E:39, 43 |
| L25 | SeckillLuaService 缺少空值检查 | `SeckillLuaService.java` | E:43-48 |

### 前端（3 个）

| 编号 | 问题 | 文件 | 行号 |
|------|------|------|------|
| L26 | SeckillButton 防重复点击依赖父组件 | `SeckillButton.vue` | E:122-127 |
| L27 | api/request.ts 错误消息读取可能失败 | `api/request.ts` | E:104 |
| L28 | utils/image.ts formatImageUrl 未校验 URL 合法性 | `utils/image.ts` | E:5-12 |

---

## 📋 五、修复优先级建议

### 🔴 P0 — 立即修复（Critical，8 个）

这些问题可导致系统被完全接管、核心功能不可用或造成直接经济损失：

1. **C1** 被禁用用户仍可访问系统 — 认证绕过
2. **C2** 重放保护签名密钥硬编码 — 签名伪造
3. **C3** Actuator 端点未授权暴露 — 信息泄露/RCE
4. **C4** 秒杀活动状态永不更新 — 核心功能不可用 ⚠️
5. **C5** SQL 注入风险 — 数据库被接管
6. **C6** 前端 XSS 漏洞 — 用户 token 被盗
7. **C7** 秒杀倒计时不更新 — 秒杀逻辑失效
8. **C8** 倒计时定时器抖动 — 秒杀关键期卡顿

### 🟠 P1 — 本周修复（High，25 个）

涉及安全漏洞、数据一致性、超卖、内存泄漏等高危风险。详见上文 High 问题列表。

### 🟡 P2 — 迭代修复（Medium，47 个）

输入校验缺失、性能问题、信息泄露、响应式问题等。建议分 2-3 个迭代完成：
- 第 1 迭代：输入校验类（M1-M30）— 批量添加校验注解，工作量小收益大
- 第 2 迭代：性能优化类（M35-M38）— 索引优化、SQL 优化
- 第 3 迭代：其余 Medium 问题

### 🟢 P3 — 日常优化（Low，28 个）

编码规范、日志、脱敏等，可在日常迭代中逐步优化。

---

## 🔧 六、重点修复代码示例

### 1. 修复 C1（认证绕过）+ C4（秒杀状态）— 最关键的两个 bug

```java
// C1: JwtAuthenticationFilter.java
private SecurityUserDetails buildUserDetailsFromClaims(Claims claims) {
    Long userId = extractUserId(claims);
    // 从数据库实时查询用户状态（建议加 Redis 缓存，TTL 30s）
    User dbUser = userMapper.selectById(userId);
    if (dbUser == null || dbUser.getStatus() != UserStatus.ACTIVE) {
        SecurityContextHolder.clearContext();
        return null;
    }
    return new SecurityUserDetails(dbUser);
}

// C4: SeckillServiceImpl.doSeckill — 移除对 DB status 的 PENDING/ENDED 判断
if (SeckillStatus.CANCELLED.getCode().equals(status)) {
    throw new BusinessException(ErrorCode.SECKILL_CANCELLED);
}
if (now.isBefore(startTime)) {
    throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
}
if (now.isAfter(endTime)) {
    throw new BusinessException(ErrorCode.SECKILL_ENDED);
}
```

### 2. 修复 C6（XSS）— 前后端双保险

```ts
// 前端：ProductDetail.vue
import DOMPurify from 'dompurify'
const safeDetailHtml = computed(() =>
  product.value?.detailHtml ? DOMPurify.sanitize(product.value.detailHtml) : ''
)
// 模板：v-html="safeDetailHtml"
```

```java
// 后端：ProductServiceImpl — 入库前清洗
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

String cleanHtml = Jsoup.clean(detailHtml, Safelist.relaxed()
    .addTags("div", "span", "img", "p", "br", "strong", "em")
    .addAttributes("img", "src", "alt", "width", "height")
    .addProtocols("img", "src", "https", "http"));
product.setDetailHtml(cleanHtml);
```

### 3. 修复 H8（优惠券并发超卖）

```java
// CouponServiceImpl.receive — 原子更新
int rows = couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
    .eq(Coupon::getId, couponId)
    .lt(Coupon::getReceivedCount, coupon.getTotalCount())
    .setSql("received_count = received_count + 1"));
if (rows == 0) {
    throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
}
```

---

## 📈 七、审查质量说明

- **审查覆盖**：后端 230 个 Java 文件 + 前端 71 个 TS/Vue 文件，全量覆盖
- **审查方式**：4 个专项审查子代理并行工作，各自负责独立模块
- **问题去重**：跨模块重复问题已合并（如前后端 XSS 问题分别记录但关联标注）
- **未修改代码**：本次审查仅产出报告，未修改任何源代码

---

## 📝 八、后续操作指引

1. **请审核本报告**，确认问题分级是否合理
2. **审核通过后**，告知我开始修复，我将：
   - 优先修复 8 个 Critical 问题（P0）
   - 然后修复 25 个 High 问题（P1）
   - 最后按迭代修复 Medium/Low 问题
3. **修复过程中**，每个问题修复后将进行编译验证
4. **全部修复完成后**，将进行全量回归测试

---

*报告生成时间：2026-08-04*
*审查团队：BackendSecurityReviewer + BackendBusinessReviewer + BackendDataReviewer + FrontendReviewer*