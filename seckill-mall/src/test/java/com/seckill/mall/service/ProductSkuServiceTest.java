package com.seckill.mall.service;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.ProductSkuMapper;
import com.seckill.mall.service.impl.ProductSkuServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 库存 Ownership 最小修复：验证 ProductSkuServiceImpl.refreshTotalStock 行为。
 * <p>
 * 场景1：有启用 SKU 时同步刷新 stock 与 totalStock（均为 Σ enabled SKU.stock）。
 * 场景2：无启用 SKU 时不覆盖 t_product.stock，仅对齐冗余字段。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
class ProductSkuServiceTest {

    /**
     * 纯单元测试环境下 MybatisPlus 未初始化实体 lambda cache，
     * LambdaUpdateWrapper.set(Product::getXxx, ...) 会抛
     * "can not find lambda cache for this entity"。
     * 这里手动初始化 Product 实体的 TableInfo，使 lambda 解析可用。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        if (TableInfoHelper.getTableInfo(Product.class) == null) {
            Configuration configuration = new Configuration();
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
            TableInfoHelper.initTableInfo(assistant, Product.class);
        }
    }

    @Mock
    private ProductSkuMapper skuMapper;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductSkuServiceImpl productSkuService;

    @Test
    @DisplayName("refreshTotalStock：有启用 SKU 时同步刷新 stock 与 totalStock")
    void refreshTotalStock_withSkus_shouldSyncStockAndTotalStock() {
        // given
        ProductSku sku1 = new ProductSku();
        sku1.setStock(30);
        sku1.setPrice(new BigDecimal("100"));
        sku1.setStatus(1);
        ProductSku sku2 = new ProductSku();
        sku2.setStock(20);
        sku2.setPrice(new BigDecimal("200"));
        sku2.setStatus(1);
        given(skuMapper.selectEnabledByProductId(1L)).willReturn(List.of(sku1, sku2));
        given(productMapper.update(any(), any())).willReturn(1);

        // when
        productSkuService.refreshTotalStock(1L);

        // then
        then(productMapper).should().update(any(), any());
        then(productMapper).should(never()).selectById(anyLong());
    }

    @Test
    @DisplayName("refreshTotalStock：无启用 SKU 时不覆盖 stock，仅对齐冗余字段")
    void refreshTotalStock_noSkus_shouldNotOverwriteStock() {
        // given
        given(skuMapper.selectEnabledByProductId(1L)).willReturn(List.of());
        Product product = new Product();
        product.setId(1L);
        product.setStock(100);
        product.setOriginalPrice(new BigDecimal("999"));
        given(productMapper.selectById(1L)).willReturn(product);
        given(productMapper.update(any(), any())).willReturn(1);

        // when
        productSkuService.refreshTotalStock(1L);

        // then
        then(productMapper).should().selectById(1L);
        then(productMapper).should().update(any(), any());
    }
}
