package com.seckill.mall.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀概览 DTO，用于 stats 模块首页概览展示。
 *
 * <p>包含秒杀活动总数、进行中、待开始、今日已完成四项指标。
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 5.4 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOverviewDTO {

    /** 秒杀活动总数 */
    private long totalActivities;

    /** 进行中秒杀数 */
    private long activeCount;

    /** 待开始秒杀数 */
    private long pendingCount;

    /** 今日已完成秒杀数 */
    private long completedTodayCount;
}