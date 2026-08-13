package com.seckill.mall.ai.gateway.advisor;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 预算护栏 Advisor（空壳，T8 实现）
 * <p>按日 token 上限 / 月成本上限 / 降级阈值三档护栏，超阈值时拒绝调用或自动降级到低成本模型。
 */
public class BudgetAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(BudgetAdvisor.class);

    public BudgetAdvisor() {
        log.info("BudgetAdvisor 初始化（空壳模式，T8 将实现预算护栏逻辑）");
    }

    @Override
    public String getName() {
        return "BudgetAdvisor";
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
