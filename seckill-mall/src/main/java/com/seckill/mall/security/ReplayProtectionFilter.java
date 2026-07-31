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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
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

    private static final String HEADER_SIGN = "X-Sign";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Value("${seckill.security.sign-secret:wnj-seckill-sign-secret-2024}")
    private String signSecret;

    @Value("${seckill.security.replay-window-seconds:60}")
    private long replayWindowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 仅拦截秒杀接口的 POST 请求
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/seckill/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String sign = request.getHeader(HEADER_SIGN);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);

        if (!StringUtils.hasText(sign) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            reject(response, ErrorCode.REPLAY_DETECTED);
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            reject(response, ErrorCode.REPLAY_DETECTED);
            return;
        }

        // 时间窗口校验：拒绝超出 ±window 的请求，防重放
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > replayWindowSeconds * 1000L) {
            log.warn("签名时间窗口校验失败 uri={} ts={} drift={}ms", request.getRequestURI(), ts, now - ts);
            reject(response, ErrorCode.REPLAY_DETECTED);
            return;
        }

        // Nonce 原子去重：60s 内重复即拒绝
        String nonceKey = "nonce:" + nonce;
        Boolean ok = redisService.setIfAbsent(nonceKey, "1", 60L, TimeUnit.SECONDS);
        if (ok == null || !ok) {
            log.warn("Nonce 重复，疑似重放 uri={} nonce={}", request.getRequestURI(), nonce);
            reject(response, ErrorCode.REPLAY_DETECTED);
            return;
        }

        // 签名校验：HMAC-SHA256(secret, timestamp + nonce + uri)
        String payload = timestamp + nonce + request.getRequestURI();
        String expected = hmacSha256Hex(signSecret, payload);
        if (!constantTimeEquals(expected, sign)) {
            log.warn("签名不匹配 uri={} expected={} actual={}", request.getRequestURI(), expected, sign);
            reject(response, ErrorCode.REPLAY_DETECTED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
    }

    /**
     * 使用 javax.crypto.Mac 计算 HMAC-SHA256，返回十六进制小写字符串。
     */
    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    /**
     * 常量时间比较，避免计时攻击。
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
