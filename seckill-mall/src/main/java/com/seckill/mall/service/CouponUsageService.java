package com.seckill.mall.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券核销服务接口（Phase P2-3 从 CouponService 拆分而来）。
 * <p>
 * 仅承载优惠券在订单流程中的核销/回退/计价能力，供订单创建与生命周期服务调用。
 * <p>
 * 与 {@link CouponService} 的边界：
 * <ul>
 *   <li>{@code CouponService}：优惠券后台管理与前台领取/查询</li>
 *   <li>{@code CouponUsageService}：订单流程中的券核销（计价 / 核销 / 回退）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponUsageService.java
 * 邮箱：nj651217@163.com
 */
public interface CouponUsageService {

    /**
     * 计算优惠金额（按 scope 筛选适用商品小计）。
     * <p>
     * 校验用户优惠券归属、状态（UNUSED）、有效期，按券类型计算优惠金额：
     * <ul>
     *   <li>AMOUNT（满减）：discount = coupon.amount</li>
     *   <li>DISCOUNT（折扣）：discount = 适用小计 × (1 - coupon.amount)</li>
     * </ul>
     * 未达门槛 / 已过期 / 已使用 等情况返回 {@link BigDecimal#ZERO}。
     *
     * @param userCouponId 用户优惠券ID
     * @param userId       用户ID（校验归属）
     * @param orderAmount  适用商品小计（前端按 scope 预计算的参与优惠的金额）
     * @param productIds   订单商品ID列表（预留，当前简化处理使用 orderAmount）
     * @return 优惠金额（非负），不可用时返回 ZERO
     */
    BigDecimal calculateDiscount(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds);

    /**
     * 核销优惠券（支付成功时调用）。
     * <p>
     * 校验用户优惠券归属与状态（UNUSED），更新为 USED，记录使用时间与订单ID，
     * 并将 Coupon.usedCount 原子 +1。
     *
     * @param userCouponId 用户优惠券ID
     * @param userId       用户ID（校验归属）
     * @param orderId      关联订单ID
     */
    void useCoupon(Long userCouponId, Long userId, Long orderId);

    /**
     * 回退优惠券（取消订单 / 退款时调用）。
     * <p>
     * 校验用户优惠券归属与状态（USED），更新回 UNUSED，清除使用时间与订单ID，
     * 并将 Coupon.usedCount 原子 -1。
     *
     * @param userCouponId 用户优惠券ID
     * @param userId       用户ID（校验归属）
     */
    void revertCoupon(Long userCouponId, Long userId);
}