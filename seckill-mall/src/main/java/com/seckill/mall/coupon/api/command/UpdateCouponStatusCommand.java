package com.seckill.mall.coupon.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启用/停用优惠券命令。
 *
 * <p>原方法：{@code CouponService.updateStatus(Long id, Integer status)}
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponStatusCommand {

    /** 优惠券 ID（必填） */
    private Long id;

    /** 状态：1-启用 / 0-停用（必填） */
    private Integer status;
}