package com.seckill.mall.aspect;

import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.utils.IpUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * C2 修复：可信反向代理 IP 白名单。
     * 仅当请求直连远端地址（request.getRemoteAddr()）在白名单时，才信任 X-Forwarded-For / X-Real-IP。
     * 生产环境通过 seckill.security.trusted-proxy-ips 显式注入（逗号分隔）。
     * 默认空集 → 不信任任何 X-Forwarded-For，全部使用 remoteAddr，最安全。
     */
    @Value("${seckill.security.trusted-proxy-ips:}")
    private String trustedProxyIpsConfig;

    private Set<String> trustedProxyIps;

    private DefaultRedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);

        // 解析可信代理白名单
        trustedProxyIps = java.util.Arrays.stream(trustedProxyIpsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        log.info("RateLimitAspect 可信代理白名单: {}", trustedProxyIps);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String limitKey = buildLimitKey(rateLimit);
        long nowSec = System.currentTimeMillis() / 1000L;

        // C3 修复：传递 seconds 参数给 Lua，由 Lua 计算 refillRate = capacity / seconds
        Long allowed = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(limitKey),
                String.valueOf(rateLimit.capacity()),
                String.valueOf(rateLimit.rate()),
                String.valueOf(nowSec),
                String.valueOf(TOKEN_COST),
                String.valueOf(rateLimit.seconds()));

        if (allowed == null || allowed == 0L) {
            log.warn("限流拦截 key={} capacity={} rate={} seconds={}", limitKey, rateLimit.capacity(), rateLimit.rate(), rateLimit.seconds());
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

    /**
     * 获取客户端真实 IP（复用 IpUtils 统一算法，传入自定义可信代理白名单）。
     */
    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        return IpUtils.getClientIp(attrs.getRequest(), trustedProxyIps);
    }
}
