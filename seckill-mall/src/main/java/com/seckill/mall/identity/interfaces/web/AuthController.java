package com.seckill.mall.identity.interfaces.web;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.ForgotPasswordResetRequest;
import com.seckill.mall.dto.ForgotPasswordSendRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.ProfileUpdateRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.identity.api.AuthApi;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.identity.api.command.ChangePasswordCommand;
import com.seckill.mall.identity.api.command.LoginCommand;
import com.seckill.mall.identity.api.command.RefreshTokenCommand;
import com.seckill.mall.identity.api.command.RegisterCommand;
import com.seckill.mall.identity.api.command.ResetPasswordCommand;
import com.seckill.mall.identity.api.command.SendCodeCommand;
import com.seckill.mall.identity.api.command.UpdateProfileCommand;
import com.seckill.mall.identity.api.command.UploadAvatarCommand;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.utils.IpUtils;
import com.seckill.mall.vo.CaptchaVO;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户认证 Controller（Phase I.4-C 已切换到 {@link AuthApi}/{@link UserApi}）。
 *
 * <p>Strangler Pattern 关键步骤：注入 {@link AuthApi} + {@link UserApi} 替代旧
 * {@code AuthService}/{@code CaptchaService}，通过 {@link IdentityApiConverter}
 * 做旧 Request → Command、Result → VO 转换，保持前端入参/出参结构不变。
 *
 * <p>职责划分：
 * <ul>
 *     <li>{@link AuthApi}：注册/登录/登出/刷新/找回密码/验证码</li>
 *     <li>{@link UserApi}：当前用户查询/资料更新/密码修改/头像上传</li>
 * </ul>
 *
 * <p>创建人：@author WNJ
 */
@Tag(name = "用户认证", description = "注册/登录/登出/Token 刷新")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApi authApi;
    private final UserApi userApi;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest req) {
        RegisterCommand command = IdentityApiConverter.toRegisterCommand(req);
        return Result.success(IdentityApiConverter.toUserVO(authApi.register(command)));
    }

    @Operation(summary = "用户登录")
    @OperationLog(module = "AUTH", action = "LOGIN", targetType = "USER")
    @RateLimit(key = "login", capacity = 10, rate = 10, seconds = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        LoginCommand command = IdentityApiConverter.toLoginCommand(
                req, IpUtils.getClientIp(request), request.getHeader("User-Agent"));
        return Result.success(IdentityApiConverter.toLoginVO(authApi.login(command)));
    }

    @Operation(summary = "退出登录")
    @OperationLog(module = "AUTH", action = "LOGOUT", targetType = "USER")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authApi.logout(authorization);
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(IdentityApiConverter.toUserVO(userApi.getCurrentUser()));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        ChangePasswordCommand command = IdentityApiConverter.toChangePasswordCommand(req);
        userApi.changePassword(command);
        return Result.success();
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest req) {
        UpdateProfileCommand command = IdentityApiConverter.toUpdateProfileCommand(req);
        return Result.success("个人信息更新成功", IdentityApiConverter.toUserVO(userApi.updateProfile(command)));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        UploadAvatarCommand command = UploadAvatarCommand.builder().file(file).build();
        return Result.success("头像上传成功", userApi.uploadAvatar(command));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        RefreshTokenCommand command = IdentityApiConverter.toRefreshTokenCommand(req);
        return Result.success(IdentityApiConverter.toTokenVO(authApi.refreshToken(command)));
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(IdentityApiConverter.toCaptchaVO(authApi.generateCaptcha()));
    }

    @Operation(summary = "找回密码-发送验证码（手机短信或邮箱）")
    @PostMapping("/forgot-password/send-code")
    public Result<Void> sendForgotPasswordCode(@Valid @RequestBody ForgotPasswordSendRequest req) {
        SendCodeCommand command = IdentityApiConverter.toSendCodeCommand(req);
        authApi.sendForgotPasswordCode(command);
        return Result.success("验证码已发送", null);
    }

    @Operation(summary = "找回密码-校验验证码并重置密码")
    @PostMapping("/forgot-password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ForgotPasswordResetRequest req) {
        ResetPasswordCommand command = IdentityApiConverter.toResetPasswordCommand(req);
        authApi.resetPassword(command);
        return Result.success("密码重置成功", null);
    }

}
