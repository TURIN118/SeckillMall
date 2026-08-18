package com.seckill.mall.coupon.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后台发放优惠券给指定用户命令。
 *
 * <p>原方法：{@code CouponService.distribute(Long couponId, Long userId)}
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributeCouponCommand {

    /** 优惠券 ID（必填） */
    private Long couponId;

    /** 用户 ID（必填） */
    private Long userId;
}