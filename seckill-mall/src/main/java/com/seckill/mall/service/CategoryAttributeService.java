package com.seckill.mall.service;

import com.seckill.mall.dto.CategoryAttributeDTO;
import com.seckill.mall.vo.CategoryAttributeVO;

import java.util.List;

/**
 * 分类规格模板服务接口
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeService.java
 * 邮箱：nj651217@163.com
 */
public interface CategoryAttributeService {

    /**
     * 获取分类的规格模板（含预设值），按 sortOrder 升序
     * 供 ProductEdit.vue 选择分类后自动带出模板属性
     */
    List<CategoryAttributeVO> getAttributesByCategory(Long categoryId);

    /**
     * 创建分类规格属性（含预设值）
     */
    CategoryAttributeVO createAttribute(CategoryAttributeDTO dto);

    /**
     * 更新分类规格属性（含预设值，先删后插）
     */
    CategoryAttributeVO updateAttribute(Long id, CategoryAttributeDTO dto);

    /**
     * 删除分类规格属性（逻辑删除，级联删除其预设值）
     */
    void deleteAttribute(Long id);

    /**
     * 为已有分类属性添加一个预设值
     */
    CategoryAttributeVO addAttributeValue(Long attributeId,
                                          CategoryAttributeDTO.CategoryAttributeValueDTO value);
}