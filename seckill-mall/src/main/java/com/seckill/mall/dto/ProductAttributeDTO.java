package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商品属性 DTO（创建 / 更新商品时携带）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttributeDTO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductAttributeDTO {

    /** 属性 ID（更新时携带，新增时为 null） */
    private Long id;

    /** 关联分类属性模板 ID，null 表示商品自定义属性 */
    private Long categoryAttributeId;

    @NotBlank(message = "属性名不能为空")
    @Size(max = 50, message = "属性名最大 50 字符")
    private String name;

    /** 属性类型：IMAGE / TEXT */
    private String type;

    private Integer sortOrder;

    /** 属性值列表 */
    private List<AttributeValueDTO> values;

    @Data
    public static class AttributeValueDTO {
        private Long id;
        @NotBlank(message = "属性值不能为空")
        @Size(max = 100, message = "属性值最大 100 字符")
        private String value;
        /** 图片型属性值的色块 URL */
        private String imageUrl;
        private Integer sortOrder;
    }
}