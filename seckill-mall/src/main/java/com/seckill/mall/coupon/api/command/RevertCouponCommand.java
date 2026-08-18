package com.seckill.mall.coupon.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回退优惠券命令（取消订单 / 退款时调用）。
 *
 * <p>原方法：{@code CouponUsageService.revertCoupon(Long userCouponId, Long userId)}
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevertCouponCommand {

    /** 用户优惠券 ID（必填） */
    private Long userCouponId;

    /** 用户 ID（必填，校验归属） */
    private Long userId;
}