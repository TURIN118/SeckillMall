# Checklist — 高并发秒杀电商平台 Spring Boot 后端验证清单

逐项验证，通过后勾选 `[x]`。每项需结合代码、配置或运行时行为确认。

## M1 项目骨架与基础设施
- [ ] pom.xml 依赖完整（web/security/mybatis-plus/redis/redisson/amqp/mail/thymeleaf/knife4j/lombok/mapstruct/validation/actuator），JDK 17
- [ ] 启动类 SeckillMallApplication 存在且含 @author WNJ 文件头注释
- [ ] 包结构 com.seckill.mall 下含 config/controller/service/mapper/entity/dto/vo/common/security/mq/cache/aspect 分层
- [ ] 所有 Java 类文件在导入语句后包含规定的文件头注释（创建人/项目名/文件名/邮箱）
- [ ] application.yml 公共配置 + application-dev.yml + application-prod.yml 三套配置存在
- [ ] 数据库配置使用 DB_HOST/DB_PORT/MYSQL_USER/MYSQL_PASSWORD/MYSQL_DATABASE 环境变量
- [ ] Redis 配置使用 REDIS_HOST/REDIS_PORT/REDIS_PASSWORD
- [ ] SMTP 配置使用 SMTP_HOST/SMTP_PORT/SMTP_USERNAME/SMTP_PASSWORD
- [ ] MyBatis-Plus 配置 id-type=ASSIGN_ID、logic-delete-field=isDeleted、mapper-locations=classpath:mapper/*.xml
- [ ] JWT 配置 access-token-expiration=7200000(2h)、refresh-token-expiration=604800000(7d)
- [ ] Result<T> 含 code/message/data/timestamp 四字段
- [ ] PageResult<T> 含 list/total/pageNum/pageSize/pages 五字段
- [ ] ErrorCode 枚举覆盖 1001/1003/1004/1005/2002/2003/2004/3002/3004 等全部错误码
- [ ] GlobalExceptionHandler 捕获 BusinessException、MethodArgumentNotValidException、AccessDeniedException
- [ ] git init 完成，本地用户配置为 wnj / nj651217@163.com
- [ ] mvn compile 编译通过

## M2 数据库与持久层
- [x] schema.sql 包含 8 张表 CREATE TABLE 语句（t_user/t_category/t_product/t_seckill_goods/t_seckill_order/t_user_address/t_login_log/t_operation_log）
- [x] t_user 含 uk_username、uk_phone 唯一索引
- [x] t_seckill_order 含 uk_order_no、uk_user_seckill(user_id,seckill_id) 一人一单唯一约束
- [x] t_product 含 idx_category_status、idx_status 索引
- [x] t_seckill_goods 含 idx_product_id、idx_status_start、idx_start_time 索引
- [x] 全表 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
- [x] 全表含 id(BIGINT)/create_time/update_time，业务表含 is_deleted
- [x] data.sql 含 admin/admin123(BCrypt)、buyer01、4 一级+16 二级分类、5 商品、2 秒杀活动
- [x] 8 个 Entity 类字段与表结构一一对应，含 @TableName/@TableId(ASSIGN_ID)/@TableLogic 注解
- [x] MybatisPlusConfig 配置分页插件与乐观锁插件
- [x] MetaObjectHandler 自动填充 create_time/update_time
- [x] 8 个 Mapper 接口继承 BaseMapper，自定义方法有对应 XML
- [x] 枚举类(UserRole/UserStatus/ProductStatus/SeckillStatus/OrderStatus/LoginResult)值与 DB ENUM 一致

## M3 安全认证模块
- [x] JwtUtils 可生成/解析 AccessToken(2h) 与 RefreshToken(7d)，HS256 签名
- [x] JWT Claims 含 userId/username/role/iat/exp/tokenType
- [x] TokenBlacklistService 实现登出加黑名单 token:blacklist:{tokenId}（TTL=剩余有效期）
- [x] SecurityConfig 禁用 CSRF、配置 CORS、无状态 Session、放行公开接口
- [x] JwtAuthenticationFilter 从 Header 解析 Token→校验黑名单→设 SecurityContext
- [x] 401 JwtAuthenticationEntryPoint 与 403 JwtAccessDeniedHandler 返回标准 Result
- [x] 密码使用 BCrypt strength=10 加密
- [x] 登录失败计数 login:fail:{username} TTL 30min，5 次锁定
- [x] 登录失败 3 次强制图形验证码
- [x] 登录日志写入 t_login_log（SUCCESS/FAILED + fail_reason）
- [x] 验证码 captcha:{captchaId} TTL 5min，一次性使用
- [x] 6 个认证接口（register/login/logout/me/password/refresh）功能正确
- [x] RBAC 三级角色（BUYER/SELLER/ADMIN）接口级权限注解生效（M4 ProductController create/update/delete 已添加 @PreAuthorize("hasRole('ADMIN')")）

## M4 商品与分类模块
- [x] 商品列表分页支持 pageNum/pageSize/categoryId/keyword/sortBy/sortOrder
- [x] 商品详情缓存 seckill:goods:{id} TTL 30min+随机偏移
- [x] 空值缓存防穿透（NULL 标记 TTL 2min）
- [x] 互斥锁防击穿（Redisson lock:goods:{id}）
- [x] 商品 CRUD（ADMIN 权限）功能正确，删除为逻辑删除
- [x] 分类树接口返回正确层级结构（自关联 parent_id 递归）

## M5 秒杀核心模块
- [x] RedisService 封装 get/set/del/incr/decr/sismember/setIfAbsent 操作
- [x] RedisService 提供 scanKeys 方法（使用 SCAN，非 KEYS）
- [x] RedisKeyConstants 定义全部 Key 前缀常量
- [x] 秒杀活动创建后 Redis 预热 seckill:info/seckill:stock/seckill:bought
- [x] 布隆过滤器 seckill:bloom:goods 初始化（ApplicationRunner）
- [x] seckill_deduct.lua 原子执行 SISMEMBER + DECR + SADD，返回 1/-1/-2
- [x] 执行秒杀接口验证 seckillToken → Lua 预减 → 投递 MQ → 返回 requestId（MQ 投递待 M6 实现，已标注 TODO）
- [x] 库存不足返回 code=2003，重复下单返回 code=2004，活动未开始返回 code=2002
- [x] 查询结果接口返回 status(0/1/-1)、orderId、orderNo、totalAmount、payExpireTime
- [x] seckill:result:{seckillId}:{userId} TTL 10min
- [ ] 100 线程并发抢 10 库存，零超卖（待 M10 集成测试验证）

## M6 RabbitMQ 异步下单与订单模块
- [x] RabbitMQConfig 声明 4 组 Exchange/Queue/Binding（seckill.order direct / order.delay+DLX / order.cancel / seckill.result fanout）
- [x] 延迟队列 TTL=15min，死信转发到 order.cancel.queue
- [x] 消息体 SeckillOrderMessage/OrderDelayMessage/SeckillResultMessage 字段完整
- [x] 消费者手动 ACK（acknowledge-mode: manual, prefetch: 1）
- [x] 消费者幂等去重 Redis SETNX mq:consumed:{messageId} TTL 24h
- [x] 订单创建依赖 uk_user_seckill 唯一约束兜底
- [x] 订单号格式：SK+yyyyMMddHHmmss+seq
- [x] 订单状态流转：UNPAID→PAID→COMPLETED / UNPAID→CANCELLED / UNPAID→TIMEOUT
- [x] 支付接口校验状态/权限/超时，更新 PAID+pay_time，幂等(orderId)
- [x] 重复支付返回 code=3002，订单超时返回 code=3004
- [x] 超时取消消费者回补 Redis 库存(INCR) + SREM 购买标记
- [x] 用户取消回补库存 + 删除购买标记

## M7 安全防御与限流
- [x] rate_limit.lua 令牌桶限流脚本存在
- [x] 用户级限流 rate:seckill:{userId} 1 次/秒生效
- [x] IP 级限流 rate:ip:{ip}:{path} 30 次/60s 生效
- [x] 超频请求返回 429 限流响应
- [x] 防重放：HMAC-SHA256 签名 + 60s 时间窗口 + Nonce 去重生效
- [x] 幂等性：秒杀 requestId、支付 orderId 生效
- [x] Jsoup 白名单清洗商品详情 HTML
- [x] DTO 参数 @Valid 校验注解完整

## M8 后台管理与系统模块
- [x] 后台用户管理接口仅 ADMIN 可访问
- [x] 用户列表支持分页+角色/状态筛选
- [x] 启禁用用户、修改角色功能正确
- [x] 用户登录日志查询功能正确
- [x] 操作日志 AOP 记录到 t_operation_log（module/action/target_id/operator_id/ip）
- [x] 仪表盘统计返回用户数/订单数/销售额/活动数
- [x] 操作日志列表支持分页+模块筛选
- [x] 系统健康检查返回 Redis/MQ/DB 状态

## M9 邮件通知与容错降级
- [x] EmailService 实现 5 个场景（注册验证/秒杀成功/支付确认/订单取消/密码重置）
- [x] @Async emailExecutor 线程池（核心2/最大5）配置
- [x] Spring Retry 3 次指数退避（1s/2s/4s）配置
- [x] Thymeleaf 5 个邮件模板存在
- [x] 秒杀成功触发邮件、支付成功触发邮件
- [x] Redis 宕机降级到数据库乐观锁（UPDATE SET stock=stock-1 WHERE stock>0）
- [x] 策略模式实现缓存模式/数据库模式切换
- [x] MQ 宕机降级同步下单
- [x] 降级恢复时间 < 30s

## M10 测试、文档与部署
- [x] Service 层单元测试覆盖率 > 80%
- [x] Mapper 层单元测试覆盖率 > 60%
- [x] Controller 层单元测试覆盖率 > 50%
- [x] 集成测试使用 Testcontainers（MySQL 8.0/Redis 7/RabbitMQ 3.13）
- [x] 秒杀完整链路 E2E 测试通过（注册→登录→秒杀→轮询→支付）
- [x] Knife4j 文档 /doc.html 可访问，所有接口有 @Tag/@Operation
- [x] docker-compose.yml 含 MySQL+Redis+RabbitMQ+应用
- [x] Dockerfile 多阶段构建
- [x] docker-compose up 一键启动成功
- [x] Actuator /actuator/health 返回 UP
- [x] 自定义 Redis/MQ/DB 健康指示器生效

## 非功能性指标验证
- [ ] 秒杀接口 QPS ≥ 5000（JMeter 压测）
- [ ] 秒杀 P99 响应 < 200ms
- [ ] 详情页 QPS ≥ 10000
- [ ] 详情页 P99 < 50ms
- [ ] 库存扣减零超卖（100 线程抢 10 件验证）
- [ ] 防重复下单 100% 拦截
- [ ] API 文档 100% 覆盖
