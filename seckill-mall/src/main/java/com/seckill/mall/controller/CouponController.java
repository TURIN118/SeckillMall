package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.CouponService;
import com.seckill.mall.vo.CouponVO;
import com.seckill.mall.vo.UserCouponVO;
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
 * 优惠券前台 Controller
 * <p>
 * 前缀 {@code /api/v1/coupons}，需登录（BUYER/ADMIN）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "优惠券前台", description = "可领取列表/领取/我的优惠券")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class CouponController {

    private final CouponService couponService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "可领取的优惠券列表")
    @GetMapping("/available")
    public Result<List<CouponVO>> available() {
        return Result.success(couponService.listAvailable());
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        couponService.receive(id, userId);
        return Result.<Void>success("领取成功", null);
    }

    @Operation(summary = "我的优惠券（可按状态筛选）")
    @GetMapping("/mine")
    public Result<List<UserCouponVO>> mine(@RequestParam(required = false) String status) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(couponService.listMine(userId, status));
    }
}