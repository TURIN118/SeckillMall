package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.XssCleanUtil;
import com.seckill.mall.entity.ProductReview;
import com.seckill.mall.entity.User;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.ProductReviewMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.ProductReviewService;
import com.seckill.mall.vo.ProductReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductReviewServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewMapper productReviewMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<ProductReviewVO> listByProductId(Long productId, int pageNum, int pageSize) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品 ID 不能为空");
        }
        int pn = pageNum < 1 ? 1 : pageNum;
        int ps = pageSize < 1 ? 10 : Math.min(pageSize, 50);

        QueryWrapper<ProductReview> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId)
                .eq("status", 1)
                .orderByDesc("create_time");

        IPage<ProductReview> page = productReviewMapper.selectPage(new Page<>(pn, ps), wrapper);
        List<ProductReview> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        Map<Long, String> userNameMap = buildUserNameMap(records);
        List<ProductReviewVO> voList = records.stream()
                .map(r -> toVO(r, userNameMap))
                .collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductReviewVO create(Long userId, Long productId, String content, Integer rating, String images) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (productId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品 ID 不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论内容不能为空");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分必须为 1-5 星");
        }
        // 校验商品存在
        if (productMapper.selectById(productId) == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setContent(XssCleanUtil.cleanStrict(content));
        review.setRating(rating);
        review.setImages(images);
        review.setStatus(1);
        productReviewMapper.insert(review);

        User user = userMapper.selectById(userId);
        ProductReviewVO vo = toVO(review, Collections.emptyMap());
        if (user != null) {
            vo.setUserName(user.getNickname() != null && !user.getNickname().isBlank()
                    ? user.getNickname() : user.getUsername());
        }
        return vo;
    }

    @Override
    public PageResult<ProductReviewVO> listAll(Integer status, int pageNum, int pageSize) {
        int pn = pageNum < 1 ? 1 : pageNum;
        int ps = pageSize < 1 ? 10 : Math.min(pageSize, 50);

        QueryWrapper<ProductReview> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        IPage<ProductReview> page = productReviewMapper.selectPage(new Page<>(pn, ps), wrapper);
        List<ProductReview> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        Map<Long, String> userNameMap = buildUserNameMap(records);
        List<ProductReviewVO> voList = records.stream()
                .map(r -> toVO(r, userNameMap))
                .collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, String replyContent) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论 ID 不能为空");
        }
        if (replyContent == null || replyContent.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }
        ProductReview review = productReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        }
        ProductReview update = new ProductReview();
        update.setId(id);
        update.setReplyContent(XssCleanUtil.cleanStrict(replyContent));
        update.setReplyTime(LocalDateTime.now());
        productReviewMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论 ID 不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值非法");
        }
        ProductReview review = productReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论不存在");
        }
        ProductReview update = new ProductReview();
        update.setId(id);
        update.setStatus(status);
        productReviewMapper.updateById(update);
    }

    /**
     * 批量查询用户名，避免 N+1
     */
    private Map<Long, String> buildUserNameMap(List<ProductReview> reviews) {
        List<Long> userIds = reviews.stream()
                .map(ProductReview::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getNickname() != null && !u.getNickname().isBlank()
                                ? u.getNickname() : u.getUsername(),
                        (a, b) -> a));
    }

    private ProductReviewVO toVO(ProductReview review, Map<Long, String> userNameMap) {
        ProductReviewVO vo = new ProductReviewVO();
        vo.setId(review.getId());
        vo.setProductId(review.getProductId());
        vo.setUserId(review.getUserId());
        vo.setUserName(userNameMap.getOrDefault(review.getUserId(), null));
        vo.setOrderId(review.getOrderId());
        vo.setContent(review.getContent());
        vo.setRating(review.getRating());
        vo.setImages(deserializeImages(review.getImages()));
        vo.setStatus(review.getStatus());
        vo.setReplyContent(review.getReplyContent());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private List<String> deserializeImages(String images) {
        if (images == null || images.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("反序列化评论图片列表失败: {}", images, e);
            return Collections.emptyList();
        }
    }
}