-- ============================================================
-- 高并发秒杀电商平台 - 初始化种子数据
-- 密码均使用 BCrypt(strength=10) 加密
--   admin  / admin123  → 管理员
--   buyer01/ buyer123   → 买家
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户数据（admin/admin123、buyer01/buyer123）
-- ------------------------------------------------------------
INSERT INTO `t_user`
  (`id`, `username`, `password`, `phone`, `email`, `nickname`, `role`, `status`, `is_deleted`)
VALUES
  (1, 'admin', '$2a$10$EjQihKeAPFfMKPEuIp59OOK/2t2pss7114YVnk.xnZyXLi2bDvgKu',
   '13800000000', 'admin@seckill.com', '系统管理员', 'ADMIN', 'ACTIVE', 0),
  (2, 'buyer01', '$2a$10$W7HMDBLUe2KqbQ7lHuHVQO/ISol4Y/VZyLjF2PDcOf7RrflnqBFki',
   '13900000001', NULL, '测试买家', 'BUYER', 'ACTIVE', 0);

-- ------------------------------------------------------------
-- 2. 商品分类数据：4 个一级分类 + 16 个二级分类
-- ------------------------------------------------------------
-- 一级分类
INSERT INTO `t_category` (`id`, `name`, `parent_id`, `sort_order`, `status`, `is_deleted`)
VALUES
  (100, '手机数码', NULL, 1, 1, 0),
  (200, '电脑办公', NULL, 2, 1, 0),
  (300, '家用电器', NULL, 3, 1, 0),
  (400, '服饰鞋包', NULL, 4, 1, 0);

-- 二级分类（每级 4 个）
INSERT INTO `t_category` (`id`, `name`, `parent_id`, `sort_order`, `status`, `is_deleted`)
VALUES
  -- 手机数码 子分类
  (101, '智能手机', 100, 1, 1, 0),
  (102, '手机配件', 100, 2, 1, 0),
  (103, '智能手表', 100, 3, 1, 0),
  (104, '蓝牙耳机', 100, 4, 1, 0),
  -- 电脑办公 子分类
  (201, '笔记本电脑', 200, 1, 1, 0),
  (202, '台式机',     200, 2, 1, 0),
  (203, '显示器',     200, 3, 1, 0),
  (204, '键盘鼠标',   200, 4, 1, 0),
  -- 家用电器 子分类
  (301, '空调',       300, 1, 1, 0),
  (302, '洗衣机',     300, 2, 1, 0),
  (303, '冰箱',       300, 3, 1, 0),
  (304, '厨房电器',   300, 4, 1, 0),
  -- 服饰鞋包 子分类
  (401, '男装',       400, 1, 1, 0),
  (402, '女装',       400, 2, 1, 0),
  (403, '运动鞋',     400, 3, 1, 0),
  (404, '箱包',       400, 4, 1, 0);

-- ------------------------------------------------------------
-- 3. 示例商品数据（5 条，关联二级分类）
-- ------------------------------------------------------------
INSERT INTO `t_product`
  (`id`, `name`, `description`, `original_price`, `stock`, `sales_count`,
   `category_id`, `main_image`, `images`, `status`, `is_deleted`)
VALUES
  (1001, 'iPhone 15 Pro Max 256GB',
   'A17 Pro芯片，4800万像素主摄，钛金属设计',
   9999.00, 500, 1280, 101,
   '/images/products/iphone15pm.jpg',
   '["/images/products/iphone15pm_1.jpg","/images/products/iphone15pm_2.jpg"]',
   'ON_SALE', 0),

  (1002, 'MacBook Pro 14英寸 M3芯片',
   'M3 Pro芯片，18GB统一内存，512GB SSD',
   14999.00, 200, 560, 201,
   '/images/products/macbook_m3.jpg',
   '["/images/products/macbook_m3_1.jpg","/images/products/macbook_m3_2.jpg"]',
   'ON_SALE', 0),

  (1003, 'AirPods Pro 2 第二代',
   '自适应降噪，个性化空间音频，USB-C充电',
   1899.00, 1000, 3200, 104,
   '/images/products/airpods_pro2.jpg',
   '["/images/products/airpods_pro2_1.jpg"]',
   'ON_SALE', 0),

  (1004, '格力空调 1.5匹 新一级能效',
   '变频冷暖，新一级能效，自清洁',
   3999.00, 300, 890, 301,
   '/images/products/gree_ac.jpg',
   '["/images/products/gree_ac_1.jpg"]',
   'ON_SALE', 0),

  (1005, 'Nike Air Max 270 运动鞋',
   '轻量透气，Max Air气垫，经典配色',
   899.00, 600, 2100, 403,
   '/images/products/nike_am270.jpg',
   '["/images/products/nike_am270_1.jpg","/images/products/nike_am270_2.jpg"]',
   'ON_SALE', 0);

-- ------------------------------------------------------------
-- 4. 示例秒杀活动数据（2 条：ACTIVE 进行中 + PENDING 待开始）
-- ------------------------------------------------------------
INSERT INTO `t_seckill_goods`
  (`id`, `product_id`, `seckill_price`, `stock_count`, `available_count`,
   `start_time`, `end_time`, `status`, `creator_id`, `is_deleted`)
VALUES
  -- 活动一：进行中 - iPhone 15 Pro Max 秒杀
  (5001, 1001, 5999.00, 100, 58,
   '2025-01-15 10:00:00', '2025-01-15 12:00:00',
   'ACTIVE', 1, 0),

  -- 活动二：待开始 - AirPods Pro 2 秒杀
  (5002, 1003, 999.00, 200, 200,
   '2025-02-01 14:00:00', '2025-02-01 16:00:00',
   'PENDING', 1, 0);
