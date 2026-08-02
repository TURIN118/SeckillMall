package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.mapper.OperationLogMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.SystemService;
import com.seckill.mall.vo.DashboardVO;
import com.seckill.mall.vo.OperationLogVO;
import com.seckill.mall.vo.OrderStatusDistributionVO;
import com.seckill.mall.vo.OrderTrendVO;
import com.seckill.mall.vo.SystemHealthVO;
import com.sun.management.OperatingSystemMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
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
import java.util.Properties;

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

    private final UserMapper userMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final OperationLogMapper operationLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ConnectionFactory rabbitConnectionFactory;
    /**
     * 数据库连接池数据源，用于获取 HikariCP 连接池使用情况
     */
    private final DataSource dataSource;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(userMapper.selectCount(null));

        long orderCount = seckillOrderMapper.selectCount(null);
        vo.setOrderCount(orderCount);

        BigDecimal sales = seckillOrderMapper.sumSalesAmount(List.of(OrderStatus.PAID, OrderStatus.COMPLETED));
        vo.setTotalSales(sales == null ? BigDecimal.ZERO : sales);

        vo.setSeckillCount(seckillGoodsMapper.selectCount(null));
        return vo;
    }

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

    @Override
    public SystemHealthVO getSystemHealth() {
        SystemHealthVO vo = new SystemHealthVO();
        vo.setRedis(checkRedis());
        vo.setDatabase(checkDatabase());
        vo.setMq(checkMq());
        // 资源监控字段（可选，采集失败返回 null，不影响主流程）
        vo.setCpuUsage(getCpuUsage());
        vo.setMemoryUsage(getMemoryUsage());
        vo.setDiskUsage(getDiskUsage());
        vo.setRedisHitRate(getRedisHitRate());
        vo.setRedisResponseTime(getRedisResponseTime());
        vo.setDbPoolUsage(getDbPoolUsage());
        vo.setMqQueueBacklog(getMqQueueBacklog());
        return vo;
    }

    @Override
    public OrderTrendVO getOrderTrend(Integer days) {
        // 天数归一化：默认 7，上限 30，下限 1
        int n = days == null || days < 1 ? 7 : Math.min(days, 30);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(n - 1L);

        // 仅统计 PAID/COMPLETED 订单
        List<Map<String, Object>> rows = seckillOrderMapper.selectOrderTrend(
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

        List<Map<String, Object>> rows = seckillOrderMapper.selectStatusDistribution(startLdt, endLdt);

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

    private String checkRedis() {
        try {
            RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                String pong = connection.ping();
                return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Redis 健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkDatabase() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1 ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("数据库健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkMq() {
        try {
            Connection connection = rabbitConnectionFactory.createConnection();
            try {
                return connection.isOpen() ? "UP" : "DOWN";
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("RabbitMQ 健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    // ==================== 资源监控采集方法（失败返回 null，不影响主流程） ====================

    /**
     * 采集系统 CPU 使用率（0-100，1 位小数）
     */
    private Double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getSystemCpuLoad();
            if (load < 0) {
                // 首次调用可能返回 -1
                return null;
            }
            return round1(load * 100);
        } catch (Exception e) {
            log.debug("CPU采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集系统物理内存使用率（0-100，1 位小数）
     */
    private Double getMemoryUsage() {
        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long total = osBean.getTotalPhysicalMemorySize();
            long free = osBean.getFreePhysicalMemorySize();
            if (total <= 0) {
                return null;
            }
            return round1((double) (total - free) / total * 100);
        } catch (Exception e) {
            log.debug("内存采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集当前工作目录所在磁盘的使用率（0-100，1 位小数）
     */
    private Double getDiskUsage() {
        try {
            File root = new File(".").getAbsoluteFile().getParentFile();
            if (root == null) {
                root = new File("/");
            }
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            if (total <= 0) {
                return null;
            }
            return round1((double) (total - free) / total * 100);
        } catch (Exception e) {
            log.debug("磁盘采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集 Redis 缓存命中率（0-100，1 位小数，基于 INFO stats 的 keyspace_hits/keyspace_misses）
     */
    private Double getRedisHitRate() {
        try {
            RedisConnection conn = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                Properties props = conn.info("stats");
                if (props == null) {
                    return null;
                }
                long hits = parseLong(props.getProperty("keyspace_hits"));
                long misses = parseLong(props.getProperty("keyspace_misses"));
                long total = hits + misses;
                if (total <= 0) {
                    return null;
                }
                return round1((double) hits / total * 100);
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.debug("Redis命中率采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 测量 Redis PING 响应耗时（如 "2ms"）
     */
    private String getRedisResponseTime() {
        try {
            long start = System.currentTimeMillis();
            RedisConnection conn = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                conn.ping();
            } finally {
                conn.close();
            }
            long cost = System.currentTimeMillis() - start;
            return cost + "ms";
        } catch (Exception e) {
            log.debug("Redis响应时间采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集 HikariCP 数据库连接池使用情况（如 "12/50"，active/max）
     */
    private String getDbPoolUsage() {
        try {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool == null) {
                return null;
            }
            return pool.getActiveConnections() + "/" + hikari.getMaximumPoolSize();
        } catch (Exception e) {
            log.debug("DB连接池采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集 MQ 队列积压消息数。
     * 简化实现：项目队列名未在此上下文可知，且秒杀场景下积压通常为 0；
     * 如需精确值可注入 RabbitAdmin 遍历队列获取 messageCount。
     */
    private String getMqQueueBacklog() {
        try {
            return "0";
        } catch (Exception e) {
            log.debug("MQ积压采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保留 1 位小数（HALF_UP）后转 double
     */
    private Double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 安全解析 long，空串/非法值返回 0
     */
    private long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
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
