package com.seckill.mall.service;


import com.seckill.mall.cache.SeckillLuaService;
import com.seckill.mall.converter.SeckillOrderConverter;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.impl.SeckillOrderServiceImpl;
import com.seckill.mall.vo.SeckillOrderVO;
import org.junit.jupiter.api.BeforeAll;
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
 * 秒杀订单领域服务单元测试（Phase 4b-1 从 OrderServiceTest 拆分而来）。
 * <p>
 * 仅覆盖 {@link SeckillOrderServiceImpl} 的秒杀订单用例。
 * 普通订单用例见 {@link OrderServiceTest}。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class SeckillOrderServiceTest {

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
    private SeckillLuaService seckillLuaService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private SeckillOrderConverter seckillOrderConverter;

    @InjectMocks
    private SeckillOrderServiceImpl seckillOrderService;

    /**
     * 初始化 MyBatis-Plus LambdaWrapper 字段缓存。
     * 纯 Mockito 测试无 Spring/MyBatis 上下文，LambdaUpdateWrapper 解析 SFunction 时
     * 需要 TableInfo 缓存，此处手动初始化避免 "can not find lambda cache" 异常。
     */
    @BeforeAll
    static void initLambdaCache() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, SeckillOrder.class);
    }

    @BeforeEach
    void setUp() {
        // @Value 字段不会被 @InjectMocks 注入，手动注入支付超时分钟数
        ReflectionTestUtils.setField(seckillOrderService, "payTimeoutMinutes", 15L);
        // 问题4修复：注入真实的 MapStruct Converter 实例（@InjectMocks 无法注入接口）
        ReflectionTestUtils.setField(seckillOrderService, "seckillOrderConverter", SeckillOrderConverter.INSTANCE);
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
        SeckillOrder order = seckillOrderService.createSeckillOrder(SECKILL_ID, USER_ID, "req-1");

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
        assertThatThrownBy(() -> seckillOrderService.createSeckillOrder(SECKILL_ID, USER_ID, "req-1"))
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
        assertThatThrownBy(() -> seckillOrderService.createSeckillOrder(SECKILL_ID, USER_ID, "req-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPEAT_SECKILL);
    }

    @Test
    @DisplayName("payOrder：UNPAID 订单支付成功，状态置为 PAID 并发送邮件")
    void payOrder_shouldMarkPaidAndSendEmail() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        // 实现中 payOrder 会两次 selectById：首次返回 UNPAID，再次返回 PAID（含 payTime/payMethod/transactionId）
        SeckillOrder paidOrder = buildOrder(OrderStatus.PAID);
        LocalDateTime payTime = LocalDateTime.now();
        paidOrder.setPayTime(payTime);
        paidOrder.setPayMethod("ALIPAY");
        paidOrder.setTransactionId("PAY20260731120000");
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order, paidOrder);
        // 乐观锁更新成功
        given(seckillOrderMapper.update(any(), any())).willReturn(1);
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("buyer@seckill.com");
        given(userMapper.selectById(USER_ID)).willReturn(user);

        // when
        SeckillOrderVO paid = seckillOrderService.payOrder(USER_ID, ORDER_ID, "ALIPAY");

        // then
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(paid.getPayTime()).isNotNull();
        assertThat(paid.getPayMethod()).isEqualTo("ALIPAY");
        assertThat(paid.getTransactionId()).startsWith("PAY");
        then(seckillOrderMapper).should().update(any(), any());
        then(emailService).should().sendPaySuccess(eq("buyer@seckill.com"), eq(order.getOrderNo()), any(BigDecimal.class), any());
    }

    @Test
    @DisplayName("payOrder：已支付订单重复支付抛 ORDER_ALREADY_PAID(3002)")
    void payOrder_shouldThrowWhenAlreadyPaid() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.PAID));

        // when / then
        assertThatThrownBy(() -> seckillOrderService.payOrder(USER_ID, ORDER_ID, "ALIPAY"))
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
        assertThatThrownBy(() -> seckillOrderService.payOrder(USER_ID, ORDER_ID, "ALIPAY"))
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
        assertThatThrownBy(() -> seckillOrderService.payOrder(USER_ID, ORDER_ID, "ALIPAY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("cancelOrder：UNPAID 订单取消成功并回补 Redis 库存")
    void cancelOrder_shouldCancelAndRollbackStock() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        SeckillOrder cancelledOrder = buildOrder(OrderStatus.CANCELLED);
        cancelledOrder.setCancelTime(LocalDateTime.now());
        cancelledOrder.setCancelReason("用户主动取消");
        // 实现中 cancelOrder 会两次 selectById：首次返回 UNPAID，再次返回 CANCELLED
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order, cancelledOrder);
        given(seckillOrderMapper.update(any(), any())).willReturn(1);
        given(userMapper.selectById(USER_ID)).willReturn(null);

        // when
        SeckillOrderVO cancelled = seckillOrderService.cancelOrder(USER_ID, ORDER_ID);

        // then
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancelTime()).isNotNull();
        assertThat(cancelled.getCancelReason()).isEqualTo("用户主动取消");
        // rollbackStock 已改为调用 seckillLuaService.rollbackDeduct + seckillGoodsMapper.restoreStockOptimistic
        then(seckillGoodsMapper).should().restoreStockOptimistic(SECKILL_ID);
        then(seckillLuaService).should().rollbackDeduct(SECKILL_ID, USER_ID);
    }

    @Test
    @DisplayName("cancelOrder：非 UNPAID 状态抛 ORDER_CANCEL_FAILED")
    void cancelOrder_shouldThrowWhenNotUnpaid() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.PAID));

        // when / then
        assertThatThrownBy(() -> seckillOrderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_FAILED);
        then(seckillLuaService).should(never()).rollbackDeduct(any(), any());
    }

    @Test
    @DisplayName("timeoutCancel：UNPAID 订单超时取消并回补库存")
    void timeoutCancel_shouldCancelAndRollback() {
        // given
        SeckillOrder order = buildOrder(OrderStatus.UNPAID);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order);
        given(seckillOrderMapper.update(any(), any())).willReturn(1);
        given(userMapper.selectById(USER_ID)).willReturn(null);

        // when
        boolean result = seckillOrderService.timeoutCancel(ORDER_ID);

        // then
        assertThat(result).isTrue();
        // rollbackStock 已改为调用 seckillLuaService.rollbackDeduct + seckillGoodsMapper.restoreStockOptimistic
        then(seckillGoodsMapper).should().restoreStockOptimistic(SECKILL_ID);
        then(seckillLuaService).should().rollbackDeduct(SECKILL_ID, USER_ID);
    }

    @Test
    @DisplayName("timeoutCancel：终态订单幂等忽略返回 false")
    void timeoutCancel_shouldIdempotentlySkipTerminal() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.PAID));

        // when
        boolean result = seckillOrderService.timeoutCancel(ORDER_ID);

        // then
        assertThat(result).isFalse();
        then(seckillLuaService).should(never()).rollbackDeduct(any(), any());
    }

    @Test
    @DisplayName("shipOrder：PAID 秒杀订单发货成功，状态置为 SHIPPED")
    void shipOrder_shouldMarkShippedOnPaidOrder() {
        // given
        SeckillOrder paid = buildOrder(OrderStatus.PAID);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(paid);
        given(seckillOrderMapper.updateById(any(SeckillOrder.class))).willReturn(1);

        // when
        seckillOrderService.shipOrder(USER_ID, ORDER_ID, "SF", "SF1234567890");

        // then
        then(seckillOrderMapper).should().updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("shipOrder：非 PAID 状态发货抛 ORDER_STATUS_ERROR")
    void shipOrder_shouldThrowWhenNotPaid() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> seckillOrderService.shipOrder(USER_ID, ORDER_ID, "SF", "SF1234567890"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(seckillOrderMapper).should(never()).updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("confirmOrder：SHIPPED 秒杀订单确认收货成功，状态置为 COMPLETED")
    void confirmOrder_shouldMarkCompletedOnShippedOrder() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.SHIPPED));
        given(seckillOrderMapper.updateById(any(SeckillOrder.class))).willReturn(1);

        // when
        seckillOrderService.confirmOrder(USER_ID, ORDER_ID);

        // then
        then(seckillOrderMapper).should().updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("confirmOrder：非 SHIPPED 状态确认收货抛 ORDER_STATUS_ERROR")
    void confirmOrder_shouldThrowWhenNotShipped() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> seckillOrderService.confirmOrder(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(seckillOrderMapper).should(never()).updateById(any(SeckillOrder.class));
    }
}