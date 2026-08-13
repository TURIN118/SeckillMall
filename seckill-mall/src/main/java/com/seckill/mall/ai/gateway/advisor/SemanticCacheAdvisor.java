package com.seckill.mall.ai.gateway.advisor;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 语义缓存 Advisor（空壳，T5 实现）
 * <p>基于 PgVector + 余弦相似度（阈值 0.95）命中缓存直接返回，避免重复调用大模型。
 */
public class SemanticCacheAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(SemanticCacheAdvisor.class);

    public SemanticCacheAdvisor() {
        log.info("SemanticCacheAdvisor 初始化（空壳模式，T5 将实现语义缓存逻辑）");
    }

    @Override
    public String getName() {
        return "SemanticCacheAdvisor";
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
