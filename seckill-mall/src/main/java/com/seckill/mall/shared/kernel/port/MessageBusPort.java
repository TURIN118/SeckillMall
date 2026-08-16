package com.seckill.mall.shared.kernel.port;

/**
 * 消息总线端口（抽象 MQ 消息发送）
 * <p>业务代码应依赖此接口而非 RabbitTemplate 具体实现。
 */
public interface MessageBusPort {

    <T> void publish(String exchange, String routingKey, T message);

    <T> void convertAndSend(String routingKey, T message);
}