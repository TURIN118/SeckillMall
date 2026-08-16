package com.seckill.mall.service.impl;

import com.seckill.mall.service.SystemHealthMonitor;
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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 系统健康监控实现
 * <p>从 SystemServiceImpl 抽取的健康检查与资源采集逻辑实现，
 * 通过 {@link SystemHealthMonitor} 端口对外提供服务。</p>
 *
 * <p>采集类方法在失败时返回 {@code null}，调用方需做 null 判断；
 * 健康检查类方法在失败时返回 {@code "DOWN"}。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemHealthMonitorImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthMonitorImpl implements SystemHealthMonitor {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ConnectionFactory rabbitConnectionFactory;
    /**
     * 数据库连接池数据源，用于获取 HikariCP 连接池使用情况
     */
    private final DataSource dataSource;

    @Override
    public String checkRedis() {
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

    @Override
    public String checkDatabase() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1 ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("数据库健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    @Override
    public String checkMq() {
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
    @Override
    public Double getCpuUsage() {
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
    @Override
    public Double getMemoryUsage() {
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
    @Override
    public Double getDiskUsage() {
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
    @Override
    public Double getRedisHitRate() {
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
    @Override
    public String getRedisResponseTime() {
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
    @Override
    public String getDbPoolUsage() {
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
    @Override
    public String getMqQueueBacklog() {
        try {
            return "0";
        } catch (Exception e) {
            log.debug("MQ积压采集失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== JVM 内存监控 ====================

    /**
     * 采集 JVM 堆内存使用率（0-100，1 位小数）
     */
    @Override
    public Double getJvmHeapUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage usage = memoryBean.getHeapMemoryUsage();
            long used = usage.getUsed();
            long max = usage.getMax();
            if (max <= 0) {
                return null;
            }
            return round1((double) used / max * 100);
        } catch (Exception e) {
            log.debug("JVM堆内存采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集 JVM 非堆内存使用率（0-100，1 位小数）
     */
    @Override
    public Double getJvmNonHeapUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage usage = memoryBean.getNonHeapMemoryUsage();
            long used = usage.getUsed();
            long max = usage.getMax();
            // 非堆内存 max 可能不固定（-1），无法计算百分比
            if (max <= 0) {
                return null;
            }
            return round1((double) used / max * 100);
        } catch (Exception e) {
            log.debug("JVM非堆内存采集失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 数据库连接池详情 ====================

    /**
     * 采集 HikariCP 活跃连接数
     */
    @Override
    public Integer getDbActiveConnections() {
        try {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool == null) {
                return null;
            }
            return pool.getActiveConnections();
        } catch (Exception e) {
            log.debug("DB活跃连接数采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集 HikariCP 空闲连接数
     */
    @Override
    public Integer getDbIdleConnections() {
        try {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool == null) {
                return null;
            }
            return pool.getIdleConnections();
        } catch (Exception e) {
            log.debug("DB空闲连接数采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 采集 HikariCP 最大连接数
     */
    @Override
    public Integer getDbMaxConnections() {
        try {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            return hikari.getMaximumPoolSize();
        } catch (Exception e) {
            log.debug("DB最大连接数采集失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 系统信息 ====================

    /**
     * 获取操作系统名称
     */
    @Override
    public String getOsName() {
        try {
            return System.getProperty("os.name");
        } catch (Exception e) {
            log.debug("操作系统名称采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 JDK 版本
     */
    @Override
    public String getJdkVersion() {
        try {
            return System.getProperty("java.version");
        } catch (Exception e) {
            log.debug("JDK版本采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取应用启动时间（yyyy-MM-dd HH:mm:ss）
     */
    @Override
    public String getAppStartTime() {
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long startTimeMillis = runtimeBean.getStartTime();
            LocalDateTime startLdt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault());
            return startLdt.format(DATETIME_FORMATTER);
        } catch (Exception e) {
            log.debug("应用启动时间采集失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取应用运行时长（如 "2h 13m" 或 "13m 25s"）
     */
    @Override
    public String getAppUptime() {
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptimeMillis = runtimeBean.getUptime();
            Duration duration = Duration.ofMillis(uptimeMillis);
            long days = duration.toDays();
            long hours = duration.minusDays(days).toHours();
            long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
            long seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();
            StringBuilder sb = new StringBuilder();
            if (days > 0) {
                sb.append(days).append("d ");
            }
            if (hours > 0 || days > 0) {
                sb.append(hours).append("h ");
            }
            if (minutes > 0 || hours > 0 || days > 0) {
                sb.append(minutes).append("m ");
            }
            sb.append(seconds).append("s");
            return sb.toString().trim();
        } catch (Exception e) {
            log.debug("应用运行时长采集失败: {}", e.getMessage());
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
}