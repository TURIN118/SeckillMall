package com.seckill.mall.aspect;

import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RateLimitAspect.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final long TOKEN_COST = 1L;

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String limitKey = buildLimitKey(rateLimit);
        long nowSec = System.currentTimeMillis() / 1000L;

        // 令牌桶：capacity/rate 控制突发与稳态速率
        Long allowed = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(limitKey),
                String.valueOf(rateLimit.capacity()),
                String.valueOf(rateLimit.rate()),
                String.valueOf(nowSec),
                String.valueOf(TOKEN_COST));

        if (allowed == null || allowed == 0L) {
            log.warn("限流拦截 key={} capacity={} rate={}", limitKey, rateLimit.capacity(), rateLimit.rate());
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return pjp.proceed();
    }

    private String buildLimitKey(RateLimit rateLimit) {
        Long userId = tryGetCurrentUserId();
        String prefix = StringUtils.hasText(rateLimit.key()) ? rateLimit.key() : "default";
        if (userId != null) {
            // 用户级限流：rate:seckill:{prefix}:{userId}
            return RedisKeyConstants.RATE_SECKILL + prefix + ":" + userId;
        }
        // 未登录用户退化为 IP 级限流：rate:ip:{ip}:{prefix}
        String ip = getClientIp();
        return RedisKeyConstants.RATE_IP + ip + ":" + prefix;
    }

    private Long tryGetCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.seckill.mall.security.SecurityUserDetails details) {
            try {
                return details.getUserId();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理取首段
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
