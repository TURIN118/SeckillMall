-- ============================================================
-- SeckillMall 数据库迁移脚本 V3
-- 新增表：t_cart（购物车）、t_user_favorite（用户收藏夹）
-- 变更表：t_product 新增 cart_count、favorite_count 冗余计数字段
--
-- 请手动执行此脚本，不自动执行。
-- 创建购物车表、收藏夹表，并为商品表添加冗余计数字段。
-- ============================================================
-- MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID，BIGINT）
-- 执行顺序：t_cart → t_user_favorite → ALTER t_product
-- ============================================================

-- ------------------------------------------------------------
-- 1. 购物车表：用户加购商品（每用户每商品唯一）
-- ------------------------------------------------------------
CREATE TABLE `t_cart` (
    `id`          BIGINT   NOT NULL             COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT   NOT NULL             COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL             COMMENT '商品ID',
    `quantity`    INT      NOT NULL DEFAULT 1   COMMENT '加购数量',
    `selected`    TINYINT  NOT NULL DEFAULT 1   COMMENT '是否选中：0-否/1-是',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- ------------------------------------------------------------
-- 2. 用户收藏夹表：用户收藏商品（每用户每商品唯一）
-- ------------------------------------------------------------
CREATE TABLE `t_user_favorite` (
    `id`          BIGINT   NOT NULL             COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT   NOT NULL             COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL             COMMENT '商品ID',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收藏夹表';

-- ------------------------------------------------------------
-- 3. 商品表新增冗余计数字段：cart_count（加购数）、favorite_count（收藏数）
--    放在 sales_count 之后，用于前台展示热度，避免频繁 COUNT 查询
-- ------------------------------------------------------------
ALTER TABLE `t_product` ADD COLUMN `cart_count`      INT NOT NULL DEFAULT 0 COMMENT '加购数量(冗余计数)' AFTER `sales_count`;
ALTER TABLE `t_product` ADD COLUMN `favorite_count`  INT NOT NULL DEFAULT 0 COMMENT '收藏数量(冗余计数)' AFTER `cart_count`;