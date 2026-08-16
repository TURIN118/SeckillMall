package com.seckill.mall.service;

import com.seckill.mall.dto.ProductSkuDTO;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.vo.ProductSkuVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SKU 服务接口
 * <p>
 * 维护商品 SKU（属性值组合）的查询、保存与价格库存聚合。
 * <p>
 * Phase 8 起，库存扣减/回补操作（deductStock/restoreStock）已迁移至
 * {@link InventoryService}，本接口仅保留查询与聚合方法。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuService.java
 * 邮箱：nj651217@163.com
 */
public interface ProductSkuService {

    /**
     * 查询商品所有 SKU（含禁用，后台用）
     *
     * @param productId 商品 ID
     * @return SKU 视图列表
     */
    List<ProductSkuVO> listByProductId(Long productId);

    /**
     * 查询商品所有启用 SKU（前台用）
     *
     * @param productId 商品 ID
     * @return 启用 SKU 视图列表
     */
    List<ProductSkuVO> listEnabledByProductId(Long productId);

    /**
     * 批量保存商品 SKU（创建 / 更新商品时调用）
     * <p>
     * 采用「先逻辑删除旧 SKU，再插入新 SKU」策略
     *
     * @param productId 商品 ID
     * @param skus      SKU DTO 列表
     */
    void saveSkus(Long productId, List<ProductSkuDTO> skus);

    /**
     * 逻辑删除商品的所有 SKU
     *
     * @param productId 商品 ID
     */
    void deleteByProductId(Long productId);

    /**
     * 根据 SKU ID 获取 SKU 实体（校验启用状态）
     *
     * @param skuId SKU ID
     * @return 启用 SKU 实体，不存在或已禁用返回 null
     */
    ProductSku getByIdEnabled(Long skuId);

    /**
     * 计算商品价格区间最小值
     *
     * @param productId 商品 ID
     * @return 最低 SKU 价格，无启用 SKU 返回 ZERO
     */
    BigDecimal calculateMinPrice(Long productId);

    /**
     * 计算商品价格区间最大值
     *
     * @param productId 商品 ID
     * @return 最高 SKU 价格，无启用 SKU 返回 ZERO
     */
    BigDecimal calculateMaxPrice(Long productId);

    /**
     * 计算商品总库存（所有启用 SKU 库存之和）
     *
     * @param productId 商品 ID
     * @return 总库存
     */
    Integer calculateTotalStock(Long productId);

    /**
     * 同步刷新 t_product 的 total_stock 冗余字段（建议3）
     * <p>
     * 库存扣减/回补后调用，保持列表页展示一致性。
     *
     * @param productId 商品 ID
     */
    void refreshTotalStock(Long productId);
}