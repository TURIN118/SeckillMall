package com.seckill.mall.service.impl;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 * <p>
 * 验证码为 6 位随机数字，存储在 Redis，key 为 {@code verify_code:{target}}，
 * 有效期 5 分钟。校验成功后立即删除，避免重复使用。
 * <ul>
 *   <li>邮箱验证码：通过 Spring Mail 真实发送</li>
 *   <li>短信验证码：控制台打印（模拟短信网关）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：VerificationCodeServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    /** Redis key 前缀 */
    private static final String CODE_KEY_PREFIX = "verify_code:";
    /** 验证码有效期（分钟） */
    private static final long CODE_TTL_MINUTES = 5L;
    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;
    /** 发送频率限制 key 前缀 */
    private static final String RATE_KEY_PREFIX = "verify_code:rate:";
    /** 发送频率限制（秒） */
    private static final long RATE_LIMIT_SECONDS = 60L;

    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void sendEmailCode(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        checkRateLimit(email);
        String code = generateCode();
        // 存入 Redis
        storeCode(email, code);
        // 通过 Spring Mail 发送
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("【秒杀商城】验证码");
            message.setText("您的验证码为：" + code + "，有效期5分钟，请勿泄露给他人。");
            mailSender.send(message);
            log.info("邮箱验证码发送成功，email={}, code={}", email, code);
        } catch (Exception e) {
            log.error("邮箱验证码发送失败，email={}", email, e);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_SEND_FAILED);
        }
    }

    @Override
    public void sendSmsCode(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号不能为空");
        }
        checkRateLimit(phone);
        String code = generateCode();
        // 存入 Redis
        storeCode(phone, code);
        // 短信网关模拟：控制台打印
        log.info("【短信验证码】phone={}, code={}（有效期5分钟）", phone, code);
        System.out.println("========================================");
        System.out.println("【短信验证码】手机号：" + phone);
        System.out.println("【短信验证码】验证码：" + code);
        System.out.println("【短信验证码】有效期：5分钟");
        System.out.println("========================================");
    }

    @Override
    public boolean verifyCode(String target, String code) {
        if (target == null || target.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String key = CODE_KEY_PREFIX + target;
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) {
            // 验证码不存在或已过期
            return false;
        }
        if (stored.equals(code)) {
            // 校验成功后立即删除，避免重复使用
            stringRedisTemplate.delete(key);
            log.info("验证码校验成功，target={}", target);
            return true;
        }
        log.warn("验证码校验失败，target={}, input={}", target, code);
        return false;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成 6 位随机数字验证码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 将验证码存入 Redis（5 分钟过期）
     */
    private void storeCode(String target, String code) {
        String key = CODE_KEY_PREFIX + target;
        stringRedisTemplate.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        // 设置发送频率限制
        String rateKey = RATE_KEY_PREFIX + target;
        stringRedisTemplate.opsForValue().set(rateKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 检查发送频率限制（60 秒内只能发送一次）
     */
    private void checkRateLimit(String target) {
        String rateKey = RATE_KEY_PREFIX + target;
        Boolean exists = stringRedisTemplate.hasKey(rateKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_RATE_LIMIT);
        }
    }
}