package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderService.java
 * 邮箱：nj651217@163.com
 */
public interface OrderService {

    /**
     * 异步下单消费者调用：创建秒杀订单（一人一单由 uk_user_seckill 兜底）。
     */
    SeckillOrder createSeckillOrder(Long seckillId, Long userId, String requestId);

    PageResult<SeckillOrder> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize);

    SeckillOrder getOrderDetail(Long userId, Long orderId);

    SeckillOrder payOrder(Long userId, Long orderId, String payMethod);

    SeckillOrder cancelOrder(Long userId, Long orderId);

    OrderStatus getOrderStatus(Long userId, Long orderId);

    /**
     * 超时取消（延迟消费者触发）：UNPAID → TIMEOUT 并回补库存，已支付等终态幂等忽略。
     *
     * @return true 表示本次执行了超时取消
     */
    boolean timeoutCancel(Long orderId);
}
