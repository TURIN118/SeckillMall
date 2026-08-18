package com.seckill.mall.category.interfaces.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类属性模板视图对象
 * <p>
 * GET /api/v1/admin/category/{categoryId}/attributes 返回
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryAttributeVO {
    private Long id;
    private Long categoryId;
    private String name;
    /** IMAGE / TEXT */
    private String type;
    /** SELECT / INPUT */
    private String inputType;
    private Integer isRequired;
    private Integer sortOrder;
    private List<CategoryAttributeValueVO> values;

    @Data
    public static class CategoryAttributeValueVO {
        private Long id;
        private String value;
        private String imageUrl;
        private Integer sortOrder;
    }
}