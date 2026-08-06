package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.AttributeType;
import com.seckill.mall.entity.enums.AttributeInputType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类属性模板实体
 * <p>
 * 对应表 {@code t_category_attribute}，在分类级别定义规格维度模板。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttribute.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_category_attribute")
public class CategoryAttribute {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    /** 属性名，如「颜色」「版本」「表带」 */
    private String name;

    /** 属性类型：IMAGE-图片型 / TEXT-文字型 */
    @TableField("type")
    private AttributeType type;

    /** 录入方式：SELECT-从预设值选 / INPUT-自由输入 */
    @TableField("input_type")
    private AttributeInputType inputType;

    /** 是否必选：0-否 / 1-是 */
    private Integer isRequired;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}