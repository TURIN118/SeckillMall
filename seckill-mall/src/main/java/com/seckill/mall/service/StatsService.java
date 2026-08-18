package com.seckill.mall.service;

import com.seckill.mall.stats.interfaces.vo.OrderStatusItemVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillOverviewVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillRankingVO;
import com.seckill.mall.stats.interfaces.vo.StatsOverviewVO;
import com.seckill.mall.stats.interfaces.vo.TrendItemVO;

import java.util.List;

/**
 * 数据看台统计服务
 *
 * <p>提供后台数据看台所需的总览指标、用户/订单趋势、秒杀排行榜等统计能力。
 * 所有数据均从数据库实时查询，不做任何模拟。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：StatsService.java
 * 邮箱：nj651217@163.com
 */
public interface StatsService {

    /**
     * 总览统计：用户数、订单数、秒杀活动数、销售总额、商品数、今日订单/注册数
     *
     * @return 总览统计 VO
     */
    StatsOverviewVO getOverview();

    /**
     * 用户注册趋势：近 N 天每日注册数（按日期升序，缺失日期补零）
     *
     * @param days 天数，建议 7 或 30；null/非正数默认 7，上限 30
     * @return 趋势数据项列表
     */
    List<TrendItemVO> getUserTrend(Integer days);

    /**
     * 订单趋势：近 N 天每日订单数（按日期升序，缺失日期补零）
     *
     * @param days 天数，建议 7 或 30；null/非正数默认 7，上限 30
     * @return 趋势数据项列表
     */
    List<TrendItemVO> getOrderTrend(Integer days);

    /**
     * 秒杀排行榜 Top N：按销售额降序（销售额相同按销量降序）
     *
     * @param limit Top N，null/非正数默认 10，上限 50
     * @return 排行榜列表
     */
    List<SeckillRankingVO> getSeckillRanking(Integer limit);

    /**
     * 订单状态分布：按 status 分组统计订单数量
     *
     * <p>按 {@code OrderStatus} 枚举自然顺序输出，即使 count=0 也返回，
     * 便于前端饼图直接消费。仅统计未逻辑删除的订单（is_deleted=0）。</p>
     *
     * @return 状态分布项列表，每项包含 status 与 count
     */
    List<OrderStatusItemVO> getOrderStatusDistribution();

    /**
     * 秒杀活动概览：进行中 / 待开始 / 今日已完成三项实时指标
     *
     * <p>所有指标均基于时间窗口动态计算（不依赖 DB status 字段，M17 修正）：
     * <ul>
     *   <li>activeCount：start_time &lt;= now &lt; end_time 且未取消</li>
     *   <li>pendingCount：start_time &gt; now 且未取消</li>
     *   <li>completedToday：end_time 在今日且已结束、未取消</li>
     * </ul>
     * 采集失败时对应字段为 null，不影响其他字段。</p>
     *
     * @return 秒杀活动概览 VO
     */
    SeckillOverviewVO getSeckillOverview();
}