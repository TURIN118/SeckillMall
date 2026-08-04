package com.seckill.mall.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserStatusUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class UserStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "状态非法")
    private String status;
}
