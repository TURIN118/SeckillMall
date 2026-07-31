package com.seckill.mall.service.impl;

import com.seckill.mall.service.EmailService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：EmailServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String REGISTER_VERIFY_TPL = "email/register-verify";
    private static final String SECKILL_SUCCESS_TPL = "email/seckill-success";
    private static final String PAY_SUCCESS_TPL = "email/pay-success";
    private static final String ORDER_CANCEL_TPL = "email/order-cancel";
    private static final String PASSWORD_RESET_TPL = "email/password-reset";

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${spring.application.name:seckill-mall}")
    private String appName;

    @Override
    @Async("emailExecutor")
    @Retryable(value = MailException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 4000))
    public void sendRegisterVerify(String toEmail, String verifyCode) {
        Map<String, Object> variables = new HashMap<>(4);
        variables.put("verifyCode", verifyCode);
        variables.put("validMinutes", 5);
        variables.put("appName", appName);
        String html = renderTemplate(REGISTER_VERIFY_TPL, variables);
        sendHtml(toEmail, "【秒杀商城】注册验证码", html);
        log.info("注册验证码邮件发送成功 to={}", toEmail);
    }

    @Override
    @Async("emailExecutor")
    @Retryable(value = MailException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 4000))
    public void sendSeckillSuccess(String toEmail, String orderNo, String goodsName, BigDecimal totalAmount) {
        Map<String, Object> variables = new HashMap<>(8);
        variables.put("orderNo", orderNo);
        variables.put("goodsName", goodsName);
        variables.put("totalAmount", totalAmount == null ? "" : totalAmount.toPlainString());
        variables.put("payTimeoutMinutes", 15);
        variables.put("appName", appName);
        String html = renderTemplate(SECKILL_SUCCESS_TPL, variables);
        sendHtml(toEmail, "【秒杀商城】秒杀成功，请尽快支付", html);
        log.info("秒杀成功邮件发送成功 to={} orderNo={}", toEmail, orderNo);
    }

    @Override
    @Async("emailExecutor")
    @Retryable(value = MailException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 4000))
    public void sendPaySuccess(String toEmail, String orderNo, BigDecimal totalAmount, String payTime) {
        Map<String, Object> variables = new HashMap<>(8);
        variables.put("orderNo", orderNo);
        variables.put("totalAmount", totalAmount == null ? "" : totalAmount.toPlainString());
        variables.put("payTime", payTime == null ? "" : payTime);
        variables.put("appName", appName);
        String html = renderTemplate(PAY_SUCCESS_TPL, variables);
        sendHtml(toEmail, "【秒杀商城】支付成功", html);
        log.info("支付成功邮件发送成功 to={} orderNo={}", toEmail, orderNo);
    }

    @Override
    @Async("emailExecutor")
    @Retryable(value = MailException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 4000))
    public void sendOrderCancel(String toEmail, String orderNo, String reason) {
        Map<String, Object> variables = new HashMap<>(8);
        variables.put("orderNo", orderNo);
        variables.put("reason", reason == null ? "" : reason);
        variables.put("appName", appName);
        String html = renderTemplate(ORDER_CANCEL_TPL, variables);
        sendHtml(toEmail, "【秒杀商城】订单取消通知", html);
        log.info("订单取消邮件发送成功 to={} orderNo={} reason={}", toEmail, orderNo, reason);
    }

    @Override
    @Async("emailExecutor")
    @Retryable(value = MailException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 4000))
    public void sendPasswordReset(String toEmail, String resetToken) {
        Map<String, Object> variables = new HashMap<>(8);
        variables.put("resetToken", resetToken);
        variables.put("validMinutes", 30);
        variables.put("appName", appName);
        String html = renderTemplate(PASSWORD_RESET_TPL, variables);
        sendHtml(toEmail, "【秒杀商城】密码重置", html);
        log.info("密码重置邮件发送成功 to={}", toEmail);
    }

    /**
     * 重试耗尽兜底：邮件发送失败不影响主业务流程，仅记录日志。
     * 仅以异常入参，兼容所有 @Retryable 方法的不同形参列表。
     */
    @Recover
    public void recover(MailException e) {
        log.error("邮件发送最终失败（重试耗尽），忽略并继续主流程", e);
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(template, context);
    }

    private void sendHtml(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, appName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("构建邮件失败 to={} subject={}", toEmail, subject, e);
            throw new org.springframework.mail.MailSendException("构建邮件失败", e);
        }
    }
}
