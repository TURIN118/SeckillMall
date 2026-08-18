package com.seckill.mall.coupon.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券分页查询条件（后台 adminListCoupons + adminListRecords 共用）。
 *
 * <p>原方法：
 * <ul>
 *     <li>{@code CouponService.listPage(Integer pageNum, Integer pageSize, String name, Integer status)}</li>
 *     <li>{@code CouponService.listRecords(Long couponId, Integer pageNum, Integer pageSize)}</li>
 * </ul>
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponQuery {

    /** 页码（默认1） */
    private Integer pageNum;

    /** 每页大小（默认10） */
    private Integer pageSize;

    /** 名称模糊筛选（可空） */
    private String name;

    /** 状态筛选（可空）：1-启用 / 0-停用 */
    private Integer status;
}