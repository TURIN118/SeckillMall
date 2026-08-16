package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存服务实现：处理无规格商品（t_product.stock）的扣减与回补。
 * <p>
 * 从 {@code OrderServiceImpl} 抽取（Phase 4b-3），建立 Inventory 模块边界。
 * SKU 库存操作仍由 {@code ProductSkuService} 负责。
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
}