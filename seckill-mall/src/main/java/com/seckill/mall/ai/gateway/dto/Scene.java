package com.seckill.mall.ai.gateway.dto;

/**
 * AI 调用场景枚举，用于多模型路由。
 * <p>不同场景路由到不同模型，实现成本与质量平衡。
 */
public enum Scene {
    /** 推理类（导购、问数）→ DeepSeek */
    REASONING,
    /** 中文理解（客服闲聊）→ 通义 */
    CHINESE,
    /** 合规场景（AIGC 文案审核）→ 文心 */
    COMPLIANCE
}