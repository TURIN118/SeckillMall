-- 秒杀库存原子预减脚本
-- KEYS[1] = seckill:stock:{seckillId}    库存 String
-- KEYS[2] = seckill:bought:{seckillId}   已购用户 Set
-- ARGV[1] = userId
-- ARGV[2] = boughtSetTtlSeconds  已购集合 TTL（活动剩余时间）
-- ARGV[3] = stockKeyTtlSeconds   库存 Key TTL（活动剩余时间，M34 新增，可选）
-- 返回值: 1=成功 / -1=库存不足 / -2=重复下单

local stockKey = KEYS[1]
local boughtKey = KEYS[2]
local userId = ARGV[1]
local ttl = tonumber(ARGV[2])
local stockTtl = tonumber(ARGV[3])

-- 1. 判重：用户是否已下单
if redis.call('SISMEMBER', boughtKey, userId) == 1 then
    return -2
end

-- 2. 预减库存
local remain = redis.call('DECR', stockKey)
if remain < 0 then
    -- 库存不足，回滚
    redis.call('INCR', stockKey)
    return -1
end

-- M34 修复：为 stockKey 设置 TTL，避免活动结束后残留 Key 造成内存泄漏
-- 仅在 stockTtl 有效时刷新；活动期内多次扣减会重复刷新 TTL，符合预期(延长至活动结束)
if stockTtl and stockTtl > 0 then
    redis.call('EXPIRE', stockKey, stockTtl)
end

-- 3. 记录已购用户并设置 TTL（首次 SADD 时生效，后续刷新）
redis.call('SADD', boughtKey, userId)
if ttl and ttl > 0 then
    redis.call('EXPIRE', boughtKey, ttl)
end
return 1
