package com.seckill.mall.order.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 立即购买命令（商品详情页直接下单）。
 *
 * <p>业务语义：用户在商品详情页立即购买，创建普通订单。
 * 校验商品状态/库存 → 扣库存 → 建订单与明细 → 发延迟取消消息。
 *
 * <p>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段。
 *
 * <p>原方法：{@code OrderService.createNormalOrder(userId, productId, skuId, quantity, addressId, remark, userCouponId)}
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyNowCommand {

    /** 商品 ID（必填） */
    private Long productId;

    /** SKU ID（null 表示无规格商品） */
    private Long skuId;

    /** 购买数量（≥1，必填） */
    private Integer quantity;

    /** 收货地址 ID（必填） */
    private Long addressId;

    /** 备注（可选） */
    private String remark;

    /** 用户优惠券 ID（null 表示不使用优惠券） */
    private Long userCouponId;
}