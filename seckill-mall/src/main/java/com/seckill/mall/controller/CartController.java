package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.CartService;
import com.seckill.mall.vo.CartItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * 购物车控制器
 * <p>
 * 前缀 {@code /api/v1/cart}，需登录且角色为 BUYER、SELLER 或 ADMIN。
 * 当前用户 ID 通过 {@link SecurityUtils#getCurrentUserId()} 获取，
 * 用户仅能操作属于自己的购物车项。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CartController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "购物车管理", description = "购物车增删改查、选中状态与数量统计")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "获取购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.getCartList(userId);
    }

    @Operation(summary = "添加到购物车")
    @PostMapping("/add")
    // 安全修复（M3）：添加 @Validated 触发请求体字段校验
    public Result<Void> add(@Validated @RequestBody AddToCartRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.addToCart(userId, req.getProductId(), req.getQuantity());
    }

    @Operation(summary = "修改购物车项数量")
    @PutMapping("/{cartId}/quantity")
    // 安全修复（M3）：添加 @Validated 触发请求体字段校验
    public Result<Void> updateQuantity(@PathVariable Long cartId,
                                       @Validated @RequestBody UpdateQuantityRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.updateQuantity(userId, cartId, req.getQuantity());
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{cartId}")
    public Result<Void> remove(@PathVariable Long cartId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.removeFromCart(userId, cartId);
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.clearCart(userId);
    }

    @Operation(summary = "更新单个购物车项选中状态")
    @PutMapping("/{cartId}/selected")
    public Result<Void> updateSelected(@PathVariable Long cartId,
                                       @RequestBody UpdateSelectedRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.updateSelected(userId, cartId, req.getSelected());
    }

    @Operation(summary = "批量更新购物车项选中状态")
    @PutMapping("/batch-selected")
    public Result<Void> batchUpdateSelected(@RequestBody BatchUpdateSelectedRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.batchUpdateSelected(userId, req.getCartIds(), req.getSelected());
    }

    @Operation(summary = "获取购物车数量")
    @GetMapping("/count")
    public Result<Integer> count() {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartService.getCartCount(userId);
    }

    /** 添加购物车请求体 */
    @Data
    public static class AddToCartRequest {
        @NotNull(message = "商品 ID 不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于 0")
        private Integer quantity;
    }

    /** 修改数量请求体 */
    @Data
    public static class UpdateQuantityRequest {
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于 0")
        private Integer quantity;
    }

    /** 更新选中状态请求体 */
    @Data
    public static class UpdateSelectedRequest {
        private Boolean selected;
    }

    /** 批量更新选中状态请求体 */
    @Data
    public static class BatchUpdateSelectedRequest {
        private List<Long> cartIds;
        private Boolean selected;
    }
}