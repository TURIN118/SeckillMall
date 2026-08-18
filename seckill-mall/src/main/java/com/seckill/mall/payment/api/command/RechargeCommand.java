package com.seckill.mall.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 充值卡充值命令。
 *
 * <p>原方法：{@code RechargeCardService.recharge(String cardNo, String cardPassword, Long userId)}
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCommand {

    /** 卡号（必填） */
    private String cardNo;

    /** 卡密明文（必填） */
    private String cardPassword;

    /** 用户 ID（必填） */
    private Long userId;
}