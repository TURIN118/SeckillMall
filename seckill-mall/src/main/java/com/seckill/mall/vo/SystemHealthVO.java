package com.seckill.mall.vo;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemHealthVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SystemHealthVO {

    private String redis;

    private String database;

    private String mq;

    /**
     * CPU 使用率（0-100，1 位小数），采集失败为 null
     */
    private Double cpuUsage;

    /**
     * 内存使用率（0-100，1 位小数），采集失败为 null
     */
    private Double memoryUsage;

    /**
     * 磁盘使用率（0-100，1 位小数），采集失败为 null
     */
    private Double diskUsage;

    /**
     * Redis 缓存命中率（0-100，1 位小数，过去 1 小时平均），采集失败为 null
     */
    private Double redisHitRate;

    /**
     * Redis 响应时间（如 "2ms"），采集失败为 null
     */
    private String redisResponseTime;

    /**
     * 数据库连接池使用情况（如 "12/50"），采集失败为 null
     */
    private String dbPoolUsage;

    /**
     * MQ 队列积压（如 "0"），采集失败为 null
     */
    private String mqQueueBacklog;

    /**
     * JVM 堆内存使用率（0-100，1 位小数），采集失败为 null
     */
    private Double jvmHeapUsage;

    /**
     * JVM 非堆内存使用率（0-100，1 位小数），采集失败为 null
     */
    private Double jvmNonHeapUsage;

    /**
     * 数据库连接池活跃连接数，采集失败为 null
     */
    private Integer dbActiveConnections;

    /**
     * 数据库连接池空闲连接数，采集失败为 null
     */
    private Integer dbIdleConnections;

    /**
     * 数据库连接池最大连接数，采集失败为 null
     */
    private Integer dbMaxConnections;

    /**
     * 操作系统名称，采集失败为 null
     */
    private String osName;

    /**
     * JDK 版本，采集失败为 null
     */
    private String jdkVersion;

    /**
     * 应用启动时间（yyyy-MM-dd HH:mm:ss），采集失败为 null
     */
    private String appStartTime;

    /**
     * 应用运行时长（如 "2h 13m"），采集失败为 null
     */
    private String appUptime;


    public boolean isAllHealthy() {
        return "UP".equals(redis) && "UP".equals(database) && "UP".equals(mq);
    }
}
