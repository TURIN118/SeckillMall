# SeckillMall 架构重构进度审计报告

> 审计时间：2026-08-17
> 审计性质：Read-only Architecture Audit（未修改任何代码）
> 审计基准：《SeckillMall 改造为 Pragmatic DDD + Modular Monolith 的详细架构重构方案》
> 代码规模：main/java 353 个文件，test/java 20 个文件

---

## 1. 最终结论

```text
当前阶段：Phase 5（Infrastructure 隔离基本完成，业务模块化未开始）
总体完成度：约 45%
最关键判断：项目已完成"分层规则治理 + 基础设施隔离 + God Service 拆分 + 跨模块 Mapper 消除"，
            但"按业务模块组织代码 + DDD 分层 + 模块 Public API"尚未开始。
            代码仍是传统分层单体（controller/service/mapper/entity 全局包），
            而非 Modular Monolith（按业务模块组织）。AI/Tracking/Shared Kernel 已模块化，核心业务未模块化。
```

---

## 2. 完成度总表

| 领域 | 权重 | 完成度 | 状态 | 核心问题 |
|---|---:|---:|---|---|
| A. 模块化 | 15% | 25% (1/4) | 少量尝试 | 仅 ai/analytics/shared 模块化，核心业务仍是全局 controller/service/mapper/entity 包 |
| B. 模块边界 | 15% | 50% (2/4) | 已经开始 | 跨模块 Mapper 大部分消除，但无 Public API，CategoryServiceImpl 仍依赖 ProductMapper |
| C. DDD | 15% | 0% (0/4) | 未开始 | 无 domain 层、无 Aggregate、无 Value Object、无 Repository 抽象、无 Domain Service |
| D. Ports & Adapters | 15% | 50% (2/4) | 已经开始 | CachePort/MessageBusPort/SeckillInventoryPort 存在，但未全面覆盖（如支付、第三方） |
| E. Infrastructure 隔离 | 10% | 75% (3/4) | 基本完成 | Service 无 RabbitTemplate；SystemHealthMonitorImpl StringRedisTemplate 合理；VerificationCodeController 泄漏 |
| F. Entity/PO/DTO/VO | 10% | 25% (1/4) | 少量尝试 | 有 VO/DTO 分离，但无 Domain Entity/PO 分离，Entity 同时用于 Domain/DB/API |
| G. Controller/Application 分层 | 5% | 50% (2/4) | 已经开始 | Controller 变薄（最大 188 行），但无 Application Service 命名约定 |
| H. Application Service | 5% | 75% (3/4) | 基本完成 | God Service 治理完成，所有 Service < 500 行；但仍承载核心业务规则（非 Domain Service） |
| I. 重复规则治理 | 5% | 50% (2/4) | 已经开始 | calculateDiscount/JWT/SeckillInventoryPort 统一；CurrentUserContext 抽象未被采用 |
| J. AI 架构 | 5% | 75% (3/4) | 基本完成 | AiGatewayService 统一入口，不进入核心事务；但仍直接依赖 Spring AI ChatClient |
| K. Tracking | 3% | 100% (4/4) | 完成 | 旁路能力，不依赖 service.impl，不影响核心业务事务 |
| L. 测试与架构治理 | 2% | 50% (2/4) | 已经开始 | ArchUnit(21 规则)+JaCoCo+Testcontainers；缺 Checkstyle/Spotless/Sonar/PMD |

**总完成度 = 0.0375 + 0.075 + 0 + 0.075 + 0.075 + 0.025 + 0.025 + 0.0375 + 0.025 + 0.0375 + 0.03 + 0.01 ≈ 45%**

---

## 3. 当前真实模块结构

### Business（传统分层，未按业务模块组织）

```text
com.seckill.mall
├── controller/      (27 个 Controller，全局包)
├── service/         (41 个 Service 接口)
│   └── impl/        (37 个 ServiceImpl，全局包)
├── mapper/          (24 个 Mapper，全局包)
├── entity/          (25 个 Entity + enums/，全局包，传统 MyBatis Entity)
├── dto/             (36 个 Request/DTO，全局包)
├── vo/              (41 个 VO，全局包)
├── converter/       (3 个 Converter)
├── config/          (11 个 Config)
├── common/          (GlobalExceptionHandler, ErrorCode)
├── exception/       (BusinessException)
├── aspect/          (RateLimitAspect, OperationLogAspect)
├── cache/           (SeckillLuaService)
├── mq/              (producer/consumer)
├── scheduler/       (SeckillStatusScheduler)
├── security/        (SecurityUtils, JwtUtils, JwtAuthenticationFilter...)
├── util/            (ProductSortUtil)
└── utils/           (MapUtils)  ← 注意：同时存在 util/ 和 utils/ 两个包
```

### AI（已模块化）

```text
com.seckill.mall.ai
├── gateway/         (advisor/config/controller/dto/entity/service) ← 统一网关
├── assistant/       (controller/service) ← 导购
├── customerservice/ (controller/service/entity) ← 客服
└── aigc/            (service) ← AIGC
```

### Analytics（已模块化）

```text
com.seckill.mall.analytics
└── tracking/        (annotation/aspect/controller/dto/entity/mq/service) ← 旁路
```

### Infrastructure（Shared Kernel）

```text
com.seckill.mall.shared.kernel
├── port/            (CachePort, MessageBusPort, DistributedLockPort) ← 端口接口
├── adapter/         (RedisCacheAdapter, RabbitMessageBusAdapter) ← 适配器
├── util/            (SseUtils)
└── CurrentUserContext.java ← 抽象接口（未被采用）
```

---

## 4. 已完成的重构（有代码证据）

### 4.1 ArchUnit 架构规则测试（Phase 1）
- **证据**：`src/test/java/com/seckill/mall/architecture/ArchitectureRulesTest.java`（214 行，21 条规则）
- 规则分三组：分层依赖规则（A 组 10 条）、AI 模块保护规则（B 组 2 条）、基础设施泄露规则（C 组 4 条）、Shared Kernel 保护规则（D 组 2 条）
- 使用 `FreezingArchRule` freeze 模式，已冻结 22 个存量违规
- 提交记录显示 "ArchitectureRulesTest: 21/21 pass"

### 4.2 Shared Kernel（Phase 2）
- **证据**：`shared/kernel/` 目录
- `CurrentUserContext` 接口（用户身份抽象）
- `port/`：CachePort, MessageBusPort, DistributedLockPort（技术中立端口）
- `adapter/`：RedisCacheAdapter, RabbitMessageBusAdapter（基础设施适配器）
- `util/`：SseUtils（SSE 流式工具）

### 4.3 AI Platform 模块化（Phase 15-16）
- **证据**：`ai/` 目录结构（gateway/assistant/customerservice/aigc）
- `AiGatewayService`（258 行）是统一调用入口，所有 AI 功能通过它调用大模型
- 集成 FallbackAdvisor 降级兜底、RouteService 多模型路由
- **AI 不进入核心事务**：`ai/` 目录无任何 OrderMapper/ProductMapper/InventoryService/OrderService 依赖（grep 0 匹配）

### 4.4 Analytics Tracking 旁路（Phase 14）
- **证据**：`analytics/tracking/` 目录结构
- ArchUnit 规则 9：`analytics.tracking 不应依赖 service.impl`（已冻结）
- `service/impl/` 无任何 TrackService/TrackProducer 依赖（grep 0 匹配）
- Tracking 失败不影响核心业务事务

### 4.5 God Service 治理（P1/P2）
- **证据**：`service/impl/` 行数统计，所有 ServiceImpl < 500 行
- 最大：SeckillOrderServiceImpl 498 行、OrderServiceImpl 497 行、ProductServiceImpl 478 行
- OrderServiceImpl 拆分为 OrderServiceImpl（创建）/OrderQueryServiceImpl（查询）/OrderLifecycleServiceImpl（状态流转）
- CouponServiceImpl 拆分为 CouponServiceImpl（管理）/CouponUsageServiceImpl（核销）
- SystemServiceImpl 拆分为 SystemServiceImpl/SystemHealthMonitorImpl
- 提交记录：`OrderServiceImpl split (1175->497)`

### 4.6 跨模块 Mapper 消除（Phase 11-15）
- **证据**：`service/impl/` 的 Mapper import 分析
- 提交记录显示 10+ 个 Service 消除了跨模块 Mapper（Wallet/Review/Coupon/Stats/Cart/Favorite/Recharge/AdminOrder/System/AdminUser）
- 大部分 Service 现在只依赖自己模块的 Mapper + 其他模块的 Service 接口
- OrderServiceImpl 依赖 MessageBusPort + ProductService/CouponUsageService/InventoryService（通过接口）

### 4.7 基础设施隔离（Phase 9）
- **证据**：`shared/kernel/port/` + `shared/kernel/adapter/`
- Service 层无 RabbitTemplate 依赖（grep 0 匹配）
- Service 层无 SecurityContextHolder 依赖（grep 0 匹配）
- Service 层仅 SystemHealthMonitorImpl 依赖 StringRedisTemplate（健康监控，合理）
- OrderServiceImpl 依赖 MessageBusPort 而非 RabbitTemplate

### 4.8 Controller 变薄
- **证据**：`controller/` 行数统计
- 最大 OrderController 188 行，大部分 < 100 行
- Controller 无 RabbitTemplate 依赖
- Controller 无 Mapper 依赖（除测试 AuthControllerTest）

### 4.9 VO/Entity 分离（部分）
- **证据**：`NormalOrderDetailVO.java` 注释明确"避免直接暴露 Entity 而违反 ArchUnit VO→Entity 分层规则"
- NormalOrderDetailVO 使用独立 NormalOrderVO/NormalOrderItemVO，不依赖 Entity
- 提交记录：`fix(vo): replace Entity references with independent VOs in NormalOrderDetailVO`

### 4.10 重复规则治理（部分）
- **证据**：
  - `CouponUsageService.calculateDiscount` 统一优惠计算（OrderServiceImpl 调用）
  - `JwtUtils` 唯一 JWT 实现（无重复）
  - `SeckillInventoryPort` 抽象秒杀库存回补
  - `SseUtils` 统一 SSE 流式逻辑（消除 ShoppingAssistantController/CustomerServiceController 重复）

### 4.11 测试工具
- **证据**：`pom.xml`
- ArchUnit 1.3.0（架构测试）
- JaCoCo 0.8.11（覆盖率）
- Testcontainers（集成测试）
- H2（Mapper 切片测试）
- MockWebServer（AI 集成测试）
- 20 个测试文件

---

## 5. 部分完成的重构

### 5.1 模块边界（缺口：无 Public API）
- 跨模块 Mapper 大部分消除，但**没有明确的模块 Public API 边界**
- 模块间通过 Service 接口协作，但 Service 接口散落在全局 `service/` 包，未按模块组织
- 无 `api/` 包、无 Application API 命名约定
- **缺口**：需要建立 `order/api/`、`product/api/` 等模块公开 API

### 5.2 Ports & Adapters（缺口：未全面覆盖）
- 已有 CachePort/MessageBusPort/DistributedLockPort/SeckillInventoryPort
- **缺口**：
  - 第三方支付未 Adapter 化（PaymentServiceImpl 仅 43 行，但未抽象支付端口）
  - AI Gateway 仍直接依赖 Spring AI ChatClient（虽在 Gateway 层合理，但未抽象为 AiModelPort）
  - StorageService 有 LocalStorageService/MinioStorageService 两实现，但无明确 StoragePort 抽象

### 5.3 Infrastructure 隔离（缺口：个别泄漏）
- **违规**：`VerificationCodeController` 直接依赖 StringRedisTemplate（line 13, 84, 174）
  - 应通过 VerificationCodeService 或 CachePort 封装
  - 已被 ArchUnit 冻结（违规文件 7e626fc8）
- SystemHealthMonitorImpl 依赖 StringRedisTemplate（合理，健康监控需直接检测连接）

### 5.4 Entity/PO/DTO/VO 分离（缺口：无 Domain Entity/PO 分离）
- 有 VO/DTO 分离（41 VO + 36 DTO）
- **缺口**：
  - 无 `domain/` 包（无 Domain Entity）
  - 无 `po/` 包（无 Database PO 分离）
  - 25 个 Entity 同时用于 Domain/Database/API（传统 MyBatis Entity）
  - OrderServiceImpl 直接依赖 Entity：Cart, NormalOrder, NormalOrderItem, Product, ProductSku, UserAddress

### 5.5 重复规则治理（缺口：CurrentUserContext 未采用）
- `CurrentUserContext` 接口存在，`SecurityUtils implements CurrentUserContext`
- **但 108 处代码直接依赖 SecurityUtils 具体实现**而非 CurrentUserContext 抽象
- OrderController 中 17 次 `securityUtils.getCurrentUserId()`
- 注释明确"业务代码应依赖此接口而非 SecurityUtils 具体实现"，但未执行

### 5.6 测试与架构治理（缺口：缺静态检查工具）
- 有 ArchUnit/JaCoCo/Testcontainers
- **缺口**：无 Checkstyle/Spotless/Sonar/PMD
- 测试覆盖核心模块：Order/Seckill/Product/Auth/AI Gateway（20 个测试文件）

---

## 6. 尚未开始的重构

### 6.1 按业务模块组织代码（未开始）
- 仍是 `controller/service/mapper/entity/dto/vo` 全局包
- 未按 `order/product/user/coupon/seckill/inventory/cart/payment/review` 等业务模块组织
- **证据**：顶层包结构（22 个全局包，无业务模块包）
- 这是 Modular Monolith 的核心要求，未开始

### 6.2 DDD 分层（未开始）
- 无 `domain/` 层（无 Domain Entity/Aggregate/Value Object/Domain Service/Domain Event）
- 无 Repository 抽象（用 MyBatis Mapper，未抽象 Domain Repository Interface）
- 业务规则仍集中在 Application Service（如 OrderServiceImpl 包含订单创建、库存校验、优惠券计算编排）
- **证据**：glob `**/domain/**/*.java` 无结果，无 `repository/` 包

### 6.3 模块 Public API（未开始）
- 无 `api/` 包
- 无 ApplicationService/UseCase/CommandHandler 命名约定
- **证据**：glob `**/api/**/*.java` 无结果，grep `ApplicationService|UseCase|CommandHandler` 无结果

### 6.4 Java 模块化（未开始）
- 无 `module-info.java`
- **证据**：glob `**/module-info.java` 无结果

### 6.5 架构治理工具（部分缺失）
- 无 Checkstyle/Spotless/Sonar/PMD
- **证据**：pom.xml 无相关插件

---

## 7. 架构违规（按严重程度排序）

| # | 文件 | 类 | 问题 | 严重程度 |
|---|---|---|---|---|
| 1 | `controller/VerificationCodeController.java:13,84,174` | VerificationCodeController | 直接依赖 StringRedisTemplate，应通过 Service/CachePort | P0（基础设施泄漏到表现层） |
| 2 | `vo/SeckillOrderVO.java:3,106-132` | SeckillOrderVO | `from(SeckillOrder)` 直接依赖 Entity，VO→Entity 违规 | P1（已冻结，未修复） |
| 3 | `service/impl/CategoryServiceImpl.java:14,56` | CategoryServiceImpl | 依赖 ProductMapper（跨模块），应通过 ProductService | P1（跨模块 Mapper） |
| 4 | 全局 108 处 | Controller/ServiceImpl/Aspect | 依赖 SecurityUtils 具体实现而非 CurrentUserContext 抽象 | P1（抽象未被采用） |
| 5 | `service/impl/ProductServiceImpl.java:17` | ProductServiceImpl | 依赖 CategoryMapper（跨模块），应通过 CategoryService | P2（跨模块 Mapper，Product 依赖 Category 可能有合理性） |
| 6 | `util/` + `utils/` | - | 同时存在 util/ 和 utils/ 两个包，命名混乱 | P2（代码组织） |
| 7 | `entity/` 全局包 | 25 个 Entity | Entity 同时用于 Domain/Database/API，未分离 | P2（DDD 未开始） |
| 8 | `service/SeckillInventoryPort.java` | SeckillInventoryPort | 端口接口放在 service/ 包而非 shared/kernel/port/ 或 seckill 模块 port/ | P2（端口位置不规范） |

---

## 8. 跨模块依赖矩阵

| From | To | 依赖方式 | 是否允许 | 问题 |
|---|---|---|---|---|
| OrderServiceImpl | ProductService | Service 接口 | ✅ 允许 | 通过 Public API（但无明确 API 边界） |
| OrderServiceImpl | ProductSkuService | Service 接口 | ✅ 允许 | 同上 |
| OrderServiceImpl | CouponUsageService | Service 接口 | ✅ 允许 | 同上 |
| OrderServiceImpl | InventoryService | Service 接口 | ✅ 允许 | 同上 |
| OrderServiceImpl | MessageBusPort | Port | ✅ 允许 | 基础设施端口 |
| OrderServiceImpl | NormalOrderMapper/NormalOrderItemMapper | 同模块 Mapper | ✅ 允许 | 订单模块内部 |
| CategoryServiceImpl | ProductMapper | 跨模块 Mapper | ❌ 违规 | 应通过 ProductService |
| ProductServiceImpl | CategoryMapper | 跨模块 Mapper | ⚠️ 存疑 | Product 依赖 Category，可论证合理 |
| ProductReviewServiceImpl | ProductMapper/ProductSkuMapper | 跨模块 Mapper | ✅ 允许 | Review 属于 Product 模块 |
| InventoryServiceImpl | ProductMapper/ProductSkuMapper | 跨模块 Mapper | ✅ 允许 | Inventory 属于 Product 模块 |
| AuthServiceImpl | UserMapper/LoginLogMapper | 跨模块 Mapper | ✅ 允许 | Auth 属于 User/Identity 模块 |
| SeckillOrderServiceImpl | SeckillGoodsMapper | 同模块 Mapper | ✅ 允许 | Seckill 模块内部 |
| AI (ai/*) | OrderMapper/ProductMapper/InventoryService | 无 | ✅ 允许 | AI 不进入核心事务（0 依赖） |
| Tracking (analytics/tracking/*) | service.impl | 无 | ✅ 允许 | Tracking 旁路（0 依赖） |
| Controller | SecurityUtils | 具体实现 | ⚠️ 存疑 | 应依赖 CurrentUserContext 抽象 |
| VerificationCodeController | StringRedisTemplate | 基础设施 | ❌ 违规 | 应通过 Service/CachePort |

---

## 9. 重复实现分析

| 能力 | 实现数量 | 位置 | 是否需要统一 |
|---|---:|---|---|
| 用户身份获取 | 2（1 抽象 + 1 具体） | CurrentUserContext(抽象) + SecurityUtils(实现) | 抽象已存在但未被采用，108 处直接依赖具体实现 |
| JWT 解析 | 1 | security/JwtUtils | ✅ 已统一 |
| 优惠计算 | 1 | CouponUsageService.calculateDiscount | ✅ 已统一（从 CouponService 拆分） |
| SSE 流式 | 1 | shared/kernel/util/SseUtils | ✅ 已统一（消除 Controller 重复） |
| 秒杀库存扣减 | 2（合理） | SeckillLuaService(Redis Lua) + SeckillDbStrategy(DB 乐观) | 合理，不同策略 |
| 秒杀库存回补 | 1 | SeckillInventoryPort.rollback | ✅ 已统一 |
| 普通订单库存回补 | 1 | OrderLifecycleServiceImpl.rollbackStockByItems | ✅ 已统一 |
| 订单状态判断 | 未发现重复 | - | ✅ 无重复 |
| 价格计算 | 未发现 PriceUtil 重复 | - | ✅ 无重复 |

---

## 10. God Service / God Class

**God Service 已治理完成**，所有 ServiceImpl < 500 行：

| 类 | 行数 | 状态 |
|---|---:|---|
| SeckillOrderServiceImpl | 498 | ✅ 已治理（接近上限） |
| OrderServiceImpl | 497 | ✅ 已治理（从 1175 行拆分） |
| ProductServiceImpl | 478 | ✅ 已治理 |
| CouponServiceImpl | 473 | ✅ 已治理（拆分出 CouponUsageService） |
| AuthServiceImpl | 440 | ✅ 已治理 |

**潜在 God Class 风险**：
- `entity/` 下 25 个 Entity 承载多职责（Domain + DB + API），但这是 DDD 未开始的问题，非 God Class
- `config/SecurityConfig.java` 集中所有安全配置，但 Config 类合理

---

## 11. Infrastructure 泄漏

```text
Domain -> Redis:        无（无 domain 层）
Domain -> MyBatis:      无（无 domain 层）
Domain -> RabbitMQ:     无（无 domain 层）
Application -> Infrastructure:
  - VerificationCodeController -> StringRedisTemplate  ❌ P0 违规
  - SystemHealthMonitorImpl -> StringRedisTemplate     ⚠️ 合理（健康监控）
Module A -> Module B Mapper:
  - CategoryServiceImpl -> ProductMapper               ❌ P1 违规
  - ProductServiceImpl -> CategoryMapper               ⚠️ 存疑
Controller -> SecurityUtils (具体实现)                 ⚠️ P1 抽象未采用
```

**注意**：由于无 domain 层，"Domain -> Infrastructure" 违规不适用。Infrastructure 泄漏主要在 Application/Controller 层。

---

## 12. 当前最严重的 P0 / P1 / P2 问题

### P0（会直接破坏目标架构或造成严重耦合）

| 优先级 | 问题 | 位置 | 为什么重要 | 推荐改法 |
|---|---|---|---|---|
| P0 | 核心业务未按模块组织代码 | controller/service/mapper/entity 全局包 | Modular Monolith 的核心要求未达成，无法建立模块边界 | 按业务模块重组为 order/product/user/coupon/seckill/... |

### P1（当前已经明显影响模块化）

| 优先级 | 问题 | 位置 | 为什么重要 | 推荐改法 |
|---|---|---|---|---|
| P1 | 无 DDD 分层（无 domain/Aggregate/Repository 抽象） | 无 domain 包 | 业务规则归属不明确，Entity 多职责 | 建立 domain 层，抽取 Aggregate 和 Repository 接口 |
| P1 | 无模块 Public API | 无 api 包 | 模块间协作无明确边界，Service 接口散落全局 | 建立模块 api 包，定义 Public API |
| P1 | VerificationCodeController 基础设施泄漏 | VerificationCodeController:13,84,174 | 表现层直接操作 Redis，违反分层 | 下沉到 VerificationCodeService，通过 CachePort |
| P1 | SeckillOrderVO 依赖 Entity | SeckillOrderVO:106-132 | VO→Entity 违规，分层不清 | 改为独立 VO 字段，通过 Converter 转换 |
| P1 | CurrentUserContext 抽象未被采用 | 108 处直接依赖 SecurityUtils | 抽象存在但未执行，无法解耦 Spring Security | 全局替换 SecurityUtils 为 CurrentUserContext |

### P2（可以后续治理）

| 优先级 | 问题 | 位置 | 为什么重要 | 推荐改法 |
|---|---|---|---|---|
| P2 | CategoryServiceImpl 依赖 ProductMapper | CategoryServiceImpl:14 | 跨模块 Mapper | 改用 ProductService |
| P2 | 无 Checkstyle/Spotless/Sonar/PMD | pom.xml | 架构治理工具不全 | 引入静态检查工具 |
| P2 | util/ 和 utils/ 重复 | util/, utils/ | 命名混乱 | 合并为 util/ |
| P2 | SeckillInventoryPort 位置不规范 | service/SeckillInventoryPort | 端口应放 port/ 包 | 移至 shared/kernel/port/ 或 seckill 模块 port/ |
| P2 | 无 Java 模块化 | 无 module-info.java | 编译期模块边界 | 后续可选引入 JPMS |

---

## 13. 当前阶段判断

```text
Phase 5（Infrastructure 隔离基本完成，业务模块化未开始）
```

**解释**：

- ✅ Phase 0（代码盘点）：已完成
- ✅ Phase 1（架构规则）：已完成（ArchUnit 21 规则）
- ✅ Phase 2（Shared Kernel）：已完成（port/adapter/CurrentUserContext）
- ⚠️ Phase 3（Identity）：部分（CurrentUserContext 抽象存在但未采用）
- ❌ Phase 4-13（业务模块 Domain/Application 重构）：**未开始**（无 domain 层、无模块化）
- ✅ Phase 5（Infrastructure 隔离）：基本完成（CachePort/MessageBusPort，个别泄漏）
- ✅ Phase 6（重复实现治理）：部分完成（calculateDiscount/JWT/SSE 统一，CurrentUserContext 未采用）
- ⚠️ Phase 7（架构治理与测试）：部分（ArchUnit/JaCoCo 有，缺其他工具）
- ✅ Phase 14（Tracking）：已完成（旁路能力）
- ✅ Phase 15-16（AI）：已完成（Gateway 统一，不进入核心事务）
- ❌ Phase 8（Modular Monolith）：**未达成**（未按业务模块组织）

**关键判断依据**：项目完成了"分层规则治理"和"基础设施隔离"（Phase 1/2/5/14/15-16），但**核心业务的模块化和 DDD 重构（Phase 4-13）未开始**。代码仍是传统分层结构，而非 Modular Monolith。不能因为 ai/analytics/shared 模块化就判断为 Phase 8。

---

## 14. 下一步最优先的 3 个动作（按 ROI 排序）

### 动作 1：建立业务模块目录结构 + 模块 Public API（最高 ROI）

```text
目标：将核心业务从全局 controller/service/mapper/entity 包重组为按业务模块组织，
      每个模块定义 Public API（api 包），模块间只能通过 Public API 协作。

涉及文件：
  - 新建 order/、product/、user/、coupon/、seckill/、inventory/、cart/、payment/、review/ 等模块包
  - 每个模块下建立 api/（Public API）、application/（Application Service）、domain/（Domain）、infrastructure/（Mapper/PO）
  - 迁移现有 controller/service/mapper/entity/dto/vo 到对应模块
  - 更新 ArchUnit 规则约束模块边界

原因：这是 Modular Monolith 的核心要求。当前所有后续重构（DDD、Repository 抽象、Aggregate）
      都依赖于模块边界建立。不做这一步，后续重构无法落地。

风险：高风险。大规模文件移动，import 路径全量修改，编译可能短期失败。
      需分模块逐步迁移，配合 ArchUnit freeze 防止恶化。

预期收益：建立模块边界后，修改一个业务模块不再影响其他模块，后续 DDD/Repository 重构有落点。

完成标准：
  1. 核心业务（order/product/user/coupon/seckill）按模块组织
  2. 每个模块有 api/ 包定义 Public API
  3. ArchUnit 规则约束模块间只能通过 api/ 协作
  4. 全量测试通过，ArchitectureRulesTest 21/21 pass
```

### 动作 2：采用 CurrentUserContext 抽象，统一用户身份获取（中等 ROI，低风险）

```text
目标：将 108 处对 SecurityUtils 具体实现的依赖替换为 CurrentUserContext 抽象，
      解耦业务层与 Spring Security 基础设施。

涉及文件：
  - controller/OrderController.java（17 处）
  - controller/CartController.java（8 处）
  - controller/UserAddressController.java（5 处）
  - controller/SeckillController.java（3 处）
  - controller/WalletController.java（3 处）
  - controller/CouponController.java（2 处）
  - controller/UserController.java（2 处）
  - controller/ProductReviewController.java（1 处）
  - controller/VerificationCodeController.java（1 处）
  - service/impl/AuthServiceImpl.java（4 处）
  - service/impl/SeckillServiceImpl.java（2 处）
  - service/impl/SeckillGoodsServiceImpl.java（1 处）
  - service/impl/SeckillActivityServiceImpl.java（1 处）
  - analytics/tracking/service/TrackService.java（1 处）
  - analytics/tracking/aspect/TrackingAspect.java（1 处）
  - aspect/OperationLogAspect.java（1 处）
  - ai/customerservice/controller/CustomerServiceController.java（1 处）
  - ai/assistant/controller/ShoppingAssistantController.java（1 处）

原因：CurrentUserContext 抽象已存在但未被采用，这是"已投入但未产出"的债务。
      统一后业务层不再依赖 Spring Security，为后续 identity 模块独立和测试解耦铺路。

风险：低风险。纯机械替换，SecurityUtils 已实现 CurrentUserContext，行为不变。

预期收益：业务层与 Spring Security 解耦，便于测试和未来 identity 模块独立。

完成标准：
  1. 所有业务代码依赖 CurrentUserContext 接口而非 SecurityUtils
  2. SecurityUtils 仅在 security/ 包内部引用
  3. 全量测试通过
```

### 动作 3：修复 P0/P1 架构违规（中等 ROI，低风险）

```text
目标：修复当前已识别的 P0/P1 违规，消除基础设施泄漏和 VO→Entity 依赖。

涉及文件：
  - controller/VerificationCodeController.java（P0：下沉 StringRedisTemplate 到 Service/CachePort）
  - vo/SeckillOrderVO.java（P1：改为独立 VO 字段，通过 Converter 转换，消除 SeckillOrder Entity 依赖）
  - service/impl/CategoryServiceImpl.java（P1：ProductMapper 改为 ProductService）

原因：这些违规已被 ArchUnit freeze，不修复会持续累积债务。
      修复后可缩减 freeze 文件，ArchUnit 规则更严格，防止再次引入。

风险：低风险。VerificationCodeController 下沉是标准操作；SeckillOrderVO 改字段需确认 JSON 响应不变；
      CategoryServiceImpl 改 ProductService 需确认无性能问题（避免 N+1）。

预期收益：消除 3 个架构违规，ArchUnit freeze 缩减，分层更清晰。

完成标准：
  1. VerificationCodeController 无 StringRedisTemplate 依赖
  2. SeckillOrderVO 无 Entity 依赖，JSON 响应结构不变
  3. CategoryServiceImpl 无 ProductMapper 依赖
  4. ArchUnit freeze 文件缩减，21/21 pass
```

---

## 15. 距离目标架构还有多远

> 当前项目已经从 **传统分层单体（Phase 0）** 演进到 **分层规则治理 + 基础设施隔离 + God Service 拆分完成的单体（Phase 5）**，距离 **Pragmatic DDD + Modular Monolith + Ports & Adapters（Phase 8）** 还缺少 **核心业务按模块组织 + DDD 分层（domain/aggregate/repository）+ 模块 Public API + Entity/PO 分离**，已完成约 45%，剩余 55% 主要是业务模块化和 DDD 重构（Phase 4-13）。

---

## 附录：审计方法说明

### 审计原则
1. 不凭目录名称判断架构
2. 不凭类名判断 DDD 是否完成
3. 不因为存在 `domain` 包就认为已经完成 DDD
4. 不因为存在 `api` 包就认为已经完成 Modular Monolith
5. 必须追踪真实依赖关系
6. 必须区分"结构迁移"和"真正解耦"
7. 必须给代码证据
8. 不确定的地方明确标记"无法确认"
9. 不为了凑完成度而推测

### 审计依据
- 代码扫描：353 个 main java 文件 + 20 个 test java 文件
- 依赖分析：grep 跨模块 Mapper/RedisTemplate/RabbitTemplate/SecurityContextHolder
- ArchUnit 规则：21 条规则 + 22 个冻结违规
- 提交历史：154 个 commit，最近 30 个为架构重构提交
- pom.xml：依赖和构建插件分析

### 审计范围
- ✅ 模块/包结构
- ✅ 基础设施依赖
- ✅ 重复实现
- ✅ 跨模块依赖
- ✅ DDD 维度
- ✅ Ports & Adapters
- ✅ Infrastructure 隔离
- ✅ Entity/PO/DTO/VO 分离
- ✅ Controller/Application 分层
- ✅ God Service
- ✅ AI 架构
- ✅ Tracking 架构
- ✅ 测试和架构治理

---

**审计完成。本次审计为 Read-only，未修改任何代码。**