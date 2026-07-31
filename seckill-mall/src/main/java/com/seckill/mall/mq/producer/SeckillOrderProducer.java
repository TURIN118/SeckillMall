package com.seckill.mall.mq.producer;

import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 投递秒杀下单消息，附带唯一 messageId 供消费者幂等去重。
     */
    public void sendSeckillOrder(SeckillOrderMessage message) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        MessagePostProcessor postProcessor = msg -> {
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        };
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                message,
                postProcessor);
        log.info("投递秒杀下单消息 messageId={} seckillId={} userId={}",
                messageId, message.getSeckillId(), message.getUserId());
    }
}
