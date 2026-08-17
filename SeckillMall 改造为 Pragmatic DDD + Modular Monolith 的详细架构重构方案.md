# SeckillMall 改造为 Pragmatic DDD + Modular Monolith 的详细架构重构方案

## 1. 文档目标

本文用于指导 SeckillMall 从当前“传统分层单体 + 多功能持续堆叠”的代码组织方式，逐步改造成：

> **Pragmatic DDD + Modular Monolith + Ports & Adapters**

的架构模式。

本方案不建议一次性推倒重写，也不建议立即拆成 Spring Cloud 微服务，而是采用**渐进式重构（Strangler / Incremental Refactoring）**：

```text
现有单体
  ↓
建立架构规则
  ↓
划分业务模块
  ↓
模块内部重构
  ↓
隔离基础设施
  ↓
消除重复实现
  ↓
建立模块 API
  ↓
测试和架构治理
  ↓
形成 Modular Monolith
  ↓
根据真实运行情况决定未来是否拆微服务
```

改造最终目标不是“目录看起来更漂亮”，而是解决以下实际问题：

- 修改一个业务导致大量无关模块跟着修改；
- Service 层职责过重，出现 God Service；
- Controller、Service、Mapper、Entity 在全局目录中散落；
- `common`、`utils`、`service` 逐渐成为无法控制的公共代码仓；
- 同一业务函数或规则在多个文件中重复实现；
- 业务代码直接依赖 MyBatis、Redis、RabbitMQ、Spring Security；
- AI、秒杀、订单、埋点等模块互相直接调用；
- 很难判断一个功能到底属于哪个业务模块；
- 后续如果需要拆微服务，需要重新改造大量代码。

---

# 2. 当前架构改造原则

## 2.1 第一原则：暂时不拆微服务

当前项目功能增长很快，Git 历史已经从商城/秒杀扩展到微信小程序、用户埋点、AI Gateway、AI 导购、AI 客服和 AIGC。

因此第一阶段必须解决：

> **“业务边界不清晰”**

而不是：

> “服务数量不够多”。

如果现在直接拆微服务，很可能从：

```text
一个混乱的大单体
```

变成：

```text
十几个互相调用的混乱微服务
```

因此：

> **先模块化，后服务化。**

---

# 3. 最终目标架构

目标架构：

```text
                              SeckillMall
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
       Business                 AI                      Analytics
       Modules                 Platform                  Platform
         │                        │                        │
 ┌───────┼────────┐       ┌───────┼────────┐        ┌─────┴─────┐
 │       │        │       │       │        │        │           │
User  Product   Order   Gateway Assistant AIGC   Tracking   Future BI
 │       │        │       │       │        │
 └───────┼────────┘       └───────┼────────┘
         │                        │
         └──────────┬─────────────┘
                    │
              Module Public API
                    │
          ┌─────────┼─────────┐
          │         │         │
        MySQL     Redis     RabbitMQ
```

整个系统仍然：

```text
一个 Spring Boot 应用
一个 JVM
一个部署单元
```

但代码结构已经变成：

```text
多个业务模块
+
明确模块边界
+
模块公开 API
+
基础设施隔离
```

---

# 4. 什么叫 Pragmatic DDD

本项目不采用“纯理论 DDD”。

只采用 DDD 中真正有价值的部分：

### 要做

- Bounded Context / 业务模块边界
- Aggregate / 聚合
- Entity / 实体
- Value Object / 值对象
- Domain Service / 领域服务
- Repository 抽象
- Application Service / 用例编排
- Domain Event / 领域事件
- 业务规则归属明确

### 不强制做

- 所有对象都必须 Value Object；
- 所有业务都必须 Domain Event；
- 所有操作都必须 Factory；
- 所有查询都必须 CQRS；
- 所有模块都必须完整 DDD 四层。

原则是：

> **业务复杂的地方使用 DDD，CRUD 简单模块保持简单。**

---

# 5. 什么叫 Modular Monolith

每个业务模块拥有自己的代码边界：

```text
order
product
user
coupon
seckill
ai
tracking
```

模块之间只能通过：

```text
Public API
+
Application API
+
Domain Event
```

协作。

禁止直接访问：

```text
其他模块 Mapper
其他模块 Entity
其他模块 Repository
其他模块 Infrastructure
其他模块 Redis
其他模块 MQ
```

例如：

```text
Order
 ├── 可以调用 ProductQuery
 ├── 可以调用 CouponQuery
 └── 可以调用 InventoryPort

Order
 ├── 禁止调用 ProductMapper
 ├── 禁止调用 CouponMapper
 └── 禁止直接操作 ProductEntity
```

---

# 6. 项目模块划分方案

结合当前 SeckillMall 的业务结构，建议划分为以下模块。

## 6.1 Identity 模块

负责：

- 用户
- 登录
- 注册
- JWT
- 角色
- 权限
- 当前用户上下文

目录：

```text
identity/
├── api/
├── application/
├── domain/
└── infrastructure/
```

不负责：

- 商品
- 订单
- 购物车
- 优惠券

---

# 7. Product 模块

负责：

- 商品
- SKU
- 商品详情
- 商品分类基础查询
- 商品上下架
- 商品基础信息

```text
product/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Product 对外提供：

```java
ProductQuery
ProductCommand
SkuQuery
```

而不是让其他模块直接引用：

```java
ProductMapper
ProductEntity
ProductServiceImpl
```

---

# 8. Inventory 模块

这是本次重构必须独立出来的模块。

负责：

- 商品库存
- SKU 库存
- 秒杀库存
- 库存扣减
- 库存恢复
- 库存同步
- 库存预占

核心接口：

```java
public interface InventoryPort {

    ReserveResult reserve(
        InventoryItemId itemId,
        Quantity quantity
    );

    void release(
        InventoryReservationId reservationId
    );

    void confirm(
        InventoryReservationId reservationId
    );
}
```

这样以后：

```text
秒杀
普通订单
购物车结算
预售
```

都可以使用库存能力。

---

# 9. Promotion / Seckill 模块

秒杀不要直接塞进 Product。

因为：

```text
Product = 商品是什么
Seckill = 商品在某个时间如何被抢购
```

两者完全是不同的业务概念。

建议：

```text
promotion/
└── seckill/
    ├── api/
    ├── application/
    ├── domain/
    │   ├── SeckillActivity
    │   ├── SeckillRule
    │   ├── SeckillOrder
    │   └── SeckillPolicy
    └── infrastructure/
```

秒杀模块依赖：

```text
ProductQuery
InventoryPort
OrderCommand
```

而不是：

```text
ProductMapper
OrderMapper
RedisTemplate
RabbitTemplate
```

---

# 10. Order 模块

Order 是建议优先进行完整 DDD 改造的模块。

负责：

- 普通订单
- 订单状态
- 订单创建
- 取消
- 支付状态
- 订单查询
- 订单生命周期

目录：

```text
order/
├── api/
├── application/
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/
└── infrastructure/
    ├── persistence/
    └── messaging/
```

核心领域对象：

```text
Order
OrderItem
OrderStatus
OrderPrice
OrderId
```

核心用例：

```text
CreateOrder
CancelOrder
PayOrder
ConfirmOrder
QueryOrder
```

---

# 11. Payment 模块

负责：

- 支付
- 支付状态
- 支付回调
- 支付超时
- 支付流水

不要让 Order 直接操作支付数据库。

应该：

```text
Order
 ↓
PaymentCommand
 ↓
Payment
```

未来如果换：

```text
微信支付
支付宝
余额支付
第三方支付
```

Order 本身不应该发生结构性变化。

---

# 12. Coupon 模块

负责：

- 优惠券
- 发放
- 领取
- 使用
- 失效
- 优惠计算

不要让：

```text
OrderService
ProductService
SeckillService
```

分别重新写：

```text
couponDiscount()
```

所有价格规则统一进入：

```text
CouponPricingPolicy
```

或：

```text
PromotionPricingService
```

成为单一事实来源。

---

# 13. Cart 模块

负责：

- 加购物车
- 删除
- 修改数量
- 查询购物车
- 勾选商品

Cart 不应该自己直接处理：

```text
库存数据库
商品 Mapper
优惠券 Mapper
```

而应该调用：

```text
ProductQuery
InventoryQuery
```

---

# 14. Review 模块

负责：

- 商品评价
- 评分
- 评价图片
- 评价审核
- 评价查询

复杂度相对较低，可以采用简化结构：

```text
review/
├── api/
├── application/
├── domain/
└── infrastructure/
```

不需要过度 DDD。

---

# 15. AI 平台模块

当前项目 AI 能力已经明显形成平台趋势，因此单独维护。

```text
ai/
├── gateway/
├── assistant/
├── customer-service/
└── aigc/
```

其中：

```text
gateway
```

负责：

- 模型路由
- 限流
- Prompt Cache
- 审计
- Budget
- Fallback
- Streaming

```text
assistant
```

负责：

- AI 导购
- Function Calling
- Product Tool
- 对话历史

```text
customer-service
```

负责：

- FAQ
- 查单
- Agent
- 转人工
- SSE

```text
aigc
```

负责：

- 商品标题
- 商品描述
- SEO
- 商品详情

---

# 16. Tracking 模块

埋点必须是独立旁路能力。

```text
analytics/
└── tracking/
    ├── api/
    ├── application/
    ├── domain/
    └── infrastructure/
```

业务只产生：

```text
UserEvent
```

然后：

```text
AOP
 ↓
TrackingEvent
 ↓
RabbitMQ
 ↓
Tracking Consumer
 ↓
Database
```

业务核心流程绝不能依赖：

```text
TrackMapper
TrackConsumer
TrackingDatabase
```

---

# 17. Infrastructure 模块

基础设施统一集中：

```text
infrastructure/
├── mysql/
├── redis/
├── rabbitmq/
├── security/
├── web/
├── observability/
└── scheduler/
```

这里允许：

```text
Spring
MyBatis
Redis
RabbitMQ
Micrometer
```

业务 Domain 层不允许直接依赖这些实现。

---

# 18. 最终目录结构

最终建议：

```text
seckill-mall/
│
├── app/
│   └── SeckillMallApplication.java
│
├── modules/
│   ├── identity/
│   ├── product/
│   ├── inventory/
│   ├── cart/
│   ├── order/
│   ├── payment/
│   ├── coupon/
│   ├── promotion/
│   │   └── seckill/
│   └── review/
│
├── ai/
│   ├── gateway/
│   ├── assistant/
│   ├── customer-service/
│   └── aigc/
│
├── analytics/
│   └── tracking/
│
├── infrastructure/
│   ├── mysql/
│   ├── redis/
│   ├── rabbitmq/
│   ├── security/
│   ├── web/
│   ├── observability/
│   └── scheduler/
│
└── shared/
    ├── kernel/
    └── web/
```

---

# 19. 模块内部标准目录

业务复杂模块统一采用：

```text
module/
├── api/
├── application/
├── domain/
└── infrastructure/
```

具体含义：

### api

给外部使用。

包括：

```text
Controller
Request
Response
Public API
```

### application

负责“一个完整业务用例怎么执行”。

例如：

```text
CreateOrderUseCase
CancelOrderUseCase
```

### domain

负责：

```text
实体
值对象
业务规则
领域服务
Repository Interface
Domain Event
```

### infrastructure

负责：

```text
MyBatis
Redis
RabbitMQ
第三方接口
文件
```

---

# 20. 模块内部依赖规则

严格执行：

```text
api
 ↓
application
 ↓
domain
 ↓
port
 ↓
infrastructure
```

其中：

```text
domain → infrastructure
```

禁止。

```text
domain → Redis
```

禁止。

```text
domain → MyBatis
```

禁止。

```text
domain → RabbitTemplate
```

禁止。

```text
domain → Spring Security
```

禁止。

---

# 21. Entity、PO、DTO、VO 必须分离

这是重构中的核心规范。

建议：

```text
domain/Order.java
```

表示业务对象。

```text
infrastructure/persistence/OrderPO.java
```

表示数据库对象。

```text
api/OrderResponse.java
```

表示 HTTP 输出。

```text
application/CreateOrderCommand.java
```

表示用例输入。

不要再让：

```text
OrderEntity
```

同时承担：

```text
Domain
+
Database
+
API
```

三个职责。

---

# 22. Repository 改造方式

当前如果是：

```text
OrderService
 ↓
OrderMapper
```

改成：

```text
OrderApplicationService
 ↓
OrderRepository
 ↓
MyBatisOrderRepository
 ↓
OrderMapper
```

例如：

```java
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    Order save(Order order);

    void delete(OrderId id);
}
```

基础设施：

```java
@Component
class MyBatisOrderRepository implements OrderRepository {
    ...
}
```

领域层永远依赖接口。

---

# 23. Redis 改造方式

原来：

```java
redisTemplate.opsForValue().decrement(...)
```

可能散落在 Service 中。

重构为：

```java
public interface SeckillInventoryPort {

    DeductResult deduct(
        SeckillId seckillId,
        UserId userId
    );

    void rollback(
        SeckillId seckillId,
        UserId userId
    );
}
```

Redis Lua：

```text
RedisSeckillInventoryAdapter
```

封装在：

```text
infrastructure/redis/
```

这样业务代码只知道：

> “扣库存”

不知道：

> “Redis 用哪个 Key、哪个 Lua、哪个 TTL”。

---

# 24. RabbitMQ 改造方式

不要：

```java
rabbitTemplate.convertAndSend(...)
```

散落整个工程。

统一定义：

```java
public interface EventPublisher {
    void publish(DomainEvent event);
}
```

RabbitMQ 实现：

```text
RabbitEventPublisher
```

放：

```text
infrastructure/rabbitmq
```

---

# 25. 当前重复函数治理方案

建立“单一事实来源”规则。

例如：

```text
getCurrentUser()
getUserId()
extractUserId()
resolveCurrentUser()
```

全部审查。

最后只保留：

```java
CurrentUserContext
```

例如：

```java
public interface CurrentUserContext {
    Long userId();
    String username();
    Set<String> roles();
}
```

所有：

```text
Order
AI
Tracking
Coupon
```

都使用它。

---

# 26. Utils 重构规则

现有 `utils` 必须进行分类。

例如：

```text
PriceUtil
```

如果里面是业务价格规则：

```text
promotion/domain/pricing
```

如果是纯数学工具：

```text
shared/kernel
```

如果是 HTTP 工具：

```text
infrastructure/web
```

如果是 JWT：

```text
infrastructure/security
```

原则：

> **不能因为“不知道放哪里”就放进 utils。**

---

# 27. Common 重构规则

原：

```text
common/
```

建议缩小为：

```text
shared/
├── kernel/
└── web/
```

只允许放真正稳定的共享能力。

例如：

```text
ErrorCode
BizException
PageQuery
PageResult
Id
Clock
```

不允许出现：

```text
OrderUtil
ProductUtil
CouponUtil
UserService
RedisService
```

---

# 28. Module API 设计

每个模块都必须定义“对外允许什么”。

例如 Product：

```java
public interface ProductQuery {

    ProductSnapshot findById(ProductId id);

    SkuSnapshot findSku(SkuId id);
}
```

Order 只能调用：

```text
ProductQuery
```

而不能：

```text
ProductMapper
ProductRepository
ProductEntity
```

这就是模块封装。

---

# 29. 跨模块调用规则

允许：

```text
Order
 ↓
ProductQuery
```

禁止：

```text
Order
 ↓
ProductMapper
```

允许：

```text
Seckill
 ↓
InventoryPort
```

禁止：

```text
Seckill
 ↓
RedisTemplate
```

允许：

```text
CustomerService
 ↓
OrderQuery
```

禁止：

```text
CustomerService
 ↓
OrderMapper
```

---

# 30. 什么时候用 Domain Event

只在业务真正存在“后置反应”时使用。

例如：

```text
OrderCreated
OrderPaid
OrderCancelled
SeckillSucceeded
UserRegistered
```

然后：

```text
OrderPaid
 ├── Tracking
 ├── Notification
 └── Points
```

Order 不需要：

```text
OrderService
  ↓
TrackingService
  ↓
EmailService
  ↓
PointsService
```

这样会越来越耦合。

---

# 31. 秒杀模块的最终结构

建议：

```text
promotion/seckill/
├── api/
│   └── SeckillController
│
├── application/
│   ├── SubmitSeckillCommand
│   └── SeckillApplicationService
│
├── domain/
│   ├── SeckillActivity
│   ├── SeckillRule
│   ├── SeckillResult
│   ├── SeckillPolicy
│   └── SeckillRepository
│
└── infrastructure/
    ├── redis/
    │   ├── SeckillLuaAdapter
    │   └── RedisSeckillRepository
    │
    ├── messaging/
    │   ├── SeckillOrderProducer
    │   └── SeckillOrderConsumer
    │
    └── persistence/
```

这样可以做到：

```text
秒杀业务规则
```

与：

```text
Redis
RabbitMQ
MySQL
```

完全分开。

---

# 32. AI Gateway 的最终结构

```text
ai/gateway/
├── api/
│   └── AiGatewayClient
│
├── application/
│   └── AiGatewayService
│
├── domain/
│   ├── AiScene
│   ├── AiRequest
│   ├── AiResponse
│   ├── AiUsage
│   └── AiPolicy
│
└── infrastructure/
    ├── model/
    ├── cache/
    ├── rate-limit/
    ├── budget/
    ├── audit/
    └── fallback/
```

AI 导购只能依赖：

```text
AiGatewayClient
```

不能直接：

```text
ChatClient
RedisTemplate
RabbitTemplate
```

---

# 33. AIGC 和 AI 客服不能直接复用业务 Mapper

例如 AI 客服需要查订单。

不要：

```text
CustomerService
 ↓
OrderMapper
```

应该：

```text
CustomerService
 ↓
OrderQuery
 ↓
Order Module
```

这样 AI 层只是一个业务消费者。

---

# 34. 埋点系统改造

推荐：

```text
Business Method
 ↓
@Tracking
 ↓
TrackingAspect
 ↓
TrackingCommand
 ↓
Async Event Publisher
 ↓
RabbitMQ
 ↓
TrackConsumer
 ↓
Analytics DB
```

并保证：

```text
Tracking Failure
        ↓
不能影响
        ↓
Business Transaction
```

---

# 35. 统一异常体系

所有模块统一定义：

```text
shared/kernel/
├── BizException
├── ErrorCode
└── DomainException
```

例如：

```text
PRODUCT_NOT_FOUND
ORDER_NOT_FOUND
ORDER_CANNOT_CANCEL
SECKILL_NOT_STARTED
SECKILL_SOLD_OUT
COUPON_EXPIRED
```

不要每个模块创建：

```text
ProductException
OrderException
CouponException
```

然后各自写一套异常处理逻辑。

模块可以有自己的 ErrorCode，但底层异常模型必须统一。

---

# 36. Controller 必须变薄

Controller 最终目标：

```java
@PostMapping
public OrderResponse create(@RequestBody CreateOrderRequest request) {
    return orderApplicationService.create(
        request.toCommand()
    );
}
```

Controller 不允许做：

```text
库存计算
优惠计算
订单状态判断
Redis 操作
MQ 操作
业务条件判断
```

Controller 只负责：

```text
HTTP
↓
Command
↓
Use Case
↓
Response
```

---

# 37. Application Service 不承担领域规则

Application Service 负责：

```text
调用哪个领域对象
事务边界
跨模块协调
发布事件
```

不应该把所有业务判断都写进去。

错误：

```java
if (stock > 0
    && coupon != null
    && user.vip()
    && time...
)
```

长期会变成 God Service。

应该把规则放进：

```text
Order
PricingPolicy
SeckillPolicy
CouponPolicy
```

---

# 38. 事务边界设计

一个 Use Case 一个主要事务边界。

例如：

```text
CreateOrderUseCase
        ↓
@Transactional
```

但不要把：

```text
MQ
远程 API
AI
邮件
埋点
```

全部放进一个数据库事务。

否则：

```text
DB Transaction
   ↓
等待 AI
   ↓
等待 MQ
   ↓
等待 HTTP
```

非常危险。

---

# 39. AI 不能参与核心交易事务

例如：

```text
创建订单
 ↓
AI 生成文案
 ↓
订单提交
```

禁止。

应该：

```text
Order Commit
 ↓
OrderCreated Event
 ↓
AI / Notification / Tracking
```

AI 属于外围能力。

---

# 40. 重构时间计划

以下按照一个初级到中级开发团队进行设计。

## 第 0 周：代码盘点

目标：

> 不修改业务，只建立认知。

工作：

1. 统计所有 package；
2. 统计 Controller；
3. 统计 Service；
4. 统计 Mapper；
5. 统计 Entity；
6. 搜索重复函数；
7. 搜索 `utils`；
8. 搜索 `common`；
9. 搜索跨模块 Mapper 调用；
10. 搜索 RedisTemplate；
11. 搜索 RabbitTemplate；
12. 搜索直接依赖 SecurityContext 的代码。

产出：

```text
ARCHITECTURE.md
DEPENDENCY-MATRIX.md
DUPLICATION-REPORT.md
```

---

# 41. 第 1 周：建立架构规则

不重构业务。

新增：

```text
ArchUnit
Checkstyle / Spotless
Sonar 或 PMD
```

先建立规则：

```text
domain 禁止依赖 infrastructure
order 禁止直接访问 product.mapper
api 禁止依赖 mapper
```

目标：

> **先阻止架构继续恶化。**

---

# 42. 第 2 周：建立 Shared Kernel

统一：

```text
BizException
ErrorCode
PageResult
PageQuery
CurrentUserContext
Id
Clock
```

清理重复：

```text
getUserId
getCurrentUser
parseToken
```

目标：

> 消灭第一批重复代码。

---

# 43. 第 3 周：改造 Identity

Identity 是其他模块依赖最多的模块之一。

先建立：

```text
identity/api
identity/application
identity/domain
identity/infrastructure
```

重点：

```text
JWT
SecurityContext
CurrentUserContext
Role
Permission
```

改完以后，其他模块不能再自己解析 JWT。

---

# 44. 第 4～5 周：改造 Product

建立：

```text
ProductQuery
ProductCommand
SkuQuery
```

让其他模块停止：

```text
ProductMapper
ProductEntity
```

直接访问。

这是第一次建立真正的 Module API。

---

# 45. 第 6 周：改造 Inventory

把：

```text
Redis
数据库库存
Lua
库存恢复
库存同步
```

统一进入 Inventory。

最终形成：

```text
InventoryPort
```

所有订单、秒杀只依赖这个接口。

---

# 46. 第 7～9 周：重点改造 Order

这是整个项目的核心重构模块。

完成：

```text
Order Domain
Order Application
Order Repository
Order API
Order Events
```

删除：

```text
OrderService 中的基础设施实现
```

逐步替换：

```text
OrderMapper
RedisTemplate
RabbitTemplate
```

为：

```text
Repository
InventoryPort
EventPublisher
```

---

# 47. 第 10 周：改造 Payment

建立：

```text
Payment
PaymentTransaction
PaymentStatus
PaymentGateway
```

支付 Provider 通过 Adapter 实现。

---

# 48. 第 11 周：改造 Coupon + Promotion

统一：

```text
Coupon
Promotion
Pricing
```

避免：

```text
Order
Product
Seckill
```

各自计算价格。

建立：

```text
PricingPolicy
```

作为价格规则的唯一来源。

---

# 49. 第 12～13 周：重构 Seckill

完成：

```text
Seckill Domain
InventoryPort
OrderCommand
SeckillRedisAdapter
SeckillMqAdapter
```

最终结构：

```text
Seckill
 ↓
InventoryPort
 ↓
Redis Adapter

Seckill
 ↓
OrderCommand
 ↓
Order Application
```

秒杀模块不能直接操作：

```text
OrderMapper
RedisTemplate
ProductMapper
```

---

# 50. 第 14 周：重构 Tracking

把：

```text
Tracking
AOP
RabbitMQ
Event DB
```

隔离成 analytics/tracking。

确保 Tracking 任何异常都不会影响订单。

---

# 51. 第 15～16 周：重构 AI

AI Gateway 保持独立。

依次改：

```text
Gateway
Assistant
Customer Service
AIGC
```

所有 AI 业务统一调用：

```text
AiGatewayClient
```

---

# 52. 第 17 周：重构 Web 与 Miniapp

Controller 逐步变薄。

最终 API 按模块暴露：

```text
/api/products
/api/orders
/api/seckill
/api/users
/api/ai
```

而不是按技术层组织。

---

# 53. 第 18 周：架构验收

必须检查：

### 目录

是否还有：

```text
utils
common
service 巨型包
```

### 依赖

是否存在：

```text
Order → ProductMapper
AI → OrderMapper
Domain → Redis
```

### 重复

是否存在：

```text
5 个 currentUser
4 个 priceCalculator
3 个 JwtParser
```

### 测试

核心模块是否有：

```text
unit test
integration test
architecture test
```

---

# 54. 重构过程中绝对不能做的事情

## 不要一次性移动所有文件

否则 Git diff 会失控。

应该：

```text
一个模块
一个阶段
一次 PR
```

---

## 不要边重构边改业务逻辑

例如：

```text
OrderService
```

正在迁移时，不要同时增加：

```text
退款
优惠券
AI
```

否则无法判断问题来自架构还是新功能。

---

## 不要为了 DDD 创建大量空壳类

例如：

```text
OrderFactory
OrderSpecification
OrderPolicy
OrderStrategy
OrderManager
OrderHelper
OrderProcessor
```

如果只是转发方法，没有业务价值，就不要创建。

---

# 55. Git 提交策略

每次重构按模块提交：

```text
refactor(identity): introduce current-user-context

refactor(product): introduce product public api

refactor(inventory): isolate inventory ports

refactor(order): introduce order domain

refactor(order): replace direct mapper dependency

refactor(seckill): isolate redis adapter

refactor(ai): introduce ai gateway facade
```

不要：

```text
refactor: clean project
```

这种提交无法审查。

---

# 56. 分支策略

建议：

```text
main
 ├── refactor/architecture
 ├── refactor/identity
 ├── refactor/product
 ├── refactor/order
 ├── refactor/inventory
 └── refactor/ai
```

每个模块完成后合并。

---

# 57. 测试要求

每次重构至少保证：

```text
编译通过
单元测试通过
集成测试通过
架构测试通过
核心 API 回归通过
```

尤其是：

```text
Order
Seckill
Inventory
Payment
AI Gateway
```

不能没有集成测试。

---

# 58. 如何判断重构是否成功

不要通过：

> “目录变漂亮了。”

来判断。

应该看下面几个指标。

### 指标 1：跨模块直接依赖数量

目标：

```text
Module → Other Mapper = 0
Module → Other Entity = 0
Module → Other Infrastructure = 0
```

### 指标 2：重复业务规则

目标：

```text
核心业务规则重复实现 = 0
```

### 指标 3：God Service

例如：

```text
OrderService > 500 lines
```

应重点审查。

### 指标 4：模块依赖复杂度

Order 不应该依赖十几个模块的底层实现。

### 指标 5：修改影响范围

例如：

> 修改商品价格模型

理想情况下：

```text
product
promotion
pricing
```

而不是：

```text
20+ 文件
```

---

# 59. 最终开发人员必须遵守的规则

以后任何新代码都遵守以下规则。

## 规则一

不知道代码属于哪个模块时：

> 先确定业务归属，再写代码。

## 规则二

不要因为一个函数被多个地方使用，就把它扔进 `utils`。

先判断：

> 这是共享技术能力，还是业务能力？

## 规则三

跨模块必须调用：

```text
Public API
```

不要调用：

```text
Mapper
Entity
Repository
```

## 规则四

业务代码不得直接操作：

```text
RedisTemplate
RabbitTemplate
SqlSession
SecurityContext
```

应该经过 Port / Adapter。

## 规则五

Controller 不写业务逻辑。

## 规则六

Application Service 不承载所有业务规则。

## 规则七

Domain 不依赖 Spring、MyBatis、Redis、RabbitMQ。

## 规则八

AI 不能成为核心交易链路的强依赖。

## 规则九

埋点属于旁路能力，失败不能影响业务。

## 规则十

任何重复业务规则必须寻找唯一归属。

---

# 60. 重构完成后的目标形态

最终项目应该能够做到：

```text
             ┌─────────────┐
             │  Product    │
             └──────┬──────┘
                    │
              ProductQuery
                    │
             ┌──────▼──────┐
             │    Order    │
             └──────┬──────┘
                    │
              InventoryPort
                    │
             ┌──────▼──────┐
             │ Inventory   │
             └─────────────┘
```

模块之间只暴露能力：

```text
ProductQuery
OrderQuery
InventoryPort
PaymentPort
AiGatewayClient
TrackingPublisher
```

而具体技术实现：

```text
MyBatis
Redis
RabbitMQ
Spring Security
Spring AI
```

全部藏到 Infrastructure。

---

# 61. 最终架构原则总结

这个项目最终要达到的不是：

> “DDD 很完整。”

而是：

> **业务边界清晰、模块可以独立理解、基础设施可以替换、重复代码可控、修改影响范围可预测。**

核心架构可以总结成：

```text
                 Modular Monolith
                        │
               ┌────────┴────────┐
               │                 │
         Business Modules     AI Platform
               │                 │
               └────────┬────────┘
                        │
                 Public Module API
                        │
                 Pragmatic DDD
                        │
              Application / Domain
                        │
                      Ports
                        │
                   Adapters
                        │
          ┌─────────────┼─────────────┐
          │             │             │
        MySQL         Redis        RabbitMQ
```

最终形成：

> **“一个部署单元，但多个清晰边界的业务模块；一个代码仓库，但每个模块可以独立理解；一个 JVM，但不再是一个没有边界的大单体。”**

这才是 SeckillMall 目前最合理的演进目标。