package com.seckill.mall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改邮箱请求（需验证码校验）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：EmailUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class EmailUpdateRequest {

    /** 新邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    private String email;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}