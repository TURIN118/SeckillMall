# Order 模块 API 契约 (ORDER-API-CONTRACT)

> 生成时间：2026-08-17
> 阶段：Phase 2.5 - Order 模块迁移前置设计
> 依据：ORDER-MIGRATION-PLAN.md + MODULE-CAPABILITY-MAP.md
> 说明：纯架构设计文档，未创建任何代码/接口。本文档定义 API 契约，实施时再创建接口。

---

## 1. 契约设计原则

1. **API 方法用业务语言命名**：如 `createOrder`、`payOrder`、`cancelOrder`，不用技术语言（如 `insert`、`updateStatus`）。
2. **入参用 Command/Query，出参用 Result/DTO**：禁止裸露 `Long userId, Long productId, ...` 多参数，封装为 Command 对象。
3. **禁止暴露 Entity/Mapper/PO**：API 签名中不出现 `NormalOrder`、`NormalOrderItem`、`NormalOrderMapper`。
4. **禁止暴露内部 Service**：`OrderLifecycleService` 合并入 `OrderApi`，不单独暴露。
5. **异常用业务错误码（ErrorCode）**：所有异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈。
6. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。

---

## 2. OrderApi（订单业务能力）

> 合并原 `OrderService`（写方法）+ `OrderLifecycleService`（状态流转）+ `OrderQueryService.deleteOrder`。

### 2.1 buyNow - 立即购买

| 项 | 定义 |
|---|---|
| **方法签名** | `OrderCreateResult buyNow(BuyNowCommand command)` |
| **调用方** | OrderController |
| **业务语义** | 用户在商品详情页立即购买，创建普通订单（校验商品状态/库存 → 扣库存 → 建订单与明细 → 发延迟取消消息） |
| **原方法** | `OrderService.createNormalOrder(userId, productId, skuId, quantity, addressId, remark, userCouponId)` |

**入参 BuyNowCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID（由 CurrentUserContext 注入，非前端传入） |
| productId | Long | 是 | 商品 ID |
| skuId | Long | 否 | SKU ID（null 表示无规格商品） |
| quantity | Integer | 是 | 购买数量（≥1） |
| addressId | Long | 是 | 收货地址 ID |
| remark | String | 否 | 备注 |
| userCouponId | Long | 否 | 用户优惠券 ID（null 表示不使用优惠券） |

**出参 OrderCreateResult**：

| 字段 | 类型 | 说明 |
|---|---|---|
| orderId | Long | 订单 ID |
| orderNo | String | 订单号 |
| totalAmount | BigDecimal | 商品总金额 |
| payAmount | BigDecimal | 实付金额 |
| payDeadline | LocalDateTime | 支付截止时间 |

**异常**：

| 错误码 | 触发场景 |
|---|---|
| `PRODUCT_NOT_FOUND` | 商品不存在 |
| `PRODUCT_OFF_SHELF` | 商品已下架 |
| `STOCK_NOT_ENOUGH` | 库存不足 |
| `ADDRESS_NOT_FOUND` | 收货地址不存在或不属于当前用户 |
| `COUPON_INVALID` | 优惠券无效或不可用 |
| `PARAM_ERROR` | 参数校验失败 |

**依赖的外部 API**：`product.api.ProductApi`、`product.api.SkuApi`、`product.api.InventoryApi`、`identity.api.AddressApi`、`coupon.api.CouponApi`、`shared.kernel.MessageBusPort`

---

### 2.2 createOrder - 购物车结算创建订单

| 项 | 定义 |
|---|---|
| **方法签名** | `OrderCreateResult createOrder(CreateOrderCommand command)` |
| **调用方** | OrderController |
| **业务语义** | 用户从购物车结算创建普通订单（校验地址+购物车项 → 计算总额 → 建订单与明细 → 扣库存 → 删购物车项 → 发延迟消息） |
| **原方法** | `OrderService.createOrderFromCart(userId, addressId, cartIds, remark, userCouponId)` |

**入参 CreateOrderCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| addressId | Long | 是 | 收货地址 ID |
| cartIds | List\<Long\> | 是 | 待结算购物车项 ID 列表（非空） |
| remark | String | 否 | 备注 |
| userCouponId | Long | 否 | 用户优惠券 ID |

**出参**：同 `OrderCreateResult`

**异常**：`ADDRESS_NOT_FOUND`、`CART_ITEM_NOT_FOUND`、`PRODUCT_OFF_SHELF`、`STOCK_NOT_ENOUGH`、`COUPON_INVALID`、`PARAM_ERROR`

**依赖的外部 API**：`cart.api.CartApi`、`product.api.ProductApi`、`product.api.SkuApi`、`product.api.InventoryApi`、`identity.api.AddressApi`、`coupon.api.CouponApi`、`shared.kernel.MessageBusPort`

---

### 2.3 payOrder - 支付订单

| 项 | 定义 |
|---|---|
| **方法签名** | `OrderDetailDTO payOrder(PayOrderCommand command)` |
| **调用方** | OrderController |
| **业务语义** | 支付普通订单（WALLET 方式扣钱包余额，其他方式模拟支付；UNPAID → PAID） |
| **原方法** | `OrderLifecycleService.payNormalOrder(userId, orderId, payMethod)` |

**入参 PayOrderCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| orderId | Long | 是 | 普通订单 ID |
| payMethod | String | 是 | 支付方式（WALLET/ALIPAY/WECHAT） |

**出参 OrderDetailDTO**：见 §4.3

**异常**：`ORDER_NOT_FOUND`、`ORDER_STATUS_NOT_UNPAID`、`WALLET_BALANCE_NOT_ENOUGH`、`PAY_FAILED`

**依赖的外部 API**：`payment.api.PaymentApi`、`identity.api.UserApi`、`coupon.api.CouponApi`、`product.api.InventoryApi`

---

### 2.4 cancelOrder - 取消订单

| 项 | 定义 |
|---|---|
| **方法签名** | `OrderDetailDTO cancelOrder(CancelOrderCommand command)` |
| **调用方** | OrderController, payment（钱包退款触发） |
| **业务语义** | 取消普通订单（仅 UNPAID 可取消；回补库存；退优惠券；异步发取消邮件） |
| **原方法** | `OrderLifecycleService.cancelNormalOrder(userId, orderId)` |

**入参 CancelOrderCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| orderId | Long | 是 | 普通订单 ID |

**出参**：`OrderDetailDTO`

**异常**：`ORDER_NOT_FOUND`、`ORDER_STATUS_NOT_UNPAID`、`CANCEL_FAILED`

**依赖的外部 API**：`product.api.InventoryApi`、`coupon.api.CouponApi`、`upload.api.EmailApi`

---

### 2.5 ship - 发货

| 项 | 定义 |
|---|---|
| **方法签名** | `void ship(ShipCommand command)` |
| **调用方** | OrderController（管理员） |
| **业务语义** | 普通订单发货（PAID → SHIPPED，设置物流信息） |
| **原方法** | `OrderLifecycleService.shipNormalOrder(userId, orderId, shippingCompany, shippingNo)` |

**入参 ShipCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 操作人 ID（管理员） |
| orderId | Long | 是 | 普通订单 ID |
| shippingCompany | String | 是 | 物流公司 |
| shippingNo | String | 是 | 快递单号 |

**异常**：`ORDER_NOT_FOUND`、`ORDER_STATUS_NOT_PAID`、`PARAM_ERROR`

**依赖的外部 API**：无（仅操作自身持久层）

---

### 2.6 confirmReceipt - 确认收货

| 项 | 定义 |
|---|---|
| **方法签名** | `void confirmReceipt(ConfirmReceiptCommand command)` |
| **调用方** | OrderController |
| **业务语义** | 确认收货（SHIPPED → COMPLETED） |
| **原方法** | `OrderLifecycleService.confirmNormalOrder(userId, orderId)` |

**入参 ConfirmReceiptCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| orderId | Long | 是 | 普通订单 ID |

**异常**：`ORDER_NOT_FOUND`、`ORDER_STATUS_NOT_SHIPPED`

**依赖的外部 API**：无

---

### 2.7 deleteOrder - 逻辑删除订单

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean deleteOrder(DeleteOrderCommand command)` |
| **调用方** | OrderController |
| **业务语义** | 逻辑删除订单（仅 COMPLETED/CANCELLED 可删除，支持秒杀+普通两类） |
| **原方法** | `OrderQueryService.deleteOrder(orderId, userId)` |

**入参 DeleteOrderCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| orderId | Long | 是 | 订单 ID（秒杀或普通） |
| userId | Long | 是 | 当前操作用户 ID |

**异常**：`ORDER_NOT_FOUND`、`ORDER_DELETE_FAILED`（状态不允许删除）

**依赖的外部 API**：`seckill.api.SeckillOrderApi`（删除秒杀订单时委托）

---

### 2.8 timeoutCancel - 超时取消（内部）

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean timeoutCancel(Long orderId)` |
| **调用方** | OrderCancelConsumer（MQ 内部） |
| **业务语义** | 普通订单超时取消（UNPAID → TIMEOUT，回补库存；已支付等终态幂等忽略） |
| **原方法** | `OrderLifecycleService.timeoutCancelNormalOrder(orderId)` |

**入参**：`Long orderId`

**出参**：`boolean`（true=本次执行了取消）

**异常**：无（异常内部捕获，返回 false）

**依赖的外部 API**：`product.api.InventoryApi`、`coupon.api.CouponApi`

---

## 3. OrderQueryApi（订单查询能力）

### 3.1 getOrderDetail - 查询订单详情

| 项 | 定义 |
|---|---|
| **方法签名** | `OrderDetailDTO getOrderDetail(Long userId, Long orderId)` |
| **调用方** | OrderController |
| **业务语义** | 查询普通订单详情（含明细列表+收货地址信息；自动处理超时状态） |
| **原方法** | `OrderQueryService.getNormalOrderDetail(userId, orderId)` |

**出参 OrderDetailDTO**：见 §4.3

**异常**：`ORDER_NOT_FOUND`、`ORDER_NOT_BELONG_TO_USER`

**依赖的外部 API**：`identity.api.AddressApi`、`product.api.ProductApi`

---

### 3.2 listOrders - 统一订单列表

| 项 | 定义 |
|---|---|
| **方法签名** | `PageResult<OrderListItemDTO> listOrders(OrderListQuery query)` |
| **调用方** | OrderController, ai（AgentService） |
| **业务语义** | 统一查询秒杀订单+普通订单，合并后按 createTime 降序，内存分页 |
| **原方法** | `OrderQueryService.getUnifiedOrderList(userId, status, orderType, pageNum, pageSize)` |

**入参 OrderListQuery**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID |
| status | String | 否 | 订单状态筛选（null=不筛选） |
| orderType | String | 否 | 订单类型筛选（null/NORMAL/SECKILL） |
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10，上限 50） |

**出参 OrderListItemDTO**：见 §4.3

**异常**：无

**依赖的外部 API**：`seckill.api.SeckillOrderApi`（查秒杀订单列表）、`product.api.ProductApi`（商品快照）

---

### 3.3 hasUserPurchased - 查询用户是否购买

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean hasUserPurchased(Long userId, Long productId, Long skuId)` |
| **调用方** | review（ProductReviewService） |
| **业务语义** | 校验用户是否通过普通订单购买了该商品（用于评价权限校验） |
| **原方法** | `OrderService.hasUserPurchasedProduct(userId, productId, skuId)` |

**入参**：`Long userId, Long productId, Long skuId`（skuId=0 表示不校验 SKU 维度）

**出参**：`boolean`

**异常**：无

**依赖的外部 API**：无（仅查自身持久层）

---

### 3.4 getWalletPaidOrders - 查询钱包支付订单

| 项 | 定义 |
|---|---|
| **方法签名** | `List<OrderSnapshot> getWalletPaidOrders(Long userId)` |
| **调用方** | payment（WalletService） |
| **业务语义** | 查询用户钱包支付的已支付普通订单（payMethod=WALLET 且 status in PAID/SHIPPED/COMPLETED） |
| **原方法** | `OrderService.getWalletPaidOrdersByUser(userId)` → `List<NormalOrder>`（**Entity 泄露，改为返回 OrderSnapshot**） |

**出参 List\<OrderSnapshot\>**：见 §4.3

**异常**：无

**依赖的外部 API**：无

---

### 3.5 getStatusDistribution - 订单状态分布

| 项 | 定义 |
|---|---|
| **方法签名** | `List<OrderStatusDistributionDTO> getStatusDistribution()` |
| **调用方** | stats, system |
| **业务语义** | 查询普通订单各状态数量分布 |
| **原方法** | （新增能力） |

**出参 OrderStatusDistributionDTO**：见 §4.3

**依赖的外部 API**：无

---

### 3.6 getOrderTrend - 订单趋势

| 项 | 定义 |
|---|---|
| **方法签名** | `List<TrendDTO> getOrderTrend()` |
| **调用方** | stats |
| **业务语义** | 查询普通订单近 N 天下单趋势 |
| **原方法** | （新增能力） |

**出参 TrendDTO**：见 §4.3

**依赖的外部 API**：无

---

## 4. 数据契约（DTO/Command/Result）

### 4.1 Command 定义

#### BuyNowCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| productId | Long | 是 | 商品 ID | BuyNowRequest.productId |
| skuId | Long | 否 | SKU ID | BuyNowRequest.skuId |
| quantity | Integer | 是 | 购买数量 | BuyNowRequest.quantity |
| addressId | Long | 是 | 收货地址 ID | BuyNowRequest.addressId |
| remark | String | 否 | 备注 | BuyNowRequest.remark |
| userCouponId | Long | 否 | 优惠券 ID | BuyNowRequest.userCouponId |

#### CreateOrderCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| addressId | Long | 是 | 收货地址 ID | CartCheckoutRequest.addressId |
| cartIds | List\<Long\> | 是 | 购物车项 ID 列表 | CartCheckoutRequest.cartIds |
| remark | String | 否 | 备注 | CartCheckoutRequest.remark |
| userCouponId | Long | 否 | 优惠券 ID | CartCheckoutRequest.userCouponId |

#### PayOrderCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| orderId | Long | 是 | 订单 ID | 路径参数 |
| payMethod | String | 是 | 支付方式 | NormalOrderPayRequest.payMethod |

#### CancelOrderCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| orderId | Long | 是 | 订单 ID | 路径参数 |

#### ShipCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 操作人 ID | CurrentUserContext |
| orderId | Long | 是 | 订单 ID | 路径参数 |
| shippingCompany | String | 是 | 物流公司 | ShipRequest.shippingCompany |
| shippingNo | String | 是 | 快递单号 | ShipRequest.shippingNo |

#### ConfirmReceiptCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| orderId | Long | 是 | 订单 ID | 路径参数 |

#### DeleteOrderCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| orderId | Long | 是 | 订单 ID | 路径参数 |
| userId | Long | 是 | 用户 ID | CurrentUserContext |

### 4.2 Query 定义

#### OrderListQuery
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| status | String | 否 | 状态筛选 | 请求参数 |
| orderType | String | 否 | 类型筛选 | 请求参数 |
| pageNum | Integer | 否 | 页码（默认 1） | 请求参数 |
| pageSize | Integer | 否 | 每页大小（默认 10） | 请求参数 |

#### AdminOrderQuery
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| pageNum | Integer | 否 | 页码 | AdminOrderQueryRequest.pageNum |
| pageSize | Integer | 否 | 每页大小 | AdminOrderQueryRequest.pageSize |
| orderNo | String | 否 | 订单号模糊匹配 | AdminOrderQueryRequest.orderNo |
| date | String | 否 | 日期筛选（yyyy-MM-dd） | AdminOrderQueryRequest.date |
| status | String | 否 | 订单状态 | AdminOrderQueryRequest.status |
| orderType | String | 否 | 订单类型（NORMAL/SECKILL） | AdminOrderQueryRequest.orderType |
| userId | Long | 否 | 用户 ID | AdminOrderQueryRequest.userId |
| productId | Long | 否 | 商品 ID | AdminOrderQueryRequest.productId |
| seckillId | Long | 否 | 秒杀活动 ID | AdminOrderQueryRequest.seckillId |
| startTime | String | 否 | 创建时间起始 | AdminOrderQueryRequest.startTime |
| endTime | String | 否 | 创建时间结束 | AdminOrderQueryRequest.endTime |
| startDate | String | 否 | 创建日期起始 | AdminOrderQueryRequest.startDate |
| endDate | String | 否 | 创建日期结束 | AdminOrderQueryRequest.endDate |
| minAmount | BigDecimal | 否 | 最小金额 | AdminOrderQueryRequest.minAmount |
| maxAmount | BigDecimal | 否 | 最大金额 | AdminOrderQueryRequest.maxAmount |
| payStartTime | String | 否 | 支付时间起始 | AdminOrderQueryRequest.payStartTime |
| payEndTime | String | 否 | 支付时间结束 | AdminOrderQueryRequest.payEndTime |
| sortBy | String | 否 | 排序字段 | AdminOrderQueryRequest.sortBy |
| sortOrder | String | 否 | 排序方向 | AdminOrderQueryRequest.sortOrder |

### 4.3 Result/DTO 定义

#### OrderCreateResult
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| orderId | Long | 订单 ID | NormalOrder.id |
| orderNo | String | 订单号 | NormalOrder.orderNo |
| totalAmount | BigDecimal | 商品总金额 | NormalOrder.totalAmount |
| payAmount | BigDecimal | 实付金额 | NormalOrder.payAmount |
| payDeadline | LocalDateTime | 支付截止时间 | NormalOrder.payExpireTime |

#### OrderDetailDTO
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| order | OrderDTO | 订单基础信息 | NormalOrder → OrderDTO |
| items | List\<OrderItemDTO\> | 订单明细列表 | NormalOrderItem → OrderItemDTO |
| receiverName | String | 收件人 | UserAddress.receiverName |
| receiverPhone | String | 手机号 | UserAddress.phone |
| province | String | 省 | UserAddress.province |
| city | String | 市 | UserAddress.city |
| district | String | 区 | UserAddress.district |
| detailAddress | String | 详细地址 | UserAddress.detailAddress |

#### OrderDTO（订单基础信息）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 订单 ID | NormalOrder.id |
| orderNo | String | 订单号 | NormalOrder.orderNo |
| userId | Long | 用户 ID | NormalOrder.userId |
| addressId | Long | 收货地址 ID | NormalOrder.addressId |
| totalAmount | BigDecimal | 商品总金额 | NormalOrder.totalAmount |
| freightAmount | BigDecimal | 运费 | NormalOrder.freightAmount |
| payAmount | BigDecimal | 实付金额 | NormalOrder.payAmount |
| status | String | 订单状态码 | NormalOrder.status.getCode() |
| shippingCompany | String | 物流公司 | NormalOrder.shippingCompany |
| shippingNo | String | 快递单号 | NormalOrder.shippingNo |
| payMethod | String | 支付方式 | NormalOrder.payMethod |
| transactionId | String | 支付流水号 | NormalOrder.transactionId |
| payTime | LocalDateTime | 支付时间 | NormalOrder.payTime |
| payExpireTime | LocalDateTime | 支付截止时间 | NormalOrder.payExpireTime |
| cancelTime | LocalDateTime | 取消时间 | NormalOrder.cancelTime |
| cancelReason | String | 取消原因 | NormalOrder.cancelReason |
| shipTime | LocalDateTime | 发货时间 | NormalOrder.shipTime |
| confirmTime | LocalDateTime | 确认收货时间 | NormalOrder.confirmTime |
| remark | String | 备注 | NormalOrder.remark |
| userCouponId | Long | 优惠券 ID | NormalOrder.userCouponId |
| discountAmount | BigDecimal | 优惠金额 | NormalOrder.discountAmount |
| createTime | LocalDateTime | 创建时间 | NormalOrder.createTime |
| updateTime | LocalDateTime | 更新时间 | NormalOrder.updateTime |

#### OrderItemDTO（订单明细）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 明细 ID | NormalOrderItem.id |
| orderId | Long | 订单 ID | NormalOrderItem.orderId |
| productId | Long | 商品 ID | NormalOrderItem.productId |
| skuId | Long | SKU ID | NormalOrderItem.skuId |
| skuAttributes | String | SKU 属性快照 | NormalOrderItem.skuAttributes |
| productName | String | 商品名称快照 | NormalOrderItem.productName |
| productImage | String | 商品主图快照 | NormalOrderItem.productImage |
| unitPrice | BigDecimal | 商品单价快照 | NormalOrderItem.unitPrice |
| quantity | Integer | 购买数量 | NormalOrderItem.quantity |
| subtotal | BigDecimal | 小计金额 | NormalOrderItem.subtotal |

#### OrderListItemDTO（统一订单列表项）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 订单 ID | NormalOrder.id / SeckillOrder.id |
| orderNo | String | 订单号 | NormalOrder.orderNo / SeckillOrder.orderNo |
| orderType | String | 订单类型（NORMAL/SECKILL） | 固定值 |
| status | String | 订单状态码 | NormalOrder.status.getCode() |
| totalAmount | BigDecimal | 订单总金额 | NormalOrder.totalAmount |
| payMethod | String | 支付方式 | NormalOrder.payMethod |
| createTime | LocalDateTime | 下单时间 | NormalOrder.createTime |
| payTime | LocalDateTime | 支付时间 | NormalOrder.payTime |
| shipTime | LocalDateTime | 发货时间 | NormalOrder.shipTime |
| items | List\<OrderItemSnapshotDTO\> | 商品快照列表 | NormalOrderItem / SeckillOrder |

#### OrderItemSnapshotDTO（商品快照，列表展示用）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| productId | Long | 商品 ID | NormalOrderItem.productId |
| productName | String | 商品名称 | NormalOrderItem.productName |
| productImage | String | 商品主图 | NormalOrderItem.productImage |
| unitPrice | BigDecimal | 商品单价 | NormalOrderItem.unitPrice |
| quantity | Integer | 购买数量 | NormalOrderItem.quantity |

#### OrderSnapshot（订单快照，供 payment 推送用）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| orderId | Long | 订单 ID | NormalOrder.id |
| orderNo | String | 订单号 | NormalOrder.orderNo |
| userId | Long | 用户 ID | NormalOrder.userId |
| payAmount | BigDecimal | 实付金额 | NormalOrder.payAmount |
| payMethod | String | 支付方式 | NormalOrder.payMethod |
| status | String | 订单状态 | NormalOrder.status.getCode() |
| payTime | LocalDateTime | 支付时间 | NormalOrder.payTime |

#### AdminOrderDTO（管理员订单视图）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 订单 ID | NormalOrder.id / SeckillOrder.id |
| orderNo | String | 订单号 | NormalOrder.orderNo |
| orderType | String | 订单类型 | NORMAL/SECKILL |
| status | String | 订单状态 | NormalOrder.status.getCode() |
| userId | Long | 用户 ID | NormalOrder.userId |
| totalAmount | BigDecimal | 总金额 | NormalOrder.totalAmount |
| payAmount | BigDecimal | 实付金额 | NormalOrder.payAmount |
| payMethod | String | 支付方式 | NormalOrder.payMethod |
| createTime | LocalDateTime | 创建时间 | NormalOrder.createTime |
| payTime | LocalDateTime | 支付时间 | NormalOrder.payTime |
| shipTime | LocalDateTime | 发货时间 | NormalOrder.shipTime |
| items | List\<OrderItemSnapshotDTO\> | 商品快照 | NormalOrderItem |

#### OrderStatusDistributionDTO
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| status | String | 订单状态码 | OrderStatus.getCode() |
| count | Long | 订单数量 | COUNT 聚合 |

#### TrendDTO
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| date | String | 日期（yyyy-MM-dd） | DATE(create_time) |
| orderCount | Long | 订单数 | COUNT 聚合 |
| amount | BigDecimal | 订单金额合计 | SUM(total_amount) |

---

## 5. 异常定义

| 错误码 | 含义 | 触发场景 | 原 ErrorCode |
|---|---|---|---|
| `ORDER_NOT_FOUND` | 订单不存在 | 查询/支付/取消/发货时订单 ID 无效 | ErrorCode.ORDER_NOT_FOUND |
| `ORDER_NOT_BELONG_TO_USER` | 订单不属于当前用户 | 查询详情时归属校验失败 | ErrorCode.ORDER_NOT_BELONG_TO_USER |
| `ORDER_STATUS_NOT_UNPAID` | 订单状态非待支付 | 支付/取消时状态不是 UNPAID | ErrorCode.ORDER_STATUS_NOT_UNPAID |
| `ORDER_STATUS_NOT_PAID` | 订单状态非已支付 | 发货时状态不是 PAID | ErrorCode.ORDER_STATUS_NOT_PAID |
| `ORDER_STATUS_NOT_SHIPPED` | 订单状态非已发货 | 确认收货时状态不是 SHIPPED | ErrorCode.ORDER_STATUS_NOT_SHIPPED |
| `ORDER_DELETE_FAILED` | 订单状态不允许删除 | 删除时状态非 COMPLETED/CANCELLED | ErrorCode.ORDER_DELETE_FAILED |
| `PRODUCT_NOT_FOUND` | 商品不存在 | 创建订单时商品 ID 无效 | ErrorCode.PRODUCT_NOT_FOUND |
| `PRODUCT_OFF_SHELF` | 商品已下架 | 创建订单时商品状态非在售 | ErrorCode.PRODUCT_OFF_SHELF |
| `STOCK_NOT_ENOUGH` | 库存不足 | 扣减库存失败 | ErrorCode.STOCK_NOT_ENOUGH |
| `ADDRESS_NOT_FOUND` | 收货地址不存在 | 地址 ID 无效或不属于用户 | ErrorCode.ADDRESS_NOT_FOUND |
| `CART_ITEM_NOT_FOUND` | 购物车项不存在 | 结算时购物车项 ID 无效 | ErrorCode.CART_ITEM_NOT_FOUND |
| `COUPON_INVALID` | 优惠券无效 | 优惠券不可用或已过期 | ErrorCode.COUPON_INVALID |
| `WALLET_BALANCE_NOT_ENOUGH` | 钱包余额不足 | 钱包支付时余额 < 订单金额 | ErrorCode.WALLET_BALANCE_NOT_ENOUGH |
| `PAY_FAILED` | 支付失败 | 支付流程异常 | ErrorCode.PAY_FAILED |
| `CANCEL_FAILED` | 取消失败 | 取消流程异常 | ErrorCode.CANCEL_FAILED |
| `PARAM_ERROR` | 参数校验失败 | 入参校验不通过 | ErrorCode.PARAM_ERROR |

---

## 6. API 不变性（契约约束）

1. **方法签名向后兼容**：API 方法一旦发布，签名只增不改不删。新增方法不影响已有调用方。
2. **DTO 字段只增不删**：已发布 DTO 的字段不可删除，字段类型不可改变（Long 不可变 Integer，BigDecimal 不可变 double）。
3. **Command 字段可新增可选字段**：新增字段必须可选（nullable），不能删除已有字段。
4. **错误码只增不删**：已定义错误码不可删除或改变含义，可新增错误码。
5. **返回类型不可窄化**：如 `OrderDetailDTO` 不可改为 `OrderDTO`（字段更少），只可扩展。
6. **枚举值只增不删**：`OrderStatus` 枚举不可删除已有值（UNPAID/PAID/SHIPPED/CANCELLED/TIMEOUT/COMPLETED/CANCELLING），可新增。

---

## 7. 与外部模块 API 的交互契约

### 7.1 order → product.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `product.api.ProductApi.getProductById(productId)` | 查商品快照 | 创建订单时校验商品状态/获取价格 | buyNow, createOrder |
| `product.api.SkuApi.getSkuById(skuId)` | 查 SKU 快照 | 创建订单时获取 SKU 价格/属性 | buyNow, createOrder |
| `product.api.InventoryApi.deduct(command)` | 扣减库存 | 创建订单时扣库存 | buyNow, createOrder |
| `product.api.InventoryApi.restore(command)` | 回补库存 | 取消/超时取消时回补库存 | cancelOrder, timeoutCancel |

### 7.2 order → identity.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `identity.api.AddressApi.getAddress(userId, addressId)` | 查收货地址 | 创建订单时校验地址+获取收件人信息 | buyNow, createOrder, getOrderDetail |
| `identity.api.UserApi.getUserById(userId)` | 查用户信息 | 支付时获取用户邮箱（发通知） | payOrder |
| `shared.kernel.CurrentUserContext.getCurrentUser()` | 获取当前用户 | Controller 注入 userId | 所有 Controller 方法 |

### 7.3 order → cart.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `cart.api.CartApi.prepareCheckout(command)` | 结算预览 | 购物车结算时获取商品快照+价格+库存 | createOrder |

### 7.4 order → coupon.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `coupon.api.CouponApi.useCoupon(command)` | 核销优惠券 | 创建订单时抵扣 | buyNow, createOrder |
| `coupon.api.CouponApi.returnCoupon(command)` | 退还优惠券 | 取消订单时退还 | cancelOrder, timeoutCancel |

### 7.5 order → payment.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `payment.api.PaymentApi.pay(command)` | 发起支付 | 支付订单时扣钱包/模拟支付 | payOrder |

> **注意**：order → payment 是单向依赖。payment 不反查 order（打破循环）。order 支付成功后调 `payment.api.PaymentApi.recordPayment()` 推送支付结果（含 OrderSnapshot）。

### 7.6 order → seckill.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `seckill.api.SeckillOrderApi.listSeckillOrders(query)` | 查秒杀订单列表 | 统一订单列表合并展示 | listOrders |
| `seckill.api.SeckillOrderApi.getSeckillOrder(orderId)` | 查秒杀订单详情 | 统一订单详情 | getOrderDetail |
| `seckill.api.SeckillOrderApi.timeoutCancel(orderId)` | 秒杀订单超时取消 | MQ 消费者处理秒杀订单超时 | OrderCancelConsumer |

### 7.7 order → upload.api

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `upload.api.EmailApi.sendEmail(command)` | 发送邮件 | 取消订单时异步发通知邮件 | cancelOrder |

### 7.8 外部 → order.api（外部调用 order）

| 调用方 | 调用的 order API | 用途 | 原调用 |
|---|---|---|---|
| **payment**（WalletService） | `order.api.OrderQueryApi.getWalletPaidOrders(userId)` | 查钱包支付订单用于流水展示 | `OrderService.getWalletPaidOrdersByUser()` → `List<NormalOrder>` |
| **review**（ProductReviewService） | `order.api.OrderQueryApi.hasUserPurchased(userId, productId, skuId)` | 校验用户是否购买该商品（评价权限） | `OrderService.hasUserPurchasedProduct()` |
| **ai**（AgentService） | `order.api.OrderQueryApi.listOrders(query)` | AI 助手查询用户订单 | `OrderQueryService.getUnifiedOrderList()` |
| **stats**（StatsService） | `order.api.OrderQueryApi.getStatusDistribution()` / `getOrderTrend()` | 统计概览/订单趋势 | （新增） |
| **system**（SystemService） | `order.api.OrderQueryApi.getStatusDistribution()` | 系统监控订单状态分布 | （新增） |

---

## 8. 契约汇总

| 类别 | 数量 | 清单 |
|---|---|---|
| **API 接口** | 3 | OrderApi, OrderQueryApi, AdminOrderApi |
| **API 方法** | 14 | buyNow, createOrder, payOrder, cancelOrder, ship, confirmReceipt, deleteOrder, timeoutCancel, getOrderDetail, listOrders, hasUserPurchased, getWalletPaidOrders, getStatusDistribution, getOrderTrend + adminListOrders |
| **Command** | 7 | BuyNowCommand, CreateOrderCommand, PayOrderCommand, CancelOrderCommand, ShipCommand, ConfirmReceiptCommand, DeleteOrderCommand |
| **Query** | 2 | OrderListQuery, AdminOrderQuery |
| **Result** | 1 | OrderCreateResult |
| **DTO** | 9 | OrderDetailDTO, OrderDTO, OrderItemDTO, OrderListItemDTO, OrderItemSnapshotDTO, OrderSnapshot, AdminOrderDTO, OrderStatusDistributionDTO, TrendDTO |
| **异常错误码** | 16 | ORDER_NOT_FOUND ... PARAM_ERROR |
| **外部依赖 API** | 7 | product.api, identity.api, cart.api, coupon.api, payment.api, seckill.api, upload.api |
| **外部调用方** | 5 | payment, review, ai, stats, system |

---

> **本文件为 Phase 2.5 Order 模块迁移前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **本文件与 ORDER-MIGRATION-PLAN.md 共同构成 Order 模块迁移的完整设计依据。**
> **下一步**：基于本契约，进入实施阶段——按迁移步骤创建 API 接口与 DTO，迁移代码。