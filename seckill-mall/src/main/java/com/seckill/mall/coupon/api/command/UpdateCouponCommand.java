package com.seckill.mall.coupon.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 编辑优惠券命令。
 *
 * <p>原方法：{@code CouponService.update(Long id, CouponCreateRequest req)}
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponCommand {

    /** 优惠券 ID（必填） */
    private Long id;

    /** 优惠券名称（必填） */
    private String name;

    /** 类型：AMOUNT-满减 / DISCOUNT-折扣（必填） */
    private String type;

    /** 满减金额或折扣值（必填） */
    private BigDecimal amount;

    /** 最低消费金额（必填） */
    private BigDecimal minAmount;

    /** 发放总数（必填） */
    private Integer totalCount;

    /** 有效期开始（必填） */
    private LocalDateTime startTime;

    /** 有效期结束（必填） */
    private LocalDateTime endTime;

    /** 状态：1-启用 / 0-停用（可选） */
    private Integer status;

    /** 适用范围：ALL-全站 / CATEGORY-分类 / PRODUCT-商品（可选，默认ALL） */
    private String scopeType;

    /** 适用分类ID（scopeType=CATEGORY时有效） */
    private Long categoryId;

    /** 适用商品ID（scopeType=PRODUCT时有效） */
    private Long productId;
}