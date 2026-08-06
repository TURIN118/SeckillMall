-- ============================================================
-- 01_schema.sql — 秒杀商城数据库架构创建脚本
-- MySQL 8.0+ / InnoDB / utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID）
-- 说明：仅包含CREATE TABLE语句，不包含任何INSERT数据
--       所有表按表名字母顺序排列
--       包含最新架构（SHIPPED枚举、物流字段等）
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
-- 表结构定义（按表名字母顺序排列）
-- ============================================================

-- ------------------------------------------------------------
-- t_banner — 轮播图表
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
-- t_cart — 购物车表
-- 用户加购商品信息，同一用户同一商品唯一约束
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart` (
    `id`          BIGINT       NOT NULL                   COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT       NOT NULL                   COMMENT '用户ID',
    `product_id`  BIGINT       NOT NULL                   COMMENT '商品ID',
    `quantity`    INT          NOT NULL DEFAULT 1         COMMENT '加购数量',
    `selected`    TINYINT      NOT NULL DEFAULT 1         COMMENT '是否选中：0-否/1-是',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- ------------------------------------------------------------
-- t_category — 商品分类表
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
-- t_coupon — 优惠券表
-- 支持满减券(AMOUNT)和折扣券(DISCOUNT)两种类型
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
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券表';

-- ------------------------------------------------------------
-- t_login_log — 登录日志表
-- 记录用户登录行为，append-only设计（无update_time/is_deleted）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_login_log`;
CREATE TABLE `t_login_log` (
    `id`             BIGINT       NOT NULL                COMMENT '主键ID',
    `user_id`        BIGINT       NOT NULL                COMMENT '用户ID',
    `login_ip`       VARCHAR(50)  NOT NULL                COMMENT '登录IP地址',
    `login_location` VARCHAR(100) NULL     DEFAULT NULL   COMMENT '登录地点（IP解析城市）',
    `user_agent`     VARCHAR(500) NULL     DEFAULT NULL   COMMENT '浏览器User-Agent',
    `login_result`   ENUM('SUCCESS','FAILED') NOT NULL    COMMENT '登录结果：SUCCESS-成功/FAILED-失败',
    `fail_reason`    VARCHAR(255) NULL     DEFAULT NULL   COMMENT '失败原因',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ------------------------------------------------------------
-- t_normal_order — 普通订单表（立即购买/购物车结算）
-- 与t_seckill_order独立：有address_id字段，无seckill_id字段
-- 拆分total_amount/freight_amount/pay_amount，含remark字段
-- 包含SHIPPED状态及物流字段（shipping_company/shipping_no/ship_time/confirm_time）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_normal_order`;
CREATE TABLE `t_normal_order` (
    `id`               BIGINT        NOT NULL            COMMENT '主键ID',
    `order_no`         VARCHAR(32)   NOT NULL            COMMENT '订单号（唯一，时间戳+随机串）',
    `user_id`          BIGINT        NOT NULL            COMMENT '下单用户ID',
    `address_id`       BIGINT        NOT NULL            COMMENT '收货地址ID',
    `total_amount`     DECIMAL(10,2) NOT NULL            COMMENT '商品总金额（明细小计之和）',
    `freight_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_amount`       DECIMAL(10,2) NOT NULL            COMMENT '实付金额 = total_amount + freight_amount',
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
    `is_deleted`       TINYINT       NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间（下单时间）',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_user_create` (`user_id`, `create_time`),
    INDEX `idx_shipping_no` (`shipping_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单表（立即购买/购物车结算）';

-- ------------------------------------------------------------
-- t_normal_order_item — 普通订单明细表
-- 一个订单对应多个明细行；冗余商品名称/主图/单价保留下单时刻快照
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_normal_order_item`;
CREATE TABLE `t_normal_order_item` (
    `id`            BIGINT        NOT NULL            COMMENT '主键ID',
    `order_id`      BIGINT        NOT NULL            COMMENT '所属普通订单ID',
    `product_id`    BIGINT        NOT NULL            COMMENT '商品ID',
    `product_name`  VARCHAR(200)  NOT NULL            COMMENT '商品名称（下单时快照）',
    `product_image` VARCHAR(255)  NULL     DEFAULT NULL COMMENT '商品主图URL（下单时快照）',
    `unit_price`    DECIMAL(10,2) NOT NULL            COMMENT '商品单价（下单时快照，原价）',
    `quantity`      INT           NOT NULL DEFAULT 1  COMMENT '购买数量',
    `subtotal`      DECIMAL(10,2) NOT NULL            COMMENT '小计金额 = unit_price × quantity',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通订单明细表';

-- ------------------------------------------------------------
-- t_operation_log — 操作日志表（后台）
-- 管理员操作审计追踪，append-only设计（无update_time/is_deleted）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
    `id`          BIGINT       NOT NULL                COMMENT '主键ID',
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

-- ------------------------------------------------------------
-- t_product — 商品表
-- 商品基本信息、库存与展示数据，主键使用AUTO_INCREMENT
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`           VARCHAR(200)  NOT NULL               COMMENT '商品名称',
    `description`    TEXT          NULL     DEFAULT NULL   COMMENT '商品简介（纯文本）',
    `original_price` DECIMAL(10,2) NOT NULL               COMMENT '原价（元）',
    `stock`          INT           NOT NULL DEFAULT 0     COMMENT '普通库存数量',
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
-- t_product_review — 商品评论表
-- 用户对商品的评价，支持评分、图片、商家回复
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_review`;
CREATE TABLE `t_product_review` (
    `id`           BIGINT       NOT NULL                COMMENT '主键ID（雪花算法）',
    `product_id`   BIGINT       NOT NULL                COMMENT '商品ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '评论用户ID',
    `order_id`     BIGINT       NULL     DEFAULT NULL   COMMENT '关联订单ID（可选）',
    `content`      TEXT         NOT NULL                COMMENT '评论内容',
    `rating`       TINYINT      NOT NULL DEFAULT 5      COMMENT '评分：1-5星',
    `images`       JSON         NULL     DEFAULT NULL   COMMENT '评论图片URL数组',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-显示/0-隐藏',
    `reply_content` TEXT        NULL     DEFAULT NULL   COMMENT '商家回复内容',
    `reply_time`   DATETIME     NULL     DEFAULT NULL   COMMENT '回复时间',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品评论表';

-- ------------------------------------------------------------
-- t_recharge_card — 充值卡表
-- 预付费充值卡，支持批次管理，卡密BCrypt加密存储
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_recharge_card`;
CREATE TABLE `t_recharge_card` (
    `id`           BIGINT        NOT NULL               COMMENT '主键ID（雪花算法）',
    `card_no`      VARCHAR(32)   NOT NULL               COMMENT '卡号(唯一)',
    `card_password` VARCHAR(64)  NOT NULL               COMMENT '卡密(BCrypt加密存储)',
    `face_value`   DECIMAL(10,2) NOT NULL               COMMENT '面额',
    `status`       ENUM('UNUSED','USED','DISABLED') NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/DISABLED-已禁用',
    `used_by`      BIGINT        NULL     DEFAULT NULL  COMMENT '使用者用户ID',
    `used_time`    DATETIME      NULL     DEFAULT NULL  COMMENT '使用时间',
    `batch_no`     VARCHAR(32)   NOT NULL               COMMENT '批次号',
    `is_deleted`   TINYINT       NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_card_no` (`card_no`),
    INDEX `idx_batch_no` (`batch_no`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='充值卡表';

-- ------------------------------------------------------------
-- t_seckill_goods — 秒杀活动表
-- 管理秒杀商品、库存与活动时间窗口，含活动名称/限购/图片/描述
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_goods`;
CREATE TABLE `t_seckill_goods` (
    `id`              BIGINT        NOT NULL            COMMENT '主键ID',
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
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status_start` (`status`, `start_time`),
    INDEX `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动表';

-- ------------------------------------------------------------
-- t_seckill_order — 秒杀订单表
-- 核心交易表，含一人一单唯一约束
-- 包含SHIPPED状态及物流字段（shipping_company/shipping_no/ship_time/confirm_time）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_order`;
CREATE TABLE `t_seckill_order` (
    `id`               BIGINT        NOT NULL            COMMENT '主键ID',
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
-- t_user — 用户表
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
-- t_user_address — 收货地址表
-- 用户收货信息管理，支持默认地址设置
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_address`;
CREATE TABLE `t_user_address` (
    `id`             BIGINT       NOT NULL             COMMENT '主键ID',
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
-- t_user_coupon — 用户优惠券表
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
    INDEX `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

-- ------------------------------------------------------------
-- t_user_favorite — 用户收藏夹表
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
    UNIQUE INDEX `uk_user_product` (`user_id`, `product_id`) COMMENT '用户+商品唯一约束',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收藏夹表';

SET FOREIGN_KEY_CHECKS = 1;