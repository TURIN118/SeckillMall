package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.entity.Category;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.CategoryMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.service.ProductService;
import com.seckill.mall.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_KEY_PREFIX = "seckill:goods:";
    private static final String LOCK_KEY_PREFIX = "lock:goods:";
    private static final String NULL_MARKER = "NULL";

    // 真实数据基础 TTL（秒）：30min
    private static final long BASE_TTL_SECONDS = 30L * 60L;
    // TTL 随机偏移上限（秒）：5min
    private static final int RANDOM_TTL_BOUND_SECONDS = 5 * 60;
    // 空值缓存 TTL（秒）：2min，防缓存穿透
    private static final long NULL_TTL_SECONDS = 2L * 60L;

    // 互斥锁重试参数（防缓存击穿）
    private static final int MAX_RETRY = 3;
    private static final long RETRY_SLEEP_MS = 50L;
    private static final long LOCK_HOLD_SECONDS = 10L;

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<ProductVO> listProducts(ProductQueryRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : Math.min(req.getPageSize(), 50);

        Page<Product> page = new Page<>(pageNum, pageSize);
        // 公开列表仅展示在售商品
        IPage<Product> result = productMapper.selectProductPage(
                page,
                req.getCategoryId(),
                req.getKeyword(),
                ProductStatus.ON_SALE,
                req.getSortBy(),
                req.getSortOrder());

        List<Product> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), pageNum, pageSize);
        }

        // 批量查询分类名称，避免 N+1
        Map<Long, String> categoryNameMap = buildCategoryNameMap(records);

        List<ProductVO> voList = records.stream()
                .map(p -> toProductVO(p, categoryNameMap))
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        String key = CACHE_KEY_PREFIX + id;

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            String cached = stringRedisTemplate.opsForValue().get(key);

            // 1. 缓存命中：区分空值标记与真实数据
            if (cached != null) {
                if (NULL_MARKER.equals(cached)) {
                    throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                ProductVO vo = deserialize(cached);
                if (vo != null) {
                    return vo;
                }
            }

            // 2. 缓存未命中：获取互斥锁，防止热点 Key 击穿
            RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + id);
            boolean locked = false;
            try {
                locked = lock.tryLock(0, LOCK_HOLD_SECONDS, TimeUnit.SECONDS);
                if (!locked) {
                    // 获取锁失败：等待后重试
                    Thread.sleep(RETRY_SLEEP_MS);
                    continue;
                }

                // 3. Double Check：拿到锁后再查一次缓存
                cached = stringRedisTemplate.opsForValue().get(key);
                if (cached != null) {
                    if (NULL_MARKER.equals(cached)) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                    }
                    ProductVO vo = deserialize(cached);
                    if (vo != null) {
                        return vo;
                    }
                }

                // 4. 仍未命中：查询数据库
                Product product = productMapper.selectById(id);
                if (product == null) {
                    // 数据库不存在 → 缓存空值标记防穿透
                    stringRedisTemplate.opsForValue().set(key, NULL_MARKER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
                    throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                ProductVO vo = toProductVO(product, buildCategoryNameMap(List.of(product)));
                // 5. 写入缓存：TTL = 30min + 随机偏移(1~5min)，防雪崩
                long ttl = BASE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(RANDOM_TTL_BOUND_SECONDS) + 1;
                stringRedisTemplate.opsForValue().set(key, serialize(vo), ttl, TimeUnit.SECONDS);
                return vo;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取商品详情被中断");
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        }
        // 超过最大重试次数
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取商品详情失败，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO createProduct(ProductCreateRequest req) {
        validateCategory(req.getCategoryId());

        Product product = new Product();
        product.setName(req.getProductName());
        product.setCategoryId(req.getCategoryId());
        product.setDescription(req.getDescription());
        product.setOriginalPrice(req.getOriginalPrice());
        product.setStock(req.getStock());
        product.setSalesCount(0);
        product.setImages(serializeImages(req.getImages()));
        product.setStatus(req.getStatus() == null ? ProductStatus.ON_SALE : req.getStatus());

        productMapper.insert(product);
        return toProductVO(product, Map.of(req.getCategoryId(), getCategoryName(req.getCategoryId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVO updateProduct(Long id, ProductUpdateRequest req) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (req.getCategoryId() != null) {
            validateCategory(req.getCategoryId());
            product.setCategoryId(req.getCategoryId());
        }
        if (req.getProductName() != null) {
            product.setName(req.getProductName());
        }
        if (req.getDescription() != null) {
            product.setDescription(req.getDescription());
        }
        if (req.getOriginalPrice() != null) {
            product.setOriginalPrice(req.getOriginalPrice());
        }
        if (req.getStock() != null) {
            product.setStock(req.getStock());
        }
        if (req.getImages() != null) {
            product.setImages(serializeImages(req.getImages()));
        }
        if (req.getStatus() != null) {
            product.setStatus(req.getStatus());
        }

        productMapper.updateById(product);
        // 更新后删除缓存，保证后续读取一致
        evictCache(id);
        return toProductVO(product, Map.of(product.getCategoryId(), getCategoryName(product.getCategoryId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // MyBatis-Plus @TableLogic 自动转为逻辑删除
        productMapper.deleteById(id);
        evictCache(id);
    }

    private void validateCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (category.getStatus() != null && category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CATEGORY_DISABLED);
        }
    }

    private void evictCache(Long id) {
        stringRedisTemplate.delete(CACHE_KEY_PREFIX + id);
    }

    private Map<Long, String> buildCategoryNameMap(List<Product> products) {
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        return categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private ProductVO toProductVO(Product product, Map<Long, String> categoryNameMap) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setProductName(product.getName());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryNameMap.getOrDefault(product.getCategoryId(), null));
        vo.setDescription(product.getDescription());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setImages(deserializeImages(product.getImages()));
        vo.setStock(product.getStock());
        vo.setSalesCount(product.getSalesCount());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (Exception e) {
            log.warn("序列化图片列表失败", e);
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

    private String serialize(ProductVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            log.warn("序列化 ProductVO 失败", e);
            return null;
        }
    }

    private ProductVO deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProductVO.class);
        } catch (Exception e) {
            log.warn("反序列化 ProductVO 失败", e);
            return null;
        }
    }
}
