-- ============================================================
-- migration_v6_shipping.sql
-- 发货功能：新增物流信息字段 + SHIPPED状态
-- ============================================================

-- 1. t_seckill_order: 修改status枚举，新增物流字段
ALTER TABLE t_seckill_order 
  MODIFY COLUMN status ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED') 
  NOT NULL DEFAULT 'UNPAID' COMMENT '订单状态',
  ADD COLUMN shipping_company VARCHAR(50) DEFAULT NULL COMMENT '物流公司' AFTER status,
  ADD COLUMN shipping_no VARCHAR(64) DEFAULT NULL COMMENT '快递单号' AFTER shipping_company,
  ADD COLUMN ship_time DATETIME DEFAULT NULL COMMENT '发货时间' AFTER shipping_no,
  ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT '确认收货时间' AFTER ship_time;

-- 2. t_normal_order: 同上
ALTER TABLE t_normal_order 
  MODIFY COLUMN status ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED') 
  NOT NULL DEFAULT 'UNPAID' COMMENT '订单状态',
  ADD COLUMN shipping_company VARCHAR(50) DEFAULT NULL COMMENT '物流公司' AFTER status,
  ADD COLUMN shipping_no VARCHAR(64) DEFAULT NULL COMMENT '快递单号' AFTER shipping_company,
  ADD COLUMN ship_time DATETIME DEFAULT NULL COMMENT '发货时间' AFTER shipping_no,
  ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT '确认收货时间' AFTER ship_time;

-- 3. 为快递单号创建索引（便于用户通过单号查询）
ALTER TABLE t_seckill_order ADD INDEX idx_shipping_no (shipping_no);
ALTER TABLE t_normal_order ADD INDEX idx_shipping_no (shipping_no);