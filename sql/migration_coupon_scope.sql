-- ============================================================
-- 优惠券功能数据库迁移脚本
-- 日期: 2026-08-09
-- 说明: 为优惠券添加适用范围字段(scope_type/category_id/product_id)
--       为普通订单添加优惠券字段(user_coupon_id/discount_amount)
-- ============================================================

-- 1. t_coupon 表新增字段：优惠券适用范围
ALTER TABLE `t_coupon` ADD COLUMN `scope_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ALL' COMMENT '适用范围：ALL-全站/CATEGORY-分类/PRODUCT-商品' AFTER `status`;
ALTER TABLE `t_coupon` ADD COLUMN `category_id` bigint NULL DEFAULT NULL COMMENT '适用分类ID（scope_type=CATEGORY时有效）' AFTER `scope_type`;
ALTER TABLE `t_coupon` ADD COLUMN `product_id` bigint NULL DEFAULT NULL COMMENT '适用商品ID（scope_type=PRODUCT时有效）' AFTER `category_id`;

-- 为 scope_type 添加索引
ALTER TABLE `t_coupon` ADD INDEX `idx_scope`(`scope_type` ASC, `category_id` ASC, `product_id` ASC) USING BTREE;

-- 2. t_normal_order 表新增字段：优惠券使用记录
ALTER TABLE `t_normal_order` ADD COLUMN `user_coupon_id` bigint NULL DEFAULT NULL COMMENT '使用的用户优惠券ID' AFTER `remark`;
ALTER TABLE `t_normal_order` ADD COLUMN `discount_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额' AFTER `user_coupon_id`;

-- 为 user_coupon_id 添加索引
ALTER TABLE `t_normal_order` ADD INDEX `idx_user_coupon`(`user_coupon_id` ASC) USING BTREE;

-- 3. 将现有优惠券的 scope_type 设置为 ALL
UPDATE `t_coupon` SET `scope_type` = 'ALL' WHERE `scope_type` IS NULL OR `scope_type` = '';