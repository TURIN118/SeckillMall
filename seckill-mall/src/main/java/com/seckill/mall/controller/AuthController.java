package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.ForgotPasswordResetRequest;
import com.seckill.mall.dto.ForgotPasswordSendRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.ProfileUpdateRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.CaptchaService;
import com.seckill.mall.service.UploadService;
import com.seckill.mall.vo.CaptchaVO;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuthController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "用户认证", description = "注册/登录/登出/Token 刷新")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final UploadService uploadService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req));
    }

    @Operation(summary = "用户登录")
    @OperationLog(module = "AUTH", action = "LOGIN", targetType = "USER")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return Result.success(authService.login(req, getClientIp(request)));
    }

    @Operation(summary = "退出登录")
    @OperationLog(module = "AUTH", action = "LOGOUT", targetType = "USER")
    @PostMapping("/logout")
    // 安全修复（L2）：Authorization 头可选，避免缺失时直接 400，由 Service 内部处理无 token 情况
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getMe());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return Result.success();
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    // 安全修复（M1）：添加 @Valid 触发 ProfileUpdateRequest 上的字段校验注解
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest req) {
        return Result.success("个人信息更新成功", authService.updateProfile(req));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success("头像上传成功", authService.uploadAvatar(file));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return Result.success(authService.refresh(req));
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.generateCaptcha());
    }

    @Operation(summary = "找回密码-发送验证码（手机短信或邮箱）")
    @PostMapping("/forgot-password/send-code")
    public Result<Void> sendForgotPasswordCode(@Valid @RequestBody ForgotPasswordSendRequest req) {
        authService.sendForgotPasswordCode(req);
        return Result.success("验证码已发送", null);
    }

    @Operation(summary = "找回密码-校验验证码并重置密码")
    @PostMapping("/forgot-password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ForgotPasswordResetRequest req) {
        authService.resetPassword(req);
        return Result.success("密码重置成功", null);
    }

    /**
     * 获取客户端 IP。
     * <p>
     * 安全修复（M2）：X-Forwarded-For 可被客户端伪造，仅在可信代理环境下才应信任该头。
     * 当前实现假设部署在受信反向代理（Nginx/网关）之后，代理会覆盖或追加真实客户端 IP。
     * <p>
     * 加固建议：
     * 1. 在反向代理层强制覆盖 X-Forwarded-For 为真实客户端 IP，禁止透传客户端伪造值；
     * 2. 或在应用侧维护可信代理网段列表，从右向左取第一个非可信 IP 作为真实客户端 IP。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多层代理时取首个 IP（仅在所有代理均受信时安全）
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
