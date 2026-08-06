package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一订单列表项视图对象
 * <p>
 * 用于在统一订单列表中同时表示秒杀订单（orderType="SECKILL"）与普通订单（orderType="NORMAL"），
 * 携带商品快照列表以支持列表页一次渲染完成，无需再逐单回查商品/明细。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderListItemVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OrderListItemVO {

    /** 订单主键 ID（秒杀订单或普通订单的 ID） */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单类型：SECKILL-秒杀订单 / NORMAL-普通订单 */
    private String orderType;

    /** 订单状态：UNPAID/PAID/SHIPPED/CANCELLED/TIMEOUT/COMPLETED */
    private String status;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 支付方式：WALLET/ALIPAY/WECHAT 等 */
    private String payMethod;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 商品快照列表（用于列表展示） */
    private List<OrderItemSnapshot> items;

    /**
     * 商品快照（列表展示用，与订单明细字段保持一致）。
     */
    @Data
    public static class OrderItemSnapshot {

        /** 商品 ID */
        private Long productId;

        /** 商品名称（下单时快照） */
        private String productName;

        /** 商品主图 URL（下单时快照） */
        private String productImage;

        /** 商品单价（秒杀订单为秒杀价，普通订单为下单原价） */
        private BigDecimal unitPrice;

        /** 购买数量 */
        private Integer quantity;
    }
}