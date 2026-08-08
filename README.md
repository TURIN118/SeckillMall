# 秒杀商城（Seckill Mall）

> 基于 Spring Boot + Vue3 的高并发秒杀电商平台，涵盖商品管理、购物车、订单、秒杀抢购、支付、用户中心、后台管理等全链路功能。

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
- [开发指南](#开发指南)

---

## 项目简介

本项目是一个完整的秒杀电商平台，模拟京东/淘宝等电商核心业务场景。系统采用前后端分离架构，后端基于 Spring Boot 3.x 构建高并发秒杀引擎，前端基于 Vue 3 + TypeScript 构建现代化 SPA 界面。

**核心亮点：**
- 秒杀抢购采用 Redis Lua 原子扣减 + RabbitMQ 异步下单，支持高并发场景
- JWT + Spring Security 双重认证，RSA 非对称加密签名
- 前端 UI 参考京东/淘宝设计风格，响应式布局适配多端
- Docker Compose 一键部署，开箱即用

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Java | 17 | 开发语言 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Spring Security | 6.x | 安全认证 |
| Redis + Redisson | 7 / 3.27.2 | 缓存 + 分布式锁 + 秒杀库存 |
| RabbitMQ | 3.13 | 异步消息队列（秒杀下单） |
| MySQL | 8.0 | 持久化数据库 |
| JWT (RSA) | — | 无状态令牌认证 |
| Knife4j | 4.5.0 | API 文档 |
| MapStruct | 1.5.5 | 对象映射 |
| Lombok | 1.18.30 | 代码简化 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.12 | 前端框架 |
| TypeScript | 5.6.x | 类型安全 |
| Element Plus | 2.8.4 | UI 组件库 |
| Pinia | 2.2.4 | 状态管理 |
| Vue Router | 4.4.5 | 路由管理 |
| Axios | 1.7.7 | HTTP 请求 |
| ECharts | 5.5.1 | 数据可视化（后台仪表盘） |
| Vite | 5.x | 构建工具 |
| Day.js | 1.11.13 | 日期处理 |
| DOMPurify | 3.4.x | XSS 防护 |

---

## 项目结构

```
seckill-mall/
├── seckill-mall/                 # 后端工程
│   ├── src/main/java/com/seckill/mall/
│   │   ├── controller/           # 接口控制器
│   │   ├── service/              # 业务逻辑层
│   │   │   └── impl/             # 业务实现
│   │   ├── entity/               # 数据实体
│   │   ├── dto/                  # 数据传输对象
│   │   ├── vo/                   # 视图对象
│   │   ├── mapper/               # MyBatis 映射接口
│   │   ├── config/               # 配置类
│   │   ├── security/             # 安全认证模块
│   │   ├── cache/                # Redis 缓存服务
│   │   ├── mq/                   # RabbitMQ 消息队列
│   │   │   ├── producer/         # 消息生产者
│   │   │   └── consumer/         # 消息消费者
│   │   ├── aspect/               # AOP 切面
│   │   ├── scheduler/            # 定时任务
│   │   └── common/               # 通用工具类
│   ├── src/main/resources/
│   │   ├── application.yml       # 主配置
│   │   ├── application-dev.yml   # 开发环境
│   │   ├── application-prod.yml  # 生产环境
│   │   ├── mapper/               # MyBatis XML 映射
│   │   └── lua/                  # Redis Lua 脚本
│   └── pom.xml                   # Maven 依赖
│
├── frontend/                     # 前端工程
│   ├── src/
│   │   ├── api/                  # API 请求封装
│   │   ├── views/                # 页面视图
│   │   │   ├── front/            # 前台页面
│   │   │   └── admin/            # 后台管理页面
│   │   ├── components/           # 公共组件
│   │   ├── layouts/              # 布局组件
│   │   ├── stores/               # Pinia 状态仓库
│   │   ├── router/               # 路由配置
│   │   ├── styles/               # 全局样式
│   │   ├── types/                # TypeScript 类型定义
│   │   └── utils/                # 工具函数
│   ├── vite.config.ts            # Vite 配置
│   └── package.json              # npm 依赖
│
├── sql/                          # 数据库脚本
├── docker-compose.yml            # Docker 编排
└── README.md                     # 项目文档
```

---

## 核心功能

### 前台功能

| 模块 | 功能说明 |
|------|----------|
| 首页 | Banner 轮播、秒杀专区、商品分类、猜你喜欢（无限滚动+分类筛选） |
| 商品列表 | 分类筛选、价格筛选、排序、分页 |
| 商品详情 | 图片轮播、SKU 规格选择、参数速览、服务保障、评价、售后说明 |
| 购物车 | 左右分栏布局、批量操作、分页、选中结算 |
| 秒杀专区 | 场次切换、倒计时、库存优先排序、6 列网格展示 |
| 订单中心 | 状态筛选、订单类型筛选、两列布局、订单删除、详情查看 |
| 订单详情 | 左右分栏、收货地址、商品列表、支付、确认收货 |
| 收藏夹 | 两列布局、6 列网格、排序、管理模式、批量操作 |
| 个人中心 | 侧边栏导航、资料修改、密码修改（验证码）、钱包余额 |
| 结算页 | 地址选择、支付方式、备注、订单确认 |

### 后台功能

| 模块 | 功能说明 |
|------|----------|
| 仪表盘 | 7 项核心指标、订单/用户趋势图、状态分布饼图、秒杀排行榜、最近订单 |
| 商品管理 | 商品 CRUD、SKU 规格管理、图片上传、上下架 |
| 订单管理 | 高级筛选、分页排序、发货、详情查看 |
| 秒杀管理 | 创建秒杀活动、场次管理、库存设置 |
| 用户管理 | 用户列表、状态管理 |
| 分类管理 | 分类树、属性管理 |
| Banner 管理 | 轮播图配置 |
| 优惠券管理 | 优惠券创建与发放 |
| 充值卡管理 | 充值卡生成与管理 |
| 评价管理 | 评价审核与回复 |
| 系统健康 | 服务状态监控 |
| 操作日志 | 操作记录查询 |

### 秒杀核心流程

```
用户请求 → 秒杀签名校验 → 防重放检测 → Redis Lua 原子扣减库存
    → RabbitMQ 异步发送下单消息 → 消费者创建订单 → 返回结果
```

**关键技术点：**
- Redis Lua 脚本保证库存扣减原子性，防止超卖
- RabbitMQ 异步削峰，消息确认机制保证可靠性
- 秒杀防重放：签名 + 时间戳 + Redis Set 去重
- 库存预预热：活动开始前将库存加载到 Redis
- 超时未支付自动取消：定时任务扫描 + 订单超时兜底检查

---

## 快速开始

### 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7+ |
| RabbitMQ | 3.13+ |
| Maven | 3.8+ |

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

# 导入数据
use seckill_mall;
source sql/01_schema.sql;
source sql/data.sql;
```

### 3. 启动后端

```bash
# 修改配置（数据库/Redis/RabbitMQ 连接信息）
cd seckill-mall
# 编辑 src/main/resources/application-dev.yml

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

- 前台首页：`http://localhost:5173`
- 后台管理：`http://localhost:5173/admin`
- API 文档：`http://localhost:8080/doc.html`（Knife4j）

---

## 环境配置

### 后端配置

编辑 `seckill-mall/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill_mall?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
  rabbitmq:
    host: localhost
    port: 5672
    username: your_mq_username
    password: your_mq_password
```

### 前端配置

编辑 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## 部署指南

### Docker Compose 一键部署

1. 创建环境变量文件 `.env`：

```env
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=seckill_mall
MYSQL_USER=seckill
MYSQL_PASSWORD=your_password
REDIS_PASSWORD=your_redis_password
RABBITMQ_USERNAME=seckill
RABBITMQ_PASSWORD=your_mq_password
SECKILL_SIGN_SECRET=your_sign_secret
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

2. 启动服务：

```bash
docker-compose up -d
```

3. 构建前端并部署：

```bash
cd frontend
npm install
npm run build
# 将 dist/ 目录部署到 Nginx 或其他静态服务器
```

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## API 概览

### 认证模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/login` | POST | 用户登录 |
| `/api/v1/auth/register` | POST | 用户注册 |
| `/api/v1/auth/refresh` | POST | 刷新令牌 |
| `/api/v1/auth/forgot-password` | POST | 找回密码 |

### 商品模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/products` | GET | 商品列表 |
| `/api/v1/products/{id}` | GET | 商品详情 |
| `/api/v1/products/{id}/skus` | GET | 商品 SKU |
| `/api/v1/products/{id}/reviews` | GET | 商品评价 |

### 秒杀模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/seckill/active` | GET | 进行中秒杀活动 |
| `/api/v1/seckill/{id}/execute` | POST | 执行秒杀 |
| `/api/v1/seckill/{id}/result` | GET | 查询秒杀结果 |

### 订单模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/orders` | GET/POST | 订单列表/创建订单 |
| `/api/v1/orders/{id}` | GET | 订单详情 |
| `/api/v1/orders/{id}/pay` | POST | 支付订单 |
| `/api/v1/orders/{id}/cancel` | POST | 取消订单 |
| `/api/v1/orders/unified` | GET | 统一订单列表 |

### 购物车模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/cart` | GET | 购物车列表 |
| `/api/v1/cart` | POST | 加入购物车 |
| `/api/v1/cart/{id}` | DELETE | 删除购物车项 |

### 后台管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/admin/orders` | GET | 后台订单列表 |
| `/api/v1/admin/users` | GET | 用户管理 |
| `/api/v1/stats/overview` | GET | 仪表盘总览 |
| `/api/v1/stats/order-trend` | GET | 订单趋势 |

> 完整 API 文档请访问 Knife4j：`http://localhost:8080/doc.html`

---

## 安全特性

| 特性 | 说明 |
|------|------|
| JWT + RSA | 非对称加密签名令牌，私钥运行时挂载不进镜像 |
| Spring Security | 基于角色的访问控制（BUYER/ADMIN/SELLER） |
| 防重放攻击 | 秒杀请求签名 + 时间戳 + Redis Set 去重 |
| 接口限流 | Redis Lua 令牌桶限流，防刷防爬 |
| XSS 防护 | DOMPurify 清洗前端输入，后端 XSS 过滤 |
| SQL 注入防护 | MyBatis-Plus 参数化查询，防全表更新拦截器 |
| CORS 白名单 | 生产环境严格限制允许来源 |
| 密码加密 | BCrypt 加密存储 |
| 敏感信息脱敏 | 日志 PII 脱敏，密钥不入库 |
| Docker 安全 | 中间件端口绑回环，非 root 运行 |

---

## 开发指南

### 后端开发

```bash
# 编译
cd seckill-mall
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests
```

### 前端开发

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 类型检查
npm run type-check

# 生产构建
npm run build
```

### 代码规范

- 后端遵循阿里巴巴 Java 开发手册
- 前端遵循 Vue 3 Composition API + `<script setup>` 风格
- TypeScript 严格模式，禁止 `any` 类型
- CSS 使用项目统一变量（`--color-primary` 等）

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 买家 | buyer | buyer123 |

> 首次部署请务必修改默认密码。

---

## 项目截图

### 前台页面

- **首页**：Banner 轮播 + 秒杀专区 + 猜你喜欢（6 列无限滚动）
- **商品详情**：左右分栏（图片+信息），Tab 详情区左右分栏（内容+侧边信息卡）
- **秒杀专区**：场次切换 + 倒计时 + 6 列网格
- **购物车**：左右分栏（商品列表+结算面板）
- **订单中心**：两列布局（状态导航+订单卡片）
- **收藏夹**：两列布局（管理面板+6 列网格）

### 后台页面

- **仪表盘**：7 项指标 + 3 图表 + 排行榜 + 最近订单
- **商品管理**：列表 + 编辑弹窗
- **秒杀管理**：活动创建 + 场次管理

---

## 版本说明

**当前版本：v1.0.0**

已完成：
- 79 项 Bug 修复（安全/并发/契约/性能/UI）
- 多轮 UI 重构（首页/商品详情/购物车/秒杀/订单/收藏夹/个人中心）
- 秒杀高并发核心链路（Redis Lua + RabbitMQ 异步）
- 完善的安全防护体系

---

## 开发者

| 信息 | 内容 |
|------|------|
| 开发者 | WNJ |
| 邮箱 | nj651217@163.com |
| 仓库 | https://gitee.com/nengjie116/seckill-mall |

---

## License

本项目仅供学习交流使用，不得用于商业用途。