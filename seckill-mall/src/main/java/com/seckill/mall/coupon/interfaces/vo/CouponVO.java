package com.seckill.mall.coupon.interfaces.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券视图对象
 * <p>
 * 用于后台管理与前台展示的优惠券信息。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CouponVO {

    /** 主键ID */
    private Long id;

    /** 优惠券名称 */
    private String name;

    /** 类型：AMOUNT-满减 / DISCOUNT-折扣 */
    private String type;

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

    /** 剩余可领取数 */
    private Integer remainCount;

    /** 有效期开始 */
    private LocalDateTime startTime;

    /** 有效期结束 */
    private LocalDateTime endTime;

    /** 状态：1-启用 / 0-停用 */
    private Integer status;

    /** 适用范围：ALL-全站 / CATEGORY-分类 / PRODUCT-商品 */
    private String scopeType;

    /** 适用分类ID（scopeType=CATEGORY时有效） */
    private Long categoryId;

    /** 适用商品ID（scopeType=PRODUCT时有效） */
    private Long productId;

    /** 适用范围描述（如"仅限手机数码"），通用券为 null */
    private String scopeLabel;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}