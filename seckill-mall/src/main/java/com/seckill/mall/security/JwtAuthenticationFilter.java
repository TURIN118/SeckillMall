package com.seckill.mall.security;

import com.seckill.mall.entity.User;

import com.seckill.mall.entity.enums.UserStatus;
import com.seckill.mall.mapper.UserMapper;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)
                && !tokenBlacklistService.isBlacklisted(token)) {
            try {
                Claims claims = jwtUtils.parseToken(token);
                // 仅 ACCESS 类型 Token 用于业务请求鉴权
                if (JwtUtils.TOKEN_TYPE_ACCESS.equals(claims.get("tokenType", String.class))) {
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
     * 安全修复（C1）：不再硬编码用户状态为 ACTIVE，而是根据 userId 从数据库实时查询用户状态，
     * 若用户不存在或状态非 ACTIVE，则返回 null，由调用方清空 SecurityContext 拒绝鉴权。
     * <p>
     * 性能建议：高频接口可引入 Redis 缓存用户状态（key=user:status:{userId}，TTL 60s），
     * 用户禁用/锁定操作需同步失效缓存。
     */
    private SecurityUserDetails buildUserDetailsFromClaims(Claims claims) {
        Object userIdVal = claims.get("userId");
        Long userId = userIdVal instanceof Number num ? num.longValue()
                : (userIdVal == null ? null : Long.valueOf(userIdVal.toString()));
        if (userId == null) {
            return null;
        }
        // 从数据库实时查询用户最新状态，避免已禁用/锁定用户的 Token 仍可鉴权
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null || dbUser.getStatus() == null
                || dbUser.getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        // 使用数据库中的真实角色与状态构建 UserDetails
        return new SecurityUserDetails(dbUser);
    }
}
