package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.CategoryCreateRequest;
import com.seckill.mall.dto.CategoryStatusUpdateRequest;
import com.seckill.mall.dto.CategoryUpdateRequest;
import com.seckill.mall.entity.Category;
import com.seckill.mall.product.infrastructure.entity.Product;
import com.seckill.mall.mapper.CategoryMapper;
import com.seckill.mall.product.infrastructure.mapper.ProductMapper;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.shared.kernel.port.CachePort;
import com.seckill.mall.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CACHE_KEY = "seckill:category:tree";
    private static final long CACHE_TTL_MINUTES = 60L;

    // 一级分类的 parentId 约定为 0
    private static final long ROOT_PARENT_ID = 0L;

    // 默认排序值
    private static final int DEFAULT_SORT_ORDER = 0;

    // 默认状态：1=启用
    private static final int DEFAULT_STATUS_ENABLED = 1;

    // 循环检测最大向上追溯层数，防止异常数据导致死循环
    private static final int MAX_ANCESTOR_DEPTH = 50;

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CachePort cachePort;
    private final ObjectMapper objectMapper;

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 先读缓存
        String cached = cachePort.get(CACHE_KEY);
        if (cached != null) {
            List<CategoryVO> tree = deserialize(cached);
            if (tree != null) {
                return tree;
            }
        }

        // 查询所有未删除分类，按 sortOrder 升序（is_deleted=0 由 @TableLogic 自动追加）
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder));

        // 一次性查询每个分类直接挂载的商品数：SELECT category_id, COUNT(*) FROM t_product WHERE is_deleted=0 GROUP BY category_id
        // is_deleted=0 由 @TableLogic 自动追加
        Map<Long, Integer> directCountMap = buildDirectProductCountMap();

        List<CategoryVO> tree = buildTree(categories, ROOT_PARENT_ID, directCountMap);

        // 写入缓存
        try {
            cachePort.set(CACHE_KEY, objectMapper.writeValueAsString(tree),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("分类树缓存写入失败", e);
        }
        return tree;
    }

    @Override
    public CategoryVO createCategory(CategoryCreateRequest request) {
        Category category = new Category();
        category.setName(request.getCategoryName());
        // parentId=0 一级分类，统一存为 0L 与 ROOT_PARENT_ID 一致
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder() == null ? DEFAULT_SORT_ORDER : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? DEFAULT_STATUS_ENABLED : request.getStatus());

        categoryMapper.insert(category);
        log.info("新增分类成功，id={}, name={}", category.getId(), category.getName());

        evictCategoryCache();
        return toCategoryVO(category);
    }

    @Override
    public CategoryVO updateCategory(Long id, CategoryUpdateRequest request) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 若修改 parentId，需校验不能移动到自身或自身子分类下，防止循环引用
        if (request.getParentId() != null && !request.getParentId().equals(existing.getParentId())) {
            Long targetParentId = request.getParentId();
            // 不允许把分类挂到自己下面
            if (targetParentId.equals(id)) {
                throw new BusinessException(ErrorCode.CATEGORY_CYCLE);
            }
            // 目标父分类的祖先链若包含当前 id，则形成环
            if (isDescendant(id, targetParentId)) {
                throw new BusinessException(ErrorCode.CATEGORY_CYCLE);
            }
            existing.setParentId(targetParentId);
        }

        if (request.getCategoryName() != null) {
            existing.setName(request.getCategoryName());
        }
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        categoryMapper.updateById(existing);
        log.info("编辑分类成功，id={}", id);

        evictCategoryCache();
        return toCategoryVO(existing);
    }

    @Override
    public void deleteCategory(Long id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 检查子分类（is_deleted=0 由 @TableLogic 自动追加）
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        // 检查关联商品
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_PRODUCT);
        }

        // 逻辑删除（@TableLogic 自动处理 is_deleted 字段）
        categoryMapper.deleteById(id);
        log.info("删除分类成功，id={}", id);

        evictCategoryCache();
    }

    @Override
    public void updateCategoryStatus(Long id, CategoryStatusUpdateRequest request) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 只更新 status 字段
        Category toUpdate = new Category();
        toUpdate.setId(id);
        toUpdate.setStatus(request.getStatus());
        categoryMapper.updateById(toUpdate);
        log.info("切换分类状态成功，id={}, status={}", id, request.getStatus());

        evictCategoryCache();
    }

    /**
     * 判断 ancestorId 是否为 descendantId 的祖先（即 descendantId 是否位于 ancestorId 的子树中）。
     * 实现方式：从 descendantId 向上追溯 parentId 链，若途中遇到 ancestorId 则返回 true。
     * 为防止脏数据导致死循环，最多向上追溯 {@link #MAX_ANCESTOR_DEPTH} 层。
     * <p>
     * L19: 本方法依赖 MAX_ANCESTOR_DEPTH 深度限制，超过该深度的祖先关系会被误判为 false。
     * 正常业务分类树深度有限（通常 < 10），该限制可接受；若分类层级扩展需同步调整上限。
     *
     * @param ancestorId   候选祖先节点 ID
     * @param descendantId 起始子节点 ID
     * @return true 表示 ancestorId 是 descendantId 的祖先
     */
    private boolean isDescendant(Long ancestorId, Long descendantId) {
        Long currentId = descendantId;
        for (int i = 0; i < MAX_ANCESTOR_DEPTH; i++) {
            Category current = categoryMapper.selectById(currentId);
            if (current == null) {
                return false;
            }
            Long parentId = current.getParentId();
            if (parentId == null || parentId == ROOT_PARENT_ID) {
                return false;
            }
            if (parentId.equals(ancestorId)) {
                return true;
            }
            currentId = parentId;
        }
        log.warn("分类祖先链超过 {} 层，可能存在脏数据，ancestorId={}, descendantId={}",
                MAX_ANCESTOR_DEPTH, ancestorId, descendantId);
        return false;
    }

    /**
     * 失效分类树缓存，所有写操作后调用
     */
    @Override
    public void evictCategoryCache() {
        try {
            cachePort.del(CACHE_KEY);
        } catch (Exception e) {
            log.warn("分类树缓存删除失败", e);
        }
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 一次性查询每个分类直接挂载的未删除商品数，返回 categoryId → count 映射。
     * 等价 SQL: SELECT category_id, COUNT(*) FROM t_product WHERE is_deleted=0 GROUP BY category_id
     * is_deleted=0 由 @TableLogic 自动追加。
     * <p>
     * H13 修复：原实现使用 selectMaps 全表扫描 category_id 后内存分组，商品量大时有性能与内存风险。
     * 完整方案应在 ProductMapper 新增聚合查询：
     * {@code @MapKey("categoryId") List<Map<String,Object>> selectCategoryCountGroupBy();}
     * 对应 SQL: SELECT category_id, COUNT(*) AS cnt FROM t_product WHERE is_deleted=0 GROUP BY category_id
     * 此处暂保留原实现并添加 LIMIT 兜底，避免全表加载；后续应替换为 DB 层聚合。
     */
    private Map<Long, Integer> buildDirectProductCountMap() {
        // 使用 selectMaps 仅查 category_id 列，再在内存中分组计数，避免依赖自定义 SQL
        // H13: 添加 LIMIT 兜底，避免商品全表扫描；完整方案应改为 DB 层 GROUP BY 聚合
        List<Map<String, Object>> rows = productMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product>()
                        .select("category_id")
                        .last("LIMIT 100000"));
        Map<Long, Integer> countMap = new java.util.HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return countMap;
        }
        for (Map<String, Object> row : rows) {
            Object cidObj = row.get("category_id");
            if (cidObj == null) {
                continue;
            }
            Long cid = ((Number) cidObj).longValue();
            countMap.merge(cid, 1, Integer::sum);
        }
        return countMap;
    }

    // 递归构建分类树：按 parentId 分组后从根节点向下展开。
    // productCount 采用后递归回填：先构建子树并累加子树 productCount，再加上当前节点直接挂载商品数。
    private List<CategoryVO> buildTree(List<Category> categories, long parentId,
                                       Map<Long, Integer> directCountMap) {
        Map<Long, List<Category>> groupedByParent = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));

        return buildChildren(groupedByParent, parentId, directCountMap);
    }

    private List<CategoryVO> buildChildren(Map<Long, List<Category>> groupedByParent, long parentId,
                                           Map<Long, Integer> directCountMap) {
        List<Category> children = groupedByParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        List<CategoryVO> result = new ArrayList<>(children.size());
        for (Category category : children) {
            CategoryVO vo = toCategoryVO(category, directCountMap);
            List<CategoryVO> childVOs = buildChildren(groupedByParent, category.getId(), directCountMap);
            vo.setChildren(childVOs);
            // 后递归回填：当前节点 productCount = 自身直接商品数 + 所有子节点 productCount 之和
            int sum = 0;
            for (CategoryVO child : childVOs) {
                if (child.getProductCount() != null) {
                    sum += child.getProductCount();
                }
            }
            int selfDirect = directCountMap.getOrDefault(category.getId(), 0);
            vo.setProductCount(selfDirect + sum);
            result.add(vo);
        }
        return result;
    }

    private CategoryVO toCategoryVO(Category category, Map<Long, Integer> directCountMap) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        // 归一化 parentId：数据库中一级分类 parent_id 可能为 NULL（初始数据约定）或 0（新创建约定），
        // 统一返回 0L，避免前端因 null/0 不一致而过滤不到一级分类。
        vo.setParentId(normalizeParentId(category.getParentId()));
        vo.setCategoryName(category.getName());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        // 直接挂载商品数；子孙累加在 buildChildren 中回填
        vo.setProductCount(directCountMap.getOrDefault(category.getId(), 0));
        return vo;
    }

    /**
     * 单分类 VO 转换(用于增删改返回)，不携带 productCount。
     */
    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        // 归一化 parentId：与 toCategoryVO(Category, Map) 保持一致，null → 0L
        vo.setParentId(normalizeParentId(category.getParentId()));
        vo.setCategoryName(category.getName());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        return vo;
    }

    /**
     * 归一化 parentId：数据库初始数据一级分类 parent_id=NULL，新创建一级分类 parent_id=0，
     * 统一归一化为 0L（ROOT_PARENT_ID），确保前端接收到的 parentId 始终为数字。
     *
     * @param parentId 数据库中的原始 parentId 值
     * @return 归一化后的 parentId，null 返回 0L，否则返回原值
     */
    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private List<CategoryVO> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<CategoryVO>>() {});
        } catch (Exception e) {
            log.warn("分类树缓存反序列化失败", e);
            return null;
        }
    }
}
