package com.seckill.mall.service.impl;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.dto.ProfileUpdateRequest;
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
import com.seckill.mall.service.UploadService;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UploadResultVO;
import com.seckill.mall.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
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
    private final UploadService uploadService;

    /** 头像文件最大大小：2MB */
    private static final long AVATAR_MAX_SIZE = 2L * 1024 * 1024;

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
        // TODO M9: 注册成功后可调用 emailService.sendRegisterVerify(user.getEmail(), verifyCode)
        //  当前注册流程未启用邮箱验证码，待邮箱字段接入注册接口后集成
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
        // TODO M9: 密码重置/修改成功后可调用 emailService.sendPasswordReset(user.getEmail(), resetToken)
        //  当前重置流程未独立成接口，待密码重置 API 落地后集成
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(ProfileUpdateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 手机号唯一性校验：仅当传入新手机号且与当前手机号不同时校验
        String newPhone = req.getPhone();
        if (newPhone != null && !newPhone.isBlank() && !newPhone.equals(user.getPhone())) {
            User existing = userMapper.findByPhone(newPhone);
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }
        // MyBatis-Plus updateById 只更新非 null 字段
        User update = new User();
        update.setId(userId);
        if (req.getNickname() != null) {
            update.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            update.setEmail(req.getEmail());
        }
        if (newPhone != null) {
            update.setPhone(newPhone);
        }
        if (req.getAvatar() != null) {
            update.setAvatarUrl(req.getAvatar());
        }
        userMapper.updateById(update);
        // 返回最新用户信息
        User latest = userMapper.selectById(userId);
        return toUserVO(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        // 头像专用限制：2MB（通用上传为 5MB，故在此前置校验）
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        // 复用通用上传服务（类型校验由 UploadService 完成）
        UploadResultVO result = uploadService.uploadImage(file, "avatar", null);
        String url = result.getUrl();
        // 持久化头像 URL 到当前用户
        Long userId = SecurityUtils.getCurrentUserId();
        User update = new User();
        update.setId(userId);
        update.setAvatarUrl(url);
        userMapper.updateById(update);
        return Map.of("avatar", url);
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
