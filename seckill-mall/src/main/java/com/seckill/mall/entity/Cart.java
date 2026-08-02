package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车实体
 * <p>
 * 对应表 {@code t_cart}，每用户每商品唯一（uk_user_product）。
 * selected 字段标识结算时是否勾选，is_deleted 由 {@link TableLogic} 自动处理。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：Cart.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_cart")
public class Cart {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long productId;

    private Integer quantity;

    /** 是否选中：0-否 / 1-是 */
    private Integer selected;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}