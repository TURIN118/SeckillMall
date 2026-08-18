package com.seckill.mall.stats.api;

import com.seckill.mall.seckill.api.dto.SeckillOverviewDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.stats.api.dto.OrderStatusItemDTO;
import com.seckill.mall.stats.api.dto.StatsOverviewDTO;
import com.seckill.mall.stats.api.dto.TrendItemDTO;

import java.util.List;

/**
 * Stats 模块统计聚合 API。
 *
 * <p>对外暴露后台数据看台所需的统计能力，供 StatsController 调用。聚合 identity/seckill/product
 * 三大模块的 API，提供总览指标、用户/订单趋势、秒杀排行榜、订单状态分布、秒杀活动概览等能力。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数（本接口所有方法均为单参数 Integer，不构成反模式，保留裸参数）</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * <p>复用策略：秒杀概览与排行榜 DTO 复用 seckill 模块定义
 * （{@link SeckillOverviewDTO}/{@link SeckillRankingDTO}），避免重复建模。
 *
 * <p>原方法映射参见 STATS-API-CONTRACT.md 第 1 节。
 *
 * @author wnj
 * @since Phase ST.2
 */
public interface StatsApi {

    /**
     * 总览统计：用户数、订单数、秒杀活动数、销售总额、商品数、今日订单/注册数。
     *
     * <p>所有指标均从数据库实时查询，不做任何模拟数据。</p>
     *
     * @return 总览统计 DTO
     */
    StatsOverviewDTO getOverview();

    /**
     * 用户注册趋势：近 N 天每日注册数（按日期升序，缺失日期补零）。
     *
     * @param days 天数，建议 7 或 30；null/非正数默认 7，上限 30
     * @return 趋势数据项列表
     */
    List<TrendItemDTO> getUserTrend(Integer days);

    /**
     * 订单趋势：近 N 天每日订单数（按日期升序，缺失日期补零）。
     *
     * @param days 天数，建议 7 或 30；null/非正数默认 7，上限 30
     * @return 趋势数据项列表
     */
    List<TrendItemDTO> getOrderTrend(Integer days);

    /**
     * 秒杀排行榜 Top N：按销售额降序（销售额相同按销量降序）。
     *
     * @param limit Top N，null/非正数默认 10，上限 50
     * @return 排行榜列表（复用 seckill 模块 SeckillRankingDTO）
     */
    List<SeckillRankingDTO> getSeckillRanking(Integer limit);

    /**
     * 订单状态分布：按 status 分组统计订单数量。
     *
     * <p>按 {@code OrderStatus} 枚举自然顺序输出，即使 count=0 也返回，
     * 便于前端饼图直接消费。仅统计未逻辑删除的订单（is_deleted=0）。</p>
     *
     * @return 状态分布项列表，每项包含 status 与 count
     */
    List<OrderStatusItemDTO> getOrderStatusDistribution();

    /**
     * 秒杀活动概览：进行中 / 待开始 / 今日已完成三项实时指标。
     *
     * <p>所有指标均基于时间窗口动态计算（不依赖 DB status 字段）。
     * 返回复用 seckill 模块的 {@link SeckillOverviewDTO}。</p>
     *
     * @return 秒杀活动概览 DTO（复用 seckill 模块）
     */
    SeckillOverviewDTO getSeckillOverview();
}