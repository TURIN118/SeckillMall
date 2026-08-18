package com.seckill.mall.category.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Category Attribute API 数据传输对象 - 分类规格模板。
 *
 * <p>对应 {@code com.seckill.mall.category.interfaces.vo.CategoryAttributeVO} 的字段，用于模块间通信，
 * 替代直接暴露 CategoryAttribute Entity。供 CategoryAttributeApi 接口方法返回使用。
 *
 * <p>注意：本类与 {@code com.seckill.mall.dto.CategoryAttributeDTO}（请求对象）不同，
 * 后者是 HTTP 请求入参，本类是 API 层出参 DTO。
 *
 * @author wnj
 * @since Phase CA.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeDTO {

    private Long id;

    private Long categoryId;

    private String name;

    /** IMAGE / TEXT */
    private String type;

    /** SELECT / INPUT */
    private String inputType;

    private Integer isRequired;

    private Integer sortOrder;

    private List<CategoryAttributeValueDTO> values;

    /**
     * 分类属性预设值 DTO。
     *
     * <p>对应 {@code com.seckill.mall.category.interfaces.vo.CategoryAttributeVO.CategoryAttributeValueVO} 的字段。
     *
     * @author wnj
     * @since Phase CA.2
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAttributeValueDTO {
        private Long id;
        private String value;
        private String imageUrl;
        private Integer sortOrder;
    }
}