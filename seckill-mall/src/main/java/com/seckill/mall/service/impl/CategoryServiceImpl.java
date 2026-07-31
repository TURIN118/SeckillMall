package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.entity.Category;
import com.seckill.mall.mapper.CategoryMapper;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 先读缓存
        String cached = stringRedisTemplate.opsForValue().get(CACHE_KEY);
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

        List<CategoryVO> tree = buildTree(categories, ROOT_PARENT_ID);

        // 写入缓存
        try {
            stringRedisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(tree),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("分类树缓存写入失败", e);
        }
        return tree;
    }

    // 递归构建分类树：按 parentId 分组后从根节点向下展开
    private List<CategoryVO> buildTree(List<Category> categories, long parentId) {
        Map<Long, List<Category>> groupedByParent = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));

        return buildChildren(groupedByParent, parentId);
    }

    private List<CategoryVO> buildChildren(Map<Long, List<Category>> groupedByParent, long parentId) {
        List<Category> children = groupedByParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        List<CategoryVO> result = new ArrayList<>(children.size());
        for (Category category : children) {
            CategoryVO vo = toCategoryVO(category);
            vo.setChildren(buildChildren(groupedByParent, category.getId()));
            result.add(vo);
        }
        return result;
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setCategoryName(category.getName());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        return vo;
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
