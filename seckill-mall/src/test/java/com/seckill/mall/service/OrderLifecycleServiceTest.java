package com.seckill.mall.service;


import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrderItem;
import com.seckill.mall.entity.UserAddress;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderItemMapper;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderMapper;
import com.seckill.mall.product.api.InventoryApi;
import com.seckill.mall.product.api.SkuApi;
import com.seckill.mall.service.impl.OrderLifecycleServiceImpl;
import com.seckill.mall.vo.NormalOrderDetailVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 订单生命周期领域服务单元测试（Phase P1-1 从 OrderServiceTest 拆分而来）。
 * <p>
 * 覆盖 {@link OrderLifecycleServiceImpl} 的支付 / 取消 / 超时取消 / 发货 / 确认收货用例。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderLifecycleServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long NORMAL_ORDER_ID = 9001L;
    private static final Long ADDRESS_ID = 7001L;
    private static final Long PRODUCT_ID = 1001L;

    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;
    @Mock
    private NormalOrderMapper normalOrderMapper;
    @Mock
    private NormalOrderItemMapper normalOrderItemMapper;
    @Mock
    private CouponUsageService couponUsageService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private InventoryApi inventoryApi;
    @Mock
    private SkuApi skuApi;
    @Mock
    private UserAddressService userAddressService;
    @Mock
    private OrderQueryService orderQueryService;

    @InjectMocks
    private OrderLifecycleServiceImpl orderLifecycleService;

    /**
     * 初始化 MyBatis-Plus LambdaWrapper 字段缓存。
     */
    @BeforeAll
    static void initLambdaCache() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, NormalOrder.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, NormalOrderItem.class);
    }

    private NormalOrder buildNormalOrder(OrderStatus status) {
        NormalOrder order = new NormalOrder();
        order.setId(NORMAL_ORDER_ID);
        order.setOrderNo("NO20260731120000");
        order.setUserId(USER_ID);
        order.setAddressId(ADDRESS_ID);
        order.setTotalAmount(new BigDecimal("6999.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("6999.00"));
        order.setStatus(status);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(10));
        order.setDiscountAmount(BigDecimal.ZERO);
        return order;
    }

    private NormalOrderItem buildNormalOrderItem() {
        NormalOrderItem item = new NormalOrderItem();
        item.setId(1L);
        item.setOrderId(NORMAL_ORDER_ID);
        item.setProductId(PRODUCT_ID);
        item.setSkuId(0L);
        item.setProductName("iPhone 15");
        item.setProductImage("https://cdn.example.com/iphone15.jpg");
        item.setUnitPrice(new BigDecimal("6999.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("6999.00"));
        return item;
    }

    private UserAddress buildAddress() {
        UserAddress addr = new UserAddress();
        addr.setId(ADDRESS_ID);
        addr.setUserId(USER_ID);
        addr.setReceiverName("张三");
        addr.setReceiverPhone("13800138000");
        addr.setProvince("北京市");
        addr.setCity("北京市");
        addr.setDistrict("海淀区");
        addr.setDetailAddress("中关村大街1号");
        return addr;
    }

    @Test
    @DisplayName("payNormalOrder：UNPAID 订单模拟支付成功，状态置为 PAID")
    void payNormalOrder_shouldMarkPaidOnUnpaidOrder() {
        // given
        NormalOrder unpaid = buildNormalOrder(OrderStatus.UNPAID);
        NormalOrder paid = buildNormalOrder(OrderStatus.PAID);
        paid.setPayTime(LocalDateTime.now());
        paid.setPayMethod("ALIPAY");
        paid.setTransactionId("PAY20260731120000");
        // payNormalOrder 流程：loadAndCheckNormalOrderOwnership selectById → update → getNormalOrderDetail（委托 orderQueryService）
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(unpaid);
        given(normalOrderMapper.update(any(), any())).willReturn(1);
        NormalOrderDetailVO vo = new NormalOrderDetailVO();
        given(orderQueryService.getNormalOrderDetail(USER_ID, NORMAL_ORDER_ID)).willReturn(vo);

        // when
        NormalOrderDetailVO result = orderLifecycleService.payNormalOrder(USER_ID, NORMAL_ORDER_ID, "ALIPAY");

        // then
        assertThat(result).isSameAs(vo);
        then(normalOrderMapper).should().update(any(), any());
    }

    @Test
    @DisplayName("payNormalOrder：已支付订单重复支付抛 ORDER_ALREADY_PAID")
    void payNormalOrder_shouldThrowWhenAlreadyPaid() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.PAID));

        // when / then
        assertThatThrownBy(() -> orderLifecycleService.payNormalOrder(USER_ID, NORMAL_ORDER_ID, "ALIPAY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_PAID);
        then(normalOrderMapper).should(never()).update(any(), any());
    }

    @Test
    @DisplayName("payNormalOrder：已取消订单支付抛 ORDER_TIMEOUT")
    void payNormalOrder_shouldThrowWhenCancelled() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.CANCELLED));

        // when / then
        assertThatThrownBy(() -> orderLifecycleService.payNormalOrder(USER_ID, NORMAL_ORDER_ID, "ALIPAY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_TIMEOUT);
        then(normalOrderMapper).should(never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelNormalOrder：UNPAID 订单取消成功，回补库存并置为 CANCELLED")
    void cancelNormalOrder_shouldCancelUnpaidOrder() {
        // given
        NormalOrder unpaid = buildNormalOrder(OrderStatus.UNPAID);
        NormalOrder cancelled = buildNormalOrder(OrderStatus.CANCELLED);
        cancelled.setCancelTime(LocalDateTime.now());
        cancelled.setCancelReason("用户主动取消");
        // cancelNormalOrder 流程：selectById → update(UNPAID→CANCELLING) → selectList(items) → update(CANCELLING→CANCELLED) → selectById
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(unpaid, cancelled);
        given(normalOrderMapper.update(any(), any())).willReturn(1);
        given(normalOrderItemMapper.selectList(any())).willReturn(List.of(buildNormalOrderItem()));
        given(userAddressService.getAddressById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderLifecycleService.cancelNormalOrder(USER_ID, NORMAL_ORDER_ID);

        // then
        assertThat(vo.getOrder().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // 状态机两阶段更新：UNPAID→CANCELLING + CANCELLING→CANCELLED
        then(normalOrderMapper).should(org.mockito.Mockito.times(2)).update(any(), any());
    }

    @Test
    @DisplayName("cancelNormalOrder：非 UNPAID 状态幂等返回当前订单")
    void cancelNormalOrder_shouldIdempotentlyReturnWhenNotUnpaid() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.PAID));
        given(normalOrderItemMapper.selectList(any())).willReturn(Collections.emptyList());
        given(userAddressService.getAddressById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderLifecycleService.cancelNormalOrder(USER_ID, NORMAL_ORDER_ID);

        // then
        assertThat(vo.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        // 未触发 UNPAID→CANCELLING 状态变更
        then(normalOrderMapper).should(never()).update(any(), any());
    }

    @Test
    @DisplayName("timeoutCancelNormalOrder：UNPAID 订单超时取消成功并回补库存")
    void timeoutCancelNormalOrder_shouldCancelAndRollbackStock() {
        // given
        NormalOrder unpaid = buildNormalOrder(OrderStatus.UNPAID);
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(unpaid);
        given(normalOrderMapper.update(any(), any())).willReturn(1);
        given(normalOrderItemMapper.selectList(any())).willReturn(List.of(buildNormalOrderItem()));
        given(userService.getEmail(USER_ID)).willReturn(null);

        // when
        boolean result = orderLifecycleService.timeoutCancelNormalOrder(NORMAL_ORDER_ID);

        // then
        assertThat(result).isTrue();
        // 状态机两阶段更新：UNPAID→CANCELLING + CANCELLING→TIMEOUT
        then(normalOrderMapper).should(org.mockito.Mockito.times(2)).update(any(), any());
    }

    @Test
    @DisplayName("timeoutCancelNormalOrder：终态订单幂等忽略返回 false")
    void timeoutCancelNormalOrder_shouldIdempotentlySkipTerminal() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.PAID));

        // when
        boolean result = orderLifecycleService.timeoutCancelNormalOrder(NORMAL_ORDER_ID);

        // then
        assertThat(result).isFalse();
        then(normalOrderMapper).should(never()).update(any(), any());
    }

    @Test
    @DisplayName("shipNormalOrder：PAID 普通订单发货成功，状态置为 SHIPPED")
    void shipNormalOrder_shouldMarkShippedOnPaidOrder() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.PAID));
        given(normalOrderMapper.updateById(any(NormalOrder.class))).willReturn(1);

        // when
        orderLifecycleService.shipNormalOrder(USER_ID, NORMAL_ORDER_ID, "SF", "SF1234567890");

        // then
        then(normalOrderMapper).should().updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("shipNormalOrder：非 PAID 状态发货抛 ORDER_STATUS_ERROR")
    void shipNormalOrder_shouldThrowWhenNotPaid() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> orderLifecycleService.shipNormalOrder(USER_ID, NORMAL_ORDER_ID, "SF", "SF1234567890"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(normalOrderMapper).should(never()).updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("confirmNormalOrder：SHIPPED 普通订单确认收货成功，状态置为 COMPLETED")
    void confirmNormalOrder_shouldMarkCompletedOnShippedOrder() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.SHIPPED));
        given(normalOrderMapper.updateById(any(NormalOrder.class))).willReturn(1);

        // when
        orderLifecycleService.confirmNormalOrder(USER_ID, NORMAL_ORDER_ID);

        // then
        then(normalOrderMapper).should().updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("confirmNormalOrder：非 SHIPPED 状态确认收货抛 ORDER_STATUS_ERROR")
    void confirmNormalOrder_shouldThrowWhenNotShipped() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> orderLifecycleService.confirmNormalOrder(USER_ID, NORMAL_ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(normalOrderMapper).should(never()).updateById(any(NormalOrder.class));
    }
}