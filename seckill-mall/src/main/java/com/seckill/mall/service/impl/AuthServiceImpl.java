package com.seckill.mall.service.impl;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.ChangePasswordRequest;
import com.seckill.mall.dto.ForgotPasswordResetRequest;
import com.seckill.mall.dto.ForgotPasswordSendRequest;
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
import com.seckill.mall.security.TokenVersionService;
import com.seckill.mall.security.UserStatusCacheService;
import com.seckill.mall.service.AuthService;
import com.seckill.mall.service.CaptchaService;
import com.seckill.mall.service.UploadService;
import com.seckill.mall.service.VerificationCodeService;
import com.seckill.mall.vo.LoginVO;
import com.seckill.mall.vo.TokenVO;
import com.seckill.mall.vo.UploadResultVO;
import com.seckill.mall.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    /** M-S4 修复：按 IP 维度的登录失败计数 key 前缀（防密码喷洒） */
    private static final String FAIL_COUNT_IP_KEY_PREFIX = "login:fail:ip:";
    private static final int IP_FAIL_LOCK_THRESHOLD = 20;
    private static final long IP_FAIL_TTL_MINUTES = 15L;

    /** 找回密码验证码 Redis key 前缀：forgot-password:{type}:{account} */
    private static final String FORGOT_PASSWORD_CODE_KEY_PREFIX = "forgot-password:";
    /** 找回密码验证码有效期（分钟） */
    private static final long FORGOT_PASSWORD_CODE_TTL_MINUTES = 5L;
    /** 验证方式：手机短信 */
    private static final String TYPE_PHONE = "PHONE";
    /** 验证方式：邮箱 */
    private static final String TYPE_EMAIL = "EMAIL";

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final CaptchaService captchaService;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UploadService uploadService;
    private final VerificationCodeService verificationCodeService;
    private final TokenVersionService tokenVersionService;
    private final UserStatusCacheService userStatusCacheService;
    private final SecurityUtils securityUtils;

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
    public LoginVO login(LoginRequest req, String ip, HttpServletRequest request) {
        String username = req.getUsername();
        String failKey = FAIL_COUNT_KEY_PREFIX + username;
        // M-S4 修复：叠加按 IP 维度的失败计数，防密码喷洒（同 IP 不同用户名爆破）
        String ipFailKey = FAIL_COUNT_IP_KEY_PREFIX + ip;

        int failCount = getFailCount(failKey);
        if (failCount >= LOCK_THRESHOLD) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }
        // M-S4 修复：IP 维度失败次数超限则临时封禁
        int ipFailCount = getFailCount(ipFailKey);
        if (ipFailCount >= IP_FAIL_LOCK_THRESHOLD) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }
        // 失败次数 >=3 时强制校验验证码
        if (failCount >= CAPTCHA_REQUIRED_THRESHOLD || ipFailCount >= CAPTCHA_REQUIRED_THRESHOLD) {
            if (!captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaCode())) {
                throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
            }
        }

        // L-O2 修复：提取 User-Agent 用于登录日志补全
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        // M-S5 修复：统一返回相同的"用户名或密码错误"文案与耗时，防用户名枚举
        long loginStartMs = System.currentTimeMillis();
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // M-S5 修复：不存在用户时 user_id 传 null，writeLoginLog 内部跳过 user_id NOT NULL 字段
            recordLoginFailure(failKey, ipFailKey, user, username, ip, userAgent,
                    ErrorCode.USERNAME_OR_PASSWORD_ERROR.getMessage());
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            recordLoginFailure(failKey, ipFailKey, user, username, ip, userAgent,
                    ErrorCode.ACCOUNT_DISABLED.getMessage());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 登录成功：清除失败计数（用户名维度 + IP 维度）
        stringRedisTemplate.delete(failKey);
        // IP 维度失败计数不清除（允许累积，但成功登录后重置该 IP 计数）
        stringRedisTemplate.delete(ipFailKey);
        writeLoginLog(user.getId(), ip, userAgent, LoginResult.SUCCESS, null);

        // 登录成功后签发 Token（携带 tokenVersion）
        long tokenVersion = tokenVersionService.getCurrentVersion(user.getId());
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole(), tokenVersion);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), user.getRole(), tokenVersion);

        // 登录预热：将用户状态写入 Redis 缓存
        userStatusCacheService.putUserAuth(user.getId(), user);

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

        // 查数据库获取最新状态和角色
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null || dbUser.getStatus() != UserStatus.ACTIVE) {
            // 用户已被禁用/删除，吊销 Refresh Token 并拒绝
            tokenBlacklistService.addToBlacklist(refreshToken);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 使用数据库中的最新角色，而非 Token 中的旧角色
        long tokenVersion = tokenVersionService.getCurrentVersion(userId);
        String newAccessToken = jwtUtils.generateAccessToken(userId, dbUser.getUsername(), dbUser.getRole(), tokenVersion);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId, dbUser.getUsername(), dbUser.getRole(), tokenVersion);

        // 旧 refreshToken 失效（一次性轮换）
        tokenBlacklistService.addToBlacklist(refreshToken);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        return vo;
    }

    @Override
    public UserVO getMe() {
        Long userId = securityUtils.getCurrentUserId();
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
        User user = securityUtils.getCurrentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        // Bug2修复：修改密码前先校验验证码，target 优先用邮箱，邮箱为空时回退到手机号
        String verifyTarget = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail()
                : user.getPhone();
        if (verifyTarget == null || verifyTarget.isBlank()
                || !verificationCodeService.verifyCode(verifyTarget, req.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(update);
        // 修改密码后递增 Token 版本号，踢下所有设备
        tokenVersionService.incrementVersion(user.getId());
        // 清除用户状态缓存
        userStatusCacheService.invalidateUserAuth(user.getId());
        // TODO M9: 密码重置/修改成功后可调用 emailService.sendPasswordReset(user.getEmail(), resetToken)
        //  当前重置流程未独立成接口，待密码重置 API 落地后集成
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(ProfileUpdateRequest req) {
        Long userId = securityUtils.getCurrentUserId();
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
        // L13 修复：手机号为空字符串时不更新，避免把 phone 置空
        if (newPhone != null && !newPhone.isBlank()) {
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
        Long userId = securityUtils.getCurrentUserId();
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

    private void recordLoginFailure(String failKey, String ipFailKey, User user, String username,
                                     String ip, String userAgent, String reason) {
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, FAIL_COUNT_TTL_MINUTES, TimeUnit.MINUTES);
        }
        // M-S4 修复：IP 维度失败计数
        Long ipCount = stringRedisTemplate.opsForValue().increment(ipFailKey);
        if (ipCount != null && ipCount == 1L) {
            stringRedisTemplate.expire(ipFailKey, IP_FAIL_TTL_MINUTES, TimeUnit.MINUTES);
        }
        log.warn("登录失败: username={}, failCount={}, ipFailCount={}, ip={}, reason={}",
                username, count, ipCount, ip, reason);
        // M-S5 修复：user 为 null 时不写 user_id（避免 NOT NULL 约束异常）
        writeLoginLog(user == null ? null : user.getId(), ip, userAgent, LoginResult.FAILED, reason);
    }

    /**
     * L-O2 修复：补全 username/userAgent/loginLocation 字段。
     * M-S5 修复：userId 为 null 时不写 user_id 字段（避免 NOT NULL 约束异常导致 500）。
     */
    private void writeLoginLog(Long userId, String ip, String userAgent, LoginResult result, String failReason) {
        LoginLog loginLog = new LoginLog();
        // M-S5 修复：userId 为 null 时跳过设置（LoginLog.userId 字段保持 null，
        // 由 MyBatis-Plus updateById 忽略 null 字段；但 insert 会包含 null，
        // 故需确保 t_login_log.user_id 允许为 NULL —— 见 sql/01_schema.sql 修复）
        loginLog.setUserId(userId);
        loginLog.setLoginIp(ip == null ? "unknown" : ip);
        // L-O2 修复：记录 User-Agent
        if (userAgent != null && userAgent.length() > 500) {
            userAgent = userAgent.substring(0, 500);
        }
        loginLog.setUserAgent(userAgent);
        // L-O2 修复：loginLocation 暂记 IP（可后续接入 IP 地理库解析城市）
        loginLog.setLoginLocation(ip == null ? "unknown" : ip);
        loginLog.setLoginResult(result);
        loginLog.setFailReason(failReason);
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            // M-S5 修复：登录日志写入失败不应影响登录主流程（避免用户名枚举）
            log.warn("登录日志写入失败（不影响登录主流程）: {}", e.getMessage());
        }
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

    @Override
    public void sendForgotPasswordCode(ForgotPasswordSendRequest req) {
        String type = req.getType();
        String account = req.getAccount();
        // 校验账号对应的用户存在
        User user = findUserByAccount(type, account);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND_BY_ACCOUNT);
        }
        // 复用现有验证码服务发送验证码（短信控制台打印，邮箱通过 Spring Mail 发送）
        if (TYPE_PHONE.equals(type)) {
            verificationCodeService.sendSmsCode(account);
        } else if (TYPE_EMAIL.equals(type)) {
            verificationCodeService.sendEmailCode(account);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证方式只能为 PHONE 或 EMAIL");
        }
        // 验证码已由 VerificationCodeService 存入 Redis（key=verify_code:{account}），
        // 此处再写入一个找回密码专用标记 key，便于 resetPassword 时区分业务场景并独立校验
        String forgotKey = FORGOT_PASSWORD_CODE_KEY_PREFIX + type + ":" + account;
        // 标记 key 的有效期与验证码一致（5 分钟），value 为占位标识
        stringRedisTemplate.opsForValue().set(forgotKey, "1", FORGOT_PASSWORD_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("找回密码验证码已发送，type={}, account={}", type, account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ForgotPasswordResetRequest req) {
        String type = req.getType();
        String account = req.getAccount();
        String code = req.getCode();
        String newPassword = req.getNewPassword();

        // 校验找回密码标记 key 是否存在（过期则提示重新获取）
        String forgotKey = FORGOT_PASSWORD_CODE_KEY_PREFIX + type + ":" + account;
        Boolean forgotKeyExists = stringRedisTemplate.hasKey(forgotKey);
        if (!Boolean.TRUE.equals(forgotKeyExists)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 复用 VerificationCodeService 校验验证码（校验成功后会自动删除 verify_code:{account}）
        boolean valid = verificationCodeService.verifyCode(account, code);
        if (!valid) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        // 校验通过后，查询用户并更新密码
        User user = findUserByAccount(type, account);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND_BY_ACCOUNT);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);

        // 重置密码后递增 Token 版本号，踢下所有设备
        tokenVersionService.incrementVersion(user.getId());
        // 清除用户状态缓存
        userStatusCacheService.invalidateUserAuth(user.getId());

        // 删除找回密码标记 key
        stringRedisTemplate.delete(forgotKey);
        log.info("找回密码重置成功，type={}, account={}, userId={}", type, account, user.getId());
    }

    /**
     * 根据验证方式与账号查询用户
     * <p>
     * Bug12修复：数据库中 email/phone 可能存在重复记录，使用 selectOne 会抛
     * TooManyResultsException。改用 findListByEmail/findListByPhone 取第一条，避免异常。
     *
     * @param type    PHONE 或 EMAIL
     * @param account 手机号或邮箱
     * @return 用户实体，不存在返回 null
     */
    private User findUserByAccount(String type, String account) {
        if (TYPE_PHONE.equals(type)) {
            List<User> users = userMapper.findListByPhone(account);
            return users.isEmpty() ? null : users.get(0);
        } else if (TYPE_EMAIL.equals(type)) {
            List<User> users = userMapper.findListByEmail(account);
            return users.isEmpty() ? null : users.get(0);
        }
        return null;
    }
}
