package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.UserCouponStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券实体
 * <p>
 * 对应表 {@code t_user_coupon}，记录用户领取的优惠券实例。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserCoupon.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_user_coupon")
public class UserCoupon {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 优惠券ID */
    private Long couponId;

    /** 状态：UNUSED-未使用 / USED-已使用 / EXPIRED-已过期 */
    @TableField("status")
    private UserCouponStatus status;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 使用的订单ID */
    private Long orderId;

    /** 逻辑删除：0-正常 / 1-已删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}