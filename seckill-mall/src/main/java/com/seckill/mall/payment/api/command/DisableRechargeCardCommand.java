package com.seckill.mall.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 禁用充值卡命令。
 *
 * <p>原方法：{@code RechargeCardService.disable(Long id)}
 *
 * @author wnj
 * @since Phase PM.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisableRechargeCardCommand {

    /** 充值卡 ID（必填） */
    private Long id;
}