package com.seckill.mall.vo;

import com.seckill.mall.entity.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductVO {

    private Long id;

    private String productName;

    private Long categoryId;

    private String categoryName;

    private String description;

    /** 商品详情富文本(HTML) */
    private String detailHtml;

    private BigDecimal originalPrice;

    private List<String> images;

    private Integer stock;

    private Integer salesCount;

    private ProductStatus status;

    private LocalDateTime createTime;

    /** 商品属性列表（无规格时为空列表） */
    private List<ProductAttributeVO> attributes;

    /** 商品 SKU 列表（无规格时为空列表） */
    private List<ProductSkuVO> skus;

    /** 是否有 SKU：true 表示多规格商品 */
    private Boolean hasSku;

    /** 价格区间最小值（有 SKU 时取最低 SKU 价格，无 SKU 时取 originalPrice） */
    private BigDecimal minPrice;

    /** 价格区间最大值（有 SKU 时取最高 SKU 价格，无 SKU 时取 originalPrice） */
    private BigDecimal maxPrice;

    /** 总库存（有 SKU 时取所有启用 SKU 库存之和，无 SKU 时取 stock） */
    private Integer totalStock;

    /** 分类规格模板（供前端区分模板属性 / 自定义属性） */
    private List<CategoryAttributeVO> categoryAttributes;
}
