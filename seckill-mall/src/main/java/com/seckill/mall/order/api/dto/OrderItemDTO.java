package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细 DTO。
 *
 * <p>对应 {@code NormalOrderItem} 实体的核心字段，用于模块间通信，
 * 替代直接暴露 Entity/PO。属于 API 层数据契约，与 infrastructure 层隔离。
 *
 * <p>来源映射：NormalOrderItem → OrderItemDTO
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    /** 明细 ID */
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** SKU 属性快照 */
    private String skuAttributes;

    /** 商品名称快照 */
    private String productName;

    /** 商品主图快照 */
    private String productImage;

    /** 商品单价快照 */
    private BigDecimal unitPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal subtotal;
}