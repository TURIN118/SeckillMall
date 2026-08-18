package com.seckill.mall.identity.application;

import com.seckill.mall.identity.api.AuthApi;
import com.seckill.mall.identity.api.command.LoginCommand;
import com.seckill.mall.identity.api.command.RefreshTokenCommand;
import com.seckill.mall.identity.api.command.RegisterCommand;
import com.seckill.mall.identity.api.command.ResetPasswordCommand;
import com.seckill.mall.identity.api.command.SendCodeCommand;
import com.seckill.mall.identity.api.dto.UserSnapshot;
import com.seckill.mall.identity.api.result.CaptchaResult;
import com.seckill.mall.identity.api.result.LoginResult;
import com.seckill.mall.identity.api.result.TokenResult;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.CaptchaService;
import com.seckill.mall.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Auth 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link AuthApi}，内部委托给旧 {@link AuthService}、{@link CaptchaService} 与
 * {@link VerificationCodeService}，通过 {@link IdentityApiConverter} 做 VO ↔ Result/Snapshot 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * <p>职责划分：
 * <ul>
 *     <li>{@link AuthService}：注册/登录/登出/刷新/找回密码/资料/头像</li>
 *     <li>{@link CaptchaService}：图形验证码生成与校验</li>
 *     <li>{@link VerificationCodeService}：邮箱/短信验证码发送与校验</li>
 * </ul>
 *
 * <p>说明：{@code login} 方法中 {@link LoginCommand} 不携带 {@code HttpServletRequest}，
 * 过渡期传 {@code null}，旧 Service 内部对 null 做兼容处理（User-Agent 从 Command 补全）。
 *
 * @author wnj
 * @since Phase I.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService implements AuthApi {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final VerificationCodeService verificationCodeService;

    @Override
    public UserSnapshot register(RegisterCommand command) {
        return IdentityApiConverter.toSnapshotFromVO(
                authService.register(IdentityApiConverter.toRegisterRequest(command)));
    }

    @Override
    public LoginResult login(LoginCommand command) {
        return IdentityApiConverter.toLoginResult(
                authService.login(
                        IdentityApiConverter.toLoginRequest(command),
                        command.getIp(),
                        null));
    }

    @Override
    public void logout(String accessToken) {
        authService.logout(accessToken);
    }

    @Override
    public TokenResult refreshToken(RefreshTokenCommand command) {
        return IdentityApiConverter.toTokenResult(
                authService.refresh(IdentityApiConverter.toRefreshTokenRequest(command)));
    }

    @Override
    public void sendForgotPasswordCode(SendCodeCommand command) {
        authService.sendForgotPasswordCode(IdentityApiConverter.toForgotPasswordSendRequest(command));
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        authService.resetPassword(IdentityApiConverter.toForgotPasswordResetRequest(command));
    }

    @Override
    public CaptchaResult generateCaptcha() {
        return IdentityApiConverter.toCaptchaResult(captchaService.generateCaptcha());
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        return captchaService.verifyCaptcha(captchaId, captchaCode);
    }

    @Override
    public void sendEmailCode(String email) {
        verificationCodeService.sendEmailCode(email);
    }

    @Override
    public void sendSmsCode(String phone) {
        verificationCodeService.sendSmsCode(phone);
    }

    @Override
    public boolean verifyCode(String target, String code) {
        return verificationCodeService.verifyCode(target, code);
    }
}