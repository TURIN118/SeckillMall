package com.seckill.mall.order.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单结果。
 *
 * <p>由 {@code buyNow} 和 {@code createOrder} 方法返回，包含订单核心信息与支付截止时间。
 *
 * <p>来源映射：
 * <ul>
 *     <li>orderId ← NormalOrder.id</li>
 *     <li>orderNo ← NormalOrder.orderNo</li>
 *     <li>totalAmount ← NormalOrder.totalAmount</li>
 *     <li>payAmount ← NormalOrder.payAmount</li>
 *     <li>payDeadline ← NormalOrder.payExpireTime</li>
 * </ul>
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateResult {

    /** 订单 ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 商品总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付截止时间 */
    private LocalDateTime payDeadline;
}