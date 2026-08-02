-- ============================================================
-- SeckillMall 数据库迁移脚本 V4
-- 新增功能：钱包余额 / 优惠券 / 充值卡
--
-- 请手动执行此脚本，不自动执行。
-- 1) t_user 新增 balance 字段（钱包余额）
-- 2) 新建 t_coupon 优惠券表
-- 3) 新建 t_user_coupon 用户优惠券表
-- 4) 新建 t_recharge_card 充值卡表
-- ============================================================
-- MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID，BIGINT）
-- 执行顺序：ALTER t_user → t_coupon → t_user_coupon → t_recharge_card
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表新增钱包余额字段
--    放在 avatar_url 之后，DECIMAL(10,2) 支持最大 99999999.99
-- ------------------------------------------------------------
ALTER TABLE `t_user` ADD COLUMN `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额' AFTER `avatar_url`;

-- ------------------------------------------------------------
-- 2. 优惠券表：满减/折扣优惠券定义
--    type=AMOUNT  → amount 为满减金额（如 10 表示满减10元）
--    type=DISCOUNT → amount 为折扣值（如 0.85 表示85折）
-- ------------------------------------------------------------
CREATE TABLE `t_coupon` (
    `id`             BIGINT        NOT NULL              COMMENT '主键ID（雪花算法）',
    `name`           VARCHAR(100)  NOT NULL              COMMENT '优惠券名称',
    `type`           ENUM('AMOUNT','DISCOUNT') NOT NULL  COMMENT '类型：AMOUNT-满减/DISCOUNT-折扣',
    `amount`         DECIMAL(10,2) NOT NULL              COMMENT '满减金额或折扣值(如0.85表示85折)',
    `min_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低消费金额',
    `total_count`    INT           NOT NULL              COMMENT '发放总数',
    `received_count` INT           NOT NULL DEFAULT 0    COMMENT '已领取数',
    `used_count`     INT           NOT NULL DEFAULT 0    COMMENT '已使用数',
    `start_time`     DATETIME      NOT NULL              COMMENT '有效期开始',
    `end_time`       DATETIME      NOT NULL              COMMENT '有效期结束',
    `status`         TINYINT       NOT NULL DEFAULT 1    COMMENT '状态：1-启用/0-停用',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券表';

-- ------------------------------------------------------------
-- 3. 用户优惠券表：用户领取的优惠券
--    status=UNUSED  → 未使用
--    status=USED    → 已使用（关联 order_id）
--    status=EXPIRED → 已过期
-- ------------------------------------------------------------
CREATE TABLE `t_user_coupon` (
    `id`           BIGINT   NOT NULL              COMMENT '主键ID（雪花算法）',
    `user_id`      BIGINT   NOT NULL              COMMENT '用户ID',
    `coupon_id`    BIGINT   NOT NULL              COMMENT '优惠券ID',
    `status`       ENUM('UNUSED','USED','EXPIRED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/EXPIRED-已过期',
    `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `use_time`     DATETIME DEFAULT NULL          COMMENT '使用时间',
    `order_id`     BIGINT   DEFAULT NULL          COMMENT '使用的订单ID',
    `is_deleted`   TINYINT  NOT NULL DEFAULT 0    COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

-- ------------------------------------------------------------
-- 4. 充值卡表：预付费充值卡（卡号唯一，卡密加密存储）
--    status=UNUSED    → 未使用
--    status=USED      → 已使用（关联 used_by）
--    status=DISABLED  → 已禁用
-- ------------------------------------------------------------
CREATE TABLE `t_recharge_card` (
    `id`            BIGINT        NOT NULL          COMMENT '主键ID（雪花算法）',
    `card_no`       VARCHAR(32)   NOT NULL          COMMENT '卡号(唯一)',
    `card_password` VARCHAR(64)   NOT NULL          COMMENT '卡密(BCrypt加密存储)',
    `face_value`    DECIMAL(10,2) NOT NULL          COMMENT '面额',
    `status`        ENUM('UNUSED','USED','DISABLED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/DISABLED-已禁用',
    `used_by`       BIGINT        DEFAULT NULL      COMMENT '使用者用户ID',
    `used_time`     DATETIME      DEFAULT NULL      COMMENT '使用时间',
    `batch_no`      VARCHAR(32)   NOT NULL          COMMENT '批次号',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_card_no` (`card_no`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='充值卡表';