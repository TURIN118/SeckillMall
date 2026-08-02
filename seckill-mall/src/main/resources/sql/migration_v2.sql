-- ============================================================
-- SeckillMall 数据库迁移脚本 V2
-- 新增表：t_banner（轮播图）、t_product_review（商品评论）
--
-- ⚠️  请手动执行此脚本，不会自动执行
-- 执行前请确认数据库连接和权限
-- ============================================================
-- MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID，BIGINT）
-- 执行顺序：t_banner → t_product_review
-- ============================================================

-- ------------------------------------------------------------
-- 1. 轮播图表：首页/活动页轮播图配置
-- ------------------------------------------------------------
CREATE TABLE `t_banner` (
    `id`          BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `title`       VARCHAR(100) DEFAULT NULL         COMMENT '轮播图标题',
    `image_url`   VARCHAR(500) NOT NULL             COMMENT '图片URL',
    `link_url`    VARCHAR(500) DEFAULT NULL         COMMENT '点击跳转链接',
    `sort_order`  INT          NOT NULL DEFAULT 0   COMMENT '排序权重（值越小越靠前）',
    `status`      TINYINT      NOT NULL DEFAULT 1   COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='轮播图表';

-- ------------------------------------------------------------
-- 2. 商品评论表：用户对商品的评论及商家回复
-- ------------------------------------------------------------
CREATE TABLE `t_product_review` (
    `id`            BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `product_id`    BIGINT       NOT NULL             COMMENT '商品ID',
    `user_id`       BIGINT       NOT NULL             COMMENT '评论用户ID',
    `order_id`      BIGINT       DEFAULT NULL         COMMENT '关联订单ID（可选）',
    `content`       TEXT         NOT NULL             COMMENT '评论内容',
    `rating`        TINYINT      NOT NULL DEFAULT 5   COMMENT '评分：1-5星',
    `images`        JSON         DEFAULT NULL         COMMENT '评论图片URL数组',
    `status`        TINYINT      NOT NULL DEFAULT 1   COMMENT '状态：1-显示/0-隐藏',
    `reply_content` TEXT         DEFAULT NULL         COMMENT '商家回复内容',
    `reply_time`    DATETIME     DEFAULT NULL         COMMENT '回复时间',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品评论表';