package com.seckill.mall.ai.gateway.controller;

import com.seckill.mall.ai.gateway.service.AiGatewayService;
import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 网关调试接口（仅 ADMIN 可访问）。
 * <p>用于在接入 DeepSeek 后验证 ChatClient 链路（限流/缓存/审计/兜底/预算）是否正常。
 * <p>生产环境可关闭或保留仅测试用途。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiGatewayController.java
 * 邮箱：nj651217@163.com
 */
@RestController
@RequestMapping("/api/v1/admin/ai-gateway")
@PreAuthorize("hasRole('ADMIN')")
public class AiGatewayController {

    private final AiGatewayService aiGatewayService;

    public AiGatewayController(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    /**
     * 同步调试调用：验证 DeepSeek 接入是否生效。
     * <p>请求体可选字段：prompt（默认"你好"）。
     *
     * @param body 请求体
     * @return 模型响应文本
     */
    @PostMapping("/test")
    @OperationLog(module = "AI_GATEWAY", action = "TEST", targetType = "AI")
    public Result<String> test(@RequestBody Map<String, String> body) {
        String result = aiGatewayService.call(
                "你是一个测试助手，简短回答。",
                body.getOrDefault("prompt", "你好"),
                "ai-gateway-test");
        return Result.success(result);
    }
}