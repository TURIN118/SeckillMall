package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发货命令（管理员操作）。
 *
 * <p>业务语义：普通订单发货（PAID → SHIPPED，设置物流信息）。
 *
 * <p>操作人身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderLifecycleService.shipNormalOrder(userId, orderId, shippingCompany, shippingNo)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipCommand {

    /** 普通订单 ID（必填） */
    private Long orderId;

    /** 物流公司（必填） */
    private String shippingCompany;

    /** 快递单号（必填） */
    private String shippingNo;
}