package com.seckill.mall.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserRoleUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UserRoleUpdateRequest {

    @NotNull(message = "角色不能为空")
    @Pattern(regexp = "^(BUYER|SELLER|ADMIN)$", message = "角色非法")
    private String role;
}
