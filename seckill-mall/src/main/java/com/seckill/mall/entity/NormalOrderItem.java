package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单明细实体
 * <p>
 * 对应表 {@code t_normal_order_item}，每条记录对应普通订单中一个商品行，
 * 冗余商品名称/主图/单价以保留下单时刻快照，避免商品后续修改影响历史订单展示。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：NormalOrderItem.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_normal_order_item")
public class NormalOrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属普通订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称（下单时快照） */
    private String productName;

    /** 商品主图URL（下单时快照） */
    private String productImage;

    /** 商品单价（下单时快照，原价） */
    private BigDecimal unitPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 = unitPrice × quantity */
    private BigDecimal subtotal;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}