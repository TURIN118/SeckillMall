package com.seckill.mall.service;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.entity.LoginLog;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
import com.seckill.mall.mapper.LoginLogMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.security.JwtUtils;
import com.seckill.mall.security.TokenBlacklistService;
import com.seckill.mall.service.impl.AuthServiceImpl;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.UserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuthServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("pass123");
        req.setPhone("13800001111");
        req.setCaptchaKey("cap-key");
        req.setCaptchaCode("ABCD");
        return req;
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("buyer01");
        user.setPassword("$2a$10$encoded");
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    @DisplayName("register：用户名已存在时抛 USERNAME_EXISTS")
    void register_shouldThrowWhenUsernameExists() {
        // given
        RegisterRequest req = buildRegisterRequest();
        given(captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaCode())).willReturn(true);
        given(userMapper.findByUsername(req.getUsername())).willReturn(buildUser());

        // when / then
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_EXISTS);
        then(userMapper).should(never()).insert(any(User.class));
    }

    @Test
    @DisplayName("register：手机号已存在时抛 PHONE_EXISTS")
    void register_shouldThrowWhenPhoneExists() {
        // given
        RegisterRequest req = buildRegisterRequest();
        given(captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaCode())).willReturn(true);
        given(userMapper.findByUsername(req.getUsername())).willReturn(null);
        given(userMapper.findByPhone(req.getPhone())).willReturn(buildUser());

        // when / then
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PHONE_EXISTS);
    }

    @Test
    @DisplayName("register：验证码错误时抛 CAPTCHA_ERROR")
    void register_shouldThrowWhenCaptchaInvalid() {
        // given
        RegisterRequest req = buildRegisterRequest();
        given(captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaCode())).willReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CAPTCHA_ERROR);
    }

    @Test
    @DisplayName("login：成功签发 AccessToken 与 RefreshToken")
    void login_shouldIssueTokensOnSuccess() {
        // given
        User user = buildUser();
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("pass123");

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("login:fail:" + user.getUsername())).willReturn(null);
        given(userMapper.findByUsername(user.getUsername())).willReturn(user);
        given(passwordEncoder.matches(req.getPassword(), user.getPassword())).willReturn(true);
        given(jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole())).willReturn("access-token");
        given(jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), user.getRole())).willReturn("refresh-token");

        // when
        LoginVO vo = authService.login(req, "127.0.0.1");

        // then
        assertThat(vo.getAccessToken()).isEqualTo("access-token");
        assertThat(vo.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(vo.getUser()).extracting(UserVO::getUsername).isEqualTo(user.getUsername());
        // 登录成功应清除失败计数并写入成功日志
        then(stringRedisTemplate).should().delete("login:fail:" + user.getUsername());
        then(loginLogMapper).should().insert(any(LoginLog.class));
    }

    @Test
    @DisplayName("login：密码错误时抛异常并累加失败计数")
    void login_shouldRecordFailureOnWrongPassword() {
        // given
        User user = buildUser();
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("wrong");

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("login:fail:" + user.getUsername())).willReturn("0");
        given(userMapper.findByUsername(user.getUsername())).willReturn(user);
        given(passwordEncoder.matches(req.getPassword(), user.getPassword())).willReturn(false);
        given(valueOperations.increment("login:fail:" + user.getUsername())).willReturn(1L);

        // when / then
        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        // 失败计数首次累加应设置 TTL
        then(stringRedisTemplate).should(times(1)).expire(eq("login:fail:" + user.getUsername()), anyLong(), any());
    }

    @Test
    @DisplayName("login：失败次数达到锁定阈值时抛 LOGIN_LOCKED")
    void login_shouldThrowWhenLocked() {
        // given
        LoginRequest req = new LoginRequest();
        req.setUsername("locked-user");
        req.setPassword("any");

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("login:fail:locked-user")).willReturn("5");

        // when / then
        assertThatThrownBy(() -> authService.login(req, "1.1.1.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_LOCKED);
        then(userMapper).should(never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("login：账号被禁用时抛 ACCOUNT_DISABLED")
    void login_shouldThrowWhenAccountDisabled() {
        // given
        User user = buildUser();
        user.setStatus(UserStatus.DISABLED);
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("pass123");

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("login:fail:" + user.getUsername())).willReturn("0");
        given(userMapper.findByUsername(user.getUsername())).willReturn(user);
        given(passwordEncoder.matches(req.getPassword(), user.getPassword())).willReturn(true);
        given(valueOperations.increment("login:fail:" + user.getUsername())).willReturn(1L);

        // when / then
        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    @DisplayName("logout：将 accessToken 加入黑名单（去除 Bearer 前缀）")
    void logout_shouldAddTokenToBlacklist() {
        // given
        String bearer = "Bearer abc.def.ghi";

        // when
        authService.logout(bearer);

        // then
        then(tokenBlacklistService).should().addToBlacklist("abc.def.ghi");
    }

    @Test
    @DisplayName("logout：空 token 不触发黑名单写入")
    void logout_shouldIgnoreBlankToken() {
        // when
        authService.logout("   ");

        // then
        then(tokenBlacklistService).should(never()).addToBlacklist(anyString());
    }
}
