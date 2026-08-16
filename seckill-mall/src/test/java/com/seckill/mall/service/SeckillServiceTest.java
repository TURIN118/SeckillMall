package com.seckill.mall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.CacheDegradeService;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.cache.SeckillLuaService;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.SeckillStatus;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mq.message.SeckillOrderMessage;
import com.seckill.mall.mq.producer.SeckillOrderProducer;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.impl.SeckillServiceImpl;
import com.seckill.mall.vo.SeckillResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class SeckillServiceTest {

    private static final Long SECKILL_ID = 5001L;
    private static final Long USER_ID = 2L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Mock
    private com.seckill.mall.service.SeckillGoodsService seckillGoodsService;
    @Mock
    private SeckillTokenService seckillTokenService;
    @Mock
    private SeckillLuaService seckillLuaService;
    @Mock
    private RedisService redisService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SeckillOrderProducer seckillOrderProducer;
    @Mock
    private CacheDegradeService cacheDegradeService;
    @Mock
    private SeckillDbStrategy seckillDbStrategy;
    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    private Map<String, String> activeInfo() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> info = new HashMap<>();
        info.put("status", SeckillStatus.ACTIVE.getCode());
        info.put("startTime", now.minusHours(1).format(FMT));
        info.put("endTime", now.plusHours(1).format(FMT));
        return info;
    }

    @Test
    @DisplayName("doSeckill：活动未开始抛 SECKILL_NOT_STARTED(2002)")
    void doSeckill_shouldThrowWhenNotStarted() {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(true);
        given(seckillTokenService.validateSeckillToken(SECKILL_ID, USER_ID, "token")).willReturn(true);
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> info = new HashMap<>();
        info.put("status", SeckillStatus.ACTIVE.getCode());
        info.put("startTime", now.plusHours(1).format(FMT));
        info.put("endTime", now.plusHours(2).format(FMT));
        given(redisService.hGetAll(RedisKeyConstants.seckillInfo(SECKILL_ID))).willReturn(info);

        // when / then
        assertThatThrownBy(() -> seckillService.doSeckill(SECKILL_ID, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SECKILL_NOT_STARTED);
        then(seckillLuaService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("doSeckill：库存不足抛 STOCK_EMPTY(2003)")
    void doSeckill_shouldThrowWhenStockEmpty() {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(true);
        given(seckillTokenService.validateSeckillToken(SECKILL_ID, USER_ID, "token")).willReturn(true);
        given(redisService.hGetAll(RedisKeyConstants.seckillInfo(SECKILL_ID))).willReturn(activeInfo());
        given(seckillLuaService.deductStock(eq(SECKILL_ID), eq(USER_ID), anyLong())).willReturn(-1L);

        // when / then
        assertThatThrownBy(() -> seckillService.doSeckill(SECKILL_ID, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STOCK_EMPTY);
    }

    @Test
    @DisplayName("doSeckill：重复下单抛 REPEAT_SECKILL(2004)")
    void doSeckill_shouldThrowWhenRepeat() {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(true);
        given(seckillTokenService.validateSeckillToken(SECKILL_ID, USER_ID, "token")).willReturn(true);
        given(redisService.hGetAll(RedisKeyConstants.seckillInfo(SECKILL_ID))).willReturn(activeInfo());
        given(seckillLuaService.deductStock(eq(SECKILL_ID), eq(USER_ID), anyLong())).willReturn(-2L);

        // when / then
        assertThatThrownBy(() -> seckillService.doSeckill(SECKILL_ID, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPEAT_SECKILL);
    }

    @Test
    @DisplayName("doSeckill：秒杀令牌无效抛 SECKILL_TOKEN_INVALID(2006)")
    void doSeckill_shouldThrowWhenTokenInvalid() {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(true);
        given(seckillTokenService.validateSeckillToken(SECKILL_ID, USER_ID, "bad")).willReturn(false);

        // when / then
        assertThatThrownBy(() -> seckillService.doSeckill(SECKILL_ID, "bad"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SECKILL_TOKEN_INVALID);
    }

    @Test
    @DisplayName("doSeckill：成功返回排队中的 requestId（MQ 异步下单）")
    void doSeckill_shouldReturnRequestIdOnSuccess() throws Exception {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(true);
        given(seckillTokenService.validateSeckillToken(SECKILL_ID, USER_ID, "token")).willReturn(true);
        given(redisService.hGetAll(RedisKeyConstants.seckillInfo(SECKILL_ID))).willReturn(activeInfo());
        given(seckillLuaService.deductStock(eq(SECKILL_ID), eq(USER_ID), anyLong())).willReturn(1L);
        given(objectMapper.writeValueAsString(any(SeckillResultVO.class))).willReturn("{}");
        // MQ 投递成功，由消费者异步下单
        given(seckillOrderProducer.sendSeckillOrder(any(SeckillOrderMessage.class))).willReturn(null);

        // when
        SeckillResultVO vo = seckillService.doSeckill(SECKILL_ID, "token");

        // then
        assertThat(vo.getStatus()).isZero();
        assertThat(vo.getRequestId()).isNotBlank();
        then(redisService).should().set(
                eq(RedisKeyConstants.seckillResult(SECKILL_ID, USER_ID)), eq("{}"),
                anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("doSeckill：MQ 宕机降级为同步下单，返回成功结果（status=1）")
    void doSeckill_shouldFallbackToSyncOrderWhenMqDown() throws Exception {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(true);
        given(seckillTokenService.validateSeckillToken(SECKILL_ID, USER_ID, "token")).willReturn(true);
        given(redisService.hGetAll(RedisKeyConstants.seckillInfo(SECKILL_ID))).willReturn(activeInfo());
        given(seckillLuaService.deductStock(eq(SECKILL_ID), eq(USER_ID), anyLong())).willReturn(1L);
        given(objectMapper.writeValueAsString(any(SeckillResultVO.class))).willReturn("{}");
        SeckillOrder syncOrder = new SeckillOrder();
        syncOrder.setId(8001L);
        syncOrder.setOrderNo("SK20260731001");
        syncOrder.setTotalAmount(new BigDecimal("5999.00"));
        syncOrder.setPayExpireTime(LocalDateTime.now().plusMinutes(15));
        given(seckillOrderProducer.sendSeckillOrder(any(SeckillOrderMessage.class))).willReturn(syncOrder);

        // when
        SeckillResultVO vo = seckillService.doSeckill(SECKILL_ID, "token");

        // then
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getOrderId()).isEqualTo(8001L);
        assertThat(vo.getOrderNo()).isEqualTo("SK20260731001");
    }

    @Test
    @DisplayName("doSeckill：Redis 不可用时降级为数据库模式并同步下单")
    void doSeckill_shouldFallbackToDbModeWhenRedisDown() {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(cacheDegradeService.isRedisAvailable()).willReturn(false);
        SeckillGoods goods = new SeckillGoods();
        goods.setId(SECKILL_ID);
        goods.setStartTime(LocalDateTime.now().minusHours(1));
        goods.setEndTime(LocalDateTime.now().plusHours(1));
        goods.setStatus(SeckillStatus.ACTIVE);
        given(seckillGoodsMapper.selectById(SECKILL_ID)).willReturn(goods);
        SeckillOrder order = new SeckillOrder();
        order.setId(8002L);
        order.setOrderNo("SK20260731002");
        given(seckillDbStrategy.executeDbModeSeckill(eq(SECKILL_ID), eq(USER_ID), anyString())).willReturn(order);

        // when
        SeckillResultVO vo = seckillService.doSeckill(SECKILL_ID, "token");

        // then
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getOrderId()).isEqualTo(8002L);
        then(seckillDbStrategy).should().executeDbModeSeckill(eq(SECKILL_ID), eq(USER_ID), anyString());
    }

    @Test
    @DisplayName("getSeckillResult：Redis 无记录时返回 status=-1")
    void getSeckillResult_shouldReturnNotFoundWhenAbsent() {
        // given
        given(securityUtils.getCurrentUserId()).willReturn(USER_ID);
        given(redisService.get(RedisKeyConstants.seckillResult(SECKILL_ID, USER_ID))).willReturn(null);

        // when
        SeckillResultVO vo = seckillService.getSeckillResult(SECKILL_ID, "req-1");

        // then
        assertThat(vo.getStatus()).isEqualTo(-1);
        assertThat(vo.getRequestId()).isEqualTo("req-1");
    }
}
