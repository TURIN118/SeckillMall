package com.seckill.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类更新请求
 * <p>
 * M-D4 修复：全字段补齐格式/长度/取值范围校验。
 * 字段语义为"可选更新"（PATCH），故不强制 @NotBlank/@NotNull，
 * 但对非 null 入参约束其取值范围，防止脏数据落库。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryUpdateRequest {

    /** 分类名称，可选；若提供则长度需在 1-32 之间 */
    @Size(min = 1, max = 32, message = "分类名称长度需在 1-32 之间")
    private String categoryName;

    /** 父分类 ID，可选（修改即移动节点）；若提供则必须 >= 0 */
    @PositiveOrZero(message = "父分类 ID 不能为负数")
    private Long parentId;

    /** 排序值，可选；若提供则必须 >= 0 */
    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder;

    /** 状态：1=启用，0=禁用，可选；若提供则必须 >= 0 */
    @Min(value = 0, message = "状态值不能为负数")
    private Integer status;
}
