package com.seckill.mall.service.impl;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.RefreshTokenRequest;
import com.seckill.mall.dto.RegisterRequest;
import com.seckill.mall.entity.LoginLog;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.LoginResult;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
import com.seckill.mall.mapper.LoginLogMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.security.JwtUtils;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.security.TokenBlacklistService;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.CaptchaService;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuthServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String FAIL_COUNT_KEY_PREFIX = "login:fail:";
    private static final long FAIL_COUNT_TTL_MINUTES = 30L;
    private static final int LOCK_THRESHOLD = 5;
    private static final int CAPTCHA_REQUIRED_THRESHOLD = 3;

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final CaptchaService captchaService;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest req) {
        if (!captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
        if (userMapper.findByUsername(req.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (userMapper.findByPhone(req.getPhone()) != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setNickname(req.getUsername());
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);
        return toUserVO(user);
    }

    @Override
    public LoginVO login(LoginRequest req, String ip) {
        String username = req.getUsername();
        String failKey = FAIL_COUNT_KEY_PREFIX + username;

        int failCount = getFailCount(failKey);
        if (failCount >= LOCK_THRESHOLD) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }
        // 失败次数 >=3 时强制校验验证码
        if (failCount >= CAPTCHA_REQUIRED_THRESHOLD) {
            if (!captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaCode())) {
                throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
            }
        }

        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordLoginFailure(failKey, user, username, ip, ErrorCode.USERNAME_OR_PASSWORD_ERROR.getMessage());
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            recordLoginFailure(failKey, user, username, ip, ErrorCode.ACCOUNT_DISABLED.getMessage());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 登录成功：清除失败计数
        stringRedisTemplate.delete(failKey);
        writeLoginLog(user.getId(), ip, LoginResult.SUCCESS, null);

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            tokenBlacklistService.addToBlacklist(accessToken);
        }
    }

    @Override
    public TokenVO refresh(RefreshTokenRequest req) {
        String refreshToken = req.getRefreshToken();
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!JwtUtils.TOKEN_TYPE_REFRESH.equals(jwtUtils.getTokenTypeFromToken(refreshToken))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);
        String roleCode = jwtUtils.getRoleFromToken(refreshToken);
        UserRole role = UserRole.fromCode(roleCode);

        // 旧 refreshToken 失效（一次性轮换）
        tokenBlacklistService.addToBlacklist(refreshToken);

        String newAccessToken = jwtUtils.generateAccessToken(userId, username, role);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId, username, role);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        return vo;
    }

    @Override
    public UserVO getMe() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        User user = SecurityUtils.getCurrentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(update);
    }

    private int getFailCount(String failKey) {
        String value = stringRedisTemplate.opsForValue().get(failKey);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void recordLoginFailure(String failKey, User user, String username, String ip, String reason) {
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, FAIL_COUNT_TTL_MINUTES, TimeUnit.MINUTES);
        }
        log.warn("登录失败: username={}, failCount={}, reason={}", username, count, reason);
        writeLoginLog(user == null ? null : user.getId(), ip, LoginResult.FAILED, reason);
    }

    private void writeLoginLog(Long userId, String ip, LoginResult result, String failReason) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setLoginIp(ip == null ? "unknown" : ip);
        loginLog.setLoginResult(result);
        loginLog.setFailReason(failReason);
        loginLogMapper.insert(loginLog);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatarUrl());
        vo.setRole(user.getRole() == null ? null : user.getRole().getCode());
        vo.setStatus(user.getStatus() == null ? null : user.getStatus().getCode());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
