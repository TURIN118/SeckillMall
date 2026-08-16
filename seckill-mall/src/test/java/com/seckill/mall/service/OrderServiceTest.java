package com.seckill.mall.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.Cart;
import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.entity.NormalOrderItem;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.UserAddress;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.NormalOrderItemMapper;
import com.seckill.mall.mapper.NormalOrderMapper;

import com.seckill.mall.service.impl.OrderServiceImpl;
import com.seckill.mall.shared.kernel.port.MessageBusPort;
import com.seckill.mall.vo.NormalOrderDetailVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 普通订单领域服务单元测试（Phase P1-1 从原 OrderServiceTest 拆分而来）。
 * <p>
 * 仅覆盖 {@link OrderServiceImpl} 的普通订单创建用例。
 * 查询用例见 {@link OrderQueryServiceTest}，状态流转用例见 {@link OrderLifecycleServiceTest}。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 2L;

    @Mock
    private NormalOrderMapper normalOrderMapper;
    @Mock
    private NormalOrderItemMapper normalOrderItemMapper;
    @Mock
    private MessageBusPort messageBusPort;
    @Mock
    private ProductSkuService productSkuService;
    @Mock
    private ProductService productService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CouponService couponService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private CartService cartService;
    @Mock
    private UserAddressService userAddressService;

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
    }

    // ==================== 普通订单创建场景测试 ====================

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

    @Test
    @DisplayName("createNormalOrder：无规格商品立即购买成功，扣库存并写入订单+明细")
    void createNormalOrder_shouldCreateOrderAndDeductStock() {
        // given
        given(productService.getProductById(PRODUCT_ID)).willReturn(buildOnSaleProduct());
        given(userAddressService.getAddressById(ADDRESS_ID)).willReturn(buildAddress());
        // 扣减商品库存成功
        given(inventoryService.deductProductStock(any(), any())).willReturn(1);
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
        given(productService.getProductById(PRODUCT_ID)).willReturn(null);

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
        given(productService.getProductById(PRODUCT_ID)).willReturn(p);
        given(userAddressService.getAddressById(ADDRESS_ID)).willReturn(buildAddress());

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
}
