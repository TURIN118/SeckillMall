package com.seckill.mall.mq.producer;

import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderProducer.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderProducer {

    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;

    /**
     * 投递秒杀下单消息，附带唯一 messageId 供消费者幂等去重。
     * MQ 宕机时降级为同步下单：返回非空 SeckillOrder 表示已同步创建，
     * 返回 null 表示 MQ 投递成功（订单由消费者异步创建）。
     */
    public SeckillOrder sendSeckillOrder(SeckillOrderMessage message) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        MessagePostProcessor postProcessor = msg -> {
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        };
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                    RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                    message,
                    postProcessor);
            log.info("投递秒杀下单消息 messageId={} seckillId={} userId={}",
                    messageId, message.getSeckillId(), message.getUserId());
            return null;
        } catch (AmqpException e) {
            // MQ 宕机降级：同步下单，保证业务可用性
            log.warn("MQ 投递失败，降级同步下单 seckillId={} userId={}",
                    message.getSeckillId(), message.getUserId(), e);
            return orderService.createSeckillOrder(
                    message.getSeckillId(), message.getUserId(), message.getRequestId());
        }
    }
}
