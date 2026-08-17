package com.seckill.mall.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.product.infrastructure.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.product.infrastructure.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.identity.infrastructure.mapper.UserMapper;
import com.seckill.mall.mq.message.OrderDelayMessage;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.mq.message.SeckillResultMessage;
import com.seckill.mall.service.EmailService;
import com.seckill.mall.service.SeckillOrderService;
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

    private final SeckillOrderService seckillOrderService;
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
        String dedupKey = RedisKeyConstants.mqConsumed(dedupId);

        // M-C1 修复：幂等键在"处理成功并提交后"再设置，处理前仅检查是否已存在。
        // 若存在则表示之前已成功处理，直接 ACK 跳过。
        // 这样可避免"处理前设置幂等键 → 处理中异常 nack → 幂等键残留 → 消息重投被跳过"的丢消息问题。
        // 并发重投风险由业务幂等性（uk_user_seckill 唯一索引）兜底。
        String existing = redisService.get(dedupKey);
        if (existing != null) {
            log.info("秒杀下单消息重复消费，直接丢弃 dedupId={}", dedupId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            SeckillOrder order = seckillOrderService.createSeckillOrder(
                    message.getSeckillId(), message.getUserId(), message.getRequestId());

            // H-C2 修复：DB 扣减失败（rows==0）必须撤销订单并写失败结果，不写 writeSuccessResult。
            // 订单创建与 DB 扣减虽不在同一事务，但扣减失败后立即物理删除订单，
            // 配合 uk_user_seckill 唯一索引保证不会产生幽灵单。
            int rows;
            try {
                rows = seckillGoodsMapper.deductStockOptimistic(message.getSeckillId());
            } catch (Exception e) {
                log.error("MQ Consumer 扣减 DB 库存异常，撤销订单 seckillId={} orderNo={}",
                        message.getSeckillId(), order.getOrderNo(), e);
                seckillOrderService.deleteSeckillOrderPhysically(order.getId());
                writeFailureResult(message);
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (rows == 0) {
                log.warn("MQ Consumer 扣减 DB 库存失败（库存已为 0），撤销订单 seckillId={} orderNo={}",
                        message.getSeckillId(), order.getOrderNo());
                seckillOrderService.deleteSeckillOrderPhysically(order.getId());
                writeFailureResult(message);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // B3 修复：Redis 库存同步使用 SET 校正到 DB 真相值，而非 DECR 二次扣减。
            // 明确"Lua 预减是唯一扣减点"，DB available_count 是唯一真相来源。
            // 消费者只负责将 Redis 校正到 DB 值，不再独立扣减。
            syncRedisStockFromDb(message.getSeckillId());

            writeSuccessResult(message, order);
            sendDelayMessage(order);
            sendResultBroadcast(order, message.getSeckillId());
            // 秒杀成功邮件：异步发送，失败由 @Recover 兜底，不影响下单主流程
            sendSeckillSuccessEmail(order);

            // M-C1 修复：处理成功并提交后，设置幂等键（长 TTL）
            redisService.set(dedupKey, "1", CONSUMED_TTL_HOURS, TimeUnit.HOURS);

            channel.basicAck(deliveryTag, false);
        } catch (BusinessException e) {
            // 业务异常（如重复下单）：写失败结果后 ACK，避免无限重试
            log.warn("秒杀下单业务失败 seckillId={} userId={} code={} msg={}",
                    message.getSeckillId(), message.getUserId(),
                    e.getErrorCode().getCode(), e.getMessage());
            writeFailureResult(message);
            // 业务异常也设置幂等键，避免重投重复报错
            redisService.set(dedupKey, "1", CONSUMED_TTL_HOURS, TimeUnit.HOURS);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // H-C6 修复：非业务异常 Nack 不重回，进入 DLQ（队列已配 DLX）。
            // M-C1：nack 时不设置幂等键（未成功处理），允许 DLQ 中消息被重新处理。
            log.error("秒杀下单消费异常，进入 DLQ dedupId={}", dedupId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * B3 修复：将 Redis 库存校正到 DB available_count 真相值。
     * <p>
     * Lua 预减是唯一扣减点，消费者不再 DECR 二次扣减。
     * DB 扣减成功后，将 Redis 的 stockKey 和 info.hash.stock 同步为 DB 最新值，
     * 确保前端轮询拿到准确库存。
     */
    private void syncRedisStockFromDb(Long seckillId) {
        try {
            SeckillGoods sg = seckillGoodsMapper.selectById(seckillId);
            if (sg == null || sg.getAvailableCount() == null) {
                return;
            }
            String stockValue = String.valueOf(sg.getAvailableCount());
            // 1) Redis 库存计数 SET 为 DB 值（校正，而非 DECR）
            redisService.set(RedisKeyConstants.seckillStock(seckillId), stockValue);
            // 2) info hash 中的 stock 字段同步为 DB 值
            redisService.hSet(RedisKeyConstants.seckillInfo(seckillId), "stock", stockValue);
        } catch (Exception e) {
            log.warn("同步 Redis 库存到 DB 值失败 seckillId={}，由补偿任务兜底", seckillId, e);
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
