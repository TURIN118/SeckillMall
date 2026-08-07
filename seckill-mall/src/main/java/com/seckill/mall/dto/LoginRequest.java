package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求
 * <p>
 * M-D4 修复：补齐格式/长度校验。
 * <ul>
 *   <li>username：用户名，长度 3-32（与注册接口 {@code RegisterRequest} 的 4-20 对齐，
 *       登录放宽下界以兼容历史 admin 账号）</li>
 *   <li>password：明文密码，长度 6-64，仅允许可打印 ASCII（排除控制字符，防止注入）</li>
 *   <li>captchaKey/captchaCode：可选，但若提供则限制长度，防止超长入参攻击</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：LoginRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    @Pattern(regexp = "^[\\x20-\\x7E]+$", message = "密码仅允许可打印 ASCII 字符")
    private String password;

    // 失败次数 >=3 时强制校验验证码
    @Size(max = 128, message = "captchaKey 长度不能超过 128")
    private String captchaKey;

    @Size(max = 16, message = "captchaCode 长度不能超过 16")
    private String captchaCode;
}
