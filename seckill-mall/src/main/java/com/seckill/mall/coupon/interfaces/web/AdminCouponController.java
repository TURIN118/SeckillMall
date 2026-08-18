package com.seckill.mall.coupon.interfaces.web;

import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.coupon.api.CouponApi;
import com.seckill.mall.coupon.api.command.CreateCouponCommand;
import com.seckill.mall.coupon.api.command.DistributeCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponStatusCommand;
import com.seckill.mall.coupon.api.dto.CouponDTO;
import com.seckill.mall.coupon.api.dto.UserCouponDTO;
import com.seckill.mall.coupon.api.query.CouponQuery;
import com.seckill.mall.coupon.application.facade.CouponApiConverter;
import com.seckill.mall.coupon.interfaces.vo.AdminCouponRecordVO;
import com.seckill.mall.coupon.interfaces.vo.CouponVO;
import com.seckill.mall.dto.CouponCreateRequest;
import com.seckill.mall.exception.BusinessException;
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
 * 优惠券后台管理 Controller（Phase CP.4 已切换到 {@link CouponApi}）。
 * <p>
 * 前缀 {@code /api/v1/admin/coupons}，需 ADMIN 角色。
 *
 * <p>Strangler Pattern：注入 {@link CouponApi} 替代旧 {@code CouponService}，
 * 通过 {@link CouponApiConverter} 做 DTO → VO 转换，保持前端出参结构不变。
 *
 * 创建人：@author WNJ
 */
@Tag(name = "优惠券后台管理", description = "优惠券 CRUD/启停/发放")
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponApi couponApi;

    @Operation(summary = "查询优惠券列表（分页）")
    @GetMapping("/list")
    public Result<PageResult<CouponVO>> list(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                             @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) Integer status) {
        CouponQuery query = CouponQuery.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .name(name)
                .status(status)
                .build();
        PageResult<CouponDTO> pageResult = couponApi.adminListCoupons(query);
        return Result.success(CouponApiConverter.toVOPageResult(pageResult));
    }

    @Operation(summary = "创建优惠券")
    @PostMapping("/create")
    public Result<CouponVO> create(@Valid @RequestBody CouponCreateRequest req) {
        CreateCouponCommand command = CouponApiConverter.toCreateCommand(req);
        CouponDTO dto = couponApi.createCoupon(command);
        return Result.success("创建优惠券成功", CouponApiConverter.toVO(dto));
    }

    @Operation(summary = "编辑优惠券")
    @PutMapping("/{id}/update")
    public Result<CouponVO> update(@PathVariable Long id, @Valid @RequestBody CouponCreateRequest req) {
        UpdateCouponCommand command = CouponApiConverter.toUpdateCommand(id, req);
        CouponDTO dto = couponApi.updateCoupon(command);
        return Result.success("编辑优惠券成功", CouponApiConverter.toVO(dto));
    }

    @Operation(summary = "删除优惠券")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponApi.deleteCoupon(id);
        return Result.<Void>success("删除优惠券成功", null);
    }

    @Operation(summary = "启用/停用优惠券")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        // 安全修复（M4）：服务端校验 status 取值范围，避免非法状态写入
        Integer status = body == null ? null : body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 仅允许 0(停用) 或 1(启用)");
        }
        UpdateCouponStatusCommand command = UpdateCouponStatusCommand.builder()
                .id(id)
                .status(status)
                .build();
        couponApi.updateCouponStatus(command);
        return Result.<Void>success("状态更新成功", null);
    }

    @Operation(summary = "发放优惠券给指定用户")
    @PostMapping("/{id}/distribute")
    public Result<String> distribute(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        // 安全修复（M4）：服务端校验 userId 非空且为正数
        Long userId = body == null ? null : body.get("userId");
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空且必须为正数");
        }
        DistributeCouponCommand command = DistributeCouponCommand.builder()
                .couponId(id)
                .userId(userId)
                .build();
        // 返回被发放用户的用户名，前端展示用（避免显示数字ID）
        String username = couponApi.distributeCoupon(command);
        return Result.success("发放成功", username);
    }

    @Operation(summary = "分页查询某优惠券的领取记录")
    @GetMapping("/{id}/records")
    public Result<PageResult<AdminCouponRecordVO>> records(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        CouponQuery query = CouponQuery.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        PageResult<UserCouponDTO> pageResult = couponApi.adminListRecords(id, query);
        return Result.success(CouponApiConverter.toAdminRecordVOPageResult(pageResult));
    }
}
