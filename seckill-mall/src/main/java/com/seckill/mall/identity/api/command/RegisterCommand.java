package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册命令。
 *
 * <p>业务语义：校验用户名/手机号唯一 → BCrypt 加密密码 → 保存用户 → 返回用户信息。
 *
 * <p>原方法：{@code AuthService.register(RegisterRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommand {

    /** 用户名（唯一，必填） */
    private String username;

    /** 密码（明文，服务端 BCrypt 加密，必填） */
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 昵称（默认同 username） */
    private String nickname;

    /** 图形验证码 ID（必填） */
    private String captchaId;

    /** 图形验证码（必填） */
    private String captchaCode;
}