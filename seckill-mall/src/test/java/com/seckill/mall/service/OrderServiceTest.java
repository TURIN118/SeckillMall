package com.seckill.mall.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.cache.SeckillLuaService;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.converter.SeckillOrderConverter;
import com.seckill.mall.entity.Cart;
import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.entity.NormalOrderItem;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.UserAddress;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.CartMapper;
import com.seckill.mall.mapper.NormalOrderItemMapper;
import com.seckill.mall.mapper.NormalOrderMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.mapper.UserAddressMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.impl.OrderServiceImpl;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.OrderListItemVO;
import com.seckill.mall.vo.SeckillOrderVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private SeckillLuaService seckillLuaService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private NormalOrderMapper normalOrderMapper;
    @Mock
    private NormalOrderItemMapper normalOrderItemMapper;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private UserAddressMapper userAddressMapper;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ProductSkuService productSkuService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderServiceImpl orderService;

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
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, NormalOrder.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, NormalOrderItem.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, Product.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, Cart.class);
    }

    @BeforeEach
    void setUp() {
        // @Value 字段不会被 @InjectMocks 注入，手动注入支付超时分钟数
        ReflectionTestUtils.setField(orderService, "payTimeoutMinutes", 15L);
        // 问题4修复：注入真实的 MapStruct Converter 实例（@InjectMocks 无法注入接口）
        ReflectionTestUtils.setField(orderService, "seckillOrderConverter", SeckillOrderConverter.INSTANCE);
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
        SeckillOrderVO paid = orderService.payOrder(USER_ID, ORDER_ID, "ALIPAY");

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
        SeckillOrder cancelledOrder = buildOrder(OrderStatus.CANCELLED);
        cancelledOrder.setCancelTime(LocalDateTime.now());
        cancelledOrder.setCancelReason("用户主动取消");
        // 实现中 cancelOrder 会两次 selectById：首次返回 UNPAID，再次返回 CANCELLED
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(order, cancelledOrder);
        given(seckillOrderMapper.update(any(), any())).willReturn(1);
        given(userMapper.selectById(USER_ID)).willReturn(null);

        // when
        SeckillOrderVO cancelled = orderService.cancelOrder(USER_ID, ORDER_ID);

        // then
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancelTime()).isNotNull();
        assertThat(cancelled.getCancelReason()).isEqualTo("用户主动取消");
        // rollbackStock 已改为调用 seckillLuaService.rollbackDeduct + seckillGoodsMapper.restoreStockOptimistic
        then(seckillGoodsMapper).should().restoreStockOptimistic(SECKILL_ID);
        then(seckillLuaService).should().rollbackDeduct(SECKILL_ID, USER_ID);
        then(redisService).should(never()).incr(any());
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
        boolean result = orderService.timeoutCancel(ORDER_ID);

        // then
        assertThat(result).isTrue();
        // rollbackStock 已改为调用 seckillLuaService.rollbackDeduct + seckillGoodsMapper.restoreStockOptimistic
        then(seckillGoodsMapper).should().restoreStockOptimistic(SECKILL_ID);
        then(seckillLuaService).should().rollbackDeduct(SECKILL_ID, USER_ID);
        then(redisService).should(never()).incr(any());
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
        then(seckillLuaService).should(never()).rollbackDeduct(any(), any());
    }

    // ==================== 普通订单场景测试（Phase 4a 补全） ====================

    private static final Long NORMAL_ORDER_ID = 9001L;
    private static final Long ADDRESS_ID = 7001L;
    private static final Long PRODUCT_ID = 1001L;

    private Product buildOnSaleProduct() {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setName("iPhone 15");
        p.setMainImage("https://cdn.example.com/iphone15.jpg");
        p.setOriginalPrice(new BigDecimal("6999.00"));
        p.setStock(100);
        p.setStatus(ProductStatus.ON_SALE);
        return p;
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

    @Test
    @DisplayName("createNormalOrder：无规格商品立即购买成功，扣库存并写入订单+明细")
    void createNormalOrder_shouldCreateOrderAndDeductStock() {
        // given
        given(productMapper.selectById(PRODUCT_ID)).willReturn(buildOnSaleProduct());
        given(userAddressMapper.selectById(ADDRESS_ID)).willReturn(buildAddress());
        // 扣减商品库存成功
        given(productMapper.update(any(), any())).willReturn(1);
        // normalOrderMapper.insert 设置 id 后会被 assembleDetail 使用
        org.mockito.Mockito.doAnswer(invocation -> {
            NormalOrder o = invocation.getArgument(0);
            o.setId(NORMAL_ORDER_ID);
            return 1;
        }).when(normalOrderMapper).insert(any(NormalOrder.class));

        // when
        NormalOrderDetailVO vo = orderService.createNormalOrder(
                USER_ID, PRODUCT_ID, 0L, 1, ADDRESS_ID, "test", null);

        // then
        assertThat(vo.getOrder().getOrderNo()).startsWith("NO");
        assertThat(vo.getOrder().getUserId()).isEqualTo(USER_ID);
        assertThat(vo.getOrder().getStatus()).isEqualTo(OrderStatus.UNPAID);
        assertThat(vo.getOrder().getTotalAmount()).isEqualByComparingTo("6999.00");
        assertThat(vo.getItems()).hasSize(1);
        assertThat(vo.getItems().get(0).getProductId()).isEqualTo(PRODUCT_ID);
        then(normalOrderMapper).should().insert(any(NormalOrder.class));
        then(normalOrderItemMapper).should().insert(any(NormalOrderItem.class));
    }

    @Test
    @DisplayName("createNormalOrder：商品不存在抛 PRODUCT_NOT_FOUND")
    void createNormalOrder_shouldThrowWhenProductMissing() {
        // given
        given(productMapper.selectById(PRODUCT_ID)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> orderService.createNormalOrder(
                USER_ID, PRODUCT_ID, 0L, 1, ADDRESS_ID, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        then(normalOrderMapper).should(never()).insert(any(NormalOrder.class));
    }

    @Test
    @DisplayName("createNormalOrder：库存不足抛 STOCK_EMPTY")
    void createNormalOrder_shouldThrowWhenStockInsufficient() {
        // given
        Product p = buildOnSaleProduct();
        p.setStock(0); // 库存为 0
        given(productMapper.selectById(PRODUCT_ID)).willReturn(p);
        given(userAddressMapper.selectById(ADDRESS_ID)).willReturn(buildAddress());

        // when / then
        assertThatThrownBy(() -> orderService.createNormalOrder(
                USER_ID, PRODUCT_ID, 0L, 1, ADDRESS_ID, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STOCK_EMPTY);
        then(normalOrderMapper).should(never()).insert(any(NormalOrder.class));
    }

    @Test
    @DisplayName("createOrderFromCart：购物车项为空抛 PARAM_ERROR")
    void createOrderFromCart_shouldThrowWhenCartIdsEmpty() {
        // when / then
        assertThatThrownBy(() -> orderService.createOrderFromCart(
                USER_ID, ADDRESS_ID, Collections.emptyList(), null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
        then(normalOrderMapper).should(never()).insert(any(NormalOrder.class));
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
        // payNormalOrder 流程：loadAndCheckNormalOrderOwnership selectById → update → getNormalOrderDetail selectById → selectList
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(unpaid, paid);
        given(normalOrderMapper.update(any(), any())).willReturn(1);
        given(normalOrderItemMapper.selectList(any())).willReturn(Collections.emptyList());
        given(userAddressMapper.selectById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderService.payNormalOrder(USER_ID, NORMAL_ORDER_ID, "ALIPAY");

        // then
        assertThat(vo.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        then(normalOrderMapper).should().update(any(), any());
    }

    @Test
    @DisplayName("payNormalOrder：已支付订单重复支付抛 ORDER_ALREADY_PAID")
    void payNormalOrder_shouldThrowWhenAlreadyPaid() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.PAID));

        // when / then
        assertThatThrownBy(() -> orderService.payNormalOrder(USER_ID, NORMAL_ORDER_ID, "ALIPAY"))
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
        assertThatThrownBy(() -> orderService.payNormalOrder(USER_ID, NORMAL_ORDER_ID, "ALIPAY"))
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
        given(userAddressMapper.selectById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderService.cancelNormalOrder(USER_ID, NORMAL_ORDER_ID);

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
        given(userAddressMapper.selectById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderService.cancelNormalOrder(USER_ID, NORMAL_ORDER_ID);

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
        given(userMapper.selectById(USER_ID)).willReturn(null);

        // when
        boolean result = orderService.timeoutCancelNormalOrder(NORMAL_ORDER_ID);

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
        boolean result = orderService.timeoutCancelNormalOrder(NORMAL_ORDER_ID);

        // then
        assertThat(result).isFalse();
        then(normalOrderMapper).should(never()).update(any(), any());
    }

    @Test
    @DisplayName("shipOrder：PAID 秒杀订单发货成功，状态置为 SHIPPED")
    void shipOrder_shouldMarkShippedOnPaidOrder() {
        // given
        SeckillOrder paid = buildOrder(OrderStatus.PAID);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(paid);
        given(seckillOrderMapper.updateById(any(SeckillOrder.class))).willReturn(1);

        // when
        orderService.shipOrder(USER_ID, ORDER_ID, "SF", "SF1234567890");

        // then
        then(seckillOrderMapper).should().updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("shipOrder：非 PAID 状态发货抛 ORDER_STATUS_ERROR")
    void shipOrder_shouldThrowWhenNotPaid() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> orderService.shipOrder(USER_ID, ORDER_ID, "SF", "SF1234567890"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(seckillOrderMapper).should(never()).updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("shipNormalOrder：PAID 普通订单发货成功，状态置为 SHIPPED")
    void shipNormalOrder_shouldMarkShippedOnPaidOrder() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.PAID));
        given(normalOrderMapper.updateById(any(NormalOrder.class))).willReturn(1);

        // when
        orderService.shipNormalOrder(USER_ID, NORMAL_ORDER_ID, "SF", "SF1234567890");

        // then
        then(normalOrderMapper).should().updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("shipNormalOrder：非 PAID 状态发货抛 ORDER_STATUS_ERROR")
    void shipNormalOrder_shouldThrowWhenNotPaid() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> orderService.shipNormalOrder(USER_ID, NORMAL_ORDER_ID, "SF", "SF1234567890"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(normalOrderMapper).should(never()).updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("confirmOrder：SHIPPED 秒杀订单确认收货成功，状态置为 COMPLETED")
    void confirmOrder_shouldMarkCompletedOnShippedOrder() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.SHIPPED));
        given(seckillOrderMapper.updateById(any(SeckillOrder.class))).willReturn(1);

        // when
        orderService.confirmOrder(USER_ID, ORDER_ID);

        // then
        then(seckillOrderMapper).should().updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("confirmOrder：非 SHIPPED 状态确认收货抛 ORDER_STATUS_ERROR")
    void confirmOrder_shouldThrowWhenNotShipped() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(buildOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> orderService.confirmOrder(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(seckillOrderMapper).should(never()).updateById(any(SeckillOrder.class));
    }

    @Test
    @DisplayName("confirmNormalOrder：SHIPPED 普通订单确认收货成功，状态置为 COMPLETED")
    void confirmNormalOrder_shouldMarkCompletedOnShippedOrder() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.SHIPPED));
        given(normalOrderMapper.updateById(any(NormalOrder.class))).willReturn(1);

        // when
        orderService.confirmNormalOrder(USER_ID, NORMAL_ORDER_ID);

        // then
        then(normalOrderMapper).should().updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("confirmNormalOrder：非 SHIPPED 状态确认收货抛 ORDER_STATUS_ERROR")
    void confirmNormalOrder_shouldThrowWhenNotShipped() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.UNPAID));

        // when / then
        assertThatThrownBy(() -> orderService.confirmNormalOrder(USER_ID, NORMAL_ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_ERROR);
        then(normalOrderMapper).should(never()).updateById(any(NormalOrder.class));
    }

    @Test
    @DisplayName("getNormalOrderDetail：查询订单详情返回订单+明细+地址")
    void getNormalOrderDetail_shouldReturnOrderDetail() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.UNPAID));
        given(normalOrderItemMapper.selectList(any())).willReturn(List.of(buildNormalOrderItem()));
        given(userAddressMapper.selectById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderService.getNormalOrderDetail(USER_ID, NORMAL_ORDER_ID);

        // then
        assertThat(vo.getOrder().getId()).isEqualTo(NORMAL_ORDER_ID);
        assertThat(vo.getItems()).hasSize(1);
        assertThat(vo.getReceiverName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("getNormalOrderDetail：订单不存在抛 ORDER_NOT_FOUND")
    void getNormalOrderDetail_shouldThrowWhenOrderNotFound() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> orderService.getNormalOrderDetail(USER_ID, NORMAL_ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("getUnifiedOrderList：分页查询用户订单列表（空结果）")
    void getUnifiedOrderList_shouldReturnEmptyPage() {
        // given
        given(seckillOrderMapper.selectList(any())).willReturn(Collections.emptyList());
        given(normalOrderMapper.selectList(any())).willReturn(Collections.emptyList());

        // when
        PageResult<OrderListItemVO> result = orderService.getUnifiedOrderList(
                USER_ID, null, null, 1, 10);

        // then
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    @DisplayName("deleteOrder：秒杀订单 CANCELLED 状态逻辑删除成功")
    void deleteOrder_shouldDeleteCancelledSeckillOrder() {
        // given
        SeckillOrder cancelled = buildOrder(OrderStatus.CANCELLED);
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(cancelled);
        given(seckillOrderMapper.deleteById(ORDER_ID)).willReturn(1);

        // when
        boolean result = orderService.deleteOrder(ORDER_ID, USER_ID);

        // then
        assertThat(result).isTrue();
        then(seckillOrderMapper).should().deleteById(ORDER_ID);
    }

    @Test
    @DisplayName("deleteOrder：订单不存在抛 ORDER_NOT_FOUND")
    void deleteOrder_shouldThrowWhenOrderNotFound() {
        // given
        given(seckillOrderMapper.selectById(ORDER_ID)).willReturn(null);
        given(normalOrderMapper.selectById(ORDER_ID)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }
}
