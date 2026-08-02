package com.seckill.mall.controller;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.CouponCreateRequest;
import com.seckill.mall.service.CouponService;
import com.seckill.mall.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 优惠券后台管理 Controller
 * <p>
 * 前缀 {@code /api/v1/admin/coupons}，需 ADMIN 角色。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminCouponController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "优惠券后台管理", description = "优惠券 CRUD/启停/发放")
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    @Operation(summary = "查询优惠券列表（分页）")
    @GetMapping("/list")
    public Result<PageResult<CouponVO>> list(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                             @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(couponService.listPage(pageNum, pageSize, name, status));
    }

    @Operation(summary = "创建优惠券")
    @PostMapping("/create")
    public Result<CouponVO> create(@Valid @RequestBody CouponCreateRequest req) {
        return Result.success("创建优惠券成功", couponService.create(req));
    }

    @Operation(summary = "编辑优惠券")
    @PutMapping("/{id}/update")
    public Result<CouponVO> update(@PathVariable Long id, @Valid @RequestBody CouponCreateRequest req) {
        return Result.success("编辑优惠券成功", couponService.update(id, req));
    }

    @Operation(summary = "删除优惠券")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.<Void>success("删除优惠券成功", null);
    }

    @Operation(summary = "启用/停用优惠券")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        couponService.updateStatus(id, body.get("status"));
        return Result.<Void>success("状态更新成功", null);
    }

    @Operation(summary = "发放优惠券给指定用户")
    @PostMapping("/{id}/distribute")
    public Result<Void> distribute(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) {
            return Result.error(1001, "用户ID不能为空");
        }
        couponService.distribute(id, userId);
        return Result.<Void>success("发放成功", null);
    }
}