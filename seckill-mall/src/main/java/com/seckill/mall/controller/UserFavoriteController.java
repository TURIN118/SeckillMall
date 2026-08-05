package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.UserFavoriteService;
import com.seckill.mall.vo.FavoriteItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户收藏夹控制器
 * <p>
 * 前缀 {@code /api/v1/favorites}，需登录且角色为 BUYER、SELLER 或 ADMIN。
 * 当前用户 ID 通过 {@link SecurityUtils#getCurrentUserId()} 获取，
 * 用户仅能操作属于自己的收藏记录。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserFavoriteController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "收藏夹管理", description = "收藏、取消收藏、列表查询、检查与数量统计")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
public class UserFavoriteController {

    private final UserFavoriteService userFavoriteService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取收藏列表")
    @GetMapping("/list")
    public Result<List<FavoriteItemVO>> list() {
        Long userId = securityUtils.getCurrentUserId();
        return userFavoriteService.getFavoriteList(userId);
    }

    @Operation(summary = "添加收藏")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody AddFavoriteRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return userFavoriteService.addFavorite(userId, req.getProductId());
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        return userFavoriteService.removeFavorite(userId, productId);
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{productId}")
    public Result<Boolean> check(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        return userFavoriteService.isFavorited(userId, productId);
    }

    @Operation(summary = "获取收藏数量")
    @GetMapping("/count")
    public Result<Integer> count() {
        Long userId = securityUtils.getCurrentUserId();
        return userFavoriteService.getFavoriteCount(userId);
    }

    /** 添加收藏请求体 */
    @Data
    public static class AddFavoriteRequest {
        private Long productId;
    }
}