package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CategoryController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "分类管理", description = "分类树")
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
}
