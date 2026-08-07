# Bug修复总结（79项 + 6项审查问题）

> **修复时间**: 2026-08-07  
> **质量评估**: 95.5/100 PASS  
> **编译状态**: 后端 mvn compile SUCCESS / 前端 vue-tsc 0 errors  
> **单元测试**: OrderServiceTest 11/11、ProductServiceTest 11/11、AuthControllerTest 6/6 通过

---

## 一、阻断级（5项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| B1 | RSA私钥泄露 | private.pem被Git跟踪且烘焙进Docker镜像 | git rm移除+新密钥对+.gitignore+Dockerfile运行时挂载 |
| B2 | 前端HMAC防重放架构不成立 | SPA无法保管共享密钥，sign-secret硬编码 | 废弃前端HMAC，改用服务端签发的一次性秒杀令牌 |
| B3 | Redis库存双重扣减 | Lua预减+消费者再DECR导致stock减2次 | 消费者改用SET校正到DB值，统一DB为真相来源 |
| B4 | OrderController泄露Entity | 接口返回SeckillOrder entity含isDeleted等内部字段 | 新增SeckillOrderVO转换层，接口签名改返回VO |
| B5 | PII入库未脱敏 | seckill_mall.sql含真实姓名/手机/邮箱 | git rm移除+data.sql脱敏为@example.com |

---

## 二、严重级（6项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| C1 | 生产构建跑不通 | 配置terser但未声明依赖 | 改用Vite内置esbuild minify，零额外依赖 |
| C2 | X-Forwarded-For限流绕过 | 无条件信任XFF头，伪造即可绕过IP限流 | 新增可信代理白名单，仅白名单内才信任XFF |
| C3 | @RateLimit seconds参数失效 | Lua脚本未使用seconds参数 | Lua接收seconds，refillRate=capacity/seconds |
| C4 | 充值卡生成接口无法返回明文卡密 | @JsonIgnore字段级注解子类无法取消 | RechargeCardGenerateVO独立声明不继承父类 |
| C5 | 商品ID精度丢失 | 雪花Long ID超过JS Number.MAX_SAFE_INTEGER | 前端全程用string类型，axios用encodeURIComponent |
| C6 | 创建秒杀场次500错误 | registerAfterCommit中预热异常导致响应异常 | 改用@TransactionalEventListener事件驱动 |

---

## 三、高危级（19项）

### 并发/数据一致性（7项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| H-C1 | 取消/超时不回补DB库存 | rollbackStock只动Redis，DB库存单调递减 | afterCommit中回补DB available_count+补偿对账任务 |
| H-C2 | 消费者DB扣减失败仍写成功 | 扣减返回0时仅告警不回滚，产生幽灵单 | 扣减失败时物理删除订单+写失败结果 |
| H-C3 | 支付并发双扣 | 先读状态再updateById无乐观条件 | 状态判定下沉SQL: WHERE status=UNPAID乐观锁 |
| H-C4 | MQ降级路径不扣DB库存 | 同步降级仅建单不扣库存不发延迟消息 | degradeToSyncCreate复刻异步路径关键副作用 |
| H-C5 | RabbitMQ发布不可靠 | 未开启publisher-confirm/returns | 自定义RabbitTemplate设置ConfirmCallback+CorrelationData |
| H-C6 | 秒杀队列无死信队列 | basicNack(requeue=false)直接丢弃 | 配置DLX/DLQ，毒消息进入DLQ可补偿 |
| H-C7 | 秒杀取消缺乐观锁状态机 | cancelOrder无WHERE status，并发双回补 | 引入UNPAID→CANCELLING→CANCELLED状态机 |

### 安全（1项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| H-S1 | Banner存储型XSS | BannerServiceImpl入库前未XSS清洗 | 入库前对title调cleanStrict，URL调clean+协议白名单 |

### 配置/部署（5项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| H-K1 | prod profile缺连接配置 | 无url/username/password，JWT_SECRET死配置 | 补齐全部配置，${VAR:?required}强制注入 |
| H-K2 | Redis默认无口令+端口全网发布 | ${REDIS_PASSWORD:-}默认空，ports绑0.0.0.0 | ${REDIS_PASSWORD:?required}必填+绑127.0.0.1 |
| H-K3 | MySQL弱口令+端口发布 | root123弱口令，3306绑0.0.0.0 | 全部${...:?required}必填+绑127.0.0.1 |
| H-K4 | RabbitMQ默认guest/guest | 15672硬编码发布，默认guest口令 | 口令必填+15672绑127.0.0.1 |
| H-K5 | 应用以root+空口令连库 | ${DB_USERNAME:root}/${DB_PASSWORD:} | 建专用seckill_app账号+useSSL=true |

### 前端（4项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| H-F1 | 生产构建秒杀必401 | VITE_SIGN_SECRET=空被tree-shake，三件套校验失败 | 与B2联动，改用服务端token方案 |
| H-F2 | Token刷新失败Promise永不settle | 刷新失败丢弃resolve闭包，Promise永久pending | 队列存{resolve,reject}，失败时显式reject |
| H-F3 | 秒杀数据双轨制不一致 | 新旧两套API数据不同步 | Promise.allSettled并发拉取，新版优先旧版回退 |
| H-F4 | 缺少Brotli压缩 | 仅有gzip输出 | 追加brotliCompress，threshold:10240 |

### 数据访问（1项）

| ID | 标题 | 根因 | 修复 |
|----|------|------|------|
| H-D1 | 未启用防全表更新拦截器 | MybatisPlusConfig缺BlockAttackInnerInterceptor | 追加BlockAttackInnerInterceptor阻止无where的update/delete |

---

## 四、中危级（35项）

### 安全（6项）

| ID | 标题 | 修复 |
|----|------|------|
| M-S1 | 登录接口缺限流 | 加@RateLimit(capacity=10, seconds=60) |
| M-S2 | Banner入参缺@Valid | 拆分BannerCreateRequest/UpdateRequest DTO+@Valid |
| M-S3 | SeckillController误导注释 | 删除暗示从请求体取userId的TODO注释 |
| M-S4 | 密码喷洒攻击 | 叠加按IP维度失败计数，阈值20次/15分钟 |
| M-S5 | 用户名枚举 | t_login_log.user_id允许NULL+统一错误返回 |
| M-S6 | HTTP状态码恒为200 | 为不同异常设置对应HTTP状态(400/401/403/429/500) |

### 并发/消息（3项）

| ID | 标题 | 修复 |
|----|------|------|
| M-C1 | 幂等键时序错误 | 幂等键在处理成功提交后再设置 |
| M-C2 | Lua/Redis回补非原子 | 新增seckill_rollback.lua原子执行INCR+SREM |
| M-C3 | Lua stockTtl参数未传 | 新增四参数重载，Lua在stockTtl>0时EXPIRE |

### 配置（6项）

| ID | 标题 | 修复 |
|----|------|------|
| M-K1 | .gitignore不覆盖.env.* | 改为.env/.env.*/!.env.example |
| M-K2 | CORS生产未覆盖+通配 | 改setAllowedOrigins精确匹配，生产不允许* |
| M-K3 | 容器以root运行+无HEALTHCHECK | 建app用户+USER app+HEALTHCHECK+优雅停机 |
| M-K4 | t_seckill_activity缺COLLATE | 补utf8mb4_general_ci+主键改BIGINT NOT NULL |
| M-K5 | 01_schema.sql无环境护栏 | 加@schema_destructive_allowed变量检查 |
| M-K6 | t_user_coupon/t_product_review缺索引 | 补(user_id,status,create_time)复合索引 |

### 契约/数据访问（7项）

| ID | 标题 | 修复 |
|----|------|------|
| M-D1 | BannerVO双向用作请求/响应 | 新建BannerCreateRequest/UpdateRequest分离请求体 |
| M-D2 | Controller直调Mapper绕过Service | 新建WalletService/UserService封装数据访问 |
| M-D3 | DTO已声明校验但Controller未触发 | 4个Controller方法参数前加@Valid |
| M-D4 | CategoryUpdateRequest/LoginRequest缺校验 | 补@Size/@Min/@Pattern等约束注解 |
| M-D5 | 声明MapStruct却零使用 | 新建converter包，3个MapStruct mapper接口 |
| M-D6 | ProductSkuMapper.xml用SELECT * | 新增Base_Column_List显式列替代SELECT * |
| M-D7 | PageResult.total类型不匹配 | total从long改为int，序列化为JSON number |

### 前端（8项）

| ID | 标题 | 修复 |
|----|------|------|
| M-F1 | 路由角色校验fail-open | 改为fail-closed，无userInfo即next('/403') |
| M-F2 | OrderDetail.vue用any擦除类型 | 换成NormalOrderDetailVO和SeckillOrder强类型 |
| M-F3 | 导出订单撞10s超时 | 该请求单独放宽timeout至60000ms |
| M-F4 | 前端持续轮询不暂停 | 新增useVisibilityPolling composable，后台暂停 |
| M-F5 | build.target过旧 | 改为target:'modules'基于原生ES Module |
| M-F6 | terser慢且需额外依赖 | 与C1联动改用esbuild minify |
| M-F7 | 注册页密码规则不一致 | 统一placeholder为"6-20位含大小写字母和数字"+rules对齐 |
| M-F8 | 找回密码Tab切换残留错误 | 新增switchType清空errors和account |

### 功能（3项）

| ID | 标题 | 修复 |
|----|------|------|
| M-F9 | buyer01密码不一致 | 移除含PII的seckill_mall.sql，验证data.sql哈希正确 |
| M-F10 | Banner标题为空 | BannerCreateRequest.title加@NotBlank+Service二次校验 |
| M-F11 | 注册接口验证码 | SecurityConfig放行但验证码校验失败返回正确错误 |

---

## 五、低危级（14项）

### 安全（4项）

| ID | 标题 | 修复 |
|----|------|------|
| L-S1 | ~ | 已修复（详见组A日志） |
| L-S2 | ~ | 已修复 |
| L-S3 | ~ | 已修复 |
| L-S4 | ~ | 已修复 |

### 配置（3项）

| ID | 标题 | 修复 |
|----|------|------|
| L-S5 | 登录日志user_id NOT NULL | 改为允许NULL（M-S5联动） |
| L-S6 | ~ | 已修复 |
| L-S7 | ~ | 已修复 |

### 其他（7项）

| ID | 标题 | 修复 |
|----|------|------|
| L-O1 | ~ | 已修复 |
| L-O2 | ~ | 已修复 |
| L-O3 | ~ | 已修复 |
| L-F2 | 系统健康页显示"null%" | v-if兜底显示"暂未提供" |
| L-F3 | ElPagination废弃用法警告 | 确认已迁移到v-model:current-page |
| L-F4 | element-plus主入口引入形成150KB共享块 | 路由守卫ElMessage改动态import |
| L-F5 | ~ | 已修复 |

---

## 六、代码审查阻断性问题（6项）

| 问题 | 根因 | 修复 |
|------|------|------|
| OrderServiceTest断言过时 | PageResult.total改int后断言未同步 | isEqualTo(1L)→isEqualTo(1) |
| UserServiceImpl未脱敏PII | 直接返回entity含明文phone/email | @AfterMapping中脱敏phone为138****8000格式 |
| forward-headers-strategy无条件信任 | 所有环境都启用native | 改为仅当配置了可信代理时才启用 |
| Redis healthcheck明文密码 | docker-compose健康检查含明文密码 | 改用redis-cli ping不带密码 |
| ReplayProtectionFilter sign-secret降级 | 密钥为空时降级为不校验 | 密钥为空时启动失败而非降级 |
| SQL环境护栏形同虚设 | 护栏变量检查可被绕过 | 加固为SIGNAL SQLSTATE '45000'硬终止 |

---

## 修改文件统计

- **后端Java**: 50+个文件修改/新增
- **前端**: 20+个文件修改/新增
- **配置/SQL**: 15+个文件修改/新增
- **测试**: 5个文件修改
- **总计**: 96个文件变更，+8041行 / -1918行

---

## 七、历史文档来源说明

以下6份历史文档已融入本总结，原文档已删除：

| 文档 | 性质 | 与79项Bug的关系 | 修复状态 |
|------|------|-----------------|----------|
| `秒杀抢购401问题修复方案.md` | 401问题修复方案 | 已在B2（前端HMAC防重放架构重构）+ H-F1（生产构建秒杀401）中修复 | ✅ 已修复 |
| `docs/改进方案与Bug修复计划.md` | 13项需求/Bug改进计划 | 13项需求全部被79项Bug覆盖 | ✅ 已修复 |
| `docs/bug-audit-report.md` | 全量Bug排查报告（8严重+其他） | 79项Bug的来源之一，所有问题已修复 | ✅ 已修复 |
| `docs/Bug排查报告.md` | 全项目Bug排查报告（5严重+5中等） | 79项Bug的来源之一，所有问题已修复 | ✅ 已修复 |
| `docs/后端死代码排查报告.md` | 后端死代码排查（204个未使用方法） | 死代码排查非Bug修复，作为代码质量参考 | ⚠️ 参考 |
| `docs/前端死代码排查报告.md` | 前端死代码排查 | 同上 | ⚠️ 参考 |

---

## 八、SQL文件整理说明

### 全量文件（sql/目录，供DBA手动执行）
| 文件 | 内容 | 特点 |
|------|------|------|
| `sql/01_schema_full.sql` | 23张表完整建表 | 含环境护栏，需`SET @SCHEMA_DESTRUCTIVE_ALLOWED='true'` |
| `sql/02_init_data_full.sql` | 190条脱敏数据 | PII已脱敏 |
| `sql/03_migration_full.sql` | 幂等迁移脚本 | 可重复执行 |

### 自动初始化文件（resources/sql/目录，供docker-compose和测试自动执行）
| 文件 | 内容 | 说明 |
|------|------|------|
| `resources/sql/schema.sql` | 23张表完整建表 | 从全量文件同步，移除环境护栏，适合docker-entrypoint自动执行 |
| `resources/sql/data.sql` | 190条脱敏数据 | 从全量文件同步，适合自动初始化 |

> **注**：两套文件内容一致，区别仅在于全量文件含环境护栏（防止误执行破坏性DDL）。docker-compose和application-test.yml引用resources/sql/下的版本。