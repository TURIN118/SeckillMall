-- 令牌桶限流脚本
-- KEYS[1] = 限流 key（如 rate:seckill:{userId} / rate:ip:{ip}:{path}）
-- ARGV[1] = capacity  桶容量
-- ARGV[2] = rate       令牌补充速率（tokens/sec）
-- ARGV[3] = nowSec     当前时间戳（秒）
-- ARGV[4] = cost       本次请求消耗令牌数（通常 1）
-- 返回值: 1=允许 / 0=拒绝

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local nowSec = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])

local lastTime = tonumber(redis.call('HGET', key, 'last_time'))
local tokens = tonumber(redis.call('HGET', key, 'tokens'))

-- 首次访问初始化为满桶
if lastTime == nil or tokens == nil then
    lastTime = nowSec
    tokens = capacity
end

-- 计算时间差并补充令牌，上限为 capacity
local delta = math.max(0, nowSec - lastTime)
tokens = math.min(capacity, tokens + delta * rate)

local allowed = 0
if tokens >= cost then
    tokens = tokens - cost
    allowed = 1
end

redis.call('HMSET', key, 'last_time', nowSec, 'tokens', tokens)
-- 120s 未访问自动过期，避免垃圾数据堆积
redis.call('EXPIRE', key, 120)
return allowed
