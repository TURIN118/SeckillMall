package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.category.infrastructure.entity.CategoryAttributeValue;
import com.seckill.mall.category.infrastructure.mapper.CategoryAttributeValueMapper;
import com.seckill.mall.service.CategoryAttributeValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类属性预设值服务实现
 * <p>
 * 实现要点：
 * <ul>
 *   <li>listByAttributeId：按 attributeId 查询，sortOrder 升序</li>
 *   <li>saveValues：事务内先逻辑删除旧值，再插入新值</li>
 *   <li>deleteByAttributeId：逻辑删除属性的所有预设值</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeValueServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAttributeValueServiceImpl implements CategoryAttributeValueService {

    private final CategoryAttributeValueMapper categoryAttributeValueMapper;

    @Override
    public List<CategoryAttributeValue> listByAttributeId(Long attributeId) {
        if (attributeId == null) {
            return new ArrayList<>();
        }
        return categoryAttributeValueMapper.selectList(
                new LambdaQueryWrapper<CategoryAttributeValue>()
                        .eq(CategoryAttributeValue::getAttributeId, attributeId)
                        .orderByAsc(CategoryAttributeValue::getSortOrder)
                        .orderByAsc(CategoryAttributeValue::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveValues(Long attributeId, List<CategoryAttributeValue> values) {
        // 先逻辑删除旧值
        deleteByAttributeId(attributeId);
        if (values == null || values.isEmpty()) {
            return;
        }
        // 再插入新值
        int sort = 0;
        for (CategoryAttributeValue value : values) {
            value.setId(null);
            value.setAttributeId(attributeId);
            if (value.getSortOrder() == null) {
                value.setSortOrder(sort++);
            }
            categoryAttributeValueMapper.insert(value);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAttributeId(Long attributeId) {
        if (attributeId == null) {
            return;
        }
        categoryAttributeValueMapper.delete(new LambdaUpdateWrapper<CategoryAttributeValue>()
                .eq(CategoryAttributeValue::getAttributeId, attributeId));
    }
}