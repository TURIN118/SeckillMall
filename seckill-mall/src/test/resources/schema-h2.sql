-- ============================================================
-- H2 兼容版表结构（MySQL 兼容模式）：仅供 Mapper 切片测试使用
-- 仅包含 UserMapper / SeckillOrderMapper 测试所需的两张表
-- 枚举字段统一使用 VARCHAR，由 MyBatis-Plus IEnum 类型处理器转换
-- ============================================================

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT        NOT NULL,
    username    VARCHAR(50)   NOT NULL,
    password    VARCHAR(100)  NOT NULL,
    phone       VARCHAR(20)   DEFAULT NULL,
    email       VARCHAR(100)  DEFAULT NULL,
    nickname    VARCHAR(50)   DEFAULT NULL,
    avatar_url  VARCHAR(255)  DEFAULT NULL,
    role        VARCHAR(20)   NOT NULL DEFAULT 'BUYER',
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    is_deleted  TINYINT       NOT NULL DEFAULT 0,
    create_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_username_h2 UNIQUE (username),
    CONSTRAINT uk_phone_h2    UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS t_seckill_order (
    id              BIGINT        NOT NULL,
    order_no        VARCHAR(32)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    seckill_id      BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    seckill_price   DECIMAL(10,2) NOT NULL,
    quantity        INT           NOT NULL DEFAULT 1,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    pay_time        TIMESTAMP     DEFAULT NULL,
    pay_expire_time TIMESTAMP     DEFAULT NULL,
    transaction_id  VARCHAR(64)   DEFAULT NULL,
    pay_method      VARCHAR(20)   DEFAULT NULL,
    cancel_time     TIMESTAMP     DEFAULT NULL,
    cancel_reason   VARCHAR(255)  DEFAULT NULL,
    is_deleted      TINYINT       NOT NULL DEFAULT 0,
    create_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_no_h2      UNIQUE (order_no),
    CONSTRAINT uk_user_seckill_h2  UNIQUE (user_id, seckill_id)
);
