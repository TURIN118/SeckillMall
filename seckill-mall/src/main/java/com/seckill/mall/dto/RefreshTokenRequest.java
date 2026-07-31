package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RefreshTokenRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
