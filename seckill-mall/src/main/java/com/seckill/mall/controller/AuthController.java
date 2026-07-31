package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.CaptchaService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuthController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return Result.success(authService.login(req, getClientIp(request)));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多层代理时取首个 IP
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
