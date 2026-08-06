2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-2] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-2] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:20.541+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:20.543+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:20.543+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:20.543+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:20.543+08:00 DEBUG 30256 --- [nio-8080-exec-2] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:20.546+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:20.546+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.CartMapper.selectCount   : ==>  Preparing: SELECT COUNT( * ) AS total FROM t_cart WHERE is_deleted=0 AND (user_id = ?)
2026-08-05T20:06:20.546+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.CartMapper.selectCount   : ==> Parameters: 1(Long)
2026-08-05T20:06:20.549+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.BannerMapper.selectList  : ==>  Preparing: SELECT id,title,image_url,link_url,sort_order,status,is_deleted,create_time,update_time FROM t_banner WHERE is_deleted=0 AND (status = ?) ORDER BY sort_order ASC,id ASC
2026-08-05T20:06:20.550+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.BannerMapper.selectList  : ==> Parameters: 1(Integer)
2026-08-05T20:06:20.550+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.CartMapper.selectCount   : <==      Total: 1
2026-08-05T20:06:20.553+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.BannerMapper.selectList  : <==      Total: 2
2026-08-05T20:06:20.561+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.S.selectSeckillPage_mpCount      : ==>  Preparing: SELECT COUNT(*) AS total FROM t_seckill_goods WHERE is_deleted = 0
2026-08-05T20:06:20.561+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.S.selectSeckillPage_mpCount      : ==> Parameters: 
2026-08-05T20:06:20.563+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.P.selectProductPage_mpCount      : ==>  Preparing: SELECT COUNT(*) AS total FROM t_product WHERE is_deleted = 0 AND status = ?
2026-08-05T20:06:20.563+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.S.selectSeckillPage_mpCount      : <==      Total: 1
2026-08-05T20:06:20.563+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.P.selectProductPage_mpCount      : ==> Parameters: ON_SALE(String)
2026-08-05T20:06:20.563+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.S.selectSeckillPage              : ==>  Preparing: SELECT id, product_id, seckill_price, stock_count, available_count, start_time, end_time, status, creator_id, seckill_name, per_limit, images, description, is_deleted, create_time, update_time FROM t_seckill_goods WHERE is_deleted = 0 ORDER BY start_time DESC LIMIT ?
2026-08-05T20:06:20.563+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.S.selectSeckillPage              : ==> Parameters: 20(Long)
2026-08-05T20:06:20.564+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.P.selectProductPage_mpCount      : <==      Total: 1
2026-08-05T20:06:20.564+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.S.selectSeckillPage              : <==      Total: 1
2026-08-05T20:06:20.564+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.ProductMapper.selectProductPage  : ==>  Preparing: SELECT id, name, description, original_price, stock, sales_count, category_id, images, main_image, detail_html, status, is_deleted, create_time, update_time FROM t_product WHERE is_deleted = 0 AND status = ? ORDER BY sales_count DESC LIMIT ?
2026-08-05T20:06:20.565+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.ProductMapper.selectProductPage  : ==> Parameters: ON_SALE(String), 12(Long)
2026-08-05T20:06:20.565+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.ProductMapper.selectBatchIds     : ==>  Preparing: SELECT id,name,description,original_price,stock,sales_count,cart_count,favorite_count,category_id,images,main_image,detail_html,status,is_deleted,create_time,update_time FROM t_product WHERE id IN ( ? ) AND is_deleted=0
2026-08-05T20:06:20.565+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.ProductMapper.selectBatchIds     : ==> Parameters: 1(Long)
2026-08-05T20:06:20.567+08:00 DEBUG 30256 --- [nio-8080-exec-8] c.s.m.m.ProductMapper.selectBatchIds     : <==      Total: 1
2026-08-05T20:06:20.570+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.ProductMapper.selectProductPage  : <==      Total: 12
2026-08-05T20:06:20.571+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.CategoryMapper.selectBatchIds    : ==>  Preparing: SELECT id,name,parent_id,sort_order,icon_url,status,is_deleted,create_time,update_time FROM t_category WHERE id IN ( ? , ? , ? , ? , ? , ? , ? , ? ) AND is_deleted=0
2026-08-05T20:06:20.571+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.CategoryMapper.selectBatchIds    : ==> Parameters: 704(Long), 703(Long), 705(Long), 701(Long), 702(Long), 801(Long), 706(Long), 802(Long)
2026-08-05T20:06:20.573+08:00 DEBUG 30256 --- [io-8080-exec-10] c.s.m.m.CategoryMapper.selectBatchIds    : <==      Total: 8
2026-08-05T20:06:22.916+08:00  WARN 30256 --- [nio-8080-exec-2] c.s.m.security.ReplayProtectionFilter    : 签名不匹配 uri=/api/v1/seckill/2084297480440176641/execute
2026-08-05T20:06:23.159+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:23.159+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:23.160+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:23.237+08:00  WARN 30256 --- [nio-8080-exec-8] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:23.500+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:23.500+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:23.501+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:23.544+08:00  WARN 30256 --- [io-8080-exec-10] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:23.837+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:23.837+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:23.838+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:23.862+08:00  WARN 30256 --- [nio-8080-exec-6] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:24.174+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:24.174+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:24.175+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:24.437+08:00  WARN 30256 --- [nio-8080-exec-5] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:24.516+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:24.516+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:24.528+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:24.759+08:00  WARN 30256 --- [nio-8080-exec-2] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:24.867+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:24.867+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:24.868+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:25.078+08:00  WARN 30256 --- [nio-8080-exec-8] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:25.204+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:25.204+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:25.206+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:25.387+08:00  WARN 30256 --- [io-8080-exec-10] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:25.542+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:25.542+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:25.544+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:25.696+08:00  WARN 30256 --- [nio-8080-exec-6] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:25.874+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:25.875+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:25.876+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:26.018+08:00  WARN 30256 --- [nio-8080-exec-5] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:26.219+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:26.219+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:26.220+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:26.327+08:00  WARN 30256 --- [nio-8080-exec-2] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:26.561+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:26.561+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:26.562+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:26.635+08:00  WARN 30256 --- [nio-8080-exec-8] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:26.901+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:26.901+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:26.902+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:26.945+08:00  WARN 30256 --- [io-8080-exec-10] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:27.240+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:27.240+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:27.242+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:27.266+08:00  WARN 30256 --- [nio-8080-exec-6] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:27.578+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:27.579+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:27.580+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:27.833+08:00  WARN 30256 --- [nio-8080-exec-5] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:27.918+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:27.918+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:27.920+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:28.145+08:00  WARN 30256 --- [nio-8080-exec-2] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:28.255+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:28.255+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:28.256+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:28.452+08:00  WARN 30256 --- [nio-8080-exec-8] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:28.581+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:28.581+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:28.582+08:00 DEBUG 30256 --- [nio-8080-exec-4] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:28.759+08:00  WARN 30256 --- [io-8080-exec-10] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:28.916+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:28.916+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:28.917+08:00 DEBUG 30256 --- [nio-8080-exec-7] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:29.072+08:00  WARN 30256 --- [nio-8080-exec-6] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:29.244+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:29.244+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:29.245+08:00 DEBUG 30256 --- [nio-8080-exec-1] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:29.393+08:00  WARN 30256 --- [nio-8080-exec-5] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:29.578+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:29.578+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:29.579+08:00 DEBUG 30256 --- [nio-8080-exec-3] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
2026-08-05T20:06:29.713+08:00  WARN 30256 --- [nio-8080-exec-2] c.s.m.security.ReplayProtectionFilter    : Nonce 重复，疑似重放 uri=/api/v1/seckill/2084297480440176641/execute nonce=46ba8ca3-9dc6-4633-82a5-f2e384f800a3
2026-08-05T20:06:29.914+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==>  Preparing: SELECT id,username,password,phone,email,nickname,avatar_url,balance,role,status,is_deleted,create_time,update_time FROM t_user WHERE id=? AND is_deleted=0
2026-08-05T20:06:29.914+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : ==> Parameters: 1(Long)
2026-08-05T20:06:29.915+08:00 DEBUG 30256 --- [nio-8080-exec-9] c.s.mall.mapper.UserMapper.selectById    : <==      Total: 1
