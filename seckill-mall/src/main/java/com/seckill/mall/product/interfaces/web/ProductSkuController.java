package com.seckill.mall.product.interfaces.web;

import com.seckill.mall.common.Result;
import com.seckill.mall.product.api.SkuApi;
import com.seckill.mall.product.api.dto.SkuSnapshot;
import com.seckill.mall.product.application.facade.ProductApiConverter;
import com.seckill.mall.vo.ProductSkuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品 SKU 控制器
 * <p>
 * 前缀 /api/v1/products/{productId}/skus，公开接口（商品详情页加载 SKU）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "商品SKU", description = "查询商品SKU列表")
@RestController
@RequestMapping("/api/v1/products/{productId}/skus")
@RequiredArgsConstructor
public class ProductSkuController {

    private final SkuApi skuApi;

    @Operation(summary = "查询商品所有启用SKU（前台用）")
    @GetMapping
    public Result<List<ProductSkuVO>> listEnabled(@PathVariable Long productId) {
        List<SkuSnapshot> snapshots = skuApi.listEnabledByProductId(productId);
        return Result.success(ProductApiConverter.toProductSkuVOList(snapshots));
    }
}
