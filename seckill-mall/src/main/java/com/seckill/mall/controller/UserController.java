package com.seckill.mall.controller;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.EmailUpdateRequest;
import com.seckill.mall.dto.PhoneUpdateRequest;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.UserService;
import com.seckill.mall.service.VerificationCodeService;
import com.seckill.mall.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人信息 Controller
 * <p>
 * 前缀 {@code /api/v1/users}，需登录（BUYER/ADMIN）。
 * 提供修改手机号/邮箱接口，均需先校验验证码。
 * <p>
 * M-D2 修复：移除对 {@code UserMapper} 的直接依赖，全部下沉到 {@link UserService}，
 * Controller 仅编排并返回 {@code Result<VO>}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "用户个人信息", description = "修改手机号/邮箱（需验证码校验）")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class UserController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "修改手机号（需验证码校验）")
    @PutMapping("/profile/phone")
    public Result<UserVO> updatePhone(@Valid @RequestBody PhoneUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        // 先校验验证码（target 为新手机号）
        if (!verificationCodeService.verifyCode(req.getPhone(), req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        UserVO vo = userService.updatePhone(userId, req.getPhone());
        return Result.success("手机号修改成功", vo);
    }

    @Operation(summary = "修改邮箱（需验证码校验）")
    @PutMapping("/profile/email")
    public Result<UserVO> updateEmail(@Valid @RequestBody EmailUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        // 先校验验证码（target 为新邮箱）
        if (!verificationCodeService.verifyCode(req.getEmail(), req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        UserVO vo = userService.updateEmail(userId, req.getEmail());
        return Result.success("邮箱修改成功", vo);
    }
}
