# 前端接口需求缺口文档（API-GAP）

> **文档目的**：对比后端已实现接口（`default.md`）与前端设计规范（`10-ai-design-spec.md`）所需接口，明确列出后端尚未开发但前端页面/组件需要的接口定义与字段说明，供后端开发补充实现。
>
> **生成日期**：2026-08-02
> **后端接口文档版本**：1.0（共 34 个接口）
> **前端设计规范**：SeckillMall Frontend AI Design Mode Specification（P01-P17 共 17 个页面 + 6 个公共组件）

---

## 一、文档对比概述

### 1.1 后端已实现接口清单（共 34 个）

| 模块 | 接口数 | 接口列表 |
|---|---|---|
| 商品管理 | 5 | `GET/POST /api/v1/products`、`GET/PUT/DELETE /api/v1/products/{id}` |
| 系统管理 | 3 | `GET /api/v1/admin/system/health`、`GET /api/v1/admin/operation-logs`、`GET /api/v1/admin/dashboard` |
| 后台用户管理 | 4 | `GET /api/v1/admin/users`、`PUT /api/v1/admin/users/{userId}/status`、`PUT /api/v1/admin/users/{userId}/role`、`GET /api/v1/admin/users/{userId}/logs` |
| 订单管理 | 5 | `GET /api/v1/orders`、`GET /api/v1/orders/{orderId}`、`GET /api/v1/orders/{orderId}/status`、`POST /api/v1/orders/{orderId}/pay`、`POST /api/v1/orders/{orderId}/cancel` |
| 秒杀活动 | 9 | `GET /api/v1/seckill/list`、`GET /api/v1/seckill/{seckillId}`、`GET /api/v1/seckill/{seckillId}/stock`、`GET /api/v1/seckill/{seckillId}/token`、`POST /api/v1/seckill/{seckillId}`、`GET /api/v1/seckill/{seckillId}/result`、`POST /api/v1/seckill/admin`、`PUT /api/v1/seckill/admin/{seckillId}`、`PUT /api/v1/seckill/admin/{seckillId}/cancel` |
| 用户认证 | 7 | `POST /api/v1/auth/login`、`POST /api/v1/auth/register`、`GET /api/v1/auth/captcha`、`POST /api/v1/auth/refresh`、`GET /api/v1/auth/me`、`PUT /api/v1/auth/password`、`POST /api/v1/auth/logout` |
| 分类管理 | 1 | `GET /api/v1/categories`（仅查询分类树） |

### 1.2 前端页面接口需求与缺口总览

| 页面 | 路由 | 所需接口数 | 已实现 | 缺口数 | 缺口接口 |
|---|---|---|---|---|---|
| P10 仪表盘 | `/admin` | 3 | 1 | 2 | 近7天订单趋势、订单状态分布 |
| P11 商品管理 | `/admin/products` | 6 | 5 | 1 | 图片上传 |
| P12 分类管理 | `/admin/categories` | 5 | 1 | 4 | 新增/编辑/删除/状态切换分类 |
| P13 秒杀管理 | `/admin/seckills` | 5 | 4 | 1 | 图片上传（与 P11 共用） |
| P14 后台订单管理 | `/admin/orders` | 1 | 0 | 1 | 后台订单高级筛选列表 |
| P09 个人中心 | `/user/profile` | 4 | 2 | 2 | 头像上传、个人信息更新 |
| **合计** | — | — | — | **11** | — |

> **说明**：图片上传接口为 P11、P13 共用，去重后实际新增接口数为 **10 个**。

---

## 二、缺口接口详细定义

### 缺口 1：分类管理 - 新增分类

#### 接口名称
新增商品分类

#### 接口地址
`POST /api/v1/categories`

#### 请求方式
`POST`

#### 请求数据类型
`application/json`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| categoryName | 分类名称 | body | 是 | string | 1-32 个字符 |
| parentId | 父分类 ID | body | 是 | integer(int64) | 0 表示一级分类 |
| sortOrder | 排序值 | body | 否 | integer(int32) | 默认 0，值越小越靠前 |
| status | 状态 | body | 否 | integer(int32) | 1=启用（默认），0=禁用 |

#### 请求示例
```json
{
  "categoryName": "手机数码",
  "parentId": 0,
  "sortOrder": 1,
  "status": 1
}
```

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | 0 表示成功 |
| message | 提示信息 | string | |
| data | 返回数据 | CategoryVO | CategoryVO |
| &emsp;&emsp;id | 分类 ID | integer(int64) | |
| &emsp;&emsp;parentId | 父分类 ID | integer(int64) | |
| &emsp;&emsp;categoryName | 分类名称 | string | |
| &emsp;&emsp;sortOrder | 排序值 | integer(int32) | |
| &emsp;&emsp;status | 状态 | integer(int32) | 1=启用，0=禁用 |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "新增分类成功",
  "data": {
    "id": 1001,
    "parentId": 0,
    "categoryName": "手机数码",
    "sortOrder": 1,
    "status": 1
  },
  "timestamp": "2026-08-02T10:00:00.000Z"
}
```

#### 使用场景
- **页面**：P12 分类管理（`src/views/admin/CategoryManage.vue`）
- **组件**：新增分类对话框（点击 "新增分类" 按钮触发）
- **权限**：`role=ADMIN`

#### 与现有接口的关系
- 配合现有 `GET /api/v1/categories` 使用：新增成功后需重新调用分类树接口刷新列表
- 与下方"编辑分类"、"删除分类"接口共同构成分类管理完整 CRUD

---

### 缺口 2：分类管理 - 编辑分类

#### 接口名称
编辑商品分类

#### 接口地址
`PUT /api/v1/categories/{id}`

#### 请求方式
`PUT`

#### 请求数据类型
`application/json`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| id | 分类 ID | path | 是 | integer(int64) | |
| categoryName | 分类名称 | body | 否 | string | 1-32 个字符 |
| parentId | 父分类 ID | body | 否 | integer(int64) | 修改父分类实现移动节点 |
| sortOrder | 排序值 | body | 否 | integer(int32) | |
| status | 状态 | body | 否 | integer(int32) | 1=启用，0=禁用 |

#### 请求示例
```json
{
  "categoryName": "手机数码-更新",
  "sortOrder": 2,
  "status": 1
}
```

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | CategoryVO | CategoryVO |
| &emsp;&emsp;id | 分类 ID | integer(int64) | |
| &emsp;&emsp;parentId | 父分类 ID | integer(int64) | |
| &emsp;&emsp;categoryName | 分类名称 | string | |
| &emsp;&emsp;sortOrder | 排序值 | integer(int32) | |
| &emsp;&emsp;status | 状态 | integer(int32) | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "编辑分类成功",
  "data": {
    "id": 1001,
    "parentId": 0,
    "categoryName": "手机数码-更新",
    "sortOrder": 2,
    "status": 1
  },
  "timestamp": "2026-08-02T10:05:00.000Z"
}
```

#### 使用场景
- **页面**：P12 分类管理（`src/views/admin/CategoryManage.vue`）
- **组件**：编辑分类对话框（点击表格行 "编辑" 按钮触发）
- **权限**：`role=ADMIN`

#### 与现有接口的关系
- 配合 `GET /api/v1/categories` 刷新列表
- 业务约束：不允许将分类移动到自己的子分类下（防止循环引用）

---

### 缺口 3：分类管理 - 删除分类

#### 接口名称
删除商品分类（逻辑删除）

#### 接口地址
`DELETE /api/v1/categories/{id}`

#### 请求方式
`DELETE`

#### 请求数据类型
`application/x-www-form-urlencoded`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| id | 分类 ID | path | 是 | integer(int64) | |

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | object | 无数据返回 |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "删除分类成功",
  "data": {},
  "timestamp": "2026-08-02T10:10:00.000Z"
}
```

#### 使用场景
- **页面**：P12 分类管理（`src/views/admin/CategoryManage.vue`）
- **组件**：表格行 "删除" 操作按钮
- **权限**：`role=ADMIN`

#### 与现有接口的关系
- 配合 `GET /api/v1/categories` 刷新列表
- **业务约束**：若分类下存在子分类或关联商品，应返回错误码并提示用户先清理子分类或转移商品
- 建议返回错误码示例：
  - `code=40001`：分类下存在子分类，无法删除
  - `code=40002`：分类下存在商品，无法删除

---

### 缺口 4：分类管理 - 切换分类状态

#### 接口名称
启用/禁用商品分类

#### 接口地址
`PUT /api/v1/categories/{id}/status`

#### 请求方式
`PUT`

#### 请求数据类型
`application/json`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| id | 分类 ID | path | 是 | integer(int64) | |
| status | 目标状态 | body | 是 | integer(int32) | 1=启用，0=禁用 |

#### 请求示例
```json
{
  "status": 0
}
```

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | object | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "状态更新成功",
  "data": {},
  "timestamp": "2026-08-02T10:15:00.000Z"
}
```

#### 使用场景
- **页面**：P12 分类管理（`src/views/admin/CategoryManage.vue`）
- **组件**：表格行状态列 `el-switch` 切换
- **权限**：`role=ADMIN`

#### 与现有接口的关系
- 可视为"编辑分类"接口的特化版本，单独提供便于权限细粒度控制
- 若后端不单独实现，前端可复用"编辑分类"接口仅传 `status` 字段

---

### 缺口 5：仪表盘 - 近7天订单趋势

#### 接口名称
近7天订单趋势统计

#### 接口地址
`GET /api/v1/admin/dashboard/order-trend`

#### 请求方式
`GET`

#### 请求数据类型
`application/x-www-form-urlencoded`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| days | 统计天数 | query | 否 | integer(int32) | 默认 7，最大 30 |

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | OrderTrendVO | OrderTrendVO |
| &emsp;&emsp;dates | 日期数组 | array | string，格式 `YYYY-MM-DD` |
| &emsp;&emsp;orderCounts | 订单数量数组 | array | integer(int64)，与 dates 一一对应 |
| &emsp;&emsp;salesAmounts | 销售额数组 | array | number，与 dates 一一对应 |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "",
  "data": {
    "dates": ["2026-07-27", "2026-07-28", "2026-07-29", "2026-07-30", "2026-07-31", "2026-08-01", "2026-08-02"],
    "orderCounts": [128, 156, 142, 189, 203, 175, 95],
    "salesAmounts": [12800.00, 15600.50, 14200.00, 18900.80, 20300.20, 17500.00, 9500.00]
  },
  "timestamp": "2026-08-02T10:20:00.000Z"
}
```

#### 使用场景
- **页面**：P10 仪表盘（`src/views/admin/Dashboard.vue`）
- **组件**：ECharts 折线图（"近7天订单趋势" 区域，位于 `el-col :span="14"`）
- **权限**：`role=ADMIN, SELLER`

#### 与现有接口的关系
- 现有 `GET /api/v1/admin/dashboard` 仅返回汇总统计（userCount/orderCount/totalSales/seckillCount），不含时序数据
- 本接口为仪表盘图表区域的专用数据源，与汇总统计接口互补
- 前端调用顺序：并行调用 `getDashboard()` 与 `getOrderTrend()` 填充页面

---

### 缺口 6：仪表盘 - 订单状态分布

#### 接口名称
订单状态分布统计

#### 接口地址
`GET /api/v1/admin/dashboard/order-status-distribution`

#### 请求方式
`GET`

#### 请求数据类型
`application/x-www-form-urlencoded`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| startTime | 统计开始时间 | query | 否 | string(date-time) | 默认近 30 天 |
| endTime | 统计结束时间 | query | 否 | string(date-time) | 默认当前时间 |

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | OrderStatusDistributionVO | OrderStatusDistributionVO |
| &emsp;&emsp;items | 状态分布项数组 | array | StatusItem |
| &emsp;&emsp;&emsp;&emsp;status | 订单状态 | string | UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED |
| &emsp;&emsp;&emsp;&emsp;count | 订单数量 | integer(int64) | |
| &emsp;&emsp;&emsp;&emsp;percentage | 占比 | number | 0-100，保留 2 位小数 |
| &emsp;&emsp;total | 订单总数 | integer(int64) | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "",
  "data": {
    "items": [
      { "status": "UNPAID", "count": 56, "percentage": 5.60 },
      { "status": "PAID", "count": 623, "percentage": 62.30 },
      { "status": "CANCELLED", "count": 89, "percentage": 8.90 },
      { "status": "TIMEOUT", "count": 45, "percentage": 4.50 },
      { "status": "COMPLETED", "count": 187, "percentage": 18.70 }
    ],
    "total": 1000
  },
  "timestamp": "2026-08-02T10:25:00.000Z"
}
```

#### 使用场景
- **页面**：P10 仪表盘（`src/views/admin/Dashboard.vue`）
- **组件**：ECharts 饼图（"订单状态分布" 区域，位于 `el-col :span="10"`）
- **权限**：`role=ADMIN, SELLER`

#### 与现有接口的关系
- 与 `GET /api/v1/admin/dashboard` 互补，提供订单维度细分数据
- 前端 ECharts 饼图配色建议：UNPAID=warning(#ea580c)、PAID=success(#059669)、CANCELLED=info(#6b7280)、TIMEOUT=danger(#dc2626)、COMPLETED=primary(#2563eb)

---

### 缺口 7：文件/图片上传

#### 接口名称
通用图片上传

#### 接口地址
`POST /api/v1/upload/image`

#### 请求方式
`POST`

#### 请求数据类型
`multipart/form-data`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| file | 图片文件 | body | 是 | file | multipart 文件流 |
| bizType | 业务类型 | query | 否 | string | `product`/`seckill`/`avatar`/`category` 等，便于后端分类存储 |
| bizId | 业务 ID | query | 否 | integer(int64) | 关联业务实体 ID（编辑场景传入） |

#### 文件约束
- **允许类型**：`image/jpeg`、`image/png`、`image/gif`、`image/webp`
- **最大尺寸**：5MB
- **建议尺寸**：宽高比 1:1，最小 200x200px

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | UploadResultVO | UploadResultVO |
| &emsp;&emsp;url | 图片访问 URL | string | 完整可访问的 URL |
| &emsp;&emsp;originalName | 原始文件名 | string | |
| &emsp;&emsp;size | 文件大小（字节） | integer(int64) | |
| &emsp;&emsp;width | 图片宽度 | integer(int32) | |
| &emsp;&emsp;height | 图片高度 | integer(int32) | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "上传成功",
  "data": {
    "url": "https://cdn.seckillmall.com/upload/product/2026/08/02/abc123.jpg",
    "originalName": "product-image.jpg",
    "size": 245680,
    "width": 800,
    "height": 800
  },
  "timestamp": "2026-08-02T10:30:00.000Z"
}
```

#### 使用场景
- **页面/组件 1**：P11 商品管理（`src/views/admin/ProductManage.vue`）的 `ImageUploader` 组件（`src/components/ImageUploader.vue`），用于商品图片上传，最多 5 张
- **页面/组件 2**：P13 秒杀管理（`src/views/admin/SeckillManage.vue`）的 `ImageUploader` 组件，用于秒杀活动图片上传
- **页面/组件 3**：P09 个人中心（`src/views/front/UserProfile.vue`）头像上传
- **公共组件**：`ImageUploader.vue`（C05）通过 `:http-request` 自定义上传处理器调用本接口
- **权限**：`role=ADMIN, SELLER`（商品/秒杀图片）；所有登录用户（头像上传）

#### 与现有接口的关系
- 现有接口中商品新增/编辑接口 `POST/PUT /api/v1/products` 的 `images` 字段为 URL 数组，依赖本接口先上传图片获取 URL
- 现有秒杀活动新增/编辑接口 `POST/PUT /api/v1/seckill/admin` 的 `images` 字段同理依赖本接口
- 调用流程：前端先调用本接口上传图片 → 获取 URL → 将 URL 数组传入商品/秒杀新增/编辑接口

---

### 缺口 8：后台订单高级筛选列表

#### 接口名称
后台订单管理列表（支持订单号、日期范围、用户 ID 等高级筛选）

#### 接口地址
`GET /api/v1/admin/orders`

#### 请求方式
`GET`

#### 请求数据类型
`application/x-www-form-urlencoded`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| pageNum | 当前页码 | query | 否 | integer(int32) | 默认 1 |
| pageSize | 每页数量 | query | 否 | integer(int32) | 默认 10 |
| orderNo | 订单号 | query | 否 | string | 模糊匹配 |
| status | 订单状态 | query | 否 | string | UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED |
| userId | 用户 ID | query | 否 | integer(int64) | 精确匹配 |
| productId | 商品 ID | query | 否 | integer(int64) | 精确匹配 |
| seckillId | 秒杀活动 ID | query | 否 | integer(int64) | 精确匹配 |
| startTime | 创建开始时间 | query | 否 | string(date-time) | 格式 `YYYY-MM-DD HH:mm:ss` |
| endTime | 创建结束时间 | query | 否 | string(date-time) | 格式 `YYYY-MM-DD HH:mm:ss` |
| payStartTime | 支付开始时间 | query | 否 | string(date-time) | |
| payEndTime | 支付结束时间 | query | 否 | string(date-time) | |
| sortBy | 排序字段 | query | 否 | string | `createTime`/`payTime`/`totalAmount`，默认 `createTime` |
| sortOrder | 排序方向 | query | 否 | string | `asc`/`desc`，默认 `desc` |

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | PageResultAdminOrderVO | PageResultAdminOrderVO |
| &emsp;&emsp;list | 订单列表 | array | AdminOrderVO |
| &emsp;&emsp;&emsp;&emsp;id | 订单 ID | integer(int64) | |
| &emsp;&emsp;&emsp;&emsp;orderNo | 订单号 | string | |
| &emsp;&emsp;&emsp;&emsp;userId | 用户 ID | integer(int64) | |
| &emsp;&emsp;&emsp;&emsp;username | 用户名 | string | 后台展示需关联用户信息 |
| &emsp;&emsp;&emsp;&emsp;seckillId | 秒杀活动 ID | integer(int64) | |
| &emsp;&emsp;&emsp;&emsp;seckillName | 秒杀活动名称 | string | |
| &emsp;&emsp;&emsp;&emsp;productId | 商品 ID | integer(int64) | |
| &emsp;&emsp;&emsp;&emsp;productName | 商品名称 | string | |
| &emsp;&emsp;&emsp;&emsp;seckillPrice | 秒杀价格 | number | |
| &emsp;&emsp;&emsp;&emsp;quantity | 购买数量 | integer(int32) | |
| &emsp;&emsp;&emsp;&emsp;totalAmount | 总金额 | number | |
| &emsp;&emsp;&emsp;&emsp;status | 订单状态 | string | UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED |
| &emsp;&emsp;&emsp;&emsp;payTime | 支付时间 | string(date-time) | |
| &emsp;&emsp;&emsp;&emsp;payExpireTime | 支付过期时间 | string(date-time) | |
| &emsp;&emsp;&emsp;&emsp;transactionId | 交易流水号 | string | |
| &emsp;&emsp;&emsp;&emsp;payMethod | 支付方式 | string | |
| &emsp;&emsp;&emsp;&emsp;cancelTime | 取消时间 | string(date-time) | |
| &emsp;&emsp;&emsp;&emsp;cancelReason | 取消原因 | string | |
| &emsp;&emsp;&emsp;&emsp;createTime | 创建时间 | string(date-time) | |
| &emsp;&emsp;&emsp;&emsp;updateTime | 更新时间 | string(date-time) | |
| &emsp;&emsp;total | 总记录数 | integer(int64) | |
| &emsp;&emsp;pageNum | 当前页码 | integer(int64) | |
| &emsp;&emsp;pageSize | 每页数量 | integer(int64) | |
| &emsp;&emsp;pages | 总页数 | integer(int64) | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "id": 10086,
        "orderNo": "SK202608021000001",
        "userId": 1001,
        "username": "zhangsan",
        "seckillId": 2001,
        "seckillName": "iPhone 16 限时秒杀",
        "productId": 3001,
        "productName": "iPhone 16 128GB",
        "seckillPrice": 4999.00,
        "quantity": 1,
        "totalAmount": 4999.00,
        "status": "PAID",
        "payTime": "2026-08-02T09:30:00.000Z",
        "payExpireTime": "2026-08-02T09:45:00.000Z",
        "transactionId": "TXN20260802093000001",
        "payMethod": "ALIPAY",
        "cancelTime": "",
        "cancelReason": "",
        "createTime": "2026-08-02T09:28:00.000Z",
        "updateTime": "2026-08-02T09:30:00.000Z"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  },
  "timestamp": "2026-08-02T10:35:00.000Z"
}
```

#### 使用场景
- **页面**：P14 后台订单管理（`src/views/admin/OrderManage.vue`）
- **组件**：筛选栏（订单号搜索、状态筛选、日期范围选择器）+ 表格 + 分页
- **权限**：`role=ADMIN`

#### 与现有接口的关系
- 现有 `GET /api/v1/orders` 仅支持 `status/pageNum/pageSize` 三个参数，且语义为"我的订单"（基于当前登录用户 ID 过滤），无法满足后台管理全量订单筛选需求
- 本接口为**后台管理专用**，需管理员权限，返回字段需额外关联用户名、商品名、秒杀活动名等展示信息
- **建议独立路径** `/api/v1/admin/orders` 而非扩展 `/api/v1/orders`，原因：
  1. 权限隔离：`/api/v1/orders` 为普通用户接口，`/api/v1/admin/orders` 为管理员接口
  2. 数据范围不同：前者仅当前用户订单，后者全量订单
  3. 返回字段不同：后者需关联用户名/商品名等冗余字段便于展示

---

### 缺口 9：用户个人信息更新

#### 接口名称
更新当前用户个人信息（昵称、邮箱、手机号、头像）

#### 接口地址
`PUT /api/v1/auth/profile`

#### 请求方式
`PUT`

#### 请求数据类型
`application/json`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| nickname | 昵称 | body | 否 | string | 1-32 个字符 |
| email | 邮箱 | body | 否 | string | 合法邮箱格式 |
| phone | 手机号 | body | 否 | string | 11 位手机号，需验证唯一性 |
| avatar | 头像 URL | body | 否 | string | 由图片上传接口返回的 URL |

#### 请求示例
```json
{
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "avatar": "https://cdn.seckillmall.com/upload/avatar/2026/08/02/abc123.jpg"
}
```

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | UserVO | UserVO |
| &emsp;&emsp;id | 用户 ID | integer(int64) | |
| &emsp;&emsp;username | 用户名 | string | |
| &emsp;&emsp;phone | 手机号 | string | |
| &emsp;&emsp;email | 邮箱 | string | |
| &emsp;&emsp;nickname | 昵称 | string | |
| &emsp;&emsp;avatar | 头像 URL | string | |
| &emsp;&emsp;role | 角色 | string | |
| &emsp;&emsp;status | 状态 | string | |
| &emsp;&emsp;createTime | 创建时间 | string(date-time) | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "个人信息更新成功",
  "data": {
    "id": 1001,
    "username": "zhangsan",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "nickname": "张三",
    "avatar": "https://cdn.seckillmall.com/upload/avatar/2026/08/02/abc123.jpg",
    "role": "BUYER",
    "status": "ACTIVE",
    "createTime": "2026-07-01T08:00:00.000Z"
  },
  "timestamp": "2026-08-02T10:40:00.000Z"
}
```

#### 使用场景
- **页面**：P09 个人中心（`src/views/front/UserProfile.vue`）
- **组件**：基本信息 Tab 中的编辑表单 + 头像上传
- **权限**：所有登录用户

#### 与现有接口的关系
- 现有 `GET /api/v1/auth/me` 仅查询用户信息，无更新接口
- 现有 `PUT /api/v1/auth/password` 仅修改密码，不涉及基本信息字段
- 本接口填补"个人信息更新"缺口，与现有接口共同构成完整的个人中心数据管理
- 调用流程：用户修改头像 → 先调用图片上传接口获取 URL → 调用本接口更新 `avatar` 字段

---

### 缺口 10：用户头像上传（专用接口，可选）

> **说明**：若后端实现通用的"缺口 7：图片上传接口"并支持 `bizType=avatar` 参数，则本接口可不必单独实现。下方提供专用接口定义供备选方案参考。

#### 接口名称
用户头像上传

#### 接口地址
`POST /api/v1/auth/avatar`

#### 请求方式
`POST`

#### 请求数据类型
`multipart/form-data`

#### 请求参数

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | 备注 |
|---|---|---|---|---|---|
| file | 头像图片文件 | body | 是 | file | multipart 文件流 |

#### 文件约束
- **允许类型**：`image/jpeg`、`image/png`、`image/webp`
- **最大尺寸**：2MB
- **建议尺寸**：正方形，最小 200x200px，最大 1024x1024px

#### 响应数据结构

| 参数名称 | 参数说明 | 类型 | schema |
|---|---|---|---|
| code | 业务状态码 | integer(int32) | |
| message | 提示信息 | string | |
| data | 返回数据 | object | |
| &emsp;&emsp;avatar | 头像 URL | string | |
| timestamp | 时间戳 | string | |

#### 响应示例
```json
{
  "code": 0,
  "message": "头像上传成功",
  "data": {
    "avatar": "https://cdn.seckillmall.com/upload/avatar/2026/08/02/abc123.jpg"
  },
  "timestamp": "2026-08-02T10:45:00.000Z"
}
```

#### 使用场景
- **页面**：P09 个人中心（`src/views/front/UserProfile.vue`）
- **组件**：左侧用户信息卡片中的 `el-avatar` 点击触发上传
- **权限**：所有登录用户

#### 与现有接口的关系
- 与"缺口 7：图片上传接口"功能重叠，二选一即可
- 上传成功后需配合"缺口 9：用户个人信息更新"接口持久化到用户档案

---

## 三、接口缺口汇总表

| 序号 | 缺口接口 | 方法 | 路径 | 优先级 | 使用页面 | 备注 |
|---|---|---|---|---|---|---|
| 1 | 新增分类 | POST | `/api/v1/categories` | 高 | P12 | 分类管理 CRUD 核心 |
| 2 | 编辑分类 | PUT | `/api/v1/categories/{id}` | 高 | P12 | 分类管理 CRUD 核心 |
| 3 | 删除分类 | DELETE | `/api/v1/categories/{id}` | 高 | P12 | 分类管理 CRUD 核心 |
| 4 | 切换分类状态 | PUT | `/api/v1/categories/{id}/status` | 中 | P12 | 可合并到编辑接口 |
| 5 | 近7天订单趋势 | GET | `/api/v1/admin/dashboard/order-trend` | 高 | P10 | 仪表盘图表 |
| 6 | 订单状态分布 | GET | `/api/v1/admin/dashboard/order-status-distribution` | 高 | P10 | 仪表盘图表 |
| 7 | 图片上传 | POST | `/api/v1/upload/image` | 高 | P11/P13/P09 | 多页面共用 |
| 8 | 后台订单高级筛选 | GET | `/api/v1/admin/orders` | 高 | P14 | 后台管理专用 |
| 9 | 个人信息更新 | PUT | `/api/v1/auth/profile` | 中 | P09 | 个人中心 |
| 10 | 用户头像上传 | POST | `/api/v1/auth/avatar` | 低 | P09 | 可由缺口 7 替代 |

---

## 四、实现优先级建议

### 4.1 P0 优先级（阻塞前端核心功能，必须优先实现）

1. **图片上传接口**（缺口 7）：阻塞 P11 商品管理、P13 秒杀管理的图片上传功能，无替代方案
2. **后台订单高级筛选接口**（缺口 8）：阻塞 P14 后台订单管理的核心筛选功能
3. **分类管理 CRUD 接口**（缺口 1、2、3）：阻塞 P12 分类管理页面的核心功能

### 4.2 P1 优先级（影响仪表盘完整展示）

4. **近7天订单趋势接口**（缺口 5）：P10 仪表盘折线图数据源
5. **订单状态分布接口**（缺口 6）：P10 仪表盘饼图数据源

> **临时方案**：若短期内无法实现，前端可使用 mock 数据或隐藏图表区域，但影响产品完整度。

### 4.3 P2 优先级（增强用户体验）

6. **个人信息更新接口**（缺口 9）：P09 个人中心编辑功能
7. **切换分类状态接口**（缺口 4）：可由"编辑分类"接口替代
8. **用户头像上传接口**（缺口 10）：可由通用"图片上传"接口替代

---

## 五、统一约定

### 5.1 响应格式约定

所有接口遵循现有后端统一响应格式：

```json
{
  "code": 0,
  "message": "string",
  "data": {},
  "timestamp": "string"
}
```

- `code=0` 表示成功，非 0 表示业务错误
- `message` 为用户可见的提示信息
- `timestamp` 为服务器时间，前端用于时间同步校准（详见设计规范 `utils/time-sync.ts`）

### 5.2 鉴权约定

- 除 `POST /api/v1/upload/image`（头像场景）外，所有缺口接口均需在请求头携带 `Authorization: Bearer ${accessToken}`
- 后台管理接口（`/api/v1/admin/**`）需校验 `role=ADMIN`（部分接口允许 `SELLER`）
- 前端 Axios 请求拦截器已统一处理鉴权头注入

### 5.3 分页约定

分页接口遵循现有约定：
- 请求参数：`pageNum`（从 1 开始）、`pageSize`
- 响应结构：`{ list, total, pageNum, pageSize, pages }`

### 5.4 时间格式约定

- 所有时间字段使用 ISO 8601 格式：`YYYY-MM-DDTHH:mm:ss.SSSZ`
- 前端使用 `dayjs` 格式化展示：`YYYY-MM-DD HH:mm:ss`

### 5.5 错误码建议

| 错误码 | 含义 | 适用接口 |
|---|---|---|
| 0 | 成功 | 所有 |
| 40001 | 参数校验失败 | 所有 |
| 40002 | 分类下存在子分类，无法删除 | 缺口 3 |
| 40003 | 分类下存在商品，无法删除 | 缺口 3 |
| 40004 | 不允许将分类移动到自身子分类下 | 缺口 2 |
| 40101 | 未登录 | 所有需鉴权接口 |
| 40301 | 无权限访问 | 后台管理接口 |
| 41501 | 文件类型不支持 | 缺口 7、10 |
| 41502 | 文件大小超限 | 缺口 7、10 |
| 50000 | 服务器内部错误 | 所有 |

---

## 六、附录：前端 API 函数声明建议

供前端 `src/api/` 目录新增函数声明参考（TypeScript）：

```ts
// src/api/category.ts（新增）
import request from './request'
import type { Result, CategoryVO } from '@/types'

export interface CategoryCreateRequest {
  categoryName: string
  parentId: number
  sortOrder?: number
  status?: number
}

export interface CategoryUpdateRequest {
  categoryName?: string
  parentId?: number
  sortOrder?: number
  status?: number
}

export const categoryApi = {
  getCategories: () => request.get<Result<CategoryVO[]>>('/api/v1/categories'),
  createCategory: (data: CategoryCreateRequest) =>
    request.post<Result<CategoryVO>>('/api/v1/categories', data),
  updateCategory: (id: number, data: CategoryUpdateRequest) =>
    request.put<Result<CategoryVO>>(`/api/v1/categories/${id}`, data),
  deleteCategory: (id: number) =>
    request.delete<Result<void>>(`/api/v1/categories/${id}`),
  updateCategoryStatus: (id: number, status: number) =>
    request.put<Result<void>>(`/api/v1/categories/${id}/status`, { status }),
}

// src/api/upload.ts（新增）
export interface UploadResultVO {
  url: string
  originalName: string
  size: number
  width: number
  height: number
}

export const uploadApi = {
  uploadImage: (file: File, bizType?: string, bizId?: number) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<Result<UploadResultVO>>('/api/v1/upload/image', formData, {
      params: { bizType, bizId },
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// src/api/admin.ts（扩展）
export interface OrderTrendVO {
  dates: string[]
  orderCounts: number[]
  salesAmounts: number[]
}

export interface OrderStatusDistributionVO {
  items: Array<{ status: string; count: number; percentage: number }>
  total: number
}

export interface AdminOrderQueryRequest {
  pageNum?: number
  pageSize?: number
  orderNo?: string
  status?: string
  userId?: number
  productId?: number
  seckillId?: number
  startTime?: string
  endTime?: string
  payStartTime?: string
  payEndTime?: string
  sortBy?: string
  sortOrder?: string
}

export interface AdminOrderVO {
  id: number
  orderNo: string
  userId: number
  username: string
  seckillId: number
  seckillName: string
  productId: number
  productName: string
  seckillPrice: number
  quantity: number
  totalAmount: number
  status: string
  payTime: string
  payExpireTime: string
  transactionId: string
  payMethod: string
  cancelTime: string
  cancelReason: string
  createTime: string
  updateTime: string
}

export const adminApi = {
  // ...现有函数...

  getOrderTrend: (days?: number) =>
    request.get<Result<OrderTrendVO>>('/api/v1/admin/dashboard/order-trend', { params: { days } }),

  getOrderStatusDistribution: (startTime?: string, endTime?: string) =>
    request.get<Result<OrderStatusDistributionVO>>(
      '/api/v1/admin/dashboard/order-status-distribution',
      { params: { startTime, endTime } },
    ),

  getAdminOrderList: (params: AdminOrderQueryRequest) =>
    request.get<Result<PageResult<AdminOrderVO>>>('/api/v1/admin/orders', { params }),
}

// src/api/auth.ts（扩展）
export interface ProfileUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

export const authApi = {
  // ...现有函数...

  updateProfile: (data: ProfileUpdateRequest) =>
    request.put<Result<UserVO>>('/api/v1/auth/profile', data),

  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<Result<{ avatar: string }>>('/api/v1/auth/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
```

---

**文档结束**

> 本文档基于 `default.md`（后端已实现接口）与 `10-ai-design-spec.md`（前端设计规范）对比生成。后端开发完成上述缺口接口后，请同步更新 `default.md` 接口文档，并通知前端开发联调。