package com.seckill.mall.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RabbitMQConfig.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ===== 秒杀异步下单（削峰）=====
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

    // ===== 秒杀下单死信队列（H-C6 修复：毒消息进 DLQ，避免丢失）=====
    public static final String SECKILL_ORDER_DLX_EXCHANGE = "seckill.order.dlx.exchange";
    public static final String SECKILL_ORDER_DLQ = "seckill.order.dlq";
    public static final String SECKILL_ORDER_DLX_ROUTING_KEY = "seckill.order.dlx";

    // ===== 订单延迟取消（TTL + DLX）=====
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    // 订单超时时间（毫秒），与 seckill.pay-timeout-minutes=15 保持一致
    public static final int ORDER_TTL_MS = 900000;

    // ===== 秒杀结果广播（fanout）=====
    public static final String SECKILL_RESULT_EXCHANGE = "seckill.result.exchange";
    public static final String SECKILL_RESULT_QUEUE = "seckill.result.queue";

    /**
     * JSON 消息转换器：生产/消费统一使用 Jackson 序列化。
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * H-C5 修复：自定义 RabbitTemplate，开启 publisher confirm / return 回调。
     * <p>
     * 前置条件：application.yml 需配置
     * {@code spring.rabbitmq.publisher-confirm-type=correlated}
     * 与 {@code spring.rabbitmq.publisher-returns=true}（由组 A 负责）。
     * 若未配置，callback 不会被调用，但不会抛异常，仅失去 broker 端 ack/路由失败的感知能力。
     * <p>
     * ConfirmCallback：broker ack=false 时记录日志，便于人工/补偿任务介入。
     * ReturnsCallback：消息路由失败（无队列绑定）时记录日志。
     *
     * @param connectionFactory Spring 自动注入的 CachingConnectionFactory
     * @param messageConverter  JSON 消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // 启用 mandatory：消息无法路由时触发 ReturnsCallback 而非静默丢弃
        template.setMandatory(true);
        // publisher confirm 回调：broker 显式 nack 时记录
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String msgId = correlationData != null ? correlationData.getId() : null;
                log.error("MQ publisher confirm 失败 messageId={} cause={}", msgId, cause);
                // TODO(补偿): 可在此触发重发或落库由补偿任务兜底
            }
        });
        // returns 回调：消息路由到无绑定队列时记录
        template.setReturnsCallback(returned -> {
            log.error("MQ 消息路由失败 exchange={} routingKey={} replyCode={} replyText={} messageId={}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText(),
                    returned.getMessage().getMessageProperties().getMessageId());
        });
        return template;
    }

    // ===== 1. 秒杀下单 direct + DLX（H-C6 修复）=====
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    /**
     * H-C6 修复：秒杀下单队列绑定死信交换器，nack(requeue=false) 时消息进入 DLQ，
     * 避免毒消息被静默丢弃。补偿消费者可从 DLQ 人工/定时介入。
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SECKILL_ORDER_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    // ===== 1.1 秒杀下单死信队列（H-C6 修复）=====
    @Bean
    public DirectExchange seckillOrderDLXExchange() {
        return new DirectExchange(SECKILL_ORDER_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderDLQ() {
        return QueueBuilder.durable(SECKILL_ORDER_DLQ).build();
    }

    @Bean
    public Binding seckillOrderDLXBinding() {
        return BindingBuilder.bind(seckillOrderDLQ())
                .to(seckillOrderDLXExchange())
                .with(SECKILL_ORDER_DLX_ROUTING_KEY);
    }

    // ===== 2. 订单延迟队列 direct + TTL + DLX =====
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .withArgument("x-message-ttl", ORDER_TTL_MS)
                .withArgument("x-dead-letter-exchange", ORDER_DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderDeadExchange() {
        return new DirectExchange(ORDER_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE).build();
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderDeadExchange())
                .with(ORDER_CANCEL_ROUTING_KEY);
    }

    // ===== 3. 秒杀结果广播 fanout =====
    @Bean
    public FanoutExchange seckillResultExchange() {
        return new FanoutExchange(SECKILL_RESULT_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillResultQueue() {
        return QueueBuilder.durable(SECKILL_RESULT_QUEUE).build();
    }

    @Bean
    public Binding seckillResultBinding() {
        return BindingBuilder.bind(seckillResultQueue())
                .to(seckillResultExchange());
    }
}
