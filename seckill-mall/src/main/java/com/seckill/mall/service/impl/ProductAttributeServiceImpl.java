package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.dto.ProductAttributeDTO;
import com.seckill.mall.product.infrastructure.entity.ProductAttribute;
import com.seckill.mall.product.infrastructure.entity.ProductAttributeValue;
import com.seckill.mall.product.domain.AttributeType;
import com.seckill.mall.product.infrastructure.mapper.ProductAttributeMapper;
import com.seckill.mall.product.infrastructure.mapper.ProductAttributeValueMapper;
import com.seckill.mall.service.ProductAttributeService;
import com.seckill.mall.vo.ProductAttributeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品属性服务实现
 * <p>
 * 实现要点：
 * <ul>
 *   <li>listByProductId：先查 t_product_attribute（按 sortOrder 升序），
 *       再批量查 t_product_attribute_value（按 sortOrder 升序），内存按 attributeId 分组组装</li>
 *   <li>saveAttributes：事务内先调用 deleteByProductId 逻辑删除旧属性与属性值，
 *       再遍历 attributes 列表插入新属性，对每个属性插入其 values 列表（设置 productId 冗余字段）</li>
 *   <li>deleteByProductId：使用 LambdaUpdateWrapper 逻辑删除 t_product_attribute 与
 *       t_product_attribute_value 中 product_id 匹配的记录</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttributeServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeMapper attributeMapper;
    private final ProductAttributeValueMapper attributeValueMapper;

    @Override
    public List<ProductAttributeVO> listByProductId(Long productId) {
        if (productId == null) {
            return new ArrayList<>();
        }
        // 1. 查所有属性，按 sortOrder 升序
        List<ProductAttribute> attrs = attributeMapper.selectList(
                new LambdaQueryWrapper<ProductAttribute>()
                        .eq(ProductAttribute::getProductId, productId)
                        .orderByAsc(ProductAttribute::getSortOrder)
                        .orderByAsc(ProductAttribute::getId));
        if (attrs.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查所有属性值，按 sortOrder 升序
        List<ProductAttributeValue> vals = attributeValueMapper.selectList(
                new LambdaQueryWrapper<ProductAttributeValue>()
                        .eq(ProductAttributeValue::getProductId, productId)
                        .orderByAsc(ProductAttributeValue::getSortOrder)
                        .orderByAsc(ProductAttributeValue::getId));

        // 3. 按 attributeId 分组
        Map<Long, List<ProductAttributeValue>> valMap = vals.stream()
                .collect(Collectors.groupingBy(ProductAttributeValue::getAttributeId));

        // 4. 组装 VO
        return attrs.stream().map(a -> {
            ProductAttributeVO vo = new ProductAttributeVO();
            vo.setId(a.getId());
            vo.setCategoryAttributeId(a.getCategoryAttributeId());
            vo.setName(a.getName());
            vo.setType(a.getType() != null ? a.getType().name() : null);
            vo.setSortOrder(a.getSortOrder());
            vo.setValues(valMap.getOrDefault(a.getId(), new ArrayList<>()).stream().map(v -> {
                ProductAttributeVO.AttributeValueVO vvo = new ProductAttributeVO.AttributeValueVO();
                vvo.setId(v.getId());
                vvo.setValue(v.getValue());
                vvo.setImageUrl(v.getImageUrl());
                vvo.setSortOrder(v.getSortOrder());
                return vvo;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttributes(Long productId, List<ProductAttributeDTO> attributes) {
        deleteByProductId(productId);
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        int attrSort = 0;
        for (ProductAttributeDTO dto : attributes) {
            ProductAttribute attr = new ProductAttribute();
            attr.setProductId(productId);
            attr.setCategoryAttributeId(dto.getCategoryAttributeId());
            attr.setName(dto.getName());
            attr.setType(AttributeType.valueOf(dto.getType()));
            attr.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : attrSort++);
            attributeMapper.insert(attr);

            if (dto.getValues() != null) {
                int valSort = 0;
                for (ProductAttributeDTO.AttributeValueDTO vdto : dto.getValues()) {
                    ProductAttributeValue val = new ProductAttributeValue();
                    val.setAttributeId(attr.getId());
                    val.setProductId(productId);
                    val.setValue(vdto.getValue());
                    val.setImageUrl(vdto.getImageUrl());
                    val.setSortOrder(vdto.getSortOrder() != null ? vdto.getSortOrder() : valSort++);
                    attributeValueMapper.insert(val);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(Long productId) {
        if (productId == null) {
            return;
        }
        // 逻辑删除属性值
        attributeValueMapper.delete(new LambdaUpdateWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, productId));
        // 逻辑删除属性
        attributeMapper.delete(new LambdaUpdateWrapper<ProductAttribute>()
                .eq(ProductAttribute::getProductId, productId));
    }
}