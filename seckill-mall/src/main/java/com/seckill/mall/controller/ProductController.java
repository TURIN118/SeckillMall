package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.ProductCreateRequest;
import com.seckill.mall.dto.ProductQueryRequest;
import com.seckill.mall.dto.ProductUpdateRequest;
import com.seckill.mall.service.ProductService;
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

    private final ProductService productService;

    @Operation(summary = "商品列表分页")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@Valid ProductQueryRequest req) {
        return Result.success(productService.listProducts(req));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @Operation(summary = "新增商品")
    @OperationLog(module = "PRODUCT", action = "CREATE", targetType = "PRODUCT")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateRequest req) {
        return Result.success(productService.createProduct(req));
    }

    @Operation(summary = "编辑商品")
    @OperationLog(module = "PRODUCT", action = "UPDATE", targetIdSpEL = "#id", targetType = "PRODUCT")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest req) {
        return Result.success(productService.updateProduct(id, req));
    }

    @Operation(summary = "删除商品（逻辑删除）")
    @OperationLog(module = "PRODUCT", action = "DELETE", targetIdSpEL = "#id", targetType = "PRODUCT")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }
}
