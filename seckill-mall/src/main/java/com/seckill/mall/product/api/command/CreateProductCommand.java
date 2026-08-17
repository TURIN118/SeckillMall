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
 * 新增商品命令。
 *
 * <p>业务语义：新增商品（保存商品基本信息 → 保存 SKU → 保存属性 → 刷新价格区间冗余字段）。
 *
 * <p>原方法：{@code ProductService.createProduct(ProductCreateRequest)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductCommand {

    /** 商品名（必填，≤200 字符） */
    private String name;

    /** 商品描述 */
    private String description;

    /** 原价（必填，>0） */
    private BigDecimal originalPrice;

    /** 库存（无 SKU 时必填，≥0） */
    private Integer stock;

    /** 分类 ID（必填） */
    private Long categoryId;

    /** 商品图片（JSON 数组） */
    private String images;

    /** 主图 URL */
    private String mainImage;

    /** 详情 HTML */
    private String detailHtml;

    /** 商品状态（ON_SALE/OFF_SHELF，必填） */
    private String status;

    /** SKU 列表（有规格商品必填） */
    private List<SkuSnapshot> skus;

    /** 属性及属性值列表 */
    private List<AttributeDTO> attributes;
}