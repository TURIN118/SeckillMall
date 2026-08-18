package com.seckill.mall.identity.interfaces.web;

import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.EmailUpdateRequest;
import com.seckill.mall.dto.PhoneUpdateRequest;
import com.seckill.mall.identity.api.AuthApi;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.identity.api.command.UpdateEmailCommand;
import com.seckill.mall.identity.api.command.UpdatePhoneCommand;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.security.SecurityUtils;
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
 * 用户个人信息 Controller（Phase I.4-C 已切换到 {@link UserApi}/{@link AuthApi}）。
 *
 * <p>前缀 {@code /api/v1/users}，需登录（BUYER/ADMIN）。
 * 提供修改手机号/邮箱接口，均需先校验验证码。
 *
 * <p>Strangler Pattern：注入 {@link UserApi} + {@link AuthApi} 替代旧
 * {@code UserService}/{@code VerificationCodeService}，通过 {@link IdentityApiConverter}
 * 做旧 Request → Command、Snapshot → VO 转换，保持前端入参/出参结构不变。
 *
 * <p>创建人：@author WNJ
 */
@Tag(name = "用户个人信息", description = "修改手机号/邮箱（需验证码校验）")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class UserController {

    private final UserApi userApi;
    private final AuthApi authApi;
    private final SecurityUtils securityUtils;

    @Operation(summary = "修改手机号（需验证码校验）")
    @PutMapping("/profile/phone")
    public Result<UserVO> updatePhone(@Valid @RequestBody PhoneUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        // 先校验验证码（target 为新手机号）
        if (!authApi.verifyCode(req.getPhone(), req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        UpdatePhoneCommand command = UpdatePhoneCommand.builder()
                .userId(userId)
                .phone(req.getPhone())
                .build();
        return Result.success("手机号修改成功",
                IdentityApiConverter.toUserVO(userApi.updateUserPhone(command)));
    }

    @Operation(summary = "修改邮箱（需验证码校验）")
    @PutMapping("/profile/email")
    public Result<UserVO> updateEmail(@Valid @RequestBody EmailUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        // 先校验验证码（target 为新邮箱）
        if (!authApi.verifyCode(req.getEmail(), req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        UpdateEmailCommand command = UpdateEmailCommand.builder()
                .userId(userId)
                .email(req.getEmail())
                .build();
        return Result.success("邮箱修改成功",
                IdentityApiConverter.toUserVO(userApi.updateUserEmail(command)));
    }
}
