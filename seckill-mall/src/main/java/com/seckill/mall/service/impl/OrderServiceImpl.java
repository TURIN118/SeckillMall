package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 一人一单：当前表结构 uk_user_seckill 约束限购 1 件
    private static final int SECKILL_QUANTITY = 1;

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final ProductMapper productMapper;
    private final RedisService redisService;

    @Value("${seckill.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder createSeckillOrder(Long seckillId, Long userId, String requestId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        Product product = productMapper.selectById(goods.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setSeckillId(seckillId);
        order.setProductId(goods.getProductId());
        order.setSeckillPrice(goods.getSeckillPrice());
        order.setQuantity(SECKILL_QUANTITY);
        order.setTotalAmount(goods.getSeckillPrice().multiply(BigDecimal.valueOf(SECKILL_QUANTITY)));
        order.setStatus(OrderStatus.UNPAID);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));

        try {
            seckillOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // uk_user_seckill 兜底：并发重复下单
            throw new BusinessException(ErrorCode.REPEAT_SECKILL);
        }
        return order;
    }

    @Override
    public PageResult<SeckillOrder> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        Page<SeckillOrder> page = new Page<>(num, size);
        IPage<SeckillOrder> result = seckillOrderMapper.selectOrderPage(page, userId, null, parseStatus(status));
        return PageResult.of(result.getRecords(), result.getTotal(), num, size);
    }

    @Override
    public SeckillOrder getOrderDetail(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder payOrder(Long userId, Long orderId, String payMethod) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        OrderStatus current = order.getStatus();

        if (current == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        if (current == OrderStatus.TIMEOUT) {
            throw new BusinessException(ErrorCode.ORDER_TIMEOUT);
        }
        if (current != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        // UNPAID 但已过支付截止时间（延迟取消任务尚未处理）
        if (order.getPayExpireTime() != null && LocalDateTime.now().isAfter(order.getPayExpireTime())) {
            throw new BusinessException(ErrorCode.ORDER_TIMEOUT);
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayMethod(payMethod);
        order.setTransactionId(generateTransactionId());
        seckillOrderMapper.updateById(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder cancelOrder(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户主动取消");
        seckillOrderMapper.updateById(order);

        // 回补 Redis 库存 + 移除已购标记，保证缓存与可售库存一致
        rollbackStock(order.getSeckillId(), userId);
        return order;
    }

    @Override
    public OrderStatus getOrderStatus(Long userId, Long orderId) {
        return loadAndCheckOwnership(userId, orderId).getStatus();
    }

    /**
     * 超时取消（由延迟消费者触发）：UNPAID → TIMEOUT，并回补库存。
     * 已支付/已取消等终态视为幂等忽略。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean timeoutCancel(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != OrderStatus.UNPAID) {
            return false;
        }
        order.setStatus(OrderStatus.TIMEOUT);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("超时未支付，自动取消");
        seckillOrderMapper.updateById(order);
        rollbackStock(order.getSeckillId(), order.getUserId());
        return true;
    }

    private void rollbackStock(Long seckillId, Long userId) {
        try {
            redisService.incr(RedisKeyConstants.seckillStock(seckillId));
            redisService.sRem(RedisKeyConstants.seckillBought(seckillId), String.valueOf(userId));
        } catch (Exception e) {
            // Redis 异常不阻断主流程，由补偿任务兜底
            log.error("回补 Redis 库存失败 seckillId={} userId={}", seckillId, userId, e);
        }
    }

    private SeckillOrder loadAndCheckOwnership(Long userId, Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderStatus parseStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> OrderStatus.UNPAID;
            case 1 -> OrderStatus.PAID;
            case 2 -> OrderStatus.CANCELLED;
            case 3 -> OrderStatus.TIMEOUT;
            case 4 -> OrderStatus.COMPLETED;
            default -> null;
        };
    }

    private String generateOrderNo() {
        return "SK" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + randomDigits(6);
    }

    private String generateTransactionId() {
        return "PAY" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + randomDigits(6);
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
