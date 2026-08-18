package com.seckill.mall.category.application;

import com.seckill.mall.category.api.CategoryApi;
import com.seckill.mall.category.api.dto.CategoryDTO;
import com.seckill.mall.category.application.facade.CategoryApiConverter;
import com.seckill.mall.dto.CategoryCreateRequest;
import com.seckill.mall.dto.CategoryStatusUpdateRequest;
import com.seckill.mall.dto.CategoryUpdateRequest;
import com.seckill.mall.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Category 应用服务 - 实现 {@link CategoryApi}，委托旧 {@link CategoryService}。
 *
 * <p>Strangler Pattern：本类作为新边界，内部委托旧 Service 实现，
 * 出参通过 {@link CategoryApiConverter} 从 VO/Entity 转换为 DTO，
 * 入参 Request 对象保持不变（HTTP 请求对象，不需要 DTO 化）。
 *
 * <p>后续可逐步将业务逻辑从旧 ServiceImpl 迁移到本类或领域层，
 * 迁移完成后删除旧 Service 与本类的委托关系。
 *
 * @author wnj
 * @since Phase CA.4
 */
@Service
@RequiredArgsConstructor
public class CategoryApplicationService implements CategoryApi {

    private final CategoryService categoryService;

    @Override
    public List<CategoryDTO> getCategoryTree() {
        return CategoryApiConverter.toDTOList(categoryService.getCategoryTree());
    }

    @Override
    public CategoryDTO createCategory(CategoryCreateRequest request) {
        return CategoryApiConverter.toDTO(categoryService.createCategory(request));
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryUpdateRequest request) {
        return CategoryApiConverter.toDTO(categoryService.updateCategory(id, request));
    }

    @Override
    public void deleteCategory(Long id) {
        categoryService.deleteCategory(id);
    }

    @Override
    public void updateCategoryStatus(Long id, CategoryStatusUpdateRequest request) {
        categoryService.updateCategoryStatus(id, request);
    }

    @Override
    public void evictCategoryCache() {
        categoryService.evictCategoryCache();
    }

    @Override
    public CategoryDTO getCategoryById(Long id) {
        return CategoryApiConverter.toDTO(categoryService.getCategoryById(id));
    }
}