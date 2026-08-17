package com.seckill.mall.order.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单快照（供跨模块传递，替代 NormalOrder Entity）。
 *
 * <p>用于 {@code payment} 等外部模块获取订单核心信息，避免暴露 Entity/PO。
 * 由 {@code getWalletPaidOrders} 等方法返回。
 *
 * <p>来源映射：
 * <ul>
 *     <li>orderId ← NormalOrder.id</li>
 *     <li>orderNo ← NormalOrder.orderNo</li>
 *     <li>userId ← NormalOrder.userId</li>
 *     <li>payAmount ← NormalOrder.payAmount</li>
 *     <li>payMethod ← NormalOrder.payMethod</li>
 *     <li>status ← NormalOrder.status.getCode()</li>
 *     <li>payTime ← NormalOrder.payTime</li>
 *     <li>createTime ← NormalOrder.createTime</li>
 * </ul>
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSnapshot {

    /** 订单 ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付方式 */
    private String payMethod;

    /** 订单状态码 */
    private String status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 下单时间 */
    private LocalDateTime createTime;
}