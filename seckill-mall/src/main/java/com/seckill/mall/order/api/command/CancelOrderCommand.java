package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消订单命令。
 *
 * <p>业务语义：取消普通订单（仅 UNPAID 可取消；回补库存；退优惠券；异步发取消邮件）。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderLifecycleService.cancelNormalOrder(userId, orderId)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand {

    /** 普通订单 ID（必填） */
    private Long orderId;
}