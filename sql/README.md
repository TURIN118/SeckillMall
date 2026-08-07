# 秒杀商城数据库脚本说明

本目录包含秒杀商城项目的完整数据库初始化与迁移脚本，由散落在项目各处的 SQL 文件整合而成。

## 文件清单

| 文件 | 用途 | 说明 |
|------|------|------|
| `01_schema_full.sql` | 完整建表脚本 | 包含全部 **23 张表**的最终态定义，按依赖顺序排列，含环境护栏 |
| `02_init_data_full.sql` | 完整初始化数据 | 包含 **190 条**种子数据（用户/分类/商品/秒杀/轮播图/充值卡），PII 已脱敏 |
| `03_migration_full.sql` | 完整幂等迁移脚本 | 整合 V2~V7 所有增量变更，约 **30 条** DDL，可重复执行 |
| `01_schema.sql` | （旧）建表脚本 | 保留，勿用于新环境 |
| `02_init_data.sql` | （旧）初始化数据 | 保留，含未脱敏数据 |
| `03_migration.sql` | （旧）业务数据迁移 | 保留，非 schema 迁移 |

## 环境要求

- **MySQL 8.0+**（需要 `JSON` 类型、`ENUM` 类型、`GENERATED` 列等特性）
- 字符集：`utf8mb4`，排序规则：`utf8mb4_general_ci`
- 存储引擎：`InnoDB`
- 客户端工具：MySQL CLI / Navicat / DataGrip 等（需支持 `DELIMITER` 语法）

## 执行顺序

### 全新环境初始化

```bash
# 1. 建表（需设置环境变量以通过护栏检查）
mysql -u root -p -e "SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true'; SOURCE 01_schema_full.sql;"

# 2. 初始化数据
mysql -u root -p seckill_mall < 02_init_data_full.sql

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
| 18 | `t_coupon` | 优惠券表 | 优惠券(V4) |
| 19 | `t_user_coupon` | 用户优惠券表 | 优惠券(V4) |
| 20 | `t_recharge_card` | 充值卡表 | 钱包(V4) |
| 21 | `t_banner` | 轮播图表 | 运营(V2) |
| 22 | `t_login_log` | 登录日志表 | 日志 |
| 23 | `t_operation_log` | 操作日志表 | 日志 |

## 初始化数据统计（190 条）

| 数据类型 | 条数 | 说明 |
|----------|------|------|
| 管理员用户 | 2 | admin / test（密码已 BCrypt 加密） |
| 商品分类 | 68 | 10 一级 + 58 二级分类树 |
| 商品 | 116 | 含秒杀商品与普通商品 |
| 秒杀商品 | 1 | 关联到商品表 |
| 轮播图 | 2 | 首页 Banner |
| 充值卡 | 1 | 示例充值卡 |

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
| M-K6 | 补充复合索引 | `t_product_review`, `t_user_coupon` |
| M-S5 | 登录日志 user_id 允许 NULL | `t_login_log` |

## 环境护栏说明

`01_schema_full.sql` 开头包含环境护栏存储过程，防止误删已有数据库：

```sql
-- 需要先设置此变量才能执行建表脚本
SET @SCHEMA_DESTRUCTIVE_ALLOWED = 'true';
SOURCE sql/01_schema_full.sql;
```

若未设置该变量，脚本会通过 `SIGNAL SQLSTATE '45000'` 抛出错误并中止执行。

## 幂等性说明

`03_migration_full.sql` 中的所有变更均通过存储过程实现幂等保护：

- `add_column_if_not_exists` — 列不存在时才 ADD COLUMN
- `add_index_if_not_exists` — 索引不存在时才 ADD INDEX
- `add_unique_if_not_exists` — 唯一索引不存在时才 ADD UNIQUE KEY
- `drop_index_if_exists` — 索引存在时才 DROP INDEX
- `CREATE TABLE IF NOT EXISTS` — 表不存在时才建表

因此该脚本可安全重复执行，不会因已存在的对象而报错。

## 注意事项

1. **不要删除旧脚本**：`01_schema.sql`、`02_init_data.sql`、`03_migration.sql` 为历史保留，供参考比对。
2. **新环境只需执行 01 + 02**：`01_schema_full.sql` 已包含所有最终态表结构，无需再执行迁移脚本。
3. **已有环境只需执行 03**：迁移脚本会将旧版表结构升级到最终态。
4. **M-S5 修复需手动执行**：`t_login_log.user_id` 改为 NULL 的变更涉及 `MODIFY COLUMN`，无法幂等保护，脚本中已注释，需手动取消注释执行。
5. **字符集统一**：所有表使用 `utf8mb4` + `utf8mb4_general_ci`，不支持 `utf8mb4_0900_ai_ci`（MySQL 8.0 默认排序规则，但本项目使用 general_ci 以保持兼容性）。
6. **外键检查**：脚本开头执行 `SET FOREIGN_KEY_CHECKS = 0`，结尾恢复为 1，确保按依赖顺序插入数据时不受外键约束影响。

## 生成信息

- 生成时间：2026-08-07
- 源文件整合范围：`sql/`、`seckill-mall/src/main/resources/sql/`、`seckill-mall/src/test/resources/`、`docs/`、项目根目录
- 对应 Java Entity：`seckill-mall/src/main/java/com/seckill/mall/entity/` 下 23 个实体类