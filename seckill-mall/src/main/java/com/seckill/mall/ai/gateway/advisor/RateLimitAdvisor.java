package com.seckill.mall.ai.gateway.advisor;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 限流 Advisor（空壳，T4 实现）
 * <p>基于 Redis 令牌桶，按用户/IP/模型维度限流，复用现有 RateLimitAspect 的 Lua 脚本模式。
 */
public class RateLimitAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAdvisor.class);

    public RateLimitAdvisor() {
        log.info("RateLimitAdvisor 初始化（空壳模式，T4 将实现限流逻辑）");
    }

    @Override
    public String getName() {
        return "RateLimitAdvisor";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
