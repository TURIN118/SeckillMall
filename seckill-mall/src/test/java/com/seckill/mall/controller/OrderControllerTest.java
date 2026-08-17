package com.seckill.mall.controller;

import com.seckill.mall.cache.RedisService;
import com.seckill.mall.order.interfaces.web.OrderController;
import com.seckill.mall.security.JwtUtils;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.security.TokenBlacklistService;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.service.SeckillOrderService;
import com.seckill.mall.vo.SeckillOrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderControllerTest.java
 * 邮箱：nj651217@163.com
 */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    private static final Long USER_ID = 2L;
    private static final Long ORDER_ID = 8001L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;
    @MockBean
    private SeckillOrderService seckillOrderService;
    @MockBean
    private SecurityUtils securityUtils;
    // 安全 Filter 依赖
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private RedisService redisService;

    private SeckillOrderVO buildPaidOrder() {
        SeckillOrderVO order = new SeckillOrderVO();
        order.setId(ORDER_ID);
        order.setOrderNo("SK20260731120000");
        order.setUserId(USER_ID);
        order.setSeckillId(5001L);
        order.setTotalAmount(new BigDecimal("5999.00"));
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setPayMethod("ALIPAY");
        return order;
    }

    @Test
    @DisplayName("pay：确认支付成功返回 PAID 订单")
    void pay_shouldReturnPaidOrder() throws Exception {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(seckillOrderService.payOrder(eq(USER_ID), eq(ORDER_ID), eq("ALIPAY")))
                .willReturn(buildPaidOrder());

        // when / then
        mockMvc.perform(post("/api/v1/orders/{orderId}/pay", ORDER_ID)
                        .param("payMethod", "ALIPAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.payMethod").value("ALIPAY"));
    }

    @Test
    @DisplayName("detail：查询订单详情")
    void detail_shouldReturnOrder() throws Exception {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(seckillOrderService.getOrderDetail(eq(USER_ID), eq(ORDER_ID)))
                .willReturn(buildPaidOrder());

        // when / then
        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(ORDER_ID.intValue()))
                .andExpect(jsonPath("$.data.orderNo").value("SK20260731120000"));
    }

    @Test
    @DisplayName("status：查询订单状态")
    void status_shouldReturnOrderStatus() throws Exception {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(seckillOrderService.getOrderStatus(eq(USER_ID), eq(ORDER_ID)))
                .willReturn("PAID");

        // when / then
        mockMvc.perform(get("/api/v1/orders/{orderId}/status", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("PAID"));
    }
}
