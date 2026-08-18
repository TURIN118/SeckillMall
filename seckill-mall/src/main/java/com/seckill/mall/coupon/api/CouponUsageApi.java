package com.seckill.mall.coupon.api;

import com.seckill.mall.coupon.api.command.RevertCouponCommand;
import com.seckill.mall.coupon.api.command.UseCouponCommand;
import com.seckill.mall.coupon.api.result.CouponCalcResult;

/**
 * Coupon 模块优惠券核销 API。
 *
 * <p>对外暴露优惠券在订单流程中的核销能力（计价 / 核销 / 回退），供 order 模块调用。
 *
 * <p>与 {@link CouponApi} 的边界：
 * <ul>
 *     <li>{@code CouponApi}：优惠券后台管理与前台领取/查询</li>
 *     <li>{@code CouponUsageApi}：订单流程中的券核销（计价 / 核销 / 回退）</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command 对象，禁止裸露多参数</li>
 *     <li>出参用 Result，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>原方法映射参见 COUPON-API-CONTRACT.md 第 8.2 节。
 *
 * @author wnj
 * @since Phase CP.2
 */
public interface CouponUsageApi {

    /**
     * 计算优惠金额（按 scope 筛选适用商品小计）。
     * <p>
     * 校验用户优惠券归属、状态（UNUSED）、有效期，按券类型计算优惠金额：
     * <ul>
     *     <li>AMOUNT（满减）：discount = coupon.amount</li>
     *     <li>DISCOUNT（折扣）：discount = 适用小计 × (1 - coupon.amount)</li>
     * </ul>
     * 未达门槛 / 已过期 / 已使用 等情况返回 {@code CouponCalcResult.discount = ZERO}。
     *
     * @param command 核销命令（使用 userCouponId/userId/orderAmount/productIds 字段）
     * @return 优惠计算结果（含 discount 字段）
     */
    CouponCalcResult calculateDiscount(UseCouponCommand command);

    /**
     * 核销优惠券（支付成功时调用）。
     * <p>
     * 校验用户优惠券归属与状态（UNUSED），更新为 USED，记录使用时间与订单ID，
     * 并将 Coupon.usedCount 原子 +1。
     *
     * @param command 核销命令（使用 userCouponId/userId/orderId 字段）
     */
    void useCoupon(UseCouponCommand command);

    /**
     * 回退优惠券（取消订单 / 退款时调用）。
     * <p>
     * 校验用户优惠券归属与状态（USED），更新回 UNUSED，清除使用时间与订单ID，
     * 并将 Coupon.usedCount 原子 -1。非 USED 状态幂等返回。
     *
     * @param command 回退命令（使用 userCouponId/userId 字段）
     */
    void revertCoupon(RevertCouponCommand command);
}