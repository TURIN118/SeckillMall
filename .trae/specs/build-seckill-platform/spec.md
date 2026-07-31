# 高并发秒杀电商平台 Spring Boot 后端 Spec

## Why
基于 8 份详细设计文档（产品/ API/ 数据库/ 安全/ 测试部署/ 中间件/ 工程规范/ 设计系统）构建一个面向电商秒杀场景的高并发在线交易平台后端。核心挑战：高并发下保证库存零超卖、系统稳定可用、防范黄牛刷单。本项目为毕业设计，需深度运用 Redis 缓存策略、RabbitMQ 异步削峰、分布式锁、Lua 原子操作、接口限流等高并发核心技术。

## Scope 说明（重要）
- **本 Spec 聚焦 Spring Boot 后端**。前端（设计文档指定 Vue 3 + Element Plus）不在本次实施范围内，作为独立交付物。
- 环境变量命名以用户规则为准：数据库使用 `DB_HOST`/`DB_PORT`/`MYSQL_USER`/`MYSQL_PASSWORD`/`MYSQL_DATABASE`（非设计文档中的 `MYSQL_HOST`/`MYSQL_PORT`）；Redis/SMTP 沿用设计文档命名。
- 所有 Java 类文件须在导入语句后添加用户规定的 `@author WNJ` 文件头注释。

## What Changes
- 从零搭建 Spring Boot 3.2 + JDK 17 + Maven 项目骨架（包名 `com.seckill.mall`）
- 建立 MySQL 8.0 数据库：8 张核心表（t_user / t_category / t_product / t_seckill_goods / t_seckill_order / t_user_address / t_login_log / t_operation_log），雪花算法主键、逻辑删除、自动填充时间
- 实现 Spring Security 6 + JWT 双 Token 无状态鉴权（Access 2h / Refresh 7d，HS256，Redis 黑名单，BCrypt strength=10）
- 实现 RBAC 三级角色（BUYER/SELLER/ADMIN）权限控制
- 实现 7 大业务模块共 ~35 个 RESTful API（用户认证/商品管理/秒杀活动/秒杀下单/订单管理/后台用户管理/系统管理）
- 实现秒杀核心链路：Redis Lua 原子库存预减 + 资格校验 → RabbitMQ 异步下单削峰 → 前端轮询结果
- 实现 Redis 多场景应用：商品详情缓存、库存预减、已购用户 Set、分布式锁、令牌桶限流、Token 黑名单、验证码、消息幂等去重、布隆过滤器
- 实现 RabbitMQ 四组拓扑：秒杀下单（direct）、延迟取消（DLX/TTL 15min）、死信处理、结果广播（fanout）
- 实现订单状态机：CREATED→UNPAID→PAID→COMPLETED / UNPAID→CANCELLED / UNPAID→TIMEOUT，含库存回补
- 实现五层秒杀安全防御：前端限流 + Redis+Lua 接口限流 + IP 限流 + 验证码 + 防重放（HMAC-SHA256 + Nonce）
- 实现容错降级：Redis 宕机降级数据库乐观锁、MQ 宕机降级同步、Sentinel 熔断
- 实现邮件通知服务（QQ SMTP + Thymeleaf + @Async + Spring Retry）
- 实现全局异常处理、统一响应封装、参数校验、操作日志审计
- 提供 Knife4j API 文档、Docker Compose 编排、JUnit 5 单元测试与集成测试

## Impact
- 新建项目：`d:\DESk\SpringBoot\seckill-mall\`（Maven 单模块）
- 依赖设计文档：02-api-design / 03-database-design / 04-security-design / 05-test-deploy-plan / 06-middleware-design / 07-engineering-standards / product-design-docs
- 设计系统文档（design-system-spec）属前端范畴，后端不直接实现
- 外部中间件：MySQL 8.0、Redis 7.x、RabbitMQ 3.13（通过 Docker Compose 启动）

## 技术栈与版本约束
| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | JDK | 17 |
| 框架 | Spring Boot | 3.2+ |
| 安全 | Spring Security | 6.x |
| ORM | MyBatis-Plus | 3.5+ |
| 数据库 | MySQL | 8.0 (InnoDB, utf8mb4_general_ci) |
| 缓存 | Redis + Redisson | 7.x / 3.27+ |
| 消息队列 | RabbitMQ + Spring AMQP | 3.13 / 3.1+ |
| 文档 | Knife4j | 4.x |
| 工具 | Lombok / MapStruct | 1.18+ / 1.5+ |
| 测试 | JUnit 5 + Mockito + Testcontainers | 5.10 / 5.x |
| 构建 | Maven | - |

## ADDED Requirements

### Requirement: 项目骨架与工程规范
系统 SHALL 按 `com.seckill.mall` 包名组织代码，采用分层架构（config/controller/service/mapper/entity/dto/vo/common/security/mq/cache），启动类为 `SeckillMallApplication`。
- 所有 Java 类文件在导入语句后添加文件头注释：`创建人：@author WNJ / 项目名称：seckill-mall / 文件名称：xxx.java / 邮箱：nj651217@163.com`
- 命名规范：Controller/Service/ServiceImpl/Mapper 后缀；Entity 无后缀；请求用 Request/DTO，响应用 VO；方法前缀 get/list/create/update/delete
- 分层职责：Controller 仅参数校验+调用 Service+返回 Result；Service 编排业务+事务；Mapper 仅数据访问
- Git 提交规范：feat/fix/docs/refactor/test/chore 前缀；本地用户名 wnj / 邮箱 nj651217@163.com

### Requirement: 统一响应与异常处理
系统 SHALL 提供统一响应封装 `Result<T>`（字段：code/message/data/timestamp）与分页响应 `PageResult<T>`（字段：list/total/pageNum/pageSize/pages）。
- 全局异常处理器 `GlobalExceptionHandler` 捕获 BusinessException、参数校验异常、安全异常并转为标准 Result
- 业务异常统一抛 `BusinessException(ErrorCode)`，ErrorCode 枚举覆盖通用(1xxx)/用户(1xxx)/秒杀(2xxx)/订单(3xxx) 错误码
- 统一错误码：1001 参数错误、1003 密码错误、1004 账号禁用、1005 验证码错误、2002 活动未开始、2003 库存不足、2004 重复下单、3002 订单已支付、3004 订单超时 等

### Requirement: 数据库与持久层
系统 SHALL 创建 8 张核心表并使用 MyBatis-Plus 访问：
- t_user（用户表，BCrypt 密码，role ENUM(BUYER/SELLER/ADMIN)，status ENUM(ACTIVE/DISABLED)，uk_username/uk_phone）
- t_category（商品分类，自关联 parent_id）
- t_product（商品表，original_price DECIMAL(10,2)，images JSON，status ENUM(ON_SALE/OFF_SHELF)，idx_category_status）
- t_seckill_goods（秒杀活动表，seckill_price/stock_count/available_count，start_time/end_time，status ENUM(PENDING/ACTIVE/ENDED/CANCELLED)，idx_status_start/idx_start_time）
- t_seckill_order（秒杀订单表，**核心**，uk_user_seckill(user_id,seckill_id) 一人一单约束，status ENUM(UNPAID/PAID/CANCELLED/TIMEOUT)，idx_user_status/idx_seckill_status）
- t_user_address（收货地址，idx_user_default）
- t_login_log（登录日志，append-only，idx_user_time）
- t_operation_log（操作日志，idx_operator_time/idx_module）
- 全表：id BIGINT 雪花算法、create_time/update_time 自动填充、is_deleted 逻辑删除（@TableLogic）
- 初始化数据：admin/admin123（ADMIN）、buyer01（BUYER）、4 个一级分类+16 个二级分类、5 个示例商品、2 个示例秒杀活动

### Requirement: 安全认证与授权
系统 SHALL 实现 JWT 双 Token 无状态鉴权：
- AccessToken 有效期 2h（7200000ms），RefreshToken 有效期 7d（604800000ms），HS256 签名
- JWT Claims：userId、username、role、iat、exp、tokenType
- 登出时将 Token 加入 Redis 黑名单 `token:blacklist:{tokenId}`（TTL=Token 剩余有效期）
- 密码 BCrypt 加密（strength=10）
- 登录安全：连续失败 5 次锁定 30 分钟（`login:fail:{username}` 计数）；失败 3 次触发图形验证码
- 登录日志写入 t_login_log（SUCCESS/FAILED + fail_reason）
- RBAC：BUYER（买家）、SELLER（卖家）、ADMIN（管理员），接口级权限注解控制
- CSRF 禁用（REST API）、CORS 配置允许指定域名
- 验证码：`captcha:{captchaId}` 存 Redis，TTL 5min，一次性使用

### Requirement: 秒杀核心链路
系统 SHALL 实现高性能秒杀下单流程（P99 < 200ms，QPS ≥ 5000，零超卖）：
- 执行秒杀 `POST /api/v1/seckill/{seckillId}` 同步链路：
  1. 验证 seckillToken（Redis `seckill:token:{seckillId}:{userId}`）
  2. 检查活动状态与时间窗口（Redis Hash `seckill:info:{seckillId}`）
  3. 执行 Lua 脚本原子操作：SISMEMBER 已购校验（`seckill:bought:{seckillId}`）+ DECR 库存预减（`seckill:stock:{seckillId}`）
  4. 库存<0 则 INCR 回滚并返回 2003；已购返回 2004
  5. 投递 SeckillOrderMessage 到 RabbitMQ `seckill.order.exchange`
  6. 立即返回 requestId，status=0 排队中
- 异步消费链路：消费者幂等去重（Redis SETNX `mq:consumed:{messageId}`）→ 创建订单（INSERT t_seckill_order，依赖 uk_user_seckill 兜底）→ 写秒杀结果 `seckill:result:{seckillId}:{userId}`（TTL 10min）→ 发送延迟取消消息（TTL 15min）
- 查询结果 `GET /api/v1/seckill/{seckillId}/result?requestId=`：返回 status(0排队/1成功/-1失败)、orderId、orderNo、totalAmount、payExpireTime

### Requirement: 订单状态机与超时取消
系统 SHALL 实现订单完整生命周期：
- 状态：CREATED→UNPAID→PAID→COMPLETED（确认收货）；UNPAID→CANCELLED（用户取消）；UNPAID→TIMEOUT（延迟消息触发）
- 支付 `POST /api/v1/orders/{orderId}/pay`：模拟支付，校验状态/权限/超时，更新 PAID+pay_time，取消延迟取消任务，幂等（orderId）
- 订单超时：RabbitMQ 延迟队列（DLX+TTL 15min）触发，校验 isPaid==false 后置 TIMEOUT，回补 Redis 库存（INCR）+ 删除购买标记（SREM）
- 用户取消：回补 Redis 库存 + 删除购买标记 + 取消通知

### Requirement: Redis 中间件设计
系统 SHALL 实现 Redis 多场景应用，Key 命名采用冒号分隔三段式：
- `seckill:goods:{goodsId}` String(JSON) 30min 商品详情缓存（空值缓存防穿透 + 互斥锁防击穿 + TTL 随机偏移防雪崩）
- `seckill:stock:{seckillId}` String(int) 活动结束 库存预减
- `seckill:bought:{seckillId}` Set 活动结束 已购用户集合
- `seckill:mark:{seckillId}:{userId}` String(1) 活动结束 秒杀资格标记
- `seckill:result:{seckillId}:{userId}` String(JSON) 10min 秒杀结果
- `rate:seckill:{userId}` String(int) 1s 用户限流（Lua 令牌桶，每用户每秒 1 次）
- `rate:ip:{ip}:{path}` String(int) 60s IP 限流
- `login:fail:{username}` String(int) 30min 登录失败计数
- `token:blacklist:{tokenId}` String(1) Token 剩余有效期
- `captcha:{captchaId}` String(JSON) 5min 验证码
- `mq:consumed:{messageId}` String(1) 24h 消息幂等
- `seckill:bloom:goods` BloomFilter 商品 ID 布隆过滤器（防穿透）
- 严禁使用 KEYS *，统一用 SCAN

### Requirement: RabbitMQ 消息队列
系统 SHALL 实现四组 RabbitMQ 拓扑：
- `seckill.order.exchange`(direct) → `seckill.order.queue`：秒杀异步下单削峰
- `order.delay.exchange`+TTL → `order.delay.queue` → DLX `order.dead.exchange` → `order.cancel.queue`：延迟取消超时订单（15min）
- `seckill.result.exchange`(fanout) → `seckill.result.queue`：秒杀结果广播
- 消费者手动 ACK（acknowledge-mode: manual，prefetch: 1），幂等去重（Redis SETNX + DB 唯一约束双重保障）
- 消息体：SeckillOrderMessage(seckillId/userId/requestId/timestamp)、OrderDelayMessage(orderId/expireTime)、SeckillResultMessage(userId/seckillId/status/orderId)

### Requirement: 安全防御与限流
系统 SHALL 实现五层秒杀防御：
- 接口限流：Redis + Lua 令牌桶，用户级每秒 1 次（`rate:seckill:{userId}`）
- IP 限流：滑动窗口 60s（`rate:ip:{ip}:{path}`，每 IP 每 60s 30 次）
- 防重放：请求签名 HMAC-SHA256 + 时间窗口 60s + Nonce 去重（Redis Set）
- 幂等性：秒杀接口 requestId、支付接口 orderId、通用写接口 token
- 输入校验：Spring Validation 全局参数校验；MyBatis 参数化查询防 SQL 注入；Jsoup 白名单清洗防 XSS

### Requirement: 容错降级
系统 SHALL 实现中间件宕机降级：
- Redis 宕机：健康检查检测，降级到数据库乐观锁（UPDATE SET stock=stock-1 WHERE stock>0），降级恢复 < 30s
- RabbitMQ 宕机：连接检测，降级同步下单 + 补偿任务
- Sentinel 熔断降级：异常比例触发，降级返回排队提示
- 策略模式实现缓存模式/数据库模式切换

### Requirement: 邮件通知服务
系统 SHALL 实现异步邮件通知（QQ SMTP smtp.qq.com:465）：
- 场景：注册验证码、秒杀成功、支付确认、订单取消、密码重置
- 实现：spring-boot-starter-mail + JavaMailSender + Thymeleaf 模板 + @Async 线程池（核心2/最大5）+ Spring Retry（3 次指数退避 1s/2s/4s）
- 环境变量：SMTP_HOST/SMTP_PORT/SMTP_USERNAME/SMTP_PASSWORD

### Requirement: 配置管理
系统 SHALL 通过 Spring Profiles 管理多环境配置：
- 环境变量：`DB_HOST`/`DB_PORT`/`MYSQL_USER`/`MYSQL_PASSWORD`/`MYSQL_DATABASE`、`REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`、`RABBITMQ_HOST`/`RABBITMQ_PORT`/`RABBITMQ_USER`/`RABBITMQ_PASSWORD`、`JWT_SECRET`、`SMTP_*`
- application.yml（公共）+ application-dev.yml + application-prod.yml
- MyBatis-Plus：id-type=ASSIGN_ID、logic-delete-field=isDeleted、mapper-locations=classpath:mapper/*.xml
- HikariCP 连接池（dev 20 / prod 50）、Lettuce 连接池（prod max-active 32）

### Requirement: 测试与部署
系统 SHALL 提供测试与容器化部署：
- 单元测试：JUnit 5 + Mockito，Service 层覆盖率 > 80%、Mapper > 60%、Controller > 50%
- 集成测试：Spring Boot Test + Testcontainers（MySQL 8.0/Redis 7/RabbitMQ 3.13）
- 性能目标：秒杀 QPS ≥ 5000、P99 < 200ms、零超卖、错误率 < 1%
- Docker Compose 编排 MySQL+Redis+RabbitMQ+应用
- Knife4j API 文档 100% 覆盖
- Spring Boot Actuator 健康检查

## API 接口清单（7 模块 ~35 接口）

### 模块1 用户认证（6 接口）
- POST `/api/v1/auth/register` 注册（无需鉴权，username/password/phone/captchaKey/captchaCode）
- POST `/api/v1/auth/login` 登录（无需鉴权，返回 accessToken/refreshToken/user）
- POST `/api/v1/auth/logout` 登出（Bearer，加入黑名单）
- GET `/api/v1/auth/me` 当前用户信息（Bearer）
- PUT `/api/v1/auth/password` 修改密码（Bearer，oldPassword/newPassword/confirmPassword）
- POST `/api/v1/auth/refresh` 刷新 Token（无需鉴权，refreshToken）

### 模块2 商品管理（6 接口）
- GET `/api/v1/products` 商品列表分页（无需鉴权，pageNum/pageSize/categoryId/keyword/sortBy/sortOrder）
- GET `/api/v1/products/{id}` 商品详情（无需鉴权）
- POST `/api/v1/products` 新增商品（ADMIN）
- PUT `/api/v1/products/{id}` 编辑商品（ADMIN）
- DELETE `/api/v1/products/{id}` 删除商品（ADMIN，逻辑删除）
- GET `/api/v1/categories` 分类树（无需鉴权）

### 模块3 秒杀活动（6 接口）
- GET `/api/v1/seckill/list` 秒杀活动列表（无需鉴权，含状态/时间筛选）
- GET `/api/v1/seckill/{seckillId}` 秒杀活动详情（无需鉴权）
- GET `/api/v1/seckill/{seckillId}/token` 获取秒杀令牌（Bearer，seckillToken）
- POST `/api/v1/seckill/admin` 创建秒杀活动（ADMIN/SELLER）
- PUT `/api/v1/seckill/admin/{seckillId}` 编辑秒杀活动（ADMIN/SELLER）
- PUT `/api/v1/seckill/admin/{seckillId}/cancel` 取消秒杀活动（ADMIN/SELLER）

### 模块4 秒杀下单（4 接口）
- POST `/api/v1/seckill/{seckillId}` 执行秒杀（Bearer，seckillToken，返回 requestId/status）
- GET `/api/v1/seckill/{seckillId}/result` 查询秒杀结果（Bearer，requestId，返回 status/orderId/orderNo/totalAmount/payExpireTime）
- GET `/api/v1/seckill/{seckillId}/stock` 查询实时库存（无需鉴权）

### 模块5 订单管理（6 接口）
- GET `/api/v1/orders` 我的订单列表（Bearer，分页+状态筛选）
- GET `/api/v1/orders/{orderId}` 订单详情（Bearer）
- POST `/api/v1/orders/{orderId}/pay` 确认支付（Bearer，payMethod，模拟支付）
- POST `/api/v1/orders/{orderId}/cancel` 取消订单（Bearer，仅 UNPAID 可取消）
- GET `/api/v1/orders/{orderId}/status` 查询订单状态（Bearer）

### 模块6 后台用户管理（4 接口，ADMIN）
- GET `/api/v1/admin/users` 用户列表（分页+角色/状态筛选）
- PUT `/api/v1/admin/users/{userId}/status` 启用/禁用用户
- PUT `/api/v1/admin/users/{userId}/role` 修改用户角色
- GET `/api/v1/admin/users/{userId}/logs` 用户登录日志

### 模块7 系统管理（3 接口，ADMIN）
- GET `/api/v1/admin/dashboard` 仪表盘统计（用户数/订单数/销售额/活动数）
- GET `/api/v1/admin/operation-logs` 操作日志列表（分页+模块筛选）
- GET `/api/v1/admin/system/health` 系统健康检查（Redis/MQ/DB 状态）

## 非功能性指标
| 类别 | 指标 | 目标 |
|------|------|------|
| 性能 | 秒杀接口 QPS | ≥ 5000 |
| 性能 | 秒杀 P99 响应 | < 200ms |
| 性能 | 详情页 QPS | ≥ 10000 |
| 性能 | 详情页 P99 | < 50ms |
| 可用性 | 系统可用性 | > 99.5% |
| 可用性 | 降级恢复 | < 30s |
| 一致性 | 库存超卖 | 零超卖 |
| 安全 | 防重复下单 | 100% 拦截 |
| 可维护 | Service 覆盖率 | > 80% |
| 可维护 | API 文档覆盖 | 100% |
