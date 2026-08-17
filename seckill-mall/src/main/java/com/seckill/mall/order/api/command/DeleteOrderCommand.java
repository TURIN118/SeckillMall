package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 逻辑删除订单命令。
 *
 * <p>业务语义：逻辑删除订单（仅 COMPLETED/CANCELLED 可删除，支持秒杀+普通两类）。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderQueryService.deleteOrder(orderId, userId)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteOrderCommand {

    /** 订单 ID（秒杀或普通，必填） */
    private Long orderId;
}