package com.seckill.mall.service;

import com.seckill.mall.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CaptchaService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5L;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate stringRedisTemplate;

    public CaptchaVO generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = randomCode();
        BufferedImage image = drawImage(code);
        String base64 = toBase64(image);

        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaId,
                code,
                CAPTCHA_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setCaptchaImage(base64);
        return vo;
    }

    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String stored = stringRedisTemplate.opsForValue().get(key);
        // 一次性：无论校验结果均删除
        stringRedisTemplate.delete(key);
        return stored != null && stored.equalsIgnoreCase(captchaCode);
    }

    private String randomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage drawImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            Random random = new Random();
            // 干扰线
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 6; i++) {
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }
            // 验证码字符
            g.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 24));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
                g.drawString(String.valueOf(code.charAt(i)), 8 + i * 26, 28);
            }
            // 噪点
            for (int i = 0; i < 30; i++) {
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private String toBase64(BufferedImage image) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            byte[] bytes = bos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("验证码图片编码失败", e);
            throw new IllegalStateException("验证码生成失败", e);
        }
    }
}
