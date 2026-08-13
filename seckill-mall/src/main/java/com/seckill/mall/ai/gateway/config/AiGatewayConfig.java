package com.seckill.mall.ai.gateway.config;

import com.seckill.mall.ai.gateway.advisor.*;
import com.seckill.mall.ai.gateway.entity.AiAuditMapper;
import com.seckill.mall.cache.CacheDegradeService;
import com.seckill.mall.cache.RedisService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * AI 网关配置：装配 ChatClient 单例 + 五大 Advisor。
 * <p>所有 AI 调用必须经此 ChatClient，实现"统一治理"。
 * <p>默认接入 DeepSeek（deepseek-chat），通过 OpenAI 兼容协议调用。
 */
@Configuration
@EnableConfigurationProperties(ModelRouteProperties.class)
public class AiGatewayConfig {

    /**
     * 限流 Lua 脚本 Bean（令牌桶），复用 seckill-mall 既有 lua/rate_limit.lua。
     * <p>供 {@link RateLimitAdvisor} 与 {@link com.seckill.mall.aspect.RateLimitAspect} 共用同一脚本。
     * <p>返回类型 Long：1=允许 / 0=拒绝。
     */
    @Bean("aiRateLimitScript")
    public DefaultRedisScript<Long> aiRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public RateLimitAdvisor rateLimitAdvisor(StringRedisTemplate redisTemplate,
                                             DefaultRedisScript<Long> aiRateLimitScript,
                                             @Value("${ai.gateway.rate-limit.user-capacity:20}") int userCapacity,
                                             @Value("${ai.gateway.rate-limit.user-rate:10}") int userRate,
                                             @Value("${ai.gateway.rate-limit.ip-capacity:100}") int ipCapacity,
                                             @Value("${ai.gateway.rate-limit.ip-rate:50}") int ipRate,
                                             @Value("${ai.gateway.rate-limit.window-seconds:60}") int windowSeconds) {
        return new RateLimitAdvisor(redisTemplate, aiRateLimitScript,
                userCapacity, userRate, ipCapacity, ipRate, windowSeconds);
    }

    @Bean
    public SemanticCacheAdvisor semanticCacheAdvisor(RedisService redisService) {
        return new SemanticCacheAdvisor(redisService);
    }

    @Bean
    public AuditAdvisor auditAdvisor(AiAuditMapper aiAuditMapper) {
        return new AuditAdvisor(aiAuditMapper);
    }

    @Bean
    public FallbackAdvisor fallbackAdvisor(CacheDegradeService cacheDegradeService) {
        return new FallbackAdvisor(cacheDegradeService);
    }

    @Bean
    public BudgetAdvisor budgetAdvisor(RedisService redisService,
                                       @Value("${ai.gateway.budget.daily-token-limit:2000000}") long dailyTokenLimit,
                                       @Value("${ai.gateway.budget.monthly-cost-limit:5000.00}") double monthlyCostLimit) {
        return new BudgetAdvisor(redisService, dailyTokenLimit, monthlyCostLimit);
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
                                  BudgetAdvisor budgetAdvisor,
                                  RateLimitAdvisor rateLimitAdvisor,
                                  SemanticCacheAdvisor semanticCacheAdvisor,
                                  AuditAdvisor auditAdvisor,
                                  FallbackAdvisor fallbackAdvisor) {
        // Advisor 执行顺序按 order 升序：budget(5) → rateLimit(10) → cache(20) → audit(30) → fallback(40)
        return ChatClient.builder(chatModel)
                .defaultOptions(defaultChatOptions)
                .defaultAdvisors(budgetAdvisor, rateLimitAdvisor, semanticCacheAdvisor,
                        auditAdvisor, fallbackAdvisor)
                .build();
    }
}
