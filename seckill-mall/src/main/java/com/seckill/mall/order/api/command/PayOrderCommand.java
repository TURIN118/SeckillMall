package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付订单命令。
 *
 * <p>业务语义：支付普通订单（WALLET 方式扣钱包余额，其他方式模拟支付；UNPAID → PAID）。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderLifecycleService.payNormalOrder(userId, orderId, payMethod)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderCommand {

    /** 普通订单 ID（必填） */
    private Long orderId;

    /** 支付方式（WALLET/ALIPAY/WECHAT，必填） */
    private String payMethod;
}