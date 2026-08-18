package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.SeckillCreateRequest;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.seckill.infrastructure.entity.SeckillGoods;
import com.seckill.mall.entity.enums.SeckillStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
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
    private final ProductApi productApi;
    private final RedisService redisService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;

    @Override
    public PageResult<SeckillGoodsVO> listSeckill(String status, Long categoryId, Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        Page<SeckillGoods> page = new Page<>(num, size);
        IPage<SeckillGoods> result = seckillGoodsMapper.selectSeckillPage(page, parseStatus(status), null, categoryId);

        List<SeckillGoods> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), num, size);
        }

        Map<Long, ProductSnapshot> productMap = buildProductMap(records);
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
        ProductSnapshot product = productApi.getProductById(goods.getProductId());
        return toVO(goods, product == null ? Collections.emptyMap() : Map.of(product.getId(), product));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillGoodsVO createSeckill(SeckillCreateRequest req) {
        ProductSnapshot product = productApi.getProductById(req.getProductId());
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
        goods.setCreatorId(securityUtils.getCurrentUserId());
        // 落库秒杀活动扩展字段
        goods.setSeckillName(req.getSeckillName());
        goods.setPerLimit(req.getPerLimit() != null && req.getPerLimit() > 0 ? req.getPerLimit() : DEFAULT_PER_LIMIT);
        goods.setImages(serializeImages(req.getImages()));
        goods.setDescription(req.getDescription());

        seckillGoodsMapper.insert(goods);
        // H9 修复：将 preheatSeckill（Redis 操作）移到事务提交后执行，避免事务回滚后缓存与 DB 不一致
        registerAfterCommit(() -> preheatSeckill(goods.getId()));
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
            if (!productApi.existsById(req.getProductId())) {
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
        // 更新秒杀活动扩展字段
        if (req.getSeckillName() != null) {
            goods.setSeckillName(req.getSeckillName());
        }
        if (req.getPerLimit() != null && req.getPerLimit() > 0) {
            goods.setPerLimit(req.getPerLimit());
        }
        if (req.getImages() != null) {
            goods.setImages(serializeImages(req.getImages()));
        }
        if (req.getDescription() != null) {
            goods.setDescription(req.getDescription());
        }
        seckillGoodsMapper.updateById(goods);

        // H9/M19 修复：evictCache 与 preheatSeckill（Redis 操作）移到事务提交后执行
        registerAfterCommit(() -> {
            evictCache(id);
            preheatSeckill(id);
        });
        ProductSnapshot product = productApi.getProductById(goods.getProductId());
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
        // M19 修复：evictCache（Redis 操作）移到事务提交后执行
        registerAfterCommit(() -> evictCache(id));
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

        // L7: TTL 兜底——取活动剩余时间与 60s 的较大值，避免活动临近结束导致缓存过早失效
        long ttlSeconds = Math.max(60L, Duration.between(LocalDateTime.now(), goods.getEndTime()).getSeconds());

        redisService.hSet(infoKey, "id", String.valueOf(goods.getId()));
        redisService.hSet(infoKey, "stock", String.valueOf(goods.getAvailableCount()));
        redisService.hSet(infoKey, "startTime", goods.getStartTime().format(DATE_TIME_FORMATTER));
        redisService.hSet(infoKey, "endTime", goods.getEndTime().format(DATE_TIME_FORMATTER));
        redisService.hSet(infoKey, "status", goods.getStatus().getCode());
        redisService.hSet(infoKey, "perLimit", String.valueOf(resolvePerLimit(goods)));
        redisService.expire(infoKey, ttlSeconds, TimeUnit.SECONDS);

        redisService.set(stockKey, String.valueOf(goods.getAvailableCount()), ttlSeconds, TimeUnit.SECONDS);

        // 布隆过滤器加入活动 ID，用于拦截不存在的 seckillId 请求
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_BLOOM_GOODS);
        bloomFilter.tryInit(10000L, 0.01);
        bloomFilter.add(seckillId);
    }

    /**
     * H9/M19: 将 Redis 操作注册到事务提交后执行；无事务上下文时直接执行。
     * 避免事务回滚后缓存与 DB 不一致。
     */
    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void evictCache(Long seckillId) {
        redisService.del(RedisKeyConstants.seckillInfo(seckillId));
        redisService.del(RedisKeyConstants.seckillStock(seckillId));
        redisService.del(RedisKeyConstants.seckillGoods(seckillId));
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
        return products.stream()
                .collect(Collectors.toMap(ProductSnapshot::getId, p -> p, (a, b) -> a));
    }

    private SeckillGoodsVO toVO(SeckillGoods goods, Map<Long, ProductSnapshot> productMap) {
        SeckillGoodsVO vo = new SeckillGoodsVO();
        vo.setId(goods.getId());
        vo.setProductId(goods.getProductId());
        vo.setSeckillPrice(goods.getSeckillPrice());
        vo.setStockCount(goods.getStockCount());
        vo.setAvailableCount(goods.getAvailableCount());
        vo.setStartTime(goods.getStartTime());
        vo.setEndTime(goods.getEndTime());
        // 动态计算秒杀活动状态（不依赖数据库存储的 status 字段，确保实时准确）
        // 已取消的活动保持原状态，其余根据 startTime/endTime 和当前时间实时计算
        if (goods.getStatus() == SeckillStatus.CANCELLED) {
            vo.setStatus(SeckillStatus.CANCELLED);
        } else {
            LocalDateTime now = LocalDateTime.now();
            if (goods.getStartTime() != null && now.isBefore(goods.getStartTime())) {
                vo.setStatus(SeckillStatus.PENDING);   // 未开始
            } else if (goods.getEndTime() != null && now.isAfter(goods.getEndTime())) {
                vo.setStatus(SeckillStatus.ENDED);     // 已结束
            } else {
                vo.setStatus(SeckillStatus.ACTIVE);    // 进行中
            }
        }
        vo.setPerLimit(resolvePerLimit(goods));
        vo.setCreateTime(goods.getCreateTime());

        ProductSnapshot product = productMap.get(goods.getProductId());
        // 秒杀活动名称：优先用活动自身字段，fallback 到商品名称（向后兼容）
        vo.setSeckillName(goods.getSeckillName() != null ? goods.getSeckillName()
                : (product != null ? product.getName() : null));
        // 活动图片：优先用活动自身字段，fallback 到商品图片
        vo.setImages(goods.getImages() != null && !goods.getImages().isBlank()
                ? deserializeImages(goods.getImages())
                : (product != null ? deserializeImages(product.getImages()) : Collections.emptyList()));
        // 活动描述：优先用活动自身字段，fallback 到商品描述
        vo.setDescription(goods.getDescription() != null ? goods.getDescription()
                : (product != null ? product.getDescription() : null));
        if (product != null) {
            vo.setProductName(product.getName());
        }
        return vo;
    }

    /**
     * 解析限购数量：优先用秒杀活动自身的 perLimit，为空或非法时 fallback 到默认值
     */
    private int resolvePerLimit(SeckillGoods goods) {
        Integer perLimit = goods.getPerLimit();
        return perLimit != null && perLimit > 0 ? perLimit : DEFAULT_PER_LIMIT;
    }

    /**
     * 将图片列表序列化为 JSON 数组字符串落库
     */
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

    private SeckillStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String trimmed = status.trim();
        // 1. 优先按枚举名解析（支持 "ACTIVE"/"PENDING"/"ENDED"/"CANCELLED"，大小写不敏感）
        try {
            return SeckillStatus.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 不是有效的枚举名，继续尝试数字解析
        }
        // 2. 兼容旧数字接口：0=PENDING, 1=ACTIVE, 2=ENDED, 3=CANCELLED
        try {
            int code = Integer.parseInt(trimmed);
            return switch (code) {
                case 0 -> SeckillStatus.PENDING;
                case 1 -> SeckillStatus.ACTIVE;
                case 2 -> SeckillStatus.ENDED;
                case 3 -> SeckillStatus.CANCELLED;
                default -> null;
            };
        } catch (NumberFormatException e2) {
            return null;
        }
    }

    // ==================== Phase 14：统计方法（从 StatsServiceImpl 迁移，消除跨模块 Mapper 依赖） ====================

    /**
     * Phase 14：秒杀活动总数，封装 seckillGoodsMapper.selectCount(null)。
     */
    @Override
    public long countAll() {
        return seckillGoodsMapper.selectCount(null);
    }

    /**
     * Phase 14：统计进行中的秒杀活动数量。
     * <p>
     * M17: DB 中 status 字段不会随时间自动更新（创建时为 PENDING，仅取消时改为 CANCELLED），
     * 直接按 status=ACTIVE 查询会漏掉所有已开始但 status 仍为 PENDING 的活动。
     * 正确做法应基于时间窗口动态计算：start_time &lt;= now &lt; end_time 且 status != CANCELLED。
     */
    @Override
    public long countActive() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SeckillGoods::getStatus, SeckillStatus.CANCELLED)
                .le(SeckillGoods::getStartTime, now)
                .gt(SeckillGoods::getEndTime, now);
        return seckillGoodsMapper.selectCount(wrapper);
    }

    /**
     * Phase 14：统计待开始的秒杀活动数量。
     * <p>
     * M17: 基于 start_time &gt; now 且未取消动态计算，不依赖 DB status 字段。
     */
    @Override
    public long countPending() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SeckillGoods::getStatus, SeckillStatus.CANCELLED)
                .gt(SeckillGoods::getStartTime, now);
        return seckillGoodsMapper.selectCount(wrapper);
    }

    /**
     * Phase 14：统计今日已完成的秒杀活动数量。
     * <p>
     * M17: 基于 end_time &lt; now 且 endTime 在今日、未取消动态计算，不依赖 DB status 字段。
     */
    @Override
    public long countCompletedToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SeckillGoods::getStatus, SeckillStatus.CANCELLED)
                .lt(SeckillGoods::getEndTime, now)
                .ge(SeckillGoods::getEndTime, startOfDay)
                .lt(SeckillGoods::getEndTime, endOfDay);
        return seckillGoodsMapper.selectCount(wrapper);
    }
}
