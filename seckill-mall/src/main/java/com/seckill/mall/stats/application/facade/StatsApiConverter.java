package com.seckill.mall.stats.application.facade;

import com.seckill.mall.seckill.api.dto.SeckillOverviewDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.seckill.interfaces.vo.SeckillOverviewVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillRankingVO;
import com.seckill.mall.stats.api.dto.OrderStatusItemDTO;
import com.seckill.mall.stats.api.dto.StatsOverviewDTO;
import com.seckill.mall.stats.api.dto.TrendItemDTO;
import com.seckill.mall.stats.interfaces.vo.OrderStatusItemVO;
import com.seckill.mall.stats.interfaces.vo.StatsOverviewVO;
import com.seckill.mall.stats.interfaces.vo.TrendItemVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stats API 转换辅助类（Strangler Pattern 门面层）。
 *
 * <p>集中存放 stats 模块旧 VO 与新 API 层 DTO 之间的转换方法，
 * 供 {@link com.seckill.mall.stats.application.StatsApplicationService} 与
 * {@link com.seckill.mall.stats.interfaces.web.StatsController} 调用。
 * 所有方法均为无状态静态方法。
 *
 * <p>转换原则：
 * <ul>
 *     <li>VO ↔ DTO：核心字段一一映射，保持前端契约不变</li>
 *     <li>SeckillOverviewVO ↔ SeckillOverviewDTO：处理字段名差异
 *         （vo.completedToday ↔ dto.completedTodayCount），类型差异
 *         （Integer ↔ long 用 Math.toIntExact），dto.totalActivities 在 stats 模块无语义置 0</li>
 *     <li>SeckillRankingVO ↔ SeckillRankingDTO：处理字段名差异
 *         （vo.productName ↔ dto.seckillName，vo.totalAmount ↔ dto.salesAmount），
 *         vo.seckillPrice 在 DTO 无对应字段，转换时丢失（保持与旧 StatsService 行为一致）</li>
 * </ul>
 *
 * <p>依赖约束：仅依赖 seckill.api.dto（API 层）与 seckill.interfaces.vo（接口层），
 * 不依赖 seckill.application（避免 stats.application → seckill.application 跨模块 application 依赖）。
 *
 * <p>参见 STATS-API-CONTRACT.md 第 4 节。
 *
 * @author wnj
 * @since Phase ST.4
 */
public class StatsApiConverter {

    // ============================================================
    // StatsOverviewVO ↔ StatsOverviewDTO 转换
    // ============================================================

    /** StatsOverviewVO → StatsOverviewDTO（全字段映射） */
    public static StatsOverviewDTO toDTO(StatsOverviewVO vo) {
        if (vo == null) {
            return null;
        }
        return StatsOverviewDTO.builder()
                .userCount(vo.getUserCount())
                .orderCount(vo.getOrderCount())
                .seckillCount(vo.getSeckillCount())
                .totalSales(vo.getTotalSales())
                .productCount(vo.getProductCount())
                .todayOrderCount(vo.getTodayOrderCount())
                .todayUserCount(vo.getTodayUserCount())
                .build();
    }

    /** StatsOverviewDTO → StatsOverviewVO（Controller 层前端契约适配，全字段映射） */
    public static StatsOverviewVO toVO(StatsOverviewDTO dto) {
        if (dto == null) {
            return null;
        }
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setUserCount(dto.getUserCount());
        vo.setOrderCount(dto.getOrderCount());
        vo.setSeckillCount(dto.getSeckillCount());
        vo.setTotalSales(dto.getTotalSales());
        vo.setProductCount(dto.getProductCount());
        vo.setTodayOrderCount(dto.getTodayOrderCount());
        vo.setTodayUserCount(dto.getTodayUserCount());
        return vo;
    }

    // ============================================================
    // TrendItemVO ↔ TrendItemDTO 转换
    // ============================================================

    /** TrendItemVO → TrendItemDTO */
    public static TrendItemDTO toDTO(TrendItemVO vo) {
        if (vo == null) {
            return null;
        }
        return TrendItemDTO.builder()
                .date(vo.getDate())
                .count(vo.getCount())
                .build();
    }

    /** TrendItemVO 列表 → TrendItemDTO 列表 */
    public static List<TrendItemDTO> toDTOList(List<TrendItemVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(StatsApiConverter::toDTO).collect(Collectors.toList());
    }

    /** TrendItemDTO → TrendItemVO（Controller 层前端契约适配） */
    public static TrendItemVO toVO(TrendItemDTO dto) {
        if (dto == null) {
            return null;
        }
        return new TrendItemVO(dto.getDate(), dto.getCount());
    }

    /** TrendItemDTO 列表 → TrendItemVO 列表 */
    public static List<TrendItemVO> toVOList(List<TrendItemDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(StatsApiConverter::toVO).collect(Collectors.toList());
    }

    // ============================================================
    // OrderStatusItemVO ↔ OrderStatusItemDTO 转换
    // ============================================================

    /** OrderStatusItemVO → OrderStatusItemDTO */
    public static OrderStatusItemDTO toDTO(OrderStatusItemVO vo) {
        if (vo == null) {
            return null;
        }
        return OrderStatusItemDTO.builder()
                .status(vo.getStatus())
                .count(vo.getCount())
                .build();
    }

    /** OrderStatusItemVO 列表 → OrderStatusItemDTO 列表 */
    public static List<OrderStatusItemDTO> toStatusDTOList(List<OrderStatusItemVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(StatsApiConverter::toDTO).collect(Collectors.toList());
    }

    /** OrderStatusItemDTO → OrderStatusItemVO（Controller 层前端契约适配） */
    public static OrderStatusItemVO toVO(OrderStatusItemDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderStatusItemVO vo = new OrderStatusItemVO();
        vo.setStatus(dto.getStatus());
        vo.setCount(dto.getCount());
        return vo;
    }

    /** OrderStatusItemDTO 列表 → OrderStatusItemVO 列表 */
    public static List<OrderStatusItemVO> toStatusVOList(List<OrderStatusItemDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(StatsApiConverter::toVO).collect(Collectors.toList());
    }

    // ============================================================
    // SeckillOverviewVO ↔ SeckillOverviewDTO 转换（stats 模块专用）
    // ============================================================

    /**
     * SeckillOverviewVO → SeckillOverviewDTO（ApplicationService 用）。
     *
     * <p>字段映射：
     * <ul>
     *     <li>vo.activeCount (Integer) → dto.activeCount (long)，null → 0</li>
     *     <li>vo.pendingCount (Integer) → dto.pendingCount (long)，null → 0</li>
     *     <li>vo.completedToday (Integer) → dto.completedTodayCount (long)，null → 0</li>
     *     <li>dto.totalActivities = 0（stats 模块旧 StatsService 不计算活动总数，保持语义一致）</li>
     * </ul>
     */
    public static SeckillOverviewDTO toOverviewDTO(SeckillOverviewVO vo) {
        if (vo == null) {
            return null;
        }
        return SeckillOverviewDTO.builder()
                .totalActivities(0L)
                .activeCount(vo.getActiveCount() == null ? 0L : vo.getActiveCount().longValue())
                .pendingCount(vo.getPendingCount() == null ? 0L : vo.getPendingCount().longValue())
                .completedTodayCount(vo.getCompletedToday() == null ? 0L : vo.getCompletedToday().longValue())
                .build();
    }

    /**
     * SeckillOverviewDTO → SeckillOverviewVO（Controller 层前端契约适配）。
     *
     * <p>字段映射：
     * <ul>
     *     <li>dto.activeCount (long) → vo.activeCount (Integer)，用 Math.toIntExact</li>
     *     <li>dto.pendingCount (long) → vo.pendingCount (Integer)，用 Math.toIntExact</li>
     *     <li>dto.completedTodayCount (long) → vo.completedToday (Integer)，用 Math.toIntExact</li>
     *     <li>dto.totalActivities 忽略（stats 模块前端契约不展示活动总数）</li>
     * </ul>
     */
    public static SeckillOverviewVO toOverviewVO(SeckillOverviewDTO dto) {
        if (dto == null) {
            return null;
        }
        SeckillOverviewVO vo = new SeckillOverviewVO();
        vo.setActiveCount(Math.toIntExact(dto.getActiveCount()));
        vo.setPendingCount(Math.toIntExact(dto.getPendingCount()));
        vo.setCompletedToday(Math.toIntExact(dto.getCompletedTodayCount()));
        return vo;
    }

    // ============================================================
    // SeckillRankingVO ↔ SeckillRankingDTO 转换（stats 模块专用）
    // ============================================================

    /**
     * SeckillRankingVO → SeckillRankingDTO（ApplicationService 用）。
     *
     * <p>字段映射：
     * <ul>
     *     <li>vo.seckillId → dto.seckillId</li>
     *     <li>vo.productName → dto.seckillName</li>
     *     <li>vo.salesCount (Long) → dto.salesCount (long)，null → 0</li>
     *     <li>vo.totalAmount → dto.salesAmount</li>
     *     <li>vo.seckillPrice 丢失（DTO 无对应字段，保持与旧 StatsService 行为一致）</li>
     * </ul>
     */
    public static SeckillRankingDTO toRankingDTO(SeckillRankingVO vo) {
        if (vo == null) {
            return null;
        }
        return SeckillRankingDTO.builder()
                .seckillId(vo.getSeckillId())
                .seckillName(vo.getProductName())
                .salesCount(vo.getSalesCount() == null ? 0L : vo.getSalesCount())
                .salesAmount(vo.getTotalAmount())
                .build();
    }

    /** SeckillRankingVO 列表 → SeckillRankingDTO 列表 */
    public static List<SeckillRankingDTO> toRankingDTOList(List<SeckillRankingVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(StatsApiConverter::toRankingDTO).collect(Collectors.toList());
    }

    /**
     * SeckillRankingDTO → SeckillRankingVO（Controller 层前端契约适配）。
     *
     * <p>字段映射：
     * <ul>
     *     <li>dto.seckillId → vo.seckillId</li>
     *     <li>dto.seckillName → vo.productName</li>
     *     <li>dto.salesCount (long) → vo.salesCount (Long)</li>
     *     <li>dto.salesAmount → vo.totalAmount</li>
     *     <li>vo.seckillPrice = null（DTO 无对应字段，前端可空兼容）</li>
     * </ul>
     */
    public static SeckillRankingVO toRankingVO(SeckillRankingDTO dto) {
        if (dto == null) {
            return null;
        }
        SeckillRankingVO vo = new SeckillRankingVO();
        vo.setSeckillId(dto.getSeckillId());
        vo.setProductName(dto.getSeckillName());
        vo.setSalesCount(dto.getSalesCount());
        vo.setTotalAmount(dto.getSalesAmount());
        return vo;
    }

    /** SeckillRankingDTO 列表 → SeckillRankingVO 列表 */
    public static List<SeckillRankingVO> toRankingVOList(List<SeckillRankingDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(StatsApiConverter::toRankingVO).collect(Collectors.toList());
    }
}