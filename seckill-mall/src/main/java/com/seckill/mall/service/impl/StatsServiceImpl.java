package com.seckill.mall.service.impl;

import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.seckill.api.SeckillGoodsApi;
import com.seckill.mall.seckill.api.SeckillOrderApi;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.seckill.application.facade.SeckillApiConverter;
import com.seckill.mall.service.StatsService;
import com.seckill.mall.vo.OrderStatusItemVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillOverviewVO;
import com.seckill.mall.seckill.interfaces.vo.SeckillRankingVO;
import com.seckill.mall.vo.StatsOverviewVO;
import com.seckill.mall.vo.TrendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看台统计服务实现
 *
 * <p>所有指标均从数据库实时查询，不做任何模拟数据。
 * 趋势接口对缺失日期补零，保证前端图表横轴连续。</p>
 *
 * <p>Phase 14：消除对 UserMapper/SeckillOrderMapper/SeckillGoodsMapper/ProductMapper
 * 4 个跨模块 Mapper 的直接依赖，统一改为领域 Service 调用。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：StatsServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 趋势查询天数上限，避免一次性拉取过多日期导致响应变慢
     */
    private static final int TREND_MAX_DAYS = 30;

    /**
     * 排行榜 Top N 上限
     */
    private static final int RANKING_MAX_LIMIT = 50;

    private final UserApi userApi;
    private final SeckillOrderApi seckillOrderApi;
    private final SeckillGoodsApi seckillGoodsApi;
    private final ProductApi productApi;

    @Override
    public StatsOverviewVO getOverview() {
        StatsOverviewVO vo = new StatsOverviewVO();

        // 用户总数
        vo.setUserCount(userApi.countAll());

        // 订单总数
        vo.setOrderCount(seckillOrderApi.countAll());

        // 秒杀活动数
        vo.setSeckillCount(seckillGoodsApi.countAll());

        // 商品总数
        vo.setProductCount(productApi.countAll());

        // 销售总额（仅统计 PAID/COMPLETED）
        BigDecimal sales = seckillOrderApi.sumSalesAmount(
                List.of(OrderStatus.PAID, OrderStatus.COMPLETED));
        vo.setTotalSales(sales == null ? BigDecimal.ZERO : sales);

        // 今日注册数 / 今日订单数
        LocalDate today = LocalDate.now();
        Long todayUser = userApi.countTodayRegistered(today);
        vo.setTodayUserCount(todayUser == null ? 0L : todayUser);

        Long todayOrder = seckillOrderApi.countTodayOrders(today);
        vo.setTodayOrderCount(todayOrder == null ? 0L : todayOrder);

        return vo;
    }

    @Override
    public List<TrendItemVO> getUserTrend(Integer days) {
        int n = normalizeDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(n - 1L);

        List<Map<String, Object>> rows = userApi.selectUserTrend(startDate, endDate);
        Map<String, Long> countByDate = indexByDate(rows);

        return fillTrend(startDate, n, countByDate);
    }

    @Override
    public List<TrendItemVO> getOrderTrend(Integer days) {
        int n = normalizeDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(n - 1L);

        // 复用现有 selectOrderTrend：按日期分组统计订单数（仅统计 PAID/COMPLETED）
        List<Map<String, Object>> rows = seckillOrderApi.selectOrderTrend(
                startDate, endDate, List.of(OrderStatus.PAID, OrderStatus.COMPLETED));
        Map<String, Long> countByDate = indexByDate(rows);

        return fillTrend(startDate, n, countByDate);
    }

    @Override
    public List<SeckillRankingVO> getSeckillRanking(Integer limit) {
        int n = normalizeLimit(limit);
        // Phase SK.5：切换到 SeckillOrderApi，返回 DTO 而非 VO，用 SeckillApiConverter 适配回 VO
        List<SeckillRankingDTO> rows = seckillOrderApi.selectSeckillRanking(
                List.of(OrderStatus.PAID, OrderStatus.COMPLETED), n);
        return rows == null ? Collections.emptyList() : SeckillApiConverter.toRankingVOList(rows);
    }

    @Override
    public List<OrderStatusItemVO> getOrderStatusDistribution() {
        // 复用现有 selectStatusDistribution：按 status 分组统计订单数（不限时间范围）
        List<Map<String, Object>> rows = seckillOrderApi.selectStatusDistribution(null, null);

        // 按状态码建立计数索引
        Map<String, Long> countByStatus = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object statusObj = row.get("status");
                if (statusObj == null) {
                    continue;
                }
                String code = statusObj.toString();
                countByStatus.put(code, toLong(row.get("cnt")));
            }
        }

        // 按 OrderStatus 枚举自然顺序组装，即使 count=0 也输出，便于前端饼图直接消费
        List<OrderStatusItemVO> result = new ArrayList<>(OrderStatus.values().length);
        for (OrderStatus os : OrderStatus.values()) {
            OrderStatusItemVO item = new OrderStatusItemVO();
            item.setStatus(os.getCode());
            item.setCount(countByStatus.getOrDefault(os.getCode(), 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public SeckillOverviewVO getSeckillOverview() {
        SeckillOverviewVO vo = new SeckillOverviewVO();
        // Phase 14：秒杀活动计数逻辑下沉至 SeckillGoodsApi，此处仅做异常兜底
        try {
            vo.setActiveCount(Math.toIntExact(seckillGoodsApi.countActive()));
        } catch (Exception e) {
            log.debug("进行中秒杀数采集失败: {}", e.getMessage());
        }
        try {
            vo.setPendingCount(Math.toIntExact(seckillGoodsApi.countPending()));
        } catch (Exception e) {
            log.debug("待开始秒杀数采集失败: {}", e.getMessage());
        }
        try {
            vo.setCompletedToday(Math.toIntExact(seckillGoodsApi.countCompletedToday()));
        } catch (Exception e) {
            log.debug("今日已完成秒杀数采集失败: {}", e.getMessage());
        }
        return vo;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 天数归一化：默认 7，下限 1，上限 30
     */
    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return 7;
        }
        return Math.min(days, TREND_MAX_DAYS);
    }

    /**
     * Top N 归一化：默认 10，下限 1，上限 50
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, RANKING_MAX_LIMIT);
    }

    /**
     * 将 SQL 返回的行列表按日期字符串建立计数索引
     */
    private Map<String, Long> indexByDate(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object dtObj = row.get("dt");
            if (dtObj == null) {
                continue;
            }
            String key = toDateString(dtObj);
            map.put(key, toLong(row.get("cnt")));
        }
        return map;
    }

    /**
     * 按天补零生成趋势列表（按日期升序）
     */
    private List<TrendItemVO> fillTrend(LocalDate startDate, int n, Map<String, Long> countByDate) {
        List<TrendItemVO> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDate cur = startDate.plusDays(i);
            String key = cur.format(DATE_FORMATTER);
            long cnt = countByDate.getOrDefault(key, 0L);
            result.add(new TrendItemVO(key, cnt));
        }
        return result;
    }

    /**
     * 将 SQL 返回的日期对象统一格式化为 yyyy-MM-dd 字符串
     */
    private String toDateString(Object dtObj) {
        if (dtObj == null) {
            return null;
        }
        if (dtObj instanceof LocalDate ld) {
            return ld.format(DATE_FORMATTER);
        }
        if (dtObj instanceof java.time.LocalDateTime ldt) {
            return ldt.toLocalDate().format(DATE_FORMATTER);
        }
        if (dtObj instanceof Date sqlDate) {
            return sqlDate.toLocalDate().format(DATE_FORMATTER);
        }
        if (dtObj instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
        }
        return dtObj.toString();
    }

    /**
     * 安全转 long
     */
    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
