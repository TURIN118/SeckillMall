package com.seckill.mall.ai.track.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seckill.mall.ai.track.entity.UserEvent;
import com.seckill.mall.ai.track.entity.UserEventMapper;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 埋点消息消费者
 * <p>从 {@code track.queue} 消费一批 {@link UserEvent}，批量落库到 {@code t_user_event}。
 * <p>幂等去重：用 {@code mq:consumed:{messageId}} 复用现有幂等模式，TTL 24h。
 * <p>批量落库：循环 {@link UserEventMapper#insert(Object)}（MyBatis-Plus 单条 insert
 * 自动填充雪花 ID 与 createTime，无需自定义 XML）。
 * <p>MANUAL ACK：成功 basicAck，失败 basicNack(requeue=false) 丢弃（埋点丢一条可接受，
 * track.queue 未配 DLX，避免毒消息无限重试占用队列）。
 * <p>消息体为 {@code List<UserEvent>}，因 Jackson2JsonMessageConverter 对 List 泛型擦除，
 * 此处用 {@link Message} 原始消息接收 + {@link ObjectMapper} 手动反序列化，确保类型正确。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrackConsumer.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackConsumer {

    private static final long CONSUMED_TTL_HOURS = 24L;

    private final UserEventMapper userEventMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.TRACK_QUEUE, ackMode = "MANUAL")
    public void handleTrack(Message message,
                            Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                            @Header(AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        // 幂等去重：优先用 messageId，缺省用 deliveryTag 兜底
        String dedupId = (messageId != null && !messageId.isBlank())
                ? messageId
                : "track:" + deliveryTag + ":" + System.currentTimeMillis();
        String dedupKey = RedisKeyConstants.mqConsumed(dedupId);

        // 处理前检查幂等键：已存在表示之前已成功处理，直接 ACK 跳过
        // （与 SeckillOrderConsumer 同模式：处理成功后才设置幂等键，避免 nack 后残留导致丢消息）
        String existing = redisService.get(dedupKey);
        if (existing != null) {
            log.debug("埋点消息重复消费，直接丢弃 dedupId={}", dedupId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            // 手动反序列化 List<UserEvent>（规避 Jackson2JsonMessageConverter 泛型擦除）
            List<UserEvent> events = objectMapper.readValue(message.getBody(),
                    new TypeReference<List<UserEvent>>() {});
            if (events == null || events.isEmpty()) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 循环单条 insert：MyBatis-Plus 自动填充雪花 ID（ASSIGN_ID）与 createTime（MetaObjectHandler）
            int success = 0;
            for (UserEvent event : events) {
                try {
                    userEventMapper.insert(event);
                    success++;
                } catch (Exception e) {
                    // 单条失败不中断整批，记录日志继续（埋点丢一条可接受）
                    log.warn("埋点单条落库失败 eventType={} userId={} err={}",
                            event.getEventType(), event.getUserId(), e.getMessage());
                }
            }
            log.debug("埋点批量落库完成 total={} success={}", events.size(), success);

            // 处理成功后设置幂等键（长 TTL）
            redisService.set(dedupKey, "1", CONSUMED_TTL_HOURS, TimeUnit.HOURS);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 反序列化失败或整体异常：nack 不重投，丢弃（track.queue 未配 DLX，埋点丢一批可接受）
            log.error("埋点消费异常，nack 丢弃 dedupId={}", dedupId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}