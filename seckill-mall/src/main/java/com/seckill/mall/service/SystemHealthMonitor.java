package com.seckill.mall.service;

/**
 * 系统健康监控端口
 * <p>从 SystemServiceImpl 抽取的健康检查与资源采集相关方法集合。
 * 实现类 {@link com.seckill.mall.service.impl.SystemHealthMonitorImpl} 负责具体的
 * Redis/DB/MQ 健康检查、CPU/内存/磁盘/JVM 资源采集、数据库连接池详情以及
 * 操作系统/JDK/应用启动信息采集。</p>
 *
 * <p>采集类方法在失败时返回 {@code null}，调用方需做 null 判断；
 * 健康检查类方法在失败时返回 {@code "DOWN"}。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemHealthMonitor.java
 * 邮箱：nj651217@163.com
 */
public interface SystemHealthMonitor {

    /**
     * Redis 健康检查（PING）
     *
     * @return "UP" 或 "DOWN"
     */
    String checkRedis();

    /**
     * 数据库健康检查（SELECT 1）
     *
     * @return "UP" 或 "DOWN"
     */
    String checkDatabase();

    /**
     * RabbitMQ 健康检查（创建并校验连接）
     *
     * @return "UP" 或 "DOWN"
     */
    String checkMq();

    /**
     * 采集系统 CPU 使用率（0-100，1 位小数）
     *
     * @return CPU 使用率，采集失败返回 null
     */
    Double getCpuUsage();

    /**
     * 采集系统物理内存使用率（0-100，1 位小数）
     *
     * @return 内存使用率，采集失败返回 null
     */
    Double getMemoryUsage();

    /**
     * 采集当前工作目录所在磁盘的使用率（0-100，1 位小数）
     *
     * @return 磁盘使用率，采集失败返回 null
     */
    Double getDiskUsage();

    /**
     * 采集 Redis 缓存命中率（0-100，1 位小数，基于 INFO stats 的 keyspace_hits/keyspace_misses）
     *
     * @return 命中率，采集失败返回 null
     */
    Double getRedisHitRate();

    /**
     * 测量 Redis PING 响应耗时（如 "2ms"）
     *
     * @return 响应耗时字符串，采集失败返回 null
     */
    String getRedisResponseTime();

    /**
     * 采集 HikariCP 数据库连接池使用情况（如 "12/50"，active/max）
     *
     * @return 连接池使用情况字符串，采集失败返回 null
     */
    String getDbPoolUsage();

    /**
     * 采集 MQ 队列积压消息数。
     * <p>简化实现：项目队列名未在此上下文可知，且秒杀场景下积压通常为 0；
     * 如需精确值可注入 RabbitAdmin 遍历队列获取 messageCount。</p>
     *
     * @return 积压消息数字符串，采集失败返回 null
     */
    String getMqQueueBacklog();

    /**
     * 采集 JVM 堆内存使用率（0-100，1 位小数）
     *
     * @return 堆内存使用率，采集失败返回 null
     */
    Double getJvmHeapUsage();

    /**
     * 采集 JVM 非堆内存使用率（0-100，1 位小数）
     *
     * @return 非堆内存使用率，采集失败返回 null
     */
    Double getJvmNonHeapUsage();

    /**
     * 采集 HikariCP 活跃连接数
     *
     * @return 活跃连接数，采集失败返回 null
     */
    Integer getDbActiveConnections();

    /**
     * 采集 HikariCP 空闲连接数
     *
     * @return 空闲连接数，采集失败返回 null
     */
    Integer getDbIdleConnections();

    /**
     * 采集 HikariCP 最大连接数
     *
     * @return 最大连接数，采集失败返回 null
     */
    Integer getDbMaxConnections();

    /**
     * 获取操作系统名称
     *
     * @return 操作系统名称，采集失败返回 null
     */
    String getOsName();

    /**
     * 获取 JDK 版本
     *
     * @return JDK 版本字符串，采集失败返回 null
     */
    String getJdkVersion();

    /**
     * 获取应用启动时间（yyyy-MM-dd HH:mm:ss）
     *
     * @return 应用启动时间字符串，采集失败返回 null
     */
    String getAppStartTime();

    /**
     * 获取应用运行时长（如 "2h 13m" 或 "13m 25s"）
     *
     * @return 应用运行时长字符串，采集失败返回 null
     */
    String getAppUptime();
}