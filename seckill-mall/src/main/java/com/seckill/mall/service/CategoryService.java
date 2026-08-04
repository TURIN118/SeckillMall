package com.seckill.mall.service;

import com.seckill.mall.dto.CategoryCreateRequest;
import com.seckill.mall.dto.CategoryStatusUpdateRequest;
import com.seckill.mall.dto.CategoryUpdateRequest;
import com.seckill.mall.vo.CategoryVO;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryService.java
 * 邮箱：nj651217@163.com
 */
public interface CategoryService {

    List<CategoryVO> getCategoryTree();

    /**
     * 新增分类
     *
     * @param request 分类创建请求
     * @return 新增分类的视图对象
     */
    CategoryVO createCategory(CategoryCreateRequest request);

    /**
     * 编辑分类（支持移动节点）
     *
     * @param id      分类 ID
     * @param request 分类更新请求
     * @return 更新后分类的视图对象
     */
    CategoryVO updateCategory(Long id, CategoryUpdateRequest request);

    /**
     * 删除分类（逻辑删除）
     *
     * @param id 分类 ID
     */
    void deleteCategory(Long id);

    /**
     * 切换分类启用/禁用状态
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
}
