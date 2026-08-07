-- M-C2 修复：秒杀库存原子回补脚本
-- KEYS[1] = seckill:stock:{seckillId}    库存 String
-- KEYS[2] = seckill:bought:{seckillId}   已购用户 Set
-- ARGV[1] = userId
-- 返回值: 1=成功

local stockKey = KEYS[1]
local boughtKey = KEYS[2]
local userId = ARGV[1]

-- 原子回补：INCR 库存 + SREM 已购标记
redis.call('INCR', stockKey)
redis.call('SREM', boughtKey, userId)
return 1