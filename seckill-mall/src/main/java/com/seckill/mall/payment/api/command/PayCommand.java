package com.seckill.mall.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付扣款命令。
 *
 * <p>原方法：{@code PaymentService.pay(Long userId, BigDecimal amount, String payMethod)}
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 支付金额（必填） */
    private BigDecimal amount;

    /** 支付方式：WALLET/ALIPAY/WECHAT 等（必填） */
    private String payMethod;
}