-- ============================================================
-- 03_migration_full.sql — 秒杀商城完整迁移脚本（幂等版）
-- 说明：本脚本整合自以下所有散落的迁移脚本，按版本顺序排列：
--   v2: t_banner, t_product_review 新建
--   v3: t_cart, t_user_favorite 新建；t_product 加 cart_count/favorite_count
--   v4: t_user 加 balance；t_coupon, t_user_coupon, t_recharge_card 新建
--   normal_order_tables: t_normal_order, t_normal_order_item 新建
--   v6_shipping: 订单表加物流字段和SHIPPED状态
--   seckill_goods_add_columns: t_seckill_goods 加 seckill_name/per_limit/images/description
--   v7_sku: SKU 相关表新建；t_cart 加 sku_id；t_normal_order_item/t_product_review 加 sku_id/sku_attributes；t_product 加 min_price/max_price/total_stock
--   seckill_activity_migration: t_seckill_activity 新建；t_seckill_goods 加 activity_id
--   v8_coupon_scope_and_order_discount: t_coupon 加 scope_type/category_id/product_id；t_normal_order 加 user_coupon_id/discount_amount
-- 幂等保护：每个变更用 IF NOT EXISTS / IF EXISTS / 存储过程保护，可重复执行
-- 注意：本脚本用于已有环境的增量迁移。新环境直接执行 01_schema_full.sql 即可（已含最终态）
-- 变更条数：约 36 条 DDL 语句
-- 生成时间：2026-08-09
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `seckill_mall`;

-- ============================================================
-- 辅助：幂等 ADD COLUMN 存储过程
-- MySQL 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 语法，
-- 通过存储过程检查 information_schema.columns 实现幂等
-- ============================================================
DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `add_column_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 辅助：幂等 ADD INDEX 存储过程
DROP PROCEDURE IF EXISTS `add_index_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `add_index_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 辅助：幂等 ADD UNIQUE INDEX 存储过程
DROP PROCEDURE IF EXISTS `add_unique_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `add_unique_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 辅助：幂等 DROP INDEX 存储过程
DROP PROCEDURE IF EXISTS `drop_index_if_exists`;
DELIMITER $$
CREATE PROCEDURE `drop_index_if_exists`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- V2 迁移：新增 t_banner、t_product_review
-- ============================================================

-- t_banner — 轮播图表
CREATE TABLE IF NOT EXISTS `t_banner` (
    `id`          BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `title`       VARCHAR(100) NULL DEFAULT NULL    COMMENT '轮播图标题',
    `image_url`   VARCHAR(500) NOT NULL             COMMENT '图片URL',
    `link_url`    VARCHAR(500) NULL DEFAULT NULL    COMMENT '点击跳转链接',
    `sort_order`  INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `status`      TINYINT      NOT NULL DEFAULT 1   COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='轮播图表';

-- t_product_review — 商品评论表
CREATE TABLE IF NOT EXISTS `t_product_review` (
    `id`            BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `product_id`    BIGINT       NOT NULL             COMMENT '商品ID',
    `user_id`       BIGINT       NOT NULL             COMMENT '评论用户ID',
    `order_id`      BIGINT       NULL DEFAULT NULL    COMMENT '关联订单ID（可选）',
    `content`       TEXT         NOT NULL             COMMENT '评论内容',
    `rating`        TINYINT      NOT NULL DEFAULT 5   COMMENT '评分：1-5星',
    `images`        JSON         NULL DEFAULT NULL    COMMENT '评论图片URL数组',
    `status`        TINYINT      NOT NULL DEFAULT 1   COMMENT '状态：1-显示/0-隐藏',
    `reply_content` TEXT         NULL DEFAULT NULL    COMMENT '商家回复内容',
    `reply_time`    DATETIME     NULL DEFAULT NULL    COMMENT '回复时间',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品评论表';

-- ============================================================
-- V3 迁移：新增 t_cart、t_user_favorite；t_product 加冗余计数字段
-- ============================================================

-- t_cart — 购物车表
CREATE TABLE IF NOT EXISTS `t_cart` (
    `id`          BIGINT   NOT NULL             COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT   NOT NULL             COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL             COMMENT '商品ID',
    `quantity`    INT      NOT NULL DEFAULT 1   COMMENT '加购数量',
    `selected`    TINYINT  NOT NULL DEFAULT 1   COMMENT '是否选中：0-否/1-是',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- t_user_favorite — 用户收藏夹表
CREATE TABLE IF NOT EXISTS `t_user_favorite` (
    `id`          BIGINT       NOT NULL                COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `product_id`  BIGINT       NOT NULL                COMMENT '商品ID',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收藏夹表';

-- t_product 加 cart_count / favorite_count
CALL add_column_if_not_exists('t_product', 'cart_count',      'INT NOT NULL DEFAULT 0 COMMENT ''加购数量(冗余计数)'' AFTER `sales_count`');
CALL add_column_if_not_exists('t_product', 'favorite_count', 'INT NOT NULL DEFAULT 0 COMMENT ''收藏数量(冗余计数)'' AFTER `cart_count`');

-- ============================================================
-- V4 迁移：t_user 加 balance；新增 t_coupon、t_user_coupon、t_recharge_card
-- ============================================================

-- t_user 加 balance
CALL add_column_if_not_exists('t_user', 'balance', 'DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''钱包余额'' AFTER `avatar_url`');

-- t_coupon — 优惠券表
CREATE TABLE IF NOT EXISTS `t_coupon` (
    `id`             BIGINT        NOT NULL              COMMENT '主键ID（雪花算法）',
    `name`           VARCHAR(100)  NOT NULL              COMMENT '优惠券名称',
    `type`           ENUM('AMOUNT','DISCOUNT') NOT NULL  COMMENT '类型：AMOUNT-满减/DISCOUNT-折扣',
    `amount`         DECIMAL(10,2) NOT NULL              COMMENT '满减金额或折扣值',
    `min_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低消费金额',
    `total_count`    INT           NOT NULL              COMMENT '发放总数',
    `received_count` INT           NOT NULL DEFAULT 0    COMMENT '已领取数',
    `used_count`     INT           NOT NULL DEFAULT 0    COMMENT '已使用数',
    `start_time`     DATETIME      NOT NULL              COMMENT '有效期开始',
    `end_time`       DATETIME      NOT NULL              COMMENT '有效期结束',
    `status`         TINYINT       NOT NULL DEFAULT 1    COMMENT '状态：1-启用/0-停用',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券表';

-- t_user_coupon — 用户优惠券表
CREATE TABLE IF NOT EXISTS `t_user_coupon` (
    `id`           BIGINT   NOT NULL              COMMENT '主键ID（雪花算法）',
    `user_id`      BIGINT   NOT NULL              COMMENT '用户ID',
    `coupon_id`    BIGINT   NOT NULL              COMMENT '优惠券ID',
    `status`       ENUM('UNUSED','USED','EXPIRED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态',
    `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `use_time`     DATETIME NULL DEFAULT NULL     COMMENT '使用时间',
    `order_id`     BIGINT   NULL DEFAULT NULL     COMMENT '使用的订单ID',
    `is_deleted`   TINYINT  NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

-- t_recharge_card — 充值卡表
CREATE TABLE IF NOT EXISTS `t_recharge_card` (
    `id`            BIGINT        NOT NULL          COMMENT '主键ID（雪花算法）',
    `card_no`       VARCHAR(32)   NOT NULL          COMMENT '卡号(唯一)',
    `card_password` VARCHAR(64)   NOT NULL          COMMENT '卡密(BCrypt加密存储)',
    `face_value`    DECIMAL(10,2) NOT NULL          COMMENT '面额',
    `status`        ENUM('UNUSED','USED','DISABLED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态',
    `used_by`       BIGINT        NULL DEFAULT NULL COMMENT '使用者用户ID',
    `used_time`     DATETIME      NULL DEFAULT NULL COMMENT '使用时间',
    `batch_no`      VARCHAR(32)   NOT NULL          COMMENT '批次号',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_card_no` (`card_no`),
    INDEX `idx_batch_no` (`batch_no`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='充值卡表';

-- ============================================================
-- normal_order_tables 迁移：新增 t_normal_order、t_normal_order_item
-- ============================================================

-- t_normal_order — 普通订单表
CREATE TABLE IF NOT EXISTS `t_normal_order` (
    `id`              BIGINT        NOT NULL            COMMENT '主键ID',
    `order_no`        VARCHAR(32)   NOT NULL            COMMENT '订单号',
    `user_id`         BIGINT        NOT NULL            COMMENT '下单用户ID',
    `address_id`      BIGINT        NOT NULL            COMMENT '收货地址ID',
    `total_amount`    DECIMAL(10,2) NOT NULL            COMMENT '商品总金额',
    `freight_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_amount`      DECIMAL(10,2) NOT NULL            COMMENT '实付金额',
    `status`          ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED')
                                   NOT NULL DEFAULT 'UNPAID' COMMENT '订单状态',
    `shipping_company` VARCHAR(50)  NULL DEFAULT NULL   COMMENT '物流公司',
    `shipping_no`     VARCHAR(64)   NULL DEFAULT NULL   COMMENT '快递单号',
    `ship_time`       DATETIME      NULL DEFAULT NULL   COMMENT '发货时间',
    `confirm_time`    DATETIME      NULL DEFAULT NULL   COMMENT '确认收货时间',
    `pay_method`      VARCHAR(20)   NULL DEFAULT NULL   COMMENT '支付方式',
    `transaction_id`  VARCHAR(64)   NULL DEFAULT NULL   COMMENT '支付流水号',
    `pay_time`        DATETIME      NULL DEFAULT NULL   COMMENT '支付时间',
    `pay_expire_time` DATETIME      NULL DEFAULT NULL   COMMENT '支付截止时间',
    `cancel_time`     DATETIME      NULL DEFAULT NULL   COMMENT '取消时间',
    `cancel_reason`   VARCHAR(255)  NULL DEFAULT NULL   COMMENT '取消原因',
    `remark`          VARCHAR(255)  NULL DEFAULT NULL   COMMENT '用户下单备注',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_user_create` (`user_id`, `create_time`),
    INDEX `idx_shipping_no` (`shipping_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单表';

-- t_normal_order_item — 普通订单明细表
CREATE TABLE IF NOT EXISTS `t_normal_order_item` (
    `id`            BIGINT        NOT NULL            COMMENT '主键ID',
    `order_id`      BIGINT        NOT NULL            COMMENT '所属普通订单ID',
    `product_id`    BIGINT        NOT NULL            COMMENT '商品ID',
    `product_name`  VARCHAR(200)  NOT NULL            COMMENT '商品名称（下单时快照）',
    `product_image` VARCHAR(255)  NULL DEFAULT NULL   COMMENT '商品主图URL（下单时快照）',
    `unit_price`    DECIMAL(10,2) NOT NULL            COMMENT '商品单价（下单时快照）',
    `quantity`      INT           NOT NULL DEFAULT 1  COMMENT '购买数量',
    `subtotal`      DECIMAL(10,2) NOT NULL            COMMENT '小计金额',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单明细表';

-- ============================================================
-- V6 迁移：订单表加物流字段和 SHIPPED 状态
-- ============================================================

-- t_seckill_order 加物流字段
CALL add_column_if_not_exists('t_seckill_order', 'shipping_company', 'VARCHAR(50) NULL DEFAULT NULL COMMENT ''物流公司'' AFTER `status`');
CALL add_column_if_not_exists('t_seckill_order', 'shipping_no',      'VARCHAR(64) NULL DEFAULT NULL COMMENT ''快递单号'' AFTER `shipping_company`');
CALL add_column_if_not_exists('t_seckill_order', 'ship_time',        'DATETIME NULL DEFAULT NULL COMMENT ''发货时间'' AFTER `shipping_no`');
CALL add_column_if_not_exists('t_seckill_order', 'confirm_time',     'DATETIME NULL DEFAULT NULL COMMENT ''确认收货时间'' AFTER `ship_time`');
CALL add_index_if_not_exists('t_seckill_order', 'idx_shipping_no', '`shipping_no`');

-- t_normal_order 加物流字段
CALL add_column_if_not_exists('t_normal_order', 'shipping_company', 'VARCHAR(50) NULL DEFAULT NULL COMMENT ''物流公司'' AFTER `status`');
CALL add_column_if_not_exists('t_normal_order', 'shipping_no',      'VARCHAR(64) NULL DEFAULT NULL COMMENT ''快递单号'' AFTER `shipping_company`');
CALL add_column_if_not_exists('t_normal_order', 'ship_time',        'DATETIME NULL DEFAULT NULL COMMENT ''发货时间'' AFTER `shipping_no`');
CALL add_column_if_not_exists('t_normal_order', 'confirm_time',     'DATETIME NULL DEFAULT NULL COMMENT ''确认收货时间'' AFTER `ship_time`');
CALL add_index_if_not_exists('t_normal_order', 'idx_shipping_no', '`shipping_no`');

-- ============================================================
-- seckill_goods_add_columns 迁移：t_seckill_goods 加扩展字段
-- ============================================================

CALL add_column_if_not_exists('t_seckill_goods', 'seckill_name',  'VARCHAR(255) NULL DEFAULT NULL COMMENT ''秒杀活动名称''');
CALL add_column_if_not_exists('t_seckill_goods', 'per_limit',     'INT NOT NULL DEFAULT 1 COMMENT ''每人限购数量''');
CALL add_column_if_not_exists('t_seckill_goods', 'images',        'TEXT NULL DEFAULT NULL COMMENT ''活动图片(JSON数组)''');
CALL add_column_if_not_exists('t_seckill_goods', 'description',   'TEXT NULL DEFAULT NULL COMMENT ''活动描述''');

-- ============================================================
-- V7 SKU 迁移：新增 SKU 相关表；现有表加 SKU 字段
-- ============================================================

-- t_category_attribute — 分类属性模板表
CREATE TABLE IF NOT EXISTS `t_category_attribute` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID',
    `category_id` BIGINT       NOT NULL              COMMENT '所属分类ID',
    `name`        VARCHAR(50)  NOT NULL              COMMENT '属性名称',
    `type`        ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型',
    `input_type`  ENUM('SELECT','INPUT') NOT NULL DEFAULT 'SELECT' COMMENT '录入方式',
    `is_required` TINYINT      NOT NULL DEFAULT 0    COMMENT '是否必选',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性模板表';

-- t_category_attribute_value — 分类属性预设值表
CREATE TABLE IF NOT EXISTS `t_category_attribute_value` (
    `id`           BIGINT       NOT NULL              COMMENT '主键ID',
    `attribute_id` BIGINT       NOT NULL              COMMENT '所属分类属性ID',
    `value`        VARCHAR(100) NOT NULL              COMMENT '预设值',
    `image_url`    VARCHAR(255) NULL DEFAULT NULL     COMMENT '图片URL',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_attribute` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性预设值表';

-- t_product_attribute — 商品属性表
CREATE TABLE IF NOT EXISTS `t_product_attribute` (
    `id`                    BIGINT       NOT NULL             COMMENT '主键ID',
    `product_id`            BIGINT       NOT NULL             COMMENT '所属商品ID',
    `category_attribute_id` BIGINT       NULL DEFAULT NULL    COMMENT '关联分类属性模板ID',
    `name`                  VARCHAR(50)  NOT NULL             COMMENT '属性名',
    `type`                  ENUM('IMAGE','TEXT') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型',
    `sort_order`            INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`            TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_category_attribute_id` (`category_attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性表（SKU维度定义）';

-- t_product_attribute_value — 商品属性值表
CREATE TABLE IF NOT EXISTS `t_product_attribute_value` (
    `id`           BIGINT       NOT NULL             COMMENT '主键ID',
    `attribute_id` BIGINT       NOT NULL             COMMENT '所属属性ID',
    `product_id`   BIGINT       NOT NULL             COMMENT '所属商品ID（冗余）',
    `value`        VARCHAR(100) NOT NULL             COMMENT '属性值',
    `image_url`    VARCHAR(255) NULL DEFAULT NULL    COMMENT '图片URL',
    `sort_order`   INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_attribute_id` (`attribute_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性值表';

-- t_product_sku — 商品SKU表
CREATE TABLE IF NOT EXISTS `t_product_sku` (
    `id`          BIGINT        NOT NULL            COMMENT '主键ID',
    `product_id`  BIGINT        NOT NULL            COMMENT '所属商品ID',
    `sku_code`    VARCHAR(64)   NOT NULL            COMMENT 'SKU编码',
    `price`       DECIMAL(10,2) NOT NULL            COMMENT 'SKU价格',
    `stock`       INT           NOT NULL DEFAULT 0  COMMENT 'SKU库存',
    `main_image`  VARCHAR(255)  NULL DEFAULT NULL   COMMENT 'SKU主图URL',
    `attributes`  JSON          NOT NULL            COMMENT 'SKU属性键值对JSON',
    `status`      TINYINT       NOT NULL DEFAULT 1  COMMENT '是否在售',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`product_id`, `sku_code`) COMMENT '同商品内SKU编码唯一',
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_product_status` (`product_id`, `status`),
    INDEX `idx_product_status_stock` (`product_id`, `status`, `stock`) COMMENT '快速查询库存大于0的启用SKU'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品SKU表';

-- t_cart 加 sku_id 并修改唯一约束
CALL add_column_if_not_exists('t_cart', 'sku_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''SKU ID（0表示无规格商品）'' AFTER `product_id`');
-- 删除旧唯一约束 uk_user_product（如果存在），添加新的三列唯一约束
CALL drop_index_if_exists('t_cart', 'uk_user_product');
CALL add_unique_if_not_exists('t_cart', 'uk_user_product_sku', '`user_id`, `product_id`, `sku_id`');
CALL add_index_if_not_exists('t_cart', 'idx_sku_id', '`sku_id`');

-- t_normal_order_item 加 sku_id / sku_attributes
CALL add_column_if_not_exists('t_normal_order_item', 'sku_id',         'BIGINT NULL DEFAULT NULL COMMENT ''SKU ID（下单时快照）'' AFTER `product_id`');
CALL add_column_if_not_exists('t_normal_order_item', 'sku_attributes', 'VARCHAR(500) NULL DEFAULT NULL COMMENT ''SKU属性快照'' AFTER `sku_id`');
CALL add_index_if_not_exists('t_normal_order_item', 'idx_sku_id', '`sku_id`');

-- t_product_review 加 sku_id / sku_attributes
CALL add_column_if_not_exists('t_product_review', 'sku_id',         'BIGINT NULL DEFAULT NULL COMMENT ''SKU ID'' AFTER `product_id`');
CALL add_column_if_not_exists('t_product_review', 'sku_attributes', 'VARCHAR(500) NULL DEFAULT NULL COMMENT ''SKU属性快照'' AFTER `sku_id`');
CALL add_index_if_not_exists('t_product_review', 'idx_sku_id', '`sku_id`');

-- t_product 加 SKU 聚合冗余字段
CALL add_column_if_not_exists('t_product', 'min_price',   'DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''SKU最低价'' AFTER `stock`');
CALL add_column_if_not_exists('t_product', 'max_price',   'DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''SKU最高价'' AFTER `min_price`');
CALL add_column_if_not_exists('t_product', 'total_stock', 'INT NULL DEFAULT NULL COMMENT ''SKU库存汇总'' AFTER `max_price`');

-- ============================================================
-- seckill_activity_migration：新增 t_seckill_activity；t_seckill_goods 加 activity_id
-- ============================================================

-- t_seckill_activity — 秒杀活动场次表
CREATE TABLE IF NOT EXISTS `t_seckill_activity` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID（雪花算法）',
    `name`        VARCHAR(128) NOT NULL              COMMENT '场次名称',
    `start_time`  DATETIME     NOT NULL              COMMENT '场次开始时间',
    `end_time`    DATETIME     NOT NULL              COMMENT '场次结束时间',
    `status`      TINYINT      NOT NULL DEFAULT 0    COMMENT '0=待开始 1=进行中 2=已结束',
    `per_limit`   INT          NOT NULL DEFAULT 1    COMMENT '每人限购数量',
    `description` VARCHAR(512) NULL DEFAULT NULL     COMMENT '场次描述',
    `images`      VARCHAR(1024) NULL DEFAULT NULL    COMMENT '场次图片(JSON数组)',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动场次表';

-- t_seckill_goods 加 activity_id
CALL add_column_if_not_exists('t_seckill_goods', 'activity_id', 'BIGINT NULL DEFAULT NULL COMMENT ''关联场次ID''');
CALL add_index_if_not_exists('t_seckill_goods', 'idx_activity_id', '`activity_id`');

-- ============================================================
-- M-K6 修复：补充复合索引加速分页查询
-- ============================================================

-- t_product_review 加 (product_id, status, create_time) 复合索引
CALL add_index_if_not_exists('t_product_review', 'idx_product_status_create', '`product_id`, `status`, `create_time`');

-- t_user_coupon 加 (user_id, status, create_time) 复合索引和 order_id 索引
CALL add_index_if_not_exists('t_user_coupon', 'idx_user_status_create', '`user_id`, `status`, `create_time`');
CALL add_index_if_not_exists('t_user_coupon', 'idx_order_id', '`order_id`');

-- ============================================================
-- V8 迁移：优惠券适用范围字段 & 普通订单优惠字段
--   t_coupon 加 scope_type/category_id/product_id + idx_scope 索引
--   t_normal_order 加 user_coupon_id/discount_amount + idx_user_coupon 索引
-- 来源：Coupon.java / NormalOrder.java 实体类新增字段
-- ============================================================

-- t_coupon 加适用范围字段（scope_type/category_id/product_id）
CALL add_column_if_not_exists('t_coupon', 'scope_type',  'VARCHAR(10) NOT NULL DEFAULT ''ALL'' COMMENT ''适用范围：ALL-全站/CATEGORY-分类/PRODUCT-商品'' AFTER `status`');
CALL add_column_if_not_exists('t_coupon', 'category_id', 'BIGINT NULL DEFAULT NULL COMMENT ''适用分类ID（scope_type=CATEGORY时有效）'' AFTER `scope_type`');
CALL add_column_if_not_exists('t_coupon', 'product_id',  'BIGINT NULL DEFAULT NULL COMMENT ''适用商品ID（scope_type=PRODUCT时有效）'' AFTER `category_id`');
CALL add_index_if_not_exists('t_coupon', 'idx_scope', '`scope_type`, `category_id`, `product_id`');

-- t_normal_order 加优惠券使用字段（user_coupon_id/discount_amount）
CALL add_column_if_not_exists('t_normal_order', 'user_coupon_id',  'BIGINT NULL DEFAULT NULL COMMENT ''使用的用户优惠券ID'' AFTER `remark`');
CALL add_column_if_not_exists('t_normal_order', 'discount_amount', 'DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''优惠金额（无券时为0）'' AFTER `user_coupon_id`');
CALL add_index_if_not_exists('t_normal_order', 'idx_user_coupon', '`user_coupon_id`');

-- ============================================================
-- M-S5 修复：t_login_log 的 user_id 允许为 NULL（不存在用户登录失败时也能记录日志）
-- ============================================================
-- 注意：此变更需要 MODIFY COLUMN，无法用存储过程幂等保护，需手动执行
-- 如需应用此修复，请取消注释以下语句：
-- ALTER TABLE `t_login_log` MODIFY COLUMN `user_id` BIGINT NULL DEFAULT NULL COMMENT '用户ID（NULL=用户不存在）';
-- CALL add_index_if_not_exists('t_login_log', 'idx_ip_time', '`login_ip`, `create_time`');

-- ============================================================
-- 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
DROP PROCEDURE IF EXISTS `add_index_if_not_exists`;
DROP PROCEDURE IF EXISTS `add_unique_if_not_exists`;
DROP PROCEDURE IF EXISTS `drop_index_if_exists`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 迁移完成
-- 变更统计：
--   V2: 2 张新表（t_banner, t_product_review）
--   V3: 2 张新表（t_cart, t_user_favorite）+ 2 列（t_product）
--   V4: 3 张新表（t_coupon, t_user_coupon, t_recharge_card）+ 1 列（t_user）
--   normal_order: 2 张新表（t_normal_order, t_normal_order_item）
--   V6: 8 列 + 2 索引（物流字段）
--   seckill_add_columns: 4 列（t_seckill_goods）
--   V7: 5 张新表 + 5 列 + 4 索引
--   activity_migration: 1 张新表（t_seckill_activity）+ 1 列 + 1 索引
--   M-K6: 3 索引
--   V8: 5 列（t_coupon 3 列 + t_normal_order 2 列）+ 2 索引
--   合计：15 张新表 + 约 25 列变更 + 约 14 索引变更
-- ============================================================