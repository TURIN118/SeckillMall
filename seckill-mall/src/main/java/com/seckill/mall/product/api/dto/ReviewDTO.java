package com.seckill.mall.product.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论 DTO。
 *
 * <p>由 {@code listByProductId}、{@code createReview}、{@code listAllReviews} 等方法返回。
 * 替代 {@code ProductReviewVO} 用于模块间通信。
 *
 * <p>来源映射：ProductReview Entity → ReviewDTO
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    /** 评论 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** SKU 属性快照 */
    private String skuAttributes;

    /** 用户 ID */
    private Long userId;

    /** 订单 ID */
    private Long orderId;

    /** 评论内容 */
    private String content;

    /** 评分（1-5） */
    private Integer rating;

    /** 评论图片（JSON） */
    private String images;

    /** 状态（1=显示, 0=隐藏） */
    private Integer status;

    /** 回复内容 */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}