package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.BannerCreateRequest;
import com.seckill.mall.dto.BannerUpdateRequest;
import com.seckill.mall.service.BannerService;
import com.seckill.mall.vo.BannerVO;
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
 * 轮播图后台管理 Controller
 * 前缀：/api/v1/admin/banners，需 ADMIN 角色
 *
 * M-D1 修复：请求体从 BannerVO 拆分为 BannerCreateRequest / BannerUpdateRequest。
 * M-S2 修复：请求体前加 @Valid 触发 jakarta.validation 约束校验。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "轮播图管理", description = "后台轮播图CRUD")
@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "查所有轮播图")
    @GetMapping("/list")
    public Result<List<BannerVO>> list() {
        return Result.success(bannerService.listAll());
    }

    @Operation(summary = "新增轮播图")
    @OperationLog(module = "BANNER", action = "CREATE", targetType = "BANNER")
    @PostMapping("/create")
    public Result<BannerVO> create(@Valid @RequestBody BannerCreateRequest req) {
        return Result.success("新增轮播图成功", bannerService.create(req));
    }

    @Operation(summary = "编辑轮播图")
    @OperationLog(module = "BANNER", action = "UPDATE", targetIdSpEL = "#id", targetType = "BANNER")
    @PutMapping("/{id}")
    public Result<BannerVO> update(@PathVariable Long id, @Valid @RequestBody BannerUpdateRequest req) {
        return Result.success("编辑轮播图成功", bannerService.update(id, req));
    }

    @Operation(summary = "删除轮播图")
    @OperationLog(module = "BANNER", action = "DELETE", targetIdSpEL = "#id", targetType = "BANNER")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.<Void>success("删除轮播图成功", null);
    }

    @Operation(summary = "切换轮播图状态")
    @OperationLog(module = "BANNER", action = "UPDATE_STATUS", targetIdSpEL = "#id", targetType = "BANNER")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody BannerUpdateRequest req) {
        bannerService.updateStatus(id, req.getStatus());
        return Result.<Void>success("状态更新成功", null);
    }
}
