package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改手机号请求（需验证码校验）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：PhoneUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class PhoneUpdateRequest {

    /** 新手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}