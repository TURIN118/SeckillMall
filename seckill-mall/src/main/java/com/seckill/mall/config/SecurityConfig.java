package com.seckill.mall.config;

import com.seckill.mall.security.JwtAccessDeniedHandler;
import com.seckill.mall.security.JwtAuthenticationEntryPoint;
import com.seckill.mall.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * 安全修复（H1）：CORS 允许的来源从配置读取，避免使用 "*" 携带凭证导致的 CORS 滥用。
     * 默认值仅用于本地开发环境，生产环境必须通过配置覆盖。
     */
    @Value("${seckill.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 安全修复（M6）：项目为纯 JWT 无状态服务，CSRF 攻击不适用；
                // 约束：后续若引入基于 Cookie 的会话或表单提交，必须重新启用 CSRF 并排除纯 API 路径
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                                "/upload/**",
                                "/images/**"
                                // 安全修复（M7）：API 文档端点（doc.html/swagger-ui/webjars/swagger-resources/v3/api-docs）
                                // 不再无条件 permitAll，统一收敛到下方 hasRole('ADMIN')，避免生产环境接口暴露
                        ).permitAll()
                        // 安全修复（M7）：API 文档端点仅 ADMIN 可访问
                        .requestMatchers(
                                "/doc.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**").hasRole("ADMIN")
                        // 安全修复（C3）：Actuator 仅 health 端点公开，其余端点需 ADMIN 角色，避免敏感运维信息泄露
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/seckill/{seckillId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/product/**").permitAll()
                        .anyRequest().authenticated())
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 安全修复（H1）：使用配置化的明确域名列表替代 "*"，避免任意来源携带凭证
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
