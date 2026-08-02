package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.vo.ProductReviewVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductReviewService.java
 * 邮箱：nj651217@163.com
 */
public interface ProductReviewService {

    /**
     * 查询商品评论分页（只查 status=1 的评论）
     *
     * @param productId 商品 ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @return 评论分页结果
     */
    PageResult<ProductReviewVO> listByProductId(Long productId, int pageNum, int pageSize);

    /**
     * 发表评论
     *
     * @param productId 商品 ID
     * @param userId    用户 ID
     * @param content   评论内容
     * @param rating    评分（1-5）
     * @param images    评论图片 URL 数组（JSON 字符串）
     * @return 评论视图对象
     */
    ProductReviewVO create(Long userId, Long productId, String content, Integer rating, String images);

    /**
     * 后台查询所有评论（可按 status 筛选）
     *
     * @param status   状态筛选（null 表示不筛选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 评论分页结果
     */
    PageResult<ProductReviewVO> listAll(Integer status, int pageNum, int pageSize);

    /**
     * 回复评论
     *
     * @param id           评论 ID
     * @param replyContent 回复内容
     */
    void reply(Long id, String replyContent);

    /**
     * 隐藏/显示评论
     *
     * @param id     评论 ID
     * @param status 状态：1-显示 / 0-隐藏
     */
    void updateStatus(Long id, Integer status);
}