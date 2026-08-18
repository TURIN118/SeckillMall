package com.seckill.mall.category.api;

import com.seckill.mall.category.api.dto.CategoryAttributeDTO;

import java.util.List;

/**
 * Category Attribute 模块 Public API - 分类规格模板管理能力。
 *
 * <p>对应旧 {@code com.seckill.mall.service.CategoryAttributeService} 的方法签名，
 * 返回类型从 VO 改为 DTO，入参 {@code com.seckill.mall.dto.CategoryAttributeDTO}
 * 保持不变（HTTP 请求对象，不需要 DTO 化）。
 *
 * <p>Strangler Pattern：后续由 ApplicationService 实现本接口，委托旧 CategoryAttributeService。
 *
 * @author wnj
 * @since Phase CA.2
 */
public interface CategoryAttributeApi {

    /**
     * 获取分类的规格模板（含预设值），按 sortOrder 升序。
     * 供 ProductEdit.vue 选择分类后自动带出模板属性。
     *
     * @param categoryId 分类 ID
     * @return 分类规格模板 DTO 列表
     */
    List<CategoryAttributeDTO> getAttributesByCategory(Long categoryId);

    /**
     * 创建分类规格属性（含预设值）。
     *
     * @param dto 分类属性请求 DTO（{@code com.seckill.mall.dto.CategoryAttributeDTO}）
     * @return 创建后的分类规格模板 DTO
     */
    CategoryAttributeDTO createAttribute(com.seckill.mall.dto.CategoryAttributeDTO dto);

    /**
     * 更新分类规格属性（含预设值，先删后插）。
     *
     * @param id  属性 ID
     * @param dto 分类属性请求 DTO（{@code com.seckill.mall.dto.CategoryAttributeDTO}）
     * @return 更新后的分类规格模板 DTO
     */
    CategoryAttributeDTO updateAttribute(Long id, com.seckill.mall.dto.CategoryAttributeDTO dto);

    /**
     * 删除分类规格属性（逻辑删除，级联删除其预设值）。
     *
     * @param id 属性 ID
     */
    void deleteAttribute(Long id);

    /**
     * 为已有分类属性添加一个预设值。
     *
     * @param attributeId 属性 ID
     * @param value       预设值请求 DTO（{@code com.seckill.mall.dto.CategoryAttributeDTO.CategoryAttributeValueDTO}）
     * @return 更新后的分类规格模板 DTO
     */
    CategoryAttributeDTO addAttributeValue(Long attributeId,
                                           com.seckill.mall.dto.CategoryAttributeDTO.CategoryAttributeValueDTO value);
}