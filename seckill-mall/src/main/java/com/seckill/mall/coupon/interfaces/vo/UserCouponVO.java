package com.seckill.mall.coupon.interfaces.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券视图对象
 * <p>
 * 包含用户领取记录以及关联优惠券的详细信息。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserCouponVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UserCouponVO {

    /** 用户优惠券ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名（关联 t_user.username） */
    private String username;

    /** 优惠券ID */
    private Long couponId;

    /** 状态：UNUSED-未使用 / USED-已使用 / EXPIRED-已过期 */
    private String status;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 使用的订单ID */
    private Long orderId;

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

    /** 创建时间 */
    private LocalDateTime createTime;
}