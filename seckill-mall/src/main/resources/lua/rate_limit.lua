-- ============================================================
-- 令牌桶限流脚本
-- 安全修复 C3：seconds 参数生效，确保 @RateLimit(seconds=60) 实际效果为每 60s 1 次
-- ============================================================
-- KEYS[1] = 限流 key（如 rate:seckill:{userId} / rate:ip:{ip}:{path}）
-- ARGV[1] = capacity  桶容量（突发上限）
-- ARGV[2] = rate       令牌补充速率（tokens/sec）—— 兼容旧调用，当 seconds>0 时被覆盖
-- ARGV[3] = nowSec     当前时间戳（秒）
-- ARGV[4] = cost       本次请求消耗令牌数（通常 1）
-- ARGV[5] = seconds    时间窗口秒数（C3 修复：>0 时令 refillRate = capacity / seconds）
-- 返回值: 1=允许 / 0=拒绝
-- ============================================================

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local nowSec = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])
local seconds = tonumber(ARGV[5])

-- C3 修复：当 seconds > 0 时，用 capacity/seconds 作为每秒补充速率
-- 例如 capacity=1, seconds=60 → 每秒补 1/60 → 60s 补满 1 个 → 每 60s 1 次
if seconds and seconds > 0 then
    rate = capacity / seconds
end
-- 兜底：rate 必须为正数
if rate == nil or rate <= 0 then
    rate = capacity
end

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
-- 过期时间取 max(120, 2*seconds)，避免窗口未结束 key 先过期
local ttl = 120
if seconds and seconds > 0 then
    ttl = math.max(120, seconds * 2)
end
redis.call('EXPIRE', key, ttl)
return allowed
