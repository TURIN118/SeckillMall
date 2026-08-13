package com.seckill.mall.ai.gateway.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 网关统一调用入口。
 * <p>所有 AI 功能（导购/AIGC/客服）必须通过本服务调用大模型，不得各自直连。
 * <p>提供同步 call() 和流式 stream() 两个方法，caller 标识调用方用于审计/限流/预算。
 */
@Service
public class AiGatewayService {

    private final ChatClient chatClient;

    public AiGatewayService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 同步调用（AIGC 文案生成等）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识（shopping-assistant/aigc/customer-service）
     * @return 模型响应文本
     */
    public String call(String systemPrompt, String userPrompt, String caller) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .toolContext(java.util.Map.of("caller", caller))
                .call()
                .content();
    }

    /**
     * 流式调用（导购/客服 SSE）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识
     * @return 流式响应 Flux<String>
     */
    public Flux<String> stream(String systemPrompt, String userPrompt, String caller) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .toolContext(java.util.Map.of("caller", caller))
                .stream()
                .content();
    }
}