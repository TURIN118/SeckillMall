package com.seckill.mall.product.application;

import com.seckill.mall.product.api.SkuApi;
import com.seckill.mall.product.api.command.SaveSkusCommand;
import com.seckill.mall.product.api.dto.SkuSnapshot;
import com.seckill.mall.product.application.facade.ProductApiConverter;
import com.seckill.mall.service.ProductSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link SkuApi}，内部委托给旧 {@link ProductSkuService}，
 * 通过 {@link ProductApiConverter} 做 VO/Entity ↔ Snapshot 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * @author wnj
 * @since Phase P.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuApplicationService implements SkuApi {

    private final ProductSkuService productSkuService;

    @Override
    public List<SkuSnapshot> listByProductId(Long productId) {
        return ProductApiConverter.toSkuSnapshotListFromVO(
                productSkuService.listByProductId(productId));
    }

    @Override
    public List<SkuSnapshot> listEnabledByProductId(Long productId) {
        return ProductApiConverter.toSkuSnapshotListFromVO(
                productSkuService.listEnabledByProductId(productId));
    }

    @Override
    public void saveSkus(SaveSkusCommand command) {
        productSkuService.saveSkus(
                command.getProductId(),
                ProductApiConverter.toProductSkuDTOList(command.getSkus()));
    }

    @Override
    public void deleteByProductId(Long productId) {
        productSkuService.deleteByProductId(productId);
    }

    @Override
    public SkuSnapshot getSkuById(Long skuId) {
        return ProductApiConverter.toSkuSnapshot(productSkuService.getByIdEnabled(skuId));
    }

    @Override
    public BigDecimal calculateMinPrice(Long productId) {
        return productSkuService.calculateMinPrice(productId);
    }

    @Override
    public BigDecimal calculateMaxPrice(Long productId) {
        return productSkuService.calculateMaxPrice(productId);
    }

    @Override
    public Integer calculateTotalStock(Long productId) {
        return productSkuService.calculateTotalStock(productId);
    }

    @Override
    public void refreshTotalStock(Long productId) {
        productSkuService.refreshTotalStock(productId);
    }
}