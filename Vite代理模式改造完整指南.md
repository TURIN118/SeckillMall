# 项目 Vite 代理模式改造完整指南

> 生成时间：2026-08-05（已更新：crypto-js → Web Crypto API）
> 关联文档：《秒杀抢购401问题修复方案.md》
> 目标：将项目从"跨域直连后端"模式改造为"Vite代理转发"模式，同时修复后端 CORS 遗漏问题
> 签名实现：Web Crypto API（浏览器原生，零依赖，异步非阻塞，密钥对象不可导出）

---

## 一、当前项目的请求架构全景

### 1.1 请求链路现状

```
┌──────────────────────────────────────────────────────────────────────┐
│  开发环境（npm run dev）                                              │
│                                                                      │
│  浏览器(5173)                                                        │
│      │                                                               │
│      │  Axios baseURL = "http://localhost:8080"                       │
│      │  请求 = "http://localhost:8080" + "/api/v1/..."               │
│      │                                                               │
│      └──跨域直连──→ Spring Boot(8080)                                │
│                        ↑                                             │
│                   端口不同 = 跨域                                     │
│                   浏览器强制要求 CORS 头                               │
│                                                                      │
│  Vite 代理(5173)：已配置但未生效 ❌                                    │
│  （因为 baseURL 是绝对 URL，请求不经过 Vite 服务器）                    │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│  生产环境（npm run build）                                            │
│                                                                      │
│  浏览器                                                              │
│      │                                                               │
│      │  Axios baseURL = "" (空)                                      │
│      │  请求 = "/api/v1/..." (相对路径)                               │
│      │                                                               │
│      └──→ 需要Nginx/网关代理 ──→ Spring Boot(8080)                   │
│            ↑                                                         │
│       项目中无 Nginx 配置 ❌                                           │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.2 涉及的所有配置文件清单

| 文件 | 当前状态 | 改造后状态 |
|------|---------|-----------|
| `frontend/.env.development` | `VITE_API_BASE_URL=http://localhost:8080` | `VITE_API_BASE_URL=`（空） |
| `frontend/.env.production` | `VITE_API_BASE_URL=`（空） | 不变 |
| `frontend/vite.config.ts` | proxy 已配置但未生效 | proxy 生效，增强配置 |
| `frontend/src/api/request.ts` | baseURL 读环境变量 | 需微调 refresh 请求的 URL 拼接 |
| `seckill-mall/.../SecurityConfig.java` | CORS 配置正确 | 不变（保留作为生产环境兜底） |
| `seckill-mall/.../ReplayProtectionFilter.java` | reject() 缺 CORS 头 | 补充 CORS 头 |

---

## 二、前端改造（4 个文件）

### 2.1 文件一：`frontend/.env.development`

**改什么**：将 `VITE_API_BASE_URL` 从绝对 URL 改为空值

**为什么**：Axios 的 `baseURL` 如果是绝对 URL（`http://localhost:8080`），请求会直接发到后端，绕过 Vite 代理。改为空值后，请求变成相对路径（`/api/v1/...`），浏览器会发到当前域名（`localhost:5173`），Vite 代理才能拦截并转发。

**改动前**：
```properties
# 开发环境
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=SeckillMall 秒杀商城
```

**改动后**：
```properties
# 开发环境
# VITE_API_BASE_URL 留空，让所有 API 请求走相对路径，由 Vite 代理转发到后端
# 代理目标在 vite.config.ts 的 server.proxy 中配置
VITE_API_BASE_URL=
VITE_APP_TITLE=SeckillMall 秒杀商城
```

**效果对比**：

| | 改动前 | 改动后 |
|---|---|---|
| Axios baseURL | `http://localhost:8080` | `""` (空) |
| 请求完整URL | `http://localhost:8080/api/v1/seckill/...` | `/api/v1/seckill/...` (相对路径) |
| 浏览器实际发到 | `localhost:8080` (跨域) | `localhost:5173` (同源，Vite代理转发到8080) |
| 是否跨域 | ✅ 跨域 | ❌ 同源 |

---

### 2.2 文件二：`frontend/vite.config.ts`

**改什么**：优化代理配置，增加 `rewrite` 规则和 `ws`（WebSocket）支持，添加开发环境专用的后端目标地址环境变量

**为什么**：当前代理配置虽然存在但过于简陋，缺少以下能力：
- 没有明确的后端目标地址配置（依赖 `VITE_API_BASE_URL`，但该变量改为空后代理目标就变成了 `http://localhost:8080` 的硬编码默认值）
- 没有配置 `ws: true`（如果后续引入 WebSocket 通知秒杀结果，代理无法转发）
- 没有配置 `secure`（HTTPS 后端支持）
- 没有配置开发环境专用的后端地址变量（与 API baseURL 解耦）

**改动前**：
```typescript
server: {
  host: '0.0.0.0',
  port: 5173,
  open: false,
  proxy: {
    '/api': {
      target: env.VITE_API_BASE_URL || 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

**改动后**：
```typescript
server: {
  host: '0.0.0.0',
  port: 5173,
  open: false,
  proxy: {
    // API 接口代理：将 /api 开头的请求转发到后端
    '/api': {
      // 后端地址从 VITE_PROXY_TARGET 读取，默认 http://localhost:8080
      target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,   // 修改请求头中的 Host 为后端地址，避免后端 Host 校验失败
      secure: false,        // 后端为 HTTP 时设为 false；HTTPS 自签名证书时也设为 false
      ws: true              // 支持 WebSocket 代理（为后续秒杀结果推送预留）
    },
    // 图片/上传文件代理：将 /images 和 /upload 开头的请求也转发到后端
    '/images': {
      target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
      secure: false
    },
    '/upload': {
      target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
      secure: false
    }
  }
}
```

**同时更新 `.env.development`，增加代理目标变量**：
```properties
# 开发环境
# VITE_API_BASE_URL 留空，让所有 API 请求走相对路径，由 Vite 代理转发到后端
VITE_API_BASE_URL=
# VITE_PROXY_TARGET 指定 Vite 代理转发的后端目标地址（仅开发环境使用）
VITE_PROXY_TARGET=http://localhost:8080
VITE_APP_TITLE=SeckillMall 秒杀商城
```

**配置说明**：

| 配置项 | 值 | 作用 |
|--------|---|------|
| `target` | `http://localhost:8080` | 代理转发的目标后端地址 |
| `changeOrigin` | `true` | 将请求头的 `Host` 改为后端地址，某些后端框架会校验 Host |
| `secure` | `false` | 允许代理到 HTTP 后端（不验证 SSL 证书） |
| `ws` | `true` | 支持 WebSocket 协议升级（为秒杀实时推送预留） |

**Vite 代理工作原理详解**：

```
浏览器发出请求：GET http://localhost:5173/api/v1/seckill/list
    ↓
Vite 开发服务器收到请求
    ↓
检查 URL 是否匹配 proxy 配置的路径前缀
    ↓
匹配 '/api' → 将请求转发到 target (http://localhost:8080)
    ↓
实际发出：GET http://localhost:8080/api/v1/seckill/list
    ↓
后端返回响应 → Vite 将响应原样返回给浏览器
    ↓
浏览器认为这是同源响应（来自 localhost:5173），无需 CORS 校验
```

---

### 2.3 文件三：`frontend/src/api/request.ts`

**改什么**：修复 Token 刷新请求的 URL 拼接逻辑

**为什么**：当前代码第114-115行在 401 错误处理中，手动拼接了刷新 Token 的请求 URL：

```typescript
const refreshRes = await axios.post<Result<TokenVO>>(
  (import.meta.env.VITE_API_BASE_URL || '') + '/api/v1/auth/refresh',
  ...
)
```

当 `VITE_API_BASE_URL` 为空时，拼接结果为 `"" + "/api/v1/auth/refresh"` = `"/api/v1/auth/refresh"`（相对路径），这在 Vite 代理模式下是正确的。但这里使用了原生 `axios` 而非项目封装的 `request` 实例，**绕过了请求拦截器**（不会自动添加 Authorization 头），这是刻意为之（避免循环），但需要确保 URL 拼接在两种模式下都正确。

**当前代码实际上已经兼容**，因为：
- 直连模式：`VITE_API_BASE_URL = "http://localhost:8080"` → URL = `http://localhost:8080/api/v1/auth/refresh`
- 代理模式：`VITE_API_BASE_URL = ""` → URL = `/api/v1/auth/refresh`（相对路径，走 Vite 代理）

**但有一个潜在问题**：原生 `axios.post` 不走 Vite 代理，因为它直接发请求而不是通过 Vite 开发服务器。在代理模式下，浏览器发出的请求是相对路径 `/api/v1/auth/refresh`，浏览器会自动将其解析为 `http://localhost:5173/api/v1/auth/refresh`，Vite 代理会拦截并转发。所以**实际上是没问题的**。

**结论：此文件无需修改**。但建议添加注释说明：

```typescript
// Token 刷新请求使用原生 axios（不走请求拦截器，避免循环）
// URL 拼接：VITE_API_BASE_URL 为空时走相对路径（Vite 代理转发），
// 为绝对 URL 时直连后端（跨域模式）
const refreshRes = await axios.post<Result<TokenVO>>(
  (import.meta.env.VITE_API_BASE_URL || '') + '/api/v1/auth/refresh',
  { refreshToken: refreshTokenValue },
  { headers: { 'Content-Type': 'application/json' }, timeout: 10000 }
)
```

---

### 2.4 文件四：`frontend/src/api/seckill.ts`（防重放签名）

**改什么**：在秒杀下单 API 调用中添加防重放签名头（`X-Sign`、`X-Timestamp`、`X-Nonce`）

**为什么**：后端 `ReplayProtectionFilter` 强制要求这三个头，不传就返回 401。即使切换到 Vite 代理模式消除了 CORS 问题，缺少签名头仍然会导致 401 错误（只是前端能正确看到错误信息了，而不是"网络错误"）。

#### 步骤 1：新建 `frontend/src/utils/replayProtection.ts`

使用 **Web Crypto API**（浏览器原生，零依赖，异步非阻塞）实现签名：

```typescript
/**
 * 防重放签名工具（Web Crypto API 实现）
 * 对应后端 ReplayProtectionFilter 的签名校验逻辑
 * 签名算法：HMAC-SHA256(secret, timestamp + nonce + uri)
 *
 * 为什么用 Web Crypto API 而非 crypto-js：
 * 1. 零依赖：浏览器原生 API，无需安装 npm 包，减少打包体积 ~70KB
 * 2. 性能更优：调用 OS 级加密实现（OpenSSL/BoringSSL），单次 HMAC 约 0.05ms（crypto-js 约 0.5ms）
 * 3. 异步非阻塞：不阻塞主线程，对秒杀按钮点击的 UI 响应更友好
 * 4. 密钥对象不可导出：CryptoKey 默认 extractable=false，即使 XSS 获取 key 对象也无法 exportKey()
 *    （注意：原始密钥字符串仍存在于 JS 内存中，此保护主要防止通过 CryptoKey 接口导出）
 * 5. 标准化：W3C 标准 API，所有现代浏览器均支持
 */

/**
 * 签名密钥
 * 开发环境从 VITE_SIGN_SECRET 读取，生产环境从构建时环境变量注入
 * 必须与后端 seckill.security.sign-secret 配置一致
 */
const SIGN_SECRET: string = import.meta.env.VITE_SIGN_SECRET || ''

/** 缓存的 CryptoKey 对象（避免每次请求重复 importKey） */
let cachedKey: CryptoKey | null = null

if (!SIGN_SECRET) {
  console.warn('[replayProtection] VITE_SIGN_SECRET 未配置，防重放签名将不生效')
}

/**
 * 获取或创建 HMAC-SHA256 的 CryptoKey 对象（带缓存）
 * Web Crypto API 的 importKey 是异步操作，缓存后只需执行一次
 */
async function getSignKey(): Promise<CryptoKey> {
  if (cachedKey) return cachedKey

  const keyBytes = new TextEncoder().encode(SIGN_SECRET)
  cachedKey = await crypto.subtle.importKey(
    'raw',                    // 密钥格式：原始字节
    keyBytes,                 // 密钥数据
    { name: 'HMAC', hash: 'SHA-256' },  // 算法：HMAC-SHA256
    false,                    // extractable=false：密钥对象不可通过 exportKey() 导出
    ['sign']                  // 密钥用途：仅用于签名
  )
  return cachedKey
}

/**
 * 将 ArrayBuffer 转为十六进制小写字符串
 * 与后端 Java 的 HexFormat.of().formatHex() 输出格式一致
 */
function bufferToHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

/**
 * 生成防重放请求头（异步）
 * @param uri 请求 URI 路径（如 /api/v1/seckill/123/execute），不含 baseURL 和 query string
 * @returns 包含 X-Sign、X-Timestamp、X-Nonce 的请求头对象
 *
 * @example
 * // 调用方式（必须 await）
 * const headers = await generateReplayHeaders('/api/v1/seckill/123/execute')
 * await post(uri, undefined, { headers })
 */
export async function generateReplayHeaders(uri: string): Promise<Record<string, string>> {
  if (!SIGN_SECRET) {
    // 密钥未配置时返回空对象，不添加签名头（开发调试用）
    return {}
  }

  const timestamp = Date.now().toString()
  const nonce = crypto.randomUUID()
  const payload = timestamp + nonce + uri
  const payloadBytes = new TextEncoder().encode(payload)

  // 使用 Web Crypto API 计算 HMAC-SHA256 签名
  const key = await getSignKey()
  const signature = await crypto.subtle.sign('HMAC', key, payloadBytes)
  const sign = bufferToHex(signature)

  return {
    'X-Sign': sign,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce
  }
}
```

#### 步骤 2：更新 `.env.development`，添加签名密钥

```properties
# 开发环境
VITE_API_BASE_URL=
VITE_PROXY_TARGET=http://localhost:8080
# 防重放签名密钥（必须与后端 seckill.security.sign-secret 一致，长度 >= 32）
VITE_SIGN_SECRET=your-sign-secret-at-least-32-chars-long-here
VITE_APP_TITLE=SeckillMall 秒杀商城
```

⚠️ **重要**：`VITE_SIGN_SECRET` 的值必须与后端 `application-dev.yml` 中 `seckill.security.sign-secret` 的值完全一致。后端当前配置为 `${SECKILL_SIGN_SECRET}`（从环境变量读取），你需要确认实际运行时的值。

#### 步骤 3：修改 `frontend/src/api/seckill.ts`

**改动前**：
```typescript
/** 执行秒杀 */
export function doSeckill(
  seckillId: number | string,
  seckillToken: string
): Promise<Result<SeckillResultVO>> {
  return post<SeckillResultVO>(`/api/v1/seckill/${seckillId}`, undefined, {
    params: { seckillToken },
    headers: { 'X-Seckill-Token': seckillToken }
  })
}

/** 一键执行秒杀（无需预取token） */
export function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
  return post<SeckillResultVO>(`/api/v1/seckill/${seckillId}/execute`)
}
```

**改动后**（注意 `generateReplayHeaders` 是 async，需要 `await`）：
```typescript
import { generateReplayHeaders } from '@/utils/replayProtection'

/** 执行秒杀（带防重放签名） */
export async function doSeckill(
  seckillId: number | string,
  seckillToken: string
): Promise<Result<SeckillResultVO>> {
  const uri = `/api/v1/seckill/${seckillId}`
  const replayHeaders = await generateReplayHeaders(uri)
  return post<SeckillResultVO>(uri, undefined, {
    params: { seckillToken },
    headers: {
      'X-Seckill-Token': seckillToken,
      ...replayHeaders
    }
  })
}

/** 一键执行秒杀（无需预取token，带防重放签名） */
export async function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
  const uri = `/api/v1/seckill/${seckillId}/execute`
  const replayHeaders = await generateReplayHeaders(uri)
  return post<SeckillResultVO>(uri, undefined, {
    headers: replayHeaders
  })
}
```

> **注意**：`doSeckill` 和 `executeSeckill` 原本就是返回 `Promise` 的函数，添加 `async` 关键字不影响调用方。它们的调用者 `handleSeckillBuy()`（Home.vue）和 `handleSeckill()`（SeckillDetail.vue）本身已经是 `async function`，使用 `await` 调用，**无需任何改动**。

---

### 2.5 前端改造总结

| 序号 | 文件 | 改动类型 | 改动内容 |
|------|------|---------|---------|
| 1 | `.env.development` | 修改 | `VITE_API_BASE_URL` 改空，新增 `VITE_PROXY_TARGET`、`VITE_SIGN_SECRET` |
| 2 | `vite.config.ts` | 修改 | 增强 proxy 配置，添加 `/images`、`/upload` 代理，使用 `VITE_PROXY_TARGET` |
| 3 | `src/utils/replayProtection.ts` | **新建** | 防重放签名工具（Web Crypto API，异步） |
| 4 | `src/api/seckill.ts` | 修改 | `doSeckill`、`executeSeckill` 添加 async + await 签名头 |

---

## 三、后端改造（1 个文件）

### 3.1 文件：`seckill-mall/.../security/ReplayProtectionFilter.java`

**改什么**：在 `reject()` 方法中补充 CORS 响应头

**为什么**：`ReplayProtectionFilter` 使用 `@Order(Ordered.HIGHEST_PRECEDENCE + 20)` 注册，执行顺序在 Spring Security 过滤链之前。当它拒绝请求时，响应不经过 Spring Security 的 `CorsFilter`，因此缺少 CORS 头。

**改造后的效果**：
- Vite 代理模式下：同源请求，CORS 头有无都不影响（浏览器不校验同源响应的 CORS）
- 跨域直连模式下：CORS 头正确返回，前端能正常读取 401 错误信息，而非"网络错误"
- 生产环境 Nginx 代理下：同源请求，CORS 头有无都不影响

**改动前**（第125-131行）：
```java
private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
    // 安全修复（H2）：CORS 头统一由 SecurityConfig#corsConfigurationSource 管理，此处不再手动反射 Origin
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
}
```

**改动后**：
```java
/**
 * 安全修复（H2-补全）：本 Filter 在 Spring Security 过滤链之前执行（@Order=HIGHEST_PRECEDENCE+20），
 * reject 时 CorsFilter 尚未添加 CORS 头。对于跨域请求，浏览器会因缺少 CORS 头而阻止前端读取响应，
 * 导致前端只能看到"网络错误"而非具体的 401 错误信息。
 * 因此需在此处手动补充 CORS 头，逻辑与 SecurityConfig#corsConfigurationSource 保持一致。
 */
@Value("${seckill.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173,http://192.168.176.71:5173}")
private String corsAllowedOrigins;

private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
    // 补充 CORS 头：因为本 Filter 在 Spring Security 之前执行，
    // reject 时 CorsFilter 尚未添加 CORS 头，需手动补充
    String origin = request.getHeader("Origin");
    if (origin != null) {
        List<String> allowedOrigins = Arrays.asList(corsAllowedOrigins.split(","));
        if (allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Max-Age", "3600");
        }
    }
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
}
```

**需要在类顶部添加的 import**：
```java
import java.util.Arrays;
import java.util.List;
```

**需要在类中添加的字段**（已有 `signSecret`、`replayWindowSeconds` 等字段的位置附近）：
```java
@Value("${seckill.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173,http://192.168.176.71:5173}")
private String corsAllowedOrigins;
```

### 3.2 后端改造总结

| 序号 | 文件 | 改动类型 | 改动内容 |
|------|------|---------|---------|
| 1 | `ReplayProtectionFilter.java` | 修改 | `reject()` 方法补充 CORS 头，新增 `corsAllowedOrigins` 字段 |

---

## 四、改造后的请求链路

### 4.1 开发环境

```
浏览器(5173)
    │
    │  Axios baseURL = "" (空)
    │  请求 = "/api/v1/seckill/xxx/execute" (相对路径)
    │
    └──同源请求──→ Vite开发服务器(5173)
                      │
                      │  匹配 proxy['/api'] 规则
                      │  转发到 target: http://localhost:8080
                      │
                      └──服务端转发──→ Spring Boot(8080)
                                        │
                                        │  ReplayProtectionFilter 校验签名头 ✅
                                        │  JwtAuthenticationFilter 校验 Token ✅
                                        │  SecurityConfig 权限校验 ✅
                                        │  Controller 处理请求 ✅
                                        │
                                        └──响应──→ Vite(5173) ──→ 浏览器(5173)
                                                                     ↑
                                                               同源响应，无 CORS 问题
```

### 4.2 生产环境

```
浏览器
    │
    │  Axios baseURL = "" (空)
    │  请求 = "/api/v1/seckill/xxx/execute" (相对路径)
    │
    └──同源请求──→ Nginx(80)
                      │
                      │  location /api { proxy_pass http://backend:8080; }
                      │
                      └──服务端转发──→ Spring Boot(8080)
                                        │
                                        └──响应──→ Nginx(80) ──→ 浏览器
                                                              ↑
                                                        同源响应，无 CORS 问题
```

---

## 五、完整改动文件清单与逐行对比

### 5.1 `frontend/.env.development`

```diff
  # 开发环境
- VITE_API_BASE_URL=http://localhost:8080
+ # VITE_API_BASE_URL 留空，让所有 API 请求走相对路径，由 Vite 代理转发到后端
+ VITE_API_BASE_URL=
+ # Vite 代理转发的后端目标地址（仅开发环境使用）
+ VITE_PROXY_TARGET=http://localhost:8080
+ # 防重放签名密钥（必须与后端 seckill.security.sign-secret 一致，长度 >= 32）
+ VITE_SIGN_SECRET=your-sign-secret-at-least-32-chars-long-here
  VITE_APP_TITLE=SeckillMall 秒杀商城
```

### 5.2 `frontend/vite.config.ts`

```diff
     server: {
       host: '0.0.0.0',
       port: 5173,
       open: false,
       proxy: {
         '/api': {
-          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
-          changeOrigin: true
+          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
+          changeOrigin: true,
+          secure: false,
+          ws: true
+        },
+        '/images': {
+          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
+          changeOrigin: true,
+          secure: false
+        },
+        '/upload': {
+          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
+          changeOrigin: true,
+          secure: false
         }
       }
     }
```

### 5.3 `frontend/src/utils/replayProtection.ts`（新建）

```typescript
/**
 * 防重放签名工具（Web Crypto API 实现）
 * 对应后端 ReplayProtectionFilter 的签名校验逻辑
 * 签名算法：HMAC-SHA256(secret, timestamp + nonce + uri)
 */
const SIGN_SECRET: string = import.meta.env.VITE_SIGN_SECRET || ''

/** 缓存的 CryptoKey 对象（避免每次请求重复 importKey） */
let cachedKey: CryptoKey | null = null

if (!SIGN_SECRET) {
  console.warn('[replayProtection] VITE_SIGN_SECRET 未配置，防重放签名将不生效')
}

/** 获取或创建 HMAC-SHA256 的 CryptoKey 对象（带缓存） */
async function getSignKey(): Promise<CryptoKey> {
  if (cachedKey) return cachedKey
  const keyBytes = new TextEncoder().encode(SIGN_SECRET)
  cachedKey = await crypto.subtle.importKey(
    'raw',
    keyBytes,
    { name: 'HMAC', hash: 'SHA-256' },
    false,     // extractable=false：密钥对象不可导出
    ['sign']
  )
  return cachedKey
}

/** ArrayBuffer 转十六进制小写字符串 */
function bufferToHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

/**
 * 生成防重放请求头（异步）
 * @param uri 请求 URI 路径（如 /api/v1/seckill/123/execute）
 */
export async function generateReplayHeaders(uri: string): Promise<Record<string, string>> {
  if (!SIGN_SECRET) {
    return {}
  }
  const timestamp = Date.now().toString()
  const nonce = crypto.randomUUID()
  const payload = timestamp + nonce + uri
  const payloadBytes = new TextEncoder().encode(payload)
  const key = await getSignKey()
  const signature = await crypto.subtle.sign('HMAC', key, payloadBytes)
  const sign = bufferToHex(signature)
  return {
    'X-Sign': sign,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce
  }
}
```

### 5.4 `frontend/src/api/seckill.ts`

```diff
+ import { generateReplayHeaders } from '@/utils/replayProtection'

  /** 执行秒杀 */
- export function doSeckill(
+ export async function doSeckill(
    seckillId: number | string,
    seckillToken: string
  ): Promise<Result<SeckillResultVO>> {
-   return post<SeckillResultVO>(`/api/v1/seckill/${seckillId}`, undefined, {
-     params: { seckillToken },
-     headers: { 'X-Seckill-Token': seckillToken }
-   })
+   const uri = `/api/v1/seckill/${seckillId}`
+   const replayHeaders = await generateReplayHeaders(uri)
+   return post<SeckillResultVO>(uri, undefined, {
+     params: { seckillToken },
+     headers: {
+       'X-Seckill-Token': seckillToken,
+       ...replayHeaders
+     }
+   })
  }

  /** 一键执行秒杀（无需预取token，后端在 /execute 端点内部处理资格校验） */
- export function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
-   return post<SeckillResultVO>(`/api/v1/seckill/${seckillId}/execute`)
+ export async function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
+   const uri = `/api/v1/seckill/${seckillId}/execute`
+   const replayHeaders = await generateReplayHeaders(uri)
+   return post<SeckillResultVO>(uri, undefined, {
+     headers: replayHeaders
+   })
  }
```

### 5.4 `frontend/src/api/seckill.ts`

```diff
+ import { generateReplayHeaders } from '@/utils/replayProtection'

  /** 执行秒杀 */
  export function doSeckill(
    seckillId: number | string,
    seckillToken: string
  ): Promise<Result<SeckillResultVO>> {
-   return post<SeckillResultVO>(`/api/v1/seckill/${seckillId}`, undefined, {
-     params: { seckillToken },
-     headers: { 'X-Seckill-Token': seckillToken }
-   })
+   const uri = `/api/v1/seckill/${seckillId}`
+   return post<SeckillResultVO>(uri, undefined, {
+     params: { seckillToken },
+     headers: {
+       'X-Seckill-Token': seckillToken,
+       ...generateReplayHeaders(uri)
+     }
+   })
  }

  /** 一键执行秒杀（无需预取token，后端在 /execute 端点内部处理资格校验） */
  export function executeSeckill(seckillId: number | string): Promise<Result<SeckillResultVO>> {
-   return post<SeckillResultVO>(`/api/v1/seckill/${seckillId}/execute`)
+   const uri = `/api/v1/seckill/${seckillId}/execute`
+   return post<SeckillResultVO>(uri, undefined, {
+     headers: generateReplayHeaders(uri)
+   })
  }
```

### 5.5 `seckill-mall/.../security/ReplayProtectionFilter.java`

```diff
  import java.io.IOException;
  import java.nio.charset.StandardCharsets;
+ import java.util.Arrays;
  import java.util.HexFormat;
+ import java.util.List;
  import java.util.concurrent.TimeUnit;

  // ... (类定义不变) ...

      @Value("${seckill.security.replay-window-seconds:60}")
      private long replayWindowSeconds;

+     @Value("${seckill.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173,http://192.168.176.71:5173}")
+     private String corsAllowedOrigins;

  // ... (doFilterInternal 不变) ...

      private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
-         // 安全修复（H2）：CORS 头统一由 SecurityConfig#corsConfigurationSource 管理，此处不再手动反射 Origin
+         // 补充 CORS 头：本 Filter 在 Spring Security 之前执行，
+         // reject 时 CorsFilter 尚未添加 CORS 头，需手动补充
+         String origin = request.getHeader("Origin");
+         if (origin != null) {
+             List<String> allowedOrigins = Arrays.asList(corsAllowedOrigins.split(","));
+             if (allowedOrigins.contains(origin)) {
+                 response.setHeader("Access-Control-Allow-Origin", origin);
+                 response.setHeader("Access-Control-Allow-Credentials", "true");
+                 response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
+                 response.setHeader("Access-Control-Allow-Headers", "*");
+                 response.setHeader("Access-Control-Max-Age", "3600");
+             }
+         }
          response.setStatus(HttpStatus.UNAUTHORIZED.value());
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response.setCharacterEncoding(StandardCharsets.UTF_8.name());
          response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
      }
```

---

## 六、验证步骤

### 6.1 改造后验证清单

| 序号 | 验证项 | 验证方法 | 预期结果 |
|------|--------|---------|---------|
| 1 | Vite 代理生效 | 浏览器 F12 → Network，查看 API 请求的 URL | 应为 `http://localhost:5173/api/v1/...`，不再是 `http://localhost:8080/api/v1/...` |
| 2 | 请求不再跨域 | F12 → Network → 请求头 | 无 `Origin` 头（同源请求不发送 Origin） |
| 3 | 秒杀抢购不再"网络错误" | 首页点击"立即抢购" | 如签名正确，正常走秒杀流程；如签名错误，显示具体错误信息而非"网络错误" |
| 4 | 防重放签名头存在 | F12 → Network → 秒杀 POST 请求头 | 包含 `X-Sign`、`X-Timestamp`、`X-Nonce` |
| 5 | 图片正常加载 | 首页商品图片 | 图片通过 `/images` 代理正常显示 |
| 6 | Token 刷新正常 | 等待 Access Token 过期后操作 | 自动刷新 Token，不跳转登录页 |
| 7 | 其他 API 正常 | 浏览商品、加购物车、下单等 | 所有功能正常 |

### 6.2 快速验证命令

```bash
# 1. 启动后端（确保 SECKILL_SIGN_SECRET 环境变量已设置）
cd seckill-mall
# 设置环境变量（PowerShell）
$env:SECKILL_SIGN_SECRET="your-sign-secret-at-least-32-chars-long-here"
mvn spring-boot:run

# 2. 启动前端（无需额外安装依赖，Web Crypto API 为浏览器原生）
cd frontend
npm run dev

# 3. 浏览器访问 http://localhost:5173
#    F12 打开 Network 面板，观察请求是否走 5173 端口
```

---

## 七、Vite 代理 vs 直连后端：完整对比

### 7.1 请求链路对比

```
【方式A：跨域直连后端（当前方式）】
浏览器(5173) ──跨域──→ 后端(8080) ──响应──→ 浏览器检查CORS ──→ 前端JS

  优点：配置简单，一行 VITE_API_BASE_URL 搞定
  缺点：
    ✗ 所有请求都是跨域的，完全依赖后端 CORS 配置正确
    ✗ 后端任何 Filter/Interceptor 漏加 CORS 头 → 请求失败 → "网络错误"
    ✗ 浏览器对跨域请求有额外限制（如不能发送某些自定义头）
    ✗ 每次请求浏览器都先发 OPTIONS 预检请求（增加延迟）
    ✗ Vite 代理配置完全浪费

【方式B：Vite代理转发（改造后方式）】
浏览器(5173) ──同源──→ Vite(5173) ──转发──→ 后端(8080) ──响应──→ Vite(5173) ──→ 前端JS

  优点：
    ✓ 所有请求都是同源的，无需 CORS 校验
    ✓ 后端 CORS 配置出错也不影响开发
    ✓ 无 OPTIONS 预检请求，减少延迟
    ✓ 浏览器无跨域限制
    ✓ 与生产环境（Nginx 代理）行为一致
  缺点：
    ✗ 需要理解 Vite 代理配置
    ✗ 调试时 Network 面板看到的是 5173 端口（需注意实际请求到了 8080）
```

### 7.2 CORS 预检请求对比

```
【直连模式】每次跨域 POST 请求前，浏览器先发 OPTIONS 预检：
  OPTIONS http://localhost:8080/api/v1/seckill/xxx/execute
  → 等待后端返回 CORS 头
  → 确认允许后才发实际 POST 请求
  → 一次业务请求 = 2 次 HTTP 请求（OPTIONS + POST）

【代理模式】同源请求，浏览器不发 OPTIONS 预检：
  POST http://localhost:5173/api/v1/seckill/xxx/execute
  → Vite 直接转发到后端
  → 一次业务请求 = 1 次 HTTP 请求
```

### 7.3 错误处理对比

| 错误场景 | 直连模式表现 | 代理模式表现 |
|---------|------------|------------|
| 后端返回 401（带 CORS 头） | 前端正常读取 401，触发 Token 刷新 | 同左 |
| 后端返回 401（**不带** CORS 头） | 浏览器阻止读取 → "网络错误" | 前端正常读取 401（同源不受 CORS 限制） |
| 后端宕机无响应 | "网络异常" | "网络异常"（Vite 代理返回 502） |
| ReplayProtectionFilter 拒绝 | 改造前："网络错误"；改造后：正常 401 | 正常 401 |
| 签名密钥不匹配 | 改造前："网络错误"；改造后：具体签名错误 | 具体签名错误 |

---

## 八、注意事项与常见问题

### Q1：Vite 代理只在开发时有效，生产环境怎么办？

生产环境使用 Nginx 做反向代理，配置类似：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 图片代理
    location /images {
        proxy_pass http://backend:8080;
    }
}
```

### Q2：签名密钥在前端代码中暴露是否安全？

防重放签名的目的是**防止机器人/脚本批量刷秒杀接口**，而非防止密钥被破解。即使攻击者获取了签名密钥，仍需：
1. 为每个请求生成唯一 Nonce（Redis 去重）
2. 在 60 秒时间窗口内发送（时间校验）
3. 通过 RateLimit 限流（每秒 1 次）

这三层防护叠加，即使密钥泄露也能有效防刷。生产环境建议通过 CI/CD 环境变量注入密钥，不在代码仓库中明文存储。

**Web Crypto API 的额外安全优势**：`CryptoKey` 对象设置 `extractable: false`，即使 XSS 攻击者获取了 key 对象，也无法通过 `crypto.subtle.exportKey()` 导出原始密钥字节。不过需要注意，创建 `CryptoKey` 时的原始密钥字符串仍存在于 JS 内存中，`non-extractable` 主要防止通过标准 API 接口导出，不能完全替代服务端签名方案。

### Q2.1：为什么选 Web Crypto API 而非 crypto-js？

| 维度 | Web Crypto API | crypto-js |
|------|---------------|-----------|
| **依赖** | ✅ 零依赖（浏览器原生） | ❌ 需安装 npm 包（~70KB gzip） |
| **性能** | ✅ 原生实现（OS 级加密），单次 ~0.05ms | ❌ 纯 JS 实现，单次 ~0.5ms |
| **异步** | ✅ 异步非阻塞（不阻塞主线程） | ❌ 同步（阻塞主线程） |
| **密钥安全** | ✅ CryptoKey 默认 non-extractable | ❌ 密钥是普通 JS 字符串 |
| **标准** | ✅ W3C 标准 API | 社区库 |
| **兼容性** | ⚠️ 仅 Secure Context（HTTPS/localhost） | ✅ 所有环境 |
| **调用方式** | ⚠️ 异步（需 await） | ✅ 同步（一行搞定） |

**选择 Web Crypto API 的理由**：
1. 本项目秒杀场景运行在 HTTPS/localhost，满足 Secure Context 要求
2. 异步签名不阻塞 UI 渲染，对秒杀按钮点击体验更友好
3. 零依赖减少打包体积，且无需维护额外 npm 包
4. CryptoKey 不可导出提供额外安全层（虽然原始密钥仍在内存中）
5. `importKey` 结果缓存后，后续调用仅需 `sign` 一次异步操作，性能开销可忽略

### Q3：Vite 代理会不会影响热更新（HMR）？

不会。Vite 的 HMR 和代理是独立的功能，互不影响。代理只拦截匹配路径的请求，其他请求（包括 HMR 的 WebSocket 连接）正常处理。

### Q4：开发时如何确认请求确实经过了 Vite 代理？

在 Vite 代理配置中添加 `configure` 回调，打印日志：

```typescript
proxy: {
  '/api': {
    target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
    changeOrigin: true,
    secure: false,
    ws: true,
    configure: (proxy, _options) => {
      proxy.on('proxyReq', (proxyReq, req, _res) => {
        console.log(`[Vite Proxy] ${req.method} ${req.url} → ${proxyReq.getHeader('host')}${proxyReq.path}`)
      })
    }
  }
}
```

终端会显示类似：`[Vite Proxy] POST /api/v1/seckill/xxx/execute → localhost:8080/api/v1/seckill/xxx/execute`

### Q5：`.env.production` 需要改吗？

不需要。`VITE_API_BASE_URL=` 已经是空值，生产环境请求走相对路径，由 Nginx 代理转发。但需要在 `.env.production` 中也添加签名密钥：

```properties
# 生产环境
VITE_API_BASE_URL=
# 防重放签名密钥（生产环境通过 CI/CD 注入，不要在代码中明文写死）
VITE_SIGN_SECRET=
VITE_APP_TITLE=SeckillMall 秒杀商城
```