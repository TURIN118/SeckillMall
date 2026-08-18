# Payment 模块 API 契约 (PAYMENT-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase PM.2 - Payment API 契约层定义
> 依据：PAYMENT-MIGRATION-PLAN.md + CouponApi 契约风格 + CartApi 契约风格
> 说明：本文件定义 Payment 模块对外暴露的 API 契约，一经发布只增不改不删。

---

## 1. PaymentApi 接口

**全限定名**：`com.seckill.mall.payment.api.PaymentApi`

**实现类**：`com.seckill.mall.payment.application.PaymentApplicationService`

**职责**：支付扣款能力（钱包扣款 + 模拟支付），供 order / seckill 模块调用。

**设计原则**：
- 方法用业务语言命名，禁止 CRUD 命名
- 入参用 Command/Query 对象，禁止裸露多参数
- 出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO
- 异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈
- 向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

```java
package com.seckill.mall.payment.api;

public interface PaymentApi {

    /**
     * 执行支付扣款。
     *
     * <p>若 payMethod 为 WALLET，原子扣减用户钱包余额，余额不足抛 {@code WALLET_BALANCE_NOT_ENOUGH}；
     * 其他 payMethod 走模拟支付（直接成功，不实际扣款）。
     *
     * @param command 支付命令（userId + amount + payMethod）
     * @throws com.seckill.mall.exception.BusinessException 钱包余额不足时抛出
     */
    void pay(PayCommand command);
}
```

---

## 2. WalletApi 接口

**全限定名**：`com.seckill.mall.payment.api.WalletApi`

**实现类**：`com.seckill.mall.payment.application.WalletApplicationService`

**职责**：钱包能力（余额查询 + 交易记录），供 payment 模块 Controller 调用。

```java
package com.seckill.mall.payment.api;

public interface WalletApi {

    /**
     * 查询指定用户余额。
     *
     * @param userId 用户 ID
     * @return 余额（用户不存在或余额为空时返回 BigDecimal.ZERO）
     */
    BigDecimal getBalance(Long userId);

    /**
     * 查询指定用户的钱包交易记录。
     *
     * <p>合并充值卡充值记录（RECHARGE，金额为正）+ 普通订单钱包支付记录（CONSUME，金额为负）
     * + 秒杀订单钱包支付记录（CONSUME，金额为负），按交易时间倒序。
     *
     * @param userId 用户 ID
     * @return 交易记录列表（DTO，不含敏感信息，卡号已脱敏）
     */
    List<WalletRecordDTO> listRecords(Long userId);
}
```

---

## 3. RechargeCardApi 接口

**全限定名**：`com.seckill.mall.payment.api.RechargeCardApi`

**实现类**：`com.seckill.mall.payment.application.RechargeCardApplicationService`

**职责**：充值卡能力（批量生成 / 充值 / 后台查询 / 禁用 / 查询已使用），供 payment 模块 Controller 和 WalletService 调用。

```java
package com.seckill.mall.payment.api;

public interface RechargeCardApi {

    /**
     * 批量生成充值卡。
     *
     * <p>使用 SecureRandom 生成卡号与卡密，卡密通过 BCrypt 加密后入库；明文仅一次性返回给调用方。
     *
     * @param command 生成命令（faceValue + count）
     * @return 生成的充值卡列表（含卡号卡密明文，仅此一次返回）
     */
    List<RechargeCardGenerateDTO> generate(GenerateRechargeCardCommand command);

    /**
     * 充值：校验卡号卡密 → 更新用户余额与卡状态。
     *
     * @param command 充值命令（cardNo + cardPassword + userId）
     * @return 充值结果（含 newBalance）
     * @throws BusinessException 卡不存在/已使用/已禁用/卡密错误时抛出
     */
    RechargeResult recharge(RechargeCommand command);

    /**
     * 后台分页查询充值卡列表。
     *
     * @param query 查询条件（pageNum + pageSize + batchNo + status）
     * @return 分页结果（DTO，不含卡密）
     */
    PageResult<RechargeCardDTO> listPage(RechargeCardQuery query);

    /**
     * 禁用充值卡（仅未使用的卡可禁用）。
     *
     * @param command 禁用命令（id）
     * @throws BusinessException 卡不存在或状态非 UNUSED 时抛出
     */
    void disable(DisableRechargeCardCommand command);

    /**
     * 查询用户已使用的充值卡（status=USED）。
     *
     * <p>仅供 WalletApplicationService 调用，用于展示钱包充值记录。
     *
     * @param userId 用户 ID
     * @return 用户已使用的充值卡列表（DTO，不含卡密）
     */
    List<RechargeCardDTO> getUsedCardsByUser(Long userId);
}
```

---

## 4. DTO 定义

### 4.1 WalletRecordDTO

**全限定名**：`com.seckill.mall.payment.api.dto.WalletRecordDTO`

**用途**：替代 `WalletRecordVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`WalletRecordVO` → `WalletRecordDTO`（由 `PaymentApiConverter.toDTO` 转换）；`RechargeCard` + `balanceAfter` → `WalletRecordDTO`（由 `WalletServiceImpl.toRechargeVO` 逻辑转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 记录ID | `WalletRecordVO.id` |
| `type` | `String` | 交易类型：RECHARGE/CONSUME/REFUND | `WalletRecordVO.type` |
| `amount` | `BigDecimal` | 交易金额（正数为入账，负数为出账） | `WalletRecordVO.amount` |
| `balanceAfter` | `BigDecimal` | 交易后余额 | `WalletRecordVO.balanceAfter` |
| `createTime` | `LocalDateTime` | 交易时间 | `WalletRecordVO.createTime` |
| `remark` | `String` | 备注 | `WalletRecordVO.remark` |

```java
package com.seckill.mall.payment.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRecordDTO {
    private Long id;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createTime;
    private String remark;
}
```

### 4.2 RechargeCardDTO

**全限定名**：`com.seckill.mall.payment.api.dto.RechargeCardDTO`

**用途**：替代 `RechargeCard` Entity 跨模块传递；替代 `RechargeCardVO` 作为 Application 层返回值。

**来源映射**：`RechargeCard` Entity → `RechargeCardDTO`（由 `PaymentApiConverter.toDTO` 转换）；`RechargeCardVO` → `RechargeCardDTO`（由 `PaymentApiConverter.toDTOFromVO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 主键ID | `RechargeCard.id` |
| `cardNo` | `String` | 卡号 | `RechargeCard.cardNo` |
| `faceValue` | `BigDecimal` | 面额 | `RechargeCard.faceValue` |
| `status` | `String` | 状态：UNUSED/USED/DISABLED | `RechargeCard.status.getCode()` |
| `usedBy` | `Long` | 使用者用户ID | `RechargeCard.usedBy` |
| `usedTime` | `LocalDateTime` | 使用时间 | `RechargeCard.usedTime` |
| `batchNo` | `String` | 批次号 | `RechargeCard.batchNo` |
| `createTime` | `LocalDateTime` | 创建时间 | `RechargeCard.createTime` |
| `updateTime` | `LocalDateTime` | 更新时间 | `RechargeCard.updateTime` |

```java
package com.seckill.mall.payment.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCardDTO {
    private Long id;
    private String cardNo;
    private BigDecimal faceValue;
    private String status;
    private Long usedBy;
    private LocalDateTime usedTime;
    private String batchNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

> **注意**：`RechargeCardDTO` **不含** `cardPassword` 字段（脱敏，避免卡密泄露）。

### 4.3 RechargeCardGenerateDTO

**全限定名**：`com.seckill.mall.payment.api.dto.RechargeCardGenerateDTO`

**用途**：充值卡生成结果 DTO，含明文卡密，仅生成接口返回一次。替代 `RechargeCardGenerateVO` 作为 Application 层返回值。

**来源映射**：`RechargeCard` Entity + `plainPwd` → `RechargeCardGenerateDTO`（由 `RechargeCardServiceImpl.generate` 逻辑转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 主键ID | `RechargeCard.id` |
| `cardNo` | `String` | 卡号 | `RechargeCard.cardNo` |
| `cardPassword` | `String` | 卡密明文（仅生成时返回一次） | 生成时的明文卡密 |
| `faceValue` | `BigDecimal` | 面额 | `RechargeCard.faceValue` |
| `status` | `String` | 状态：UNUSED/USED/DISABLED | `RechargeCardStatus.UNUSED.getCode()` |
| `batchNo` | `String` | 批次号 | `RechargeCard.batchNo` |
| `createTime` | `LocalDateTime` | 创建时间 | `RechargeCard.createTime` |

```java
package com.seckill.mall.payment.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCardGenerateDTO {
    private Long id;
    private String cardNo;
    private String cardPassword;
    private BigDecimal faceValue;
    private String status;
    private String batchNo;
    private LocalDateTime createTime;
}
```

---

## 5. Command 定义

### 5.1 PayCommand

**全限定名**：`com.seckill.mall.payment.api.command.PayCommand`

**业务语义**：执行支付扣款。

**原方法**：`PaymentService.pay(Long userId, BigDecimal amount, String payMethod)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Long` | 是 | 用户 ID |
| `amount` | `BigDecimal` | 是 | 支付金额 |
| `payMethod` | `String` | 是 | 支付方式（WALLET/ALIPAY/WECHAT 等） |

```java
package com.seckill.mall.payment.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayCommand {
    private Long userId;
    private BigDecimal amount;
    private String payMethod;
}
```

### 5.2 RechargeCommand

**全限定名**：`com.seckill.mall.payment.api.command.RechargeCommand`

**业务语义**：充值卡充值。

**原方法**：`RechargeCardService.recharge(String cardNo, String cardPassword, Long userId)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `cardNo` | `String` | 是 | 卡号 |
| `cardPassword` | `String` | 是 | 卡密（明文） |
| `userId` | `Long` | 是 | 用户 ID |

```java
package com.seckill.mall.payment.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCommand {
    private String cardNo;
    private String cardPassword;
    private Long userId;
}
```

### 5.3 GenerateRechargeCardCommand

**全限定名**：`com.seckill.mall.payment.api.command.GenerateRechargeCardCommand`

**业务语义**：批量生成充值卡。

**原方法**：`RechargeCardService.generate(BigDecimal faceValue, Integer count)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `faceValue` | `BigDecimal` | 是 | 面额（必须大于0） |
| `count` | `Integer` | 是 | 生成数量（1-1000） |

```java
package com.seckill.mall.payment.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRechargeCardCommand {
    private BigDecimal faceValue;
    private Integer count;
}
```

### 5.4 DisableRechargeCardCommand

**全限定名**：`com.seckill.mall.payment.api.command.DisableRechargeCardCommand`

**业务语义**：禁用充值卡。

**原方法**：`RechargeCardService.disable(Long id)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Long` | 是 | 充值卡 ID |

```java
package com.seckill.mall.payment.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisableRechargeCardCommand {
    private Long id;
}
```

---

## 6. Query 定义

### 6.1 RechargeCardQuery

**全限定名**：`com.seckill.mall.payment.api.query.RechargeCardQuery`

**用途**：充值卡分页查询条件（后台 listPage）。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` | `Integer` | 否 | 页码（默认1） |
| `pageSize` | `Integer` | 否 | 每页大小（默认10） |
| `batchNo` | `String` | 否 | 批次号筛选 |
| `status` | `String` | 否 | 状态筛选：UNUSED/USED/DISABLED |

```java
package com.seckill.mall.payment.api.query;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCardQuery {
    private Integer pageNum;
    private Integer pageSize;
    private String batchNo;
    private String status;
}
```

---

## 7. Result 定义

### 7.1 RechargeResult

**全限定名**：`com.seckill.mall.payment.api.result.RechargeResult`

**用途**：充值结果，`RechargeCardApi.recharge` 返回值。

| 字段 | 类型 | 说明 |
|---|---|---|
| `newBalance` | `BigDecimal` | 充值后的最新余额 |

```java
package com.seckill.mall.payment.api.result;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeResult {
    /** 充值后的最新余额 */
    private BigDecimal newBalance;
}
```

> **WalletController 适配**：`BigDecimal newBalance = rechargeCardApi.recharge(command).getNewBalance();`

---

## 8. 转换器（PaymentApiConverter）

**全限定名**：`com.seckill.mall.payment.application.facade.PaymentApiConverter`

**职责**：集中存放旧 VO/Entity 与新 API 层 DTO/Command 之间的转换方法，供 `PaymentApplicationService` / `WalletApplicationService` / `RechargeCardApplicationService` 调用。所有方法均为无状态静态方法。

### 8.1 转换方法清单

| 方法 | 签名 | 用途 |
|---|---|---|
| `toPayCommand` | `PayCommand toPayCommand(Long userId, BigDecimal amount, String payMethod)` | 裸参数 → Command |
| `toRechargeCommand` | `RechargeCommand toRechargeCommand(String cardNo, String cardPassword, Long userId)` | 裸参数 → Command |
| `toRechargeCommand` | `RechargeCommand toRechargeCommand(WalletRechargeRequest req, Long userId)` | Request + userId → Command |
| `toGenerateCommand` | `GenerateRechargeCardCommand toGenerateCommand(RechargeCardGenerateRequest req)` | Request → Command |
| `toDisableCommand` | `DisableRechargeCardCommand toDisableCommand(Long id)` | 裸参数 → Command |
| `toRechargeCardQuery` | `RechargeCardQuery toRechargeCardQuery(Integer pageNum, Integer pageSize, String batchNo, String status)` | 裸参数 → Query |
| `toRechargeResult` | `RechargeResult toRechargeResult(BigDecimal newBalance)` | BigDecimal → Result |
| `toDTO` | `WalletRecordDTO toDTO(WalletRecordVO vo)` | WalletRecordVO → WalletRecordDTO |
| `toDTOList` | `List<WalletRecordDTO> toDTOList(List<WalletRecordVO> voList)` | WalletRecordVO 列表 → WalletRecordDTO 列表 |
| `toVO` | `WalletRecordVO toVO(WalletRecordDTO dto)` | WalletRecordDTO → WalletRecordVO（Controller 层前端契约适配） |
| `toRechargeCardDTO` | `RechargeCardDTO toRechargeCardDTO(RechargeCardVO vo)` | RechargeCardVO → RechargeCardDTO |
| `toRechargeCardDTOList` | `List<RechargeCardDTO> toRechargeCardDTOList(List<RechargeCardVO> voList)` | RechargeCardVO 列表 → RechargeCardDTO 列表 |
| `toRechargeCardVO` | `RechargeCardVO toRechargeCardVO(RechargeCardDTO dto)` | RechargeCardDTO → RechargeCardVO |
| `toRechargeCardVOList` | `List<RechargeCardVO> toRechargeCardVOList(List<RechargeCardDTO> dtoList)` | RechargeCardDTO 列表 → RechargeCardVO 列表 |
| `toGenerateDTO` | `RechargeCardGenerateDTO toGenerateDTO(RechargeCardGenerateVO vo)` | RechargeCardGenerateVO → RechargeCardGenerateDTO |
| `toGenerateDTOList` | `List<RechargeCardGenerateDTO> toGenerateDTOList(List<RechargeCardGenerateVO> voList)` | RechargeCardGenerateVO 列表 → RechargeCardGenerateDTO 列表 |
| `toGenerateVO` | `RechargeCardGenerateVO toGenerateVO(RechargeCardGenerateDTO dto)` | RechargeCardGenerateDTO → RechargeCardGenerateVO |
| `toGenerateVOList` | `List<RechargeCardGenerateVO> toGenerateVOList(List<RechargeCardGenerateDTO> dtoList)` | RechargeCardGenerateDTO 列表 → RechargeCardGenerateVO 列表 |

### 8.2 转换规则

- **Entity → DTO**：`status` 字段 `RechargeCardStatus` → `String`（`getCode()`），丢弃 `isDeleted`/`cardPassword` 等基础设施/敏感字段
- **VO → DTO**：全字段映射（核心 + 展示），保留前端展示信息
- **DTO → VO**：全字段映射（核心 + 展示），保留前端契约
- **Request → Command**：字段一一对应，丢弃 Bean Validation 注解（Command 是纯 POJO）
- **BigDecimal → Result**：`RechargeResult` 仅含 `newBalance` 字段，包装裸 BigDecimal

---

## 9. ApplicationService 委托映射

### 9.1 PaymentApplicationService implements PaymentApi

委托 `PaymentService`：

| PaymentApi 方法 | 委托 PaymentService 方法 | 转换 |
|---|---|---|
| `pay(command)` | `paymentService.pay(userId, amount, payMethod)` | Command 解包 |

### 9.2 WalletApplicationService implements WalletApi

委托 `WalletService`：

| WalletApi 方法 | 委托 WalletService 方法 | 转换 |
|---|---|---|
| `getBalance(userId)` | `walletService.getBalance(userId)` | 直接透传 BigDecimal |
| `listRecords(userId)` | `walletService.listRecords(userId)` | `List<WalletRecordVO>` → `List<WalletRecordDTO>`（`toDTOList`） |

### 9.3 RechargeCardApplicationService implements RechargeCardApi

委托 `RechargeCardService`：

| RechargeCardApi 方法 | 委托 RechargeCardService 方法 | 转换 |
|---|---|---|
| `generate(command)` | `rechargeCardService.generate(faceValue, count)` | Command 解包，`List<RechargeCardGenerateVO>` → `List<RechargeCardGenerateDTO>`（`toGenerateDTOList`） |
| `recharge(command)` | `rechargeCardService.recharge(cardNo, cardPassword, userId)` | Command 解包，`BigDecimal` → `RechargeResult`（`toRechargeResult`） |
| `listPage(query)` | `rechargeCardService.listPage(pageNum, pageSize, batchNo, status)` | Query 解包，`PageResult<RechargeCardVO>` → `PageResult<RechargeCardDTO>`（`toRechargeCardDTOList`） |
| `disable(command)` | `rechargeCardService.disable(id)` | Command 解包 |
| `getUsedCardsByUser(userId)` | `rechargeCardService.getUsedCardsByUser(userId)` | `List<RechargeCard>` → `List<RechargeCardDTO>`（`toRechargeCardDTOListFromEntity`） |

> **注意**：`getUsedCardsByUser` 旧方法返回 `List<RechargeCard>`（Entity），`RechargeCardApplicationService` 委托后需将 Entity 列表转为 DTO 列表。转换器需新增 `toRechargeCardDTOListFromEntity(List<RechargeCard> entities)` 方法。

---

## 10. 异常契约

PaymentApi / WalletApi / RechargeCardApi 方法在业务异常时抛出 `BusinessException(ErrorCode)`，不泄露堆栈。主要错误码：

| ErrorCode | 触发场景 |
|---|---|
| `WALLET_BALANCE_NOT_ENOUGH` | 钱包余额不足 |
| `RECHARGE_CARD_NOT_FOUND` | 充值卡不存在 |
| `RECHARGE_CARD_USED` | 充值卡已使用 |
| `RECHARGE_CARD_DISABLED` | 充值卡已禁用 |
| `RECHARGE_CARD_PASSWORD_ERROR` | 卡密错误 |
| `PARAM_ERROR` | 面额非法、数量非法、状态非法等参数校验失败 |

---

## 11. 向后兼容承诺

1. **方法签名只增不改不删**：PaymentApi / WalletApi / RechargeCardApi 一经发布，不修改已有方法签名，不删除已有方法
2. **DTO 字段只增不删不改变类型**：WalletRecordDTO / RechargeCardDTO / RechargeCardGenerateDTO 字段不删除、不重命名、不改变类型
3. **Command 字段只增不删不改变类型**：同上
4. **Query 字段只增不删不改变类型**：RechargeCardQuery 同上
5. **Result 字段只增不删不改变类型**：RechargeResult 同上
6. **新增方法/字段保持向后兼容**：新增方法不影响已有调用方，新增字段默认 null 不影响已有逻辑

---

## 12. 引用关系

### 12.1 PaymentApi 被调用方

| 调用方 | 调用方法 | 用途 |
|---|---|---|
| `order.service.impl.OrderLifecycleServiceImpl` | `pay` | 普通订单支付扣款 |
| `seckill.service.impl.SeckillOrderServiceImpl` | `pay` | 秒杀订单支付扣款 |

### 12.2 WalletApi 被调用方

| 调用方 | 调用方法 | 用途 |
|---|---|---|
| `payment.interfaces.web.WalletController` | `getBalance` + `listRecords` | 钱包前端 REST 端点 |

### 12.3 RechargeCardApi 被调用方

| 调用方 | 调用方法 | 用途 |
|---|---|---|
| `payment.interfaces.web.WalletController` | `recharge` | 充值端点 |
| `payment.interfaces.web.AdminRechargeCardController` | `generate` + `listPage` + `disable` | 充值卡后台管理 REST 端点 |
| `payment.application.WalletApplicationService` | `getUsedCardsByUser` | 钱包交易记录查充值记录（过渡期保留旧 RechargeCardService 调用） |

### 12.4 PaymentApi 依赖的契约

| 依赖 | 用途 |
|---|---|
| `identity.api.UserApi` | 钱包扣款（deductBalance） |

### 12.5 WalletApi 依赖的契约

| 依赖 | 用途 |
|---|---|
| `identity.api.UserApi` | 查余额（getUserById） |
| `order.api.OrderQueryApi` | 查普通订单钱包支付记录（getWalletPaidOrders） |
| `seckill`（SeckillOrderService，过渡期保留） | 查秒杀订单钱包支付记录（getWalletPaidOrdersByUser） |
| `payment.api.RechargeCardApi`（过渡期保留旧 RechargeCardService） | 查充值卡使用记录（getUsedCardsByUser） |

### 12.6 RechargeCardApi 依赖的契约

| 依赖 | 用途 |
|---|---|
| `identity.api.UserApi` | 充值后加余额（addBalance + getUserById） |