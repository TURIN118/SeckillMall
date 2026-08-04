package com.seckill.mall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProfileUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProfileUpdateRequest {

    /**
     * 昵称（可选，长度 1-32）
     */
    @Size(min = 1, max = 32, message = "昵称长度1-32")
    private String nickname;

    /**
     * 邮箱（可选，由 Service 层校验格式）
     */
    @Email(message = "邮箱格式不合法")
    private String email;

    /**
     * 手机号（可选，11 位，由 Service 层校验唯一性）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不合法")
    private String phone;

    /**
     * 头像 URL（可选）
     */
    private String avatar;
}