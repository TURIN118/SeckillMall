package com.seckill.mall.stats.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单状态分布项 DTO（数据看台专用）。
 *
 * <p>用于 {@code /api/v1/admin/stats/order-status-distribution} 接口，
 * 返回每个订单状态对应的订单数量。本类仅包含 status 与 count 两个扁平字段，
 * 便于前端饼图直接消费。
 *
 * <p>来源映射：{@code OrderStatusItemVO} → {@code OrderStatusItemDTO}
 * （由 {@code StatsApiConverter.toDTO} 转换）。
 *
 * <p>参见 STATS-API-CONTRACT.md 第 2.3 节。
 *
 * @author wnj
 * @since Phase ST.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusItemDTO {

    /**
     * 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
     */
    private String status;

    /**
     * 该状态订单数量
     */
    private Long count;
}