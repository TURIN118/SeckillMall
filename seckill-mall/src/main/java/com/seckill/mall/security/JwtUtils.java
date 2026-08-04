package com.seckill.mall.security;

import com.seckill.mall.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：JwtUtils.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
public class JwtUtils {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, UserRole role) {
        return buildToken(userId, username, role, accessTokenExpiration, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(Long userId, String username, UserRole role) {
        return buildToken(userId, username, role, refreshTokenExpiration, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(Long userId, String username, UserRole role, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.getCode())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                // 安全修复（L5）：当前使用 HS256 对称签名，密钥泄露后可伪造任意 Token。
                // 建议升级为 RS256/ES256 非对称签名：私钥签发、公钥校验，降低密钥泄露影响面。
                // 暂不修改算法以保持与现有已签发 Token 的兼容性，需在密钥轮换窗口期统一升级。
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Object value = parseToken(token).get(CLAIM_USER_ID);
        if (value instanceof Number num) {
            return num.longValue();
        }
        return value == null ? null : Long.valueOf(value.toString());
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).get(CLAIM_USERNAME, String.class);
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get(CLAIM_ROLE, String.class);
    }

    public String getTokenTypeFromToken(String token) {
        return parseToken(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    public boolean isAccessTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public long getTokenRemainingTime(String token) {
        try {
            long exp = parseToken(token).getExpiration().getTime();
            long remaining = exp - System.currentTimeMillis();
            return Math.max(remaining, 0L);
        } catch (ExpiredJwtException e) {
            return 0L;
        }
    }

    public String getTokenId(String token) {
        return parseToken(token).getId();
    }
}
