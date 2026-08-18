# Seckill 模块迁移计划 (SECKILL-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase SK - Seckill 模块迁移（Strangler Pattern 第七阶段，最后一个业务模块）
> 依据：PAYMENT-MIGRATION-PLAN.md + COUPON-MIGRATION-PLAN.md + CART-MIGRATION-PLAN.md + PRODUCT-MIGRATION-PLAN.md + ORDER-MIGRATION-PLAN.md + IDENTITY-MIGRATION-PLAN.md + Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Seckill 当前结构

### 1.1 当前包结构

Seckill 相关代码散落在 8 个不同的顶层包中，共 **30+ 核心文件**，是迄今最复杂的业务模块（含 MQ + Scheduler + Cache + Lua 三大基础设施组件）：

#### Service 接口（7 个，位于 `service` 包）

| 文件 | 当前包 | 职责 | 暴露为 API |
|---|---|---|---|
| `SeckillService` | `service` | 秒杀下单核心（doSeckill + getSeckillResult） | ✅ SeckillApi |
| `SeckillOrderService` | `service` | 秒杀订单全生命周期（创建/查询/支付/取消/发货/确认/超时/统计/物理删除） | ✅ SeckillOrderApi |
| `SeckillActivityService` | `service` | 秒杀场次 CRUD（含场次下商品批量预热） | ✅ SeckillActivityApi |
| `SeckillGoodsService` | `service` | 秒杀商品 CRUD + 库存查询 + 预热 + 概览统计 | ✅ SeckillGoodsApi |
| `SeckillTokenService` | `service` | 秒杀令牌生成/校验（Redis TTL = 活动剩余时间） | ❌ 内部（归位 seckill.application） |
| `SeckillInventoryPort` | `service` | 库存端口接口（统一 DB + Redis 回补） | ❌ 内部（归位 seckill.domain） |
| `SeckillDbStrategy` | `service` | DB 直降策略（Redis 不可用降级） | ❌ 内部（归位 seckill.application） |

#### Service 实现（5 个，位于 `service.impl` 包）

| 文件 | 当前包 | 实现接口 |
|---|---|---|
| `SeckillServiceImpl` | `service.impl` | `SeckillService` |
| `SeckillOrderServiceImpl` | `service.impl` | `SeckillOrderService` |
| `SeckillActivityServiceImpl` | `service.impl` | `SeckillActivityService` |
| `SeckillGoodsServiceImpl` | `service.impl` | `SeckillGoodsService` |
| `SeckillInventoryPortImpl` | `service.impl` | `SeckillInventoryPort` |

#### Entity（3 个，位于 `entity` 包）

| 文件 | 表 | 说明 |
|---|---|---|
| `SeckillActivity` | `t_seckill_activity` | 秒杀场次（name + startTime + endTime + perLimit） |
| `SeckillGoods` | `t_seckill_goods` | 秒杀商品（activityId 关联场次 + seckillPrice + stockCount + availableCount + status） |
| `SeckillOrder` | `t_seckill_order` | 秒杀订单（orderNo + userId + seckillId + productId + seckillPrice + status + 物流/支付/取消字段） |

#### Mapper（3 个，位于 `mapper` 包）

| Mapper | 自定义方法 | 说明 |
|---|---|---|
| `SeckillActivityMapper` | 无 | 仅继承 `BaseMapper<SeckillActivity>` |
| `SeckillGoodsMapper` | `selectSeckillPage` / `selectActiveList` / `deductStockOptimistic` / `restoreStockOptimistic` / `updatePendingToActive` / `updateActiveToEnded` | 含乐观锁扣减/回补 + 状态调度 SQL |
| `SeckillOrderMapper` | `findByUserAndSeckill` / `selectOrderPage` / `sumSalesAmount` / `selectOrderTrend` / `selectStatusDistribution` / `selectAdminOrderPage` / `countTodayOrders` / `selectSeckillRanking` / `deleteTerminalOrders` / `deletePhysical` | 含统计 + 物理删除 SQL |

#### Enum（位于 `entity.enums` 包）

| 文件 | 值 | 说明 |
|---|---|---|
| `SeckillStatus` | PENDING / ACTIVE / ENDED / CANCELLED | 实现 MyBatis-Plus `IEnum<String>`，含 `@JsonValue` / `@JsonCreator` |

#### VO（6 个，位于 `vo` 包）

| 文件 | 用途 |
|---|---|
| `SeckillActivityVO` | 场次视图（含 goodsList 嵌套） |
| `SeckillGoodsVO` | 秒杀商品视图（含 productName + images 列表） |
| `SeckillOrderVO` | 秒杀订单视图（含 status code + statusDescription 中文描述） |
| `SeckillOverviewVO` | 概览视图（activeCount + pendingCount + completedToday） |
| `SeckillRankingVO` | 排行榜视图（seckillId + productName + seckillPrice + salesCount + totalAmount） |
| `SeckillResultVO` | 秒杀结果视图（status 0/1/-1 + requestId + orderId + orderNo + totalAmount + payExpireTime） |

#### DTO（2 个，位于 `dto` 包，Controller 层请求 DTO，含 Bean Validation）

| 文件 | 用途 |
|---|---|
| `SeckillCreateRequest` | 创建/编辑秒杀商品请求（productId + seckillName + seckillPrice + stockCount + startTime + endTime + perLimit + images + description） |
| `SeckillActivityCreateRequest` | 创建秒杀场次请求（name + startTime + endTime + perLimit + goodsItems 列表，内嵌 `ActivityGoodsItem`） |

#### Converter（1 个，位于 `converter` 包）

| 文件 | 说明 |
|---|---|
| `SeckillOrderConverter` | MapStruct 转换器，`SeckillOrder` entity → `SeckillOrderVO`，`@AfterMapping` 处理枚举 → code + 中文描述 |

#### Controller（1 个，位于 `controller` 包）

| 文件 | 基路径 | 端点数 |
|---|---|---|
| `SeckillController` | `/api/v1/seckill` | 12 个端点（list/detail/token/stock/doSeckill/execute/result/create/update/cancel + 场次 4 个） |

#### MQ（位于 `mq` 包，4 个文件）

| 文件 | 当前包 | 职责 |
|---|---|---|
| `SeckillOrderProducer` | `mq.producer` | 异步下单生产者（发送 SeckillOrderMessage） |
| `SeckillOrderConsumer` | `mq.consumer` | 异步下单消费者（扣减 DB 库存 + 创建订单 + 发送 SeckillResultMessage） |
| `SeckillOrderMessage` | `mq.message` | 异步下单消息（seckillId + userId + requestId + timestamp） |
| `SeckillResultMessage` | `mq.message` | 秒杀结果消息（userId + seckillId + status + orderId + orderNo + totalAmount） |

#### Scheduler（位于 `scheduler` 包，1 个文件）

| 文件 | 职责 |
|---|---|
| `SeckillStatusScheduler` | ① 每 60s 更新 PENDING→ACTIVE / ACTIVE→ENDED；② 每 5min 库存对账补偿（Redis 校正到 DB available_count） |

#### Cache（位于 `cache` 包，2 个文件）

| 文件 | 职责 |
|---|---|
| `SeckillLuaService` | Lua 脚本原子预减库存（deductStock） + 原子回补（rollbackDeduct），加载 `lua/seckill_deduct.lua` + `lua/seckill_rollback.lua` |
| `SeckillBloomInitializer` | `ApplicationRunner` 启动时初始化布隆过滤器（分页加载所有 seckillId，期望 10000 元素，误判率 0.01） |

### 1.2 Service 方法签名

#### SeckillService（2 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `doSeckill` | `SeckillResultVO doSeckill(Long seckillId, String seckillToken)` | 执行秒杀下单（Redis 预减 + 异步 MQ + DB 降级） | ⚠️ 返回 VO，裸参数（2 个），内部通过 securityUtils.getCurrentUserId() 获取 userId |
| `getSeckillResult` | `SeckillResultVO getSeckillResult(Long seckillId, String requestId)` | 查询秒杀结果（校验 requestId 归属当前用户） | ⚠️ 返回 VO，裸参数（2 个），内部校验 userId |

#### SeckillOrderService（17 个方法，最复杂）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `createSeckillOrder` | `SeckillOrder createSeckillOrder(Long seckillId, Long userId, String requestId)` | 异步消费者创建订单 | ⚠️ 返回 Entity（泄漏 PO），仅供 SeckillDbStrategy 内部调用 |
| `deleteSeckillOrderPhysically` | `void deleteSeckillOrderPhysically(Long orderId)` | 物理删除幽灵单 | ✅ |
| `getOrderList` | `PageResult<SeckillOrderVO> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize)` | 用户订单分页 | ⚠️ 返回 VO，裸参数（4 个） |
| `getOrderDetail` | `SeckillOrderVO getOrderDetail(Long userId, Long orderId)` | 订单详情 | ⚠️ 返回 VO，裸参数（2 个） |
| `payOrder` | `SeckillOrderVO payOrder(Long userId, Long orderId, String payMethod)` | 支付秒杀订单 | ⚠️ 返回 VO，裸参数（3 个），调用 PaymentApi.pay |
| `cancelOrder` | `SeckillOrderVO cancelOrder(Long userId, Long orderId)` | 取消订单 | ⚠️ 返回 VO，裸参数（2 个） |
| `getOrderStatus` | `String getOrderStatus(Long userId, Long orderId)` | 查订单状态字符串 | ⚠️ 裸参数（2 个） |
| `timeoutCancel` | `boolean timeoutCancel(Long orderId)` | 超时取消（延迟消费者触发） | ✅ |
| `shipOrder` | `void shipOrder(Long userId, Long orderId, String shippingCompany, String shippingNo)` | 发货 | ⚠️ 裸参数（4 个） |
| `confirmOrder` | `void confirmOrder(Long userId, Long orderId)` | 确认收货 | ⚠️ 裸参数（2 个） |
| `getSeckillOrderById` | `SeckillOrder getSeckillOrderById(Long orderId)` | 返回 Entity（模块间内部调用） | ⚠️ 返回 Entity，仅供 OrderServiceImpl 跨模块内部调用 |
| `getSeckillOrdersForUnifiedList` | `List<SeckillOrder> getSeckillOrdersForUnifiedList(Long userId, Integer status, int limit)` | 统一订单列表（返回 Entity） | ⚠️ 返回 Entity，仅供 OrderServiceImpl 跨模块内部调用 |
| `logicalDeleteSeckillOrder` | `boolean logicalDeleteSeckillOrder(Long orderId)` | 逻辑删除 | ✅ |
| `hasUserPurchasedSeckill` | `boolean hasUserPurchasedSeckill(Long userId, Long productId)` | 校验用户是否通过秒杀购买该商品 | ⚠️ 裸参数（2 个），被 ProductReviewServiceImpl 调用 |
| `getWalletPaidOrdersByUser` | `List<SeckillOrder> getWalletPaidOrdersByUser(Long userId)` | 钱包支付的已支付秒杀订单（返回 Entity） | ⚠️ 返回 Entity，仅供 WalletServiceImpl 跨模块内部调用 |
| `countAll` | `long countAll()` | 订单总数 | ✅ |
| `sumSalesAmount` | `BigDecimal sumSalesAmount(List<OrderStatus> statuses)` | 销售总额 | ✅ |
| `countTodayOrders` | `Long countTodayOrders(LocalDate today)` | 今日订单数 | ✅ |
| `selectOrderTrend` | `List<Map<String, Object>> selectOrderTrend(...)` | 订单趋势 | ⚠️ 返回裸 Map |
| `selectSeckillRanking` | `List<SeckillRankingVO> selectSeckillRanking(...)` | 秒杀排行 Top N | ⚠️ 返回 VO |
| `selectStatusDistribution` | `List<Map<String, Object>> selectStatusDistribution(...)` | 状态分布 | ⚠️ 返回裸 Map |
| `selectAdminOrderPage` | `IPage<AdminOrderVO> selectAdminOrderPage(...)` | 后台订单高级筛选 | ⚠️ 返回 IPage<VO> |

#### SeckillActivityService（4 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `createActivity` | `SeckillActivityVO createActivity(SeckillActivityCreateRequest req)` | 创建场次（含商品列表） | ⚠️ 返回 VO，入参为 Request（含校验注解） |
| `listActivities` | `List<SeckillActivityVO> listActivities()` | 查所有场次 | ⚠️ 返回 VO |
| `getActivityDetail` | `SeckillActivityVO getActivityDetail(Long activityId)` | 场次详情 | ⚠️ 返回 VO |
| `deleteActivity` | `void deleteActivity(Long activityId)` | 逻辑删除场次 | ✅ |

#### SeckillGoodsService（11 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `listSeckill` | `PageResult<SeckillGoodsVO> listSeckill(String status, Long categoryId, Integer pageNum, Integer pageSize)` | 秒杀商品分页 | ⚠️ 返回 VO，裸参数（4 个） |
| `getSeckillDetail` | `SeckillGoodsVO getSeckillDetail(Long seckillId)` | 秒杀商品详情 | ⚠️ 返回 VO |
| `createSeckill` | `SeckillGoodsVO createSeckill(SeckillCreateRequest req)` | 创建秒杀商品 | ⚠️ 返回 VO，入参为 Request |
| `updateSeckill` | `SeckillGoodsVO updateSeckill(Long id, SeckillCreateRequest req)` | 编辑秒杀商品 | ⚠️ 返回 VO |
| `cancelSeckill` | `void cancelSeckill(Long id)` | 取消秒杀活动 | ✅ |
| `getStock` | `Integer getStock(Long seckillId)` | 查实时库存 | ✅ |
| `preheatSeckill` | `void preheatSeckill(Long seckillId)` | 预热秒杀（Redis 库存 + 布隆过滤器） | ✅ |
| `countAll` | `long countAll()` | 秒杀活动总数 | ✅ |
| `countActive` | `long countActive()` | 进行中秒杀数（时间窗口动态计算） | ✅ |
| `countPending` | `long countPending()` | 待开始秒杀数 | ✅ |
| `countCompletedToday` | `long countCompletedToday()` | 今日已完成秒杀数 | ✅ |

### 1.3 Controller 端点

| 端点 | 方法 | 权限 | 委托 Service |
|---|---|---|---|
| `GET /api/v1/seckill/list` | 秒杀活动列表 | 公开 | `SeckillGoodsService.listSeckill` |
| `GET /api/v1/seckill/{seckillId}` | 秒杀活动详情 | 公开 | `SeckillGoodsService.getSeckillDetail` |
| `GET /api/v1/seckill/{seckillId}/token` | 获取秒杀令牌 | BUYER | `SeckillTokenService.getSeckillToken` |
| `GET /api/v1/seckill/{seckillId}/stock` | 查询实时库存 | 公开 | `SeckillGoodsService.getStock` |
| `POST /api/v1/seckill/{seckillId}` | 执行秒杀 | BUYER + @RateLimit | `SeckillService.doSeckill` |
| `POST /api/v1/seckill/{seckillId}/execute` | 一键秒杀（无需预取 token） | BUYER + @RateLimit | `SeckillTokenService.getSeckillToken` + `SeckillService.doSeckill` |
| `GET /api/v1/seckill/{seckillId}/result` | 查询秒杀结果 | BUYER | `SeckillService.getSeckillResult` |
| `POST /api/v1/seckill/admin` | 创建秒杀活动 | ADMIN/SELLER | `SeckillGoodsService.createSeckill` |
| `PUT /api/v1/seckill/admin/{seckillId}` | 编辑秒杀活动 | ADMIN/SELLER | `SeckillGoodsService.updateSeckill` |
| `PUT /api/v1/seckill/admin/{seckillId}/cancel` | 取消秒杀活动 | ADMIN/SELLER | `SeckillGoodsService.cancelSeckill` |
| `POST /api/v1/seckill/activities` | 创建秒杀场次 | ADMIN/SELLER | `SeckillActivityService.createActivity` |
| `GET /api/v1/seckill/activities` | 查询所有场次 | 公开 | `SeckillActivityService.listActivities` |
| `GET /api/v1/seckill/activities/{activityId}` | 场次详情 | 公开 | `SeckillActivityService.getActivityDetail` |
| `DELETE /api/v1/seckill/activities/{activityId}` | 删除场次 | ADMIN/SELLER | `SeckillActivityService.deleteActivity` |

### 1.4 跨模块依赖关系

#### Seckill 依赖的模块（出向依赖）

| 被依赖模块 | 依赖方式 | 用途 |
|---|---|---|
| **identity** | `UserApi` | SeckillOrderServiceImpl.payOrder 查用户信息 |
| **product** | `ProductApi`（过渡期保留 ProductService） | SeckillGoodsServiceImpl.createSeckill 查商品信息 + SeckillOrderServiceImpl 查商品名 |
| **payment** | `PaymentApi`（替代 `PaymentService`） | SeckillOrderServiceImpl.payOrder 钱包扣款 |
| **order** | `OrderStatus` 枚举（共享） | SeckillOrder.status 字段类型，SeckillOrderService 多个方法入参 |

#### 依赖 Seckill 的模块（入向依赖，14 处外部调用方）

| 调用方 | 调用方法 | 调用方模块 | 用途 |
|---|---|---|---|
| `WalletServiceImpl` | `SeckillOrderService.getWalletPaidOrdersByUser` | payment | 钱包交易记录查秒杀订单支付记录 |
| `StatsServiceImpl` | `SeckillOrderService.countAll/sumSalesAmount/countTodayOrders/selectOrderTrend/selectStatusDistribution` | stats | 仪表盘统计 |
| `StatsServiceImpl` | `SeckillGoodsService.countAll/countActive/countPending/countCompletedToday` | stats | 秒杀概览统计 |
| `StatsServiceImpl` | `SeckillOrderService.selectSeckillRanking` | stats | 秒杀排行榜 |
| `OrderQueryServiceImpl` | `SeckillOrderService.getSeckillOrderById/getSeckillOrdersForUnifiedList` | order | 统一订单列表/详情 |
| `ProductReviewServiceImpl` | `SeckillOrderService.hasUserPurchasedSeckill` | review | 商品评价权限校验 |
| `OrderController` | `SeckillOrderService.getOrderList/getOrderDetail/payOrder/cancelOrder/shipOrder/confirmOrder` | order | 订单 Controller 秒杀订单端点 |
| `OrderCancelConsumer` | `SeckillOrderService.timeoutCancel` | order | 订单取消 MQ 消费者触发秒杀订单超时取消 |
| `SystemServiceImpl` | `SeckillOrderService.countAll` | system | 系统仪表盘 |
| `AdminOrderServiceImpl` | `SeckillOrderService.selectAdminOrderPage` | order | 后台订单高级筛选 |

### 1.5 Entity / VO / DTO 字段

#### SeckillActivity Entity（t_seckill_activity）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `name` | `String` | 场次名称 |
| `startTime` | `LocalDateTime` | 开始时间 |
| `endTime` | `LocalDateTime` | 结束时间 |
| `status` | `Integer` | 0=待开始 1=进行中 2=已结束 |
| `perLimit` | `Integer` | 每人限购数量 |
| `description` | `String` | 场次描述 |
| `images` | `String` | 场次图片（JSON 数组字符串） |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### SeckillGoods Entity（t_seckill_goods）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `productId` | `Long` | 商品 ID |
| `activityId` | `Long` | 关联秒杀场次 ID（可为空表示独立活动） |
| `seckillPrice` | `BigDecimal` | 秒杀价 |
| `stockCount` | `Integer` | 总库存 |
| `availableCount` | `Integer` | 可用库存 |
| `startTime` | `LocalDateTime` | 开始时间 |
| `endTime` | `LocalDateTime` | 结束时间 |
| `status` | `SeckillStatus` | 状态（PENDING/ACTIVE/ENDED/CANCELLED） |
| `creatorId` | `Long` | 创建人 ID |
| `seckillName` | `String` | 活动名称 |
| `perLimit` | `Integer` | 每人限购 |
| `images` | `String` | 活动图片（JSON 数组字符串） |
| `description` | `String` | 活动描述 |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### SeckillOrder Entity（t_seckill_order）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `orderNo` | `String` | 订单号 |
| `userId` | `Long` | 用户 ID |
| `seckillId` | `Long` | 秒杀活动 ID |
| `productId` | `Long` | 商品 ID |
| `seckillPrice` | `BigDecimal` | 秒杀价 |
| `quantity` | `Integer` | 购买数量 |
| `totalAmount` | `BigDecimal` | 订单总金额 |
| `status` | `OrderStatus` | 订单状态（共享 order 模块枚举） |
| `shippingCompany` | `String` | 物流公司 |
| `shippingNo` | `String` | 快递单号 |
| `shipTime` | `LocalDateTime` | 发货时间 |
| `confirmTime` | `LocalDateTime` | 确认收货时间 |
| `payTime` | `LocalDateTime` | 支付时间 |
| `payExpireTime` | `LocalDateTime` | 支付过期时间 |
| `transactionId` | `String` | 支付交易号 |
| `payMethod` | `String` | 支付方式 |
| `cancelTime` | `LocalDateTime` | 取消时间 |
| `cancelReason` | `String` | 取消原因 |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### VO 字段（6 个，详见 [SECKILL-API-CONTRACT.md](SECKILL-API-CONTRACT.md) 第 4 节）

- `SeckillActivityVO`：id + name + startTime + endTime + status + perLimit + description + images(List<String>) + createTime + goodsList(List<SeckillGoodsVO>)
- `SeckillGoodsVO`：id + productId + productName + seckillName + seckillPrice + stockCount + availableCount + startTime + endTime + status(SeckillStatus) + perLimit + images(List<String>) + description + createTime
- `SeckillOrderVO`：id + orderNo + userId + seckillId + productId + seckillPrice + quantity + totalAmount + status(String) + statusDescription + 物流/支付/取消字段 + createTime + updateTime
- `SeckillOverviewVO`：activeCount + pendingCount + completedToday
- `SeckillRankingVO`：seckillId + productName + seckillPrice + salesCount + totalAmount
- `SeckillResultVO`：status(0/1/-1) + requestId + orderId + orderNo + totalAmount + payExpireTime

#### DTO 字段（2 个，保留在 dto 包，Controller 层请求 DTO）

- `SeckillCreateRequest`：productId + seckillName + seckillPrice + stockCount + startTime + endTime + perLimit + images + description（含 @NotNull/@NotBlank/@DecimalMin/@Min/@Size/@JsonFormat 校验注解）
- `SeckillActivityCreateRequest`：name + startTime + endTime + perLimit + description + images + goodsItems(List<ActivityGoodsItem>)，内嵌 `ActivityGoodsItem`（productId + seckillName + seckillPrice + stockCount + images + description）

### 1.6 Converter（MapStruct）

| Converter | 方法 | 说明 |
|---|---|---|
| `SeckillOrderConverter` | `SeckillOrderVO toVO(SeckillOrder entity)` | entity → VO，`@Mapping(target="status", ignore=true)` + `@AfterMapping enrichStatus` 处理枚举 → code + 中文描述，屏蔽 isDeleted |

### 1.7 MQ 消息字段

#### SeckillOrderMessage（异步下单消息）

| 字段 | 类型 | 说明 |
|---|---|---|
| `seckillId` | `Long` | 秒杀活动 ID |
| `userId` | `Long` | 用户 ID |
| `requestId` | `String` | 请求 ID（用于结果查询） |
| `timestamp` | `Long` | 时间戳 |

#### SeckillResultMessage（秒杀结果消息）

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | `Long` | 用户 ID |
| `seckillId` | `Long` | 秒杀活动 ID |
| `status` | `Integer` | 0=排队中 / 1=成功 / -1=失败 |
| `orderId` | `Long` | 订单 ID |
| `orderNo` | `String` | 订单号 |
| `totalAmount` | `BigDecimal` | 订单总金额 |

---

## 2. Seckill 目标结构（Pragmatic DDD + Modular Monolith）

### 2.1 目标包结构

```
com.seckill.mall.seckill/
├── package-info.java                                       # 模块根包
├── api/                                                     # API 契约层（对外暴露）
│   ├── package-info.java
│   ├── SeckillApi.java                                      # 秒杀下单 API（doSeckill + getSeckillResult）— 替代 SeckillService
│   ├── SeckillOrderApi.java                                 # 秒杀订单 API（17 方法）— 替代 SeckillOrderService
│   ├── SeckillActivityApi.java                              # 秒杀场次 API（4 方法）— 替代 SeckillActivityService
│   ├── SeckillGoodsApi.java                                 # 秒杀商品 API（11 方法）— 替代 SeckillGoodsService
│   ├── dto/
│   │   ├── package-info.java
│   │   ├── SeckillActivityDTO.java                          # 场次 DTO（替代 SeckillActivityVO 跨模块传递）
│   │   ├── SeckillGoodsDTO.java                             # 秒杀商品 DTO（替代 SeckillGoodsVO 跨模块传递）
│   │   ├── SeckillOrderDTO.java                             # 秒杀订单 DTO（替代 SeckillOrder Entity 跨模块传递）
│   │   ├── SeckillOverviewDTO.java                          # 概览 DTO（替代 SeckillOverviewVO）
│   │   ├── SeckillRankingDTO.java                           # 排行榜 DTO（替代 SeckillRankingVO）
│   │   └── SeckillResultDTO.java                            # 秒杀结果 DTO（替代 SeckillResultVO 跨模块传递）
│   ├── command/
│   │   ├── package-info.java
│   │   ├── SeckillCommand.java                              # 执行秒杀命令（seckillId + seckillToken + userId）
│   │   ├── SeckillResultQuery.java                          # 查询秒杀结果命令（seckillId + requestId + userId）
│   │   ├── CreateActivityCommand.java                       # 创建场次命令
│   │   ├── CreateSeckillGoodsCommand.java                   # 创建秒杀商品命令
│   │   ├── UpdateSeckillGoodsCommand.java                   # 编辑秒杀商品命令
│   │   ├── CancelSeckillCommand.java                        # 取消秒杀命令
│   │   ├── PaySeckillOrderCommand.java                      # 支付秒杀订单命令（userId + orderId + payMethod）
│   │   ├── CancelSeckillOrderCommand.java                   # 取消秒杀订单命令
│   │   ├── ShipSeckillOrderCommand.java                     # 发货命令（userId + orderId + shippingCompany + shippingNo）
│   │   └── ConfirmSeckillOrderCommand.java                  # 确认收货命令
│   ├── query/
│   │   ├── package-info.java
│   │   ├── SeckillGoodsQuery.java                           # 秒杀商品分页查询（status + categoryId + pageNum + pageSize）
│   │   ├── SeckillOrderQuery.java                           # 秒杀订单分页查询（userId + status + pageNum + pageSize）
│   │   ├── SeckillOrderDetailQuery.java                     # 秒杀订单详情查询（userId + orderId）
│   │   └── SeckillRankingQuery.java                         # 排行榜查询（statuses + limit）
│   └── result/
│       ├── package-info.java
│       └── SeckillResult.java                               # 秒杀下单结果（含 orderId + orderNo + totalAmount + payExpireTime）
├── application/                                             # 应用层
│   ├── package-info.java
│   ├── SeckillApplicationService.java                       # 实现 SeckillApi，委托 SeckillService
│   ├── SeckillOrderApplicationService.java                  # 实现 SeckillOrderApi，委托 SeckillOrderService
│   ├── SeckillActivityApplicationService.java               # 实现 SeckillActivityApi，委托 SeckillActivityService
│   ├── SeckillGoodsApplicationService.java                  # 实现 SeckillGoodsApi，委托 SeckillGoodsService
│   ├── SeckillTokenService.java                             # 从 service 包迁入（内部令牌生成/校验）
│   ├── SeckillDbStrategy.java                               # 从 service 包迁入（内部 DB 降级策略）
│   └── facade/
│       ├── package-info.java
│       └── SeckillApiConverter.java                         # VO/Entity ↔ DTO/Command 转换
├── domain/                                                  # 领域层
│   ├── package-info.java
│   ├── SeckillStatus.java                                   # 从 entity.enums 迁入
│   └── SeckillInventoryPort.java                            # 从 service 包迁入（库存端口接口）
├── infrastructure/                                          # 基础设施层
│   ├── package-info.java
│   ├── mapper/
│   │   ├── package-info.java
│   │   ├── SeckillActivityMapper.java                       # 从 mapper 包迁入
│   │   ├── SeckillGoodsMapper.java                          # 从 mapper 包迁入
│   │   └── SeckillOrderMapper.java                          # 从 mapper 包迁入
│   ├── entity/
│   │   ├── package-info.java
│   │   ├── SeckillActivity.java                             # 从 entity 包迁入
│   │   ├── SeckillGoods.java                                # 从 entity 包迁入
│   │   └── SeckillOrder.java                                # 从 entity 包迁入
│   ├── persistence/
│   │   ├── package-info.java
│   │   └── SeckillInventoryPortImpl.java                    # 从 service.impl 迁入（库存端口实现）
│   ├── mq/
│   │   ├── package-info.java
│   │   ├── producer/
│   │   │   ├── package-info.java
│   │   │   └── SeckillOrderProducer.java                    # 从 mq.producer 迁入
│   │   ├── consumer/
│   │   │   ├── package-info.java
│   │   │   └── SeckillOrderConsumer.java                    # 从 mq.consumer 迁入
│   │   └── message/
│   │       ├── package-info.java
│   │       ├── SeckillOrderMessage.java                     # 从 mq.message 迁入
│   │       └── SeckillResultMessage.java                    # 从 mq.message 迁入
│   ├── scheduler/
│   │   ├── package-info.java
│   │   └── SeckillStatusScheduler.java                      # 从 scheduler 包迁入
│   └── cache/
│       ├── package-info.java
│       ├── SeckillLuaService.java                           # 从 cache 包迁入
│       └── SeckillBloomInitializer.java                     # 从 cache 包迁入
└── interfaces/                                              # 接口层
    ├── package-info.java
    ├── vo/
    │   ├── package-info.java
    │   ├── SeckillActivityVO.java                            # 从 vo 包迁入（前端展示用）
    │   ├── SeckillGoodsVO.java                              # 从 vo 包迁入
    │   ├── SeckillOrderVO.java                              # 从 vo 包迁入
    │   ├── SeckillOverviewVO.java                            # 从 vo 包迁入
    │   ├── SeckillRankingVO.java                            # 从 vo 包迁入
    │   └── SeckillResultVO.java                             # 从 vo 包迁入
    └── web/
        ├── package-info.java
        └── SeckillController.java                           # 从 controller 包迁入
```

### 2.2 分层依赖规则（目标）

```
interfaces ──→ application ──→ api
                    │
                    └──→ service（旧层，Strangler 过渡期）
                              │
                              └──→ infrastructure.mapper ──→ infrastructure.entity
                              └──→ infrastructure.cache / mq / scheduler

api ──✗──→ infrastructure     （API 不依赖基础设施）
application ──✗──→ mapper     （Application 不直接依赖 Mapper）
interfaces ──✗──→ mapper      （Interfaces 不直接依赖 Mapper）
```

### 2.3 与其他模块的关系（目标）

| 关系 | 模块 | 依赖方式 |
|---|---|---|
| Seckill → Identity | `UserApi` | 秒杀订单支付查用户信息 |
| Seckill → Product | `ProductApi`（过渡期保留 ProductService） | 创建秒杀商品查商品信息 |
| Seckill → Payment | `PaymentApi`（替代 `PaymentService`） | 秒杀订单支付扣款 |
| Seckill → Order | `OrderStatus` 枚举（共享） | 秒杀订单状态字段 |
| Payment → Seckill | `SeckillOrderApi`（替代 `SeckillOrderService`） | 钱包交易记录查秒杀订单支付记录 |
| Order → Seckill | `SeckillOrderApi`（替代 `SeckillOrderService`） | 统一订单列表/详情/支付/取消/发货/确认 |
| Stats → Seckill | `SeckillOrderApi` + `SeckillGoodsApi` | 仪表盘统计 + 概览 + 排行榜 |
| Review → Seckill | `SeckillOrderApi` | 商品评价权限校验 |
| System → Seckill | `SeckillOrderApi` | 系统仪表盘 |

---

## 3. 文件迁移路径

### 3.1 移动文件（git mv）

| 源路径 | 目标路径 | 阶段 |
|---|---|---|
| `entity/SeckillActivity.java` | `seckill/infrastructure/entity/SeckillActivity.java` | Phase SK.3 |
| `entity/SeckillGoods.java` | `seckill/infrastructure/entity/SeckillGoods.java` | Phase SK.3 |
| `entity/SeckillOrder.java` | `seckill/infrastructure/entity/SeckillOrder.java` | Phase SK.3 |
| `mapper/SeckillActivityMapper.java` | `seckill/infrastructure/mapper/SeckillActivityMapper.java` | Phase SK.3 |
| `mapper/SeckillGoodsMapper.java` | `seckill/infrastructure/mapper/SeckillGoodsMapper.java` | Phase SK.3 |
| `mapper/SeckillOrderMapper.java` | `seckill/infrastructure/mapper/SeckillOrderMapper.java` | Phase SK.3 |
| `entity/enums/SeckillStatus.java` | `seckill/domain/SeckillStatus.java` | Phase SK.3 |
| `service/SeckillInventoryPort.java` | `seckill/domain/SeckillInventoryPort.java` | Phase SK.3 |
| `service/impl/SeckillInventoryPortImpl.java` | `seckill/infrastructure/persistence/SeckillInventoryPortImpl.java` | Phase SK.3 |
| `service/SeckillTokenService.java` | `seckill/application/SeckillTokenService.java` | Phase SK.4 |
| `service/SeckillDbStrategy.java` | `seckill/application/SeckillDbStrategy.java` | Phase SK.4 |
| `mq/producer/SeckillOrderProducer.java` | `seckill/infrastructure/mq/producer/SeckillOrderProducer.java` | Phase SK.3 |
| `mq/consumer/SeckillOrderConsumer.java` | `seckill/infrastructure/mq/consumer/SeckillOrderConsumer.java` | Phase SK.3 |
| `mq/message/SeckillOrderMessage.java` | `seckill/infrastructure/mq/message/SeckillOrderMessage.java` | Phase SK.3 |
| `mq/message/SeckillResultMessage.java` | `seckill/infrastructure/mq/message/SeckillResultMessage.java` | Phase SK.3 |
| `scheduler/SeckillStatusScheduler.java` | `seckill/infrastructure/scheduler/SeckillStatusScheduler.java` | Phase SK.3 |
| `cache/SeckillLuaService.java` | `seckill/infrastructure/cache/SeckillLuaService.java` | Phase SK.3 |
| `cache/SeckillBloomInitializer.java` | `seckill/infrastructure/cache/SeckillBloomInitializer.java` | Phase SK.3 |
| `vo/SeckillActivityVO.java` | `seckill/interfaces/vo/SeckillActivityVO.java` | Phase SK.4 |
| `vo/SeckillGoodsVO.java` | `seckill/interfaces/vo/SeckillGoodsVO.java` | Phase SK.4 |
| `vo/SeckillOrderVO.java` | `seckill/interfaces/vo/SeckillOrderVO.java` | Phase SK.4 |
| `vo/SeckillOverviewVO.java` | `seckill/interfaces/vo/SeckillOverviewVO.java` | Phase SK.4 |
| `vo/SeckillRankingVO.java` | `seckill/interfaces/vo/SeckillRankingVO.java` | Phase SK.4 |
| `vo/SeckillResultVO.java` | `seckill/interfaces/vo/SeckillResultVO.java` | Phase SK.4 |
| `controller/SeckillController.java` | `seckill/interfaces/web/SeckillController.java` | Phase SK.4 |

### 3.2 新增文件

| 目标路径 | 阶段 |
|---|---|
| `seckill/package-info.java` 及所有子包 `package-info.java`（共 22 个） | Phase SK.0 |
| `seckill/api/SeckillApi.java` | Phase SK.2 |
| `seckill/api/SeckillOrderApi.java` | Phase SK.2 |
| `seckill/api/SeckillActivityApi.java` | Phase SK.2 |
| `seckill/api/SeckillGoodsApi.java` | Phase SK.2 |
| `seckill/api/dto/{SeckillActivityDTO,SeckillGoodsDTO,SeckillOrderDTO,SeckillOverviewDTO,SeckillRankingDTO,SeckillResultDTO}.java` | Phase SK.2 |
| `seckill/api/command/{SeckillCommand,SeckillResultQuery,CreateActivityCommand,CreateSeckillGoodsCommand,UpdateSeckillGoodsCommand,CancelSeckillCommand,PaySeckillOrderCommand,CancelSeckillOrderCommand,ShipSeckillOrderCommand,ConfirmSeckillOrderCommand}.java` | Phase SK.2 |
| `seckill/api/query/{SeckillGoodsQuery,SeckillOrderQuery,SeckillOrderDetailQuery,SeckillRankingQuery}.java` | Phase SK.2 |
| `seckill/api/result/SeckillResult.java` | Phase SK.2 |
| `seckill/application/SeckillApplicationService.java` | Phase SK.4 |
| `seckill/application/SeckillOrderApplicationService.java` | Phase SK.4 |
| `seckill/application/SeckillActivityApplicationService.java` | Phase SK.4 |
| `seckill/application/SeckillGoodsApplicationService.java` | Phase SK.4 |
| `seckill/application/facade/SeckillApiConverter.java` | Phase SK.4 |

### 3.3 保留不动的文件

| 文件 | 原因 |
|---|---|
| `service/SeckillService.java` | Strangler 过渡期保留，SeckillApplicationService 委托它 |
| `service/impl/SeckillServiceImpl.java` | 同上 |
| `service/SeckillOrderService.java` | Strangler 过渡期保留，SeckillOrderApplicationService 委托它 |
| `service/impl/SeckillOrderServiceImpl.java` | 同上 |
| `service/SeckillActivityService.java` | Strangler 过渡期保留，SeckillActivityApplicationService 委托它 |
| `service/impl/SeckillActivityServiceImpl.java` | 同上 |
| `service/SeckillGoodsService.java` | Strangler 过渡期保留，SeckillGoodsApplicationService 委托它 |
| `service/impl/SeckillGoodsServiceImpl.java` | 同上 |
| `dto/SeckillCreateRequest.java` | Controller 层请求 DTO，含 Bean Validation 注解，保留在 dto 包 |
| `dto/SeckillActivityCreateRequest.java` | Controller 层请求 DTO，含 Bean Validation 注解，保留在 dto 包 |
| `converter/SeckillOrderConverter.java` | MapStruct 转换器，被 SeckillOrderServiceImpl 使用，保留原位（过渡期） |

### 3.4 后续可删除的文件（Strangler 完成后）

| 文件 | 删除时机 |
|---|---|
| `service/SeckillService.java` + `service/impl/SeckillServiceImpl.java` | 业务逻辑完全迁入 seckill.application 后 |
| `service/SeckillOrderService.java` + `service/impl/SeckillOrderServiceImpl.java` | 同上 |
| `service/SeckillActivityService.java` + `service/impl/SeckillActivityServiceImpl.java` | 同上 |
| `service/SeckillGoodsService.java` + `service/impl/SeckillGoodsServiceImpl.java` | 同上 |
| `converter/SeckillOrderConverter.java` | SeckillOrderServiceImpl 删除后，转换逻辑迁入 SeckillApiConverter |

---

## 4. API 边界（SeckillApi + SeckillOrderApi + SeckillActivityApi + SeckillGoodsApi）

### 4.1 设计原则

1. **方法用业务语言命名**，禁止 CRUD 命名
2. **入参用 Command/Query 对象**，禁止裸露多参数
3. **出参用 Result/DTO/Snapshot**，禁止暴露 Entity/Mapper/PO
4. **异常通过 `BusinessException(ErrorCode)` 抛出**，不泄露堆栈
5. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

### 4.2 SeckillApi 方法清单（秒杀下单 API）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `executeSeckill` | `SeckillResult executeSeckill(SeckillCommand command)` | `SeckillService.doSeckill` |
| `getSeckillResult` | `SeckillResultDTO getSeckillResult(SeckillResultQuery query)` | `SeckillService.getSeckillResult` |

> **关键变化**：`SeckillService.doSeckill(Long, String)` 裸参数 → `SeckillApi.executeSeckill(SeckillCommand)`。Command 含 `seckillId`/`seckillToken`/`userId` 三字段（userId 由 Controller 注入，Service 内部不再通过 securityUtils 获取）。返回 `SeckillResult`（替代 `SeckillResultVO` 跨模块传递）。

### 4.3 SeckillOrderApi 方法清单（秒杀订单 API，17 方法）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `createSeckillOrder` | `SeckillOrderDTO createSeckillOrder(CreateSeckillOrderCommand command)` | `SeckillOrderService.createSeckillOrder` |
| `deleteSeckillOrderPhysically` | `void deleteSeckillOrderPhysically(Long orderId)` | `SeckillOrderService.deleteSeckillOrderPhysically` |
| `getOrderList` | `PageResult<SeckillOrderDTO> getOrderList(SeckillOrderQuery query)` | `SeckillOrderService.getOrderList` |
| `getOrderDetail` | `SeckillOrderDTO getOrderDetail(SeckillOrderDetailQuery query)` | `SeckillOrderService.getOrderDetail` |
| `payOrder` | `SeckillOrderDTO payOrder(PaySeckillOrderCommand command)` | `SeckillOrderService.payOrder` |
| `cancelOrder` | `SeckillOrderDTO cancelOrder(CancelSeckillOrderCommand command)` | `SeckillOrderService.cancelOrder` |
| `getOrderStatus` | `String getOrderStatus(SeckillOrderDetailQuery query)` | `SeckillOrderService.getOrderStatus` |
| `timeoutCancel` | `boolean timeoutCancel(Long orderId)` | `SeckillOrderService.timeoutCancel` |
| `shipOrder` | `void shipOrder(ShipSeckillOrderCommand command)` | `SeckillOrderService.shipOrder` |
| `confirmOrder` | `void confirmOrder(ConfirmSeckillOrderCommand command)` | `SeckillOrderService.confirmOrder` |
| `getSeckillOrderById` | `SeckillOrderDTO getSeckillOrderById(Long orderId)` | `SeckillOrderService.getSeckillOrderById` |
| `getSeckillOrdersForUnifiedList` | `List<SeckillOrderDTO> getSeckillOrdersForUnifiedList(SeckillOrderQuery query)` | `SeckillOrderService.getSeckillOrdersForUnifiedList` |
| `logicalDeleteSeckillOrder` | `boolean logicalDeleteSeckillOrder(Long orderId)` | `SeckillOrderService.logicalDeleteSeckillOrder` |
| `hasUserPurchasedSeckill` | `boolean hasUserPurchasedSeckill(Long userId, Long productId)` | `SeckillOrderService.hasUserPurchasedSeckill` |
| `getWalletPaidOrdersByUser` | `List<SeckillOrderDTO> getWalletPaidOrdersByUser(Long userId)` | `SeckillOrderService.getWalletPaidOrdersByUser` |
| `countAll` | `long countAll()` | `SeckillOrderService.countAll` |
| `sumSalesAmount` | `BigDecimal sumSalesAmount(List<OrderStatus> statuses)` | `SeckillOrderService.sumSalesAmount` |
| `countTodayOrders` | `Long countTodayOrders(LocalDate today)` | `SeckillOrderService.countTodayOrders` |
| `selectOrderTrend` | `List<Map<String, Object>> selectOrderTrend(...)` | `SeckillOrderService.selectOrderTrend` |
| `selectSeckillRanking` | `List<SeckillRankingDTO> selectSeckillRanking(SeckillRankingQuery query)` | `SeckillOrderService.selectSeckillRanking` |
| `selectStatusDistribution` | `List<Map<String, Object>> selectStatusDistribution(...)` | `SeckillOrderService.selectStatusDistribution` |
| `selectAdminOrderPage` | `IPage<AdminOrderVO> selectAdminOrderPage(...)` | `SeckillOrderService.selectAdminOrderPage` |

> **关键变化**：
> - `createSeckillOrder` / `getSeckillOrderById` / `getSeckillOrdersForUnifiedList` / `getWalletPaidOrdersByUser` 旧方法返回 `SeckillOrder`（Entity）→ 新 API 返回 `SeckillOrderDTO`，消除 Entity 泄漏。`OrderServiceImpl` / `WalletServiceImpl` 适配新返回类型。
> - 所有裸参数方法改为 Command/Query 对象入参。
> - `selectSeckillRanking` 返回 `List<SeckillRankingVO>` → `List<SeckillRankingDTO>`。

### 4.4 SeckillActivityApi 方法清单（秒杀场次 API，4 方法）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `createActivity` | `SeckillActivityDTO createActivity(CreateActivityCommand command)` | `SeckillActivityService.createActivity` |
| `listActivities` | `List<SeckillActivityDTO> listActivities()` | `SeckillActivityService.listActivities` |
| `getActivityDetail` | `SeckillActivityDTO getActivityDetail(Long activityId)` | `SeckillActivityService.getActivityDetail` |
| `deleteActivity` | `void deleteActivity(Long activityId)` | `SeckillActivityService.deleteActivity` |

> **关键变化**：返回 `SeckillActivityVO` → `SeckillActivityDTO`。Controller 层通过 `SeckillApiConverter.toVO` 转回 VO 保持前端契约。

### 4.5 SeckillGoodsApi 方法清单（秒杀商品 API，11 方法）

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `listSeckill` | `PageResult<SeckillGoodsDTO> listSeckill(SeckillGoodsQuery query)` | `SeckillGoodsService.listSeckill` |
| `getSeckillDetail` | `SeckillGoodsDTO getSeckillDetail(Long seckillId)` | `SeckillGoodsService.getSeckillDetail` |
| `createSeckill` | `SeckillGoodsDTO createSeckill(CreateSeckillGoodsCommand command)` | `SeckillGoodsService.createSeckill` |
| `updateSeckill` | `SeckillGoodsDTO updateSeckill(UpdateSeckillGoodsCommand command)` | `SeckillGoodsService.updateSeckill` |
| `cancelSeckill` | `void cancelSeckill(Long id)` | `SeckillGoodsService.cancelSeckill` |
| `getStock` | `Integer getStock(Long seckillId)` | `SeckillGoodsService.getStock` |
| `preheatSeckill` | `void preheatSeckill(Long seckillId)` | `SeckillGoodsService.preheatSeckill` |
| `countAll` | `long countAll()` | `SeckillGoodsService.countAll` |
| `countActive` | `long countActive()` | `SeckillGoodsService.countActive` |
| `countPending` | `long countPending()` | `SeckillGoodsService.countPending` |
| `countCompletedToday` | `long countCompletedToday()` | `SeckillGoodsService.countCompletedToday` |

> **关键变化**：返回 `SeckillGoodsVO` → `SeckillGoodsDTO`。所有裸参数方法改为 Command/Query 对象入参。

### 4.6 DTO/Command/Query/Result 定义

详见 [SECKILL-API-CONTRACT.md](SECKILL-API-CONTRACT.md)。

---

## 5. 迁移步骤（Phase SK）

> 对应 task 57-62，共 6 个子任务（Phase SK.0 + SK.2 + SK.3 + SK.4 + SK.5 + SK.6）。

### Phase SK.0：创建包结构（task 57）

- 创建 `seckill/` 及所有子包（共 22 个 package-info.java）
- 每个包创建 `package-info.java`
- **不修改任何现有代码**

### Phase SK.2：创建 API 层（task 58）

- 创建 4 个 API 接口（`SeckillApi` + `SeckillOrderApi` + `SeckillActivityApi` + `SeckillGoodsApi`）
- 创建 6 个 DTO（`SeckillActivityDTO`、`SeckillGoodsDTO`、`SeckillOrderDTO`、`SeckillOverviewDTO`、`SeckillRankingDTO`、`SeckillResultDTO`）
- 创建 10 个 Command（`SeckillCommand`、`SeckillResultQuery`、`CreateActivityCommand`、`CreateSeckillGoodsCommand`、`UpdateSeckillGoodsCommand`、`CancelSeckillCommand`、`PaySeckillOrderCommand`、`CancelSeckillOrderCommand`、`ShipSeckillOrderCommand`、`ConfirmSeckillOrderCommand`）
- 创建 4 个 Query（`SeckillGoodsQuery`、`SeckillOrderQuery`、`SeckillOrderDetailQuery`、`SeckillRankingQuery`）
- 创建 1 个 Result（`SeckillResult`）
- **不修改任何现有代码**

### Phase SK.3：迁移持久化层 + MQ/Scheduler/Cache（task 59）

- `git mv` 3 个 Entity → `seckill/infrastructure/entity/`
- `git mv` 3 个 Mapper → `seckill/infrastructure/mapper/`
- `git mv SeckillStatus.java` → `seckill/domain/`
- `git mv SeckillInventoryPort.java` → `seckill/domain/`
- `git mv SeckillInventoryPortImpl.java` → `seckill/infrastructure/persistence/`
- `git mv` 2 个 MQ Producer/Consumer → `seckill/infrastructure/mq/producer|consumer/`
- `git mv` 2 个 MQ Message → `seckill/infrastructure/mq/message/`
- `git mv SeckillStatusScheduler.java` → `seckill/infrastructure/scheduler/`
- `git mv` 2 个 Cache → `seckill/infrastructure/cache/`
- 更新所有 package 声明
- 更新所有 import（SeckillServiceImpl、SeckillOrderServiceImpl、SeckillActivityServiceImpl、SeckillGoodsServiceImpl、SeckillTokenService、SeckillDbStrategy、SeckillInventoryPortImpl、SeckillOrderProducer、SeckillOrderConsumer、SeckillStatusScheduler、SeckillLuaService、SeckillBloomInitializer、SeckillOrderConverter 等引用上述类的所有文件）
- **回归测试**：mvn test

### Phase SK.4：创建 Application 层 + 移动 Controller/VO + 内部 Service 归位（task 60）

- 创建 `SeckillApplicationService implements SeckillApi`，委托 `SeckillService`
- 创建 `SeckillOrderApplicationService implements SeckillOrderApi`，委托 `SeckillOrderService`
- 创建 `SeckillActivityApplicationService implements SeckillActivityApi`，委托 `SeckillActivityService`
- 创建 `SeckillGoodsApplicationService implements SeckillGoodsApi`，委托 `SeckillGoodsService`
- 创建 `SeckillApiConverter`：VO ↔ DTO、Command → 裸参数 转换
- `git mv SeckillTokenService.java` → `seckill/application/`（内部令牌服务归位）
- `git mv SeckillDbStrategy.java` → `seckill/application/`（内部 DB 策略归位）
- `git mv` 6 个 VO → `seckill/interfaces/vo/`
- `git mv SeckillController.java` → `seckill/interfaces/web/`
- 更新 package 声明与所有 import
- 切换 Controller 注入 4 个 API 替代旧 4 个 Service，调用 API 方法后通过 Converter 转回 VO
- **不删除旧 4 个 Service + 4 个 ServiceImpl**（Strangler 过渡）
- **回归测试**：mvn test

### Phase SK.5：迁移外部调用方（task 61）

- `WalletServiceImpl`：`SeckillOrderService` → `SeckillOrderApi`
  - `seckillOrderService.getWalletPaidOrdersByUser(userId)` → `seckillOrderApi.getWalletPaidOrdersByUser(userId)`，返回 `List<SeckillOrder>` → `List<SeckillOrderDTO>`，适配字段访问
- `StatsServiceImpl`：`SeckillOrderService` + `SeckillGoodsService` → `SeckillOrderApi` + `SeckillGoodsApi`
  - 统计方法全部切换到 API，`selectSeckillRanking` 返回 `List<SeckillRankingVO>` → `List<SeckillRankingDTO>`
- `OrderQueryServiceImpl`：`SeckillOrderService` → `SeckillOrderApi`
  - `getSeckillOrderById` / `getSeckillOrdersForUnifiedList` 返回 `List<SeckillOrder>` → `List<SeckillOrderDTO>`，适配字段访问
- `ProductReviewServiceImpl`：`SeckillOrderService` → `SeckillOrderApi`
  - `hasUserPurchasedSeckill` 切换
- `OrderController`：`SeckillOrderService` → `SeckillOrderApi`
  - 6 个订单端点方法切换，返回 `SeckillOrderVO` → `SeckillOrderDTO` 后通过 Converter 转回 VO
- `OrderCancelConsumer`：`SeckillOrderService` → `SeckillOrderApi`
  - `timeoutCancel` 切换
- `SystemServiceImpl`：`SeckillOrderService` → `SeckillOrderApi`
  - `countAll` 切换
- `AdminOrderServiceImpl`：`SeckillOrderService` → `SeckillOrderApi`
  - `selectAdminOrderPage` 切换
- 更新测试中的 mock 声明（`WalletServiceTest`、`StatsServiceTest`、`OrderQueryServiceTest`、`ProductReviewServiceTest`、`OrderControllerTest`、`OrderCancelConsumerTest`、`SystemServiceTest`、`AdminOrderServiceTest` 等）
- **回归测试**：mvn test

### Phase SK.6：ArchUnit 规则（task 62）

新增 3 条 seckill 模块包边界规则：

| 规则 | 内容 |
|---|---|
| `seckill_api_should_not_depend_on_infrastructure` | `seckill.api` 不应依赖 `seckill.infrastructure` |
| `seckill_application_should_not_depend_on_mapper` | `seckill.application` 不应直接依赖 `seckill.infrastructure.mapper` |
| `seckill_interfaces_should_not_depend_on_mapper` | `seckill.interfaces` 不应直接依赖 `seckill.infrastructure.mapper` |

**回归标准**：mvn test Failures ≤ 1, Errors ≤ 19, ArchUnit 全绿（42 条 = 39 + 3）。

---

## 6. 风险点与缓解

### 6.1 SeckillOrder Entity 被 order / payment 模块跨模块引用（Entity 泄漏）

**风险**：`SeckillOrderService` 的 4 个方法（`createSeckillOrder` / `getSeckillOrderById` / `getSeckillOrdersForUnifiedList` / `getWalletPaidOrdersByUser`）返回 `SeckillOrder` Entity，被 `OrderServiceImpl`、`WalletServiceImpl`、`SeckillDbStrategy` 跨模块/内部调用并访问字段。切换到 `SeckillOrderApi` 返回 `SeckillOrderDTO` 后需适配所有字段访问。

**缓解**：`SeckillOrderDTO` 包含 `id`/`orderNo`/`userId`/`seckillId`/`productId`/`seckillPrice`/`quantity`/`totalAmount`/`status`/`payMethod`/`payTime`/`shippingCompany`/`shippingNo`/`shipTime`/`confirmTime`/`payExpireTime`/`transactionId`/`cancelTime`/`cancelReason`/`createTime`/`updateTime` 等所有外部访问字段。`OrderServiceImpl` / `WalletServiceImpl` 调用 API 后通过 `SeckillApiConverter.toVO` 或直接访问 DTO 字段，逻辑完全等价。

> **注意**：`SeckillDbStrategy` 是 seckill 模块内部组件（归位 `seckill.application`），其调用 `SeckillOrderService.createSeckillOrder` 在过渡期保留旧 Service 调用，不切换到 API（避免内部循环依赖）。

### 6.2 14 处外部调用方切换风险

**风险**：14 处外部调用方分布在 7 个模块（payment / stats / order / review / system / order Controller / order MQ），切换面广，任一处遗漏或适配错误都会导致编译失败或运行时 NPE。

**缓解**：
- Phase SK.5 按模块逐一切换，每切换一个模块跑一次 mvn test 验证
- 所有切换遵循统一模式：旧 Service 方法 → 新 API 方法 + Converter 转换
- 测试 mock 声明同步更新，避免 `@MockBean` 类型不匹配

### 6.3 MQ Producer/Consumer 归位风险

**风险**：`SeckillOrderProducer` / `SeckillOrderConsumer` / `SeckillOrderMessage` / `SeckillResultMessage` 迁入 `seckill/infrastructure/mq/` 后，Spring Boot 自动扫描 `@Component` / `@RabbitListener` 注解需保证新包在 `@ComponentScan` 范围内。RabbitMQ 队列/交换机配置与 package 无关，但消息序列化（`Serializable`）依赖类全限定名，迁移后旧消息无法反序列化。

**缓解**：
- 迁移前确认 RabbitMQ 中无未消费的旧消息（或清空队列）
- `SeckillOrderMessage` / `SeckillResultMessage` 实现 `Serializable`，`serialVersionUID` 保持不变
- Spring Boot `@ComponentScan` 默认扫描主类所在包及子包，`com.seckill.mall.seckill` 在 `com.seckill.mall` 下，自动扫描生效
- 迁移后立即跑 mvn test 验证 MQ 集成测试

### 6.4 SeckillStatusScheduler 归位风险

**风险**：`SeckillStatusScheduler` 含 2 个 `@Scheduled` 定时任务（状态更新 + 库存对账），迁入 `seckill/infrastructure/scheduler/` 后需保证 `@EnableScheduling` 仍生效。`@EnableScheduling` 注解在主类上，与 package 无关，但需确认新包被组件扫描覆盖。

**缓解**：仅移动文件位置，不修改 `@Scheduled` / `@Component` / `@EnableScheduling` 注解。迁移后跑 mvn test 验证定时任务仍触发（通过日志或集成测试）。

### 6.5 SeckillLuaService + SeckillBloomInitializer 归位风险

**风险**：
- `SeckillLuaService` 在 `@PostConstruct` 中加载 `lua/seckill_deduct.lua` + `lua/seckill_rollback.lua`，ClassPathResource 与 package 无关，但需确认 Lua 脚本仍在 `resources/lua/` 目录
- `SeckillBloomInitializer` 实现 `ApplicationRunner`，启动时初始化布隆过滤器，迁移后需保证启动顺序正确（在 RedissonClient 就绪后）

**缓解**：仅移动 Java 文件位置，不动 `resources/lua/` 下的 Lua 脚本。`@Component` + `@PostConstruct` + `ApplicationRunner` 与 package 无关，Spring Boot 自动扫描生效。迁移后跑 mvn test 验证秒杀下单流程（依赖 Lua 脚本）和启动日志（布隆过滤器初始化）。

### 6.6 SeckillTokenService + SeckillDbStrategy 归位为 application 层内部组件

**风险**：`SeckillTokenService` 和 `SeckillDbStrategy` 当前在 `service` 包，被 `SeckillController` 和 `SeckillServiceImpl` 调用。归位到 `seckill/application/` 后，`SeckillController`（在 `seckill/interfaces/web/`）需更新 import。`SeckillDbStrategy` 依赖 `SeckillOrderService`（过渡期保留旧 Service），形成 `application → service` 依赖，不违反模块内包边界。

**缓解**：Phase SK.4 将 `SeckillTokenService` / `SeckillDbStrategy` 与 4 个 ApplicationService 一起迁入 `seckill/application/`，同步更新 `SeckillController` import。`SeckillController` 注入 `SeckillTokenService`（内部组件）+ 4 个 API（对外契约），符合 interfaces → application 的分层规则。

### 6.7 SeckillInventoryPort + SeckillInventoryPortImpl 端口/适配器归位

**风险**：`SeckillInventoryPort`（接口）在 `service` 包，`SeckillInventoryPortImpl`（实现）在 `service.impl` 包。按 DDD 端口/适配器模式，接口应归位 `seckill/domain/`，实现归位 `seckill/infrastructure/persistence/`。迁移后需更新所有 import。

**缓解**：Phase SK.3 同步迁移接口和实现，更新 `SeckillServiceImpl` / `SeckillOrderServiceImpl` 等引用方的 import。Spring Boot `@Component` 自动扫描生效，DI 注入不受影响。

### 6.8 SeckillOrder.status 字段依赖 order 模块的 OrderStatus 枚举

**风险**：`SeckillOrder` Entity 的 `status` 字段类型为 `com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus`（order 模块枚举），形成 seckill → order 的编译期依赖。`SeckillOrderService` 多个方法入参/出参也引用 `OrderStatus`。这是历史遗留的跨模块共享枚举，不在本次迁移范围。

**缓解**：保留 `SeckillOrder.status` 为 `OrderStatus` 类型，`SeckillOrderDTO.status` 用 `String`（code）避免 DTO 层依赖 order 模块。`SeckillApiConverter.toDTO` 将 `OrderStatus` → `String`（`getCode()`），`SeckillApiConverter.toEntity`（如需）将 `String` → `OrderStatus`（`OrderStatus.fromCode`）。未来可考虑将 `OrderStatus` 提取为共享内核（shared kernel），但不在本次迁移范围。

### 6.9 SeckillOrderConverter（MapStruct）位置

**风险**：`SeckillOrderConverter` 在 `converter/` 包，被 `SeckillOrderServiceImpl` 使用。迁移 `SeckillOrder` Entity 和 `SeckillOrderVO` 后 Converter 的 import 需更新。MapStruct 编译时生成实现类，全限定名变化可能导致 Spring Bean 注入失败。

**缓解**：本次迁移**不动** `SeckillOrderConverter`，保留在 `converter/` 包（过渡期）。仅更新其 import 指向新的 `seckill/infrastructure/entity/SeckillOrder` 和 `seckill/interfaces/vo/SeckillOrderVO`。MapStruct `@Mapper(componentModel = "spring")` 生成的 Bean 仍被 Spring 扫描，DI 不受影响。待 Strangler 完成后，转换逻辑迁入 `SeckillApiConverter`，再删除 `SeckillOrderConverter`。

### 6.10 6 个 VO 被前端接口契约锁定

**风险**：6 个 VO 是 `/api/v1/seckill/*` 端点的返回体，前端依赖其字段。若切换为 DTO 可能破坏前端契约。`SeckillGoodsVO.status` 字段类型为 `SeckillStatus` 枚举（带 `@JsonValue`），`SeckillOrderVO.status` 为 `String`，序列化行为不同。

**缓解**：**Controller 仍返回 VO**（保留前端契约），内部调用 API 拿到 DTO 后通过 `SeckillApiConverter.toVO` 转回 VO。6 个 VO 移极迁入 `seckill/interfaces/vo/` 但保留所有字段和注解。`SeckillStatus` 枚举的 `@JsonValue` / `@JsonCreator` 与 package 无关，Jackson 序列化行为保持不变。

### 6.11 SeckillCreateRequest / SeckillActivityCreateRequest 位置

**风险**：2 个 Request DTO 在 `dto/` 包，含 Bean Validation 注解，被 `SeckillController` 使用。若迁入 `seckill/api/command/` 可能与 Command 语义重复。`SeckillActivityCreateRequest` 内嵌 `ActivityGoodsItem` 静态内部类，迁移复杂。

**缓解**：本次迁移**不动** 2 个 Request DTO，保留在 `dto/` 包（Controller 层请求 DTO）。`CreateSeckillGoodsCommand` / `CreateActivityCommand` 是 API 层 Command，由 `SeckillApiConverter.toCommand(Request)` 转换，两者职责不同：前者是 HTTP 请求体（含校验注解），后者是 API 契约入参（纯 POJO）。

### 6.12 SeckillController 内部通过 securityUtils.getCurrentUserId() 获取 userId

**风险**：`SeckillService.doSeckill` / `getSeckillResult` 内部通过 `securityUtils.getCurrentUserId()` 获取 userId，而非入参传递。切换到 `SeckillApi.executeSeckill(SeckillCommand)` 后，Command 含 `userId` 字段，需由 Controller 注入，改变调用契约。

**缓解**：`SeckillController` 在调用 API 前通过 `securityUtils.getCurrentUserId()` 获取 userId 并填入 Command。`SeckillApplicationService.executeSeckill` 委托旧 `SeckillService.doSeckill(seckillId, seckillToken)`，旧 Service 内部仍通过 securityUtils 获取 userId（过渡期保留）。未来可将 userId 显式传入 Service，但不在本次迁移范围。

### 6.13 Strangler 过渡期 4 个 Service + 4 个 ServiceImpl 保留

**风险**：4 个 Service + 4 个 ServiceImpl 在过渡期保留，可能与 4 个 API + 4 个 ApplicationService 并存导致语义重复。`SeckillOrderService` 有 17 个方法，门面委托代码量大。

**缓解**：4 个 ApplicationService 仅做门面委托，不含业务逻辑；待所有外部调用方迁移完毕且稳定后，再考虑将业务逻辑从旧 ServiceImpl 迁入 ApplicationService 并删除旧 Service（不在本次迁移范围）。

### 6.14 ArchUnit freeze 模式

**风险**：新增 3 条 seckill 模块规则后，freeze 模式首次运行会冻结当前违规。若 `seckill.application` 当前仍依赖 `seckill.infrastructure.mapper`（通过旧 Service 间接依赖），可能被冻结而非立即报错。

**缓解**：Phase SK.4 保证 4 个 ApplicationService 不直接 import 3 个 Mapper；Phase SK.5 保证 14 处外部调用方不再直接 import 旧 4 个 Service。新增规则后跑 ArchUnit 验证全绿。

### 6.15 Lua 脚本与 Redis Key 常量位置

**风险**：`SeckillLuaService` 加载 `lua/seckill_deduct.lua` + `lua/seckill_rollback.lua`，并使用 `cache.RedisKeyConstants` 中的 key 生成方法。`RedisKeyConstants` 是全局共享常量类（在 `cache` 包），迁移 `SeckillLuaService` 后仍需引用 `cache.RedisKeyConstants`，形成 seckill → cache 的跨模块依赖。

**缓解**：`RedisKeyConstants` 作为全局共享常量保留在 `cache` 包（基础设施共享），不迁入 seckill 模块。`SeckillLuaService` 引用 `RedisKeyConstants` 属于基础设施层共享，不违反模块边界（`infrastructure.cache` → `cache` 是基础设施层内部依赖，非 API 层泄漏）。Lua 脚本保留在 `resources/lua/` 不动。

---

## 7. 回滚策略

### 7.1 单 Phase 回滚

每个 Phase 完成后提交一次 git commit，若后续 Phase 失败可回滚到上一 Phase 提交：

```powershell
git revert <commit-sha>
```

### 7.2 全量回滚

若整体迁移失败，回滚到 Phase SK 开始前的提交：

```powershell
git revert <phase-sk-start-commit>..HEAD
```

### 7.3 文件级回滚

若仅个别文件迁移出错，可单独回滚该文件：

```powershell
git checkout <previous-commit> -- <file-path>
```

### 7.4 回滚验证

回滚后跑 mvn test 验证回归基线：Tests run ≥ 138, Failures ≤ 1, Errors ≤ 19, ArchUnit 39 条全绿。

---

## 8. 回归测试基线

| 指标 | 基线 | 目标 |
|---|---|---|
| Tests run | 138 | ≥ 138 |
| Failures | 1 | ≤ 1 |
| Errors | 19 | ≤ 19 |
| ArchUnit 规则 | 39 全绿 | 42 全绿（新增 3 条 seckill 规则） |

> **说明**：基线为 Payment 模块迁移完成后的状态（Tests run: 138, Failures: 1, Errors: 19, ArchUnit 39 条）。seckill 模块迁移完成后 ArchUnit 规则增至 42 条。

---

## 9. 参考文档

- [PAYMENT-MIGRATION-PLAN.md](PAYMENT-MIGRATION-PLAN.md) - Payment 模块迁移经验（最近完成，主要参考）
- [COUPON-MIGRATION-PLAN.md](COUPON-MIGRATION-PLAN.md) - Coupon 模块迁移经验
- [CART-MIGRATION-PLAN.md](CART-MIGRATION-PLAN.md) - Cart 模块迁移经验
- [PRODUCT-MIGRATION-PLAN.md](PRODUCT-MIGRATION-PLAN.md) - Product 模块迁移经验
- [ORDER-MIGRATION-PLAN.md](ORDER-MIGRATION-PLAN.md) - Order 模块迁移经验
- [IDENTITY-MIGRATION-PLAN.md](IDENTITY-MIGRATION-PLAN.md) - Identity 模块迁移经验
- [SECKILL-API-CONTRACT.md](SECKILL-API-CONTRACT.md) - Seckill 模块 API 契约
- [MODULE-CAPABILITY-MAP.md](MODULE-CAPABILITY-MAP.md) - 模块能力地图（seckill 模块分析依据）