# Tasks — 高并发秒杀电商平台 Spring Boot 后端

任务按里程碑（M1~M10）组织，每个任务标注优先级（P0 关键 / P1 高 / P2 中 / P3 低）与依赖关系。验证方式标注于任务末尾。

## M1 项目骨架与基础设施（P0）
- [x] Task 1.1: 初始化 Maven 项目骨架
  - 创建 `seckill-mall/pom.xml`：Spring Boot 3.2+ parent，依赖 web/security/data-redis/data-jdbc/mybatis-plus/redisson/amqp/mail/thymeleaf/knife4j/lombok/mapstruct/validation/actuator；JDK 17
  - 创建启动类 `SeckillMallApplication.java`（含文件头注释 @author WNJ）
  - 创建包结构：config/controller/service(/impl)/mapper/entity/dto/vo/common/security/mq/cache/aspect
  - git init 初始化仓库，配置本地用户 wnj / nj651217@163.com
  - 验证：`mvn compile` 通过
- [x] Task 1.2: 编写 application.yml 多环境配置
  - application.yml（公共）：server.port=8080、jackson 时区 Asia/Shanghai、mybatis-plus（id-type=ASSIGN_ID、logic-delete-field=isDeleted、mapper-locations）
  - application-dev.yml：datasource（DB_HOST/DB_PORT/MYSQL_USER/MYSQL_PASSWORD/MYSQL_DATABASE）、redis（REDIS_HOST/REDIS_PORT/REDIS_PASSWORD）、rabbitmq（RABBITMQ_*）、mail（SMTP_HOST/SMTP_PORT/SMTP_USERNAME/SMTP_PASSWORD）、jwt（JWT_SECRET/access 2h/refresh 7d）、seckill（pay-timeout 15min/rate-limit）
  - application-prod.yml：连接池调优（HikariCP 50/Lettuce 32）、日志 WARN
  - 验证：应用可按 dev profile 启动
- [x] Task 1.3: 实现统一响应与异常处理
  - `common/Result.java`（code/message/data/timestamp）、`common/PageResult.java`（list/total/pageNum/pageSize/pages）
  - `common/ErrorCode.java` 枚举（通用 1xxx/用户 1xxx/秒杀 2xxx/订单 3xxx 全部错误码）
  - `common/BusinessException.java`
  - `common/GlobalExceptionHandler.java`（捕获 BusinessException/MethodArgumentNotValid/AccessDenied/其他）
  - 验证：单元测试覆盖异常转 Result

## M2 数据库与持久层（P0）
- [x] Task 2.1: 创建数据库 DDL 与初始化脚本
  - `src/main/resources/sql/schema.sql`：8 张表 CREATE TABLE（含全部索引：uk_username/uk_phone/uk_user_seckill/idx_category_status/idx_status_start 等）
  - `src/main/resources/sql/data.sql`：admin/admin123(BCrypt)、buyer01、4 一级+16 二级分类、5 商品、2 秒杀活动
  - 验证：在 MySQL 8.0 执行成功，表结构与设计文档一致
- [x] Task 2.2: 实现实体类与 MyBatis-Plus 配置
  - 8 个 Entity（User/Category/Product/SeckillGoods/SeckillOrder/UserAddress/LoginLog/OperationLog），@TableName/@TableId(ASSIGN_ID)/@TableLogic/@TableField(fill) 注解
  - `config/MybatisPlusConfig.java`：分页插件、乐观锁插件
  - `config/MetaObjectHandler.java`：自动填充 create_time/update_time
  - 验证：实体字段与表结构一一对应
- [x] Task 2.3: 实现 Mapper 层
  - 8 个 Mapper 接口（继承 BaseMapper），含自定义方法：SeckillOrderMapper.findByUserAndSeckill、ProductMapper.selectProductPage 等
  - `resources/mapper/*.xml` 对应自定义 SQL（参数化查询）
  - 验证：@MybatisTest 切片测试通过
- [x] Task 2.4: 实现枚举与常量
  - UserRole(BUYER/SELLER/ADMIN)、UserStatus(ACTIVE/DISABLED)、ProductStatus(ON_SALE/OFF_SHELF)、SeckillStatus(PENDING/ACTIVE/ENDED/CANCELLED)、OrderStatus(UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED)、LoginResult(SUCCESS/FAILED)
  - 验证：枚举值与 DB ENUM 一致

## M3 安全认证模块（P0）
- [x] Task 3.1: 实现 JWT 工具与 Token 管理
  - `security/JwtUtils.java`：生成/解析/验证 AccessToken(2h) 与 RefreshToken(7d)，HS256，Claims(userId/username/role/iat/exp/tokenType)
  - `security/TokenBlacklistService.java`：Redis 黑名单 `token:blacklist:{tokenId}`（TTL=剩余有效期）
  - 验证：Token 生成解析单测通过
- [x] Task 3.2: 实现 Spring Security 配置与过滤器
  - `config/SecurityConfig.java`：禁用 CSRF、CORS 配置、无状态 Session、放行/auth/**与公开接口、其余需鉴权
  - `security/JwtAuthenticationFilter.java`：从 Header 解析 Token→校验黑名单→加载用户→设 SecurityContext
  - `security/JwtAuthenticationEntryPoint.java`（401）、`JwtAccessDeniedHandler.java`（403）
  - 验证：受保护接口无 Token 返回 401
- [x] Task 3.3: 实现用户认证业务与接口
  - `service/AuthService.java`(/Impl)：register、login、logout、refresh、getMe、changePassword
  - `controller/AuthController.java`：6 个接口，@Valid 参数校验
  - 登录流程：校验验证码→查用户→BCrypt 比对→失败计数(login:fail 30min，5 次锁定，3 次强制验证码)→成功写 t_login_log→签发双 Token
  - 注册：用户名/手机号唯一校验、BCrypt 加密、默认 BUYER
  - `service/CaptchaService.java`：生成图形验证码，`captcha:{captchaId}` TTL 5min，一次性
  - 验证：登录/注册/登出/刷新接口联调通过

## M4 商品与分类模块（P1）
- [x] Task 4.1: 实现商品管理业务与接口
  - `service/ProductService.java`(/Impl)：listProducts(分页+分类+关键词+排序)、getProductDetail、createProduct(ADMIN)、updateProduct(ADMIN)、deleteProduct(逻辑删除)
  - `controller/ProductController.java`：6 个接口
  - 商品详情缓存：`seckill:goods:{id}` String(JSON) 30min+随机偏移，空值缓存防穿透，互斥锁防击穿
  - 验证：商品 CRUD 与分页接口通过
- [x] Task 4.2: 实现分类管理
  - `service/CategoryService.java`(/Impl)：getCategoryTree（递归构建分类树）
  - `controller/CategoryController.java`：GET /api/v1/categories
  - 验证：分类树返回正确层级

## M5 秒杀核心模块（P0）
- [x] Task 5.1: 实现 Redis 缓存服务与 Key 管理
  - `cache/RedisService.java`：封装 get/set/del/incr/decr/sismember/setIfAbsent/scanKeys(SCAN) 等操作
  - `cache/RedisKeyConstants.java`：所有 Key 前缀常量（seckill:goods/seckill:stock/seckill:bought/seckill:result/rate:seckill/...）
  - 验证：Redis 操作单测通过
- [x] Task 5.2: 实现秒杀活动管理
  - `service/SeckillGoodsService.java`(/Impl)：listSeckill、getSeckillDetail、createSeckill(ADMIN/SELLER)、updateSeckill、cancelSeckill
  - `controller/SeckillController.java`（活动管理部分）
  - 活动缓存预热：start_time 前/活动开始时加载 `seckill:info:{id}`(Hash)、`seckill:stock:{id}`(int)、初始化 `seckill:bought:{id}`(Set)
  - 布隆过滤器 `seckill:bloom:goods` 初始化（ApplicationRunner）
  - 验证：活动创建后 Redis 预热数据正确
- [x] Task 5.3: 实现 Lua 脚本原子库存预减
  - `resources/lua/seckill_deduct.lua`：原子执行 SISMEMBER 已购校验 + DECR 库存预减 + SADD 已购标记，返回 1 成功 / -1 库存不足 / -2 重复
  - `cache/SeckillLuaService.java`：执行 Lua 脚本（DefaultRedisScript）
  - 验证：100 并发抢 10 库存零超卖
- [x] Task 5.4: 实现秒杀令牌与执行秒杀接口
  - `service/SeckillTokenService.java`：getSeckillToken 生成 `seckill:token:{seckillId}:{userId}`，TTL 限活动期间
  - `service/SeckillService.java`(/Impl).doSeckill：验证 token→Lua 预减→投递 MQ→返回 requestId(排队中)
  - `controller/SeckillController.java`：POST /api/v1/seckill/{id}、GET /token、GET /stock
  - 验证：秒杀接口返回 requestId，库存正确扣减
- [x] Task 5.5: 实现查询秒杀结果接口
  - GET /api/v1/seckill/{seckillId}/result?requestId=：读 `seckill:result:{seckillId}:{userId}`(TTL 10min)，返回 status(0/1/-1)+orderId+orderNo+totalAmount+payExpireTime
  - 验证：异步下单完成后轮询能拿到成功结果

## M6 RabbitMQ 异步下单与订单模块（P0）
- [x] Task 6.1: 实现 RabbitMQ 配置与拓扑
  - `config/RabbitMQConfig.java`：声明 4 组 Exchange/Queue/Binding（seckill.order direct / order.delay+DLX TTL 15min / order.cancel / seckill.result fanout）
  - `mq/message/`：SeckillOrderMessage、OrderDelayMessage、SeckillResultMessage
  - 验证：应用启动后 MQ 队列自动声明
- [x] Task 6.2: 实现秒杀下单消息生产者与消费者
  - `mq/producer/SeckillOrderProducer.java`：发送 SeckillOrderMessage 到 seckill.order.exchange
  - `mq/consumer/SeckillOrderConsumer.java`：手动 ACK，幂等去重(Redis SETNX mq:consumed:{messageId})→调用 OrderService.createSeckillOrder→写 seckill:result→发送延迟取消消息(15min)→发送结果广播→basicAck
  - 验证：消息消费后订单创建，结果可查询
- [x] Task 6.3: 实现订单 Service 与状态机
  - `service/OrderService.java`(/Impl)：createSeckillOrder(INSERT t_seckill_order，uk_user_seckill 兜底)、getOrderList、getOrderDetail、payOrder、cancelOrder、getOrderStatus
  - 订单号生成：时间戳+随机串（SK+yyyyMMddHHmmss+seq）
  - `mq/consumer/OrderCancelConsumer.java`：消费延迟消息，校验未支付→置 TIMEOUT→回补 Redis 库存(INCR)+SREM 购买标记
  - 验证：订单状态流转正确，超时自动取消并回补库存
- [x] Task 6.4: 实现订单 Controller 与支付接口
  - `controller/OrderController.java`：5 个接口
  - 支付：模拟支付，校验状态/权限/超时→更新 PAID+pay_time+transactionId→取消延迟任务→发支付邮件
  - 验证：支付后订单 PAID，重复支付返回 3002

## M7 安全防御与限流（P1）
- [x] Task 7.1: 实现接口限流（Lua 令牌桶）
  - `resources/lua/rate_limit.lua`：令牌桶限流脚本
  - `aspect/RateLimitAspect.java` 或 `security/RateLimitFilter.java`：用户级 rate:seckill:{userId} 1次/秒，IP 级 rate:ip:{ip}:{path} 30次/60s
  - `annotation/RateLimit.java` 注解
  - 验证：超频请求被限流返回 429
- [ ] Task 7.2: 实现防重放与幂等
  - 防重放：HMAC-SHA256 请求签名 + 时间窗口 60s + Nonce 去重（Redis Set `nonce:{value}` TTL 60s）
  - 幂等：秒杀 requestId、支付 orderId
  - 验证：重放请求被拦截
- [ ] Task 7.3: 实现 XSS 防护与输入校验
  - Jsoup 白名单清洗工具（商品详情 HTML 过滤）
  - DTO 参数 @Valid 校验注解（@NotBlank/@Size/@Pattern 等）
  - 验证：恶意 HTML 被清洗

## M8 后台管理与系统模块（P2）
- [ ] Task 8.1: 实现后台用户管理
  - `service/AdminUserService.java`(/Impl)、`controller/AdminUserController.java`：用户列表/启禁用/改角色/登录日志
  - 操作日志 AOP `aspect/OperationLogAspect.java`：记录到 t_operation_log
  - 验证：后台接口仅 ADMIN 可访问
- [x] Task 8.2: 实现系统管理
  - `service/SystemService.java`：dashboard 统计（用户数/订单数/销售额/活动数）、操作日志列表、系统健康检查（Redis/MQ/DB）
  - `controller/SystemController.java`：3 个接口
  - 验证：仪表盘数据正确

## M9 邮件通知与容错降级（P2）
- [ ] Task 9.1: 实现邮件通知服务
  - `service/EmailService.java`(/Impl)：sendRegisterVerify/sendSeckillSuccess/sendPaySuccess/sendOrderCancel/sendPasswordReset
  - `config/AsyncConfig.java`：emailExecutor 线程池（核心2/最大5）
  - `config/RetryConfig.java`：Spring Retry（3 次指数退避 1s/2s/4s）
  - Thymeleaf 模板：templates/email/register-verify.html、seckill-success.html、pay-success.html、order-cancel.html、password-reset.html
  - 验证：秒杀成功/支付成功触发邮件
- [x] Task 9.2: 实现容错降级
  - `cache/CacheDegradeService.java`：Redis 健康检查，宕机切换数据库模式
  - `service/SeckillService` 策略模式：缓存模式(Redis Lua) / 数据库模式(乐观锁 UPDATE WHERE stock>0)
  - MQ 宕机降级：同步下单 + 补偿任务
  - 验证：停 Redis 后秒杀降级到 DB 模式不超卖

## M10 测试、文档与部署（P2）
- [x] Task 10.1: 编写单元测试
  - Service 层测试（Mockito Mock Mapper）：SeckillService/OrderService/AuthService 核心方法，覆盖率 > 80%
  - Mapper 层测试（@MybatisTest）：CRUD 与自定义查询
  - 验证：mvn test 通过，JaCoCo 覆盖率达标
- [x] Task 10.2: 编写集成测试
  - Testcontainers 拉起 MySQL/Redis/RabbitMQ
  - 秒杀完整链路 E2E 测试：注册→登录→秒杀→轮询结果→支付
  - 验证：集成测试通过
- [x] Task 10.3: 配置 Knife4j API 文档
  - `config/Knife4jConfig.java`，所有 Controller 添加 @Tag/@Operation 注解
  - 验证：访问 /doc.html 查看完整文档
- [x] Task 10.4: 编写 Docker Compose 与 Dockerfile
  - `docker-compose.yml`：MySQL 8.0 + Redis 7 + RabbitMQ 3.13 + 应用
  - `Dockerfile`：多阶段构建（Maven 编译 + JRE 运行）
  - 验证：docker-compose up 一键启动
- [x] Task 10.5: 配置 Actuator 健康检查
  - 暴露 health/info 端点，自定义 Redis/MQ/DB 健康指示器
  - 验证：/actuator/health 返回 UP

# Task Dependencies
- M2 依赖 M1（项目骨架）
- M3 依赖 M2（需 User 实体与 Mapper）
- M4 依赖 M2、M3（需鉴权）
- M5 依赖 M2、M3、M4（需商品/活动实体与鉴权）
- M6 依赖 M5（秒杀下单消息由 M5 发送）
- M7 可与 M5/M6 并行（限流/防重放独立组件）
- M8 依赖 M3、M6（需鉴权与订单数据）
- M9 可与 M6/M8 并行（邮件与降级独立）
- M10 依赖所有业务模块完成

# 可并行任务
- Task 4.1 / 4.2（商品与分类）可与 Task 3.3 后并行
- Task 7.1 / 7.2 / 7.3 可并行
- Task 9.1 / 9.2 可并行
- Task 10.1 / 10.3 / 10.4 / 10.5 可并行
