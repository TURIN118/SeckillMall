package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.product.infrastructure.entity.Product;
import com.seckill.mall.product.infrastructure.entity.ProductSku;
import com.seckill.mall.product.domain.ProductStatus;
import com.seckill.mall.product.infrastructure.mapper.ProductMapper;
import com.seckill.mall.product.infrastructure.mapper.ProductSkuMapper;
import com.seckill.mall.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存服务实现：处理无规格商品（t_product.stock）与 SKU（t_product_sku.stock）的扣减与回补。
 * <p>
 * 从 {@code OrderServiceImpl} 抽取（Phase 4b-3），建立 Inventory 模块边界。
 * Phase 8 起将 SKU 库存操作（deductSkuStock/rollbackSkuStock）从 {@code ProductSkuService}
 * 迁移至此，建立库存操作统一入口。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：InventoryServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public int deductProductStock(Long productId, Integer quantity) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ProductStatus.ON_SALE)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity)
                .setSql("sales_count = sales_count + " + quantity);
        return productMapper.update(null, wrapper);
    }

    @Override
    public void rollbackProductStock(Long productId, Integer quantity) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ProductStatus.ON_SALE)
                .setSql("stock = stock + " + quantity)
                .setSql("sales_count = sales_count - " + quantity);
        int rows = productMapper.update(null, wrapper);
        if (rows == 0) {
            log.warn("回补商品库存失败（商品不存在或已下架），productId={}, qty={}", productId, quantity);
        }
    }

    @Override
    public boolean deductSkuStock(Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity <= 0) {
            return false;
        }
        // 使用参数绑定防 SQL 注入，禁止字符串拼接
        // {0} 为 MyBatis 参数占位符，预编译参数化
        int rows = productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .ge(ProductSku::getStock, quantity)
                .setSql("stock = stock - {0}", quantity));
        return rows > 0;
    }

    @Override
    public void rollbackSkuStock(Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity <= 0) {
            return;
        }
        // 参数绑定防注入
        productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .setSql("stock = stock + {0}", quantity));
    }
}