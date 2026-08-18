package com.seckill.mall.stats.application;

import com.seckill.mall.seckill.api.dto.SeckillOverviewDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.service.StatsService;
import com.seckill.mall.stats.api.StatsApi;
import com.seckill.mall.stats.api.dto.OrderStatusItemDTO;
import com.seckill.mall.stats.api.dto.StatsOverviewDTO;
import com.seckill.mall.stats.api.dto.TrendItemDTO;
import com.seckill.mall.stats.application.facade.StatsApiConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stats 模块 Application 层门面服务（Strangler Pattern）。
 *
 * <p>实现 {@link StatsApi}，作为新 API 层与旧 {@link StatsService} 实现之间的
 * 绞杀者门面（Strangler Facade）。本类不重写任何业务逻辑，仅做：
 * <ul>
 *     <li>委托：所有方法体调用旧 StatsService 对应方法</li>
 *     <li>适配：通过 {@link StatsApiConverter} 将旧 VO 返回值转换为新 DTO</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>不修改旧 StatsService / StatsServiceImpl 的任何业务行为</li>
 *     <li>不引入新的 SQL / Mapper / 跨模块 Service 依赖</li>
 *     <li>保持旧 StatsService 在 Spring 容器中仍可被其他潜在调用方注入（向后兼容）</li>
 * </ul>
 *
 * <p>迁移路径：Phase ST.4 完成后，StatsController 切换为依赖 StatsApi（本类），
 * 旧 StatsService 仅被本类引用。后续 Phase 可将 StatsServiceImpl 的业务逻辑
 * 平滑迁入本类或新建的领域服务，再删除旧 StatsService。
 *
 * <p>参见 STATS-API-CONTRACT.md 第 3 节。
 *
 * @author wnj
 * @since Phase ST.4
 */
@Service
@RequiredArgsConstructor
public class StatsApplicationService implements StatsApi {

    private final StatsService statsService;

    @Override
    public StatsOverviewDTO getOverview() {
        return StatsApiConverter.toDTO(statsService.getOverview());
    }

    @Override
    public List<TrendItemDTO> getUserTrend(Integer days) {
        return StatsApiConverter.toDTOList(statsService.getUserTrend(days));
    }

    @Override
    public List<TrendItemDTO> getOrderTrend(Integer days) {
        return StatsApiConverter.toDTOList(statsService.getOrderTrend(days));
    }

    @Override
    public List<SeckillRankingDTO> getSeckillRanking(Integer limit) {
        // 旧 StatsService.getSeckillRanking 返回 List<SeckillRankingVO>，
        // 通过 StatsApiConverter 转为 List<SeckillRankingDTO>
        return StatsApiConverter.toRankingDTOList(statsService.getSeckillRanking(limit));
    }

    @Override
    public List<OrderStatusItemDTO> getOrderStatusDistribution() {
        return StatsApiConverter.toStatusDTOList(statsService.getOrderStatusDistribution());
    }

    @Override
    public SeckillOverviewDTO getSeckillOverview() {
        // 旧 StatsService.getSeckillOverview 返回 SeckillOverviewVO，
        // 通过 StatsApiConverter 转为 SeckillOverviewDTO
        return StatsApiConverter.toOverviewDTO(statsService.getSeckillOverview());
    }
}