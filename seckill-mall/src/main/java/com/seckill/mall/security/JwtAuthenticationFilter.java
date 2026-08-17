package com.seckill.mall.security;

import com.seckill.mall.identity.domain.UserStatus;
import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.identity.infrastructure.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：JwtAuthenticationFilter.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;
    private final UserStatusCacheService userStatusCacheService;
    private final TokenVersionService tokenVersionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)
                && !tokenBlacklistService.isBlacklisted(token)) {
            try {
                Claims claims = jwtUtils.parseToken(token);
                Object userIdVal = claims.get("userId");
                Long userId = userIdVal instanceof Number num ? num.longValue()
                        : (userIdVal == null ? null : Long.valueOf(userIdVal.toString()));
                // 仅 ACCESS 类型 Token 用于业务请求鉴权
                if (JwtUtils.TOKEN_TYPE_ACCESS.equals(claims.get("tokenType", String.class))) {
                    // Token 版本号校验
                    long tokenVersion = jwtUtils.getTokenVersionFromToken(token);
                    long currentVersion = tokenVersionService.getCurrentVersion(userId);
                    if (tokenVersion != currentVersion) {
                        log.debug("Token 版本号不匹配，拒绝鉴权: userId={}, tokenVersion={}, currentVersion={}",
                                userId, tokenVersion, currentVersion);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    SecurityUserDetails userDetails = buildUserDetailsFromClaims(claims);
                    // 用户不存在或已被禁用/锁定，拒绝鉴权并清空上下文
                    if (userDetails == null || !userDetails.isEnabled()) {
                        log.debug("Token 用户状态无效，拒绝鉴权");
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("JWT 鉴权失败，清空上下文: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 从 JWT Claims 构建 UserDetails。
     * <p>
     * 优先从 Redis 缓存获取用户状态/角色，未命中再查 DB 并回填缓存。
     * 若用户不存在或状态非 ACTIVE，则返回 null，由调用方清空 SecurityContext 拒绝鉴权。
     */
    private SecurityUserDetails buildUserDetailsFromClaims(Claims claims) {
        Object userIdVal = claims.get("userId");
        Long userId = userIdVal instanceof Number num ? num.longValue()
                : (userIdVal == null ? null : Long.valueOf(userIdVal.toString()));
        if (userId == null) {
            return null;
        }

        // 先查 Redis 缓存，未命中再查 DB
        UserStatusCacheService.UserAuthCache cached = userStatusCacheService.getUserAuth(userId);
        if (cached != null) {
            // 缓存命中，直接构建 UserDetails
            if (cached.status() != UserStatus.ACTIVE) {
                return null;
            }
            // 从缓存构建 User 对象（仅填充 SecurityUserDetails 所需字段）
            User cachedUser = new User();
            cachedUser.setId(userId);
            cachedUser.setUsername(cached.username());
            cachedUser.setRole(cached.role());
            cachedUser.setStatus(cached.status());
            cachedUser.setAvatarUrl(cached.avatarUrl());
            return new SecurityUserDetails(cachedUser);
        }

        // 缓存未命中，查 DB 并回填缓存
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null || dbUser.getStatus() == null
                || dbUser.getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        userStatusCacheService.putUserAuth(userId, dbUser);
        return new SecurityUserDetails(dbUser);
    }
}
