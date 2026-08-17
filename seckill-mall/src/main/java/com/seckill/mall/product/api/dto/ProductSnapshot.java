package com.seckill.mall.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品快照 DTO，替代 Product Entity 跨模块传递。
 *
 * <p>跨模块只读传递时使用，避免暴露 {@code Product} Entity。
 * 裁剪掉 {@code isDeleted} 等基础设施字段。
 *
 * <p>来源映射：Product Entity → ProductSnapshot
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSnapshot {

    /** 商品 ID */
    private Long id;

    /** 商品名 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存（无 SKU 时用） */
    private Integer stock;

    /** 价格区间最小值（冗余） */
    private BigDecimal minPrice;

    /** 价格区间最大值（冗余） */
    private BigDecimal maxPrice;

    /** 总库存（冗余） */
    private Integer totalStock;

    /** 销量 */
    private Integer salesCount;

    /** 加购数量（冗余） */
    private Integer cartCount;

    /** 收藏数量（冗余） */
    private Integer favoriteCount;

    /** 分类 ID */
    private Long categoryId;

    /** 商品图片（JSON） */
    private String images;

    /** 主图 URL */
    private String mainImage;

    /** 详情 HTML */
    private String detailHtml;

    /** 商品状态码（ON_SALE/OFF_SHELF） */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}