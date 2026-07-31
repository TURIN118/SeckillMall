package com.seckill.mall.security;

import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
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

    private SecurityUserDetails buildUserDetailsFromClaims(Claims claims) {
        Object userIdVal = claims.get("userId");
        Long userId = userIdVal instanceof Number num ? num.longValue()
                : (userIdVal == null ? null : Long.valueOf(userIdVal.toString()));
        String username = claims.get("username", String.class);
        String roleCode = claims.get("role", String.class);

        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setRole(UserRole.fromCode(roleCode));
        // Token 通过校验即视为有效用户，状态默认启用
        user.setStatus(UserStatus.ACTIVE);
        return new SecurityUserDetails(user);
    }
}
