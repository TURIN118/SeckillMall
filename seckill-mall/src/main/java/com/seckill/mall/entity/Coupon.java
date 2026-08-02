package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.CouponType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体
 * <p>
 * 对应表 {@code t_coupon}，定义优惠券的发放规则与库存计数。
 * <ul>
 *   <li>type=AMOUNT  → amount 为满减金额</li>
 *   <li>type=DISCOUNT → amount 为折扣值（如 0.85 表示85折）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：Coupon.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_coupon")
public class Coupon {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 优惠券名称 */
    private String name;

    /** 类型：AMOUNT-满减 / DISCOUNT-折扣 */
    @TableField("type")
    private CouponType type;

    /** 满减金额或折扣值（如 0.85 表示85折） */
    private BigDecimal amount;

    /** 最低消费金额 */
    private BigDecimal minAmount;

    /** 发放总数 */
    private Integer totalCount;

    /** 已领取数 */
    private Integer receivedCount;

    /** 已使用数 */
    private Integer usedCount;

    /** 有效期开始 */
    private LocalDateTime startTime;

    /** 有效期结束 */
    private LocalDateTime endTime;

    /** 状态：1-启用 / 0-停用 */
    private Integer status;

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