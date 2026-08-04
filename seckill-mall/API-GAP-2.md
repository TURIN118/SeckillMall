# 前端接口需求缺口文档 2（API-GAP-2）

> **文档目的**：补充 `API-GAP.md` 之外的新需求。本文档聚焦两件事：
> 1. 系统健康接口需扩展返回**资源监控字段**（CPU/内存/磁盘/Redis 命中率等），供 P17 系统健康页展示真实数据；
> 2. 操作日志接口已存在但**无数据**，需后端通过 AOP 切面记录关键操作到 `t_operation_log` 表。
>
> **生成日期**：2026-08-02
> **关联文档**：`API-GAP.md`（前端接口需求缺口文档 1）
> **后端接口文档版本**：1.0
> **前端设计规范**：SeckillMall Frontend AI Design Mode Specification（P16 操作日志 / P17 系统健康）

---

## 一、文档概述

| 编号 | 主题 | 涉及接口 | 现状 | 需求类型 |
|---|---|---|---|---|
| GAP2-1 | 系统资源监控接口 | `GET /api/v1/admin/system/health` | 接口已存在，仅返回 `redis/database/mq/allHealthy`，**不含资源监控数据** | 扩展响应字段 |
| GAP2-2 | 操作日志记录说明 | `GET /api/v1/admin/operation-logs` | 接口已存在，但 `t_operation_log` 表**几乎无数据** | 后端补充 AOP 记录逻辑 |

> **说明**：本文档不新增接口地址，仅对已存在接口提出**响应扩展**与**数据生产**要求。前端已对 GAP2-1 做兼容处理（字段可选，缺失时显示占位 `—` 而非假数据），后端补充字段后前端自动展示真实数据。

---

## 二、GAP2-1：系统资源监控接口（扩展现有健康检查响应）

### 接口名称
系统健康检查（扩展资源监控字段）

### 接口地址
`GET /api/v1/admin/system/health`

### 请求方式
`GET`

### 请求数据类型
`application/x-www-form-urlencoded`

### 请求参数
无

### 现有响应结构（`SystemHealthVO`）
| 参数名称 | 参数说明 | 类型 | 备注 |
|---|---|---|---|
| redis | Redis 服务状态 | string | `UP`/`DOWN` |
| database | 数据库服务状态 | string | `UP`/`DOWN` |
| mq | RabbitMQ 服务状态 | string | `UP`/`DOWN` |
| allHealthy | 是否全部健康 | boolean | `redis/database/mq` 均为 `UP` 时为 true |

### 扩展响应结构（`SystemResourceVO`，新增字段均为**可选**）

| 参数名称 | 参数说明 | 类型 | 是否必须 | 备注 |
|---|---|---|---|---|
| redis | Redis 服务状态 | string | 是 | `UP`/`DOWN`，原有字段 |
| database | 数据库服务状态 | string | 是 | `UP`/`DOWN`，原有字段 |
| mq | RabbitMQ 服务状态 | string | 是 | `UP`/`DOWN`，原有字段 |
| allHealthy | 是否全部健康 | boolean | 是 | 原有字段 |
| cpuUsage | CPU 使用率 | number | 否 | 0-100，保留 1 位小数 |
| memoryUsage | 内存使用率 | number | 否 | 0-100，保留 1 位小数 |
| diskUsage | 磁盘使用率 | number | 否 | 0-100，保留 1 位小数 |
| redisHitRate | Redis 缓存命中率 | number | 否 | 0-100，保留 1 位小数，过去 1 小时平均 |
| redisResponseTime | Redis 响应时间 | string | 否 | 人类可读，如 `"2ms"` |
| dbPoolUsage | 数据库连接池使用情况 | string | 否 | 人类可读，如 `"12/50"` |
| mqQueueBacklog | MQ 队列积压 | string | 否 | 人类可读，如 `"0"` |

> **兼容性约束**：新增字段全部可选。后端未扩展时仅返回原有 4 个字段，前端对缺失字段显示占位 `—`（数值类进度条按 0 渲染空条），**不展示任何 mock 假数据**。后端补充字段后前端自动展示真实数据，无需再次发版。

### 响应示例（后端扩展后）
```json
{
  "code": 0,
  "message": "",
  "data": {
    "redis": "UP",
    "database": "UP",
    "mq": "UP",
    "allHealthy": true,
    "cpuUsage": 23.4,
    "memoryUsage": 61.2,
    "diskUsage": 45.8,
    "redisHitRate": 98.7,
    "redisResponseTime": "2ms",
    "dbPoolUsage": "12/50",
    "mqQueueBacklog": "0"
  },
  "timestamp": "2026-08-02T10:00:00.000Z"
}
```

### 响应示例（后端未扩展，兼容现状）
```json
{
  "code": 0,
  "message": "",
  "data": {
    "redis": "UP",
    "database": "UP",
    "mq": "UP",
    "allHealthy": true
  },
  "timestamp": "2026-08-02T10:00:00.000Z"
}
```
> 此时前端资源区显示 `—`，服务卡片 meta 显示 `—`，不报错。

### 使用场景
- **页面**：P17 系统健康（`src/views/admin/SystemHealth.vue`）
- **组件区域**：
  - 服务卡片（Redis/MySQL/RabbitMQ）的 meta 文本：`redisResponseTime` / `dbPoolUsage` / `mqQueueBacklog`
  - 资源监控列表（CPU/内存/磁盘使用率进度条）：`cpuUsage` / `memoryUsage` / `diskUsage`
  - Redis 缓存命中率大数字：`redisHitRate`
- **权限**：`role=ADMIN`
- **刷新策略**：前端每 30 秒自动调用一次，并提供手动"刷新"按钮

### 与现有接口的关系
- 本接口为现有 `GET /api/v1/admin/system/health` 的**响应扩展**，不改变请求方式与路径
- 后端实现建议：在 `SystemServiceImpl.getSystemHealth()` 中追加资源采集逻辑（如读取 `OperatingSystemMXBean` 获取 CPU/内存，读取 `File` 磁盘空间，通过 `RedisTemplate` 执行 `INFO stats` 计算 hit rate），填充 `SystemHealthVO` 的新字段后返回

### 前端已完成的兼容改造
- `src/api/system.ts`：新增 `SystemResourceVO` 接口（扩展自 `SystemHealthVO`），`getSystemHealth()` 返回类型改为 `Result<SystemResourceVO>`
- `src/views/admin/SystemHealth.vue`：移除写死的 mock（`cpuUsage=23/memUsage=61/diskUsage=45/hitRate=98.7` 及服务卡片 `'响应 2ms'/'连接池 12/50'/'队列积压 0'`），改为从 `health.value` 读取，缺失时显示 `—`

---

## 三、GAP2-2：操作日志记录说明

### 现状结论
经后端代码调查（`seckill-mall` 模块），操作日志的**基础设施已完整实现**：

| 组件 | 路径 | 状态 |
|---|---|---|
| 注解 | `com.seckill.mall.annotation.OperationLog` | 已实现（`module/action/targetIdSpEL/targetType`） |
| AOP 切面 | `com.seckill.mall.aspect.OperationLogAspect` | 已实现（`@AfterReturning` + SpEL 解析 + IP 抓取） |
| 记录器 | `com.seckill.mall.aspect.OperationLogRecorder` | 已实现 |
| Mapper | `com.seckill.mall.mapper.OperationLogMapper` | 已实现 |
| 实体/表 | `com.seckill.mall.entity.OperationLog` → `t_operation_log` | 已实现 |
| 查询接口 | `GET /api/v1/admin/operation-logs` | 已实现（分页 + module 筛选） |

**但关键问题在于：仅有 `AdminUserController` 的 2 个方法标注了 `@OperationLog`**：
- `PUT /api/v1/admin/users/{userId}/status` → `module=USER, action=UPDATE_STATUS`
- `PUT /api/v1/admin/users/{userId}/role` → `module=USER, action=UPDATE_ROLE`

其余关键操作（登录/登出/商品 CRUD/分类 CRUD/秒杀活动管理/订单操作等）**均未标注 `@OperationLog`**，导致 `t_operation_log` 表几乎无数据，前端 P16 操作日志页查询返回空列表。

### 需求：补充 `@OperationLog` 注解覆盖范围
后端需在以下 Controller 方法上补充 `@OperationLog` 注解，使操作日志切面自动记录到 `t_operation_log` 表：

| 模块 | 操作 (action) | 建议注解 | 涉及接口（示例） |
|---|---|---|---|
| AUTH | LOGIN | `@OperationLog(module="AUTH", action="LOGIN", targetType="USER")` | `POST /api/v1/auth/login` |
| AUTH | LOGOUT | `@OperationLog(module="AUTH", action="LOGOUT", targetType="USER")` | `POST /api/v1/auth/logout` |
| PRODUCT | CREATE | `@OperationLog(module="PRODUCT", action="CREATE", targetIdSpEL="#req.productId", targetType="PRODUCT")` | `POST /api/v1/products` |
| PRODUCT | UPDATE | `@OperationLog(module="PRODUCT", action="UPDATE", targetIdSpEL="#id", targetType="PRODUCT")` | `PUT /api/v1/products/{id}` |
| PRODUCT | DELETE | `@OperationLog(module="PRODUCT", action="DELETE", targetIdSpEL="#id", targetType="PRODUCT")` | `DELETE /api/v1/products/{id}` |
| CATEGORY | CREATE / UPDATE / DELETE / STATUS | `@OperationLog(module="CATEGORY", action="...", targetIdSpEL="#id", targetType="CATEGORY")` | `POST/PUT/DELETE /api/v1/categories[/{id}]`（参见 API-GAP.md 缺口 1-4） |
| SECKILL | CREATE / UPDATE / CANCEL | `@OperationLog(module="SECKILL", action="...", targetIdSpEL="#seckillId", targetType="SECKILL")` | `POST /api/v1/seckill/admin`、`PUT /api/v1/seckill/admin/{seckillId}`、`PUT /api/v1/seckill/admin/{seckillId}/cancel` |
| ORDER | PAY / CANCEL | `@OperationLog(module="ORDER", action="...", targetIdSpEL="#orderId", targetType="ORDER")` | `POST /api/v1/orders/{orderId}/pay`、`POST /api/v1/orders/{orderId}/cancel` |
| USER | UPDATE_STATUS / UPDATE_ROLE | （已存在，无需改动） | `PUT /api/v1/admin/users/{userId}/status`、`PUT /api/v1/admin/users/{userId}/role` |

> **登录特殊处理**：`POST /api/v1/auth/login` 在认证成功前 `SecurityUtils.getCurrentUserId()` 可能取不到操作人 ID。建议在 `OperationLogAspect.fillOperator` 中对登录场景做兼容：若取不到当前用户 ID，可从登录请求参数的 `username` 回查用户 ID，或在认证成功后通过 `SecurityContext` 获取。

### 记录字段说明（`t_operation_log` 表）
| 字段 | 说明 | 来源 |
|---|---|---|
| operator_id | 操作人用户 ID | `SecurityUtils.getCurrentUserId()` |
| operator_name | 操作人用户名 | 关联 `t_user` 查询（VO 拼装） |
| module | 业务模块 | 注解 `module` 属性 |
| action | 具体操作 | 注解 `action` 属性 |
| target_id | 操作目标 ID | 注解 `targetIdSpEL` 解析 |
| target_type | 目标类型 | 注解 `targetType`，缺省取 `module` |
| detail | 操作详情 | （可选）扩展记录变更摘要 |
| ip_address | 客户端 IP | `X-Forwarded-For` / `X-Real-IP` / `RemoteAddr` |
| operation_time | 操作时间 | 服务端写入时间 |

### 查询接口（已存在，无需改动）
- **接口**：`GET /api/v1/admin/operation-logs`
- **请求参数**：`pageNum` / `pageSize` / `module`（可选）/ `operatorId`（可选）
- **响应**：`Result<PageResult<OperationLogVO>>`
- **权限**：`role=ADMIN`
- **前端调用**：`src/views/admin/OperationLog.vue` → `getOperationLogs({pageNum, pageSize, module})`，参数名与后端 `OperationLogQueryRequest` 完全匹配，**前端无需改动**

### 结论
- 操作日志接口本身**无问题**，前端调用**正确**
- 日志空白的根因是**后端记录逻辑覆盖不足**：仅用户状态/角色变更 2 个操作被记录
- 修复方式：后端按上表为各关键 Controller 方法补充 `@OperationLog` 注解即可，无需改动前端

---

## 四、变更影响与兼容性

| 变更项 | 影响范围 | 兼容性 |
|---|---|---|
| GAP2-1 响应扩展 | `SystemHealthVO` 新增可选字段 | 完全向后兼容：旧前端忽略新字段；新前端对缺失字段显示 `—` |
| GAP2-2 注解补充 | 后端 Controller 新增注解 | 对前端透明：日志数据量增加，P16 页面自动展示 |

---

## 五、验收标准

### GAP2-1 验收
1. 调用 `GET /api/v1/admin/system/health`，响应 `data` 中包含 `cpuUsage/memoryUsage/diskUsage/redisHitRate` 数值字段及 `redisResponseTime/dbPoolUsage/mqQueueBacklog` 字符串字段
2. P17 系统健康页资源监控区显示真实 CPU/内存/磁盘使用率与 Redis 命中率，服务卡片显示真实响应时间/连接池/积压
3. 后端未扩展时，P17 页面不报错，资源区显示 `—`，不显示 mock 假数据

### GAP2-2 验收
1. 执行登录、商品增删改、秒杀活动管理、订单支付/取消等操作后，`t_operation_log` 表产生对应记录
2. P16 操作日志页能查询到上述操作记录，分页与模块筛选正常
3. 各记录的 `operator_id/module/action/target_id/ip_address/operation_time` 字段正确填充