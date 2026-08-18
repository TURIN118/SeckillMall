package com.seckill.mall.category.api;

import com.seckill.mall.category.api.dto.CategoryDTO;
import com.seckill.mall.dto.CategoryCreateRequest;
import com.seckill.mall.dto.CategoryStatusUpdateRequest;
import com.seckill.mall.dto.CategoryUpdateRequest;

import java.util.List;

/**
 * Category 模块 Public API - 分类管理能力。
 *
 * <p>对应旧 {@code com.seckill.mall.service.CategoryService} 的方法签名，
 * 返回类型从 VO/Entity 改为 DTO，入参 Request 对象保持不变（HTTP 请求对象，不需要 DTO 化）。
 *
 * <p>Strangler Pattern：后续由 ApplicationService 实现本接口，委托旧 CategoryService。
 *
 * @author wnj
 * @since Phase CA.2
 */
public interface CategoryApi {

    /**
     * 获取分类树（含 productCount 统计）。
     *
     * @return 分类树节点列表（一级分类为根，children 递归填充）
     */
    List<CategoryDTO> getCategoryTree();

    /**
     * 新增分类。
     *
     * @param request 分类创建请求
     * @return 新增分类的 DTO
     */
    CategoryDTO createCategory(CategoryCreateRequest request);

    /**
     * 编辑分类（支持移动节点）。
     *
     * @param id      分类 ID
     * @param request 分类更新请求
     * @return 更新后分类的 DTO
     */
    CategoryDTO updateCategory(Long id, CategoryUpdateRequest request);

    /**
     * 删除分类（逻辑删除）。
     *
     * @param id 分类 ID
     */
    void deleteCategory(Long id);

    /**
     * 切换分类启用/禁用状态。
     *
     * @param id      分类 ID
     * @param request 状态更新请求
     */
    void updateCategoryStatus(Long id, CategoryStatusUpdateRequest request);

    /**
     * 失效分类树缓存。
     * 商品增删改后需调用，以保证分类树 productCount 及时更新。
     */
    void evictCategoryCache();

    /**
     * 根据 ID 查询分类（模块间内部调用用）。
     *
     * @param id 分类 ID
     * @return 分类 DTO，不存在返回 null
     */
    CategoryDTO getCategoryById(Long id);
}