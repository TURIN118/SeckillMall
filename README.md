# 秒杀商城（Seckill Mall）

> 基于 **Spring Boot 3.2.5 + Vue 3.5.12** 的高并发秒杀电商平台，涵盖商品管理、购物车、订单、秒杀抢购、支付、用户中心、后台管理等全链路功能。前后端分离架构，Docker Compose 一键部署（含前端 Nginx 反向代理）。

**核心亮点：**

- ⚡ **Redis Lua 原子扣减**：秒杀库存通过 Lua 脚本原子操作，杜绝超卖
- 🚀 **RabbitMQ 异步下单**：消息队列削峰填谷，支持高并发抢购
- 🔐 **JWT + RSA 非对称加密**：私钥运行时挂载，不进镜像不入库
- 🐳 **Docker Compose 一键部署**：5 服务编排（MySQL/Redis/RabbitMQ/后端/前端 Nginx），开箱即用
- 🛡️ **完善安全体系**：防重放、限流、XSS/SQL 注入防护、CORS 白名单、BCrypt、PII 脱敏
- 📊 **全链路后台管理**：14 个后台页面 + 16 个前台页面，覆盖电商全场景

---

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [核心功能](#核心功能)
- [快速开始](#快速开始)
- [环境配置](#环境配置)
- [部署指南](#部署指南)
- [API 概览](#api-概览)
- [安全特性](#安全特性)
- [数据库设计](#数据库设计)
- [开发指南](#开发指南)
- [项目截图](#项目截图)
- [版本说明](#版本说明)
- [开发者信息](#开发者信息)
- [License](#license)

---

## 项目简介

本项目是一个完整的秒杀电商平台，模拟京东/淘宝等电商核心业务场景。系统采用前后端分离架构：

- **后端**：基于 Spring Boot 3.2.5 构建高并发秒杀引擎，集成 Redis + RabbitMQ 实现库存原子扣减与异步下单
- **前端**：基于 Vue 3 + TypeScript + Element Plus 构建现代化 SPA 界面，参考京东/淘宝设计风格

系统已完成多轮 Bug 修复与 UI 重构，具备生产级安全防护与高并发处理能力。

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Java | 17 | 开发语言 |
| MyBatis-Plus | 3.5.5 | ORM 框架（含防全表更新拦截） |
| Spring Security | 6.x | 安全认证（基于角色的访问控制） |
| Redis + Redisson | 7 / 3.27.2 | 缓存 + 分布式锁 + 秒杀库存原子操作 |
| RabbitMQ | 3.13 | 异步消息队列（秒杀下单削峰） |
| MySQL | 8.0 | 持久化数据库（utf8mb4 字符集） |
| JWT (JJWT 0.12.3) | RSA 非对称 | 无状态令牌认证 |
| Knife4j | 4.5.0 | API 文档（OpenAPI 3） |
| MapStruct | 1.5.5 | 对象映射（DTO/VO/Entity 转换） |
| Lombok | 1.18.30 | 代码简化 |
| Spring Retry | — | 重试机制 |
| JaCoCo | 0.8.11 | 测试覆盖率统计 |
| Testcontainers | — | 集成测试编排 |
| Apache POI | 5.2.3 | 操作日志导出 Excel |
| Jsoup | 1.17.2 | XSS 过滤（HTML 清洗） |
| Micrometer + Prometheus | — | 指标监控 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.12 | 前端框架（Composition API） |
| TypeScript | 5.6.x | 类型安全（严格模式） |
| Element Plus | 2.8.4 | UI 组件库 |
| Pinia | 2.2.4 | 状态管理 |
| Vue Router | 4.4.5 | 路由管理（history 模式） |
| Axios | 1.7.7 | HTTP 请求 |
| ECharts | 5.5.1 | 数据可视化（后台仪表盘） |
| Vite | 5.4.x | 构建工具 |
| Day.js | 1.11.13 | 日期处理 |
| DOMPurify | 3.4.x | XSS 防护（前端 HTML 清洗） |
| WangEditor | 5.1.x | 富文本编辑器（商品详情） |
| xlsx | 0.18.5 | Excel 导入导出 |

---

## 项目结构

```
SpringBoot/                              # 项目根目录
├── seckill-mall/                        # 后端工程（Spring Boot 3.2.5）
│   ├── src/main/java/com/seckill/mall/
│   │   ├── controller/                  # 26 个接口控制器
│   │   ├── service/                     # 业务逻辑层
│   │   │   └── impl/                    # 业务实现
│   │   ├── entity/                      # 23 个数据实体
│   │   ├── dto/                         # 数据传输对象（请求入参）
│   │   ├── vo/                          # 视图对象（响应出参）
│   │   ├── mapper/                      # MyBatis 映射接口
│   │   ├── config/                      # 配置类（Redis/MQ/Security/Knife4j）
│   │   ├── security/                    # 安全认证模块（JWT/RSA/Filter）
│   │   ├── cache/                       # Redis 缓存服务
│   │   ├── mq/                          # RabbitMQ 消息队列
│   │   │   ├── producer/                # 消息生产者（秒杀下单）
│   │   │   └── consumer/                # 消息消费者（异步创建订单）
│   │   ├── aspect/                      # AOP 切面（限流/日志/脱敏）
│   │   ├── scheduler/                   # 定时任务（超时订单取消）
│   │   └── common/                      # 通用工具类
│   ├── src/main/resources/
│   │   ├── application.yml              # 主配置
│   │   ├── application-dev.yml          # 开发环境
│   │   ├── application-prod.yml         # 生产环境
│   │   ├── mapper/                      # MyBatis XML 映射
│   │   ├── lua/                         # Redis Lua 脚本
│   │   │   ├── seckill_deduct.lua       # 秒杀库存原子扣减
│   │   │   ├── seckill_rollback.lua     # 库存回滚
│   │   │   └── rate_limit.lua           # 令牌桶限流
│   │   └── sql/                         # Docker 初始化 SQL（无护栏）
│   │       ├── schema.sql               # 完整建表（23 张表）
│   │       └── data.sql                 # 初始化数据（190 条）
│   ├── images/                          # 商品图片资源（已 gitignore）
│   ├── Dockerfile                       # 后端镜像构建（多阶段，非 root）
│   └── pom.xml                          # Maven 依赖
│
├── frontend/                            # 前端工程（Vue 3.5.12）
│   ├── src/
│   │   ├── api/                         # API 请求封装（Axios）
│   │   ├── views/                       # 页面视图
│   │   │   ├── front/                   # 14 个前台页面
│   │   │   ├── admin/                   # 14 个后台管理页面
│   │   │   ├── NotFound.vue             # 404 页面
│   │   │   └── Forbidden.vue            # 403 页面
│   │   ├── components/                  # 公共组件
│   │   ├── layouts/                     # 布局组件（前台/后台）
│   │   ├── stores/                      # Pinia 状态仓库
│   │   ├── router/                      # 路由配置（含权限守卫）
│   │   ├── styles/                      # 全局样式
│   │   ├── types/                       # TypeScript 类型定义
│   │   └── utils/                       # 工具函数
│   ├── Dockerfile                       # 前端镜像构建（Node+Nginx 多阶段）
│   ├── nginx.conf                       # Nginx 配置（SPA + 反向代理）
│   ├── .dockerignore
│   └── package.json                     # npm 依赖
│
├── sql/                                 # 数据库脚本（手动执行，含环境护栏）
│   ├── 01_schema_full.sql               # 完整建表脚本（23 张表，有护栏）
│   ├── 02_init_data_full.sql            # 完整初始化数据（190 条）
│   ├── 03_migration_full.sql            # 幂等迁移脚本
│   └── README.md                        # SQL 说明文档
│
├── docs/                                # 技术文档（已 gitignore）
├── docker-compose.yml                   # Docker 编排（5 服务）
├── .env.example                         # 环境变量模板
├── .gitignore
└── README.md                            # 项目文档（本文件）
```

---

## 核心功能

### 前台功能（16 个页面）

| 序号 | 页面 | 路由 | 功能说明 |
|------|------|------|----------|
| 1 | 首页 | `/` | Banner 轮播、秒杀专区入口、商品分类导航、猜你喜欢（无限滚动 + 分类筛选） |
| 2 | 商品列表 | `/products` | 分类筛选、价格区间筛选、多维度排序、分页 |
| 3 | 商品详情 | `/product/:id` | 图片轮播、SKU 规格选择、参数速览、服务保障、商品评价、售后说明 |
| 4 | 购物车 | `/cart` | 左右分栏布局、批量选择/删除、分页、选中结算 |
| 5 | 秒杀专区 | `/seckill` | 场次切换、倒计时、库存优先排序、6 列网格展示 |
| 6 | 结算页 | `/checkout` | 收货地址选择、支付方式、订单备注、订单确认 |
| 7 | 订单中心 | `/orders` | 状态筛选、订单类型筛选、两列布局、订单删除、详情查看 |
| 8 | 订单详情 | `/order/:id` | 左右分栏、收货地址、商品列表、支付、确认收货 |
| 9 | 收藏夹 | `/favorites` | 两列布局、6 列网格、排序、管理模式、批量操作 |
| 10 | 个人中心 | `/profile` | 侧边栏导航、资料修改、密码修改（验证码）、钱包余额 |
| 11 | 我的优惠券 | `/coupons` | 优惠券列表、状态筛选、使用说明 |
| 12 | 登录 | `/login` | 账号/邮箱/手机号登录、图形验证码、记住我 |
| 13 | 注册 | `/register` | 用户注册、验证码、协议同意 |
| 14 | 找回密码 | `/forgot-password` | 邮箱/手机验证码重置密码 |
| 15 | 404 页面 | `*` | NotFound 友好提示，返回首页 |
| 16 | 403 页面 | `/403` | Forbidden 无权限提示 |

### 后台功能（14 个页面）

| 序号 | 页面 | 路由 | 功能说明 |
|------|------|------|----------|
| 1 | 仪表盘 | `/admin/dashboard` | 7 项核心指标、订单/用户趋势图、状态分布饼图、秒杀排行榜、最近订单 |
| 2 | 商品管理 | `/admin/products` | 商品 CRUD、SKU 规格管理、图片上传、上下架 |
| 3 | 商品编辑 | `/admin/products/edit` | 商品信息编辑、富文本详情、SKU 配置 |
| 4 | 订单管理 | `/admin/orders` | 高级筛选、分页排序、发货、详情查看 |
| 5 | 秒杀管理 | `/admin/seckill` | 创建秒杀活动、场次管理、库存设置 |
| 6 | 用户管理 | `/admin/users` | 用户列表、状态管理、角色分配 |
| 7 | 分类管理 | `/admin/categories` | 分类树、属性管理 |
| 8 | 分类属性管理 | `/admin/category-attributes` | 分类属性模板、预设值管理 |
| 9 | 轮播图管理 | `/admin/banners` | 轮播图配置、排序、上下线 |
| 10 | 优惠券管理 | `/admin/coupons` | 优惠券创建、发放、适用范围配置 |
| 11 | 充值卡管理 | `/admin/recharge-cards` | 充值卡生成、批次管理、状态查询 |
| 12 | 评价管理 | `/admin/reviews` | 评价审核、回复、删除 |
| 13 | 系统健康 | `/admin/system` | 服务状态监控、依赖检查 |
| 14 | 操作日志 | `/admin/logs` | 操作记录查询、Excel 导出 |

### 秒杀核心流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户请求 │───▶│ 签名校验 │───▶│ 防重放  │───▶│ Redis Lua│───▶│ RabbitMQ │───▶│ 消费者   │
│  抢购   │    │ HMAC+时间│    │ Redis Set│    │ 原子扣减 │    │ 异步发送 │    │ 创建订单 │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                                       │                                               │
                                       ▼                                               ▼
                                 ┌──────────┐                                   ┌──────────┐
                                 │ 返回失败 │                                   │ 返回成功 │
                                 └──────────┘                                   └──────────┘
```

**详细步骤：**

1. **用户请求**：前端发起秒杀请求，携带商品 ID、用户 ID、签名、时间戳
2. **签名校验**：后端使用 `SECKILL_SIGN_SECRET` 校验 HMAC 签名，防止请求伪造
3. **防重放检测**：Redis Set 存储已处理请求的 `(userId+goodsId+timestamp)`，重复请求直接拒绝
4. **Redis Lua 原子扣减**：执行 `seckill_deduct.lua` 脚本，原子性检查库存并扣减，防止超卖
5. **RabbitMQ 异步发送**：扣减成功后发送下单消息到队列，立即返回"排队中"
6. **消费者创建订单**：消费者异步处理消息，创建秒杀订单，写入数据库
7. **查询结果**：前端轮询秒杀结果接口，获取订单号或失败原因

### 关键技术点

| 技术点 | 实现方式 | 解决问题 |
|--------|----------|----------|
| **Lua 原子性** | `seckill_deduct.lua` 脚本在 Redis 单线程执行 | 库存扣减原子操作，防止超卖 |
| **异步削峰** | RabbitMQ 消息队列 + 手动 ACK | 高并发请求削峰填谷，保护数据库 |
| **防重放攻击** | 签名 + 时间戳 + Redis Set 去重 | 防止用户重复抢购、请求重放 |
| **库存预热** | 活动开始前将库存加载到 Redis | 减少数据库压力，提升响应速度 |
| **超时取消** | 定时任务扫描 + 订单超时兜底检查 | 自动取消未支付订单，回滚库存 |
| **限流防刷** | `rate_limit.lua` 令牌桶 + AOP 切面 | 接口限流，防止恶意刷单 |
| **库存回滚** | `seckill_rollback.lua` 脚本 | 订单取消/超时后回滚秒杀库存 |

---

## 快速开始

### 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端构建环境 |
| Maven | 3.8+ | 后端依赖管理 |
| MySQL | 8.0+ | 持久化数据库 |
| Redis | 7+ | 缓存 + 秒杀库存 |
| RabbitMQ | 3.13+ | 异步消息队列 |

### 1. 克隆项目

```bash
git clone https://gitee.com/nengjie116/seckill-mall.git
cd seckill-mall
```

### 2. 初始化数据库

```bash
# 登录 MySQL，创建数据库
mysql -u root -p
CREATE DATABASE seckill_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 导入数据（使用 sql/ 目录脚本，含环境护栏）
use seckill_mall;
SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true';
SOURCE sql/01_schema_full.sql;
SOURCE sql/02_init_data_full.sql;
```

### 3. 启动后端

```bash
cd seckill-mall

# 修改配置（数据库/Redis/RabbitMQ 连接信息）
# 编辑 src/main/resources/application-dev.yml

# 准备 JWT RSA 密钥（放入 seckill-mall/src/main/resources/keys/）
# private.pem 和 public.pem

# 编译运行
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端默认启动在 `http://localhost:8080`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认启动在 `http://localhost:5173`

### 5. 访问系统

- **前台首页**：`http://localhost:5173`
- **后台管理**：`http://localhost:5173/admin`
- **API 文档**：`http://localhost:8080/doc.html`（Knife4j）

---

## 环境配置

### 后端配置

编辑 `seckill-mall/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill_mall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: seckill_app
    password: your_password
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20

  data:
    redis:
      host: localhost
      port: 6379
      # password: your_redis_password  # Redis 无密码时注释

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    publisher-confirm-type: correlated
    publisher-returns: true

jwt:
  rsa:
    private-key-path: classpath:keys/private.pem
    public-key-path: classpath:keys/public.pem
  access-token-expiration: 1800000      # 30 分钟
  refresh-token-expiration: 604800000   # 7 天

seckill:
  pay-timeout-minutes: 15               # 支付超时时间
  rate-limit-per-user: 1                # 每用户秒杀限流
  rate-limit-per-ip: 30                 # 每 IP 限流
  security:
    sign-secret: your-sign-secret-at-least-32-chars
    replay-window-seconds: 60           # 防重放窗口
```

### 前端配置

编辑 `frontend/.env.development`：

```env
# 后端 API 基础路径（开发环境直连后端）
VITE_API_BASE_URL=http://localhost:8080
```

生产环境（`frontend/.env.production`）：

```env
# 生产环境走 Nginx 反向代理相对路径
VITE_API_BASE_URL=/api
```

---

## 部署指南

### Docker Compose 一键部署（推荐）

本项目提供完整的 Docker Compose 编排，包含 5 个服务，一键启动全栈环境。

#### 部署步骤

**步骤 1：复制环境变量模板并修改配置**

```bash
cp .env.example .env
```

编辑 `.env`，将所有 `change_me_*` 占位符替换为强随机值：

```env
# MySQL
MYSQL_ROOT_PASSWORD=your_strong_root_password   # >= 16 位强随机
MYSQL_DATABASE=seckill_mall
MYSQL_USER=seckill
MYSQL_PASSWORD=your_strong_mysql_password
DB_PORT=3306

# Redis
REDIS_PASSWORD=your_strong_redis_password
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_USERNAME=seckill
RABBITMQ_PASSWORD=your_strong_rabbitmq_password
RABBITMQ_PORT=5672
RABBITMQ_MGMT_PORT=15672

# 应用
SECKILL_SIGN_SECRET=your_sign_secret_at_least_32_chars_long
CORS_ALLOWED_ORIGINS=https://yourdomain.com
TRUSTED_PROXY_IPS=172.18.0.0/16    # Docker 网段，信任 Nginx 代理
JWT_KEYS_DIR=./keys

# 前端
FRONTEND_PORT=80
```

**步骤 2：准备 JWT RSA 密钥**

```bash
mkdir -p ./keys
# 生成 RSA 私钥
openssl genrsa -out ./keys/private.pem 2048
# 生成 RSA 公钥
openssl rsa -in ./keys/private.pem -pubout -out ./keys/public.pem
# 设置权限
chmod 600 ./keys/private.pem
chmod 644 ./keys/public.pem
```

> ⚠️ **安全提示**：`./keys/` 目录已被 `.gitignore` 忽略（`*.pem`），私钥不会入库。生产环境建议通过 K8s Secret 或 Vault 挂载。

**步骤 3：启动所有服务**

```bash
# 构建并启动（首次会构建后端和前端镜像，耗时较长）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f app        # 后端日志
docker-compose logs -f frontend   # 前端日志
```

**步骤 4：访问系统**

- **前端首页**：`http://localhost`（端口 80，对外暴露）
- **后台管理**：`http://localhost/admin`
- **API 文档**：`http://localhost/api/doc.html`（经 Nginx 代理）
- **RabbitMQ 管理台**：`http://localhost:15672`（仅本机可访问）

#### 服务架构说明

```
浏览器
   │
   ▼
Nginx (frontend 容器, 端口 80, 对外)
   │
   ├── /            → Vue SPA 静态资源
   ├── /api/        → 反向代理 → app:8080/     (后端 API)
   ├── /images/     → 反向代理 → app:8080/images/ (商品图片)
   └── /upload/     → 反向代理 → app:8080/upload/ (上传文件)
                          │
                          ▼
                    Spring Boot (app 容器, 端口 8080, 仅内部网络)
                          │
                          ├── MySQL (mysql 容器, 端口 3306, 绑回环)
                          ├── Redis (redis 容器, 端口 6379, 绑回环)
                          └── RabbitMQ (rabbitmq 容器, 端口 5672, 绑回环)
```

#### 端口说明

| 服务 | 容器端口 | 宿听地址 | 说明 |
|------|----------|----------|------|
| frontend | 80 | `0.0.0.0` | **对外暴露**，用户浏览器访问入口 |
| app | 8080 | `127.0.0.1` | 仅本机可访问，前端经 Nginx 内部网络代理 |
| mysql | 3306 | `127.0.0.1` | 仅本机可访问，调试用 |
| redis | 6379 | `127.0.0.1` | 仅本机可访问，调试用 |
| rabbitmq | 5672 / 15672 | `127.0.0.1` | 仅本机可访问，调试用 |

> 🔒 **安全设计**：除前端 80 端口对外，所有中间件端口均绑定回环地址（`127.0.0.1`），不发布到 `0.0.0.0`，避免公网暴露。生产环境可删除中间件 ports 配置，仅靠内部 Docker 网络通信。

#### 5 个服务说明

| 服务 | 镜像 | 说明 |
|------|------|------|
| **mysql** | `mysql:8.0` | 数据库，自动执行 `schema.sql` + `data.sql` 初始化 |
| **redis** | `redis:7-alpine` | 缓存，启用 `protected-mode` + `requirepass` + AOF 持久化 |
| **rabbitmq** | `rabbitmq:3.13-management` | 消息队列，含管理控制台 |
| **app** | 自构建（多阶段 Maven + JRE） | Spring Boot 后端，非 root 运行，RSA 私钥运行时挂载 |
| **frontend** | 自构建（Node + Nginx） | Vue SPA + Nginx 反向代理，非 root 运行 |

#### 常用运维命令

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（⚠️ 清空所有数据）
docker-compose down -v

# 重新构建镜像
docker-compose build

# 仅重建后端
docker-compose up -d --build app

# 查看资源占用
docker stats
```

### Nginx 配置示例（手动部署参考）

如不使用 Docker Compose 部署前端，可参考以下 Nginx 配置手动部署：

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # 隐藏 Nginx 版本号
    server_tokens off;

    root /path/to/frontend/dist;
    index index.html;

    # 支持商品图片上传
    client_max_body_size 20m;

    # gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理 API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 反向代理商品图片
    location /images/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }

    # 静态资源长缓存
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## API 概览

> 完整 API 文档请访问 Knife4j：`http://localhost:8080/doc.html`

### 认证模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/login` | POST | 用户登录（账号/邮箱/手机号） |
| `/api/v1/auth/register` | POST | 用户注册 |
| `/api/v1/auth/refresh` | POST | 刷新令牌 |
| `/api/v1/auth/forgot-password` | POST | 找回密码 |
| `/api/v1/auth/logout` | POST | 退出登录 |

### 商品模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/products` | GET | 商品列表（分类/价格/排序/分页） |
| `/api/v1/products/{id}` | GET | 商品详情 |
| `/api/v1/products/{id}/skus` | GET | 商品 SKU 列表 |
| `/api/v1/products/{id}/reviews` | GET | 商品评价 |
| `/api/v1/banners` | GET | 轮播图列表 |
| `/api/v1/categories` | GET | 分类树 |

### 秒杀模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/seckill/active` | GET | 进行中秒杀活动 |
| `/api/v1/seckill/{id}/execute` | POST | 执行秒杀（签名 + 防重放） |
| `/api/v1/seckill/{id}/result` | GET | 查询秒杀结果 |

### 订单模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/orders` | GET/POST | 订单列表/创建订单 |
| `/api/v1/orders/{id}` | GET | 订单详情 |
| `/api/v1/orders/{id}/pay` | POST | 支付订单 |
| `/api/v1/orders/{id}/cancel` | POST | 取消订单 |
| `/api/v1/orders/{id}/confirm` | POST | 确认收货 |
| `/api/v1/orders/unified` | GET | 统一订单列表（含秒杀+普通） |

### 购物车模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/cart` | GET | 购物车列表 |
| `/api/v1/cart` | POST | 加入购物车 |
| `/api/v1/cart/{id}` | PUT/DELETE | 修改/删除购物车项 |

### 用户模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/user/profile` | GET/PUT | 个人资料 |
| `/api/v1/user/password` | PUT | 修改密码 |
| `/api/v1/user/addresses` | GET/POST | 收货地址管理 |
| `/api/v1/user/favorites` | GET/POST | 收藏夹 |
| `/api/v1/user/coupons` | GET | 我的优惠券 |
| `/api/v1/user/wallet` | GET | 钱包余额 |

### 后台管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/admin/products` | CRUD | 商品管理 |
| `/api/v1/admin/orders` | GET | 后台订单列表 |
| `/api/v1/admin/users` | GET/PUT | 用户管理 |
| `/api/v1/admin/seckill` | CRUD | 秒杀活动管理 |
| `/api/v1/admin/coupons` | CRUD | 优惠券管理 |
| `/api/v1/admin/reviews` | GET/PUT | 评价管理 |
| `/api/v1/stats/overview` | GET | 仪表盘总览 |
| `/api/v1/stats/order-trend` | GET | 订单趋势 |
| `/api/v1/admin/logs` | GET | 操作日志 |

---

## 安全特性

| 特性 | 实现方式 | 说明 |
|------|----------|------|
| **JWT + RSA** | JJWT 0.12.3 + RSA 非对称加密 | 私钥签名、公钥验签；私钥运行时挂载，不进镜像不入库 |
| **Spring Security** | 基于角色的访问控制 | 角色：BUYER / ADMIN / SELLER，接口级权限拦截 |
| **防重放攻击** | 签名 + 时间戳 + Redis Set 去重 | 秒杀请求 60 秒窗口内不可重复 |
| **接口限流** | Redis Lua 令牌桶 + AOP 切面 | 用户级 + IP 级双重限流，防刷防爬 |
| **XSS 防护** | DOMPurify（前端）+ Jsoup（后端） | 双重 HTML 清洗，过滤恶意脚本 |
| **SQL 注入防护** | MyBatis-Plus 参数化查询 | 防全表更新/删除拦截器，禁止拼接 SQL |
| **CORS 白名单** | 生产环境严格限制允许来源 | `CORS_ALLOWED_ORIGINS` 必填，缺失即报错 |
| **密码加密** | BCrypt 哈希存储 | 不存明文，自带盐值 |
| **敏感信息脱敏** | 日志 PII 脱敏 + AOP 切面 | 手机号/邮箱/地址日志脱敏，密钥不入库 |
| **Docker 安全** | 中间件端口绑回环 + 非 root 运行 | MySQL/Redis/RabbitMQ 仅本机可访问，容器非 root 用户 |
| **可信代理白名单** | `TRUSTED_PROXY_IPS` 配置 | 仅信任指定代理的 `X-Forwarded-*` 头，防伪造 |
| **口令必填** | `${VAR:?required}` 语法 | Docker Compose 所有口令缺失即报错，无默认值 |
| **Actuator 收敛** | 端点白名单 + 详情授权 | 禁用 env/beans/threaddump 等敏感端点 |

---

## 数据库设计

### 表清单（23 张表 ↔ 23 个实体类）

| 序号 | 表名 | 说明 | 所属模块 |
|------|------|------|----------|
| 1 | `t_user` | 用户表 | 用户 |
| 2 | `t_category` | 商品分类表 | 分类 |
| 3 | `t_category_attribute` | 分类属性模板表 | SKU |
| 4 | `t_category_attribute_value` | 分类属性预设值表 | SKU |
| 5 | `t_product` | 商品表 | 商品 |
| 6 | `t_product_attribute` | 商品属性表 | SKU |
| 7 | `t_product_attribute_value` | 商品属性值表 | SKU |
| 8 | `t_product_sku` | 商品 SKU 表 | SKU |
| 9 | `t_seckill_activity` | 秒杀活动场次表 | 秒杀 |
| 10 | `t_seckill_goods` | 秒杀商品表 | 秒杀 |
| 11 | `t_seckill_order` | 秒杀订单表 | 秒杀 |
| 12 | `t_user_address` | 用户收货地址表 | 用户 |
| 13 | `t_normal_order` | 普通订单表 | 普通订单 |
| 14 | `t_normal_order_item` | 普通订单明细表 | 普通订单 |
| 15 | `t_cart` | 购物车表 | 购物车 |
| 16 | `t_user_favorite` | 用户收藏夹表 | 收藏 |
| 17 | `t_product_review` | 商品评论表 | 评论 |
| 18 | `t_coupon` | 优惠券表 | 优惠券 |
| 19 | `t_user_coupon` | 用户优惠券表 | 优惠券 |
| 20 | `t_recharge_card` | 充值卡表 | 钱包 |
| 21 | `t_banner` | 轮播图表 | 运营 |
| 22 | `t_login_log` | 登录日志表 | 日志 |
| 23 | `t_operation_log` | 操作日志表 | 日志 |

> 实体类位于 `seckill-mall/src/main/java/com/seckill/mall/entity/`，通过 MyBatis-Plus `@TableName` 注解与表一一对应。

### 初始化数据统计（190 条）

| 数据类型 | 条数 | 说明 |
|----------|------|------|
| 用户 | 2 | admin / buyer01（BCrypt 加密） |
| 商品分类 | 68 | 10 一级 + 58 二级分类树 |
| 商品 | 116 | 含秒杀商品与普通商品 |
| 秒杀活动 | 1 | 秒杀活动场次 |
| 轮播图 | 2 | 首页 Banner |
| 充值卡 | 1 | 示例充值卡 |
| **合计** | **190** | — |

> 所有 PII 已脱敏：邮箱使用 `@example.com`，手机号使用虚构号码，密码 BCrypt 哈希存储。

### SQL 文件说明

项目维护两个 SQL 目录，用途不同：

| 目录 | 用途 | 是否含环境护栏 | 执行方式 |
|------|------|----------------|----------|
| `sql/` | 手动执行、运维参考、版本归档 | 是（`DELIMITER`/存储过程） | 人工通过 MySQL CLI 执行 |
| `seckill-mall/src/main/resources/sql/` | Docker MySQL 容器自动初始化 | 否（无护栏） | `docker-compose.yml` 挂载到 `/docker-entrypoint-initdb.d/` 自动执行 |

**`sql/` 目录文件：**

| 文件 | 用途 |
|------|------|
| `01_schema_full.sql` | 完整建表脚本（23 张表，含环境护栏，需 `SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true'`） |
| `02_init_data_full.sql` | 完整初始化数据（190 条，PII 已脱敏） |
| `03_migration_full.sql` | 幂等迁移脚本（整合 V2~V8 所有增量变更，可重复执行） |
| `README.md` | SQL 详细说明文档 |

> ⚠️ `sql/03_migration.sql`（含真实 PII 的旧迁移脚本）已被 `.gitignore` 忽略，需单独审查后才能入库。

---

## 开发指南

### 后端开发

```bash
cd seckill-mall

# 编译
mvn clean compile

# 运行测试（含 Testcontainers 集成测试）
mvn test

# 打包（跳过测试）
mvn clean package -DskipTests

# 运行（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生成测试覆盖率报告（target/site/jacoco/index.html）
mvn test
```

### 前端开发

```bash
cd frontend

# 安装依赖
npm install

# 开发模式（热更新）
npm run dev

# 类型检查（vue-tsc）
npm run type-check

# 生产构建（含类型检查）
npm run build

# 仅构建（跳过类型检查，用于 Docker 镜像构建）
npm run build:only

# 预览构建产物
npm run preview
```

### 代码规范

- **后端**：遵循阿里巴巴 Java 开发手册
- **前端**：遵循 Vue 3 Composition API + `<script setup>` 风格
- **TypeScript**：严格模式，禁止 `any` 类型
- **CSS**：使用项目统一变量（`--color-primary` 等）
- **命名**：实体类驼峰 → 表名下划线，统一加 `t_` 前缀

### 默认账号

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 后台管理 + 前台购物 |
| 买家 | buyer01 | buyer123 | 前台购物 |

> ⚠️ **首次部署请务必修改默认密码**。初始化数据中密码已 BCrypt 加密存储。

---

## 项目截图

### 前台页面

- **首页**：Banner 轮播 + 秒杀专区入口 + 猜你喜欢（6 列无限滚动）
- **商品详情**：左右分栏（图片 + 信息），Tab 详情区左右分栏（内容 + 侧边信息卡）
- **秒杀专区**：场次切换 + 倒计时 + 6 列网格展示
- **购物车**：左右分栏（商品列表 + 结算面板）
- **订单中心**：两列布局（状态导航 + 订单卡片）
- **收藏夹**：两列布局（管理面板 + 6 列网格）
- **个人中心**：侧边栏导航 + 资料编辑表单

### 后台页面

- **仪表盘**：7 项核心指标 + 3 图表（订单趋势/用户增长/状态分布）+ 秒杀排行榜 + 最近订单
- **商品管理**：列表 + 编辑弹窗 + SKU 配置
- **秒杀管理**：活动创建 + 场次管理 + 库存设置
- **订单管理**：高级筛选 + 发货操作 + 详情查看
- **操作日志**：日志查询 + Excel 导出

---

## 版本说明

**当前版本：v1.0.0**

### 已完成功能

- ✅ 完整电商前台（16 个页面）：首页、商品、购物车、秒杀、订单、收藏、个人中心
- ✅ 完整后台管理（14 个页面）：仪表盘、商品、订单、秒杀、用户、分类、优惠券、日志
- ✅ 秒杀高并发核心链路：Redis Lua 原子扣减 + RabbitMQ 异步下单 + 防重放
- ✅ 完善的安全防护体系：JWT+RSA、Spring Security、限流、XSS/SQL 注入防护、CORS、脱敏
- ✅ Docker Compose 一键部署（5 服务，含前端 Nginx 反向代理）
- ✅ 23 张表完整数据库设计 + 190 条初始化数据
- ✅ 多轮 Bug 修复（安全/并发/契约/性能/UI）
- ✅ 多轮 UI 重构（首页/商品详情/购物车/秒杀/订单/收藏夹/个人中心）
- ✅ 测试覆盖率统计（JaCoCo）+ Testcontainers 集成测试
- ✅ API 文档（Knife4j）+ 指标监控（Prometheus）

---

## 开发者信息

| 信息 | 内容 |
|------|------|
| 开发者 | WNJ |
| 邮箱 | nj651217@163.com |
| 仓库 | https://gitee.com/nengjie116/seckill-mall |

---

## License

本项目仅供学习交流使用，不得用于商业用途。
