package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认收货命令。
 *
 * <p>业务语义：确认收货（SHIPPED → COMPLETED）。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderLifecycleService.confirmNormalOrder(userId, orderId)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReceiptCommand {

    /** 普通订单 ID（必填） */
    private Long orderId;
}