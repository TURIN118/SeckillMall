# Order 模块迁移基线 (ORDER-MIGRATION-BASELINE)

> 生成时间：2026-08-17
> 阶段：Phase 3.0 - 迁移准备阶段（基线快照）
> 基线 commit：`f053ded`（`f053dedbb057b57bfc5b4afa2e3b05b73a5f7d2d`）
> commit message：`docs(arch): Phase 2.5 - Order 模块迁移前置设计`
> 依据：ORDER-MIGRATION-PLAN.md + ORDER-API-CONTRACT.md + MODULE-MAP.md
> 说明：本文档为 Order 模块迁移前的基线快照，记录迁移起点状态，用于迁移过程中对比验证和回滚参照。未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. 测试基线

### 1.1 mvn test 全量结果

| 指标 | 数值 |
|---|---|
| 总测试数 | **120** |
| 通过 | **100** |
| 失败（Failures） | **1** |
| 错误（Errors） | **19** |
| 跳过（Skipped） | **0** |
| 报告文件数 | 18 |
| 执行结果 | BUILD FAILURE（因 1F+19E，属迁移前已存在的已知问题） |

> **说明**：1 Failure + 19 Errors 为**迁移前已存在的已知问题**（主要为 `@WebMvcTest` ApplicationContext 加载失败：OrderControllerTest、ProductControllerTest、SeckillOrderMapperTest 等上下文初始化失败）。迁移过程中**不得新增失败**，即每步提交后 Failures ≤ 1 且 Errors ≤ 19。

### 1.2 ArchitectureRulesTest 结果

| 指标 | 数值 |
|---|---|
| ArchUnit 规则数 | **21** |
| 通过 | **21** |
| 失败 | **0** |
| 错误 | **0** |
| 状态 | ✅ **全绿** |

**21 条 ArchUnit 规则清单**（全部通过）：

| # | 规则名 | 耗时 |
|---|---|---|
| 1 | controller_should_not_depend_on_mapper | 3.976s |
| 2 | controller_should_not_depend_on_entity | 0.013s |
| 3 | entity_should_not_depend_on_service | 0.021s |
| 4 | entity_should_not_depend_on_controller | 0.008s |
| 5 | entity_should_not_depend_on_mapper | 0.007s |
| 6 | entity_should_not_depend_on_vo | 0.006s |
| 7 | entity_should_not_depend_on_dto | 0.008s |
| 8 | vo_should_not_depend_on_entity | 0.024s |
| 9 | dto_should_not_depend_on_entity | 0.022s |
| 10 | mapper_should_not_depend_on_service | 0.007s |
| 11 | mapper_should_not_depend_on_controller | 0.002s |
| 12 | ai_gateway_should_not_depend_on_ai_assistant | 0.007s |
| 13 | ai_gateway_should_not_depend_on_ai_customerservice | 0.002s |
| 14 | ai_gateway_should_not_depend_on_ai_aigc | 0.002s |
| 15 | analytics_tracking_should_not_depend_on_service_impl | 0.003s |
| 16 | service_should_not_depend_on_rabbit_template | 0.058s |
| 17 | service_should_not_depend_on_string_redis_template | 0.008s |
| 18 | controller_should_not_depend_on_redis_template | 0.005s |
| 19 | controller_should_not_depend_on_string_redis_template | 0.004s |
| 20 | shared_kernel_should_not_depend_on_business | 0.003s |
| 21 | shared_kernel_port_should_not_depend_on_infrastructure | 0.002s |

### 1.3 基线 commit

| 项 | 值 |
|---|---|
| commit hash | `f053dedbb057b57bfc5b4afa2e3b05b73a5f7d2d` |
| commit 短 hash | `f053ded` |
| commit message | `docs(arch): Phase 2.5 - Order 模块迁移前置设计` |
| ArchUnit frozen violations | `f464c084-355f-4627-a262-60a6ca1b845e`（已冻结） |

### 1.4 回归标准

| 检查项 | 基线值 | 迁移中要求 |
|---|---|---|
| mvn test Failures | 1 | ≤ 1（不得新增失败） |
| mvn test Errors | 19 | ≤ 19（不得新增错误） |
| ArchUnit 规则通过数 | 21 | = 21（全绿，不得退化） |
| 编译 | ✅ 通过 | ✅ 每步必须通过 |
| 新增跨模块 Mapper 调用 | 0 | ≤ 0（不得新增） |
| 新增跨模块 Entity 引用 | 0 | ≤ 0（不得新增） |

> **关键**：迁移过程中每步提交后必须满足上述回归标准。若某步导致测试新增失败或 ArchUnit 退化，必须回滚该步并修复后重试。

---

## 2. 文件清单

### 2.1 概要

| 指标 | 数值 |
|---|---|
| Order 相关文件总数 | **30 个** |
| 总代码行数 | **3128 行** |
| 涉及的顶层包 | 7 个（controller / service / service.impl / entity / entity.enums / mapper / dto / vo / mq.consumer / mq.message） |
| MyBatis XML 文件 | **0 个**（NormalOrderMapper / NormalOrderItemMapper 使用 MyBatis-Plus 注解方式，无 XML namespace 需更新） |

### 2.2 完整文件清单（按包分组）

路径前缀：`seckill-mall/src/main/java/com/seckill/mall/`

#### Controller（2 个，250 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 1 | `controller/OrderController.java` | 213 | 普通订单+秒杀订单统一入口（创建/查询/支付/取消/发货/确认收货/删除） |
| 2 | `controller/AdminOrderController.java` | 37 | 后台订单高级筛选 |

#### Service 接口（4 个，246 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 3 | `service/OrderService.java` | 91 | 普通订单创建（立即购买/购物车结算）+ 购买校验 + 钱包支付订单查询 |
| 4 | `service/OrderQueryService.java` | 59 | 订单只读查询（详情/统一列表）+ 逻辑删除 |
| 5 | `service/OrderLifecycleService.java` | 74 | 订单状态流转（支付/取消/超时/发货/确认收货） |
| 6 | `service/AdminOrderService.java` | 22 | 后台订单分页查询 |

#### Service 实现（4 个，1517 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 7 | `service/impl/OrderServiceImpl.java` | 536 | OrderService 实现 |
| 8 | `service/impl/OrderQueryServiceImpl.java` | 429 | OrderQueryService 实现 |
| 9 | `service/impl/OrderLifecycleServiceImpl.java` | 423 | OrderLifecycleService 实现 |
| 10 | `service/impl/AdminOrderServiceImpl.java` | 129 | AdminOrderService 实现 |

> ⚠️ **合并风险**：OrderServiceImpl(536) + OrderLifecycleServiceImpl(423) = 959 行，合并为 OrderApplicationService 后将远超 500 行 God Service 限制，需按用例进一步拆分（创建/支付/取消 子服务）。

#### Entity / Enum（3 个，232 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 11 | `entity/NormalOrder.java` | 105 | 普通订单 PO |
| 12 | `entity/NormalOrderItem.java` | 67 | 普通订单明细 PO |
| 13 | `entity/enums/OrderStatus.java` | 60 | 订单状态枚举 |

#### Mapper（2 个，38 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 14 | `mapper/NormalOrderMapper.java` | 19 | 普通订单 Mapper（MyBatis-Plus 注解） |
| 15 | `mapper/NormalOrderItemMapper.java` | 19 | 普通订单明细 Mapper（MyBatis-Plus 注解） |

#### DTO / Request（5 个，286 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 16 | `dto/BuyNowRequest.java` | 42 | 立即购买请求 |
| 17 | `dto/CartCheckoutRequest.java` | 35 | 购物车结算请求 |
| 18 | `dto/NormalOrderPayRequest.java` | 23 | 普通订单支付请求 |
| 19 | `dto/ShipRequest.java` | 30 | 发货请求 |
| 20 | `dto/AdminOrderQueryRequest.java` | 156 | 后台订单查询请求 |

#### VO（8 个，468 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 21 | `vo/NormalOrderVO.java` | 97 | 普通订单视图 |
| 22 | `vo/NormalOrderItemVO.java` | 63 | 普通订单明细视图 |
| 23 | `vo/NormalOrderDetailVO.java` | 41 | 普通订单详情视图 |
| 24 | `vo/AdminOrderVO.java` | 85 | 后台订单视图 |
| 25 | `vo/OrderListItemVO.java` | 74 | 统一订单列表项视图 |
| 26 | `vo/OrderStatusItemVO.java` | 30 | 订单状态项视图 |
| 27 | `vo/OrderTrendVO.java` | 31 | 订单趋势视图 |
| 28 | `vo/OrderStatusDistributionVO.java` | 47 | 订单状态分布视图 |

#### MQ（2 个，91 行）

| # | 文件 | 行数 | 职责 |
|---|---|---|---|
| 29 | `mq/consumer/OrderCancelConsumer.java` | 65 | 订单超时取消延迟消费者 |
| 30 | `mq/message/OrderDelayMessage.java` | 26 | 订单延迟消息 |

### 2.3 行数汇总校验

| 包 | 文件数 | 行数 |
|---|---|---|
| controller | 2 | 250 |
| service（接口） | 4 | 246 |
| service.impl | 4 | 1517 |
| entity + entity.enums | 3 | 232 |
| mapper | 2 | 38 |
| dto | 5 | 286 |
| vo | 8 | 468 |
| mq | 2 | 91 |
| **合计** | **30** | **3128** |

---

## 3. 入口与依赖检查

### 3.1 基线 commit 信息

| 项目 | 值 |
|---|---|
| commit hash（完整） | `f053dedbb057b57bfc5b4afa2e3b05b73a5f7d2d` |
| commit hash（短） | `f053ded` |
| commit message | `docs(arch): Phase 2.5 - Order 模块迁移前置设计` |
| 工作区状态 | Order 相关文件干净（无未提交变更） |
| 基线时点 | Phase 2.5 设计文档提交后，Phase 3.0 迁移实施前 |

### 3.2 入口点（Entry Points）

Order 模块的对外入口共 **5 个**：

| # | 入口 | 类型 | 路径 | 说明 |
|---|---|---|---|---|
| 1 | `OrderController` | HTTP REST | `controller/OrderController.java` | 普通订单+秒杀订单统一 HTTP 入口 |
| 2 | `AdminOrderController` | HTTP REST | `controller/AdminOrderController.java` | 后台订单 HTTP 入口 |
| 3 | `OrderService` | Java API | `service/OrderService.java` | 被 payment（WalletServiceImpl）、review（ProductReviewServiceImpl）跨模块调用 |
| 4 | `OrderQueryService` | Java API | `service/OrderQueryService.java` | 被 ai（AgentService）跨模块调用 |
| 5 | `OrderCancelConsumer` | MQ Consumer | `mq/consumer/OrderCancelConsumer.java` | 延迟消息消费入口 |

### 3.3 跨模块依赖（order 依赖外部）

从 4 个 ServiceImpl + 2 个 Controller + 1 个 Consumer 的字段注入提取：

| 注入项 | 所属模块 | 依赖性质 | 注入位置 |
|---|---|---|---|
| `NormalOrderMapper` | order | 自身 | OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl |
| `NormalOrderItemMapper` | order | 自身 | 同上 |
| `MessageBusPort` | shared.kernel | ✅ Port | OrderServiceImpl |
| `ObjectMapper` | Spring | ✅ 框架 | OrderServiceImpl |
| `OrderQueryService` | order | 自身 | OrderLifecycleServiceImpl |
| `ProductService` | product | ⚠️ 跨模块 | OrderServiceImpl, OrderQueryServiceImpl |
| `ProductSkuService` | product | ⚠️ 跨模块 | OrderServiceImpl, OrderLifecycleServiceImpl |
| `InventoryService` | product(inventory) | ⚠️ 跨模块 | OrderServiceImpl, OrderLifecycleServiceImpl |
| `UserAddressService` | identity | ⚠️ 跨模块 | OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl |
| `UserService` | identity | ⚠️ 跨模块 | OrderLifecycleServiceImpl |
| `SecurityUtils` | identity(security) | ⚠️ 跨模块 | OrderController |
| `CartService` | cart | ⚠️ 跨模块 | OrderServiceImpl |
| `CouponUsageService` | coupon | ⚠️ 跨模块 | OrderServiceImpl, OrderLifecycleServiceImpl |
| `PaymentService` | payment | ⚠️ 跨模块 | OrderLifecycleServiceImpl |
| `SeckillOrderService` | seckill | ⚠️ 跨模块 | OrderQueryServiceImpl, AdminOrderServiceImpl, OrderController, OrderCancelConsumer |
| `EmailService` | upload | ⚠️ 跨模块 | OrderLifecycleServiceImpl |

**跨模块 Service 依赖统计：7 个模块**（product / identity / cart / coupon / payment / seckill / upload）

### 3.4 跨模块 Entity 引用（order 引用外部 Entity）

| 被引用的 Entity | 所属模块 | 引用位置 | 问题 |
|---|---|---|---|
| `Cart` | cart | OrderServiceImpl | order 直接操作 cart 的 Entity |
| `Product` | product | OrderServiceImpl, OrderQueryServiceImpl | order 直接引用 product Entity |
| `ProductSku` | product | OrderServiceImpl | order 直接引用 product Entity |
| `UserAddress` | identity | OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl | order 直接引用 identity Entity |
| `SeckillOrder` | seckill | OrderQueryServiceImpl | order 查询时直接引用 seckill Entity |

**跨模块 Entity 引用：5 类 Entity，涉及 4 个模块**

### 3.5 order 被外部调用（外部依赖 order）

| 外部调用方 | 所属模块 | 调用的 order API | 问题 |
|---|---|---|---|
| `WalletServiceImpl` | payment | `OrderService.getWalletPaidOrdersByUser()` → `List<NormalOrder>` | ⚠️ 返回 Entity，需改为 `List<OrderSnapshot>` |
| `ProductReviewServiceImpl` | review | `OrderService.hasUserPurchasedProduct()` → `boolean` | 返回值无泄露，仅改调用路径 |
| `AgentService` | ai | `OrderQueryService.getUnifiedOrderList()` | 需改为 `order.api.OrderQueryApi.listOrders()` |

**被外部调用：3 个模块**（payment / review / ai）

### 3.6 MyBatis XML 检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| `resources/mapper/**/NormalOrder*.xml` | **不存在** | NormalOrderMapper 使用 MyBatis-Plus 注解方式（`@Select`/`@Insert` 等） |
| `resources/mapper/**/NormalOrderItem*.xml` | **不存在** | NormalOrderItemMapper 同上 |
| 迁移时 XML namespace 更新 | **不需要** | Mapper 移动后仅需更新 Java import，无 XML namespace 需同步 |

---

## 4. 目标迁移路径

> 完整的文件迁移映射表见 `ORDER-MIGRATION-PLAN.md` 第 3 节「文件迁移路径」。
> 此处摘录迁移类型统计和关键约束。

### 4.1 迁移类型统计

| 迁移类型 | 文件数 | 说明 |
|---|---|---|
| **移动**（纯路径变更，改 import） | 16 个 | Entity/Mapper/VO/MQ Message 等仅改包路径 |
| **改造**（路径变更 + 逻辑修改） | 12 个 | Controller/Service/ServiceImpl/DTO/Consumer 需改注入和签名 |
| **合并**（多个文件合并为一个） | 2 个 | OrderLifecycleService → OrderApi；OrderLifecycleServiceImpl → OrderApplicationService |
| **新增**（迁移过程中新建） | 24 个 | API 接口 + Command + Query + Result + DTO + ApplicationService |
| **删除**（迁移完成后移除） | 8 个 | 原 Service 接口 + ServiceImpl（被 ApplicationService 替代） |

### 4.2 目标包结构

```
com.seckill.mall.order
├── api/                          # Public API（接口 + DTO + Command + Result）
│   ├── dto/
│   ├── command/
│   ├── query/
│   └── result/
├── application/                  # 应用服务（用例编排，实现 api 接口）
├── domain/                       # 领域模型（OrderStatus）
├── infrastructure/               # 持久化（mapper + entity）
│   ├── mapper/
│   └── entity/
├── interfaces/                   # Controller + VO
│   └── vo/
└── mq/                           # MQ
    └── message/
```

### 4.3 关键迁移约束

1. **Entity 不外泄**：`NormalOrder` / `NormalOrderItem` 仅 `order.infrastructure` 内部可见，对外通过 `OrderSnapshot` / `OrderDetailDTO` 暴露
2. **Mapper 不外泄**：`NormalOrderMapper` / `NormalOrderItemMapper` 仅 `order.infrastructure` 内部可见
3. **Controller 不直接注入跨模块 Service**：`OrderController` 移除 `SeckillOrderService` 直接注入，改为通过 `OrderApplicationService` 内部调 `seckill.api`
4. **SecurityUtils 替换**：`OrderController` 中的 `SecurityUtils` → `shared.kernel.CurrentUserContext`
5. **返回值不泄露 Entity**：`getWalletPaidOrdersByUser()` 返回 `List<NormalOrder>` → `getWalletPaidOrders()` 返回 `List<OrderSnapshot>`

---

## 5. 文件移动顺序

> 完整的 8 步迁移计划见 `ORDER-MIGRATION-PLAN.md` 第 6 节「迁移步骤」。
> 此处列出移动顺序和关键约束。

### 5.1 迁移步骤总览

| Step | 操作 | 影响文件 | 验证方式 | 可回滚性 |
|---|---|---|---|---|
| **Step 1** | 创建 order 模块空包结构 | 新增 ~15 个 `package-info.java` | `mvn compile` | 删除新增包 |
| **Step 2** | 定义 order.api（接口 + DTO + Command + Result） | 新增 24 个文件 | `mvn compile` | 删除新增文件 |
| **Step 3** | 迁移 infrastructure（Mapper + Entity + OrderStatus） | 4 个移动 + ~15 个 import 更新 | `mvn compile` + `mvn test` | `git revert` |
| **Step 4** | 迁移 application（Service → ApplicationService） | 3 个新增 + 8 个删除 + 外部 import 更新 | `mvn compile` + `mvn test` | `git revert`（建议拆 4a/4b/4c） |
| **Step 5** | 迁移 interfaces（Controller + VO） | 2 个改造 + 8 个 VO 移动 | `mvn compile` + API 测试 | `git revert` |
| **Step 6** | 迁移 mq（OrderCancelConsumer + OrderDelayMessage） | 2 个移动 + import 更新 | `mvn compile` + MQ 测试 | `git revert` |
| **Step 7** | 更新外部调用方（payment / review / ai） | 3 个外部文件改造 | `mvn compile` + 相关测试 | `git revert` |
| **Step 8** | ArchUnit 规则验证 | 1 个测试文件更新 | ArchUnit 全绿 | `git revert` |

### 5.2 文件移动顺序（按依赖方向）

迁移必须按**依赖方向从底层到上层**进行，确保每步编译可通过：

```
Step 1: 空包结构（无依赖）
  ↓
Step 2: order.api（接口 + DTO，依赖 shared.kernel）
  ↓
Step 3: order.infrastructure（Mapper + Entity，依赖 order.domain）
  ↓
Step 4: order.application（实现 api，依赖 infrastructure + 外部 api）
  ↓
Step 5: order.interfaces（Controller，依赖 application）
  ↓
Step 6: order.mq（Consumer，依赖 application）
  ↓
Step 7: 外部调用方更新（依赖 order.api）
  ↓
Step 8: ArchUnit 规则验证
```

### 5.3 Step 4 拆分建议（关键约束）

Step 4 是最大的一步（合并 2 个 ServiceImpl + 改造 2 个 ServiceImpl + 删除 4 个原 Service），**必须拆为 3 个子步**：

| 子步 | 操作 | 关键约束 |
|---|---|---|
| **Step 4a** | 创建 3 个 ApplicationService（复制原 impl 逻辑，暂保留原 Service） | 原 Service 接口和 impl **保留不删**，新旧并存 |
| **Step 4b** | 切换 Controller / Consumer 到 ApplicationService | 原 Service 暂时无引用但**保留**，4b 失败可回滚到 4a |
| **Step 4c** | 确认无误后删除原 Service 接口和 impl | 此时原 Service 已无引用，安全删除 |

> ⚠️ **关键**：4b 失败时可回滚到 4a（原 Service 仍在），不会导致编译断裂。

---

## 6. 风险点

> 完整的风险点表见 `ORDER-MIGRATION-PLAN.md` 第 7 节「风险点」。
> 此处列出基线时点确认的风险和缓解措施。

### 6.1 风险清单

| # | 风险 | 等级 | 影响 | 缓解措施 |
|---|---|---|---|---|
| 1 | **import 大量修改导致编译错误** | 高 | 30 个文件移动 + 12 个改造，import 路径全变 | 分步迁移（Step 3-6），每步编译验证；IDE 批量更新 import |
| 2 | **跨模块 Entity 引用需同步改造** | 高 | order 引用 5 类外部 Entity，需外部模块先提供 DTO | Step 4 前确认外部模块最小 API 契约集（ORDER-MIGRATION-PLAN.md 5.3 节）已定义 |
| 3 | **OrderController 承担秒杀订单入口** | 高 | OrderController 同时是普通订单和秒杀订单的 Controller | Step 5 中 OrderApplicationService 内部调 seckill.api，Controller 路由不变 |
| 4 | **合并后 God Service 超限** | 高 | OrderServiceImpl(536) + OrderLifecycleServiceImpl(423) = 959 行，远超 500 行限制 | 合并后按用例拆分（创建/支付/取消 子服务），或保持分离的 ApplicationService |
| 5 | **MyBatis XML namespace** | 低 | ⚠️ 基线确认：**无 XML 文件**，此风险不适用 | 无需处理（Mapper 使用注解方式） |
| 6 | **OrderService.getWalletPaidOrdersByUser 返回 Entity** | 中 | payment/WalletServiceImpl 依赖返回 `List<NormalOrder>` | Step 7 同步更新 WalletServiceImpl，改为 `List<OrderSnapshot>` |
| 7 | **测试可能失败** | 中 | 现有测试可能直接依赖 OrderService 接口或 NormalOrder entity | Step 4 后运行全量测试，修复测试 import |
| 8 | **ArchUnit 规则可能报新违规** | 低 | 新包结构可能触发已有 ArchUnit 规则 | Step 8 更新规则，逐步修复 |
| 9 | **Spring Bean 注入冲突** | 低 | 合并 Service 后 bean 名称可能冲突 | 使用 `@Service("orderApplicationService")` 显式命名 |

### 6.2 基线时点新增发现

| 发现 | 影响 | 处理建议 |
|---|---|---|
| **实际文件数为 30 个**（ORDER-MIGRATION-PLAN.md 中写 28 个） | 文件清单需以本文档 30 个为准 | 已在本文档 2.2 节修正 |
| **无 MyBatis XML 文件** | ORDER-MIGRATION-PLAN.md 第 7 节风险 4「MyBatis XML namespace」不适用 | 已在本文档 6.1 节标注为低风险/不适用 |

---

## 7. 预计提交粒度

### 7.1 提交计划

每个 Step（含子步）单独提交，提交信息标注 `Phase 3.0 Order Step N`：

| 提交 # | 对应 Step | 提交信息 | 影响文件数 | 验证要求 |
|---|---|---|---|---|
| C1 | Step 1 | `feat(order): Phase 3.0 Step 1 - 创建 order 模块包结构` | ~15 新增 | `mvn compile` ✅ |
| C2 | Step 2 | `feat(order): Phase 3.0 Step 2 - 定义 order.api 接口与数据契约` | 24 新增 | `mvn compile` ✅ |
| C3 | Step 3 | `refactor(order): Phase 3.0 Step 3 - 迁移 infrastructure（Mapper+Entity）` | 4 移动 + ~15 import | `mvn compile` ✅ + `mvn test` ✅ |
| C4 | Step 4a | `refactor(order): Phase 3.0 Step 4a - 创建 ApplicationService（保留原 Service）` | 3 新增 | `mvn compile` ✅ + `mvn test` ✅ |
| C5 | Step 4b | `refactor(order): Phase 3.0 Step 4b - 切换 Controller/Consumer 到 ApplicationService` | ~5 改造 | `mvn compile` ✅ + `mvn test` ✅ |
| C6 | Step 4c | `refactor(order): Phase 3.0 Step 4c - 删除原 Service 接口和 impl` | 8 删除 | `mvn compile` ✅ + `mvn test` ✅ |
| C7 | Step 5 | `refactor(order): Phase 3.0 Step 5 - 迁移 interfaces（Controller+VO）` | 2 改造 + 8 移动 | `mvn compile` ✅ + API 测试 ✅ |
| C8 | Step 6 | `refactor(order): Phase 3.0 Step 6 - 迁移 mq（Consumer+Message）` | 2 移动 | `mvn compile` ✅ + MQ 测试 ✅ |
| C9 | Step 7 | `refactor(order): Phase 3.0 Step 7 - 更新外部调用方（payment/review/ai）` | 3 改造 | `mvn compile` ✅ + 相关测试 ✅ |
| C10 | Step 8 | `test(order): Phase 3.0 Step 8 - ArchUnit 规则验证` | 1 更新 | ArchUnit 全绿 ✅ |

### 7.2 提交粒度汇总

| 指标 | 数值 |
|---|---|
| 预计提交数 | **10 个**（C1-C10） |
| 每提交最大影响文件数 | ~24 个（C2 新增 API 契约） |
| 每提交必须通过的验证 | `mvn compile` + `mvn test`（相关子集） |
| 可回滚的最小单元 | 单个提交（C1-C10 均可独立 `git revert`） |
| 关键不可逆提交 | C6（删除原 Service）— 执行前确认 C5 验证全绿 |

### 7.3 回滚策略

| 失败场景 | 回滚方式 | 保留的成果 |
|---|---|---|
| C3 失败 | `git revert C3` | C1-C2（包结构 + API）保留 |
| C4 失败 | `git revert C4` | C1-C3 保留 |
| C5 失败 | `git revert C5` | C1-C4 保留（原 Service 仍在，可回滚到 4a） |
| C6 失败 | `git revert C6` | C1-C5 保留（原 Service 恢复） |
| C7-C8 失败 | `git revert C7/C8` | C1-C6 保留 |
| C9 失败 | `git revert C9` | C1-C8 保留（order 已迁移，外部调用方暂未更新，需临时保留旧 Service 接口作为适配层） |
| 严重问题 | `git reset --hard f053ded` | 回滚到基线 commit |

---

## 8. 迁移前最终检查清单

> 迁移实施前，逐项确认以下检查项全部通过。

### 8.1 基线确认

- [ ] **BC-1**：当前 commit 为 `f053ded`（`git rev-parse HEAD` 确认）
- [ ] **BC-2**：工作区干净，无未提交的 Order 相关变更（`git status` 确认）
- [ ] **BC-3**：本文档文件清单 30 个文件与实际文件一致（`glob` 验证）
- [ ] **BC-4**：本文档行数汇总 3128 行与实际一致

### 8.2 测试基线确认

- [ ] **TC-1**：`mvn test` 全量执行成功（或仅有已知 skip）
- [ ] **TC-2**：Order 相关测试用例清单已记录（第 1 节已补充）
- [ ] **TC-3**：ArchUnit 规则测试当前状态已记录
- [ ] **TC-4**：已知失败测试已记录并标注原因

### 8.3 设计文档确认

- [ ] **DC-1**：`ORDER-MIGRATION-PLAN.md` 已审核通过
- [ ] **DC-2**：`ORDER-API-CONTRACT.md` 已审核通过
- [ ] **DC-3**：`MODULE-MAP.md` 已审核通过
- [ ] **DC-4**：`TARGET-MODULE-DESIGN.md` 已审核通过

### 8.4 依赖确认

- [ ] **DEP-1**：外部模块最小 API 契约集（ORDER-MIGRATION-PLAN.md 5.3 节）已定义
- [ ] **DEP-2**：7 个跨模块依赖（product/identity/cart/coupon/payment/seckill/upload）的 API 契约已确认
- [ ] **DEP-3**：3 个外部调用方（payment/review/ai）的改造方案已确认
- [ ] **DEP-4**：无 MyBatis XML 文件（本文档 3.6 节已确认）

### 8.5 风险确认

- [ ] **RK-1**：OrderController 秒杀订单入口的处理方案已确认（Step 5）
- [ ] **RK-2**：合并后 God Service 超限的拆分方案已确认（Step 4 / 6.1 风险 4）
- [ ] **RK-3**：`getWalletPaidOrdersByUser` 返回 Entity 的改造方案已确认（Step 7）
- [ ] **RK-4**：Step 4 拆分为 4a/4b/4c 的方案已确认（5.3 节）

### 8.6 回滚准备

- [ ] **RB-1**：基线 commit `f053ded` 已打标签或记录（`git tag pre-order-migration f053ded` 建议执行）
- [ ] **RB-2**：每步提交信息规范已确认（`Phase 3.0 Order Step N`）
- [ ] **RB-3**：紧急回滚命令已确认（`git reset --hard f053ded`）

---

> **本文件为 Phase 3.0 Order 模块迁移基线快照，未创建任何代码/接口/包，未修改任何现有代码。**
> **测试基线（第 1 节）待 team-leader 补充。**
> **下一步**：确认第 8 节检查清单全部通过后，进入 Step 1（创建 order 模块包结构）。