package com.seckill.mall.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品摘要 DTO，列表/统计用。
 *
 * <p>由 {@code listProducts} 方法返回，包含商品列表展示摘要信息。
 * 替代 {@code ProductVO} 用于模块间通信。
 *
 * <p>来源映射：Product Entity → ProductSummaryDTO
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO {

    /** 商品 ID */
    private Long id;

    /** 商品名 */
    private String name;

    /** 主图 URL */
    private String mainImage;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 价格区间最小值 */
    private BigDecimal minPrice;

    /** 价格区间最大值 */
    private BigDecimal maxPrice;

    /** 总库存 */
    private Integer totalStock;

    /** 销量 */
    private Integer salesCount;

    /** 商品状态码（ON_SALE/OFF_SHELF） */
    private String status;

    /** 分类 ID */
    private Long categoryId;

    /** 创建时间 */
    private LocalDateTime createTime;
}