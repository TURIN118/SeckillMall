package com.seckill.mall.dto;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryUpdateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CategoryUpdateRequest {

    /** 分类名称，可选 */
    private String categoryName;

    /** 父分类 ID，可选（修改即移动节点） */
    private Long parentId;

    /** 排序值，可选 */
    private Integer sortOrder;

    /** 状态：1=启用，0=禁用，可选 */
    private Integer status;
}