package com.seckill.mall.coupon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券 DTO，替代 UserCoupon Entity / UserCouponVO / AdminCouponRecordVO 跨模块与跨层传递。
 *
 * <p>包含用户领取记录以及关联优惠券的详细信息（含用户名、券名等关联字段）。
 *
 * <p>来源映射：
 * <ul>
 *     <li>UserCoupon + Coupon + username → UserCouponDTO（{@code CouponApiConverter.toUserCouponDTO}）</li>
 *     <li>UserCouponVO → UserCouponDTO（{@code CouponApiConverter.toUserCouponDTOFromVO}）</li>
 * </ul>
 *
 * <p>status 字段：Entity 层 {@code UserCouponStatus} 枚举 → DTO 层 {@code String}（getCode()）。
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponDTO {

    /** 用户优惠券主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 用户名（关联 t_user.username） */
    private String username;

    /** 优惠券 ID */
    private Long couponId;

    /** 优惠券名称（关联字段） */
    private String couponName;

    /** 优惠券类型：AMOUNT-满减 / DISCOUNT-折扣（关联字段） */
    private String couponType;

    /** 满减金额或折扣值（关联字段） */
    private BigDecimal couponAmount;

    /** 最低消费金额（关联字段） */
    private BigDecimal minAmount;

    /** 有效期开始（关联字段） */
    private LocalDateTime couponStartTime;

    /** 有效期结束（关联字段） */
    private LocalDateTime couponEndTime;

    /** 状态：UNUSED-未使用 / USED-已使用 / EXPIRED-已过期 */
    private String status;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 使用的订单 ID */
    private Long orderId;

    /** 创建时间 */
    private LocalDateTime createTime;
}