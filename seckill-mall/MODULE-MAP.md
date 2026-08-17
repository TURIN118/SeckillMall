# SeckillMall 模块地图 (MODULE-MAP)

> 生成时间：2026-08-17
> 阶段：Phase 6 第一阶段 - 建立模块地图
> 说明：本文件由代码扫描自动生成，未修改任何代码。
> 扫描范围：`seckill-mall/src/main/java/com/seckill/mall/**/*.java`

---

## 1. 总览

### 1.1 项目结构概述

项目采用 **单 Maven 模块 + 包内分层** 的传统架构，源码根包为 `com.seckill.mall`。当前包结构如下（21 个直接子包）：

| 包 | 类型 | 说明 |
|---|---|---|
| `ai` | ✅ 已模块化 | AI 网关/助手/客服/AIGC，含子包 gateway/assistant/customerservice/aigc |
| `analytics.tracking` | ✅ 已模块化 | 用户行为埋点旁路 |
| `shared.kernel` | ✅ 已模块化 | 共享内核：CachePort/MessageBusPort/DistributedLockPort/CurrentUserContext + adapter |
| `controller` | ❌ 传统分层 | 27 个 Controller（含 Admin 系列） |
| `service` + `service.impl` | ❌ 传统分层 | 25 个 Service 接口 + 31 个 ServiceImpl |
| `entity` + `entity.enums` | ❌ 传统分层 | 24 个 Entity/PO + 11 个枚举 |
| `mapper` | ❌ 传统分层 | 22 个 Mapper |
| `dto` | ❌ 传统分层 | 33 个请求 DTO |
| `vo` | ❌ 传统分层 | 40 个响应 VO |
| `cache` | ⚠️ 横切基础设施 | RedisService/RedisKeyConstants/SeckillLuaService/CacheDegradeService/SeckillBloomInitializer |
| `security` | ⚠️ 横切基础设施 | JWT/过滤器/UserDetails/Token 黑名单等 12 个类 |
| `config` | ⚠️ 横切基础设施 | 10 个配置类 |
| `aspect` | ⚠️ 横切基础设施 | RateLimitAspect/OperationLogAspect/OperationLogRecorder |
| `annotation` | ⚠️ 横切基础设施 | OperationLog/RateLimit 注解 |
| `converter` | ⚠️ 横切基础设施 | UserConverter/RechargeCardConverter/SeckillOrderConverter |
| `mq` | ⚠️ 横切基础设施 | producer/consumer/message（主要为 seckill 服务） |
| `scheduler` | ⚠️ 横切基础设施 | SeckillStatusScheduler |
| `common` | ⚠️ 横切基础设施 | Result/ErrorCode/PageResult/GlobalExceptionHandler/XssCleanUtil |
| `exception` | ⚠️ 横切基础设施 | BusinessException |
| `util` / `utils` | ⚠️ 横切基础设施 | ProductSortUtil/MapUtils/DataMaskUtil/IpUtils |

### 1.2 已模块化的部分

- **`com.seckill.mall.ai`**：4 个子模块（gateway/assistant/customerservice/aigc），各自含 controller/service/dto/entity/config。AI 模块通过 `ProductService`、`OrderQueryService`、`SecurityUtils`、`shared.kernel.util.SseUtils` 与核心业务交互。
- **`com.seckill.mall.analytics.tracking`**：完整自包含（controller/service/mq/entity/dto/aspect/annotation），仅依赖 `SecurityUtils`。
- **`com.seckill.mall.shared.kernel`**：定义 3 个 Port（CachePort/MessageBusPort/DistributedLockPort）+ CurrentUserContext + 2 个 Adapter（RedisCacheAdapter/RabbitMessageBusAdapter）+ SseUtils。

### 1.3 仍为传统分层的核心业务模块列表

经扫描识别出 **13 个业务模块**（含 3 个已模块化）：

| # | 模块 | 代号 | 状态 |
|---|---|---|---|
| 1 | identity（用户/认证/安全） | IDN | 传统分层 |
| 2 | product（商品/分类/SKU/属性/Banner） | PRD | 传统分层 |
| 3 | inventory（库存） | INV | 传统分层（无独立 Entity/Mapper） |
| 4 | cart（购物车） | CRT | 传统分层 |
| 5 | order（订单） | ORD | 传统分层 |
| 6 | payment（支付/钱包/充值卡） | PAY | 传统分层 |
| 7 | coupon（优惠券） | CUP | 传统分层 |
| 8 | seckill（秒杀） | SKL | 传统分层（已有部分 Port） |
| 9 | review（商品评价） | REV | 传统分层 |
| 10 | stats（统计/报表） | STA | 传统分层 |
| 11 | system（系统管理/运维/操作日志） | SYS | 传统分层 |
| 12 | upload（文件上传/存储/邮件） | UPL | 传统分层 |
| 13 | ai（AI） | AI | ✅ 已模块化 |
| 14 | analytics.tracking（埋点） | ANA | ✅ 已模块化 |
| 15 | shared.kernel（共享内核） | SK | ✅ 已模块化 |

---

## 2. 模块详情

### 2.1 identity（用户/认证/安全）

- **负责领域**：用户注册/登录/认证、用户资料管理、收货地址、用户收藏、验证码、图形验证码、管理员用户管理、JWT 安全基础设施。
- **Controller**：`AuthController`、`UserController`、`UserAddressController`、`UserFavoriteController`、`VerificationCodeController`、`AdminUserController`
- **Service（接口）**：`AuthService`、`UserService`、`UserAddressService`、`UserFavoriteService`、`VerificationCodeService`、`CaptchaService`（具体类）、`AdminUserService`
- **ServiceImpl**：`AuthServiceImpl`、`UserServiceImpl`、`UserAddressServiceImpl`、`UserFavoriteServiceImpl`、`VerificationCodeServiceImpl`、`AdminUserServiceImpl`
- **Mapper**：`UserMapper`、`UserAddressMapper`、`UserFavoriteMapper`、`LoginLogMapper`
- **Entity/PO**：`User`、`UserAddress`、`UserFavorite`、`LoginLog`；枚举 `UserStatus`、`UserRole`、`LoginResult`
- **DTO**：`LoginRequest`、`RegisterRequest`、`RefreshTokenRequest`、`ChangePasswordRequest`、`ProfileUpdateRequest`、`PhoneUpdateRequest`、`EmailUpdateRequest`、`ForgotPasswordSendRequest`、`ForgotPasswordResetRequest`、`UserListRequest`、`UserStatusUpdateRequest`、`UserRoleUpdateRequest`
- **VO**：`UserVO`、`UserAddressVO`、`FavoriteItemVO`、`LoginVO`、`TokenVO`、`CaptchaVO`、`LoginLogVO`
- **其他**：`converter.UserConverter`；`security` 包全部（`SecurityUtils`、`JwtUtils`、`JwtAuthenticationFilter`、`JwtAuthenticationEntryPoint`、`JwtAccessDeniedHandler`、`ReplayProtectionFilter`、`SecurityUserDetails`、`SecurityUserDetailsService`、`TokenVersionService`、`TokenBlacklistService`、`UserStatusCacheService`、`RsaKeyProvider`）
- **依赖关系**：
  - **依赖**：`product`（UserFavoriteServiceImpl→ProductService + Product entity）；`upload`（AuthServiceImpl→UploadService）；`shared.kernel`（CachePort、CurrentUserContext）
  - **被依赖**：`order`、`payment`、`coupon`、`seckill`、`review`、`stats`、`analytics`、`ai`（几乎所有模块依赖 UserService/SecurityUtils）

### 2.2 product（商品/分类/SKU/属性/Banner）

- **负责领域**：商品 SPU/SKU 管理、商品分类及分类属性、商品属性及属性值、首页 Banner、商品上下架。
- **Controller**：`ProductController`、`ProductSkuController`、`CategoryController`、`CategoryAttributeController`、`BannerController`、`BannerPublicController`、`AdminProductSkuController`
- **Service（接口）**：`ProductService`、`ProductSkuService`、`ProductAttributeService`、`CategoryService`、`CategoryAttributeService`、`CategoryAttributeValueService`、`BannerService`
- **ServiceImpl**：`ProductServiceImpl`、`ProductSkuServiceImpl`、`ProductAttributeServiceImpl`、`CategoryServiceImpl`、`CategoryAttributeServiceImpl`、`CategoryAttributeValueServiceImpl`、`BannerServiceImpl`
- **Mapper**：`ProductMapper`、`ProductSkuMapper`、`ProductAttributeMapper`、`ProductAttributeValueMapper`、`CategoryMapper`、`CategoryAttributeMapper`、`CategoryAttributeValueMapper`、`BannerMapper`
- **Entity/PO**：`Product`、`ProductSku`、`ProductAttribute`、`ProductAttributeValue`、`Category`、`CategoryAttribute`、`CategoryAttributeValue`、`Banner`；枚举 `ProductStatus`、`AttributeType`、`AttributeInputType`
- **DTO**：`ProductCreateRequest`、`ProductUpdateRequest`、`ProductQueryRequest`、`SkuGenerateRequest`、`ProductSkuDTO`、`ProductAttributeDTO`、`CategoryAttributeDTO`、`CategoryCreateRequest`、`CategoryUpdateRequest`、`CategoryStatusUpdateRequest`、`BannerCreateRequest`、`BannerUpdateRequest`
- **VO**：`ProductVO`、`ProductSkuVO`、`ProductAttributeVO`、`CategoryAttributeVO`、`CategoryVO`、`BannerVO`
- **其他**：`util.ProductSortUtil`
- **依赖关系**：
  - **依赖**：`shared.kernel`（CachePort）；内部 Category 与 Product 互依（CategoryServiceImpl→ProductMapper，ProductServiceImpl→CategoryMapper/CategoryService）
  - **被依赖**：`identity`、`inventory`、`cart`、`order`、`coupon`、`seckill`、`review`、`stats`、`ai`（Product 是被依赖最多的基础模块）

### 2.3 inventory（库存）

- **负责领域**：商品库存查询与扣减/回补。⚠️ 无独立 Entity/Mapper，直接复用 product 的 ProductMapper/ProductSkuMapper。
- **Controller**：无独立 Controller（通过 order 调用）
- **Service（接口）**：`InventoryService`
- **ServiceImpl**：`InventoryServiceImpl`
- **Mapper**：⚠️ 无独立 Mapper，直接注入 `ProductMapper`、`ProductSkuMapper`（跨模块 Mapper 调用）
- **Entity/PO**：⚠️ 无独立 Entity，引用 `Product`、`ProductSku`、`ProductStatus`
- **依赖关系**：
  - **依赖**：`product`（ProductMapper + ProductSkuMapper + Product/ProductSku entity）⚠️ 跨模块 Mapper
  - **被依赖**：`order`（OrderServiceImpl、OrderLifecycleServiceImpl）

### 2.4 cart（购物车）

- **负责领域**：购物车增删改查、购物车结算。
- **Controller**：`CartController`
- **Service（接口）**：`CartService`
- **ServiceImpl**：`CartServiceImpl`
- **Mapper**：`CartMapper`
- **Entity/PO**：`Cart`
- **DTO**：`CartCheckoutRequest`
- **VO**：`CartItemVO`
- **依赖关系**：
  - **依赖**：`product`（ProductService + ProductSkuService + Product/ProductSku entity）⚠️ 跨模块 Service + Entity
  - **被依赖**：`order`（OrderServiceImpl→CartService + Cart entity）

### 2.5 order（订单）

- **负责领域**：普通订单创建/查询/生命周期（支付/发货/取消/完成）、管理员订单管理、订单取消延迟消息消费。
- **Controller**：`OrderController`、`AdminOrderController`
- **Service（接口）**：`OrderService`、`OrderQueryService`、`OrderLifecycleService`、`AdminOrderService`
- **ServiceImpl**：`OrderServiceImpl`、`OrderQueryServiceImpl`、`OrderLifecycleServiceImpl`、`AdminOrderServiceImpl`
- **Mapper**：`NormalOrderMapper`、`NormalOrderItemMapper`
- **Entity/PO**：`NormalOrder`、`NormalOrderItem`；枚举 `OrderStatus`
- **DTO**：`AdminOrderQueryRequest`、`NormalOrderPayRequest`、`ShipRequest`、`BuyNowRequest`
- **VO**：`NormalOrderVO`、`NormalOrderItemVO`、`NormalOrderDetailVO`、`AdminOrderVO`、`OrderListItemVO`、`OrderStatusItemVO`、`OrderTrendVO`、`OrderStatusDistributionVO`
- **其他**：`mq.consumer.OrderCancelConsumer`、`mq.message.OrderDelayMessage`
- **依赖关系**：
  - **依赖**：`cart`（CartService + Cart entity）、`coupon`（CouponUsageService）、`inventory`（InventoryService）、`product`（ProductService/ProductSkuService + Product/ProductSku entity）、`identity`（UserAddressService/UserService + UserAddress entity）、`payment`（PaymentService）、`seckill`（SeckillOrderService + SeckillOrder entity）、`shared.kernel`（MessageBusPort）、`upload`（EmailService）
  - **被依赖**：`payment`（WalletServiceImpl→OrderService）、`review`（ProductReviewServiceImpl→OrderService）、`ai`（AgentService→OrderQueryService）

### 2.6 payment（支付/钱包/充值卡）

- **负责领域**：支付服务、用户钱包余额与流水、充值卡生成与核销。
- **Controller**：`WalletController`、`AdminRechargeCardController`
- **Service（接口）**：`PaymentService`、`WalletService`、`RechargeCardService`
- **ServiceImpl**：`PaymentServiceImpl`、`WalletServiceImpl`、`RechargeCardServiceImpl`
- **Mapper**：`RechargeCardMapper`
- **Entity/PO**：`RechargeCard`；枚举 `RechargeCardStatus`
- **DTO**：`WalletRechargeRequest`、`RechargeCardGenerateRequest`
- **VO**：`WalletRecordVO`、`RechargeCardVO`、`RechargeCardGenerateVO`
- **其他**：`converter.RechargeCardConverter`
- **依赖关系**：
  - **依赖**：`order`（WalletServiceImpl→OrderService + NormalOrder entity）、`seckill`（WalletServiceImpl→SeckillOrderService + SeckillOrder entity）、`identity`（UserService + User entity）
  - **被依赖**：`order`（OrderLifecycleServiceImpl→PaymentService）、`seckill`（SeckillOrderServiceImpl→PaymentService）

### 2.7 coupon（优惠券）

- **负责领域**：优惠券模板创建/查询、用户领券/用券、优惠券核销。
- **Controller**：`CouponController`、`AdminCouponController`
- **Service（接口）**：`CouponService`、`CouponUsageService`
- **ServiceImpl**：`CouponServiceImpl`、`CouponUsageServiceImpl`
- **Mapper**：`CouponMapper`、`UserCouponMapper`
- **Entity/PO**：`Coupon`、`UserCoupon`；枚举 `CouponType`、`UserCouponStatus`
- **DTO**：`CouponCreateRequest`
- **VO**：`CouponVO`、`UserCouponVO`、`AdminCouponRecordVO`
- **依赖关系**：
  - **依赖**：`product`（CategoryService + ProductService + Category/Product entity）、`identity`（UserService + User entity）
  - **被依赖**：`order`（OrderServiceImpl/OrderLifecycleServiceImpl→CouponUsageService）

### 2.8 seckill（秒杀）

- **负责领域**：秒杀活动管理、秒杀商品管理、秒杀下单（令牌+Lua 原子扣减+MQ 异步落单）、秒杀订单查询/库存回补、秒杀状态补偿调度。
- **Controller**：`SeckillController`
- **Service（接口）**：`SeckillService`、`SeckillActivityService`、`SeckillGoodsService`、`SeckillOrderService`、`SeckillTokenService`（具体类）、`SeckillInventoryPort`（端口）、`SeckillDbStrategy`（策略接口）
- **ServiceImpl**：`SeckillServiceImpl`、`SeckillActivityServiceImpl`、`SeckillGoodsServiceImpl`、`SeckillOrderServiceImpl`、`SeckillInventoryPortImpl`
- **Mapper**：`SeckillActivityMapper`、`SeckillGoodsMapper`、`SeckillOrderMapper`
- **Entity/PO**：`SeckillActivity`、`SeckillGoods`、`SeckillOrder`；枚举 `SeckillStatus`
- **DTO**：`SeckillActivityCreateRequest`、`SeckillCreateRequest`
- **VO**：`SeckillActivityVO`、`SeckillGoodsVO`、`SeckillOrderVO`、`SeckillOverviewVO`、`SeckillRankingVO`、`SeckillResultVO`
- **其他**：`converter.SeckillOrderConverter`、`scheduler.SeckillStatusScheduler`、`mq.producer.SeckillOrderProducer`、`mq.consumer.SeckillOrderConsumer`、`mq.message.SeckillOrderMessage`、`mq.message.SeckillResultMessage`；依赖 `cache` 包（RedisService/SeckillLuaService/CacheDegradeService/RedisKeyConstants/SeckillBloomInitializer）
- **依赖关系**：
  - **依赖**：`product`（ProductService + Product entity）、`identity`（UserService + User entity + SecurityUtils）、`payment`（PaymentService）、`upload`（EmailService）、`cache`（RedisService/SeckillLuaService 等基础设施）；MQ Consumer 跨模块注入 `ProductMapper`、`UserMapper` ⚠️
  - **被依赖**：`order`（OrderQueryServiceImpl/AdminOrderServiceImpl→SeckillOrderService）、`payment`（WalletServiceImpl→SeckillOrderService）、`review`（ProductReviewServiceImpl→SeckillOrderService）、`stats`（StatsServiceImpl→SeckillOrderService/SeckillGoodsService）、`system`（SystemServiceImpl→SeckillOrderService）

### 2.9 review（商品评价）

- **负责领域**：商品评价提交与查询、管理员评价审核。
- **Controller**：`ProductReviewController`、`AdminReviewController`
- **Service（接口）**：`ProductReviewService`
- **ServiceImpl**：`ProductReviewServiceImpl`
- **Mapper**：`ProductReviewMapper`（⚠️ 另注入 `ProductMapper`、`ProductSkuMapper` 跨模块）
- **Entity/PO**：`ProductReview`（⚠️ 引用 `ProductSku`、`User` 跨模块 entity）
- **VO**：`ProductReviewVO`
- **依赖关系**：
  - **依赖**：`order`（OrderService）、`seckill`（SeckillOrderService）、`identity`（UserService + User entity）、`product`（ProductMapper + ProductSkuMapper + ProductSku entity）⚠️ 跨模块 Mapper
  - **被依赖**：无（仅 Controller 暴露）

### 2.10 stats（统计/报表）

- **负责领域**：首页统计概览、秒杀排行、订单趋势、订单状态分布。
- **Controller**：`StatsController`
- **Service（接口）**：`StatsService`
- **ServiceImpl**：`StatsServiceImpl`
- **Mapper**：无独立 Mapper（通过其他模块 Service 聚合数据）
- **Entity/PO**：引用 `OrderStatus` 枚举
- **VO**：`StatsOverviewVO`、`TrendItemVO`、`SeckillOverviewVO`、`SeckillRankingVO`、`OrderStatusItemVO`、`OrderTrendVO`、`OrderStatusDistributionVO`
- **依赖关系**：
  - **依赖**：`product`（ProductService）、`seckill`（SeckillGoodsService + SeckillOrderService）、`identity`（UserService）
  - **被依赖**：无

### 2.11 system（系统管理/运维/操作日志/健康监控）

- **负责领域**：系统健康监控、操作日志查询、运维信息。
- **Controller**：`SystemController`
- **Service（接口）**：`SystemService`、`SystemHealthMonitor`
- **ServiceImpl**：`SystemServiceImpl`、`SystemHealthMonitorImpl`
- **Mapper**：`OperationLogMapper`
- **Entity/PO**：`OperationLog`；引用 `OrderStatus` 枚举
- **DTO**：`OperationLogQueryRequest`
- **VO**：`SystemHealthVO`、`OperationLogVO`
- **其他**：`aspect.OperationLogAspect`、`aspect.OperationLogRecorder`、`annotation.OperationLog`（⚠️ OperationLogAspect 注入 `UserMapper` 跨模块）
- **依赖关系**：
  - **依赖**：`seckill`（SeckillOrderService）；`identity`（OperationLogAspect→UserMapper）⚠️ 跨模块 Mapper
  - **被依赖**：无

### 2.12 upload（文件上传/存储/邮件）

- **负责领域**：文件上传（本地/MinIO 双实现）、邮件发送。
- **Controller**：`UploadController`
- **Service（接口）**：`UploadService`、`StorageService`、`EmailService`
- **ServiceImpl**：`UploadServiceImpl`、`LocalStorageService`、`MinioStorageService`、`EmailServiceImpl`
- **Mapper**：无
- **Entity/PO**：无
- **VO**：`UploadResultVO`
- **其他**：`config.UploadProperties`
- **依赖关系**：
  - **依赖**：无业务模块依赖（仅依赖 StorageService 内部）
  - **被依赖**：`identity`（AuthServiceImpl→UploadService）、`order`（OrderLifecycleServiceImpl→EmailService）、`seckill`（SeckillOrderServiceImpl→EmailService）

### 2.13 ai（AI）— ✅ 已模块化

- **负责领域**：AI 网关（路由/限流/预算/审计/语义缓存/降级）、购物助手（Function Calling 商品搜索）、智能客服（FAQ + 多轮对话）、AIGC 内容生成与安全审核。
- **子模块**：`ai.gateway`、`ai.assistant`、`ai.customerservice`、`ai.aigc`
- **依赖关系**：
  - **依赖**：`product`（ProductSearchTool→ProductService + ProductQueryRequest + ProductVO）、`order`（AgentService→OrderQueryService + OrderListItemVO）、`identity`（SecurityUtils）、`shared.kernel`（SseUtils）、`common`、`cache`（RedisService/CacheDegradeService）
  - **被依赖**：无

### 2.14 analytics.tracking（埋点）— ✅ 已模块化

- **负责领域**：用户行为事件采集（注解驱动 + MQ 异步落库）。
- **依赖关系**：
  - **依赖**：`identity`（SecurityUtils）、`cache`（RedisService/RedisKeyConstants）
  - **被依赖**：无（通过 `@Tracking` 注解横切生效）

### 2.15 shared.kernel（共享内核）— ✅ 已模块化

- **负责领域**：定义跨模块共享的抽象端口与适配器。
- **Port**：`CachePort`、`MessageBusPort`、`DistributedLockPort`
- **其他**：`CurrentUserContext`、`adapter.RedisCacheAdapter`、`adapter.RabbitMessageBusAdapter`、`util.SseUtils`
- **依赖关系**：
  - **依赖**：无业务模块（仅依赖 Spring/Redis/RabbitMQ 基础设施）
  - **被依赖**：`identity`（SecurityUtils→CurrentUserContext；AuthServiceImpl/CaptchaService→CachePort）、`product`（ProductServiceImpl/CategoryServiceImpl→CachePort）、`order`（OrderServiceImpl→MessageBusPort）、`ai`（SseUtils）、`analytics`（间接）

---

## 3. 模块依赖矩阵

图例：`S`=Service 注入  `M`=Mapper 调用  `E`=Entity 引用  `P`=Port/shared.kernel  `I`=基础设施(cache/security)

> 行 = 依赖方，列 = 被依赖方。`—` 表示无依赖或自身。

| 依赖方 ＼ 被依赖方 | IDN | PRD | INV | CRT | ORD | PAY | CUP | SKL | REV | STA | SYS | UPL | AI | ANA | SK |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **IDN** identity | — | S,E | — | — | — | — | — | — | — | — | — | S | — | — | P |
| **PRD** product | — | — | — | — | — | — | — | — | — | — | — | — | — | — | P |
| **INV** inventory | — | M,E | — | — | — | — | — | — | — | — | — | — | — | — | — |
| **CRT** cart | — | S,E | — | — | — | — | — | — | — | — | — | — | — | — | — |
| **ORD** order | S,E | S,E | S | S,E | — | S | S | S,E | — | — | — | S | — | — | P |
| **PAY** payment | S,E | — | — | — | S,E | — | — | S,E | — | — | — | — | — | — | — |
| **CUP** coupon | S,E | S,E | — | — | — | — | — | — | — | — | — | — | — | — | — |
| **SKL** seckill | S,I | S,E | — | — | — | S | — | — | — | — | — | S | — | — | I |
| **REV** review | S,E | M,E | — | — | S | — | — | S | — | — | — | — | — | — | — |
| **STA** stats | S | S | — | — | — | — | — | S | — | — | — | — | — | — | — |
| **SYS** system | M | — | — | — | — | — | — | S | — | — | — | — | — | — | — |
| **UPL** upload | — | — | — | — | — | — | — | — | — | — | — | — | — | — | — |
| **AI** ai | I | S | — | — | S | — | — | — | — | — | — | — | — | — | P |
| **ANA** analytics | I | — | — | — | — | — | — | — | — | — | — | — | — | — | — |
| **SK** shared.kernel | — | — | — | — | — | — | — | — | — | — | — | — | — | — | — |

**依赖密度排名（被依赖次数，含 S/M/E）**：
1. **PRD product** — 被 9 个模块依赖（IDN/INV/CRT/ORD/CUP/SKL/REV/STA/AI）
2. **IDN identity** — 被 8 个模块依赖（ORD/PAY/CUP/SKL/REV/STA/SYS/AI/ANA）
3. **SKL seckill** — 被 5 个模块依赖（ORD/PAY/REV/STA/SYS）
4. **ORD order** — 被 3 个模块依赖（PAY/REV/AI）
5. **PAY payment** — 被 2 个模块依赖（ORD/SKL）
6. **CRT/CUP/INV** — 各被 1 个模块依赖（均被 ORD 依赖）

---

## 4. 迁移风险表

| 模块 | 当前代码位置 | 目标位置 | 风险等级 | 风险说明 |
|---|---|---|---|---|
| identity | `controller/service/entity/mapper/security/converter` 散落 | `com.seckill.mall.identity.{api,app,domain,infra,security}` | **高** | 被依赖最多（8+ 模块）；security 包与 UserMapper 强耦合（5 处）；UserFavorite 跨模块依赖 product；迁移会牵一发动全身 |
| product | `controller/service/entity/mapper/util` 散落 | `com.seckill.mall.product.{api,app,domain,infra}` | **高** | 被依赖最多（9 模块）；Category 与 Product 互依；ProductServiceImpl 跨模块调 CategoryMapper；是基础模块，Public API 设计需极其谨慎 |
| inventory | `service/impl/InventoryServiceImpl`（无独立 Entity/Mapper） | `com.seckill.mall.inventory.{app,domain}` | **中** | 无独立持久层，直接复用 product 的 Mapper/Entity；需决定是独立模块还是 product 子域；迁移需引入 inventory 专属端口 |
| cart | `controller/service/entity/mapper` 散落 | `com.seckill.mall.cart.{api,app,domain,infra}` | **中** | 依赖 product（Service+Entity）；被 order 依赖（Cart entity 被直接引用）；需为 order 提供 CheckoutItem DTO 替代 Cart entity 泄漏 |
| order | `controller/service/entity/mapper/mq` 散落 | `com.seckill.mall.order.{api,app,domain,infra,mq}` | **高** | 依赖 7 个模块（cart/coupon/inventory/product/identity/payment/seckill）；是核心交易链路；OrderQuery 引用 SeckillOrder entity 跨模块；AdminOrder 依赖 SeckillOrderService |
| payment | `controller/service/entity/mapper/converter` 散落 | `com.seckill.mall.payment.{api,app,domain,infra}` | **中** | WalletServiceImpl 聚合 order/seckill/identity 三模块数据（NormalOrder/SeckillOrder/User entity）；需通过应用服务聚合而非直接引用 entity |
| coupon | `controller/service/entity/mapper` 散落 | `com.seckill.mall.coupon.{api,app,domain,infra}` | **中** | 依赖 product（Category/Product entity）+ identity（User entity）；CouponService 持有过多 entity 引用 |
| seckill | `controller/service/entity/mapper/mq/scheduler/converter/cache` 散落 | `com.seckill.mall.seckill.{api,app,domain,infra,mq,scheduler}` | **高** | 依赖 product/identity/payment；MQ Consumer 跨模块注入 ProductMapper/UserMapper；强依赖 cache 基础设施（RedisService/SeckillLuaService）而非 shared.kernel.Port；已有 SeckillInventoryPort 可演进 |
| review | `controller/service/entity/mapper` 散落 | `com.seckill.mall.review.{api,app,domain,infra}` | **中** | 跨模块注入 ProductMapper/ProductSkuMapper（2 处）；依赖 order/seckill/identity Service；需用 product 模块 Public API 替代直接 Mapper |
| stats | `controller/service/vo` 散落 | `com.seckill.mall.stats.{api,app}` | **低** | 纯聚合查询，无独立持久层；依赖 product/seckill/identity Service（可通过 facade 收敛）；VO 命名与 seckill 耦合（SeckillOverviewVO/SeckillRankingVO） |
| system | `controller/service/entity/mapper/aspect/annotation` 散落 | `com.seckill.mall.system.{api,app,domain,infra}` | **低** | 依赖 seckill（SeckillOrderService）；OperationLogAspect 跨模块注入 UserMapper；aspect/annotation 横切，迁移需保留横切能力 |
| upload | `controller/service/config` 散落 | `com.seckill.mall.upload.{api,app,infra}` 或归入 `shared.infrastructure` | **低** | 无业务依赖，被依赖少；StorageService 双实现（Local/MinIO）已是策略模式；EmailService 可考虑归入 shared.infrastructure |
| ai | `ai.*`（已模块化） | 维持现状，收敛 Public API | **低** | 已按子模块组织；ProductSearchTool/AgentService 跨模块调 ProductService/OrderQueryService，需通过模块 facade 收敛 |
| analytics.tracking | `analytics.tracking.*`（已模块化） | 维持现状 | **低** | 已自包含，仅依赖 SecurityUtils |
| shared.kernel | `shared.kernel.*`（已模块化） | 扩展 Port 覆盖面 | **低** | DistributedLockPort 尚无使用者；CachePort 仅 5 处使用、MessageBusPort 仅 1 处；cache 包（RedisService 等）未纳入 Port 抽象，26 处直接依赖 |

---

## 5. 关键发现与建议

### 5.1 跨模块 Mapper 调用情况（共约 12 处）

| 调用方 | 被调用的跨模块 Mapper | 所属模块 → 被依赖模块 | 处置建议 |
|---|---|---|---|
| `CategoryServiceImpl` | `ProductMapper` | product(category) → product | 模块内，可接受 |
| `ProductServiceImpl` | `CategoryMapper` | product → product(category) | 模块内，可接受 |
| `InventoryServiceImpl` | `ProductMapper`, `ProductSkuMapper` | inventory → product ⚠️ | 改为通过 product 模块 Public API |
| `ProductReviewServiceImpl` | `ProductMapper`, `ProductSkuMapper` | review → product ⚠️ | 改为通过 product 模块 Public API |
| `SeckillOrderConsumer` | `ProductMapper`, `UserMapper` | seckill(mq) → product, identity ⚠️ | 改为通过 product/identity facade |
| `SeckillOrderProducer` | `SeckillGoodsMapper` | seckill 内部 | 可接受 |
| `OperationLogAspect` | `UserMapper` | system → identity ⚠️ | 改为通过 CurrentUserContext（已有 shared.kernel） |
| `SecurityUtils` | `UserMapper` | identity(security) → identity | 模块内，可接受 |
| `SecurityUserDetailsService` | `UserMapper` | identity(security) → identity | 模块内，可接受 |
| `JwtAuthenticationFilter` | `UserMapper` | identity(security) → identity | 模块内，可接受 |
| `UserStatusCacheService` | `UserMapper` | identity(security) → identity | 模块内，可接受 |

**真正跨业务模块的 Mapper 调用：4 处**（inventory→product、review→product×2、seckill-mq→product/identity、system→identity），需在 Phase 6 后续阶段通过模块 Public API 消除。

### 5.2 跨模块 Entity 引用情况（共约 22 处）

跨模块直接引用对方 Entity/PO 的典型案例：
- `order` → `cart.Cart`、`product.Product/ProductSku`、`identity.UserAddress`、`seckill.SeckillOrder`
- `payment` → `order.NormalOrder`、`seckill.SeckillOrder`、`identity.User`
- `coupon` → `product.Category/Product`、`identity.User`
- `seckill` → `product.Product`、`identity.User`
- `review` → `product.ProductSku`、`identity.User`
- `identity(UserFavorite)` → `product.Product`
- `cart` → `product.Product/ProductSku`

**建议**：Phase 6 应为每个模块定义 **Public API（Facade/Application Service）**，对外只暴露 DTO/VO，禁止跨模块直接引用 Entity。这是从"传统分层"走向"Modular Monolith"的核心改造点。

### 5.3 Controller 直接依赖 Mapper/RedisTemplate 的情况（共 3 处）

| Controller | 直接依赖 | 问题 | 建议 |
|---|---|---|---|
| `VerificationCodeController` | `StringRedisTemplate`（直接注入 Redis） | Controller 绕过 Service 直接操作基础设施 | 下沉到 VerificationCodeServiceImpl，通过 CachePort 调用 |
| `WalletController` | `entity.User` | Controller 直接引用领域 Entity | 改用 SecurityUtils 获取 userId，不暴露 User entity |
| `AdminUserController` | `entity.enums.UserRole`, `UserStatus` | Controller 引用 Entity 枚举 | 枚举可放 shared.kernel 或模块 API 包，影响较小 |

### 5.4 已有的 shared.kernel Port 使用情况

| Port | 使用者 | 使用次数 | 评价 |
|---|---|---|---|
| `CachePort` | CaptchaService、AuthServiceImpl、CategoryServiceImpl、ProductServiceImpl、VerificationCodeServiceImpl | 5 处 | ✅ 已启用，但覆盖不足 |
| `MessageBusPort` | OrderServiceImpl | 1 处 | ⚠️ 仅 1 处，seckill 等仍直接用 RabbitTemplate/Producer |
| `DistributedLockPort` | （无） | 0 处 | ⚠️ 已定义未使用，seckill 仍直接用 RedissonClient |
| `CurrentUserContext` | SecurityUtils | 1 处 | ✅ 已启用 |
| `SseUtils` | ShoppingAssistantController、CustomerServiceController | 2 处 | ✅ 已启用 |

**关键差距**：`cache` 包（RedisService/RedisKeyConstants/SeckillLuaService/CacheDegradeService）被 **26 处** 直接依赖，未纳入 shared.kernel.Port 抽象。这是 Port 化的最大未覆盖面。`DistributedLockPort` 已定义但 0 使用，seckill 仍直接依赖 `RedissonClient`。

### 5.5 其他架构问题

1. **inventory 无独立持久层**：InventoryService 直接操作 product 的 Mapper/Entity，模块边界模糊。建议要么归并 product，要么定义 InventoryPort 由 product 实现。
2. **WalletService 是"上帝聚合"**：WalletServiceImpl 同时引用 order/seckill/identity 三模块的 Service + Entity，是潜在的 God Service 复发点。
3. **stats/system 无独立持久层**：纯聚合模块，依赖多个模块 Service。建议引入 StatsQueryFacade 统一入口。
4. **seckill 与 cache 基础设施强耦合**：SeckillServiceImpl 直接依赖 4 个 cache 类 + RedissonClient + ObjectMapper，未通过 Port 抽象。DistributedLockPort 已存在却未使用。
5. **MQ Consumer 跨模块 Mapper**：SeckillOrderConsumer 直接注入 ProductMapper/UserMapper，是模块边界最严重的破坏点之一。
6. **VO 跨模块命名耦合**：`SeckillOverviewVO`、`SeckillRankingVO` 被 stats 模块使用但命名属 seckill；`OrderTrendVO`、`OrderStatusDistributionVO` 命名属 order 但被 system 使用。建议各模块自有 VO，跨模块用 DTO 传递。
7. **security 包归属**：security 既含横切过滤器，又含 UserStatusCacheService（业务缓存），与 identity 边界模糊。建议 security 拆分为 `identity.security`（认证）+ `shared.security`（通用过滤器）。
8. **EmailService 归属**：EmailService 被多个模块依赖（order/seckill），更像共享基础设施，建议归入 `shared.infrastructure` 或 `system.notification`。

### 5.6 Phase 6 后续阶段建议优先级

| 优先级 | 改造项 | 涉及模块 |
|---|---|---|
| P0 | 为 product / identity 定义 Public API（Facade + DTO），消除跨模块 Entity 泄漏 | product, identity |
| P0 | 消除 4 处真正跨模块 Mapper 调用（inventory/review/seckill-mq/system） | inventory, review, seckill, system |
| P1 | 将 `cache` 包纳入 shared.kernel.Port 抽象，收敛 26 处直接依赖 | shared.kernel, seckill, ai, analytics |
| P1 | 启用 DistributedLockPort 替代 seckill 直接依赖 RedissonClient | seckill, shared.kernel |
| P1 | 下沉 VerificationCodeController 的 StringRedisTemplate 到 Service | identity |
| P2 | 拆分 security 包为 identity.security + shared.security | identity, shared |
| P2 | 收敛 WalletService 的多模块聚合，改为通过各模块 facade | payment |
| P2 | 为 order/seckill 定义模块边界，隔离核心交易链路 | order, seckill |

---

> **本文件由 Phase 6 第一阶段代码扫描生成，未修改任何 .java / pom.xml / 测试文件。**
> **下一步**：基于本模块地图，由架构团队评审模块边界与 Public API 设计，进入 Phase 6 第二阶段（建立模块 Public API）。