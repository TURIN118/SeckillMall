package com.seckill.mall.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TokenBlacklistService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    public void addToBlacklist(String token) {
        try {
            long remainingMs = jwtUtils.getTokenRemainingTime(token);
            if (remainingMs <= 0) {
                // Token 已过期，无需加入黑名单
                return;
            }
            String tokenId = jwtUtils.getTokenId(token);
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + tokenId,
                    "1",
                    remainingMs,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.warn("加入 Token 黑名单失败: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String tokenId = jwtUtils.getTokenId(token);
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + tokenId));
        } catch (Exception e) {
            log.debug("校验 Token 黑名单失败: {}", e.getMessage());
            return false;
        }
    }
}
