package com.seckill.mall.config;

import com.seckill.mall.security.JwtAccessDeniedHandler;
import com.seckill.mall.security.JwtAuthenticationEntryPoint;
import com.seckill.mall.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SecurityConfig.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;


    /**
     * M-K2 修复：CORS 允许的来源从配置读取，使用 @ConfigurationProperties 风格统一管理。
     * 生产环境通过 application-prod.yml 的 seckill.security.cors.allowed-origins 显式注入。
     * 默认值仅用于本地开发环境。
     */
    @Value("${seckill.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173}")
    private String corsAllowedOrigins;

    /**
     * L-S3 修复：生产环境标志，用于收紧 CORS 与安全头策略。
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 安全修复（M6）：项目为纯 JWT 无状态服务，CSRF 攻击不适用；
                // 约束：后续若引入基于 Cookie 的会话或表单提交，必须重新启用 CSRF 并排除纯 API 路径
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // L-S2 修复：补充安全响应头（HSTS、CSP、X-Content-Type-Options、Referrer-Policy）
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // CSP：仅允许同源 + 必要的 inline（Vue SPA 需要 style/attr）
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Content-Security-Policy",
                                "default-src 'self'; img-src 'self' data: https:; " +
                                "style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "X-Permitted-Cross-Domain-Policies", "none"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "X-XSS-Protection", "1; mode=block")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/captcha",
                                "/api/v1/auth/forgot-password/**",
                                "/api/v1/products/**",
                                "/api/v1/seckill/list",
                                "/api/v1/seckill/*/stock",
                                "/api/v1/banners/active",
                                "/api/v1/verification/**",
                                // T9：埋点上报允许未登录用户访问（userId 为 null）
                                "/api/v1/track/**",
                                "/upload/**",
                                "/images/**"
                        ).permitAll()
                        .requestMatchers(
                                "/doc.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // L-S7 修复：/actuator/info 也需要 ADMIN 权限，避免泄露应用元信息
                        .requestMatchers("/actuator/info").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/seckill/{seckillId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/product/**").permitAll()
                        .anyRequest().authenticated())
                // B2 修复后 ReplayProtectionFilter 通过 @Component + @Order 自动注册为 Servlet Filter，
                // 在 Spring Security 之前执行（@Order=HIGHEST_PRECEDENCE+20），此处无需重复注册
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength=10，平衡安全性与性能
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * M-K2 修复：CORS 配置。
     * - 改用 setAllowedOrigins（精确匹配）替代 setAllowedOriginPatterns（模式匹配），更严格
     * - 生产环境（prod profile）校验不允许通配 "*"
     * - L-S3 修复：确保生产 allowed-origins 不含通配
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // L-S3 修复：生产环境不允许通配 "*"
        if ("prod".equalsIgnoreCase(activeProfile) && origins.contains("*")) {
            throw new IllegalStateException(
                    "生产环境 seckill.security.cors.allowed-origins 不允许使用通配 '*'，必须显式列出可信来源");
        }
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "seckill.security.cors.allowed-origins 必须配置至少一个可信来源");
        }

        // M-K2 修复：使用 setAllowedOrigins（精确匹配）而非 setAllowedOriginPatterns
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        log.info("CORS allowed origins: {} (profile={})", origins, activeProfile);
        return source;
    }
}
