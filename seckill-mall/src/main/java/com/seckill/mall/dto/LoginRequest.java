package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：LoginRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    // 失败次数 >=3 时强制校验验证码
    private String captchaKey;

    private String captchaCode;
}
