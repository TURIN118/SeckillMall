-- ============================================================
-- 高并发秒杀电商平台 - 数据库表结构 DDL
-- MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID）
-- 执行顺序：t_user → t_category → t_product → t_seckill_goods
--           → t_seckill_order → t_user_address → t_login_log → t_operation_log
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表：存储平台所有用户（买家/卖家/管理员）
-- ------------------------------------------------------------
CREATE TABLE `t_user` (
    `id`          BIGINT        NOT NULL                 COMMENT '主键ID（雪花算法）',
    `username`    VARCHAR(50)   NOT NULL                 COMMENT '用户名（唯一）',
    `password`    VARCHAR(100)  NOT NULL                 COMMENT '密码（BCrypt加密）',
    `phone`       VARCHAR(20)   DEFAULT NULL             COMMENT '手机号（唯一）',
    `email`       VARCHAR(100)  DEFAULT NULL             COMMENT '邮箱地址',
    `nickname`    VARCHAR(50)   DEFAULT NULL             COMMENT '用户昵称',
    `avatar_url`  VARCHAR(255)  DEFAULT NULL             COMMENT '头像URL',
    `role`        ENUM('BUYER','SELLER','ADMIN')
                                NOT NULL DEFAULT 'BUYER' COMMENT '角色：BUYER-买家/SELLER-卖家/ADMIN-管理员',
    `status`      ENUM('ACTIVE','DISABLED')
                                NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常/DISABLED-禁用',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 商品分类表：支持多级分类（自关联 parent_id）
-- ------------------------------------------------------------
CREATE TABLE `t_category` (
    `id`          BIGINT        NOT NULL                 COMMENT '主键ID（雪花算法）',
    `name`        VARCHAR(50)   NOT NULL                 COMMENT '分类名称',
    `parent_id`   BIGINT        DEFAULT NULL             COMMENT '父分类ID（NULL=一级分类）',
    `sort_order`  INT           NOT NULL DEFAULT 0       COMMENT '排序权重（值越小越靠前）',
    `icon_url`    VARCHAR(255)  DEFAULT NULL             COMMENT '分类图标URL',
    `status`      TINYINT       NOT NULL DEFAULT 1       COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. 商品表：商品基本信息、库存与展示数据
-- ------------------------------------------------------------
CREATE TABLE `t_product` (
    `id`             BIGINT        NOT NULL              COMMENT '主键ID',
    `name`           VARCHAR(200)  NOT NULL              COMMENT '商品名称',
    `description`    TEXT          DEFAULT NULL          COMMENT '商品简介（纯文本）',
    `original_price` DECIMAL(10,2) NOT NULL              COMMENT '原价（元）',
    `stock`          INT           NOT NULL DEFAULT 0    COMMENT '普通库存数量',
    `sales_count`    INT           NOT NULL DEFAULT 0    COMMENT '累计销量',
    `category_id`    BIGINT        NOT NULL              COMMENT '所属分类ID',
    `images`         JSON          DEFAULT NULL          COMMENT '商品图片URL数组',
    `main_image`     VARCHAR(255)  DEFAULT NULL          COMMENT '主图URL',
    `detail_html`    TEXT          DEFAULT NULL          COMMENT '商品详情（富文本HTML）',
    `status`         ENUM('ON_SALE','OFF_SHELF')
                                 NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE-在售/OFF_SHELF-下架',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

-- ------------------------------------------------------------
-- 4. 秒杀活动表：管理秒杀商品、库存与活动时间窗口
-- ------------------------------------------------------------
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
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status_start` (`status`, `start_time`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动表';

-- ------------------------------------------------------------
-- 5. 秒杀订单表：核心交易表，含一人一单唯一约束
-- ------------------------------------------------------------
CREATE TABLE `t_seckill_order` (
    `id`              BIGINT        NOT NULL            COMMENT '主键ID',
    `order_no`        VARCHAR(32)   NOT NULL            COMMENT '订单号（唯一，时间戳+随机串）',
    `user_id`         BIGINT        NOT NULL            COMMENT '下单用户ID',
    `seckill_id`      BIGINT        NOT NULL            COMMENT '秒杀活动ID',
    `product_id`      BIGINT        NOT NULL            COMMENT '商品ID（冗余，减少JOIN）',
    `seckill_price`   DECIMAL(10,2) NOT NULL            COMMENT '下单时秒杀价（价格快照）',
    `quantity`        INT           NOT NULL DEFAULT 1  COMMENT '购买数量',
    `total_amount`    DECIMAL(10,2) NOT NULL            COMMENT '订单总金额（秒杀价×数量）',
    `status`          ENUM('UNPAID','PAID','CANCELLED','TIMEOUT','COMPLETED')
                                   NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-待支付/PAID-已支付/CANCELLED-已取消/TIMEOUT-超时/COMPLETED-已完成',
    `pay_time`        DATETIME      DEFAULT NULL        COMMENT '支付时间',
    `pay_expire_time` DATETIME      DEFAULT NULL        COMMENT '支付截止时间（超时自动取消）',
    `transaction_id`  VARCHAR(64)   DEFAULT NULL        COMMENT '第三方支付流水号',
    `pay_method`      VARCHAR(20)   DEFAULT NULL        COMMENT '支付方式（ALIPAY/WECHAT等）',
    `cancel_time`     DATETIME      DEFAULT NULL        COMMENT '取消时间',
    `cancel_reason`   VARCHAR(255)  DEFAULT NULL        COMMENT '取消原因',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间（下单时间）',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_seckill` (`user_id`, `seckill_id`) COMMENT '一人一单约束（核心）',
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_seckill_status` (`seckill_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀订单表';

-- ------------------------------------------------------------
-- 6. 收货地址表：用户收货信息管理
-- ------------------------------------------------------------
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
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收货地址表';

-- ------------------------------------------------------------
-- 7. 登录日志表：记录用户登录行为（append-only，无 update_time/is_deleted）
-- ------------------------------------------------------------
CREATE TABLE `t_login_log` (
    `id`             BIGINT       NOT NULL             COMMENT '主键ID',
    `user_id`        BIGINT       NOT NULL             COMMENT '用户ID',
    `login_ip`       VARCHAR(50)  NOT NULL             COMMENT '登录IP地址',
    `login_location` VARCHAR(100) DEFAULT NULL         COMMENT '登录地点（IP解析城市）',
    `user_agent`     VARCHAR(500) DEFAULT NULL         COMMENT '浏览器User-Agent',
    `login_result`   ENUM('SUCCESS','FAILED')
                                 NOT NULL              COMMENT '登录结果：SUCCESS-成功/FAILED-失败',
    `fail_reason`    VARCHAR(255) DEFAULT NULL         COMMENT '失败原因',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ------------------------------------------------------------
-- 8. 操作日志表（后台）：管理员操作审计追踪（append-only）
-- ------------------------------------------------------------
CREATE TABLE `t_operation_log` (
    `id`          BIGINT       NOT NULL             COMMENT '主键ID',
    `operator_id` BIGINT       NOT NULL             COMMENT '操作人ID',
    `module`      VARCHAR(50)  NOT NULL             COMMENT '操作模块（商品管理/用户管理/秒杀管理等）',
    `action`      VARCHAR(50)  NOT NULL             COMMENT '操作类型（CREATE/UPDATE/DELETE/EXPORT等）',
    `target_id`   VARCHAR(64)  DEFAULT NULL         COMMENT '操作目标ID',
    `target_type` VARCHAR(50)  DEFAULT NULL         COMMENT '操作目标类型（PRODUCT/SECKILL/USER等）',
    `detail`      TEXT         DEFAULT NULL         COMMENT '操作详情（JSON变更快照）',
    `ip_address`  VARCHAR(50)  DEFAULT NULL         COMMENT '操作IP地址',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator_id`, `create_time`),
    KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表（后台）';
