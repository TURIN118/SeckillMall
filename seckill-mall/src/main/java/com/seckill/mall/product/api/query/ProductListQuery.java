package com.seckill.mall.product.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品列表查询条件。
 *
 * <p>支持按分类/状态/关键字筛选，分页查询。
 *
 * <p>原方法：{@code ProductService.listProducts(ProductQueryRequest)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListQuery {

    /** 分类 ID 筛选 */
    private Long categoryId;

    /** 商品状态筛选（ON_SALE/OFF_SHELF） */
    private String status;

    /** 商品名关键字模糊匹配 */
    private String keyword;

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10，上限 50） */
    private Integer pageSize;
}