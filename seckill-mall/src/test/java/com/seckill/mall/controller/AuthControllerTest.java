package com.seckill.mall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.GlobalExceptionHandler;
import com.seckill.mall.config.UploadProperties;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.identity.infrastructure.mapper.UserMapper;
import com.seckill.mall.identity.interfaces.web.AuthController;
import com.seckill.mall.security.JwtUtils;
import com.seckill.mall.security.TokenBlacklistService;
import com.seckill.mall.security.TokenVersionService;
import com.seckill.mall.security.UserStatusCacheService;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.CaptchaService;
import com.seckill.mall.service.UploadService;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.UserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuthControllerTest.java
 * 邮箱：nj651217@163.com
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
// 问题2修复：WebMvcConfig 依赖 UploadProperties，@WebMvcTest 不会自动加载
// @ConfigurationProperties bean，需显式 Import 并提供测试属性，避免上下文初始化失败。
// 同时提供 sign-secret 以通过 ReplayProtectionFilter 的 @PostConstruct 启动校验。
@Import({UploadProperties.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "upload.base-dir=/tmp/test-upload",
        "upload.base-url=/uploads",
        "seckill.security.sign-secret=test-sign-secret-with-at-least-32-chars-length"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private CaptchaService captchaService;
    // AuthController 还依赖 UploadService，需 mock 以完成构造器注入
    @MockBean
    private UploadService uploadService;
    // 安全 Filter 依赖：便于 OncePerRequestFilter 子类装配，addFilters=false 时不参与请求链
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private RedisService redisService;
    // JwtAuthenticationFilter 依赖以下 bean，需 mock 以完成 Filter 装配
    @MockBean
    private UserMapper userMapper;
    @MockBean
    private UserStatusCacheService userStatusCacheService;
    @MockBean
    private TokenVersionService tokenVersionService;

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("tester1");
        req.setPassword("pass123");
        req.setPhone("13800001234");
        req.setCaptchaKey("cap-key");
        req.setCaptchaCode("ABCD");
        return req;
    }

    @Test
    @DisplayName("register：用户名缺失时参数校验失败返回 PARAM_ERROR")
    void register_shouldFailValidationWhenUsernameBlank() throws Exception {
        // given
        RegisterRequest req = validRegisterRequest();
        req.setUsername("");

        // when / then
        // M-S6 修复：参数校验失败现返回 400 而非 200
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("register：手机号格式非法时参数校验失败")
    void register_shouldFailValidationWhenPhoneInvalid() throws Exception {
        // given
        RegisterRequest req = validRegisterRequest();
        req.setPhone("123");

        // when / then
        // M-S6 修复：参数校验失败现返回 400 而非 200
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("register：合法请求返回成功")
    void register_shouldReturnSuccess() throws Exception {
        // given
        RegisterRequest req = validRegisterRequest();
        UserVO vo = new UserVO();
        vo.setId(1L);
        vo.setUsername(req.getUsername());
        given(authService.register(any(RegisterRequest.class))).willReturn(vo);

        // when / then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.username").value(req.getUsername()));
    }

    @Test
    @DisplayName("login：用户名缺失时校验失败")
    void login_shouldFailValidationWhenUsernameBlank() throws Exception {
        // given
        LoginRequest req = new LoginRequest();
        req.setUsername("");
        req.setPassword("pass123");

        // when / then
        // M-S6 修复：参数校验失败现返回 400 而非 200
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("login：成功签发 Token")
    void login_shouldReturnTokensOnSuccess() throws Exception {
        // given
        LoginRequest req = new LoginRequest();
        req.setUsername("buyer01");
        req.setPassword("buyer123");
        LoginVO vo = new LoginVO();
        vo.setAccessToken("access-token");
        vo.setRefreshToken("refresh-token");
        given(authService.login(any(LoginRequest.class), anyString(), any())).willReturn(vo);

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("login：业务异常（密码错误）返回错误码")
    void login_shouldReturnErrorOnBusinessException() throws Exception {
        // given
        LoginRequest req = new LoginRequest();
        req.setUsername("buyer01");
        // 密码需满足 @Size(min=6) 校验，否则校验失败返回 400 而非业务异常 403
        req.setPassword("wrongpwd");
        given(authService.login(any(LoginRequest.class), anyString(), any()))
                .willThrow(new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR));

        // when / then
        // M-S6 修复：USERNAME_OR_PASSWORD_ERROR 现返回 403 而非 200
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode()));
    }
}
