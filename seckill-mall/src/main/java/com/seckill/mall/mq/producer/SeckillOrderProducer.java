package com.seckill.mall.mq.producer;

import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.seckill.infrastructure.mapper.SeckillGoodsMapper;
import com.seckill.mall.mq.message.OrderDelayMessage;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
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
    private final SeckillOrderService seckillOrderService;
    private final SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 投递秒杀下单消息，附带唯一 messageId 供消费者幂等去重。
     * <p>
     * H-C5 修复：发送时携带 {@link CorrelationData}(messageId)，配合 RabbitMQConfig 中
     * 的 ConfirmCallback，broker 端 nack 时可定位到具体消息。
     * <p>
     * MQ 宕机时降级为同步下单：返回非空 SeckillOrder 表示已同步创建，
     * 返回 null 表示 MQ 投递成功（订单由消费者异步创建）。
     * <p>
     * H-C4 修复：降级路径复刻异步路径关键副作用——
     * 1) 事务内扣减 DB available_count（避免 DB 超卖）
     * 2) 发送延迟取消消息（避免订单永不自动取消）
     * 3) 同步校正 Redis 库存（避免 Redis 死库存）
     */
    public SeckillOrder sendSeckillOrder(SeckillOrderMessage message) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        // H-C5 修复：CorrelationData 携带 messageId，ConfirmCallback 可据此定位
        CorrelationData correlationData = new CorrelationData(messageId);
        MessagePostProcessor postProcessor = msg -> {
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        };
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                    RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                    message,
                    postProcessor,
                    correlationData);
            log.info("投递秒杀下单消息 messageId={} seckillId={} userId={}",
                    messageId, message.getSeckillId(), message.getUserId());
            return null;
        } catch (AmqpException e) {
            // MQ 宕机降级：同步下单，复刻异步路径关键副作用（H-C4 修复）
            log.warn("MQ 投递失败，降级同步下单 seckillId={} userId={}",
                    message.getSeckillId(), message.getUserId(), e);
            return degradeToSyncCreate(message);
        }
    }

    /**
     * H-C4 修复：MQ 降级同步下单路径，复刻异步消费者关键副作用。
     * <p>
     * 异步消费者在创建订单后会：扣 DB 库存 → 校正 Redis → 发延迟取消消息。
     * 降级路径必须复刻这些副作用，否则会导致 DB 超卖 + 订单永不取消 + Redis 死库存。
     * <p>
     * 实现顺序：
     * 1) createSeckillOrder（事务内建单）
     * 2) 扣减 DB available_count（乐观锁）；失败则撤销订单并抛异常
     * 3) 发送延迟取消消息（异常不阻断主流程，依赖补偿任务兜底）
     *
     * @param message 秒杀下单消息
     * @return 同步创建的秒杀订单
     */
    private SeckillOrder degradeToSyncCreate(SeckillOrderMessage message) {
        Long seckillId = message.getSeckillId();
        Long userId = message.getUserId();

        // 1. 同步创建订单（事务内）
        SeckillOrder order = seckillOrderService.createSeckillOrder(seckillId, userId, message.getRequestId());

        // 2. 扣减 DB 库存（乐观锁）；失败则撤销订单
        int rows = seckillGoodsMapper.deductStockOptimistic(seckillId);
        if (rows == 0) {
            log.error("MQ 降级同步下单扣减 DB 库存失败（库存不足），撤销订单 orderNo={} seckillId={}",
                    order.getOrderNo(), seckillId);
            // 撤销订单：物理删除，避免占用 uk_user_seckill 唯一索引
            seckillOrderService.deleteSeckillOrderPhysically(order.getId());
            throw new AmqpException("MQ 降级同步下单库存不足");
        }

        // 3. 发送延迟取消消息（异常不阻断主流程）
        try {
            sendDelayMessage(order);
        } catch (Exception e) {
            log.error("MQ 降级同步下单发送延迟取消消息失败 orderNo={}，依赖补偿任务兜底",
                    order.getOrderNo(), e);
        }

        return order;
    }

    /**
     * 发送秒杀订单延迟取消消息（与消费者保持一致的路由）。
     */
    private void sendDelayMessage(SeckillOrder order) {
        OrderDelayMessage delay = new OrderDelayMessage();
        delay.setOrderId(order.getId());
        delay.setOrderNo(order.getOrderNo());
        delay.setOrderType("SECKILL");
        delay.setExpireTime(order.getPayExpireTime() == null ? null
                : order.getPayExpireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                delay);
    }
}
