package com.seckill.mall.controller;

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

    private final VerificationCodeService verificationCodeService;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-email")
    public Result<Void> sendEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        verificationCodeService.sendEmailCode(email);
        return Result.<Void>success("发送成功", null);
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-sms")
    public Result<Void> sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        verificationCodeService.sendSmsCode(phone);
        return Result.<Void>success("发送成功", null);
    }

    @Operation(summary = "校验验证码")
    @PostMapping("/verify")
    public Result<Boolean> verify(@RequestBody Map<String, String> body) {
        String target = body.get("target");
        String code = body.get("code");
        boolean ok = verificationCodeService.verifyCode(target, code);
        if (!ok) {
            return Result.error(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        return Result.success(true);
    }
}
