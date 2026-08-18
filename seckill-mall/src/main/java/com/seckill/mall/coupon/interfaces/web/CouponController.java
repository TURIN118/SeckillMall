package com.seckill.mall.coupon.interfaces.web;

import com.seckill.mall.common.Result;
import com.seckill.mall.coupon.api.CouponApi;
import com.seckill.mall.coupon.api.command.ClaimCouponCommand;
import com.seckill.mall.coupon.api.dto.CouponDTO;
import com.seckill.mall.coupon.api.dto.UserCouponDTO;
import com.seckill.mall.coupon.api.query.UserCouponQuery;
import com.seckill.mall.coupon.application.facade.CouponApiConverter;
import com.seckill.mall.coupon.interfaces.vo.CouponVO;
import com.seckill.mall.coupon.interfaces.vo.UserCouponVO;
import com.seckill.mall.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券前台 Controller（Phase CP.4 已切换到 {@link CouponApi}）。
 * <p>
 * 前缀 {@code /api/v1/coupons}，需登录（BUYER/ADMIN）。
 *
 * <p>Strangler Pattern：注入 {@link CouponApi} 替代旧 {@code CouponService}，
 * 通过 {@link CouponApiConverter} 做 DTO → VO 转换，保持前端出参结构不变。
 *
 * 创建人：@author WNJ
 */
@Tag(name = "优惠券前台", description = "可领取列表/领取/我的优惠券")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class CouponController {

    private final CouponApi couponApi;
    private final SecurityUtils securityUtils;

    @Operation(summary = "可领取的优惠券列表（支持按商品筛选）")
    @GetMapping("/available")
    public Result<List<CouponVO>> available(@RequestParam(required = false) Long productId) {
        List<CouponDTO> dtoList = couponApi.listAvailableCoupons(productId);
        return Result.success(CouponApiConverter.toVOList(dtoList));
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        ClaimCouponCommand command = ClaimCouponCommand.builder()
                .couponId(id)
                .userId(userId)
                .build();
        couponApi.claimCoupon(command);
        return Result.<Void>success("领取成功", null);
    }

    @Operation(summary = "我的优惠券（可按状态筛选）")
    @GetMapping("/mine")
    public Result<List<UserCouponVO>> mine(@RequestParam(required = false) String status) {
        Long userId = securityUtils.getCurrentUserId();
        UserCouponQuery query = UserCouponQuery.builder()
                .userId(userId)
                .status(status)
                .build();
        List<UserCouponDTO> dtoList = couponApi.listMyCoupons(query);
        return Result.success(CouponApiConverter.toUserCouponVOList(dtoList));
    }
}
