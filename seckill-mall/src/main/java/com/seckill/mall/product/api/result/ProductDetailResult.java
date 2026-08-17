package com.seckill.mall.product.api.result;

import com.seckill.mall.product.api.dto.AttributeDTO;
import com.seckill.mall.product.api.dto.SkuSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情结果。
 *
 * <p>由 {@code getProductDetail}、{@code createProduct}、{@code updateProduct} 方法返回，
 * 包含商品完整信息、SKU 列表与属性列表。
 *
 * <p>来源映射：Product Entity + ProductSku List + ProductAttribute List → ProductDetailResult
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResult {

    /** 商品 ID */
    private Long id;

    /** 商品名 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 价格区间最小值 */
    private BigDecimal minPrice;

    /** 价格区间最大值 */
    private BigDecimal maxPrice;

    /** 总库存 */
    private Integer totalStock;

    /** 销量 */
    private Integer salesCount;

    /** 加购数量 */
    private Integer cartCount;

    /** 收藏数量 */
    private Integer favoriteCount;

    /** 分类 ID */
    private Long categoryId;

    /** 商品图片 */
    private String images;

    /** 主图 URL */
    private String mainImage;

    /** 详情 HTML */
    private String detailHtml;

    /** 商品状态码（ON_SALE/OFF_SHELF） */
    private String status;

    /** SKU 列表 */
    private List<SkuSnapshot> skus;

    /** 属性及属性值列表 */
    private List<AttributeDTO> attributes;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}