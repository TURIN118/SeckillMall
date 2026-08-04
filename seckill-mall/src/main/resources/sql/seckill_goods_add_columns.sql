-- ============================================================
-- 秒杀活动表扩展字段迁移脚本 (seckill_goods_add_columns.sql)
-- ============================================================
-- 说明：为 t_seckill_goods 表新增秒杀活动名称、每人限购数量、活动图片、活动描述 4 个字段
-- 背景：原表未单独存储这些字段，创建秒杀活动时这 4 个字段被静默丢弃
-- 执行前请先确认 t_seckill_goods 表已创建（见 schema.sql）
-- ============================================================

-- 秒杀活动名称
ALTER TABLE `t_seckill_goods` ADD COLUMN `seckill_name` VARCHAR(255) DEFAULT NULL COMMENT '秒杀活动名称';

-- 每人限购数量（默认 1 件，保持与历史一人一单逻辑兼容）
ALTER TABLE `t_seckill_goods` ADD COLUMN `per_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限购数量';

-- 活动图片（JSON 数组字符串，如 ["url1","url2"]）
ALTER TABLE `t_seckill_goods` ADD COLUMN `images` TEXT DEFAULT NULL COMMENT '活动图片(JSON数组)';

-- 活动描述
ALTER TABLE `t_seckill_goods` ADD COLUMN `description` TEXT DEFAULT NULL COMMENT '活动描述';

-- ------------------------------------------------------------
-- 验证（执行后可运行以下查询验证）
-- ------------------------------------------------------------
-- DESC t_seckill_goods;
-- SELECT seckill_name, per_limit, images, description FROM t_seckill_goods LIMIT 5;