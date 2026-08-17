package com.seckill.mall.order.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付订单结果。
 *
 * <p>由 {@code payOrder} 方法返回，包含支付后的订单状态与支付流水信息。
 *
 * <p>注意：ORDER-API-CONTRACT.md 中 {@code payOrder} 返回 {@code OrderDetailDTO}，
 * 本类为 Phase 3.2 实施时按任务要求细化的支付专用结果对象，
 * 仅包含支付后前端需要的核心字段，符合"返回业务结果"的契约原则。
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayResult {

    /** 订单 ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 支付后订单状态码（PAID） */
    private String status;

    /** 支付方式 */
    private String payMethod;

    /** 支付流水号 */
    private String transactionId;

    /** 支付时间 */
    private LocalDateTime payTime;
}