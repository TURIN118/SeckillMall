package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.AttributeType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品属性实体（SKU 维度定义）
 * <p>
 * 对应表 {@code t_product_attribute}，如「颜色」「版本」「表带」。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttribute.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_product_attribute")
public class ProductAttribute {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    /** 关联分类属性模板 ID，null 表示商品自定义属性 */
    private Long categoryAttributeId;

    /** 属性名，如「颜色」「版本」「表带」 */
    private String name;

    /** 属性类型：IMAGE-图片型 / TEXT-文字型 */
    @TableField("type")
    private AttributeType type;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}