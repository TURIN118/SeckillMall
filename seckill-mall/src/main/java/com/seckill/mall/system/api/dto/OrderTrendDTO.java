package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单趋势 DTO - 替代 OrderTrendVO 作为 Application 层返回值。
 *
 * <p>来源映射：{@code OrderTrendVO} → {@code OrderTrendDTO}（由 SystemApiConverter.toTrendDTO 转换）。
 *
 * <p>详见 SYSTEM-API-CONTRACT.md §2.3。
 *
 * @author wnj
 * @since Phase SY.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendDTO {

    /** 日期列表，格式 yyyy-MM-dd */
    private List<String> dates;

    /** 每日订单数列表，与 dates 一一对应 */
    private List<Long> orderCounts;

    /** 每日销售额列表（仅统计 PAID/COMPLETED），与 dates 一一对应 */
    private List<BigDecimal> salesAmounts;
}