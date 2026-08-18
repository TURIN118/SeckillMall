package com.seckill.mall.mapper;

import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.seckill.infrastructure.mapper.SeckillOrderMapper;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderMapperTest.java
 * 邮箱：nj651217@163.com
 */
@SpringBootTest(classes = MapperTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class SeckillOrderMapperTest {

    private static final Long USER_ID = 2001L;
    private static final Long SECKILL_ID = 6001L;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    private SeckillOrder buildOrder(String orderNo, OrderStatus status) {
        SeckillOrder order = new SeckillOrder();
        order.setId(System.currentTimeMillis());
        order.setOrderNo(orderNo);
        order.setUserId(USER_ID);
        order.setSeckillId(SECKILL_ID);
        order.setProductId(1001L);
        order.setSeckillPrice(new BigDecimal("5999.00"));
        order.setQuantity(1);
        order.setTotalAmount(new BigDecimal("5999.00"));
        order.setStatus(status);
        return order;
    }

    @Test
    @DisplayName("insert：写入订单后主键回填")
    void insert_shouldPersistOrder() {
        // given
        SeckillOrder order = buildOrder("SK-MAP-001", OrderStatus.UNPAID);

        // when
        int affected = seckillOrderMapper.insert(order);

        // then
        assertThat(affected).isEqualTo(1);
        assertThat(order.getId()).isNotNull();
    }

    @Test
    @DisplayName("findByUserAndSeckill：根据用户与活动定位一人一单")
    void findByUserAndSeckill_shouldReturnOrder() {
        // given
        seckillOrderMapper.insert(buildOrder("SK-MAP-002", OrderStatus.UNPAID));

        // when
        SeckillOrder found = seckillOrderMapper.findByUserAndSeckill(USER_ID, SECKILL_ID);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo(USER_ID);
        assertThat(found.getSeckillId()).isEqualTo(SECKILL_ID);
    }

    @Test
    @DisplayName("sumSalesAmount：按状态集合汇总销售金额，无数据返回 0")
    void sumSalesAmount_shouldReturnZeroWhenNoMatch() {
        // when
        BigDecimal amount = seckillOrderMapper.sumSalesAmount(List.of(OrderStatus.PAID));

        // then
        assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sumSalesAmount：汇总已支付订单金额")
    void sumSalesAmount_shouldSumPaidOrders() {
        // given
        seckillOrderMapper.insert(buildOrder("SK-MAP-003", OrderStatus.PAID));
        seckillOrderMapper.insert(buildOrder("SK-MAP-004", OrderStatus.PAID));

        // when
        BigDecimal amount = seckillOrderMapper.sumSalesAmount(List.of(OrderStatus.PAID));

        // then
        // 两条 5999.00 已支付订单，合计 11998.00
        assertThat(amount).isEqualByComparingTo(new BigDecimal("11998.00"));
    }
}
