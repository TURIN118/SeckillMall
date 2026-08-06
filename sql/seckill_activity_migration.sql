-- =====================================================================
-- 秒杀活动场次化重构迁移脚本
-- 创建时间: 2026-08-07
-- 说明: 新增 t_seckill_activity 场次表，t_seckill_goods 增加 activity_id 关联场次
-- =====================================================================

-- 1. 创建秒杀活动场次表
CREATE TABLE IF NOT EXISTS t_seckill_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL COMMENT '场次名称',
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待开始 1=进行中 2=已结束',
  per_limit INT NOT NULL DEFAULT 1,
  description VARCHAR(512),
  images VARCHAR(1024),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动场次表';

-- 2. t_seckill_goods 增加场次关联字段
ALTER TABLE t_seckill_goods ADD COLUMN activity_id BIGINT COMMENT '关联场次ID';
ALTER TABLE t_seckill_goods ADD INDEX idx_activity_id (activity_id);