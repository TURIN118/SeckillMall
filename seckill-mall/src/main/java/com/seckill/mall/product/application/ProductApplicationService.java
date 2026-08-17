package com.seckill.mall.product.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.command.CreateProductCommand;
import com.seckill.mall.product.api.command.UpdateCartCountCommand;
import com.seckill.mall.product.api.command.UpdateFavoriteCountCommand;
import com.seckill.mall.product.api.command.UpdateProductCommand;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.product.api.dto.ProductSummaryDTO;
import com.seckill.mall.product.api.query.ProductListQuery;
import com.seckill.mall.product.api.result.ProductDetailResult;
import com.seckill.mall.product.application.facade.ProductApiConverter;
import com.seckill.mall.service.ProductService;
import com.seckill.mall.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Product 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link ProductApi}，内部委托给旧 {@link ProductService}，
 * 通过 {@link ProductApiConverter} 做 VO/Entity ↔ DTO/Result/Snapshot 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO/Entity 转换为 API 层 DTO/Result/Snapshot</li>
 * </ol>
 *
 * @author wnj
 * @since Phase P.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductApplicationService implements ProductApi {

    private final ProductService productService;

    @Override
    public PageResult<ProductSummaryDTO> listProducts(ProductListQuery query) {
        PageResult<ProductVO> voPage = productService.listProducts(
                ProductApiConverter.toProductQueryRequest(query));
        List<ProductSummaryDTO> dtoList = voPage.getList().stream()
                .map(ProductApiConverter::toSummaryDTO)
                .collect(Collectors.toList());
        return PageResult.of(dtoList, voPage.getTotal(), voPage.getPageNum(), voPage.getPageSize());
    }

    @Override
    public ProductDetailResult getProductDetail(Long id) {
        ProductVO vo = productService.getProductDetail(id);
        return ProductApiConverter.toDetailResult(vo);
    }

    @Override
    public ProductDetailResult createProduct(CreateProductCommand command) {
        ProductVO vo = productService.createProduct(
                ProductApiConverter.toProductCreateRequest(command));
        return ProductApiConverter.toDetailResult(vo);
    }

    @Override
    public ProductDetailResult updateProduct(UpdateProductCommand command) {
        ProductVO vo = productService.updateProduct(
                command.getId(),
                ProductApiConverter.toProductUpdateRequest(command));
        return ProductApiConverter.toDetailResult(vo);
    }

    @Override
    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }

    @Override
    public boolean existsById(Long id) {
        return productService.existsById(id);
    }

    @Override
    public ProductSnapshot getProductById(Long id) {
        return ProductApiConverter.toSnapshot(productService.getProductById(id));
    }

    @Override
    public List<ProductSnapshot> getProductsByIds(List<Long> ids) {
        return ProductApiConverter.toSnapshotList(productService.getProductsByIds(ids));
    }

    @Override
    public long countAll() {
        return productService.countAll();
    }

    @Override
    public void updateCartCount(UpdateCartCountCommand command) {
        productService.updateCartCount(command.getProductId(), command.getDelta());
    }

    @Override
    public void updateFavoriteCount(UpdateFavoriteCountCommand command) {
        productService.updateFavoriteCount(command.getProductId(), command.getDelta());
    }
}