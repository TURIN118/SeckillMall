package com.seckill.mall.identity.interfaces.web;

import com.seckill.mall.common.Result;
import com.seckill.mall.identity.api.FavoriteApi;
import com.seckill.mall.identity.api.command.AddFavoriteCommand;
import com.seckill.mall.identity.api.command.RemoveFavoriteCommand;
import com.seckill.mall.identity.api.dto.FavoriteItemDTO;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.security.SecurityUtils;
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
 * 用户收藏夹控制器（Phase I.4-C 已切换到 {@link FavoriteApi}）。
 *
 * <p>前缀 {@code /api/v1/favorites}，需登录且角色为 BUYER、SELLER 或 ADMIN。
 *
 * <p>Strangler Pattern：注入 {@link FavoriteApi} 替代旧 {@code UserFavoriteService}，
 * 通过 {@link IdentityApiConverter} 做 DTO → VO 转换，保持前端出参结构不变。
 *
 * <p>创建人：@author WNJ
 */
@Tag(name = "收藏夹管理", description = "收藏、取消收藏、列表查询、检查与数量统计")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
public class UserFavoriteController {

    private final FavoriteApi favoriteApi;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取收藏列表")
    @GetMapping("/list")
    public Result<List<FavoriteItemVO>> list() {
        Long userId = securityUtils.getCurrentUserId();
        List<FavoriteItemDTO> dtoList = favoriteApi.listFavorites(userId);
        return Result.success(IdentityApiConverter.toFavoriteItemVOList(dtoList));
    }

    @Operation(summary = "添加收藏")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody AddFavoriteRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        AddFavoriteCommand command = AddFavoriteCommand.builder()
                .userId(userId)
                .productId(req.getProductId())
                .build();
        favoriteApi.addFavorite(command);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        RemoveFavoriteCommand command = RemoveFavoriteCommand.builder()
                .userId(userId)
                .productId(productId)
                .build();
        favoriteApi.removeFavorite(command);
        return Result.success();
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{productId}")
    public Result<Boolean> check(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(favoriteApi.checkFavorite(userId, productId));
    }

    @Operation(summary = "获取收藏数量")
    @GetMapping("/count")
    public Result<Integer> count() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(favoriteApi.getFavoriteCount(userId));
    }

    /** 添加收藏请求体 */
    @Data
    public static class AddFavoriteRequest {
        private Long productId;
    }
}
