package com.seckill.mall.product.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.product.api.command.CreateReviewCommand;
import com.seckill.mall.product.api.command.ReplyReviewCommand;
import com.seckill.mall.product.api.command.UpdateReviewStatusCommand;
import com.seckill.mall.product.api.dto.ReviewDTO;
import com.seckill.mall.product.api.query.ReviewListQuery;

/**
 * Product 模块评论能力 API。
 *
 * <p>对外暴露评论查询、发表、回复、状态管理等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>出参用 ReviewDTO，禁止暴露 ProductReview Entity</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出</li>
 * </ul>
 *
 * @author wnj
 * @since Phase P.2
 */
public interface ReviewApi {

    /**
     * 查询商品评论分页（只查 status=1 的显示评论）。
     *
     * @param query 评论列表查询条件
     * @return 分页结果
     */
    PageResult<ReviewDTO> listByProductId(ReviewListQuery query);

    /**
     * 发表评论。
     *
     * @param command 发表评论命令
     * @return 评论 DTO
     * @throws com.seckill.mall.exception.BusinessException {@code PRODUCT_NOT_FOUND}、
     *         {@code SKU_NOT_BELONG_TO_PRODUCT}、{@code REVIEW_PURCHASE_REQUIRED}、{@code PARAM_ERROR}
     */
    ReviewDTO createReview(CreateReviewCommand command);

    /**
     * 后台查询所有评论（可按 status 筛选）。
     *
     * @param query 评论列表查询条件
     * @return 分页结果
     */
    PageResult<ReviewDTO> listAllReviews(ReviewListQuery query);

    /**
     * 回复评论。
     *
     * @param command 回复评论命令
     * @throws com.seckill.mall.exception.BusinessException {@code REVIEW_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void replyReview(ReplyReviewCommand command);

    /**
     * 隐藏/显示评论。
     *
     * @param command 更新评论状态命令
     * @throws com.seckill.mall.exception.BusinessException {@code REVIEW_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void updateReviewStatus(UpdateReviewStatusCommand command);
}