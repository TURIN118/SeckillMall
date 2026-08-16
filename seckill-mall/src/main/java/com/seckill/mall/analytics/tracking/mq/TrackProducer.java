package com.seckill.mall.analytics.tracking.mq;

import com.seckill.mall.analytics.tracking.entity.UserEvent;
import com.seckill.mall.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 埋点消息生产者
 * <p>将一批 {@link UserEvent} 投递到 {@code track.exchange}，由
 * {@link TrackConsumer} 异步批量落库。
 * <p>投递时携带唯一 {@code messageId}（CorrelationData），供消费者幂等去重，
 * 并配合 RabbitMQConfig 的 ConfirmCallback 实现 broker 端 nack 感知。
 * <p>MQ 宕机时仅记录日志降级（埋点丢一条可接受，不影响主流程）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrackProducer.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 投递一批埋点事件到 MQ。
     *
     * @param events 埋点事件列表（非空）
     */
    public void send(List<UserEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        String messageId = UUID.randomUUID().toString().replace("-", "");
        CorrelationData correlationData = new CorrelationData(messageId);
        MessagePostProcessor postProcessor = msg -> {
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        };
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TRACK_EXCHANGE,
                    RabbitMQConfig.TRACK_ROUTING_KEY,
                    events,
                    postProcessor,
                    correlationData);
            log.debug("投递埋点消息 messageId={} size={}", messageId, events.size());
        } catch (AmqpException e) {
            // MQ 宕机降级：埋点丢一批可接受，仅记录日志，不影响主流程
            log.warn("埋点 MQ 投递失败，丢弃本批 size={} messageId={}", events.size(), messageId, e);
        }
    }
}