package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.system.infrastructure.mapper.OperationLogMapper;
import com.seckill.mall.seckill.api.SeckillOrderApi;
import com.seckill.mall.service.SystemHealthMonitor;
import com.seckill.mall.service.SystemService;
import com.seckill.mall.system.interfaces.vo.OperationLogVO;
import com.seckill.mall.system.interfaces.vo.OrderStatusDistributionVO;
import com.seckill.mall.system.interfaces.vo.OrderTrendVO;
import com.seckill.mall.system.interfaces.vo.SystemHealthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SeckillOrderApi seckillOrderApi;
    private final OperationLogMapper operationLogMapper;
    /**
     * 系统健康监控端口，负责 Redis/DB/MQ 健康检查与资源采集
     */
    private final SystemHealthMonitor systemHealthMonitor;

    @Override

    public PageResult<OperationLogVO> getOperationLogs(OperationLogQueryRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : req.getPageSize();

        Page<OperationLogVO> page = new Page<>(pageNum, pageSize);
        IPage<OperationLogVO> result = operationLogMapper.selectOperationLogVOPage(
                page, req.getModule(), req.getOperatorId());
        List<OperationLogVO> list = result.getRecords() == null
                ? Collections.emptyList() : result.getRecords();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 导出最大条数，防止数据量过大导致 OOM */
    private static final int MAX_EXPORT_SIZE = 10000;

    @Override
    public List<OperationLogVO> listAllForExport(String module) {
        List<OperationLogVO> list = operationLogMapper.selectOperationLogVOList(module, MAX_EXPORT_SIZE);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public SystemHealthVO getSystemHealth() {
        SystemHealthVO vo = new SystemHealthVO();
        vo.setRedis(systemHealthMonitor.checkRedis());
        vo.setDatabase(systemHealthMonitor.checkDatabase());
        vo.setMq(systemHealthMonitor.checkMq());
        // 资源监控字段（可选，采集失败返回 null，不影响主流程）
        vo.setCpuUsage(systemHealthMonitor.getCpuUsage());
        vo.setMemoryUsage(systemHealthMonitor.getMemoryUsage());
        vo.setDiskUsage(systemHealthMonitor.getDiskUsage());
        vo.setRedisHitRate(systemHealthMonitor.getRedisHitRate());
        vo.setRedisResponseTime(systemHealthMonitor.getRedisResponseTime());
        vo.setDbPoolUsage(systemHealthMonitor.getDbPoolUsage());
        vo.setMqQueueBacklog(systemHealthMonitor.getMqQueueBacklog());
        // JVM 内存监控
        vo.setJvmHeapUsage(systemHealthMonitor.getJvmHeapUsage());
        vo.setJvmNonHeapUsage(systemHealthMonitor.getJvmNonHeapUsage());
        // 数据库连接池详情
        vo.setDbActiveConnections(systemHealthMonitor.getDbActiveConnections());
        vo.setDbIdleConnections(systemHealthMonitor.getDbIdleConnections());
        vo.setDbMaxConnections(systemHealthMonitor.getDbMaxConnections());
        // 系统信息
        vo.setOsName(systemHealthMonitor.getOsName());
        vo.setJdkVersion(systemHealthMonitor.getJdkVersion());
        vo.setAppStartTime(systemHealthMonitor.getAppStartTime());
        vo.setAppUptime(systemHealthMonitor.getAppUptime());

        return vo;
    }

    @Override
    public OrderTrendVO getOrderTrend(Integer days) {
        // 天数归一化：默认 7，上限 30，下限 1
        int n = days == null || days < 1 ? 7 : Math.min(days, 30);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(n - 1L);

        // 仅统计 PAID/COMPLETED 订单
        List<Map<String, Object>> rows = seckillOrderApi.selectOrderTrend(
                startDate, endDate, List.of(OrderStatus.PAID, OrderStatus.COMPLETED));

        // 查询结果按日期建立索引，便于按天补零
        Map<String, Map<String, Object>> rowMap = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object dtObj = row.get("dt");
                if (dtObj == null) {
                    continue;
                }
                String dtKey = toDateString(dtObj);
                rowMap.put(dtKey, row);
            }
        }

        OrderTrendVO vo = new OrderTrendVO();
        List<String> dates = new ArrayList<>(n);
        List<Long> orderCounts = new ArrayList<>(n);
        List<BigDecimal> salesAmounts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDate cur = startDate.plusDays(i);
            String key = cur.format(DATE_FORMATTER);
            dates.add(key);

            Map<String, Object> row = rowMap.get(key);
            if (row == null) {
                orderCounts.add(0L);
                salesAmounts.add(BigDecimal.ZERO);
            } else {
                orderCounts.add(toLong(row.get("cnt")));
                salesAmounts.add(toBigDecimal(row.get("amt")));
            }
        }
        vo.setDates(dates);
        vo.setOrderCounts(orderCounts);
        vo.setSalesAmounts(salesAmounts);
        return vo;
    }

    @Override
    public OrderStatusDistributionVO getOrderStatusDistribution(String startTime, String endTime) {
        // 默认近 30 天 ~ 当前
        LocalDateTime startLdt = parseDateTime(startTime, LocalDateTime.now().minusDays(30));
        LocalDateTime endLdt = parseDateTime(endTime, LocalDateTime.now());

        List<Map<String, Object>> rows = seckillOrderApi.selectStatusDistribution(startLdt, endLdt);

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

        // 按 OrderStatus 枚举自然顺序组装，即使 count=0 也输出
        List<OrderStatusDistributionVO.StatusItem> items = new ArrayList<>();
        long total = 0L;
        for (OrderStatus os : OrderStatus.values()) {
            long cnt = countByStatus.getOrDefault(os.getCode(), 0L);
            total += cnt;
            OrderStatusDistributionVO.StatusItem item = new OrderStatusDistributionVO.StatusItem();
            item.setStatus(os.getCode());
            item.setCount(cnt);
            item.setPercentage(0.0d);
            items.add(item);
        }

        // 总数确定后回填百分比（total=0 时保持 0）
        if (total > 0L) {
            BigDecimal hundred = BigDecimal.valueOf(100);
            BigDecimal totalBd = BigDecimal.valueOf(total);
            for (OrderStatusDistributionVO.StatusItem item : items) {
                double pct = BigDecimal.valueOf(item.getCount())
                        .multiply(hundred)
                        .divide(totalBd, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                item.setPercentage(pct);
            }
        }

        OrderStatusDistributionVO vo = new OrderStatusDistributionVO();
        vo.setItems(items);
        vo.setTotal(total);
        return vo;
    }

    /**
     * 将 SQL 返回的日期对象统一格式化为 yyyy-MM-dd 字符串
     */
    private String toDateString(Object dtObj) {
        if (dtObj == null) {
            return null;
        }
        if (dtObj instanceof java.time.LocalDate ld) {
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

    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal bd) {
            return bd;
        }
        if (obj instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("解析BigDecimal失败，obj={}", obj);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析时间字符串，失败时返回默认值
     */
    private LocalDateTime parseDateTime(String text, LocalDateTime defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(text, DATETIME_FORMATTER);
        } catch (Exception e) {
            log.warn("时间解析失败，使用默认值: text={}, default={}, err={}", text, defaultValue, e.getMessage());
            return defaultValue;
        }
    }
}
