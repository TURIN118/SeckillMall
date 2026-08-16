package com.seckill.mall.mq.consumer;

import com.rabbitmq.client.Channel;
import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.mq.message.OrderDelayMessage;
import com.seckill.mall.service.OrderLifecycleService;
import com.seckill.mall.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderCancelConsumer.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelConsumer {

    private static final String ORDER_TYPE_NORMAL = "NORMAL";

    private final OrderLifecycleService orderLifecycleService;
    private final SeckillOrderService seckillOrderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCEL_QUEUE)
    public void handleOrderCancel(OrderDelayMessage message,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            String orderType = message.getOrderType();
            boolean cancelled;

            if (ORDER_TYPE_NORMAL.equalsIgnoreCase(orderType)) {
                // Bug1修复：普通订单超时取消
                cancelled = orderLifecycleService.timeoutCancelNormalOrder(message.getOrderId());
                if (cancelled) {
                    log.info("普通订单超时取消成功 orderId={} orderNo={}", message.getOrderId(), message.getOrderNo());
                } else {
                    log.info("普通订单超时取消跳过（已支付或不存在）orderId={}", message.getOrderId());
                }
            } else {
                // 秒杀订单超时取消（默认行为）
                cancelled = seckillOrderService.timeoutCancel(message.getOrderId());
                if (cancelled) {
                    log.info("秒杀订单超时取消成功 orderId={} orderNo={}", message.getOrderId(), message.getOrderNo());
                } else {
                    log.info("秒杀订单超时取消跳过（已支付或不存在）orderId={}", message.getOrderId());
                }
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 异常不重回，避免重复扣减风险；依赖补偿任务兜底
            log.error("订单超时取消消费异常 orderId={}", message.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
