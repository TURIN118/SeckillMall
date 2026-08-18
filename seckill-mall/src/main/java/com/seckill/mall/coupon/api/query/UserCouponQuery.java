package com.seckill.mall.coupon.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户优惠券查询条件（前台 listMine）。
 *
 * <p>原方法：{@code CouponService.listMine(Long userId, String status)}
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponQuery {

    /** 用户 ID（必填） */
    private Long userId;

    /** 状态筛选（可空）：UNUSED/USED/EXPIRED */
    private String status;
}