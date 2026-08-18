package com.seckill.mall.coupon.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 优惠金额计算结果，{@code CouponUsageApi.calculateDiscount} 返回值。
 *
 * <p>过渡期仅含 {@code discount} 字段，保持与旧 {@code CouponUsageService.calculateDiscount}
 * 返回 {@code BigDecimal} 等价。未来可扩展 {@code applicable}（是否可用）、{@code reason}（不可用原因）等字段。
 *
 * <p>OrderServiceImpl 适配：{@code BigDecimal discount = couponUsageApi.calculateDiscount(command).getDiscount();}
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCalcResult {

    /** 优惠金额（非负），不可用时为 {@code BigDecimal.ZERO} */
    private BigDecimal discount;
}