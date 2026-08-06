-- ============================================================
-- SeckillMall 数据库迁移脚本 V7：SKU 多规格商品系统
-- 文件：migration_v7_sku.sql
--
-- 迁移目的：
--   1. 新增分类属性模板表 t_category_attribute / t_category_attribute_value
--   2. 新增 SKU 相关表 t_product_attribute / t_product_attribute_value / t_product_sku
--   3. 现有表变更：
--      - t_cart              新增 sku_id (NOT NULL DEFAULT 0)，调整唯一约束为 (user_id, product_id, sku_id)
--      - t_normal_order_item 新增 sku_id / sku_attributes（下单快照）
--      - t_product_review    新增 sku_id / sku_attributes（评论快照）
--      - t_product           新增 min_price / max_price / total_stock（SKU 聚合冗余字段）
--
-- ⚠️  请手动执行此脚本，不会自动执行
-- 执行前请确认数据库连接和权限，并备份 t_cart / t_normal_order_item / t_product_review / t_product 四张表
--
-- 环境要求：MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID，BIGINT）
--
-- 执行顺序（依赖关系）：
--   t_category_attribute      → t_category_attribute_value
--   → t_product_attribute     → t_product_attribute_value → t_product_sku
--   → ALTER t_cart            → ALTER t_normal_order_item → ALTER t_product_review
--   → ALTER t_product
-- ============================================================


-- ============================================================
-- 一、新增表：分类规格模板
-- ============================================================

-- ------------------------------------------------------------
-- 1.0 分类属性模板表 t_category_attribute
--    存储分类级别的规格维度模板定义，如「智能手表」分类下定义「颜色 / 版本 / 表带」。
--    商家添加商品时选择分类后自动带出该分类的属性模板，避免每个商品重复配置。
--    使用 CREATE TABLE IF NOT EXISTS 避免重复执行报错。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_category_attribute` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID',
    `category_id` BIGINT       NOT NULL              COMMENT '所属分类ID',
    `name`        VARCHAR(50)  NOT NULL              COMMENT '属性名称（如：颜色、版本、表带）',
    `type`        ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：TEXT-文字/IMAGE-图片(色块)',
    `input_type`  ENUM('SELECT','INPUT') NOT NULL DEFAULT 'SELECT' COMMENT '录入方式：SELECT-从预设值选/INPUT-自由输入',
    `is_required` TINYINT      NOT NULL DEFAULT 0    COMMENT '是否必选：0-否/1-是',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性模板表';

-- ------------------------------------------------------------
-- 1.1 分类属性预设值表 t_category_attribute_value
--    存储分类属性模板的预设可选值，如「颜色」属性模板下预设「曜石黑 / 银月白 / 褚霞红」。
--    使用 CREATE TABLE IF NOT EXISTS 避免重复执行报错。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_category_attribute_value` (
    `id`           BIGINT       NOT NULL              COMMENT '主键ID',
    `attribute_id` BIGINT       NOT NULL              COMMENT '所属分类属性ID',
    `value`        VARCHAR(100) NOT NULL              COMMENT '预设值（如：曜石黑、标准版）',
    `image_url`    VARCHAR(255) DEFAULT NULL          COMMENT '图片类型时的色块/图片URL',
    `sort_order`   INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attribute` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性预设值表';


-- ============================================================
-- 二、新增表：SKU 相关表
-- ============================================================

-- ------------------------------------------------------------
-- 2.0 商品属性表 t_product_attribute（含 category_attribute_id 字段）
--    存储商品的规格维度定义，如「颜色」「版本」「表带」。
--    category_attribute_id 关联分类属性模板（NULL=商品自定义属性，非 NULL=来自分类模板）。
--    使用 CREATE TABLE IF NOT EXISTS 避免重复执行报错。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_product_attribute` (
    `id`                    BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `product_id`            BIGINT       NOT NULL             COMMENT '所属商品ID',
    `category_attribute_id` BIGINT       DEFAULT NULL         COMMENT '关联分类属性模板ID（NULL=商品自定义属性）',
    `name`                  VARCHAR(50)  NOT NULL             COMMENT '属性名（如：颜色/版本/表带）',
    `type`                  ENUM('IMAGE','TEXT') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：IMAGE-图片型/TEXT-文字型',
    `sort_order`            INT          NOT NULL DEFAULT 0   COMMENT '排序权重（值越小越靠前）',
    `is_deleted`            TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_category_attribute_id` (`category_attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性表（SKU维度定义）';

-- ------------------------------------------------------------
-- 2.1 商品属性值表 t_product_attribute_value
--    存储每个属性下的可选值，如「颜色」下的「曜石黑」「银月白」等。
--    product_id 冗余字段，加速「按商品查所有属性值」场景。
--    使用 CREATE TABLE IF NOT EXISTS 避免重复执行报错。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_product_attribute_value` (
    `id`           BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `attribute_id` BIGINT       NOT NULL             COMMENT '所属属性ID',
    `product_id`   BIGINT       NOT NULL             COMMENT '所属商品ID（冗余，加速查询）',
    `value`        VARCHAR(100) NOT NULL             COMMENT '属性值（如：曜石黑/旗舰版/氟橡胶表带）',
    `image_url`    VARCHAR(255) DEFAULT NULL         COMMENT '图片型属性值的色块URL或图片URL（文字型为NULL）',
    `sort_order`   INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attribute_id` (`attribute_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性值表（SKU维度可选值）';

-- ------------------------------------------------------------
-- 2.2 商品SKU表 t_product_sku（含联合索引 idx_product_status_stock）
--    存储每个属性值组合的独立价格 / 库存 / 图片 / 编码 / 状态。
--    - sku_code: NOT NULL，由 generateSkuCode 可靠生成或后台手填，同商品内唯一。
--    - status:   是否在售（1-在售/0-停售），与库存无关。stock=0 时 status 仍为 1。
--    - attributes: JSON 字符串，记录该 SKU 的属性键值对，便于订单/评论快照。
--    - idx_product_status_stock: 联合索引，快速查询「某商品下库存大于 0 的启用 SKU」
--      （SQL: WHERE product_id = ? AND status = 1 AND stock > 0），前台展示可选规格时直接命中。
--    使用 CREATE TABLE IF NOT EXISTS 避免重复执行报错。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_product_sku` (
    `id`          BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `product_id`  BIGINT        NOT NULL            COMMENT '所属商品ID',
    `sku_code`    VARCHAR(64)   NOT NULL            COMMENT 'SKU编码（同商品内唯一，由generateSkuCode可靠生成或后台手填，禁止NULL）',
    `price`       DECIMAL(10,2) NOT NULL            COMMENT 'SKU价格（元）',
    `stock`       INT           NOT NULL DEFAULT 0  COMMENT 'SKU库存',
    `main_image`  VARCHAR(255)  DEFAULT NULL        COMMENT 'SKU主图URL（NULL表示沿用商品主图）',
    `attributes`  JSON          NOT NULL            COMMENT 'SKU属性键值对JSON，如{"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}',
    `status`      TINYINT       NOT NULL DEFAULT 1  COMMENT '是否在售：1-在售/0-停售（与库存无关，库存为0时status仍为1，前端因stock=0置灰）',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`product_id`, `sku_code`) COMMENT '同商品内SKU编码唯一',
    KEY `idx_product_id` (`product_id`),
    KEY `idx_product_status` (`product_id`, `status`),
    KEY `idx_product_status_stock` (`product_id`, `status`, `stock`) COMMENT '快速查询某商品下库存大于0的启用SKU（前台展示可选规格）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品SKU表（属性值组合）';


-- ============================================================
-- 三、现有表修改（ALTER）
-- ⚠️ MySQL 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 语法，
--    若脚本重复执行会报错。请确保仅在未迁移的环境执行；
--    若需重复执行，请先检查字段/索引是否已存在。
-- ============================================================

-- ------------------------------------------------------------
-- 3.0 t_cart 新增 sku_id 并修改唯一约束（建议8：NOT NULL DEFAULT 0）
--    sku_id 设为 NOT NULL DEFAULT 0，用 0 代表「无规格商品」，而非 NULL。
--    这样 (user_id, product_id, sku_id) 三列唯一约束可完美工作，
--    应用层无需为 NULL 做特殊处理（MySQL 中 NULL 不参与唯一性比较）。
-- ------------------------------------------------------------
-- 新增 sku_id 字段（NOT NULL DEFAULT 0，0 代表无规格商品）
ALTER TABLE `t_cart` ADD COLUMN `sku_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'SKU ID（0表示无规格商品，非0为具体SKU）' AFTER `product_id`;

-- 删除原唯一约束 uk_user_product，改为 (user_id, product_id, sku_id) 三列唯一
ALTER TABLE `t_cart` DROP INDEX `uk_user_product`;
ALTER TABLE `t_cart` ADD UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`) COMMENT '用户+商品+SKU唯一约束（sku_id=0表示无规格，完美工作）';

-- 新增索引加速按 SKU 查询
ALTER TABLE `t_cart` ADD KEY `idx_sku_id` (`sku_id`);

-- ------------------------------------------------------------
-- 3.1 t_normal_order_item 新增 sku_id / sku_attributes（下单快照）
--    sku_attributes 用 VARCHAR(500) 存储可读字符串快照（如「曜石黑 / 旗舰版 / 氟橡胶表带」），
--    而非 JSON，因为订单展示只需可读文本，且历史订单不受后续 SKU 修改影响。
-- ------------------------------------------------------------
ALTER TABLE `t_normal_order_item` ADD COLUMN `sku_id`         BIGINT       DEFAULT NULL COMMENT 'SKU ID（下单时快照，NULL表示无规格）' AFTER `product_id`;
ALTER TABLE `t_normal_order_item` ADD COLUMN `sku_attributes` VARCHAR(500) DEFAULT NULL COMMENT 'SKU属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带）' AFTER `sku_id`;
ALTER TABLE `t_normal_order_item` ADD KEY `idx_sku_id` (`sku_id`);

-- ------------------------------------------------------------
-- 3.2 t_product_review 新增 sku_id / sku_attributes（评论快照）
-- ------------------------------------------------------------
ALTER TABLE `t_product_review` ADD COLUMN `sku_id`         BIGINT       DEFAULT NULL COMMENT 'SKU ID（NULL表示无规格评论）' AFTER `product_id`;
ALTER TABLE `t_product_review` ADD COLUMN `sku_attributes` VARCHAR(500) DEFAULT NULL COMMENT 'SKU属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带）' AFTER `sku_id`;
ALTER TABLE `t_product_review` ADD KEY `idx_sku_id` (`sku_id`);

-- ------------------------------------------------------------
-- 3.3 t_product 新增 SKU 聚合冗余字段（建议3）
--    有 SKU 时由 Service 层维护 min_price / max_price / total_stock。
--    original_price / stock 保留原语义（无 SKU 商品的价格 / 库存），
--    有 SKU 时不再作为兜底默认值，列表页/详情页有 SKU 时不读取。
--    无 SKU 的旧商品三个字段为 NULL，向后兼容无需迁移历史数据。
-- ------------------------------------------------------------
ALTER TABLE `t_product` ADD COLUMN `min_price`    DECIMAL(10,2) DEFAULT NULL COMMENT 'SKU最低价（有SKU时由Service层维护）' AFTER `stock`;
ALTER TABLE `t_product` ADD COLUMN `max_price`    DECIMAL(10,2) DEFAULT NULL COMMENT 'SKU最高价（有SKU时由Service层维护）' AFTER `min_price`;
ALTER TABLE `t_product` ADD COLUMN `total_stock`  INT           DEFAULT NULL COMMENT 'SKU库存汇总（有SKU时由Service层维护）' AFTER `max_price`;

-- ============================================================
-- 迁移完成
-- 执行后请校验：
--   1. 五张新表已创建：t_category_attribute / t_category_attribute_value
--      / t_product_attribute / t_product_attribute_value / t_product_sku
--   2. t_cart 唯一约束已变更为 uk_user_product_sku(user_id, product_id, sku_id)
--   3. t_normal_order_item / t_product_review 已含 sku_id / sku_attributes 字段
--   4. t_product 已含 min_price / max_price / total_stock 字段
-- ============================================================