package com.seckill.mall.category.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Category API 数据传输对象 - 分类树节点。
 *
 * <p>对应 {@code com.seckill.mall.category.interfaces.vo.CategoryVO} 的字段，用于模块间通信，
 * 替代直接暴露 Category Entity。供 CategoryApi 接口方法返回使用。
 *
 * @author wnj
 * @since Phase CA.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;

    private Long parentId;

    private String categoryName;

    private Integer sortOrder;

    private Integer status;

    /**
     * 该分类及其所有子孙分类下的商品总数(仅统计未删除商品)。
     * 一级分类的 productCount 为其子树商品总数；二级分类为自身直接挂载商品数。
     * 由 ApplicationService 转换时回填。
     */
    private Integer productCount;

    private List<CategoryDTO> children;
}