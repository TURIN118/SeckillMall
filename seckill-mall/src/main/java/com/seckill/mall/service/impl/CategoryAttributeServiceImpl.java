package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.dto.CategoryAttributeDTO;
import com.seckill.mall.entity.CategoryAttribute;
import com.seckill.mall.entity.CategoryAttributeValue;
import com.seckill.mall.entity.enums.AttributeInputType;
import com.seckill.mall.entity.enums.AttributeType;
import com.seckill.mall.mapper.CategoryAttributeMapper;
import com.seckill.mall.mapper.CategoryAttributeValueMapper;
import com.seckill.mall.service.CategoryAttributeService;
import com.seckill.mall.service.CategoryAttributeValueService;
import com.seckill.mall.vo.CategoryAttributeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类规格模板服务实现
 * <p>
 * 实现要点：
 * <ul>
 *   <li>getAttributesByCategory：先查 t_category_attribute（按 sortOrder 升序），
 *       再批量查 t_category_attribute_value（按 sortOrder 升序），内存按 attributeId 分组组装</li>
 *   <li>createAttribute：插入 t_category_attribute，再遍历 values 插入 t_category_attribute_value</li>
 *   <li>updateAttribute：事务内更新 t_category_attribute，调用 saveValues 先删后插预设值</li>
 *   <li>deleteAttribute：事务内逻辑删除 t_category_attribute 与其所有 t_category_attribute_value</li>
 *   <li>addAttributeValue：单条插入 t_category_attribute_value，返回更新后的属性 VO</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    private final CategoryAttributeMapper categoryAttributeMapper;
    private final CategoryAttributeValueMapper categoryAttributeValueMapper;
    private final CategoryAttributeValueService categoryAttributeValueService;

    @Override
    public List<CategoryAttributeVO> getAttributesByCategory(Long categoryId) {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        // 1. 查所有属性，按 sortOrder 升序
        List<CategoryAttribute> attrs = categoryAttributeMapper.selectList(
                new LambdaQueryWrapper<CategoryAttribute>()
                        .eq(CategoryAttribute::getCategoryId, categoryId)
                        .orderByAsc(CategoryAttribute::getSortOrder)
                        .orderByAsc(CategoryAttribute::getId));
        if (attrs.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查所有预设值，按 sortOrder 升序
        List<Long> attrIds = attrs.stream().map(CategoryAttribute::getId).collect(Collectors.toList());
        List<CategoryAttributeValue> vals = categoryAttributeValueMapper.selectList(
                new LambdaQueryWrapper<CategoryAttributeValue>()
                        .in(CategoryAttributeValue::getAttributeId, attrIds)
                        .orderByAsc(CategoryAttributeValue::getSortOrder)
                        .orderByAsc(CategoryAttributeValue::getId));

        // 3. 按 attributeId 分组
        Map<Long, List<CategoryAttributeValue>> valMap = vals.stream()
                .collect(Collectors.groupingBy(CategoryAttributeValue::getAttributeId));

        // 4. 组装 VO
        return attrs.stream().map(a -> toVO(a, valMap.getOrDefault(a.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAttributeVO createAttribute(CategoryAttributeDTO dto) {
        CategoryAttribute attr = new CategoryAttribute();
        attr.setCategoryId(dto.getCategoryId());
        attr.setName(dto.getName());
        attr.setType(AttributeType.valueOf(dto.getType()));
        attr.setInputType(AttributeInputType.valueOf(dto.getInputType()));
        attr.setIsRequired(dto.getIsRequired());
        attr.setSortOrder(dto.getSortOrder());
        categoryAttributeMapper.insert(attr);

        // 插入预设值
        if (dto.getValues() != null && !dto.getValues().isEmpty()) {
            List<CategoryAttributeValue> values = dto.getValues().stream().map(vdto -> {
                CategoryAttributeValue val = new CategoryAttributeValue();
                val.setValue(vdto.getValue());
                val.setImageUrl(vdto.getImageUrl());
                val.setSortOrder(vdto.getSortOrder());
                return val;
            }).collect(Collectors.toList());
            categoryAttributeValueService.saveValues(attr.getId(), values);
        }

        return toVO(attr, categoryAttributeValueService.listByAttributeId(attr.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAttributeVO updateAttribute(Long id, CategoryAttributeDTO dto) {
        CategoryAttribute attr = categoryAttributeMapper.selectById(id);
        if (attr == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类规格属性不存在");
        }
        attr.setCategoryId(dto.getCategoryId());
        attr.setName(dto.getName());
        attr.setType(AttributeType.valueOf(dto.getType()));
        attr.setInputType(AttributeInputType.valueOf(dto.getInputType()));
        attr.setIsRequired(dto.getIsRequired());
        attr.setSortOrder(dto.getSortOrder());
        categoryAttributeMapper.updateById(attr);

        // 先删后插预设值
        List<CategoryAttributeValue> values = new ArrayList<>();
        if (dto.getValues() != null) {
            values = dto.getValues().stream().map(vdto -> {
                CategoryAttributeValue val = new CategoryAttributeValue();
                val.setValue(vdto.getValue());
                val.setImageUrl(vdto.getImageUrl());
                val.setSortOrder(vdto.getSortOrder());
                return val;
            }).collect(Collectors.toList());
        }
        categoryAttributeValueService.saveValues(id, values);

        return toVO(attr, categoryAttributeValueService.listByAttributeId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttribute(Long id) {
        // 逻辑删除属性
        categoryAttributeMapper.deleteById(id);
        // 级联逻辑删除预设值
        categoryAttributeValueService.deleteByAttributeId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAttributeVO addAttributeValue(Long attributeId,
                                                 CategoryAttributeDTO.CategoryAttributeValueDTO value) {
        CategoryAttribute attr = categoryAttributeMapper.selectById(attributeId);
        if (attr == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类规格属性不存在");
        }
        CategoryAttributeValue val = new CategoryAttributeValue();
        val.setValue(value.getValue());
        val.setImageUrl(value.getImageUrl());
        val.setSortOrder(value.getSortOrder());
        categoryAttributeValueService.saveValues(attributeId, List.of(val));

        return toVO(attr, categoryAttributeValueService.listByAttributeId(attributeId));
    }

    /**
     * 组装 VO
     */
    private CategoryAttributeVO toVO(CategoryAttribute attr, List<CategoryAttributeValue> values) {
        CategoryAttributeVO vo = new CategoryAttributeVO();
        vo.setId(attr.getId());
        vo.setCategoryId(attr.getCategoryId());
        vo.setName(attr.getName());
        vo.setType(attr.getType() != null ? attr.getType().name() : null);
        vo.setInputType(attr.getInputType() != null ? attr.getInputType().name() : null);
        vo.setIsRequired(attr.getIsRequired());
        vo.setSortOrder(attr.getSortOrder());
        vo.setValues(values.stream().map(v -> {
            CategoryAttributeVO.CategoryAttributeValueVO vvo = new CategoryAttributeVO.CategoryAttributeValueVO();
            vvo.setId(v.getId());
            vvo.setValue(v.getValue());
            vvo.setImageUrl(v.getImageUrl());
            vvo.setSortOrder(v.getSortOrder());
            return vvo;
        }).collect(Collectors.toList()));
        return vo;
    }
}