# Order 模块迁移计划 (ORDER-MIGRATION-PLAN)

> 生成时间：2026-08-17
> 阶段：Phase 2.5 - Order 模块迁移前置设计
> 依据：MODULE-MAP.md + MODULE-CAPABILITY-MAP.md + TARGET-MODULE-DESIGN.md
> 说明：纯架构设计文档，未创建任何代码/接口/包。

---

## 1. Order 当前结构

### 1.1 当前包结构

order 相关代码散落在 7 个不同的顶层包中，共 **28 个文件**：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `OrderController` | `controller` | 普通订单+秒杀订单统一入口（创建/查询/支付/取消/发货/确认收货/删除） |
| `AdminOrderController` | `controller` | 后台订单高级筛选 |
| `OrderService` | `service` | 普通订单创建（立即购买/购物车结算）+ 购买校验 + 钱包支付订单查询 |
| `OrderQueryService` | `service` | 订单只读查询（详情/统一列表）+ 逻辑删除 |
| `OrderLifecycleService` | `service` | 订单状态流转（支付/取消/超时/发货/确认收货） |
| `AdminOrderService` | `service` | 后台订单分页查询 |
| `OrderServiceImpl` | `service.impl` | OrderService 实现（536 行） |
| `OrderQueryServiceImpl` | `service.impl` | OrderQueryService 实现（429 行） |
| `OrderLifecycleServiceImpl` | `service.impl` | OrderLifecycleService 实现（423 行） |
| `AdminOrderServiceImpl` | `service.impl` | AdminOrderService 实现（129 行） |
| `NormalOrder` | `entity` | 普通订单 PO |
| `NormalOrderItem` | `entity` | 普通订单明细 PO |
| `OrderStatus` | `entity.enums` | 订单状态枚举 |
| `NormalOrderMapper` | `mapper` | 普通订单 Mapper |
| `NormalOrderItemMapper` | `mapper` | 普通订单明细 Mapper |
| `BuyNowRequest` | `dto` | 立即购买请求 |
| `CartCheckoutRequest` | `dto` | 购物车结算请求 |
| `NormalOrderPayRequest` | `dto` | 普通订单支付请求 |
| `ShipRequest` | `dto` | 发货请求 |
| `AdminOrderQueryRequest` | `dto` | 后台订单查询请求 |
| `NormalOrderVO` | `vo` | 普通订单视图 |
| `NormalOrderItemVO` | `vo` | 普通订单明细视图 |
| `NormalOrderDetailVO` | `vo` | 普通订单详情视图 |
| `AdminOrderVO` | `vo` | 后台订单视图 |
| `OrderListItemVO` | `vo` | 统一订单列表项视图 |
| `OrderStatusItemVO` | `vo` | 订单状态项视图 |
| `OrderTrendVO` | `vo` | 订单趋势视图 |
| `OrderStatusDistributionVO` | `vo` | 订单状态分布视图 |
| `OrderCancelConsumer` | `mq.consumer` | 订单超时取消延迟消费者 |
| `OrderDelayMessage` | `mq.message` | 订单延迟消息 |

### 1.2 当前依赖（注入的 Service/Mapper/Port）

从 4 个 ServiceImpl + 2 个 Controller + 1 个 Consumer 的字段注入提取：

| 注入项 | 类型 | 所属模块 | 依赖性质 | 注入位置 |
|---|---|---|---|---|
| `NormalOrderMapper` | Mapper | order | 自身 | OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl |
| `NormalOrderItemMapper` | Mapper | order | 自身 | 同上 |
| `MessageBusPort` | Port | shared.kernel | ✅ Port | OrderServiceImpl |
| `ProductService` | Service | product | ⚠️ 跨模块 | OrderServiceImpl, OrderQueryServiceImpl |
| `ProductSkuService` | Service | product | ⚠️ 跨模块 | OrderServiceImpl, OrderLifecycleServiceImpl |
| `CartService` | Service | cart | ⚠️ 跨模块 | OrderServiceImpl |
| `UserAddressService` | Service | identity | ⚠️ 跨模块 | OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl |
| `UserService` | Service | identity | ⚠️ 跨模块 | OrderLifecycleServiceImpl |
| `CouponUsageService` | Service | coupon | ⚠️ 跨模块 | OrderServiceImpl, OrderLifecycleServiceImpl |
| `PaymentService` | Service | payment | ⚠️ 跨模块 | OrderLifecycleServiceImpl |
| `InventoryService` | Service | product(inventory) | ⚠️ 跨模块 | OrderServiceImpl, OrderLifecycleServiceImpl |
| `EmailService` | Service | upload | ⚠️ 跨模块 | OrderLifecycleServiceImpl |
| `SeckillOrderService` | Service | seckill | ⚠️ 跨模块 | OrderQueryServiceImpl, AdminOrderServiceImpl, OrderController, OrderCancelConsumer |
| `OrderQueryService` | Service | order | 自身 | OrderLifecycleServiceImpl |
| `SecurityUtils` | 工具类 | identity(security) | ⚠️ 跨模块 | OrderController |
| `ObjectMapper` | Spring | Spring | ✅ 框架 | OrderServiceImpl |

**跨模块 Service 依赖统计：7 个模块（product/identity/cart/coupon/payment/seckill/upload）**

### 1.3 当前问题

#### 跨模块 Entity 引用（order 引用其他模块的 Entity）

| 被引用的 Entity | 所属模块 | 引用位置 | 问题 |
|---|---|---|---|
| `Cart` | cart | OrderServiceImpl | order 直接操作 cart 的 Entity |
| `Product` | product | OrderServiceImpl, OrderQueryServiceImpl | order 直接引用 product Entity |
| `ProductSku` | product | OrderServiceImpl | order 直接引用 product Entity |
| `UserAddress` | identity | OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl | order 直接引用 identity Entity |
| `SeckillOrder` | seckill | OrderQueryServiceImpl | order 查询时直接引用 seckill Entity |

**跨模块 Entity 引用：5 类 Entity，涉及 4 个模块**

#### order 自身 Entity 泄露给外部

| 泄露的 Entity | 引用方 | 泄露位置 | 问题 |
|---|---|---|---|
| `NormalOrder` | payment（WalletServiceImpl） | `OrderService.getWalletPaidOrdersByUser()` 返回 `List<NormalOrder>` | API 返回 Entity |
| `NormalOrder` | review（ProductReviewServiceImpl） | 通过 `OrderService.hasUserPurchasedProduct()` 间接 | 当前返回 boolean，无泄露 |

#### Controller 直接依赖基础设施/跨模块 Service

| Controller | 依赖 | 问题 |
|---|---|---|
| `OrderController` | `SeckillOrderService`（seckill Service） | ⚠️ Controller 直接注入跨模块 Service，应通过 order.application 聚合 |
| `OrderController` | `SecurityUtils`（identity security） | ⚠️ 应通过 shared.kernel.CurrentUserContext |
| `OrderController` | 同时注入 4 个 Service（OrderService/OrderQueryService/OrderLifecycleService/SeckillOrderService） | Controller 承担过多编排职责 |

#### Service 暴露过多

| Service | 问题 |
|---|---|
| `OrderService` | 混合了创建用例 + 购买校验 + 钱包支付订单查询（`getWalletPaidOrdersByUser` 仅供 payment 跨模块调用，且返回 Entity） |
| `OrderController` | 同时是普通订单和秒杀订单的入口，职责过宽 |

---

## 2. Order 目标结构

### 2.1 目标包结构

```
com.seckill.mall.order
├── api/                          # Public API（接口 + DTO + Command + Result）
│   ├── OrderApi.java             # 订单业务能力（创建/支付/取消/发货/确认收货）
│   ├── OrderQueryApi.java        # 订单查询能力（详情/列表/管理员查询/购买校验）
│   ├── dto/                      # 对外数据契约
│   │   ├── OrderDetailDTO.java
│   │   ├── OrderListItemDTO.java
│   │   ├── AdminOrderDTO.java
│   │   ├── OrderStatusDistributionDTO.java
│   │   ├── TrendDTO.java
│   │   └── OrderSnapshot.java    # 订单快照（供 payment 推送用）
│   ├── command/                  # 入参 Command
│   │   ├── CreateOrderCommand.java
│   │   ├── BuyNowCommand.java
│   │   ├── PayOrderCommand.java
│   │   ├── CancelOrderCommand.java
│   │   ├── ShipCommand.java
│   │   ├── ConfirmReceiptCommand.java
│   │   └── DeleteOrderCommand.java
│   ├── query/                    # 查询入参 Query
│   │   ├── OrderListQuery.java
│   │   └── AdminOrderQuery.java
│   └── result/                   # 出参 Result
│       ├── OrderCreateResult.java
│       ├── PayResult.java
│       └── OrderStatusItemDTO.java
├── application/                  # 应用服务（用例编排，实现 api 接口）
│   ├── OrderApplicationService.java    # 实现 OrderApi
│   ├── OrderQueryApplicationService.java  # 实现 OrderQueryApi
│   └── AdminOrderApplicationService.java  # 实现 AdminOrderApi
├── domain/                       # 领域模型（暂保留，初期可空或放 OrderStatus）
│   └── OrderStatus.java          # 订单状态枚举（可后续提升为领域模型）
├── infrastructure/               # 持久化
│   ├── mapper/
│   │   ├── NormalOrderMapper.java
│   │   └── NormalOrderItemMapper.java
│   ├── entity/
│   │   ├── NormalOrder.java      # PO（模块内部可见）
│   │   └── NormalOrderItem.java
│   └── repository/               # Repository（可选，封装 Mapper）
├── interfaces/                   # Controller
│   ├── OrderController.java
│   ├── AdminOrderController.java
│   └── vo/                       # 面向前端的 VO
│       ├── NormalOrderDetailVO.java
│       ├── OrderListItemVO.java
│       ├── AdminOrderVO.java
│       └── ...（前端展示 VO）
└── mq/                           # MQ
    ├── OrderCancelConsumer.java
    └── message/
        └── OrderDelayMessage.java
```

### 2.2 目标依赖规则

| 层 | 允许依赖 | 禁止依赖 |
|---|---|---|
| `order.api` | shared.kernel（仅常量/枚举）、JDK | 任何模块的 infrastructure/mapper/domain/entity |
| `order.application` | order.api、order.domain、order.infrastructure（自身）、其他模块的 **api**、shared.kernel | 其他模块的 infrastructure/mapper/domain/entity |
| `order.infrastructure` | order.domain（自身）、MyBatis-Plus、shared.kernel | 任何业务模块 |
| `order.interfaces` | order.application、order.api.dto/command/query、order.interfaces.vo、shared.kernel（CurrentUserContext） | order.infrastructure（Mapper）、其他模块的 Service |
| `order.mq` | order.application、order.api、shared.kernel（MessageBusPort） | 其他模块的 Service/Mapper |

---

## 3. 文件迁移路径

### 3.1 迁移映射表

| 当前路径 | 目标路径 | 迁移类型 | 说明 |
|---|---|---|---|
| `controller/OrderController.java` | `order/interfaces/OrderController.java` | 改造 | 改为依赖 application 服务，移除 SeckillOrderService 直接注入 |
| `controller/AdminOrderController.java` | `order/interfaces/AdminOrderController.java` | 改造 | 改为依赖 AdminOrderApplicationService |
| `service/OrderService.java` | `order/api/OrderApi.java` | 改造 | 接口转为 API，方法签名改为 Command/Result |
| `service/OrderQueryService.java` | `order/api/OrderQueryApi.java` | 改造 | 接口转为 API |
| `service/OrderLifecycleService.java` | （合并到 OrderApi） | 合并 | 生命周期方法并入 OrderApi，接口删除 |
| `service/AdminOrderService.java` | `order/api/AdminOrderApi.java` | 改造 | 接口转为 API |
| `service/impl/OrderServiceImpl.java` | `order/application/OrderApplicationService.java` | 改造 | 改为依赖外部 api，实现 OrderApi |
| `service/impl/OrderQueryServiceImpl.java` | `order/application/OrderQueryApplicationService.java` | 改造 | 改为依赖外部 api |
| `service/impl/OrderLifecycleServiceImpl.java` | （合并到 OrderApplicationService） | 合并 | 生命周期逻辑并入 OrderApplicationService |
| `service/impl/AdminOrderServiceImpl.java` | `order/application/AdminOrderApplicationService.java` | 改造 | 改为依赖 seckill.api |
| `entity/NormalOrder.java` | `order/infrastructure/entity/NormalOrder.java` | 移动 | PO 归入 infrastructure，模块内部可见 |
| `entity/NormalOrderItem.java` | `order/infrastructure/entity/NormalOrderItem.java` | 移动 | 同上 |
| `entity/enums/OrderStatus.java` | `order/domain/OrderStatus.java` | 移动 | 枚举提升到 domain（可共享给 api.dto） |
| `mapper/NormalOrderMapper.java` | `order/infrastructure/mapper/NormalOrderMapper.java` | 移动 | Mapper 归入 infrastructure |
| `mapper/NormalOrderItemMapper.java` | `order/infrastructure/mapper/NormalOrderItemMapper.java` | 移动 | 同上 |
| `dto/BuyNowRequest.java` | `order/api/command/BuyNowCommand.java` | 改造 | Request → Command |
| `dto/CartCheckoutRequest.java` | `order/api/command/CreateOrderCommand.java` | 改造 | Request → Command |
| `dto/NormalOrderPayRequest.java` | `order/api/command/PayOrderCommand.java` | 改造 | Request → Command |
| `dto/ShipRequest.java` | `order/api/command/ShipCommand.java` | 改造 | Request → Command |
| `dto/AdminOrderQueryRequest.java` | `order/api/query/AdminOrderQuery.java` | 改造 | Request → Query |
| `vo/NormalOrderDetailVO.java` | `order/interfaces/vo/NormalOrderDetailVO.java` | 移动 | 前端 VO 归 interfaces |
| `vo/NormalOrderItemVO.java` | `order/interfaces/vo/NormalOrderItemVO.java` | 移动 | 同上 |
| `vo/NormalOrderVO.java` | `order/interfaces/vo/NormalOrderVO.java` | 移动 | 同上 |
| `vo/AdminOrderVO.java` | `order/interfaces/vo/AdminOrderVO.java` | 移动 | 同上 |
| `vo/OrderListItemVO.java` | `order/interfaces/vo/OrderListItemVO.java` | 移动 | 同上 |
| `vo/OrderStatusItemVO.java` | `order/interfaces/vo/OrderStatusItemVO.java` | 移动 | 同上 |
| `vo/OrderTrendVO.java` | `order/api/dto/TrendDTO.java` | 改造 | VO → DTO（跨模块用 DTO） |
| `vo/OrderStatusDistributionVO.java` | `order/api/dto/OrderStatusDistributionDTO.java` | 改造 | VO → DTO |
| `mq/consumer/OrderCancelConsumer.java` | `order/mq/OrderCancelConsumer.java` | 改造 | 改为依赖 order.application + seckill.api |
| `mq/message/OrderDelayMessage.java` | `order/mq/message/OrderDelayMessage.java` | 移动 | 消息归 order.mq |

### 3.2 新增文件清单

| 新增文件 | 包路径 | 说明 |
|---|---|---|
| `OrderApi.java` | `order.api` | 订单业务能力 API 接口（合并 OrderService + OrderLifecycleService 的写方法） |
| `OrderQueryApi.java` | `order.api` | 订单查询能力 API 接口 |
| `AdminOrderApi.java` | `order.api` | 管理员订单查询 API 接口 |
| `CreateOrderCommand.java` | `order.api.command` | 购物车结算创建订单入参 |
| `BuyNowCommand.java` | `order.api.command` | 立即购买入参 |
| `PayOrderCommand.java` | `order.api.command` | 支付订单入参 |
| `CancelOrderCommand.java` | `order.api.command` | 取消订单入参 |
| `ShipCommand.java` | `order.api.command` | 发货入参 |
| `ConfirmReceiptCommand.java` | `order.api.command` | 确认收货入参 |
| `DeleteOrderCommand.java` | `order.api.command` | 删除订单入参 |
| `OrderListQuery.java` | `order.api.query` | 订单列表查询入参 |
| `AdminOrderQuery.java` | `order.api.query` | 管理员订单查询入参 |
| `OrderCreateResult.java` | `order.api.result` | 创建订单出参 |
| `PayResult.java` | `order.api.result` | 支付出参 |
| `OrderDetailDTO.java` | `order.api.dto` | 订单详情 DTO（供外部模块用） |
| `OrderListItemDTO.java` | `order.api.dto` | 订单列表项 DTO |
| `AdminOrderDTO.java` | `order.api.dto` | 管理员订单 DTO |
| `OrderSnapshot.java` | `order.api.dto` | 订单快照（供 payment 推送 recordPayment 用） |
| `OrderStatusDistributionDTO.java` | `order.api.dto` | 订单状态分布 DTO |
| `TrendDTO.java` | `order.api.dto` | 订单趋势 DTO |
| `OrderStatusItemDTO.java` | `order.api.dto` | 订单状态项 DTO |
| `OrderApplicationService.java` | `order.application` | 实现 OrderApi（合并 OrderServiceImpl + OrderLifecycleServiceImpl） |
| `OrderQueryApplicationService.java` | `order.application` | 实现 OrderQueryApi |
| `AdminOrderApplicationService.java` | `order.application` | 实现 AdminOrderApi |

**新增文件：24 个**

### 3.3 改造文件清单

| 改造文件 | 改造内容 |
|---|---|
| `OrderController` | 移除 `SeckillOrderService` 注入，秒杀订单操作委托 `OrderApplicationService`（内部调 seckill.api）；`SecurityUtils` → `CurrentUserContext`；注入改为 `OrderApplicationService` + `OrderQueryApplicationService` |
| `AdminOrderController` | 注入改为 `AdminOrderApplicationService` |
| `OrderServiceImpl` → `OrderApplicationService` | 字段注入改为外部模块 api（ProductApi/SkuApi/CartApi/CouponApi/AddressApi/PaymentApi/EmailApi/InventoryApi）；Entity 引用改为 DTO；合并 OrderLifecycleServiceImpl 逻辑 |
| `OrderQueryServiceImpl` → `OrderQueryApplicationService` | 字段注入改为外部 api；`SeckillOrder` entity → `SeckillOrderDTO`；`Product`/`UserAddress` entity → DTO |
| `AdminOrderServiceImpl` → `AdminOrderApplicationService` | `SeckillOrderService` → `seckill.api.SeckillOrderApi` |
| `OrderCancelConsumer` | `OrderLifecycleService` → `OrderApplicationService`；`SeckillOrderService` → `seckill.api.SeckillOrderApi` |
| `OrderService` 接口 | 转为 `OrderApi`，`getWalletPaidOrdersByUser()` 返回 `List<OrderSnapshot>` 而非 `List<NormalOrder>` |
| 外部调用方 `WalletServiceImpl`（payment） | `OrderService.getWalletPaidOrdersByUser()` → `order.api.OrderQueryApi.getWalletPaidOrders()` 返回 `List<OrderSnapshot>` |
| 外部调用方 `ProductReviewServiceImpl`（review） | `OrderService.hasUserPurchasedProduct()` → `order.api.OrderQueryApi.hasUserPurchased()` |
| 外部调用方 `AgentService`（ai） | `OrderQueryService.getUnifiedOrderList()` → `order.api.OrderQueryApi.listOrders()` 返回 `List<OrderListItemDTO>` |

**改造文件：10+ 个（含外部调用方）**

---

## 4. API 边界

### 4.1 对外暴露的 API

详细定义见 `ORDER-API-CONTRACT.md`，此处概要列出：

| API 接口 | 方法数 | 暴露给 |
|---|---|---|
| `OrderApi` | 7（createOrder/buyNow/payOrder/cancelOrder/ship/confirmReceipt/deleteOrder） | OrderController, payment, review |
| `OrderQueryApi` | 5（listOrders/getOrderDetail/adminListOrders/hasUserPurchased/getWalletPaidOrders/getStatusDistribution/getOrderTrend） | OrderController, ai, stats, system, payment, review |
| `AdminOrderApi` | 1（adminListOrders） | AdminOrderController |

### 4.2 对内隐藏的实现

| 隐藏项 | 类型 | 说明 |
|---|---|---|
| `NormalOrder` / `NormalOrderItem` | Entity/PO | 仅 order.infrastructure 内部可见 |
| `NormalOrderMapper` / `NormalOrderItemMapper` | Mapper | 仅 order.infrastructure 内部可见 |
| `OrderApplicationService` | ApplicationService | 实现细节，不对外暴露 |
| `OrderQueryApplicationService` | ApplicationService | 实现细节 |
| `AdminOrderApplicationService` | ApplicationService | 实现细节 |
| `OrderLifecycleService` | （合并删除） | 生命周期逻辑并入 OrderApplicationService |
| `OrderCancelConsumer` | MQ Consumer | 内部消息消费 |
| `OrderDelayMessage` | MQ Message | 内部消息 |

---

## 5. 外部模块依赖

### 5.1 order 依赖的外部模块 API

| 外部模块 | 当前依赖方式 | 目标依赖方式 | 需对方提供的 API |
|---|---|---|---|
| **product** | `ProductService` + `ProductSkuService` + `InventoryService` + `Product`/`ProductSku` entity | `product.api.ProductApi` + `product.api.SkuApi` + `product.api.InventoryApi` | `getProductById()` → `ProductSnapshot`；`getSkuById()` → `SkuSnapshot`；`deduct()`/`restore()` |
| **identity** | `UserService` + `UserAddressService` + `UserAddress` entity + `SecurityUtils` | `identity.api.UserApi` + `identity.api.AddressApi` + `shared.kernel.CurrentUserContext` | `getUserById()` → `UserDTO`；`getAddress()` → `AddressDTO`；`getCurrentUser()` |
| **cart** | `CartService` + `Cart` entity | `cart.api.CartApi` | `prepareCheckout()` → `CheckoutPreview`（含商品快照+价格+库存） |
| **coupon** | `CouponUsageService` | `coupon.api.CouponApi` | `useCoupon()` → `CouponUseResult`；`returnCoupon()` |
| **payment** | `PaymentService` | `payment.api.PaymentApi` | `pay()` → `PayResult` |
| **seckill** | `SeckillOrderService` + `SeckillOrder` entity | `seckill.api.SeckillOrderApi` | `getSeckillOrder()` → `SeckillOrderDTO`；`listSeckillOrders()`；`timeoutCancel()` |
| **upload** | `EmailService` | `upload.api.EmailApi` | `sendEmail()` |
| **shared.kernel** | `MessageBusPort` | `shared.kernel.MessageBusPort` | `publish()`（已使用） |

### 5.2 依赖 order 的外部模块

| 外部模块 | 当前调用 | 目标调用 | 需 order 提供的 API |
|---|---|---|---|
| **payment**（WalletServiceImpl） | `OrderService.getWalletPaidOrdersByUser()` → `List<NormalOrder>` | `order.api.OrderQueryApi.getWalletPaidOrders()` → `List<OrderSnapshot>` | `getWalletPaidOrders(UserId)` |
| **review**（ProductReviewServiceImpl） | `OrderService.hasUserPurchasedProduct()` | `order.api.OrderQueryApi.hasUserPurchased()` | `hasUserPurchased(UserId, ProductId, SkuId)` |
| **ai**（AgentService） | `OrderQueryService.getUnifiedOrderList()` | `order.api.OrderQueryApi.listOrders()` | `listOrders(OrderListQuery)` |
| **stats** | （当前无直接调用，目标新增） | `order.api.OrderQueryApi.getStatusDistribution()` / `getOrderTrend()` | `getStatusDistribution()`；`getOrderTrend()` |
| **system** | （当前无直接调用，目标新增） | `order.api.OrderQueryApi.getStatusDistribution()` | `getStatusDistribution()` |

### 5.3 同步需定义的 API 契约（最小契约集）

order 迁移时，需同步定义以下外部模块的最小 API 契约（order 迁移的前置条件）：

| 外部模块 | 需定义的 API 方法 | 返回类型 | 紧迫性 |
|---|---|---|---|
| `product.api.ProductApi` | `getProductById(ProductId)` | `ProductSnapshot` | P0（order 创建用例必需） |
| `product.api.SkuApi` | `getSkuById(SkuId)` | `SkuSnapshot` | P0 |
| `product.api.InventoryApi` | `deduct(DeductStockCommand)` / `restore(RestoreStockCommand)` | `DeductResult` / `void` | P0 |
| `identity.api.AddressApi` | `getAddress(UserId, AddressId)` | `AddressDTO` | P0 |
| `identity.api.UserApi` | `getUserById(UserId)` | `UserDTO` | P1（支付用例需要） |
| `cart.api.CartApi` | `prepareCheckout(CheckoutCommand)` | `CheckoutPreview` | P0 |
| `coupon.api.CouponApi` | `useCoupon(UseCouponCommand)` / `returnCoupon(ReturnCouponCommand)` | `CouponUseResult` / `void` | P0 |
| `payment.api.PaymentApi` | `pay(PayCommand)` | `PayResult` | P0 |
| `seckill.api.SeckillOrderApi` | `listSeckillOrders(SeckillOrderQuery)` / `getSeckillOrder(OrderId)` / `timeoutCancel(OrderId)` | `PageResult<SeckillOrderDTO>` / `SeckillOrderDTO` / `boolean` | P1（统一列表/管理员查询/消费者需要） |
| `upload.api.EmailApi` | `sendEmail(SendEmailCommand)` | `void` | P1（取消通知） |
| `shared.kernel.CurrentUserContext` | `getCurrentUser()` | `CurrentUser` | P0（替代 SecurityUtils） |

---

## 6. 迁移步骤

### Step 1: 创建 order 模块包结构

**操作**：创建以下空包（含 `package-info.java`）：
```
com.seckill.mall.order
com.seckill.mall.order.api
com.seckill.mall.order.api.dto
com.seckill.mall.order.api.command
com.seckill.mall.order.api.query
com.seckill.mall.order.api.result
com.seckill.mall.order.application
com.seckill.mall.order.domain
com.seckill.mall.order.infrastructure
com.seckill.mall.order.infrastructure.mapper
com.seckill.mall.order.infrastructure.entity
com.seckill.mall.order.interfaces
com.seckill.mall.order.interfaces.vo
com.seckill.mall.order.mq
com.seckill.mall.order.mq.message
```

**影响文件**：新增约 15 个 `package-info.java`
**验证方式**：`mvn compile` 通过（空包不影响编译）
**可回滚性**：删除新增包即可

### Step 2: 定义 order.api（接口 + DTO + Command + Result）

**操作**：创建 API 接口和数据契约（详见 `ORDER-API-CONTRACT.md`）：
- `OrderApi`、`OrderQueryApi`、`AdminOrderApi` 接口
- 10 个 Command、2 个 Query、3 个 Result、7 个 DTO
- 暂无实现，仅定义契约

**影响文件**：新增 24 个文件（见 3.2 新增文件清单）
**验证方式**：`mvn compile` 通过（接口和 DTO 无依赖循环）
**可回滚性**：删除新增文件即可

### Step 3: 迁移 infrastructure（Mapper + Entity）

**操作**：
1. 移动 `NormalOrder`、`NormalOrderItem` → `order.infrastructure.entity`
2. 移动 `NormalOrderMapper`、`NormalOrderItemMapper` → `order.infrastructure.mapper`
3. 移动 `OrderStatus` → `order.domain`
4. 更新所有引用这些类的文件的 import（MyBatis XML namespace 需同步更新）

**影响文件**：4 个移动 + 约 15 个 import 更新（OrderServiceImpl 等）
**验证方式**：`mvn compile` 通过 + `mvn test` 通过（Mapper 测试）
**可回滚性**：`git revert` 此步提交

### Step 4: 迁移 application（Service → ApplicationService）

**操作**：
1. 创建 `OrderApplicationService`（合并 `OrderServiceImpl` + `OrderLifecycleServiceImpl`），实现 `OrderApi`
2. 创建 `OrderQueryApplicationService`（改造 `OrderQueryServiceImpl`），实现 `OrderQueryApi`
3. 创建 `AdminOrderApplicationService`（改造 `AdminOrderServiceImpl`），实现 `AdminOrderApi`
4. 字段注入改为外部模块 api（ProductApi/SkuApi/CartApi 等）
5. Entity 引用改为 DTO（通过外部 api 返回 DTO）
6. 删除原 `OrderService`/`OrderQueryService`/`OrderLifecycleService`/`AdminOrderService` 接口及 impl

**影响文件**：3 个新增 + 8 个删除 + 外部调用方 import 更新
**验证方式**：`mvn compile` 通过 + `mvn test` 通过
**可回滚性**：`git revert` 此步提交（最大的一步，建议拆为 4a/4b/4c 三个子步）

> **建议拆分**：
> - Step 4a：创建 3 个 ApplicationService（复制原 impl 逻辑，暂保留原 Service）
> - Step 4b：切换 Controller/Consumer 到 ApplicationService
> - Step 4c：删除原 Service 接口和 impl

### Step 5: 迁移 interfaces（Controller）

**操作**：
1. 移动 `OrderController` → `order.interfaces`，改为注入 `OrderApplicationService` + `OrderQueryApplicationService`
2. 移动 `AdminOrderController` → `order.interfaces`，改为注入 `AdminOrderApplicationService`
3. `OrderController` 移除 `SeckillOrderService` 直接注入，秒杀订单操作委托 `OrderApplicationService`（内部调 seckill.api）
4. `SecurityUtils` → `shared.kernel.CurrentUserContext`
5. 移动前端 VO → `order.interfaces.vo`

**影响文件**：2 个改造 + 8 个 VO 移动
**验证方式**：`mvn compile` 通过 + API 接口测试
**可回滚性**：`git revert` 此步提交

### Step 6: 迁移 mq（OrderCancelConsumer）

**操作**：
1. 移动 `OrderCancelConsumer` → `order.mq`，改为注入 `OrderApplicationService` + `seckill.api.SeckillOrderApi`
2. 移动 `OrderDelayMessage` → `order.mq.message`

**影响文件**：2 个移动 + import 更新
**验证方式**：`mvn compile` 通过 + MQ 集成测试
**可回滚性**：`git revert` 此步提交

### Step 7: 更新外部调用方

**操作**：
1. `WalletServiceImpl`（payment）：`OrderService.getWalletPaidOrdersByUser()` → `order.api.OrderQueryApi.getWalletPaidOrders()`，返回 `List<OrderSnapshot>`
2. `ProductReviewServiceImpl`（review）：`OrderService.hasUserPurchasedProduct()` → `order.api.OrderQueryApi.hasUserPurchased()`
3. `AgentService`（ai）：`OrderQueryService.getUnifiedOrderList()` → `order.api.OrderQueryApi.listOrders()`

**影响文件**：3 个外部文件改造
**验证方式**：`mvn compile` 通过 + 相关模块测试
**可回滚性**：`git revert` 此步提交

### Step 8: ArchUnit 规则验证

**操作**：
1. 更新 `ArchitectureRulesTest`，新增 order 模块的包边界规则
2. 运行 `mvn test -Dtest=ArchitectureRulesTest`
3. 确认无违规依赖

**影响文件**：1 个测试文件更新
**验证方式**：ArchUnit 测试全绿
**可回滚性**：`git revert` 此步提交

---

## 7. 风险点

| 风险 | 等级 | 影响 | 缓解措施 |
|---|---|---|---|
| **import 大量修改导致编译错误** | 高 | 28 个文件移动 + 10+ 个改造，import 路径全变 | 分步迁移（Step 3-6），每步编译验证；IDE 批量更新 import |
| **跨模块 Entity 引用需同步改造** | 高 | order 引用 5 类外部 Entity（Cart/Product/ProductSku/UserAddress/SeckillOrder），需外部模块先提供 DTO | Step 4 前确认外部模块最小 API 契约集（5.3 节）已定义 |
| **OrderController 承担秒杀订单入口** | 高 | OrderController 同时是普通订单和秒杀订单的 Controller，迁移时需保留秒杀订单路由 | Step 5 中 OrderApplicationService 内部调 seckill.api，Controller 路由不变 |
| **MyBatis XML namespace 需同步更新** | 中 | NormalOrderMapper/NormalOrderItemMapper 移动后，XML 的 namespace 需更新 | Step 3 中同步更新 XML；检查 `resources/mapper/*.xml` |
| **OrderService.getWalletPaidOrdersByUser 返回 Entity** | 中 | payment/WalletServiceImpl 依赖返回 `List<NormalOrder>`，改为 `List<OrderSnapshot>` 需同步改造 | Step 7 同步更新 WalletServiceImpl |
| **OrderLifecycleService 合并到 OrderApplicationService** | 中 | 合并两个 ServiceImpl（536+423 行），可能超 500 行 God Service 限制 | 合并后检查行数，必要时按用例进一步拆分（创建/支付/取消 子服务） |
| **测试可能失败** | 中 | 现有测试可能直接依赖 OrderService 接口或 NormalOrder entity | Step 4 后运行全量测试，修复测试 import |
| **ArchUnit 规则可能报新违规** | 低 | 新包结构可能触发已有 ArchUnit 规则 | Step 8 更新规则，逐步修复 |
| **Spring Bean 注入冲突** | 低 | 合并 Service 后 bean 名称可能冲突 | 使用 `@Service("orderApplicationService")` 显式命名 |

---

## 8. 回滚策略

### 8.1 每 Step 的回滚方式

| Step | 回滚方式 | 说明 |
|---|---|---|
| Step 1 | 删除新增空包 | 无代码影响 |
| Step 2 | 删除新增 API/DTO 文件 | 无代码影响（未引用） |
| Step 3 | `git revert` | Mapper/Entity 移动涉及 import，需整体回滚 |
| Step 4 | `git revert`（或拆为 4a/4b/4c 分别 revert） | 最大的一步，建议每子步单独提交 |
| Step 5 | `git revert` | Controller 改造 |
| Step 6 | `git revert` | MQ 改造 |
| Step 7 | `git revert` | 外部调用方改造 |
| Step 8 | `git revert` | ArchUnit 规则更新 |

### 8.2 紧急回滚

如果迁移导致严重问题（编译失败/测试全红/运行时错误）：

```bash
# 回滚到迁移前最后一个绿色提交
git log --oneline -20          # 找到迁移前的 commit
git revert <commit>..HEAD      # 回滚所有迁移提交
# 或
git reset --hard <迁移前commit>  # 硬回滚（丢弃所有迁移变更）
```

**前提**：每个 Step 单独提交，提交信息标注 `Phase 2.5 Order Step N`。

### 8.3 部分回滚

如果某个 Step 失败，保留之前成果：

| 失败的 Step | 回滚范围 | 保留的成果 |
|---|---|---|
| Step 4 失败 | 仅 revert Step 4 | Step 1-3（包结构 + API + infrastructure）保留 |
| Step 5 失败 | 仅 revert Step 5 | Step 1-4 保留（application 已迁移，Controller 暂未切换） |
| Step 7 失败 | 仅 revert Step 7 | Step 1-6 保留（order 已迁移，外部调用方暂未更新，需临时保留旧 Service 接口作为适配层） |

**关键**：Step 4 建议拆为 4a/4b/4c，4a 创建新 ApplicationService 时**保留原 Service**，4b 切换 Controller 后原 Service 暂时无引用但保留，4c 确认无误后再删除原 Service。这样 4b 失败时可回滚到 4a（原 Service 仍在）。

---

> **本文件为 Phase 2.5 Order 模块迁移前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **下一步**：基于本迁移计划，生成 `ORDER-API-CONTRACT.md`（API 契约详细定义）。