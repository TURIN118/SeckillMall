package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码-重置密码请求
 * <p>
 * 校验验证码通过后，使用 newPassword 重置对应用户的密码（BCrypt 加密存储）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ForgotPasswordResetRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ForgotPasswordResetRequest {

    /** 验证方式：PHONE 或 EMAIL */
    @NotBlank(message = "验证方式不能为空")
    @Pattern(regexp = "^(PHONE|EMAIL)$", message = "验证方式只能为 PHONE 或 EMAIL")
    private String type;

    /** 手机号或邮箱 */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需为 6-20 个字符")
    private String newPassword;
}