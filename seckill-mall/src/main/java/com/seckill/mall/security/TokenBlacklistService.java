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
            // 安全修复（H4）：Fail-Closed 策略——Redis 异常时视为已黑名单，拒绝请求；
            // 避免故障期间已登出/吊销 Token 仍可访问。日志级别提升为 error 触发告警。
            log.error("校验 Token 黑名单失败，采用 Fail-Closed 策略拒绝请求: {}", e.getMessage());
            return true;
        }
    }
}
