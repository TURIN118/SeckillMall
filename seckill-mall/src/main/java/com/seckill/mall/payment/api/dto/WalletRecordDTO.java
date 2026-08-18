package com.seckill.mall.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包交易记录 DTO，替代 WalletRecordVO 跨模块与跨层传递。
 *
 * <p>包含交易记录完整字段（核心 + 展示），供 Application 层返回与 Controller 层转回 VO 使用。
 *
 * <p>来源映射：
 * <ul>
 *     <li>WalletRecordVO → WalletRecordDTO（{@code PaymentApiConverter.toDTO}）</li>
 *     <li>RechargeCard + balanceAfter → WalletRecordDTO（{@code WalletServiceImpl.toRechargeVO} 逻辑转换）</li>
 * </ul>
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRecordDTO {

    /** 记录ID */
    private Long id;

    /** 交易类型：RECHARGE-充值 / CONSUME-消费 / REFUND-退款 */
    private String type;

    /** 交易金额（正数为入账，负数为出账） */
    private BigDecimal amount;

    /** 交易后余额 */
    private BigDecimal balanceAfter;

    /** 交易时间 */
    private LocalDateTime createTime;

    /** 备注 */
    private String remark;
}