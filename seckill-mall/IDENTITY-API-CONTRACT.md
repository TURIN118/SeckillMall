# Identity 模块 API 契约 (IDENTITY-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase 4 - Identity 模块迁移（Strangler Pattern 第三阶段）
> 依据：IDENTITY-MIGRATION-PLAN.md + PRODUCT-API-CONTRACT.md（格式参考）
> 说明：纯架构设计文档，未创建任何代码/接口。本文档定义 API 契约，实施时再创建接口。

---

## 1. 契约设计原则

1. **API 方法用业务语言命名**：如 `register`、`login`、`getUserById`、`addFavorite`，不用技术语言（如 `insert`、`updateById`）。
2. **入参用 Command/Query，出参用 Result/DTO/Snapshot**：禁止裸露 `Long userId, String phone, ...` 多参数，封装为 Command 对象（简单只读查询可保留基本类型入参）。
3. **禁止暴露 Entity/Mapper/PO**：API 签名中不出现 `User`、`UserAddress`、`UserFavorite`、`LoginLog`、`UserMapper` 等基础设施类型。
4. **禁止暴露内部 Service**：`AuthServiceImpl`、`UserServiceImpl` 等实现类不对外暴露，仅通过 `UserApi` 等 API 接口交互。
5. **异常用业务错误码（ErrorCode）**：所有异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈。
6. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO/Snapshot 字段只增不删不改变类型。
7. **Entity → Snapshot 映射**：跨模块传递时，`User` Entity 映射为 `UserSnapshot`，`UserAddress` Entity 映射为 `AddressDTO`，裁剪掉 `password`/`isDeleted` 等基础设施字段。
8. **security 包不通过 API 暴露**：`SecurityUtils`、`JwtUtils` 等横切基础设施不通过 identity.api 暴露，外部模块通过 `shared.kernel.CurrentUserContext` 获取当前用户。

---

## 2. UserApi（用户业务能力）

> 包路径：`com.seckill.mall.identity.api.UserApi`
> 职责：用户资料管理 + 密码/手机/邮箱修改 + 余额操作 + 统计查询 + 跨模块只读快照
> 原 Service：`UserService`（13 方法）+ `AuthService` 部分（getMe/updateProfile/changePassword/uploadAvatar）
> 实现类：`UserApplicationService`（`identity.application`）

### 2.1 getUserById - 查询用户快照

| 项 | 定义 |
|---|---|
| **方法签名** | `UserSnapshot getUserById(Long userId)` |
| **调用方** | order, payment, coupon, seckill, review, stats（跨模块只读） |
| **业务语义** | 根据 ID 查询用户快照（跨模块只读访问，**返回 Snapshot 而非 Entity**） |
| **原方法** | `UserService.getUserById(Long)` → `User`（**Entity 泄露，改为返回 UserSnapshot**） |

**入参**：`Long userId`（用户 ID）

**出参 UserSnapshot**：见 §7.1（不存在返回 null）

**异常**：无

**依赖的外部 API**：无（仅查自身持久层）

---

### 2.2 getCurrentUser - 获取当前登录用户

| 项 | 定义 |
|---|---|
| **方法签名** | `UserSnapshot getCurrentUser()` |
| **调用方** | AuthController（/auth/me 端点） |
| **业务语义** | 获取当前登录用户的完整信息（从安全上下文提取 userId 后查询） |
| **原方法** | `AuthService.getMe()` → `UserVO` |

**入参**：无

**出参**：`UserSnapshot`（见 §7.1）

**异常**：`UNAUTHORIZED`

**依赖的外部 API**：`shared.kernel.CurrentUserContext`（获取当前用户 ID）

---

### 2.3 updateProfile - 更新个人资料

| 项 | 定义 |
|---|---|
| **方法签名** | `UserSnapshot updateProfile(UpdateProfileCommand command)` |
| **调用方** | AuthController（/auth/profile 端点） |
| **业务语义** | 更新当前登录用户的个人信息（昵称/邮箱/手机号/头像 URL） |
| **原方法** | `AuthService.updateProfile(ProfileUpdateRequest)` → `UserVO` |

**入参 UpdateProfileCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| avatarUrl | String | 否 | 头像 URL |

**出参**：`UserSnapshot`（更新后的用户信息）

**异常**：`UNAUTHORIZED`、`PARAM_ERROR`

**依赖的外部 API**：`shared.kernel.CurrentUserContext`

---

### 2.4 changePassword - 修改密码

| 项 | 定义 |
|---|---|
| **方法签名** | `void changePassword(ChangePasswordCommand command)` |
| **调用方** | AuthController（/auth/password 端点） |
| **业务语义** | 修改当前登录用户的密码（校验旧密码，BCrypt 加密新密码） |
| **原方法** | `AuthService.changePassword(ChangePasswordRequest)` |

**入参 ChangePasswordCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | String | 是 | 旧密码（明文，服务端 BCrypt 校验） |
| newPassword | String | 是 | 新密码（明文，服务端 BCrypt 加密存储） |

**出参**：`void`

**异常**：`UNAUTHORIZED`、`PASSWORD_INCORRECT`、`PARAM_ERROR`

**依赖的外部 API**：`shared.kernel.CurrentUserContext`；security 包（TokenVersionService 更新密码版本）

---

### 2.5 updateUserPhone - 修改手机号

| 项 | 定义 |
|---|---|
| **方法签名** | `UserSnapshot updateUserPhone(UpdatePhoneCommand command)` |
| **调用方** | UserController（/users/profile/phone 端点，需先完成验证码校验） |
| **业务语义** | 修改用户手机号（调用方需先完成验证码校验） |
| **原方法** | `UserService.updatePhone(Long, String)` → `UserVO` |

**入参 UpdatePhoneCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| phone | String | 是 | 新手机号（11 位） |

**出参**：`UserSnapshot`（更新后的用户信息）

**异常**：`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：无

---

### 2.6 updateUserEmail - 修改邮箱

| 项 | 定义 |
|---|---|
| **方法签名** | `UserSnapshot updateUserEmail(UpdateEmailCommand command)` |
| **调用方** | UserController（/users/profile/email 端点，需先完成验证码校验） |
| **业务语义** | 修改用户邮箱（调用方需先完成验证码校验） |
| **原方法** | `UserService.updateEmail(Long, String)` → `UserVO` |

**入参 UpdateEmailCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| email | String | 是 | 新邮箱 |

**出参**：`UserSnapshot`

**异常**：`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：无

---

### 2.7 getUserEmail - 查询用户邮箱

| 项 | 定义 |
|---|---|
| **方法签名** | `String getUserEmail(Long userId)` |
| **调用方** | order（订单通知邮件）、内部服务 |
| **业务语义** | 查询用户邮箱（用于订单通知等场景） |
| **原方法** | `UserService.getEmail(Long)` |

**入参**：`Long userId`

**出参**：`String`（邮箱地址，用户不存在时返回 null）

**异常**：无

**依赖的外部 API**：无

---

### 2.8 getUserDisplayNamesByIds - 批量查询用户显示名

| 项 | 定义 |
|---|---|
| **方法签名** | `Map<Long, String> getUserDisplayNamesByIds(List<Long> userIds)` |
| **调用方** | review, stats, system（批量展示用户名） |
| **业务语义** | 批量查询用户显示名（nickname 优先，回退 username） |
| **原方法** | `UserService.getUserDisplayNamesByIds(List<Long>)` |

**入参**：`List<Long> userIds`

**出参**：`Map<Long, String>`（userId → displayName，空列表返回 emptyMap）

**异常**：无

**依赖的外部 API**：无

---

### 2.9 getUsernamesByIds - 批量查询用户名

| 项 | 定义 |
|---|---|
| **方法签名** | `Map<Long, String> getUsernamesByIds(List<Long> userIds)` |
| **调用方** | stats, system |
| **业务语义** | 批量查询用户名（username） |
| **原方法** | `UserService.getUsernamesByIds(List<Long>)` |

**入参**：`List<Long> userIds`

**出参**：`Map<Long, String>`（userId → username，空列表返回 emptyMap）

**异常**：无

**依赖的外部 API**：无

---

### 2.10 countAll - 用户总数

| 项 | 定义 |
|---|---|
| **方法签名** | `long countAll()` |
| **调用方** | stats（统计概览） |
| **业务语义** | 查询用户总数（封装 Mapper.selectCount，消除跨模块 Mapper 依赖） |
| **原方法** | `UserService.countAll()` |

**入参**：无

**出参**：`long`

**异常**：无

**依赖的外部 API**：无

---

### 2.11 countTodayRegistered - 今日注册用户数

| 项 | 定义 |
|---|---|
| **方法签名** | `Long countTodayRegistered(LocalDate today)` |
| **调用方** | stats |
| **业务语义** | 查询指定日期的注册用户数 |
| **原方法** | `UserService.countTodayRegistered(LocalDate)` |

**入参**：`LocalDate today`（查询日期）

**出参**：`Long`（注册数，可能为 null）

**异常**：无

**依赖的外部 API**：无

---

### 2.12 selectUserTrend - 用户注册趋势

| 项 | 定义 |
|---|---|
| **方法签名** | `List<Map<String, Object>> selectUserTrend(LocalDate startDate, LocalDate endDate)` |
| **调用方** | stats |
| **业务语义** | 查询日期范围内的用户注册趋势 |
| **原方法** | `UserService.selectUserTrend(LocalDate, LocalDate)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| startDate | LocalDate | 起始日期（含） |
| endDate | LocalDate | 结束日期（含） |

**出参**：`List<Map<String, Object>>`（每行包含 dt(日期)、cnt(注册数)）

**异常**：无

**依赖的外部 API**：无

---

### 2.13 addBalance - 用户余额增加

| 项 | 定义 |
|---|---|
| **方法签名** | `void addBalance(Long userId, BigDecimal amount)` |
| **调用方** | payment（RechargeCardServiceImpl 充值卡核销增加余额） |
| **业务语义** | 用户钱包余额增加（原子操作 `balance = balance + amount`，避免覆盖更新） |
| **原方法** | `UserService.addBalance(Long, BigDecimal)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户 ID |
| amount | BigDecimal | 增加金额（>0） |

**出参**：`void`

**异常**：`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：无

---

### 2.14 deductBalance - 扣减用户钱包余额

| 项 | 定义 |
|---|---|
| **方法签名** | `int deductBalance(Long userId, BigDecimal amount)` |
| **调用方** | payment（WalletServiceImpl 支付扣款） |
| **业务语义** | 扣减用户钱包余额（原子操作） |
| **原方法** | `UserService.deductBalance(Long, BigDecimal)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户 ID |
| amount | BigDecimal | 扣减金额（>0） |

**出参**：`int`（受影响行数，0 表示余额不足或用户不存在）

**异常**：无（余额不足时返回 0，由调用方判断）

**依赖的外部 API**：无

---

### 2.15 uploadAvatar - 上传头像

| 项 | 定义 |
|---|---|
| **方法签名** | `Map<String, String> uploadAvatar(UploadAvatarCommand command)` |
| **调用方** | AuthController（/auth/avatar 端点） |
| **业务语义** | 上传当前登录用户的头像，并持久化头像 URL |
| **原方法** | `AuthService.uploadAvatar(MultipartFile)` |

**入参 UploadAvatarCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | MultipartFile | 是 | 头像文件（image/jpeg、image/png、image/webp，最大 2MB） |

**出参**：`Map<String, String>`（包含 `avatar` → URL 的映射）

**异常**：`UNAUTHORIZED`、`FILE_TOO_LARGE`、`FILE_TYPE_NOT_SUPPORTED`

**依赖的外部 API**：`upload.api.UploadApi`（过渡期保留 `UploadService` 引用）；`shared.kernel.CurrentUserContext`

---

## 3. AuthApi（认证能力）

> 包路径：`com.seckill.mall.identity.api.AuthApi`
> 职责：注册/登录/登出/刷新令牌/找回密码 + 验证码（邮箱/短信/图形）
> 原 Service：`AuthService` 部分（register/login/logout/refresh/sendForgotPasswordCode/resetPassword）+ `VerificationCodeService` + `CaptchaService`
> 实现类：`AuthApplicationService`（`identity.application`），委托 `VerificationCodeApplicationService` 和 `CaptchaApplicationService`

### 3.1 register - 用户注册

| 项 | 定义 |
|---|---|
| **方法签名** | `UserSnapshot register(RegisterCommand command)` |
| **调用方** | AuthController（/auth/register 端点） |
| **业务语义** | 用户注册（校验用户名/手机号唯一 → BCrypt 加密密码 → 保存用户 → 返回用户信息） |
| **原方法** | `AuthService.register(RegisterRequest)` → `UserVO` |

**入参 RegisterCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码（明文，服务端 BCrypt 加密） |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| nickname | String | 否 | 昵称（默认同 username） |
| captchaId | String | 是 | 图形验证码 ID |
| captchaCode | String | 是 | 图形验证码 |

**出参**：`UserSnapshot`（注册后的用户信息）

**异常**：`USERNAME_ALREADY_EXISTS`、`PHONE_ALREADY_EXISTS`、`CAPTCHA_INVALID`、`PARAM_ERROR`

**依赖的外部 API**：security 包（SecurityUserDetailsService）；`CaptchaApplicationService`（验证码校验）

---

### 3.2 login - 用户登录

| 项 | 定义 |
|---|---|
| **方法签名** | `LoginResult login(LoginCommand command)` |
| **调用方** | AuthController（/auth/login 端点） |
| **业务语义** | 用户登录（校验用户名密码 → 生成 JWT → 记录登录日志 → 返回 token + 用户信息） |
| **原方法** | `AuthService.login(LoginRequest, String, HttpServletRequest)` → `LoginVO` |

**入参 LoginCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码（明文） |
| captchaId | String | 是 | 图形验证码 ID |
| captchaCode | String | 是 | 图形验证码 |
| ip | String | 是 | 客户端 IP（密码喷洒防护） |
| userAgent | String | 否 | User-Agent（日志补全） |

**出参 LoginResult**：见 §7.6

**异常**：`USERNAME_OR_PASSWORD_ERROR`、`CAPTCHA_INVALID`、`USER_DISABLED`、`USER_LOCKED`、`PARAM_ERROR`

**依赖的外部 API**：security 包（JwtUtils 生成 token、TokenVersionService、SecurityUserDetailsService）；`CaptchaApplicationService`

---

### 3.3 logout - 退出登录

| 项 | 定义 |
|---|---|
| **方法签名** | `void logout(String accessToken)` |
| **调用方** | AuthController（/auth/logout 端点） |
| **业务语义** | 退出登录（将 access token 加入黑名单） |
| **原方法** | `AuthService.logout(String)` |

**入参**：`String accessToken`（Authorization 头）

**出参**：`void`

**异常**：无

**依赖的外部 API**：security 包（TokenBlacklistService）

---

### 3.4 refreshToken - 刷新令牌

| 项 | 定义 |
|---|---|
| **方法签名** | `TokenResult refreshToken(RefreshTokenCommand command)` |
| **调用方** | AuthController（/auth/refresh 端点） |
| **业务语义** | 刷新令牌（校验 refresh token → 生成新 access token + refresh token） |
| **原方法** | `AuthService.refresh(RefreshTokenRequest)` → `TokenVO` |

**入参 RefreshTokenCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| refreshToken | String | 是 | 刷新令牌 |

**出参 TokenResult**：见 §7.7

**异常**：`REFRESH_TOKEN_INVALID`、`REFRESH_TOKEN_EXPIRED`、`USER_DISABLED`

**依赖的外部 API**：security 包（JwtUtils、TokenVersionService、TokenBlacklistService）

---

### 3.5 sendForgotPasswordCode - 找回密码发送验证码

| 项 | 定义 |
|---|---|
| **方法签名** | `void sendForgotPasswordCode(SendCodeCommand command)` |
| **调用方** | AuthController（/auth/forgot-password/send-code 端点） |
| **业务语义** | 根据 type（PHONE/EMAIL）查询用户是否存在，存在则发送验证码并存入 Redis |
| **原方法** | `AuthService.sendForgotPasswordCode(ForgotPasswordSendRequest)` |

**入参 SendCodeCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | String | 是 | 发送方式（PHONE/EMAIL） |
| account | String | 是 | 手机号或邮箱 |

**出参**：`void`

**异常**：`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：`VerificationCodeApplicationService`（发送验证码）

---

### 3.6 resetPassword - 找回密码重置

| 项 | 定义 |
|---|---|
| **方法签名** | `void resetPassword(ResetPasswordCommand command)` |
| **调用方** | AuthController（/auth/forgot-password/reset 端点） |
| **业务语义** | 校验验证码 → 查询用户 → BCrypt 加密新密码 → 更新 → 删除 Redis 验证码 |
| **原方法** | `AuthService.resetPassword(ForgotPasswordResetRequest)` |

**入参 ResetPasswordCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | String | 是 | 发送方式（PHONE/EMAIL） |
| account | String | 是 | 手机号或邮箱 |
| code | String | 是 | 验证码 |
| newPassword | String | 是 | 新密码（明文） |

**出参**：`void`

**异常**：`VERIFICATION_CODE_INVALID`、`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：`VerificationCodeApplicationService`（校验验证码）

---

### 3.7 generateCaptcha - 生成图形验证码

| 项 | 定义 |
|---|---|
| **方法签名** | `CaptchaResult generateCaptcha()` |
| **调用方** | AuthController（/auth/captcha 端点） |
| **业务语义** | 生成图形验证码（UUID + Base64 图片 + 存入 Redis，TTL 5 分钟） |
| **原方法** | `CaptchaService.generateCaptcha()` → `CaptchaVO` |

**入参**：无

**出参 CaptchaResult**：见 §7.8

**异常**：无

**依赖的外部 API**：`shared.kernel.CachePort`（存储验证码）

---

### 3.8 verifyCaptcha - 校验图形验证码

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean verifyCaptcha(String captchaId, String captchaCode)` |
| **调用方** | AuthApplicationService（内部，注册/登录时校验） |
| **业务语义** | 校验图形验证码（一次性，校验后无论结果均删除） |
| **原方法** | `CaptchaService.verifyCaptcha(String, String)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| captchaId | String | 验证码 ID |
| captchaCode | String | 用户输入的验证码 |

**出参**：`boolean`（true=校验成功）

**异常**：无

**依赖的外部 API**：`shared.kernel.CachePort`

---

### 3.9 sendEmailCode - 发送邮箱验证码

| 项 | 定义 |
|---|---|
| **方法签名** | `void sendEmailCode(String email)` |
| **调用方** | VerificationCodeController（/verification/send-email 端点） |
| **业务语义** | 发送邮箱验证码（通过 Spring Mail 真实发送，存入 Redis，TTL 5 分钟） |
| **原方法** | `VerificationCodeService.sendEmailCode(String)` |

**入参**：`String email`（目标邮箱）

**出参**：`void`

**异常**：`PARAM_ERROR`

**依赖的外部 API**：`shared.kernel.CachePort`；upload 模块（EmailService，过渡期）

---

### 3.10 sendSmsCode - 发送短信验证码

| 项 | 定义 |
|---|---|
| **方法签名** | `void sendSmsCode(String phone)` |
| **调用方** | VerificationCodeController（/verification/send-sms 端点） |
| **业务语义** | 发送短信验证码（控制台打印 + Redis 存储，TTL 5 分钟） |
| **原方法** | `VerificationCodeService.sendSmsCode(String)` |

**入参**：`String phone`（目标手机号）

**出参**：`void`

**异常**：`PARAM_ERROR`

**依赖的外部 API**：`shared.kernel.CachePort`

---

### 3.11 verifyCode - 校验验证码

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean verifyCode(String target, String code)` |
| **调用方** | VerificationCodeController（/verification/verify 端点）、UserController（修改手机/邮箱前校验）、AuthApplicationService（找回密码校验） |
| **业务语义** | 校验验证码（从 Redis 获取并比对） |
| **原方法** | `VerificationCodeService.verifyCode(String, String)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| target | String | 验证码目标（邮箱或手机号） |
| code | String | 用户输入的验证码 |

**出参**：`boolean`（true=校验成功）

**异常**：无

**依赖的外部 API**：`shared.kernel.CachePort`

---

## 4. AddressApi（收货地址能力）

> 包路径：`com.seckill.mall.identity.api.AddressApi`
> 职责：收货地址 CRUD + 设置默认地址 + 跨模块只读查询
> 原 Service：`UserAddressService`（6 方法）
> 实现类：`AddressApplicationService`（`identity.application`）

### 4.1 listAddresses - 查询用户地址列表

| 项 | 定义 |
|---|---|
| **方法签名** | `List<AddressDTO> listAddresses(Long userId)` |
| **调用方** | UserAddressController（/addresses/list 端点） |
| **业务语义** | 查询指定用户的所有收货地址（按默认地址优先、更新时间倒序排列） |
| **原方法** | `UserAddressService.listByUserId(Long)` → `List<UserAddressVO>` |

**入参**：`Long userId`

**出参**：`List<AddressDTO>`（见 §7.2）

**异常**：无

**依赖的外部 API**：无

---

### 4.2 getAddressById - 查询单个地址

| 项 | 定义 |
|---|---|
| **方法签名** | `AddressDTO getAddressById(Long addressId)` |
| **调用方** | order（OrderServiceImpl 下单时校验收货地址归属、组装订单详情） |
| **业务语义** | 根据地址 ID 查询地址（跨模块只读，**返回 DTO 而非 Entity**） |
| **原方法** | `UserAddressService.getAddressById(Long)` → `UserAddress`（**Entity 泄露，改为返回 AddressDTO**） |

**入参**：`Long addressId`

**出参**：`AddressDTO`（见 §7.2，不存在返回 null）

**异常**：无

**依赖的外部 API**：无

---

### 4.3 saveAddress - 新增收货地址

| 项 | 定义 |
|---|---|
| **方法签名** | `AddressDTO saveAddress(SaveAddressCommand command)` |
| **调用方** | UserAddressController（/addresses/create 端点） |
| **业务语义** | 新增收货地址（若用户此前无地址则自动设为默认；若 isDefault=1 则先取消其他默认） |
| **原方法** | `UserAddressService.create(Long, UserAddressVO)` → `UserAddressVO` |

**入参 SaveAddressCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| receiverName | String | 是 | 收件人姓名 |
| receiverPhone | String | 是 | 收件人手机号 |
| province | String | 是 | 省份 |
| city | String | 是 | 城市 |
| district | String | 是 | 区/县 |
| detailAddress | String | 是 | 详细地址 |
| isDefault | Integer | 否 | 是否默认（0/1，默认 0） |

**出参**：`AddressDTO`（含生成的 id）

**异常**：`PARAM_ERROR`

**依赖的外部 API**：无

---

### 4.4 updateAddress - 编辑收货地址

| 项 | 定义 |
|---|---|
| **方法签名** | `AddressDTO updateAddress(UpdateAddressCommand command)` |
| **调用方** | UserAddressController（/addresses/{id} 端点） |
| **业务语义** | 编辑收货地址（校验归属当前用户；若 isDefault 由 0 改 1 则先取消其他默认） |
| **原方法** | `UserAddressService.update(Long, Long, UserAddressVO)` → `UserAddressVO` |

**入参 UpdateAddressCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| addressId | Long | 是 | 地址 ID |
| receiverName | String | 否 | 收件人姓名 |
| receiverPhone | String | 否 | 收件人手机号 |
| province | String | 否 | 省份 |
| city | String | 否 | 城市 |
| district | String | 否 | 区/县 |
| detailAddress | String | 否 | 详细地址 |
| isDefault | Integer | 否 | 是否默认 |

**出参**：`AddressDTO`

**异常**：`ADDRESS_NOT_FOUND`、`ADDRESS_NOT_BELONG_TO_USER`、`PARAM_ERROR`

**依赖的外部 API**：无

---

### 4.5 deleteAddress - 删除收货地址

| 项 | 定义 |
|---|---|
| **方法签名** | `void deleteAddress(Long userId, Long addressId)` |
| **调用方** | UserAddressController（/addresses/{id} 端点） |
| **业务语义** | 逻辑删除收货地址（校验归属当前用户） |
| **原方法** | `UserAddressService.delete(Long, Long)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户 ID |
| addressId | Long | 地址 ID |

**出参**：`void`

**异常**：`ADDRESS_NOT_FOUND`、`ADDRESS_NOT_BELONG_TO_USER`

**依赖的外部 API**：无

---

### 4.6 setDefaultAddress - 设置默认地址

| 项 | 定义 |
|---|---|
| **方法签名** | `void setDefaultAddress(Long userId, Long addressId)` |
| **调用方** | UserAddressController（/addresses/{id}/default 端点） |
| **业务语义** | 设置默认地址（先将该用户所有地址 is_default 置 0，再将目标置 1，事务内完成） |
| **原方法** | `UserAddressService.setDefault(Long, Long)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户 ID |
| addressId | Long | 地址 ID |

**出参**：`void`

**异常**：`ADDRESS_NOT_FOUND`、`ADDRESS_NOT_BELONG_TO_USER`

**依赖的外部 API**：无

---

## 5. FavoriteApi（用户收藏能力）

> 包路径：`com.seckill.mall.identity.api.FavoriteApi`
> 职责：收藏管理（列表/添加/移除/检查/数量）
> 原 Service：`UserFavoriteService`（5 方法）
> 实现类：`FavoriteApplicationService`（`identity.application`）

### 5.1 listFavorites - 查询收藏列表

| 项 | 定义 |
|---|---|
| **方法签名** | `List<FavoriteItemDTO> listFavorites(Long userId)` |
| **调用方** | UserFavoriteController（/favorites/list 端点） |
| **业务语义** | 获取指定用户的收藏列表（含商品展示信息） |
| **原方法** | `UserFavoriteService.getFavoriteList(Long)` → `Result<List<FavoriteItemVO>>` |

**入参**：`Long userId`

**出参**：`List<FavoriteItemDTO>`（见 §7.3）

**异常**：无

**依赖的外部 API**：`product.api.ProductApi`（查商品快照展示信息）

---

### 5.2 addFavorite - 添加收藏

| 项 | 定义 |
|---|---|
| **方法签名** | `void addFavorite(AddFavoriteCommand command)` |
| **调用方** | UserFavoriteController（/favorites/add 端点） |
| **业务语义** | 添加收藏（若已存在含逻辑删除的记录则恢复 is_deleted=0，否则新建；同步递增 t_product.favorite_count） |
| **原方法** | `UserFavoriteService.addFavorite(Long, Long)` |

**入参 AddFavoriteCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| productId | Long | 是 | 商品 ID |

**出参**：`void`

**异常**：`PRODUCT_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：`product.api.ProductApi`（updateFavoriteCount 递增收藏计数）

---

### 5.3 removeFavorite - 取消收藏

| 项 | 定义 |
|---|---|
| **方法签名** | `void removeFavorite(RemoveFavoriteCommand command)` |
| **调用方** | UserFavoriteController（/favorites/{productId} 端点） |
| **业务语义** | 取消收藏（逻辑删除；同步递减 t_product.favorite_count） |
| **原方法** | `UserFavoriteService.removeFavorite(Long, Long)` |

**入参 RemoveFavoriteCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| productId | Long | 是 | 商品 ID |

**出参**：`void`

**异常**：无

**依赖的外部 API**：`product.api.ProductApi`（updateFavoriteCount 递减收藏计数）

---

### 5.4 checkFavorite - 检查是否已收藏

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean checkFavorite(Long userId, Long productId)` |
| **调用方** | UserFavoriteController（/favorites/check/{productId} 端点） |
| **业务语义** | 检查指定用户是否已收藏某商品 |
| **原方法** | `UserFavoriteService.isFavorited(Long, Long)` |

**入参**：

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户 ID |
| productId | Long | 商品 ID |

**出参**：`boolean`（true=已收藏）

**异常**：无

**依赖的外部 API**：无

---

### 5.5 getFavoriteCount - 获取收藏数量

| 项 | 定义 |
|---|---|
| **方法签名** | `int getFavoriteCount(Long userId)` |
| **调用方** | UserFavoriteController（/favorites/count 端点） |
| **业务语义** | 获取指定用户的收藏数量 |
| **原方法** | `UserFavoriteService.getFavoriteCount(Long)` |

**入参**：`Long userId`

**出参**：`int`（收藏数量）

**异常**：无

**依赖的外部 API**：无

---

## 6. AdminUserApi（管理员用户管理）

> 包路径：`com.seckill.mall.identity.api.AdminUserApi`
> 职责：管理员用户管理（列表/状态/角色/登录日志）
> 原 Service：`AdminUserService`（4 方法）
> 实现类：`AdminUserApplicationService`（`identity.application`）

### 6.1 listUsers - 管理员用户列表

| 项 | 定义 |
|---|---|
| **方法签名** | `PageResult<UserSummaryDTO> listUsers(UserListQuery query)` |
| **调用方** | AdminUserController（/admin/users 端点） |
| **业务语义** | 管理员查询用户列表（支持按角色/状态筛选 + 分页） |
| **原方法** | `AdminUserService.getUserList(UserListRequest)` → `PageResult<UserVO>` |

**入参 UserListQuery**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10，上限 50） |
| role | String | 否 | 角色筛选（BUYER/SELLER/ADMIN） |
| status | String | 否 | 状态筛选（ACTIVE/LOCKED/DISABLED） |
| keyword | String | 否 | 用户名/昵称关键字模糊匹配 |

**出参**：`PageResult<UserSummaryDTO>`（见 §7.4）

**异常**：`PARAM_ERROR`

**依赖的外部 API**：无

---

### 6.2 updateUserStatus - 更新用户状态

| 项 | 定义 |
|---|---|
| **方法签名** | `void updateUserStatus(UpdateUserStatusCommand command)` |
| **调用方** | AdminUserController（/admin/users/{userId}/status 端点） |
| **业务语义** | 启用/禁用/锁定用户（同步更新 security 缓存中的用户状态） |
| **原方法** | `AdminUserService.updateUserStatus(Long, UserStatus)` |

**入参 UpdateUserStatusCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| status | String | 是 | 用户状态（ACTIVE/LOCKED/DISABLED） |

**出参**：`void`

**异常**：`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：security 包（UserStatusCacheService 更新缓存）

---

### 6.3 updateUserRole - 更新用户角色

| 项 | 定义 |
|---|---|
| **方法签名** | `void updateUserRole(UpdateUserRoleCommand command)` |
| **调用方** | AdminUserController（/admin/users/{userId}/role 端点） |
| **业务语义** | 修改用户角色（BUYER/SELLER/ADMIN） |
| **原方法** | `AdminUserService.updateUserRole(Long, UserRole)` |

**入参 UpdateUserRoleCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| role | String | 是 | 用户角色（BUYER/SELLER/ADMIN） |

**出参**：`void`

**异常**：`USER_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：无

---

### 6.4 getUserLoginLogs - 用户登录日志

| 项 | 定义 |
|---|---|
| **方法签名** | `PageResult<LoginLogDTO> getUserLoginLogs(LoginLogQuery query)` |
| **调用方** | AdminUserController（/admin/users/{userId}/logs 端点） |
| **业务语义** | 查询指定用户的登录日志（分页） |
| **原方法** | `AdminUserService.getUserLoginLogs(Long, Integer, Integer)` |

**入参 LoginLogQuery**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |

**出参**：`PageResult<LoginLogDTO>`（见 §7.5）

**异常**：无

**依赖的外部 API**：无

---

## 7. DTO / Snapshot / Result 定义

### 7.1 UserSnapshot（用户快照）

> 包路径：`identity.api.dto.UserSnapshot`
> 用途：替代 `User` Entity 跨模块传递，裁剪掉 password/isDeleted 等基础设施字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户 ID |
| username | String | 用户名 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| nickname | String | 昵称 |
| avatarUrl | String | 头像 URL |
| balance | BigDecimal | 钱包余额 |
| role | String | 角色（BUYER/SELLER/ADMIN） |
| status | String | 状态（ACTIVE/LOCKED/DISABLED） |

**映射规则**：`User` → `UserSnapshot`，由 `UserConverter` 完成。`role`/`status` 枚举转为 String 名称。

---

### 7.2 AddressDTO（收货地址 DTO）

> 包路径：`identity.api.dto.AddressDTO`
> 用途：替代 `UserAddress` Entity 跨模块传递

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 地址 ID |
| userId | Long | 用户 ID |
| receiverName | String | 收件人姓名 |
| receiverPhone | String | 收件人手机号 |
| province | String | 省份 |
| city | String | 城市 |
| district | String | 区/县 |
| detailAddress | String | 详细地址 |
| isDefault | Integer | 是否默认（0/1） |

---

### 7.3 FavoriteItemDTO（收藏项 DTO）

> 包路径：`identity.api.dto.FavoriteItemDTO`
> 用途：收藏列表展示项（含商品快照信息）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 收藏记录 ID |
| userId | Long | 用户 ID |
| productId | Long | 商品 ID |
| productName | String | 商品名（来自 ProductSnapshot） |
| productMainImage | String | 商品主图（来自 ProductSnapshot） |
| productPrice | BigDecimal | 商品价格（来自 ProductSnapshot） |
| productStatus | String | 商品状态（来自 ProductSnapshot） |
| createTime | LocalDateTime | 收藏时间 |

**依赖**：通过 `product.api.ProductApi.getProductById()` 获取商品快照信息。

---

### 7.4 UserSummaryDTO（用户摘要）

> 包路径：`identity.api.dto.UserSummaryDTO`
> 用途：管理员用户列表/统计场景

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| avatarUrl | String | 头像 URL |
| balance | BigDecimal | 钱包余额 |
| role | String | 角色 |
| status | String | 状态 |
| createTime | LocalDateTime | 注册时间 |

---

### 7.5 LoginLogDTO（登录日志 DTO）

> 包路径：`identity.api.dto.LoginLogDTO`
> 用途：管理员查询用户登录日志

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 日志 ID |
| userId | Long | 用户 ID |
| username | String | 用户名 |
| ip | String | 登录 IP |
| userAgent | String | User-Agent |
| loginResult | String | 登录结果（SUCCESS/FAILURE） |
| loginTime | LocalDateTime | 登录时间 |

---

### 7.6 LoginResult（登录结果）

> 包路径：`identity.api.result.LoginResult`
> 用途：登录成功后的返回结果

| 字段 | 类型 | 说明 |
|---|---|---|
| accessToken | String | 访问令牌 |
| refreshToken | String | 刷新令牌 |
| tokenType | String | 令牌类型（"Bearer"） |
| expiresIn | Long | 过期时间（秒） |
| user | UserSnapshot | 用户信息快照 |

---

### 7.7 TokenResult（令牌结果）

> 包路径：`identity.api.result.TokenResult`
> 用途：刷新令牌后的返回结果

| 字段 | 类型 | 说明 |
|---|---|---|
| accessToken | String | 新访问令牌 |
| refreshToken | String | 新刷新令牌 |
| tokenType | String | 令牌类型（"Bearer"） |
| expiresIn | Long | 过期时间（秒） |

---

### 7.8 CaptchaResult（图形验证码结果）

> 包路径：`identity.api.result.CaptchaResult`
> 用途：图形验证码生成结果

| 字段 | 类型 | 说明 |
|---|---|---|
| captchaId | String | 验证码 ID（UUID） |
| captchaImage | String | Base64 编码的验证码图片（data:image/png;base64,...） |

---

## 8. 异常定义

### 8.1 业务异常清单

| ErrorCode | 说明 | 抛出方 |
|---|---|---|
| `USER_NOT_FOUND` | 用户不存在 | UserApi, AuthApi, AdminUserApi, AddressApi |
| `USERNAME_ALREADY_EXISTS` | 用户名已存在 | AuthApi.register |
| `PHONE_ALREADY_EXISTS` | 手机号已存在 | AuthApi.register |
| `USERNAME_OR_PASSWORD_ERROR` | 用户名或密码错误 | AuthApi.login |
| `PASSWORD_INCORRECT` | 旧密码不正确 | UserApi.changePassword |
| `USER_DISABLED` | 用户已禁用 | AuthApi.login, AuthApi.refreshToken |
| `USER_LOCKED` | 用户已锁定 | AuthApi.login |
| `CAPTCHA_INVALID` | 图形验证码无效 | AuthApi.register, AuthApi.login |
| `VERIFICATION_CODE_INVALID` | 验证码无效 | AuthApi.verifyCode, AuthApi.resetPassword |
| `VERIFICATION_CODE_RATE_LIMIT` | 验证码发送频率超限 | AuthApi.sendEmailCode, AuthApi.sendSmsCode |
| `REFRESH_TOKEN_INVALID` | 刷新令牌无效 | AuthApi.refreshToken |
| `REFRESH_TOKEN_EXPIRED` | 刷新令牌已过期 | AuthApi.refreshToken |
| `ADDRESS_NOT_FOUND` | 地址不存在 | AddressApi.updateAddress, AddressApi.deleteAddress, AddressApi.setDefaultAddress |
| `ADDRESS_NOT_BELONG_TO_USER` | 地址不属于当前用户 | AddressApi.updateAddress, AddressApi.deleteAddress, AddressApi.setDefaultAddress |
| `PRODUCT_NOT_FOUND` | 商品不存在 | FavoriteApi.addFavorite |
| `UNAUTHORIZED` | 未登录 | UserApi.getCurrentUser, UserApi.updateProfile, UserApi.changePassword, UserApi.uploadAvatar |
| `PARAM_ERROR` | 参数错误 | 所有 API |
| `FILE_TOO_LARGE` | 文件过大 | UserApi.uploadAvatar |
| `FILE_TYPE_NOT_SUPPORTED` | 文件类型不支持 | UserApi.uploadAvatar |

### 8.2 异常处理规则

1. 所有异常通过 `BusinessException(ErrorCode)` 抛出，由 `GlobalExceptionHandler` 统一捕获返回 `Result.error(ErrorCode)`。
2. **不泄露堆栈**：异常消息用业务语言，不包含 SQL/类名/行号。
3. **不泄露用户存在性**：登录失败统一返回 `USERNAME_OR_PASSWORD_ERROR`，不区分用户不存在与密码错误。

---

## 9. 调用方标注汇总

### 9.1 跨模块调用方矩阵

| API 方法 | order | payment | coupon | seckill | review | stats | system | ai | analytics | Controller |
|---|---|---|---|---|---|---|---|---|---|---|
| `UserApi.getUserById` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | | | | |
| `UserApi.getCurrentUser` | | | | | | | | | | | AuthController |
| `UserApi.updateProfile` | | | | | | | | | | | AuthController |
| `UserApi.changePassword` | | | | | | | | | | | AuthController |
| `UserApi.updateUserPhone` | | | | | | | | | | | UserController |
| `UserApi.updateUserEmail` | | | | | | | | | | | UserController |
| `UserApi.getUserEmail` | ✅ | | | | | | | | | |
| `UserApi.getUserDisplayNamesByIds` | | | | | ✅ | ✅ | ✅ | | | |
| `UserApi.getUsernamesByIds` | | | | | | ✅ | ✅ | | | |
| `UserApi.countAll` | | | | | | ✅ | | | | |
| `UserApi.countTodayRegistered` | | | | | | ✅ | | | | |
| `UserApi.selectUserTrend` | | | | | | ✅ | | | | |
| `UserApi.addBalance` | | ✅ | | | | | | | | |
| `UserApi.deductBalance` | | ✅ | | | | | | | | |
| `UserApi.uploadAvatar` | | | | | | | | | | | AuthController |
| `AuthApi.register` | | | | | | | | | | | AuthController |
| `AuthApi.login` | | | | | | | | | | | AuthController |
| `AuthApi.logout` | | | | | | | | | | | AuthController |
| `AuthApi.refreshToken` | | | | | | | | | | | AuthController |
| `AuthApi.sendForgotPasswordCode` | | | | | | | | | | | AuthController |
| `AuthApi.resetPassword` | | | | | | | | | | | AuthController |
| `AuthApi.generateCaptcha` | | | | | | | | | | | AuthController |
| `AuthApi.verifyCaptcha` | | | | | | | | | | | （内部） |
| `AuthApi.sendEmailCode` | | | | | | | | | | | VerificationCodeController |
| `AuthApi.sendSmsCode` | | | | | | | | | | | VerificationCodeController |
| `AuthApi.verifyCode` | | | | | | | | | | | VerificationCodeController, UserController |
| `AddressApi.listAddresses` | | | | | | | | | | | UserAddressController |
| `AddressApi.getAddressById` | ✅ | | | | | | | | | |
| `AddressApi.saveAddress` | | | | | | | | | | | UserAddressController |
| `AddressApi.updateAddress` | | | | | | | | | | | UserAddressController |
| `AddressApi.deleteAddress` | | | | | | | | | | | UserAddressController |
| `AddressApi.setDefaultAddress` | | | | | | | | | | | UserAddressController |
| `FavoriteApi.listFavorites` | | | | | | | | | | | UserFavoriteController |
| `FavoriteApi.addFavorite` | | | | | | | | | | | UserFavoriteController |
| `FavoriteApi.removeFavorite` | | | | | | | | | | | UserFavoriteController |
| `FavoriteApi.checkFavorite` | | | | | | | | | | | UserFavoriteController |
| `FavoriteApi.getFavoriteCount` | | | | | | | | | | | UserFavoriteController |
| `AdminUserApi.listUsers` | | | | | | | | | | | AdminUserController |
| `AdminUserApi.updateUserStatus` | | | | | | | | | | | AdminUserController |
| `AdminUserApi.updateUserRole` | | | | | | | | | | | AdminUserController |
| `AdminUserApi.getUserLoginLogs` | | | | | | | | | | | AdminUserController |

### 9.2 SecurityUtils → CurrentUserContext 替换矩阵

| 当前调用方 | 当前调用 | 目标调用 | 说明 |
|---|---|---|---|
| AuthController | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext.getCurrentUserId()` | Controller 层切换 |
| UserController | `SecurityUtils.getCurrentUserId()` / `getCurrentEmail()` | `shared.kernel.CurrentUserContext` | 同上 |
| UserAddressController | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | 同上 |
| UserFavoriteController | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | 同上 |
| VerificationCodeController | `SecurityUtils.getCurrentEmail()` | `shared.kernel.CurrentUserContext` | 同上 |
| seckill 模块 | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | I.4-C 切换 |
| ai 模块 | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | I.4-C 切换 |
| analytics 模块 | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | I.4-C 切换 |
| AuthApplicationService（内部） | `SecurityUtils.getCurrentUserId()` | 保留 SecurityUtils 引用 | application 层可引用 security 包 |

> **说明**：Controller 层和外部业务模块逐步将 `SecurityUtils` 替换为 `shared.kernel.CurrentUserContext`。`AuthApplicationService` 等内部 ApplicationService 可保留对 security 包的直接引用（认证逻辑需要）。

---

## 10. 与 Product API 契约的差异对比

| 维度 | Product API | Identity API | 说明 |
|---|---|---|---|
| API 接口数 | 5（ProductApi/SkuApi/ReviewApi/AttributeApi/InventoryApi） | 5（UserApi/AuthApi/AddressApi/FavoriteApi/AdminUserApi） | 数量相同 |
| API 方法总数 | 32 | 40 | Identity 方法更多（含认证/验证码/余额/统计） |
| Snapshot/DTO 数 | 5 | 7 | Identity 多 LoginResultDTO/TokenResultDTO/CaptchaResultDTO |
| Command 数 | 11 | 16 | Identity 多认证/地址/收藏/管理员相关 Command |
| Query 数 | 2 | 2 | 相同 |
| Result 数 | 2 | 3 | Identity 多 CaptchaResult |
| 跨模块调用方数 | 9 | 9 | 相同（但 identity 被依赖更基础） |
| Entity 泄露方法 | 3（getProductById/getProductsByIds/getByIdEnabled） | 2（getUserById/getAddressById） | Identity 泄露更少 |
| 特殊约束 | 无 | security 包保持原位 + SecurityUtils 全项目引用 | Identity 独有 |

**结论**：Identity API 契约方法数更多（认证/验证码/余额/统计能力丰富），但 Entity 泄露点更少（仅 2 个方法返回 Entity）。最大的特殊性在于 security 包的横切约束和 SecurityUtils 的全项目引用，需通过 `shared.kernel.CurrentUserContext` 逐步替换。

---

> **本文件为 Phase 4 Identity 模块迁移前置设计文档，未创建任何代码/接口。**
> **前置文档**：`IDENTITY-MIGRATION-PLAN.md`（迁移计划）
> **下一步**：按 Phase I.0 → I.6 逐步执行迁移，参考本契约创建 API 接口和 DTO。