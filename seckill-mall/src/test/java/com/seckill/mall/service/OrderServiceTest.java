package com.seckill.mall.service;

import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long SECKILL_ID = 5001L;
    private static final Long USER_ID = 2L;
    private static final Long ORDER_ID = 8001L;

    @Mock
    private SeckillOrderMapper seckillOrderMapper;
    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // @Value 字段不会被 @InjectMocks 注入，手动注入支付超时分钟数
        ReflectionTestUtils.setField(orderService, "payTimeoutMinutes", 15L);
    }

    private SeckillGoods buildGoods() {
        SeckillGoods goods = new SeckillGoods();
        goods.setId(SECKILL_ID);
        goods.setProductId(1001L);
        goods.setSeckillPrice(new BigDecimal("5999.00"));
        return goods;
    }

    private Product buildProduct() {
        Product p = new Product();
        p.setId(1001L);
        p.setName("iPhone");
        return p;
    }

    private SeckillOrder buildOrder(OrderStatus status) {
        SeckillOrder order = new SeckillOrder();
        order.setId(ORDER_ID);
        order.setOrderNo("SK20260731120000");
        order.setUserId(USER_ID);
        order.setSeckillId(SECKILL_ID);
        order.setProductId(1001L);
        order.setSeckillPrice(new BigDecimal("5999.00"));
        order.setQuantity(1);
        order.setTotalAmount(new BigDecimal("5999.00"));
        order.setStatus(status);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(10));
        return order;
    }

    @Test
    @DisplayName("createSeckillOrder：生成订单号并写入，金额=秒杀价×数量")
    void createSeckillOrder_shouldGenerateOrderNoAndInsert() {
        // given
        given(seckillGoodsMapper.selectById(SECKILL_ID)).willReturn(buildGoods());
        given(productMapper.selectById(1001L)).willReturn(buildProduct());

        // when
        SeckillOrder order = orderService.createSeckillOrder(SECKILL_ID, USER_ID, "req-1");

        // then
        assertThat(order.getOrderNo()).startsWith("SK").hasSize(22);
        assertThat(order.getUserId()).isEqualTo(USER_ID);
        assertThat(order.getSeckillId()).isEqualTo(SECKILL_ID);
        assertThat(order.getQuantity()).isEqualTo(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("5999.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.UNPAID);
        assertThat(order.getPayExpireTime()).isNotNull();
        then(seckillOrderMapper).should().insert(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("createSeckillOrder：秒杀商品不存在抛 SECKILL_NOT_FOUND")
    void createSeckillOrder_shouldThrowWhenGoodsMissing() {
        // given
        given(seckillGoodsMapper.selectById(SECKILL_ID)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> orderService.createSeckillOrder(SECKILL_ID, USER_ID, "req-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SECKILL_NOT_FOUND);
        then(seckillOrderMapper).should(never()).insert(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("createSeckillOrder：uk_user_seckill 命中重复键时抛 REPEAT_SECKILL")
    void createSeckillOrder_shouldThrowRepeatOnDuplicateKey() {
        // given
        given(seckillGoodsMapper.selectById(SECKILL_ID)).willReturn(buildGoods());
        given(productMapper.selectById(1001L)).willReturn(buildProduct());
        org.mockito.Mockito.doThrow(new DuplicateKeyException("uk_user_seckill"))
                .when(seckillOrderMapper).insert(any(SeckillOrder.class));

        // when / then
        assertThatThrownBy(() -> orderService.createSeckillOrder(SECKILL_ID, USER_ID, "req-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPEAT_SECKILL);
    }

    @Test
    @DisplayName("payOrder：UNPAID 订单支付成功，状态置为 PAID 并发送邮件")
    void payOrder_shouldMarkPaidAndSendEmail() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order);
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("buyer@seckill.com");
        given(userMapper.selectById(USER_ID)).willReturn(user);

        // when
        SeckillOrder paid = orderService.payOrder(USER_ID, ORDER_ID, "ALIPAY");

        // then
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPayTime()).isNotNull();
        assertThat(paid.getPayMethod()).isEqualTo("ALIPAY");
        assertThat(paid.getTransactionId()).startsWith("PAY");
        then(seckillOrderMapper).should().updateById(any(SeckillOrder.class));
        then(emailService).should().sendPaySuccess(eq("buyer@seckill.com"), eq(order.getOrderNo()), any(BigDecimal.class), any());
    }

    @Test
    @DisplayName("payOrder：已支付订单重复支付抛 ORDER_ALREADY_PAID(3002)")
    void payOrder_shouldThrowWhenAlreadyPaid() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.PAID));

        // when / then
        assertThatThrownBy(() -> orderService.payOrder(USER_ID, ORDER_ID, "ALIPAY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_PAID);
        then(seckillOrderMapper).should(never()).updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("payOrder：超过支付截止时间抛 ORDER_TIMEOUT(3004)")
    void payOrder_shouldThrowWhenExpired() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        order.setPayExpireTime(LocalDateTime.now().minusMinutes(1));
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order);

        // when / then
        assertThatThrownBy(() -> orderService.payOrder(USER_ID, ORDER_ID, "ALIPAY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_TIMEOUT);
    }

    @Test
    @DisplayName("payOrder：订单不属于当前用户抛 ORDER_NOT_FOUND")
    void payOrder_shouldThrowWhenNotOwner() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        order.setUserId(999L);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order);

        // when / then
        assertThatThrownBy(() -> orderService.payOrder(USER_ID, ORDER_ID, "ALIPAY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("cancelOrder：UNPAID 订单取消成功并回补 Redis 库存")
    void cancelOrder_shouldCancelAndRollbackStock() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order);
        given(userMapper.selectById(USER_ID)).willReturn(null);

        // when
        SeckillOrder cancelled = orderService.cancelOrder(USER_ID, ORDER_ID);

        // then
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancelTime()).isNotNull();
        assertThat(cancelled.getCancelReason()).isEqualTo("用户主动取消");
        then(redisService).should().incr(RedisKeyConstants.seckillStock(SECKILL_ID));
        then(redisService).should().sRem(RedisKeyConstants.seckillBought(SECKILL_ID), String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("cancelOrder：非 UNPAID 状态抛 ORDER_CANCEL_FAILED")
    void cancelOrder_shouldThrowWhenNotUnpaid() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.PAID));

        // when / then
        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_FAILED);
        then(redisService).should(never()).incr(any());
    }

    @Test
    @DisplayName("timeoutCancel：UNPAID 订单超时取消并回补库存")
    void timeoutCancel_shouldCancelAndRollback() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order);
        given(userMapper.selectById(USER_ID)).willReturn(null);

        // when
        boolean result = orderService.timeoutCancel(ORDER_ID);

        // then
        assertThat(result).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.TIMEOUT);
        then(redisService).should().incr(RedisKeyConstants.seckillStock(SECKILL_ID));
    }

    @Test
    @DisplayName("timeoutCancel：终态订单幂等忽略返回 false")
    void timeoutCancel_shouldIdempotentlySkipTerminal() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.PAID));

        // when
        boolean result = orderService.timeoutCancel(ORDER_ID);

        // then
        assertThat(result).isFalse();
        then(redisService).should(never()).incr(any());
    }
}
