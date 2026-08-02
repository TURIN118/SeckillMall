package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：Product.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String description;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer salesCount;

    /** 加购数量（冗余计数，由购物车操作维护） */
    private Integer cartCount;

    /** 收藏数量（冗余计数，由收藏操作维护） */
    private Integer favoriteCount;

    private Long categoryId;

    private String images;

    private String mainImage;

    private String detailHtml;

    @TableField("status")
    private ProductStatus status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
