package com.seckill.mall.ai.gateway.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 调用审计实体（append-only，无 is_deleted/update_time）
 * <p>对应表 {@code t_ai_audit}，由 {@link com.seckill.mall.ai.gateway.advisor.AuditAdvisor}
 * 在每次 LLM 调用结束后写入，记录 caller/model/tokens/cost/elapsed 等指标，
 * 供运营分析与成本治理。
 * <p>{@code createTime} 由 {@link com.seckill.mall.config.MetaObjectHandler} 自动填充。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiAudit.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_ai_audit")
public class AiAudit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 调用方标识（如 "chat"、"seckill-ai"），来自 toolContext */
    private String caller;

    /** 登录用户 ID（可空，匿名调用为 null） */
    private Long userId;

    /** 模型名（如 "deepseek-chat"），优先取响应元数据，回退 toolContext */
    private String model;

    /** prompt 的 SHA-256 哈希（用于去重分析，可空） */
    private String promptHash;

    /** 输入 token 数 */
    private Integer tokensIn;

    /** 输出 token 数 */
    private Integer tokensOut;

    /** 本次调用成本（¥，按模型费率计算） */
    private BigDecimal cost;

    /** 耗时（毫秒） */
    private Integer elapsedMs;

    /** 是否成功：1=成功，0=失败（异常或降级） */
    private Integer success;

    /** 是否升级人工：1=是，0=否（预留，P0 默认 0） */
    private Integer escalated;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}