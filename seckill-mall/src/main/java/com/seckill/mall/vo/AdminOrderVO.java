package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminOrderVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class AdminOrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    /**
     * 关联 t_user.username
     */
    private String username;

    private Long seckillId;

    /**
     * 秒杀活动名称。t_seckill_goods 表无 name 字段，这里用关联商品的 productName 代替
     */
    private String seckillName;

    private Long productId;

    /**
     * 关联 t_product.name
     */
    private String productName;

    private BigDecimal seckillPrice;

    private Integer quantity;

    private BigDecimal totalAmount;

    /**
     * 订单状态：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
     */
    private String status;

    private LocalDateTime payTime;

    private LocalDateTime payExpireTime;

    private String transactionId;

    private String payMethod;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}