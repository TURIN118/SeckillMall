package com.seckill.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryStatusUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为 0")
    @Max(value = 1, message = "状态最大值为 1")
    private Integer status;
}