package com.seckill.mall.identity.api;

import com.seckill.mall.identity.api.command.LoginCommand;
import com.seckill.mall.identity.api.command.RefreshTokenCommand;
import com.seckill.mall.identity.api.command.RegisterCommand;
import com.seckill.mall.identity.api.command.ResetPasswordCommand;
import com.seckill.mall.identity.api.command.SendCodeCommand;
import com.seckill.mall.identity.api.dto.UserSnapshot;
import com.seckill.mall.identity.api.result.CaptchaResult;
import com.seckill.mall.identity.api.result.LoginResult;
import com.seckill.mall.identity.api.result.TokenResult;

/**
 * Identity 模块认证能力 API。
 *
 * <p>对外暴露注册/登录/登出/刷新令牌/找回密码 + 验证码（邮箱/短信/图形）等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * @author wnj
 * @since Phase I.2
 */
public interface AuthApi {

    /**
     * 用户注册。
     *
     * @param command 注册命令
     * @return 注册后的用户信息
     * @throws com.seckill.mall.exception.BusinessException {@code USERNAME_ALREADY_EXISTS}、{@code PHONE_ALREADY_EXISTS}、{@code CAPTCHA_INVALID}、{@code PARAM_ERROR}
     */
    UserSnapshot register(RegisterCommand command);

    /**
     * 用户登录。
     *
     * @param command 登录命令
     * @return 登录结果
     * @throws com.seckill.mall.exception.BusinessException {@code USERNAME_OR_PASSWORD_ERROR}、{@code CAPTCHA_INVALID}、{@code USER_DISABLED}、{@code USER_LOCKED}、{@code PARAM_ERROR}
     */
    LoginResult login(LoginCommand command);

    /**
     * 退出登录（将 access token 加入黑名单）。
     *
     * @param accessToken Authorization 头
     */
    void logout(String accessToken);

    /**
     * 刷新令牌。
     *
     * @param command 刷新令牌命令
     * @return 令牌结果
     * @throws com.seckill.mall.exception.BusinessException {@code REFRESH_TOKEN_INVALID}、{@code REFRESH_TOKEN_EXPIRED}、{@code USER_DISABLED}
     */
    TokenResult refreshToken(RefreshTokenCommand command);

    /**
     * 找回密码发送验证码。
     *
     * @param command 发送验证码命令
     * @throws com.seckill.mall.exception.BusinessException {@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void sendForgotPasswordCode(SendCodeCommand command);

    /**
     * 找回密码重置。
     *
     * @param command 重置密码命令
     * @throws com.seckill.mall.exception.BusinessException {@code VERIFICATION_CODE_INVALID}、{@code USER_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void resetPassword(ResetPasswordCommand command);

    /**
     * 生成图形验证码。
     *
     * @return 图形验证码结果
     */
    CaptchaResult generateCaptcha();

    /**
     * 校验图形验证码（一次性，校验后无论结果均删除）。
     *
     * @param captchaId   验证码 ID
     * @param captchaCode 用户输入的验证码
     * @return {@code true} 校验成功
     */
    boolean verifyCaptcha(String captchaId, String captchaCode);

    /**
     * 发送邮箱验证码。
     *
     * @param email 目标邮箱
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}
     */
    void sendEmailCode(String email);

    /**
     * 发送短信验证码。
     *
     * @param phone 目标手机号
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}
     */
    void sendSmsCode(String phone);

    /**
     * 校验验证码（从 Redis 获取并比对）。
     *
     * @param target 验证码目标（邮箱或手机号）
     * @param code   用户输入的验证码
     * @return {@code true} 校验成功
     */
    boolean verifyCode(String target, String code);
}