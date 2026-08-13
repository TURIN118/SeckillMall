package com.seckill.mall.ai.gateway.config;

import com.seckill.mall.ai.gateway.advisor.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 网关配置：装配 ChatClient 单例 + 五大 Advisor。
 * <p>所有 AI 调用必须经此 ChatClient，实现"统一治理"。
 * <p>默认接入 DeepSeek（deepseek-chat），通过 OpenAI 兼容协议调用。
 */
@Configuration
public class AiGatewayConfig {

    @Bean
    public RateLimitAdvisor rateLimitAdvisor() {
        return new RateLimitAdvisor();
    }

    @Bean
    public SemanticCacheAdvisor semanticCacheAdvisor() {
        return new SemanticCacheAdvisor();
    }

    @Bean
    public AuditAdvisor auditAdvisor() {
        return new AuditAdvisor();
    }

    @Bean
    public FallbackAdvisor fallbackAdvisor() {
        return new FallbackAdvisor();
    }

    @Bean
    public BudgetAdvisor budgetAdvisor() {
        return new BudgetAdvisor();
    }

    /**
     * 默认聊天参数：DeepSeek 模型 + 保守温度 + 2000 token 上限。
     * <p>Spring AI 1.0.0-M3 的 OpenAiChatOptions.Builder 使用 withXxx 风格。
     */
    @Bean
    public OpenAiChatOptions defaultChatOptions() {
        return OpenAiChatOptions.builder()
                .withModel("deepseek-chat")
                .withTemperature(0.7)
                .withMaxTokens(2000)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel,
                                  OpenAiChatOptions defaultChatOptions,
                                  RateLimitAdvisor rateLimitAdvisor,
                                  SemanticCacheAdvisor semanticCacheAdvisor,
                                  AuditAdvisor auditAdvisor,
                                  FallbackAdvisor fallbackAdvisor,
                                  BudgetAdvisor budgetAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultOptions(defaultChatOptions)
                .defaultAdvisors(rateLimitAdvisor, semanticCacheAdvisor,
                        auditAdvisor, fallbackAdvisor, budgetAdvisor)
                .build();
    }
}