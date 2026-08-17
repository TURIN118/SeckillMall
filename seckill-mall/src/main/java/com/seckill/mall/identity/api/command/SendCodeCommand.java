package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码命令（找回密码场景）。
 *
 * <p>业务语义：根据 type（PHONE/EMAIL）查询用户是否存在，存在则发送验证码并存入 Redis。
 *
 * <p>原方法：{@code AuthService.sendForgotPasswordCode(ForgotPasswordSendRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeCommand {

    /** 发送方式（PHONE/EMAIL，必填） */
    private String type;

    /** 手机号或邮箱（必填） */
    private String account;
}