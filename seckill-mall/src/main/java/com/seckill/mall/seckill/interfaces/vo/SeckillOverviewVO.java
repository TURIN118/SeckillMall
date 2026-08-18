package com.seckill.mall.seckill.interfaces.vo;

import lombok.Data;

/**
 * 秒杀活动概览 VO
 *
 * <p>用于后台仪表盘展示秒杀活动的实时概览指标，包括进行中、待开始、今日已完成三项。
 * 所有指标均基于时间窗口动态计算，不依赖 DB status 字段（M17 修正）。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOverviewVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillOverviewVO {

    /**
     * 进行中秒杀活动数量（start_time <= now < end_time 且未取消），采集失败为 null
     */
    private Integer activeCount;

    /**
     * 待开始秒杀活动数量（start_time > now 且未取消），采集失败为 null
     */
    private Integer pendingCount;

    /**
     * 今日已完成秒杀活动数量（end_time 在今日且已结束、未取消），采集失败为 null
     */
    private Integer completedToday;
}