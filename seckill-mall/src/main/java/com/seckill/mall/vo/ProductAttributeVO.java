package com.seckill.mall.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品属性视图对象（商品详情接口返回）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttributeVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductAttributeVO {
    private Long id;
    /** 关联分类属性模板 ID，null 表示商品自定义属性（前端据此区分模板属性 / 自定义属性） */
    private Long categoryAttributeId;
    private String name;
    /** IMAGE / TEXT */
    private String type;
    private Integer sortOrder;
    private List<AttributeValueVO> values;

    @Data
    public static class AttributeValueVO {
        private Long id;
        private String value;
        private String imageUrl;
        private Integer sortOrder;
    }
}