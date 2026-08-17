package com.seckill.mall.product.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发表评论命令。
 *
 * <p>业务语义：发表评论（校验 SKU 归属 → 校验用户是否购买过该 SKU → 保存评论）。
 *
 * <p>原方法：{@code ProductReviewService.create(Long, Long, Long, String, Integer, String)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewCommand {

    /** 用户 ID（由 CurrentUserContext 注入） */
    private Long userId;

    /** 商品 ID（必填） */
    private Long productId;

    /** SKU ID（null 或 0 表示无规格评论） */
    private Long skuId;

    /** 评论内容（必填，≤1000 字符，已 HTML 转义） */
    private String content;

    /** 评分（必填，1-5） */
    private Integer rating;

    /** 评论图片 URL 数组（JSON 字符串） */
    private String images;
}