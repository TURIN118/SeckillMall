package com.seckill.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLogQueryRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OperationLogQueryRequest {

    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于 1")
    @Max(value = 100, message = "每页大小不能超过 100")
    private Integer pageSize = 10;

    private String module;

    private Long operatorId;
}
