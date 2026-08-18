package com.seckill.mall.service;

import com.seckill.mall.category.infrastructure.entity.CategoryAttributeValue;

import java.util.List;

/**
 * 分类属性预设值服务接口
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeValueService.java
 * 邮箱：nj651217@163.com
 */
public interface CategoryAttributeValueService {

    /** 按属性 ID 查所有预设值（按 sortOrder 升序） */
    List<CategoryAttributeValue> listByAttributeId(Long attributeId);

    /** 批量保存属性的预设值（先逻辑删除旧值再插入新值） */
    void saveValues(Long attributeId, List<CategoryAttributeValue> values);

    /** 逻辑删除属性的所有预设值 */
    void deleteByAttributeId(Long attributeId);
}