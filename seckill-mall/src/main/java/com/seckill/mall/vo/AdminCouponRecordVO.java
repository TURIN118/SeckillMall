package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台优惠券领取记录视图对象
 * <p>
 * 关联 t_user_coupon 领取记录，并 JOIN t_user 补 username、JOIN t_coupon 补 couponName。
 * 用于后台「优惠券管理 - 领取记录」弹窗展示。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminCouponRecordVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class AdminCouponRecordVO {

    /** 领取记录主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名（关联 t_user.username） */
    private String username;

    /** 优惠券ID */
    private Long couponId;

    /** 优惠券名称（关联 t_coupon.name） */
    private String couponName;

    /** 状态：UNUSED-未使用 / USED-已使用 / EXPIRED-已过期 */
    private String status;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 使用的订单ID */
    private Long orderId;

    /** 创建时间 */
    private LocalDateTime createTime;
}