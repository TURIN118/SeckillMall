package com.seckill.mall.service;


import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrderItem;
import com.seckill.mall.seckill.api.SeckillOrderApi;
import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.identity.infrastructure.entity.UserAddress;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderItemMapper;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderMapper;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.service.impl.OrderQueryServiceImpl;
import com.seckill.mall.vo.NormalOrderDetailVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 订单查询领域服务单元测试（Phase P1-1 从 OrderServiceTest 拆分而来）。
 * <p>
 * 覆盖 {@link OrderQueryServiceImpl} 的详情查询 / 统一列表 / 逻辑删除用例。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderQueryServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long ORDER_ID = 8001L;
    private static final Long NORMAL_ORDER_ID = 9001L;
    private static final Long ADDRESS_ID = 7001L;
    private static final Long PRODUCT_ID = 1001L;

    @Mock
    private NormalOrderMapper normalOrderMapper;
    @Mock
    private NormalOrderItemMapper normalOrderItemMapper;
    @Mock
    private UserAddressService userAddressService;
    @Mock
    private ProductApi productApi;
    @Mock
    private SeckillOrderApi seckillOrderApi;
    // 保留 SeckillOrderService：getSeckillOrderById 不在 API 中（API 不暴露 Entity）
    @Mock
    private SeckillOrderService seckillOrderService;

    @InjectMocks
    private OrderQueryServiceImpl orderQueryService;

    /**
     * 初始化 MyBatis-Plus LambdaWrapper 字段缓存。
     */
    @BeforeAll
    static void initLambdaCache() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, SeckillOrder.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, NormalOrder.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, NormalOrderItem.class);
    }

    private NormalOrder buildNormalOrder(OrderStatus status) {
        NormalOrder order = new NormalOrder();
        order.setId(NORMAL_ORDER_ID);
        order.setOrderNo("NO20260731120000");
        order.setUserId(USER_ID);
        order.setAddressId(ADDRESS_ID);
        order.setTotalAmount(new java.math.BigDecimal("6999.00"));
        order.setFreightAmount(java.math.BigDecimal.ZERO);
        order.setPayAmount(new java.math.BigDecimal("6999.00"));
        order.setStatus(status);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(10));
        order.setDiscountAmount(java.math.BigDecimal.ZERO);
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
        item.setUnitPrice(new java.math.BigDecimal("6999.00"));
        item.setQuantity(1);
        item.setSubtotal(new java.math.BigDecimal("6999.00"));
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
    @DisplayName("getNormalOrderDetail：查询订单详情返回订单+明细+地址")
    void getNormalOrderDetail_shouldReturnOrderDetail() {
        // given
        given(normalOrderMapper.selectById(NORMAL_ORDER_ID)).willReturn(buildNormalOrder(OrderStatus.UNPAID));
        given(normalOrderItemMapper.selectList(any())).willReturn(List.of(buildNormalOrderItem()));
        given(userAddressService.getAddressById(ADDRESS_ID)).willReturn(buildAddress());

        // when
        NormalOrderDetailVO vo = orderQueryService.getNormalOrderDetail(USER_ID, NORMAL_ORDER_ID);

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
        assertThatThrownBy(() -> orderQueryService.getNormalOrderDetail(USER_ID, NORMAL_ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("getUnifiedOrderList：分页查询用户订单列表（空结果）")
    void getUnifiedOrderList_shouldReturnEmptyPage() {
        // given
        given(seckillOrderApi.getSeckillOrdersForUnifiedList(any(), any(), org.mockito.ArgumentMatchers.anyInt())).willReturn(Collections.emptyList());
        given(normalOrderMapper.selectList(any())).willReturn(Collections.emptyList());

        // when
        com.seckill.mall.common.PageResult<com.seckill.mall.vo.OrderListItemVO> result = orderQueryService.getUnifiedOrderList(
                USER_ID, null, null, 1, 10);

        // then
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    @DisplayName("deleteOrder：秒杀订单 CANCELLED 状态逻辑删除成功")
    void deleteOrder_shouldDeleteCancelledSeckillOrder() {
        // given
        SeckillOrder cancelled = new SeckillOrder();
        cancelled.setId(ORDER_ID);
        cancelled.setOrderNo("SK20260731120000");
        cancelled.setUserId(USER_ID);
        cancelled.setStatus(OrderStatus.CANCELLED);
        given(seckillOrderService.getSeckillOrderById(ORDER_ID)).willReturn(cancelled);
        given(seckillOrderApi.logicalDeleteSeckillOrder(ORDER_ID)).willReturn(true);

        // when
        boolean result = orderQueryService.deleteOrder(ORDER_ID, USER_ID);

        // then
        assertThat(result).isTrue();
        then(seckillOrderApi).should().logicalDeleteSeckillOrder(ORDER_ID);
    }

    @Test
    @DisplayName("deleteOrder：订单不存在抛 ORDER_NOT_FOUND")
    void deleteOrder_shouldThrowWhenOrderNotFound() {
        // given
        given(seckillOrderService.getSeckillOrderById(ORDER_ID)).willReturn(null);
        given(normalOrderMapper.selectById(ORDER_ID)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> orderQueryService.deleteOrder(ORDER_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }
}