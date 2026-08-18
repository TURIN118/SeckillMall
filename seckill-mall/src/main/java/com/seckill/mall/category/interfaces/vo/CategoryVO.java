package com.seckill.mall.category.interfaces.vo;

import lombok.Data;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryVO {

    private Long id;

    private Long parentId;

    private String categoryName;

    private Integer sortOrder;

    private Integer status;

    /**
     * 该分类及其所有子孙分类下的商品总数(仅统计未删除商品)。
     * 一级分类的 productCount 为其子树商品总数；二级分类为自身直接挂载商品数。
     * 由 CategoryServiceImpl.getCategoryTree() 后递归回填。
     */
    private Integer productCount;

    private List<CategoryVO> children;
}
