package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.service.StatsService;
import com.seckill.mall.vo.OrderStatusItemVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillOverviewVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillRankingVO;
import com.seckill.mall.vo.StatsOverviewVO;
import com.seckill.mall.vo.TrendItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据看台统计控制器
 *
 * <p>提供后台数据看台所需的总览指标、用户/订单趋势、秒杀排行榜等接口。
 * 全部接口仅 ADMIN 角色可访问，数据均从数据库实时统计得到。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：StatsController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "后台数据看台", description = "总览/趋势/排行榜统计")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "总览统计（用户/订单/秒杀/销售/商品/今日新增）")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.success(statsService.getOverview());
    }

    @Operation(summary = "用户注册趋势（近 N 天每日注册数，默认 7 天）")
    @GetMapping("/user-trend")
    public Result<List<TrendItemVO>> userTrend(@RequestParam(required = false) Integer days) {
        return Result.success(statsService.getUserTrend(days));
    }

    @Operation(summary = "订单趋势（近 N 天每日订单数，默认 7 天）")
    @GetMapping("/order-trend")
    public Result<List<TrendItemVO>> orderTrend(@RequestParam(required = false) Integer days) {
        return Result.success(statsService.getOrderTrend(days));
    }

    @Operation(summary = "秒杀排行榜 Top N（按销售额降序，默认 10）")
    @GetMapping("/seckill-ranking")
    public Result<List<SeckillRankingVO>> seckillRanking(@RequestParam(required = false) Integer limit) {
        return Result.success(statsService.getSeckillRanking(limit));
    }

    @Operation(summary = "订单状态分布（按状态分组统计订单数量）")
    @GetMapping("/order-status-distribution")
    public Result<List<OrderStatusItemVO>> orderStatusDistribution() {
        return Result.success(statsService.getOrderStatusDistribution());
    }

    @Operation(summary = "秒杀活动概览（进行中/待开始/今日已完成）")
    @GetMapping("/seckill-overview")
    public Result<SeckillOverviewVO> seckillOverview() {
        return Result.success(statsService.getSeckillOverview());
    }
}