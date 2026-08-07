-- =====================================================================
-- 秒杀活动场次化重构迁移脚本
-- 创建时间: 2026-08-07
-- 说明: 新增 t_seckill_activity 场次表，t_seckill_goods 增加 activity_id 关联场次
-- M-K4 修复：补 COLLATE=utf8mb4_general_ci；主键改 BIGINT NOT NULL；对齐 status 类型
-- =====================================================================

-- 1. 创建秒杀活动场次表
CREATE TABLE IF NOT EXISTS `t_seckill_activity` (
  -- M-K4 修复：主键改 BIGINT NOT NULL（与项目雪花算法一致，去掉 AUTO_INCREMENT）
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花算法）',
  `name` VARCHAR(128) NOT NULL COMMENT '场次名称',
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  -- M-K4 修复：status 用 TINYINT 与项目其他表对齐（0=待开始 1=进行中 2=已结束）
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待开始 1=进行中 2=已结束',
  `per_limit` INT NOT NULL DEFAULT 1,
  `description` VARCHAR(512),
  `images` VARCHAR(1024),
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_time` (`start_time`, `end_time`)
-- M-K4 修复：补 COLLATE=utf8mb4_general_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动场次表';

-- 2. t_seckill_goods 增加场次关联字段
ALTER TABLE t_seckill_goods ADD COLUMN activity_id BIGINT COMMENT '关联场次ID';
ALTER TABLE t_seckill_goods ADD INDEX idx_activity_id (activity_id);
