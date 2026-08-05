package com.seckill.mall.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户级 Token 版本号服务。
 * 通过递增版本号实现批量吊销用户所有 Token。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionService {

    private static final String KEY_PREFIX = "user:token-version:";
    /** 版本号缓存 30 天，避免永久占用内存（用户 30 天未登录自动清理） */
    private static final long VERSION_TTL_DAYS = 30L;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取当前版本号。不存在时返回 1 并写入初始值。
     */
    public long getCurrentVersion(Long userId) {
        try {
            String key = KEY_PREFIX + userId;
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value != null) {
                return Long.parseLong(value);
            }
            // 首次访问，初始化为 1
            stringRedisTemplate.opsForValue().set(key, "1", VERSION_TTL_DAYS, TimeUnit.DAYS);
            return 1L;
        } catch (Exception e) {
            log.warn("获取 Token 版本号失败，降级返回 1: userId={}, error={}", userId, e.getMessage());
            return 1L;
        }
    }

    /**
     * 递增版本号（踢下所有设备）。
     * 递增后所有旧 Token 中的 version 与新 version 不一致，全部失效。
     */
    public long incrementVersion(Long userId) {
        try {
            String key = KEY_PREFIX + userId;
            Long newVersion = stringRedisTemplate.opsForValue().increment(key);
            if (newVersion != null) {
                stringRedisTemplate.expire(key, VERSION_TTL_DAYS, TimeUnit.DAYS);
                return newVersion;
            }
            return 1L;
        } catch (Exception e) {
            log.warn("递增 Token 版本号失败: userId={}, error={}", userId, e.getMessage());
            return 1L;
        }
    }
}