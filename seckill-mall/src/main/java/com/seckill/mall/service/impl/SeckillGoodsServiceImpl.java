package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.enums.SeckillStatus;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.SeckillGoodsService;
import com.seckill.mall.vo.SeckillGoodsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillGoodsServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // 一人一单：当前表结构 uk_user_seckill 约束限购 1 件
    private static final int DEFAULT_PER_LIMIT = 1;

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final ProductMapper productMapper;
    private final RedisService redisService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<SeckillGoodsVO> listSeckill(Integer status, Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        Page<SeckillGoods> page = new Page<>(num, size);
        IPage<SeckillGoods> result = seckillGoodsMapper.selectSeckillPage(page, parseStatus(status), null);

        List<SeckillGoods> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), num, size);
        }

        Map<Long, Product> productMap = buildProductMap(records);
        List<SeckillGoodsVO> voList = records.stream()
                .map(g -> toVO(g, productMap))
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), num, size);
    }

    @Override
    public SeckillGoodsVO getSeckillDetail(Long seckillId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        Product product = productMapper.selectById(goods.getProductId());
        return toVO(goods, product == null ? Collections.emptyMap() : Map.of(product.getId(), product));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillGoodsVO createSeckill(SeckillCreateRequest req) {
        Product product = productMapper.selectById(req.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
        if (req.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间不能早于当前时间");
        }

        SeckillGoods goods = new SeckillGoods();
        goods.setProductId(req.getProductId());
        goods.setSeckillPrice(req.getSeckillPrice());
        goods.setStockCount(req.getStockCount());
        goods.setAvailableCount(req.getStockCount());
        goods.setStartTime(req.getStartTime());
        goods.setEndTime(req.getEndTime());
        goods.setStatus(SeckillStatus.PENDING);
        goods.setCreatorId(SecurityUtils.getCurrentUserId());

        seckillGoodsMapper.insert(goods);
        preheatSeckill(goods.getId());
        return toVO(goods, Map.of(product.getId(), product));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillGoodsVO updateSeckill(Long id, SeckillCreateRequest req) {
        SeckillGoods goods = seckillGoodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        if (req.getStartTime() != null && req.getEndTime() != null
                && !req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
        if (req.getProductId() != null) {
            if (productMapper.selectById(req.getProductId()) == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            goods.setProductId(req.getProductId());
        }
        if (req.getSeckillPrice() != null) {
            goods.setSeckillPrice(req.getSeckillPrice());
        }
        if (req.getStockCount() != null) {
            goods.setStockCount(req.getStockCount());
            goods.setAvailableCount(req.getStockCount());
        }
        if (req.getStartTime() != null) {
            goods.setStartTime(req.getStartTime());
        }
        if (req.getEndTime() != null) {
            goods.setEndTime(req.getEndTime());
        }
        seckillGoodsMapper.updateById(goods);

        evictCache(id);
        preheatSeckill(id);
        Product product = productMapper.selectById(goods.getProductId());
        return toVO(goods, product == null ? Collections.emptyMap() : Map.of(product.getId(), product));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSeckill(Long id) {
        SeckillGoods goods = seckillGoodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        goods.setStatus(SeckillStatus.CANCELLED);
        seckillGoodsMapper.updateById(goods);
        evictCache(id);
    }

    @Override
    public Integer getStock(Long seckillId) {
        String stock = redisService.get(RedisKeyConstants.seckillStock(seckillId));
        if (stock != null) {
            try {
                return Integer.parseInt(stock);
            } catch (NumberFormatException e) {
                log.warn("库存缓存格式异常 seckillId={} value={}", seckillId, stock);
            }
        }
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        return goods.getAvailableCount();
    }

    @Override
    public void preheatSeckill(Long seckillId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        String infoKey = RedisKeyConstants.seckillInfo(seckillId);
        String stockKey = RedisKeyConstants.seckillStock(seckillId);

        long ttlSeconds = Math.max(60L, Duration.between(LocalDateTime.now(), goods.getEndTime()).getSeconds());

        redisService.hSet(infoKey, "id", String.valueOf(goods.getId()));
        redisService.hSet(infoKey, "stock", String.valueOf(goods.getAvailableCount()));
        redisService.hSet(infoKey, "startTime", goods.getStartTime().format(DATE_TIME_FORMATTER));
        redisService.hSet(infoKey, "endTime", goods.getEndTime().format(DATE_TIME_FORMATTER));
        redisService.hSet(infoKey, "status", goods.getStatus().getCode());
        redisService.hSet(infoKey, "perLimit", String.valueOf(DEFAULT_PER_LIMIT));
        redisService.expire(infoKey, ttlSeconds, TimeUnit.SECONDS);

        redisService.set(stockKey, String.valueOf(goods.getAvailableCount()), ttlSeconds, TimeUnit.SECONDS);

        // 布隆过滤器加入活动 ID，用于拦截不存在的 seckillId 请求
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_BLOOM_GOODS);
        bloomFilter.tryInit(10000L, 0.01);
        bloomFilter.add(seckillId);
    }

    private void evictCache(Long seckillId) {
        redisService.del(RedisKeyConstants.seckillInfo(seckillId));
        redisService.del(RedisKeyConstants.seckillStock(seckillId));
        redisService.del(RedisKeyConstants.seckillGoods(seckillId));
    }

    private Map<Long, Product> buildProductMap(List<SeckillGoods> goodsList) {
        List<Long> productIds = goodsList.stream()
                .map(SeckillGoods::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        return products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }

    private SeckillGoodsVO toVO(SeckillGoods goods, Map<Long, Product> productMap) {
        SeckillGoodsVO vo = new SeckillGoodsVO();
        vo.setId(goods.getId());
        vo.setProductId(goods.getProductId());
        vo.setSeckillPrice(goods.getSeckillPrice());
        vo.setStockCount(goods.getStockCount());
        vo.setAvailableCount(goods.getAvailableCount());
        vo.setStartTime(goods.getStartTime());
        vo.setEndTime(goods.getEndTime());
        vo.setStatus(goods.getStatus());
        vo.setPerLimit(DEFAULT_PER_LIMIT);
        vo.setCreateTime(goods.getCreateTime());

        Product product = productMap.get(goods.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            // 实体未单独存储秒杀活动名称，复用商品名称展示
            vo.setSeckillName(product.getName());
            vo.setImages(deserializeImages(product.getImages()));
            vo.setDescription(product.getDescription());
        }
        return vo;
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

    private SeckillStatus parseStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> SeckillStatus.PENDING;
            case 1 -> SeckillStatus.ACTIVE;
            case 2 -> SeckillStatus.ENDED;
            case 3 -> SeckillStatus.CANCELLED;
            default -> null;
        };
    }
}
