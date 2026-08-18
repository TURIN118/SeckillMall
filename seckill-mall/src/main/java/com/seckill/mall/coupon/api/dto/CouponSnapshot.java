package com.seckill.mall.coupon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券只读快照，供跨模块计价/核销时传递优惠券核心字段。
 *
 * <p>当前 {@code CouponUsageApi} 方法入参为 {@code UseCouponCommand}（含 userCouponId），
 * ApplicationService 内部通过 userCouponId 查库获取 Coupon 信息，不需要调用方传入本快照。
 * 本类预留为未来优化（如缓存预加载场景），Phase CP.2 创建为最小类（仅含 id 字段），
 * 不在本次迁移中被使用。
 *
 * @author wnj
 * @since Phase CP.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSnapshot {

    /** 优惠券主键 ID */
    private Long id;
}