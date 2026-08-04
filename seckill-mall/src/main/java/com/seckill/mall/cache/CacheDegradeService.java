package com.seckill.mall.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CacheDegradeService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheDegradeService {

    // 健康检测结果缓存窗口：避免每次请求都执行 ping 探测
    private static final long HEALTH_CACHE_TTL_MS = 10_000L;

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private final AtomicReference<HealthSnapshot> redisSnapshot = new AtomicReference<>();
    private final AtomicReference<HealthSnapshot> mqSnapshot = new AtomicReference<>();

    /**
     * 探测 Redis 是否可用：10s 内复用上次结果，避免高频 ping 拖垮请求。
     * <p>
     * L23 并发说明：本方法使用 AtomicReference 实现非阻塞读缓存，存在以下竞态：
     * <ul>
     *   <li>多线程同时过期后会并发执行 pingRedis，造成短暂放大探测，但结果一致，可接受；</li>
     *   <li>set 为最后写入胜出语义，极端情况下可能让一个新结果被旧结果覆盖，
     *       但下次 TTL 过期会自愈，对降级场景影响可忽略。</li>
     * </ul>
     * 若需强一致可改为 synchronized 或 volatile + double-check，但会引入阻塞，得不偿失。
     */
    public boolean isRedisAvailable() {
        HealthSnapshot snapshot = redisSnapshot.get();
        long now = System.currentTimeMillis();
        if (snapshot != null && now - snapshot.timestamp() < HEALTH_CACHE_TTL_MS) {
            return snapshot.available();
        }
        boolean available = pingRedis();
        redisSnapshot.set(new HealthSnapshot(available, now));
        return available;
    }

    /**
     * 探测 RabbitMQ 是否可用：复用同一探测窗口策略。
     */
    public boolean isMqAvailable() {
        HealthSnapshot snapshot = mqSnapshot.get();
        long now = System.currentTimeMillis();
        if (snapshot != null && now - snapshot.timestamp() < HEALTH_CACHE_TTL_MS) {
            return snapshot.available();
        }
        boolean available = pingMq();
        mqSnapshot.set(new HealthSnapshot(available, now));
        return available;
    }

    private boolean pingRedis() {
        // H15 修复：finally 关闭连接，避免连接泄漏
        RedisConnection connection = null;
        try {
            // Lettuce 下 getConnection().ping() 在共享连接上执行 PING 命令
            connection = redisTemplate.getConnectionFactory().getConnection();
            String reply = connection.ping();
            return "PONG".equalsIgnoreCase(reply);
        } catch (Exception e) {
            log.warn("Redis 健康探测失败，触发降级：{}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                    // 关闭连接异常忽略，避免掩盖原始探测结果
                }
            }
        }
    }

    private boolean pingMq() {
        try {
            // 利用 RabbitTemplate 当前连接进行探测；无活动连接时返回 false
            return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> true));
        } catch (Exception e) {
            log.warn("RabbitMQ 健康探测失败，触发降级：{}", e.getMessage());
            return false;
        }
    }

    private record HealthSnapshot(boolean available, long timestamp) {
    }
}
