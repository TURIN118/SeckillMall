package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一订单列表项 DTO。
 *
 * <p>由 {@code listOrders} 方法返回，统一展示秒杀订单与普通订单。
 * 合并后按 createTime 降序，内存分页。
 *
 * <p>来源映射：NormalOrder / SeckillOrder → OrderListItemDTO
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemDTO {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单类型（NORMAL/SECKILL） */
    private String orderType;

    /** 订单状态码 */
    private String status;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 支付方式 */
    private String payMethod;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 商品快照列表 */
    private List<OrderItemSnapshotDTO> items;
}