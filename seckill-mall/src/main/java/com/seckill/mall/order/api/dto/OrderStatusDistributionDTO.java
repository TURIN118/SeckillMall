package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单状态分布 DTO。
 *
 * <p>由 {@code getStatusDistribution} 方法返回，用于统计概览与系统监控。
 *
 * <p>来源映射：OrderStatus.getCode() / COUNT 聚合
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDistributionDTO {

    /** 订单状态码 */
    private String status;

    /** 订单数量 */
    private Long count;
}