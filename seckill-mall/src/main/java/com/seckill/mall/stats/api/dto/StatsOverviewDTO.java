package com.seckill.mall.stats.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 数据看台总览统计 DTO。
 *
 * <p>聚合用户、订单、秒杀活动、商品、销售总额及今日新增等核心指标，
 * 供后台数据看台顶部卡片行展示使用。所有数值均由后端从数据库统计得到，
 * 不存在任何模拟数据。
 *
 * <p>来源映射：{@code StatsOverviewVO} → {@code StatsOverviewDTO}
 * （由 {@code StatsApiConverter.toDTO} 转换）。
 *
 * <p>参见 STATS-API-CONTRACT.md 第 2.1 节。
 *
 * @author wnj
 * @since Phase ST.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsOverviewDTO {

    /**
     * 用户总数（t_user 全量，含逻辑删除过滤后的有效用户）
     */
    private Long userCount;

    /**
     * 订单总数（t_seckill_order 全量）
     */
    private Long orderCount;

    /**
     * 秒杀活动数（t_seckill_goods 全量）
     */
    private Long seckillCount;

    /**
     * 销售总额（仅统计 PAID/COMPLETED 订单的 SUM(total_amount)）
     */
    private BigDecimal totalSales;

    /**
     * 商品总数（t_product 全量）
     */
    private Long productCount;

    /**
     * 今日订单数（create_time 落在当天 00:00:00 ~ 23:59:59）
     */
    private Long todayOrderCount;

    /**
     * 今日注册数（t_user.create_time 落在当天）
     */
    private Long todayUserCount;
}