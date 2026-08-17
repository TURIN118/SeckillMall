package com.seckill.mall.product.api.command;

import com.seckill.mall.product.api.dto.AttributeDTO;
import com.seckill.mall.product.api.dto.SkuSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 编辑商品命令。
 *
 * <p>业务语义：编辑商品（更新基本信息 → 重建 SKU → 重建属性 → 刷新冗余字段）。
 *
 * <p>原方法：{@code ProductService.updateProduct(Long, ProductUpdateRequest)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductCommand {

    /** 商品 ID（必填） */
    private Long id;

    /** 商品名 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 分类 ID */
    private Long categoryId;

    /** 商品图片 */
    private String images;

    /** 主图 URL */
    private String mainImage;

    /** 详情 HTML */
    private String detailHtml;

    /** 商品状态 */
    private String status;

    /** SKU 列表（传入则重建） */
    private List<SkuSnapshot> skus;

    /** 属性列表（传入则重建） */
    private List<AttributeDTO> attributes;
}