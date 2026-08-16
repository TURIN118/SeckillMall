package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.Coupon;
import com.seckill.mall.entity.UserCoupon;
import com.seckill.mall.entity.enums.CouponType;
import com.seckill.mall.entity.enums.UserCouponStatus;
import com.seckill.mall.mapper.CouponMapper;
import com.seckill.mall.mapper.UserCouponMapper;
import com.seckill.mall.service.CouponUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券核销服务实现（Phase P2-3 从 CouponServiceImpl 拆分而来）。
 * <p>
 * 仅承载优惠券在订单流程中的核销/回退/计价能力，业务逻辑与原 CouponServiceImpl 完全一致，
 * 仅做机械移动以消除 God Service 行数超限问题。
 * <p>
 * 事务策略：
 * <ul>
 *   <li>{@link #calculateDiscount} 纯查询计算，无副作用，不加事务</li>
 *   <li>{@link #useCoupon} / {@link #revertCoupon} 涉及多表写入，需事务保护</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponUsageServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponUsageServiceImpl implements CouponUsageService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public BigDecimal calculateDiscount(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds) {
        if (userCouponId == null || userId == null || orderAmount == null) {
            return BigDecimal.ZERO;
        }
        // 查询用户优惠券
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
            return BigDecimal.ZERO;
        }
        // 校验状态：必须未使用
        if (userCoupon.getStatus() != UserCouponStatus.UNUSED) {
            return BigDecimal.ZERO;
        }
        // 查询关联优惠券
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        // 校验有效期
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            return BigDecimal.ZERO;
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            return BigDecimal.ZERO;
        }
        // 适用小计：当前简化处理，传入的 orderAmount 已是前端按 scope 预计算的适用商品小计
        BigDecimal applicableAmount = orderAmount;
        // 校验门槛：适用小计 >= minAmount
        if (coupon.getMinAmount() != null && applicableAmount.compareTo(coupon.getMinAmount()) < 0) {
            return BigDecimal.ZERO;
        }
        // 按券类型计算优惠金额
        BigDecimal discount;
        if (coupon.getType() == CouponType.AMOUNT) {
            // 满减券：优惠 = coupon.amount
            discount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        } else if (coupon.getType() == CouponType.DISCOUNT) {
            // 折扣券：优惠 = 适用小计 × (1 - coupon.amount)
            if (coupon.getAmount() == null) {
                discount = BigDecimal.ZERO;
            } else {
                BigDecimal discountRate = BigDecimal.ONE.subtract(coupon.getAmount());
                discount = applicableAmount.multiply(discountRate);
            }
        } else {
            discount = BigDecimal.ZERO;
        }
        // 优惠金额不能超过适用小计（避免负实付）
        if (discount.compareTo(applicableAmount) > 0) {
            discount = applicableAmount;
        }
        // 优惠金额不能为负
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userCouponId, Long userId, Long orderId) {
        if (userCouponId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户优惠券ID/用户ID不能为空");
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, "优惠券不存在或不属于当前用户");
        }
        if (userCoupon.getStatus() != UserCouponStatus.UNUSED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券已使用或已过期，无法核销");
        }
        // 乐观锁更新：UNUSED → USED，防并发重复核销
        int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, userCouponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(UserCoupon::getStatus, UserCouponStatus.USED)
                .set(UserCoupon::getUseTime, LocalDateTime.now())
                .set(UserCoupon::getOrderId, orderId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券核销失败，可能已被并发使用");
        }
        // Coupon.usedCount 原子 +1
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, userCoupon.getCouponId())
                .setSql("used_count = used_count + 1"));
        log.info("优惠券核销成功，userCouponId={}, userId={}, orderId={}", userCouponId, userId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revertCoupon(Long userCouponId, Long userId) {
        if (userCouponId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户优惠券ID/用户ID不能为空");
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, "优惠券不存在或不属于当前用户");
        }
        if (userCoupon.getStatus() != UserCouponStatus.USED) {
            // 幂等：非 USED 状态直接返回（可能已回退过）
            log.info("优惠券回退幂等返回，userCouponId={}, status={}", userCouponId, userCoupon.getStatus());
            return;
        }
        // 乐观锁更新：USED → UNUSED，清除使用时间与订单ID
        int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, userCouponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.USED)
                .set(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(UserCoupon::getUseTime, null)
                .set(UserCoupon::getOrderId, null));
        if (rows == 0) {
            log.warn("优惠券回退失败，userCouponId={}，可能被并发修改", userCouponId);
            return;
        }
        // Coupon.usedCount 原子 -1（防负数兜底）
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, userCoupon.getCouponId())
                .gt(Coupon::getUsedCount, 0)
                .setSql("used_count = used_count - 1"));
        log.info("优惠券回退成功，userCouponId={}, userId={}", userCouponId, userId);
    }
}