package com.seckill.mall.product.api;

import com.seckill.mall.product.api.command.SaveSkusCommand;
import com.seckill.mall.product.api.dto.SkuSnapshot;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product 模块 SKU 能力 API。
 *
 * <p>对外暴露 SKU 查询、保存、价格库存聚合等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>出参用 SkuSnapshot，禁止暴露 ProductSku Entity</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出</li>
 * </ul>
 *
 * @author wnj
 * @since Phase P.2
 */
public interface SkuApi {

    /**
     * 查询商品所有 SKU（含禁用，后台用）。
     *
     * @param productId 商品 ID
     * @return SKU 快照列表
     */
    List<SkuSnapshot> listByProductId(Long productId);

    /**
     * 查询商品所有启用 SKU（前台用）。
     *
     * @param productId 商品 ID
     * @return SKU 快照列表
     */
    List<SkuSnapshot> listEnabledByProductId(Long productId);

    /**
     * 批量保存商品 SKU（先逻辑删除旧 SKU，再插入新 SKU）。
     *
     * @param command 保存 SKU 命令
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}
     */
    void saveSkus(SaveSkusCommand command);

    /**
     * 逻辑删除商品的所有 SKU。
     *
     * @param productId 商品 ID
     */
    void deleteByProductId(Long productId);

    /**
     * 根据 SKU ID 获取启用 SKU 快照。
     *
     * @param skuId SKU ID
     * @return SKU 快照（不存在或已禁用返回 null）
     */
    SkuSnapshot getSkuById(Long skuId);

    /**
     * 计算商品价格区间最小值。
     *
     * @param productId 商品 ID
     * @return 最小价格（无启用 SKU 返回 {@code BigDecimal.ZERO}）
     */
    BigDecimal calculateMinPrice(Long productId);

    /**
     * 计算商品价格区间最大值。
     *
     * @param productId 商品 ID
     * @return 最大价格
     */
    BigDecimal calculateMaxPrice(Long productId);

    /**
     * 计算商品总库存（所有启用 SKU 库存之和）。
     *
     * @param productId 商品 ID
     * @return 总库存
     */
    Integer calculateTotalStock(Long productId);

    /**
     * 刷新总库存冗余字段。
     *
     * @param productId 商品 ID
     */
    void refreshTotalStock(Long productId);
}