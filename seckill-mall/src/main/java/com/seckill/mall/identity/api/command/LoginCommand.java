package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录命令。
 *
 * <p>业务语义：校验用户名密码 → 生成 JWT → 记录登录日志 → 返回 token + 用户信息。
 *
 * <p>原方法：{@code AuthService.login(LoginRequest, String, HttpServletRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCommand {

    /** 用户名（必填） */
    private String username;

    /** 密码（明文，必填） */
    private String password;

    /** 图形验证码 ID（必填） */
    private String captchaId;

    /** 图形验证码（必填） */
    private String captchaCode;

    /** 客户端 IP（必填，密码喷洒防护） */
    private String ip;

    /** User-Agent（日志补全） */
    private String userAgent;
}