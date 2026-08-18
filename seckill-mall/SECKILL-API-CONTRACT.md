# Seckill 模块 API 契约 (SECKILL-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase SK.2 - Seckill API 契约层定义
> 依据：SECKILL-MIGRATION-PLAN.md + PaymentApi 契约风格 + CouponApi 契约风格
> 说明：本文件定义 Seckill 模块对外暴露的 API 契约，一经发布只增不改不删。

---

## 1. SeckillApi 接口

**全限定名**：`com.seckill.mall.seckill.api.SeckillApi`

**实现类**：`com.seckill.mall.seckill.application.SeckillApplicationService`

**职责**：秒杀核心能力（执行秒杀 + 查询秒杀结果），供 SeckillController 调用。

**设计原则**：
- 方法用业务语言命名，禁止 CRUD 命名
- 入参用 Command/Query 对象，禁止裸露多参数
- 出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO
- 异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈
- 向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

```java
package com.seckill.mall.seckill.api;

public interface SeckillApi {

    /**
     * 执行秒杀下单。
     *
     * <p>令牌校验 → 库存预扣（Redis Lua 原子操作）→ 异步发消息创建订单。
     *
     * @param command 秒杀命令（seckillId + seckillToken，userId 从 CurrentUserContext 获取）
     * @return 秒杀结果（含 requestId，用于轮询秒杀结果）
     */
    SeckillResult executeSeckill(SeckillCommand command);

    /**
     * 查询秒杀结果（前端轮询用）。
     *
     * @param seckillId 秒杀商品 ID
     * @param requestId 请求 ID（executeSeckill 返回的）
     * @return 秒杀结果（含订单号、支付状态等）
     */
    SeckillResult getSeckillResult(Long seckillId, String requestId);
}
```

---

## 2. SeckillOrderApi 接口

**全限定名**：`com.seckill.mall.seckill.api.SeckillOrderApi`

**实现类**：`com.seckill.mall.seckill.application.SeckillOrderApplicationService`

**职责**：秒杀订单能力（查询/支付/取消/发货/确认/超时/统计），供 order、payment、review、stats、system 模块调用。

```java
package com.seckill.mall.seckill.api;

public interface SeckillOrderApi {

    // === 用户端 ===

    /**
     * 查询用户秒杀订单列表。
     *
     * @param query 查询条件（userId + status + pageNum + pageSize）
     * @return 分页结果（DTO，不含 Entity）
     */
    PageResult<SeckillOrderDTO> listOrders(SeckillOrderQuery query);

    /**
     * 查询秒杀订单详情。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 订单 DTO
     */
    SeckillOrderDTO getOrderDetail(Long userId, Long orderId);

    /**
     * 支付秒杀订单。
     *
     * @param command 支付命令（userId + orderId + payMethod）
     * @return 更新后的订单 DTO
     */
    SeckillOrderDTO payOrder(SeckillPayCommand command);

    /**
     * 取消秒杀订单。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 更新后的订单 DTO
     */
    SeckillOrderDTO cancelOrder(Long userId, Long orderId);

    /**
     * 查询订单状态（返回字符串而非枚举，避免序列化枚举内部结构）。
     */
    String getOrderStatus(Long userId, Long orderId);

    // === 管理端 ===

    /**
     * 超时取消（延迟消费者触发）：UNPAID → TIMEOUT 并回补库存。
     *
     * @param orderId 订单 ID
     * @return true 表示本次执行了超时取消
     */
    boolean timeoutCancel(Long orderId);

    /**
     * 发货：PAID → SHIPPED。
     */
    void shipOrder(SeckillShipCommand command);

    /**
     * 确认收货：SHIPPED → COMPLETED。
     */
    void confirmOrder(Long userId, Long orderId);

    // === 跨模块查询 ===

    /**
     * 校验用户是否通过秒杀订单购买了该商品（review 模块评价权限校验用）。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return true=已购买
     */
    boolean hasUserPurchasedSeckill(Long userId, Long productId);

    /**
     * 查询用户钱包支付的已支付秒杀订单（payment 模块 WalletService 用）。
     *
     * @param userId 用户 ID
     * @return 秒杀订单 DTO 列表
     */
    List<SeckillOrderDTO> getWalletPaidOrdersByUser(Long userId);

    /**
     * 统一订单列表查询（order 模块 OrderQueryService 用）。
     *
     * @param userId 用户 ID
     * @param status 订单状态序号（null=不筛选）
     * @param limit  最大返回条数
     * @return 秒杀订单 DTO 列表
     */
    List<SeckillOrderDTO> getSeckillOrdersForUnifiedList(Long userId, Integer status, int limit);

    /**
     * 逻辑删除秒杀订单（order 模块 deleteOrder 用）。
     *
     * @param orderId 订单 ID
     * @return true 表示删除成功
     */
    boolean logicalDeleteSeckillOrder(Long orderId);

    // === 统计（stats 模块用）===

    /** 订单总数 */
    long countAll();

    /** 销售总额 */
    BigDecimal sumSalesAmount(List<OrderStatus> statuses);

    /** 今日订单数 */
    Long countTodayOrders(LocalDate today);

    /** 订单趋势 */
    List<Map<String, Object>> selectOrderTrend(LocalDate startDate, LocalDate endDate, List<OrderStatus> statuses);

    /** 秒杀排行榜 Top N */
    List<SeckillRankingDTO> selectSeckillRanking(List<OrderStatus> statuses, int limit);

    /** 订单状态分布 */
    List<Map<String, Object>> selectStatusDistribution(LocalDateTime startTime, LocalDateTime endTime);
}
```

---

## 3. SeckillActivityApi 接口

**全限定名**：`com.seckill.mall.seckill.api.SeckillActivityApi`

**实现类**：`com.seckill.mall.seckill.application.SeckillActivityApplicationService`

**职责**：秒杀场次能力（创建/查询/删除），供 SeckillController 调用。

```java
package com.seckill.mall.seckill.api;

public interface SeckillActivityApi {

    /**
     * 创建秒杀场次（含场次下所有商品）。
     *
     * @param command 创建命令
     * @return 场次 DTO
     */
    SeckillActivityDTO createActivity(CreateActivityCommand command);

    /**
     * 查询所有场次列表（含每场次下商品列表）。
     */
    List<SeckillActivityDTO> listActivities();

    /**
     * 查询场次详情（含商品列表）。
     */
    SeckillActivityDTO getActivityDetail(Long activityId);

    /**
     * 删除场次（逻辑删除）。
     */
    void deleteActivity(Long activityId);
}
```

---

## 4. SeckillGoodsApi 接口

**全限定名**：`com.seckill.mall.seckill.api.SeckillGoodsApi`

**实现类**：`com.seckill.mall.seckill.application.SeckillGoodsApplicationService`

**职责**：秒杀商品能力（查询/创建/更新/取消/库存/预热/统计），供 SeckillController 和 stats 模块调用。

```java
package com.seckill.mall.seckill.api;

public interface SeckillGoodsApi {

    // === 查询 ===

    /**
     * 秒杀商品分页列表。
     *
     * @param query 查询条件（status + categoryId + pageNum + pageSize）
     * @return 分页结果
     */
    PageResult<SeckillGoodsDTO> listSeckill(SeckillGoodsQuery query);

    /**
     * 秒杀商品详情。
     */
    SeckillGoodsDTO getSeckillDetail(Long seckillId);

    // === 管理 ===

    /**
     * 创建秒杀商品。
     */
    SeckillGoodsDTO createSeckill(CreateSeckillGoodsCommand command);

    /**
     * 更新秒杀商品。
     */
    SeckillGoodsDTO updateSeckill(Long id, CreateSeckillGoodsCommand command);

    /**
     * 取消秒杀商品。
     */
    void cancelSeckill(Long id);

    // === 库存 ===

    /** 查询库存 */
    Integer getStock(Long seckillId);

    /** 预热秒杀商品（Redis + 布隆过滤器） */
    void preheatSeckill(Long seckillId);

    // === 统计（stats 模块用）===

    /** 秒杀活动总数 */
    long countAll();

    /** 进行中秒杀数 */
    long countActive();

    /** 待开始秒杀数 */
    long countPending();

    /** 今日已完成秒杀数 */
    long countCompletedToday();
}
```

---

## 5. DTO 定义

### 5.1 SeckillActivityDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillActivityDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 场次 ID |
| name | String | 场次名称 |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| status | Integer | 状态 |
| goodsList | List<SeckillGoodsDTO> | 场次下商品列表 |

### 5.2 SeckillGoodsDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillGoodsDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 秒杀商品 ID |
| productId | Long | 原商品 ID |
| productName | String | 商品名称 |
| productImage | String | 商品主图 |
| seckillPrice | BigDecimal | 秒杀价格 |
| originalPrice | BigDecimal | 原价 |
| totalStock | Integer | 总库存 |
| availableStock | Integer | 可用库存 |
| limitPerUser | Integer | 每人限购 |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| status | Integer | 状态 |
| activityId | Long | 所属场次 ID |

### 5.3 SeckillOrderDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillOrderDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |
| userId | Long | 用户 ID |
| seckillId | Long | 秒杀商品 ID |
| productId | Long | 商品 ID |
| quantity | Integer | 购买数量 |
| seckillPrice | BigDecimal | 秒杀价格 |
| payAmount | BigDecimal | 支付金额 |
| payMethod | String | 支付方式 |
| status | String | 订单状态 |
| createTime | LocalDateTime | 创建时间 |
| payTime | LocalDateTime | 支付时间 |
| shipTime | LocalDateTime | 发货时间 |
| confirmTime | LocalDateTime | 确认收货时间 |
| shippingCompany | String | 物流公司 |
| shippingNo | String | 快递单号 |
| transactionId | String | 交易流水号 |

### 5.4 SeckillOverviewDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillOverviewDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| totalActivities | long | 秒杀活动总数 |
| activeCount | long | 进行中秒杀数 |
| pendingCount | long | 待开始秒杀数 |
| completedTodayCount | long | 今日已完成秒杀数 |

### 5.5 SeckillRankingDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillRankingDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| seckillId | Long | 秒杀商品 ID |
| seckillName | String | 秒杀商品名称 |
| salesCount | long | 销售数量 |
| salesAmount | BigDecimal | 销售总额 |

### 5.6 SeckillResultDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillResultDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 是否成功 |
| requestId | String | 请求 ID |
| orderId | Long | 订单 ID（成功时） |
| message | String | 提示信息 |

---

## 6. Command 定义

### 6.1 SeckillCommand

**全限定名**：`com.seckill.mall.seckill.api.command.SeckillCommand`

| 字段 | 类型 | 说明 |
|------|------|------|
| seckillId | Long | 秒杀商品 ID |
| seckillToken | String | 秒杀令牌 |

### 6.2 SeckillPayCommand

**全限定名**：`com.seckill.mall.seckill.api.command.SeckillPayCommand`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| orderId | Long | 订单 ID |
| payMethod | String | 支付方式 |

### 6.3 SeckillShipCommand

**全限定名**：`com.seckill.mall.seckill.api.command.SeckillShipCommand`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 操作人 ID（管理员） |
| orderId | Long | 订单 ID |
| shippingCompany | String | 物流公司 |
| shippingNo | String | 快递单号 |

### 6.4 CreateActivityCommand

**全限定名**：`com.seckill.mall.seckill.api.command.CreateActivityCommand`

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 场次名称 |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| goodsList | List<CreateSeckillGoodsCommand> | 场次下商品列表 |

### 6.5 CreateSeckillGoodsCommand

**全限定名**：`com.seckill.mall.seckill.api.command.CreateSeckillGoodsCommand`

| 字段 | 类型 | 说明 |
|------|------|------|
| productId | Long | 原商品 ID |
| seckillPrice | BigDecimal | 秒杀价格 |
| totalStock | Integer | 总库存 |
| limitPerUser | Integer | 每人限购 |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| activityId | Long | 所属场次 ID |

---

## 7. Query 定义

### 7.1 SeckillOrderQuery

**全限定名**：`com.seckill.mall.seckill.api.query.SeckillOrderQuery`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| status | Integer | 订单状态（null=不筛选） |
| pageNum | Integer | 页码 |
| pageSize | Integer | 每页大小 |

### 7.2 SeckillGoodsQuery

**全限定名**：`com.seckill.mall.seckill.api.query.SeckillGoodsQuery`

| 字段 | 类型 | 说明 |
|------|------|------|
| status | String | 状态 |
| categoryId | Long | 分类 ID |
| pageNum | Integer | 页码 |
| pageSize | Integer | 每页大小 |

---

## 8. Result 定义

### 8.1 SeckillResult

**全限定名**：`com.seckill.mall.seckill.api.result.SeckillResult`

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 是否成功 |
| requestId | String | 请求 ID（用于轮询） |
| orderId | Long | 订单 ID（成功时） |
| message | String | 提示信息 |

---

## 9. 委托映射（API 方法 → 旧 Service 方法）

| API 接口 | API 方法 | 委托旧 Service | 旧 Service 方法 |
|----------|---------|---------------|----------------|
| SeckillApi | executeSeckill | SeckillService | doSeckill |
| SeckillApi | getSeckillResult | SeckillService | getSeckillResult |
| SeckillOrderApi | listOrders | SeckillOrderService | getOrderList |
| SeckillOrderApi | getOrderDetail | SeckillOrderService | getOrderDetail |
| SeckillOrderApi | payOrder | SeckillOrderService | payOrder |
| SeckillOrderApi | cancelOrder | SeckillOrderService | cancelOrder |
| SeckillOrderApi | getOrderStatus | SeckillOrderService | getOrderStatus |
| SeckillOrderApi | timeoutCancel | SeckillOrderService | timeoutCancel |
| SeckillOrderApi | shipOrder | SeckillOrderService | shipOrder |
| SeckillOrderApi | confirmOrder | SeckillOrderService | confirmOrder |
| SeckillOrderApi | hasUserPurchasedSeckill | SeckillOrderService | hasUserPurchasedSeckill |
| SeckillOrderApi | getWalletPaidOrdersByUser | SeckillOrderService | getWalletPaidOrdersByUser |
| SeckillOrderApi | getSeckillOrdersForUnifiedList | SeckillOrderService | getSeckillOrdersForUnifiedList |
| SeckillOrderApi | logicalDeleteSeckillOrder | SeckillOrderService | logicalDeleteSeckillOrder |
| SeckillOrderApi | countAll | SeckillOrderService | countAll |
| SeckillOrderApi | sumSalesAmount | SeckillOrderService | sumSalesAmount |
| SeckillOrderApi | countTodayOrders | SeckillOrderService | countTodayOrders |
| SeckillOrderApi | selectOrderTrend | SeckillOrderService | selectOrderTrend |
| SeckillOrderApi | selectSeckillRanking | SeckillOrderService | selectSeckillRanking |
| SeckillOrderApi | selectStatusDistribution | SeckillOrderService | selectStatusDistribution |
| SeckillActivityApi | createActivity | SeckillActivityService | createActivity |
| SeckillActivityApi | listActivities | SeckillActivityService | listActivities |
| SeckillActivityApi | getActivityDetail | SeckillActivityService | getActivityDetail |
| SeckillActivityApi | deleteActivity | SeckillActivityService | deleteActivity |
| SeckillGoodsApi | listSeckill | SeckillGoodsService | listSeckill |
| SeckillGoodsApi | getSeckillDetail | SeckillGoodsService | getSeckillDetail |
| SeckillGoodsApi | createSeckill | SeckillGoodsService | createSeckill |
| SeckillGoodsApi | updateSeckill | SeckillGoodsService | updateSeckill |
| SeckillGoodsApi | cancelSeckill | SeckillGoodsService | cancelSeckill |
| SeckillGoodsApi | getStock | SeckillGoodsService | getStock |
| SeckillGoodsApi | preheatSeckill | SeckillGoodsService | preheatSeckill |
| SeckillGoodsApi | countAll | SeckillGoodsService | countAll |
| SeckillGoodsApi | countActive | SeckillGoodsService | countActive |
| SeckillGoodsApi | countPending | SeckillGoodsService | countPending |
| SeckillGoodsApi | countCompletedToday | SeckillGoodsService | countCompletedToday |

---

## 10. 转换器

**全限定名**：`com.seckill.mall.seckill.application.SeckillApiConverter`

**职责**：在 API 层与旧 Service 层之间做类型转换。

| 转换方向 | 方法 | 说明 |
|----------|------|------|
| Command → 裸参数 | fromCommand(SeckillCommand) → (seckillId, seckillToken) | 拆解 Command |
| VO → DTO | toDTO(SeckillActivityVO) → SeckillActivityDTO | 场次 VO 转 DTO |
| VO → DTO | toDTO(SeckillGoodsVO) → SeckillGoodsDTO | 商品 VO 转 DTO |
| VO → DTO | toDTO(SeckillOrderVO) → SeckillOrderDTO | 订单 VO 转 DTO |
| VO → DTO | toDTO(SeckillRankingVO) → SeckillRankingDTO | 排行 VO 转 DTO |
| Entity → DTO | toDTO(SeckillOrder) → SeckillOrderDTO | 订单 Entity 转 DTO |
| Result → VO | toVO(SeckillResult) → SeckillResultVO | 结果转回 VO（Controller 用） |
| DTO → VO | toVO(SeckillOrderDTO) → SeckillOrderVO | DTO 转回 VO（Controller 用） |
| DTO → VO | toVO(SeckillGoodsDTO) → SeckillGoodsVO | DTO 转回 VO |
| DTO → VO | toVO(SeckillActivityDTO) → SeckillActivityVO | DTO 转回 VO |
| Request → Command | toCommand(SeckillActivityCreateRequest) → CreateActivityCommand | 请求转 Command |
| Request → Command | toCommand(SeckillCreateRequest) → CreateSeckillGoodsCommand | 请求转 Command |
| Request → Query | toQuery(SeckillOrderQueryRequest) → SeckillOrderQuery | 请求转 Query |

---

## 11. 异常契约

| 场景 | 异常 | ErrorCode |
|------|------|-----------|
| 秒杀令牌无效 | BusinessException | SECKILL_TOKEN_INVALID |
| 秒杀活动未开始 | BusinessException | SECKILL_NOT_STARTED |
| 秒杀活动已结束 | BusinessException | SECKILL_ENDED |
| 库存不足 | BusinessException | SECKILL_STOCK_EMPTY |
| 重复秒杀 | BusinessException | SECKILL_DUPLICATE |
| 订单不存在 | BusinessException | SECKILL_ORDER_NOT_FOUND |
| 订单状态非法 | BusinessException | SECKILL_ORDER_STATUS_ILLEGAL |

---

## 12. 向后兼容承诺

1. **方法签名只增不改不删**：API 接口方法一经发布，不修改方法名、参数类型、返回类型，不删除方法。
2. **DTO 字段只增不删不改变类型**：DTO 字段一经发布，不删除字段、不改变字段类型、不改变字段语义。
3. **Command/Query 字段只增-不删不改变类型**：入参字段一经发布，不删除、不改变类型。
4. **异常 ErrorCode 只增不改**：错误码一经发布，不修改含义、不删除。
5. **委托关系保持不变**：ApplicationService 委托旧 Service 的映射关系在旧 Service 删除前保持不变。
6. **前端契约不破坏**：Controller 出参 VO 结构保持不变，通过 Converter 做 DTO→VO 反向转换。

---

## 13. 内部 Service（不暴露为 API）

以下 Service 是 seckill 模块内部实现，不对外暴露 API 接口，但需归位到 seckill 模块包下：

| Service | 归位路径 | 说明 |
|---------|---------|------|
| SeckillTokenService | seckill/infrastructure/ | 令牌生成/校验 |
| SeckillInventoryPort | seckill/domain/ | 库存端口接口 |
| SeckillInventoryPortImpl | seckill/infrastructure/ | 库存端口实现 |
| SeckillDbStrategy | seckill/infrastructure/ | DB 分片策略 |
| SeckillStatusScheduler | seckill/infrastructure/scheduler/ | 状态定时调度 |
| SeckillOrderProducer | seckill/infrastructure/mq/ | MQ 生产者 |
| SeckillOrderConsumer | seckill/infrastructure/mq/ | MQ 消费者 |
| SeckillOrderMessage | seckill/infrastructure/mq/ | MQ 消息 |
| SeckillResultMessage | seckill/infrastructure/mq/ | MQ 结果消息 |
| SeckillLuaService | seckill/infrastructure/cache/ | Redis Lua 脚本 |
| SeckillBloomInitializer | seckill/infrastructure/cache/ | 布隆过滤器初始化 |