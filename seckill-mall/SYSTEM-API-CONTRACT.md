# System 模块 API 契约 (SYSTEM-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase SY.2 - System API 契约层定义
> 依据：SYSTEM-MIGRATION-PLAN.md + StatsApi 契约风格 + OrderApi 契约风格
> 说明：本文件定义 System 模块对外暴露的 API 契约，一经发布只增不改不删。

---

## 1. SystemApi 接口

**全限定名**：`com.seckill.mall.system.api.SystemApi`

**实现类**：`com.seckill.mall.system.application.SystemApplicationService`（Phase SY.4 创建）

**职责**：后台系统运维能力（操作日志/健康检查/订单趋势/订单状态分布），供 system 模块 Controller 调用。

**设计原则**：
- 方法用业务语言命名，禁止 CRUD 命名
- 入参用 Command/Query 对象，禁止裸露多参数（单参数 Integer/String 保留裸参数）
- 出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO
- 异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈
- 向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

```java
package com.seckill.mall.system.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.system.api.dto.OperationLogDTO;
import com.seckill.mall.system.api.dto.OrderStatusDistributionDTO;
import com.seckill.mall.system.api.dto.OrderTrendDTO;
import com.seckill.mall.system.api.dto.SystemHealthDTO;
import com.seckill.mall.system.api.query.OperationLogQuery;

import java.util.List;

public interface SystemApi {

    /**
     * 操作日志分页查询（按模块/操作人筛选）。
     *
     * @param query 查询条件（pageNum/pageSize/module/operatorId）
     * @return 操作日志分页结果
     */
    PageResult<OperationLogDTO> getOperationLogs(OperationLogQuery query);

    /**
     * 查询所有符合条件的操作日志（不分页），用于 Excel 导出。
     *
     * <p>为防止数据量过大，最多返回 10000 条（按时间倒序）。</p>
     *
     * @param module 模块名筛选，null/空串表示不筛选
     * @return 操作日志列表
     */
    List<OperationLogDTO> listAllForExport(String module);

    /**
     * 系统健康检查：Redis/DB/MQ 状态 + 资源监控 + JVM + DB Pool + 系统信息。
     *
     * @return 系统健康 DTO
     */
    SystemHealthDTO getSystemHealth();

    /**
     * 近 N 天订单趋势（按日期分组统计订单数与销售额，仅统计 PAID/COMPLETED）。
     *
     * @param days 天数，可选，默认 7，最大 30
     * @return 订单趋势 DTO
     */
    OrderTrendDTO getOrderTrend(Integer days);

    /**
     * 订单状态分布（按 status 分组统计）。
     *
     * <p>按 {@code OrderStatus} 枚举自然顺序输出，即使 count=0 也返回，
     * 便于前端饼图直接消费。</p>
     *
     * @param startTime 起始时间字符串（yyyy-MM-dd HH:mm:ss），可选，默认近 30 天
     * @param endTime   结束时间字符串（yyyy-MM-dd HH:mm:ss），可选，默认当前
     * @return 状态分布 DTO
     */
    OrderStatusDistributionDTO getOrderStatusDistribution(String startTime, String endTime);
}
```

---

## 2. DTO 定义

### 2.1 OperationLogDTO

**全限定名**：`com.seckill.mall.system.api.dto.OperationLogDTO`

**用途**：替代 `OperationLogVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`OperationLogVO` → `OperationLogDTO`（由 `SystemApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 日志 ID | `OperationLogVO.id` |
| `operatorId` | `Long` | 操作人 ID | `OperationLogVO.operatorId` |
| `operatorName` | `String` | 操作人名称 | `OperationLogVO.operatorName` |
| `module` | `String` | 模块名 | `OperationLogVO.module` |
| `action` | `String` | 操作动作 | `OperationLogVO.action` |
| `targetId` | `String` | 目标 ID | `OperationLogVO.targetId` |
| `targetType` | `String` | 目标类型 | `OperationLogVO.targetType` |
| `detail` | `String` | 详情 | `OperationLogVO.detail` |
| `ipAddress` | `String` | IP 地址 | `OperationLogVO.ipAddress` |
| `operationTime` | `LocalDateTime` | 操作时间 | `OperationLogVO.operationTime` |

```java
package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogDTO {
    /** 日志 ID */
    private Long id;
    /** 操作人 ID */
    private Long operatorId;
    /** 操作人名称（联表查询） */
    private String operatorName;
    /** 模块名 */
    private String module;
    /** 操作动作 */
    private String action;
    /** 目标 ID */
    private String targetId;
    /** 目标类型 */
    private String targetType;
    /** 详情 */
    private String detail;
    /** IP 地址 */
    private String ipAddress;
    /** 操作时间 */
    private LocalDateTime operationTime;
}
```

### 2.2 SystemHealthDTO

**全限定名**：`com.seckill.mall.system.api.dto.SystemHealthDTO`

**用途**：替代 `SystemHealthVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`SystemHealthVO` → `SystemHealthDTO`（由 `SystemApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `redis` | `String` | Redis 健康状态（UP/DOWN） | `SystemHealthVO.redis` |
| `database` | `String` | 数据库健康状态 | `SystemHealthVO.database` |
| `mq` | `String` | MQ 健康状态 | `SystemHealthVO.mq` |
| `cpuUsage` | `Double` | CPU 使用率（0-100） | `SystemHealthVO.cpuUsage` |
| `memoryUsage` | `Double` | 内存使用率 | `SystemHealthVO.memoryUsage` |
| `diskUsage` | `Double` | 磁盘使用率 | `SystemHealthVO.diskUsage` |
| `redisHitRate` | `Double` | Redis 命中率 | `SystemHealthVO.redisHitRate` |
| `redisResponseTime` | `String` | Redis 响应时间 | `SystemHealthVO.redisResponseTime` |
| `dbPoolUsage` | `String` | DB 连接池使用情况 | `SystemHealthVO.dbPoolUsage` |
| `mqQueueBacklog` | `String` | MQ 队列积压 | `SystemHealthVO.mqQueueBacklog` |
| `jvmHeapUsage` | `Double` | JVM 堆内存使用率 | `SystemHealthVO.jvmHeapUsage` |
| `jvmNonHeapUsage` | `Double` | JVM 非堆内存使用率 | `SystemHealthVO.jvmNonHeapUsage` |
| `dbActiveConnections` | `Integer` | DB 活跃连接数 | `SystemHealthVO.dbActiveConnections` |
| `dbIdleConnections` | `Integer` | DB 空闲连接数 | `SystemHealthVO.dbIdleConnections` |
| `dbMaxConnections` | `Integer` | DB 最大连接数 | `SystemHealthVO.dbMaxConnections` |
| `osName` | `String` | 操作系统名称 | `SystemHealthVO.osName` |
| `jdkVersion` | `String` | JDK 版本 | `SystemHealthVO.jdkVersion` |
| `appStartTime` | `String` | 应用启动时间 | `SystemHealthVO.appStartTime` |
| `appUptime` | `String` | 应用运行时长 | `SystemHealthVO.appUptime` |

```java
package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDTO {
    /** Redis 健康状态（UP/DOWN） */
    private String redis;
    /** 数据库健康状态 */
    private String database;
    /** MQ 健康状态 */
    private String mq;
    /** CPU 使用率（0-100，1 位小数），采集失败为 null */
    private Double cpuUsage;
    /** 内存使用率（0-100，1 位小数），采集失败为 null */
    private Double memoryUsage;
    /** 磁盘使用率（0-100，1 位小数），采集失败为 null */
    private Double diskUsage;
    /** Redis 缓存命中率（0-100，1 位小数），采集失败为 null */
    private Double redisHitRate;
    /** Redis 响应时间（如 "2ms"），采集失败为 null */
    private String redisResponseTime;
    /** 数据库连接池使用情况（如 "12/50"），采集失败为 null */
    private String dbPoolUsage;
    /** MQ 队列积压（如 "0"），采集失败为 null */
    private String mqQueueBacklog;
    /** JVM 堆内存使用率（0-100，1 位小数），采集失败为 null */
    private Double jvmHeapUsage;
    /** JVM 非堆内存使用率（0-100，1 位小数），采集失败为 null */
    private Double jvmNonHeapUsage;
    /** 数据库连接池活跃连接数，采集失败为 null */
    private Integer dbActiveConnections;
    /** 数据库连接池空闲连接数，采集失败为 null */
    private Integer dbIdleConnections;
    /** 数据库连接池最大连接数，采集失败为 null */
    private Integer dbMaxConnections;
    /** 操作系统名称，采集失败为 null */
    private String osName;
    /** JDK 版本，采集失败为 null */
    private String jdkVersion;
    /** 应用启动时间（yyyy-MM-dd HH:mm:ss），采集失败为 null */
    private String appStartTime;
    /** 应用运行时长（如 "2h 13m"），采集失败为 null */
    private String appUptime;
}
```

### 2.3 OrderTrendDTO

**全限定名**：`com.seckill.mall.system.api.dto.OrderTrendDTO`

**用途**：替代 `OrderTrendVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`OrderTrendVO` → `OrderTrendDTO`（由 `SystemApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `dates` | `List<String>` | 日期列表（yyyy-MM-dd） | `OrderTrendVO.dates` |
| `orderCounts` | `List<Long>` | 每日订单数列表 | `OrderTrendVO.orderCounts` |
| `salesAmounts` | `List<BigDecimal>` | 每日销售额列表 | `OrderTrendVO.salesAmounts` |

```java
package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendDTO {
    /** 日期列表，格式 yyyy-MM-dd */
    private List<String> dates;
    /** 每日订单数列表，与 dates 一一对应 */
    private List<Long> orderCounts;
    /** 每日销售额列表（仅统计 PAID/COMPLETED），与 dates 一一对应 */
    private List<BigDecimal> salesAmounts;
}
```

### 2.4 OrderStatusDistributionDTO

**全限定名**：`com.seckill.mall.system.api.dto.OrderStatusDistributionDTO`

**用途**：替代 `OrderStatusDistributionVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`OrderStatusDistributionVO` → `OrderStatusDistributionDTO`（由 `SystemApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `items` | `List<StatusItem>` | 状态分布项列表 | `OrderStatusDistributionVO.items` |
| `total` | `Long` | 订单总数 | `OrderStatusDistributionVO.total` |

**StatusItem 内嵌类**：

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `status` | `String` | 状态码 | `OrderStatusDistributionVO.StatusItem.status` |
| `count` | `Long` | 该状态订单数 | `OrderStatusDistributionVO.StatusItem.count` |
| `percentage` | `Double` | 占比（0-100，2 位小数） | `OrderStatusDistributionVO.StatusItem.percentage` |

```java
package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDistributionDTO {
    /** 状态分布项列表，按 OrderStatus 枚举自然顺序输出 */
    private List<StatusItem> items;
    /** 订单总数（各状态 count 之和） */
    private Long total;

    /** 单个状态分布项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusItem {
        /** 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED */
        private String status;
        /** 该状态订单数 */
        private Long count;
        /** 占比（0-100，保留 2 位小数） */
        private Double percentage;
    }
}
```

---

## 3. Query 定义

### 3.1 OperationLogQuery

**全限定名**：`com.seckill.mall.system.api.query.OperationLogQuery`

**用途**：封装 `getOperationLogs` 的查询条件，替代 `OperationLogQueryRequest`（HTTP 请求对象）作为 API 层入参。

**与 OperationLogQueryRequest 的关系**：
- `OperationLogQueryRequest`（保留在 `dto/` 包）：HTTP 请求对象，带 `@Min/@Max` validation 注解，由 Controller 接收
- `OperationLogQuery`（新建在 `system/api/query/`）：API 查询对象，无 validation 注解，由 Controller 在适配层转换：`Request → Query`

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `pageNum` | `Integer` | 页码（默认 1） | `OperationLogQueryRequest.pageNum` |
| `pageSize` | `Integer` | 每页大小（默认 10） | `OperationLogQueryRequest.pageSize` |
| `module` | `String` | 模块名筛选 | `OperationLogQueryRequest.module` |
| `operatorId` | `Long` | 操作人 ID 筛选 | `OperationLogQueryRequest.operatorId` |

```java
package com.seckill.mall.system.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogQuery {
    /** 页码（默认 1） */
    private Integer pageNum;
    /** 每页大小（默认 10，上限 100） */
    private Integer pageSize;
    /** 模块名筛选（null=不筛选） */
    private String module;
    /** 操作人 ID 筛选（null=不筛选） */
    private Long operatorId;
}
```

---

## 4. 转换器（SystemApiConverter）

**全限定名**：`com.seckill.mall.system.application.facade.SystemApiConverter`

**职责**：集中存放旧 VO 与新 API 层 DTO 之间的转换方法，供 `SystemApplicationService` / `SystemController` 调用。所有方法均为无状态静态方法。

> **Phase SY.2 阶段**：本节为设计预留，转换器在 Phase SY.4 创建。

### 4.1 转换方法清单

| 方法 | 签名 | 用途 |
|---|---|---|
| `toQuery` | `OperationLogQuery toQuery(OperationLogQueryRequest req)` | HTTP Request → API Query |
| `toDTO` | `OperationLogDTO toDTO(OperationLogVO vo)` | OperationLogVO → OperationLogDTO |
| `toVO` | `OperationLogVO toVO(OperationLogDTO dto)` | OperationLogDTO → OperationLogVO |
| `toDTOList` | `List<OperationLogDTO> toDTOList(List<OperationLogVO> voList)` | OperationLogVO 列表 → OperationLogDTO 列表 |
| `toVOList` | `List<OperationLogVO> toVOList(List<OperationLogDTO> dtoList)` | OperationLogDTO 列表 → OperationLogVO 列表 |
| `toHealthDTO` | `SystemHealthDTO toHealthDTO(SystemHealthVO vo)` | SystemHealthVO → SystemHealthDTO |
| `toHealthVO` | `SystemHealthVO toHealthVO(SystemHealthDTO dto)` | SystemHealthDTO → SystemHealthVO |
| `toTrendDTO` | `OrderTrendDTO toTrendDTO(OrderTrendVO vo)` | OrderTrendVO → OrderTrendDTO |
| `toTrendVO` | `OrderTrendVO toTrendVO(OrderTrendDTO dto)` | OrderTrendDTO → OrderTrendVO |
| `toDistDTO` | `OrderStatusDistributionDTO toDistDTO(OrderStatusDistributionVO vo)` | OrderStatusDistributionVO → OrderStatusDistributionDTO |
| `toDistVO` | `OrderStatusDistributionVO toDistVO(OrderStatusDistributionDTO dto)` | OrderStatusDistributionDTO → OrderStatusDistributionVO |

### 4.2 转换规则

- **VO → DTO**：全字段映射（核心 + 展示），保留前端展示信息
- **DTO → VO**：全字段映射（核心 + 展示），保留前端契约
- **Request → Query**：字段一一对应，忽略 validation 注解
- **StatusItem**：内嵌类字段一一对应（status/count/percentage）

---

## 5. ApplicationService 委托映射

### 5.1 SystemApplicationService implements SystemApi

委托 `SystemService`（Strangler 过渡期）：

| SystemApi 方法 | 委托 SystemService 方法 | 转换 |
|---|---|---|
| `getOperationLogs(query)` | `systemService.getOperationLogs(req)` | `toDTOList(voList)` + 分页信息 |
| `listAllForExport(module)` | `systemService.listAllForExport(module)` | `toDTOList(voList)` |
| `getSystemHealth()` | `systemService.getSystemHealth()` | `toHealthDTO(vo)` |
| `getOrderTrend(days)` | `systemService.getOrderTrend(days)` | `toTrendDTO(vo)` |
| `getOrderStatusDistribution(startTime, endTime)` | `systemService.getOrderStatusDistribution(startTime, endTime)` | `toDistDTO(vo)` |

> **Phase SY.2 阶段**：本节为设计预留，ApplicationService 在 Phase SY.4 创建。

---

## 6. Controller 适配（Phase SY.4）

`SystemController` 切换注入 `SystemApi` 替代旧 `SystemService`，调用 API 方法后通过 `SystemApiConverter` 转回 VO：

| Controller 方法 | 旧调用 | 新调用 |
|---|---|---|
| `operationLogs(req)` | `systemService.getOperationLogs(req)` | `systemApi.getOperationLogs(toQuery(req))` → `SystemApiConverter.toVOList(dtoList)` |
| `exportLogs(module)` | `systemService.listAllForExport(module)` | `systemApi.listAllForExport(module)` → `SystemApiConverter.toVOList(dtoList)` |
| `systemHealth()` | `systemService.getSystemHealth()` | `systemApi.getSystemHealth()` → `SystemApiConverter.toHealthVO(dto)` |
| `orderTrend(days)` | `systemService.getOrderTrend(days)` | `systemApi.getOrderTrend(days)` → `SystemApiConverter.toTrendVO(dto)` |
| `orderStatusDistribution(startTime, endTime)` | `systemService.getOrderStatusDistribution(startTime, endTime)` | `systemApi.getOrderStatusDistribution(startTime, endTime)` → `SystemApiConverter.toDistVO(dto)` |

> **Phase SY.2 阶段**：本节为设计预留，Controller 适配在 Phase SY.4 执行。