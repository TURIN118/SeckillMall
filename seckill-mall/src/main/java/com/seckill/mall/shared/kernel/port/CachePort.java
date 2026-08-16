package com.seckill.mall.shared.kernel.port;

import java.util.concurrent.TimeUnit;

/**
 * 缓存能力端口（抽象 Redis 缓存操作）
 * <p>业务代码应依赖此接口而非 StringRedisTemplate/RedisService 具体实现。
 * <p>现有 RedisService 后续可实现此接口。
 */
public interface CachePort {

    String get(String key);

    void set(String key, String value);

    void set(String key, String value, long timeout, TimeUnit unit);

    Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit);

    Boolean del(String key);

    Long incr(String key);

    Long decr(String key);

    Boolean exists(String key);
}