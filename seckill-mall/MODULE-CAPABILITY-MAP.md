# SeckillMall 模块能力映射 (MODULE-CAPABILITY-MAP)

> 生成时间：2026-08-17
> 阶段：Phase 6 第二阶段前置设计
> 依据：MODULE-MAP.md
> 说明：纯架构设计文档，未创建任何代码/接口/包。

---

## 1. 设计原则

本文件为后续 Public API 设计提供能力依据，遵循以下原则：

1. **暴露业务能力，隐藏内部实现**：模块对外只暴露"能做什么"（业务能力），不暴露"怎么做"（Service 实现、Mapper、Entity）。每个业务能力对应一个 API 方法，用业务语言命名。
2. **禁止 Entity 泄漏**：API 的入参用 `Command/Query`，出参用 `Result/DTO/Snapshot`，**严禁**出现 Entity/PO/Mapper。跨模块共享的只读快照用 `XxxSnapshot`，可编辑视图用 `XxxDTO`。
3. **Command/Result/DTO 通信**：模块间通信一律通过值对象，不传递可变领域对象。Command 表意图，Result 表结果，Snapshot 表只读快照。
4. **API 粒度适中**：一个 API 方法对应一个完整的业务用例，不拆成 CRUD 细粒度暴露给外部（CRUD 是内部实现）。管理类能力（Admin）与用户类能力分开标注。
5. **单向依赖无环**：模块 A 调用模块 B 的 API，则 B 不可反向调用 A。当前 `order ↔ payment` 的双向依赖需通过 API 方向调整打破。
6. **Port 归 shared.kernel**：跨模块的技术能力（缓存/消息/锁/当前用户）通过 shared.kernel 的 Port 暴露，不通过具体模块。

---

## 2. 模块能力分析

### 2.1 identity

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 用户注册 | `UserId register(RegisterCommand)` | AuthController |
| 2 | 登录认证 | `LoginResult login(LoginCommand)` | AuthController |
| 3 | 刷新令牌 | `TokenResult refreshToken(RefreshTokenCommand)` | AuthController |
| 4 | 修改密码 | `void changePassword(ChangePasswordCommand)` | AuthController |
| 5 | 更新个人资料 | `UserProfile updateProfile(UpdateProfileCommand)` | UserController |
| 6 | 忘记密码（发送/重置） | `void sendForgotPasswordCode(SendCodeCommand)` / `void resetPassword(ResetPasswordCommand)` | AuthController |
| 7 | 查询用户信息 | `UserProfile getUserById(UserId)` / `UserProfile getCurrentUser()` | 多模块（order/payment/coupon/seckill/review/stats） |
| 8 | 收货地址管理 | `List<AddressDTO> listAddresses(UserId)` / `AddressDTO saveAddress(SaveAddressCommand)` / `void deleteAddress(DeleteAddressCommand)` | UserAddressController + order |
| 9 | 用户收藏管理 | `List<FavoriteItemDTO> listFavorites(UserId)` / `void addFavorite(AddFavoriteCommand)` / `void removeFavorite(RemoveFavoriteCommand)` | UserFavoriteController |
| 10 | 发送验证码 | `void sendVerificationCode(SendCodeCommand)` | VerificationCodeController |
| 11 | 图形验证码 | `CaptchaResult generateCaptcha()` | AuthController |
| 12 | 管理员用户管理 | `PageResult<UserDTO> listUsers(UserListQuery)` / `void updateUserStatus(UpdateUserStatusCommand)` / `void updateUserRole(UpdateUserRoleCommand)` | AdminUserController |
| 13 | 获取当前用户上下文 | `CurrentUser getCurrentUser()` | 全模块（通过 shared.kernel.CurrentUserContext） |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| order | `UserService.getUserById()` + `UserAddressService.*` | `identity.api.UserApi.getUserById()` + `identity.api.AddressApi.*` | User/UserAddress entity 不得泄露 |
| payment | `UserService.getUserById()` | `identity.api.UserApi.getUserById()` | 返回 UserDTO 而非 User |
| coupon | `UserService.getUserById()` | `identity.api.UserApi.getUserById()` | 返回 UserDTO |
| seckill | `UserService.getUserById()` + `SecurityUtils.getCurrentUserId()` | `identity.api.UserApi.getUserById()` + `shared.kernel.CurrentUserContext` | SecurityUtils 归入 shared.kernel |
| review | `UserService.getUserById()` | `identity.api.UserApi.getUserById()` | 返回 UserDTO |
| stats | `UserService.getUserById/count()` | `identity.api.UserApi.*` | 返回 UserDTO |
| system | `UserMapper.selectById()`（OperationLogAspect） | `shared.kernel.CurrentUserContext.getCurrentUser()` | 不再直接查 UserMapper |
| ai | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | 安全上下文归 shared.kernel |
| analytics | `SecurityUtils.getCurrentUserId()` | `shared.kernel.CurrentUserContext` | 同上 |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `User` | payment, coupon, seckill, review | `UserDTO` / `UserSnapshot` | 只需 id/nickname/phone/status 等只读字段 |
| `UserAddress` | order | `AddressDTO` | 下单时收货地址快照 |
| `User`（WalletController） | Controller 层 | 不暴露，改用 `CurrentUserContext.getCurrentUserId()` | Controller 不应引用 Entity |

#### 不应暴露的 Service（内部实现）

- `AdminUserService`：管理员专用，仅 AdminUserController 调用，不对外模块暴露
- `VerificationCodeService`：验证码内部逻辑，其他模块通过 `UserApi` 间接使用
- `CaptchaService`：仅 AuthController 使用
- `security` 包内部类：`JwtUtils`、`TokenVersionService`、`TokenBlacklistService`、`UserStatusCacheService`、`SecurityUserDetailsService`、`JwtAuthenticationFilter`、`ReplayProtectionFilter`、`RsaKeyProvider` 均为框架层内部实现

---

### 2.2 product

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 查询商品详情 | `ProductSnapshot getProductById(ProductId)` | cart, order, coupon, seckill, review, stats, ai |
| 2 | 查询商品 SKU | `SkuSnapshot getSkuById(SkuId)` / `List<SkuSnapshot> listSkusByProductId(ProductId)` | cart, order, review |
| 3 | 搜索商品 | `PageResult<ProductSnapshot> searchProducts(ProductQuery)` | ai, stats |
| 4 | 查询分类 | `CategoryDTO getCategoryById(CategoryId)` / `List<CategoryDTO> listCategories()` | coupon |
| 5 | 查询分类属性 | `List<CategoryAttributeDTO> listCategoryAttributes(CategoryId)` | 前端直连 |
| 6 | 查询 Banner | `List<BannerDTO> listBanners()` | 前端直连 |
| 7 | 查询商品库存 | `StockInfo getStock(SkuId)` | inventory, order |
| 8 | 创建/更新商品（管理） | `ProductId createProduct(CreateProductCommand)` / `void updateProduct(UpdateProductCommand)` | AdminController |
| 9 | 商品上下架（管理） | `void updateProductStatus(UpdateProductStatusCommand)` | AdminController |
| 10 | SKU 生成（管理） | `List<SkuDTO> generateSkus(SkuGenerateCommand)` | AdminController |
| 11 | 分类管理（管理） | `CategoryId createCategory(CreateCategoryCommand)` / `void updateCategory(UpdateCategoryCommand)` | AdminController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| identity(UserFavorite) | `ProductService.getProductById()` | `product.api.ProductApi.getProductById()` | 返回 ProductSnapshot |
| inventory | `ProductMapper.*` + `ProductSkuMapper.*` | `product.api.ProductApi.getStock()` / `product.api.SkuApi.*` | 消除跨模块 Mapper |
| cart | `ProductService.*` + `ProductSkuService.*` | `product.api.ProductApi` + `product.api.SkuApi` | 返回 Snapshot |
| order | `ProductService.*` + `ProductSkuService.*` | `product.api.ProductApi` + `product.api.SkuApi` | 返回 Snapshot |
| coupon | `CategoryService.*` + `ProductService.*` | `product.api.CategoryApi` + `product.api.ProductApi` | 返回 DTO |
| seckill | `ProductService.getProductById()` | `product.api.ProductApi.getProductById()` | 返回 ProductSnapshot |
| review | `ProductMapper.*` + `ProductSkuMapper.*` | `product.api.ProductApi` + `product.api.SkuApi` | 消除跨模块 Mapper |
| stats | `ProductService.*` | `product.api.ProductApi` | 返回 Snapshot |
| ai | `ProductService.searchProducts()` | `product.api.ProductApi.searchProducts()` | 返回 ProductSnapshot |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `Product` | identity, cart, order, coupon, seckill, review | `ProductSnapshot` | 只读快照，含 id/name/price/status/mainImage 等 |
| `ProductSku` | cart, order, review | `SkuSnapshot` | 含 id/productId/price/stock/attributes |
| `Category` | coupon | `CategoryDTO` | 含 id/name/parentId |

#### 不应暴露的 Service（内部实现）

- `ProductAttributeService`：商品属性内部管理，仅 AdminController 用
- `CategoryAttributeService`：分类属性内部管理
- `CategoryAttributeValueService`：属性值内部管理
- `ProductSkuService` 的写方法（SKU 生成/更新）：内部管理，读方法合并到 `ProductApi`/`SkuApi`
- `BannerService`：仅前端 Banner 展示，可独立或归入 product.api

---

### 2.3 inventory

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 查询库存 | `StockInfo getStock(SkuId)` | order |
| 2 | 扣减库存 | `DeductResult deduct(DeductStockCommand)` | order |
| 3 | 回补库存 | `void restore(RestoreStockCommand)` | order |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| order | `InventoryService.*` | `inventory.api.InventoryApi.*` | 当前已是 Service 调用，转为 API |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `Product` / `ProductSku`（inventory 引用 product） | inventory 内部 | `product.api.ProductApi.getStock()` | inventory 不再直接引用 Product entity，通过 product API 获取库存数据 |

#### 不应暴露的 Service（内部实现）

- `InventoryService` 全部方法通过 `InventoryApi` 暴露，无额外内部 Service

---

### 2.4 cart

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 查询购物车 | `List<CartItemDTO> listCart(UserId)` | CartController |
| 2 | 添加购物车 | `void addToCart(AddToCartCommand)` | CartController |
| 3 | 更新购物车项 | `void updateCartItem(UpdateCartItemCommand)` | CartController |
| 4 | 删除购物车项 | `void removeCartItem(RemoveCartItemCommand)` | CartController |
| 5 | 结算预览 | `CheckoutPreview prepareCheckout(CheckoutCommand)` | order |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| order | `CartService.*` + `Cart` entity 直接引用 | `cart.api.CartApi.prepareCheckout()` | 返回 CheckoutPreview（含商品快照+价格+库存），不泄露 Cart entity |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `Cart` | order | `CartItemDTO` / `CheckoutPreview` | order 通过 `prepareCheckout()` 获取结算快照 |
| `Product` / `ProductSku`（cart 引用 product） | cart 内部 | `product.api.ProductApi` / `SkuApi` | cart 不再直接引用 Product entity |

#### 不应暴露的 Service（内部实现）

- `CartService` 全部方法通过 `CartApi` 暴露

---

### 2.5 order

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 创建订单（购物车结算） | `OrderCreateResult createOrder(CreateOrderCommand)` | OrderController |
| 2 | 立即购买下单 | `OrderCreateResult buyNow(BuyNowCommand)` | OrderController |
| 3 | 查询订单列表 | `PageResult<OrderListItemDTO> listOrders(OrderListQuery)` | OrderController + ai |
| 4 | 查询订单详情 | `OrderDetailDTO getOrderDetail(OrderId)` | OrderController |
| 5 | 支付订单 | `PayResult payOrder(PayOrderCommand)` | OrderController |
| 6 | 取消订单 | `void cancelOrder(CancelOrderCommand)` | OrderController + payment |
| 7 | 发货 | `void ship(ShipCommand)` | AdminOrderController |
| 8 | 确认收货 | `void confirmReceipt(ConfirmReceiptCommand)` | OrderController |
| 9 | 管理员查询订单 | `PageResult<AdminOrderDTO> adminListOrders(AdminOrderQuery)` | AdminOrderController |
| 10 | 查询订单状态分布 | `List<OrderStatusDistributionDTO> getStatusDistribution()` | stats, system |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| payment | `OrderService.*` + `NormalOrder` entity | `order.api.OrderApi.getOrderDetail()` / `cancelOrder()` | 返回 OrderDTO 而非 NormalOrder |
| review | `OrderService.*`（查询用户是否买过该商品） | `order.api.OrderApi.queryUserPurchased()` | 新增能力：查询用户购买记录 |
| ai | `OrderQueryService.*` | `order.api.OrderApi.listOrders()` | 返回 OrderListItemDTO |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `NormalOrder` | payment | `OrderDTO` / `OrderSnapshot` | 钱包流水查询关联订单 |
| `NormalOrderItem` | payment | `OrderItemDTO` | 订单项快照 |
| `OrderStatus`（枚举） | stats, system | 放 `order.api.dto` 或 `shared.kernel` | 枚举可共享 |
| `Cart` / `Product` / `ProductSku` / `UserAddress` / `SeckillOrder`（order 引入） | order 内部 | 通过各模块 API 获取 DTO | order 不再直接引用其他模块 Entity |

#### 不应暴露的 Service（内部实现）

- `OrderLifecycleService`：内部生命周期编排（支付/发货/取消/完成），合并到 `OrderApi` 内部实现
- `AdminOrderService`：管理员专用，通过 AdminOrderController 暴露
- `mq.consumer.OrderCancelConsumer`：内部消息消费

---

### 2.6 payment

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 支付 | `PayResult pay(PayCommand)` | order, seckill |
| 2 | 查询支付状态 | `PaymentStatus getPaymentStatus(PaymentId)` | order |
| 3 | 钱包余额查询 | `WalletBalance getWalletBalance(UserId)` | WalletController |
| 4 | 钱包流水查询 | `PageResult<WalletRecordDTO> listWalletRecords(WalletRecordQuery)` | WalletController |
| 5 | 钱包充值 | `void rechargeWallet(RechargeWalletCommand)` | WalletController |
| 6 | 充值卡生成（管理） | `List<RechargeCardDTO> generateCards(GenerateCardsCommand)` | AdminRechargeCardController |
| 7 | 充值卡核销 | `RedeemResult redeemCard(RedeemCardCommand)` | WalletController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| order | `PaymentService.*` | `payment.api.PaymentApi.pay()` / `getPaymentStatus()` | 返回 PayResult |
| seckill | `PaymentService.*` | `payment.api.PaymentApi.pay()` | 秒杀支付 |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `NormalOrder` / `SeckillOrder` / `User`（payment 引入） | payment 内部 | 通过 `order.api` / `seckill.api` / `identity.api` 获取 DTO | WalletService 不再直接引用三模块 Entity |

#### 不应暴露的 Service（内部实现）

- `WalletService`：钱包是 payment 子能力，通过 `PaymentApi`/`WalletApi` 暴露
- `RechargeCardService`：充值卡管理，通过 AdminController 暴露
- `LocalStorageService` / `MinioStorageService`：存储策略实现（属 upload，此处不重复）

---

### 2.7 coupon

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 查询可用优惠券 | `List<CouponDTO> listAvailableCoupons(AvailableCouponQuery)` | CouponController + order |
| 2 | 领取优惠券 | `void claimCoupon(ClaimCouponCommand)` | CouponController |
| 3 | 核销优惠券 | `CouponUseResult useCoupon(UseCouponCommand)` | order |
| 4 | 退还优惠券 | `void returnCoupon(ReturnCouponCommand)` | order |
| 5 | 查询优惠券模板（管理） | `PageResult<CouponDTO> listCoupons(CouponQuery)` | AdminCouponController |
| 6 | 创建优惠券（管理） | `void createCoupon(CreateCouponCommand)` | AdminCouponController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| order | `CouponUsageService.*` | `coupon.api.CouponApi.useCoupon()` / `returnCoupon()` | 返回 CouponUseResult |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `Coupon` / `UserCoupon` | 仅 Controller/Converter | `CouponDTO` / `UserCouponDTO` | 已有 VO，转为 DTO |
| `Category` / `Product` / `User`（coupon 引入） | coupon 内部 | 通过 `product.api` / `identity.api` 获取 DTO | 不再直接引用 Entity |

#### 不应暴露的 Service（内部实现）

- `CouponService` 的管理方法（创建/查询模板）：仅 AdminController 用
- `CouponUsageService`：核销/退还是核心能力，通过 `CouponApi` 暴露

---

### 2.8 seckill

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 查询秒杀活动 | `SeckillActivityDTO getActivity(ActivityId)` / `List<SeckillActivityDTO> listActivities()` | SeckillController |
| 2 | 查询秒杀商品 | `SeckillGoodsDTO getSeckillGoods(SeckillId)` / `PageResult<SeckillGoodsDTO> listSeckillGoods(SeckillGoodsQuery)` | SeckillController + stats |
| 3 | 秒杀下单 | `SeckillResult executeSeckill(SeckillCommand)` | SeckillController |
| 4 | 查询秒杀订单 | `SeckillOrderDTO getSeckillOrder(OrderId)` / `PageResult<SeckillOrderDTO> listSeckillOrders(SeckillOrderQuery)` | order, payment, review, system |
| 5 | 秒杀排行 | `List<SeckillRankingDTO> getRanking()` | stats |
| 6 | 秒杀概览 | `SeckillOverviewDTO getOverview()` | stats |
| 7 | 创建秒杀活动（管理） | `void createActivity(CreateActivityCommand)` | SeckillController |
| 8 | 创建秒杀商品（管理） | `void createSeckillGoods(CreateSeckillGoodsCommand)` | SeckillController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| order | `SeckillOrderService.*` + `SeckillOrder` entity | `seckill.api.SeckillOrderApi.*` | 返回 SeckillOrderDTO |
| payment | `SeckillOrderService.*` + `SeckillOrder` entity | `seckill.api.SeckillOrderApi.*` | 返回 SeckillOrderDTO |
| review | `SeckillOrderService.*`（查询用户秒杀购买记录） | `seckill.api.SeckillOrderApi.queryUserPurchased()` | 新增能力 |
| stats | `SeckillGoodsService.*` + `SeckillOrderService.*` | `seckill.api.SeckillApi.getOverview()` / `getRanking()` | 返回 DTO |
| system | `SeckillOrderService.*` | `seckill.api.SeckillOrderApi.*` | 返回 DTO |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `SeckillOrder` | order, payment | `SeckillOrderDTO` | 订单关联查询 |
| `SeckillGoods` | stats | `SeckillGoodsDTO` | 排行/概览 |
| `Product` / `User`（seckill 引入） | seckill 内部 | 通过 `product.api` / `identity.api` 获取 DTO | MQ Consumer 不再直接注入 ProductMapper/UserMapper |

#### 不应暴露的 Service（内部实现）

- `SeckillTokenService`：令牌生成/校验是内部实现
- `SeckillInventoryPort` / `SeckillInventoryPortImpl`：库存端口是内部实现
- `SeckillDbStrategy`：DB 策略是内部实现
- `SeckillService` 的内部方法（Lua 执行、布隆过滤、结果缓存）
- `scheduler.SeckillStatusScheduler`：内部调度
- `mq.producer.SeckillOrderProducer` / `mq.consumer.SeckillOrderConsumer`：内部消息

---

### 2.9 review

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 提交评价 | `void submitReview(SubmitReviewCommand)` | ProductReviewController |
| 2 | 查询商品评价 | `PageResult<ReviewDTO> listReviews(ReviewQuery)` | ProductReviewController |
| 3 | 管理员审核评价 | `void auditReview(AuditReviewCommand)` | AdminReviewController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| （review 不被其他模块调用） | — | — | review 是终端模块，仅 Controller 暴露 |
| review → order | `OrderService.*` | `order.api.OrderApi.queryUserPurchased()` | 查询用户是否购买 |
| review → seckill | `SeckillOrderService.*` | `seckill.api.SeckillOrderApi.queryUserPurchased()` | 查询用户秒杀购买 |
| review → product | `ProductMapper.*` + `ProductSkuMapper.*` | `product.api.ProductApi` / `SkuApi` | 消除跨模块 Mapper |
| review → identity | `UserService.*` | `identity.api.UserApi.getUserById()` | 返回 UserDTO |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `ProductSku` / `User`（review 引入） | review 内部 | 通过 `product.api` / `identity.api` 获取 DTO | 不再直接引用 Entity |

#### 不应暴露的 Service（内部实现）

- `ProductReviewService` 全部通过 `ReviewApi` 暴露

---

### 2.10 stats

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 统计概览 | `StatsOverviewDTO getOverview()` | StatsController |
| 2 | 订单趋势 | `List<TrendDTO> getOrderTrend()` | StatsController |
| 3 | 秒杀排行 | `List<SeckillRankingDTO> getSeckillRanking()` | StatsController |
| 4 | 订单状态分布 | `List<OrderStatusDistributionDTO> getOrderStatusDistribution()` | StatsController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| （stats 不被其他模块调用） | — | — | stats 是终端聚合模块 |
| stats → product | `ProductService.*` | `product.api.ProductApi` | 返回 Snapshot |
| stats → seckill | `SeckillGoodsService.*` + `SeckillOrderService.*` | `seckill.api.SeckillApi.getOverview()` / `getRanking()` | 返回 DTO |
| stats → identity | `UserService.*` | `identity.api.UserApi` | 返回 UserDTO |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `OrderStatus`（枚举） | stats | 放 `shared.kernel` 或 `order.api.dto` | 枚举共享 |
| `SeckillOverviewVO` / `SeckillRankingVO`（命名属 seckill） | stats | 由 `seckill.api` 提供 `SeckillOverviewDTO` / `SeckillRankingDTO` | VO 归属纠正 |

#### 不应暴露的 Service（内部实现）

- `StatsService` 全部通过 `StatsApi` 暴露

---

### 2.11 system

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 系统健康监控 | `SystemHealthDTO getHealth()` | SystemController |
| 2 | 操作日志查询 | `PageResult<OperationLogDTO> listOperationLogs(OperationLogQuery)` | SystemController |
| 3 | 登录日志查询 | `PageResult<LoginLogDTO> listLoginLogs(LoginLogQuery)` | SystemController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| （system 不被其他模块调用） | — | — | system 是终端模块 |
| system → seckill | `SeckillOrderService.*` | `seckill.api.SeckillOrderApi` | 返回 DTO |
| system → identity | `UserMapper.*`（OperationLogAspect） | `shared.kernel.CurrentUserContext` | 不再直接查 UserMapper |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `OrderStatus`（枚举） | system | 放 `shared.kernel` | 枚举共享 |
| `User`（OperationLogAspect） | system 内部 | `shared.kernel.CurrentUserContext` | 不再引用 User entity |

#### 不应暴露的 Service（内部实现）

- `SystemHealthMonitor`：内部健康检查实现
- `OperationLogAspect` / `OperationLogRecorder`：横切注解驱动，内部实现
- `annotation.OperationLog`：注解定义，横切

---

### 2.12 upload

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 文件上传 | `UploadResult upload(UploadCommand)` | UploadController + identity(AuthService) |
| 2 | 发送邮件 | `void sendEmail(SendEmailCommand)` | order, seckill |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| identity | `UploadService.upload()` | `upload.api.UploadApi.upload()` | 头像上传 |
| order | `EmailService.send()` | `upload.api.EmailApi.sendEmail()` 或 `shared.infrastructure.EmailPort` | 订单通知邮件 |
| seckill | `EmailService.send()` | `upload.api.EmailApi.sendEmail()` 或 `shared.infrastructure.EmailPort` | 秒杀通知邮件 |

#### Entity 泄漏 → 需 DTO 化

无 Entity 泄漏（upload 无 Entity）。

#### 不应暴露的 Service（内部实现）

- `StorageService`：存储策略接口，内部实现
- `LocalStorageService` / `MinioStorageService`：具体存储实现
- `EmailServiceImpl`：邮件发送实现

---

### 2.13 ai

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | AI 网关路由 | `AiResponse route(AiRequest)` | AiGatewayController |
| 2 | 购物助手 | `AssistantResponse assist(AssistantCommand)` | ShoppingAssistantController |
| 3 | 智能客服 | `CustomerServiceResponse chat(ChatCommand)` | CustomerServiceController |
| 4 | AIGC 生成 | `AigcResult generate(AigcCommand)` | AigcController |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| （ai 不被其他模块调用） | — | — | ai 是终端模块 |
| ai → product | `ProductService.searchProducts()` + `ProductQueryRequest` + `ProductVO` | `product.api.ProductApi.searchProducts()` | 返回 ProductSnapshot |
| ai → order | `OrderQueryService.*` + `OrderListItemVO` | `order.api.OrderApi.listOrders()` | 返回 OrderListItemDTO |
| ai → identity | `SecurityUtils` | `shared.kernel.CurrentUserContext` | 安全上下文 |

#### Entity 泄漏 → 需 DTO 化

| 泄漏的 Entity | 引用方 | 替代 DTO | 说明 |
|---|---|---|---|
| `ProductQueryRequest` / `ProductVO`（product 的 DTO/VO） | ai 内部 | `product.api.dto.ProductQuery` / `ProductSnapshot` | 通过 product.api.dto 通信 |
| `OrderListItemVO`（order 的 VO） | ai 内部 | `order.api.dto.OrderListItemDTO` | 通过 order.api.dto 通信 |

#### 不应暴露的 Service（内部实现）

- `AiGatewayService` / `RouteService`：网关路由内部
- `AssistantService` / `ProductSearchTool` / `ChatHistoryService`：助手内部
- `AgentService` / `FaqService`：客服内部
- `AigcService` / `ContentSafetyService` / `PromptTemplates`：AIGC 内部
- `advisor.*`（Budget/RateLimit/Audit/Fallback/SemanticCache）：网关切面内部

---

### 2.14 analytics.tracking

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 事件追踪 | `void track(TrackEventCommand)` | TrackController + @Tracking 注解横切 |

#### 当前跨模块调用 → 应转换为 API

| 调用方 | 当前调用 | 目标 API | 说明 |
|---|---|---|---|
| （analytics 不被其他模块调用） | — | — | analytics 是旁路模块 |
| analytics → identity | `SecurityUtils` | `shared.kernel.CurrentUserContext` | 安全上下文 |

#### Entity 泄漏 → 需 DTO 化

无跨模块 Entity 泄漏（analytics 自包含）。

#### 不应暴露的 Service（内部实现）

- `TrackService`：内部事件处理
- `TrackingAspect`：横切注解驱动
- `mq.TrackProducer` / `TrackConsumer`：内部消息

---

### 2.15 shared.kernel

#### 对外提供的业务能力

| # | 业务能力 | API 方法签名（建议） | 暴露给 |
|---|---|---|---|
| 1 | 缓存 | `CachePort.get/put/delete/exists` | 全模块 |
| 2 | 消息总线 | `MessageBusPort.publish/subscribe` | 全模块 |
| 3 | 分布式锁 | `DistributedLockPort.tryLock/unlock` | 全模块 |
| 4 | 当前用户上下文 | `CurrentUserContext.getCurrentUser()` | 全模块 |
| 5 | SSE 工具 | `SseUtils.*` | ai |

#### 当前跨模块调用 → 应转换为 API

shared.kernel 是所有模块的依赖底座，通过 Port 暴露。无需转换，但需**扩展覆盖面**：
- `cache` 包（RedisService 等 26 处直接依赖）应纳入 `CachePort` 抽象
- `DistributedLockPort`（0 使用）应替代 seckill 直接依赖 RedissonClient

#### Entity 泄漏 → 需 DTO 化

无（Port 只定义抽象，不含 Entity）。

#### 不应暴露的 Service（内部实现）

- `adapter.RedisCacheAdapter` / `RabbitMessageBusAdapter`：适配器实现，由 Spring 注入到 Port，不直接被业务模块调用

---

## 3. 能力汇总

### 3.1 需要定义 Public API 的模块（按优先级）

| 优先级 | 模块 | 需暴露能力数 | 被依赖模块数 | 理由 |
|---|---|---|---|---|
| **P0** | product | 11 | 9 | 被依赖最多，Product/ProductSku Entity 泄漏最严重（7 模块引用），是基础模块 |
| **P0** | identity | 13 | 8 | 被依赖第二多，User/UserAddress 泄漏，SecurityUtils 需归 shared.kernel |
| **P0** | order | 10 | 3 | 核心交易链路，NormalOrder 泄漏给 payment，依赖 7 个模块需收敛 |
| **P1** | seckill | 8 | 5 | SeckillOrder 泄漏给 order/payment/review/stats，MQ Consumer 跨模块 Mapper |
| **P1** | payment | 7 | 2 | 被 order/seckill 依赖，WalletService 多模块聚合需收敛 |
| **P1** | coupon | 6 | 1 | 被 order 依赖，核销/退还是核心能力 |
| **P1** | cart | 5 | 1 | Cart entity 泄漏给 order，需提供 CheckoutPreview |
| **P1** | inventory | 3 | 1 | 跨模块 Mapper 调 product，需通过 product API |
| **P2** | review | 3 | 0 | 不被依赖，但自己跨模块调 product/order/seckill/identity 需收敛 |
| **P2** | stats | 4 | 0 | 纯聚合，不被依赖，VO 命名耦合需纠正 |
| **P2** | system | 3 | 0 | 不被依赖，OperationLogAspect 跨模块 UserMapper 需收敛 |
| **P2** | upload | 2 | 3 | 被 identity/order/seckill 依赖，无 Entity 泄露，EmailService 归属待决策 |
| **P2** | ai | 4 | 0 | 已模块化，收敛 facade 即可 |
| **P2** | analytics | 1 | 0 | 已模块化，仅 SecurityUtils→CurrentUserContext |
| **—** | shared.kernel | 5 | 15 | 已是 Port，需扩展覆盖面（cache 26 处 + DistributedLockPort 0 使用） |

### 3.2 Entity 泄漏全景

按严重程度（被引用模块数 × 引用频次）排序：

| 排名 | 泄漏的 Entity | 所属模块 | 被引用模块数 | 替代 DTO | 严重程度 |
|---|---|---|---|---|---|
| 1 | `Product` | product | 7（identity/cart/order/coupon/seckill/review/ai） | `ProductSnapshot` | 🔴 极高 |
| 2 | `ProductSku` | product | 4（cart/order/review/ai） | `SkuSnapshot` | 🔴 极高 |
| 3 | `User` | identity | 4（payment/coupon/seckill/review） | `UserDTO` / `UserSnapshot` | 🔴 高 |
| 4 | `NormalOrder` + `NormalOrderItem` | order | 1（payment） | `OrderDTO` / `OrderSnapshot` | 🟡 中 |
| 5 | `SeckillOrder` | seckill | 2（order/payment） | `SeckillOrderDTO` | 🟡 中 |
| 6 | `Cart` | cart | 1（order） | `CartItemDTO` / `CheckoutPreview` | 🟡 中 |
| 7 | `UserAddress` | identity | 1（order） | `AddressDTO` | 🟡 中 |
| 8 | `Category` | product | 1（coupon） | `CategoryDTO` | 🟢 低 |
| 9 | `SeckillGoods` | seckill | 1（stats） | `SeckillGoodsDTO` | 🟢 低 |
| 10 | `OrderStatus`（枚举） | order | 2（stats/system） | 放 `shared.kernel` 或 `order.api.dto` | 🟢 低 |

**Entity 泄漏总计：约 22 处，涉及 10 类 Entity/枚举，跨 8 个模块。**

### 3.3 应隐藏的内部 Service 汇总

| 模块 | 应隐藏的内部 Service / 类 | 数量 | 说明 |
|---|---|---|---|
| identity | `AdminUserService`、`VerificationCodeService`、`CaptchaService`、`security` 包内部类（JwtUtils/TokenVersionService/TokenBlacklistService/UserStatusCacheService/SecurityUserDetailsService/JwtAuthenticationFilter/ReplayProtectionFilter/RsaKeyProvider） | 11 | 管理类/验证码/安全框架内部 |
| product | `ProductAttributeService`、`CategoryAttributeService`、`CategoryAttributeValueService`、`ProductSkuService`（写方法） | 4 | 属性/分类属性内部管理 |
| order | `OrderLifecycleService`、`AdminOrderService`、`mq.consumer.OrderCancelConsumer` | 3 | 生命周期编排/管理/消息消费 |
| payment | `WalletService`、`RechargeCardService` | 2 | 子能力，通过 PaymentApi 暴露 |
| seckill | `SeckillTokenService`、`SeckillInventoryPort`、`SeckillDbStrategy`、`scheduler`、`mq.*` | 6+ | 令牌/库存/策略/调度/消息内部 |
| system | `SystemHealthMonitor`、`OperationLogAspect`、`OperationLogRecorder` | 3 | 健康监控/横切内部 |
| upload | `StorageService`、`LocalStorageService`、`MinioStorageService`、`EmailServiceImpl` | 4 | 存储策略/邮件实现 |
| ai | `AiGatewayService`、`RouteService`、`AssistantService`、`ProductSearchTool`、`ChatHistoryService`、`AgentService`、`FaqService`、`AigcService`、`ContentSafetyService`、`advisor.*` | 10+ | 各子模块内部 Service |
| analytics | `TrackService`、`TrackingAspect`、`mq.*` | 3+ | 事件处理/横切/消息内部 |
| shared.kernel | `adapter.RedisCacheAdapter`、`RabbitMessageBusAdapter` | 2 | 适配器实现 |

**应隐藏的内部 Service 总计：约 48 个类/Service。**

---

## 4. 关键设计结论

### 4.1 API 定义优先级

1. **第一批（P0）**：`product.api` + `identity.api` — 被依赖最多、Entity 泄漏最严重，是所有其他模块的基础。必须最先定义。
2. **第二批（P0）**：`order.api` — 核心交易链路，依赖 P0 模块，自身又被 payment/review/ai 依赖。
3. **第三批（P1）**：`seckill.api` + `payment.api` + `coupon.api` + `cart.api` + `inventory.api` — 依赖 P0，被 order 或互相依赖。
4. **第四批（P2）**：`review.api` + `stats.api` + `system.api` + `upload.api` — 终端模块或低依赖。

### 4.2 需新增的业务能力

在分析中发现以下当前缺失、但跨模块调用需要的能力，需在目标 API 中新增：

| 模块 | 新增能力 | 方法签名 | 调用方 |
|---|---|---|---|
| order | 查询用户购买记录 | `boolean queryUserPurchased(UserId, ProductId)` | review |
| seckill | 查询用户秒杀购买记录 | `boolean queryUserPurchased(UserId, SeckillId)` | review |
| cart | 结算预览 | `CheckoutPreview prepareCheckout(CheckoutCommand)` | order |
| seckill | 秒杀概览/排行（聚合） | `SeckillOverviewDTO getOverview()` / `List<SeckillRankingDTO> getRanking()` | stats |

### 4.3 依赖方向调整（打破循环）

当前 `order ↔ payment` 存在双向依赖（order→payment 支付，payment→order 查询订单）。目标设计：
- **order → payment.api**（order 调用 payment 发起支付）✅ 保留
- **payment → order.api**（payment 查询订单详情用于钱包流水）→ 改为 **payment 不主动查 order**，而是 order 在支付成功后**推送支付结果**给 payment（通过 `payment.api.PaymentApi.recordPayment()` 或事件）
- 这样依赖方向变为 `order → payment.api` 单向，payment 不再依赖 order

---

> **本文件为 Phase 6 第二阶段前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **下一步**：基于本能力映射，生成 `TARGET-MODULE-DESIGN.md`（目标模块设计与依赖关系）。