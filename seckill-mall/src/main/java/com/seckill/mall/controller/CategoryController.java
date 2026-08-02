package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.CategoryCreateRequest;
import com.seckill.mall.dto.CategoryStatusUpdateRequest;
import com.seckill.mall.dto.CategoryUpdateRequest;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.vo.CategoryVO;
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

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "分类管理", description = "分类树/分类CRUD")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树")
    @GetMapping
    public Result<List<CategoryVO>> tree() {
        return Result.success(categoryService.getCategoryTree());
    }

    @Operation(summary = "新增分类")
    @OperationLog(module = "CATEGORY", action = "CREATE", targetType = "CATEGORY")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success("新增分类成功", categoryService.createCategory(request));
    }

    @Operation(summary = "编辑分类")
    @OperationLog(module = "CATEGORY", action = "UPDATE", targetIdSpEL = "#id", targetType = "CATEGORY")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id,
                                     @RequestBody CategoryUpdateRequest request) {
        return Result.success("编辑分类成功", categoryService.updateCategory(id, request));
    }

    @Operation(summary = "删除分类")
    @OperationLog(module = "CATEGORY", action = "DELETE", targetIdSpEL = "#id", targetType = "CATEGORY")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.<Void>success("删除分类成功", null);
    }

    @Operation(summary = "切换分类状态")
    @OperationLog(module = "CATEGORY", action = "UPDATE_STATUS", targetIdSpEL = "#id", targetType = "CATEGORY")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody CategoryStatusUpdateRequest request) {
        categoryService.updateCategoryStatus(id, request);
        return Result.<Void>success("状态更新成功", null);
    }
}
