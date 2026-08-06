/*
 Navicat Premium Dump SQL

 Source Server         : 本地-docker
 Source Server Type    : MySQL
 Source Server Version : 90600 (9.6.0)
 Source Host           : localhost:3307
 Source Schema         : seckill_mall

 Target Server Type    : MySQL
 Target Server Version : 90600 (9.6.0)
 File Encoding         : 65001

 Date: 06/08/2026 11:16:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_banner
-- ----------------------------
DROP TABLE IF EXISTS `t_banner`;
CREATE TABLE `t_banner`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '轮播图标题',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图片URL',
  `link_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '点击跳转链接',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序权重（值越小越靠前）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用/0-禁用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '轮播图表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_banner
-- ----------------------------
INSERT INTO `t_banner` VALUES (2084176345211932674, '', '/images/products/2026/08/03/e2d47612baab438c8c68c608bf878a15.jpg', '', 0, 1, 0, '2026-08-03 15:15:37', '2026-08-03 15:15:37');
INSERT INTO `t_banner` VALUES (2084219980917788674, '超级品牌日 / 爆品推荐', '/images/products/2026/08/03/917de0913a5845c581932ca9117bfb2f.png', '', 1, 1, 0, '2026-08-03 18:09:01', '2026-08-03 18:09:01');

-- ----------------------------
-- Table structure for t_cart
-- ----------------------------
DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '加购数量',
  `selected` tinyint NOT NULL DEFAULT 1 COMMENT '是否选中：0-否/1-是',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_product`(`user_id` ASC, `product_id` ASC) USING BTREE COMMENT '用户+商品唯一约束',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '购物车表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cart
-- ----------------------------
INSERT INTO `t_cart` VALUES (2084217735874629633, 2, 87, 2, 0, 0, '2026-08-03 18:00:06', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220370899980290, 2, 86, 1, 0, 0, '2026-08-03 18:10:34', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220374553219074, 2, 85, 1, 0, 0, '2026-08-03 18:10:35', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220379015958529, 2, 73, 1, 0, 0, '2026-08-03 18:10:36', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220384556634114, 2, 83, 1, 0, 0, '2026-08-03 18:10:37', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220386246938626, 2, 82, 1, 0, 0, '2026-08-03 18:10:38', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220388843212801, 2, 81, 1, 0, 0, '2026-08-03 18:10:38', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220392408371202, 2, 77, 1, 0, 0, '2026-08-03 18:10:39', '2026-08-03 15:07:54');
INSERT INTO `t_cart` VALUES (2084220394484551681, 2, 78, 1, 1, 1, '2026-08-03 18:10:40', '2026-08-04 01:59:10');
INSERT INTO `t_cart` VALUES (2084220397189877761, 2, 79, 1, 1, 1, '2026-08-03 18:10:40', '2026-08-04 01:59:10');
INSERT INTO `t_cart` VALUES (2084220399194755074, 2, 80, 1, 1, 1, '2026-08-03 18:10:41', '2026-08-04 01:59:10');

-- ----------------------------
-- Table structure for t_category
-- ----------------------------
DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父分类ID（NULL=一级分类）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序权重（值越小越靠前）',
  `icon_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类图标URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用/0-禁用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_category
-- ----------------------------
INSERT INTO `t_category` VALUES (100, '手机数码', NULL, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (101, '智能手机', 100, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (102, '手机配件', 100, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (103, '智能手表', 100, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (104, '蓝牙耳机', 100, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (105, '摄影摄像', 100, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (106, '电子教育', 100, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (200, '电脑办公', NULL, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (201, '笔记本电脑', 200, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (202, '台式机', 200, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (203, '显示器', 200, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (204, '键盘鼠标', 200, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (205, '打印耗材', 200, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (206, '电脑配件', 200, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (300, '家用电器', NULL, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (301, '空调', 300, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (302, '洗衣机', 300, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (303, '冰箱', 300, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (304, '厨房电器', 300, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (305, '生活电器', 300, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (306, '个护健康', 300, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (400, '服饰鞋包', NULL, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (401, '男装', 400, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (402, '女装', 400, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (403, '运动鞋', 400, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (404, '箱包', 400, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (405, '童装童鞋', 400, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (406, '内衣配饰', 400, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (500, '美妆个护', NULL, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (501, '面部护理', 500, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (502, '彩妆香水', 500, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (503, '身体护理', 500, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (504, '美发护发', 500, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (505, '男性护理', 500, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (506, '美容仪器', 500, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (600, '食品生鲜', NULL, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (601, '休闲零食', 600, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (602, '米面粮油', 600, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (603, '生鲜水果', 600, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (604, '酒水饮料', 600, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (605, '茶饮咖啡', 600, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (606, '营养保健', 600, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (700, '家居家纺', NULL, 7, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (701, '床品套件', 700, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (702, '被枕芯类', 700, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (703, '家居收纳', 700, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (704, '厨房用品', 700, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (705, '卫浴用品', 700, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (706, '家具装饰', 700, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (800, '运动户外', NULL, 8, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (801, '运动服饰', 800, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (802, '健身器材', 800, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (803, '户外装备', 800, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (804, '骑行装备', 800, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (805, '钓鱼用品', 800, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (806, '游泳装备', 800, 6, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (900, '图书文娱', NULL, 9, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (901, '图书杂志', 900, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (902, '文具用品', 900, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (903, '音乐影视', 900, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (904, '数字内容', 900, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (905, '收藏礼品', 900, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (1000, '母婴玩具', NULL, 10, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (1001, '婴儿用品', 1000, 1, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (1002, '玩具模型', 1000, 2, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (1003, '童书绘本', 1000, 3, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (1004, '孕产用品', 1000, 4, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');
INSERT INTO `t_category` VALUES (1005, '营养辅食', 1000, 5, NULL, 1, 0, '2026-08-03 01:41:32', '2026-08-03 01:41:32');

-- ----------------------------
-- Table structure for t_coupon
-- ----------------------------
DROP TABLE IF EXISTS `t_coupon`;
CREATE TABLE `t_coupon`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '优惠券名称',
  `type` enum('AMOUNT','DISCOUNT') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：AMOUNT-满减/DISCOUNT-折扣',
  `amount` decimal(10, 2) NOT NULL COMMENT '满减金额或折扣值(如0.85表示85折)',
  `min_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '最低消费金额',
  `total_count` int NOT NULL COMMENT '发放总数',
  `received_count` int NOT NULL DEFAULT 0 COMMENT '已领取数',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用数',
  `start_time` datetime NOT NULL COMMENT '有效期开始',
  `end_time` datetime NOT NULL COMMENT '有效期结束',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用/0-停用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '优惠券表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_coupon
-- ----------------------------

-- ----------------------------
-- Table structure for t_login_log
-- ----------------------------
DROP TABLE IF EXISTS `t_login_log`;
CREATE TABLE `t_login_log`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录IP地址',
  `login_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录地点（IP解析城市）',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浏览器User-Agent',
  `login_result` enum('SUCCESS','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录结果：SUCCESS-成功/FAILED-失败',
  `fail_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '登录日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_login_log
-- ----------------------------
INSERT INTO `t_login_log` VALUES (2083737755868774401, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 10:12:50');
INSERT INTO `t_login_log` VALUES (2083737755868774402, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 10:12:50');
INSERT INTO `t_login_log` VALUES (2083759245255299073, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 11:38:13');
INSERT INTO `t_login_log` VALUES (2083759245255299074, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 11:38:13');
INSERT INTO `t_login_log` VALUES (2083761814501380097, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 11:48:26');
INSERT INTO `t_login_log` VALUES (2083761814761426945, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 11:48:26');
INSERT INTO `t_login_log` VALUES (2083764620868243457, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 11:59:35');
INSERT INTO `t_login_log` VALUES (2083764621249925121, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 11:59:35');
INSERT INTO `t_login_log` VALUES (2083788034068762626, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 13:32:37');
INSERT INTO `t_login_log` VALUES (2083788034198786049, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 13:32:37');
INSERT INTO `t_login_log` VALUES (2083788065291161601, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 13:32:44');
INSERT INTO `t_login_log` VALUES (2083788065484099585, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 13:32:44');
INSERT INTO `t_login_log` VALUES (2083793176570691586, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 13:53:03');
INSERT INTO `t_login_log` VALUES (2083793176897847298, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 13:53:03');
INSERT INTO `t_login_log` VALUES (2083793207805673473, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 13:53:10');
INSERT INTO `t_login_log` VALUES (2083793208065720321, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'FAILED', '用户名或密码错误', '2026-08-02 13:53:10');
INSERT INTO `t_login_log` VALUES (2083793361002627074, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 13:53:47');
INSERT INTO `t_login_log` VALUES (2083860923161796609, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 18:22:15');
INSERT INTO `t_login_log` VALUES (2083860923417649154, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 18:22:15');
INSERT INTO `t_login_log` VALUES (2083861305057370114, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 18:23:46');
INSERT INTO `t_login_log` VALUES (2083861305443246082, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 18:23:46');
INSERT INTO `t_login_log` VALUES (2083876489889140737, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 19:24:06');
INSERT INTO `t_login_log` VALUES (2083876490144993282, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 19:24:06');
INSERT INTO `t_login_log` VALUES (2083883585552601090, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 19:52:18');
INSERT INTO `t_login_log` VALUES (2083884959833083905, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 19:57:46');
INSERT INTO `t_login_log` VALUES (2083884960084742146, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 19:57:46');
INSERT INTO `t_login_log` VALUES (2083889985146114050, 2, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 20:17:44');
INSERT INTO `t_login_log` VALUES (2083889985146114051, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 20:17:44');
INSERT INTO `t_login_log` VALUES (2083916438462058497, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 22:02:51');
INSERT INTO `t_login_log` VALUES (2083916438462058498, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-02 22:02:51');
INSERT INTO `t_login_log` VALUES (2084096303207337985, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 09:57:34');
INSERT INTO `t_login_log` VALUES (2084096303723237377, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 09:57:34');
INSERT INTO `t_login_log` VALUES (2084098057378516993, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 10:04:32');
INSERT INTO `t_login_log` VALUES (2084098057764392961, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 10:04:32');
INSERT INTO `t_login_log` VALUES (2084139455263813633, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 12:49:02');
INSERT INTO `t_login_log` VALUES (2084139455318339585, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 12:49:02');
INSERT INTO `t_login_log` VALUES (2084217912912007170, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 18:00:48');
INSERT INTO `t_login_log` VALUES (2084217913293688833, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 18:00:48');
INSERT INTO `t_login_log` VALUES (2084267917559484417, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 21:19:30');
INSERT INTO `t_login_log` VALUES (2084267917878251521, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 21:19:30');
INSERT INTO `t_login_log` VALUES (2084295146821734402, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 23:07:42');
INSERT INTO `t_login_log` VALUES (2084295146821734403, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-03 23:07:42');
INSERT INTO `t_login_log` VALUES (2084439079673421826, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 08:39:38');
INSERT INTO `t_login_log` VALUES (2084439079673421827, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 08:39:38');
INSERT INTO `t_login_log` VALUES (2084439373903847425, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 08:40:48');
INSERT INTO `t_login_log` VALUES (2084439374415552513, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 08:40:49');
INSERT INTO `t_login_log` VALUES (2084458722274316289, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 09:57:41');
INSERT INTO `t_login_log` VALUES (2084458722442088450, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 09:57:41');
INSERT INTO `t_login_log` VALUES (2084458951346229250, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 09:58:36');
INSERT INTO `t_login_log` VALUES (2084459023781859329, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 09:58:53');
INSERT INTO `t_login_log` VALUES (2084459024037711873, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 09:58:53');
INSERT INTO `t_login_log` VALUES (2084502283745234945, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 12:50:47');
INSERT INTO `t_login_log` VALUES (2084574991862513666, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 17:39:42');
INSERT INTO `t_login_log` VALUES (2084574991862513667, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-04 17:39:42');
INSERT INTO `t_login_log` VALUES (2084871231833722881, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 13:16:51');
INSERT INTO `t_login_log` VALUES (2084871231972134913, 2, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 13:16:51');
INSERT INTO `t_login_log` VALUES (2084871626920382466, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 13:18:26');
INSERT INTO `t_login_log` VALUES (2084871627239149570, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 13:18:26');
INSERT INTO `t_login_log` VALUES (2084977074650550273, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 20:17:26');
INSERT INTO `t_login_log` VALUES (2084977076315688962, 1, '0:0:0:0:0:0:0:1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 20:17:27');
INSERT INTO `t_login_log` VALUES (2085006839709245441, 2, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 22:15:43');
INSERT INTO `t_login_log` VALUES (2085006841189834754, 2, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-05 22:15:43');
INSERT INTO `t_login_log` VALUES (2085167530460213250, 1, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-06 08:54:14');
INSERT INTO `t_login_log` VALUES (2085167531944996865, 1, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-06 08:54:15');
INSERT INTO `t_login_log` VALUES (2085201993697226753, 1, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-06 11:11:11');
INSERT INTO `t_login_log` VALUES (2085201993781112833, 1, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, '2026-08-06 11:11:11');

-- ----------------------------
-- Table structure for t_normal_order
-- ----------------------------
DROP TABLE IF EXISTS `t_normal_order`;
CREATE TABLE `t_normal_order`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号（唯一，时间戳+随机串）',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `address_id` bigint NOT NULL COMMENT '收货地址ID',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '商品总金额（明细小计之和）',
  `freight_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
  `pay_amount` decimal(10, 2) NOT NULL COMMENT '实付金额 = total_amount + freight_amount',
  `status` enum('UNPAID','PAID','CANCELLED','TIMEOUT','COMPLETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-待支付/PAID-已支付/CANCELLED-已取消/TIMEOUT-超时/COMPLETED-已完成',
  `pay_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付方式（WALLET/ALIPAY/WECHAT等）',
  `transaction_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付流水号',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `pay_expire_time` datetime NULL DEFAULT NULL COMMENT '支付截止时间（超时自动取消）',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '取消原因',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户下单备注',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（下单时间）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ship_time` datetime NULL DEFAULT NULL COMMENT '发货时间',
  `confirm_time` datetime NULL DEFAULT NULL COMMENT '确认收货时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_user_create`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '普通订单表（立即购买/购物车结算）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_normal_order
-- ----------------------------
INSERT INTO `t_normal_order` VALUES (2084295258893537282, 'NO20260803230808253679', 2, 2083864352164700162, 199.00, 0.00, 199.00, 'UNPAID', NULL, NULL, NULL, '2026-08-03 23:23:09', NULL, NULL, NULL, 0, '2026-08-03 23:08:09', '2026-08-03 23:08:09', NULL, NULL);
INSERT INTO `t_normal_order` VALUES (2084459096947298305, 'NO20260804095910908525', 2, 2083864352164700162, 1587.90, 0.00, 1587.90, 'CANCELLED', NULL, NULL, NULL, '2026-08-04 10:14:11', '2026-08-05 22:16:08', '用户主动取消', '测试', 0, '2026-08-04 09:59:11', '2026-08-04 09:59:11', NULL, NULL);

-- ----------------------------
-- Table structure for t_normal_order_item
-- ----------------------------
DROP TABLE IF EXISTS `t_normal_order_item`;
CREATE TABLE `t_normal_order_item`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `order_id` bigint NOT NULL COMMENT '所属普通订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称（下单时快照）',
  `product_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品主图URL（下单时快照）',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '商品单价（下单时快照，原价）',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `subtotal` decimal(10, 2) NOT NULL COMMENT '小计金额 = unit_price × quantity',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '普通订单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_normal_order_item
-- ----------------------------
INSERT INTO `t_normal_order_item` VALUES (2084295259023560706, 2084295258893537282, 80, '乐扣乐扣 耐热玻璃保鲜盒 6件', '/images/products/80_locknlock_box/images/01_main.png', 199.00, 1, 199.00, 0, '2026-08-03 23:08:09', '2026-08-03 23:08:09');
INSERT INTO `t_normal_order_item` VALUES (2084459096947298306, 2084459096947298305, 78, '太力 真空压缩袋 12件套', '/images/products/78_taili_bag/images/01_main.jpg', 89.90, 1, 89.90, 0, '2026-08-04 09:59:11', '2026-08-04 09:59:11');
INSERT INTO `t_normal_order_item` VALUES (2084459096947298307, 2084459096947298305, 79, '双立人 锋韵 6件套锅', '/images/products/79_zwilling_pot/images/01_main.jpg', 1299.00, 1, 1299.00, 0, '2026-08-04 09:59:11', '2026-08-04 09:59:11');
INSERT INTO `t_normal_order_item` VALUES (2084459097010212866, 2084459096947298305, 80, '乐扣乐扣 耐热玻璃保鲜盒 6件', '/images/products/80_locknlock_box/images/01_main.png', 199.00, 1, 199.00, 0, '2026-08-04 09:59:11', '2026-08-04 09:59:11');

-- ----------------------------
-- Table structure for t_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作模块（商品管理/用户管理/秒杀管理等）',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型（CREATE/UPDATE/DELETE/EXPORT等）',
  `target_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作目标ID',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作目标类型（PRODUCT/SECKILL/USER等）',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '操作详情（JSON变更快照）',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operator_time`(`operator_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表（后台）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_operation_log
-- ----------------------------
INSERT INTO `t_operation_log` VALUES (2083759197389901826, 1, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:38:02');
INSERT INTO `t_operation_log` VALUES (2083759245385322497, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:38:13');
INSERT INTO `t_operation_log` VALUES (2083759245385322498, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:38:13');
INSERT INTO `t_operation_log` VALUES (2083761785132863490, 1, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:48:19');
INSERT INTO `t_operation_log` VALUES (2083764620931158018, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:59:35');
INSERT INTO `t_operation_log` VALUES (2083764621317033985, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:59:35');
INSERT INTO `t_operation_log` VALUES (2083764689558360066, 1, 'PRODUCT', 'UPDATE', '1001', 'PRODUCT', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 11:59:51');
INSERT INTO `t_operation_log` VALUES (2083764795548422145, 1, 'CATEGORY', 'CREATE', NULL, 'CATEGORY', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 12:00:16');
INSERT INTO `t_operation_log` VALUES (2083771578119671809, 1, 'PRODUCT', 'UPDATE', '1001', 'PRODUCT', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 12:27:13');
INSERT INTO `t_operation_log` VALUES (2083788065354076162, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:32:44');
INSERT INTO `t_operation_log` VALUES (2083788065551208450, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:32:44');
INSERT INTO `t_operation_log` VALUES (2083790581114404865, 1, 'CATEGORY', 'UPDATE', '200', 'CATEGORY', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:42:44');
INSERT INTO `t_operation_log` VALUES (2083790607148449794, 1, 'CATEGORY', 'UPDATE', '300', 'CATEGORY', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:42:50');
INSERT INTO `t_operation_log` VALUES (2083790629185323010, 1, 'CATEGORY', 'UPDATE', '400', 'CATEGORY', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:42:56');
INSERT INTO `t_operation_log` VALUES (2083793147386724353, 1, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:52:56');
INSERT INTO `t_operation_log` VALUES (2083793361128456194, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 13:53:47');
INSERT INTO `t_operation_log` VALUES (2083860923547672578, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 18:22:15');
INSERT INTO `t_operation_log` VALUES (2083860923547672579, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 18:22:15');
INSERT INTO `t_operation_log` VALUES (2083861305120284674, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 18:23:46');
INSERT INTO `t_operation_log` VALUES (2083861305443246083, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 18:23:46');
INSERT INTO `t_operation_log` VALUES (2083876415222140929, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:23:49');
INSERT INTO `t_operation_log` VALUES (2083876490014969857, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:24:06');
INSERT INTO `t_operation_log` VALUES (2083876490144993283, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:24:06');
INSERT INTO `t_operation_log` VALUES (2083883428006154241, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:51:41');
INSERT INTO `t_operation_log` VALUES (2083883585615515650, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:52:18');
INSERT INTO `t_operation_log` VALUES (2083884890186665985, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:57:29');
INSERT INTO `t_operation_log` VALUES (2083884959895998466, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:57:46');
INSERT INTO `t_operation_log` VALUES (2083884960151851009, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:57:46');
INSERT INTO `t_operation_log` VALUES (2083885028808413185, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 19:58:02');
INSERT INTO `t_operation_log` VALUES (2083889985318080513, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-02 20:17:44');
INSERT INTO `t_operation_log` VALUES (2083889985318080514, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 20:17:44');
INSERT INTO `t_operation_log` VALUES (2083894983535939585, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 20:37:36');
INSERT INTO `t_operation_log` VALUES (2083916438592081921, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 22:02:51');
INSERT INTO `t_operation_log` VALUES (2083916438592081922, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-02 22:02:51');
INSERT INTO `t_operation_log` VALUES (2084096254624714754, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 09:57:22');
INSERT INTO `t_operation_log` VALUES (2084096303274446850, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 09:57:34');
INSERT INTO `t_operation_log` VALUES (2084096303786151937, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 09:57:34');
INSERT INTO `t_operation_log` VALUES (2084098057437237250, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 10:04:32');
INSERT INTO `t_operation_log` VALUES (2084098057827307522, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 10:04:32');
INSERT INTO `t_operation_log` VALUES (2084139455444168705, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 12:49:02');
INSERT INTO `t_operation_log` VALUES (2084139455444168706, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 12:49:02');
INSERT INTO `t_operation_log` VALUES (2084175871213637634, 1, 'SECKILL', 'CANCEL', '5002', 'SECKILL', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 15:13:44');
INSERT INTO `t_operation_log` VALUES (2084175878411063297, 1, 'SECKILL', 'CANCEL', '5001', 'SECKILL', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 15:13:46');
INSERT INTO `t_operation_log` VALUES (2084176345274847233, 1, 'BANNER', 'CREATE', NULL, 'BANNER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 15:15:38');
INSERT INTO `t_operation_log` VALUES (2084217913042030593, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 18:00:48');
INSERT INTO `t_operation_log` VALUES (2084217913360797697, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 18:00:48');
INSERT INTO `t_operation_log` VALUES (2084219980984897537, 1, 'BANNER', 'CREATE', NULL, 'BANNER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 18:09:01');
INSERT INTO `t_operation_log` VALUES (2084267917752422401, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 21:19:30');
INSERT INTO `t_operation_log` VALUES (2084267917941166082, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 21:19:30');
INSERT INTO `t_operation_log` VALUES (2084284125197455361, 1, 'REVIEW', 'UPDATE_STATUS', '2083861658318430209', 'REVIEW', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 22:23:54');
INSERT INTO `t_operation_log` VALUES (2084284133623812097, 1, 'REVIEW', 'UPDATE_STATUS', '2083861658318430209', 'REVIEW', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 22:23:56');
INSERT INTO `t_operation_log` VALUES (2084295146951757825, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 23:07:42');
INSERT INTO `t_operation_log` VALUES (2084295146951757826, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 23:07:42');
INSERT INTO `t_operation_log` VALUES (2084295259086475266, 2, 'ORDER', 'CREATE', NULL, 'ORDER', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 23:08:09');
INSERT INTO `t_operation_log` VALUES (2084297480557617154, 1, 'SECKILL', 'CREATE', NULL, 'SECKILL', NULL, '0:0:0:0:0:0:0:1', '2026-08-03 23:16:58');
INSERT INTO `t_operation_log` VALUES (2084439080235458562, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 08:39:38');
INSERT INTO `t_operation_log` VALUES (2084439080235458563, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 08:39:38');
INSERT INTO `t_operation_log` VALUES (2084439373966761986, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 08:40:48');
INSERT INTO `t_operation_log` VALUES (2084439374415552514, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 08:40:49');
INSERT INTO `t_operation_log` VALUES (2084439462294609922, 1, 'SECKILL', 'UPDATE', '2084297480440176641', 'SECKILL', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 08:41:09');
INSERT INTO `t_operation_log` VALUES (2084458722442088449, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 09:57:41');
INSERT INTO `t_operation_log` VALUES (2084458722505003010, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 09:57:41');
INSERT INTO `t_operation_log` VALUES (2084458951409143809, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 09:58:36');
INSERT INTO `t_operation_log` VALUES (2084459023781859330, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 09:58:53');
INSERT INTO `t_operation_log` VALUES (2084459024037711874, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 09:58:53');
INSERT INTO `t_operation_log` VALUES (2084459097073127425, 2, 'ORDER', 'CREATE', NULL, 'ORDER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 09:59:11');
INSERT INTO `t_operation_log` VALUES (2084502283942367234, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 12:50:47');
INSERT INTO `t_operation_log` VALUES (2084574992273555457, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 17:39:42');
INSERT INTO `t_operation_log` VALUES (2084574992273555458, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-04 17:39:42');
INSERT INTO `t_operation_log` VALUES (2084871232425119745, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 13:16:52');
INSERT INTO `t_operation_log` VALUES (2084871232425119746, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 13:16:52');
INSERT INTO `t_operation_log` VALUES (2084871627109126145, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 13:18:26');
INSERT INTO `t_operation_log` VALUES (2084871627423698946, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 13:18:26');
INSERT INTO `t_operation_log` VALUES (2084977046586462209, 1, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 20:17:20');
INSERT INTO `t_operation_log` VALUES (2084977074839293954, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 20:17:26');
INSERT INTO `t_operation_log` VALUES (2084977076441518082, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '0:0:0:0:0:0:0:1', '2026-08-05 20:17:27');
INSERT INTO `t_operation_log` VALUES (2085006798852530178, 1, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '127.0.0.1', '2026-08-05 22:15:33');
INSERT INTO `t_operation_log` VALUES (2085006839902183425, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-05 22:15:43');
INSERT INTO `t_operation_log` VALUES (2085006841315663874, 2, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-05 22:15:43');
INSERT INTO `t_operation_log` VALUES (2085006892075130881, 2, 'ORDER', 'PAY', '2085006853474951169', 'ORDER', NULL, '127.0.0.1', '2026-08-05 22:15:55');
INSERT INTO `t_operation_log` VALUES (2085006944894001154, 2, 'ORDER', 'CANCEL', '2084459096947298305', 'ORDER', NULL, '127.0.0.1', '2026-08-05 22:16:08');
INSERT INTO `t_operation_log` VALUES (2085167490161340418, 2, 'AUTH', 'LOGOUT', NULL, 'USER', NULL, '127.0.0.1', '2026-08-06 08:54:05');
INSERT INTO `t_operation_log` VALUES (2085167530586042369, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-06 08:54:14');
INSERT INTO `t_operation_log` VALUES (2085167532075020289, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-06 08:54:15');
INSERT INTO `t_operation_log` VALUES (2085201994242486274, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-06 11:11:11');
INSERT INTO `t_operation_log` VALUES (2085201994242486275, 1, 'AUTH', 'LOGIN', NULL, 'USER', NULL, '127.0.0.1', '2026-08-06 11:11:11');

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '商品简介（纯文本）',
  `original_price` decimal(10, 2) NOT NULL COMMENT '原价（元）',
  `stock` int NOT NULL DEFAULT 0 COMMENT '普通库存数量',
  `sales_count` int NOT NULL DEFAULT 0 COMMENT '累计销量',
  `cart_count` int NOT NULL DEFAULT 0 COMMENT '加购数量(冗余计数)',
  `favorite_count` int NOT NULL DEFAULT 0 COMMENT '收藏数量(冗余计数)',
  `category_id` bigint NOT NULL COMMENT '所属分类ID',
  `images` json NULL COMMENT '商品图片URL数组',
  `main_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主图URL',
  `detail_html` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '商品详情（富文本HTML）',
  `status` enum('ON_SALE','OFF_SHELF') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE-在售/OFF_SHELF-下架',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_status`(`category_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 117 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_product
-- ----------------------------
INSERT INTO `t_product` VALUES (1, '小米14 Pro 12GB+256GB', '小米14 Pro 搭载骁龙8 Gen3，6.73英寸2K全等深四曲屏，徕卡专业光学镜头，光影猎人900主摄，系统内建澎湃OS。', 4999.00, 320, 0, 0, 0, 101, '[\"/images/products/01_xiaomi14pro/images/01_main.jpg\", \"/images/products/01_xiaomi14pro/images/02_detail.png\", \"/images/products/01_xiaomi14pro/images/03_detail.jpg\"]', '/images/products/01_xiaomi14pro/images/01_main.jpg', '<h3>小米14 Pro 12GB+256GB</h3><p>小米14 Pro 搭载骁龙8 Gen3，6.73英寸2K全等深四曲屏，徕卡专业光学镜头，光影猎人900主摄，系统内建澎湃OS。</p><p>分类：手机数码/智能手机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (2, '华为 Mate 60 Pro 12GB+512GB', '华为 Mate 60 Pro 搭载麒麟9000S，6.82英寸等深四曲屏，超可靠玄武架构，卫星通话，全焦段超清影像。', 6999.00, 210, 0, 0, 0, 101, '[\"/images/products/02_huaweimate60pro/images/01_main.jpg\", \"/images/products/02_huaweimate60pro/images/02_detail.jpg\", \"/images/products/02_huaweimate60pro/images/03_detail.jpg\"]', '/images/products/02_huaweimate60pro/images/01_main.jpg', '<h3>华为 Mate 60 Pro 12GB+512GB</h3><p>华为 Mate 60 Pro 搭载麒麟9000S，6.82英寸等深四曲屏，超可靠玄武架构，卫星通话，全焦段超清影像。</p><p>分类：手机数码/智能手机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (3, '小米 67W 氮化镓充电器', '小米 67W 氮化镓充电器，小巧便携，支持小米私有快充协议，可为手机、平板快速回血。', 99.00, 1500, 0, 0, 0, 102, '[\"/images/products/03_xiaomi_67w_charger/images/01_main.jpg\", \"/images/products/03_xiaomi_67w_charger/images/02_detail.jpg\", \"/images/products/03_xiaomi_67w_charger/images/03_detail.jpg\"]', '/images/products/03_xiaomi_67w_charger/images/01_main.jpg', '<h3>小米 67W 氮化镓充电器</h3><p>小米 67W 氮化镓充电器，小巧便携，支持小米私有快充协议，可为手机、平板快速回血。</p><p>分类：手机数码/手机配件</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (4, '华为 66W 超级快充充电器', '华为 66W 超级快充充电器，支持华为 SuperCharge 快充协议，兼容多款华为设备，出行必备。', 199.00, 1200, 0, 0, 0, 102, '[\"/images/products/04_huawei_66w_charger/images/01_main.jpg\", \"/images/products/04_huawei_66w_charger/images/02_detail.jpg\", \"/images/products/04_huawei_66w_charger/images/03_detail.jpg\"]', '/images/products/04_huawei_66w_charger/images/01_main.jpg', '<h3>华为 66W 超级快充充电器</h3><p>华为 66W 超级快充充电器，支持华为 SuperCharge 快充协议，兼容多款华为设备，出行必备。</p><p>分类：手机数码/手机配件</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (5, '小米 Watch S3', '小米 Watch S3 支持可更换表圈，全新澎湃OS，全天候健康监测，150+运动模式，蓝牙通话。', 799.00, 640, 0, 0, 0, 103, '[\"/images/products/05_xiaomi_watch_s3/images/01_main.jpg\", \"/images/products/05_xiaomi_watch_s3/images/02_detail.jpg\", \"/images/products/05_xiaomi_watch_s3/images/03_detail.png\"]', '/images/products/05_xiaomi_watch_s3/images/01_main.jpg', '<h3>小米 Watch S3</h3><p>小米 Watch S3 支持可更换表圈，全新澎湃OS，全天候健康监测，150+运动模式，蓝牙通话。</p><p>分类：手机数码/智能手表</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (6, '华为 Watch GT 4', '华为 Watch GT 4 科学睡眠与心率监测，100+运动模式，经典圆形设计，续航长达14天。', 1488.00, 530, 0, 0, 0, 103, '[\"/images/products/06_huawei_watch_gt4/images/01_main.png\", \"/images/products/06_huawei_watch_gt4/images/02_detail.jpg\", \"/images/products/06_huawei_watch_gt4/images/03_detail.jpg\"]', '/images/products/06_huawei_watch_gt4/images/01_main.png', '<h3>华为 Watch GT 4</h3><p>华为 Watch GT 4 科学睡眠与心率监测，100+运动模式，经典圆形设计，续航长达14天。</p><p>分类：手机数码/智能手表</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (7, '小米 Buds 4 Pro', '小米 Buds 4 Pro 支持主动降噪与空间音频，48kHz 无线高清传输，舒适入耳佩戴。', 999.00, 880, 0, 0, 0, 104, '[\"/images/products/07_xiaomi_buds4pro/images/01_main.jpg\", \"/images/products/07_xiaomi_buds4pro/images/02_detail.png\", \"/images/products/07_xiaomi_buds4pro/images/03_detail.png\"]', '/images/products/07_xiaomi_buds4pro/images/01_main.jpg', '<h3>小米 Buds 4 Pro</h3><p>小米 Buds 4 Pro 支持主动降噪与空间音频，48kHz 无线高清传输，舒适入耳佩戴。</p><p>分类：手机数码/蓝牙耳机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (8, '华为 FreeBuds Pro 3', '华为 FreeBuds Pro 3 搭载麒麟音频芯片，超CD级无损音质，智慧动态降噪，全场景互联。', 1499.00, 760, 0, 0, 0, 104, '[\"/images/products/08_huawei_freebuds_pro3/images/01_main.png\", \"/images/products/08_huawei_freebuds_pro3/images/02_detail.jpg\", \"/images/products/08_huawei_freebuds_pro3/images/03_detail.jpg\"]', '/images/products/08_huawei_freebuds_pro3/images/01_main.png', '<h3>华为 FreeBuds Pro 3</h3><p>华为 FreeBuds Pro 3 搭载麒麟音频芯片，超CD级无损音质，智慧动态降噪，全场景互联。</p><p>分类：手机数码/蓝牙耳机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (9, '大疆 Osmo Pocket 3 一英寸口袋云台相机', '大疆 Osmo Pocket 3 一英寸 CMOS 口袋云台相机，2英寸旋转触屏，4K/120fps，三轴机械增稳。', 3499.00, 300, 0, 0, 0, 105, '[\"/images/products/09_dji_osmo_pocket3/images/01_main.jpg\", \"/images/products/09_dji_osmo_pocket3/images/02_detail.jpg\", \"/images/products/09_dji_osmo_pocket3/images/03_detail.jpg\"]', '/images/products/09_dji_osmo_pocket3/images/01_main.jpg', '<h3>大疆 Osmo Pocket 3 一英寸口袋云台相机</h3><p>大疆 Osmo Pocket 3 一英寸 CMOS 口袋云台相机，2英寸旋转触屏，4K/120fps，三轴机械增稳。</p><p>分类：手机数码/摄影摄像</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (10, '影石 Insta360 GO 3 拇指相机', '影石 Insta360 GO 3 拇指大小防抖相机，磁吸穿戴拍摄，支持FlowState防抖与多种创意模式。', 2298.00, 420, 0, 0, 0, 105, '[\"/images/products/10_insta360_go3/images/01_main.jpg\", \"/images/products/10_insta360_go3/images/02_detail.jpg\", \"/images/products/10_insta360_go3/images/03_detail.jpg\"]', '/images/products/10_insta360_go3/images/01_main.jpg', '<h3>影石 Insta360 GO 3 拇指相机</h3><p>影石 Insta360 GO 3 拇指大小防抖相机，磁吸穿戴拍摄，支持FlowState防抖与多种创意模式。</p><p>分类：手机数码/摄影摄像</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (11, '小米 米兔儿童学习手表 6C', '小米 米兔儿童学习手表 6C，1.4英寸彩屏，支持视频通话、精准定位、学习应用，守护孩子安全。', 599.00, 950, 0, 0, 0, 106, '[\"/images/products/11_mitu_learning_watch/images/01_main.jpg\", \"/images/products/11_mitu_learning_watch/images/02_detail.jpg\", \"/images/products/11_mitu_learning_watch/images/03_detail.jpg\"]', '/images/products/11_mitu_learning_watch/images/01_main.jpg', '<h3>小米 米兔儿童学习手表 6C</h3><p>小米 米兔儿童学习手表 6C，1.4英寸彩屏，支持视频通话、精准定位、学习应用，守护孩子安全。</p><p>分类：手机数码/电子教育</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (12, '科大讯飞 AI 学习机 T20 Pro', '科大讯飞 AI 学习机 T20 Pro，13.3英寸2.5K护眼屏，AI精准学覆盖小初高，8GB+512GB大存储。', 8999.00, 180, 0, 0, 0, 106, '[\"/images/products/12_iflytek_t20_pro/images/01_main.png\", \"/images/products/12_iflytek_t20_pro/images/02_detail.jpg\", \"/images/products/12_iflytek_t20_pro/images/03_detail.jpg\"]', '/images/products/12_iflytek_t20_pro/images/01_main.png', '<h3>科大讯飞 AI 学习机 T20 Pro</h3><p>科大讯飞 AI 学习机 T20 Pro，13.3英寸2.5K护眼屏，AI精准学覆盖小初高，8GB+512GB大存储。</p><p>分类：手机数码/电子教育</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (13, '联想 ThinkPad X1 Carbon 14', '联想 ThinkPad X1 Carbon 14 碳纤维轻薄机身，14英寸2.8K OLED，商务旗舰笔记本。', 12999.00, 160, 0, 0, 0, 201, '[\"/images/products/13_thinkpad_x1carbon/images/01_main.jpg\", \"/images/products/13_thinkpad_x1carbon/images/02_detail.jpg\", \"/images/products/13_thinkpad_x1carbon/images/03_detail.jpg\"]', '/images/products/13_thinkpad_x1carbon/images/01_main.jpg', '<h3>联想 ThinkPad X1 Carbon 14</h3><p>联想 ThinkPad X1 Carbon 14 碳纤维轻薄机身，14英寸2.8K OLED，商务旗舰笔记本。</p><p>分类：电脑办公/笔记本电脑</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (14, '华为 MateBook X Pro 14', '华为 MateBook X Pro 14 轻薄全金属机身，3.1K原色全面屏，Super Turbo 性能释放。', 9999.00, 140, 0, 0, 0, 201, '[\"/images/products/14_huawei_matebook_xpro/images/01_main.jpg\", \"/images/products/14_huawei_matebook_xpro/images/02_detail.jpg\", \"/images/products/14_huawei_matebook_xpro/images/03_detail.jpg\"]', '/images/products/14_huawei_matebook_xpro/images/01_main.jpg', '<h3>华为 MateBook X Pro 14</h3><p>华为 MateBook X Pro 14 轻薄全金属机身，3.1K原色全面屏，Super Turbo 性能释放。</p><p>分类：电脑办公/笔记本电脑</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (15, '联想 拯救者刃7000K 台式机', '联想 拯救者刃7000K 游戏台式机，i7+RTX独显，强劲散热，畅玩3A大作。', 8499.00, 90, 0, 0, 0, 202, '[\"/images/products/15_legion_7000k/images/01_main.png\", \"/images/products/15_legion_7000k/images/02_detail.jpg\", \"/images/products/15_legion_7000k/images/03_detail.jpg\"]', '/images/products/15_legion_7000k/images/01_main.png', '<h3>联想 拯救者刃7000K 台式机</h3><p>联想 拯救者刃7000K 游戏台式机，i7+RTX独显，强劲散热，畅玩3A大作。</p><p>分类：电脑办公/台式机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (16, '苹果 Mac mini M2', '苹果 Mac mini M2 小巧桌面主机，8核CPU+10核GPU，静音高能效。', 4499.00, 210, 0, 0, 0, 202, '[\"/images/products/16_mac_mini_m2/images/01_main.jpg\", \"/images/products/16_mac_mini_m2/images/02_detail.jpg\", \"/images/products/16_mac_mini_m2/images/03_detail.png\"]', '/images/products/16_mac_mini_m2/images/01_main.jpg', '<h3>苹果 Mac mini M2</h3><p>苹果 Mac mini M2 小巧桌面主机，8核CPU+10核GPU，静音高能效。</p><p>分类：电脑办公/台式机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (17, '戴尔 U2723QE 27英寸显示器', '戴尔 U2723QE 27英寸4K IPS Black面板，Type-C 90W反向供电，专业色彩。', 3999.00, 180, 0, 0, 0, 203, '[\"/images/products/17_dell_u2723qe/images/01_main.jpg\", \"/images/products/17_dell_u2723qe/images/02_detail.png\", \"/images/products/17_dell_u2723qe/images/03_detail.jpg\"]', '/images/products/17_dell_u2723qe/images/01_main.jpg', '<h3>戴尔 U2723QE 27英寸显示器</h3><p>戴尔 U2723QE 27英寸4K IPS Black面板，Type-C 90W反向供电，专业色彩。</p><p>分类：电脑办公/显示器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (18, 'LG 27GP850 27英寸显示器', 'LG 27GP850 27英寸2K Nano IPS，180Hz高刷，电竞显示器。', 2299.00, 260, 0, 0, 0, 203, '[\"/images/products/18_lg_27gp850/images/01_main.jpg\", \"/images/products/18_lg_27gp850/images/02_detail.jpg\", \"/images/products/18_lg_27gp850/images/03_detail.jpg\"]', '/images/products/18_lg_27gp850/images/01_main.jpg', '<h3>LG 27GP850 27英寸显示器</h3><p>LG 27GP850 27英寸2K Nano IPS，180Hz高刷，电竞显示器。</p><p>分类：电脑办公/显示器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (19, '罗技 MX Keys S 无线键盘', '罗技 MX Keys S 背光无线键盘，智能背光，多设备切换，办公舒适。', 699.00, 540, 0, 0, 0, 204, '[\"/images/products/19_logi_mxkeys/images/01_main.jpg\", \"/images/products/19_logi_mxkeys/images/02_detail.jpg\", \"/images/products/19_logi_mxkeys/images/03_detail.jpg\"]', '/images/products/19_logi_mxkeys/images/01_main.jpg', '<h3>罗技 MX Keys S 无线键盘</h3><p>罗技 MX Keys S 背光无线键盘，智能背光，多设备切换，办公舒适。</p><p>分类：电脑办公/键盘鼠标</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (20, '罗技 MX Master 3S 鼠标', '罗技 MX Master 3S 无线鼠标，8K DPI静音按键，电磁滚轮，多设备操控。', 699.00, 620, 0, 0, 0, 204, '[\"/images/products/20_logi_mxmaster3s/images/01_main.jpg\", \"/images/products/20_logi_mxmaster3s/images/02_detail.jpg\", \"/images/products/20_logi_mxmaster3s/images/03_detail.jpg\"]', '/images/products/20_logi_mxmaster3s/images/01_main.jpg', '<h3>罗技 MX Master 3S 鼠标</h3><p>罗技 MX Master 3S 无线鼠标，8K DPI静音按键，电磁滚轮，多设备操控。</p><p>分类：电脑办公/键盘鼠标</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (21, '惠普 136w 激光打印机', '惠普 136w 黑白激光多功能一体机，打印复印扫描，无线连接。', 1299.00, 230, 0, 0, 0, 205, '[\"/images/products/21_hp_136w/images/01_main.jpg\", \"/images/products/21_hp_136w/images/02_detail.jpg\", \"/images/products/21_hp_136w/images/03_detail.jpg\"]', '/images/products/21_hp_136w/images/01_main.jpg', '<h3>惠普 136w 激光打印机</h3><p>惠普 136w 黑白激光多功能一体机，打印复印扫描，无线连接。</p><p>分类：电脑办公/打印耗材</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (22, '佳能 G3810 喷墨打印机', '佳能 G3810 高容量连供彩色喷墨一体机，低成本打印照片文档。', 1099.00, 200, 0, 0, 0, 205, '[\"/images/products/22_canon_g3810/images/01_main.png\", \"/images/products/22_canon_g3810/images/02_detail.jpg\", \"/images/products/22_canon_g3810/images/03_detail.jpg\"]', '/images/products/22_canon_g3810/images/01_main.png', '<h3>佳能 G3810 喷墨打印机</h3><p>佳能 G3810 高容量连供彩色喷墨一体机，低成本打印照片文档。</p><p>分类：电脑办公/打印耗材</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (23, '三星 980 PRO 2TB SSD', '三星 980 PRO 2TB NVMe M.2 固态硬盘，PCIe4.0 读取7000MB/s。', 1099.00, 480, 0, 0, 0, 206, '[\"/images/products/23_samsung_980pro/images/01_main.jpg\", \"/images/products/23_samsung_980pro/images/02_detail.jpg\", \"/images/products/23_samsung_980pro/images/03_detail.jpg\"]', '/images/products/23_samsung_980pro/images/01_main.jpg', '<h3>三星 980 PRO 2TB SSD</h3><p>三星 980 PRO 2TB NVMe M.2 固态硬盘，PCIe4.0 读取7000MB/s。</p><p>分类：电脑办公/电脑配件</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (24, '西部数据 My Passport 2TB', '西部数据 My Passport 2TB 移动硬盘，USB3.0 便携加密备份。', 499.00, 560, 0, 0, 0, 206, '[\"/images/products/24_wd_mypassport/images/01_main.jpg\", \"/images/products/24_wd_mypassport/images/02_detail.jpg\", \"/images/products/24_wd_mypassport/images/03_detail.jpg\"]', '/images/products/24_wd_mypassport/images/01_main.jpg', '<h3>西部数据 My Passport 2TB</h3><p>西部数据 My Passport 2TB 移动硬盘，USB3.0 便携加密备份。</p><p>分类：电脑办公/电脑配件</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (25, '美的 KFR-35GW 1.5匹空调', '美的 KFR-35GW 1.5匹变频空调，新一级能效，自清洁静音。', 2499.00, 300, 0, 0, 0, 301, '[\"/images/products/25_midea_35gw/images/01_main.png\", \"/images/products/25_midea_35gw/images/02_detail.jpg\", \"/images/products/25_midea_35gw/images/03_detail.png\"]', '/images/products/25_midea_35gw/images/01_main.png', '<h3>美的 KFR-35GW 1.5匹空调</h3><p>美的 KFR-35GW 1.5匹变频空调，新一级能效，自清洁静音。</p><p>分类：家用电器/空调</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (26, '格力 KFR-35GW 1.5匹空调', '格力 KFR-35GW 1.5匹变频空调，云佳系列，省电节能。', 2799.00, 280, 0, 0, 0, 301, '[\"/images/products/26_gree_35gw/images/01_main.png\", \"/images/products/26_gree_35gw/images/02_detail.jpg\", \"/images/products/26_gree_35gw/images/03_detail.jpg\"]', '/images/products/26_gree_35gw/images/01_main.png', '<h3>格力 KFR-35GW 1.5匹空调</h3><p>格力 KFR-35GW 1.5匹变频空调，云佳系列，省电节能。</p><p>分类：家用电器/空调</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (27, '海尔 EG100MATE81S 10kg 洗衣机', '海尔 EG100MATE81S 10公斤滚筒洗衣机，变频直驱，除菌螨。', 2599.00, 220, 0, 0, 0, 302, '[\"/images/products/27_haier_10kg/images/01_main.png\", \"/images/products/27_haier_10kg/images/02_detail.jpg\", \"/images/products/27_haier_10kg/images/03_detail.jpg\"]', '/images/products/27_haier_10kg/images/01_main.png', '<h3>海尔 EG100MATE81S 10kg 洗衣机</h3><p>海尔 EG100MATE81S 10公斤滚筒洗衣机，变频直驱，除菌螨。</p><p>分类：家用电器/洗衣机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (28, '小天鹅 TG100V818 10kg 洗衣机', '小天鹅 TG100V818 10公斤滚筒洗衣机，水魔方护色洗，变频静音。', 2199.00, 240, 0, 0, 0, 302, '[\"/images/products/28_littleswan_10kg/images/01_main.jpg\", \"/images/products/28_littleswan_10kg/images/02_detail.png\", \"/images/products/28_littleswan_10kg/images/03_detail.jpg\"]', '/images/products/28_littleswan_10kg/images/01_main.jpg', '<h3>小天鹅 TG100V818 10kg 洗衣机</h3><p>小天鹅 TG100V818 10公斤滚筒洗衣机，水魔方护色洗，变频静音。</p><p>分类：家用电器/洗衣机</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (29, '海尔 BCD-501WLHFD9DGYU1 冰箱', '海尔 BCD-501 501升十字对开门冰箱，风冷无霜，一级能效。', 4299.00, 150, 0, 0, 0, 303, '[\"/images/products/29_haier_501fridge/images/01_main.jpg\", \"/images/products/29_haier_501fridge/images/02_detail.jpg\", \"/images/products/29_haier_501fridge/images/03_detail.jpg\"]', '/images/products/29_haier_501fridge/images/01_main.jpg', '<h3>海尔 BCD-501WLHFD9DGYU1 冰箱</h3><p>海尔 BCD-501 501升十字对开门冰箱，风冷无霜，一级能效。</p><p>分类：家用电器/冰箱</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (30, '美的 BCD-508WTPZM 冰箱', '美的 BCD-508 508升法式多门冰箱，变频风冷，净味保鲜。', 3999.00, 160, 0, 0, 0, 303, '[\"/images/products/30_midea_508fridge/images/01_main.jpg\", \"/images/products/30_midea_508fridge/images/02_detail.jpg\", \"/images/products/30_midea_508fridge/images/03_detail.jpg\"]', '/images/products/30_midea_508fridge/images/01_main.jpg', '<h3>美的 BCD-508WTPZM 冰箱</h3><p>美的 BCD-508 508升法式多门冰箱，变频风冷，净味保鲜。</p><p>分类：家用电器/冰箱</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (31, '美的 M1-L213B 微波炉', '美的 M1-L213B 微波炉，20升容量，旋钮操作，快捷加热。', 399.00, 400, 0, 0, 0, 304, '[\"/images/products/31_midea_microwave/images/01_main.png\", \"/images/products/31_midea_microwave/images/02_detail.jpg\", \"/images/products/31_midea_microwave/images/03_detail.jpg\"]', '/images/products/31_midea_microwave/images/01_main.png', '<h3>美的 M1-L213B 微波炉</h3><p>美的 M1-L213B 微波炉，20升容量，旋钮操作，快捷加热。</p><p>分类：家用电器/厨房电器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (32, '九阳 L18-Y933 破壁机', '九阳 L18-Y933 破壁机，高速破壁，加热料理，多功能营养机。', 599.00, 380, 0, 0, 0, 304, '[\"/images/products/32_joyoung_blender/images/01_main.jpg\", \"/images/products/32_joyoung_blender/images/02_detail.jpg\", \"/images/products/32_joyoung_blender/images/03_detail.jpg\"]', '/images/products/32_joyoung_blender/images/01_main.jpg', '<h3>九阳 L18-Y933 破壁机</h3><p>九阳 L18-Y933 破壁机，高速破壁，加热料理，多功能营养机。</p><p>分类：家用电器/厨房电器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (33, '戴森 V12 无线吸尘器', '戴森 V12 Detect Slim 无线吸尘器，激光探测，强劲吸力，轻量设计。', 3990.00, 170, 0, 0, 0, 305, '[\"/images/products/33_dyson_v12/images/01_main.jpg\", \"/images/products/33_dyson_v12/images/02_detail.jpg\", \"/images/products/33_dyson_v12/images/03_detail.png\"]', '/images/products/33_dyson_v12/images/01_main.jpg', '<h3>戴森 V12 无线吸尘器</h3><p>戴森 V12 Detect Slim 无线吸尘器，激光探测，强劲吸力，轻量设计。</p><p>分类：家用电器/生活电器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (34, '飞利浦 HD9200 空气炸锅', '飞利浦 HD9200 空气炸锅，无油健康炸烤，大容量家用。', 899.00, 320, 0, 0, 0, 305, '[\"/images/products/34_philips_airfryer/images/01_main.jpg\", \"/images/products/34_philips_airfryer/images/02_detail.jpg\", \"/images/products/34_philips_airfryer/images/03_detail.jpg\"]', '/images/products/34_philips_airfryer/images/01_main.jpg', '<h3>飞利浦 HD9200 空气炸锅</h3><p>飞利浦 HD9200 空气炸锅，无油健康炸烤，大容量家用。</p><p>分类：家用电器/生活电器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (35, '飞利浦 HX9924 电动牙刷', '飞利浦 HX9924 电动牙刷，声波震动，多模式清洁，智能压力感应。', 1299.00, 350, 0, 0, 0, 306, '[\"/images/products/35_philips_toothbrush/images/01_main.jpg\", \"/images/products/35_philips_toothbrush/images/02_detail.jpg\", \"/images/products/35_philips_toothbrush/images/03_detail.png\"]', '/images/products/35_philips_toothbrush/images/01_main.jpg', '<h3>飞利浦 HX9924 电动牙刷</h3><p>飞利浦 HX9924 电动牙刷，声波震动，多模式清洁，智能压力感应。</p><p>分类：家用电器/个护健康</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (36, '博朗 92S 电动剃须刀', '博朗 92S 电动剃须刀，往复式刀头，智能感应，干湿两用。', 1599.00, 300, 0, 0, 0, 306, '[\"/images/products/36_braun_shaver/images/01_main.jpg\", \"/images/products/36_braun_shaver/images/02_detail.jpg\", \"/images/products/36_braun_shaver/images/03_detail.png\"]', '/images/products/36_braun_shaver/images/01_main.jpg', '<h3>博朗 92S 电动剃须刀</h3><p>博朗 92S 电动剃须刀，往复式刀头，智能感应，干湿两用。</p><p>分类：家用电器/个护健康</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (37, '优衣库 男士羊毛衫', '优衣库 男士羊毛衫，柔软保暖，基础百搭针织上衣。', 299.00, 600, 0, 0, 0, 401, '[\"/images/products/37_uniqlo_sweater/images/01_main.jpg\", \"/images/products/37_uniqlo_sweater/images/02_detail.jpg\", \"/images/products/37_uniqlo_sweater/images/03_detail.jpg\"]', '/images/products/37_uniqlo_sweater/images/01_main.jpg', '<h3>优衣库 男士羊毛衫</h3><p>优衣库 男士羊毛衫，柔软保暖，基础百搭针织上衣。</p><p>分类：服饰鞋包/男装</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (38, '海澜之家 男士西装外套', '海澜之家 男士西装外套，修身商务正装，挺括有型。', 599.00, 420, 0, 0, 0, 401, '[\"/images/products/38_heilan_suit/images/01_main.jpg\", \"/images/products/38_heilan_suit/images/02_detail.jpg\", \"/images/products/38_heilan_suit/images/03_detail.jpg\"]', '/images/products/38_heilan_suit/images/01_main.jpg', '<h3>海澜之家 男士西装外套</h3><p>海澜之家 男士西装外套，修身商务正装，挺括有型。</p><p>分类：服饰鞋包/男装</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (39, '优衣库 女装摇粒绒外套', '优衣库 女装摇粒绒外套，轻盈保暖，柔软亲肤。', 199.00, 680, 0, 0, 0, 402, '[\"/images/products/39_uniqlo_fleece/images/01_main.jpg\", \"/images/products/39_uniqlo_fleece/images/02_detail.jpg\", \"/images/products/39_uniqlo_fleece/images/03_detail.jpg\"]', '/images/products/39_uniqlo_fleece/images/01_main.jpg', '<h3>优衣库 女装摇粒绒外套</h3><p>优衣库 女装摇粒绒外套，轻盈保暖，柔软亲肤。</p><p>分类：服饰鞋包/女装</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (40, 'ONLY 女装针织连衣裙', 'ONLY 女装针织连衣裙，修身显瘦，优雅通勤。', 499.00, 360, 0, 0, 0, 402, '[\"/images/products/40_only_dress/images/01_main.jpg\", \"/images/products/40_only_dress/images/02_detail.jpg\", \"/images/products/40_only_dress/images/03_detail.jpg\"]', '/images/products/40_only_dress/images/01_main.jpg', '<h3>ONLY 女装针织连衣裙</h3><p>ONLY 女装针织连衣裙，修身显瘦，优雅通勤。</p><p>分类：服饰鞋包/女装</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (41, '耐克 Air Max 90 运动鞋', '耐克 Air Max 90 经典复古运动鞋，气垫缓震，百搭潮流。', 899.00, 520, 0, 0, 0, 403, '[\"/images/products/41_nike_airmax90/images/01_main.jpg\", \"/images/products/41_nike_airmax90/images/02_detail.jpg\", \"/images/products/41_nike_airmax90/images/03_detail.jpg\"]', '/images/products/41_nike_airmax90/images/01_main.jpg', '<h3>耐克 Air Max 90 运动鞋</h3><p>耐克 Air Max 90 经典复古运动鞋，气垫缓震，百搭潮流。</p><p>分类：服饰鞋包/运动鞋</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (42, '阿迪达斯 Ultraboost 22 运动鞋', '阿迪达斯 Ultraboost 22 跑步鞋，BOOST中底，回弹舒适。', 1099.00, 480, 0, 0, 0, 403, '[\"/images/products/42_adidas_ultraboost/images/01_main.jpg\", \"/images/products/42_adidas_ultraboost/images/02_detail.jpg\", \"/images/products/42_adidas_ultraboost/images/03_detail.jpg\"]', '/images/products/42_adidas_ultraboost/images/01_main.jpg', '<h3>阿迪达斯 Ultraboost 22 运动鞋</h3><p>阿迪达斯 Ultraboost 22 跑步鞋，BOOST中底，回弹舒适。</p><p>分类：服饰鞋包/运动鞋</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (43, '新秀丽 拉杆箱', '新秀丽 拉杆箱，PC箱体轻盈抗压，静音万向轮，出差旅行。', 1299.00, 260, 0, 0, 0, 404, '[\"/images/products/43_samsonite_luggage/images/01_main.jpg\", \"/images/products/43_samsonite_luggage/images/02_detail.jpg\", \"/images/products/43_samsonite_luggage/images/03_detail.jpg\"]', '/images/products/43_samsonite_luggage/images/01_main.jpg', '<h3>新秀丽 拉杆箱</h3><p>新秀丽 拉杆箱，PC箱体轻盈抗压，静音万向轮，出差旅行。</p><p>分类：服饰鞋包/箱包</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (44, '瑞动 双肩包', '瑞动 双肩包，防泼水商务背包，多隔层大容量。', 399.00, 440, 0, 0, 0, 404, '[\"/images/products/44_ruidong_backpack/images/01_main.jpg\", \"/images/products/44_ruidong_backpack/images/02_detail.jpg\", \"/images/products/44_ruidong_backpack/images/03_detail.jpg\"]', '/images/products/44_ruidong_backpack/images/01_main.jpg', '<h3>瑞动 双肩包</h3><p>瑞动 双肩包，防泼水商务背包，多隔层大容量。</p><p>分类：服饰鞋包/箱包</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (45, '巴拉巴拉 儿童羽绒服', '巴拉巴拉 儿童羽绒服，保暖防风，萌趣卡通设计。', 399.00, 500, 0, 0, 0, 405, '[\"/images/products/45_balabala_down/images/01_main.jpg\", \"/images/products/45_balabala_down/images/02_detail.jpg\", \"/images/products/45_balabala_down/images/03_detail.jpg\"]', '/images/products/45_balabala_down/images/01_main.jpg', '<h3>巴拉巴拉 儿童羽绒服</h3><p>巴拉巴拉 儿童羽绒服，保暖防风，萌趣卡通设计。</p><p>分类：服饰鞋包/童装童鞋</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (46, '安踏 儿童运动鞋', '安踏 儿童运动鞋，轻便缓震，透气网面，校园跑步。', 299.00, 620, 0, 0, 0, 405, '[\"/images/products/46_anta_kids/images/01_main.jpg\", \"/images/products/46_anta_kids/images/02_detail.jpg\", \"/images/products/46_anta_kids/images/03_detail.jpg\"]', '/images/products/46_anta_kids/images/01_main.jpg', '<h3>安踏 儿童运动鞋</h3><p>安踏 儿童运动鞋，轻便缓震，透气网面，校园跑步。</p><p>分类：服饰鞋包/童装童鞋</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (47, '曼妮芬 文胸', '曼妮芬 文胸，聚拢舒适无钢圈，亲肤透气。', 199.00, 700, 0, 0, 0, 406, '[\"/images/products/47_maniform_bra/images/01_main.jpg\", \"/images/products/47_maniform_bra/images/02_detail.jpg\", \"/images/products/47_maniform_bra/images/03_detail.jpg\"]', '/images/products/47_maniform_bra/images/01_main.jpg', '<h3>曼妮芬 文胸</h3><p>曼妮芬 文胸，聚拢舒适无钢圈，亲肤透气。</p><p>分类：服饰鞋包/内衣配饰</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (48, '浪莎 棉袜 5双装', '浪莎 棉袜 5双装，精梳棉中筒袜，吸汗透气舒适。', 39.90, 1200, 0, 0, 0, 406, '[\"/images/products/48_langsha_socks/images/01_main.jpg\", \"/images/products/48_langsha_socks/images/02_detail.jpg\", \"/images/products/48_langsha_socks/images/03_detail.png\"]', '/images/products/48_langsha_socks/images/01_main.jpg', '<h3>浪莎 棉袜 5双装</h3><p>浪莎 棉袜 5双装，精梳棉中筒袜，吸汗透气舒适。</p><p>分类：服饰鞋包/内衣配饰</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (49, '兰蔻 清滢柔肤水', '兰蔻 清滢柔肤水（粉水），补水保湿，温和不刺激。', 420.00, 480, 0, 0, 0, 501, '[\"/images/products/49_lancome_toner/images/01_main.jpg\", \"/images/products/49_lancome_toner/images/02_detail.jpg\", \"/images/products/49_lancome_toner/images/03_detail.jpg\"]', '/images/products/49_lancome_toner/images/01_main.jpg', '<h3>兰蔻 清滢柔肤水</h3><p>兰蔻 清滢柔肤水（粉水），补水保湿，温和不刺激。</p><p>分类：美妆个护/面部护理</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (50, '雅诗兰黛 特润修护肌透精华露', '雅诗兰黛 小棕瓶精华，熬夜修护，紧致弹润。', 760.00, 420, 0, 0, 0, 501, '[\"/images/products/50_estee_serum/images/01_main.jpg\", \"/images/products/50_estee_serum/images/02_detail.jpg\", \"/images/products/50_estee_serum/images/03_detail.png\"]', '/images/products/50_estee_serum/images/01_main.jpg', '<h3>雅诗兰黛 特润修护肌透精华露</h3><p>雅诗兰黛 小棕瓶精华，熬夜修护，紧致弹润。</p><p>分类：美妆个护/面部护理</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (51, 'YSL 圣罗兰 纯口红', 'YSL 圣罗兰纯口红，丝绒哑光，显色持久。', 340.00, 540, 0, 0, 0, 502, '[\"/images/products/51_ysl_lipstick/images/01_main.jpg\", \"/images/products/51_ysl_lipstick/images/02_detail.jpg\", \"/images/products/51_ysl_lipstick/images/03_detail.jpg\"]', '/images/products/51_ysl_lipstick/images/01_main.jpg', '<h3>YSL 圣罗兰 纯口红</h3><p>YSL 圣罗兰纯口红，丝绒哑光，显色持久。</p><p>分类：美妆个护/彩妆香水</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (52, 'DIOR 迪奥 真我香水', 'DIOR 迪奥真我女士香水，优雅花香调，持久留香。', 890.00, 360, 0, 0, 0, 502, '[\"/images/products/52_dior_perfume/images/01_main.jpg\", \"/images/products/52_dior_perfume/images/02_detail.jpg\", \"/images/products/52_dior_perfume/images/03_detail.jpg\"]', '/images/products/52_dior_perfume/images/01_main.jpg', '<h3>DIOR 迪奥 真我香水</h3><p>DIOR 迪奥真我女士香水，优雅花香调，持久留香。</p><p>分类：美妆个护/彩妆香水</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (53, '凡士林 经典修护润肤露', '凡士林 经典修护润肤露，深层保湿，缓解干燥。', 49.90, 900, 0, 0, 0, 503, '[\"/images/products/53_vaseline_lotion/images/01_main.jpg\", \"/images/products/53_vaseline_lotion/images/02_detail.jpg\", \"/images/products/53_vaseline_lotion/images/03_detail.jpg\"]', '/images/products/53_vaseline_lotion/images/01_main.jpg', '<h3>凡士林 经典修护润肤露</h3><p>凡士林 经典修护润肤露，深层保湿，缓解干燥。</p><p>分类：美妆个护/身体护理</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (54, '力士 幽莲魅肌沐浴露', '力士 幽莲魅肌沐浴露，持久留香，泡沫丰富。', 39.90, 1000, 0, 0, 0, 503, '[\"/images/products/54_lux_bodywash/images/01_main.png\", \"/images/products/54_lux_bodywash/images/02_detail.jpg\", \"/images/products/54_lux_bodywash/images/03_detail.jpg\"]', '/images/products/54_lux_bodywash/images/01_main.png', '<h3>力士 幽莲魅肌沐浴露</h3><p>力士 幽莲魅肌沐浴露，持久留香，泡沫丰富。</p><p>分类：美妆个护/身体护理</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (55, '潘婷 三分钟奇迹奢护精华素', '潘婷 三分钟奇迹护发素，修护损伤，顺滑易梳。', 59.90, 880, 0, 0, 0, 504, '[\"/images/products/55_pantene_conditioner/images/01_main.jpg\", \"/images/products/55_pantene_conditioner/images/02_detail.jpg\", \"/images/products/55_pantene_conditioner/images/03_detail.jpg\"]', '/images/products/55_pantene_conditioner/images/01_main.jpg', '<h3>潘婷 三分钟奇迹奢护精华素</h3><p>潘婷 三分钟奇迹护发素，修护损伤，顺滑易梳。</p><p>分类：美妆个护/美发护发</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (56, '施华蔻 羊绒脂深层修护洗发露', '施华蔻 羊绒脂洗发露，深层修护，柔顺亮泽。', 69.90, 820, 0, 0, 0, 504, '[\"/images/products/56_schwarzkopf_shampoo/images/01_main.jpg\", \"/images/products/56_schwarzkopf_shampoo/images/02_detail.jpg\", \"/images/products/56_schwarzkopf_shampoo/images/03_detail.jpg\"]', '/images/products/56_schwarzkopf_shampoo/images/01_main.jpg', '<h3>施华蔻 羊绒脂深层修护洗发露</h3><p>施华蔻 羊绒脂洗发露，深层修护，柔顺亮泽。</p><p>分类：美妆个护/美发护发</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (57, '欧莱雅 男士控油洗面奶', '欧莱雅 男士控油洗面奶，深层清洁，清爽控油。', 49.90, 920, 0, 0, 0, 505, '[\"/images/products/57_loreal_menface/images/01_main.png\", \"/images/products/57_loreal_menface/images/02_detail.jpg\", \"/images/products/57_loreal_menface/images/03_detail.jpg\"]', '/images/products/57_loreal_menface/images/01_main.png', '<h3>欧莱雅 男士控油洗面奶</h3><p>欧莱雅 男士控油洗面奶，深层清洁，清爽控油。</p><p>分类：美妆个护/男性护理</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (58, '妮维雅 男士水活多效润肤霜', '妮维雅 男士润肤霜，保湿补水，清爽不油腻。', 59.90, 860, 0, 0, 0, 505, '[\"/images/products/58_nivea_mencream/images/01_main.jpg\", \"/images/products/58_nivea_mencream/images/02_detail.jpg\", \"/images/products/58_nivea_mencream/images/03_detail.jpg\"]', '/images/products/58_nivea_mencream/images/01_main.jpg', '<h3>妮维雅 男士水活多效润肤霜</h3><p>妮维雅 男士润肤霜，保湿补水，清爽不油腻。</p><p>分类：美妆个护/男性护理</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (59, '雅萌 MAX 二代射频仪', '雅萌 MAX 二代射频美容仪，多级射频提拉紧致，家用院线级。', 4999.00, 160, 0, 0, 0, 506, '[\"/images/products/59_ymax_rf/images/01_main.jpg\", \"/images/products/59_ymax_rf/images/02_detail.jpg\", \"/images/products/59_ymax_rf/images/03_detail.jpg\"]', '/images/products/59_ymax_rf/images/01_main.jpg', '<h3>雅萌 MAX 二代射频仪</h3><p>雅萌 MAX 二代射频美容仪，多级射频提拉紧致，家用院线级。</p><p>分类：美妆个护/美容仪器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (60, 'Tripollar STOP X 美容仪', 'Tripollar STOP X 射频美容仪，提拉紧致，淡化细纹。', 3280.00, 150, 0, 0, 0, 506, '[\"/images/products/60_tripollar_stopx/images/01_main.jpg\", \"/images/products/60_tripollar_stopx/images/02_detail.jpg\", \"/images/products/60_tripollar_stopx/images/03_detail.png\"]', '/images/products/60_tripollar_stopx/images/01_main.jpg', '<h3>Tripollar STOP X 美容仪</h3><p>Tripollar STOP X 射频美容仪，提拉紧致，淡化细纹。</p><p>分类：美妆个护/美容仪器</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (61, '三只松鼠 每日坚果 750g', '三只松鼠 每日坚果 750g，30小袋混合果仁，营养便携。', 89.90, 1000, 0, 0, 0, 601, '[\"/images/products/61_threesquirrels_nuts/images/01_main.jpg\", \"/images/products/61_threesquirrels_nuts/images/02_detail.jpg\", \"/images/products/61_threesquirrels_nuts/images/03_detail.jpg\"]', '/images/products/61_threesquirrels_nuts/images/01_main.jpg', '<h3>三只松鼠 每日坚果 750g</h3><p>三只松鼠 每日坚果 750g，30小袋混合果仁，营养便携。</p><p>分类：食品生鲜/休闲零食</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (62, '良品铺子 肉松饼礼盒', '良品铺子 肉松饼礼盒，酥香松软，休闲零食。', 59.90, 900, 0, 0, 0, 601, '[\"/images/products/62_bestore_cake/images/01_main.jpg\", \"/images/products/62_bestore_cake/images/02_detail.jpg\", \"/images/products/62_bestore_cake/images/03_detail.jpg\"]', '/images/products/62_bestore_cake/images/01_main.jpg', '<h3>良品铺子 肉松饼礼盒</h3><p>良品铺子 肉松饼礼盒，酥香松软，休闲零食。</p><p>分类：食品生鲜/休闲零食</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (63, '福临门 东北珍珠米 5kg', '福临门 东北珍珠米 5kg，粒粒饱满，清香软糯。', 49.90, 800, 0, 0, 0, 602, '[\"/images/products/63_fulinmen_rice/images/01_main.jpg\", \"/images/products/63_fulinmen_rice/images/02_detail.jpg\", \"/images/products/63_fulinmen_rice/images/03_detail.jpg\"]', '/images/products/63_fulinmen_rice/images/01_main.jpg', '<h3>福临门 东北珍珠米 5kg</h3><p>福临门 东北珍珠米 5kg，粒粒饱满，清香软糯。</p><p>分类：食品生鲜/米面粮油</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (64, '金龙鱼 葵花籽油 5L', '金龙鱼 葵花籽油 5L，清淡少油烟，家庭烹饪。', 79.90, 700, 0, 0, 0, 602, '[\"/images/products/64_golden_dragon_oil/images/01_main.jpg\", \"/images/products/64_golden_dragon_oil/images/02_detail.jpg\", \"/images/products/64_golden_dragon_oil/images/03_detail.jpg\"]', '/images/products/64_golden_dragon_oil/images/01_main.jpg', '<h3>金龙鱼 葵花籽油 5L</h3><p>金龙鱼 葵花籽油 5L，清淡少油烟，家庭烹饪。</p><p>分类：食品生鲜/米面粮油</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (65, '褚橙 冰糖橙 5kg', '褚橙 冰糖橙 5kg，皮薄多汁，甜酸适口。', 99.00, 500, 0, 0, 0, 603, '[\"/images/products/65_chu_orange/images/01_main.jpg\", \"/images/products/65_chu_orange/images/02_detail.jpg\", \"/images/products/65_chu_orange/images/03_detail.jpg\"]', '/images/products/65_chu_orange/images/01_main.jpg', '<h3>褚橙 冰糖橙 5kg</h3><p>褚橙 冰糖橙 5kg，皮薄多汁，甜酸适口。</p><p>分类：食品生鲜/生鲜水果</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (66, '烟台红富士苹果 5kg', '烟台红富士苹果 5kg，脆甜多汁，新鲜应季。', 49.90, 600, 0, 0, 0, 603, '[\"/images/products/66_redapple/images/01_main.jpg\", \"/images/products/66_redapple/images/02_detail.gif\", \"/images/products/66_redapple/images/03_detail.jpg\"]', '/images/products/66_redapple/images/01_main.jpg', '<h3>烟台红富士苹果 5kg</h3><p>烟台红富士苹果 5kg，脆甜多汁，新鲜应季。</p><p>分类：食品生鲜/生鲜水果</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (67, '茅台 飞天 500ml', '茅台 飞天 53度 500ml，酱香型白酒，经典名酒。', 1499.00, 120, 0, 0, 0, 604, '[\"/images/products/67_maotai/images/01_main.jpg\", \"/images/products/67_maotai/images/02_detail.jpg\", \"/images/products/67_maotai/images/03_detail.jpg\"]', '/images/products/67_maotai/images/01_main.jpg', '<h3>茅台 飞天 500ml</h3><p>茅台 飞天 53度 500ml，酱香型白酒，经典名酒。</p><p>分类：食品生鲜/酒水饮料</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (68, '农夫山泉 矿泉水 550ml×24', '农夫山泉 饮用天然水 550ml×24瓶，天然弱碱性。', 35.90, 1200, 0, 0, 0, 604, '[\"/images/products/68_nongfuspring/images/01_main.jpg\", \"/images/products/68_nongfuspring/images/02_detail.png\", \"/images/products/68_nongfuspring/images/03_detail.png\"]', '/images/products/68_nongfuspring/images/01_main.jpg', '<h3>农夫山泉 矿泉水 550ml×24</h3><p>农夫山泉 饮用天然水 550ml×24瓶，天然弱碱性。</p><p>分类：食品生鲜/酒水饮料</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (69, '立顿 精选红茶包 100片', '立顿 精选红茶包 100片，便捷冲泡，醇香红茶。', 49.90, 900, 0, 0, 0, 605, '[\"/images/products/69_lipton_tea/images/01_main.jpg\", \"/images/products/69_lipton_tea/images/02_detail.jpg\", \"/images/products/69_lipton_tea/images/03_detail.jpg\"]', '/images/products/69_lipton_tea/images/01_main.jpg', '<h3>立顿 精选红茶包 100片</h3><p>立顿 精选红茶包 100片，便捷冲泡，醇香红茶。</p><p>分类：食品生鲜/茶饮咖啡</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (70, '雀巢 1+2 原味咖啡 13g×40条', '雀巢 1+2 原味咖啡 40条，速溶即饮，香浓便捷。', 69.90, 850, 0, 0, 0, 605, '[\"/images/products/70_nestle_coffee/images/01_main.jpg\", \"/images/products/70_nestle_coffee/images/02_detail.jpg\", \"/images/products/70_nestle_coffee/images/03_detail.jpg\"]', '/images/products/70_nestle_coffee/images/01_main.jpg', '<h3>雀巢 1+2 原味咖啡 13g×40条</h3><p>雀巢 1+2 原味咖啡 40条，速溶即饮，香浓便捷。</p><p>分类：食品生鲜/茶饮咖啡</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (71, '汤臣倍健 蛋白粉 1kg', '汤臣倍健 蛋白粉 1kg，动植物双蛋白，补充营养。', 298.00, 600, 0, 0, 0, 606, '[\"/images/products/71_byhealth_protein/images/01_main.jpg\", \"/images/products/71_byhealth_protein/images/02_detail.jpg\", \"/images/products/71_byhealth_protein/images/03_detail.jpg\"]', '/images/products/71_byhealth_protein/images/01_main.jpg', '<h3>汤臣倍健 蛋白粉 1kg</h3><p>汤臣倍健 蛋白粉 1kg，动植物双蛋白，补充营养。</p><p>分类：食品生鲜/营养保健</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (72, 'Swisse 钙片+维生素D 150片', 'Swisse 钙+维生素D 150片，骨骼健康，易吸收。', 129.00, 700, 0, 0, 0, 606, '[\"/images/products/72_swisse_calcium/images/01_main.jpg\", \"/images/products/72_swisse_calcium/images/02_detail.jpg\", \"/images/products/72_swisse_calcium/images/03_detail.jpg\"]', '/images/products/72_swisse_calcium/images/01_main.jpg', '<h3>Swisse 钙片+维生素D 150片</h3><p>Swisse 钙+维生素D 150片，骨骼健康，易吸收。</p><p>分类：食品生鲜/营养保健</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (73, '罗莱家纺 床品四件套', '罗莱家纺 床品四件套，纯棉亲肤，简约印花。', 699.00, 380, 0, 1, 0, 701, '[\"/images/products/73_luolai_bedding/images/01_main.jpg\", \"/images/products/73_luolai_bedding/images/02_detail.jpg\", \"/images/products/73_luolai_bedding/images/03_detail.jpg\"]', '/images/products/73_luolai_bedding/images/01_main.jpg', '<h3>罗莱家纺 床品四件套</h3><p>罗莱家纺 床品四件套，纯棉亲肤，简约印花。</p><p>分类：家居家纺/床品套件</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:35');
INSERT INTO `t_product` VALUES (74, '富安娜 绣花床品四件套', '富安娜 绣花床品四件套，贡缎工艺，优雅卧室。', 899.00, 320, 0, 0, 0, 701, '[\"/images/products/74_funana_bedding/images/01_main.jpg\", \"/images/products/74_funana_bedding/images/02_detail.jpg\", \"/images/products/74_funana_bedding/images/03_detail.jpg\"]', '/images/products/74_funana_bedding/images/01_main.jpg', '<h3>富安娜 绣花床品四件套</h3><p>富安娜 绣花床品四件套，贡缎工艺，优雅卧室。</p><p>分类：家居家纺/床品套件</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (75, '梦洁家纺 95%白鹅绒被', '梦洁家纺 95%白鹅绒被，轻盈保暖，亲肤面料。', 1299.00, 240, 0, 0, 0, 702, '[\"/images/products/75_mengjie_duvet/images/01_main.jpg\", \"/images/products/75_mengjie_duvet/images/02_detail.jpg\", \"/images/products/75_mengjie_duvet/images/03_detail.jpg\"]', '/images/products/75_mengjie_duvet/images/01_main.jpg', '<h3>梦洁家纺 95%白鹅绒被</h3><p>梦洁家纺 95%白鹅绒被，轻盈保暖，亲肤面料。</p><p>分类：家居家纺/被枕芯类</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (76, '8H 乳胶枕头', '8H 天然乳胶枕头，符合人体工学，透气支撑。', 299.00, 460, 0, 0, 0, 702, '[\"/images/products/76_8h_pillow/images/01_main.jpg\", \"/images/products/76_8h_pillow/images/02_detail.jpg\", \"/images/products/76_8h_pillow/images/03_detail.jpg\"]', '/images/products/76_8h_pillow/images/01_main.jpg', '<h3>8H 乳胶枕头</h3><p>8H 天然乳胶枕头，符合人体工学，透气支撑。</p><p>分类：家居家纺/被枕芯类</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (77, '禧天龙 收纳箱 60L', '禧天龙 收纳箱 60L，大容量折叠储物，透明可视。', 79.90, 700, 0, 1, 0, 703, '[\"/images/products/77_xitianlong_box/images/01_main.jpg\", \"/images/products/77_xitianlong_box/images/02_detail.jpg\", \"/images/products/77_xitianlong_box/images/03_detail.jpg\"]', '/images/products/77_xitianlong_box/images/01_main.jpg', '<h3>禧天龙 收纳箱 60L</h3><p>禧天龙 收纳箱 60L，大容量折叠储物，透明可视。</p><p>分类：家居家纺/家居收纳</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:39');
INSERT INTO `t_product` VALUES (78, '太力 真空压缩袋 12件套', '太力 真空压缩袋 12件套，省空间收纳被服。', 89.90, 680, 0, 1, 0, 703, '[\"/images/products/78_taili_bag/images/01_main.jpg\", \"/images/products/78_taili_bag/images/02_detail.jpg\", \"/images/products/78_taili_bag/images/03_detail.jpg\"]', '/images/products/78_taili_bag/images/01_main.jpg', '<h3>太力 真空压缩袋 12件套</h3><p>太力 真空压缩袋 12件套，省空间收纳被服。</p><p>分类：家居家纺/家居收纳</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-05 14:16:07');
INSERT INTO `t_product` VALUES (79, '双立人 锋韵 6件套锅', '双立人 锋韵 6件套锅具，不锈钢复合底，耐用导热。', 1299.00, 260, 0, 1, 0, 704, '[\"/images/products/79_zwilling_pot/images/01_main.jpg\", \"/images/products/79_zwilling_pot/images/02_detail.jpg\", \"/images/products/79_zwilling_pot/images/03_detail.jpg\"]', '/images/products/79_zwilling_pot/images/01_main.jpg', '<h3>双立人 锋韵 6件套锅</h3><p>双立人 锋韵 6件套锅具，不锈钢复合底，耐用导热。</p><p>分类：家居家纺/厨房用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-05 14:16:07');
INSERT INTO `t_product` VALUES (80, '乐扣乐扣 耐热玻璃保鲜盒 6件', '乐扣乐扣 耐热玻璃保鲜盒 6件，密封保鲜，微波炉可用。', 199.00, 559, 1, 2, 1, 704, '[\"/images/products/80_locknlock_box/images/01_main.png\", \"/images/products/80_locknlock_box/images/02_detail.jpg\", \"/images/products/80_locknlock_box/images/03_detail.jpg\"]', '/images/products/80_locknlock_box/images/01_main.png', '<h3>乐扣乐扣 耐热玻璃保鲜盒 6件</h3><p>乐扣乐扣 耐热玻璃保鲜盒 6件，密封保鲜，微波炉可用。</p><p>分类：家居家纺/厨房用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-05 14:16:07');
INSERT INTO `t_product` VALUES (81, '九牧 360°旋转淋浴花洒', '九牧 360°旋转淋浴花洒，增压出水，多档切换。', 699.00, 340, 0, 1, 0, 705, '[\"/images/products/81_jomoo_shower/images/01_main.jpg\", \"/images/products/81_jomoo_shower/images/02_detail.jpg\", \"/images/products/81_jomoo_shower/images/03_detail.png\"]', '/images/products/81_jomoo_shower/images/01_main.jpg', '<h3>九牧 360°旋转淋浴花洒</h3><p>九牧 360°旋转淋浴花洒，增压出水，多档切换。</p><p>分类：家居家纺/卫浴用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:38');
INSERT INTO `t_product` VALUES (82, '箭牌 AE2330M 喷射虹吸马桶', '箭牌 AE2330M 虹吸式马桶，节水静音，易洁釉面。', 1099.00, 200, 0, 1, 0, 705, '[\"/images/products/82_arrow_toilet/images/01_main.jpg\", \"/images/products/82_arrow_toilet/images/02_detail.jpg\", \"/images/products/82_arrow_toilet/images/03_detail.png\"]', '/images/products/82_arrow_toilet/images/01_main.jpg', '<h3>箭牌 AE2330M 喷射虹吸马桶</h3><p>箭牌 AE2330M 虹吸式马桶，节水静音，易洁釉面。</p><p>分类：家居家纺/卫浴用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:37');
INSERT INTO `t_product` VALUES (83, '宜家 KALLAX 卡莱克书架', '宜家 KALLAX 卡莱克书架，方格储物展示，简约百搭。', 399.00, 300, 0, 1, 0, 706, '[\"/images/products/83_ikea_kallax/images/01_main.jpg\", \"/images/products/83_ikea_kallax/images/02_detail.jpg\", \"/images/products/83_ikea_kallax/images/03_detail.jpg\"]', '/images/products/83_ikea_kallax/images/01_main.jpg', '<h3>宜家 KALLAX 卡莱克书架</h3><p>宜家 KALLAX 卡莱克书架，方格储物展示，简约百搭。</p><p>分类：家居家纺/家具装饰</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:37');
INSERT INTO `t_product` VALUES (84, '林氏木业 现代简约布艺沙发', '林氏木业 现代简约布艺沙发，舒适坐感，小户型适用。', 2399.00, 140, 0, 0, 0, 706, '[\"/images/products/84_linsy_sof/images/01_main.jpg\", \"/images/products/84_linsy_sof/images/02_detail.jpg\", \"/images/products/84_linsy_sof/images/03_detail.jpg\"]', '/images/products/84_linsy_sof/images/01_main.jpg', '<h3>林氏木业 现代简约布艺沙发</h3><p>林氏木业 现代简约布艺沙发，舒适坐感，小户型适用。</p><p>分类：家居家纺/家具装饰</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (85, '耐克 男士速干运动T恤', '耐克 男士速干运动T恤，Dri-FIT透气排汗，训练必备。', 199.00, 600, 0, 1, 0, 801, '[\"/images/products/85_nike_tshirt/images/01_main.jpg\", \"/images/products/85_nike_tshirt/images/02_detail.jpg\", \"/images/products/85_nike_tshirt/images/03_detail.jpg\"]', '/images/products/85_nike_tshirt/images/01_main.jpg', '<h3>耐克 男士速干运动T恤</h3><p>耐克 男士速干运动T恤，Dri-FIT透气排汗，训练必备。</p><p>分类：运动户外/运动服饰</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:34');
INSERT INTO `t_product` VALUES (86, '阿迪达斯 男士休闲运动裤', '阿迪达斯 男士休闲运动裤，舒适面料，日常健身。', 259.00, 560, 0, 1, 0, 801, '[\"/images/products/86_adidas_pants/images/01_main.jpg\", \"/images/products/86_adidas_pants/images/02_detail.jpg\", \"/images/products/86_adidas_pants/images/03_detail.jpg\"]', '/images/products/86_adidas_pants/images/01_main.jpg', '<h3>阿迪达斯 男士休闲运动裤</h3><p>阿迪达斯 男士休闲运动裤，舒适面料，日常健身。</p><p>分类：运动户外/运动服饰</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:10:34');
INSERT INTO `t_product` VALUES (87, '舒华 SH-T3900 跑步机', '舒华 SH-T3900 跑步机，家用静音减震，多档变速。', 2999.00, 120, 0, 1, 0, 802, '[\"/images/products/87_shuhua_treadmill/images/01_main.jpg\", \"/images/products/87_shuhua_treadmill/images/02_detail.jpg\", \"/images/products/87_shuhua_treadmill/images/03_detail.jpg\"]', '/images/products/87_shuhua_treadmill/images/01_main.jpg', '<h3>舒华 SH-T3900 跑步机</h3><p>舒华 SH-T3900 跑步机，家用静音减震，多档变速。</p><p>分类：运动户外/健身器材</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 10:00:05');
INSERT INTO `t_product` VALUES (88, '力动 可调节哑铃 20kg', '力动 可调节哑铃 20kg，快速切换重量，居家训练。', 499.00, 300, 0, 0, 0, 802, '[\"/images/products/88_lidong_dumbbell/images/01_main.jpg\", \"/images/products/88_lidong_dumbbell/images/02_detail.jpg\", \"/images/products/88_lidong_dumbbell/images/03_detail.jpg\"]', '/images/products/88_lidong_dumbbell/images/01_main.jpg', '<h3>力动 可调节哑铃 20kg</h3><p>力动 可调节哑铃 20kg，快速切换重量，居家训练。</p><p>分类：运动户外/健身器材</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (89, '探路者 三季帐篷', '探路者 三季帐篷，防雨透气，双人户外露营。', 399.00, 260, 0, 0, 0, 803, '[\"/images/products/89_toread_tent/images/01_main.jpg\", \"/images/products/89_toread_tent/images/02_detail.jpg\", \"/images/products/89_toread_tent/images/03_detail.jpg\"]', '/images/products/89_toread_tent/images/01_main.jpg', '<h3>探路者 三季帐篷</h3><p>探路者 三季帐篷，防雨透气，双人户外露营。</p><p>分类：运动户外/户外装备</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (90, '骆驼 登山包 40L', '骆驼 登山包 40L，大容量防水，徒步旅行背负。', 299.00, 340, 0, 0, 0, 803, '[\"/images/products/90_camel_backpack/images/01_main.jpg\", \"/images/products/90_camel_backpack/images/02_detail.jpg\", \"/images/products/90_camel_backpack/images/03_detail.jpg\"]', '/images/products/90_camel_backpack/images/01_main.jpg', '<h3>骆驼 登山包 40L</h3><p>骆驼 登山包 40L，大容量防水，徒步旅行背负。</p><p>分类：运动户外/户外装备</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (91, '捷安特 Talon 山地自行车', '捷安特 Talon 山地自行车，铝合金车架，避震前叉。', 2299.00, 160, 0, 0, 0, 804, '[\"/images/products/91_giant_talon/images/01_main.jpg\", \"/images/products/91_giant_talon/images/02_detail.jpg\", \"/images/products/91_giant_talon/images/03_detail.jpg\"]', '/images/products/91_giant_talon/images/01_main.jpg', '<h3>捷安特 Talon 山地自行车</h3><p>捷安特 Talon 山地自行车，铝合金车架，避震前叉。</p><p>分类：运动户外/骑行装备</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (92, '洛克兄弟 骑行头盔', '洛克兄弟 骑行头盔，轻量通风，PC外壳EPS内衬。', 199.00, 420, 0, 0, 0, 804, '[\"/images/products/92_rockbros_helmet/images/01_main.jpg\", \"/images/products/92_rockbros_helmet/images/02_detail.jpg\", \"/images/products/92_rockbros_helmet/images/03_detail.jpg\"]', '/images/products/92_rockbros_helmet/images/01_main.jpg', '<h3>洛克兄弟 骑行头盔</h3><p>洛克兄弟 骑行头盔，轻量通风，PC外壳EPS内衬。</p><p>分类：运动户外/骑行装备</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (93, '钓鱼郎 碳素钓鱼竿 5.4米', '钓鱼郎 碳素手竿 5.4米，轻硬挺直，野钓休闲。', 159.00, 500, 0, 0, 0, 805, '[\"/images/products/93_diaoyulang_rod/images/01_main.jpg\", \"/images/products/93_diaoyulang_rod/images/02_detail.jpg\", \"/images/products/93_diaoyulang_rod/images/03_detail.jpg\"]', '/images/products/93_diaoyulang_rod/images/01_main.jpg', '<h3>钓鱼郎 碳素钓鱼竿 5.4米</h3><p>钓鱼郎 碳素手竿 5.4米，轻硬挺直，野钓休闲。</p><p>分类：运动户外/钓鱼用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (94, '龙王恨 台钓线组', '龙王恨 台钓线组，强力耐磨，成品主线绑好即用。', 29.90, 900, 0, 0, 0, 805, '[\"/images/products/94_longwanghen_line/images/01_main.jpg\", \"/images/products/94_longwanghen_line/images/02_detail.jpg\", \"/images/products/94_longwanghen_line/images/03_detail.jpg\"]', '/images/products/94_longwanghen_line/images/01_main.jpg', '<h3>龙王恨 台钓线组</h3><p>龙王恨 台钓线组，强力耐磨，成品主线绑好即用。</p><p>分类：运动户外/钓鱼用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (95, '速比涛 成人泳镜', '速比涛 成人泳镜，防雾高清，贴合舒适。', 99.00, 600, 0, 0, 0, 806, '[\"/images/products/95_speedo_goggles/images/01_main.jpg\", \"/images/products/95_speedo_goggles/images/02_detail.jpg\", \"/images/products/95_speedo_goggles/images/03_detail.jpg\"]', '/images/products/95_speedo_goggles/images/01_main.jpg', '<h3>速比涛 成人泳镜</h3><p>速比涛 成人泳镜，防雾高清，贴合舒适。</p><p>分类：运动户外/游泳装备</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (96, 'arena 阿瑞娜 男士泳裤', 'arena 阿瑞娜 男士泳裤，速干面料，修身竞技。', 129.00, 520, 0, 0, 0, 806, '[\"/images/products/96_arena_swimsuit/images/01_main.jpg\", \"/images/products/96_arena_swimsuit/images/02_detail.jpg\", \"/images/products/96_arena_swimsuit/images/03_detail.jpg\"]', '/images/products/96_arena_swimsuit/images/01_main.jpg', '<h3>arena 阿瑞娜 男士泳裤</h3><p>arena 阿瑞娜 男士泳裤，速干面料，修身竞技。</p><p>分类：运动户外/游泳装备</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (97, '《三体》全集', '《三体》全集（刘慈欣科幻三部曲），雨果奖经典长篇。', 128.00, 800, 0, 0, 0, 901, '[\"/images/products/97_threebody/images/01_main.jpg\", \"/images/products/97_threebody/images/02_detail.jpg\", \"/images/products/97_threebody/images/03_detail.jpg\"]', '/images/products/97_threebody/images/01_main.jpg', '<h3>《三体》全集</h3><p>《三体》全集（刘慈欣科幻三部曲），雨果奖经典长篇。</p><p>分类：图书文娱/图书杂志</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (98, '《读者》合订本', '《读者》合订本，经典文摘半月刊，温暖治愈。', 39.90, 700, 0, 0, 0, 901, '[\"/images/products/98_duzhe/images/01_main.jpg\", \"/images/products/98_duzhe/images/02_detail.jpg\", \"/images/products/98_duzhe/images/03_detail.jpg\"]', '/images/products/98_duzhe/images/01_main.jpg', '<h3>《读者》合订本</h3><p>《读者》合订本，经典文摘半月刊，温暖治愈。</p><p>分类：图书文娱/图书杂志</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (99, '晨光 GP1008 中性笔 12支', '晨光 GP1008 中性笔 12支装，顺滑速干，学生办公。', 19.90, 1500, 0, 0, 0, 902, '[\"/images/products/99_mg_gelpen/images/01_main.jpg\", \"/images/products/99_mg_gelpen/images/02_detail.jpg\", \"/images/products/99_mg_gelpen/images/03_detail.jpg\"]', '/images/products/99_mg_gelpen/images/01_main.jpg', '<h3>晨光 GP1008 中性笔 12支</h3><p>晨光 GP1008 中性笔 12支装，顺滑速干，学生办公。</p><p>分类：图书文娱/文具用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (100, '国誉 Campus 笔记本 A5', '国誉 Campus 笔记本 A5，平摊装订，书写顺滑。', 15.90, 1400, 0, 0, 0, 902, '[\"/images/products/100_kokuyo_notebook/images/01_main.jpg\", \"/images/products/100_kokuyo_notebook/images/02_detail.jpg\", \"/images/products/100_kokuyo_notebook/images/03_detail.jpg\"]', '/images/products/100_kokuyo_notebook/images/01_main.jpg', '<h3>国誉 Campus 笔记本 A5</h3><p>国誉 Campus 笔记本 A5，平摊装订，书写顺滑。</p><p>分类：图书文娱/文具用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (101, '周杰伦《最伟大的作品》专辑CD', '周杰伦《最伟大的作品》实体CD专辑，经典收藏。', 99.00, 500, 0, 0, 0, 903, '[\"/images/products/101_jay_cd/images/01_main.jpg\", \"/images/products/101_jay_cd/images/02_detail.jpg\", \"/images/products/101_jay_cd/images/03_detail.jpg\"]', '/images/products/101_jay_cd/images/01_main.jpg', '<h3>周杰伦《最伟大的作品》专辑CD</h3><p>周杰伦《最伟大的作品》实体CD专辑，经典收藏。</p><p>分类：图书文娱/音乐影视</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (102, '《流浪地球 2》电影BD', '《流浪地球 2》蓝光BD碟，科幻巨制收藏版。', 89.00, 480, 0, 0, 0, 903, '[\"/images/products/102_wandering_earth_bd/images/01_main.jpg\", \"/images/products/102_wandering_earth_bd/images/02_detail.jpg\", \"/images/products/102_wandering_earth_bd/images/03_detail.jpg\"]', '/images/products/102_wandering_earth_bd/images/01_main.jpg', '<h3>《流浪地球 2》电影BD</h3><p>《流浪地球 2》蓝光BD碟，科幻巨制收藏版。</p><p>分类：图书文娱/音乐影视</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (103, '起点读书 VIP年卡', '起点读书 VIP年卡，全网小说畅读，会员权益。', 198.00, 999, 0, 0, 0, 904, '[\"/images/products/103_qidian_vip/images/01_main.png\", \"/images/products/103_qidian_vip/images/02_detail.png\", \"/images/products/103_qidian_vip/images/03_detail.jpg\"]', '/images/products/103_qidian_vip/images/01_main.png', '<h3>起点读书 VIP年卡</h3><p>起点读书 VIP年卡，全网小说畅读，会员权益。</p><p>分类：图书文娱/数字内容</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (104, '网易云音乐 黑胶VIP年卡', '网易云音乐 黑胶VIP年卡，无损音质畅听，会员特权。', 158.00, 999, 0, 0, 0, 904, '[\"/images/products/104_netease_vip/images/01_main.jpg\", \"/images/products/104_netease_vip/images/02_detail.jpg\", \"/images/products/104_netease_vip/images/03_detail.jpg\"]', '/images/products/104_netease_vip/images/01_main.jpg', '<h3>网易云音乐 黑胶VIP年卡</h3><p>网易云音乐 黑胶VIP年卡，无损音质畅听，会员特权。</p><p>分类：图书文娱/数字内容</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (105, '故宫文创 千里江山图书签', '故宫文创 千里江山图书签，国风金属书签，雅致礼品。', 49.90, 600, 0, 0, 0, 905, '[\"/images/products/105_gugong_bookmark/images/01_main.jpg\", \"/images/products/105_gugong_bookmark/images/02_detail.jpg\", \"/images/products/105_gugong_bookmark/images/03_detail.jpg\"]', '/images/products/105_gugong_bookmark/images/01_main.jpg', '<h3>故宫文创 千里江山图书签</h3><p>故宫文创 千里江山图书签，国风金属书签，雅致礼品。</p><p>分类：图书文娱/收藏礼品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (106, '中国集邮 2024年生肖邮票', '中国集邮 2024甲辰龙年生肖邮票，官方发行收藏。', 88.00, 400, 0, 0, 0, 905, '[\"/images/products/106_chinapost_stamp/images/01_main.jpg\", \"/images/products/106_chinapost_stamp/images/02_detail.jpg\", \"/images/products/106_chinapost_stamp/images/03_detail.jpg\"]', '/images/products/106_chinapost_stamp/images/01_main.jpg', '<h3>中国集邮 2024年生肖邮票</h3><p>中国集邮 2024甲辰龙年生肖邮票，官方发行收藏。</p><p>分类：图书文娱/收藏礼品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (107, '好奇 铂金装 纸尿裤 L码', '好奇 铂金装 纸尿裤 L码，柔软透气，海量吸收。', 159.00, 700, 0, 0, 0, 1001, '[\"/images/products/107_huggies_diaper/images/01_main.jpg\", \"/images/products/107_huggies_diaper/images/02_detail.jpg\", \"/images/products/107_huggies_diaper/images/03_detail.jpg\"]', '/images/products/107_huggies_diaper/images/01_main.jpg', '<h3>好奇 铂金装 纸尿裤 L码</h3><p>好奇 铂金装 纸尿裤 L码，柔软透气，海量吸收。</p><p>分类：母婴玩具/婴儿用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (108, '贝亲 宽口径奶瓶 160ml', '贝亲 宽口径奶瓶 160ml，PPSU材质，防胀气奶嘴。', 79.00, 800, 0, 0, 0, 1001, '[\"/images/products/108_pigeon_bottle/images/01_main.jpg\", \"/images/products/108_pigeon_bottle/images/02_detail.jpg\", \"/images/products/108_pigeon_bottle/images/03_detail.jpg\"]', '/images/products/108_pigeon_bottle/images/01_main.jpg', '<h3>贝亲 宽口径奶瓶 160ml</h3><p>贝亲 宽口径奶瓶 160ml，PPSU材质，防胀气奶嘴。</p><p>分类：母婴玩具/婴儿用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (109, '乐高 创意百变高手 10283', '乐高 创意百变高手系列 10283 大众T2面包车，经典收藏拼装。', 3999.00, 120, 0, 0, 0, 1002, '[\"/images/products/109_lego_10283/images/01_main.jpg\", \"/images/products/109_lego_10283/images/02_detail.jpg\", \"/images/products/109_lego_10283/images/03_detail.jpg\"]', '/images/products/109_lego_10283/images/01_main.jpg', '<h3>乐高 创意百变高手 10283</h3><p>乐高 创意百变高手系列 10283 大众T2面包车，经典收藏拼装。</p><p>分类：母婴玩具/玩具模型</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (110, '孩之宝 变形金刚 领袖级', '孩之宝 变形金刚 领袖级，可变形机器人，男孩玩具。', 599.00, 300, 0, 0, 0, 1002, '[\"/images/products/110_hasbro_transformers/images/01_main.jpg\", \"/images/products/110_hasbro_transformers/images/02_detail.jpg\", \"/images/products/110_hasbro_transformers/images/03_detail.jpg\"]', '/images/products/110_hasbro_transformers/images/01_main.jpg', '<h3>孩之宝 变形金刚 领袖级</h3><p>孩之宝 变形金刚 领袖级，可变形机器人，男孩玩具。</p><p>分类：母婴玩具/玩具模型</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (111, '《小王子》绘本版', '《小王子》绘本版，经典童话插图，亲子共读。', 39.90, 800, 0, 0, 0, 1003, '[\"/images/products/111_littleprince/images/01_main.webp\", \"/images/products/111_littleprince/images/02_detail.jpg\", \"/images/products/111_littleprince/images/03_detail.jpg\"]', '/images/products/111_littleprince/images/01_main.webp', '<h3>《小王子》绘本版</h3><p>《小王子》绘本版，经典童话插图，亲子共读。</p><p>分类：母婴玩具/童书绘本</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (112, '《海底两万里》少儿版', '《海底两万里》少儿彩绘版，科幻名著启蒙。', 29.90, 760, 0, 0, 0, 1003, '[\"/images/products/112_20000leagues/images/01_main.jpg\", \"/images/products/112_20000leagues/images/02_detail.jpg\", \"/images/products/112_20000leagues/images/03_detail.jpg\"]', '/images/products/112_20000leagues/images/01_main.jpg', '<h3>《海底两万里》少儿版</h3><p>《海底两万里》少儿彩绘版，科幻名著启蒙。</p><p>分类：母婴玩具/童书绘本</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (113, '十月结晶 孕妇睡衣', '十月结晶 孕妇睡衣，纯棉宽松，哺乳期舒适。', 129.00, 500, 0, 0, 0, 1004, '[\"/images/products/113_october_maternity/images/01_main.jpg\", \"/images/products/113_october_maternity/images/02_detail.jpg\", \"/images/products/113_october_maternity/images/03_detail.jpg\"]', '/images/products/113_october_maternity/images/01_main.jpg', '<h3>十月结晶 孕妇睡衣</h3><p>十月结晶 孕妇睡衣，纯棉宽松，哺乳期舒适。</p><p>分类：母婴玩具/孕产用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (114, '嫚熙 待产包 入院全套', '嫚熙 待产包入院全套，母婴用品一站式准备。', 199.00, 420, 0, 0, 0, 1004, '[\"/images/products/114_munxin_bag/images/01_main.jpg\", \"/images/products/114_munxin_bag/images/02_detail.jpg\", \"/images/products/114_munxin_bag/images/03_detail.jpg\"]', '/images/products/114_munxin_bag/images/01_main.jpg', '<h3>嫚熙 待产包 入院全套</h3><p>嫚熙 待产包入院全套，母婴用品一站式准备。</p><p>分类：母婴玩具/孕产用品</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (115, '嘉宝 婴儿米粉 1段', '嘉宝 婴儿米粉 1段，高铁营养，易冲调初辅食。', 59.90, 700, 0, 0, 0, 1005, '[\"/images/products/115_gerber_rice/images/01_main.jpg\", \"/images/products/115_gerber_rice/images/02_detail.jpg\", \"/images/products/115_gerber_rice/images/03_detail.jpg\"]', '/images/products/115_gerber_rice/images/01_main.jpg', '<h3>嘉宝 婴儿米粉 1段</h3><p>嘉宝 婴儿米粉 1段，高铁营养，易冲调初辅食。</p><p>分类：母婴玩具/营养辅食</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');
INSERT INTO `t_product` VALUES (116, '小皮 苹果香蕉泥 6袋', '小皮 苹果香蕉泥 6袋装，有机果泥，婴标辅食。', 79.90, 650, 0, 0, 0, 1005, '[\"/images/products/116_picpple_puree/images/01_main.png\", \"/images/products/116_picpple_puree/images/02_detail.jpg\", \"/images/products/116_picpple_puree/images/03_detail.png\"]', '/images/products/116_picpple_puree/images/01_main.png', '<h3>小皮 苹果香蕉泥 6袋</h3><p>小皮 苹果香蕉泥 6袋装，有机果泥，婴标辅食。</p><p>分类：母婴玩具/营养辅食</p><ul><li>正品保障，官方/授权渠道同款</li><li>支持七天无理由退换（以实际渠道政策为准）</li><li>详细规格参数以官网页面为准</li></ul>', 'ON_SALE', 0, '2026-08-03 06:23:50', '2026-08-03 06:23:50');

-- ----------------------------
-- Table structure for t_product_review
-- ----------------------------
DROP TABLE IF EXISTS `t_product_review`;
CREATE TABLE `t_product_review`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `order_id` bigint NULL DEFAULT NULL COMMENT '关联订单ID（可选）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',
  `rating` tinyint NOT NULL DEFAULT 5 COMMENT '评分：1-5星',
  `images` json NULL COMMENT '评论图片URL数组',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-显示/0-隐藏',
  `reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '商家回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_product_review
-- ----------------------------
INSERT INTO `t_product_review` VALUES (2083861658318430209, 1002, 2, NULL, '很可以的', 5, NULL, 1, NULL, NULL, 0, '2026-08-02 18:25:10', '2026-08-03 22:23:56');

-- ----------------------------
-- Table structure for t_recharge_card
-- ----------------------------
DROP TABLE IF EXISTS `t_recharge_card`;
CREATE TABLE `t_recharge_card`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `card_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '卡号(唯一)',
  `card_password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '卡密(BCrypt加密存储)',
  `face_value` decimal(10, 2) NOT NULL COMMENT '面额',
  `status` enum('UNUSED','USED','DISABLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/DISABLED-已禁用',
  `used_by` bigint NULL DEFAULT NULL COMMENT '使用者用户ID',
  `used_time` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `batch_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '批次号',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_card_no`(`card_no` ASC) USING BTREE,
  INDEX `idx_batch_no`(`batch_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '充值卡表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_recharge_card
-- ----------------------------
INSERT INTO `t_recharge_card` VALUES (2083861187470057473, '36281930920314979580', '$2a$10$.iSID3LSZdL0E8IsxxUhV.KE116VqWCqBPCU5gcWX1YPOGcSeLsGW', 50.00, 'UNUSED', NULL, NULL, 'B20260802182317', 0, '2026-08-02 18:23:18', '2026-08-02 18:23:18');
INSERT INTO `t_recharge_card` VALUES (2083871743547625474, '62401382180878571473', '$2a$10$FGBjha7fklxoUQ6izhaIpuJM1j9aeU0OBgW5AQ9xkYGQ66Zi1A5V2', 100.00, 'USED', 2, '2026-08-02 19:07:01', 'B20260802190514', 0, '2026-08-02 19:05:15', '2026-08-02 11:07:00');
INSERT INTO `t_recharge_card` VALUES (2084458789097967618, '34995717701477370796', '$2a$10$WiVHBaN5hsJ2heLCH0lrSeMTksnOws0FcFY27XvGWHflAllkFO8ru', 50000.00, 'USED', 2, '2026-08-04 09:58:21', 'B20260804095757', 0, '2026-08-04 09:57:57', '2026-08-04 01:58:20');

-- ----------------------------
-- Table structure for t_seckill_goods
-- ----------------------------
DROP TABLE IF EXISTS `t_seckill_goods`;
CREATE TABLE `t_seckill_goods`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `product_id` bigint NOT NULL COMMENT '关联商品ID',
  `seckill_price` decimal(10, 2) NOT NULL COMMENT '秒杀价格（元）',
  `stock_count` int NOT NULL COMMENT '秒杀总库存',
  `available_count` int NOT NULL COMMENT '可用库存（实时扣减）',
  `start_time` datetime NOT NULL COMMENT '秒杀开始时间',
  `end_time` datetime NOT NULL COMMENT '秒杀结束时间',
  `status` enum('PENDING','ACTIVE','ENDED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待开始/ACTIVE-进行中/ENDED-已结束/CANCELLED-已取消',
  `creator_id` bigint NOT NULL COMMENT '创建人ID（管理员）',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `seckill_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '秒杀活动名称',
  `per_limit` int NOT NULL DEFAULT 1 COMMENT '每人限购数量',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '活动图片(JSON数组)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '活动描述',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_status_start`(`status` ASC, `start_time` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '秒杀活动表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_seckill_goods
-- ----------------------------
INSERT INTO `t_seckill_goods` VALUES (2084297480440176641, 1, 3598.00, 100, 100, '2026-08-04 00:00:00', '2026-08-08 00:00:00', 'ACTIVE', 1, 0, '2026-08-03 23:16:58', '2026-08-06 00:53:38', '小米手机秒杀', 1, '[\"/images/products/01_xiaomi14pro/images/01_main.jpg\",\"/images/products/01_xiaomi14pro/images/02_detail.png\",\"/images/products/01_xiaomi14pro/images/03_detail.jpg\"]', '小米手机秒杀');

-- ----------------------------
-- Table structure for t_seckill_order
-- ----------------------------
DROP TABLE IF EXISTS `t_seckill_order`;
CREATE TABLE `t_seckill_order`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号（唯一，时间戳+随机串）',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `seckill_id` bigint NOT NULL COMMENT '秒杀活动ID',
  `product_id` bigint NOT NULL COMMENT '商品ID（冗余，减少JOIN）',
  `seckill_price` decimal(10, 2) NOT NULL COMMENT '下单时秒杀价（价格快照）',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单总金额（秒杀价×数量）',
  `status` enum('UNPAID','PAID','CANCELLED','TIMEOUT','COMPLETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-待支付/PAID-已支付/CANCELLED-已取消/TIMEOUT-超时/COMPLETED-已完成',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `pay_expire_time` datetime NULL DEFAULT NULL COMMENT '支付截止时间（超时自动取消）',
  `transaction_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方支付流水号',
  `pay_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付方式（ALIPAY/WECHAT等）',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '取消原因',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（下单时间）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ship_time` datetime NULL DEFAULT NULL COMMENT '发货时间',
  `confirm_time` datetime NULL DEFAULT NULL COMMENT '确认收货时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_user_seckill`(`user_id` ASC, `seckill_id` ASC) USING BTREE COMMENT '一人一单约束（核心）',
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_seckill_status`(`seckill_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '秒杀订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_seckill_order
-- ----------------------------
INSERT INTO `t_seckill_order` VALUES (2084977615598329858, 'SK20260805201935674361', 1, 2084297480440176641, 1, 3598.00, 1, 3598.00, 'TIMEOUT', NULL, '2026-08-05 20:34:35', NULL, NULL, '2026-08-05 20:34:35', '超时未支付，自动取消', 0, '2026-08-05 20:19:35', '2026-08-05 20:19:35', NULL, NULL);
INSERT INTO `t_seckill_order` VALUES (2085006853474951169, 'SK20260805221546190058', 2, 2084297480440176641, 1, 3598.00, 1, 3598.00, 'PAID', '2026-08-05 22:15:55', '2026-08-05 22:30:46', 'PAY20260805221555191028', 'ALIPAY', NULL, NULL, 0, '2026-08-05 22:15:46', '2026-08-05 22:15:46', NULL, NULL);

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名（唯一）',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号（唯一）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱地址',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像URL',
  `balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额',
  `role` enum('BUYER','SELLER','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'BUYER' COMMENT '角色：BUYER-买家/SELLER-卖家/ADMIN-管理员',
  `status` enum('ACTIVE','DISABLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常/DISABLED-禁用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, 'admin', '$2a$10$EjQihKeAPFfMKPEuIp59OOK/2t2pss7114YVnk.xnZyXLi2bDvgKu', '13800000000', '2864269343@qq.com', '系统管理员', NULL, 0.00, 'ADMIN', 'ACTIVE', 0, '2026-07-31 11:57:10', '2026-08-05 13:01:00');
INSERT INTO `t_user` VALUES (2, 'buyer01', '$2a$10$EjQihKeAPFfMKPEuIp59OOK/2t2pss7114YVnk.xnZyXLi2bDvgKu', '13900000001', '2864269343@qq.com', '测试买家', NULL, 50100.00, 'BUYER', 'ACTIVE', 0, '2026-07-31 11:57:10', '2026-08-05 13:01:04');

-- ----------------------------
-- Table structure for t_user_address
-- ----------------------------
DROP TABLE IF EXISTS `t_user_address`;
CREATE TABLE `t_user_address`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人手机号',
  `province` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '省份',
  `city` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '城市',
  `district` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '区/县',
  `detail_address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '详细地址',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否/1-是',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_default`(`user_id` ASC, `is_default` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '收货地址表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_address
-- ----------------------------
INSERT INTO `t_user_address` VALUES (2083864352164700162, 2, '吴同学', '13859253073', '福建省', '泉州市', '丰泽区', '泉州信息工程学院', 1, 0, '2026-08-02 18:35:53', '2026-08-02 18:35:53');

-- ----------------------------
-- Table structure for t_user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `t_user_coupon`;
CREATE TABLE `t_user_coupon`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `status` enum('UNUSED','USED','EXPIRED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用/USED-已使用/EXPIRED-已过期',
  `receive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint NULL DEFAULT NULL COMMENT '使用的订单ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_coupon_id`(`coupon_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户优惠券表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_coupon
-- ----------------------------

-- ----------------------------
-- Table structure for t_user_favorite
-- ----------------------------
DROP TABLE IF EXISTS `t_user_favorite`;
CREATE TABLE `t_user_favorite`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常/1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_product`(`user_id` ASC, `product_id` ASC) USING BTREE COMMENT '用户+商品唯一约束',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户收藏夹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_favorite
-- ----------------------------
INSERT INTO `t_user_favorite` VALUES (2084298673325400066, 2, 80, 0, '2026-08-03 23:21:43', '2026-08-03 23:21:43');

SET FOREIGN_KEY_CHECKS = 1;
