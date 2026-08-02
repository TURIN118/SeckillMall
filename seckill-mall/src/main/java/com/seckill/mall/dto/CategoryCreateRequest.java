package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryCreateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryCreateRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 32, message = "分类名称长度需在 1-32 之间")
    private String categoryName;

    @NotNull(message = "父分类 ID 不能为空")
    private Long parentId;

    /** 排序值，可选，默认 0 */
    private Integer sortOrder;

    /** 状态：1=启用（默认），0=禁用 */
    private Integer status;
}