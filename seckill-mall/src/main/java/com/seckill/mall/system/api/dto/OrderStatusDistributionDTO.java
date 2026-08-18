package com.seckill.mall.system.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单状态分布 DTO - 替代 OrderStatusDistributionVO 作为 Application 层返回值。
 *
 * <p>来源映射：{@code OrderStatusDistributionVO} → {@code OrderStatusDistributionDTO}
 * （由 SystemApiConverter.toDistDTO 转换）。
 *
 * <p>详见 SYSTEM-API-CONTRACT.md §2.4。
 *
 * @author wnj
 * @since Phase SY.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDistributionDTO {

    /** 状态分布项列表，按 OrderStatus 枚举自然顺序输出 */
    private List<StatusItem> items;

    /** 订单总数（各状态 count 之和） */
    private Long total;

    /** 单个状态分布项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusItem {

        /** 状态码：UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED */
        private String status;

        /** 该状态订单数 */
        private Long count;

        /** 占比（0-100，保留 2 位小数） */
        private Double percentage;
    }
}