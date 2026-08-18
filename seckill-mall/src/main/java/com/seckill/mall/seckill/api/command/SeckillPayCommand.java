package com.seckill.mall.seckill.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀订单支付命令。
 *
 * <p>原方法：{@code SeckillOrderService.payOrder(Long userId, Long orderId, String payMethod)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 6.2 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillPayCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 订单 ID（必填） */
    private Long orderId;

    /** 支付方式：WALLET/ALIPAY/WECHAT 等（必填） */
    private String payMethod;
}