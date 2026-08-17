package com.seckill.mall.product.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回复评论命令。
 *
 * <p>业务语义：商家/管理员回复评论。
 *
 * <p>原方法：{@code ProductReviewService.reply(Long, String)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyReviewCommand {

    /** 评论 ID（必填） */
    private Long reviewId;

    /** 回复内容（必填） */
    private String replyContent;
}