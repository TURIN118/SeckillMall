-- ============================================================
-- 01_schema_full.sql 鈥?绉掓潃鍟嗗煄鏁版嵁搴撳畬鏁存灦鏋勫垱寤鸿剼鏈紙鏈€缁堟€侊級
-- MySQL 8.0+ / InnoDB / utf8mb4_general_ci
-- 涓婚敭绛栫暐锛歁yBatis-Plus 闆姳绠楁硶锛圓SSIGN_ID锛?
-- 璇存槑锛氭湰鑴氭湰鏁村悎鑷?sql/01_schema.sql銆乻eckill-mall/src/main/resources/sql/schema.sql銆?
--       migration_v2~v7銆乶ormal_order_tables銆乻eckill_activity_migration銆?
--       seckill_goods_add_columns 绛夋墍鏈夋暎钀?SQL锛屽凡鍚堝苟鍏ㄩ儴杩佺Щ鍙樻洿鑷虫渶缁堟€併€?
--       浠呭寘鍚?CREATE TABLE IF NOT EXISTS 璇彞锛屼笉鍖呭惈浠讳綍 INSERT 鏁版嵁銆?
--       鎵€鏈夎〃鎸変緷璧栭『搴忔帓鍒楋紝鍖呭惈鎵€鏈夌储寮曘€?
-- 琛ㄦ暟閲忥細23 寮?
-- 鐢熸垚鏃堕棿锛?026-08-07
-- ============================================================


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 鍒涘缓鏁版嵁搴?
-- ============================================================
CREATE DATABASE IF NOT EXISTS `seckill_mall`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `seckill_mall`;

-- ============================================================
-- 琛ㄧ粨鏋勫畾涔夛紙鎸変緷璧栭『搴忔帓鍒楋級
-- 渚濊禆閾撅細
--   t_user 鈫?t_category 鈫?t_category_attribute 鈫?t_category_attribute_value
--   鈫?t_product 鈫?t_product_attribute 鈫?t_product_attribute_value 鈫?t_product_sku
--   鈫?t_seckill_activity 鈫?t_seckill_goods 鈫?t_seckill_order
--   鈫?t_user_address 鈫?t_normal_order 鈫?t_normal_order_item
--   鈫?t_cart 鈫?t_user_favorite 鈫?t_product_review
--   鈫?t_coupon 鈫?t_user_coupon 鈫?t_recharge_card
--   鈫?t_banner 鈫?t_login_log 鈫?t_operation_log
-- ============================================================

-- ------------------------------------------------------------
-- 1. t_user 鈥?鐢ㄦ埛琛?
-- 瀛樺偍骞冲彴鎵€鏈夌敤鎴凤紙涔板/鍗栧/绠＄悊鍛橈級锛屽惈閽卞寘浣欓瀛楁
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`          BIGINT       NOT NULL                   COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `username`    VARCHAR(50)  NOT NULL                   COMMENT '鐢ㄦ埛鍚嶏紙鍞竴锛?,
    `password`    VARCHAR(100) NOT NULL                   COMMENT '瀵嗙爜锛圔Crypt鍔犲瘑锛?,
    `phone`       VARCHAR(20)  NULL     DEFAULT NULL      COMMENT '鎵嬫満鍙凤紙鍞竴锛?,
    `email`       VARCHAR(100) NULL     DEFAULT NULL      COMMENT '閭鍦板潃',
    `nickname`    VARCHAR(50)  NULL     DEFAULT NULL      COMMENT '鐢ㄦ埛鏄电О',
    `avatar_url`  VARCHAR(255) NULL     DEFAULT NULL      COMMENT '澶村儚URL',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0.00     COMMENT '閽卞寘浣欓',
    `role`        ENUM('BUYER','SELLER','ADMIN')
                               NOT NULL DEFAULT 'BUYER'  COMMENT '瑙掕壊锛欱UYER-涔板/SELLER-鍗栧/ADMIN-绠＄悊鍛?,
    `status`      ENUM('ACTIVE','DISABLED')
                               NOT NULL DEFAULT 'ACTIVE' COMMENT '鐘舵€侊細ACTIVE-姝ｅ父/DISABLED-绂佺敤',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛琛?;

-- ------------------------------------------------------------
-- 2. t_category 鈥?鍟嗗搧鍒嗙被琛?
-- 鏀寔澶氱骇鍒嗙被锛堣嚜鍏宠仈parent_id锛夛紝涓€绾у垎绫籶arent_id涓篘ULL
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category` (
    `id`          BIGINT       NOT NULL                   COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `name`        VARCHAR(50)  NOT NULL                   COMMENT '鍒嗙被鍚嶇О',
    `parent_id`   BIGINT       NULL     DEFAULT NULL      COMMENT '鐖跺垎绫籌D锛圢ULL=涓€绾у垎绫伙級',
    `sort_order`  INT          NOT NULL DEFAULT 0         COMMENT '鎺掑簭鏉冮噸锛堝€艰秺灏忚秺闈犲墠锛?,
    `icon_url`    VARCHAR(255) NULL     DEFAULT NULL      COMMENT '鍒嗙被鍥炬爣URL',
    `status`      TINYINT      NOT NULL DEFAULT 1         COMMENT '鐘舵€侊細1-鍚敤/0-绂佺敤',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍟嗗搧鍒嗙被琛?;

-- ------------------------------------------------------------
-- 3. t_category_attribute 鈥?鍒嗙被灞炴€фā鏉胯〃
-- 瀛樺偍鍒嗙被绾у埆鐨勮鏍肩淮搴︽ā鏉垮畾涔夛紙濡傘€屾櫤鑳芥墜琛ㄣ€嶅垎绫讳笅瀹氫箟銆岄鑹?鐗堟湰/琛ㄥ甫銆嶏級
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_category_attribute`;
CREATE TABLE `t_category_attribute` (
    `id`          BIGINT       NOT NULL              COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `category_id` BIGINT       NOT NULL              COMMENT '鎵€灞炲垎绫籌D',
    `name`        VARCHAR(50)  NOT NULL              COMMENT '灞炴€у悕绉帮紙濡傦細棰滆壊銆佺増鏈€佽〃甯︼級',
    `type`        ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '灞炴€х被鍨嬶細TEXT-鏂囧瓧/IMAGE-鍥剧墖(鑹插潡)',
    `input_type`  ENUM('SELECT','INPUT') NOT NULL DEFAULT 'SELECT' COMMENT '褰曞叆鏂瑰紡锛歋ELECT-浠庨璁惧€奸€?INPUT-鑷敱杈撳叆',
    `is_required` TINYINT      NOT NULL DEFAULT 0    COMMENT '鏄惁蹇呴€夛細0-鍚?1-鏄?,
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '鎺掑簭鏉冮噸',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍒嗙被灞炴€фā鏉胯〃';

-- ------------------------------------------------------------
-- 4. t_category_attribute_value 鈥?鍒嗙被灞炴€ч璁惧€艰〃
-- 瀛樺偍鍒嗙被灞炴€фā鏉跨殑棰勮鍙€夊€硷紙濡傘€岄鑹层€嶄笅棰勮銆屾洔鐭抽粦/閾舵湀鐧?瑜氶湠绾€嶏級
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_category_attribute_value`;
CREATE TABLE `t_category_attribute_value` (
    `id`           BIGINT       NOT NULL              COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `attribute_id` BIGINT       NOT NULL              COMMENT '鎵€灞炲垎绫诲睘鎬D',
    `value`        VARCHAR(100) NOT NULL              COMMENT '棰勮鍊硷紙濡傦細鏇滅煶榛戙€佹爣鍑嗙増锛?,
    `image_url`    VARCHAR(255) NULL     DEFAULT NULL COMMENT '鍥剧墖绫诲瀷鏃剁殑鑹插潡/鍥剧墖URL',
    `sort_order`   INT          NOT NULL DEFAULT 0    COMMENT '鎺掑簭鏉冮噸',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0    COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_attribute` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍒嗙被灞炴€ч璁惧€艰〃';

-- ------------------------------------------------------------
-- 5. t_product 鈥?鍟嗗搧琛?
-- 鍟嗗搧鍩烘湰淇℃伅銆佸簱瀛樹笌灞曠ず鏁版嵁锛屼富閿娇鐢ˋUTO_INCREMENT
-- 鍖呭惈 SKU 鑱氬悎鍐椾綑瀛楁锛坢in_price/max_price/total_stock锛寁7杩佺Щ锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
    `name`           VARCHAR(200)  NOT NULL               COMMENT '鍟嗗搧鍚嶇О',
    `description`    TEXT          NULL     DEFAULT NULL   COMMENT '鍟嗗搧绠€浠嬶紙绾枃鏈級',
    `original_price` DECIMAL(10,2) NOT NULL               COMMENT '鍘熶环锛堝厓锛?,
    `stock`          INT           NOT NULL DEFAULT 0     COMMENT '鏅€氬簱瀛樻暟閲?,
    `min_price`      DECIMAL(10,2) NULL     DEFAULT NULL   COMMENT 'SKU鏈€浣庝环锛堟湁SKU鏃剁敱Service灞傜淮鎶わ級',
    `max_price`      DECIMAL(10,2) NULL     DEFAULT NULL   COMMENT 'SKU鏈€楂樹环锛堟湁SKU鏃剁敱Service灞傜淮鎶わ級',
    `total_stock`    INT           NULL     DEFAULT NULL   COMMENT 'SKU搴撳瓨姹囨€伙紙鏈塖KU鏃剁敱Service灞傜淮鎶わ級',
    `sales_count`    INT           NOT NULL DEFAULT 0     COMMENT '绱閿€閲?,
    `cart_count`     INT           NOT NULL DEFAULT 0     COMMENT '鍔犺喘鏁伴噺(鍐椾綑璁℃暟)',
    `favorite_count` INT           NOT NULL DEFAULT 0     COMMENT '鏀惰棌鏁伴噺(鍐椾綑璁℃暟)',
    `category_id`    BIGINT        NOT NULL               COMMENT '鎵€灞炲垎绫籌D',
    `images`         JSON          NULL     DEFAULT NULL   COMMENT '鍟嗗搧鍥剧墖URL鏁扮粍',
    `main_image`     VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '涓诲浘URL',
    `detail_html`    TEXT          NULL     DEFAULT NULL   COMMENT '鍟嗗搧璇︽儏锛堝瘜鏂囨湰HTML锛?,
    `status`         ENUM('ON_SALE','OFF_SHELF') NOT NULL DEFAULT 'ON_SALE' COMMENT '鐘舵€侊細ON_SALE-鍦ㄥ敭/OFF_SHELF-涓嬫灦',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0     COMMENT '閫昏緫鍒犻櫎',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_category_status` (`category_id`, `status`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=117 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍟嗗搧琛?;

-- ------------------------------------------------------------
-- 6. t_product_attribute 鈥?鍟嗗搧灞炴€ц〃锛圫KU缁村害瀹氫箟锛?
-- 瀛樺偍鍟嗗搧鐨勮鏍肩淮搴﹀畾涔夛紙濡傘€岄鑹层€嶃€岀増鏈€嶃€岃〃甯︺€嶏級
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_attribute`;
CREATE TABLE `t_product_attribute` (
    `id`                    BIGINT       NOT NULL             COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `product_id`            BIGINT       NOT NULL             COMMENT '鎵€灞炲晢鍝両D',
    `category_attribute_id` BIGINT       NULL     DEFAULT NULL COMMENT '鍏宠仈鍒嗙被灞炴€фā鏉縄D锛圢ULL=鍟嗗搧鑷畾涔夊睘鎬э級',
    `name`                  VARCHAR(50)  NOT NULL             COMMENT '灞炴€у悕锛堝锛氶鑹?鐗堟湰/琛ㄥ甫锛?,
    `type`                  ENUM('IMAGE','TEXT') NOT NULL DEFAULT 'TEXT' COMMENT '灞炴€х被鍨嬶細IMAGE-鍥剧墖鍨?TEXT-鏂囧瓧鍨?,
    `sort_order`            INT          NOT NULL DEFAULT 0   COMMENT '鎺掑簭鏉冮噸锛堝€艰秺灏忚秺闈犲墠锛?,
    `is_deleted`            TINYINT      NOT NULL DEFAULT 0   COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_category_attribute_id` (`category_attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍟嗗搧灞炴€ц〃锛圫KU缁村害瀹氫箟锛?;

-- ------------------------------------------------------------
-- 7. t_product_attribute_value 鈥?鍟嗗搧灞炴€у€艰〃锛圫KU缁村害鍙€夊€硷級
-- 瀛樺偍姣忎釜灞炴€т笅鐨勫彲閫夊€硷紙濡傘€岄鑹层€嶄笅鐨勩€屾洔鐭抽粦銆嶃€岄摱鏈堢櫧銆嶇瓑锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_attribute_value`;
CREATE TABLE `t_product_attribute_value` (
    `id`           BIGINT       NOT NULL             COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `attribute_id` BIGINT       NOT NULL             COMMENT '鎵€灞炲睘鎬D',
    `product_id`   BIGINT       NOT NULL             COMMENT '鎵€灞炲晢鍝両D锛堝啑浣欙紝鍔犻€熸煡璇級',
    `value`        VARCHAR(100) NOT NULL             COMMENT '灞炴€у€硷紙濡傦細鏇滅煶榛?鏃楄埌鐗?姘熸鑳惰〃甯︼級',
    `image_url`    VARCHAR(255) NULL     DEFAULT NULL COMMENT '鍥剧墖鍨嬪睘鎬у€肩殑鑹插潡URL鎴栧浘鐗嘦RL锛堟枃瀛楀瀷涓篘ULL锛?,
    `sort_order`   INT          NOT NULL DEFAULT 0   COMMENT '鎺掑簭鏉冮噸',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0   COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_attribute_id` (`attribute_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍟嗗搧灞炴€у€艰〃锛圫KU缁村害鍙€夊€硷級';

-- ------------------------------------------------------------
-- 8. t_product_sku 鈥?鍟嗗搧SKU琛紙灞炴€у€肩粍鍚堬級
-- 瀛樺偍姣忎釜灞炴€у€肩粍鍚堢殑鐙珛浠锋牸/搴撳瓨/鍥剧墖/缂栫爜/鐘舵€?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_sku`;
CREATE TABLE `t_product_sku` (
    `id`          BIGINT        NOT NULL            COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `product_id`  BIGINT        NOT NULL            COMMENT '鎵€灞炲晢鍝両D',
    `sku_code`    VARCHAR(64)   NOT NULL            COMMENT 'SKU缂栫爜锛堝悓鍟嗗搧鍐呭敮涓€锛岀敱generateSkuCode鍙潬鐢熸垚鎴栧悗鍙版墜濉紝绂佹NULL锛?,
    `price`       DECIMAL(10,2) NOT NULL            COMMENT 'SKU浠锋牸锛堝厓锛?,
    `stock`       INT           NOT NULL DEFAULT 0  COMMENT 'SKU搴撳瓨',
    `main_image`  VARCHAR(255)  NULL     DEFAULT NULL COMMENT 'SKU涓诲浘URL锛圢ULL琛ㄧず娌跨敤鍟嗗搧涓诲浘锛?,
    `attributes`  JSON          NOT NULL            COMMENT 'SKU灞炴€ч敭鍊煎JSON锛屽{"棰滆壊":"鏇滅煶榛?,"鐗堟湰":"鏃楄埌鐗?,"琛ㄥ甫":"姘熸鑳惰〃甯?}',
    `status`      TINYINT       NOT NULL DEFAULT 1  COMMENT '鏄惁鍦ㄥ敭锛?-鍦ㄥ敭/0-鍋滃敭锛堜笌搴撳瓨鏃犲叧锛屽簱瀛樹负0鏃秙tatus浠嶄负1锛屽墠绔洜stock=0缃伆锛?,
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0  COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`product_id`, `sku_code`) COMMENT '鍚屽晢鍝佸唴SKU缂栫爜鍞竴',
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_product_status` (`product_id`, `status`),
    INDEX `idx_product_status_stock` (`product_id`, `status`, `stock`) COMMENT '蹇€熸煡璇㈡煇鍟嗗搧涓嬪簱瀛樺ぇ浜?鐨勫惎鐢⊿KU锛堝墠鍙板睍绀哄彲閫夎鏍硷級'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍟嗗搧SKU琛紙灞炴€у€肩粍鍚堬級';

-- ------------------------------------------------------------
-- 9. t_seckill_activity 鈥?绉掓潃娲诲姩鍦烘琛?
-- 绉掓潃鍦烘鍖栭噸鏋勶細涓€涓満娆″寘鍚涓鏉€鍟嗗搧
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_activity`;
CREATE TABLE `t_seckill_activity` (
    `id`          BIGINT       NOT NULL              COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `name`        VARCHAR(128) NOT NULL              COMMENT '鍦烘鍚嶇О',
    `start_time`  DATETIME     NOT NULL              COMMENT '鍦烘寮€濮嬫椂闂?,
    `end_time`    DATETIME     NOT NULL              COMMENT '鍦烘缁撴潫鏃堕棿',
    `status`      TINYINT      NOT NULL DEFAULT 0    COMMENT '0=寰呭紑濮?1=杩涜涓?2=宸茬粨鏉?,
    `per_limit`   INT          NOT NULL DEFAULT 1    COMMENT '姣忎汉闄愯喘鏁伴噺',
    `description` VARCHAR(512) NULL     DEFAULT NULL COMMENT '鍦烘鎻忚堪',
    `images`      VARCHAR(1024) NULL    DEFAULT NULL COMMENT '鍦烘鍥剧墖(JSON鏁扮粍)',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='绉掓潃娲诲姩鍦烘琛?;

-- ------------------------------------------------------------
-- 10. t_seckill_goods 鈥?绉掓潃娲诲姩琛?
-- 绠＄悊绉掓潃鍟嗗搧銆佸簱瀛樹笌娲诲姩鏃堕棿绐楀彛锛屽惈娲诲姩鍚嶇О/闄愯喘/鍥剧墖/鎻忚堪/鍦烘鍏宠仈
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_goods`;
CREATE TABLE `t_seckill_goods` (
    `id`              BIGINT        NOT NULL            COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `product_id`      BIGINT        NOT NULL            COMMENT '鍏宠仈鍟嗗搧ID',
    `seckill_price`   DECIMAL(10,2) NOT NULL            COMMENT '绉掓潃浠锋牸锛堝厓锛?,
    `stock_count`     INT           NOT NULL            COMMENT '绉掓潃鎬诲簱瀛?,
    `available_count` INT           NOT NULL            COMMENT '鍙敤搴撳瓨锛堝疄鏃舵墸鍑忥級',
    `start_time`      DATETIME      NOT NULL            COMMENT '绉掓潃寮€濮嬫椂闂?,
    `end_time`        DATETIME      NOT NULL            COMMENT '绉掓潃缁撴潫鏃堕棿',
    `status`          ENUM('PENDING','ACTIVE','ENDED','CANCELLED')
                                    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-寰呭紑濮?ACTIVE-杩涜涓?ENDED-宸茬粨鏉?CANCELLED-宸插彇娑?,
    `creator_id`      BIGINT        NOT NULL            COMMENT '鍒涘缓浜篒D锛堢鐞嗗憳锛?,
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '閫昏緫鍒犻櫎',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    `seckill_name`    VARCHAR(255)  NULL     DEFAULT NULL COMMENT '绉掓潃娲诲姩鍚嶇О',
    `per_limit`       INT           NOT NULL DEFAULT 1  COMMENT '姣忎汉闄愯喘鏁伴噺',
    `images`          TEXT          NULL     DEFAULT NULL COMMENT '娲诲姩鍥剧墖(JSON鏁扮粍)',
    `description`     TEXT          NULL     DEFAULT NULL COMMENT '娲诲姩鎻忚堪',
    `activity_id`     BIGINT        NULL     DEFAULT NULL COMMENT '鍏宠仈鍦烘ID',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status_start` (`status`, `start_time`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='绉掓潃娲诲姩琛?;

-- ------------------------------------------------------------
-- 11. t_seckill_order 鈥?绉掓潃璁㈠崟琛?
-- 鏍稿績浜ゆ槗琛紝鍚竴浜轰竴鍗曞敮涓€绾︽潫
-- 鍖呭惈SHIPPED鐘舵€佸強鐗╂祦瀛楁锛坰hipping_company/shipping_no/ship_time/confirm_time锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_order`;
CREATE TABLE `t_seckill_order` (
    `id`               BIGINT        NOT NULL            COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `order_no`         VARCHAR(32)   NOT NULL            COMMENT '璁㈠崟鍙凤紙鍞竴锛屾椂闂存埑+闅忔満涓诧級',
    `user_id`          BIGINT        NOT NULL            COMMENT '涓嬪崟鐢ㄦ埛ID',
    `seckill_id`       BIGINT        NOT NULL            COMMENT '绉掓潃娲诲姩ID',
    `product_id`       BIGINT        NOT NULL            COMMENT '鍟嗗搧ID锛堝啑浣欙紝鍑忓皯JOIN锛?,
    `seckill_price`    DECIMAL(10,2) NOT NULL            COMMENT '涓嬪崟鏃剁鏉€浠凤紙浠锋牸蹇収锛?,
    `quantity`         INT           NOT NULL DEFAULT 1  COMMENT '璐拱鏁伴噺',
    `total_amount`     DECIMAL(10,2) NOT NULL            COMMENT '璁㈠崟鎬婚噾棰濓紙绉掓潃浠访楁暟閲忥級',
    `status`           ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED')
                                     NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-寰呮敮浠?PAID-宸叉敮浠?SHIPPED-宸插彂璐?CANCELLED-宸插彇娑?TIMEOUT-瓒呮椂/COMPLETED-宸插畬鎴?,
    `shipping_company` VARCHAR(50)   NULL     DEFAULT NULL COMMENT '鐗╂祦鍏徃',
    `shipping_no`      VARCHAR(64)   NULL     DEFAULT NULL COMMENT '蹇€掑崟鍙?,
    `ship_time`        DATETIME      NULL     DEFAULT NULL COMMENT '鍙戣揣鏃堕棿',
    `confirm_time`     DATETIME      NULL     DEFAULT NULL COMMENT '纭鏀惰揣鏃堕棿',
    `pay_time`         DATETIME      NULL     DEFAULT NULL COMMENT '鏀粯鏃堕棿',
    `pay_expire_time`  DATETIME      NULL     DEFAULT NULL COMMENT '鏀粯鎴鏃堕棿锛堣秴鏃惰嚜鍔ㄥ彇娑堬級',
    `transaction_id`   VARCHAR(64)   NULL     DEFAULT NULL COMMENT '绗笁鏂规敮浠樻祦姘村彿',
    `pay_method`       VARCHAR(20)   NULL     DEFAULT NULL COMMENT '鏀粯鏂瑰紡锛圓LIPAY/WECHAT绛夛級',
    `cancel_time`      DATETIME      NULL     DEFAULT NULL COMMENT '鍙栨秷鏃堕棿',
    `cancel_reason`    VARCHAR(255)  NULL     DEFAULT NULL COMMENT '鍙栨秷鍘熷洜',
    `is_deleted`       TINYINT       NOT NULL DEFAULT 0  COMMENT '閫昏緫鍒犻櫎',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿锛堜笅鍗曟椂闂达級',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_seckill` (`user_id`, `seckill_id`) COMMENT '涓€浜轰竴鍗曠害鏉燂紙鏍稿績锛?,
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_seckill_status` (`seckill_id`, `status`),
    INDEX `idx_shipping_no` (`shipping_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='绉掓潃璁㈠崟琛?;

-- ------------------------------------------------------------
-- 12. t_user_address 鈥?鏀惰揣鍦板潃琛?
-- 鐢ㄦ埛鏀惰揣淇℃伅绠＄悊锛屾敮鎸侀粯璁ゅ湴鍧€璁剧疆
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_address`;
CREATE TABLE `t_user_address` (
    `id`             BIGINT       NOT NULL             COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `user_id`        BIGINT       NOT NULL             COMMENT '鎵€灞炵敤鎴稩D',
    `receiver_name`  VARCHAR(50)  NOT NULL             COMMENT '鏀惰揣浜哄鍚?,
    `receiver_phone` VARCHAR(20)  NOT NULL             COMMENT '鏀惰揣浜烘墜鏈哄彿',
    `province`       VARCHAR(30)  NOT NULL             COMMENT '鐪佷唤',
    `city`           VARCHAR(30)  NOT NULL             COMMENT '鍩庡競',
    `district`       VARCHAR(30)  NOT NULL             COMMENT '鍖?鍘?,
    `detail_address` VARCHAR(200) NOT NULL             COMMENT '璇︾粏鍦板潃',
    `is_default`     TINYINT      NOT NULL DEFAULT 0   COMMENT '鏄惁榛樿鍦板潃锛?-鍚?1-鏄?,
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0   COMMENT '閫昏緫鍒犻櫎',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏀惰揣鍦板潃琛?;

-- ------------------------------------------------------------
-- 13. t_normal_order 鈥?鏅€氳鍗曡〃锛堢珛鍗宠喘涔?璐墿杞︾粨绠楋級
-- 涓巘_seckill_order鐙珛锛氭湁address_id瀛楁锛屾棤seckill_id瀛楁
-- 鎷嗗垎total_amount/freight_amount/pay_amount锛屽惈remark瀛楁
-- 鍖呭惈SHIPPED鐘舵€佸強鐗╂祦瀛楁锛坰hipping_company/shipping_no/ship_time/confirm_time锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_normal_order`;
CREATE TABLE `t_normal_order` (
    `id`               BIGINT        NOT NULL            COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `order_no`         VARCHAR(32)   NOT NULL            COMMENT '璁㈠崟鍙凤紙鍞竴锛屾椂闂存埑+闅忔満涓诧級',
    `user_id`          BIGINT        NOT NULL            COMMENT '涓嬪崟鐢ㄦ埛ID',
    `address_id`       BIGINT        NOT NULL            COMMENT '鏀惰揣鍦板潃ID',
    `total_amount`     DECIMAL(10,2) NOT NULL            COMMENT '鍟嗗搧鎬婚噾棰濓紙鏄庣粏灏忚涔嬪拰锛?,
    `freight_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '杩愯垂',
    `pay_amount`       DECIMAL(10,2) NOT NULL            COMMENT '瀹炰粯閲戦 = total_amount + freight_amount',
    `status`           ENUM('UNPAID','PAID','SHIPPED','CANCELLED','TIMEOUT','COMPLETED')
                                     NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID-寰呮敮浠?PAID-宸叉敮浠?SHIPPED-宸插彂璐?CANCELLED-宸插彇娑?TIMEOUT-瓒呮椂/COMPLETED-宸插畬鎴?,
    `shipping_company` VARCHAR(50)   NULL     DEFAULT NULL COMMENT '鐗╂祦鍏徃',
    `shipping_no`      VARCHAR(64)   NULL     DEFAULT NULL COMMENT '蹇€掑崟鍙?,
    `ship_time`        DATETIME      NULL     DEFAULT NULL COMMENT '鍙戣揣鏃堕棿',
    `confirm_time`     DATETIME      NULL     DEFAULT NULL COMMENT '纭鏀惰揣鏃堕棿',
    `pay_method`       VARCHAR(20)   NULL     DEFAULT NULL COMMENT '鏀粯鏂瑰紡锛圵ALLET/ALIPAY/WECHAT绛夛級',
    `transaction_id`   VARCHAR(64)   NULL     DEFAULT NULL COMMENT '鏀粯娴佹按鍙?,
    `pay_time`         DATETIME      NULL     DEFAULT NULL COMMENT '鏀粯鏃堕棿',
    `pay_expire_time`  DATETIME      NULL     DEFAULT NULL COMMENT '鏀粯鎴鏃堕棿锛堣秴鏃惰嚜鍔ㄥ彇娑堬級',
    `cancel_time`      DATETIME      NULL     DEFAULT NULL COMMENT '鍙栨秷鏃堕棿',
    `cancel_reason`    VARCHAR(255)  NULL     DEFAULT NULL COMMENT '鍙栨秷鍘熷洜',
    `remark`           VARCHAR(255)  NULL     DEFAULT NULL COMMENT '鐢ㄦ埛涓嬪崟澶囨敞',
    `is_deleted`       TINYINT       NOT NULL DEFAULT 0   COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿锛堜笅鍗曟椂闂达級',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_user_create` (`user_id`, `create_time`),
    INDEX `idx_shipping_no` (`shipping_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏅€氳鍗曡〃锛堢珛鍗宠喘涔?璐墿杞︾粨绠楋級';

-- ------------------------------------------------------------
-- 14. t_normal_order_item 鈥?鏅€氳鍗曟槑缁嗚〃
-- 涓€涓鍗曞搴斿涓槑缁嗚锛涘啑浣欏晢鍝佸悕绉?涓诲浘/鍗曚环淇濈暀涓嬪崟鏃跺埢蹇収
-- 鍖呭惈 SKU 蹇収瀛楁锛坰ku_id/sku_attributes锛寁7杩佺Щ锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_normal_order_item`;
CREATE TABLE `t_normal_order_item` (
    `id`              BIGINT        NOT NULL            COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `order_id`        BIGINT        NOT NULL            COMMENT '鎵€灞炴櫘閫氳鍗旾D',
    `product_id`      BIGINT        NOT NULL            COMMENT '鍟嗗搧ID',
    `sku_id`          BIGINT        NULL     DEFAULT NULL COMMENT 'SKU ID锛堜笅鍗曟椂蹇収锛孨ULL琛ㄧず鏃犺鏍硷級',
    `sku_attributes`  VARCHAR(500)  NULL     DEFAULT NULL COMMENT 'SKU灞炴€у揩鐓э紙濡傦細鏇滅煶榛?/ 鏃楄埌鐗?/ 姘熸鑳惰〃甯︼級',
    `product_name`    VARCHAR(200)  NOT NULL            COMMENT '鍟嗗搧鍚嶇О锛堜笅鍗曟椂蹇収锛?,
    `product_image`   VARCHAR(255)  NULL     DEFAULT NULL COMMENT '鍟嗗搧涓诲浘URL锛堜笅鍗曟椂蹇収锛?,
    `unit_price`      DECIMAL(10,2) NOT NULL            COMMENT '鍟嗗搧鍗曚环锛堜笅鍗曟椂蹇収锛屽師浠凤級',
    `quantity`        INT           NOT NULL DEFAULT 1  COMMENT '璐拱鏁伴噺',
    `subtotal`        DECIMAL(10,2) NOT NULL            COMMENT '灏忚閲戦 = unit_price 脳 quantity',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0  COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鏅€氳鍗曟槑缁嗚〃';

-- ------------------------------------------------------------
-- 15. t_cart 鈥?璐墿杞﹁〃
-- 鐢ㄦ埛鍔犺喘鍟嗗搧淇℃伅锛屽惈SKU缁村害锛屽敮涓€绾︽潫涓?user_id, product_id, sku_id)
-- sku_id=0 琛ㄧず鏃犺鏍煎晢鍝侊紙v7杩佺Щ锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart` (
    `id`          BIGINT   NOT NULL             COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `user_id`     BIGINT   NOT NULL             COMMENT '鐢ㄦ埛ID',
    `product_id`  BIGINT   NOT NULL             COMMENT '鍟嗗搧ID',
    `sku_id`      BIGINT   NOT NULL DEFAULT 0   COMMENT 'SKU ID锛?琛ㄧず鏃犺鏍煎晢鍝侊紝闈?涓哄叿浣揝KU锛?,
    `quantity`    INT      NOT NULL DEFAULT 1   COMMENT '鍔犺喘鏁伴噺',
    `selected`    TINYINT  NOT NULL DEFAULT 1   COMMENT '鏄惁閫変腑锛?-鍚?1-鏄?,
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0   COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`) COMMENT '鐢ㄦ埛+鍟嗗搧+SKU鍞竴绾︽潫锛坰ku_id=0琛ㄧず鏃犺鏍硷級',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='璐墿杞﹁〃';

-- ------------------------------------------------------------
-- 16. t_user_favorite 鈥?鐢ㄦ埛鏀惰棌澶硅〃
-- 鐢ㄦ埛鏀惰棌鐨勫晢鍝侊紝鍚屼竴鐢ㄦ埛鍚屼竴鍟嗗搧鍞竴绾︽潫
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_favorite`;
CREATE TABLE `t_user_favorite` (
    `id`          BIGINT       NOT NULL                COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `user_id`     BIGINT       NOT NULL                COMMENT '鐢ㄦ埛ID',
    `product_id`  BIGINT       NOT NULL                COMMENT '鍟嗗搧ID',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0      COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '鐢ㄦ埛+鍟嗗搧鍞竴绾︽潫',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛鏀惰棌澶硅〃';

-- ------------------------------------------------------------
-- 17. t_product_review 鈥?鍟嗗搧璇勮琛?
-- 鐢ㄦ埛瀵瑰晢鍝佺殑璇勪环锛屾敮鎸佽瘎鍒嗐€佸浘鐗囥€佸晢瀹跺洖澶嶃€丼KU蹇収
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_product_review`;
CREATE TABLE `t_product_review` (
    `id`              BIGINT       NOT NULL                COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `product_id`      BIGINT       NOT NULL                COMMENT '鍟嗗搧ID',
    `sku_id`          BIGINT       NULL     DEFAULT NULL   COMMENT 'SKU ID锛圢ULL琛ㄧず鏃犺鏍艰瘎璁猴級',
    `sku_attributes`  VARCHAR(500) NULL     DEFAULT NULL   COMMENT 'SKU灞炴€у揩鐓э紙濡傦細鏇滅煶榛?/ 鏃楄埌鐗?/ 姘熸鑳惰〃甯︼級',
    `user_id`         BIGINT       NOT NULL                COMMENT '璇勮鐢ㄦ埛ID',
    `order_id`        BIGINT       NULL     DEFAULT NULL   COMMENT '鍏宠仈璁㈠崟ID锛堝彲閫夛級',
    `content`         TEXT         NOT NULL                COMMENT '璇勮鍐呭',
    `rating`          TINYINT      NOT NULL DEFAULT 5      COMMENT '璇勫垎锛?-5鏄?,
    `images`          JSON         NULL     DEFAULT NULL   COMMENT '璇勮鍥剧墖URL鏁扮粍',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '鐘舵€侊細1-鏄剧ず/0-闅愯棌',
    `reply_content`   TEXT         NULL     DEFAULT NULL   COMMENT '鍟嗗鍥炲鍐呭',
    `reply_time`      DATETIME     NULL     DEFAULT NULL   COMMENT '鍥炲鏃堕棿',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_sku_id` (`sku_id`),
    INDEX `idx_product_status_create` (`product_id`, `status`, `create_time`) COMMENT '鍔犻€熷晢鍝佽瘎璁哄垎椤垫煡璇紙M-K6淇锛?
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍟嗗搧璇勮琛?;

-- ------------------------------------------------------------
-- 18. t_coupon 鈥?浼樻儬鍒歌〃
-- 鏀寔婊″噺鍒?AMOUNT)鍜屾姌鎵ｅ埜(DISCOUNT)涓ょ绫诲瀷
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_coupon`;
CREATE TABLE `t_coupon` (
    `id`              BIGINT        NOT NULL               COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `name`            VARCHAR(100)  NOT NULL               COMMENT '浼樻儬鍒稿悕绉?,
    `type`            ENUM('AMOUNT','DISCOUNT') NOT NULL   COMMENT '绫诲瀷锛欰MOUNT-婊″噺/DISCOUNT-鎶樻墸',
    `amount`          DECIMAL(10,2) NOT NULL               COMMENT '婊″噺閲戦鎴栨姌鎵ｅ€?濡?.85琛ㄧず85鎶?',
    `min_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '鏈€浣庢秷璐归噾棰?,
    `total_count`     INT           NOT NULL               COMMENT '鍙戞斁鎬绘暟',
    `received_count`  INT           NOT NULL DEFAULT 0     COMMENT '宸查鍙栨暟',
    `used_count`      INT           NOT NULL DEFAULT 0     COMMENT '宸蹭娇鐢ㄦ暟',
    `start_time`      DATETIME      NOT NULL               COMMENT '鏈夋晥鏈熷紑濮?,
    `end_time`        DATETIME      NOT NULL               COMMENT '鏈夋晥鏈熺粨鏉?,
    `status`          TINYINT       NOT NULL DEFAULT 1     COMMENT '鐘舵€侊細1-鍚敤/0-鍋滅敤',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0     COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='浼樻儬鍒歌〃';

-- ------------------------------------------------------------
-- 19. t_user_coupon 鈥?鐢ㄦ埛浼樻儬鍒歌〃
-- 鐢ㄦ埛棰嗗彇鐨勪紭鎯犲埜璁板綍锛屽叧鑱斾紭鎯犲埜鍜岃鍗?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user_coupon`;
CREATE TABLE `t_user_coupon` (
    `id`           BIGINT       NOT NULL               COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `user_id`      BIGINT       NOT NULL               COMMENT '鐢ㄦ埛ID',
    `coupon_id`    BIGINT       NOT NULL               COMMENT '浼樻儬鍒窱D',
    `status`       ENUM('UNUSED','USED','EXPIRED') NOT NULL DEFAULT 'UNUSED' COMMENT '鐘舵€侊細UNUSED-鏈娇鐢?USED-宸蹭娇鐢?EXPIRED-宸茶繃鏈?,
    `receive_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '棰嗗彇鏃堕棿',
    `use_time`     DATETIME     NULL     DEFAULT NULL  COMMENT '浣跨敤鏃堕棿',
    `order_id`     BIGINT       NULL     DEFAULT NULL  COMMENT '浣跨敤鐨勮鍗旾D',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0     COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_coupon_id` (`coupon_id`),
    INDEX `idx_user_status_create` (`user_id`, `status`, `create_time`) COMMENT '鍔犻€?鎴戠殑浼樻儬鍒?鍒嗛〉鏌ヨ锛圡-K6淇锛?,
    INDEX `idx_order_id` (`order_id`) COMMENT '鍔犻€熻鍗曞叧鑱斾紭鎯犲埜鏌ヨ锛圡-K6淇锛?
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐢ㄦ埛浼樻儬鍒歌〃';

-- ------------------------------------------------------------
-- 20. t_recharge_card 鈥?鍏呭€煎崱琛?
-- 棰勪粯璐瑰厖鍊煎崱锛屾敮鎸佹壒娆＄鐞嗭紝鍗″瘑BCrypt鍔犲瘑瀛樺偍
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_recharge_card`;
CREATE TABLE `t_recharge_card` (
    `id`            BIGINT        NOT NULL               COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `card_no`       VARCHAR(32)   NOT NULL               COMMENT '鍗″彿(鍞竴)',
    `card_password` VARCHAR(64)   NOT NULL               COMMENT '鍗″瘑(BCrypt鍔犲瘑瀛樺偍)',
    `face_value`    DECIMAL(10,2) NOT NULL               COMMENT '闈㈤',
    `status`        ENUM('UNUSED','USED','DISABLED') NOT NULL DEFAULT 'UNUSED' COMMENT '鐘舵€侊細UNUSED-鏈娇鐢?USED-宸蹭娇鐢?DISABLED-宸茬鐢?,
    `used_by`       BIGINT        NULL     DEFAULT NULL  COMMENT '浣跨敤鑰呯敤鎴稩D',
    `used_time`     DATETIME      NULL     DEFAULT NULL  COMMENT '浣跨敤鏃堕棿',
    `batch_no`      VARCHAR(32)   NOT NULL               COMMENT '鎵规鍙?,
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0     COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_card_no` (`card_no`),
    INDEX `idx_batch_no` (`batch_no`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鍏呭€煎崱琛?;

-- ------------------------------------------------------------
-- 21. t_banner 鈥?杞挱鍥捐〃
-- 瀛樺偍棣栭〉杞挱鍥句俊鎭紝鏀寔鎺掑簭鍜屽惎鐢?绂佺敤鎺у埗
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_banner`;
CREATE TABLE `t_banner` (
    `id`          BIGINT       NOT NULL                   COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `title`       VARCHAR(100) NULL     DEFAULT NULL      COMMENT '杞挱鍥炬爣棰?,
    `image_url`   VARCHAR(500) NOT NULL                   COMMENT '鍥剧墖URL',
    `link_url`    VARCHAR(500) NULL     DEFAULT NULL      COMMENT '鐐瑰嚮璺宠浆閾炬帴',
    `sort_order`  INT          NOT NULL DEFAULT 0         COMMENT '鎺掑簭鏉冮噸锛堝€艰秺灏忚秺闈犲墠锛?,
    `status`      TINYINT      NOT NULL DEFAULT 1         COMMENT '鐘舵€侊細1-鍚敤/0-绂佺敤',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0         COMMENT '閫昏緫鍒犻櫎锛?-姝ｅ父/1-宸插垹闄?,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '鍒涘缓鏃堕棿',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='杞挱鍥捐〃';

-- ------------------------------------------------------------
-- 22. t_login_log 鈥?鐧诲綍鏃ュ織琛?
-- 璁板綍鐢ㄦ埛鐧诲綍琛屼负锛宎ppend-only璁捐锛堟棤update_time/is_deleted锛?
-- M-S5 淇锛歶ser_id 鍏佽涓?NULL锛屼笉瀛樺湪鐢ㄦ埛鐧诲綍澶辫触鏃朵篃鑳借褰曟棩蹇?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_login_log`;
CREATE TABLE `t_login_log` (
    `id`             BIGINT       NOT NULL                COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `user_id`        BIGINT       NULL     DEFAULT NULL   COMMENT '鐢ㄦ埛ID锛圢ULL=鐢ㄦ埛涓嶅瓨鍦級',
    `login_ip`       VARCHAR(50)  NOT NULL                COMMENT '鐧诲綍IP鍦板潃',
    `login_location` VARCHAR(100) NULL     DEFAULT NULL   COMMENT '鐧诲綍鍦扮偣锛圛P瑙ｆ瀽鍩庡競锛?,
    `user_agent`     VARCHAR(500) NULL     DEFAULT NULL   COMMENT '娴忚鍣║ser-Agent',
    `login_result`   ENUM('SUCCESS','FAILED') NOT NULL    COMMENT '鐧诲綍缁撴灉锛歋UCCESS-鎴愬姛/FAILED-澶辫触',
    `fail_reason`    VARCHAR(255) NULL     DEFAULT NULL   COMMENT '澶辫触鍘熷洜',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鐧诲綍鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `create_time`),
    INDEX `idx_ip_time` (`login_ip`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鐧诲綍鏃ュ織琛?;

-- ------------------------------------------------------------
-- 23. t_operation_log 鈥?鎿嶄綔鏃ュ織琛紙鍚庡彴锛?
-- 绠＄悊鍛樻搷浣滃璁¤拷韪紝append-only璁捐锛堟棤update_time/is_deleted锛?
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
    `id`          BIGINT       NOT NULL                COMMENT '涓婚敭ID锛堥洩鑺辩畻娉曪級',
    `operator_id` BIGINT       NOT NULL                COMMENT '鎿嶄綔浜篒D',
    `module`      VARCHAR(50)  NOT NULL                COMMENT '鎿嶄綔妯″潡锛堝晢鍝佺鐞?鐢ㄦ埛绠＄悊/绉掓潃绠＄悊绛夛級',
    `action`      VARCHAR(50)  NOT NULL                COMMENT '鎿嶄綔绫诲瀷锛圕REATE/UPDATE/DELETE/EXPORT绛夛級',
    `target_id`   VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '鎿嶄綔鐩爣ID',
    `target_type` VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '鎿嶄綔鐩爣绫诲瀷锛圥RODUCT/SECKILL/USER绛夛級',
    `detail`      TEXT         NULL     DEFAULT NULL   COMMENT '鎿嶄綔璇︽儏锛圝SON鍙樻洿蹇収锛?,
    `ip_address`  VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '鎿嶄綔IP鍦板潃',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鎿嶄綔鏃堕棿',
    PRIMARY KEY (`id`),
    INDEX `idx_operator_time` (`operator_id`, `create_time`),
    INDEX `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='鎿嶄綔鏃ュ織琛紙鍚庡彴锛?;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 寤鸿〃瀹屾垚锛屽叡 23 寮犺〃
-- 琛ㄦ竻鍗曪細
--   t_user, t_category, t_category_attribute, t_category_attribute_value,
--   t_product, t_product_attribute, t_product_attribute_value, t_product_sku,
--   t_seckill_activity, t_seckill_goods, t_seckill_order,
--   t_user_address, t_normal_order, t_normal_order_item,
--   t_cart, t_user_favorite, t_product_review,
--   t_coupon, t_user_coupon, t_recharge_card,
--   t_banner, t_login_log, t_operation_log
-- ============================================================
