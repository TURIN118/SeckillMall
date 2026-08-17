package com.seckill.mall.product.api.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论列表查询条件。
 *
 * <p>前台查询时 productId 必填；后台查询时 productId 可空，按 status 筛选。
 *
 * <p>原方法：{@code ProductReviewService.listByProductId(Long, int, int)} /
 * {@code ProductReviewService.listAll(Integer, int, int)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListQuery {

    /** 商品 ID（前台必填，后台可空） */
    private Long productId;

    /** 状态筛选（null=不筛选，1=显示，0=隐藏，后台用） */
    private Integer status;

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10） */
    private Integer pageSize;
}