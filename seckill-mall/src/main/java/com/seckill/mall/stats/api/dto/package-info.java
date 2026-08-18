/**
 * Stats API 数据传输对象 - 对外数据契约。
 *
 * <p>包含总览统计 DTO（StatsOverviewDTO）、趋势数据项 DTO（TrendItemDTO）、
 * 订单状态分布项 DTO（OrderStatusItemDTO），用于模块间通信，替代直接暴露 VO。
 *
 * <p>秒杀概览与排行榜 DTO 复用 seckill 模块定义（SeckillOverviewDTO/SeckillRankingDTO），
 * 不在本包重复定义。
 *
 * @author wnj
 * @since Phase ST.0
 */
package com.seckill.mall.stats.api.dto;