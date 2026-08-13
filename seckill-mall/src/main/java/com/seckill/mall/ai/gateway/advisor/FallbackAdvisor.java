package com.seckill.mall.ai.gateway.advisor;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 降级 Advisor（空壳，T7 实现）
 * <p>超时/异常时按多模型路由规则降级到备用模型（deepseek -> qwen -> ernie），并支持有限次重试。
 */
public class FallbackAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(FallbackAdvisor.class);

    public FallbackAdvisor() {
        log.info("FallbackAdvisor 初始化（空壳模式，T7 将实现降级与重试逻辑）");
    }

    @Override
    public String getName() {
        return "FallbackAdvisor";
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
