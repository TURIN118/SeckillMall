# Coupon 模块 API 契约 (COUPON-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase CP.2 - Coupon API 契约层定义
> 依据：COUPON-MIGRATION-PLAN.md + CartApi 契约风格 + ProductApi 契约风格
> 说明：本文件定义 Coupon 模块对外暴露的 API 契约，一经发布只增不改不删。

---

## 1. CouponApi 接口

**全限定名**：`com.seckill.mall.coupon.api.CouponApi`

**实现类**：`com.seckill.mall.coupon.application.CouponApplicationService`

**职责**：优惠券后台管理（CRUD/启停/发放/领取记录）+ 前台领取与查询。

**设计原则**：
- 方法用业务语言命名，禁止 CRUD 命名
- 入参用 Command/Query 对象，禁止裸露多参数
- 出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO
- 异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈
- 向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

```java
package com.seckill.mall.coupon.api;

public interface CouponApi {

    // ==================== 前台 ====================

    /** 查询当前可领取的优惠券列表（启用、在有效期内、有剩余；支持按商品筛选） */
    List<CouponDTO> listAvailableCoupons(Long productId);

    /** 用户领取优惠券（校验启用/有效期/库存/重复领取） */
    void claimCoupon(ClaimCouponCommand command);

    /** 查询我的优惠券列表（可按状态筛选） */
    List<UserCouponDTO> listMyCoupons(UserCouponQuery query);

    // ==================== 后台管理 ====================

    /** 分页查询优惠券列表（后台） */
    PageResult<CouponDTO> adminListCoupons(CouponQuery query);

    /** 创建优惠券 */
    CouponDTO createCoupon(CreateCouponCommand command);

    /** 编辑优惠券 */
    CouponDTO updateCoupon(UpdateCouponCommand command);

    /** 删除优惠券（逻辑删除） */
    void deleteCoupon(Long id);

    /** 启用/停用优惠券 */
    void updateCouponStatus(UpdateCouponStatusCommand command);

    /** 后台发放优惠券给指定用户，返回被发放用户的用户名 */
    String distributeCoupon(DistributeCouponCommand command);

    /** 后台分页查询某优惠券的领取记录 */
    PageResult<UserCouponDTO> adminListRecords(Long couponId, CouponQuery query);
}
```

---

## 2. CouponUsageApi 接口

**全限定名**：`com.seckill.mall.coupon.api.CouponUsageApi`

**实现类**：`com.seckill.mall.coupon.application.CouponUsageApplicationService`

**职责**：优惠券在订单流程中的核销能力（计价 / 核销 / 回退），供 order 模块调用。

```java
package com.seckill.mall.coupon.api;

public interface CouponUsageApi {

    /**
     * 计算优惠金额（按 scope 筛选适用商品小计）。
     * 校验用户优惠券归属、状态（UNUSED）、有效期，按券类型计算优惠金额。
     * 不可用时返回 CouponCalcResult.discount = ZERO。
     */
    CouponCalcResult calculateDiscount(UseCouponCommand command);

    /**
     * 核销优惠券（支付成功时调用）。
     * 校验归属与状态（UNUSED），更新为 USED，记录使用时间与订单ID，Coupon.usedCount 原子 +1。
     */
    void useCoupon(UseCouponCommand command);

    /**
     * 回退优惠券（取消订单 / 退款时调用）。
     * 校验归属与状态（USED），更新回 UNUSED，清除使用时间与订单ID，Coupon.usedCount 原子 -1。
     * 非 USED 状态幂等返回。
     */
    void revertCoupon(RevertCouponCommand command);
}
```

> **注意**：`UseCouponCommand` 同时用于 `calculateDiscount` 和 `useCoupon`。`calculateDiscount` 仅使用 `userCouponId/userId/orderAmount/productIds` 字段（`orderId` 可空）；`useCoupon` 使用 `userCouponId/userId/orderId` 字段（`orderAmount/productIds` 可空）。复用同一 Command 减少类数量，字段语义清晰。

---

## 3. DTO 定义

### 3.1 CouponDTO

**全限定名**：`com.seckill.mall.coupon.api.dto.CouponDTO`

**用途**：替代 `Coupon` Entity 跨模块传递；替代 `CouponVO` 作为 Application 层返回值。

**来源映射**：`Coupon` Entity → `CouponDTO`（由 `CouponApiConverter.toDTO` 转换）；`CouponVO` → `CouponDTO`（由 `CouponApiConverter.toDTOFromVO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 优惠券主键 ID | `Coupon.id` |
| `name` | `String` | 优惠券名称 | `Coupon.name` |
| `type` | `String` | 类型：AMOUNT/DISCOUNT | `Coupon.type.getCode()` |
| `amount` | `BigDecimal` | 满减金额或折扣值 | `Coupon.amount` |
| `minAmount` | `BigDecimal` | 最低消费金额 | `Coupon.minAmount` |
| `totalCount` | `Integer` | 发放总数 | `Coupon.totalCount` |
| `receivedCount` | `Integer` | 已领取数 | `Coupon.receivedCount` |
| `usedCount` | `Integer` | 已使用数 | `Coupon.usedCount` |
| `remainCount` | `Integer` | 剩余可领取数 | `totalCount - receivedCount` |
| `startTime` | `LocalDateTime` | 有效期开始 | `Coupon.startTime` |
| `endTime` | `LocalDateTime` | 有效期结束 | `Coupon.endTime` |
| `status` | `Integer` | 状态：1-启用 / 0-停用 | `Coupon.status` |
| `scopeType` | `String` | 适用范围：ALL/CATEGORY/PRODUCT | `Coupon.scopeType` |
| `categoryId` | `Long` | 适用分类ID | `Coupon.categoryId` |
| `productId` | `Long` | 适用商品ID | `Coupon.productId` |
| `scopeLabel` | `String` | 适用范围描述（如"仅限手机数码"） | 计算字段（buildScopeLabel） |
| `createTime` | `LocalDateTime` | 创建时间 | `Coupon.createTime` |
| `updateTime` | `LocalDateTime` | 更新时间 | `Coupon.updateTime` |

```java
package com.seckill.mall.coupon.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {
    private Long id;
    private String name;
    private String type;
    private BigDecimal amount;
    private BigDecimal minAmount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    private Integer remainCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String scopeType;
    private Long categoryId;
    private Long productId;
    private String scopeLabel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 3.2 UserCouponDTO

**全限定名**：`com.seckill.mall.coupon.api.dto.UserCouponDTO`

**用途**：替代 `UserCoupon` Entity 跨模块传递；替代 `UserCouponVO` / `AdminCouponRecordVO` 作为 Application 层返回值。

**来源映射**：`UserCoupon` + `Coupon` + `username` → `UserCouponDTO`（由 `CouponApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 用户优惠券主键 ID | `UserCoupon.id` |
| `userId` | `Long` | 用户 ID | `UserCoupon.userId` |
| `username` | `String` | 用户名（关联字段） | `UserApi.getUserById` |
| `couponId` | `Long` | 优惠券 ID | `UserCoupon.couponId` |
| `couponName` | `String` | 优惠券名称（关联字段） | `Coupon.name` |
| `couponType` | `String` | 优惠券类型（关联字段） | `Coupon.type.getCode()` |
| `couponAmount` | `BigDecimal` | 满减金额或折扣值（关联字段） | `Coupon.amount` |
| `minAmount` | `BigDecimal` | 最低消费金额（关联字段） | `Coupon.minAmount` |
| `couponStartTime` | `LocalDateTime` | 有效期开始（关联字段） | `Coupon.startTime` |
| `couponEndTime` | `LocalDateTime` | 有效期结束（关联字段） | `Coupon.endTime` |
| `status` | `String` | 状态：UNUSED/USED/EXPIRED | `UserCoupon.status.getCode()` |
| `receiveTime` | `LocalDateTime` | 领取时间 | `UserCoupon.receiveTime` |
| `useTime` | `LocalDateTime` | 使用时间 | `UserCoupon.useTime` |
| `orderId` | `Long` | 使用的订单 ID | `UserCoupon.orderId` |
| `createTime` | `LocalDateTime` | 创建时间 | `UserCoupon.createTime` |

```java
package com.seckill.mall.coupon.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long couponId;
    private String couponName;
    private String couponType;
    private BigDecimal couponAmount;
    private BigDecimal minAmount;
    private LocalDateTime couponStartTime;
    private LocalDateTime couponEndTime;
    private String status;
    private LocalDateTime receiveTime;
    private LocalDateTime useTime;
    private Long orderId;
    private LocalDateTime createTime;
}
```

### 3.3 CouponSnapshot

**全限定名**：`com.seckill.mall.coupon.api.dto.CouponSnapshot`

**用途**：优惠券只读快照，供跨模块计价/核销时传递优惠券核心字段（不含库存计数等可变状态）。

> **说明**：当前 `CouponUsageApi` 方法入参为 `UseCouponCommand`（含 `userCouponId`），ApplicationService 内部通过 `userCouponId` 查库获取 Coupon 信息，**不需要**调用方传入 `CouponSnapshot`。本类预留为未来优化（如缓存预加载场景），Phase CP.2 创建为空类（仅含 `id` 字段），不在本次迁移中被使用。

```java
package com.seckill.mall.coupon.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSnapshot {
    /** 优惠券主键 ID */
    private Long id;
}
```

---

## 4. Command 定义

### 4.1 CreateCouponCommand

**全限定名**：`com.seckill.mall.coupon.api.command.CreateCouponCommand`

**业务语义**：创建优惠券。

**原方法**：`CouponService.create(CouponCreateRequest req)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | `String` | 是 | 优惠券名称 |
| `type` | `String` | 是 | 类型：AMOUNT/DISCOUNT |
| `amount` | `BigDecimal` | 是 | 金额/折扣值 |
| `minAmount` | `BigDecimal` | 是 | 最低消费金额 |
| `totalCount` | `Integer` | 是 | 发放总数 |
| `startTime` | `LocalDateTime` | 是 | 有效期开始 |
| `endTime` | `LocalDateTime` | 是 | 有效期结束 |
| `status` | `Integer` | 否 | 状态（默认1） |
| `scopeType` | `String` | 否 | 适用范围（默认ALL） |
| `categoryId` | `Long` | 否 | 适用分类ID |
| `productId` | `Long` | 否 | 适用商品ID |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponCommand {
    private String name;
    private String type;
    private BigDecimal amount;
    private BigDecimal minAmount;
    private Integer totalCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String scopeType;
    private Long categoryId;
    private Long productId;
}
```

### 4.2 UpdateCouponCommand

**全限定名**：`com.seckill.mall.coupon.api.command.UpdateCouponCommand`

**业务语义**：编辑优惠券。

**原方法**：`CouponService.update(Long id, CouponCreateRequest req)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Long` | 是 | 优惠券 ID |
| `name` | `String` | 是 | 优惠券名称 |
| `type` | `String` | 是 | 类型 |
| `amount` | `BigDecimal` | 是 | 金额/折扣值 |
| `minAmount` | `BigDecimal` | 是 | 最低消费金额 |
| `totalCount` | `Integer` | 是 | 发放总数 |
| `startTime` | `LocalDateTime` | 是 | 有效期开始 |
| `endTime` | `LocalDateTime` | 是 | 有效期结束 |
| `status` | `Integer` | 否 | 状态 |
| `scopeType` | `String` | 否 | 适用范围 |
| `categoryId` | `Long` | 否 | 适用分类ID |
| `productId` | `Long` | 否 | 适用商品ID |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponCommand {
    private Long id;
    private String name;
    private String type;
    private BigDecimal amount;
    private BigDecimal minAmount;
    private Integer totalCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String scopeType;
    private Long categoryId;
    private Long productId;
}
```

### 4.3 UpdateCouponStatusCommand

**全限定名**：`com.seckill.mall.coupon.api.command.UpdateCouponStatusCommand`

**业务语义**：启用/停用优惠券。

**原方法**：`CouponService.updateStatus(Long id, Integer status)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | `Long` | 是 | 优惠券 ID |
| `status` | `Integer` | 是 | 状态：1-启用 / 0-停用 |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponStatusCommand {
    private Long id;
    private Integer status;
}
```

### 4.4 DistributeCouponCommand

**全限定名**：`com.seckill.mall.coupon.api.command.DistributeCouponCommand`

**业务语义**：后台发放优惠券给指定用户。

**原方法**：`CouponService.distribute(Long couponId, Long userId)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `couponId` | `Long` | 是 | 优惠券 ID |
| `userId` | `Long` | 是 | 用户 ID |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributeCouponCommand {
    private Long couponId;
    private Long userId;
}
```

### 4.5 ClaimCouponCommand

**全限定名**：`com.seckill.mall.coupon.api.command.ClaimCouponCommand`

**业务语义**：用户领取优惠券。

**原方法**：`CouponService.receive(Long couponId, Long userId)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `couponId` | `Long` | 是 | 优惠券 ID |
| `userId` | `Long` | 是 | 用户 ID |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCouponCommand {
    private Long couponId;
    private Long userId;
}
```

### 4.6 UseCouponCommand

**全限定名**：`com.seckill.mall.coupon.api.command.UseCouponCommand`

**业务语义**：核销优惠券（支付成功时调用）或计算优惠金额（订单创建时调用）。

**原方法**：
- `CouponUsageService.calculateDiscount(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds)`
- `CouponUsageService.useCoupon(Long userCouponId, Long userId, Long orderId)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userCouponId` | `Long` | 是 | 用户优惠券 ID |
| `userId` | `Long` | 是 | 用户 ID（校验归属） |
| `orderId` | `Long` | useCoupon 必填 | 关联订单 ID（核销时使用，计价时可空） |
| `orderAmount` | `BigDecimal` | calculateDiscount 必填 | 适用商品小计（计价时使用，核销时可空） |
| `productIds` | `List<Long>` | 否 | 订单商品 ID 列表（预留，当前简化处理） |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseCouponCommand {
    private Long userCouponId;
    private Long userId;
    private Long orderId;
    private BigDecimal orderAmount;
    private List<Long> productIds;
}
```

### 4.7 RevertCouponCommand

**全限定名**：`com.seckill.mall.coupon.api.command.RevertCouponCommand`

**业务语义**：回退优惠券（取消订单 / 退款时调用）。

**原方法**：`CouponUsageService.revertCoupon(Long userCouponId, Long userId)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userCouponId` | `Long` | 是 | 用户优惠券 ID |
| `userId` | `Long` | 是 | 用户 ID（校验归属） |

```java
package com.seckill.mall.coupon.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevertCouponCommand {
    private Long userCouponId;
    private Long userId;
}
```

---

## 5. Query 定义

### 5.1 CouponQuery

**全限定名**：`com.seckill.mall.coupon.api.query.CouponQuery`

**用途**：优惠券分页查询条件（后台 listPage + listRecords 共用）。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNum` | `Integer` | 否 | 页码（默认1） |
| `pageSize` | `Integer` | 否 | 每页大小（默认10） |
| `name` | `String` | 否 | 名称模糊筛选 |
| `status` | `Integer` | 否 | 状态筛选 |

```java
package com.seckill.mall.coupon.api.query;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponQuery {
    private Integer pageNum;
    private Integer pageSize;
    private String name;
    private Integer status;
}
```

### 5.2 UserCouponQuery

**全限定名**：`com.seckill.mall.coupon.api.query.UserCouponQuery`

**用途**：用户优惠券查询条件（前台 listMine）。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Long` | 是 | 用户 ID |
| `status` | `String` | 否 | 状态筛选：UNUSED/USED/EXPIRED |

```java
package com.seckill.mall.coupon.api.query;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponQuery {
    private Long userId;
    private String status;
}
```

---

## 6. Result 定义

### 6.1 CouponCalcResult

**全限定名**：`com.seckill.mall.coupon.api.result.CouponCalcResult`

**用途**：优惠金额计算结果，`CouponUsageApi.calculateDiscount` 返回值。

| 字段 | 类型 | 说明 |
|---|---|---|
| `discount` | `BigDecimal` | 优惠金额（非负），不可用时为 `BigDecimal.ZERO` |

```java
package com.seckill.mall.coupon.api.result;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCalcResult {
    /** 优惠金额（非负），不可用时为 ZERO */
    private BigDecimal discount;
}
```

> **OrderServiceImpl 适配**：`BigDecimal discount = couponUsageApi.calculateDiscount(command).getDiscount();`

---

## 7. 转换器（CouponApiConverter）

**全限定名**：`com.seckill.mall.coupon.application.facade.CouponApiConverter`

**职责**：集中存放旧 VO/Entity 与新 API 层 DTO/Command 之间的转换方法，供 `CouponApplicationService` / `CouponUsageApplicationService` 调用。所有方法均为无状态静态方法。

### 7.1 转换方法清单

| 方法 | 签名 | 用途 |
|---|---|---|
| `toDTO` | `CouponDTO toDTO(Coupon entity)` | Coupon Entity → CouponDTO |
| `toDTOList` | `List<CouponDTO> toDTOList(List<Coupon> entities)` | Coupon Entity 列表 → CouponDTO 列表 |
| `toDTOFromVO` | `CouponDTO toDTOFromVO(CouponVO vo)` | CouponVO → CouponDTO |
| `toDTOListFromVO` | `List<CouponDTO> toDTOListFromVO(List<CouponVO> voList)` | CouponVO 列表 → CouponDTO 列表 |
| `toVO` | `CouponVO toVO(CouponDTO dto)` | CouponDTO → CouponVO（Controller 层前端契约适配） |
| `toUserCouponDTO` | `UserCouponDTO toUserCouponDTO(UserCoupon entity, Coupon coupon, String username)` | UserCoupon + Coupon + username → UserCouponDTO |
| `toUserCouponDTOFromVO` | `UserCouponDTO toUserCouponDTOFromVO(UserCouponVO vo)` | UserCouponVO → UserCouponDTO |
| `toUserCouponVO` | `UserCouponVO toUserCouponVO(UserCouponDTO dto)` | UserCouponDTO → UserCouponVO |
| `toAdminRecordVO` | `AdminCouponRecordVO toAdminRecordVO(UserCouponDTO dto)` | UserCouponDTO → AdminCouponRecordVO |
| `toCreateCommand` | `CreateCouponCommand toCreateCommand(CouponCreateRequest req)` | 请求 DTO → Command |
| `toUpdateCommand` | `UpdateCouponCommand toUpdateCommand(Long id, CouponCreateRequest req)` | 请求 DTO → Command |
| `toCouponQuery` | `CouponQuery toCouponQuery(Integer pageNum, Integer pageSize, String name, Integer status)` | 裸参数 → Query |
| `toUseCouponCommand` | `UseCouponCommand toUseCouponCommand(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds)` | 裸参数 → Command（计价） |
| `toUseCouponCommandForUse` | `UseCouponCommand toUseCouponCommandForUse(Long userCouponId, Long userId, Long orderId)` | 裸参数 → Command（核销） |
| `toRevertCouponCommand` | `RevertCouponCommand toRevertCouponCommand(Long userCouponId, Long userId)` | 裸参数 → Command |

### 7.2 转换规则

- **Entity → DTO**：`type` 字段 `CouponType` → `String`（`getCode()`），`status` 字段 `UserCouponStatus` → `String`（`getCode()`），丢弃 `isDeleted` 等基础设施字段
- **VO → DTO**：全字段映射（核心 + 展示），保留前端展示信息
- **DTO → VO**：全字段映射（核心 + 展示），保留前端契约
- **Request → Command**：字段一一对应，丢弃 Bean Validation 注解（Command 是纯 POJO）

---

## 8. ApplicationService 委托映射

### 8.1 CouponApplicationService implements CouponApi

委托 `CouponService`：

| CouponApi 方法 | 委托 CouponService 方法 | 转换 |
|---|---|---|
| `listAvailableCoupons(productId)` | `couponService.listAvailable(productId)` | `List<CouponVO>` → `List<CouponDTO>`（`toDTOListFromVO`） |
| `claimCoupon(command)` | `couponService.receive(couponId, userId)` | Command 解包 |
| `listMyCoupons(query)` | `couponService.listMine(userId, status)` | `List<UserCouponVO>` → `List<UserCouponDTO>`（`toUserCouponDTOFromVO`） |
| `adminListCoupons(query)` | `couponService.listPage(pageNum, pageSize, name, status)` | `PageResult<CouponVO>` → `PageResult<CouponDTO>`（`toDTOListFromVO`） |
| `createCoupon(command)` | `couponService.create(req)` | Command → Request（`toCouponCreateRequest`），`CouponVO` → `CouponDTO`（`toDTOFromVO`） |
| `updateCoupon(command)` | `couponService.update(id, req)` | 同上 |
| `deleteCoupon(id)` | `couponService.delete(id)` | 直接委托 |
| `updateCouponStatus(command)` | `couponService.updateStatus(id, status)` | Command 解包 |
| `distributeCoupon(command)` | `couponService.distribute(couponId, userId)` | Command 解包，返回 username 直接透传 |
| `adminListRecords(couponId, query)` | `couponService.listRecords(couponId, pageNum, pageSize)` | `PageResult<AdminCouponRecordVO>` → `PageResult<UserCouponDTO>` |

### 8.2 CouponUsageApplicationService implements CouponUsageApi

委托 `CouponUsageService`：

| CouponUsageApi 方法 | 委托 CouponUsageService 方法 | 转换 |
|---|---|---|
| `calculateDiscount(command)` | `couponUsageService.calculateDiscount(userCouponId, userId, orderAmount, productIds)` | Command 解包，`BigDecimal` → `CouponCalcResult`（包装） |
| `useCoupon(command)` | `couponUsageService.useCoupon(userCouponId, userId, orderId)` | Command 解包 |
| `revertCoupon(command)` | `couponUsageService.revertCoupon(userCouponId, userId)` | Command 解包 |

---

## 9. 异常契约

CouponApi / CouponUsageApi 方法在业务异常时抛出 `BusinessException(ErrorCode)`，不泄露堆栈。主要错误码：

| ErrorCode | 触发场景 |
|---|---|
| `COUPON_NOT_FOUND` | 优惠券不存在 |
| `COUPON_DISABLED` | 优惠券已停用 |
| `COUPON_EXPIRED` | 优惠券已过期 |
| `COUPON_NOT_STARTED` | 优惠券尚未开始 |
| `COUPON_OUT_OF_STOCK` | 优惠券库存不足 |
| `COUPON_ALREADY_RECEIVED` | 用户已领取过该优惠券 |
| `PARAM_ERROR` | 状态值非法、折扣值非法、时间非法、核销失败等 |
| `USER_NOT_FOUND` | 发放时用户不存在 |

---

## 10. 向后兼容承诺

1. **方法签名只增不改不删**：CouponApi / CouponUsageApi 一经发布，不修改已有方法签名，不删除已有方法
2. **DTO 字段只增不删不改变类型**：CouponDTO / UserCouponDTO / CouponSnapshot 字段不删除、不重命名、不改变类型
3. **Command 字段只增不删不改变类型**：同上
4. **Query 字段只增不删不改变类型**：同上
5. **Result 字段只增不删不改变类型**：CouponCalcResult 同上
6. **新增方法/字段保持向后兼容**：新增方法不影响已有调用方，新增字段默认 null 不影响已有逻辑

---

## 11. 引用关系

### 11.1 CouponApi 被调用方

| 调用方 | 调用方法 | 用途 |
|---|---|---|
| `coupon.interfaces.web.CouponController` | `listAvailableCoupons` + `claimCoupon` + `listMyCoupons` | 前端 REST 端点 |
| `coupon.interfaces.web.AdminCouponController` | `adminListCoupons` + `createCoupon` + `updateCoupon` + `deleteCoupon` + `updateCouponStatus` + `distributeCoupon` + `adminListRecords` | 后台管理 REST 端点 |

### 11.2 CouponUsageApi 被调用方

| 调用方 | 调用方法 | 用途 |
|---|---|---|
| `order.service.impl.OrderServiceImpl` | `calculateDiscount` | 订单创建时计算优惠金额 |
| `order.service.impl.OrderLifecycleServiceImpl` | `useCoupon` + `revertCoupon` | 支付成功核销、取消/超时回退 |

### 11.3 CouponApi 依赖的契约

| 依赖 | 用途 |
|---|---|
| `product.api.ProductApi` | listAvailable 按商品筛选、buildScopeLabel 查商品名 |
| `identity.api.UserApi` | distribute/listRecords/listMine 查用户名 |
| `category`（CategoryService，过渡期保留） | buildScopeLabel 查分类名 |

### 11.4 CouponUsageApi 依赖的契约

| 依赖 | 用途 |
|---|---|
| 无 | CouponUsageService 仅访问 coupon 模块内部 Mapper，不依赖其他模块 |