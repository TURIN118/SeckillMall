package com.seckill.mall.vo;

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

    private List<CategoryVO> children;
}
