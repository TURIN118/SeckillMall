package com.seckill.mall.ai.gateway.service;

import com.seckill.mall.ai.gateway.advisor.FallbackAdvisor;
import com.seckill.mall.ai.gateway.dto.Scene;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 网关统一调用入口。
 * <p>所有 AI 功能（导购/AIGC/客服）必须通过本服务调用大模型，不得各自直连。
 * <p>提供同步 call() 和流式 stream() 两个方法，caller 标识调用方用于审计/限流/预算。
 * <p>支持按 Scene 场景路由到不同模型（多模型路由策略）。
 * <p>所有方法均集成降级兜底：LLM 调用异常时通过 {@link FallbackAdvisor#fallback(String, Throwable)}
 * 返回按 caller 区分的兜底文案，保证用户体验。
 */
@Service
public class AiGatewayService {

    private final ChatClient chatClient;
    private final RouteService routeService;
    private final FallbackAdvisor fallbackAdvisor;

    public AiGatewayService(ChatClient chatClient,
                            RouteService routeService,
                            FallbackAdvisor fallbackAdvisor) {
        this.chatClient = chatClient;
        this.routeService = routeService;
        this.fallbackAdvisor = fallbackAdvisor;
    }

    /**
     * 同步调用（AIGC 文案生成等）。
     * <p>使用 ChatClient 默认模型（deepseek-chat）。
     * <p>LLM 调用异常时返回降级兜底文案。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识（shopping-assistant/aigc/customer-service）
     * @return 模型响应文本
     */
    public String call(String systemPrompt, String userPrompt, String caller) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .toolContext(java.util.Map.of("caller", caller))
                    .call()
                    .content();
        } catch (Exception e) {
            return fallbackAdvisor.fallback(caller, e);
        }
    }

    /**
     * 流式调用（导购/客服 SSE）。
     * <p>使用 ChatClient 默认模型（deepseek-chat）。
     * <p>流式异常时通过 {@code onErrorResume} 返回降级兜底文案。
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
                .content()
                .onErrorResume(e -> Flux.just(fallbackAdvisor.fallback(caller, e)));
    }

    /**
     * 同步调用（指定场景，按路由选择模型）。
     * <p>根据 Scene 经 RouteService 解析目标模型名，构造 OpenAiChatOptions 覆盖默认模型。
     * <p>LLM 调用异常时返回降级兜底文案。
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
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(sceneOptions)
                    .toolContext(java.util.Map.of("caller", caller))
                    .call()
                    .content();
        } catch (Exception e) {
            return fallbackAdvisor.fallback(caller, e);
        }
    }

    /**
     * 流式调用（指定场景，按路由选择模型）。
     * <p>根据 Scene 经 RouteService 解析目标模型名，构造 OpenAiChatOptions 覆盖默认模型。
     * <p>流式异常时通过 {@code onErrorResume} 返回降级兜底文案。
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
                .content()
                .onErrorResume(e -> Flux.just(fallbackAdvisor.fallback(caller, e)));
    }

    /**
     * 流式调用 + function-calling 工具（导购助手等需要工具调用的场景）。
     * <p>Spring AI 1.0.0-M3 通过 {@code .functions(FunctionCallback...)} 注册工具回调，
     * 大模型可在生成过程中调用工具并将结果回填继续生成。
     * <p>使用 ChatClient 默认模型（deepseek-chat）。
     * <p>流式异常时通过 {@code onErrorResume} 返回降级兜底文案。
     *
     * <h3>API 适配说明</h3>
     * <p>1.0.0-M3 不存在 {@code .tools(Object...)} 方法（1.0.0-M4+ 才引入），
     * 等价 API 为 {@code .functions(FunctionCallback...)}。本方法保留 {@code tools} 命名以对齐上层语义，
     * 内部转发到 {@code .functions()}。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识
     * @param tools        function-calling 工具回调数组（{@link FunctionCallback} 实例），
     *                     为空时不注册工具，等价于 {@link #stream(String, String, String)}
     * @return 流式响应 Flux<String>
     */
    public Flux<String> stream(String systemPrompt, String userPrompt, String caller, FunctionCallback... tools) {
        if (tools == null || tools.length == 0) {
            return stream(systemPrompt, userPrompt, caller);
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .functions(tools)
                .toolContext(java.util.Map.of("caller", caller))
                .stream()
                .content()
                .onErrorResume(e -> Flux.just(fallbackAdvisor.fallback(caller, e)));
    }

    /**
     * 同步调用 + function-calling 工具。
     * <p>用于需要工具调用但非流式返回的场景（如内部批量推荐）。
     * <p>LLM 调用异常时返回降级兜底文案。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param caller       调用方标识
     * @param tools        function-calling 工具回调数组
     * @return 模型响应文本
     */
    public String call(String systemPrompt, String userPrompt, String caller, FunctionCallback... tools) {
        if (tools == null || tools.length == 0) {
            return call(systemPrompt, userPrompt, caller);
        }
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .functions(tools)
                    .toolContext(java.util.Map.of("caller", caller))
                    .call()
                    .content();
        } catch (Exception e) {
            return fallbackAdvisor.fallback(caller, e);
        }
    }
}
