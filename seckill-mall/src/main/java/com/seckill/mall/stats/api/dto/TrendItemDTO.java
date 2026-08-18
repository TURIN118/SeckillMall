package com.seckill.mall.stats.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势数据项 DTO。
 *
 * <p>用于用户注册趋势、订单趋势等按日期分组的统计接口，
 * 每一项表示某一天（或某一时间粒度）的数量。
 *
 * <p>来源映射：{@code TrendItemVO} → {@code TrendItemDTO}
 * （由 {@code StatsApiConverter.toDTO} 转换）。
 *
 * <p>参见 STATS-API-CONTRACT.md 第 2.2 节。
 *
 * @author wnj
 * @since Phase ST.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendItemDTO {

    /**
     * 日期字符串，格式 yyyy-MM-dd
     */
    private String date;

    /**
     * 当日数量（注册数 / 订单数等，由调用方语义决定）
     */
    private Long count;
}