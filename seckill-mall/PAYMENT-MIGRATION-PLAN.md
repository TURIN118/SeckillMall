# Payment 模块迁移计划 (PAYMENT-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase PM - Payment 模块迁移（Strangler Pattern 第六阶段）
> 依据：COUPON-MIGRATION-PLAN.md + CART-MIGRATION-PLAN.md + PRODUCT-MIGRATION-PLAN.md + ORDER-MIGRATION-PLAN.md + Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Payment 当前结构

### 1.1 当前包结构

Payment 相关代码散落在 6 个不同的顶层包中，共 **15 个核心文件**：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `WalletController` | `controller` | 钱包前台：余额/充值/交易记录（/api/v1/wallet） |
| `AdminRechargeCardController` | `controller` | 充值卡后台：生成/列表/禁用（/api/v1/admin/recharge-cards） |
| `PaymentService` | `service` | 支付扣款业务接口（钱包扣款 + 模拟支付） |
| `PaymentServiceImpl` | `service.impl` | PaymentService 实现 |
| `WalletService` | `service` | 钱包业务接口（余额查询 + 交易记录） |
| `WalletServiceImpl` | `service.impl` | WalletService 实现 |
| `RechargeCardService` | `service` | 充值卡业务接口（生成/充值/列表/禁用/查询已使用） |
| `RechargeCardServiceImpl` | `service.impl` | RechargeCardService 实现 |
| `RechargeCard` | `entity` | 充值卡 PO（t_recharge_card） |
| `RechargeCardMapper` | `mapper` | 充值卡 Mapper |
| `RechargeCardStatus` | `entity.enums` | 充值卡状态枚举（UNUSED/USED/DISABLED） |
| `WalletRecordVO` | `vo` | 钱包交易记录视图 |
| `RechargeCardVO` | `vo` | 充值卡视图（不含卡密，@JsonIgnore） |
| `RechargeCardGenerateVO` | `vo` | 充值卡生成视图（含明文卡密，仅生成时返回） |
| `RechargeCardConverter` | `converter` | 充值卡 entity ↔ VO 转换器（MapStruct） |

**请求 DTO（保留在 dto 包，Controller 层请求 DTO，含 Bean Validation 注解）**：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `WalletRechargeRequest` | `dto` | 钱包充值请求（卡号 + 卡密） |
| `NormalOrderPayRequest` | `dto` | 普通订单支付请求（payMethod） |
| `RechargeCardGenerateRequest` | `dto` | 充值卡生成请求（faceValue + count） |

### 1.2 Service 方法签名

#### PaymentService（1 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `pay` | `void pay(Long userId, BigDecimal amount, String payMethod)` | 支付扣款（WALLET 扣余额，其他模拟支付） | ⚠️ 裸参数（3 个），被 OrderLifecycleServiceImpl + SeckillOrderServiceImpl 跨模块调用 |

#### WalletService（2 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `getBalance` | `BigDecimal getBalance(Long userId)` | 查询用户余额 | ⚠️ 返回裸 BigDecimal |
| `listRecords` | `List<WalletRecordVO> listRecords(Long userId)` | 钱包交易记录 | ⚠️ 返回 VO |

#### RechargeCardService（5 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `generate` | `List<RechargeCardGenerateVO> generate(BigDecimal faceValue, Integer count)` | 批量生成充值卡 | ⚠️ 返回 VO，裸参数 |
| `recharge` | `BigDecimal recharge(String cardNo, String cardPassword, Long userId)` | 充值卡充值 | ⚠️ 返回裸 BigDecimal，裸参数（3 个） |
| `listPage` | `PageResult<RechargeCardVO> listPage(Integer pageNum, Integer pageSize, String batchNo, String status)` | 后台分页查询 | ⚠️ 返回 VO，裸参数（4 个） |
| `disable` | `void disable(Long id)` | 禁用充值卡 | ✅ |
| `getUsedCardsByUser` | `List<RechargeCard> getUsedCardsByUser(Long userId)` | 查询用户已使用的充值卡 | ⚠️ 返回 Entity（泄漏 PO），仅供 WalletServiceImpl 跨模块内部调用 |

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `WalletController` | `/api/v1/wallet` | `GET /balance` | 查询当前用户余额 | BUYER/ADMIN |
| | | `POST /recharge` | 充值（通过充值卡） | BUYER/ADMIN |
| | | `GET /records` | 交易记录 | BUYER/ADMIN |
| `AdminRechargeCardController` | `/api/v1/admin/recharge-cards` | `GET /list` | 分页查询充值卡列表 | ADMIN |
| | | `POST /generate` | 批量生成充值卡 | ADMIN |
| | | `PUT /{id}/disable` | 禁用充值卡 | ADMIN |

### 1.4 跨模块依赖关系

#### Payment 依赖的模块（出向依赖）

| 被依赖模块 | 依赖方式 | 用途 |
|---|---|---|
| **identity** | `UserApi` | PaymentServiceImpl 钱包扣款（deductBalance）；WalletServiceImpl 查余额（getUserById）；RechargeCardServiceImpl 充值后加余额（addBalance + getUserById） |
| **order** | `OrderQueryApi` | WalletServiceImpl.listRecords 查普通订单钱包支付记录（getWalletPaidOrders） |
| **seckill** | `SeckillOrderService` | WalletServiceImpl.listRecords 查秒杀订单钱包支付记录（getWalletPaidOrdersByUser） |

#### 依赖 Payment 的模块（入向依赖）

| 调用方模块 | 依赖方式 | 用途 |
|---|---|---|
| **order** | `PaymentService.pay` | `OrderLifecycleServiceImpl.payNormalOrder` 普通订单支付扣款 |
| **seckill** | `PaymentService.pay` | `SeckillOrderServiceImpl.payOrder` 秒杀订单支付扣款 |
| **payment 内部** | `RechargeCardService.getUsedCardsByUser` | `WalletServiceImpl.listRecords` 查充值记录 |
| **payment 内部** | `RechargeCardService.recharge` | `WalletController.recharge` 充值端点 |

### 1.5 Entity / VO / DTO 字段

#### RechargeCard Entity（t_recharge_card）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `cardNo` | `String` | 卡号（唯一） |
| `cardPassword` | `String` | 卡密（BCrypt 加密存储） |
| `faceValue` | `BigDecimal` | 面额 |
| `status` | `RechargeCardStatus` | 状态：UNUSED/USED/DISABLED |
| `usedBy` | `Long` | 使用者用户ID |
| `usedTime` | `LocalDateTime` | 使用时间 |
| `batchNo` | `String` | 批次号 |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### WalletRecordVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 记录ID |
| `type` | `String` | 交易类型：RECHARGE/CONSUME/REFUND |
| `amount` | `BigDecimal` | 交易金额（正数为入账，负数为出账） |
| `balanceAfter` | `BigDecimal` | 交易后余额 |
| `createTime` | `LocalDateTime` | 交易时间 |
| `remark` | `String` | 备注 |

#### RechargeCardVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键ID |
| `cardNo` | `String` | 卡号 |
| `cardPassword` | `String` | 卡密明文（@JsonIgnore，列表查询时屏蔽） |
| `faceValue` | `BigDecimal` | 面额 |
| `status` | `String` | 状态：UNUSED/USED/DISABLED |
| `usedBy` | `Long` | 使用者用户ID |
| `usedTime` | `LocalDateTime` | 使用时间 |
| `batchNo` | `String` | 批次号 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### RechargeCardGenerateVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键ID |
| `cardNo` | `String` | 卡号 |
| `cardPassword` | `String` | 卡密明文（仅生成时返回一次，不带 @JsonIgnore） |
| `faceValue` | `BigDecimal` | 面额 |
| `status` | `String` | 状态：UNUSED/USED/DISABLED |
| `batchNo` | `String` | 批次号 |
| `createTime` | `LocalDateTime` | 创建时间 |

#### WalletRechargeRequest（保留在 dto 包，Controller 层请求 DTO）

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `cardNo` | `String` | `@NotBlank` + `@Size(max=64)` | 卡号 |
| `cardPassword` | `String` | `@NotBlank` + `@Size(max=128)` | 卡密（明文） |

#### NormalOrderPayRequest（保留在 dto 包，Controller 层请求 DTO）

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `payMethod` | `String` | `@NotBlank` | 支付方式：WALLET/ALIPAY/WECHAT 等 |

#### RechargeCardGenerateRequest（保留在 dto 包，Controller 层请求 DTO）

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `faceValue` | `BigDecimal` | `@NotNull` + `@DecimalMin(0.01)` | 面额 |
| `count` | `Integer` | `@NotNull` + `@Min(1)` + `@Max(1000)` | 生成数量 |

### 1.6 Mapper 自定义方法

| Mapper | 自定义方法 | 说明 |
|---|---|---|
| `RechargeCardMapper` | 无 | 仅继承 `BaseMapper<RechargeCard>`，复杂查询使用 Lambda Wrapper |

### 1.7 Converter（MapStruct）

| Converter | 方法 | 说明 |
|---|---|---|
| `RechargeCardConverter` | `RechargeCardVO toVO(RechargeCard entity)` | entity → VO（列表查询场景，脱敏：不映射 cardPassword，status 枚举 → String） |

---

## 2. Payment 目标结构（Pragmatic DDD + Modular Monolith）

### 2.1 目标包结构

```
com.seckill.mall.payment/
├── package-info.java                                  # 模块根包
├── api/                                                # API 契约层（对外暴露）
│   ├── package-info.java
│   ├── PaymentApi.java                                 # 支付 API（钱包扣款 + 模拟支付）— 替代 PaymentService
│   ├── WalletApi.java                                  # 钱包 API（余额查询 + 交易记录）— 替代 WalletService
│   ├── RechargeCardApi.java                            # 充值卡 API（生成/充值/列表/禁用）— 替代 RechargeCardService
│   ├── dto/
│   │   ├── package-info.java
│   │   ├── WalletRecordDTO.java                        # 钱包交易记录 DTO（替代 WalletRecordVO 跨模块传递）
│   │   ├── RechargeCardDTO.java                        # 充值卡 DTO（替代 RechargeCardVO 跨模块传递）
│   │   └── RechargeCardGenerateDTO.java                # 充值卡生成 DTO（含明文卡密，仅生成时返回）
│   ├── command/
│   │   ├── package-info.java
│   │   ├── PayCommand.java                             # 支付扣款命令
│   │   ├── RechargeCommand.java                        # 充值命令（卡号 + 卡密 + userId）
│   │   ├── GenerateRechargeCardCommand.java            # 批量生成充值卡命令
│   │   └── DisableRechargeCardCommand.java             # 禁用充值卡命令
│   ├── query/
│   │   ├── package-info.java
│   │   └── RechargeCardQuery.java                      # 充值卡分页查询（pageNum + pageSize + batchNo + status）
│   └── result/
│       ├── package-info.java
│       └── RechargeResult.java                         # 充值结果（newBalance）
├── application/                                        # 应用层
│   ├── package-info.java
│   ├── PaymentApplicationService.java                  # 实现 PaymentApi，委托 PaymentService
│   ├── WalletApplicationService.java                   # 实现 WalletApi，委托 WalletService
│   ├── RechargeCardApplicationService.java             # 实现 RechargeCardApi，委托 RechargeCardService
│   └── facade/
│       ├── package-info.java
│       └── PaymentApiConverter.java                    # VO/Entity ↔ DTO/Command 转换
├── domain/                                             # 领域层
│   ├── package-info.java
│   └── RechargeCardStatus.java                         # 从 entity.enums 迁入
├── infrastructure/                                     # 基础设施层
│   ├── package-info.java
│   ├── mapper/
│   │   ├── package-info.java
│   │   └── RechargeCardMapper.java                     # 从 mapper 包迁入
│   └── entity/
│       ├── package-info.java
│       └── RechargeCard.java                           # 从 entity 包迁入
└── interfaces/                                         # 接口层
    ├── package-info.java
    ├── vo/
    │   ├── package-info.java
    │   ├── WalletRecordVO.java                         # 从 vo 包迁入（前端展示用）
    │   ├── RechargeCardVO.java                         # 从 vo 包迁入
    │   └── RechargeCardGenerateVO.java                 # 从 vo 包迁入
    └── web/
        ├── package-info.java
        ├── WalletController.java                       # 从 controller 包迁入
        └── AdminRechargeCardController.java            # 从 controller 包迁入
```

### 2.2 分层依赖规则（目标）

```
interfaces ──→ application ──→ api
                    │
                    └──→ service（旧层，Strangler 过渡期）
                              │
                              └──→ infrastructure.mapper ──→ infrastructure.entity

api ──✗──→ infrastructure     （API 不依赖基础设施）
application ──✗──→ mapper     （Application 不直接依赖 Mapper）
interfaces ──✗──→ mapper      （Interfaces 不直接依赖 Mapper）
```

### 2.3 与其他模块的关系（目标）

| 关系 | 模块 | 依赖方式 |
|---|---|---|
| Payment → Identity | `UserApi` | 钱包扣款、余额查询、充值后加余额 |
| Payment → Order | `OrderQueryApi` | 钱包交易记录查普通订单支付记录 |
| Payment → Seckill | `SeckillOrderService`（过渡期保留） | 钱包交易记录查秒杀订单支付记录 |
| Order → Payment | `PaymentApi`（替代 `PaymentService`） | 普通订单支付扣款 |
| Seckill → Payment | `PaymentApi`（替代 `PaymentService`） | 秒杀订单支付扣款 |

---

## 3. 文件迁移路径

### 3.1 移动文件（git mv）

| 源路径 | 目标路径 | 阶段 |
|---|---|---|
| `entity/RechargeCard.java` | `payment/infrastructure/entity/RechargeCard.java` | Phase PM.3 |
| `mapper/RechargeCardMapper.java` | `payment/infrastructure/mapper/RechargeCardMapper.java` | Phase PM.3 |
| `entity/enums/RechargeCardStatus.java` | `payment/domain/RechargeCardStatus.java` | Phase PM.3 |
| `vo/WalletRecordVO.java` | `payment/interfaces/vo/WalletRecordVO.java` | Phase PM.4 |
| `vo/RechargeCardVO.java` | `payment/interfaces/vo/RechargeCardVO.java` | Phase PM.4 |
| `vo/RechargeCardGenerateVO.java` | `payment/interfaces/vo/RechargeCardGenerateVO.java` | Phase PM.4 |
| `controller/WalletController.java` | `payment/interfaces/web/WalletController.java` | Phase PM.4 |
| `controller/AdminRechargeCardController.java` | `payment/interfaces/web/AdminRechargeCardController.java` | Phase PM.4 |

### 3.2 新增文件

| 目标路径 | 阶段 |
|---|---|
| `payment/package-info.java` 及所有子包 `package-info.java`（共 15 个） | Phase PM.0 |
| `payment/api/PaymentApi.java` | Phase PM.2 |
| `payment/api/WalletApi.java` | Phase PM.2 |
| `payment/api/RechargeCardApi.java` | Phase PM.2 |
| `payment/api/dto/{WalletRecordDTO,RechargeCardDTO,RechargeCardGenerateDTO}.java` | Phase PM.2 |
| `payment/api/command/{Pay,Recharge,GenerateRechargeCard,DisableRechargeCard}Command.java` | Phase PM.2 |
| `payment/api/query/RechargeCardQuery.java` | Phase PM.2 |
| `payment/api/result/RechargeResult.java` | Phase PM.2 |
| `payment/application/PaymentApplicationService.java` | Phase PM.4 |
| `payment/application/WalletApplicationService.java` | Phase PM.4 |
| `payment/application/RechargeCardApplicationService.java` | Phase PM.4 |
| `payment/application/facade/PaymentApiConverter.java` | Phase PM.4 |

### 3.3 保留不动的文件

| 文件 | 原因 |
|---|---|
| `service/PaymentService.java` | Strangler 过渡期保留，PaymentApplicationService 委托它 |
| `service/impl/PaymentServiceImpl.java` | 同上 |
| `service/WalletService.java` | Strangler 过渡期保留，WalletApplicationService 委托它 |
| `service/impl/WalletServiceImpl.java` | 同上 |
| `service/RechargeCardService.java` | Strangler 过渡期保留，RechargeCardApplicationService 委托它 |
| `service/impl/RechargeCardServiceImpl.java` | 同上 |
| `dto/WalletRechargeRequest.java` | Controller 层请求 DTO，含 Bean Validation 注解，保留在 dto 包 |
| `dto/NormalOrderPayRequest.java` | Controller 层请求 DTO，含 Bean Validation 注解，保留在 dto 包 |
| `dto/RechargeCardGenerateRequest.java` | Controller 层请求 DTO，含 Bean Validation 注解，保留在 dto 包 |
| `converter/RechargeCardConverter.java` | MapStruct 转换器，被 RechargeCardServiceImpl 使用，保留原位（过渡期） |

### 3.4 后续可删除的文件（Strangler 完成后）

| 文件 | 删除时机 |
|---|---|
| `service/PaymentService.java` + `service/impl/PaymentServiceImpl.java` | 业务逻辑完全迁入 payment.application 后 |
| `service/WalletService.java` + `service/impl/WalletServiceImpl.java` | 同上 |
| `service/RechargeCardService.java` + `service/impl/RechargeCardServiceImpl.java` | 同上 |
| `converter/RechargeCardConverter.java` | RechargeCardServiceImpl 删除后，转换逻辑迁入 PaymentApiConverter |

---

## 4. API 边界（PaymentApi + WalletApi + RechargeCardApi）

### 4.1 设计原则

1. **方法用业务语言命名**，禁止 CRUD 命名
2. **入参用 Command/Query 对象**，禁止裸露多参数
3. **出参用 Result/DTO/Snapshot**，禁止暴露 Entity/Mapper/PO
4. **异常通过 `BusinessException(ErrorCode)` 抛出**，不泄露堆栈
5. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

### 4.2 PaymentApi 方法清单（支付 API）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `pay` | `void pay(PayCommand command)` | `PaymentService.pay` |

> **关键变化**：`PaymentService.pay(Long, BigDecimal, String)` 裸参数 → `PaymentApi.pay(PayCommand)`。Command 含 `userId`/`amount`/`payMethod` 三字段。

### 4.3 WalletApi 方法清单（钱包 API）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `getBalance` | `BigDecimal getBalance(Long userId)` | `WalletService.getBalance` |
| `listRecords` | `List<WalletRecordDTO> listRecords(Long userId)` | `WalletService.listRecords` |

> **关键变化**：`WalletService.listRecords` 返回 `List<WalletRecordVO>` → `WalletApi.listRecords` 返回 `List<WalletRecordDTO>`。Controller 层通过 `PaymentApiConverter.toVO` 转回 VO 保持前端契约。

### 4.4 RechargeCardApi 方法清单（充值卡 API）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `generate` | `List<RechargeCardGenerateDTO> generate(GenerateRechargeCardCommand command)` | `RechargeCardService.generate` |
| `recharge` | `RechargeResult recharge(RechargeCommand command)` | `RechargeCardService.recharge` |
| `listPage` | `PageResult<RechargeCardDTO> listPage(RechargeCardQuery query)` | `RechargeCardService.listPage` |
| `disable` | `void disable(DisableRechargeCardCommand command)` | `RechargeCardService.disable` |
| `getUsedCardsByUser` | `List<RechargeCardDTO> getUsedCardsByUser(Long userId)` | `RechargeCardService.getUsedCardsByUser` |

> **关键变化**：
> - `RechargeCardService.recharge` 返回 `BigDecimal` → `RechargeCardApi.recharge` 返回 `RechargeResult`（含 `newBalance` 字段），消除裸 BigDecimal 返回。
> - `RechargeCardService.getUsedCardsByUser` 返回 `List<RechargeCard>`（泄漏 Entity）→ `RechargeCardApi.getUsedCardsByUser` 返回 `List<RechargeCardDTO>`，消除 Entity 泄漏。`WalletServiceImpl` 适配新返回类型。
> - 所有裸参数方法改为 Command/Query 对象入参。

### 4.5 DTO/Command/Query/Result 定义

详见 [PAYMENT-API-CONTRACT.md](PAYMENT-API-CONTRACT.md)。

---

## 5. 迁移步骤（Phase PM）

### Phase PM.0：创建包结构

- 创建 `payment/` 及所有子包（共 15 个 package-info.java）
- 每个包创建 `package-info.java`
- **不修改任何现有代码**

### Phase PM.2：创建 API 层

- 创建 `PaymentApi` + `WalletApi` + `RechargeCardApi` 接口
- 创建 3 个 DTO（`WalletRecordDTO`、`RechargeCardDTO`、`RechargeCardGenerateDTO`）
- 创建 4 个 Command（`PayCommand`、`RechargeCommand`、`GenerateRechargeCardCommand`、`DisableRechargeCardCommand`）
- 创建 1 个 Query（`RechargeCardQuery`）
- 创建 1 个 Result（`RechargeResult`）
- **不修改任何现有代码**

### Phase PM.3：迁移持久化层

- `git mv entity/RechargeCard.java payment/infrastructure/entity/RechargeCard.java`
- `git mv mapper/RechargeCardMapper.java payment/infrastructure/mapper/RechargeCardMapper.java`
- `git mv entity/enums/RechargeCardStatus.java payment/domain/RechargeCardStatus.java`
- 更新 package 声明
- 更新所有 import（RechargeCardServiceImpl、RechargeCardConverter、RechargeCardService、WalletServiceImpl 等引用 RechargeCard/RechargeCardStatus/RechargeCardMapper 的类）
- **回归测试**：mvn test

### Phase PM.4：创建 Application 层 + 移动 Controller/VO

- 创建 `PaymentApplicationService implements PaymentApi`，委托 `PaymentService`
- 创建 `WalletApplicationService implements WalletApi`，委托 `WalletService`
- 创建 `RechargeCardApplicationService implements RechargeCardApi`，委托 `RechargeCardService`
- 创建 `PaymentApiConverter`：VO ↔ DTO、Command → 裸参数 转换
- `git mv vo/WalletRecordVO.java payment/interfaces/vo/WalletRecordVO.java`
- `git mv vo/RechargeCardVO.java payment/interfaces/vo/RechargeCardVO.java`
- `git mv vo/RechargeCardGenerateVO.java payment/interfaces/vo/RechargeCardGenerateVO.java`
- `git mv controller/WalletController.java payment/interfaces/web/WalletController.java`
- `git mv controller/AdminRechargeCardController.java payment/interfaces/web/AdminRechargeCardController.java`
- 更新 package 声明与所有 import
- 切换 Controller 注入 `WalletApi` + `RechargeCardApi` 替代旧 `WalletService` + `RechargeCardService`，调用 API 方法后通过 Converter 转回 VO
- **不删除旧 PaymentService / WalletService / RechargeCardService**（Strangler 过渡）
- **回归测试**：mvn test

### Phase PM.5：迁移外部调用方

- `OrderLifecycleServiceImpl`：`PaymentService` → `PaymentApi`
  - `paymentService.pay(userId, payAmount, payMethod)` → `paymentApi.pay(PayCommand.builder().userId(userId).amount(payAmount).payMethod(payMethod).build())`
- `SeckillOrderServiceImpl`：`PaymentService` → `PaymentApi`
  - 同上
- 更新测试中的 mock 声明（`OrderLifecycleServiceTest`、`SeckillOrderServiceTest`）
- **回归测试**：mvn test

### Phase PM.6：ArchUnit 规则

新增 3 条 payment 模块包边界规则：

| 规则 | 内容 |
|---|---|
| `payment_api_should_not_depend_on_infrastructure` | `payment.api` 不应依赖 `payment.infrastructure` |
| `payment_application_should_not_depend_on_mapper` | `payment.application` 不应直接依赖 `payment.infrastructure.mapper` |
| `payment_interfaces_should_not_depend_on_mapper` | `payment.interfaces` 不应直接依赖 `payment.infrastructure.mapper` |

**回归标准**：mvn test Failures ≤ 1, Errors ≤ 19, ArchUnit 全绿（39 条 = 36 + 3）。

---

## 6. 风险点与缓解

### 6.1 OrderLifecycleServiceImpl / SeckillOrderServiceImpl 对 PaymentService 的强耦合

**风险**：`OrderLifecycleServiceImpl.payNormalOrder` 和 `SeckillOrderServiceImpl.payOrder` 直接调用 `paymentService.pay(userId, payAmount, payMethod)`。切换到 `PaymentApi` 后需保证 Command 封装后与旧逻辑等价。

**缓解**：`PayCommand` 含 `userId`/`amount`/`payMethod` 三字段，`PaymentApplicationService.pay` 委托旧 `PaymentService.pay(command.getUserId(), command.getAmount(), command.getPayMethod())`，完全等价。详见 [PAYMENT-API-CONTRACT.md](PAYMENT-API-CONTRACT.md)。

### 6.2 WalletServiceImpl 对 RechargeCard Entity 的依赖（Entity 泄漏）

**风险**：`WalletServiceImpl.listRecords` 调用 `rechargeCardService.getUsedCardsByUser(userId)` 返回 `List<RechargeCard>`（Entity），并访问 `card.getId()`/`card.getFaceValue()`/`card.getUsedTime()`/`card.getCardNo()` 等字段。切换到 `RechargeCardApi.getUsedCardsByUser` 返回 `List<RechargeCardDTO>` 后需适配字段访问。

**缓解**：`RechargeCardDTO` 包含 `id`/`cardNo`/`faceValue`/`usedTime` 等所有 WalletServiceImpl 所需字段。`WalletServiceImpl` 调用 `rechargeCardApi.getUsedCardsByUser(userId)` 后通过 `PaymentApiConverter.toRechargeVO` 转换，字段访问从 `card.getXxx()` 改为 `dto.getXxx()`，逻辑完全等价。

> **注意**：`WalletServiceImpl` 在过渡期保留对 `RechargeCardService` 的依赖（Strangler 模式），Phase PM.4 仅切换 Controller 调用 API，`WalletServiceImpl` 内部仍调用旧 `RechargeCardService`。`WalletApplicationService` 委托 `WalletService`，间接依赖 `RechargeCardService`，不违反 payment 模块内部包边界规则。后续可考虑将 `WalletServiceImpl` 内部调用也切换到 `RechargeCardApi`，但不在本次迁移范围。

### 6.3 WalletRecordVO / RechargeCardVO / RechargeCardGenerateVO 被前端接口契约锁定

**风险**：3 个 VO 是 `/api/v1/wallet/*` 和 `/api/v1/admin/recharge-cards/*` 端点的返回体，前端依赖其字段。若切换为 DTO 可能破坏前端契约。

**缓解**：**Controller 仍返回 VO**（保留前端契约），内部调用 `WalletApi`/`RechargeCardApi` 拿到 DTO 后通过 `PaymentApiConverter.toVO` 转回 VO。3 个 VO 移极迁入 `payment/interfaces/vo/` 但保留所有字段。

### 6.4 RechargeCardVO.cardPassword 的 @JsonIgnore 注解

**风险**：`RechargeCardVO.cardPassword` 带 `@JsonIgnore`，列表查询时不序列化。`RechargeCardGenerateVO` 不带 `@JsonIgnore`，生成时显式返回明文卡密。迁移 package 后需保证 Jackson 序列化行为不变。

**缓解**：仅移动文件位置，不修改 VO 内容。`@JsonIgnore` 注解与 package 无关，Jackson 序列化行为保持不变。

### 6.5 WalletRechargeRequest / NormalOrderPayRequest / RechargeCardGenerateRequest 位置

**风险**：3 个 Request DTO 在 `dto/` 包，含 Bean Validation 注解，被 `WalletController` 和 `AdminRechargeCardController` 使用。若迁入 `payment/api/command/` 可能与 Command 语义重复。

**缓解**：本次迁移**不动** 3 个 Request DTO，保留在 `dto/` 包（Controller 层请求 DTO）。`PayCommand`/`RechargeCommand`/`GenerateRechargeCardCommand` 是 API 层 Command，由 `PaymentApiConverter.toCommand(Request)` 转换，两者职责不同：前者是 HTTP 请求体（含校验注解），后者是 API 契约入参（纯 POJO）。

### 6.6 RechargeCardStatus 枚举迁移影响面

**风险**：`RechargeCardStatus` 实现 MyBatis-Plus `IEnum<String>` 接口，被 `RechargeCard` Entity 的 `@TableField` 字段使用。迁移 package 后需更新所有 import，包括 Entity、ServiceImpl、可能的 JSON 序列化配置。

**缓解**：仅移动文件位置，不修改枚举内容。迁移后立即跑 mvn test 验证充值卡生成/充值/禁用/列表场景。MyBatis-Plus `IEnum` 机制与 package 无关，仅依赖枚举类全限定名，迁移后 Spring Boot 自动扫描 `@Mapper` 仍能正确映射。

### 6.7 RechargeCardConverter（MapStruct）位置

**风险**：`RechargeCardConverter` 在 `converter/` 包，被 `RechargeCardServiceImpl` 使用（虽然当前 `RechargeCardServiceImpl` 用手工 `toVO`，Converter 是 M-D5 修复预留）。迁移 `RechargeCard` Entity 后 Converter 的 import 需更新。

**缓解**：本次迁移**不动** `RechargeCardConverter`，保留在 `converter/` 包（过渡期）。仅更新其 import 指向新的 `payment/infrastructure/entity/RechargeCard` 和 `payment/interfaces/vo/RechargeCardVO`。待 Strangler 完成后，转换逻辑迁入 `PaymentApiConverter`，再删除 `RechargeCardConverter`。

### 6.8 Strangler 过渡期 PaymentService/WalletService/RechargeCardService 保留

**风险**：3 个 Service + 3 个 ServiceImpl 在过渡期保留，可能与 3 个 API + 3 个 ApplicationService 并存导致语义重复。

**缓解**：3 个 ApplicationService 仅做门面委托，不含业务逻辑；待所有外部调用方迁移完毕且稳定后，再考虑将业务逻辑从旧 ServiceImpl 迁入 ApplicationService 并删除旧 Service（不在本次迁移范围）。

### 6.9 ArchUnit freeze 模式

**风险**：新增 3 条 payment 模块规则后，freeze 模式首次运行会冻结当前违规。若 `payment.application` 当前仍依赖 `payment.infrastructure.mapper`（通过旧 Service 间接依赖），可能被冻结而非立即报错。

**缓解**：Phase PM.4 保证 3 个 ApplicationService 不直接 import `RechargeCardMapper`；Phase PM.5 保证 `OrderLifecycleServiceImpl` / `SeckillOrderServiceImpl` 不再直接 import `PaymentService`。新增规则后跑 ArchUnit 验证全绿。

### 6.10 PaymentApi.pay 返回类型保持 void

**风险**：旧 `PaymentService.pay` 返回 `void`，新 `PaymentApi.pay` 也返回 `void`。`OrderLifecycleServiceImpl` / `SeckillOrderServiceImpl` 调用后不使用返回值，切换无适配成本。

**缓解**：保持 `void` 返回，无需 Result 包装。未来可扩展为返回 `PaymentResult`（含 transactionId 等），但不在本次迁移范围。

---

## 7. 回归测试基线

| 指标 | 基线 | 目标 |
|---|---|---|
| Tests run | 129 | ≥ 129 |
| Failures | 1 | ≤ 1 |
| Errors | 19 | ≤ 19 |
| ArchUnit 规则 | 36 全绿 | 39 全绿（新增 3 条 payment 规则） |

---

## 8. 参考文档

- [COUPON-MIGRATION-PLAN.md](COUPON-MIGRATION-PLAN.md) - Coupon 模块迁移经验（最近完成，主要参考）
- [CART-MIGRATION-PLAN.md](CART-MIGRATION-PLAN.md) - Cart 模块迁移经验
- [PRODUCT-MIGRATION-PLAN.md](PRODUCT-MIGRATION-PLAN.md) - Product 模块迁移经验
- [ORDER-MIGRATION-PLAN.md](ORDER-MIGRATION-PLAN.md) - Order 模块迁移经验
- [PAYMENT-API-CONTRACT.md](PAYMENT-API-CONTRACT.md) - Payment 模块 API 契约