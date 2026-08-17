package com.seckill.mall.product.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品属性值实体
 * <p>
 * 对应表 {@code t_product_attribute_value}，如颜色属性下的「曜石黑」「银月白」。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductAttributeValue.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_product_attribute_value")
public class ProductAttributeValue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long attributeId;

    /** 冗余字段，加速按商品查所有属性值 */
    private Long productId;

    /** 属性值，如「曜石黑」「旗舰版」 */
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