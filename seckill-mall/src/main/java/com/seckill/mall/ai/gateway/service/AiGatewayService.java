package com.seckill.mall.ai.gateway.service;

import com.seckill.mall.ai.gateway.dto.Scene;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 网关统一调用入口。
 * <p>所有 AI 功能（导购/AIGC/客服）必须通过本服务调用大模型，不得各自直连。
 * <p>提供同步 call() 和流式 stream() 两个方法，caller 标识调用方用于审计/限流/预算。
 * <p>支持按 Scene 场景路由到不同模型（多模型路由策略）。
 */
@Service
public class AiGatewayService {

    private final ChatClient chatClient;
    private final RouteService routeService;

    public AiGatewayService(ChatClient chatClient, RouteService routeService) {
        this.chatClient = chatClient;
        this.routeService = routeService;
    }

    /**
     * 同步调用（AIGC 文案生成等）。
     * <p>使用 ChatClient 默认模型（deepseek-chat）。
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
     * 流式调用（导购/客服 SSE）。
     * <p>使用 ChatClient 默认模型（deepseek-chat）。
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

    /**
     * 同步调用（指定场景，按路由选择模型）。
     * <p>根据 Scene 经 RouteService 解析目标模型名，构造 OpenAiChatOptions 覆盖默认模型。
     *
     * @param scene        调用场景（REASONING/CHINESE/COMPLIANCE）
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识
     * @return 模型响应文本
     */
    public String call(Scene scene, String systemPrompt, String userPrompt, String caller) {
        String model = routeService.resolveModel(scene);
        OpenAiChatOptions sceneOptions = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(0.7)
                .withMaxTokens(2000)
                .build();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(sceneOptions)
                .toolContext(java.util.Map.of("caller", caller))
                .call()
                .content();
    }

    /**
     * 流式调用（指定场景，按路由选择模型）。
     * <p>根据 Scene 经 RouteService 解析目标模型名，构造 OpenAiChatOptions 覆盖默认模型。
     *
     * @param scene        调用场景（REASONING/CHINESE/COMPLIANCE）
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识
     * @return 流式响应 Flux<String>
     */
    public Flux<String> stream(Scene scene, String systemPrompt, String userPrompt, String caller) {
        String model = routeService.resolveModel(scene);
        OpenAiChatOptions sceneOptions = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(0.7)
                .withMaxTokens(2000)
                .build();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(sceneOptions)
                .toolContext(java.util.Map.of("caller", caller))
                .stream()
                .content();
    }
}
