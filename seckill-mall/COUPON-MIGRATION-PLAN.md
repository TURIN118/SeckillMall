# Coupon 模块迁移计划 (COUPON-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase CP - Coupon 模块迁移（Strangler Pattern 第五阶段）
> 依据：CART-MIGRATION-PLAN.md + PRODUCT-MIGRATION-PLAN.md + ORDER-MIGRATION-PLAN.md + Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Coupon 当前结构

### 1.1 当前包结构

Coupon 相关代码散落在 6 个不同的顶层包中，共 **15 个核心文件**：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `CouponController` | `controller` | 优惠券前台：可领取列表/领取/我的优惠券（/api/v1/coupons） |
| `AdminCouponController` | `controller` | 优惠券后台：CRUD/启停/发放/领取记录（/api/v1/admin/coupons） |
| `CouponService` | `service` | 优惠券管理+领取+查询业务接口 |
| `CouponServiceImpl` | `service.impl` | CouponService 实现 |
| `CouponUsageService` | `service` | 优惠券核销业务接口（计价/核销/回退） |
| `CouponUsageServiceImpl` | `service.impl` | CouponUsageService 实现 |
| `Coupon` | `entity` | 优惠券 PO（t_coupon） |
| `UserCoupon` | `entity` | 用户优惠券 PO（t_user_coupon） |
| `CouponMapper` | `mapper` | 优惠券 Mapper |
| `UserCouponMapper` | `mapper` | 用户优惠券 Mapper |
| `CouponType` | `entity.enums` | 优惠券类型枚举（AMOUNT/DISCOUNT） |
| `UserCouponStatus` | `entity.enums` | 用户优惠券状态枚举（UNUSED/USED/EXPIRED） |
| `CouponVO` | `vo` | 优惠券视图（后台管理+前台展示） |
| `UserCouponVO` | `vo` | 用户优惠券视图（含关联券信息） |
| `AdminCouponRecordVO` | `vo` | 后台领取记录视图（含用户名/券名） |
| `CouponCreateRequest` | `dto` | 优惠券创建/更新请求 |

### 1.2 Service 方法签名

#### CouponService（10 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `listPage` | `PageResult<CouponVO> listPage(Integer pageNum, Integer pageSize, String name, Integer status)` | 后台分页查询 | ⚠️ 裸参数（4 个） |
| `create` | `CouponVO create(CouponCreateRequest req)` | 创建优惠券 | ⚠️ 返回 VO |
| `update` | `CouponVO update(Long id, CouponCreateRequest req)` | 编辑优惠券 | ⚠️ 返回 VO |
| `delete` | `void delete(Long id)` | 删除优惠券（逻辑删除） | ✅ |
| `updateStatus` | `void updateStatus(Long id, Integer status)` | 启用/停用 | ⚠️ 裸参数 |
| `distribute` | `String distribute(Long couponId, Long userId)` | 后台发放给指定用户 | ⚠️ 裸参数，返回用户名 |
| `listRecords` | `PageResult<AdminCouponRecordVO> listRecords(Long couponId, Integer pageNum, Integer pageSize)` | 后台领取记录分页 | ⚠️ 裸参数（3 个） |
| `listAvailable` | `List<CouponVO> listAvailable(Long productId)` | 可领取列表（支持按商品筛选） | ⚠️ 返回 VO |
| `receive` | `void receive(Long couponId, Long userId)` | 用户领取优惠券 | ⚠️ 裸参数 |
| `listMine` | `List<UserCouponVO> listMine(Long userId, String status)` | 我的优惠券 | ⚠️ 返回 VO |

#### CouponUsageService（3 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `calculateDiscount` | `BigDecimal calculateDiscount(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds)` | 计算优惠金额 | ⚠️ 裸参数（4 个），被 OrderServiceImpl 跨模块调用 |
| `useCoupon` | `void useCoupon(Long userCouponId, Long userId, Long orderId)` | 核销优惠券（支付成功） | ⚠️ 裸参数，被 OrderLifecycleServiceImpl 跨模块调用 |
| `revertCoupon` | `void revertCoupon(Long userCouponId, Long userId)` | 回退优惠券（取消/退款） | ⚠️ 裸参数，被 OrderLifecycleServiceImpl 跨模块调用 |

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `CouponController` | `/api/v1/coupons` | `GET /available` | 可领取列表（支持按商品筛选） | BUYER/ADMIN |
| | | `POST /{id}/receive` | 领取优惠券 | BUYER/ADMIN |
| | | `GET /mine` | 我的优惠券（可按状态筛选） | BUYER/ADMIN |
| `AdminCouponController` | `/api/v1/admin/coupons` | `GET /list` | 分页查询优惠券列表 | ADMIN |
| | | `POST /create` | 创建优惠券 | ADMIN |
| | | `PUT /{id}/update` | 编辑优惠券 | ADMIN |
| | | `DELETE /{id}` | 删除优惠券 | ADMIN |
| | | `PUT /{id}/status` | 启用/停用 | ADMIN |
| | | `POST /{id}/distribute` | 发放给指定用户 | ADMIN |
| | | `GET /{id}/records` | 分页查询领取记录 | ADMIN |

### 1.4 跨模块依赖关系

#### Coupon 依赖的模块（出向依赖）

| 被依赖模块 | 依赖方式 | 用途 |
|---|---|---|
| **product** | `ProductApi` | listAvailable 按商品筛选、buildScopeLabel 查商品名 |
| **identity** | `UserApi` | distribute/listRecords/listMine 查用户名 |
| **category** | `CategoryService` | buildScopeLabel 查分类名（scopeType=CATEGORY） |

#### 依赖 Coupon 的模块（入向依赖）

| 调用方模块 | 依赖方式 | 用途 |
|---|---|---|
| **order** | `CouponUsageService.calculateDiscount` | `OrderServiceImpl.applyCouponToOrder` 订单创建时计算优惠金额 |
| **order** | `CouponUsageService.useCoupon` | `OrderLifecycleServiceImpl.payNormal` 支付成功时核销优惠券 |
| **order** | `CouponUsageService.revertCoupon` | `OrderLifecycleServiceImpl.timeoutCancelNormalOrder` + `cancelNormal` 取消/超时回退优惠券 |

### 1.5 Entity / VO / DTO 字段

#### Coupon Entity（t_coupon）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `name` | `String` | 优惠券名称 |
| `type` | `CouponType` | 类型：AMOUNT-满减 / DISCOUNT-折扣 |
| `amount` | `BigDecimal` | 满减金额或折扣值（如 0.85 表示85折） |
| `minAmount` | `BigDecimal` | 最低消费金额 |
| `totalCount` | `Integer` | 发放总数 |
| `receivedCount` | `Integer` | 已领取数 |
| `usedCount` | `Integer` | 已使用数 |
| `startTime` | `LocalDateTime` | 有效期开始 |
| `endTime` | `LocalDateTime` | 有效期结束 |
| `status` | `Integer` | 状态：1-启用 / 0-停用 |
| `scopeType` | `String` | 适用范围：ALL/CATEGORY/PRODUCT |
| `categoryId` | `Long` | 适用分类ID（scopeType=CATEGORY） |
| `productId` | `Long` | 适用商品ID（scopeType=PRODUCT） |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### UserCoupon Entity（t_user_coupon）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `userId` | `Long` | 用户ID |
| `couponId` | `Long` | 优惠券ID |
| `status` | `UserCouponStatus` | 状态：UNUSED/USED/EXPIRED |
| `receiveTime` | `LocalDateTime` | 领取时间 |
| `useTime` | `LocalDateTime` | 使用时间 |
| `orderId` | `Long` | 使用的订单ID |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### CouponVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键ID |
| `name` | `String` | 优惠券名称 |
| `type` | `String` | 类型：AMOUNT/DISCOUNT |
| `amount` | `BigDecimal` | 满减金额或折扣值 |
| `minAmount` | `BigDecimal` | 最低消费金额 |
| `totalCount` | `Integer` | 发放总数 |
| `receivedCount` | `Integer` | 已领取数 |
| `usedCount` | `Integer` | 已使用数 |
| `remainCount` | `Integer` | 剩余可领取数 |
| `startTime` | `LocalDateTime` | 有效期开始 |
| `endTime` | `LocalDateTime` | 有效期结束 |
| `status` | `Integer` | 状态：1-启用 / 0-停用 |
| `scopeType` | `String` | 适用范围 |
| `categoryId` | `Long` | 适用分类ID |
| `productId` | `Long` | 适用商品ID |
| `scopeLabel` | `String` | 适用范围描述（如"仅限手机数码"） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### UserCouponVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 用户优惠券ID |
| `userId` | `Long` | 用户ID |
| `username` | `String` | 用户名（关联字段） |
| `couponId` | `Long` | 优惠券ID |
| `status` | `String` | 状态：UNUSED/USED/EXPIRED |
| `receiveTime` | `LocalDateTime` | 领取时间 |
| `useTime` | `LocalDateTime` | 使用时间 |
| `orderId` | `Long` | 使用的订单ID |
| `couponName` | `String` | 优惠券名称（关联字段） |
| `couponType` | `String` | 优惠券类型（关联字段） |
| `couponAmount` | `BigDecimal` | 满减金额或折扣值（关联字段） |
| `minAmount` | `BigDecimal` | 最低消费金额（关联字段） |
| `couponStartTime` | `LocalDateTime` | 有效期开始（关联字段） |
| `couponEndTime` | `LocalDateTime` | 有效期结束（关联字段） |
| `createTime` | `LocalDateTime` | 创建时间 |

#### AdminCouponRecordVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 领取记录主键ID |
| `userId` | `Long` | 用户ID |
| `username` | `String` | 用户名（关联字段） |
| `couponId` | `Long` | 优惠券ID |
| `couponName` | `String` | 优惠券名称（关联字段） |
| `status` | `String` | 状态：UNUSED/USED/EXPIRED |
| `receiveTime` | `LocalDateTime` | 领取时间 |
| `useTime` | `LocalDateTime` | 使用时间 |
| `orderId` | `Long` | 使用的订单ID |
| `createTime` | `LocalDateTime` | 创建时间 |

#### CouponCreateRequest（保留在 dto 包，Controller 层请求 DTO）

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `name` | `String` | `@NotBlank` + `@Size(max=100)` | 优惠券名称 |
| `type` | `String` | `@NotBlank` + `@Pattern(AMOUNT\|DISCOUNT)` | 类型 |
| `amount` | `BigDecimal` | `@NotNull` + `@DecimalMin(0)` | 金额/折扣值 |
| `minAmount` | `BigDecimal` | `@NotNull` + `@DecimalMin(0)` | 最低消费金额 |
| `totalCount` | `Integer` | `@NotNull` + `@Positive` | 发放总数 |
| `startTime` | `LocalDateTime` | `@NotNull` | 有效期开始 |
| `endTime` | `LocalDateTime` | `@NotNull` | 有效期结束 |
| `status` | `Integer` | - | 状态（可选，默认1） |
| `scopeType` | `String` | - | 适用范围（可选，默认ALL） |
| `categoryId` | `Long` | - | 适用分类ID |
| `productId` | `Long` | - | 适用商品ID |

### 1.6 Mapper 自定义方法

| Mapper | 自定义方法 | 说明 |
|---|---|---|
| `CouponMapper` | 无 | 仅继承 `BaseMapper<Coupon>`，复杂查询使用 Lambda Wrapper |
| `UserCouponMapper` | 无 | 仅继承 `BaseMapper<UserCoupon>`，复杂查询使用 Lambda Wrapper |

---

## 2. Coupon 目标结构（Pragmatic DDD + Modular Monolith）

### 2.1 目标包结构

```
com.seckill.mall.coupon/
├── package-info.java                                  # 模块根包
├── api/                                                # API 契约层（对外暴露）
│   ├── package-info.java
│   ├── CouponApi.java                                 # 优惠券管理 API（领取、查询、后台管理）
│   ├── CouponUsageApi.java                            # 优惠券核销 API（计价、核销、回退）
│   ├── dto/
│   │   ├── package-info.java
│   │   ├── CouponDTO.java                             # 优惠券 DTO（替代 CouponVO 跨模块传递）
│   │   ├── UserCouponDTO.java                         # 用户优惠券 DTO（替代 UserCouponVO 跨模块传递）
│   │   └── CouponSnapshot.java                        # 优惠券快照（核销计算只读快照）
│   ├── command/
│   │   ├── package-info.java
│   │   ├── CreateCouponCommand.java                   # 创建优惠券命令
│   │   ├── UpdateCouponCommand.java                   # 编辑优惠券命令
│   │   ├── UpdateCouponStatusCommand.java             # 启停命令
│   │   ├── DistributeCouponCommand.java               # 发放命令
│   │   ├── ClaimCouponCommand.java                    # 领取命令
│   │   ├── UseCouponCommand.java                      # 核销命令
│   │   └── RevertCouponCommand.java                   # 回退命令
│   ├── query/
│   │   ├── package-info.java
│   │   ├── CouponQuery.java                           # 优惠券分页查询
│   │   └── UserCouponQuery.java                       # 用户优惠券查询
│   └── result/
│       ├── package-info.java
│       └── CouponCalcResult.java                      # 优惠计算结果
├── application/                                        # 应用层
│   ├── package-info.java
│   ├── CouponApplicationService.java                   # 实现 CouponApi，委托 CouponService
│   ├── CouponUsageApplicationService.java              # 实现 CouponUsageApi，委托 CouponUsageService
│   └── facade/
│       ├── package-info.java
│       └── CouponApiConverter.java                     # VO/Entity ↔ DTO/Command 转换
├── domain/                                             # 领域层
│   ├── package-info.java
│   ├── CouponType.java                                 # 从 entity.enums 迁入
│   └── UserCouponStatus.java                           # 从 entity.enums 迁入
├── infrastructure/                                     # 基础设施层
│   ├── package-info.java
│   ├── mapper/
│   │   ├── package-info.java
│   │   ├── CouponMapper.java                           # 从 mapper 包迁入
│   │   └── UserCouponMapper.java                       # 从 mapper 包迁入
│   └── entity/
│       ├── package-info.java
│       ├── Coupon.java                                 # 从 entity 包迁入
│       └── UserCoupon.java                             # 从 entity 包迁入
└── interfaces/                                         # 接口层
    ├── package-info.java
    ├── vo/
    │   ├── package-info.java
    │   ├── CouponVO.java                               # 从 vo 包迁入（前端展示用）
    │   ├── UserCouponVO.java                           # 从 vo 包迁入
    │   └── AdminCouponRecordVO.java                    # 从 vo 包迁入
    └── web/
        ├── package-info.java
        ├── CouponController.java                       # 从 controller 包迁入
        └── AdminCouponController.java                  # 从 controller 包迁入
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
| Coupon → Product | `ProductApi` | listAvailable 按商品筛选、buildScopeLabel 查商品名 |
| Coupon → Identity | `UserApi` | distribute/listRecords/listMine 查用户名 |
| Coupon → Category | `CategoryService` | buildScopeLabel 查分类名（过渡期保留） |
| Order → Coupon | `CouponUsageApi`（替代 `CouponUsageService`） | 订单创建计价、支付核销、取消/超时回退 |

---

## 3. 文件迁移路径

### 3.1 移动文件（git mv）

| 源路径 | 目标路径 | 阶段 |
|---|---|---|
| `entity/Coupon.java` | `coupon/infrastructure/entity/Coupon.java` | Phase CP.3 |
| `entity/UserCoupon.java` | `coupon/infrastructure/entity/UserCoupon.java` | Phase CP.3 |
| `mapper/CouponMapper.java` | `coupon/infrastructure/mapper/CouponMapper.java` | Phase CP.3 |
| `mapper/UserCouponMapper.java` | `coupon/infrastructure/mapper/UserCouponMapper.java` | Phase CP.3 |
| `entity/enums/CouponType.java` | `coupon/domain/CouponType.java` | Phase CP.3 |
| `entity/enums/UserCouponStatus.java` | `coupon/domain/UserCouponStatus.java` | Phase CP.3 |
| `vo/CouponVO.java` | `coupon/interfaces/vo/CouponVO.java` | Phase CP.4 |
| `vo/UserCouponVO.java` | `coupon/interfaces/vo/UserCouponVO.java` | Phase CP.4 |
| `vo/AdminCouponRecordVO.java` | `coupon/interfaces/vo/AdminCouponRecordVO.java` | Phase CP.4 |
| `controller/CouponController.java` | `coupon/interfaces/web/CouponController.java` | Phase CP.4 |
| `controller/AdminCouponController.java` | `coupon/interfaces/web/AdminCouponController.java` | Phase CP.4 |

### 3.2 新增文件

| 目标路径 | 阶段 |
|---|---|
| `coupon/package-info.java` 及所有子包 `package-info.java` | Phase CP.0 |
| `coupon/api/CouponApi.java` | Phase CP.2 |
| `coupon/api/CouponUsageApi.java` | Phase CP.2 |
| `coupon/api/dto/{CouponDTO,UserCouponDTO,CouponSnapshot}.java` | Phase CP.2 |
| `coupon/api/command/{CreateCoupon,UpdateCoupon,UpdateCouponStatus,DistributeCoupon,ClaimCoupon,UseCoupon,RevertCoupon}Command.java` | Phase CP.2 |
| `coupon/api/query/{CouponQuery,UserCouponQuery}.java` | Phase CP.2 |
| `coupon/api/result/CouponCalcResult.java` | Phase CP.2 |
| `coupon/application/CouponApplicationService.java` | Phase CP.4 |
| `coupon/application/CouponUsageApplicationService.java` | Phase CP.4 |
| `coupon/application/facade/CouponApiConverter.java` | Phase CP.4 |

### 3.3 保留不动的文件

| 文件 | 原因 |
|---|---|
| `service/CouponService.java` | Strangler 过渡期保留，CouponApplicationService 委托它 |
| `service/impl/CouponServiceImpl.java` | 同上 |
| `service/CouponUsageService.java` | Strangler 过渡期保留，CouponUsageApplicationService 委托它 |
| `service/impl/CouponUsageServiceImpl.java` | 同上 |
| `dto/CouponCreateRequest.java` | Controller 层请求 DTO，含 Bean Validation 注解，保留在 dto 包 |

### 3.4 后续可删除的文件（Strangler 完成后）

| 文件 | 删除时机 |
|---|---|
| `service/CouponService.java` + `service/impl/CouponServiceImpl.java` | 业务逻辑完全迁入 coupon.application 后 |
| `service/CouponUsageService.java` + `service/impl/CouponUsageServiceImpl.java` | 同上 |

---

## 4. API 边界（CouponApi + CouponUsageApi）

### 4.1 设计原则

1. **方法用业务语言命名**，禁止 CRUD 命名
2. **入参用 Command/Query 对象**，禁止裸露多参数
3. **出参用 Result/DTO/Snapshot**，禁止暴露 Entity/Mapper/PO
4. **异常通过 `BusinessException(ErrorCode)` 抛出**，不泄露堆栈
5. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

### 4.2 CouponApi 方法清单（优惠券管理 API）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `listAvailableCoupons` | `List<CouponDTO> listAvailableCoupons(Long productId)` | `CouponService.listAvailable` |
| `claimCoupon` | `void claimCoupon(ClaimCouponCommand command)` | `CouponService.receive` |
| `listMyCoupons` | `List<UserCouponDTO> listMyCoupons(UserCouponQuery query)` | `CouponService.listMine` |
| `adminListCoupons` | `PageResult<CouponDTO> adminListCoupons(CouponQuery query)` | `CouponService.listPage` |
| `createCoupon` | `CouponDTO createCoupon(CreateCouponCommand command)` | `CouponService.create` |
| `updateCoupon` | `CouponDTO updateCoupon(UpdateCouponCommand command)` | `CouponService.update` |
| `deleteCoupon` | `void deleteCoupon(Long id)` | `CouponService.delete` |
| `updateCouponStatus` | `void updateCouponStatus(UpdateCouponStatusCommand command)` | `CouponService.updateStatus` |
| `distributeCoupon` | `String distributeCoupon(DistributeCouponCommand command)` | `CouponService.distribute` |
| `adminListRecords` | `PageResult<UserCouponDTO> adminListRecords(Long couponId, CouponQuery query)` | `CouponService.listRecords` |

### 4.3 CouponUsageApi 方法清单（优惠券核销 API）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `calculateDiscount` | `CouponCalcResult calculateDiscount(UseCouponCommand command)` | `CouponUsageService.calculateDiscount` |
| `useCoupon` | `void useCoupon(UseCouponCommand command)` | `CouponUsageService.useCoupon` |
| `revertCoupon` | `void revertCoupon(RevertCouponCommand command)` | `CouponUsageService.revertCoupon` |

> **关键变化**：
> - `CouponUsageService.calculateDiscount` 返回 `BigDecimal` → `CouponUsageApi.calculateDiscount` 返回 `CouponCalcResult`（含 discount + applicable 标识），消除裸 BigDecimal 返回。过渡期 `CouponCalcResult` 仅含 `discount` 字段，保持与旧逻辑等价。
> - 所有裸参数方法（`useCoupon(Long, Long, Long)` 等）改为 Command 对象入参。

### 4.4 DTO/Command/Query/Result 定义

详见 [COUPON-API-CONTRACT.md](COUPON-API-CONTRACT.md)。

---

## 5. 迁移步骤（Phase CP）

### Phase CP.0：创建包结构

- 创建 `coupon/` 及所有子包
- 每个包创建 `package-info.java`
- **不修改任何现有代码**

### Phase CP.2：创建 API 层

- 创建 `CouponApi` + `CouponUsageApi` 接口
- 创建 3 个 DTO（`CouponDTO`、`UserCouponDTO`、`CouponSnapshot`）
- 创建 7 个 Command（`CreateCoupon`、`UpdateCoupon`、`UpdateCouponStatus`、`DistributeCoupon`、`ClaimCoupon`、`UseCoupon`、`RevertCoupon`）
- 创建 2 个 Query（`CouponQuery`、`UserCouponQuery`）
- 创建 1 个 Result（`CouponCalcResult`）
- **不修改任何现有代码**

### Phase CP.3：迁移持久化层

- `git mv entity/Coupon.java coupon/infrastructure/entity/Coupon.java`
- `git mv entity/UserCoupon.java coupon/infrastructure/entity/UserCoupon.java`
- `git mv mapper/CouponMapper.java coupon/infrastructure/mapper/CouponMapper.java`
- `git mv mapper/UserCouponMapper.java coupon/infrastructure/mapper/UserCouponMapper.java`
- `git mv entity/enums/CouponType.java coupon/domain/CouponType.java`
- `git mv entity/enums/UserCouponStatus.java coupon/domain/UserCouponStatus.java`
- 更新 package 声明
- 更新所有 import（CouponServiceImpl、CouponUsageServiceImpl、其他引用 Coupon/UserCoupon/CouponType/UserCouponStatus 的类）
- **回归测试**：mvn test

### Phase CP.4：创建 Application 层 + 移动 Controller/VO

- 创建 `CouponApplicationService implements CouponApi`，委托 `CouponService`
- 创建 `CouponUsageApplicationService implements CouponUsageApi`，委托 `CouponUsageService`
- 创建 `CouponApiConverter`：VO ↔ DTO、Command → 裸参数 转换
- `git mv vo/CouponVO.java coupon/interfaces/vo/CouponVO.java`
- `git mv vo/UserCouponVO.java coupon/interfaces/vo/UserCouponVO.java`
- `git mv vo/AdminCouponRecordVO.java coupon/interfaces/vo/AdminCouponRecordVO.java`
- `git mv controller/CouponController.java coupon/interfaces/web/CouponController.java`
- `git mv controller/AdminCouponController.java coupon/interfaces/web/AdminCouponController.java`
- 更新 package 声明与所有 import
- 切换 Controller 注入 `CouponApi` 替代 `CouponService`，调用 API 方法后通过 Converter 转回 VO
- **不删除旧 CouponService / CouponUsageService**（Strangler 过渡）
- **回归测试**：mvn test

### Phase CP.5：迁移外部调用方

- `OrderServiceImpl`：`CouponUsageService` → `CouponUsageApi`
  - `couponUsageService.calculateDiscount(userCouponId, userId, orderAmount, productIds)` → `couponUsageApi.calculateDiscount(command)`（返回 `CouponCalcResult`，取 `discount` 字段）
- `OrderLifecycleServiceImpl`：`CouponUsageService` → `CouponUsageApi`
  - `couponUsageService.useCoupon(userCouponId, userId, orderId)` → `couponUsageApi.useCoupon(command)`
  - `couponUsageService.revertCoupon(userCouponId, userId)` → `couponUsageApi.revertCoupon(command)`
- 更新测试中的 mock 声明（`OrderServiceTest`、`OrderLifecycleServiceTest`）
- **回归测试**：mvn test

### Phase CP.6：ArchUnit 规则

新增 3 条 coupon 模块包边界规则：

| 规则 | 内容 |
|---|---|
| `coupon_api_should_not_depend_on_infrastructure` | `coupon.api` 不应依赖 `coupon.infrastructure` |
| `coupon_application_should_not_depend_on_mapper` | `coupon.application` 不应直接依赖 `coupon.infrastructure.mapper` |
| `coupon_interfaces_should_not_depend_on_mapper` | `coupon.interfaces` 不应直接依赖 `coupon.infrastructure.mapper` |

**回归标准**：mvn test Failures ≤ 1, Errors ≤ 19, ArchUnit 全绿（36 条 = 33 + 3）。

---

## 6. 风险点与缓解

### 6.1 OrderServiceImpl/OrderLifecycleServiceImpl 对 CouponUsageService 的强耦合

**风险**：`OrderServiceImpl.applyCouponToOrder` 直接调用 `couponUsageService.calculateDiscount` 获取 `BigDecimal`；`OrderLifecycleServiceImpl` 在支付成功/取消/超时三处调用 `useCoupon`/`revertCoupon`。切换到 `CouponUsageApi` 后返回 `CouponCalcResult`，需保证取 `discount` 字段后与旧逻辑等价。

**缓解**：`CouponCalcResult` 仅含 `discount` 字段（`BigDecimal`），`CouponUsageApplicationService.calculateDiscount` 委托旧 `CouponUsageService.calculateDiscount` 后包装为 `CouponCalcResult`。`OrderServiceImpl` 调用 `couponUsageApi.calculateDiscount(command).getDiscount()` 即与旧逻辑等价。详见 [COUPON-API-CONTRACT.md](COUPON-API-CONTRACT.md)。

### 6.2 CouponServiceImpl 对 CategoryService 的依赖

**风险**：`CouponServiceImpl.buildScopeLabel` 调用 `categoryService.getCategoryById` 查分类名，`CategoryService` 尚未迁移为 API。迁移后 `CouponApplicationService` 委托 `CouponService`，间接依赖 `CategoryService`，不违反 coupon 模块内部包边界规则（ArchUnit 仅约束 coupon.api/application/interfaces 不依赖 coupon.infrastructure.mapper）。

**缓解**：本次迁移**不动** `CategoryService` 引用，保留在 `CouponServiceImpl` 中。`CouponApplicationService` 仅做门面委托，不直接 import `CategoryService`。待 Category 模块迁移后再切换。

### 6.3 CouponVO/UserCouponVO/AdminCouponRecordVO 被前端接口契约锁定

**风险**：3 个 VO 是 `/api/v1/coupons/*` 和 `/api/v1/admin/coupons/*` 端点的返回体，前端依赖其字段。若切换为 DTO 可能破坏前端契约。

**缓解**：**Controller 仍返回 VO**（保留前端契约），内部调用 `CouponApi` 拿到 DTO 后通过 `CouponApiConverter.toVO` 转回 VO。3 个 VO 移极迁入 `coupon/interfaces/vo/` 但保留所有字段。

### 6.4 CouponCreateRequest 位置

**风险**：`CouponCreateRequest` 在 `dto/` 包，含 Bean Validation 注解（`@NotBlank`/`@Pattern` 等），被 `AdminCouponController` 使用。若迁入 `coupon/api/command/` 可能与 `CreateCouponCommand` 语义重复。

**缓解**：本次迁移**不动** `CouponCreateRequest`，保留在 `dto/` 包（Controller 层请求 DTO）。`CreateCouponCommand` 是 API 层 Command，由 `CouponApiConverter.toCommand(CouponCreateRequest)` 转换，两者职责不同：前者是 HTTP 请求体（含校验注解），后者是 API 契约入参（纯 POJO）。

### 6.5 CouponType/UserCouponStatus 枚举迁移影响面

**风险**：`CouponType` 和 `UserCouponStatus` 实现 MyBatis-Plus `IEnum<String>` 接口，被 `Coupon`/`UserCoupon` Entity 的 `@TableField` 字段使用。迁移 package 后需更新所有 import，包括 Entity、ServiceImpl、可能的 JSON 序列化配置。

**缓解**：仅移动文件位置，不修改枚举内容。迁移后立即跑 mvn test 验证优惠券 CRUD/领取/核销场景。MyBatis-Plus `IEnum` 机制与 package 无关，仅依赖枚举类全限定名，迁移后 Spring Boot 自动扫描 `@Mapper` 仍能正确映射。

### 6.6 Strangler 过渡期 CouponService/CouponUsageService 保留

**风险**：`CouponService` + `CouponServiceImpl` + `CouponUsageService` + `CouponUsageServiceImpl` 在过渡期保留，可能与 `CouponApi` + `CouponApplicationService` + `CouponUsageApi` + `CouponUsageApplicationService` 并存导致语义重复。

**缓解**：`CouponApplicationService` / `CouponUsageApplicationService` 仅做门面委托，不含业务逻辑；待所有外部调用方迁移完毕且稳定后，再考虑将业务逻辑从旧 ServiceImpl 迁入 ApplicationService 并删除旧 Service（不在本次迁移范围）。

### 6.7 ArchUnit freeze 模式

**风险**：新增 3 条 coupon 模块规则后，freeze 模式首次运行会冻结当前违规。若 `coupon.application` 当前仍依赖 `coupon.infrastructure.mapper`（通过旧 Service 间接依赖），可能被冻结而非立即报错。

**缓解**：Phase CP.4 保证 `CouponApplicationService` / `CouponUsageApplicationService` 不直接 import `CouponMapper`/`UserCouponMapper`；Phase CP.5 保证 `OrderServiceImpl` / `OrderLifecycleServiceImpl` 不再直接 import `CouponUsageService`。新增规则后跑 ArchUnit 验证全绿。

### 6.8 CouponUsageApi.calculateDiscount 返回类型变更

**风险**：旧 `CouponUsageService.calculateDiscount` 返回 `BigDecimal`，新 `CouponUsageApi.calculateDiscount` 返回 `CouponCalcResult`。`OrderServiceImpl.applyCouponToOrder` 需适配新返回类型。

**缓解**：`CouponCalcResult` 仅含 `discount` 字段（`BigDecimal`），`OrderServiceImpl` 调用 `.getDiscount()` 即可。过渡期 `CouponCalcResult` 不新增其他字段，保持最小变更。未来可扩展 `applicable`（是否可用）、`reason`（不可用原因）等字段。

---

## 7. 回归测试基线

| 指标 | 基线 | 目标 |
|---|---|---|
| Tests run | 129 | ≥ 129 |
| Failures | 1 | ≤ 1 |
| Errors | 19 | ≤ 19 |
| ArchUnit 规则 | 33 全绿 | 36 全绿（新增 3 条 coupon 规则） |

---

## 8. 参考文档

- [CART-MIGRATION-PLAN.md](CART-MIGRATION-PLAN.md) - Cart 模块迁移经验（最近完成，主要参考）
- [PRODUCT-MIGRATION-PLAN.md](PRODUCT-MIGRATION-PLAN.md) - Product 模块迁移经验
- [ORDER-MIGRATION-PLAN.md](ORDER-MIGRATION-PLAN.md) - Order 模块迁移经验
- [COUPON-API-CONTRACT.md](COUPON-API-CONTRACT.md) - Coupon 模块 API 契约