# Identity 模块迁移计划 (IDENTITY-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase 4 - Identity 模块迁移（Strangler Pattern 第三阶段）
> 依据：ORDER-MIGRATION-PLAN.md + PRODUCT-MIGRATION-PLAN.md（前两模块迁移经验）+ Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Identity 当前结构

### 1.1 当前包结构

Identity 相关代码散落在 6 个不同的顶层包中，共 **33 个核心文件**（不含 DTO/VO/security）：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `AuthController` | `controller` | 注册/登录/登出/Token 刷新/忘记密码/图形验证码（/api/v1/auth） |
| `UserController` | `controller` | 修改手机号/邮箱（需验证码校验）（/api/v1/users） |
| `UserAddressController` | `controller` | 收货地址 CRUD + 设置默认（/api/v1/addresses） |
| `UserFavoriteController` | `controller` | 收藏/取消/列表/检查/数量（/api/v1/favorites） |
| `AdminUserController` | `controller` | 后台用户列表/启禁用/改角色/登录日志（/api/v1/admin/users） |
| `VerificationCodeController` | `controller` | 邮箱/短信验证码发送与校验（/api/v1/verification） |
| `AuthService` | `service` | 认证服务接口（注册/登录/登出/刷新/改密/资料/头像/找回密码） |
| `UserService` | `service` | 用户资料服务接口（改手机/邮箱/查邮箱/查用户/统计/余额） |
| `UserAddressService` | `service` | 收货地址服务接口 |
| `UserFavoriteService` | `service` | 用户收藏服务接口 |
| `AdminUserService` | `service` | 管理员用户管理接口 |
| `VerificationCodeService` | `service` | 验证码服务接口 |
| `CaptchaService` | `service` | 图形验证码具体类（直接 @Service） |
| `AuthServiceImpl` | `service.impl` | AuthService 实现 |
| `UserServiceImpl` | `service.impl` | UserService 实现 |
| `UserAddressServiceImpl` | `service.impl` | UserAddressService 实现 |
| `UserFavoriteServiceImpl` | `service.impl` | UserFavoriteService 实现（已注入 ProductApi） |
| `AdminUserServiceImpl` | `service.impl` | AdminUserService 实现 |
| `VerificationCodeServiceImpl` | `service.impl` | VerificationCodeService 实现 |
| `User` | `entity` | 用户 PO（t_user） |
| `UserAddress` | `entity` | 收货地址 PO（t_user_address） |
| `UserFavorite` | `entity` | 用户收藏 PO（t_user_favorite） |
| `LoginLog` | `entity` | 登录日志 PO（t_login_log） |
| `UserStatus` | `entity.enums` | 用户状态枚举（ACTIVE/LOCKED/DISABLED） |
| `UserRole` | `entity.enums` | 用户角色枚举（BUYER/SELLER/ADMIN） |
| `LoginResult` | `entity.enums` | 登录结果枚举 |
| `UserMapper` | `mapper` | 用户 Mapper |
| `UserAddressMapper` | `mapper` | 收货地址 Mapper |
| `UserFavoriteMapper` | `mapper` | 用户收藏 Mapper |
| `LoginLogMapper` | `mapper` | 登录日志 Mapper |
| `UserConverter` | `converter` | User ↔ UserVO 转换器 |

> **security 包（12 类，保持原位不迁移）**：`SecurityUtils`、`JwtUtils`、`JwtAuthenticationFilter`、`JwtAuthenticationEntryPoint`、`JwtAccessDeniedHandler`、`ReplayProtectionFilter`、`SecurityUserDetails`、`SecurityUserDetailsService`、`TokenVersionService`、`TokenBlacklistService`、`UserStatusCacheService`、`RsaKeyProvider`。这些是横切基础设施，被全项目引用，保持原位 `com.seckill.mall.security`。

### 1.2 Service 方法签名

#### AuthService（11 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `register` | `UserVO register(RegisterRequest req)` | 用户注册 | ✅ |
| `login` | `LoginVO login(LoginRequest req, String ip, HttpServletRequest request)` | 用户登录 | ✅ |
| `logout` | `void logout(String accessToken)` | 退出登录 | ✅ |
| `refresh` | `TokenVO refresh(RefreshTokenRequest req)` | 刷新令牌 | ✅ |
| `getMe` | `UserVO getMe()` | 获取当前用户 | ✅ |
| `changePassword` | `void changePassword(ChangePasswordRequest req)` | 修改密码 | ✅ |
| `updateProfile` | `UserVO updateProfile(ProfileUpdateRequest req)` | 更新资料 | ✅ |
| `uploadAvatar` | `Map<String,String> uploadAvatar(MultipartFile file)` | 上传头像 | ✅ 依赖 upload |
| `sendForgotPasswordCode` | `void sendForgotPasswordCode(ForgotPasswordSendRequest req)` | 找回密码-发码 | ✅ |
| `resetPassword` | `void resetPassword(ForgotPasswordResetRequest req)` | 找回密码-重置 | ✅ |

#### UserService（13 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `updatePhone` | `UserVO updatePhone(Long userId, String phone)` | 修改手机号 | ✅ |
| `updateEmail` | `UserVO updateEmail(Long userId, String email)` | 修改邮箱 | ✅ |
| `getEmail` | `String getEmail(Long userId)` | 查邮箱（通知用） | ✅ 跨模块只读 |
| `getUserById` | `User getUserById(Long userId)` | 查用户实体 | ⚠️ 返回 Entity（跨模块只读访问） |
| `getUserDisplayNamesByIds` | `Map<Long,String> getUserDisplayNamesByIds(List<Long>)` | 批量查显示名 | ✅ |
| `getUsernamesByIds` | `Map<Long,String> getUsernamesByIds(List<Long>)` | 批量查用户名 | ✅ |
| `countAll` | `long countAll()` | 用户总数 | ✅ 封装 Mapper |
| `countTodayRegistered` | `Long countTodayRegistered(LocalDate today)` | 今日注册数 | ✅ 被 stats 调用 |
| `selectUserTrend` | `List<Map<String,Object>> selectUserTrend(LocalDate, LocalDate)` | 注册趋势 | ✅ 被 stats 调用 |
| `addBalance` | `void addBalance(Long userId, BigDecimal amount)` | 余额增加 | ✅ 被 payment 调用 |
| `deductBalance` | `int deductBalance(Long userId, BigDecimal amount)` | 余额扣减 | ✅ 被 payment 调用 |
| `selectUserPage` | `IPage<User> selectUserPage(IPage<User>, Wrapper<User>)` | 分页查用户 | ⚠️ 暴露 MyBatis-Plus 类型（内部用） |
| `updateUserById` | `int updateUserById(User user)` | 更新用户 | ⚠️ 暴露 Entity（内部用） |

#### UserAddressService（6 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `listByUserId` | `List<UserAddressVO> listByUserId(Long userId)` | 地址列表 | ✅ |
| `create` | `UserAddressVO create(Long userId, UserAddressVO vo)` | 新增地址 | ✅ |
| `update` | `UserAddressVO update(Long userId, Long id, UserAddressVO vo)` | 编辑地址 | ✅ |
| `delete` | `void delete(Long userId, Long id)` | 删除地址 | ✅ |
| `setDefault` | `void setDefault(Long userId, Long id)` | 设默认地址 | ✅ |
| `getAddressById` | `UserAddress getAddressById(Long addressId)` | 查地址实体 | ⚠️ 返回 Entity（跨模块只读，被 order 调用） |

#### UserFavoriteService（5 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `getFavoriteList` | `Result<List<FavoriteItemVO>> getFavoriteList(Long userId)` | 收藏列表 | ✅ 已注入 ProductApi |
| `addFavorite` | `Result<Void> addFavorite(Long userId, Long productId)` | 添加收藏 | ✅ |
| `removeFavorite` | `Result<Void> removeFavorite(Long userId, Long productId)` | 取消收藏 | ✅ |
| `isFavorited` | `Result<Boolean> isFavorited(Long userId, Long productId)` | 检查收藏 | ✅ |
| `getFavoriteCount` | `Result<Integer> getFavoriteCount(Long userId)` | 收藏数量 | ✅ |

#### AdminUserService（4 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `getUserList` | `PageResult<UserVO> getUserList(UserListRequest req)` | 用户列表 | ✅ |
| `updateUserStatus` | `void updateUserStatus(Long userId, UserStatus status)` | 改状态 | ✅ |
| `updateUserRole` | `void updateUserRole(Long userId, UserRole role)` | 改角色 | ✅ |
| `getUserLoginLogs` | `PageResult<LoginLogVO> getUserLoginLogs(Long, Integer, Integer)` | 登录日志 | ✅ |

#### VerificationCodeService（3 个方法）

| 方法 | 签名 | 性质 |
|---|---|---|
| `sendEmailCode` | `void sendEmailCode(String email)` | 发邮箱验证码 |
| `sendSmsCode` | `void sendSmsCode(String phone)` | 发短信验证码 |
| `verifyCode` | `boolean verifyCode(String target, String code)` | 校验验证码 |

#### CaptchaService（2 个方法，具体类）

| 方法 | 签名 | 性质 |
|---|---|---|
| `generateCaptcha` | `CaptchaVO generateCaptcha()` | 生成图形验证码 |
| `verifyCaptcha` | `boolean verifyCaptcha(String captchaId, String captchaCode)` | 校验图形验证码 |

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `AuthController` | `/api/v1/auth` | `POST /register` | 用户注册 | 公开 |
| | | `POST /login` | 用户登录 | 公开 |
| | | `POST /logout` | 退出登录 | 登录 |
| | | `GET /me` | 当前用户信息 | 登录 |
| | | `PUT /password` | 修改密码 | 登录 |
| | | `PUT /profile` | 更新资料 | 登录 |
| | | `POST /avatar` | 上传头像 | 登录 |
| | | `POST /refresh` | 刷新令牌 | 公开 |
| | | `GET /captcha` | 图形验证码 | 公开 |
| | | `POST /forgot-password/send-code` | 找回密码-发码 | 公开 |
| | | `POST /forgot-password/reset` | 找回密码-重置 | 公开 |
| `UserController` | `/api/v1/users` | `PUT /profile/phone` | 修改手机号 | BUYER/ADMIN |
| | | `PUT /profile/email` | 修改邮箱 | BUYER/ADMIN |
| `UserAddressController` | `/api/v1/addresses` | `GET /list` | 地址列表 | BUYER/ADMIN |
| | | `POST /create` | 新增地址 | BUYER/ADMIN |
| | | `PUT /{id}` | 编辑地址 | BUYER/ADMIN |
| | | `DELETE /{id}` | 删除地址 | BUYER/ADMIN |
| | | `PUT /{id}/default` | 设默认地址 | BUYER/ADMIN |
| `UserFavoriteController` | `/api/v1/favorites` | `GET /list` | 收藏列表 | BUYER/SELLER/ADMIN |
| | | `POST /add` | 添加收藏 | BUYER/SELLER/ADMIN |
| | | `DELETE /{productId}` | 取消收藏 | BUYER/SELLER/ADMIN |
| | | `GET /check/{productId}` | 检查收藏 | BUYER/SELLER/ADMIN |
| | | `GET /count` | 收藏数量 | BUYER/SELLER/ADMIN |
| `AdminUserController` | `/api/v1/admin/users` | `GET /` | 用户列表 | ADMIN |
| | | `PUT /{userId}/status` | 改状态 | ADMIN |
| | | `PUT /{userId}/role` | 改角色 | ADMIN |
| | | `GET /{userId}/logs` | 登录日志 | ADMIN |
| `VerificationCodeController` | `/api/v1/verification` | `POST /send-email` | 发邮箱码 | 公开 |
| | | `POST /send-sms` | 发短信码 | 公开 |
| | | `POST /verify` | 校验验证码 | 公开 |

### 1.4 Entity 字段

#### User（t_user，13 字段）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键（ASSIGN_ID） |
| `username` | String | 用户名 |
| `password` | String | 密码哈希（@JsonIgnore） |
| `phone` | String | 手机号 |
| `email` | String | 邮箱 |
| `nickname` | String | 昵称 |
| `avatarUrl` | String | 头像 URL |
| `balance` | BigDecimal | 钱包余额 |
| `role` | UserRole | 角色枚举 |
| `status` | UserStatus | 状态枚举 |
| `isDeleted` | Integer | 逻辑删除 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### UserAddress（t_user_address，11 字段）

`id, userId, receiverName, receiverPhone, province, city, district, detailAddress, isDefault, isDeleted, createTime, updateTime`

#### UserFavorite（t_user_favorite，6 字段）

`id, userId, productId, isDeleted, createTime, updateTime`

#### LoginLog（t_login_log）

登录日志记录（含 userId, ip, userAgent, loginResult, loginTime 等）

### 1.5 当前问题

#### 跨模块 Entity 引用（外部模块引用 Identity Entity）

| 被引用的 Entity | 引用方 | 所属模块 | 问题 |
|---|---|---|---|
| `User` | OrderServiceImpl, OrderQueryServiceImpl | order | order 直接引用 identity Entity |
| `User` | WalletServiceImpl, RechargeCardServiceImpl | payment | payment 直接引用 identity Entity |
| `User` | CouponServiceImpl | coupon | coupon 直接引用 identity Entity |
| `User` | SeckillOrderServiceImpl, SeckillGoodsServiceImpl | seckill | seckill 直接引用 identity Entity |
| `User` | ProductReviewServiceImpl | review | review 直接引用 identity Entity |
| `User` | StatsServiceImpl | stats | stats 直接引用 identity Entity |
| `User` | SecurityUserDetailsService, SecurityUserDetails, TokenVersionService, UserStatusCacheService | security（横切） | security 引用 User Entity（保持原位，需更新 import） |
| `UserAddress` | OrderServiceImpl, OrderQueryServiceImpl | order | order 直接引用 identity Entity |

**跨模块 Entity 引用：2 类 Entity（User/UserAddress）被 7 个外部业务模块 + security 包引用**

#### Service 暴露 Entity 给外部

| Service 方法 | 返回类型 | 调用方 | 问题 |
|---|---|---|---|
| `UserService.getUserById(Long)` | `User`（Entity） | order, payment, coupon, seckill, review, stats | ⚠️ API 返回 Entity |
| `UserAddressService.getAddressById(Long)` | `UserAddress`（Entity） | order | ⚠️ API 返回 Entity |
| `UserService.selectUserPage(IPage, Wrapper)` | `IPage<User>` | AdminUserServiceImpl（内部） | ⚠️ 暴露 MyBatis-Plus 类型 |
| `UserService.updateUserById(User)` | `int` | AdminUserServiceImpl（内部） | ⚠️ 暴露 Entity |

#### security 包对 User Entity 的引用（特殊约束）

| security 类 | 引用的 Entity | 迁移处理 |
|---|---|---|
| `SecurityUserDetailsService` | `User` | User 移动后更新 import |
| `SecurityUserDetails` | `User` | User 移动后更新 import |
| `TokenVersionService` | `User` | User 移动后更新 import |
| `UserStatusCacheService` | `User` | User 移动后更新 import |
| `SecurityUtils` | `User`（可能） | User 移动后更新 import |

> **关键约束**：security 包 12 个类**保持原位**（`com.seckill.mall.security`），不迁移到 identity 包。但它们引用的 `User` Entity 会迁移到 `identity.infrastructure.entity`，因此需同步更新 security 包中所有引用 User 的 import 路径。

#### SecurityUtils 被全项目使用

`SecurityUtils` 被几乎所有模块的 Controller/Service 引用（获取当前用户 ID/角色/邮箱等）。**保持原位不迁移**，但目标架构中应逐步引导外部模块改用 `shared.kernel.CurrentUserContext`。

---

## 2. Identity 目标结构

### 2.1 目标包结构

```
com.seckill.mall.identity
├── api/                          # Public API（接口 + DTO + Command + Query + Result）
│   ├── UserApi.java              # 用户业务能力（资料/密码/手机邮箱/余额/统计）
│   ├── AuthApi.java              # 认证能力（注册/登录/登出/刷新/找回密码/验证码/图形验证码）
│   ├── AddressApi.java           # 收货地址能力（CRUD + 默认地址）
│   ├── FavoriteApi.java          # 用户收藏能力（列表/添加/移除/检查/数量）
│   ├── AdminUserApi.java         # 管理员用户管理（列表/状态/角色/日志）
│   ├── dto/                      # 对外数据契约（Snapshot/DTO）
│   │   ├── UserSnapshot.java         # 用户快照（替代 User Entity 跨模块传递）
│   │   ├── AddressDTO.java           # 收货地址 DTO（替代 UserAddress Entity）
│   │   ├── FavoriteItemDTO.java      # 收藏项 DTO
│   │   ├── LoginResultDTO.java       # 登录结果 DTO
│   │   ├── TokenResultDTO.java       # 令牌结果 DTO
│   │   ├── CaptchaResultDTO.java     # 图形验证码结果 DTO
│   │   ├── LoginLogDTO.java          # 登录日志 DTO
│   │   └── UserSummaryDTO.java       # 用户摘要（列表/统计用）
│   ├── command/                  # 入参 Command
│   │   ├── RegisterCommand.java
│   │   ├── LoginCommand.java
│   │   ├── RefreshTokenCommand.java
│   │   ├── ChangePasswordCommand.java
│   │   ├── UpdateProfileCommand.java
│   │   ├── UpdatePhoneCommand.java
│   │   ├── UpdateEmailCommand.java
│   │   ├── UploadAvatarCommand.java
│   │   ├── SendCodeCommand.java
│   │   ├── ResetPasswordCommand.java
│   │   ├── SaveAddressCommand.java
│   │   ├── UpdateAddressCommand.java
│   │   ├── AddFavoriteCommand.java
│   │   ├── RemoveFavoriteCommand.java
│   │   ├── UpdateUserStatusCommand.java
│   │   └── UpdateUserRoleCommand.java
│   ├── query/                    # 查询入参 Query
│   │   ├── UserListQuery.java
│   │   └── LoginLogQuery.java
│   └── result/                   # 出参 Result
│       ├── LoginResult.java          # 登录出参（含 token + 用户快照）
│       ├── TokenResult.java          # 刷新令牌出参
│       └── CaptchaResult.java        # 图形验证码出参
├── application/                  # 应用服务（用例编排，实现 api 接口）
│   ├── UserApplicationService.java       # 实现 UserApi
│   ├── AuthApplicationService.java       # 实现 AuthApi
│   ├── AddressApplicationService.java    # 实现 AddressApi
│   ├── FavoriteApplicationService.java   # 实现 FavoriteApi
│   ├── AdminUserApplicationService.java  # 实现 AdminUserApi
│   ├── VerificationCodeApplicationService.java  # 验证码（内部，AuthApi 委托）
│   ├── CaptchaApplicationService.java    # 图形验证码（内部，AuthApi 委托）
│   └── facade/                    # 转换器门面
│       └── UserConverter.java         # User ↔ UserSnapshot/UserVO 转换
├── domain/                       # 领域模型
│   ├── UserStatus.java               # 用户状态枚举
│   ├── UserRole.java                 # 用户角色枚举
│   └── LoginResultType.java          # 登录结果类型枚举
├── infrastructure/               # 持久化
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── UserAddressMapper.java
│   │   ├── UserFavoriteMapper.java
│   │   └── LoginLogMapper.java
│   ├── entity/                   # PO（模块内部可见）
│   │   ├── User.java
│   │   ├── UserAddress.java
│   │   ├── UserFavorite.java
│   │   └── LoginLog.java
│   └── repository/               # Repository（可选，封装 Mapper）
└── interfaces/                   # Controller
    ├── AuthController.java
    ├── UserController.java
    ├── UserAddressController.java
    ├── UserFavoriteController.java
    ├── AdminUserController.java
    ├── VerificationCodeController.java
    └── vo/                       # 面向前端的 VO
        ├── UserVO.java
        ├── UserAddressVO.java
        ├── FavoriteItemVO.java
        ├── LoginVO.java
        ├── TokenVO.java
        ├── CaptchaVO.java
        └── LoginLogVO.java
```

> **不在 identity 包内的文件（保持原位）**：
> - `com.seckill.mall.security.*`（12 个安全基础设施类）— 横切基础设施，全项目引用
> - `com.seckill.mall.shared.kernel.CurrentUserContext` — 当前用户上下文（已在 shared.kernel）

### 2.2 目标依赖规则

| 层 | 允许依赖 | 禁止依赖 |
|---|---|---|
| `identity.api` | shared.kernel（仅常量/枚举）、JDK | 任何模块的 infrastructure/mapper/domain/entity |
| `identity.application` | identity.api、identity.domain、identity.infrastructure（自身）、其他模块的 **api**（product.api、upload.api）、shared.kernel、security（横切，仅限认证相关 ApplicationService） | 其他模块的 infrastructure/mapper/domain/entity |
| `identity.infrastructure` | identity.domain（自身）、MyBatis-Plus、shared.kernel | 任何业务模块 |
| `identity.interfaces` | identity.application、identity.api.dto/command/query、identity.interfaces.vo、shared.kernel（CurrentUserContext）、security（SecurityUtils，过渡期） | identity.infrastructure（Mapper）、其他模块的 Service |
| `security`（保持原位） | identity.infrastructure.entity（**仅 User**，通过 import 引用）、shared.kernel、Spring Security | identity.application、identity.api（避免环依赖） |

> **特殊说明**：security 包引用 `identity.infrastructure.entity.User` 是单向依赖（security → identity.infrastructure.entity），不构成环。identity.application 中的 AuthApplicationService 可引用 security 包的 JwtUtils/TokenVersionService 等完成认证逻辑，这是允许的（application → security 横切基础设施）。

### 2.3 API 边界设计

#### 对外暴露的 API

| API 接口 | 方法数 | 暴露给 | 说明 |
|---|---|---|---|
| `UserApi` | 14 | UserController, order, payment, coupon, seckill, review, stats | 用户资料 + 余额 + 统计 + 跨模块只读快照 |
| `AuthApi` | 11 | AuthController, VerificationCodeController | 认证 + 验证码 + 图形验证码 + 找回密码 |
| `AddressApi` | 6 | UserAddressController, order | 收货地址 CRUD + 默认地址 |
| `FavoriteApi` | 5 | UserFavoriteController | 收藏管理 |
| `AdminUserApi` | 4 | AdminUserController | 管理员用户管理 |

#### 对内隐藏的实现

| 隐藏项 | 类型 | 说明 |
|---|---|---|
| `User` / `UserAddress` / `UserFavorite` / `LoginLog` | Entity/PO | 仅 identity.infrastructure 内部可见 |
| 4 个 Mapper | Mapper | 仅 identity.infrastructure 内部可见 |
| 5 个 ApplicationService | ApplicationService | 实现细节，不对外暴露 |
| `VerificationCodeApplicationService` / `CaptchaApplicationService` | ApplicationService | 内部实现，AuthApi 委托调用 |
| `AuthServiceImpl` / `UserServiceImpl` / `UserAddressServiceImpl` / `UserFavoriteServiceImpl` / `AdminUserServiceImpl` / `VerificationCodeServiceImpl` / `CaptchaService` | （合并删除） | 逻辑并入 ApplicationService |
| security 包 12 个类 | 横切基础设施 | 保持原位，不对外模块暴露（仅 identity.application 可引用） |

#### Entity → Snapshot 映射（消除跨模块 Entity 引用）

| 原 Entity 返回 | 新 Snapshot/DTO | 字段裁剪 |
|---|---|---|
| `User` | `UserSnapshot` | id, username, phone, email, nickname, avatarUrl, balance, role, status（按需裁剪，不暴露 password/isDeleted/createTime/updateTime） |
| `UserAddress` | `AddressDTO` | id, userId, receiverName, receiverPhone, province, city, district, detailAddress, isDefault（不暴露 isDeleted/createTime/updateTime） |
| `List<User>` | `List<UserSnapshot>` | 同上 |

---

## 3. 文件迁移路径

### 3.1 迁移映射表

| 当前路径 | 目标路径 | 迁移类型 | 说明 |
|---|---|---|---|
| `controller/AuthController.java` | `identity/interfaces/AuthController.java` | 改造 | 改为依赖 AuthApplicationService |
| `controller/UserController.java` | `identity/interfaces/UserController.java` | 改造 | 改为依赖 UserApplicationService + VerificationCodeApplicationService |
| `controller/UserAddressController.java` | `identity/interfaces/UserAddressController.java` | 改造 | 改为依赖 AddressApplicationService；SecurityUtils → CurrentUserContext |
| `controller/UserFavoriteController.java` | `identity/interfaces/UserFavoriteController.java` | 改造 | 改为依赖 FavoriteApplicationService；SecurityUtils → CurrentUserContext |
| `controller/AdminUserController.java` | `identity/interfaces/AdminUserController.java` | 改造 | 改为依赖 AdminUserApplicationService |
| `controller/VerificationCodeController.java` | `identity/interfaces/VerificationCodeController.java` | 改造 | 改为依赖 VerificationCodeApplicationService；SecurityUtils → CurrentUserContext |
| `service/AuthService.java` | `identity/api/AuthApi.java` | 改造 | 接口转为 API |
| `service/UserService.java` | `identity/api/UserApi.java` | 改造 | 接口转为 API，getUserById 返回 UserSnapshot，selectUserPage/updateUserById 内部化 |
| `service/UserAddressService.java` | `identity/api/AddressApi.java` | 改造 | 接口转为 API，getAddressById 返回 AddressDTO |
| `service/UserFavoriteService.java` | `identity/api/FavoriteApi.java` | 改造 | 接口转为 API |
| `service/AdminUserService.java` | `identity/api/AdminUserApi.java` | 改造 | 接口转为 API |
| `service/VerificationCodeService.java` | （内部化到 `identity/application/VerificationCodeApplicationService.java`） | 合并 | 验证码为内部能力，不独立暴露 API |
| `service/CaptchaService.java` | （内部化到 `identity/application/CaptchaApplicationService.java`） | 合并 | 图形验证码为内部能力，通过 AuthApi 暴露 |
| `service/impl/AuthServiceImpl.java` | `identity/application/AuthApplicationService.java` | 改造 | 改为依赖外部 api，实现 AuthApi |
| `service/impl/UserServiceImpl.java` | `identity/application/UserApplicationService.java` | 改造 | 实现 UserApi；getUserById 返回 UserSnapshot |
| `service/impl/UserAddressServiceImpl.java` | `identity/application/AddressApplicationService.java` | 改造 | 实现 AddressApi；getAddressById 返回 AddressDTO |
| `service/impl/UserFavoriteServiceImpl.java` | `identity/application/FavoriteApplicationService.java` | 改造 | 实现 FavoriteApi；ProductApi 引用不变 |
| `service/impl/AdminUserServiceImpl.java` | `identity/application/AdminUserApplicationService.java` | 改造 | 实现 AdminUserApi；selectUserPage/updateUserById 内部化 |
| `service/impl/VerificationCodeServiceImpl.java` | `identity/application/VerificationCodeApplicationService.java` | 改造 | 内部实现 |
| `entity/User.java` | `identity/infrastructure/entity/User.java` | 移动 | PO 归入 infrastructure；**security 包需同步更新 import** |
| `entity/UserAddress.java` | `identity/infrastructure/entity/UserAddress.java` | 移动 | 同上 |
| `entity/UserFavorite.java` | `identity/infrastructure/entity/UserFavorite.java` | 移动 | 同上 |
| `entity/LoginLog.java` | `identity/infrastructure/entity/LoginLog.java` | 移动 | 同上 |
| `entity/enums/UserStatus.java` | `identity/domain/UserStatus.java` | 移动 | 枚举提升到 domain |
| `entity/enums/UserRole.java` | `identity/domain/UserRole.java` | 移动 | 枚举提升到 domain |
| `entity/enums/LoginResult.java` | `identity/domain/LoginResultType.java` | 移动 | 枚举提升到 domain |
| `mapper/UserMapper.java` | `identity/infrastructure/mapper/UserMapper.java` | 移动 | Mapper 归入 infrastructure |
| `mapper/UserAddressMapper.java` | `identity/infrastructure/mapper/UserAddressMapper.java` | 移动 | 同上 |
| `mapper/UserFavoriteMapper.java` | `identity/infrastructure/mapper/UserFavoriteMapper.java` | 移动 | 同上 |
| `mapper/LoginLogMapper.java` | `identity/infrastructure/mapper/LoginLogMapper.java` | 移动 | 同上 |
| `converter/UserConverter.java` | `identity/application/facade/UserConverter.java` | 移动 | 转换器归入 application.facade |
| `dto/LoginRequest.java` | `identity/api/command/LoginCommand.java` | 改造 | Request → Command |
| `dto/RegisterRequest.java` | `identity/api/command/RegisterCommand.java` | 改造 | Request → Command |
| `dto/RefreshTokenRequest.java` | `identity/api/command/RefreshTokenCommand.java` | 改造 | Request → Command |
| `dto/ChangePasswordRequest.java` | `identity/api/command/ChangePasswordCommand.java` | 改造 | Request → Command |
| `dto/ProfileUpdateRequest.java` | `identity/api/command/UpdateProfileCommand.java` | 改造 | Request → Command |
| `dto/PhoneUpdateRequest.java` | `identity/api/command/UpdatePhoneCommand.java` | 改造 | Request → Command |
| `dto/EmailUpdateRequest.java` | `identity/api/command/UpdateEmailCommand.java` | 改造 | Request → Command |
| `dto/ForgotPasswordSendRequest.java` | `identity/api/command/SendCodeCommand.java` | 改造 | Request → Command |
| `dto/ForgotPasswordResetRequest.java` | `identity/api/command/ResetPasswordCommand.java` | 改造 | Request → Command |
| `dto/UserListRequest.java` | `identity/api/query/UserListQuery.java` | 改造 | Request → Query |
| `dto/UserStatusUpdateRequest.java` | `identity/api/command/UpdateUserStatusCommand.java` | 改造 | Request → Command |
| `dto/UserRoleUpdateRequest.java` | `identity/api/command/UpdateUserRoleCommand.java` | 改造 | Request → Command |
| `vo/UserVO.java` | `identity/interfaces/vo/UserVO.java` | 移动 | 前端 VO 归 interfaces |
| `vo/UserAddressVO.java` | `identity/interfaces/vo/UserAddressVO.java` | 移动 | 同上 |
| `vo/FavoriteItemVO.java` | `identity/interfaces/vo/FavoriteItemVO.java` | 移动 | 同上 |
| `vo/LoginVO.java` | `identity/interfaces/vo/LoginVO.java` | 移动 | 同上 |
| `vo/TokenVO.java` | `identity/interfaces/vo/TokenVO.java` | 移动 | 同上 |
| `vo/CaptchaVO.java` | `identity/interfaces/vo/CaptchaVO.java` | 移动 | 同上 |
| `vo/LoginLogVO.java` | `identity/interfaces/vo/LoginLogVO.java` | 移动 | 同上 |

> **保持原位不迁移的文件**：
> - `security/*`（12 个安全基础设施类）— 保持 `com.seckill.mall.security` 包
> - `shared.kernel.CurrentUserContext` — 已在 shared.kernel

### 3.2 新增文件清单

| 新增文件 | 包路径 | 说明 |
|---|---|---|
| `UserApi.java` | `identity.api` | 用户业务能力 API 接口 |
| `AuthApi.java` | `identity.api` | 认证能力 API 接口 |
| `AddressApi.java` | `identity.api` | 收货地址能力 API 接口 |
| `FavoriteApi.java` | `identity.api` | 用户收藏能力 API 接口 |
| `AdminUserApi.java` | `identity.api` | 管理员用户管理 API 接口 |
| `UserSnapshot.java` | `identity.api.dto` | 用户快照（替代 User Entity 跨模块传递） |
| `AddressDTO.java` | `identity.api.dto` | 收货地址 DTO（替代 UserAddress Entity） |
| `FavoriteItemDTO.java` | `identity.api.dto` | 收藏项 DTO |
| `LoginResultDTO.java` | `identity.api.dto` | 登录结果 DTO |
| `TokenResultDTO.java` | `identity.api.dto` | 令牌结果 DTO |
| `CaptchaResultDTO.java` | `identity.api.dto` | 图形验证码结果 DTO |
| `LoginLogDTO.java` | `identity.api.dto` | 登录日志 DTO |
| `UserSummaryDTO.java` | `identity.api.dto` | 用户摘要（列表/统计用） |
| `RegisterCommand.java` | `identity.api.command` | 注册入参 |
| `LoginCommand.java` | `identity.api.command` | 登录入参 |
| `RefreshTokenCommand.java` | `identity.api.command` | 刷新令牌入参 |
| `ChangePasswordCommand.java` | `identity.api.command` | 修改密码入参 |
| `UpdateProfileCommand.java` | `identity.api.command` | 更新资料入参 |
| `UpdatePhoneCommand.java` | `identity.api.command` | 修改手机号入参 |
| `UpdateEmailCommand.java` | `identity.api.command` | 修改邮箱入参 |
| `UploadAvatarCommand.java` | `identity.api.command` | 上传头像入参 |
| `SendCodeCommand.java` | `identity.api.command` | 发送验证码入参 |
| `ResetPasswordCommand.java` | `identity.api.command` | 重置密码入参 |
| `SaveAddressCommand.java` | `identity.api.command` | 保存地址入参 |
| `UpdateAddressCommand.java` | `identity.api.command` | 更新地址入参 |
| `AddFavoriteCommand.java` | `identity.api.command` | 添加收藏入参 |
| `RemoveFavoriteCommand.java` | `identity.api.command` | 移除收藏入参 |
| `UpdateUserStatusCommand.java` | `identity.api.command` | 更新用户状态入参 |
| `UpdateUserRoleCommand.java` | `identity.api.command` | 更新用户角色入参 |
| `UserListQuery.java` | `identity.api.query` | 用户列表查询入参 |
| `LoginLogQuery.java` | `identity.api.query` | 登录日志查询入参 |
| `LoginResult.java` | `identity.api.result` | 登录出参 |
| `TokenResult.java` | `identity.api.result` | 刷新令牌出参 |
| `CaptchaResult.java` | `identity.api.result` | 图形验证码出参 |
| `UserApplicationService.java` | `identity.application` | 实现 UserApi |
| `AuthApplicationService.java` | `identity.application` | 实现 AuthApi |
| `AddressApplicationService.java` | `identity.application` | 实现 AddressApi |
| `FavoriteApplicationService.java` | `identity.application` | 实现 FavoriteApi |
| `AdminUserApplicationService.java` | `identity.application` | 实现 AdminUserApi |
| `VerificationCodeApplicationService.java` | `identity.application` | 验证码内部实现 |
| `CaptchaApplicationService.java` | `identity.application` | 图形验证码内部实现 |

**新增文件：40 个**

### 3.3 改造文件清单

| 改造文件 | 改造内容 |
|---|---|
| `AuthController` | 注入改为 `AuthApplicationService` |
| `UserController` | 注入改为 `UserApplicationService` + `VerificationCodeApplicationService`；`SecurityUtils` → `CurrentUserContext` |
| `UserAddressController` | 注入改为 `AddressApplicationService`；`SecurityUtils` → `CurrentUserContext` |
| `UserFavoriteController` | 注入改为 `FavoriteApplicationService`；`SecurityUtils` → `CurrentUserContext` |
| `AdminUserController` | 注入改为 `AdminUserApplicationService` |
| `VerificationCodeController` | 注入改为 `VerificationCodeApplicationService`；`SecurityUtils` → `CurrentUserContext` |
| `AuthServiceImpl` → `AuthApplicationService` | 字段注入改为外部模块 api；实现 AuthApi |
| `UserServiceImpl` → `UserApplicationService` | 实现 UserApi；`getUserById()` 返回 `UserSnapshot` 而非 `User`；`selectUserPage`/`updateUserById` 内部化 |
| `UserAddressServiceImpl` → `AddressApplicationService` | 实现 AddressApi；`getAddressById()` 返回 `AddressDTO` 而非 `UserAddress` |
| `UserFavoriteServiceImpl` → `FavoriteApplicationService` | 实现 FavoriteApi；ProductApi 引用不变 |
| `AdminUserServiceImpl` → `AdminUserApplicationService` | 实现 AdminUserApi；`selectUserPage`/`updateUserById` 改为内部 Mapper 调用 |
| `VerificationCodeServiceImpl` → `VerificationCodeApplicationService` | 内部实现 |
| `CaptchaService` → `CaptchaApplicationService` | 内部实现 |
| `UserService` 接口 | 转为 `UserApi`，`getUserById()` 返回 `UserSnapshot`；移除 `selectUserPage`/`updateUserById`（内部化） |
| `UserAddressService` 接口 | 转为 `AddressApi`，`getAddressById()` 返回 `AddressDTO` |
| **security 包引用 User 的类**（SecurityUserDetailsService, SecurityUserDetails, TokenVersionService, UserStatusCacheService, SecurityUtils 等） | `import com.seckill.mall.entity.User` → `import com.seckill.mall.identity.infrastructure.entity.User` |
| 外部调用方 `OrderServiceImpl`（order） | `UserService` → `identity.api.UserApi`；`UserAddressService` → `identity.api.AddressApi`；`User`/`UserAddress` entity → Snapshot/DTO |
| 外部调用方 `OrderQueryServiceImpl`（order） | 同上 |
| 外部调用方 `WalletServiceImpl`（payment） | `UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot` |
| 外部调用方 `RechargeCardServiceImpl`（payment） | `UserService.addBalance()` → `identity.api.UserApi.addBalance()` |
| 外部调用方 `CouponServiceImpl`（coupon） | `UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot` |
| 外部调用方 `SeckillOrderServiceImpl` / `SeckillGoodsServiceImpl`（seckill） | `UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot` |
| 外部调用方 `ProductReviewServiceImpl`（review） | `UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot` |
| 外部调用方 `StatsServiceImpl`（stats） | `UserService` → `identity.api.UserApi` |
| 外部调用方 `SystemServiceImpl`（system） | `UserMapper` → `identity.api.UserApi`（消除直接 Mapper 依赖）或 `shared.kernel.CurrentUserContext` |
| 外部调用方 `OperationLogAspect`（system aspect） | `UserMapper.selectById()` → `shared.kernel.CurrentUserContext.getCurrentUser()` |

**改造文件：25+ 个（含外部调用方 + security 包 import 更新）**

---

## 4. 外部模块依赖

### 4.1 identity 依赖的外部模块 API

| 外部模块 | 当前依赖方式 | 目标依赖方式 | 需对方提供的 API |
|---|---|---|---|
| **product** | `UserFavoriteServiceImpl` → `ProductApi`（已迁移完成） | `product.api.ProductApi` | `getProductById()` → `ProductSnapshot`（已就绪） |
| **upload** | `AuthServiceImpl` → `UploadService`（头像上传） | `upload.api.UploadApi` | `uploadFile()` → `UploadResult`（upload 迁移后切换） |
| **shared.kernel** | `CachePort`（CaptchaService/VerificationCodeService）、`CurrentUserContext` | `shared.kernel.CachePort` + `shared.kernel.CurrentUserContext` | `get()`/`set()`/`del()`；`getCurrentUser()` |
| **security**（横切） | `AuthServiceImpl` → `JwtUtils`/`TokenVersionService`/`TokenBlacklistService`/`SecurityUserDetailsService` | security 包保持原位，`AuthApplicationService` 直接引用 | JWT 生成/校验、Token 版本管理、Token 黑名单 |

> **说明**：identity 对外依赖较少，主要是 product（已迁移完成）和 upload（尚未迁移，过渡期保留 UploadService 引用）。security 包是横切基础设施，保持原位，AuthApplicationService 直接引用 security 包中的类完成认证逻辑。

### 4.2 依赖 identity 的外部模块

| 外部模块 | 当前调用 | 目标调用 | 需 identity 提供的 API |
|---|---|---|---|
| **order**（OrderServiceImpl, OrderQueryServiceImpl） | `UserService.getUserById()` + `UserAddressService.getAddressById()` + `User`/`UserAddress` entity | `identity.api.UserApi.getUserById()` + `identity.api.AddressApi.getAddressById()` | `getUserById()` → `UserSnapshot`；`getAddressById()` → `AddressDTO` |
| **payment**（WalletServiceImpl, RechargeCardServiceImpl） | `UserService.getUserById()` + `UserService.addBalance()` + `User` entity | `identity.api.UserApi.getUserById()` + `identity.api.UserApi.addBalance()` | `getUserById()` → `UserSnapshot`；`addBalance()` |
| **coupon**（CouponServiceImpl） | `UserService.getUserById()` + `User` entity | `identity.api.UserApi.getUserById()` | `getUserById()` → `UserSnapshot` |
| **seckill**（SeckillOrderServiceImpl, SeckillGoodsServiceImpl） | `UserService.getUserById()` + `SecurityUtils.getCurrentUserId()` + `User` entity | `identity.api.UserApi.getUserById()` + `shared.kernel.CurrentUserContext` | `getUserById()` → `UserSnapshot` |
| **review**（ProductReviewServiceImpl） | `UserService.getUserById()` + `User` entity | `identity.api.UserApi.getUserById()` | `getUserById()` → `UserSnapshot` |
| **stats**（StatsServiceImpl） | `UserService.countAll()` + `countTodayRegistered()` + `selectUserTrend()` + `getUserDisplayNamesByIds()` | `identity.api.UserApi.*` | `countAll()`；`countTodayRegistered()`；`selectUserTrend()`；`getUserDisplayNamesByIds()` |
| **system**（SystemServiceImpl, OperationLogAspect） | `UserMapper.selectById()` | `shared.kernel.CurrentUserContext.getCurrentUser()` 或 `identity.api.UserApi.getUserById()` | 消除直接 Mapper 依赖 |
| **ai**（AgentService 等） | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | `getCurrentUser()` → `CurrentUser` |
| **analytics**（TrackingService） | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | 同上 |
| **security**（SecurityUserDetailsService 等） | `User` entity 直接引用 | `identity.infrastructure.entity.User`（import 更新） | User Entity 移动后更新 import |

**依赖 identity 的外部模块：9 个业务模块（order, payment, coupon, seckill, review, stats, system, ai, analytics）+ security 横切包**

### 4.3 同步需定义的 API 契约（最小契约集）

identity 迁移时，需同步定义以下外部模块的最小 API 契约（identity 迁移的前置条件）：

| 外部模块 | 需定义的 API 方法 | 返回类型 | 紧迫性 |
|---|---|---|---|
| `shared.kernel.CurrentUserContext` | `getCurrentUser()` / `getCurrentUserId()` / `getCurrentEmail()` | `CurrentUser` / `Long` / `String` | P0（替代 SecurityUtils，Controller 层切换必需） |
| `product.api.ProductApi` | `getProductById(Long)` | `ProductSnapshot` | ✅ 已就绪（Product 已迁移完成） |
| `upload.api.UploadApi` | `uploadFile(MultipartFile)` | `UploadResult` | P1（upload 尚未迁移，过渡期保留 UploadService 引用） |

> **说明**：identity 迁移的前置条件较轻量。product 已迁移完成，ProductApi 已就绪。shared.kernel.CurrentUserContext 应已存在（前两模块迁移时已引入）。upload 尚未迁移，过渡期 AuthApplicationService 保留对 UploadService 的引用，待 upload 迁移后切换为 UploadApi。

---

## 5. 迁移步骤

> 遵循 Strangler Pattern（绞杀者模式）：逐步迁移，每步可独立编译/测试/回滚，旧 Service 在外部调用方全部切换前保留作为适配层。
> 阶段编号沿用 Order/Product 模块迁移经验（I.0 → I.6），便于团队对齐。

### Phase I.0：创建 identity 模块包结构

**操作**：创建以下空包（含 `package-info.java`）：
```
com.seckill.mall.identity
com.seckill.mall.identity.api
com.seckill.mall.identity.api.dto
com.seckill.mall.identity.api.command
com.seckill.mall.identity.api.query
com.seckill.mall.identity.api.result
com.seckill.mall.identity.application
com.seckill.mall.identity.application.facade
com.seckill.mall.identity.domain
com.seckill.mall.identity.infrastructure
com.seckill.mall.identity.infrastructure.mapper
com.seckill.mall.identity.infrastructure.entity
com.seckill.mall.identity.interfaces
com.seckill.mall.identity.interfaces.vo
```

**影响文件**：新增约 14 个 `package-info.java`
**验证方式**：`mvn compile` 通过（空包不影响编译）
**可回滚性**：删除新增包即可
**提交粒度**：1 次提交

### Phase I.2：定义 identity.api（接口 + DTO + Command + Query + Result）

**操作**：创建 API 接口和数据契约：
- `UserApi`、`AuthApi`、`AddressApi`、`FavoriteApi`、`AdminUserApi` 接口
- 16 个 Command、2 个 Query、3 个 Result、7 个 DTO/Snapshot
- 暂无实现，仅定义契约
- `UserApi.getUserById()` 返回 `UserSnapshot`（非 Entity）
- `AddressApi.getAddressById()` 返回 `AddressDTO`（非 Entity）

**影响文件**：新增 40 个文件（见 3.2 新增文件清单）
**验证方式**：`mvn compile` 通过（接口和 DTO 无依赖循环）
**可回滚性**：删除新增文件即可
**提交粒度**：1 次提交

### Phase I.3：迁移 infrastructure（Mapper + Entity + 枚举）

**操作**：
1. 移动 4 个 Entity → `identity.infrastructure.entity`
2. 移动 4 个 Mapper → `identity.infrastructure.mapper`
3. 移动 `UserStatus`、`UserRole`、`LoginResult` → `identity.domain`
4. **更新 security 包中所有引用 User/UserStatus/UserRole 的 import**：
   - `SecurityUserDetailsService` → `import com.seckill.mall.identity.infrastructure.entity.User`
   - `SecurityUserDetails` → 同上
   - `TokenVersionService` → 同上
   - `UserStatusCacheService` → 同上
   - `SecurityUtils` → 同上（如有引用）
5. 更新所有引用这些类的业务文件的 import（MyBatis XML namespace 需同步更新）
6. 检查 `resources/mapper/*.xml` 中 User/UserAddress/UserFavorite/LoginLog 相关 Mapper 的 namespace

**影响文件**：11 个移动 + security 包 5+ 个 import 更新 + 约 15 个业务文件 import 更新
**验证方式**：`mvn compile` 通过 + `mvn test` 通过（Mapper 测试）
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交
**风险**：⚠️ 高 — security 包引用 User Entity，需同步更新 import；外部模块（order/payment/coupon/seckill/review/stats）import User Entity，需同步更新

### Phase I.4-A：创建 Application 层门面（保留原 Service）

**操作**：
1. 创建 `UserApplicationService`（复制 `UserServiceImpl` 逻辑），实现 `UserApi`；`getUserById()` 返回 `UserSnapshot`
2. 创建 `AuthApplicationService`（复制 `AuthServiceImpl` 逻辑），实现 `AuthApi`；引用 security 包的 JwtUtils 等
3. 创建 `AddressApplicationService`（复制 `UserAddressServiceImpl` 逻辑），实现 `AddressApi`；`getAddressById()` 返回 `AddressDTO`
4. 创建 `FavoriteApplicationService`（复制 `UserFavoriteServiceImpl` 逻辑），实现 `FavoriteApi`；ProductApi 引用不变
5. 创建 `AdminUserApplicationService`（复制 `AdminUserServiceImpl` 逻辑），实现 `AdminUserApi`；`selectUserPage`/`updateUserById` 内部化
6. 创建 `VerificationCodeApplicationService`（复制 `VerificationCodeServiceImpl` 逻辑）
7. 创建 `CaptchaApplicationService`（复制 `CaptchaService` 逻辑）
8. 移动 `UserConverter` → `identity.application.facade.UserConverter`
9. 字段注入改为内部 Mapper（已迁移到 infrastructure）+ 外部模块 api
10. **保留原 Service 接口和 impl**（作为适配层，暂不删除）

**影响文件**：7 个新增 ApplicationService + 1 个 Converter 移动
**验证方式**：`mvn compile` 通过 + `mvn test` 通过
**可回滚性**：删除新增 ApplicationService 即可（原 Service 仍在）
**提交粒度**：1 次提交
**说明**：此步为"双轨期"开始，新旧并存，外部调用方仍用旧 Service

### Phase I.4-B：移动 Controller 到 interfaces

**操作**：
1. 移动 6 个 Controller → `identity.interfaces`
2. `AuthController` 注入改为 `AuthApplicationService`
3. `UserController` 注入改为 `UserApplicationService` + `VerificationCodeApplicationService`；`SecurityUtils` → `CurrentUserContext`
4. `UserAddressController` 注入改为 `AddressApplicationService`；`SecurityUtils` → `CurrentUserContext`
5. `UserFavoriteController` 注入改为 `FavoriteApplicationService`；`SecurityUtils` → `CurrentUserContext`
6. `AdminUserController` 注入改为 `AdminUserApplicationService`
7. `VerificationCodeController` 注入改为 `VerificationCodeApplicationService`；`SecurityUtils` → `CurrentUserContext`
8. 移动前端 VO → `identity.interfaces.vo`

**影响文件**：6 个 Controller 改造 + 7 个 VO 移动
**验证方式**：`mvn compile` 通过 + Controller API 接口测试
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交
**说明**：Controller 切换到 ApplicationService 后，原 Service 仅被外部模块引用

### Phase I.4-C：切换外部调用方到 identity.api

**操作**：
1. `OrderServiceImpl` / `OrderQueryServiceImpl`（order）：`UserService` → `identity.api.UserApi`；`UserAddressService` → `identity.api.AddressApi`；`User`/`UserAddress` entity → `UserSnapshot`/`AddressDTO`
2. `WalletServiceImpl` / `RechargeCardServiceImpl`（payment）：`UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot`；`addBalance()`/`deductBalance()` 改调 UserApi
3. `CouponServiceImpl`（coupon）：`UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot`
4. `SeckillOrderServiceImpl` / `SeckillGoodsServiceImpl`（seckill）：`UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot`；`SecurityUtils` → `CurrentUserContext`
5. `ProductReviewServiceImpl`（review）：`UserService` → `identity.api.UserApi`；`User` entity → `UserSnapshot`
6. `StatsServiceImpl`（stats）：`UserService` → `identity.api.UserApi`
7. `SystemServiceImpl` / `OperationLogAspect`（system）：`UserMapper` → `identity.api.UserApi`（消除直接 Mapper 依赖）或 `CurrentUserContext`
8. `AgentService` 等（ai）：`SecurityUtils` → `shared.kernel.CurrentUserContext`
9. `TrackingService`（analytics）：`SecurityUtils` → `shared.kernel.CurrentUserContext`

**影响文件**：15+ 个外部文件改造
**验证方式**：`mvn compile` 通过 + 全量测试
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交（或按模块拆为多个子提交）
**说明**：此步完成后，原 identity Service 接口和 impl 无任何引用

### Phase I.5：删除旧 Service 接口和 impl（清理绞杀残留）

**操作**：
1. 删除 `AuthService` / `AuthServiceImpl`
2. 删除 `UserService` / `UserServiceImpl`
3. 删除 `UserAddressService` / `UserAddressServiceImpl`
4. 删除 `UserFavoriteService` / `UserFavoriteServiceImpl`
5. 删除 `AdminUserService` / `AdminUserServiceImpl`
6. 删除 `VerificationCodeService` / `VerificationCodeServiceImpl`
7. 删除 `CaptchaService`（具体类，已由 `CaptchaApplicationService` 替代）
8. 删除旧 DTO（已由 Command/Query 替代）：`LoginRequest`、`RegisterRequest`、`RefreshTokenRequest`、`ChangePasswordRequest`、`ProfileUpdateRequest`、`PhoneUpdateRequest`、`EmailUpdateRequest`、`ForgotPasswordSendRequest`、`ForgotPasswordResetRequest`、`UserListRequest`、`UserStatusUpdateRequest`、`UserRoleUpdateRequest`

**影响文件**：19 个删除
**验证方式**：`mvn compile` 通过 + 全量测试全绿
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交
**说明**：此步为"双轨期"结束，绞杀完成

### Phase I.6：ArchUnit 边界规则验证

**操作**：
1. 更新 `ArchitectureRulesTest`，新增 identity 模块的包边界规则：
   - `identity.api` 只能依赖 shared.kernel + JDK
   - `identity.application` 只能依赖 identity.api + identity.domain + identity.infrastructure + 其他模块 api + shared.kernel + security（横切）
   - `identity.infrastructure` 只能依赖 identity.domain + MyBatis-Plus + shared.kernel
   - `identity.interfaces` 只能依赖 identity.application + identity.api + identity.interfaces.vo + shared.kernel + security（过渡期）
   - 禁止任何业务模块直接依赖 identity.infrastructure.mapper / identity.infrastructure.entity
   - **例外**：security 包允许依赖 `identity.infrastructure.entity.User`（横切基础设施特殊约束）
2. 运行 `mvn test -Dtest=ArchitectureRulesTest`
3. 确认无违规依赖

**影响文件**：1 个测试文件更新
**验证方式**：ArchUnit 测试全绿
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交

---

## 6. 风险点与回滚策略

### 6.1 风险点

| 风险 | 等级 | 影响 | 缓解措施 |
|---|---|---|---|
| **security 包引用 User Entity，移动后需更新 import** | 高 | 5+ 个 security 类引用 User/UserStatus/UserRole，Entity 移动后 import 全部失效 | I.3 中同步更新 security 包所有 import；编译验证 |
| **User Entity 被全项目引用** | 高 | User 是被依赖最多的 Entity，7 个业务模块 + security 包引用 | I.3 中同步更新所有引用方的 import；I.4-C 中将 Entity 引用改为 UserSnapshot |
| **SecurityUtils 被全项目使用** | 高 | 几乎所有 Controller/Service 引用 SecurityUtils.getCurrentUserId() | SecurityUtils 保持原位不迁移；I.4-B/I.4-C 中逐步将 Controller/Service 改用 `shared.kernel.CurrentUserContext` |
| **import 大量修改导致编译错误** | 高 | 11 个文件移动 + 25+ 个改造，import 路径全变 | 分步迁移（I.3 → I.4），每步编译验证；IDE 批量更新 import |
| **跨模块 Entity 引用需同步改造** | 高 | 7 个外部模块引用 User/UserAddress Entity，需改为 Snapshot/DTO | I.4-C 中同步将外部模块的 Entity 引用改为 Snapshot/DTO；先确认 UserSnapshot 字段覆盖所有外部使用场景 |
| **User.balance 字段被 payment 模块直接操作** | 中 | WalletServiceImpl/RechargeCardServiceImpl 通过 UserService.addBalance/deductBalance 操作余额 | UserApi 保留 addBalance/deductBalance 方法；确认 UserSnapshot 包含 balance 字段 |
| **MyBatis XML namespace 需同步更新** | 中 | 4 个 Mapper 移动后，XML 的 namespace 需更新 | I.3 中同步更新 XML；检查 `resources/mapper/User*.xml` |
| **UserService.selectUserPage/updateUserById 暴露 MyBatis-Plus 类型** | 中 | 这两个方法暴露 IPage/Wrapper/User，是内部封装方法 | 内部化到 AdminUserApplicationService，不通过 UserApi 暴露；AdminUserApi.listUsers 接收 UserListQuery |
| **VerificationCodeController 直接使用 StringRedisTemplate** | 中 | Controller 层直接操作 Redis（每日限额 Lua 脚本），违反分层 | I.4-B 中将限额逻辑下沉到 VerificationCodeApplicationService；通过 CachePort 执行 Lua |
| **upload 模块尚未迁移** | 低 | AuthApplicationService 依赖 UploadService（头像上传），upload 未迁移 | 过渡期保留 UploadService 引用；待 upload 迁移后切换为 UploadApi |
| **测试可能失败** | 中 | 现有测试可能直接依赖 UserService 接口或 User entity | I.4 后运行全量测试，修复测试 import |
| **ArchUnit 规则可能报新违规** | 低 | 新包结构可能触发已有 ArchUnit 规则；security → identity.infrastructure.entity 需加例外 | I.6 更新规则，为 security 包添加例外条款 |
| **Spring Bean 注入冲突** | 低 | I.4-A 双轨期新旧 Service 并存，bean 名称可能冲突 | 使用 `@Service("userApplicationService")` 显式命名；或用 `@Primary` 标注新 ApplicationService |

### 6.2 回滚策略

#### 每 Phase 的回滚方式

| Phase | 回滚方式 | 说明 |
|---|---|---|
| I.0 | 删除新增空包 | 无代码影响 |
| I.2 | 删除新增 API/DTO 文件 | 无代码影响（未引用） |
| I.3 | `git revert` | Mapper/Entity 移动涉及 import（含 security 包），需整体回滚 |
| I.4-A | 删除新增 ApplicationService | 原 Service 仍在，无影响（双轨期安全） |
| I.4-B | `git revert` | Controller 改造，回滚后 Controller 仍用旧 Service |
| I.4-C | `git revert` | 外部调用方改造，回滚后外部仍用旧 Service |
| I.5 | `git revert` | 删除旧 Service，回滚后恢复旧 Service |
| I.6 | `git revert` | ArchUnit 规则更新 |

#### 紧急回滚

如果迁移导致严重问题（编译失败/测试全红/运行时错误）：

```bash
# 回滚到迁移前最后一个绿色提交
git log --oneline -20          # 找到迁移前的 commit
git revert <commit>..HEAD      # 回滚所有迁移提交
# 或
git reset --hard <迁移前commit>  # 硬回滚（丢弃所有迁移变更）
```

**前提**：每个 Phase 单独提交，提交信息标注 `Phase 4 Identity I.X`。

#### 部分回滚（双轨期保障）

| 失败的 Phase | 回滚范围 | 保留的成果 |
|---|---|---|
| I.4-A 失败 | 仅删除新增 ApplicationService | I.0-I.3（包结构 + API + infrastructure）保留 |
| I.4-B 失败 | 仅 revert I.4-B | I.0-I.4-A 保留（ApplicationService 已创建，Controller 暂未切换，原 Service 仍在） |
| I.4-C 失败 | 仅 revert I.4-C | I.0-I.4-B 保留（Controller 已切换，外部调用方暂未更新，原 Service 作为适配层仍在） |
| I.5 失败 | 仅 revert I.5 | I.0-I.4-C 保留（旧 Service 恢复，与新 ApplicationService 并存，无引用冲突） |

**关键**：I.4-A 创建新 ApplicationService 时**保留原 Service**，I.4-B/I.4-C 逐步切换调用方，I.5 确认无误后再删除原 Service。这样任一阶段失败均可安全回滚到上一阶段（原 Service 仍在）。

---

## 7. 预计提交粒度

| Phase | 提交数 | 提交信息模板 | 说明 |
|---|---|---|---|
| I.0 | 1 | `refactor(identity): I.0 create identity module package structure` | 空包 |
| I.2 | 1 | `refactor(identity): I.2 define identity api contracts (5 Api + 40 dto/command/query/result)` | API 契约 |
| I.3 | 1 | `refactor(identity): I.3 move infrastructure (4 entity + 4 mapper + 3 enum) + update security imports` | 持久化层迁移 + security import 更新 |
| I.4-A | 1 | `refactor(identity): I.4-A create application services (7 ApplicationService, keep legacy Service)` | Application 门面（双轨期开始） |
| I.4-B | 1 | `refactor(identity): I.4-B move controllers to interfaces, switch to ApplicationService` | Controller 迁移 |
| I.4-C | 1（或按模块拆 3-5 个） | `refactor(identity): I.4-C switch external callers to identity.api` | 外部调用方切换 |
| I.5 | 1 | `refactor(identity): I.5 remove legacy Service interfaces and impls` | 清理绞杀残留 |
| I.6 | 1 | `test(identity): I.6 add ArchUnit boundary rules for identity module` | 边界规则 |

**总计：8 次提交（最小粒度），最多 10-12 次（按模块拆分 I.4-C）**

---

## 8. 与 Order/Product 模块迁移的差异对比

| 维度 | Order 模块 | Product 模块 | Identity 模块 | 说明 |
|---|---|---|---|---|
| 核心文件数 | 28 | 18（+Inventory 2） | 33（+security 12 保持原位） | Identity 文件最多，但 security 不迁移 |
| Service 数 | 4 | 4（+Inventory 1） | 7 | Identity Service 最多（含验证码/图形验证码） |
| Controller 数 | 2 | 4 | 6 | Identity Controller 最多 |
| Entity 数 | 2 | 5 | 4 | Identity 含 User/UserAddress/UserFavorite/LoginLog |
| 对外依赖的模块数 | 7 | 2 | 3（product/upload/shared.kernel）+ security 横切 | Identity 依赖 product（已就绪）+ upload（未迁移） |
| 被依赖的外部模块数 | 5 | 9 | 9（order/payment/coupon/seckill/review/stats/system/ai/analytics）+ security | Identity 被依赖最多（User 是基础 Entity） |
| 跨模块 Entity 引用 | 5 类 / 4 模块 | 2 类 / 6 模块 | 2 类 / 7 模块 + security | Identity 的 User 被最广泛引用 |
| 跨模块 Mapper 引用 | 0 | 2 | 1（system→UserMapper） | Identity 存在 1 处违规 Mapper 引用 |
| security 包特殊处理 | 无 | 无 | **12 类保持原位，仅更新 import** | Identity 独有约束 |
| MQ 组件 | 2 | 0 | 0 | Identity 无 MQ |
| 迁移步骤数 | 8 | 8 | 8（I.0-I.6，I.4 拆 A/B/C） | 步骤对齐 |

**结论**：Identity 模块是三个已迁移模块中**被依赖最广、约束最特殊**的模块。最大的风险点是：
1. **security 包引用 User Entity** — 移动 Entity 后需同步更新 security 包 5+ 个类的 import（I.3 重点）
2. **User Entity 被 7 个业务模块引用** — 需在 I.4-C 中将所有 Entity 引用改为 UserSnapshot
3. **SecurityUtils 被全项目使用** — 保持原位不迁移，逐步引导外部模块改用 `shared.kernel.CurrentUserContext`

---

> **本文件为 Phase 4 Identity 模块迁移前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **下一步**：基于本迁移计划，生成 `IDENTITY-API-CONTRACT.md`（API 契约详细定义），然后按 Phase I.0 → I.6 逐步执行迁移。