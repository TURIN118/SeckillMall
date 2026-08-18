# System 模块迁移计划 (SYSTEM-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase SY - System 模块迁移（Strangler Pattern 第八阶段）
> 依据：PAYMENT-MIGRATION-PLAN.md + COUPON-MIGRATION-PLAN.md + CART-MIGRATION-PLAN.md + PRODUCT-MIGRATION-PLAN.md + ORDER-MIGRATION-PLAN.md + SECKILL-MIGRATION-PLAN.md + STATS-MIGRATION-PLAN.md + Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：Phase SY.0+SY.2+SY.3 阶段已创建包结构、API 层（SystemApi+DTO+Query）并迁移持久化层（OperationLog/OperationLogMapper）。未修改任何业务行为，未删除任何旧代码。

---

## 1. System 当前结构

### 1.1 当前包结构

System 相关代码散落在 5 个不同的顶层包中，共 **11 个核心文件**：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `SystemController` | `controller` | 后台系统管理：操作日志/导出/健康检查/订单趋势/订单状态分布（/api/v1/admin） |
| `SystemService` | `service` | 系统管理服务接口（5 个方法） |
| `SystemServiceImpl` | `service.impl` | SystemService 实现（聚合 SeckillOrderApi + OperationLogMapper + SystemHealthMonitor） |
| `SystemHealthMonitor` | `service` | 系统健康监控端口（18 方法，仅被 SystemServiceImpl 内部使用） |
| `SystemHealthMonitorImpl` | `service.impl` | 健康监控实现（Redis/DB/MQ/JVM/OS 指标采集） |
| `OperationLog` | `entity` | 操作日志持久化实体（t_operation_log） |
| `OperationLogMapper` | `mapper` | 操作日志 MyBatis Mapper |
| `OperationLogVO` | `vo` | 操作日志视图（含 operatorName 等展示字段） |
| `SystemHealthVO` | `vo` | 系统健康视图（Redis/DB/MQ + 资源监控 + JVM + DB Pool + 系统信息） |
| `OrderTrendVO` | `vo` | 订单趋势视图（dates + orderCounts + salesAmounts） |
| `OrderStatusDistributionVO` | `vo` | 订单状态分布视图（items + total，含 StatusItem 内嵌类） |
| `OperationLogQueryRequest` | `dto` | 操作日志查询请求（pageNum/pageSize/module/operatorId） |

**关键特征**：System 是**后台运维聚合模块**，包含两部分：
1. **操作日志管理**：依赖独立 Entity（OperationLog）+ Mapper（OperationLogMapper），是本模块的持久化资产
2. **运维聚合查询**：依赖跨模块 API（SeckillOrderApi）+ 内部健康监控（SystemHealthMonitor）

### 1.2 Service 方法签名

#### SystemService（5 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `getOperationLogs` | `PageResult<OperationLogVO> getOperationLogs(OperationLogQueryRequest req)` | 操作日志分页查询 | ⚠️ 返回 VO，入参用 Request DTO |
| `listAllForExport` | `List<OperationLogVO> listAllForExport(String module)` | 导出操作日志（最多 10000 条） | ⚠️ 返回 VO，单参数可保留 |
| `getSystemHealth` | `SystemHealthVO getSystemHealth()` | 系统健康检查 | ⚠️ 返回 VO |
| `getOrderTrend` | `OrderTrendVO getOrderTrend(Integer days)` | 订单趋势（近 N 天） | ⚠️ 返回 VO，单参数可保留 |
| `getOrderStatusDistribution` | `OrderStatusDistributionVO getOrderStatusDistribution(String startTime, String endTime)` | 订单状态分布 | ⚠️ 返回 VO，双参数需 Query 封装 |

#### SystemHealthMonitor（18 方法，仅内部使用，**不 API 化**）

健康监控端口仅被 `SystemServiceImpl.getSystemHealth` 内部调用，无外部消费者，**不需要 API 化**。后续 Phase SY.4 可考虑将其迁入 `system/infrastructure/monitor/` 作为内部基础设施。

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `SystemController` | `/api/v1/admin` | `GET /operation-logs` | 操作日志列表（分页+模块筛选） | ADMIN |
| | | `GET /operation-logs/export` | 导出操作日志为 Excel（最多 10000 条） | ADMIN |
| | | `GET /system/health` | 系统健康检查 | ADMIN |
| | | `GET /dashboard/order-trend` | 近N天订单趋势 | ADMIN |
| | | `GET /dashboard/order-status-distribution` | 订单状态分布 | ADMIN |

### 1.4 跨模块依赖关系

#### System 依赖的模块（出向依赖）

| 被依赖模块 | 依赖方式 | 用途 |
|---|---|---|
| **seckill** | `SeckillOrderApi` | `SystemServiceImpl.getOrderTrend` 查订单趋势；`getOrderStatusDistribution` 查订单状态分布 |
| **order** | `OrderStatus`（infrastructure.persistence.entity） | `SystemServiceImpl` 引用 `OrderStatus.PAID/COMPLETED` 枚举 |

#### 依赖 System 的模块（入向依赖）

| 调用方模块 | 依赖方式 | 用途 |
|---|---|---|
| **无** | — | System 是顶层运维模块，无其他业务模块依赖 System |

> **关键特征**：System 是**纯叶子模块**（无入向依赖），迁移风险较低，可放心重构。
> **注意**：`OperationLog`（注解）和 `OperationLogMapper` 可能被 AOP 切面或测试引用，迁移时需更新所有 import。

### 1.5 VO 字段

#### OperationLogVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 日志 ID |
| `operatorId` | `Long` | 操作人 ID |
| `operatorName` | `String` | 操作人名称（联表查询） |
| `module` | `String` | 模块名 |
| `action` | `String` | 操作动作 |
| `targetId` | `String` | 目标 ID |
| `targetType` | `String` | 目标类型 |
| `detail` | `String` | 详情 |
| `ipAddress` | `String` | IP 地址 |
| `operationTime` | `LocalDateTime` | 操作时间 |

#### SystemHealthVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `redis` | `String` | Redis 健康状态（UP/DOWN） |
| `database` | `String` | 数据库健康状态 |
| `mq` | `String` | MQ 健康状态 |
| `cpuUsage` | `Double` | CPU 使用率（0-100） |
| `memoryUsage` | `Double` | 内存使用率 |
| `diskUsage` | `Double` | 磁盘使用率 |
| `redisHitRate` | `Double` | Redis 命中率 |
| `redisResponseTime` | `String` | Redis 响应时间 |
| `dbPoolUsage` | `String` | DB 连接池使用情况 |
| `mqQueueBacklog` | `String` | MQ 队列积压 |
| `jvmHeapUsage` | `Double` | JVM 堆内存使用率 |
| `jvmNonHeapUsage` | `Double` | JVM 非堆内存使用率 |
| `dbActiveConnections` | `Integer` | DB 活跃连接数 |
| `dbIdleConnections` | `Integer` | DB 空闲连接数 |
| `dbMaxConnections` | `Integer` | DB 最大连接数 |
| `osName` | `String` | 操作系统名称 |
| `jdkVersion` | `String` | JDK 版本 |
| `appStartTime` | `String` | 应用启动时间 |
| `appUptime` | `String` | 应用运行时长 |

#### OrderTrendVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `dates` | `List<String>` | 日期列表（yyyy-MM-dd） |
| `orderCounts` | `List<Long>` | 每日订单数列表 |
| `salesAmounts` | `List<BigDecimal>` | 每日销售额列表 |

#### OrderStatusDistributionVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `items` | `List<StatusItem>` | 状态分布项列表 |
| `total` | `Long` | 订单总数 |

**StatusItem 内嵌类**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `status` | `String` | 状态码 |
| `count` | `Long` | 该状态订单数 |
| `percentage` | `Double` | 占比（0-100，2 位小数） |

### 1.6 Entity 字段（OperationLog）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 日志 ID（ASSIGN_ID） |
| `operatorId` | `Long` | 操作人 ID |
| `module` | `String` | 模块名 |
| `action` | `String` | 操作动作 |
| `targetId` | `String` | 目标 ID |
| `targetType` | `String` | 目标类型 |
| `detail` | `String` | 详情 |
| `ipAddress` | `String` | IP 地址 |
| `createTime` | `LocalDateTime` | 创建时间（自动填充） |

---

## 2. System 目标结构（Pragmatic DDD + Modular Monolith）

### 2.1 目标包结构

```
com.seckill.mall.system/
├── package-info.java                                  # 模块根包
├── api/                                                # API 契约层（对外暴露）
│   ├── package-info.java
│   ├── SystemApi.java                                  # 系统 API（5 个方法）— 替代 SystemService
│   ├── dto/
│   │   ├── package-info.java
│   │   ├── OperationLogDTO.java                        # 操作日志 DTO（替代 OperationLogVO 跨模块传递）
│   │   ├── SystemHealthDTO.java                        # 系统健康 DTO（替代 SystemHealthVO）
│   │   ├── OrderTrendDTO.java                          # 订单趋势 DTO（替代 OrderTrendVO）
│   │   └── OrderStatusDistributionDTO.java             # 订单状态分布 DTO（替代 OrderStatusDistributionVO）
│   ├── query/
│   │   ├── package-info.java
│   │   └── OperationLogQuery.java                      # 操作日志查询条件（封装 pageNum/pageSize/module/operatorId）
│   └── result/
│       └── package-info.java                           # 预留结果对象包（当前无 Result 类）
├── application/                                        # 应用层
│   ├── package-info.java
│   ├── SystemApplicationService.java                    # 实现 SystemApi，委托 SystemService
│   └── facade/
│       ├── package-info.java
│       └── SystemApiConverter.java                      # VO ↔ DTO 转换
├── infrastructure/                                     # 基础设施层
│   ├── package-info.java
│   ├── entity/
│   │   ├── package-info.java
│   │   └── OperationLog.java                            # 从 entity 包迁入（持久化实体）
│   └── mapper/
│       ├── package-info.java
│       └── OperationLogMapper.java                      # 从 mapper 包迁入（MyBatis Mapper）
└── interfaces/                                         # 接口层
    ├── package-info.java
    ├── vo/
    │   ├── package-info.java
    │   ├── OperationLogVO.java                          # 从 vo 包迁入（前端展示用）— Phase SY.4
    │   ├── SystemHealthVO.java                          # 从 vo 包迁入 — Phase SY.4
    │   ├── OrderTrendVO.java                            # 从 vo 包迁入 — Phase SY.4
    │   └── OrderStatusDistributionVO.java               # 从 vo 包迁入 — Phase SY.4
    └── web/
        ├── package-info.java
        └── SystemController.java                        # 从 controller 包迁入 — Phase SY.4
```

> **注意**：system 模块**有 infrastructure 层**（OperationLog Entity + OperationLogMapper），**无 domain 层**（采用 Pragmatic DDD，不强制独立领域模型）。
> **OperationLogQueryRequest** 保持在 `dto/` 包不动，因为它是 HTTP 请求对象（带 validation 注解），不是 API 出参。Phase SY.4 Controller 迁移时再决定是否迁入 `interfaces/web/dto/`。

### 2.2 分层依赖规则（目标）

```
interfaces ──→ application ──→ api
                     │
                     └──→ service（旧层，Strangler 过渡期）
                               │
                               ├──→ infrastructure（mapper/entity）
                               ├──→ SystemHealthMonitor（内部端口）
                               └──→ 其他模块 API（seckill）

api ──✗──→ infrastructure     （API 不依赖基础设施）
application ──✗──→ mapper     （Application 不直接依赖 Mapper，通过旧 service 间接访问）
interfaces ──✗──→ mapper      （Interfaces 不直接依赖 Mapper）
```

### 2.3 与其他模块的关系（目标）

| 关系 | 模块 | 依赖方式 |
|---|---|---|
| System → Seckill | `SeckillOrderApi` | 订单趋势、订单状态分布 |
| System → Order | `OrderStatus`（枚举） | 订单状态枚举（PAID/COMPLETED） |
| 无 → System | — | System 是纯叶子模块，无入向依赖 |

---

## 3. 文件迁移路径

### 3.1 移动文件（git mv）

| 源路径 | 目标路径 | 阶段 |
|---|---|---|
| `entity/OperationLog.java` | `system/infrastructure/entity/OperationLog.java` | Phase SY.3 |
| `mapper/OperationLogMapper.java` | `system/infrastructure/mapper/OperationLogMapper.java` | Phase SY.3 |
| `vo/OperationLogVO.java` | `system/interfaces/vo/OperationLogVO.java` | Phase SY.4 |
| `vo/SystemHealthVO.java` | `system/interfaces/vo/SystemHealthVO.java` | Phase SY.4 |
| `vo/OrderTrendVO.java` | `system/interfaces/vo/OrderTrendVO.java` | Phase SY.4 |
| `vo/OrderStatusDistributionVO.java` | `system/interfaces/vo/OrderStatusDistributionVO.java` | Phase SY.4 |
| `controller/SystemController.java` | `system/interfaces/web/SystemController.java` | Phase SY.4 |

### 3.2 新增文件

| 目标路径 | 阶段 |
|---|---|
| `system/package-info.java` 及所有子包 `package-info.java`（共 13 个） | Phase SY.0 |
| `system/api/SystemApi.java` | Phase SY.2 |
| `system/api/dto/{OperationLogDTO,SystemHealthDTO,OrderTrendDTO,OrderStatusDistributionDTO}.java` | Phase SY.2 |
| `system/api/query/OperationLogQuery.java` | Phase SY.2 |
| `system/application/SystemApplicationService.java` | Phase SY.4 |
| `system/application/facade/SystemApiConverter.java` | Phase SY.4 |

### 3.3 保留不动的文件

| 文件 | 原因 |
|---|---|
| `service/SystemService.java` | Strangler 过渡期保留，SystemApplicationService 委托它 |
| `service/impl/SystemServiceImpl.java` | 同上 |
| `service/SystemHealthMonitor.java` | 内部端口，仅被 SystemServiceImpl 使用，不需要 API 化 |
| `service/impl/SystemHealthMonitorImpl.java` | 同上 |
| `dto/OperationLogQueryRequest.java` | HTTP 请求对象（带 validation），不是 API 出参，保持不动 |

### 3.4 后续可删除的文件（Strangler 完成后）

| 文件 | 删除时机 |
|---|---|
| `service/SystemService.java` + `service/impl/SystemServiceImpl.java` | 业务逻辑完全迁入 system.application 后 |
| `service/SystemHealthMonitor.java` + `service/impl/SystemHealthMonitorImpl.java` | 健康监控迁入 system/infrastructure/monitor/ 后 |

---

## 4. API 边界（SystemApi）

### 4.1 设计原则

1. **方法用业务语言命名**，禁止 CRUD 命名
2. **入参用 Command/Query 对象**，禁止裸露多参数（`getOrderStatusDistribution(startTime, endTime)` 双参数需 Query 封装；`getOrderTrend(days)` 单参数保留裸参数）
3. **出参用 Result/DTO/Snapshot**，禁止暴露 Entity/Mapper/PO
4. **异常通过 `BusinessException(ErrorCode)` 抛出**，不泄露堆栈
5. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

### 4.2 SystemApi 方法清单

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `getOperationLogs` | `PageResult<OperationLogDTO> getOperationLogs(OperationLogQuery query)` | `SystemService.getOperationLogs` |
| `listAllForExport` | `List<OperationLogDTO> listAllForExport(String module)` | `SystemService.listAllForExport` |
| `getSystemHealth` | `SystemHealthDTO getSystemHealth()` | `SystemService.getSystemHealth` |
| `getOrderTrend` | `OrderTrendDTO getOrderTrend(Integer days)` | `SystemService.getOrderTrend` |
| `getOrderStatusDistribution` | `OrderStatusDistributionDTO getOrderStatusDistribution(String startTime, String endTime)` | `SystemService.getOrderStatusDistribution` |

> **关键变化**：
> - 5 个方法返回类型从 VO 改为 DTO，消除 VO 跨模块传递。
> - `getOperationLogs` 入参从 `OperationLogQueryRequest`（HTTP 请求对象）改为 `OperationLogQuery`（API 查询对象，无 validation 注解）。
> - `getOrderStatusDistribution` 保留双参数（startTime/endTime），与旧签名一致，避免破坏现有调用。
> - `getOrderTrend(days)` 保留裸参数（单参数不构成反模式）。
> - `listAllForExport(module)` 保留裸参数（单参数不构成反模式）。

### 4.3 DTO 定义

详见 [SYSTEM-API-CONTRACT.md](SYSTEM-API-CONTRACT.md)。

---

## 5. 迁移步骤（Phase SY）

### Phase SY.0：创建包结构

- 创建 `system/` 及所有子包（共 13 个 package-info.java）
- 每个包创建 `package-info.java`
- **不修改任何现有代码**

### Phase SY.2：创建 API 层

- 创建 `SystemApi` 接口
- 创建 4 个 DTO（`OperationLogDTO`、`SystemHealthDTO`、`OrderTrendDTO`、`OrderStatusDistributionDTO`）
- 创建 1 个 Query（`OperationLogQuery`）
- **不修改任何现有代码**

### Phase SY.3：迁移持久化层

- `git mv entity/OperationLog.java system/infrastructure/entity/OperationLog.java`
- `git mv mapper/OperationLogMapper.java system/infrastructure/mapper/OperationLogMapper.java`
- 更新 2 个文件的 package 声明
- 更新所有引用旧包的 import：
  - `com.seckill.mall.entity.OperationLog` → `com.seckill.mall.system.infrastructure.entity.OperationLog`
  - `com.seckill.mall.mapper.OperationLogMapper` → `com.seckill.mall.system.infrastructure.mapper.OperationLogMapper`
- **回归测试**：mvn compile + mvn test

### Phase SY.4：创建 Application 层 + 移动 Controller/VO

- 创建 `SystemApplicationService implements SystemApi`，委托 `SystemService`
- 创建 `SystemApiConverter`：VO ↔ DTO 转换
- `git mv vo/OperationLogVO.java system/interfaces/vo/OperationLogVO.java`
- `git mv vo/SystemHealthVO.java system/interfaces/vo/SystemHealthVO.java`
- `git mv vo/OrderTrendVO.java system/interfaces/vo/OrderTrendVO.java`
- `git mv vo/OrderStatusDistributionVO.java system/interfaces/vo/OrderStatusDistributionVO.java`
- `git mv controller/SystemController.java system/interfaces/web/SystemController.java`
- 更新 package 声明与所有 import
- 切换 Controller 注入 `SystemApi` 替代旧 `SystemService`，调用 API 方法后通过 Converter 转回 VO
- **不删除旧 SystemService / SystemServiceImpl**（Strangler 过渡）
- **回归测试**：mvn test

### Phase SY.5：ArchUnit 规则

新增 system 模块包边界规则：

| 规则 | 内容 |
|---|---|
| `system_api_should_not_depend_on_infrastructure` | `system.api` 不应依赖 `system.infrastructure` |
| `system_application_should_not_depend_on_mapper` | `system.application` 不应直接依赖 `system.infrastructure.mapper`（通过旧 service 间接访问） |
| `system_interfaces_should_not_depend_on_service` | `system.interfaces` 不应直接依赖旧 `service` 包（强制走 application 层） |

**回归标准**：mvn test Failures ≤ 1, Errors ≤ 19, ArchUnit 45 条规则全绿。

---

## 6. 风险点与缓解

### 6.1 OperationLog 注解类名冲突

**风险**：项目中存在 `com.seckill.mall.annotation.OperationLog`（注解）和 `com.seckill.mall.entity.OperationLog`（实体）两个同名类。迁移实体后全限定名变为 `com.seckill.mall.system.infrastructure.entity.OperationLog`，避免与注解混淆。

**缓解**：迁移后所有 import 使用全限定名，IDE 自动优化 import 时不会混淆。

### 6.2 OperationLogMapper 被 AOP/测试引用

**风险**：`OperationLogMapper` 可能被 AOP 切面（如操作日志切面）或测试类直接注入。迁移包后所有引用方需更新 import。

**缓解**：迁移前用 grep 搜索所有引用，逐一更新；迁移后执行 mvn compile 验证。

### 6.3 MyBatis XML 命名空间

**风险**：`OperationLogMapper.xml` 的 `namespace` 属性可能硬编码了旧全限定名 `com.seckill.mall.mapper.OperationLogMapper`，迁移后需同步更新。

**缓解**：检查 `src/main/resources/mapper/` 下的 XML 文件，更新 namespace。

### 6.4 SystemHealthMonitor 不 API 化

**风险**：`SystemHealthMonitor`（18 方法）仅被 `SystemServiceImpl` 内部使用，若强行 API 化会膨胀 API 表面积。

**缓解**：不 API 化，保留在旧 `service` 包；Phase SY.4 后可考虑迁入 `system/infrastructure/monitor/` 作为内部基础设施。

### 6.5 OperationLogQueryRequest 保留不动

**风险**：`OperationLogQueryRequest` 是 HTTP 请求对象（带 `@Min/@Max` validation），若迁入 `system/api/query/` 会污染 API 层（API 层不应依赖 web validation）。

**缓解**：保留在 `dto/` 包不动；API 层定义独立的 `OperationLogQuery`（无 validation 注解），由 Controller 在适配层将 Request → Query。

---

## 7. 回滚策略

### 7.1 Phase SY.0+SY.2 回滚

仅新增文件，未修改现有代码，直接删除新增文件即可：
```bash
git rm -r seckill-mall/src/main/java/com/seckill/mall/system/
git rm seckill-mall/SYSTEM-MIGRATION-PLAN.md seckill-mall/SYSTEM-API-CONTRACT.md
```

### 7.2 Phase SY.3 回滚

持久化层迁移可通过 git revert 回滚：
```bash
git mv seckill-mall/src/main/java/com/seckill/mall/system/infrastructure/entity/OperationLog.java seckill-mall/src/main/java/com/seckill/mall/entity/OperationLog.java
git mv seckill-mall/src/main/java/com/seckill/mall/system/infrastructure/mapper/OperationLogMapper.java seckill-mall/src/main/java/com/seckill/mall/mapper/OperationLogMapper.java
# 还原 package 声明和所有 import
```

### 7.3 Phase SY.4 回滚

Application 层 + Controller/VO 迁移回滚：
- 删除 `SystemApplicationService`、`SystemApiConverter`
- 将 VO 和 Controller git mv 回原位置
- 还原 Controller 注入 `SystemService`
- 还原所有 package 声明和 import

---

## 8. 验证清单

| 项 | 命令 | 期望 |
|---|---|---|
| 编译 | `mvn compile -q` | BUILD SUCCESS |
| 单元测试 | `mvn test` | Failures ≤ 1, Errors ≤ 19 |
| ArchUnit | `mvn test` | 45 条规则全绿 |
| import 检查 | `grep -r "com.seckill.mall.entity.OperationLog" seckill-mall/src` | 无结果（除注释） |
| import 检查 | `grep -r "com.seckill.mall.mapper.OperationLogMapper" seckill-mall/src` | 无结果（除注释） |

---

## 9. 后续 Task

| Task | 内容 | 阶段 |
|---|---|---|
| 67 | 迁移 4 个 VO 到 system/interfaces/vo/ + Controller 到 system/interfaces/web/ | Phase SY.4 |
| 68 | 创建 SystemApplicationService + SystemApiConverter + 切换 Controller 注入 | Phase SY.4 |
| 69 | 新增 ArchUnit system 模块规则 | Phase SY.5 |