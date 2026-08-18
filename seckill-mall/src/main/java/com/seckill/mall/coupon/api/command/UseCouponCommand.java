package com.seckill.mall.coupon.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 核销优惠券命令（同时用于计价与核销）。
 *
 * <p>原方法：
 * <ul>
 *     <li>{@code CouponUsageService.calculateDiscount(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds)}</li>
 *     <li>{@code CouponUsageService.useCoupon(Long userCouponId, Long userId, Long orderId)}</li>
 * </ul>
 *
 * <p>字段使用说明：
 * <ul>
 *     <li>{@code calculateDiscount}：使用 userCouponId/userId/orderAmount/productIds（orderId 可空）</li>
 *     <li>{@code useCoupon}：使用 userCouponId/userId/orderId（orderAmount/productIds 可空）</li>
 * </ul>
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseCouponCommand {

    /** 用户优惠券 ID（必填） */
    private Long userCouponId;

    /** 用户 ID（必填，校验归属） */
    private Long userId;

    /** 关联订单 ID（useCoupon 必填，calculateDiscount 可空） */
    private Long orderId;

    /** 适用商品小计（calculateDiscount 必填，useCoupon 可空） */
    private BigDecimal orderAmount;

    /** 订单商品 ID 列表（预留，当前简化处理使用 orderAmount） */
    private List<Long> productIds;
}