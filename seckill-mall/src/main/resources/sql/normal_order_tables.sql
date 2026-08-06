-- ============================================================
-- SeckillMall 数据库迁移脚本：普通订单（需求5 立即购买 + 需求13 购物车结算）
--
-- 请手动执行此脚本，不自动执行。
-- 1) 新建 t_normal_order  普通订单主表
-- 2) 新建 t_normal_order_item 普通订单明细表
-- ============================================================
-- MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID，BIGINT）
-- 执行顺序：t_normal_order → t_normal_order_item
-- ============================================================

-- ------------------------------------------------------------
-- 1. 普通订单主表
--    与 t_seckill_order 相互独立：
--      * 增加 address_id 字段（普通订单需要收货地址）
--      * 去掉 seckill_id 字段（非秒杀场景）
--      * 拆分 total_amount / freight_amount / pay_amount
--      * 增加 remark 字段（用户下单备注）
-- ------------------------------------------------------------
CREATE TABLE `t_normal_order` (
    `id`              BIGINT        NOT NULL            COMMENT '主键ID',
    `order_no`        VARCHAR(32)   NOT NULL            COMMENT '订单号（唯一，时间戳+随机串）',
    `user_id`         BIGINT        NOT NULL            COMMENT '下单用户ID',
    `address_id`      BIGINT        NOT NULL            COMMENT '收货地址ID',
    `total_amount`    DECIMAL(10,2) NOT NULL            COMMENT '商品总金额（明细小计之和）',
    `freight_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_amount`      DECIMAL(10,2) NOT NULL            COMMENT '实付金额 = total_amount + freight_amount',
    `status`          ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED')
                                   NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-待支付/PAID-已支付/SHIPPED-已发货/CANCELLED-已取消/TIMEOUT-超时/COMPLETED-已完成',
    `shipping_company` VARCHAR(50)  DEFAULT NULL        COMMENT '物流公司',
    `shipping_no`     VARCHAR(64)   DEFAULT NULL        COMMENT '快递单号',
    `ship_time`       DATETIME      DEFAULT NULL        COMMENT '发货时间',
    `confirm_time`    DATETIME      DEFAULT NULL        COMMENT '确认收货时间',
    `pay_method`      VARCHAR(20)   DEFAULT NULL        COMMENT '支付方式（WALLET/ALIPAY/WECHAT等）',
    `transaction_id`  VARCHAR(64)   DEFAULT NULL        COMMENT '支付流水号',
    `pay_time`        DATETIME      DEFAULT NULL        COMMENT '支付时间',
    `pay_expire_time` DATETIME      DEFAULT NULL        COMMENT '支付截止时间（超时自动取消）',
    `cancel_time`     DATETIME      DEFAULT NULL        COMMENT '取消时间',
    `cancel_reason`   VARCHAR(255)  DEFAULT NULL        COMMENT '取消原因',
    `remark`          VARCHAR(255)  DEFAULT NULL        COMMENT '用户下单备注',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间（下单时间）',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_user_create` (`user_id`, `create_time`),
    KEY `idx_shipping_no` (`shipping_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单表（立即购买/购物车结算）';

-- ------------------------------------------------------------
-- 2. 普通订单明细表
--    一个订单对应多个明细行；冗余商品名称/主图/单价保留下单时刻快照。
-- ------------------------------------------------------------
CREATE TABLE `t_normal_order_item` (
    `id`            BIGINT        NOT NULL            COMMENT '主键ID',
    `order_id`      BIGINT        NOT NULL            COMMENT '所属普通订单ID',
    `product_id`    BIGINT        NOT NULL            COMMENT '商品ID',
    `product_name`  VARCHAR(200)  NOT NULL            COMMENT '商品名称（下单时快照）',
    `product_image` VARCHAR(255)  DEFAULT NULL        COMMENT '商品主图URL（下单时快照）',
    `unit_price`    DECIMAL(10,2) NOT NULL            COMMENT '商品单价（下单时快照，原价）',
    `quantity`      INT           NOT NULL DEFAULT 1  COMMENT '购买数量',
    `subtotal`      DECIMAL(10,2) NOT NULL            COMMENT '小计金额 = unit_price × quantity',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单明细表';