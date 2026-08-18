package com.seckill.mall.payment.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 充值结果，{@code RechargeCardApi.recharge} 返回值。
 *
 * <p>过渡期仅含 {@code newBalance} 字段，保持与旧 {@code RechargeCardService.recharge}
 * 返回 {@code BigDecimal} 等价。未来可扩展 {@code faceValue}（本次充值面额）、{@code cardNo}（卡号）等字段。
 *
 * <p>WalletController 适配：{@code BigDecimal newBalance = rechargeCardApi.recharge(command).getNewBalance();}
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeResult {

    /** 充值后的最新余额 */
    private BigDecimal newBalance;
}