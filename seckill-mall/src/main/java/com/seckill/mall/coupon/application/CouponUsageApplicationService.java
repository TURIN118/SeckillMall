package com.seckill.mall.coupon.application;

import com.seckill.mall.coupon.api.CouponUsageApi;
import com.seckill.mall.coupon.api.command.RevertCouponCommand;
import com.seckill.mall.coupon.api.command.UseCouponCommand;
import com.seckill.mall.coupon.api.result.CouponCalcResult;
import com.seckill.mall.service.CouponUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * CouponUsage 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link CouponUsageApi}，内部委托给旧 {@link CouponUsageService}，
 * 将旧 Service 返回的 {@code BigDecimal} 包装为 {@link CouponCalcResult}。
 *
 * <p>本类不包含任何业务逻辑，仅做 Command 解包与结果包装。
 *
 * @author wnj
 * @since Phase CP.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponUsageApplicationService implements CouponUsageApi {

    private final CouponUsageService couponUsageService;

    @Override
    public CouponCalcResult calculateDiscount(UseCouponCommand command) {
        BigDecimal discount = couponUsageService.calculateDiscount(
                command.getUserCouponId(),
                command.getUserId(),
                command.getOrderAmount(),
                command.getProductIds());
        return CouponCalcResult.builder()
                .discount(discount == null ? BigDecimal.ZERO : discount)
                .build();
    }

    @Override
    public void useCoupon(UseCouponCommand command) {
        couponUsageService.useCoupon(
                command.getUserCouponId(),
                command.getUserId(),
                command.getOrderId());
    }

    @Override
    public void revertCoupon(RevertCouponCommand command) {
        couponUsageService.revertCoupon(
                command.getUserCouponId(),
                command.getUserId());
    }
}