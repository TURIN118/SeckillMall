package com.seckill.mall.product.interfaces.web;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.command.CreateProductCommand;
import com.seckill.mall.product.api.command.UpdateProductCommand;
import com.seckill.mall.product.api.dto.ProductSummaryDTO;
import com.seckill.mall.product.api.query.ProductListQuery;
import com.seckill.mall.product.api.result.ProductDetailResult;
import com.seckill.mall.product.application.facade.ProductApiConverter;
import com.seckill.mall.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "商品管理", description = "商品 CRUD 与分页")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductApi productApi;

    @Operation(summary = "商品列表分页")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@Valid ProductQueryRequest req) {
        ProductListQuery query = ProductApiConverter.toProductListQuery(req);
        PageResult<ProductSummaryDTO> dtoPage = productApi.listProducts(query);
        return Result.success(ProductApiConverter.toProductVOPage(dtoPage));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        ProductDetailResult result = productApi.getProductDetail(id);
        return Result.success(ProductApiConverter.toProductVO(result));
    }

    @Operation(summary = "新增商品")
    @OperationLog(module = "PRODUCT", action = "CREATE", targetType = "PRODUCT")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateRequest req) {
        CreateProductCommand cmd = ProductApiConverter.toCreateProductCommand(req);
        ProductDetailResult result = productApi.createProduct(cmd);
        return Result.success(ProductApiConverter.toProductVO(result));
    }

    @Operation(summary = "编辑商品")
    @OperationLog(module = "PRODUCT", action = "UPDATE", targetIdSpEL = "#id", targetType = "PRODUCT")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest req) {
        UpdateProductCommand cmd = ProductApiConverter.toUpdateProductCommand(id, req);
        ProductDetailResult result = productApi.updateProduct(cmd);
        return Result.success(ProductApiConverter.toProductVO(result));
    }

    @Operation(summary = "删除商品（逻辑删除）")
    @OperationLog(module = "PRODUCT", action = "DELETE", targetIdSpEL = "#id", targetType = "PRODUCT")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productApi.deleteProduct(id);
        return Result.success();
    }
}
