# SeckillMall 目标模块设计 (TARGET-MODULE-DESIGN)

> 生成时间：2026-08-17
> 阶段：Phase 6 第二阶段前置设计
> 依据：MODULE-MAP.md + MODULE-CAPABILITY-MAP.md
> 说明：纯架构设计文档，未创建任何代码/接口/包。

---

## 1. 目标架构总览

### 1.1 目标架构风格

**Pragmatic DDD + Modular Monolith**（实用领域驱动设计 + 模块化单体）

- **Modular Monolith**：单 Maven 模块、单部署单元，但内部按业务模块严格划分包边界，模块间通过 Public API 通信。
- **Pragmatic DDD**：每个模块内部采用 DDD 分层（api / application / domain / infrastructure），但不强制完整 DDD 战术模式（不要求所有模块都有聚合根/值对象/领域事件），按模块复杂度务实选择。

### 1.2 模块内部分层（每个模块的标准结构）

```
com.seckill.mall.<module>/
├── api/                  # Public API（接口 + DTO/Command/Result/Snapshot）
│   ├── dto/              # 对外数据契约（DTO/Snapshot/Command/Result）
│   ├── <Module>Api.java  # 对外 API 接口
│   └── <Sub>Api.java     # 子能力 API 接口
├── application/          # 应用服务（用例编排，实现 api 接口）
├── domain/               # 领域模型（Entity/值对象/领域服务/Repository 接口）
├── infrastructure/       # 基础设施（Repository 实现/Mapper/缓存适配/MQ）
└── interfaces/           # 入站适配器（Controller/AdminController/Scheduler/Consumer）
```

### 1.3 模块通信规则

| 规则 | 描述 |
|---|---|
| ✅ **模块间只能通过 `xxx.api` 通信** | 模块 A 调用模块 B，只能依赖 `B.api` 包下的接口和 DTO |
| ❌ **禁止访问 `xxx.infrastructure`** | 其他模块不可访问对方的 Mapper/Repository 实现/缓存适配 |
| ❌ **禁止访问 `xxx.domain`** | 其他模块不可访问对方的 Entity/值对象/领域服务 |
| ❌ **禁止 API 暴露 Entity/Mapper/PO** | API 的入参用 Command/Query，出参用 Result/DTO/Snapshot |
| ✅ **shared.kernel 是共享底座** | 所有模块可依赖 shared.kernel 的 Port（CachePort/MessageBusPort/DistributedLockPort/CurrentUserContext） |

### 1.4 依赖方向总原则

**单向无环**：模块依赖关系构成有向无环图（DAG）。当前存在的 `order ↔ payment` 双向依赖必须在目标设计中打破为单向。

---

## 2. 模块设计详情

### 2.1 identity

#### 模块职责
管理用户身份、认证、资料、收货地址、收藏与验证码；提供 JWT 安全基础设施与当前用户上下文。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `UserApi.getUserById(UserId)` | 查询用户信息 | order, payment, coupon, seckill, review, stats | `UserDTO` |
| `UserApi.getCurrentUser()` | 获取当前用户 | 全模块（经 shared.kernel.CurrentUserContext） | `CurrentUser` |
| `AddressApi.listAddresses(UserId)` | 查询收货地址列表 | order | `List<AddressDTO>` |
| `AddressApi.getAddress(UserId, AddressId)` | 查询单个地址 | order | `AddressDTO` |
| `AddressApi.saveAddress(SaveAddressCommand)` | 保存地址 | UserAddressController | `AddressDTO` |
| `AddressApi.deleteAddress(DeleteAddressCommand)` | 删除地址 | UserAddressController | `void` |
| `FavoriteApi.listFavorites(UserId)` | 查询收藏列表 | UserFavoriteController | `List<FavoriteItemDTO>` |
| `FavoriteApi.addFavorite(AddFavoriteCommand)` | 添加收藏 | UserFavoriteController | `void` |
| `FavoriteApi.removeFavorite(RemoveFavoriteCommand)` | 移除收藏 | UserFavoriteController | `void` |
| `AuthApi.register(RegisterCommand)` | 用户注册 | AuthController | `UserId` |
| `AuthApi.login(LoginCommand)` | 登录认证 | AuthController | `LoginResult` |
| `AuthApi.refreshToken(RefreshTokenCommand)` | 刷新令牌 | AuthController | `TokenResult` |
| `AuthApi.changePassword(ChangePasswordCommand)` | 修改密码 | AuthController | `void` |
| `AuthApi.updateProfile(UpdateProfileCommand)` | 更新资料 | UserController | `UserProfile` |
| `AuthApi.sendVerificationCode(SendCodeCommand)` | 发送验证码 | VerificationCodeController | `void` |
| `AuthApi.generateCaptcha()` | 图形验证码 | AuthController | `CaptchaResult` |
| `AdminUserApi.listUsers(UserListQuery)` | 管理员用户列表 | AdminUserController | `PageResult<UserDTO>` |
| `AdminUserApi.updateUserStatus(UpdateUserStatusCommand)` | 更新用户状态 | AdminUserController | `void` |
| `AdminUserApi.updateUserRole(UpdateUserRoleCommand)` | 更新用户角色 | AdminUserController | `void` |

#### 允许依赖
- `shared.kernel`（CachePort、CurrentUserContext）
- `product.api.ProductApi`（UserFavorite 查商品快照）
- `upload.api.UploadApi`（头像上传）

#### 禁止依赖
- 其他模块的 `infrastructure` / `mapper` / `domain`（Entity）
- 直接依赖 `RedisTemplate`（应通过 `CachePort`）
- `order` / `payment` / `coupon` / `seckill` 等业务模块的任何包

---

### 2.2 product

#### 模块职责
管理商品 SPU/SKU、分类及属性、商品属性值、首页 Banner 与**商品库存**（inventory 作为 product 子域）。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `ProductApi.getProductById(ProductId)` | 查询商品详情 | cart, order, coupon, seckill, review, stats, ai | `ProductSnapshot` |
| `ProductApi.searchProducts(ProductQuery)` | 搜索商品 | ai, stats | `PageResult<ProductSnapshot>` |
| `ProductApi.getStock(SkuId)` | 查询库存 | order | `StockInfo` |
| `SkuApi.getSkuById(SkuId)` | 查询 SKU | cart, order, review | `SkuSnapshot` |
| `SkuApi.listSkusByProductId(ProductId)` | 查询商品 SKU 列表 | cart, order | `List<SkuSnapshot>` |
| `CategoryApi.getCategoryById(CategoryId)` | 查询分类 | coupon | `CategoryDTO` |
| `CategoryApi.listCategories()` | 分类列表 | 前端 | `List<CategoryDTO>` |
| `CategoryApi.listCategoryAttributes(CategoryId)` | 分类属性 | 前端 | `List<CategoryAttributeDTO>` |
| `BannerApi.listBanners()` | Banner 列表 | 前端 | `List<BannerDTO>` |
| `InventoryApi.deduct(DeductStockCommand)` | 扣减库存 | order | `DeductResult` |
| `InventoryApi.restore(RestoreStockCommand)` | 回补库存 | order | `void` |
| `AdminProductApi.createProduct(CreateProductCommand)` | 创建商品 | AdminController | `ProductId` |
| `AdminProductApi.updateProduct(UpdateProductCommand)` | 更新商品 | AdminController | `void` |
| `AdminProductApi.updateProductStatus(UpdateProductStatusCommand)` | 上下架 | AdminController | `void` |
| `AdminProductApi.generateSkus(SkuGenerateCommand)` | SKU 生成 | AdminController | `List<SkuDTO>` |

#### 允许依赖
- `shared.kernel`（CachePort）

#### 禁止依赖
- 任何业务模块的 `api` / `infrastructure` / `domain`（product 是基础模块，不依赖其他业务模块）
- 直接依赖 `RedisTemplate`（应通过 `CachePort`）

---

### 2.3 inventory

> **设计决策：inventory 作为 product 子域**（详见第 6 节决策 1）。不独立成模块，库存能力通过 `product.api.InventoryApi` 暴露。此处仅记录其归属。

#### 模块职责
商品库存的查询、扣减与回补——归属 product 模块子域（`product.inventory`）。

#### Public API 边界
见 `product.api.InventoryApi`（已并入 product 的 API 边界表）。

#### 允许依赖
- `product.domain` / `product.infrastructure`（模块内部）

#### 禁止依赖
- 任何其他业务模块

---

### 2.4 cart

#### 模块职责
管理用户购物车条目与结算预览。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `CartApi.listCart(UserId)` | 查询购物车 | CartController | `List<CartItemDTO>` |
| `CartApi.addToCart(AddToCartCommand)` | 添加购物车 | CartController | `void` |
| `CartApi.updateCartItem(UpdateCartItemCommand)` | 更新购物车项 | CartController | `void` |
| `CartApi.removeCartItem(RemoveCartItemCommand)` | 删除购物车项 | CartController | `void` |
| `CartApi.prepareCheckout(CheckoutCommand)` | 结算预览 | order | `CheckoutPreview` |

#### 允许依赖
- `shared.kernel`
- `product.api.ProductApi` / `product.api.SkuApi`（查商品快照与库存）

#### 禁止依赖
- `order` / 其他业务模块的任何包
- 直接引用 `product.domain.Product` / `ProductSku`（Entity 泄漏）

---

### 2.5 order

#### 模块职责
管理普通订单的全生命周期（创建、查询、支付、发货、取消、完成）与管理员订单查询。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `OrderApi.createOrder(CreateOrderCommand)` | 创建订单 | OrderController | `OrderCreateResult` |
| `OrderApi.buyNow(BuyNowCommand)` | 立即购买 | OrderController | `OrderCreateResult` |
| `OrderApi.listOrders(OrderListQuery)` | 订单列表 | OrderController, ai | `PageResult<OrderListItemDTO>` |
| `OrderApi.getOrderDetail(OrderId)` | 订单详情 | OrderController | `OrderDetailDTO` |
| `OrderApi.payOrder(PayOrderCommand)` | 支付订单 | OrderController | `PayResult` |
| `OrderApi.cancelOrder(CancelOrderCommand)` | 取消订单 | OrderController, payment | `void` |
| `OrderApi.ship(ShipCommand)` | 发货 | AdminOrderController | `void` |
| `OrderApi.confirmReceipt(ConfirmReceiptCommand)` | 确认收货 | OrderController | `void` |
| `OrderApi.queryUserPurchased(UserId, ProductId)` | 查询用户是否购买 | review | `boolean` |
| `OrderApi.getStatusDistribution()` | 订单状态分布 | stats, system | `List<OrderStatusDistributionDTO>` |
| `OrderApi.getOrderTrend()` | 订单趋势 | stats | `List<TrendDTO>` |
| `AdminOrderApi.adminListOrders(AdminOrderQuery)` | 管理员订单列表 | AdminOrderController | `PageResult<AdminOrderDTO>` |

#### 允许依赖
- `shared.kernel`（MessageBusPort、CurrentUserContext）
- `identity.api.UserApi` / `identity.api.AddressApi`
- `product.api.ProductApi` / `product.api.SkuApi` / `product.api.InventoryApi`
- `cart.api.CartApi`（结算预览）
- `coupon.api.CouponApi`（核销/退还）
- `payment.api.PaymentApi`（发起支付）
- `seckill.api.SeckillOrderApi`（查询秒杀订单）
- `upload.api.EmailApi`（订单通知邮件）

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 直接引用 `cart.Cart` / `product.Product` / `identity.UserAddress` / `seckill.SeckillOrder`（Entity 泄漏）
- `payment` 反向调用 order 的内部（依赖方向单向：order → payment.api）

---

### 2.6 payment

#### 模块职责
提供支付能力、用户钱包余额与流水管理、充值卡生成与核销。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `PaymentApi.pay(PayCommand)` | 发起支付 | order, seckill | `PayResult` |
| `PaymentApi.getPaymentStatus(PaymentId)` | 查询支付状态 | order | `PaymentStatus` |
| `PaymentApi.recordPayment(RecordPaymentCommand)` | 记录支付结果（order 推送） | order | `void` |
| `WalletApi.getWalletBalance(UserId)` | 钱包余额 | WalletController | `WalletBalance` |
| `WalletApi.listWalletRecords(WalletRecordQuery)` | 钱包流水 | WalletController | `PageResult<WalletRecordDTO>` |
| `WalletApi.rechargeWallet(RechargeWalletCommand)` | 钱包充值 | WalletController | `void` |
| `RechargeCardApi.generateCards(GenerateCardsCommand)` | 充值卡生成 | AdminController | `List<RechargeCardDTO>` |
| `RechargeCardApi.redeemCard(RedeemCardCommand)` | 充值卡核销 | WalletController | `RedeemResult` |

#### 允许依赖
- `shared.kernel`
- `identity.api.UserApi`（查用户）
- `seckill.api.SeckillOrderApi`（查秒杀订单用于流水）

#### 禁止依赖
- **`order` 的任何包**（打破 order↔payment 环，payment 不反查 order，由 order 推送 `recordPayment` 含订单快照）
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 直接引用 `order.NormalOrder` / `seckill.SeckillOrder` / `identity.User`（Entity 泄漏）

---

### 2.7 coupon

#### 模块职责
管理优惠券模板、用户领券用券与优惠券核销退还。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `CouponApi.listAvailableCoupons(AvailableCouponQuery)` | 查询可用优惠券 | CouponController, order | `List<CouponDTO>` |
| `CouponApi.claimCoupon(ClaimCouponCommand)` | 领取优惠券 | CouponController | `void` |
| `CouponApi.useCoupon(UseCouponCommand)` | 核销优惠券 | order | `CouponUseResult` |
| `CouponApi.returnCoupon(ReturnCouponCommand)` | 退还优惠券 | order | `void` |
| `AdminCouponApi.listCoupons(CouponQuery)` | 优惠券模板列表 | AdminCouponController | `PageResult<CouponDTO>` |
| `AdminCouponApi.createCoupon(CreateCouponCommand)` | 创建优惠券 | AdminCouponController | `void` |

#### 允许依赖
- `shared.kernel`
- `identity.api.UserApi`
- `product.api.CategoryApi` / `product.api.ProductApi`

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 直接引用 `product.Category` / `product.Product` / `identity.User`（Entity 泄漏）

---

### 2.8 seckill

#### 模块职责
管理秒杀活动、秒杀商品、秒杀下单（令牌+Lua 原子扣减+MQ 异步落单）、秒杀订单查询与库存回补、状态补偿调度。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `SeckillApi.getActivity(ActivityId)` | 查询秒杀活动 | SeckillController | `SeckillActivityDTO` |
| `SeckillApi.listActivities()` | 活动列表 | SeckillController | `List<SeckillActivityDTO>` |
| `SeckillApi.getSeckillGoods(SeckillId)` | 查询秒杀商品 | SeckillController, stats | `SeckillGoodsDTO` |
| `SeckillApi.listSeckillGoods(SeckillGoodsQuery)` | 秒杀商品列表 | SeckillController | `PageResult<SeckillGoodsDTO>` |
| `SeckillApi.executeSeckill(SeckillCommand)` | 秒杀下单 | SeckillController | `SeckillResult` |
| `SeckillApi.getOverview()` | 秒杀概览 | stats | `SeckillOverviewDTO` |
| `SeckillApi.getRanking()` | 秒杀排行 | stats | `List<SeckillRankingDTO>` |
| `SeckillOrderApi.getSeckillOrder(OrderId)` | 查询秒杀订单 | order, payment, review, system | `SeckillOrderDTO` |
| `SeckillOrderApi.listSeckillOrders(SeckillOrderQuery)` | 秒杀订单列表 | order | `PageResult<SeckillOrderDTO>` |
| `SeckillOrderApi.queryUserPurchased(UserId, SeckillId)` | 查询用户秒杀购买 | review | `boolean` |
| `AdminSeckillApi.createActivity(CreateActivityCommand)` | 创建活动 | SeckillController | `void` |
| `AdminSeckillApi.createSeckillGoods(CreateSeckillGoodsCommand)` | 创建秒杀商品 | SeckillController | `void` |

#### 允许依赖
- `shared.kernel`（CachePort、DistributedLockPort、MessageBusPort、CurrentUserContext）
- `identity.api.UserApi`
- `product.api.ProductApi`
- `payment.api.PaymentApi`
- `upload.api.EmailApi`

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 直接依赖 `RedissonClient` / `RedisService`（应通过 `DistributedLockPort` / `CachePort`）
- MQ Consumer 直接注入 `product.infrastructure.ProductMapper` / `identity.infrastructure.UserMapper`
- 直接引用 `product.Product` / `identity.User`（Entity 泄漏）

---

### 2.9 review

#### 模块职责
管理商品评价的提交、查询与管理员审核。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `ReviewApi.submitReview(SubmitReviewCommand)` | 提交评价 | ProductReviewController | `void` |
| `ReviewApi.listReviews(ReviewQuery)` | 查询商品评价 | ProductReviewController | `PageResult<ReviewDTO>` |
| `AdminReviewApi.auditReview(AuditReviewCommand)` | 审核评价 | AdminReviewController | `void` |

#### 允许依赖
- `shared.kernel`（CurrentUserContext）
- `identity.api.UserApi`
- `product.api.ProductApi` / `product.api.SkuApi`
- `order.api.OrderApi`（查询用户购买记录）
- `seckill.api.SeckillOrderApi`（查询用户秒杀购买记录）

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 直接注入 `product.infrastructure.ProductMapper` / `ProductSkuMapper`（跨模块 Mapper）
- 直接引用 `product.ProductSku` / `identity.User`（Entity 泄漏）

---

### 2.10 stats

#### 模块职责
提供首页统计概览、秒杀排行、订单趋势与订单状态分布的聚合查询。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `StatsApi.getOverview()` | 统计概览 | StatsController | `StatsOverviewDTO` |
| `StatsApi.getOrderTrend()` | 订单趋势 | StatsController | `List<TrendDTO>` |
| `StatsApi.getSeckillRanking()` | 秒杀排行 | StatsController | `List<SeckillRankingDTO>` |
| `StatsApi.getOrderStatusDistribution()` | 订单状态分布 | StatsController | `List<OrderStatusDistributionDTO>` |

#### 允许依赖
- `shared.kernel`
- `identity.api.UserApi`
- `product.api.ProductApi`
- `seckill.api.SeckillApi`
- `order.api.OrderApi`

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 自有 VO 命名耦合其他模块（`SeckillOverviewVO` 应改为 `seckill.api.dto.SeckillOverviewDTO`）

---

### 2.11 system

#### 模块职责
提供系统健康监控、操作日志与登录日志查询；提供操作日志横切注解驱动。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `SystemApi.getHealth()` | 系统健康 | SystemController | `SystemHealthDTO` |
| `SystemApi.listOperationLogs(OperationLogQuery)` | 操作日志 | SystemController | `PageResult<OperationLogDTO>` |
| `SystemApi.listLoginLogs(LoginLogQuery)` | 登录日志 | SystemController | `PageResult<LoginLogDTO>` |

#### 允许依赖
- `shared.kernel`（CurrentUserContext）
- `seckill.api.SeckillOrderApi`

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- `OperationLogAspect` 直接注入 `identity.infrastructure.UserMapper`（改用 `CurrentUserContext`）
- 直接引用 `identity.User`（Entity 泄漏）

---

### 2.12 upload

#### 模块职责
提供文件上传（本地/MinIO 双策略）与邮件发送能力。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `UploadApi.upload(UploadCommand)` | 文件上传 | UploadController, identity | `UploadResult` |
| `EmailApi.sendEmail(SendEmailCommand)` | 发送邮件 | order, seckill | `void` |

#### 允许依赖
- `shared.kernel`

#### 禁止依赖
- 任何业务模块的 `api` / `infrastructure` / `domain`（upload 是底层基础设施模块）
- `StorageService` 实现细节暴露给外部

---

### 2.13 ai

#### 模块职责
提供 AI 网关路由、购物助手、智能客服与 AIGC 内容生成能力。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `AiGatewayApi.route(AiRequest)` | AI 网关路由 | AiGatewayController | `AiResponse` |
| `AssistantApi.assist(AssistantCommand)` | 购物助手 | ShoppingAssistantController | `AssistantResponse` |
| `CustomerServiceApi.chat(ChatCommand)` | 智能客服 | CustomerServiceController | `CustomerServiceResponse` |
| `AigcApi.generate(AigcCommand)` | AIGC 生成 | AigcController | `AigcResult` |

#### 允许依赖
- `shared.kernel`（CurrentUserContext、SseUtils、CachePort）
- `product.api.ProductApi`
- `order.api.OrderApi`

#### 禁止依赖
- 任何模块的 `infrastructure` / `mapper` / `domain`
- 直接引用 `product.ProductQueryRequest` / `ProductVO` / `order.OrderListItemVO`（改用 `product.api.dto` / `order.api.dto`）

---

### 2.14 analytics.tracking

#### 模块职责
提供用户行为事件采集（注解驱动 + MQ 异步落库）。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `TrackingApi.track(TrackEventCommand)` | 事件追踪 | TrackController, @Tracking 横切 | `void` |

#### 允许依赖
- `shared.kernel`（CurrentUserContext、MessageBusPort、CachePort）

#### 禁止依赖
- 任何业务模块的 `api` / `infrastructure` / `domain`
- 直接依赖 `identity.security.SecurityUtils`（改用 `shared.kernel.CurrentUserContext`）

---

### 2.15 shared.kernel

#### 模块职责
定义跨模块共享的抽象端口（缓存/消息/锁/当前用户）与适配器实现，是所有模块的依赖底座。

#### Public API 边界

| API | 方法签名（建议） | 暴露给 | 返回类型 |
|---|---|---|---|
| `CachePort.get/put/delete/exists` | 缓存操作 | 全模块 | `<T>` |
| `MessageBusPort.publish/subscribe` | 消息总线 | 全模块 | `void` |
| `DistributedLockPort.tryLock/unlock` | 分布式锁 | 全模块 | `boolean` |
| `CurrentUserContext.getCurrentUser()` | 当前用户上下文 | 全模块 | `CurrentUser` |
| `SseUtils.*` | SSE 工具 | ai | — |

#### 允许依赖
- Spring/Redis/RabbitMQ/Redisson 基础设施框架

#### 禁止依赖
- 任何业务模块（shared.kernel 是最底层，不可反向依赖业务模块）

---

## 3. 目标依赖关系

### 3.1 目标依赖矩阵

图例：`A`=API 调用  `P`=Port/shared.kernel  `—`=无依赖

> inventory 已归入 product 子域，不再独立成列。行 = 依赖方，列 = 被依赖方。

| 依赖方 ＼ 被依赖方 | IDN | PRD | CRT | ORD | PAY | CUP | SKL | REV | STA | SYS | UPL | AI | ANA | SK |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **IDN** identity | — | A | — | — | — | — | — | — | — | — | A | — | — | P |
| **PRD** product | — | — | — | — | — | — | — | — | — | — | — | — | — | P |
| **CRT** cart | — | A | — | — | — | — | — | — | — | — | — | — | — | — |
| **ORD** order | A | A | A | — | A | A | A | — | — | — | A | — | — | P |
| **PAY** payment | A | — | — | — | — | — | A | — | — | — | — | — | — | P |
| **CUP** coupon | A | A | — | — | — | — | — | — | — | — | — | — | — | — |
| **SKL** seckill | A | A | — | — | A | — | — | — | — | — | A | — | — | P |
| **REV** review | A | A | — | A | — | — | A | — | — | — | — | — | — | P |
| **STA** stats | A | A | — | A | — | — | A | — | — | — | — | — | — | P |
| **SYS** system | — | — | — | — | — | — | A | — | — | — | — | — | — | P |
| **UPL** upload | — | — | — | — | — | — | — | — | — | — | — | — | — | P |
| **AI** ai | — | A | — | A | — | — | — | — | — | — | — | — | — | P |
| **ANA** analytics | — | — | — | — | — | — | — | — | — | — | — | — | — | P |
| **SK** shared.kernel | — | — | — | — | — | — | — | — | — | — | — | — | — | — |

### 3.2 依赖变化对比

| 模块对 | 当前依赖 | 目标依赖 | 变化 |
|---|---|---|---|
| IDN → PRD | S, E | A | Service+Entity → 仅 API（ProductSnapshot） |
| IDN → UPL | S | A | Service → API |
| INV → PRD | M, E | （归入 PRD） | inventory 合并到 product 子域，消除跨模块 Mapper |
| CRT → PRD | S, E | A | Service+Entity → 仅 API（ProductSnapshot/SkuSnapshot） |
| ORD → IDN | S, E | A | Service+Entity → 仅 API（UserDTO/AddressDTO） |
| ORD → PRD | S, E | A | Service+Entity → 仅 API（ProductSnapshot/SkuSnapshot/InventoryApi） |
| ORD → CRT | S, E | A | Service+Entity → 仅 API（CheckoutPreview） |
| ORD → PAY | S | A | Service → API |
| ORD → CUP | S | A | Service → API |
| ORD → SKL | S, E | A | Service+Entity → 仅 API（SeckillOrderDTO） |
| ORD → UPL | S | A | Service → API（EmailApi） |
| **PAY → ORD** | **S, E** | **—（消除）** | **🔴 关键变化：payment 不再依赖 order，打破环** |
| PAY → IDN | S, E | A | Service+Entity → 仅 API（UserDTO） |
| PAY → SKL | S, E | A | Service+Entity → 仅 API（SeckillOrderDTO） |
| CUP → IDN | S, E | A | Service+Entity → 仅 API（UserDTO） |
| CUP → PRD | S, E | A | Service+Entity → 仅 API（CategoryDTO/ProductSnapshot） |
| SKL → IDN | S, I | A | Service+基础设施 → 仅 API + Port（CurrentUserContext） |
| SKL → PRD | S, E | A | Service+Entity → 仅 API（ProductSnapshot） |
| SKL → PAY | S | A | Service → API |
| SKL → UPL | S | A | Service → API（EmailApi） |
| SKL → cache | I | P | 基础设施直接依赖 → Port（CachePort/DistributedLockPort） |
| REV → IDN | S, E | A | Service+Entity → 仅 API |
| REV → PRD | M, E | A | **Mapper+Entity → 仅 API**（消除跨模块 Mapper） |
| REV → ORD | S | A | Service → API |
| REV → SKL | S | A | Service → API |
| STA → IDN | S | A | Service → API |
| STA → PRD | S | A | Service → API |
| STA → SKL | S | A | Service → API |
| STA → ORD | — | A | 新增：订单趋势/状态分布通过 order.api |
| SYS → IDN | M | —（消除） | **Mapper → 消除**（改用 CurrentUserContext） |
| SYS → SKL | S | A | Service → API |
| AI → IDN | I | P | 基础设施 → Port（CurrentUserContext） |
| AI → PRD | S | A | Service → API |
| AI → ORD | S | A | Service → API |
| AI → cache | I | P | 基础设施 → Port |
| ANA → IDN | I | P | 基础设施 → Port（CurrentUserContext） |
| ANA → cache | I | P | 基础设施 → Port |

**变化统计**：
- 消除跨模块 Mapper 调用：**4 处**（INV→PRD 归并、REV→PRD、SKL-MQ→PRD/IDN、SYS→IDN）
- 消除跨模块 Entity 引用：**22 处** → 全部转为 DTO/Snapshot
- 消除循环依赖：**1 处**（PAY→ORD 消除）
- 基础设施直接依赖 → Port：**28 处**（cache 26 处 + SecurityUtils 2 处）

### 3.3 目标依赖图（文字描述）

目标依赖关系构成**有向无环图（DAG）**，层次如下：

```
第 0 层（底座）：shared.kernel
第 1 层（基础）：product, upload
第 2 层：identity（→product, upload）, cart（→product）, coupon（→product）
第 3 层：payment（→identity, seckill）  [注：payment→seckill 见下方说明]
第 4 层：seckill（→identity, product, payment, upload）
第 5 层：order（→identity, product, cart, coupon, payment, seckill, upload）
第 6 层（终端）：review（→identity, product, order, seckill）
                 stats（→identity, product, seckill, order）
                 system（→seckill）
                 ai（→product, order）
                 analytics（→shared.kernel）
```

**关于 payment 与 seckill 的互相依赖**：
- `seckill → payment`（秒杀下单调支付）
- `payment → seckill`（钱包流水查秒杀订单）

这构成 `seckill ↔ payment` 的双向依赖。处理方式与 order↔payment 类似：**payment 不主动查 seckill，由 seckill 支付成功后推送 `payment.api.PaymentApi.recordPayment()` 含订单快照**。这样 `payment → seckill` 消除，变为 `seckill → payment` 单向。

修正后的层次：
```
第 0 层：shared.kernel
第 1 层：product, upload
第 2 层：identity, cart, coupon
第 3 层：payment（→identity）  [不再→seckill]
第 4 层：seckill（→identity, product, payment, upload）
第 5 层：order（→identity, product, cart, coupon, payment, seckill, upload）
第 6 层：review, stats, system, ai, analytics
```

### 3.4 循环依赖处理

#### 环 1：order ↔ payment

| 方向 | 当前 | 目标 | 处理方式 |
|---|---|---|---|
| order → payment | order 调 PaymentService 发起支付 | order → payment.api.PaymentApi.pay() | ✅ 保留（单向） |
| payment → order | WalletService 查 OrderService + NormalOrder entity | **消除** | order 支付成功后调 `payment.api.recordPayment(RecordPaymentCommand)` 推送支付结果（含 OrderSnapshot），payment 本地存储快照，查询流水用本地数据 |

**结果**：order → payment 单向，无环。

#### 环 2：seckill ↔ payment

| 方向 | 当前 | 目标 | 处理方式 |
|---|---|---|---|
| seckill → payment | seckill 调 PaymentService 发起支付 | seckill → payment.api.PaymentApi.pay() | ✅ 保留（单向） |
| payment → seckill | WalletService 查 SeckillOrderService + SeckillOrder entity | **消除** | seckill 支付成功后调 `payment.api.recordPayment()` 推送（含 SeckillOrderSnapshot），payment 本地存储 |

**结果**：seckill → payment 单向，无环。

#### 无环验证

修正后所有依赖方向：
- IDN → PRD, UPL, SK
- PRD → SK
- CRT → PRD
- CUP → IDN, PRD
- PAY → IDN, SK（仅 Port，无业务模块）
- SKL → IDN, PRD, PAY, UPL, SK
- ORD → IDN, PRD, CRT, CUP, PAY, SKL, UPL, SK
- REV → IDN, PRD, ORD, SKL, SK
- STA → IDN, PRD, SKL, ORD, SK
- SYS → SKL, SK
- AI → PRD, ORD, SK
- ANA → SK

拓扑排序验证（从底到顶）：SK → PRD/UPL → IDN/CRT/CUP → PAY → SKL → ORD → REV/STA/SYS/AI/ANA。**无回边，确认无环** ✅

---

## 4. 禁止依赖规则（全局）

以下规则将作为 ArchUnit 架构测试的基础：

### 规则 1：任何模块禁止访问其他模块的 infrastructure 包
```java
// ArchUnit 伪代码
for each module M in {identity, product, cart, order, payment, coupon, seckill, review, stats, system, upload}:
    classes().that().resideInAPackage("..mall.<M>..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..mall.<M>..", "..mall.shared.kernel..", "..mall.<其他>.api..", "java..", "org..")
// 即：模块 M 的代码只能依赖自身包 + shared.kernel + 其他模块的 api 包
```

### 规则 2：任何模块禁止访问其他模块的 mapper 包
```java
classes().that().resideInAPackage("..mall..")
    .and().resideOutsideOfPackage("..mall.<M>..")  // 不属于模块 M
    .should().notDependOnClassesThat()
    .resideInAPackage("..mall.<M>.infrastructure..")  // 不依赖 M 的基础设施
// 等价：mapper 只能被同模块的 infrastructure/repository 访问
```

### 规则 3：任何模块禁止访问其他模块的 domain（entity）包
```java
classes().that().resideInAPackage("..mall..")
    .and().resideOutsideOfPackage("..mall.<M>..")
    .should().notDependOnClassesThat()
    .resideInAPackage("..mall.<M>.domain..")
// 即：Entity/领域模型只在模块内部可见
```

### 规则 4：Controller 禁止依赖 Mapper/RedisTemplate/RabbitTemplate
```java
classes().that().resideInAPackage("..mall.<M>.interfaces..")
    .should().notDependOnClassesThat()
    .resideInAnyPackage("..mall.<M>.infrastructure.mapper..",
                        "org.springframework.data.redis..",
                        "org.springframework.amqp..")
// Controller 只能依赖 application/api，不碰基础设施
```

### 规则 5：API 禁止暴露 Entity/Mapper/PO
```java
classes().that().resideInAPackage("..mall.<M>.api..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..mall.<M>.api.dto..", "java..", "java.time..")
// API 接口的方法签名只能用 DTO/Command/Result/基本类型
```

### 规则 6：模块间通信只能通过 api 包
```java
classes().that().resideInAPackage("..mall.<M>..")
    .and().resideOutsideOfPackage("..mall.<M>.api..")  // 非 API 定义处
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..mall.<M>..", "..mall.shared.kernel..", "..mall.<其他>.api..")
// 模块内部代码依赖其他模块时，只能引用 other.api
```

### 规则 7：shared.kernel 禁止依赖任何业务模块
```java
classes().that().resideInAPackage("..mall.shared.kernel..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..mall.shared.kernel..", "java..", "org..")
// 共享内核是最底层，不反向依赖业务
```

### 规则 8：禁止循环依赖（模块级）
```java
// 用 ArchUnit 的 SlicesRuleDefinition 检查模块间无环
SlicesRuleDefinition.slices()
    .matching("..mall.(*)..")
    .should().beFreeOfCycles()
```

---

## 5. 允许依赖规则（全局）

### 规则 1：任何模块 → shared.kernel
```java
classes().that().resideInAPackage("..mall.<M>..")
    .mayDependOnClassesThat().resideInAPackage("..mall.shared.kernel..")
// 所有模块可使用 Port/CurrentUserContext
```

### 规则 2：module.application → module.api（自身）
```java
classes().that().resideInAPackage("..mall.<M>.application..")
    .should().implement().resideInAPackage("..mall.<M>.api..")
// 应用服务实现自身 API 接口
```

### 规则 3：module → other.module.api
```java
classes().that().resideInAPackage("..mall.<M>..")
    .mayDependOnClassesThat().resideInAPackage("..mall.<N>.api..")  // N ≠ M
// 跨模块只依赖对方的 API 接口与 DTO
```

### 规则 4：module.interfaces → module.application
```java
classes().that().resideInAPackage("..mall.<M>.interfaces..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..mall.<M>.application..", "..mall.<M>.api..", "..mall.shared.kernel..")
// Controller 只调应用服务
```

### 规则 5：module.application → module.domain（自身）
```java
classes().that().resideInAPackage("..mall.<M>.application..")
    .mayDependOnClassesThat().resideInAPackage("..mall.<M>.domain..")
// 应用服务编排领域模型
```

### 规则 6：module.infrastructure → module.domain（自身，实现 Repository）
```java
classes().that().resideInAPackage("..mall.<M>.infrastructure..")
    .mayDependOnClassesThat().resideInAPackage("..mall.<M>.domain..")
// Repository 实现依赖领域模型接口
```

### 规则 7：module.infrastructure → shared.kernel（实现 Port）
```java
classes().that().resideInAPackage("..mall.<M>.infrastructure..")
    .mayDependOnClassesThat().resideInAPackage("..mall.shared.kernel..")
// 基础设施层可实现 shared.kernel 的 Port
```

---

## 6. 特殊处理决策

### 决策 1：inventory 归属 → **product 子域**

| 选项 | 决策 |
|---|---|
| A. 独立模块 | ❌ |
| **B. product 子域** | ✅ **采纳** |

**理由**：
1. inventory 无独立 Entity/Mapper，当前直接操作 product 的 `ProductMapper`/`ProductSkuMapper`，本质是商品属性的持久化操作。
2. 库存是商品的核心属性，"商品含库存"是更自然的领域建模。
3. 归并后消除 inventory→product 的跨模块 Mapper 调用（4 处之一）。
4. 库存能力通过 `product.api.InventoryApi`（deduct/restore/getStock）暴露给 order，边界清晰。
5. 未来若库存逻辑复杂到需独立（如多仓/分仓），可再拆分为独立模块，Pragmatic DDD 允许演进。

**实现**：`com.seckill.mall.product.inventory` 子包，`InventoryServiceImpl` 归入 `product.application`，通过 `product.api.InventoryApi` 暴露。

---

### 决策 2：upload / EmailService 归属 → **upload 独立模块，EmailService 通过 upload.api.EmailApi 暴露**

| 选项 | 决策 |
|---|---|
| A. upload 独立 + EmailService 归 shared.infrastructure | ❌（引入新概念 shared.infrastructure，增加复杂度） |
| **B. upload 独立，EmailService 归 upload** | ✅ **采纳** |
| C. EmailService 归 system.notification | ❌（system 是运维模块，邮件是通用 IO 能力） |

**理由**：
1. upload 已自包含，无业务依赖，是天然的底层基础设施模块。
2. EmailService 与文件上传同属"外部 IO 通知"能力，归入 upload 模块内聚合理。
3. order/seckill 通过 `upload.api.EmailApi.sendEmail()` 调用，不引入新的 shared.infrastructure 概念。
4. upload 依赖 shared.kernel，被 identity/order/seckill 依赖，依赖方向单向清晰。

---

### 决策 3：security 包归属 → **归入 identity 模块内部（identity.security），SecurityUtils 提升到 shared.kernel.CurrentUserContext**

| 选项 | 决策 |
|---|---|
| **A. security 归 identity 内部 + SecurityUtils→shared.kernel** | ✅ **采纳** |
| B. 拆分为 identity.security + shared.security | ❌（过度拆分，ReplayProtectionFilter/RsaKeyProvider 量少） |
| C. security 独立模块 | ❌（security 与 User 强耦合，独立无意义） |

**理由**：
1. security 的核心是用户认证（JWT/UserDetails/Token），本质属 identity 模块。
2. `SecurityUtils.getCurrentUserId()` 是跨模块高频调用，提升到 `shared.kernel.CurrentUserContext`（已有），所有模块通过 Port 获取当前用户，不直接依赖 identity.security。
3. `JwtAuthenticationFilter`/`SecurityUserDetailsService`/`TokenVersionService`/`TokenBlacklistService`/`UserStatusCacheService` 归入 `identity.infrastructure.security`，模块内部可见。
4. `ReplayProtectionFilter`/`RsaKeyProvider` 归入 `identity.infrastructure.security` 或 `config`，量少不单独拆分。

**实现**：`com.seckill.mall.identity.infrastructure.security` 子包；`SecurityUtils` 的公共能力由 `shared.kernel.CurrentUserContext` 替代。

---

### 决策 4：stats / system 聚合模块的依赖收敛 → **保持独立模块，通过各模块 API 聚合**

| 选项 | 决策 |
|---|---|
| **A. 保持独立，application 层调多模块 API 聚合** | ✅ **采纳** |
| B. 引入 StatsQueryFacade 统一入口 | ❌（Facade 是 ESB 反模式，增加间接层） |
| C. 合并 stats+system | ❌（职责不同：stats 是业务报表，system 是运维） |

**理由**：
1. stats/system 是纯查询聚合模块，无独立持久层，天然依赖多个模块的只读 API。
2. 在 `application` 层直接调用 `product.api`/`seckill.api`/`order.api`/`identity.api` 聚合 DTO，简单直接。
3. 不引入额外 Facade 层（Pragmatic 原则：不过度设计）。
4. stats 的 VO 命名耦合（`SeckillOverviewVO`）通过调用 `seckill.api.SeckillApi.getOverview()` 获取 `SeckillOverviewDTO` 纠正。

---

### 决策 5：WalletService 多模块聚合 → **拆分为查询应用服务 + 充值命令服务，通过 DTO 聚合**

| 选项 | 决策 |
|---|---|
| A. 保持 WalletService 聚合多模块 Entity | ❌（God Service 复发） |
| **B. 拆分为查询+命令服务，通过 API 获取 DTO** | ✅ **采纳** |
| C. 用 CQRS 读写分离 | ❌（过度设计，当前规模不需要） |

**理由**：
1. WalletServiceImpl 当前引用 order/seckill/identity 三模块的 Service + Entity，是 God Service。
2. 拆分为：
   - `WalletQueryApplicationService`：钱包余额/流水查询，通过 `seckill.api.SeckillOrderApi` 获取 SeckillOrderDTO（而非 Entity），流水记录由 order/seckill 推送的支付结果快照生成。
   - `WalletCommandApplicationService`：充值/核销命令处理。
3. payment 不再反查 order/seckill，而是接收 order/seckill 推送的 `RecordPaymentCommand`（含订单快照），本地存储流水。
4. 消除 payment→order、payment→seckill 的 Entity 泄漏，同时打破循环依赖。

---

### 决策 6：order ↔ payment 循环依赖 → **order → payment.api 单向，payment 不依赖 order**

| 选项 | 决策 |
|---|---|
| A. 保持双向，用 API 边界隔离 | ❌（API 双向仍是循环，ArchUnit 会报错） |
| **B. order→payment 单向，order 推送支付结果** | ✅ **采纳** |
| C. 提取支付编排到独立模块 | ❌（过度拆分，支付编排是 order 的职责） |

**理由**：
1. order 调 `payment.api.PaymentApi.pay()` 发起支付（order→payment 单向）。
2. 支付成功后，order 调 `payment.api.PaymentApi.recordPayment(RecordPaymentCommand)` 推送支付结果，`RecordPaymentCommand` 含 `OrderSnapshot`（订单快照）。
3. payment 本地存储支付记录与订单快照，查询流水时用本地数据，不反查 order。
4. 同理处理 seckill↔payment：seckill 支付后推送 `RecordPaymentCommand`（含 `SeckillOrderSnapshot`）。
5. 结果：payment 只依赖 identity.api（查用户），不依赖 order/seckill，循环彻底打破。

**事件时序**：
```
order/seckill → payment.api.pay() → 返回 PayResult
order/seckill → payment.api.recordPayment(含订单快照) → payment 本地存储
wallet 查询 → payment 本地流水（含快照）→ 不反查 order/seckill
```

---

### 决策 7：cache 包 Port 化 → **纳入 shared.kernel.CachePort 扩展，Lua 操作保留 seckill 内部**

| 选项 | 决策 |
|---|---|
| **A. 通用操作纳入 CachePort，特化操作保留模块内部** | ✅ **采纳** |
| B. 全部纳入 shared.kernel | ❌（Lua 脚本是 seckill 特化，不应放共享内核） |
| C. 保持 cache 包独立 | ❌（26 处直接依赖未收敛） |

**理由**：
1. `RedisService` 的通用操作（get/put/delete/expire/incr）纳入 `shared.kernel.CachePort`，收敛 26 处直接依赖。
2. `SeckillLuaService`（Lua 脚本原子扣减/回补）是 seckill 特化能力，归入 `seckill.infrastructure`，不放共享内核。
3. `CacheDegradeService`（降级）作为 `CachePort` 的增强能力，纳入 shared.kernel。
4. `RedisKeyConstants` 各模块自有（如 `seckill.infrastructure.SeckillRedisKeys`），不共享。
5. `SeckillBloomInitializer` 归入 `seckill.infrastructure`。
6. `DistributedLockPort`（已定义 0 使用）替代 seckill 直接依赖 `RedissonClient`。

---

### 决策 8：VO 跨模块命名耦合 → **各模块自有 VO/DTO，跨模块用 DTO 通信**

| 选项 | 决策 |
|---|---|
| **A. VO 归属其业务模块的 api.dto，跨模块用 DTO** | ✅ **采纳** |
| B. 统一放 common.vo | ❌（回到传统分层） |
| C. 保持现状 | ❌（命名耦合未解决） |

**理由**：
1. `SeckillOverviewVO`/`SeckillRankingVO` → 归 `seckill.api.dto.SeckillOverviewDTO`/`SeckillRankingDTO`，stats 通过 `seckill.api.SeckillApi.getOverview()` 获取。
2. `OrderTrendVO`/`OrderStatusDistributionVO` → 归 `order.api.dto`，stats/system 通过 `order.api.OrderApi.getOrderTrend()` 获取。
3. Controller 层的 VO（面向前端）保留在 `module.interfaces.vo`，与 API 的 DTO 分离（VO 可含前端展示字段，DTO 是模块间契约）。
4. 消除命名耦合，每个 VO/DTO 归属其业务模块。

---

## 7. 迁移顺序建议

> **前置基础设施**：shared.kernel 扩展（CachePort 覆盖 cache 包、DistributedLockPort 启用、CurrentUserContext 替代 SecurityUtils）
>
> **业务模块迁移顺序**（order 第一个，作为改造破冰点）：

| 顺序 | 模块 | 理由 | 前置条件 | 风险 |
|---|---|---|---|---|
| **前置** | shared.kernel | 扩展 Port 覆盖面（cache 26 处 + DistributedLockPort），为所有模块提供抽象底座 | 无 | 中 |
| **1** | **order** | 核心交易链路，迁移 order 将驱动其依赖的 7 个模块建立 Public API，是整个改造的破冰点 | product.api/identity.api/cart.api/coupon.api/payment.api/seckill.api 同步定义 | **高** |
| **2** | product | 被依赖最多（9 模块），基础模块；inventory 归并到此 | shared.kernel.CachePort | **高** |
| **3** | identity | 被依赖第二多（8 模块）；security 归并、SecurityUtils→CurrentUserContext | shared.kernel.CurrentUserContext | **高** |
| **4** | cart | Cart entity 泄漏给 order，需提供 CheckoutPreview | product.api | 中 |
| **5** | coupon | 被 order 依赖，核销/退还是核心能力 | product.api/identity.api | 中 |
| **6** | payment | 被 order/seckill 依赖；打破 order↔payment/seckill↔payment 环；WalletService 拆分 | identity.api | 中 |
| **7** | seckill | 被 5 模块依赖；MQ Consumer 跨模块 Mapper 消除；cache→Port | product.api/identity.api/payment.api/shared.kernel.DistributedLockPort | **高** |
| **8** | review | 终端模块，跨模块 Mapper 调 product 消除 | product.api/order.api/seckill.api/identity.api | 中 |
| **9** | stats | 纯聚合，VO 命名耦合纠正 | product.api/seckill.api/order.api/identity.api | 低 |
| **10** | system | OperationLogAspect 跨模块 UserMapper 消除 | seckill.api/shared.kernel.CurrentUserContext | 低 |
| **11** | upload | EmailService 归属决策落地 | shared.kernel | 低 |
| **12** | ai | 收敛 facade，ProductSearchTool/AgentService 通过 API | product.api/order.api/shared.kernel | 低 |
| **13** | analytics | SecurityUtils→CurrentUserContext | shared.kernel | 低 |

**迁移策略说明**：
- **顺序 1（order）与顺序 2-3（product/identity）同步进行**：order 迁移时，其依赖的 product.api/identity.api 需同步定义。order 是"驱动者"，暴露所有需要建立的 API 契约，product/identity 是"被驱动者"，按 order 的需求定义 API。
- **每完成一个模块的迁移，运行 ArchUnit 规则测试**，确保不引入新的违规依赖。
- **迁移完成后，目标依赖矩阵（第 3.1 节）作为 ArchUnit 冻结规则的基准**。

---

> **本文件为 Phase 6 第二阶段前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **本文件与 MODULE-MAP.md、MODULE-CAPABILITY-MAP.md 共同构成 Phase 6 第二阶段的设计依据。**
> **下一步**：基于本目标设计，进入 Phase 6 第二阶段实施——按迁移顺序定义各模块的 Public API 接口与 DTO。