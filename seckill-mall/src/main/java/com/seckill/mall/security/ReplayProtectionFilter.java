package com.seckill.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.cache.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀防重放过滤器。
 * <p>
 * B2 修复（架构重构）：废弃前端 HMAC 签名方案（浏览器 SPA 无法安全保管共享密钥），
 * 改为校验服务端签发的一次性短时效秒杀令牌（X-Seckill-Token）。
 * <ul>
 *   <li>前端通过 {@code GET /api/v1/seckill/{seckillId}/token} 获取服务端签发的 token</li>
 *   <li>下单时携带 {@code X-Seckill-Token} 头</li>
 *   <li>本 Filter 校验 token 存在性 + 格式 + nonce 原子去重（防重放）</li>
 *   <li>token 的真正合法性（Redis 比对、绑定 seckillId+userId）由 SeckillServiceImpl 在认证后校验</li>
 * </ul>
 * <p>
 * L-O1 修复：返回 403 + "缺少秒杀令牌"，区分"未登录"（401 由 JWT Filter 处理）和"缺少令牌"（403）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ReplayProtectionFilter.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ReplayProtectionFilter extends OncePerRequestFilter {

    /** 后端签发的秒杀令牌头 */
    private static final String HEADER_SECKILL_TOKEN = "X-Seckill-Token";

    /** nonce 去重 key 前缀 */
    private static final String NONCE_KEY_PREFIX = "seckill:nonce:";

    /** nonce 去重窗口（秒），与 token TTL 对齐 */
    private static final long NONCE_TTL_SECONDS = 60L;

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    /**
     * B2 修复后 sign-secret 仅用于服务端 token 签发（SeckillTokenService 内部使用），
     * 此处保留配置以兼容启动校验，但不再用于前端 HMAC 校验。
     */
    @Value("${seckill.security.sign-secret:}")
    private String signSecret;

    @Value("${seckill.security.replay-window-seconds:60}")
    private long replayWindowSeconds;

    @Value("${seckill.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173}")
    private String corsAllowedOrigins;

    /**
     * B2 修复：启动期校验签名密钥配置（服务端 token 签发用）。
     * 生产环境必须显式配置且长度 >= 32，fail-fast 阻止用弱密钥启动；
     * 开发环境（dev profile）宽容处理，仅 warn。
     */
    @jakarta.annotation.PostConstruct
    public void validateSignSecret() {
        if (signSecret == null || signSecret.length() < 32) {
            boolean isDev = environment != null && environment.acceptsProfiles(Profiles.of("dev"));
            if (isDev) {
                log.warn("seckill.security.sign-secret 未配置或长度 < 32，服务端 token 签发将使用默认弱密钥（仅开发环境允许）");
            } else {
                throw new IllegalStateException("seckill.security.sign-secret 不能为空且长度必须 >= 32");
            }
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 仅拦截秒杀下单接口的 POST 请求（排除后台管理接口 /admin、场次管理接口 /activities、一键执行接口 /execute）
        // Bug3修复：/api/v1/seckill/{id}/execute 是"一键执行秒杀"接口，设计上无需预取 token（内部自动获取），需排除拦截
        String requestUri = request.getRequestURI();
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !requestUri.startsWith("/api/v1/seckill/")
                || requestUri.startsWith("/api/v1/seckill/admin")
                || requestUri.startsWith("/api/v1/seckill/activities")
                || requestUri.endsWith("/execute")) {
            filterChain.doFilter(request, response);
            return;
        }

        // B2 修复：校验后端签发的秒杀令牌（X-Seckill-Token），而非前端 HMAC 三件套
        String seckillToken = request.getHeader(HEADER_SECKILL_TOKEN);
        if (!StringUtils.hasText(seckillToken)) {
            // L-O1 修复：返回 403 + "缺少秒杀令牌"，区分未登录（401）和缺少令牌（403）
            reject(request, response, ErrorCode.REPLAY_DETECTED, "缺少秒杀令牌，请先获取令牌");
            return;
        }

        // token 格式校验：服务端签发的是 UUID（32 位十六进制），防注入
        if (!isValidTokenFormat(seckillToken)) {
            log.warn("秒杀令牌格式非法 uri={}", request.getRequestURI());
            reject(request, response, ErrorCode.SECKILL_TOKEN_INVALID, "秒杀令牌格式非法");
            return;
        }

        // nonce 原子去重：同一 token 在窗口内仅允许使用一次，防重放
        String nonceKey = NONCE_KEY_PREFIX + seckillToken;
        Boolean ok = redisService.setIfAbsent(nonceKey, "1", NONCE_TTL_SECONDS, TimeUnit.SECONDS);
        if (ok == null || !ok) {
            log.warn("秒杀令牌重复使用，疑似重放 uri={} token={}", request.getRequestURI(), seckillToken);
            reject(request, response, ErrorCode.REPLAY_DETECTED, "秒杀令牌已使用，请重新获取");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 校验秒杀令牌格式：32 位十六进制（UUID 去横线）或标准 UUID 格式。
     */
    private boolean isValidTokenFormat(String token) {
        if (token == null || token.length() > 64) {
            return false;
        }
        // 允许 UUID 去横线（32 位 hex）或标准 UUID（36 位含横线）
        String normalized = token.replace("-", "");
        if (normalized.length() != 32) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 安全修复（H2-补全）：本 Filter 在 Spring Security 过滤链之前执行，
     * reject 时 CorsFilter 尚未添加 CORS 头，需手动补充。
     * L-O1 修复：返回 403 + 明确错误信息。
     */
    private void reject(HttpServletRequest request, HttpServletResponse response,
                         ErrorCode errorCode, String message) throws IOException {
        // 补充 CORS 头
        String origin = request.getHeader("Origin");
        if (origin != null) {
            List<String> allowedOrigins = Arrays.asList(corsAllowedOrigins.split(","));
            if (allowedOrigins.contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
                response.setHeader("Access-Control-Allow-Headers", "*");
                response.setHeader("Access-Control-Max-Age", "3600");
            }
        }
        // L-O1 修复：返回 403（而非 401），区分"未登录"和"缺少秒杀令牌"
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode.getCode(), message)));
    }
}
