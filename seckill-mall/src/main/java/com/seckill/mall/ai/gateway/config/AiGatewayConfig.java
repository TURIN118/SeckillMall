package com.seckill.mall.ai.gateway.config;

import com.seckill.mall.ai.gateway.advisor.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 网关配置：装配 ChatClient 单例 + 五大 Advisor。
 * <p>所有 AI 调用必须经此 ChatClient，实现"统一治理"。
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

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel,
                                  RateLimitAdvisor rateLimitAdvisor,
                                  SemanticCacheAdvisor semanticCacheAdvisor,
                                  AuditAdvisor auditAdvisor,
                                  FallbackAdvisor fallbackAdvisor,
                                  BudgetAdvisor budgetAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(rateLimitAdvisor, semanticCacheAdvisor,
                        auditAdvisor, fallbackAdvisor, budgetAdvisor)
                .build();
    }
}