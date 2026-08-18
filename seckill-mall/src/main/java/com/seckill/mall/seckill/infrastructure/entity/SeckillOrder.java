package com.seckill.mall.seckill.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrder.java
 * 邮箱：nj651217@163.com
 */
@Data
@TableName("t_seckill_order")
public class SeckillOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long seckillId;

    private Long productId;

    private BigDecimal seckillPrice;

    private Integer quantity;

    private BigDecimal totalAmount;

    @TableField("status")
    private OrderStatus status;

    /** 物流公司 */
    private String shippingCompany;

    /** 快递单号 */
    private String shippingNo;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 确认收货时间 */
    private LocalDateTime confirmTime;

    private LocalDateTime payTime;

    private LocalDateTime payExpireTime;

    private String transactionId;

    private String payMethod;

    private LocalDateTime cancelTime;

    private String cancelReason;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
