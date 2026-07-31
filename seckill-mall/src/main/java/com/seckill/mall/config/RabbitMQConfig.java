package com.seckill.mall.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
@Configuration
public class RabbitMQConfig {

    // ===== 秒杀异步下单（削峰）=====
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

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

    // ===== 1. 秒杀下单 direct =====
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SECKILL_ORDER_ROUTING_KEY);
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
