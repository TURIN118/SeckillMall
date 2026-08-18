package com.seckill.mall.stats.interfaces.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据看台总览统计 VO
 *
 * <p>聚合用户、订单、秒杀活动、商品、销售总额及今日新增等核心指标，
 * 供后台数据看台顶部卡片行展示使用。所有数值均由后端从数据库统计得到，
 * 不存在任何模拟数据。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：StatsOverviewVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class StatsOverviewVO {

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