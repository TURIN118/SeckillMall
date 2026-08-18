package com.seckill.mall.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值卡生成 DTO，含明文卡密，仅生成接口返回一次。
 *
 * <p>替代 RechargeCardGenerateVO 作为 Application 层返回值。
 *
 * <p>来源映射：RechargeCard Entity + plainPwd → RechargeCardGenerateDTO
 * （由 {@code RechargeCardServiceImpl.generate} 逻辑转换）。
 *
 * <p><b>注意</b>：本 DTO 含 {@code cardPassword} 明文字段，仅在生成接口返回一次，
 * 不应用于列表/查询场景（那些场景应使用 {@link RechargeCardDTO}）。
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCardGenerateDTO {

    /** 主键ID */
    private Long id;

    /** 卡号 */
    private String cardNo;

    /**
     * 卡密明文（仅生成时返回一次）。
     * <p>
     * 不带 @JsonIgnore，Jackson 默认序列化该字段。
     */
    private String cardPassword;

    /** 面额 */
    private BigDecimal faceValue;

    /** 状态：UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
    private String status;

    /** 批次号 */
    private String batchNo;

    /** 创建时间 */
    private LocalDateTime createTime;
}