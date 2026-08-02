package com.seckill.mall.service;

/**
 * 验证码服务接口
 * <p>
 * 提供邮箱验证码、短信验证码的发送与校验能力。
 * 验证码统一存储在 Redis，key 为 {@code verify_code:{target}}，有效期 5 分钟。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：VerificationCodeService.java
 * 邮箱：nj651217@163.com
 */
public interface VerificationCodeService {

    /**
     * 发送邮箱验证码（通过 Spring Mail 真实发送）
     *
     * @param email 目标邮箱
     */
    void sendEmailCode(String email);

    /**
     * 发送短信验证码（控制台打印 + Redis 存储）
     *
     * @param phone 目标手机号
     */
    void sendSmsCode(String phone);

    /**
     * 校验验证码
     *
     * @param target 验证码目标（邮箱或手机号）
     * @param code   用户输入的验证码
     * @return 校验成功返回 true，否则 false
     */
    boolean verifyCode(String target, String code);
}