package com.seckill.mall.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SKU 快照 DTO，替代 ProductSku Entity 跨模块传递。
 *
 * <p>跨模块只读传递时使用，避免暴露 {@code ProductSku} Entity。
 * 裁剪掉 {@code isDeleted}、{@code createTime}、{@code updateTime} 等基础设施字段。
 *
 * <p>来源映射：ProductSku Entity → SkuSnapshot
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuSnapshot {

    /** SKU ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** SKU 编码 */
    private String skuCode;

    /** SKU 价格 */
    private BigDecimal price;

    /** SKU 库存 */
    private Integer stock;

    /** SKU 主图 URL */
    private String mainImage;

    /** SKU 属性键值对 JSON */
    private String attributes;

    /** 状态（1=启用, 0=禁用） */
    private Integer status;
}