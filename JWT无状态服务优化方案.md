# JWT 无状态服务优化方案

> **项目**：seckill-mall 秒杀电商平台  
> **编写日期**：2026-08-05  
> **版本**：v2.0（根据疑问反馈修订）  
> **状态**：待实施（本文档仅作为方案记录，不直接修改代码）

---

## 一、背景与现状

### 1.1 当前架构概述

项目采用纯 JWT 无状态服务架构，核心组件如下：

| 组件 | 文件位置 | 职责 |
|------|---------|------|
| JwtUtils | `security/JwtUtils.java` | Token 签发、解析、校验 |
| JwtAuthenticationFilter | `security/JwtAuthenticationFilter.java` | 请求级 Token 解析与身份构建 |
| TokenBlacklistService | `security/TokenBlacklistService.java` | Token 黑名单管理（Redis） |
| SecurityUserDetails | `security/SecurityUserDetails.java` | 用户身份信息载体 |
| SecurityUtils | `security/SecurityUtils.java` | 静态工具类，获取当前用户 |
| SecurityConfig | `config/SecurityConfig.java` | Spring Security 配置 |
| ReplayProtectionFilter | `security/ReplayProtectionFilter.java` | 秒杀接口重放防护 |
| JwtAuthenticationEntryPoint | `security/JwtAuthenticationEntryPoint.java` | 未认证响应处理 |
| JwtAccessDeniedHandler | `security/JwtAccessDeniedHandler.java` | 权限不足响应处理 |

### 1.2 请求鉴权完整流转

```
客户端请求
  Header: Authorization: Bearer <access_token>
  Header: X-Sign / X-Timestamp / X-Nonce (秒杀接口)
      │
      ▼
  ① ReplayProtectionFilter (@Order(HIGHEST_PRECEDENCE+20))
     仅拦截 POST /api/v1/seckill/*
     HMAC-SHA256 签名 + 时间窗口(±60s) + Nonce 去重(Redis 60s TTL)
      │
      ▼
  ② JwtAuthenticationFilter (Before UsernamePasswordFilter)
     解析 Bearer Token
     ├─ 校验签名 + 过期时间
     ├─ 校验 tokenType == ACCESS
     ├─ 校验 Token 黑名单(Redis)
     └─ 根据 userId 查 DB 构建 UserDetails → 写入 SecurityContext
      │
      ▼
  ③ Spring Security 鉴权
     ├─ URL 级: permitAll / hasRole
     └─ 方法级: @PreAuthorize("hasRole('ADMIN')")
      │
      ▼
  ④ 业务 Controller / Service
     SecurityUtils.getCurrentUserId() ← 从 SecurityContext 取身份
```

### 1.3 当前 Token 机制参数

| 参数 | 值 | 配置位置 |
|------|-----|---------|
| 签名算法 | HS256（HMAC-SHA256 对称） | `JwtUtils.java` |
| 密钥来源 | 配置文件明文 | `application-dev.yml: jwt.secret` |
| Access Token 有效期 | 2 小时（7200000ms） | `application-dev.yml: jwt.access-token-expiration` |
| Refresh Token 有效期 | 7 天（604800000ms） | `application-dev.yml: jwt.refresh-token-experation` |
| Claims 字段 | userId, username, role, tokenType, jti | `JwtUtils.buildToken()` |
| 黑名单存储 | Redis `token:blacklist:{jti}` | `TokenBlacklistService` |
| 黑名单 TTL | 等于 Token 剩余有效期 | `TokenBlacklistService.addToBlacklist()` |
| 黑名单异常策略 | Fail-Closed（Redis 故障时拒绝请求） | `TokenBlacklistService.isBlacklisted()` |
| Refresh 轮换 | 一次性使用，旧 Token 立即入黑名单 | `AuthServiceImpl.refresh()` |
| 用户状态校验 | 每次请求查数据库 | `JwtAuthenticationFilter.buildUserDetailsFromClaims()` |

---

## 二、已识别问题与改进方向

### 🔴 P0 — 高优先级（性能 / 安全基线）

---

#### 2.1 每次请求查数据库 —— 性能瓶颈

**问题位置**：`JwtAuthenticationFilter.java` 第 97 行

```java
User dbUser = userMapper.selectById(userId);  // 每个请求都查一次 DB
```

**问题描述**：

每个需要鉴权的请求都触发一次数据库查询。在秒杀高并发场景下（万级 QPS），数据库连接池将被鉴权查询占满，成为严重性能瓶颈。

**影响评估**：

| 并发量 | 当前（查 DB） | 改进后（查 Redis） |
|--------|-------------|------------------|
| 100 QPS | 100 次 DB 查询/秒 | ~0 次 DB 查询/秒（缓存命中） |
| 1000 QPS | 1000 次 DB 查询/秒 | ~16 次 DB 查询/秒（60s TTL，约 1000/60） |
| 10000 QPS | 10000 次 DB 查询/秒 → 连接池耗尽 | ~166 次 DB 查询/秒 |

**改进方案**：引入 Redis 缓存用户状态

##### ❓ 疑问解答：什么时候向 Redis 中存入用户数据？

Redis 缓存的写入发生在以下 **三种情境** 中：

**情境 1：Filter 鉴权时缓存未命中（主要写入路径）**

```
请求到达 → 从 Token 取 userId
         │
         ▼
  Redis 查缓存: user:auth:{userId}
  ├─ 命中 → 直接从缓存构建 UserDetails（不查 DB）
  └─ 未命中 → 查 DB → 将结果写入 Redis（TTL 60s）→ 构建 UserDetails
```

这是最频繁的写入路径。用户首次请求或缓存过期后的第一次请求会触发 DB 查询并写入 Redis，后续 60 秒内的请求全部走 Redis。

**情境 2：用户登录成功时（主动预热）**

```
用户登录成功 → 签发 Token → 同时将用户状态写入 Redis
```

登录后立即写入缓存，避免登录后的第一个请求还要查 DB。实现在 `AuthServiceImpl.login()` 方法中。

**情境 3：管理员修改用户状态/角色时（主动刷新）**

```
管理员禁用用户 → 删除旧缓存 → 写入新缓存（状态=DISABLED）
管理员修改角色 → 删除旧缓存 → 写入新缓存（角色=新角色）
```

确保状态变更后缓存立即更新，而不是等 60 秒 TTL 自然过期。

##### ❓ 疑问解答：在什么文件中实现？

| 文件 | 改动内容 | 情境 |
|------|---------|------|
| **新增** `security/UserStatusCacheService.java` | 封装 Redis 缓存读写逻辑 | 核心服务 |
| `JwtAuthenticationFilter.java` | `buildUserDetailsFromClaims()` 改为先查 Redis，未命中再查 DB | 情境 1 |
| `AuthServiceImpl.java` | `login()` 方法中登录成功后写入缓存 | 情境 2 |
| `AuthServiceImpl.java` | `changePassword()` 后清除缓存 | 情境 3 |
| `AdminUserServiceImpl.java` | 禁用/锁定/修改角色操作后清除并重写缓存 | 情境 3 |

##### ❓ 疑问解答：Redis 中用户缓存是否需要设定过期时间？

**需要，必须设定 TTL。** 原因如下：

| 考量 | 说明 |
|------|------|
| **内存控制** | 不设 TTL 的缓存会无限增长。用户量从 1 万涨到 100 万，Redis 内存占用线性增长，最终 OOM |
| **数据一致性** | TTL 是最终一致性的兜底机制。即使主动失效逻辑出现 Bug（忘记删缓存），TTL 也能保证最多 60 秒后缓存自动修正 |
| **用户注销场景** | 用户注销账号后，其缓存应自动清理，而非永久残留 |

**TTL 设为 60 秒的理由**：

| TTL | 优点 | 缺点 |
|-----|------|------|
| 10s | 数据一致性极高 | 缓存命中率低，DB 压力仍大 |
| **60s** | **平衡：命中率 > 95%，DB 压力降低 98%** | **状态变更最多 60s 延迟（配合主动失效可忽略）** |
| 300s | 缓存命中率极高 | 状态变更延迟 5 分钟，安全风险大 |

**关键**：TTL 只是兜底，实际状态变更时通过**主动删除缓存**实现秒级生效，TTL 处理的是"忘记主动删"的异常情况。

##### 缓存数据结构设计

```
Redis Key:   user:auth:{userId}
Redis Type:  Hash
TTL:         60 秒

字段：
  status    → "ACTIVE" / "DISABLED" / "LOCKED"
  role      → "ADMIN" / "SELLER" / "BUYER"
  username  → "张三"
  nickname  → "昵称"（可选，为 SecurityUserDetails 扩展做准备）
```

选择 Hash 而非 String+JSON 的理由：
- 可单独读取 `status` 字段，不需要反序列化整个 JSON
- 修改单个字段不需要读取-修改-写入整个对象

##### UserStatusCacheService 接口设计

```java
@Service
public class UserStatusCacheService {

    private static final String KEY_PREFIX = "user:auth:";
    private static final long CACHE_TTL_SECONDS = 60L;

    /**
     * 获取用户缓存（命中返回，未命中返回 null）
     */
    public UserAuthCache getUserAuth(Long userId);

    /**
     * 写入用户缓存（登录成功 / DB 查询后回填）
     */
    public void putUserAuth(Long userId, User user);

    /**
     * 使缓存失效（状态/角色变更时调用）
     */
    public void invalidateUserAuth(Long userId);

    /**
     * 刷新缓存（删除旧缓存后立即从 DB 重新加载）
     * 用于管理员修改用户状态后，确保下一个请求能立即拿到最新数据
     */
    public void refreshUserAuth(Long userId);
}
```

##### 缓存一致性保障策略

```
                    ┌─────────────────────────────────────────┐
                    │          缓存一致性双保险机制              │
                    │                                         │
                    │  ① 主动失效（秒级生效）                    │
                    │     管理员修改状态 → 立即删除/重写缓存       │
                    │     ↓                                   │
                    │     下一个请求 → 缓存未命中 → 查 DB → 回填   │
                    │                                         │
                    │  ② TTL 兜底（60s 生效）                    │
                    │     即使主动失效逻辑出 Bug                  │
                    │     ↓                                   │
                    │     最多 60s 后缓存自动过期 → 查 DB → 回填   │
                    └─────────────────────────────────────────┘
```

##### Redis 故障降级策略

```
Redis 可用时 → 查 Redis 缓存
Redis 不可用时 → 降级查 DB（保证可用性，牺牲性能）
Redis 恢复后 → 缓存自动重建（首次请求未命中 → 查 DB → 回填）
```

**注意**：此处降级策略与 Token 黑名单的 Fail-Closed 策略不同。黑名单必须 Fail-Closed（拒绝请求），因为放行已吊销 Token 是安全风险；而用户状态缓存 Fail-Open（降级查 DB）是安全的，因为 DB 查询结果仍然是准确的。

---

#### 2.2 密钥硬编码在配置文件 —— 密钥泄露风险

**问题位置**：`application-dev.yml` 第 45 行

```yaml
jwt:
  secret: MySecretKeyForJwtHS256Algorithm2024SeckillMall   # 明文硬编码
```

**问题描述**：

JWT 签名密钥直接写在配置文件中，随代码提交到 Git 仓库。一旦仓库泄露，攻击者可以伪造任意用户的 Token。

**改进方案**：采用 `.env` 文件 + Spring Boot `spring.config.import`

##### 实施步骤

**步骤 1：在项目根目录创建 `.env` 文件**

文件位置：`D:\DESk\SpringBoot\seckill-mall\.env`

```properties
# JWT 签名密钥（不提交到 Git）
JWT_SECRET=MySecretKeyForJwtHS256Algorithm2024SeckillMall

# RSA 私钥路径（改进 2.3 RS256 升级后使用）
JWT_RSA_PRIVATE_KEY_PATH=classpath:keys/private.pem
JWT_RSA_PUBLIC_KEY_PATH=classpath:keys/public.pem
```

**步骤 2：修改 `application.yml`，引入 `.env` 文件**

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  # ... 其余配置不变
```

说明：
- `optional:` 前缀表示文件不存在时不报错（CI/CD 环境可能通过环境变量注入）
- `[.properties]` 表示按 properties 格式解析
- `.env` 中的变量会自动成为 Spring Environment 的属性源

**步骤 3：修改 `application-dev.yml`，密钥改为引用变量**

```yaml
jwt:
  secret: ${JWT_SECRET}   # 从 .env 文件或环境变量读取
```

**步骤 4：修改 `application-prod.yml`，生产环境强制环境变量**

```yaml
jwt:
  secret: ${JWT_SECRET}   # 生产环境必须通过环境变量注入，无默认值
```

**步骤 5：将 `.env` 加入 `.gitignore`**

```
# .gitignore 新增
.env
*.env
```

**步骤 6：创建 `.env.example` 作为模板（提交到 Git）**

文件位置：`D:\DESk\SpringBoot\seckill-mall\.env.example`

```properties
# 复制此文件为 .env 并填入真实值
# cp .env.example .env

JWT_SECRET=your-jwt-secret-key-here-min-32-chars
JWT_RSA_PRIVATE_KEY_PATH=classpath:keys/private.pem
JWT_RSA_PUBLIC_KEY_PATH=classpath:keys/public.pem
```

##### 涉及文件

| 文件 | 改动 |
|------|------|
| **新增** `seckill-mall/.env` | 密钥等敏感配置（不提交 Git） |
| **新增** `seckill-mall/.env.example` | 模板文件（提交 Git，供开发者参考） |
| `application.yml` | 新增 `spring.config.import: optional:file:.env[.properties]` |
| `application-dev.yml` | `jwt.secret` 改为 `${JWT_SECRET}` |
| `application-prod.yml` | `jwt.secret` 改为 `${JWT_SECRET}` |
| `.gitignore` | 新增 `.env` 和 `*.env` 排除规则 |

##### 本地开发流程

```bash
# 首次克隆项目后
cd seckill-mall
cp .env.example .env
# 编辑 .env 填入真实密钥
# 启动 Spring Boot，自动从 .env 读取配置
```

---

### 🟡 P1 — 中优先级（安全加固 / 功能补全）

---

#### 2.3 HS256 对称签名 → RS256 非对称签名（直接切换，无过渡期）

**问题位置**：`JwtUtils.java` 第 71-74 行

```java
.signWith(key, Jwts.SIG.HS256)
```

**问题描述**：

HS256 使用同一个密钥进行签名和验证。一旦密钥泄露，攻击者可以伪造任意用户的 Token。

**改进方案**：直接升级为 RS256，所有旧 Token 立即失效，用户需要重新登录。

##### ❓ 疑问解答：为什么不需要过渡期？

| 对比项 | 有过渡期方案 | 直接切换方案 |
|--------|------------|------------|
| 复杂度 | 需要同时支持 HS256+RS256 双算法验证，代码复杂 | 代码简洁，仅 RS256 一种算法 |
| 安全性 | 过渡期内 HS256 漏洞仍然存在 | 立即消除 HS256 风险 |
| 用户体验 | 旧 Token 自然过期前可继续使用 | 所有用户需重新登录（一次性影响） |
| 维护成本 | 需要后续清理 HS256 代码 | 无额外清理工作 |

**结论**：项目当前处于开发阶段，用户量有限，重新登录的影响可忽略。直接切换更简洁、更安全。

##### RS256 密钥生成

使用 OpenSSL 生成 RSA 2048 密钥对：

```bash
# 在 seckill-mall/src/main/resources/keys/ 目录下执行

# 生成私钥（PKCS#8 格式，Java 原生支持）
openssl genrsa -out private.pem 2048

# 从私钥导出公钥
openssl rsa -in private.pem -pubout -out public.pem
```

生成后文件结构：

```
seckill-mall/src/main/resources/
  └── keys/
      ├── private.pem   ← 签发 Token 用（仅后端持有）
      └── public.pem    ← 验证 Token 用（可公开）
```

**注意**：`private.pem` 虽然在 `resources` 目录中，但不会暴露给外部（Spring Boot 打包在 JAR 内部）。如需更高安全性，可通过 `.env` 配置私钥的绝对路径，将密钥文件放在 JAR 外部。

##### JwtUtils 改造方案

```java
@Slf4j
@Component
public class JwtUtils {

    // Claims 键名（不变）
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private RSAPrivateKey privateKey;   // 签发用
    private RSAPublicKey publicKey;     // 验证用

    @PostConstruct
    public void init() {
        // 从 .env 配置的路径加载密钥对
        this.privateKey = RsaKeyProvider.loadPrivateKey();
        this.publicKey = RsaKeyProvider.loadPublicKey();
    }

    public String generateAccessToken(Long userId, String username, UserRole role) {
        return buildToken(userId, username, role, accessTokenExpiration, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(Long userId, String username, UserRole role) {
        return buildToken(userId, username, role, refreshTokenExpiration, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(Long userId, String username, UserRole role,
                              long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .issuer("seckill-mall")
                .audience().add("seckill-mall-api").and()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.getCode())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(privateKey, Jwts.SIG.RS256)  // ← RS256 私钥签名
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)                 // ← RS256 公钥验证
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 其余方法（validateToken, getUserIdFromToken 等）逻辑不变
    // ...
}
```

##### 新增 RsaKeyProvider

```java
@Component
public class RsaKeyProvider {

    @Value("${jwt.rsa.private-key-path:classpath:keys/private.pem}")
    private String privateKeyPath;

    @Value("${jwt.rsa.public-key-path:classpath:keys/public.pem}")
    private String publicKeyPath;

    public static RSAPrivateKey loadPrivateKey() {
        // 读取 PEM 文件 → 去除头尾标记 → Base64 解码 → 生成 RSAPrivateKey
    }

    public static RSAPublicKey loadPublicKey() {
        // 读取 PEM 文件 → 去除头尾标记 → Base64 解码 → 生成 RSAPublicKey
    }
}
```

##### 切换影响与应对

| 影响 | 应对措施 |
|------|---------|
| 所有旧 HS256 Token 立即失效 | 用户需重新登录（开发阶段可接受） |
| 前端无需改动 | RS256 对前端透明，前端只负责传递 Token，不解析签名 |
| `.env` 新增 RSA 密钥路径配置 | `.env.example` 中已预留 |
| `JwtUtils` 中移除 `secret` 字段 | 不再需要 HS256 对称密钥 |

##### 涉及文件

| 文件 | 改动 |
|------|------|
| `JwtUtils.java` | 签名/验证改为 RS256，密钥改为 KeyPair |
| **新增** `security/RsaKeyProvider.java` | 加载 RSA 密钥对 |
| **新增** `resources/keys/private.pem` | RSA 私钥文件 |
| **新增** `resources/keys/public.pem` | RSA 公钥文件 |
| `.env` / `.env.example` | 新增 RSA 密钥路径配置 |
| `application.yml` | 新增 RSA 密钥路径配置项 |

---

#### 2.4 Refresh Token 刷新时未校验用户最新状态

**问题位置**：`AuthServiceImpl.java` 第 155-178 行

```java
public TokenVO refresh(RefreshTokenRequest req) {
    String refreshToken = req.getRefreshToken();
    // ... 校验签名、tokenType ...
    Long userId = jwtUtils.getUserIdFromToken(refreshToken);
    String username = jwtUtils.getUsernameFromToken(refreshToken);
    String roleCode = jwtUtils.getRoleFromToken(refreshToken);  // ← 用 Token 中的旧角色
    UserRole role = UserRole.fromCode(roleCode);
    // ← 没有查数据库！直接用旧角色签发新 Token
    String newAccessToken = jwtUtils.generateAccessToken(userId, username, role);
}
```

**问题描述**：

如果用户角色从 ADMIN 被降级为 BUYER，但在 Refresh Token 过期前（最长 7 天），用户仍可以刷新出带有 ADMIN 角色的新 Access Token。同样，已被禁用的用户仍可刷新出新 Token。

##### ❓ 疑问解答：为什么 2.4 从数据库查询，而 2.1 改成从 Redis 查询？

这是一个非常好的问题。核心原因是**两个操作的安全等级和频率不同**：

| 维度 | 2.1 Filter 鉴权（每次请求） | 2.4 Refresh 刷新（Token 过期时） |
|------|--------------------------|-------------------------------|
| **调用频率** | 极高（每个请求都执行） | 极低（仅 Access Token 过期时，约 30 分钟~2 小时一次） |
| **操作性质** | **验证身份**（只读，不产生副作用） | **签发新 Token**（写入，有安全副作用） |
| **安全要求** | 允许秒级延迟（60s TTL 可接受） | **必须零延迟**（签发带错误权限的 Token 不可接受） |
| **性能要求** | 极高（万级 QPS） | 低（偶尔一次） |
| **数据源选择** | Redis 缓存（快，允许短暂不一致） | **数据库（慢，但绝对准确）** |

**具体场景说明**：

```
场景：管理员将用户从 ADMIN 降级为 BUYER

┌─ 2.1 Filter 鉴权（用 Redis）─────────────────────────────┐
│                                                          │
│  降级后 60 秒内：                                          │
│    Redis 缓存仍是 ADMIN → 请求放行（短暂不一致，可接受）      │
│    ↓ 最坏情况：用户在这 60s 内仍能访问 ADMIN 接口            │
│    ↓ 但管理员已主动删除缓存 → 实际延迟 < 1s                  │
│                                                          │
│  60 秒后：                                                │
│    Redis 缓存过期 → 查 DB → 得到 BUYER → 拒绝 ADMIN 请求    │
│                                                          │
│  风险评估：短暂不一致窗口 < 60s，且仅影响"查看"类操作         │
│  不会产生持久性安全影响                                     │
└──────────────────────────────────────────────────────────┘

┌─ 2.4 Refresh 刷新（用 DB）────────────────────────────────┐
│                                                          │
│  如果用 Redis：                                           │
│    降级后 60 秒内用户刷新 Token：                           │
│    Redis 缓存仍是 ADMIN → 签发新 ADMIN Access Token        │
│    ↓ 新 Token 有效期 30 分钟！                             │
│    ↓ 降级操作被完全绕过，用户持 ADMIN Token 持续 30 分钟     │
│    ↓ 即使 Redis 缓存更新，已签发的 Token 无法撤回            │
│                                                          │
│  如果用 DB：                                              │
│    降级后用户刷新 Token：                                   │
│    查 DB → 得到 BUYER → 签发 BUYER Token                   │
│    ↓ 降级立即生效，无任何延迟                               │
│                                                          │
│  风险评估：签发 Token 是"写操作"，错误权限会持续整个有效期     │
│  必须使用最权威的数据源（DB），不允许任何延迟                  │
└──────────────────────────────────────────────────────────┘
```

**一句话总结**：**验证身份允许短暂延迟（Redis），签发凭证必须零延迟（DB）。**

##### 改进方案

```java
public TokenVO refresh(RefreshTokenRequest req) {
    String refreshToken = req.getRefreshToken();
    if (!jwtUtils.validateToken(refreshToken)) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    if (!JwtUtils.TOKEN_TYPE_REFRESH.equals(jwtUtils.getTokenTypeFromToken(refreshToken))) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    Long userId = jwtUtils.getUserIdFromToken(refreshToken);

    // ===== 关键改动：查数据库获取最新状态和角色 =====
    User dbUser = userMapper.selectById(userId);
    if (dbUser == null || dbUser.getStatus() != UserStatus.ACTIVE) {
        // 用户已被禁用/删除，吊销 Refresh Token 并拒绝
        tokenBlacklistService.addToBlacklist(refreshToken);
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    // 使用数据库中的最新角色，而非 Token 中的旧角色
    String newAccessToken = jwtUtils.generateAccessToken(userId, dbUser.getUsername(), dbUser.getRole());
    String newRefreshToken = jwtUtils.generateRefreshToken(userId, dbUser.getUsername(), dbUser.getRole());

    // 旧 refreshToken 失效（一次性轮换）
    tokenBlacklistService.addToBlacklist(refreshToken);

    TokenVO vo = new TokenVO();
    vo.setAccessToken(newAccessToken);
    vo.setRefreshToken(newRefreshToken);
    return vo;
}
```

##### 涉及文件

| 文件 | 改动 |
|------|------|
| `AuthServiceImpl.java` | `refresh()` 方法增加数据库查询，使用 DB 中的最新角色 |

---

#### 2.5 缺少用户级 Token 批量吊销能力

**问题描述**：

当前黑名单基于单个 Token 的 `jti`，只能吊销单个 Token。以下场景无法支持：

| 场景 | 当前能力 | 期望能力 |
|------|---------|---------|
| 用户主动登出 | ✅ 吊销当前 Access Token | ✅ |
| 修改密码后踢下所有设备 | ❌ 只能吊销一个 Token | ✅ 吊销该用户所有 Token |
| 管理员强制踢人 | ❌ 无法实现 | ✅ 吊销该用户所有 Token |
| 检测到异常登录 | ❌ 无法批量处理 | ✅ 吊销该用户所有 Token |

**改进方案**：增加用户级 Token 版本号

```
Redis: user:token-version:{userId} = <version>

签发 Token 时：
  - 查询 Redis 获取当前 version
  - 将 version 写入 Token Claims

验证 Token 时（JwtAuthenticationFilter）：
  - 从 Token Claims 取出 version
  - 从 Redis 取出当前 version
  - 对比：一致 → 放行，不一致 → 拒绝

踢下所有设备：
  - 递增 Redis 中的 version → 所有旧 Token 的 version 与新 version 不一致 → 全部失效
```

**数据结构**：

```
Redis Key: user:token-version:{userId}
Value: 自增整数（如 1, 2, 3...）
TTL: 无（永久有效，与用户生命周期一致）
```

**Token Claims 新增**：

```java
.claim("tokenVersion", currentVersion)  // 签发时写入
```

**涉及文件**：

| 文件 | 改动 |
|------|------|
| `JwtUtils.java` | `buildToken()` 新增 `tokenVersion` 参数 |
| `JwtAuthenticationFilter.java` | 验证时对比 Token 中的 version 与 Redis 中的 version |
| `AuthServiceImpl.java` | 登录/刷新时查询并写入 version |
| `AuthServiceImpl.java` | 修改密码后递增 version |
| `AdminUserServiceImpl.java` | 禁用/锁定用户后递增 version |
| **新增** `security/TokenVersionService.java` | 封装 version 的查询、递增逻辑 |

---

### 🟡 P2 — 中低优先级（代码质量）

---

#### 2.6 SecurityUtils 静态 UserMapper —— 反模式

**问题位置**：`SecurityUtils.java`

**问题描述**：

1. 静态变量 + Spring @Autowired = 反模式，测试难以 Mock
2. `getCurrentUser()` 重复查数据库（Filter 中已查过一次）
3. `SecurityUserDetails` 缺少 nickname 等常用字段

**改进方案**：丰富 SecurityUserDetails + 改 SecurityUtils 为非静态 Bean

**涉及文件**：

| 文件 | 改动 |
|------|------|
| `SecurityUserDetails.java` | 新增 nickname、avatar 字段 |
| `JwtAuthenticationFilter.java` | 构建 UserDetails 时填充新字段 |
| `SecurityUtils.java` | 改为非静态 Bean |
| 所有调用 `SecurityUtils` 的 Service | 适配新调用方式 |

---

### 🟢 P3 — 低优先级（安全增强）

---

#### 2.7 Access Token 有效期偏长

**当前配置**：2 小时 → **建议缩短为 30 分钟**

前提：前端实现 Access Token 自动刷新（见前端方案章节）。

**涉及文件**：

| 文件 | 改动 |
|------|------|
| `application-dev.yml` | `jwt.access-token-expiration` 改为 1800000 |
| `application-prod.yml` | 同步调整 |

#### 2.8 Token 缺少 iss/aud 标准字段

已在改进 2.3 的 RS256 改造中一并实现（`buildToken()` 中新增 `.issuer()` 和 `.audience()`）。

---

## 三、前端修改详细方案

### 3.1 当前前端 Token 机制分析

| 组件 | 文件 | 当前行为 |
|------|------|---------|
| request.ts | `frontend/src/api/request.ts` | 请求拦截器添加 Bearer Token；401 时清空 Token 并跳转登录 |
| user.ts | `frontend/src/stores/user.ts` | Pinia Store 管理 Token；`refreshTokenAction()` 刷新 Token；`isTokenExpired()` 检查过期 |
| router/index.ts | `frontend/src/router/index.ts` | 路由守卫中检测 Token 过期并尝试刷新 |

**当前问题**：

1. **401 时直接跳转登录，未尝试刷新**：`request.ts` 响应拦截器收到 401 后直接清空 Token 跳转登录页，没有先尝试用 Refresh Token 刷新
2. **路由守卫有刷新逻辑，但 axios 拦截器没有**：两个层面的逻辑不一致
3. **并发请求时可能重复刷新**：多个请求同时 401 时，每个都会触发 `refreshTokenAction()`，导致多次刷新调用

### 3.2 前端改进项 1：axios 响应拦截器增加自动刷新

**文件**：`frontend/src/api/request.ts`

**改动说明**：401 响应时先尝试用 Refresh Token 刷新，刷新成功后重试原请求；刷新失败才跳转登录页。

```typescript
// ===== 改造后的响应拦截器 =====

/** 刷新锁：防止并发请求时重复刷新 */
let isRefreshing = false
/** 等待刷新完成的请求队列 */
let pendingRequests: Array<(token: string) => void> = []

request.interceptors.response.use(
  (response) => {
    // ... 原有的业务码处理逻辑不变 ...
  },
  async (error) => {
    const { response, config } = error
    if (response) {
      const status = response.status
      const currentPath = window.location.pathname + window.location.search

      if (status === 401) {
        // ===== 关键改动：先尝试刷新 Token =====
        const refreshTokenValue = localStorage.getItem(REFRESH_TOKEN_KEY)

        if (!refreshTokenValue) {
          // 无 Refresh Token，直接跳转登录
          clearTokensAndRedirect(currentPath)
          return Promise.reject(error)
        }

        if (isRefreshing) {
          // 正在刷新中，将请求加入等待队列
          return new Promise((resolve) => {
            pendingRequests.push((newToken: string) => {
              config.headers.Authorization = `Bearer ${newToken}`
              resolve(request(config))
            })
          })
        }

        isRefreshing = true
        try {
          // 调用刷新接口
          const res = await post<TokenVO>('/api/v1/auth/refresh', {
            refreshToken: refreshTokenValue
          })
          const newAccessToken = res.data.accessToken
          const newRefreshToken = res.data.refreshToken

          // 更新存储
          localStorage.setItem(ACCESS_TOKEN_KEY, newAccessToken)
          localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)

          // 处理等待队列中的请求
          pendingRequests.forEach(cb => cb(newAccessToken))
          pendingRequests = []

          // 重试原请求
          config.headers.Authorization = `Bearer ${newAccessToken}`
          return request(config)
        } catch (refreshError) {
          // 刷新失败，清空 Token 并跳转登录
          pendingRequests = []
          clearTokensAndRedirect(currentPath)
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      // 403 / 429 等其他状态码处理不变 ...
    }
    // ... 其余错误处理不变 ...
  }
)

/** 清空 Token 并跳转登录页 */
function clearTokensAndRedirect(currentPath: string) {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  if (!currentPath.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
  }
}
```

### 3.3 前端改进项 2：.env 文件适配

**文件**：`frontend/.env.development` 和 `frontend/.env.production`

**改动说明**：前端 `.env` 文件仅管理 Vite 相关配置（`VITE_` 前缀），JWT 密钥等后端配置在前端 `.env` 中不需要。前端无需改动。

当前前端 `.env` 内容：

```properties
# .env.development
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=SeckillMall 秒杀商城
```

这些配置保持不变，后端 `.env` 是独立的文件，互不影响。

### 3.4 前端改进项 3：RS256 升级适配

**改动说明**：**前端无需任何改动。**

RS256 是后端签名算法的变更，对前端完全透明：
- 前端只负责在 `Authorization` 头中传递 Token 字符串
- 前端不解析、不验证 Token 的签名（这是后端的事）
- Token 的格式仍然是 `xxxxx.yyyyy.zzzzz`（三段式 JWT）
- 唯一的影响是切换后所有旧 Token 失效，用户需要重新登录（前端会自动跳转登录页）

### 3.5 前端改进项 4：Access Token 有效期缩短后的适配

当后端将 Access Token 有效期从 2 小时缩短为 30 分钟后，前端的 `isTokenExpired()` 方法中"提前 5 分钟视为过期"的缓冲值需要调整：

**文件**：`frontend/src/stores/user.ts`

```typescript
// 修改前
function isTokenExpired(): boolean {
  // 提前 5 分钟过期缓冲
  return payload.exp * 1000 - 5 * 60 * 1000 < Date.now()
}

// 修改后：有效期 30 分钟时，提前 2 分钟刷新更合理
function isTokenExpired(): boolean {
  // 有效期 30 分钟，提前 2 分钟视为过期，触发刷新
  return payload.exp * 1000 - 2 * 60 * 1000 < Date.now()
}
```

### 3.6 前端改进项 5：登出时调用后端接口

**文件**：`frontend/src/stores/user.ts`

当前 `logout()` 已经调用了 `authApi.logout()`，无需改动。后端改进 2.5（Token 版本号）实施后，登出时后端可同时递增 version 实现全设备踢下，前端无需额外改动。

### 3.7 前端改动汇总

| 序号 | 改进项 | 文件 | 改动量 | 依赖后端改进 |
|------|--------|------|--------|------------|
| 1 | axios 401 自动刷新 | `api/request.ts` | 中 | 无（现有刷新接口即可） |
| 2 | .env 适配 | 无需改动 | 无 | 2.2 |
| 3 | RS256 适配 | 无需改动 | 无 | 2.3 |
| 4 | Token 过期缓冲调整 | `stores/user.ts` | 小 | 2.7 |
| 5 | 登出适配 | 无需改动 | 无 | 2.5 |

---

## 四、实施路线图

### 阶段一：安全基线加固（1-2 天）

> 目标：修复最紧迫的安全和性能问题

| 序号 | 改进项 | 优先级 | 预估工时 | 风险 |
|------|--------|--------|---------|------|
| 1 | 密钥从配置文件移到 `.env` | P0 | 1h | 低 |
| 2 | HS256 → RS256 直接切换 | P1 | 3h | 低（用户需重新登录） |
| 3 | Refresh 刷新时校验用户最新状态 | P1 | 0.5h | 低 |
| 4 | Token 补充 iss/aud 标准字段 | P3 | 0.5h | 低（已在 RS256 改造中一并实现） |

**验收标准**：
- `.env` 文件包含密钥配置，`.gitignore` 排除 `.env`
- RS256 签名的 Token 可正常签发和验证
- 被禁用用户无法通过 Refresh Token 获取新 Access Token
- Token 解析后包含 `iss`、`aud` 字段

### 阶段二：性能优化 + 前端改造（2-3 天）

> 目标：解决高并发瓶颈 + 前端自动刷新

| 序号 | 改进项 | 优先级 | 预估工时 | 风险 |
|------|--------|--------|---------|------|
| 5 | 用户状态 Redis 缓存 | P0 | 4h | 中（需确保缓存一致性） |
| 6 | 前端 axios 401 自动刷新 | P0 | 2h | 低 |
| 7 | SecurityUtils 重构 | P2 | 3h | 中（影响面广） |

**验收标准**：
- 鉴权请求 Redis 缓存命中率 > 95%
- 管理员禁用用户后，缓存同步失效
- 前端 401 时自动尝试刷新，刷新成功后重试原请求
- 并发请求时不会重复刷新

### 阶段三：安全深度加固（2-3 天）

> 目标：增强 Token 管理能力 + 缩短有效期

| 序号 | 改进项 | 优先级 | 预估工时 | 风险 |
|------|--------|--------|---------|------|
| 8 | 用户级 Token 版本号（批量吊销） | P1 | 4h | 中 |
| 9 | 缩短 Access Token 有效期 | P3 | 1h | 低（前端已实现自动刷新） |
| 10 | 前端 Token 过期缓冲调整 | P3 | 0.5h | 低 |

**验收标准**：
- 修改密码后，该用户所有设备被踢下线
- 管理员可强制踢下指定用户的所有设备
- Access Token 有效期 30 分钟，前端自动刷新无感

---

## 五、各改进项依赖关系

```
阶段一（安全基线）            阶段二（性能+前端）          阶段三（安全加固）
──────────────            ──────────────           ──────────────
① .env 密钥管理 ──┐
② RS256 直接切换 ──┤       ⑤ Redis 用户缓存 ─────→ ⑧ Token 版本号
③ 刷新校验状态 ────┤       ⑥ 前端 401 自动刷新 ───→ ⑨ 缩短有效期
④ iss/aud 字段 ───┘                              ⑩ 前端缓冲调整
                   ⑦ SecurityUtils 重构
```

**关键依赖**：
- ②（RS256）依赖 ①（.env 密钥管理），因为 RSA 密钥路径需要从 .env 读取
- ⑤（Redis 缓存）应在 ⑧（Token 版本号）之前实施，版本号验证也依赖 Redis
- ⑥（前端自动刷新）应在 ⑨（缩短有效期）之前实施，否则缩短有效期会导致频繁掉线
- ②（RS256 直接切换）不需要过渡期，一次性切换

---

## 六、风险与回滚策略

| 改进项 | 主要风险 | 回滚策略 |
|--------|---------|---------|
| .env 密钥管理 | 本地开发忘记创建 .env | `.env.example` 提供模板；启动时校验密钥非空 |
| RS256 直接切换 | 所有用户需重新登录 | 开发阶段可接受；生产环境建议在低峰期切换 |
| Redis 用户缓存 | 缓存与 DB 不一致 | TTL 60s 兜底 + 主动失效；Redis 故障降级查 DB |
| 刷新校验 DB | 刷新接口增加一次 DB 查询 | 频率极低（30min~2h 一次），性能影响可忽略 |
| Token 版本号 | Redis 中 version 丢失 | version 丢失时降级为仅黑名单校验 |
| 前端 401 自动刷新 | 并发请求重复刷新 | 刷新锁 + 等待队列机制 |
| 缩短有效期 | 前端未实现自动刷新导致频繁掉线 | 前端先实现刷新（阶段二），后端再缩短有效期（阶段三） |
| SecurityUtils 重构 | 影响面广 | 保留旧静态方法标记 @Deprecated，逐步迁移 |

---

## 七、测试验证要点

### 7.1 功能测试

| 测试场景 | 预期结果 |
|----------|---------|
| 正常登录 → 获取 RS256 Token → 访问受保护接口 | 200 正常响应 |
| Access Token 过期 → 前端自动刷新 → 重试原请求 | 用户无感知，请求成功 |
| 并发请求同时 401 → 仅刷新一次 | 其他请求等待刷新完成后自动重试 |
| 用户被禁用 → 用旧 Access Token 请求 | 401 拒绝 |
| 用户被禁用 → 用 Refresh Token 刷新 | 401 拒绝（改进 2.4） |
| 用户角色从 ADMIN 降为 BUYER → 刷新 Token | 新 Token 角色 = BUYER（改进 2.4） |
| 修改密码 → 旧 Token 请求 | 401 拒绝（改进 2.5） |
| 修改密码 → 其他设备旧 Token 请求 | 401 拒绝（改进 2.5） |
| 登出 → 旧 Token 请求 | 401 拒绝 |

### 7.2 性能测试

| 测试场景 | 基线指标 | 目标指标 |
|----------|---------|---------|
| 1000 QPS 鉴权请求 | DB 查询 ~1000 次/秒 | DB 查询 ~16 次/秒 |
| Redis 缓存命中延迟 | — | < 1ms |
| 单次鉴权总耗时 | ~5ms（含 DB） | ~1ms（含 Redis） |

### 7.3 安全测试

| 测试场景 | 预期结果 |
|----------|---------|
| 伪造 Token（错误 RS256 签名） | 401 拒绝 |
| 用旧 HS256 Token 请求 | 401 拒绝（签名算法不匹配） |
| 过期 Token | 401 拒绝 |
| Refresh Token 当 Access Token 使用 | 401 拒绝 |
| 已黑名单 Token | 401 拒绝 |
| Redis 黑名单服务故障 | 401 拒绝（Fail-Closed） |
| 秒杀接口重放请求 | 401 拒绝 |

---

## 八、附录

### A. 当前安全机制清单

| 编号 | 机制 | 状态 | 备注 |
|------|------|------|------|
| M6 | CSRF 禁用（无状态服务） | ✅ 合理 | 若引入 Cookie 会话需重新启用 |
| M7 | API 文档端点权限控制 | ✅ 已优化 | 改为 permitAll + 生产环境禁用 |
| M9 | 账户过期/锁定与 enabled 联动 | ✅ 已实现 | SecurityUserDetails 中 |
| M11 | 限流防暴力破解 | ✅ 已实现 | @RateLimit 注解 |
| C1 | 用户状态实时校验 | ✅ 已实现 | 但每次查 DB，需优化为 Redis |
| C2 | 签名密钥启动校验 | ✅ 已实现 | ReplayProtectionFilter @PostConstruct |
| C3 | Actuator 端点权限收敛 | ✅ 已实现 | 仅 health 公开 |
| H1 | CORS 白名单 | ✅ 已实现 | 配置化域名列表 |
| H2 | CORS 头统一管理 | ✅ 已实现 | 不在各 Handler 中手动设置 |
| H3 | 签名日志不泄露 | ✅ 已实现 | 不输出 expected/actual |
| H4 | 黑名单 Fail-Closed | ✅ 已实现 | Redis 异常时拒绝 |
| L5 | HS256 → RS256 | ⏳ 待实施 | 本方案改进 2.3，直接切换无过渡期 |

### B. 配置参数参考值

| 参数 | 当前值 | 建议值 | 说明 |
|------|--------|--------|------|
| `jwt.access-token-expiration` | 7200000 (2h) | 1800000 (30min) | 阶段三调整 |
| `jwt.refresh-token-expiration` | 604800000 (7d) | 604800000 (7d) | 保持不变 |
| 签名算法 | HS256 | RS256 | 阶段一直接切换 |
| 密钥管理方式 | 配置文件明文 | .env 文件 | 阶段一 |
| 用户状态缓存 TTL | — | 60s | 阶段二新增 |
| Token 版本号 TTL | — | 无过期 | 阶段三新增 |
| 前端 Token 过期缓冲 | 5 分钟 | 2 分钟 | 配合有效期缩短 |

### C. 新增文件清单

| 文件路径 | 用途 | 阶段 |
|----------|------|------|
| `seckill-mall/.env` | 敏感配置（不提交 Git） | 阶段一 |
| `seckill-mall/.env.example` | 敏感配置模板（提交 Git） | 阶段一 |
| `seckill-mall/src/main/resources/keys/private.pem` | RSA 私钥 | 阶段一 |
| `seckill-mall/src/main/resources/keys/public.pem` | RSA 公钥 | 阶段一 |
| `security/RsaKeyProvider.java` | RSA 密钥加载 | 阶段一 |
| `security/UserStatusCacheService.java` | 用户状态 Redis 缓存 | 阶段二 |
| `security/TokenVersionService.java` | Token 版本号管理 | 阶段三 |
