package com.seckill.mall.identity.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码结果。
 *
 * <p>由 {@code AuthApi.generateCaptcha} 方法返回，包含验证码 ID 与 Base64 编码的验证码图片。
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult {

    /** 验证码 ID（UUID） */
    private String captchaId;

    /** Base64 编码的验证码图片（data:image/png;base64,...） */
    private String captchaImage;
}