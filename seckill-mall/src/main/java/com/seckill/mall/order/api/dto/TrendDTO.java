package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单趋势 DTO。
 *
 * <p>由 {@code getOrderTrend} 方法返回，用于统计概览展示近 N 天下单趋势。
 *
 * <p>来源映射：DATE(create_time) / COUNT 聚合 / SUM(total_amount)
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDTO {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 订单数 */
    private Long orderCount;

    /** 订单金额合计 */
    private BigDecimal amount;
}