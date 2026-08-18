package com.seckill.mall.category.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类属性预设值实体
 * <p>
 * 对应表 {@code t_category_attribute_value}，存储分类属性模板的预设可选值。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeValue.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_category_attribute_value")
public class CategoryAttributeValue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long attributeId;

    /** 预设值，如「曜石黑」「标准版」 */
    private String value;

    /** 图片型属性值的色块 URL，文字型为 null */
    private String imageUrl;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}