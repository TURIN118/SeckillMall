package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 分类属性模板 DTO（创建 / 更新分类规格模板时携带）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeDTO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryAttributeDTO {

    private Long id;

    private Long categoryId;

    @NotBlank(message = "属性名不能为空")
    @Size(max = 50, message = "属性名最大 50 字符")
    private String name;

    /** 属性类型：IMAGE / TEXT */
    private String type;

    /** 录入方式：SELECT / INPUT */
    private String inputType;

    /** 是否必选：0-否 / 1-是 */
    private Integer isRequired;

    private Integer sortOrder;

    /** 预设值列表（inputType=SELECT 时使用） */
    private List<CategoryAttributeValueDTO> values;

    @Data
    public static class CategoryAttributeValueDTO {
        private Long id;
        @NotBlank(message = "预设值不能为空")
        @Size(max = 100, message = "预设值最大 100 字符")
        private String value;
        /** 图片型属性值的色块 URL */
        private String imageUrl;
        private Integer sortOrder;
    }
}