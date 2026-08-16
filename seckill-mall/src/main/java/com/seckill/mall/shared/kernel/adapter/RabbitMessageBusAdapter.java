package com.seckill.mall.shared.kernel.adapter;

import com.seckill.mall.shared.kernel.port.MessageBusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * MessageBusPort 的 RabbitMQ 适配器。
 * <p>
 * 将 RabbitTemplate 的具体实现封装在适配器层，
 * 业务代码依赖 {@link MessageBusPort} 接口而非 RabbitTemplate，
 * 实现基础设施与业务逻辑的解耦。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RabbitMessageBusAdapter.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessageBusAdapter implements MessageBusPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public <T> void publish(String exchange, String routingKey, T message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    @Override
    public <T> void convertAndSend(String routingKey, T message) {
        rabbitTemplate.convertAndSend(routingKey, message);
    }
}