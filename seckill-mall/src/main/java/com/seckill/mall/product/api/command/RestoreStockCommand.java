package com.seckill.mall.product.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回补库存命令。
 *
 * <p>业务语义：回补商品库存或 SKU 库存。
 *
 * <p>约束：{@code productId} 与 {@code skuId} 二选一
 * （无规格商品传 productId，有规格商品传 skuId）。
 *
 * <p>原方法：{@code InventoryService.rollbackProductStock(Long, Integer)} /
 * {@code InventoryService.rollbackSkuStock(Long, Integer)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreStockCommand {

    /** 商品 ID（无规格商品） */
    private Long productId;

    /** SKU ID（有规格商品） */
    private Long skuId;

    /** 回补数量（≥1，必填） */
    private Integer quantity;
}