# Stats 模块迁移计划 (STATS-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase ST - Stats 模块迁移（Strangler Pattern 第七阶段）
> 依据：PAYMENT-MIGRATION-PLAN.md + COUPON-MIGRATION-PLAN.md + CART-MIGRATION-PLAN.md + PRODUCT-MIGRATION-PLAN.md + ORDER-MIGRATION-PLAN.md + SECKILL-MIGRATION-PLAN.md + Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Stats 当前结构

### 1.1 当前包结构

Stats 相关代码散落在 3 个不同的顶层包中，共 **5 个核心文件**：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `StatsController` | `controller` | 后台数据看台：总览/趋势/排行榜/状态分布/秒杀概览（/api/v1/admin/stats） |
| `StatsService` | `service` | 数据看台统计服务接口（6 个方法） |
| `StatsServiceImpl` | `service.impl` | StatsService 实现（聚合多模块 API） |
| `StatsOverviewVO` | `vo` | 总览统计视图（用户/订单/秒杀/销售/商品/今日新增） |
| `TrendItemVO` | `vo` | 趋势数据项视图（日期 + 数量） |
| `OrderStatusItemVO` | `vo` | 订单状态分布项视图（status + count） |

**关键特征**：Stats 是**统计聚合模块**，无独立 Entity/Mapper，所有数据均通过跨模块 API 实时查询聚合：
- `UserApi`（identity 模块）：用户总数、今日注册数、用户注册趋势
- `SeckillOrderApi`（seckill 模块）：订单总数、销售总额、今日订单数、订单趋势、秒杀排行榜、订单状态分布
- `SeckillGoodsApi`（seckill 模块）：秒杀活动数、进行中/待开始/今日已完成数
- `ProductApi`（product 模块）：商品总数

### 1.2 Service 方法签名

#### StatsService（6 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `getOverview` | `StatsOverviewVO getOverview()` | 总览统计（用户/订单/秒杀/销售/商品/今日新增） | ⚠️ 返回 VO |
| `getUserTrend` | `List<TrendItemVO> getUserTrend(Integer days)` | 用户注册趋势（近 N 天每日注册数） | ⚠️ 返回 VO，单参数可保留 |
| `getOrderTrend` | `List<TrendItemVO> getOrderTrend(Integer days)` | 订单趋势（近 N 天每日订单数） | ⚠️ 返回 VO，单参数可保留 |
| `getSeckillRanking` | `List<SeckillRankingVO> getSeckillRanking(Integer limit)` | 秒杀排行榜 Top N | ⚠️ 返回 seckill 模块 VO（跨模块 VO 引用），单参数可保留 |
| `getOrderStatusDistribution` | `List<OrderStatusItemVO> getOrderStatusDistribution()` | 订单状态分布 | ⚠️ 返回 VO |
| `getSeckillOverview` | `SeckillOverviewVO getSeckillOverview()` | 秒杀活动概览（进行中/待开始/今日已完成） | ⚠️ 返回 seckill 模块 VO（跨模块 VO 引用） |

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `StatsController` | `/api/v1/admin/stats` | `GET /overview` | 总览统计 | ADMIN |
| | | `GET /user-trend` | 用户注册趋势（默认 7 天） | ADMIN |
| | | `GET /order-trend` | 订单趋势（默认 7 天） | ADMIN |
| | | `GET /seckill-ranking` | 秒杀排行榜 Top N（默认 10） | ADMIN |
| | | `GET /order-status-distribution` | 订单状态分布 | ADMIN |
| | | `GET /seckill-overview` | 秒杀活动概览 | ADMIN |

### 1.4 跨模块依赖关系

#### Stats 依赖的模块（出向依赖）

| 被依赖模块 | 依赖方式 | 用途 |
|---|---|---|
| **identity** | `UserApi` | `StatsServiceImpl.getOverview` 查用户总数、今日注册数；`getUserTrend` 查用户注册趋势 |
| **seckill** | `SeckillOrderApi` | `StatsServiceImpl.getOverview` 查订单总数、销售总额、今日订单数；`getOrderTrend` 查订单趋势；`getSeckillRanking` 查秒杀排行榜；`getOrderStatusDistribution` 查订单状态分布 |
| **seckill** | `SeckillGoodsApi` | `StatsServiceImpl.getOverview` 查秒杀活动数；`getSeckillOverview` 查进行中/待开始/今日已完成数 |
| **seckill** | `SeckillApiConverter` | `StatsServiceImpl.getSeckillRanking` 调用 `SeckillApiConverter.toRankingVOList` 将 DTO 转 VO |
| **product** | `ProductApi` | `StatsServiceImpl.getOverview` 查商品总数 |
| **order** | `OrderStatus`（infrastructure.persistence.entity） | `StatsServiceImpl` 引用 `OrderStatus.PAID/COMPLETED` 枚举 |

#### 依赖 Stats 的模块（入向依赖）

| 调用方模块 | 依赖方式 | 用途 |
|---|---|---|
| **无** | — | Stats 是顶层聚合模块，无其他模块依赖 Stats |

> **关键特征**：Stats 是**纯叶子模块**（无入向依赖），迁移风险最低，可放心重构。

### 1.5 VO 字段

#### StatsOverviewVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `userCount` | `Long` | 用户总数 |
| `orderCount` | `Long` | 订单总数 |
| `seckillCount` | `Long` | 秒杀活动数 |
| `totalSales` | `BigDecimal` | 销售总额（仅 PAID/COMPLETED） |
| `productCount` | `Long` | 商品总数 |
| `todayOrderCount` | `Long` | 今日订单数 |
| `todayUserCount` | `Long` | 今日注册数 |

#### TrendItemVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `date` | `String` | 日期字符串（yyyy-MM-dd） |
| `count` | `Long` | 当日数量 |

#### OrderStatusItemVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `status` | `String` | 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED |
| `count` | `Long` | 该状态订单数量 |

#### SeckillOverviewVO（来自 seckill 模块，stats 引用）

| 字段 | 类型 | 说明 |
|---|---|---|
| `activeCount` | `Integer` | 进行中秒杀数（采集失败为 null） |
| `pendingCount` | `Integer` | 待开始秒杀数（采集失败为 null） |
| `completedToday` | `Integer` | 今日已完成秒杀数（采集失败为 null） |

#### SeckillRankingVO（来自 seckill 模块，stats 引用）

| 字段 | 类型 | 说明 |
|---|---|---|
| `seckillId` | `Long` | 秒杀商品 ID |
| `seckillName` | `String` | 秒杀商品名称 |
| `salesCount` | `long` | 销售数量 |
| `salesAmount` | `BigDecimal` | 销售总额 |

### 1.6 已存在的可复用 DTO（seckill 模块）

stats 模块的 `getSeckillRanking` 和 `getSeckillOverview` 返回的 DTO 已在 seckill 模块 API 层定义，可直接复用：

| DTO | 全限定名 | 字段 | 用途 |
|---|---|---|---|
| `SeckillOverviewDTO` | `com.seckill.mall.seckill.api.dto.SeckillOverviewDTO` | `totalActivities`/`activeCount`/`pendingCount`/`completedTodayCount`（long） | 秒杀活动概览 |
| `SeckillRankingDTO` | `com.seckill.mall.seckill.api.dto.SeckillRankingDTO` | `seckillId`/`seckillName`/`salesCount`/`salesAmount` | 秒杀排行榜 |

> **字段映射差异**（由后续 Phase ST.4 Application 层 Converter 处理）：
> - `SeckillOverviewVO.completedToday`（Integer）↔ `SeckillOverviewDTO.completedTodayCount`（long）
> - `SeckillOverviewVO` 无 `totalActivities` 字段，`SeckillOverviewDTO` 有该字段（stats 场景置 0 或忽略）
> - `SeckillRankingVO` 与 `SeckillRankingDTO` 字段语义一致，仅类型差异（`Long` vs `long`）

### 1.7 Converter 依赖

| Converter | 位置 | 用途 |
|---|---|---|
| `SeckillApiConverter` | `seckill.application.facade` | `StatsServiceImpl.getSeckillRanking` 调用 `toRankingVOList` 将 `List<SeckillRankingDTO>` 转回 `List<SeckillRankingVO>` |

---

## 2. Stats 目标结构（Pragmatic DDD + Modular Monolith）

### 2.1 目标包结构

```
com.seckill.mall.stats/
├── package-info.java                                  # 模块根包
├── api/                                                # API 契约层（对外暴露）
│   ├── package-info.java
│   ├── StatsApi.java                                   # 统计 API（6 个方法）— 替代 StatsService
│   ├── dto/
│   │   ├── package-info.java
│   │   ├── StatsOverviewDTO.java                       # 总览统计 DTO（替代 StatsOverviewVO 跨模块传递）
│   │   ├── TrendItemDTO.java                           # 趋势数据项 DTO（替代 TrendItemVO 跨模块传递）
│   │   └── OrderStatusItemDTO.java                     # 订单状态分布项 DTO（替代 OrderStatusItemVO 跨模块传递）
│   └── result/
│       └── package-info.java                           # 预留结果对象包（当前无 Result 类）
├── application/                                        # 应用层
│   ├── package-info.java
│   ├── StatsApplicationService.java                    # 实现 StatsApi，委托 StatsService
│   └── facade/
│       ├── package-info.java
│       └── StatsApiConverter.java                      # VO ↔ DTO 转换
└── interfaces/                                         # 接口层
    ├── package-info.java
    ├── vo/
    │   ├── package-info.java
    │   ├── StatsOverviewVO.java                        # 从 vo 包迁入（前端展示用）
    │   ├── TrendItemVO.java                            # 从 vo 包迁入
    │   └── OrderStatusItemVO.java                      # 从 vo 包迁入
    └── web/
        ├── package-info.java
        └── StatsController.java                        # 从 controller 包迁入
```

> **注意**：stats 模块**无 infrastructure 层**（无 Entity/Mapper），**无 domain 层**（无独立领域模型），**无 command/query 包**（所有方法入参均为单参数 Integer，不构成"裸露多参数"反模式）。

### 2.2 分层依赖规则（目标）

```
interfaces ──→ application ──→ api
                     │
                     └──→ service（旧层，Strangler 过渡期）
                               │
                               └──→ 其他模块 API（identity/seckill/product）

api ──✗──→ infrastructure     （API 不依赖基础设施，stats 无 infrastructure）
application ──✗──→ mapper     （Application 不直接依赖 Mapper，stats 无 Mapper）
interfaces ──✗──→ mapper      （Interfaces 不直接依赖 Mapper，stats 无 Mapper）
```

### 2.3 与其他模块的关系（目标）

| 关系 | 模块 | 依赖方式 |
|---|---|---|
| Stats → Identity | `UserApi` | 用户总数、今日注册数、用户注册趋势 |
| Stats → Seckill | `SeckillOrderApi` | 订单总数、销售总额、今日订单数、订单趋势、秒杀排行榜、订单状态分布 |
| Stats → Seckill | `SeckillGoodsApi` | 秒杀活动数、进行中/待开始/今日已完成数 |
| Stats → Seckill | `SeckillOverviewDTO`/`SeckillRankingDTO`（复用） | 秒杀概览与排行榜 DTO |
| Stats → Product | `ProductApi` | 商品总数 |
| Stats → Order | `OrderStatus`（枚举） | 订单状态枚举（PAID/COMPLETED） |
| 无 → Stats | — | Stats 是纯叶子模块，无入向依赖 |

---

## 3. 文件迁移路径

### 3.1 移动文件（git mv）

| 源路径 | 目标路径 | 阶段 |
|---|---|---|
| `vo/StatsOverviewVO.java` | `stats/interfaces/vo/StatsOverviewVO.java` | Phase ST.4 |
| `vo/TrendItemVO.java` | `stats/interfaces/vo/TrendItemVO.java` | Phase ST.4 |
| `vo/OrderStatusItemVO.java` | `stats/interfaces/vo/OrderStatusItemVO.java` | Phase ST.4 |
| `controller/StatsController.java` | `stats/interfaces/web/StatsController.java` | Phase ST.4 |

### 3.2 新增文件

| 目标路径 | 阶段 |
|---|---|
| `stats/package-info.java` 及所有子包 `package-info.java`（共 9 个） | Phase ST.0 |
| `stats/api/StatsApi.java` | Phase ST.2 |
| `stats/api/dto/{StatsOverviewDTO,TrendItemDTO,OrderStatusItemDTO}.java` | Phase ST.2 |
| `stats/application/StatsApplicationService.java` | Phase ST.4 |
| `stats/application/facade/StatsApiConverter.java` | Phase ST.4 |

### 3.3 保留不动的文件

| 文件 | 原因 |
|---|---|
| `service/StatsService.java` | Strangler 过渡期保留，StatsApplicationService 委托它 |
| `service/impl/StatsServiceImpl.java` | 同上 |

### 3.4 后续可删除的文件（Strangler 完成后）

| 文件 | 删除时机 |
|---|---|
| `service/StatsService.java` + `service/impl/StatsServiceImpl.java` | 业务逻辑完全迁入 stats.application 后 |

---

## 4. API 边界（StatsApi）

### 4.1 设计原则

1. **方法用业务语言命名**，禁止 CRUD 命名
2. **入参用 Command/Query 对象**，禁止裸露多参数（stats 所有方法均为单参数 Integer，不构成反模式，保留裸参数）
3. **出参用 Result/DTO/Snapshot**，禁止暴露 Entity/Mapper/PO
4. **异常通过 `BusinessException(ErrorCode)` 抛出**，不泄露堆栈
5. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型
6. **复用已有 DTO**：秒杀概览与排行榜 DTO 复用 seckill 模块定义，避免重复建模

### 4.2 StatsApi 方法清单

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `getOverview` | `StatsOverviewDTO getOverview()` | `StatsService.getOverview` |
| `getUserTrend` | `List<TrendItemDTO> getUserTrend(Integer days)` | `StatsService.getUserTrend` |
| `getOrderTrend` | `List<TrendItemDTO> getOrderTrend(Integer days)` | `StatsService.getOrderTrend` |
| `getSeckillRanking` | `List<SeckillRankingDTO> getSeckillRanking(Integer limit)` | `StatsService.getSeckillRanking` |
| `getOrderStatusDistribution` | `List<OrderStatusItemDTO> getOrderStatusDistribution()` | `StatsService.getOrderStatusDistribution` |
| `getSeckillOverview` | `SeckillOverviewDTO getSeckillOverview()` | `StatsService.getSeckillOverview` |

> **关键变化**：
> - 6 个方法返回类型从 VO 改为 DTO，消除 VO 跨模块传递。
> - `getSeckillRanking` 返回 `List<SeckillRankingVO>`（seckill 模块 VO）→ `List<SeckillRankingDTO>`（复用 seckill 模块 DTO），消除跨模块 VO 引用。
> - `getSeckillOverview` 返回 `SeckillOverviewVO`（seckill 模块 VO）→ `SeckillOverviewDTO`（复用 seckill 模块 DTO），消除跨模块 VO 引用。
> - 入参 `Integer days`/`Integer limit` 保留裸参数（单参数不构成反模式）。

### 4.3 DTO 定义

详见 [STATS-API-CONTRACT.md](STATS-API-CONTRACT.md)。

---

## 5. 迁移步骤（Phase ST）

### Phase ST.0：创建包结构

- 创建 `stats/` 及所有子包（共 9 个 package-info.java）
- 每个包创建 `package-info.java`
- **不修改任何现有代码**

### Phase ST.2：创建 API 层

- 创建 `StatsApi` 接口
- 创建 3 个 DTO（`StatsOverviewDTO`、`TrendItemDTO`、`OrderStatusItemDTO`）
- 复用 seckill 模块的 `SeckillOverviewDTO`、`SeckillRankingDTO`（import 即可，不重新定义）
- **不修改任何现有代码**

### Phase ST.4：创建 Application 层 + 移动 Controller/VO

- 创建 `StatsApplicationService implements StatsApi`，委托 `StatsService`
- 创建 `StatsApiConverter`：VO ↔ DTO 转换，处理 `SeckillOverviewVO` ↔ `SeckillOverviewDTO` 字段映射差异
- `git mv vo/StatsOverviewVO.java stats/interfaces/vo/StatsOverviewVO.java`
- `git mv vo/TrendItemVO.java stats/interfaces/vo/TrendItemVO.java`
- `git mv vo/OrderStatusItemVO.java stats/interfaces/vo/OrderStatusItemVO.java`
- `git mv controller/StatsController.java stats/interfaces/web/StatsController.java`
- 更新 package 声明与所有 import
- 切换 Controller 注入 `StatsApi` 替代旧 `StatsService`，调用 API 方法后通过 Converter 转回 VO
- **不删除旧 StatsService / StatsServiceImpl**（Strangler 过渡）
- **回归测试**：mvn test

### Phase ST.5：ArchUnit 规则

新增 2 条 stats 模块包边界规则（无 infrastructure，故无 mapper 规则）：

| 规则 | 内容 |
|---|---|
| `stats_api_should_not_depend_on_infrastructure` | `stats.api` 不应依赖 `stats.infrastructure`（预留，stats 当前无 infrastructure） |
| `stats_interfaces_should_not_depend_on_service` | `stats.interfaces` 不应直接依赖旧 `service` 包（强制走 application 层） |

> **注意**：stats 模块无 infrastructure 层，故无需 `application_should_not_depend_on_mapper` 规则。

**回归标准**：mvn test Failures ≤ 1, Errors ≤ 19, ArchUnit 全绿。

---

## 6. 风险点与缓解

### 6.1 SeckillOverviewVO 与 SeckillOverviewDTO 字段映射差异

**风险**：`SeckillOverviewVO`（seckill 模块 VO）字段为 `activeCount`/`pendingCount`/`completedToday`（Integer），而 `SeckillOverviewDTO`（seckill 模块 DTO）字段为 `totalActivities`/`activeCount`/`pendingCount`/`completedTodayCount`（long）。`StatsServiceImpl.getSeckillOverview` 当前返回 `SeckillOverviewVO`，切换到 `StatsApi.getSeckillOverview` 返回 `SeckillOverviewDTO` 后需适配字段映射。

**缓解**：`StatsApiConverter.toVO(SeckillOverviewDTO)` 在 Phase ST.4 处理映射：
- `dto.activeCount` → `vo.activeCount`（long → Integer，Math.toIntExact）
- `dto.pendingCount` → `vo.pendingCount`
- `dto.completedTodayCount` → `vo.completedToday`
- `dto.totalActivities` 忽略（VO 无此字段）

详见 [STATS-API-CONTRACT.md](STATS-API-CONTRACT.md) 第 5 节。

### 6.2 StatsServiceImpl 对 SeckillApiConverter 的跨模块依赖

**风险**：`StatsServiceImpl.getSeckillRanking` 调用 `SeckillApiConverter.toRankingVOList`（seckill 模块 facade），属于 stats → seckill 的 facade 层依赖。迁移到 `StatsApplicationService` 后，若仍调用 `SeckillApiConverter`，则 stats.application 依赖 seckill.application.facade，违反模块边界。

**缓解**：Phase ST.4 后，`StatsApplicationService.getSeckillRanking` 直接返回 `List<SeckillRankingDTO>`（StatsApi 契约），不再调用 `SeckillApiConverter.toRankingVOList`。Converter 调用下沉到 Controller 层：`StatsController` 调用 `StatsApiConverter.toVOList(dtoList)` 将 DTO 转回 VO。`StatsApiConverter` 位于 stats.application.facade，不依赖 seckill.application.facade。

### 6.3 StatsController 返回 VO 的前端契约

**风险**：`StatsController` 6 个端点返回 `Result<StatsOverviewVO>`/`Result<List<TrendItemVO>>` 等，前端依赖 VO 字段。切换为 DTO 可能破坏前端契约。

**缓解**：**Controller 仍返回 VO**（保留前端契约），内部调用 `StatsApi` 拿到 DTO 后通过 `StatsApiConverter.toVO` 转回 VO。3 个 VO 迁入 `stats/interfaces/vo/` 但保留所有字段。

### 6.4 StatsServiceImpl 对 OrderStatus 枚举的跨模块依赖

**风险**：`StatsServiceImpl` 引用 `com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus`（order 模块 infrastructure 层），属于 stats → order.infrastructure 的依赖，违反模块边界（不应依赖其他模块的 infrastructure）。

**缓解**：本次迁移**不动** `StatsServiceImpl`（Strangler 过渡期保留）。后续可推动 order 模块将 `OrderStatus` 提升到 `order.api` 或 `order.domain` 层，再切换 stats 的 import。不在本次迁移范围。

### 6.5 Strangler 过渡期 StatsService 保留

**风险**：`StatsService` + `StatsServiceImpl` 在过渡期保留，可能与 `StatsApi` + `StatsApplicationService` 并存导致语义重复。

**缓解**：`StatsApplicationService` 仅做门面委托，不含业务逻辑；待稳定后再将业务逻辑从旧 `StatsServiceImpl` 迁入 `StatsApplicationService` 并删除旧 Service（不在本次迁移范围）。

### 6.6 ArchUnit freeze 模式

**风险**：新增 2 条 stats 模块规则后，freeze 模式首次运行会冻结当前违规。

**缓解**：Phase ST.4 保证 `StatsApplicationService` 不直接 import 旧 `StatsService` 之外的 service 包；Phase ST.4 保证 `StatsController` 不再直接 import `StatsService`。新增规则后跑 ArchUnit 验证全绿。

---

## 7. 回归测试基线

| 指标 | 基线 | 目标 |
|---|---|---|
| Tests run | 129 | ≥ 129 |
| Failures | 1 | ≤ 1 |
| Errors | 19 | ≤ 19 |
| ArchUnit 规则 | 39 全绿 | 41 全绿（新增 2 条 stats 规则） |

---

## 8. 参考文档

- [PAYMENT-MIGRATION-PLAN.md](PAYMENT-MIGRATION-PLAN.md) - Payment 模块迁移经验（最近完成，主要参考）
- [SECKILL-MIGRATION-PLAN.md](SECKILL-MIGRATION-PLAN.md) - Seckill 模块迁移经验（提供可复用 DTO）
- [COUPON-MIGRATION-PLAN.md](COUPON-MIGRATION-PLAN.md) - Coupon 模块迁移经验
- [CART-MIGRATION-PLAN.md](CART-MIGRATION-PLAN.md) - Cart 模块迁移经验
- [PRODUCT-MIGRATION-PLAN.md](PRODUCT-MIGRATION-PLAN.md) - Product 模块迁移经验
- [ORDER-MIGRATION-PLAN.md](ORDER-MIGRATION-PLAN.md) - Order 模块迁移经验
- [STATS-API-CONTRACT.md](STATS-API-CONTRACT.md) - Stats 模块 API 契约