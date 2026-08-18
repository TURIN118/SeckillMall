package com.seckill.mall.category.interfaces.web;

import com.seckill.mall.category.api.CategoryAttributeApi;
import com.seckill.mall.category.application.facade.CategoryApiConverter;
import com.seckill.mall.category.interfaces.vo.CategoryAttributeVO;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.CategoryAttributeDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类规格模板控制器（后台管理）
 * <p>
 * 前缀 /api/v1/admin/category，需管理员权限。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryAttributeController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "分类规格模板", description = "分类级别定义规格维度模板")
@RestController
@RequestMapping("/api/v1/admin/category")
@RequiredArgsConstructor
public class CategoryAttributeController {

    private final CategoryAttributeApi categoryAttributeApi;

    @Operation(summary = "获取分类的规格模板（含预设值）")
    @GetMapping("/{categoryId}/attributes")
    public Result<List<CategoryAttributeVO>> listByCategory(@PathVariable Long categoryId) {
        return Result.success(
                CategoryApiConverter.toAttrVOList(categoryAttributeApi.getAttributesByCategory(categoryId)));
    }

    @Operation(summary = "创建分类规格属性")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/attributes")
    public Result<CategoryAttributeVO> create(@Valid @RequestBody CategoryAttributeDTO dto) {
        com.seckill.mall.category.api.dto.CategoryAttributeDTO result =
                categoryAttributeApi.createAttribute(dto);
        return Result.success(CategoryApiConverter.toVO(result));
    }

    @Operation(summary = "更新分类规格属性")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/attributes/{id}")
    public Result<CategoryAttributeVO> update(@PathVariable Long id, @Valid @RequestBody CategoryAttributeDTO dto) {
        com.seckill.mall.category.api.dto.CategoryAttributeDTO result =
                categoryAttributeApi.updateAttribute(id, dto);
        return Result.success(CategoryApiConverter.toVO(result));
    }

    @Operation(summary = "删除分类规格属性")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/attributes/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryAttributeApi.deleteAttribute(id);
        return Result.success();
    }

    @Operation(summary = "为分类规格属性添加预设值")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/attributes/{id}/values")
    public Result<CategoryAttributeVO> addValue(@PathVariable Long id,
                                                @Valid @RequestBody CategoryAttributeDTO.CategoryAttributeValueDTO value) {
        com.seckill.mall.category.api.dto.CategoryAttributeDTO result =
                categoryAttributeApi.addAttributeValue(id, value);
        return Result.success(CategoryApiConverter.toVO(result));
    }
}
