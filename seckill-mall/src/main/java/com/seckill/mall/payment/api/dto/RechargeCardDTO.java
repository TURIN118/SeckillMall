package com.seckill.mall.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值卡 DTO，替代 RechargeCard Entity / RechargeCardVO 跨模块与跨层传递。
 *
 * <p>包含充值卡完整字段（核心 + 展示），供 Application 层返回与 Controller 层转回 VO 使用。
 *
 * <p>来源映射：
 * <ul>
 *     <li>RechargeCard Entity → RechargeCardDTO（{@code PaymentApiConverter.toDTO}）</li>
 *     <li>RechargeCardVO → RechargeCardDTO（{@code PaymentApiConverter.toDTOFromVO}）</li>
 * </ul>
 *
 * <p>status 字段：Entity 层 {@code RechargeCardStatus} 枚举 → DTO 层 {@code String}（getCode()）。
 *
 * <p><b>注意</b>：本 DTO <b>不含</b> {@code cardPassword} 字段（脱敏，避免卡密泄露）。
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCardDTO {

    /** 主键ID */
    private Long id;

    /** 卡号 */
    private String cardNo;

    /** 面额 */
    private BigDecimal faceValue;

    /** 状态：UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
    private String status;

    /** 使用者用户ID */
    private Long usedBy;

    /** 使用时间 */
    private LocalDateTime usedTime;

    /** 批次号 */
    private String batchNo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}