package com.seckill.mall.service;

/**
 * 库存服务：处理商品库存的扣减与回补。
 * <p>
 * Phase 8 起统一负责无规格商品（t_product.stock）与 SKU（t_product_sku.stock）的库存操作，
 * 建立库存操作统一入口。未来可统一为 InventoryPort，适配多数据源（Redis/DB）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：InventoryService.java
 * 邮箱：nj651217@163.com
 */
public interface InventoryService {

    /**
     * 乐观锁扣减商品库存与销量：WHERE stock >= quantity AND status=ON_SALE。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 受影响行数：1=成功，0=库存不足或商品不在售
     */
    int deductProductStock(Long productId, Integer quantity);

    /**
     * 回补商品库存与销量（乐观锁，仅对在售商品生效）。
     * 回补失败仅记录日志，不抛异常。
     *
     * @param productId 商品 ID
     * @param quantity  回补数量
     */
    void rollbackProductStock(Long productId, Integer quantity);

    /**
     * 扣减 SKU 库存（乐观锁，返回是否成功）。
     * <p>
     * 使用参数绑定防 SQL 注入：{@code stock = stock - #{quantity} WHERE id = ? AND stock >= ?}
     *
     * @param skuId    SKU ID
     * @param quantity 扣减数量
     * @return true=扣减成功，false=库存不足或 SKU 不存在
     */
    boolean deductSkuStock(Long skuId, Integer quantity);

    /**
     * 回补 SKU 库存（取消订单 / 超时）。
     * <p>
     * 使用参数绑定防注入：{@code stock = stock + #{quantity}}
     *
     * @param skuId    SKU ID
     * @param quantity 回补数量
     */
    void rollbackSkuStock(Long skuId, Integer quantity);
}