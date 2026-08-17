package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品快照 DTO（订单列表展示用）。
 *
 * <p>用于订单列表项中展示商品核心信息，来源为订单明细的商品快照字段。
 *
 * <p>来源映射：NormalOrderItem → OrderItemSnapshotDTO
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSnapshotDTO {

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品主图 */
    private String productImage;

    /** 商品单价 */
    private BigDecimal unitPrice;

    /** 购买数量 */
    private Integer quantity;
}