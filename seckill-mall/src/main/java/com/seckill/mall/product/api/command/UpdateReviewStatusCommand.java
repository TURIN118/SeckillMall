package com.seckill.mall.product.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新评论状态命令（隐藏/显示）。
 *
 * <p>业务语义：隐藏/显示评论（status: 1=显示, 0=隐藏）。
 *
 * <p>原方法：{@code ProductReviewService.updateStatus(Long, Integer)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewStatusCommand {

    /** 评论 ID（必填） */
    private Long reviewId;

    /** 状态（必填，1=显示, 0=隐藏） */
    private Integer status;
}