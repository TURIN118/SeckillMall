package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.XssCleanUtil;
import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.entity.NormalOrderItem;
import com.seckill.mall.entity.ProductReview;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.mapper.NormalOrderItemMapper;
import com.seckill.mall.mapper.NormalOrderMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.ProductReviewMapper;
import com.seckill.mall.mapper.ProductSkuMapper;
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
    private final ProductSkuMapper productSkuMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final NormalOrderMapper normalOrderMapper;

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
    public ProductReviewVO create(Long userId, Long productId, Long skuId,
                                 String content, Integer rating, String images) {
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
        // 1. 校验商品存在
        if (productMapper.selectById(productId) == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 5.7.4 建议13：校验 skuId 属于该 productId
        Long effectiveSkuId = (skuId == null || skuId == 0L) ? 0L : skuId;
        String skuAttributes = null;
        if (effectiveSkuId != 0L) {
            ProductSku sku = productSkuMapper.selectById(effectiveSkuId);
            if (sku == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不存在");
            }
            if (!sku.getProductId().equals(productId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不属于该商品，无法发表评论");
            }
            skuAttributes = convertAttributesToReadable(sku.getAttributes());
        }

        // 3. 5.7.4 建议13：校验用户是否购买了该 SKU（防止恶意评论）
        boolean hasPurchased = checkUserPurchasedSku(userId, productId, effectiveSkuId);
        if (!hasPurchased) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "您未购买该商品规格，无法发表评论");
        }

        // 4. 创建评论
        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setSkuId(effectiveSkuId);
        review.setSkuAttributes(skuAttributes);
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

    /**
     * 5.7.4 建议13：校验用户是否购买了该 SKU
     * <p>
     * 查询 t_normal_order_item 关联 t_normal_order，确认存在已完成 / 已发货订单。
     * skuId = 0 时不校验 SKU 维度，仅校验商品维度。
     *
     * @param userId     用户 ID
     * @param productId  商品 ID
     * @param skuId      SKU ID（0 表示无规格）
     * @return true=已购买
     */
    private boolean checkUserPurchasedSku(Long userId, Long productId, Long skuId) {
        // 查询该用户对该商品的订单明细，关联订单表确认状态
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getProductId, productId)
                        .eq(skuId != 0L, NormalOrderItem::getSkuId, skuId));
        if (items.isEmpty()) {
            return false;
        }
        // 收集订单 ID，查询订单状态是否为 COMPLETED / SHIPPED
        List<Long> orderIds = items.stream()
                .map(NormalOrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        List<NormalOrder> orders = normalOrderMapper.selectList(
                new LambdaQueryWrapper<NormalOrder>()
                        .eq(NormalOrder::getUserId, userId)
                        .in(NormalOrder::getId, orderIds)
                        .in(NormalOrder::getStatus, OrderStatus.COMPLETED, OrderStatus.SHIPPED));
        return !orders.isEmpty();
    }

    /**
     * 5.7.4：将 SKU attributes JSON 转可读字符串
     * 如 {"颜色":"曜石黑","版本":"旗舰版"} → "颜色: 曜石黑 / 版本: 旗舰版"
     */
    private String convertAttributesToReadable(String attributesJson) {
        if (attributesJson == null || attributesJson.isEmpty()) {
            return null;
        }
        try {
            Map<String, String> map = objectMapper.readValue(attributesJson,
                    new TypeReference<Map<String, String>>() {});
            return map.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(" / "));
        } catch (Exception e) {
            log.warn("转换 SKU 属性 JSON 失败: {}", attributesJson, e);
            return attributesJson;
        }
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
        // 5.7.4：填充 skuId / skuAttributes（直接从实体读取，已快照）
        vo.setSkuId(review.getSkuId());
        vo.setSkuAttributes(review.getSkuAttributes());
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