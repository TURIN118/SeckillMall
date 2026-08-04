package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 找回密码-发送验证码请求
 * <p>
 * 支持手机短信与邮箱两种验证方式，通过 type 字段区分：
 * <ul>
 *   <li>PHONE：account 为手机号，发送短信验证码</li>
 *   <li>EMAIL：account 为邮箱，发送邮箱验证码</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ForgotPasswordSendRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ForgotPasswordSendRequest {

    /** 验证方式：PHONE 或 EMAIL */
    @NotBlank(message = "验证方式不能为空")
    @Pattern(regexp = "^(PHONE|EMAIL)$", message = "验证方式只能为 PHONE 或 EMAIL")
    private String type;

    /** 手机号或邮箱 */
    @NotBlank(message = "账号不能为空")
    private String account;
}