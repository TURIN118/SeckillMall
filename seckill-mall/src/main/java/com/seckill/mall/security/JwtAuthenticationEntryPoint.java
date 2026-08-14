package com.seckill.mall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：JwtAuthenticationEntryPoint.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /** SSE 端点路径前缀（认证失败时需返回 text/event-stream 格式错误） */
    private static final String SSE_ENDPOINT_PREFIX = "/api/v1/ai/";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("未认证访问: {} - {}", request.getRequestURI(), authException.getMessage());
        // 安全修复（H2）：CORS 头统一由 SecurityConfig#corsConfigurationSource 管理，此处不再手动反射 Origin
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // SSE 端点认证失败：返回 text/event-stream 格式错误事件，避免前端 content-type 不匹配
        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.startsWith(SSE_ENDPOINT_PREFIX)) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // SSE 错误事件格式：event: error\ndata: {json}\n\n
            String errorJson = objectMapper.writeValueAsString(
                    Result.error(ErrorCode.UNAUTHORIZED));
            response.getWriter().write("event: error\ndata: " + errorJson + "\n\n");
            return;
        }

        // 非 SSE 端点：返回 JSON 格式错误（原有逻辑）
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.error(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
