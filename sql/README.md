# 秒杀商城数据库脚本说明

本目录包含秒杀商城项目的完整数据库初始化与迁移脚本，由散落在项目各处的 SQL 文件整合而成。

项目同时维护两个 SQL 目录，用途不同，需配合使用：

| 目录 | 用途 | 是否含环境护栏 | 执行方式 |
|------|------|----------------|----------|
| `sql/`（本目录） | 手动执行、运维参考、版本归档 | 是（`DELIMITER`/存储过程） | 人工通过 MySQL CLI 执行 |
| `seckill-mall/src/main/resources/sql/` | Docker MySQL 容器自动初始化 | 否（无护栏，避免容器初始化不兼容） | `docker-compose.yml` 挂载到 `/docker-entrypoint-initdb.d/` 自动执行 |

## 文件清单

### `sql/` 目录（手动执行，含环境护栏）

| 文件 | 用途 | 说明 |
|------|------|------|
| `01_schema_full.sql` | 完整建表脚本 | 包含全部 **23 张表**的最终态定义，按依赖顺序排列，含环境护栏（`DELIMITER`/存储过程） |
| `02_init_data_full.sql` | 完整初始化数据 | 包含 **190 条**种子数据（用户/分类/商品/秒杀活动/轮播图/充值卡），PII 已脱敏 |
| `03_migration_full.sql` | 完整幂等迁移脚本 | 整合 V2~V8 所有增量变更，可重复执行，用于已有环境升级到最终态 |
| `README.md` | 本说明文档 | — |

### `seckill-mall/src/main/resources/sql/` 目录（Docker 自动初始化，无护栏）

| 文件 | 用途 | 说明 |
|------|------|------|
| `schema.sql` | 完整建表脚本 | 与 `sql/01_schema_full.sql` 表结构完全一致，仅去掉了环境护栏存储过程，适配 Docker `/docker-entrypoint-initdb.d/` 自动初始化 |
| `data.sql` | 完整初始化数据 | 与 `sql/02_init_data_full.sql` 内容完全一致 |

> 已删除 `migration_v7_sku.sql`（ALTER TABLE 修改语句，不符合"一次性创建完整语句"要求；其变更已合并至 `schema.sql` 与 `01_schema_full.sql` 的最终态表结构中）。

## 环境要求

- **MySQL 8.0+**（需要 `JSON` 类型、`ENUM` 类型、`GENERATED` 列等特性）
- 字符集：`utf8mb4`，排序规则：`utf8mb4_general_ci`
- 存储引擎：`InnoDB`
- 客户端工具：MySQL CLI / Navicat / DataGrip 等（需支持 `DELIMITER` 语法，仅 `sql/` 目录脚本需要）

## 执行顺序

### 全新环境初始化（手动，使用 `sql/` 目录）

```bash
# 1. 建表（需设置环境变量以通过护栏检查）
mysql -u root -p -e "SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true'; SOURCE sql/01_schema_full.sql;"

# 2. 初始化数据
mysql -u root -p seckill_mall < sql/02_init_data_full.sql

# 迁移脚本无需执行（01 已含最终态表结构）
```

或通过 MySQL CLI 交互模式：

```sql
-- 1. 建表
SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true';
SOURCE sql/01_schema_full.sql;

-- 2. 初始化数据
SOURCE sql/02_init_data_full.sql;
```

### 全新环境初始化（Docker，使用 `resources/sql/` 目录，自动）

`docker-compose.yml` 已将 `seckill-mall/src/main/resources/sql/` 挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d/`，容器首次启动时按文件名字母序自动执行 `schema.sql` → `data.sql`，无需人工干预，也无需设置护栏变量。

### 已有环境增量迁移

```sql
-- 仅执行迁移脚本即可（幂等，可重复执行）
SOURCE sql/03_migration_full.sql;
```

## 数据库表清单（23 张）

按依赖顺序排列：

| 序号 | 表名 | 说明 | 所属模块 |
|------|------|------|----------|
| 1 | `t_user` | 用户表 | 用户 |
| 2 | `t_category` | 商品分类表 | 分类 |
| 3 | `t_category_attribute` | 分类属性模板表 | SKU(V7) |
| 4 | `t_category_attribute_value` | 分类属性预设值表 | SKU(V7) |
| 5 | `t_product` | 商品表 | 商品 |
| 6 | `t_product_attribute` | 商品属性表 | SKU(V7) |
| 7 | `t_product_attribute_value` | 商品属性值表 | SKU(V7) |
| 8 | `t_product_sku` | 商品SKU表 | SKU(V7) |
| 9 | `t_seckill_activity` | 秒杀活动场次表 | 秒杀(场次化) |
| 10 | `t_seckill_goods` | 秒杀商品表 | 秒杀 |
| 11 | `t_seckill_order` | 秒杀订单表 | 秒杀 |
| 12 | `t_user_address` | 用户收货地址表 | 用户 |
| 13 | `t_normal_order` | 普通订单表 | 普通订单 |
| 14 | `t_normal_order_item` | 普通订单明细表 | 普通订单 |
| 15 | `t_cart` | 购物车表 | 购物车(V3) |
| 16 | `t_user_favorite` | 用户收藏夹表 | 收藏(V3) |
| 17 | `t_product_review` | 商品评论表 | 评论(V2) |
| 18 | `t_coupon` | 优惠券表 | 优惠券(V4/V8) |
| 19 | `t_user_coupon` | 用户优惠券表 | 优惠券(V4) |
| 20 | `t_recharge_card` | 充值卡表 | 钱包(V4) |
| 21 | `t_banner` | 轮播图表 | 运营(V2) |
| 22 | `t_login_log` | 登录日志表 | 日志 |
| 23 | `t_operation_log` | 操作日志表 | 日志 |

## 表与实体类对应关系（23 张表 ↔ 23 个实体类）

项目在 `seckill-mall/src/main/java/com/seckill/mall/entity/` 下有 **23 个实体类**（不含 `package-info.java`），通过 MyBatis-Plus `@TableName` 注解与上述 23 张表一一对应：

| 实体类 | 表名 | 实体类 | 表名 |
|--------|------|--------|------|
| `User` | `t_user` | `NormalOrder` | `t_normal_order` |
| `Category` | `t_category` | `NormalOrderItem` | `t_normal_order_item` |
| `CategoryAttribute` | `t_category_attribute` | `Cart` | `t_cart` |
| `CategoryAttributeValue` | `t_category_attribute_value` | `UserFavorite` | `t_user_favorite` |
| `Product` | `t_product` | `ProductReview` | `t_product_review` |
| `ProductAttribute` | `t_product_attribute` | `Coupon` | `t_coupon` |
| `ProductAttributeValue` | `t_product_attribute_value` | `UserCoupon` | `t_user_coupon` |
| `ProductSku` | `t_product_sku` | `RechargeCard` | `t_recharge_card` |
| `SeckillActivity` | `t_seckill_activity` | `Banner` | `t_banner` |
| `SeckillGoods` | `t_seckill_goods` | `LoginLog` | `t_login_log` |
| `SeckillOrder` | `t_seckill_order` | `OperationLog` | `t_operation_log` |
| `UserAddress` | `t_user_address` | — | — |

> 命名规则：实体类驼峰 → 表名下划线，统一加 `t_` 前缀。SQL 表结构与实体类字段保持一致（含 V8 新增的 `t_normal_order.user_coupon_id/discount_amount`、`t_coupon.scope_type/category_id/product_id` 等字段）。

## 初始化数据统计（190 条）

| 数据类型 | 条数 | 说明 |
|----------|------|------|
| 用户 | 2 | admin / buyer01（密码已 BCrypt 加密） |
| 商品分类 | 68 | 10 一级 + 58 二级分类树 |
| 商品 | 116 | 含秒杀商品与普通商品 |
| 秒杀活动 | 1 | 秒杀活动场次（关联秒杀商品） |
| 轮播图 | 2 | 首页 Banner |
| 充值卡 | 1 | 示例充值卡 |
| **合计** | **190** | — |

### PII 脱敏说明

所有个人身份信息（PII）已替换为虚构示例数据：

- **邮箱**：使用 `@example.com` 域名（如 `admin@example.com`）
- **手机号**：使用虚构号码（如 `13800000000`）
- **地址**：使用虚构地址（如"示例地址"）
- **密码**：使用 BCrypt 哈希存储，不含明文口令

## 迁移版本历史

| 版本 | 变更内容 | 影响表 |
|------|----------|--------|
| V2 | 新增轮播图、评论功能 | `t_banner`, `t_product_review` |
| V3 | 新增购物车、收藏功能 | `t_cart`, `t_user_favorite`, `t_product` |
| V4 | 新增钱包、优惠券、充值卡 | `t_user`, `t_coupon`, `t_user_coupon`, `t_recharge_card` |
| normal_order | 新增普通订单 | `t_normal_order`, `t_normal_order_item` |
| V6_shipping | 订单加物流字段 | `t_seckill_order`, `t_normal_order` |
| seckill_add_columns | 秒杀商品加扩展字段 | `t_seckill_goods` |
| V7_sku | SKU 系统 | `t_category_attribute`, `t_category_attribute_value`, `t_product_attribute`, `t_product_attribute_value`, `t_product_sku`, `t_cart`, `t_normal_order_item`, `t_product_review`, `t_product` |
| seckill_activity | 秒杀场次化 | `t_seckill_activity`, `t_seckill_goods` |
| V8 | 订单优惠券关联 + 优惠券适用范围 | `t_normal_order`(user_coupon_id/discount_amount), `t_coupon`(scope_type/category_id/product_id) |
| M-K6 | 补充复合索引 | `t_product_review`, `t_user_coupon` |
| M-S5 | 登录日志 user_id 允许 NULL | `t_login_log` |

## 环境护栏说明

`sql/01_schema_full.sql` 开头包含环境护栏存储过程，防止误删已有数据库：

```sql
-- 需要先设置此变量才能执行建表脚本
SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true';
SOURCE sql/01_schema_full.sql;
```

若未设置该变量，脚本会通过 `SIGNAL SQLSTATE '45000'` 抛出错误并中止执行。

> **注意**：`seckill-mall/src/main/resources/sql/schema.sql` 不含此护栏（Docker 容器初始化时无法预先设置会话变量），因此仅适合在全新环境由容器首次启动时执行，禁止手动连到已有库执行。

## 幂等性说明

`03_migration_full.sql` 中的所有变更均通过存储过程实现幂等保护：

- `add_column_if_not_exists` — 列不存在时才 ADD COLUMN
- `add_index_if_not_exists` — 索引不存在时才 ADD INDEX
- `add_unique_if_not_exists` — 唯一索引不存在时才 ADD UNIQUE KEY
- `drop_index_if_exists` — 索引存在时才 DROP INDEX
- `CREATE TABLE IF NOT EXISTS` — 表不存在时才建表

因此该脚本可安全重复执行，不会因已存在的对象而报错。

## 注意事项

1. **两个目录的表结构必须保持一致**：`sql/01_schema_full.sql`（有护栏）与 `seckill-mall/src/main/resources/sql/schema.sql`（无护栏）的 CREATE TABLE 部分应完全一致；`sql/02_init_data_full.sql` 与 `seckill-mall/src/main/resources/sql/data.sql` 应完全一致。修改时需同步更新。
2. **一次性创建完整语句**：`schema.sql` 与 `01_schema_full.sql` 均为最终态完整建表脚本，不包含任何 `ALTER TABLE` 修改语句；所有迁移变更（V2~V8）已合并到表定义中。
3. **新环境只需执行 01 + 02**：`01_schema_full.sql` 已包含所有最终态表结构，无需再执行迁移脚本。
4. **已有环境只需执行 03**：迁移脚本会将旧版表结构升级到最终态。
5. **M-S5 修复需手动执行**：`t_login_log.user_id` 改为 NULL 的变更涉及 `MODIFY COLUMN`，无法幂等保护，脚本中已注释，需手动取消注释执行。
6. **字符集统一**：所有表使用 `utf8mb4` + `utf8mb4_general_ci`，不支持 `utf8mb4_0900_ai_ci`（MySQL 8.0 默认排序规则，但本项目使用 general_ci 以保持兼容性）。
7. **外键检查**：脚本开头执行 `SET FOREIGN_KEY_CHECKS = 0`，结尾恢复为 1，确保按依赖顺序插入数据时不受外键约束影响。
8. **Docker 初始化兼容性**：`resources/sql/` 下脚本不能包含 `DELIMITER`/存储过程（Docker `/docker-entrypoint-initdb.d/` 初始化时可能不兼容），仅保留纯 SQL 语句。

## 生成信息

- 生成时间：2026-08-09
- 源文件整合范围：`sql/`、`seckill-mall/src/main/resources/sql/`、`seckill-mall/src/test/resources/`、`docs/`、项目根目录
- 对应 Java Entity：`seckill-mall/src/main/java/com/seckill/mall/entity/` 下 23 个实体类
