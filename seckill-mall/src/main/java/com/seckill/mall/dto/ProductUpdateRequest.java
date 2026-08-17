package com.seckill.mall.dto;

import com.seckill.mall.product.domain.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductUpdateRequest {

    @Size(max = 100, message = "商品名称最大 100 字符")
    private String productName;

    private Long categoryId;

    @Size(max = 500, message = "商品简述最大 500 字符")
    private String description;

    /** 商品详情富文本(HTML)，由 wangEditor 产生，不进行 XSS 清洗以保留合法标签 */
    private String detailHtml;

    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    private BigDecimal originalPrice;

    private List<String> images;

    @Min(value = 0, message = "库存数量不能小于 0")
    private Integer stock;

    private ProductStatus status;

    /** 商品属性列表（可选，无规格时为 null） */
    @Valid
    private List<ProductAttributeDTO> attributes;

    /** 商品 SKU 列表（可选，无规格时为 null） */
    @Valid
    private List<ProductSkuDTO> skus;
}
