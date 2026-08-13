package com.seckill.mall.ai.gateway.advisor;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 审计 Advisor（空壳，T6 实现）
 * <p>记录每次调用的 caller、prompt 摘要、token 消耗、成本、耗时，落库 ai_audit_log 供运营分析。
 */
public class AuditAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(AuditAdvisor.class);

    public AuditAdvisor() {
        log.info("AuditAdvisor 初始化（空壳模式，T6 将实现审计落库逻辑）");
    }

    @Override
    public String getName() {
        return "AuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
