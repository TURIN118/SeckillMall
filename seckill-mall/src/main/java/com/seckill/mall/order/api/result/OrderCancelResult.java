package com.seckill.mall.order.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 取消订单结果。
 *
 * <p>由 {@code cancelOrder} 方法返回，包含取消后的订单状态与取消时间。
 *
 * <p>注意：ORDER-API-CONTRACT.md 中 {@code cancelOrder} 返回 {@code OrderDetailDTO}，
 * 本类为 Phase 3.2 实施时按任务要求细化的取消专用结果对象，
 * 仅包含取消后前端需要的核心字段，符合"返回业务结果"的契约原则。
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelResult {

    /** 订单 ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 取消后订单状态码（CANCELLED） */
    private String status;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;
}