package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ChangePasswordRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需为 6-20 个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * Bug2修复：修改密码增加验证码校验。
     * 验证码发送目标为当前用户邮箱（邮箱为空时回退到手机号），
     * 由前端调用 /api/v1/verification/send-email 发送。
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
