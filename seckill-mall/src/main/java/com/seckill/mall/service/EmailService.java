package com.seckill.mall.service;

import java.math.BigDecimal;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：EmailService.java
 * 邮箱：nj651217@163.com
 */
public interface EmailService {

    /**
     * 注册验证码邮件。
     */
    void sendRegisterVerify(String toEmail, String verifyCode);

    /**
     * 秒杀成功通知邮件。
     */
    void sendSeckillSuccess(String toEmail, String orderNo, String goodsName, BigDecimal totalAmount);

    /**
     * 支付成功确认邮件。
     */
    void sendPaySuccess(String toEmail, String orderNo, BigDecimal totalAmount, String payTime);

    /**
     * 订单取消通知邮件。
     */
    void sendOrderCancel(String toEmail, String orderNo, String reason);

    /**
     * 密码重置邮件。
     */
    void sendPasswordReset(String toEmail, String resetToken);
}
