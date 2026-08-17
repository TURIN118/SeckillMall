package com.seckill.mall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.security.JwtUtils;
import com.seckill.mall.security.TokenBlacklistService;
import com.seckill.mall.product.interfaces.web.ProductController;
import com.seckill.mall.service.ProductService;
import com.seckill.mall.vo.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductControllerTest.java
 * 邮箱：nj651217@163.com
 */
@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;
    // 安全 Filter 依赖
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private RedisService redisService;

    @Test
    @DisplayName("list：分页参数非法（pageNum<1）时校验失败")
    void list_shouldFailWhenPageNumInvalid() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/products")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("list：合法分页参数返回成功列表")
    void list_shouldReturnPagedResult() throws Exception {
        // given
        ProductVO vo = new ProductVO();
        vo.setId(1L);
        vo.setProductName("iPhone");
        vo.setOriginalPrice(new BigDecimal("9999.00"));
        given(productService.listProducts(any(ProductQueryRequest.class)))
                .willReturn(PageResult.of(List.of(vo), 1L, 1L, 10L));

        // when / then
        mockMvc.perform(get("/api/v1/products")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].productName").value("iPhone"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("detail：根据 ID 查询商品详情")
    void detail_shouldReturnProduct() throws Exception {
        // given
        ProductVO vo = new ProductVO();
        vo.setId(1001L);
        vo.setProductName("iPhone");
        given(productService.getProductDetail(1001L)).willReturn(vo);

        // when / then
        mockMvc.perform(get("/api/v1/products/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.productName").value("iPhone"));
    }

    @Test
    @DisplayName("create：商品名称缺失时校验失败")
    void create_shouldFailWhenNameBlank() throws Exception {
        // given
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName("");
        req.setCategoryId(101L);
        req.setOriginalPrice(new BigDecimal("9.99"));
        req.setStock(10);

        // when / then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("create：价格为 0 时校验失败")
    void create_shouldFailWhenPriceInvalid() throws Exception {
        // given
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName("test");
        req.setCategoryId(101L);
        req.setOriginalPrice(new BigDecimal("0"));
        req.setStock(10);

        // when / then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
