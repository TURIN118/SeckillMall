package com.seckill.mall.security;

import com.seckill.mall.identity.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
@RequiredArgsConstructor
public class JwtUtils {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RsaKeyProvider rsaKeyProvider;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    public void init() {
        this.privateKey = rsaKeyProvider.loadPrivateKey();
        this.publicKey = rsaKeyProvider.loadPublicKey();
        log.info("RSA 密钥对加载成功，JWT 签名算法: RS256");
    }

    public String generateAccessToken(Long userId, String username, UserRole role, long tokenVersion) {
        return buildToken(userId, username, role, accessTokenExpiration, TOKEN_TYPE_ACCESS, tokenVersion);
    }

    public String generateRefreshToken(Long userId, String username, UserRole role, long tokenVersion) {
        return buildToken(userId, username, role, refreshTokenExpiration, TOKEN_TYPE_REFRESH, tokenVersion);
    }

    private String buildToken(Long userId, String username, UserRole role, long expirationMs, String tokenType, long tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .issuer("seckill-mall")
                .audience().add("seckill-mall-api").and()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.getCode())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
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

    public long getTokenVersionFromToken(String token) {
        Object value = parseToken(token).get(CLAIM_TOKEN_VERSION);
        if (value instanceof Number num) {
            return num.longValue();
        }
        return value == null ? 1L : Long.valueOf(value.toString());
    }
}
