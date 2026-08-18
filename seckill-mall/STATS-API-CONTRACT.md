# Stats 模块 API 契约 (STATS-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase ST.2 - Stats API 契约层定义
> 依据：STATS-MIGRATION-PLAN.md + PaymentApi 契约风格 + SeckillApi 契约风格
> 说明：本文件定义 Stats 模块对外暴露的 API 契约，一经发布只增不改不删。

---

## 1. StatsApi 接口

**全限定名**：`com.seckill.mall.stats.api.StatsApi`

**实现类**：`com.seckill.mall.stats.application.StatsApplicationService`

**职责**：数据看台统计聚合能力（总览/趋势/排行榜/状态分布/秒杀概览），供 stats 模块 Controller 调用。

**设计原则**：
- 方法用业务语言命名，禁止 CRUD 命名
- 入参用 Command/Query 对象，禁止裸露多参数（stats 所有方法均为单参数 Integer，不构成反模式，保留裸参数）
- 出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO
- 异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈
- 向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型
- 复用已有 DTO：秒杀概览与排行榜 DTO 复用 seckill 模块定义，避免重复建模

```java
package com.seckill.mall.stats.api;

import com.seckill.mall.seckill.api.dto.SeckillOverviewDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.stats.api.dto.OrderStatusItemDTO;
import com.seckill.mall.stats.api.dto.StatsOverviewDTO;
import com.seckill.mall.stats.api.dto.TrendItemDTO;

import java.util.List;

public interface StatsApi {

    /**
     * 总览统计：用户数、订单数、秒杀活动数、销售总额、商品数、今日订单/注册数。
     *
     * @return 总览统计 DTO
     */
    StatsOverviewDTO getOverview();

    /**
     * 用户注册趋势：近 N 天每日注册数（按日期升序，缺失日期补零）。
     *
     * @param days 天数，建议 7 或 30；null/非正数默认 7，上限 30
     * @return 趋势数据项列表
     */
    List<TrendItemDTO> getUserTrend(Integer days);

    /**
     * 订单趋势：近 N 天每日订单数（按日期升序，缺失日期补零）。
     *
     * @param days 天数，建议 7 或 30；null/非正数默认 7，上限 30
     * @return 趋势数据项列表
     */
    List<TrendItemDTO> getOrderTrend(Integer days);

    /**
     * 秒杀排行榜 Top N：按销售额降序（销售额相同按销量降序）。
     *
     * @param limit Top N，null/非正数默认 10，上限 50
     * @return 排行榜列表（复用 seckill 模块 SeckillRankingDTO）
     */
    List<SeckillRankingDTO> getSeckillRanking(Integer limit);

    /**
     * 订单状态分布：按 status 分组统计订单数量。
     *
     * <p>按 {@code OrderStatus} 枚举自然顺序输出，即使 count=0 也返回，
     * 便于前端饼图直接消费。仅统计未逻辑删除的订单（is_deleted=0）。</p>
     *
     * @return 状态分布项列表，每项包含 status 与 count
     */
    List<OrderStatusItemDTO> getOrderStatusDistribution();

    /**
     * 秒杀活动概览：进行中 / 待开始 / 今日已完成三项实时指标。
     *
     * <p>所有指标均基于时间窗口动态计算（不依赖 DB status 字段）。
     * 返回复用 seckill 模块的 SeckillOverviewDTO。</p>
     *
     * @return 秒杀活动概览 DTO（复用 seckill 模块）
     */
    SeckillOverviewDTO getSeckillOverview();
}
```

---

## 2. DTO 定义

### 2.1 StatsOverviewDTO

**全限定名**：`com.seckill.mall.stats.api.dto.StatsOverviewDTO`

**用途**：替代 `StatsOverviewVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`StatsOverviewVO` → `StatsOverviewDTO`（由 `StatsApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `userCount` | `Long` | 用户总数 | `StatsOverviewVO.userCount` |
| `orderCount` | `Long` | 订单总数 | `StatsOverviewVO.orderCount` |
| `seckillCount` | `Long` | 秒杀活动数 | `StatsOverviewVO.seckillCount` |
| `totalSales` | `BigDecimal` | 销售总额（仅 PAID/COMPLETED） | `StatsOverviewVO.totalSales` |
| `productCount` | `Long` | 商品总数 | `StatsOverviewVO.productCount` |
| `todayOrderCount` | `Long` | 今日订单数 | `StatsOverviewVO.todayOrderCount` |
| `todayUserCount` | `Long` | 今日注册数 | `StatsOverviewVO.todayUserCount` |

```java
package com.seckill.mall.stats.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsOverviewDTO {
    /** 用户总数 */
    private Long userCount;
    /** 订单总数 */
    private Long orderCount;
    /** 秒杀活动数 */
    private Long seckillCount;
    /** 销售总额（仅统计 PAID/COMPLETED 订单） */
    private BigDecimal totalSales;
    /** 商品总数 */
    private Long productCount;
    /** 今日订单数 */
    private Long todayOrderCount;
    /** 今日注册数 */
    private Long todayUserCount;
}
```

### 2.2 TrendItemDTO

**全限定名**：`com.seckill.mall.stats.api.dto.TrendItemDTO`

**用途**：替代 `TrendItemVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`TrendItemVO` → `TrendItemDTO`（由 `StatsApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `date` | `String` | 日期字符串（yyyy-MM-dd） | `TrendItemVO.date` |
| `count` | `Long` | 当日数量 | `TrendItemVO.count` |

```java
package com.seckill.mall.stats.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendItemDTO {
    /** 日期字符串，格式 yyyy-MM-dd */
    private String date;
    /** 当日数量（注册数 / 订单数等，由调用方语义决定） */
    private Long count;
}
```

### 2.3 OrderStatusItemDTO

**全限定名**：`com.seckill.mall.stats.api.dto.OrderStatusItemDTO`

**用途**：替代 `OrderStatusItemVO` 作为 Application 层返回值，供 Controller 转回 VO。

**来源映射**：`OrderStatusItemVO` → `OrderStatusItemDTO`（由 `StatsApiConverter.toDTO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `status` | `String` | 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED | `OrderStatusItemVO.status` |
| `count` | `Long` | 该状态订单数量 | `OrderStatusItemVO.count` |

```java
package com.seckill.mall.stats.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusItemDTO {
    /** 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED */
    private String status;
    /** 该状态订单数量 */
    private Long count;
}
```

---

## 3. 复用 DTO（来自 seckill 模块）

### 3.1 SeckillOverviewDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillOverviewDTO`

**用途**：`StatsApi.getSeckillOverview` 返回值，复用 seckill 模块定义，避免重复建模。

**字段**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `totalActivities` | `long` | 秒杀活动总数（stats 场景不使用，置 0 或忽略） |
| `activeCount` | `long` | 进行中秒杀数 |
| `pendingCount` | `long` | 待开始秒杀数 |
| `completedTodayCount` | `long` | 今日已完成秒杀数 |

> **字段映射差异**（由 `StatsApiConverter.toVO` 处理）：
> - `dto.activeCount`（long）→ `vo.activeCount`（Integer，`Math.toIntExact`）
> - `dto.pendingCount`（long）→ `vo.pendingCount`（Integer）
> - `dto.completedTodayCount`（long）→ `vo.completedToday`（Integer，**字段名不同**）
> - `dto.totalActivities` 忽略（`SeckillOverviewVO` 无此字段）

### 3.2 SeckillRankingDTO

**全限定名**：`com.seckill.mall.seckill.api.dto.SeckillRankingDTO`

**用途**：`StatsApi.getSeckillRanking` 返回值，复用 seckill 模块定义。

**字段**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `seckillId` | `Long` | 秒杀商品 ID |
| `seckillName` | `String` | 秒杀商品名称 |
| `salesCount` | `long` | 销售数量 |
| `salesAmount` | `BigDecimal` | 销售总额 |

> **字段映射**：与 `SeckillRankingVO` 字段语义一致，仅类型差异（`Long` vs `long`），`StatsApiConverter.toVO` 直接映射。

---

## 4. 转换器（StatsApiConverter）

**全限定名**：`com.seckill.mall.stats.application.facade.StatsApiConverter`

**职责**：集中存放旧 VO 与新 API 层 DTO 之间的转换方法，供 `StatsApplicationService` / `StatsController` 调用。所有方法均为无状态静态方法。

> **Phase ST.2 阶段**：本节为设计预留，转换器在 Phase ST.4 创建。

### 4.1 转换方法清单

| 方法 | 签名 | 用途 |
|---|---|---|
| `toDTO` | `StatsOverviewDTO toDTO(StatsOverviewVO vo)` | StatsOverviewVO → StatsOverviewDTO |
| `toVO` | `StatsOverviewVO toVO(StatsOverviewDTO dto)` | StatsOverviewDTO → StatsOverviewVO（Controller 层前端契约适配） |
| `toDTOList` | `List<TrendItemDTO> toDTOList(List<TrendItemVO> voList)` | TrendItemVO 列表 → TrendItemDTO 列表 |
| `toVOList` | `List<TrendItemVO> toVOList(List<TrendItemDTO> dtoList)` | TrendItemDTO 列表 → TrendItemVO 列表 |
| `toStatusDTOList` | `List<OrderStatusItemDTO> toStatusDTOList(List<OrderStatusItemVO> voList)` | OrderStatusItemVO 列表 → OrderStatusItemDTO 列表 |
| `toStatusVOList` | `List<OrderStatusItemVO> toStatusVOList(List<OrderStatusItemDTO> dtoList)` | OrderStatusItemDTO 列表 → OrderStatusItemVO 列表 |
| `toOverviewVO` | `SeckillOverviewVO toOverviewVO(SeckillOverviewDTO dto)` | SeckillOverviewDTO → SeckillOverviewVO（处理字段名/类型差异） |
| `toRankingVOList` | `List<SeckillRankingVO> toRankingVOList(List<SeckillRankingDTO> dtoList)` | SeckillRankingDTO 列表 → SeckillRankingVO 列表 |

### 4.2 转换规则

- **VO → DTO**：全字段映射（核心 + 展示），保留前端展示信息
- **DTO → VO**：全字段映射（核心 + 展示），保留前端契约
- **SeckillOverviewDTO → SeckillOverviewVO**：字段名映射（`completedTodayCount` → `completedToday`），类型转换（`long` → `Integer`，`Math.toIntExact`），忽略 `totalActivities`
- **SeckillRankingDTO → SeckillRankingVO**：字段一一对应，类型转换（`long` → `Long`，自动装箱）

---

## 5. ApplicationService 委托映射

### 5.1 StatsApplicationService implements StatsApi

委托 `StatsService`（Strangler 过渡期）：

| StatsApi 方法 | 委托 StatsService 方法 | 转换 |
|---|---|---|
| `getOverview()` | `statsService.getOverview()` | `toDTO(vo)` |
| `getUserTrend(days)` | `statsService.getUserTrend(days)` | `toDTOList(voList)` |
| `getOrderTrend(days)` | `statsService.getOrderTrend(days)` | `toDTOList(voList)` |
| `getSeckillRanking(limit)` | `statsService.getSeckillRanking(limit)` | `SeckillApiConverter.toDTOList(voList)`（复用 seckill 模块 Converter） |
| `getOrderStatusDistribution()` | `statsService.getOrderStatusDistribution()` | `toStatusDTOList(voList)` |
| `getSeckillOverview()` | `statsService.getSeckillOverview()` | `SeckillApiConverter.toDTO(vo)`（复用 seckill 模块 Converter） |

> **Phase ST.2 阶段**：本节为设计预留，ApplicationService 在 Phase ST.4 创建。

---

## 6. Controller 适配（Phase ST.4）

`StatsController` 切换注入 `StatsApi` 替代旧 `StatsService`，调用 API 方法后通过 `StatsApiConverter` 转回 VO：

| Controller 方法 | 旧调用 | 新调用 |
|---|---|---|
| `overview()` | `statsService.getOverview()` | `statsApi.getOverview()` → `StatsApiConverter.toVO(dto)` |
| `userTrend(days)` | `statsService.getUserTrend(days)` | `statsApi.getUserTrend(days)` → `StatsApiConverter.toVOList(dtoList)` |
| `orderTrend(days)` | `statsService.getOrderTrend(days)` | `statsApi.getOrderTrend(days)` → `StatsApiConverter.toVOList(dtoList)` |
| `seckillRanking(limit)` | `statsService.getSeckillRanking(limit)` | `statsApi.getSeckillRanking(limit)` → `StatsApiConverter.toRankingVOList(dtoList)` |
| `orderStatusDistribution()` | `statsService.getOrderStatusDistribution()` | `statsApi.getOrderStatusDistribution()` → `StatsApiConverter.toStatusVOList(dtoList)` |
| `seckillOverview()` | `statsService.getSeckillOverview()` | `statsApi.getSeckillOverview()` → `StatsApiConverter.toOverviewVO(dto)` |

> **Phase ST.2 阶段**：本节为设计预留，Controller 适配在 Phase ST.4 执行。