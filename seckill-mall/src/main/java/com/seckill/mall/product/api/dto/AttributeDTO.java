package com.seckill.mall.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品属性 DTO。
 *
 * <p>包含属性基本信息及属性值列表。由 {@code listAttributesByProductId} 等方法返回。
 * 替代 {@code ProductAttributeVO} 用于模块间通信。
 *
 * <p>来源映射：ProductAttribute Entity → AttributeDTO
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDTO {

    /** 属性 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 分类属性模板 ID */
    private Long categoryAttributeId;

    /** 属性名（如「颜色」） */
    private String name;

    /** 属性类型（IMAGE/TEXT） */
    private String type;

    /** 排序序号 */
    private Integer sortOrder;

    /** 属性值列表 */
    private List<AttributeValueDTO> values;

    /**
     * 属性值 DTO。
     *
     * <p>来源映射：ProductAttributeValue Entity → AttributeValueDTO
     *
     * @author wnj
     * @since Phase P.2
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeValueDTO {

        /** 属性值 ID */
        private Long id;

        /** 属性 ID */
        private Long attributeId;

        /** 商品 ID */
        private Long productId;

        /** 属性值（如「曜石黑」） */
        private String value;

        /** 图片型属性值的色块 URL */
        private String imageUrl;

        /** 排序序号 */
        private Integer sortOrder;
    }
}
