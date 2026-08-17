package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.SeckillActivityCreateRequest;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.entity.SeckillActivity;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.enums.SeckillStatus;
import com.seckill.mall.mapper.SeckillActivityMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.security.SecurityUtils;

import com.seckill.mall.service.SeckillActivityService;
import com.seckill.mall.service.SeckillGoodsService;
import com.seckill.mall.vo.SeckillActivityVO;
import com.seckill.mall.vo.SeckillGoodsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillActivityServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl implements SeckillActivityService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final ProductApi productApi;
    private final SeckillGoodsService seckillGoodsService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillActivityVO createActivity(SeckillActivityCreateRequest req) {
        // 1. 参数校验
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
        if (req.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间不能早于当前时间");
        }
        if (req.getGoodsItems() == null || req.getGoodsItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场次下至少需要一个秒杀商品");
        }

        // 2. 校验所有商品存在
        List<Long> productIds = req.getGoodsItems().stream()
                .map(SeckillActivityCreateRequest.ActivityGoodsItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<ProductSnapshot> products = productApi.getProductsByIds(productIds);
        if (products.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "部分商品不存在");
        }

        // 3. 创建场次
        SeckillActivity activity = new SeckillActivity();
        activity.setName(req.getName());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setStatus(0);
        activity.setPerLimit(req.getPerLimit() != null && req.getPerLimit() > 0 ? req.getPerLimit() : 1);
        activity.setDescription(req.getDescription());
        activity.setImages(serializeImages(req.getImages()));
        seckillActivityMapper.insert(activity);

        // 4. 批量创建场次下的秒杀商品
        Map<Long, ProductSnapshot> productMap = products.stream()
                .collect(Collectors.toMap(ProductSnapshot::getId, p -> p, (a, b) -> a));

        List<SeckillGoods> goodsList = req.getGoodsItems().stream().map(item -> {
            SeckillGoods g = new SeckillGoods();
            g.setProductId(item.getProductId());
            g.setActivityId(activity.getId());
            g.setSeckillPrice(item.getSeckillPrice());
            g.setStockCount(item.getStockCount());
            g.setAvailableCount(item.getStockCount());
            g.setStartTime(req.getStartTime());
            g.setEndTime(req.getEndTime());
            g.setStatus(SeckillStatus.PENDING);
            g.setSeckillName(item.getSeckillName());
            g.setPerLimit(activity.getPerLimit());
            g.setImages(serializeImages(item.getImages()));
            g.setDescription(item.getDescription());
            g.setCreatorId(securityUtils.getCurrentUserId());
            return g;
        }).collect(Collectors.toList());

        List<Long> createdGoodsIds = new java.util.ArrayList<>();
        for (SeckillGoods g : goodsList) {
            seckillGoodsMapper.insert(g);
            createdGoodsIds.add(g.getId());
        }

        // C6 修复：使用 @TransactionalEventListener(AFTER_COMMIT) 替代 registerAfterCommit，
        // 预热异常不影响主流程（事务已提交），且事件驱动方式更标准。
        eventPublisher.publishEvent(new SeckillPreheatEvent(createdGoodsIds));

        return toActivityVO(activity, goodsList, productMap);
    }

    /**
     * C6 修复：事务提交后预热缓存事件监听器。
     * <p>
     * 使用 {@link TransactionalEventListener}(phase = AFTER_COMMIT) 确保：
     * 1) 预热在事务提交后执行，避免回滚后缓存与 DB 不一致
     * 2) 预热异常不影响主流程（事务已提交，异常被 try-catch 吞掉）
     * 3) 无事务上下文时（如手动调用），@TransactionalEventListener 默认不执行，
     *    但可通过 fallbackExecution=true 启用。此处保留默认行为。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeckillPreheat(SeckillPreheatEvent event) {
        for (Long gid : event.getGoodsIds()) {
            try {
                seckillGoodsService.preheatSeckill(gid);
            } catch (Exception e) {
                // C6 修复：预热异常仅记录日志，不影响主流程
                log.warn("场次商品预热失败 gid={}", gid, e);
            }
        }
    }

    /**
     * C6 修复：秒杀场次创建后预热事件。
     */
    public static class SeckillPreheatEvent {
        private final List<Long> goodsIds;

        public SeckillPreheatEvent(List<Long> goodsIds) {
            this.goodsIds = goodsIds;
        }

        public List<Long> getGoodsIds() {
            return goodsIds;
        }
    }

    @Override
    public List<SeckillActivityVO> listActivities() {
        List<SeckillActivity> activities = seckillActivityMapper.selectList(null);
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }
        return activities.stream().map(a -> {
            List<SeckillGoods> goods = listGoodsByActivity(a.getId());
            return toActivityVO(a, goods, buildProductMap(goods));
        }).collect(Collectors.toList());
    }

    @Override
    public SeckillActivityVO getActivityDetail(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND, "场次不存在");
        }
        List<SeckillGoods> goods = listGoodsByActivity(activityId);
        return toActivityVO(activity, goods, buildProductMap(goods));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND, "场次不存在");
        }
        seckillActivityMapper.deleteById(activityId);
        // 场次下商品不强制删除，保留订单历史关联
    }

    /* ==================== 私有辅助方法 ==================== */

    private List<SeckillGoods> listGoodsByActivity(Long activityId) {
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillGoods::getActivityId, activityId);
        return seckillGoodsMapper.selectList(wrapper);
    }

    private Map<Long, ProductSnapshot> buildProductMap(List<SeckillGoods> goodsList) {
        List<Long> productIds = goodsList.stream()
                .map(SeckillGoods::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductSnapshot> products = productApi.getProductsByIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductSnapshot::getId, p -> p, (a, b) -> a));
    }

    private SeckillActivityVO toActivityVO(SeckillActivity activity,
                                            List<SeckillGoods> goodsList,
                                            Map<Long, ProductSnapshot> productMap) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        // 动态计算状态：0=待开始 1=进行中 2=已结束
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            vo.setStatus(0);
        } else if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            vo.setStatus(2);
        } else {
            vo.setStatus(1);
        }
        vo.setPerLimit(activity.getPerLimit());
        vo.setDescription(activity.getDescription());
        vo.setImages(deserializeImages(activity.getImages()));
        vo.setCreateTime(activity.getCreateTime());
        vo.setGoodsList(goodsList.stream().map(g -> toGoodsVO(g, productMap)).collect(Collectors.toList()));
        return vo;
    }

    private SeckillGoodsVO toGoodsVO(SeckillGoods goods, Map<Long, ProductSnapshot> productMap) {
        SeckillGoodsVO vo = new SeckillGoodsVO();
        vo.setId(goods.getId());
        vo.setProductId(goods.getProductId());
        vo.setSeckillPrice(goods.getSeckillPrice());
        vo.setStockCount(goods.getStockCount());
        vo.setAvailableCount(goods.getAvailableCount());
        vo.setStartTime(goods.getStartTime());
        vo.setEndTime(goods.getEndTime());
        if (goods.getStatus() == SeckillStatus.CANCELLED) {
            vo.setStatus(SeckillStatus.CANCELLED);
        } else {
            LocalDateTime now = LocalDateTime.now();
            if (goods.getStartTime() != null && now.isBefore(goods.getStartTime())) {
                vo.setStatus(SeckillStatus.PENDING);
            } else if (goods.getEndTime() != null && now.isAfter(goods.getEndTime())) {
                vo.setStatus(SeckillStatus.ENDED);
            } else {
                vo.setStatus(SeckillStatus.ACTIVE);
            }
        }
        vo.setPerLimit(goods.getPerLimit());
        vo.setCreateTime(goods.getCreateTime());
        ProductSnapshot product = productMap.get(goods.getProductId());
        vo.setSeckillName(goods.getSeckillName() != null ? goods.getSeckillName()
                : (product != null ? product.getName() : null));
        vo.setImages(goods.getImages() != null && !goods.getImages().isBlank()
                ? deserializeImages(goods.getImages())
                : (product != null ? deserializeImages(product.getImages()) : Collections.emptyList()));
        vo.setDescription(goods.getDescription() != null ? goods.getDescription()
                : (product != null ? product.getDescription() : null));
        if (product != null) {
            vo.setProductName(product.getName());
        }
        return vo;
    }

    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (Exception e) {
            log.warn("序列化图片列表失败: {}", images, e);
            return null;
        }
    }

    private List<String> deserializeImages(String images) {
        if (images == null || images.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("反序列化图片列表失败: {}", images, e);
            return Collections.emptyList();
        }
    }
}
