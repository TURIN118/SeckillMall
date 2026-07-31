package com.seckill.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductQueryRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductQueryRequest {

    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小不能小于 1")
    @Max(value = 100, message = "每页大小不能超过 100")
    private Integer pageSize = 10;

    private Long categoryId;

    @Size(max = 100, message = "关键词最大 100 字符")
    private String keyword;

    @Pattern(regexp = "^(price|sales|createTime)?$",
            message = "排序字段非法")
    private String sortBy;

    @Pattern(regexp = "^(asc|desc|ASC|DESC)?$",
            message = "排序方向仅支持 asc/desc")
    private String sortOrder;
}
