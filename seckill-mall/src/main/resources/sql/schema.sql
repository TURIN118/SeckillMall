-- ============================================================
-- 01_schema_full.sql — 秒杀商城数据库完整架构创建脚本（最终态）
-- MySQL 8.0+ / InnoDB / utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID）
-- 说明：本脚本整合自 sql/01_schema.sql、seckill-mall/src/main/resources/sql/schema.sql、
--       migration_v2~v7、normal_order_tables、seckill_activity_migration、
--       seckill_goods_add_columns 等所有散落 SQL，已合并全部迁移变更至最终态。
--       仅包含 CREATE TABLE IF NOT EXISTS 语句，不包含任何 INSERT 数据。
--       所有表按依赖顺序排列，包含所有索引。
-- 表数量：23 张
-- 生成时间：2026-08-09
-- ============================================================


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS `seckill_mall`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `seckill_mall`;

-- ============================================================
-- 表结构定义（按依赖顺序排列）
-- 依赖链：
--   t_user → t_category → t_category_attribute → t_category_attribute_value
--   → t_product → t_product_attribute → t_product_attribute_value → t_product_sku
--   → t_seckill_activity → t_seckill_goods → t_seckill_order
--   → t_user_address → t_normal_order → t_normal_order_item
--   → t_cart → t_user_favorite → t_product_review
--   → t_coupon → t_user_coupon → t_recharge_card
--   → t_banner → t_login_log → t_operation_log
-- ============================================================

-- ------------------------------------------------------------
-- 1. t_user — 用户表
-- 存储平台所有用户（买家/卖家/管理员），含钱包余额字段
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`          BIGINT       NOT NULL                   COMMENT '主键ID（雪花算法）',
    `username`    VARCHAR(50)  NOT NULL                   COMMENT '用户名（唯一）',
    `password`    VARCHAR(100) NOT NULL                   COMMENT '密码（BCrypt加密）',
    `phone`       VARCHAR(20)  NULL     DEFAULT NULL      COMMENT '手机号（唯一）',
    `email`       VARCHAR(100) NULL     DEFAULT NULL      COMMENT '邮箱地址',
    `nickname`    VARCHAR(50)  NULL     DEFAULT NULL      COMMENT '用户昵称',
    `avatar_url`  VARCHAR(255) NULL     DEFAULT NULL      COMMENT '头像URL',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0.00     COMMENT '钱包余额',
    `role`        ENUM('BUYER','SELLER','ADMIN')
                               NOT NULL DEFAULT 'BUYER'  COMMENT '角色：BUYER-买家/SELLER-卖家/ADMIN-管理员',
    `status`      ENUM('ACTIVE','DISABLED')
                               NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常/DISABLED-禁用',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. t_category — 商品分类表
-- 支持多级分类（自关联parent_id），一级分类parent_id为NULL
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category` (
    `id`          BIGINT       NOT NULL                   COMMENT '主键ID（雪花算法）',
    `name`        VARCHAR(50)  NOT NULL                   COMMENT '分类名称',
    `parent_id`   BIGINT       NULL     DEFAULT NULL      COMMENT '父分类ID（NULL=一级分类）',
    `sort_order`  INT          NOT NULL DEFAULT 0         COMMENT '排序权重（值越小越靠前）',
    `icon_url`    VARCHAR(255) NULL     DEFAULT NULL      COMMENT '分类图标URL',
    `status`      TINYINT      NOT NULL DEFAULT 1         COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. t_category_attribute — 分类属性模板表
-- 存储分类级别的规格维度模板定义（如「智能手表」分类下定义「颜色/版本/表带」）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_category_attribute`;
CREATE TABLE `t_category_attribute` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID（雪花算法）',
    `category_id` BIGINT       NOT NULL              COMMENT '所属分类ID',
    `name`        VARCHAR(50)  NOT NULL              COMMENT '属性名称（如：颜色、版本、表带）',
    `type`        ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：TEXT-文字/IMAGE-图片(色块)',
    `input_type`  ENUM('SELECT','INPUT') NOT NULL DEFAULT 'SELECT' COMMENT '录入方式：SELECT-从预设值选/INPUT-自由输入',
    `is_required` TINYINT      NOT NULL DEFAULT 0    COMMENT '是否必选：0-否/1-是',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性模板表';

-- ------------------------------------------------------------
-- 4. t_category_attribute_value — 分类属性预设值表
-- 存储分类属性模板的预设可选值（如「颜色」下预设「曜石黑/银月白/褚霞红」）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_category_attribute_value`;
CREATE TABLE `t_category_attribute_value` (
    `id`           BIGINT       NOT NULL              COMMENT '主键ID（雪花算法）',
    `attribute_id` BIGINT       NOT NULL              COMMENT '所属分类属性ID',
    `value`        VARCHAR(100) NOT NULL              COMMENT '预设值（如：曜石黑、标准版）',
    `image_url`    VARCHAR(255) NULL     DEFAULT NULL COMMENT '图片类型时的色块/图片URL',
    `sort_order`   INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_attribute` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性预设值表';

-- ------------------------------------------------------------
-- 5. t_product — 商品表
-- 商品基本信息、库存与展示数据，主键使用AUTO_INCREMENT
-- 包含 SKU 聚合冗余字段（min_price/max_price/total_stock，v7迁移）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`           VARCHAR(200)  NOT NULL               COMMENT '商品名称',
    `description`    TEXT          NULL     DEFAULT NULL   COMMENT '商品简介（纯文本）',
    `original_price` DECIMAL(10,2) NOT NULL               COMMENT '原价（元）',
    `stock`          INT           NOT NULL DEFAULT 0     COMMENT '普通库存数量',
    `min_price`      DECIMAL(10,2) NULL     DEFAULT NULL   COMMENT 'SKU最低价（有SKU时由Service层维护）',
    `max_price`      DECIMAL(10,2) NULL     DEFAULT NULL   COMMENT 'SKU最高价（有SKU时由Service层维护）',
    `total_stock`    INT           NULL     DEFAULT NULL   COMMENT 'SKU库存汇总（有SKU时由Service层维护）',
    `sales_count`    INT           NOT NULL DEFAULT 0     COMMENT '累计销量',
    `cart_count`     INT           NOT NULL DEFAULT 0     COMMENT '加购数量(冗余计数)',
    `favorite_count` INT           NOT NULL DEFAULT 0     COMMENT '收藏数量(冗余计数)',
    `category_id`    BIGINT        NOT NULL               COMMENT '所属分类ID',
    `images`         JSON          NULL     DEFAULT NULL   COMMENT '商品图片URL数组',
    `main_image`     VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '主图URL',
    `detail_html`    TEXT          NULL     DEFAULT NULL   COMMENT '商品详情（富文本HTML）',
    `status`         ENUM('ON_SALE','OFF_SHELF') NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE-在售/OFF_SHELF-下架',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0     COMMENT '逻辑删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category_status` (`category_id`, `status`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=117 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

-- ------------------------------------------------------------
-- 6. t_product_attribute — 商品属性表（SKU维度定义）
-- 存储商品的规格维度定义（如「颜色」「版本」「表带」）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_attribute`;
CREATE TABLE `t_product_attribute` (
    `id`                    BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `product_id`            BIGINT       NOT NULL             COMMENT '所属商品ID',
    `category_attribute_id` BIGINT       NULL     DEFAULT NULL COMMENT '关联分类属性模板ID（NULL=商品自定义属性）',
    `name`                  VARCHAR(50)  NOT NULL             COMMENT '属性名（如：颜色/版本/表带）',
    `type`                  ENUM('IMAGE','TEXT') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：IMAGE-图片型/TEXT-文字型',
    `sort_order`            INT          NOT NULL DEFAULT 0   COMMENT '排序权重（值越小越靠前）',
    `is_deleted`            TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_category_attribute_id` (`category_attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性表（SKU维度定义）';

-- ------------------------------------------------------------
-- 7. t_product_attribute_value — 商品属性值表（SKU维度可选值）
-- 存储每个属性下的可选值（如「颜色」下的「曜石黑」「银月白」等）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_attribute_value`;
CREATE TABLE `t_product_attribute_value` (
    `id`           BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `attribute_id` BIGINT       NOT NULL             COMMENT '所属属性ID',
    `product_id`   BIGINT       NOT NULL             COMMENT '所属商品ID（冗余，加速查询）',
    `value`        VARCHAR(100) NOT NULL             COMMENT '属性值（如：曜石黑/旗舰版/氟橡胶表带）',
    `image_url`    VARCHAR(255) NULL     DEFAULT NULL COMMENT '图片型属性值的色块URL或图片URL（文字型为NULL）',
    `sort_order`   INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_attribute_id` (`attribute_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性值表（SKU维度可选值）';

-- ------------------------------------------------------------
-- 8. t_product_sku — 商品SKU表（属性值组合）
-- 存储每个属性值组合的独立价格/库存/图片/编码/状态
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_sku`;
CREATE TABLE `t_product_sku` (
    `id`          BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `product_id`  BIGINT        NOT NULL            COMMENT '所属商品ID',
    `sku_code`    VARCHAR(64)   NOT NULL            COMMENT 'SKU编码（同商品内唯一，由generateSkuCode可靠生成或后台手填，禁止NULL）',
    `price`       DECIMAL(10,2) NOT NULL            COMMENT 'SKU价格（元）',
    `stock`       INT           NOT NULL DEFAULT 0  COMMENT 'SKU库存',
    `main_image`  VARCHAR(255)  NULL     DEFAULT NULL COMMENT 'SKU主图URL（NULL表示沿用商品主图）',
    `attributes`  JSON          NOT NULL            COMMENT 'SKU属性键值对JSON，如{"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}',
    `status`      TINYINT       NOT NULL DEFAULT 1  COMMENT '是否在售：1-在售/0-停售（与库存无关，库存为0时status仍为1，前端因stock=0置灰）',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`product_id`, `sku_code`) COMMENT '同商品内SKU编码唯一',
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_product_status` (`product_id`, `status`),
    INDEX `idx_product_status_stock` (`product_id`, `status`, `stock`) COMMENT '快速查询某商品下库存大于0的启用SKU（前台展示可选规格）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品SKU表（属性值组合）';

-- ------------------------------------------------------------
-- 9. t_seckill_activity — 秒杀活动场次表
-- 秒杀场次化重构：一个场次包含多个秒杀商品
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_activity`;
CREATE TABLE `t_seckill_activity` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID（雪花算法）',
    `name`        VARCHAR(128) NOT NULL              COMMENT '场次名称',
    `start_time`  DATETIME     NOT NULL              COMMENT '场次开始时间',
    `end_time`    DATETIME     NOT NULL              COMMENT '场次结束时间',
    `status`      TINYINT      NOT NULL DEFAULT 0    COMMENT '0=待开始 1=进行中 2=已结束',
    `per_limit`   INT          NOT NULL DEFAULT 1    COMMENT '每人限购数量',
    `description` VARCHAR(512) NULL     DEFAULT NULL COMMENT '场次描述',
    `images`      VARCHAR(1024) NULL    DEFAULT NULL COMMENT '场次图片(JSON数组)',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动场次表';

-- ------------------------------------------------------------
-- 10. t_seckill_goods — 秒杀活动表
-- 管理秒杀商品、库存与活动时间窗口，含活动名称/限购/图片/描述/场次关联
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_goods`;
CREATE TABLE `t_seckill_goods` (
    `id`              BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `product_id`      BIGINT        NOT NULL            COMMENT '关联商品ID',
    `seckill_price`   DECIMAL(10,2) NOT NULL            COMMENT '秒杀价格（元）',
    `stock_count`     INT           NOT NULL            COMMENT '秒杀总库存',
    `available_count` INT           NOT NULL            COMMENT '可用库存（实时扣减）',
    `start_time`      DATETIME      NOT NULL            COMMENT '秒杀开始时间',
    `end_time`        DATETIME      NOT NULL            COMMENT '秒杀结束时间',
    `status`          ENUM('PENDING','ACTIVE','ENDED','CANCELLED')
                                    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待开始/ACTIVE-进行中/ENDED-已结束/CANCELLED-已取消',
    `creator_id`      BIGINT        NOT NULL            COMMENT '创建人ID（管理员）',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `seckill_name`    VARCHAR(255)  NULL     DEFAULT NULL COMMENT '秒杀活动名称',
    `per_limit`       INT           NOT NULL DEFAULT 1  COMMENT '每人限购数量',
    `images`          TEXT          NULL     DEFAULT NULL COMMENT '活动图片(JSON数组)',
    `description`     TEXT          NULL     DEFAULT NULL COMMENT '活动描述',
    `activity_id`     BIGINT        NULL     DEFAULT NULL COMMENT '关联场次ID',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status_start` (`status`, `start_time`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动表';

-- ------------------------------------------------------------
-- 11. t_seckill_order — 秒杀订单表
-- 核心交易表，含一人一单唯一约束
-- 包含SHIPPED状态及物流字段（shipping_company/shipping_no/ship_time/confirm_time）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_order`;
CREATE TABLE `t_seckill_order` (
    `id`               BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `order_no`         VARCHAR(32)   NOT NULL            COMMENT '订单号（唯一，时间戳+随机串）',
    `user_id`          BIGINT        NOT NULL            COMMENT '下单用户ID',
    `seckill_id`       BIGINT        NOT NULL            COMMENT '秒杀活动ID',
    `product_id`       BIGINT        NOT NULL            COMMENT '商品ID（冗余，减少JOIN）',
    `seckill_price`    DECIMAL(10,2) NOT NULL            COMMENT '下单时秒杀价（价格快照）',
    `quantity`         INT           NOT NULL DEFAULT 1  COMMENT '购买数量',
    `total_amount`     DECIMAL(10,2) NOT NULL            COMMENT '订单总金额（秒杀价×数量）',
    `status`           ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED')
                                     NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-待支付/PAID-已支付/SHIPPED-已发货/CANCELLED-已取消/TIMEOUT-超时/COMPLETED-已完成',
    `shipping_company` VARCHAR(50)   NULL     DEFAULT NULL COMMENT '物流公司',
    `shipping_no`      VARCHAR(64)   NULL     DEFAULT NULL COMMENT '快递单号',
    `ship_time`        DATETIME      NULL     DEFAULT NULL COMMENT '发货时间',
    `confirm_time`     DATETIME      NULL     DEFAULT NULL COMMENT '确认收货时间',
    `pay_time`         DATETIME      NULL     DEFAULT NULL COMMENT '支付时间',
    `pay_expire_time`  DATETIME      NULL     DEFAULT NULL COMMENT '支付截止时间（超时自动取消）',
    `transaction_id`   VARCHAR(64)   NULL     DEFAULT NULL COMMENT '第三方支付流水号',
    `pay_method`       VARCHAR(20)   NULL     DEFAULT NULL COMMENT '支付方式（ALIPAY/WECHAT等）',
    `cancel_time`      DATETIME      NULL     DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`    VARCHAR(255)  NULL     DEFAULT NULL COMMENT '取消原因',
    `is_deleted`       TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间（下单时间）',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_seckill` (`user_id`, `seckill_id`) COMMENT '一人一单约束（核心）',
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_seckill_status` (`seckill_id`, `status`),
    INDEX `idx_shipping_no` (`shipping_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀订单表';

-- ------------------------------------------------------------
-- 12. t_user_address — 收货地址表
-- 用户收货信息管理，支持默认地址设置
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_address`;
CREATE TABLE `t_user_address` (
    `id`             BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `user_id`        BIGINT       NOT NULL             COMMENT '所属用户ID',
    `receiver_name`  VARCHAR(50)  NOT NULL             COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20)  NOT NULL             COMMENT '收货人手机号',
    `province`       VARCHAR(30)  NOT NULL             COMMENT '省份',
    `city`           VARCHAR(30)  NOT NULL             COMMENT '城市',
    `district`       VARCHAR(30)  NOT NULL             COMMENT '区/县',
    `detail_address` VARCHAR(200) NOT NULL             COMMENT '详细地址',
    `is_default`     TINYINT      NOT NULL DEFAULT 0   COMMENT '是否默认地址：0-否/1-是',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收货地址表';

-- ------------------------------------------------------------
-- 13. t_normal_order — 普通订单表（立即购买/购物车结算）
-- 与t_seckill_order独立：有address_id字段，无seckill_id字段
-- 拆分total_amount/freight_amount/pay_amount，含remark字段
-- 包含SHIPPED状态及物流字段（shipping_company/shipping_no/ship_time/confirm_time）
-- 包含优惠券字段（user_coupon_id/discount_amount，V8迁移新增）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_normal_order`;
CREATE TABLE `t_normal_order` (
    `id`               BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `order_no`         VARCHAR(32)   NOT NULL            COMMENT '订单号（唯一，时间戳+随机串）',
    `user_id`          BIGINT        NOT NULL            COMMENT '下单用户ID',
    `address_id`       BIGINT        NOT NULL            COMMENT '收货地址ID',
    `total_amount`     DECIMAL(10,2) NOT NULL            COMMENT '商品总金额（明细小计之和）',
    `freight_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_amount`       DECIMAL(10,2) NOT NULL            COMMENT '实付金额 = total_amount + freight_amount - discount_amount',
    `status`           ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED')
                                     NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-待支付/PAID-已支付/SHIPPED-已发货/CANCELLED-已取消/TIMEOUT-超时/COMPLETED-已完成',
    `shipping_company` VARCHAR(50)   NULL     DEFAULT NULL COMMENT '物流公司',
    `shipping_no`      VARCHAR(64)   NULL     DEFAULT NULL COMMENT '快递单号',
    `ship_time`        DATETIME      NULL     DEFAULT NULL COMMENT '发货时间',
    `confirm_time`     DATETIME      NULL     DEFAULT NULL COMMENT '确认收货时间',
    `pay_method`       VARCHAR(20)   NULL     DEFAULT NULL COMMENT '支付方式（WALLET/ALIPAY/WECHAT等）',
    `transaction_id`   VARCHAR(64)   NULL     DEFAULT NULL COMMENT '支付流水号',
    `pay_time`         DATETIME      NULL     DEFAULT NULL COMMENT '支付时间',
    `pay_expire_time`  DATETIME      NULL     DEFAULT NULL COMMENT '支付截止时间（超时自动取消）',
    `cancel_time`      DATETIME      NULL     DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`    VARCHAR(255)  NULL     DEFAULT NULL COMMENT '取消原因',
    `remark`           VARCHAR(255)  NULL     DEFAULT NULL COMMENT '用户下单备注',
    `user_coupon_id`   BIGINT        NULL     DEFAULT NULL COMMENT '使用的用户优惠券ID（V8迁移新增）',
    `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额（V8迁移新增，无券时为0）',
    `is_deleted`       TINYINT       NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间（下单时间）',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_user_create` (`user_id`, `create_time`),
    INDEX `idx_shipping_no` (`shipping_no`),
    INDEX `idx_user_coupon` (`user_coupon_id`) COMMENT '加速按优惠券查询订单（V8迁移新增）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单表（立即购买/购物车结算）';

-- ------------------------------------------------------------
-- 14. t_normal_order_item — 普通订单明细表
-- 一个订单对应多个明细行；冗余商品名称/主图/单价保留下单时刻快照
-- 包含 SKU 快照字段（sku_id/sku_attributes，v7迁移）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_normal_order_item`;
CREATE TABLE `t_normal_order_item` (
    `id`              BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `order_id`        BIGINT        NOT NULL            COMMENT '所属普通订单ID',
    `product_id`      BIGINT        NOT NULL            COMMENT '商品ID',
    `sku_id`          BIGINT        NULL     DEFAULT NULL COMMENT 'SKU ID（下单时快照，NULL表示无规格）',
    `sku_attributes`  VARCHAR(500)  NULL     DEFAULT NULL COMMENT 'SKU属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带）',
    `product_name`    VARCHAR(200)  NOT NULL            COMMENT '商品名称（下单时快照）',
    `product_image`   VARCHAR(255)  NULL     DEFAULT NULL COMMENT '商品主图URL（下单时快照）',
    `unit_price`      DECIMAL(10,2) NOT NULL            COMMENT '商品单价（下单时快照，原价）',
    `quantity`        INT           NOT NULL DEFAULT 1  COMMENT '购买数量',
    `subtotal`        DECIMAL(10,2) NOT NULL            COMMENT '小计金额 = unit_price × quantity',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单明细表';

-- ------------------------------------------------------------
-- 15. t_cart — 购物车表
-- 用户加购商品信息，含SKU维度，唯一约束为(user_id, product_id, sku_id)
-- sku_id=0 表示无规格商品（v7迁移）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart` (
    `id`          BIGINT   NOT NULL             COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT   NOT NULL             COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL             COMMENT '商品ID',
    `sku_id`      BIGINT   NOT NULL DEFAULT 0   COMMENT 'SKU ID（0表示无规格商品，非0为具体SKU）',
    `quantity`    INT      NOT NULL DEFAULT 1   COMMENT '加购数量',
    `selected`    TINYINT  NOT NULL DEFAULT 1   COMMENT '是否选中：0-否/1-是',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`) COMMENT '用户+商品+SKU唯一约束（sku_id=0表示无规格）',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- ------------------------------------------------------------
-- 16. t_user_favorite — 用户收藏夹表
-- 用户收藏的商品，同一用户同一商品唯一约束
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_favorite`;
CREATE TABLE `t_user_favorite` (
    `id`          BIGINT       NOT NULL                COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `product_id`  BIGINT       NOT NULL                COMMENT '商品ID',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收藏夹表';

-- ------------------------------------------------------------
-- 17. t_product_review — 商品评论表
-- 用户对商品的评价，支持评分、图片、商家回复、SKU快照
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_review`;
CREATE TABLE `t_product_review` (
    `id`              BIGINT       NOT NULL                COMMENT '主键ID（雪花算法）',
    `product_id`      BIGINT       NOT NULL                COMMENT '商品ID',
    `sku_id`          BIGINT       NULL     DEFAULT NULL   COMMENT 'SKU ID（NULL表示无规格评论）',
    `sku_attributes`  VARCHAR(500) NULL     DEFAULT NULL   COMMENT 'SKU属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带）',
    `user_id`         BIGINT       NOT NULL                COMMENT '评论用户ID',
    `order_id`        BIGINT       NULL     DEFAULT NULL   COMMENT '关联订单ID（可选）',
    `content`         TEXT         NOT NULL                COMMENT '评论内容',
    `rating`          TINYINT      NOT NULL DEFAULT 5      COMMENT '评分：1-5星',
    `images`          JSON         NULL     DEFAULT NULL   COMMENT '评论图片URL数组',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-显示/0-隐藏',
    `reply_content`   TEXT         NULL     DEFAULT NULL   COMMENT '商家回复内容',
    `reply_time`      DATETIME     NULL     DEFAULT NULL   COMMENT '回复时间',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_sku_id` (`sku_id`),
    INDEX `idx_product_status_create` (`product_id`, `status`, `create_time`) COMMENT '加速商品评论分页查询（M-K6修复）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品评论表';

-- ------------------------------------------------------------
-- 18. t_coupon — 优惠券表
-- 支持满减券(AMOUNT)和折扣券(DISCOUNT)两种类型
-- 支持适用范围（ALL-全站/CATEGORY-分类/PRODUCT-商品），V8迁移新增 scope_type/category_id/product_id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_coupon`;
CREATE TABLE `t_coupon` (
    `id`              BIGINT        NOT NULL               COMMENT '主键ID（雪花算法）',
    `name`            VARCHAR(100)  NOT NULL               COMMENT '优惠券名称',
    `type`            ENUM('AMOUNT','DISCOUNT') NOT NULL   COMMENT '类型：AMOUNT-满减/DISCOUNT-折扣',
    `amount`          DECIMAL(10,2) NOT NULL               COMMENT '满减金额或折扣值(如0.85表示85折)',
    `min_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '最低消费金额',
    `total_count`     INT           NOT NULL               COMMENT '发放总数',
    `received_count`  INT           NOT NULL DEFAULT 0     COMMENT '已领取数',
    `used_count`      INT           NOT NULL DEFAULT 0     COMMENT '已使用数',
    `start_time`      DATETIME      NOT NULL               COMMENT '有效期开始',
    `end_time`        DATETIME      NOT NULL               COMMENT '有效期结束',
    `status`          TINYINT       NOT NULL DEFAULT 1     COMMENT '状态：1-启用/0-停用',
    `scope_type`      VARCHAR(10)   NOT NULL DEFAULT 'ALL' COMMENT '适用范围：ALL-全站/CATEGORY-分类/PRODUCT-商品',
    `category_id`     BIGINT        NULL     DEFAULT NULL  COMMENT '适用分类ID（scope_type=CATEGORY时有效）',
    `product_id`      BIGINT        NULL     DEFAULT NULL  COMMENT '适用商品ID（scope_type=PRODUCT时有效）',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_scope` (`scope_type`, `category_id`, `product_id`) COMMENT '加速按适用范围查询优惠券（V8迁移新增）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券表';

-- ------------------------------------------------------------
-- 19. t_user_coupon — 用户优惠券表
-- 用户领取的优惠券记录，关联优惠券和订单
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_coupon`;
CREATE TABLE `t_user_coupon` (
    `id`           BIGINT       NOT NULL               COMMENT '主键ID（雪花算法）',
    `user_id`      BIGINT       NOT NULL               COMMENT '用户ID',
    `coupon_id`    BIGINT       NOT NULL               COMMENT '优惠券ID',
    `status`       ENUM('UNUSED','USED','EXPIRED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/EXPIRED-已过期',
    `receive_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `use_time`     DATETIME     NULL     DEFAULT NULL  COMMENT '使用时间',
    `order_id`     BIGINT       NULL     DEFAULT NULL  COMMENT '使用的订单ID',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_coupon_id` (`coupon_id`),
    INDEX `idx_user_status_create` (`user_id`, `status`, `create_time`) COMMENT '加速"我的优惠券"分页查询（M-K6修复）',
    INDEX `idx_order_id` (`order_id`) COMMENT '加速订单关联优惠券查询（M-K6修复）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

-- ------------------------------------------------------------
-- 20. t_recharge_card — 充值卡表
-- 预付费充值卡，支持批次管理，卡密BCrypt加密存储
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_recharge_card`;
CREATE TABLE `t_recharge_card` (
    `id`            BIGINT        NOT NULL               COMMENT '主键ID（雪花算法）',
    `card_no`       VARCHAR(32)   NOT NULL               COMMENT '卡号(唯一)',
    `card_password` VARCHAR(64)   NOT NULL               COMMENT '卡密(BCrypt加密存储)',
    `face_value`    DECIMAL(10,2) NOT NULL               COMMENT '面额',
    `status`        ENUM('UNUSED','USED','DISABLED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/DISABLED-已禁用',
    `used_by`       BIGINT        NULL     DEFAULT NULL  COMMENT '使用者用户ID',
    `used_time`     DATETIME      NULL     DEFAULT NULL  COMMENT '使用时间',
    `batch_no`      VARCHAR(32)   NOT NULL               COMMENT '批次号',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_card_no` (`card_no`),
    INDEX `idx_batch_no` (`batch_no`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='充值卡表';

-- ------------------------------------------------------------
-- 21. t_banner — 轮播图表
-- 存储首页轮播图信息，支持排序和启用/禁用控制
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_banner`;
CREATE TABLE `t_banner` (
    `id`          BIGINT       NOT NULL                   COMMENT '主键ID（雪花算法）',
    `title`       VARCHAR(100) NULL     DEFAULT NULL      COMMENT '轮播图标题',
    `image_url`   VARCHAR(500) NOT NULL                   COMMENT '图片URL',
    `link_url`    VARCHAR(500) NULL     DEFAULT NULL      COMMENT '点击跳转链接',
    `sort_order`  INT          NOT NULL DEFAULT 0         COMMENT '排序权重（值越小越靠前）',
    `status`      TINYINT      NOT NULL DEFAULT 1         COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='轮播图表';

-- ------------------------------------------------------------
-- 22. t_login_log — 登录日志表
-- 记录用户登录行为，append-only设计（无update_time/is_deleted）
-- M-S5 修复：user_id 允许为 NULL，不存在用户登录失败时也能记录日志
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_login_log`;
CREATE TABLE `t_login_log` (
    `id`             BIGINT       NOT NULL                COMMENT '主键ID（雪花算法）',
    `user_id`        BIGINT       NULL     DEFAULT NULL   COMMENT '用户ID（NULL=用户不存在）',
    `login_ip`       VARCHAR(50)  NOT NULL                COMMENT '登录IP地址',
    `login_location` VARCHAR(100) NULL     DEFAULT NULL   COMMENT '登录地点（IP解析城市）',
    `user_agent`     VARCHAR(500) NULL     DEFAULT NULL   COMMENT '浏览器User-Agent',
    `login_result`   ENUM('SUCCESS','FAILED') NOT NULL    COMMENT '登录结果：SUCCESS-成功/FAILED-失败',
    `fail_reason`    VARCHAR(255) NULL     DEFAULT NULL   COMMENT '失败原因',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `create_time`),
    INDEX `idx_ip_time` (`login_ip`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ------------------------------------------------------------
-- 23. t_operation_log — 操作日志表（后台）
-- 管理员操作审计追踪，append-only设计（无update_time/is_deleted）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
    `id`          BIGINT       NOT NULL                COMMENT '主键ID（雪花算法）',
    `operator_id` BIGINT       NOT NULL                COMMENT '操作人ID',
    `module`      VARCHAR(50)  NOT NULL                COMMENT '操作模块（商品管理/用户管理/秒杀管理等）',
    `action`      VARCHAR(50)  NOT NULL                COMMENT '操作类型（CREATE/UPDATE/DELETE/EXPORT等）',
    `target_id`   VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '操作目标ID',
    `target_type` VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '操作目标类型（PRODUCT/SECKILL/USER等）',
    `detail`      TEXT         NULL     DEFAULT NULL   COMMENT '操作详情（JSON变更快照）',
    `ip_address`  VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '操作IP地址',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operator_time` (`operator_id`, `create_time`),
    INDEX `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表（后台）';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 建表完成，共 23 张表
-- 表清单：
--   t_user, t_category, t_category_attribute, t_category_attribute_value,
--   t_product, t_product_attribute, t_product_attribute_value, t_product_sku,
--   t_seckill_activity, t_seckill_goods, t_seckill_order,
--   t_user_address, t_normal_order, t_normal_order_item,
--   t_cart, t_user_favorite, t_product_review,
--   t_coupon, t_user_coupon, t_recharge_card,
--   t_banner, t_login_log, t_operation_log
-- ============================================================
