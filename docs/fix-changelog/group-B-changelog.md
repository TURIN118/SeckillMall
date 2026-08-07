# 组B 修复变更日志（后端秒杀核心+并发+MQ+支付+OrderController契约）

> **修复人**: 组B（coding-engineer）  
> **修复时间**: 2026-08-07  
> **Bug 数量**: 14 项（B3, B4, C6, H-C1~H-C7, M-C1~M-C3, M-S3）  
> **编译状态**: ✅ 通过（mvn compile）

---

## B3. Redis 秒杀库存双重扣减

### 根因分析
每笔成功订单 `seckillStock` 被减 2 次（Lua 预减 1 + 消费者再 DECR 1），而 `seckillInfo.stock` 哈希被设为 DB 正确值，二者不一致。活动卖出一半时前端即报告售罄。

### 修复策略
1. 消费者"同步 Redis"使用 `SET`（校正到 DB 值）而非 `DECR`
2. 明确"Lua 预减是唯一扣减点"，统一以 DB `available_count` 为真相来源
3. 新增 `syncRedisStockFromDb` 方法，DB 扣减成功后将 Redis stockKey 和 info.hash.stock 同步为 DB 最新值

### 修改文件
- `mq/consumer/SeckillOrderConsumer.java`：移除 `redisService.decr`，改为 `syncRedisStockFromDb`（SET 校正）

### 预期效果
Redis 库存与 DB available_count 保持一致，活动不再提前售罄。

### 潜在风险
- SET 操作覆盖 Redis 值，如果 Lua 预减后有短暂窗口未同步，前端可能看到旧值。由补偿任务兜底。

### 回归测试用例
1. 库存 100 的活动，秒杀 50 单后 Redis 库存应为 50（非 0）
2. 并发秒杀后 Redis 与 DB 库存一致

---

## B4. OrderController Entity 泄露

### 根因分析
`OrderController` 返回 `Result<PageResult<SeckillOrder>>`、`Result<SeckillOrder>`，`SeckillOrder` 是 entity（含 `isDeleted`/外键/内部金额），`OrderStatus` 是枚举，直接暴露。

### 修复策略
1. 完善 `SeckillOrderVO`，添加 `from(SeckillOrder)` 转换层
2. `OrderController` 接口签名改为 `Result<PageResult<SeckillOrderVO>>` / `Result<SeckillOrderVO>`
3. `OrderStatus` 仅在 VO 以字符串 code + 中文 description 暴露
4. `getOrderStatus` 返回 `String` 而非 `OrderStatus` 枚举

### 修改文件
- `vo/SeckillOrderVO.java`：添加 `from()` 静态转换方法
- `controller/OrderController.java`：所有秒杀订单接口签名改为返回 VO
- `service/OrderService.java`：接口签名同步修改
- `service/impl/OrderServiceImpl.java`：实现层返回 VO

### 预期效果
前端不再接触 Entity 内部字段，契约稳定，entity 变更不影响前端。

### 潜在风险
- 前端如果依赖 `isDeleted` 等内部字段会断裂（但这些字段本不应暴露）

### 回归测试用例
1. 订单列表接口返回 JSON 不含 `isDeleted` 字段
2. `status` 为字符串 `"UNPAID"`，`statusDescription` 为 `"待支付"`

---

## C6. 创建秒杀场次 500 错误

### 根因分析
`createActivity` 标注 `@Transactional`，`registerAfterCommit` 中 `preheatSeckill` 在 afterCommit 阶段抛错。虽然 afterCommit 不会回滚事务，但异常可能导致响应异常。

### 修复策略
1. 将预热逻辑改为 `@TransactionalEventListener(phase = AFTER_COMMIT)` 事件驱动
2. 定义 `SeckillPreheatEvent` 内部事件类
3. `createActivity` 中发布事件（替代 `registerAfterCommit`）
4. 监听器中 try-catch 吞掉预热异常，不影响主流程

### 修改文件
- `service/impl/SeckillActivityServiceImpl.java`：改用事件驱动预热

### 预期效果
创建秒杀场次不再因预热异常返回 500，预热失败仅记录日志。

### 潜在风险
- `@TransactionalEventListener` 默认在无事务上下文时不执行，需确保 `createActivity` 始终在事务中调用

### 回归测试用例
1. 创建场次时 Redis 不可用，接口仍返回 200
2. 创建成功后预热异步执行，失败不影响主流程

---

## H-C1. 取消/超时不回补 DB available_count

### 根因分析
`rollbackStock` 只动 Redis；全项目唯一 scheduler `SeckillStatusScheduler` 只更新状态，"补偿任务兜底"实际不存在。DB 库存单调递减永不恢复。

### 修复策略
1. `rollbackStock` 在 afterCommit 中回补 DB `available_count`（`restoreStockOptimistic`）
2. Redis 回补改为 Lua 原子执行（M-C2 联动）
3. `SeckillStatusScheduler` 新增 `reconcileStock` 库存对账补偿任务（每 5 分钟），将 Redis 校正到 DB 值

### 修改文件
- `service/impl/OrderServiceImpl.java`：`rollbackStock` 添加 DB 回补
- `scheduler/SeckillStatusScheduler.java`：新增 `reconcileStock` 对账任务
- `mapper/SeckillGoodsMapper.java` + `xml`：新增 `restoreStockOptimistic`

### 预期效果
取消/超时后 DB 库存恢复，不再假售罄/丢单。补偿任务兜底 Redis 不一致。

### 潜在风险
- 对账任务每 5 分钟执行，有最多 5 分钟的不一致窗口

### 回归测试用例
1. 秒杀 1 单后取消，DB available_count 恢复为原值
2. Redis 异常时取消，DB 仍正确回补，补偿任务校正 Redis

---

## H-C2. 消费者 DB 扣减失败仍写成功

### 根因分析
订单先于库存扣减创建，扣减返回 0 时仅告警不回滚，仍 `writeSuccessResult`。产生幽灵单/超卖。

### 修复策略
1. DB 扣减失败（rows==0）或异常时，物理删除订单（`deleteSeckillOrderPhysically`）
2. 写失败结果（`writeFailureResult`），不写 `writeSuccessResult`
3. 新增 `deleteSeckillOrderPhysically` 方法，仅在 UNPAID 状态时物理删除

### 修改文件
- `mq/consumer/SeckillOrderConsumer.java`：扣减失败撤销订单
- `service/OrderService.java` + `impl/OrderServiceImpl.java`：新增 `deleteSeckillOrderPhysically`
- `mapper/SeckillOrderMapper.java`：新增 `deletePhysical`

### 预期效果
DB 扣减失败不再产生幽灵单，用户收到失败结果。

### 潜在风险
- 物理删除订单可能影响关联数据审计，但幽灵单无业务关联

### 回归测试用例
1. 库存为 0 时秒杀，消费者扣减失败，订单被删除，用户收到失败结果
2. 扣减异常时同上

---

## H-C3. 支付并发双扣

### 根因分析
先"读状态"再 `updateById`，无 `WHERE status=UNPAID` 乐观条件；两并发请求都通过校验、各扣一次余额。

### 修复策略
1. 状态判定下沉到 SQL：`UPDATE ... SET status=PAID WHERE id=? AND status='UNPAID'`
2. 钱包扣减放到状态变更之前，若状态变更失败（rows==0）事务回滚，钱包扣减也回滚
3. 状态变更失败后重新查询返回具体错误（ORDER_ALREADY_PAID / ORDER_STATUS_ERROR）
4. 同时修复 `payOrder`（秒杀）和 `payNormalOrder`（普通）

### 修改文件
- `service/impl/OrderServiceImpl.java`：`payOrder` 和 `payNormalOrder` 乐观锁

### 预期效果
并发支付只有一个成功，钱包不会被扣两次。

### 潜在风险
- 钱包扣减后状态变更失败会回滚，但极短时间内余额已变（事务隔离保证一致性）

### 回归测试用例
1. 同一订单并发支付 2 次，只成功 1 次，余额只扣 1 次
2. 已支付订单再次支付返回 ORDER_ALREADY_PAID

---

## H-C4. MQ 同步降级路径不扣 DB 库存

### 根因分析
捕获 `AmqpException` 后直接 `createSeckillOrder`（仅建单，DB 扣减只在异步消费者做），且同步路径不发送延迟消息。导致 DB 超卖 + 订单永不自动取消。

### 修复策略
1. 降级路径 `degradeToSyncCreate` 复刻异步路径关键副作用：
   - 事务内扣 DB 库存（`deductStockOptimistic`）
   - 扣减失败撤销订单并抛异常
   - 发送延迟取消消息（异常不阻断主流程）

### 修改文件
- `mq/producer/SeckillOrderProducer.java`：新增 `degradeToSyncCreate` 方法

### 预期效果
MQ 宕机时降级路径与异步路径行为一致，不产生 DB 超卖或永不取消的订单。

### 潜在风险
- 降级路径同步执行，性能低于异步路径，但保证一致性

### 回归测试用例
1. MQ 宕机时秒杀，DB 库存正确扣减，订单 15 分钟后自动取消
2. 降级路径库存不足时，订单被删除，用户收到失败

---

## H-C5. RabbitMQ 发布不可靠

### 根因分析
未开启 `publisher-confirm`/`publisher-returns`，发送时无 `CorrelationData`。broker 端丢消息时应用无感知。

### 修复策略
1. `RabbitMQConfig` 中自定义 `RabbitTemplate` @Bean，设置 `ConfirmCallback` 和 `ReturnsCallback`
2. `setMandatory(true)` 启用 ReturnsCallback
3. `SeckillOrderProducer` 发送时传 `CorrelationData(messageId)`
4. ConfirmCallback 处理 ack=false，记录日志便于补偿
5. 注：application.yml 的 publisher-confirm-type 配置由组A负责添加

### 修改文件
- `config/RabbitMQConfig.java`：新增 `rabbitTemplate` @Bean
- `mq/producer/SeckillOrderProducer.java`：发送传 `CorrelationData`

### 预期效果
broker 端 nack 或路由失败时应用有日志感知，便于人工/补偿介入。

### 潜在风险
- 若 yml 未配置 publisher-confirm-type，callback 不会被调用（不抛异常，仅失去感知）

### 回归测试用例
1. broker nack 时日志记录 messageId
2. 消息路由失败时日志记录 exchange/routingKey

---

## H-C6. 秒杀下单队列无死信队列

### 根因分析
`seckillOrderQueue` 无 DLX；`basicNack(requeue=false)` 直接丢弃。任何非业务异常都会让"待创建订单"彻底消失。

### 修复策略
1. 为 `seckillOrderQueue` 配置 DLX/DLQ（`x-dead-letter-exchange`/`x-dead-letter-routing-key`）
2. 新增 `seckillOrderDLXExchange`、`seckillOrderDLQ`、`seckillOrderDLXBinding`
3. 消费者 `basicNack(requeue=false)` 消息进入 DLQ（代码不变，队列配置变了）

### 修改文件
- `config/RabbitMQConfig.java`：新增 DLX/DLQ 配置

### 预期效果
毒消息进入 DLQ 而非丢弃，可从 DLQ 人工/定时补偿。

### 潜在风险
- DLQ 消息需要人工或补偿消费者处理，否则会堆积

### 回归测试用例
1. 消费者抛非业务异常，消息进入 DLQ 而非消失
2. DLQ 中消息可被查询和处理

---

## H-C7. 秒杀取消缺乐观锁状态机

### 根因分析
`cancelOrder` 和 `timeoutCancel` 用普通 `updateById` 无 `WHERE status`，用户取消与延迟超时并发时 Redis 库存 `+2`（双回补）。

### 修复策略
1. 引入 `UNPAID→CANCELLING→CANCELLED/TIMEOUT` 乐观锁状态机
2. `cancelOrder`：先 `UNPAID→CANCELLING`（rows==0 则被抢占），回补库存，再 `CANCELLING→CANCELLED`
3. `timeoutCancel`：先 `UNPAID→CANCELLING`（rows==0 则被抢占），回补库存，再 `CANCELLING→TIMEOUT`
4. 使用 `LambdaUpdateWrapper` 的 `.eq(status, UNPAID)` 乐观条件

### 修改文件
- `service/impl/OrderServiceImpl.java`：`cancelOrder` 和 `timeoutCancel` 状态机

### 预期效果
并发取消/超时只有一个请求能回补库存，不再双回补。

### 潜在风险
- CANCELLING 中间态如果进程崩溃，订单停留在 CANCELLING。由补偿任务兜底（可扩展）

### 回归测试用例
1. 用户取消与超时取消并发，只回补 1 次库存
2. CANCELLING 状态的订单不会被二次取消

---

## M-C1. 幂等键时序

### 根因分析
幂等键在处理前设置，处理中异常 nack 丢弃致消息丢失。幂等键残留导致消息重投被跳过。

### 修复策略
1. 幂等键在"处理成功并提交后"再设置（`redisService.set` 在 `basicAck` 之前）
2. 处理前仅 `GET` 检查是否已存在，不 `SETNX`
3. 业务异常也设置幂等键（避免重投重复报错）
4. nack 时不设置幂等键，消息进 DLQ（H-C6 联动）

### 修改文件
- `mq/consumer/SeckillOrderConsumer.java`：幂等键时序调整

### 预期效果
处理中异常不会因幂等键残留导致消息丢失，消息进 DLQ 可补偿。

### 潜在风险
- 并发重投可能重复处理，由业务幂等性（uk_user_seckill）兜底

### 回归测试用例
1. 消费者处理中异常，消息进 DLQ，重投不被幂等键跳过
2. 处理成功后幂等键正确设置

---

## M-C2. Lua/Redis 回补非原子

### 根因分析
`rollbackDeduct` 用 `incr` + `sRem` 两步非原子操作，并发场景可能不一致。

### 修复策略
1. 新增 `seckill_rollback.lua` 脚本，原子执行 `INCR` + `SREM`
2. `SeckillLuaService.rollbackDeduct` 改用 Lua 脚本执行
3. `OrderServiceImpl.rollbackStock` 调用 `seckillLuaService.rollbackDeduct`

### 修改文件
- `resources/lua/seckill_rollback.lua`：新增原子回补脚本
- `cache/SeckillLuaService.java`：加载并执行回补脚本
- `service/impl/OrderServiceImpl.java`：调用 Lua 原子回补

### 预期效果
Redis 库存回补原子执行，不再有并发不一致。

### 潜在风险
- Lua 脚本执行失败由补偿任务兜底

### 回归测试用例
1. 并发回补库存，Redis 值正确（不丢失不多加）

---

## M-C3. Lua stockTtl 参数未传

### 根因分析
`SeckillLuaService.deductStock` 未传 `stockTtl` 给 Lua 脚本，`seckill_deduct.lua` 中 `stockTtl` 为 nil，stockKey 无 TTL，活动结束后残留。

### 修复策略
1. 新增四参数重载 `deductStock(seckillId, userId, boughtSetTtlSeconds, stockTtlSeconds)`
2. 保留三参数重载兼容现有调用方，内部默认 `stockTtl = boughtSetTtl`
3. Lua 脚本在 `stockTtl > 0` 时 `EXPIRE` stockKey

### 修改文件
- `cache/SeckillLuaService.java`：新增四参数重载

### 预期效果
stockKey 在活动结束后自动过期，不残留。

### 潜在风险
- 三参数重载默认 stockTtl = boughtSetTtl，与活动剩余时间一致，符合预期

### 回归测试用例
1. 秒杀后 stockKey 有 TTL，活动结束后自动过期

---

## M-S3. SeckillController 误导注释

### 根因分析
`SeckillController` 中有误导性 TODO 注释，暗示后续可能改回从请求体取 userId，引入越权风险。

### 修复策略
1. 删除 `doSeckill` 方法中的 `TODO(service-layer)` 注释
2. 删除 `result` 方法中的 `TODO(service-layer)` 注释
3. 保留安全修复说明注释，明确"Service 层通过 SecurityUtils 获取调用方身份"

### 修改文件
- `controller/SeckillController.java`：清理误导注释

### 预期效果
后续开发者不会被误导改回从请求体取 userId。

### 潜在风险
- 无

### 回归测试用例
1. 代码审查无误导性 TODO 注释

---

## 修改文件总清单

| 文件 | 修复的 Bug |
|------|-----------|
| `config/RabbitMQConfig.java` | H-C5, H-C6 |
| `mq/consumer/SeckillOrderConsumer.java` | B3, H-C2, M-C1, H-C6 |
| `mq/producer/SeckillOrderProducer.java` | H-C4, H-C5 |
| `cache/SeckillLuaService.java` | M-C2, M-C3 |
| `resources/lua/seckill_rollback.lua` | M-C2 |
| `service/impl/OrderServiceImpl.java` | B4, H-C1, H-C2, H-C3, H-C7, M-C2 |
| `service/OrderService.java` | B4, H-C2 |
| `controller/OrderController.java` | B4 |
| `vo/SeckillOrderVO.java` | B4 |
| `service/impl/SeckillActivityServiceImpl.java` | C6 |
| `controller/SeckillController.java` | M-S3 |
| `scheduler/SeckillStatusScheduler.java` | H-C1 |
| `mapper/SeckillGoodsMapper.java` + `xml` | H-C1 |
| `mapper/SeckillOrderMapper.java` | H-C2 |