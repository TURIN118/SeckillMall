package com.seckill.mall.product.api;

import com.seckill.mall.product.api.command.DeductStockCommand;
import com.seckill.mall.product.api.command.RestoreStockCommand;

/**
 * Product 模块库存能力 API。
 *
 * <p>对外暴露库存扣减/回补契约（无规格商品 + SKU）。
 * 统一负责 t_product.stock 与 t_product_sku.stock 的库存操作，建立库存操作统一入口。
 *
 * <p>设计原则：
 * <ul>
 *     <li>通过返回值判断成功/失败，不抛异常（扣减失败返回 0/false）</li>
 *     <li>回补失败静默处理（仅记录日志，不抛异常）</li>
 * </ul>
 *
 * @author wnj
 * @since Phase P.2
 */
public interface InventoryApi {

    /**
     * 乐观锁扣减商品库存与销量（WHERE stock >= quantity AND status=ON_SALE）。
     *
     * @param command 扣减库存命令（productId + quantity）
     * @return 受影响行数（1=成功，0=库存不足或商品不在售）
     */
    int deductProductStock(DeductStockCommand command);

    /**
     * 回补商品库存与销量（乐观锁，仅对在售商品生效；回补失败仅记录日志，不抛异常）。
     *
     * @param command 回补库存命令（productId + quantity）
     */
    void rollbackProductStock(RestoreStockCommand command);

    /**
     * 扣减 SKU 库存（乐观锁，stock = stock - quantity WHERE id = ? AND stock >= ?）。
     *
     * @param command 扣减库存命令（skuId + quantity）
     * @return {@code true} 扣减成功，{@code false} 库存不足或 SKU 不存在
     */
    boolean deductSkuStock(DeductStockCommand command);

    /**
     * 回补 SKU 库存（stock = stock + quantity）。
     *
     * @param command 回补库存命令（skuId + quantity）
     */
    void rollbackSkuStock(RestoreStockCommand command);
}