package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.dto.ProductSkuDTO;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.ProductSkuMapper;
import com.seckill.mall.service.ProductSkuService;
import com.seckill.mall.vo.ProductSkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品 SKU 服务实现
 * <p>
 * 实现要点：
 * <ul>
 *   <li>saveSkus：事务内先 deleteByProductId 物理删除旧 SKU，再遍历插入新 SKU。
 *       skuCode 为空时调用 generateSkuCode 可靠生成（基于 productId + SHA-256 摘要，
 *       避免 hashCode 碰撞风险）。物理删除避免编辑商品时 sku_code 唯一键冲突
 *       （uk_product_sku_code 不包含 is_deleted，逻辑删除后旧记录仍占用唯一键）</li>
 *   <li>calculateMinPrice / calculateMaxPrice / calculateTotalStock：查询启用 SKU 列表后内存聚合。
 *       聚合结果同步写入 t_product.min_price / max_price / total_stock 冗余字段（建议3）</li>
 *   <li>refreshTotalStock：库存变更后同步刷新 t_product.total_stock，保持列表页展示一致性</li>
 * </ul>
 * <p>
 * Phase 8 起，库存扣减/回补操作（deductStock/restoreStock）已迁移至
 * {@link com.seckill.mall.service.InventoryService}，本实现仅保留查询与聚合方法。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {

    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public List<ProductSkuVO> listByProductId(Long productId) {
        if (productId == null) {
            return new ArrayList<>();
        }
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getId));
        return skus.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductSkuVO> listEnabledByProductId(Long productId) {
        if (productId == null) {
            return new ArrayList<>();
        }
        List<ProductSku> skus = skuMapper.selectEnabledByProductId(productId);
        return skus.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkus(Long productId, List<ProductSkuDTO> skus) {
        deleteByProductId(productId);
        if (skus == null || skus.isEmpty()) {
            return;
        }
        for (ProductSkuDTO dto : skus) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            // sku_code NOT NULL，可靠生成（避免 hashCode 碰撞）
            sku.setSkuCode(dto.getSkuCode() != null && !dto.getSkuCode().isEmpty()
                    ? dto.getSkuCode()
                    : generateSkuCode(productId, dto.getAttributes()));
            sku.setPrice(dto.getPrice());
            sku.setStock(dto.getStock());
            sku.setMainImage(dto.getMainImage());
            sku.setAttributes(dto.getAttributes());
            sku.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            skuMapper.insert(sku);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(Long productId) {
        if (productId == null) {
            return;
        }
        // 物理删除：避免 sku_code 唯一键冲突
        // 唯一索引 uk_product_sku_code(product_id, sku_code) 不包含 is_deleted 字段，
        // 逻辑删除后旧记录仍占用唯一键；编辑商品时 SKU 重新生成，
        // 若属性组合不变则 sku_code 重复，插入会触发 DuplicateKeyException。
        // 编辑场景下旧 SKU 不需要保留，故使用物理删除。
        skuMapper.physicalDeleteByProductId(productId);
    }

    @Override
    public ProductSku getByIdEnabled(Long skuId) {
        if (skuId == null) {
            return null;
        }
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            return null;
        }
        return sku;
    }

    @Override
    public BigDecimal calculateMinPrice(Long productId) {
        List<ProductSkuVO> skus = listEnabledByProductId(productId);
        return skus.stream().map(ProductSkuVO::getPrice).min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateMaxPrice(Long productId) {
        List<ProductSkuVO> skus = listEnabledByProductId(productId);
        return skus.stream().map(ProductSkuVO::getPrice).max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Integer calculateTotalStock(Long productId) {
        List<ProductSkuVO> skus = listEnabledByProductId(productId);
        return skus.stream().mapToInt(ProductSkuVO::getStock).sum();
    }

    @Override
    public void refreshTotalStock(Long productId) {
        if (productId == null) {
            return;
        }
        Integer totalStock = calculateTotalStock(productId);
        BigDecimal minPrice = calculateMinPrice(productId);
        BigDecimal maxPrice = calculateMaxPrice(productId);
        // 同步刷新 t_product 冗余字段，保持列表页展示一致性
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getTotalStock, totalStock)
                .set(Product::getMinPrice, minPrice)
                .set(Product::getMaxPrice, maxPrice));
    }

    private ProductSkuVO toVO(ProductSku sku) {
        ProductSkuVO vo = new ProductSkuVO();
        vo.setId(sku.getId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setPrice(sku.getPrice());
        vo.setStock(sku.getStock());
        vo.setMainImage(sku.getMainImage());
        vo.setAttributes(sku.getAttributes());
        vo.setStatus(sku.getStatus());
        return vo;
    }

    /**
     * 可靠生成 SKU 编码（建议12 已落实，与建议2 配合）。
     * <p>
     * 方案：基于 productId + attributesJson 的 SHA-256 摘要（取前 16 位），
     * 确保 sku_code NOT NULL 且同商品内唯一，避免 hashCode 碰撞风险。
     * <p>
     * 注：项目无 commons-codec 依赖，使用 Java 内置 MessageDigest 计算 SHA-256。
     *
     * @param productId       商品 ID
     * @param attributesJson  SKU 属性 JSON，如 {"颜色":"曜石黑","版本":"旗舰版"}
     * @return 可靠的 SKU 编码，非空且同商品内唯一
     */
    private String generateSkuCode(Long productId, String attributesJson) {
        String input = productId + ":" + (attributesJson == null ? "" : attributesJson);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // 取前 8 字节（16 位十六进制字符）
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return "SKU-" + productId + "-" + sb;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 内置算法，理论上不会缺失；兜底使用时间戳
            log.warn("SHA-256 算法不可用，兜底使用时间戳生成 SKU 编码", e);
            return "SKU-" + productId + "-" + System.currentTimeMillis();
        }
    }
}