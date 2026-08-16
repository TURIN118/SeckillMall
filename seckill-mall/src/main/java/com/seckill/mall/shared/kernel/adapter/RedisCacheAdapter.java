package com.seckill.mall.shared.kernel.adapter;

import com.seckill.mall.shared.kernel.port.CachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * CachePort 的 Redis 实现：基于 StringRedisTemplate 适配。
 * <p>业务代码依赖 {@link CachePort}，由本类桥接到 Spring Data Redis。
 * <p>管理类操作（如 flushDb）不在此封装，仍由 SystemServiceImpl 直接持有 StringRedisTemplate。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RedisCacheAdapter.java
 * 邮箱：nj651217@163.com
 */
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    @Override
    public Boolean del(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Override
    public Long incr(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long decr(String key) {
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    @Override
    public Boolean exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        stringRedisTemplate.expire(key, timeout, unit);
    }
}