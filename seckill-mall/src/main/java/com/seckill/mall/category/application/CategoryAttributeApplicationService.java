package com.seckill.mall.category.application;

import com.seckill.mall.category.api.CategoryAttributeApi;
import com.seckill.mall.category.api.dto.CategoryAttributeDTO;
import com.seckill.mall.category.application.facade.CategoryApiConverter;
import com.seckill.mall.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Category Attribute 应用服务 - 实现 {@link CategoryAttributeApi}，委托旧 {@link CategoryAttributeService}。
 *
 * <p>Strangler Pattern：本类作为新边界，内部委托旧 Service 实现，
 * 出参通过 {@link CategoryApiConverter} 从 VO 转换为 DTO，
 * 入参 {@code com.seckill.mall.dto.CategoryAttributeDTO}（请求对象）保持不变。
 *
 * @author wnj
 * @since Phase CA.4
 */
@Service
@RequiredArgsConstructor
public class CategoryAttributeApplicationService implements CategoryAttributeApi {

    private final CategoryAttributeService categoryAttributeService;

    @Override
    public List<CategoryAttributeDTO> getAttributesByCategory(Long categoryId) {
        return CategoryApiConverter.toAttrDTOList(
                categoryAttributeService.getAttributesByCategory(categoryId));
    }

    @Override
    public CategoryAttributeDTO createAttribute(com.seckill.mall.dto.CategoryAttributeDTO dto) {
        return CategoryApiConverter.toDTO(categoryAttributeService.createAttribute(dto));
    }

    @Override
    public CategoryAttributeDTO updateAttribute(Long id,
                                                com.seckill.mall.dto.CategoryAttributeDTO dto) {
        return CategoryApiConverter.toDTO(categoryAttributeService.updateAttribute(id, dto));
    }

    @Override
    public void deleteAttribute(Long id) {
        categoryAttributeService.deleteAttribute(id);
    }

    @Override
    public CategoryAttributeDTO addAttributeValue(
            Long attributeId,
            com.seckill.mall.dto.CategoryAttributeDTO.CategoryAttributeValueDTO value) {
        return CategoryApiConverter.toDTO(
                categoryAttributeService.addAttributeValue(attributeId, value));
    }
}