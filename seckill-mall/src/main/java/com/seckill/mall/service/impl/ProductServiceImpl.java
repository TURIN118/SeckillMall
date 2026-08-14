package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.XssCleanUtil;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.entity.Category;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.CategoryMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.service.ProductAttributeService;
import com.seckill.mall.service.ProductService;
import com.seckill.mall.service.ProductSkuService;
import com.seckill.mall.vo.ProductAttributeVO;
import com.seckill.mall.vo.ProductSkuVO;
import com.seckill.mall.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
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

    // ===== 排序字段白名单（与 ProductMapper.xml ORDER BY 支持的字段对齐）=====
    /** Mapper XML 实际支持的标准排序字段 */
    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of("price", "sales", "createTime");
    /** 前端常用别名 → Mapper 标准字段 的归一化映射 */
    private static final java.util.Map<String, String> SORT_FIELD_ALIASES = java.util.Map.of(
            "salesCount", "sales",
            "originalPrice", "price",
            "id", "createTime"
    );
    /** 默认排序字段（非法值回退） */
    private static final String DEFAULT_SORT_FIELD = "createTime";
    /** 合法排序方向 */
    private static final java.util.Set<String> ALLOWED_SORT_ORDERS = java.util.Set.of("asc", "desc");
    /** 默认排序方向（非法值回退） */
    private static final String DEFAULT_SORT_ORDER = "desc";

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final ProductAttributeService productAttributeService;
    private final ProductSkuService productSkuService;

    @Override
    public PageResult<ProductVO> listProducts(ProductQueryRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : Math.min(req.getPageSize(), 50);

        Page<Product> page = new Page<>(pageNum, pageSize);
        // 状态筛选: 指定 status 则按 status 筛选；未指定则返回所有商品(含下架)，便于后台管理
        ProductStatus statusFilter = null;
        if (req.getStatus() != null && !req.getStatus().isEmpty()) {
            // L11: ProductStatus.valueOf 对非法值抛 IllegalArgumentException，转换为业务异常
            try {
                statusFilter = ProductStatus.valueOf(req.getStatus());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品状态值非法: " + req.getStatus());
            }
        }
        // 排序字段/方向白名单过滤，防 SQL 注入；非法值回退默认值
        String sortField = sanitizeSortBy(req.getSortBy());
        String sortOrder = sanitizeSortOrder(req.getSortOrder());

        // 分类筛选：一级分类(parentId=0)展开为所有二级分类 ID 集合，按 IN 查询；
        // 二级分类保持等值查询；空子分类直接返回空结果避免 IN() 语法错误。
        Long categoryId = req.getCategoryId();
        List<Long> categoryIds = null;
        if (categoryId != null) {
            Category category = categoryMapper.selectById(categoryId);
            if (category != null) {
                Long parentId = category.getParentId();
                if (parentId == null || parentId == 0L) {
                    // 一级分类：展开子分类
                    List<Category> children = categoryMapper.selectByParentId(categoryId);
                    if (children == null || children.isEmpty()) {
                        // 一级分类下无二级分类，直接返回空结果
                        return PageResult.of(Collections.emptyList(), 0L, pageNum, pageSize);
                    }
                    categoryIds = children.stream()
                            .map(Category::getId)
                            .collect(Collectors.toList());
                    // 不再用等值
                    categoryId = null;
                }
                // 二级分类：保持 categoryId 等值查询
            }
            // 分类不存在时忽略分类筛选条件（与原行为一致：不抛错）
        }

        IPage<Product> result = productMapper.selectProductPage(
                page,
                categoryId,
                categoryIds,
                req.getKeyword(),
                statusFilter,
                req.getMinPrice(),
                req.getMaxPrice(),
                sortField,
                sortOrder);

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
                // 加载属性与 SKU（5.7.1）
                enrichWithSkuInfo(vo, product);
                // 5. 写入缓存：TTL = 30min + 随机偏移(1~5min)，防雪崩
                long ttl = BASE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(RANDOM_TTL_BOUND_SECONDS) + 1;
                stringRedisTemplate.opsForValue().set(key, serialize(vo), ttl, TimeUnit.SECONDS);
                return vo;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取商品详情被中断");
            } finally {
                // M14 修复：unlock 前校验锁仍被当前线程持有，避免锁租约过期后 unlock 抛 IllegalMonitorStateException
                if (locked && lock.isHeldByCurrentThread()) {
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
        product.setDescription(XssCleanUtil.clean(req.getDescription()));
        // 富文本内容保留原始 HTML，不做 XSS 清洗（wangEditor 已做白名单过滤）
        product.setDetailHtml(req.getDetailHtml());
        product.setOriginalPrice(req.getOriginalPrice());
        product.setStock(req.getStock());
        product.setSalesCount(0);
        product.setImages(serializeImages(req.getImages()));
        product.setStatus(req.getStatus() == null ? ProductStatus.ON_SALE : req.getStatus());

        productMapper.insert(product);

        // 5.7.1：保存属性与 SKU（创建商品时调用）
        saveAttributesAndSkus(product, req.getAttributes(), req.getSkus());

        // 商品新增后分类树 productCount 需更新，失效分类树缓存
        categoryService.evictCategoryCache();
        ProductVO vo = toProductVO(product, Map.of(req.getCategoryId(), getCategoryName(req.getCategoryId())));
        // 回填属性与 SKU 信息
        enrichWithSkuInfo(vo, product);
        return vo;
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
            product.setDescription(XssCleanUtil.clean(req.getDescription()));
        }
        // 富文本内容保留原始 HTML，不做 XSS 清洗（wangEditor 已做白名单过滤）
        if (req.getDetailHtml() != null) {
            product.setDetailHtml(req.getDetailHtml());
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

        // 5.7.1：保存属性与 SKU（更新商品时调用，内部先逻辑删除旧数据再插入新数据）
        saveAttributesAndSkus(product, req.getAttributes(), req.getSkus());

        // 更新后删除缓存，保证后续读取一致
        evictCache(id);
        // 商品分类可能变更，失效分类树缓存以更新 productCount
        categoryService.evictCategoryCache();
        ProductVO vo = toProductVO(product, Map.of(product.getCategoryId(), getCategoryName(product.getCategoryId())));
        // 回填属性与 SKU 信息
        enrichWithSkuInfo(vo, product);
        return vo;
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
        // 5.7.1：删除商品时同步逻辑删除属性与 SKU
        productAttributeService.deleteByProductId(id);
        productSkuService.deleteByProductId(id);
        evictCache(id);
        // 商品删除后分类树 productCount 需更新，失效分类树缓存
        categoryService.evictCategoryCache();
    }

    /**
     * 白名单过滤排序字段，防 SQL 注入。
     * 将前端传入的 sortBy 归一化为 Mapper 支持的标准字段(price/sales/createTime)：
     * 1. 空值 → 默认值 createTime
     * 2. 命中别名映射(如 salesCount→sales, originalPrice→price, id→createTime) → 标准字段
     * 3. 命中白名单(price/sales/createTime) → 原值
     * 4. 其他非法值 → 默认值 createTime
     */
    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_FIELD;
        }
        String trimmed = sortBy.trim();
        // 先查别名映射
        String normalized = SORT_FIELD_ALIASES.get(trimmed);
        if (normalized != null) {
            return normalized;
        }
        // 再查白名单
        if (ALLOWED_SORT_FIELDS.contains(trimmed)) {
            return trimmed;
        }
        return DEFAULT_SORT_FIELD;
    }

    /**
     * 白名单过滤排序方向，防 SQL 注入。
     * 将 sortOrder 归一化为小写 asc/desc，非法值回退为默认值 desc。
     * 兼容前端传入的 ASC/DESC 大写形式。
     */
    private String sanitizeSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return DEFAULT_SORT_ORDER;
        }
        String lower = sortOrder.trim().toLowerCase();
        if (ALLOWED_SORT_ORDERS.contains(lower)) {
            return lower;
        }
        return DEFAULT_SORT_ORDER;
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
        vo.setDetailHtml(product.getDetailHtml());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setImages(deserializeImages(product.getImages()));
        vo.setStock(product.getStock());
        vo.setSalesCount(product.getSalesCount());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    /**
     * 5.7.1：保存商品属性与 SKU，并同步刷新 t_product 的冗余字段。
     * <p>
     * 若 skus 非空，将 t_product.original_price 设为最低 SKU 价格、
     * t_product.stock 设为 SKU 库存之和，保持列表页展示一致性。
     *
     * @param product    商品实体（已含 id）
     * @param attributes 属性 DTO 列表（可空）
     * @param skus       SKU DTO 列表（可空）
     */
    private void saveAttributesAndSkus(Product product,
                                       List<com.seckill.mall.dto.ProductAttributeDTO> attributes,
                                       List<com.seckill.mall.dto.ProductSkuDTO> skus) {
        Long productId = product.getId();
        // 保存属性
        if (attributes != null && !attributes.isEmpty()) {
            productAttributeService.saveAttributes(productId, attributes);
        }
        // 保存 SKU
        if (skus != null && !skus.isEmpty()) {
            productSkuService.saveSkus(productId, skus);
            // 有 SKU 时：t_product.original_price 设为最低 SKU 价格、stock 设为 SKU 库存之和
            BigDecimal minPrice = productSkuService.calculateMinPrice(productId);
            Integer totalStock = productSkuService.calculateTotalStock(productId);
            BigDecimal maxPrice = productSkuService.calculateMaxPrice(productId);
            product.setOriginalPrice(minPrice);
            product.setStock(totalStock);
            product.setMinPrice(minPrice);
            product.setMaxPrice(maxPrice);
            product.setTotalStock(totalStock);
            productMapper.updateById(product);
        } else {
            // 无 SKU 时：冗余字段与 originalPrice / stock 保持一致
            product.setMinPrice(product.getOriginalPrice());
            product.setMaxPrice(product.getOriginalPrice());
            product.setTotalStock(product.getStock());
            productMapper.updateById(product);
        }
    }

    /**
     * 5.7.1：为 ProductVO 填充属性、SKU、hasSku、minPrice、maxPrice、totalStock 字段。
     *
     * @param vo      商品视图
     * @param product 商品实体
     */
    private void enrichWithSkuInfo(ProductVO vo, Product product) {
        List<ProductAttributeVO> attributes = productAttributeService.listByProductId(product.getId());
        List<ProductSkuVO> skus = productSkuService.listEnabledByProductId(product.getId());
        vo.setAttributes(attributes);
        vo.setSkus(skus);
        vo.setHasSku(!skus.isEmpty());
        if (!skus.isEmpty()) {
            vo.setMinPrice(skus.stream().map(ProductSkuVO::getPrice).min(BigDecimal::compareTo)
                    .orElse(product.getOriginalPrice()));
            vo.setMaxPrice(skus.stream().map(ProductSkuVO::getPrice).max(BigDecimal::compareTo)
                    .orElse(product.getOriginalPrice()));
            vo.setTotalStock(skus.stream().mapToInt(ProductSkuVO::getStock).sum());
        } else {
            vo.setMinPrice(product.getOriginalPrice());
            vo.setMaxPrice(product.getOriginalPrice());
            vo.setTotalStock(product.getStock());
        }
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
