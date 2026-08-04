package com.seckill.mall.controller;

import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 验证码 Controller
 * <p>
 * 前缀 {@code /api/v1/verification}，发送与校验接口均可匿名访问
 * （在 {@code SecurityConfig} 白名单中配置）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：VerificationCodeController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "验证码", description = "邮箱/短信验证码发送与校验")
@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationCodeController {

    /** 邮箱格式正则 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    /** 手机号格式正则：11 位数字、首位为 1 */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1\\d{10}$");

    private final VerificationCodeService verificationCodeService;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-email")
    // 安全修复（M11）：限流防暴力破解，60s 内同一 key 仅允许 1 次
    @RateLimit(key = "send-email", capacity = 1, rate = 1, seconds = 60)
    public Result<Void> sendEmail(@RequestBody Map<String, String> body) {
        // 安全修复（H7）：服务端强校验邮箱格式，避免空值/非法字符触发下游异常或被滥用
        String email = body == null ? null : body.get("email");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return Result.error(ErrorCode.PARAM_ERROR);
        }
        verificationCodeService.sendEmailCode(email);
        return Result.<Void>success("发送成功", null);
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-sms")
    // 安全修复（M11）：限流防暴力破解，60s 内同一 key 仅允许 1 次
    @RateLimit(key = "send-sms", capacity = 1, rate = 1, seconds = 60)
    public Result<Void> sendSms(@RequestBody Map<String, String> body) {
        // 安全修复（H7）：服务端强校验手机号格式
        String phone = body == null ? null : body.get("phone");
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            return Result.error(ErrorCode.PARAM_ERROR);
        }
        verificationCodeService.sendSmsCode(phone);
        return Result.<Void>success("发送成功", null);
    }

    @Operation(summary = "校验验证码")
    @PostMapping("/verify")
    // 安全修复（M11）：限流防暴力枚举验证码，60s 内同一 key 仅允许 5 次（capacity=5）
    @RateLimit(key = "verify-code", capacity = 5, rate = 5, seconds = 60)
    public Result<Boolean> verify(@RequestBody Map<String, String> body) {
        String target = body == null ? null : body.get("target");
        String code = body == null ? null : body.get("code");
        if (target == null || target.isEmpty() || code == null || code.isEmpty()) {
            return Result.error(ErrorCode.PARAM_ERROR);
        }
        boolean ok = verificationCodeService.verifyCode(target, code);
        if (!ok) {
            return Result.error(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        return Result.success(true);
    }
}
