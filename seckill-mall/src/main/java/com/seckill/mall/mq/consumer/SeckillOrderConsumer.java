package com.seckill.mall.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.User;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.mq.message.OrderDelayMessage;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.mq.message.SeckillResultMessage;
import com.seckill.mall.service.EmailService;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderConsumer.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private static final long RESULT_TTL_MINUTES = 10L;
    private static final long CONSUMED_TTL_HOURS = 24L;

    private final OrderService orderService;
    private final RabbitTemplate rabbitTemplate;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final ProductMapper productMapper;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillOrderMessage message,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                   @Header(AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        // 幂等去重 key：优先使用消息 ID，缺省时退化为业务维度组合
        String dedupId = (messageId != null && !messageId.isBlank())
                ? messageId
                : message.getSeckillId() + ":" + message.getUserId() + ":" + message.getRequestId();

        Boolean first = redisService.setIfAbsent(
                RedisKeyConstants.mqConsumed(dedupId), "1", CONSUMED_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(first)) {
            log.info("秒杀下单消息重复消费，直接丢弃 dedupId={}", dedupId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            SeckillOrder order = orderService.createSeckillOrder(
                    message.getSeckillId(), message.getUserId(), message.getRequestId());
            // Bug4修复：MQ Consumer创建订单后同步扣减DB的available_count，确保首页进度百分比正确
            try {
                int rows = seckillGoodsMapper.deductStockOptimistic(message.getSeckillId());
                if (rows == 0) {
                    log.warn("MQ Consumer扣减DB库存失败（库存可能已为0），seckillId={}", message.getSeckillId());
                } else {
                    // M6 修复: DB 扣减成功后同步更新 Redis 缓存，避免前端轮询拿到过期库存
                    // 1) Redis 库存计数 -1
                    redisService.decr(RedisKeyConstants.seckillStock(message.getSeckillId()));
                    // 2) 同步 info hash 中的 stock 字段为 DB 最新值
                    SeckillGoods sg = seckillGoodsMapper.selectById(message.getSeckillId());
                    if (sg != null && sg.getAvailableCount() != null) {
                        redisService.hSet(RedisKeyConstants.seckillInfo(message.getSeckillId()),
                                "stock", String.valueOf(sg.getAvailableCount()));
                    }
                }
            } catch (Exception e) {
                log.error("MQ Consumer扣减DB库存异常，seckillId={}", message.getSeckillId(), e);
            }
            writeSuccessResult(message, order);
            sendDelayMessage(order);
            sendResultBroadcast(order, message.getSeckillId());
            // 秒杀成功邮件：异步发送，失败由 @Recover 兜底，不影响下单主流程
            sendSeckillSuccessEmail(order);
            channel.basicAck(deliveryTag, false);
        } catch (BusinessException e) {
            // 业务异常（如重复下单）：写失败结果后 ACK，避免无限重试
            log.warn("秒杀下单业务失败 seckillId={} userId={} code={} msg={}",
                    message.getSeckillId(), message.getUserId(),
                    e.getErrorCode().getCode(), e.getMessage());
            writeFailureResult(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 非业务异常：Nack 不重回，避免毒消息阻塞队列
            log.error("秒杀下单消费异常，将丢弃 dedupId={}", dedupId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void writeSuccessResult(SeckillOrderMessage message, SeckillOrder order) {
        SeckillResultVO vo = new SeckillResultVO();
        vo.setStatus(1);
        vo.setRequestId(message.getRequestId());
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayExpireTime(order.getPayExpireTime());
        writeResult(message.getSeckillId(), message.getUserId(), vo);
    }

    private void writeFailureResult(SeckillOrderMessage message) {
        SeckillResultVO vo = new SeckillResultVO();
        vo.setStatus(-1);
        vo.setRequestId(message.getRequestId());
        writeResult(message.getSeckillId(), message.getUserId(), vo);
    }

    private void writeResult(Long seckillId, Long userId, SeckillResultVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            redisService.set(RedisKeyConstants.seckillResult(seckillId, userId), json,
                    RESULT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("写入秒杀结果失败 seckillId={} userId={}", seckillId, userId, e);
        }
    }

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

    private void sendResultBroadcast(SeckillOrder order, Long seckillId) {
        SeckillResultMessage result = new SeckillResultMessage();
        result.setUserId(order.getUserId());
        result.setSeckillId(seckillId);
        result.setStatus(1);
        result.setOrderId(order.getId());
        result.setOrderNo(order.getOrderNo());
        result.setTotalAmount(order.getTotalAmount());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_RESULT_EXCHANGE, "", result);
    }

    /**
     * 发送秒杀成功邮件：获取用户邮箱与商品名称，异步发送。
     * 任何异常均吞掉，确保不影响下单主流程。
     */
    private void sendSeckillSuccessEmail(SeckillOrder order) {
        try {
            User user = userMapper.selectById(order.getUserId());
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }
            String goodsName = resolveGoodsName(order.getSeckillId());
            emailService.sendSeckillSuccess(user.getEmail(), order.getOrderNo(),
                    goodsName, order.getTotalAmount());
        } catch (Exception e) {
            log.warn("秒杀成功邮件触发失败 orderNo={}", order.getOrderNo(), e);
        }
    }

    private String resolveGoodsName(Long seckillId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            return "秒杀商品";
        }
        Product product = productMapper.selectById(goods.getProductId());
        return product == null ? "秒杀商品" : product.getName();
    }
}
