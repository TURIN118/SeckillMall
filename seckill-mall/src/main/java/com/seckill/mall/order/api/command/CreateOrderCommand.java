package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 购物车结算创建订单命令。
 *
 * <p>业务语义：用户从购物车结算创建普通订单。
 * 校验地址+购物车项 → 计算总额 → 建订单与明细 → 扣库存 → 删购物车项 → 发延迟消息。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderService.createOrderFromCart(userId, addressId, cartIds, remark, userCouponId)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {

    /** 收货地址 ID（必填） */
    private Long addressId;

    /** 待结算购物车项 ID 列表（非空，必填） */
    private List<Long> cartIds;

    /** 备注（可选） */
    private String remark;

    /** 用户优惠券 ID（null 表示不使用优惠券） */
    private Long userCouponId;
}