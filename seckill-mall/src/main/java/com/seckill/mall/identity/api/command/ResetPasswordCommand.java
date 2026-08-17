package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重置密码命令（找回密码场景）。
 *
 * <p>业务语义：校验验证码 → 查询用户 → BCrypt 加密新密码 → 更新 → 删除 Redis 验证码。
 *
 * <p>原方法：{@code AuthService.resetPassword(ForgotPasswordResetRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordCommand {

    /** 发送方式（PHONE/EMAIL，必填） */
    private String type;

    /** 手机号或邮箱（必填） */
    private String account;

    /** 验证码（必填） */
    private String code;

    /** 新密码（明文，必填） */
    private String newPassword;
}