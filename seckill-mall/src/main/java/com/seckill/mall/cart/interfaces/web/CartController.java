package com.seckill.mall.cart.interfaces.web;

import com.seckill.mall.cart.api.CartApi;
import com.seckill.mall.cart.api.command.AddToCartCommand;
import com.seckill.mall.cart.api.command.BatchUpdateSelectedCommand;
import com.seckill.mall.cart.api.command.RemoveCartItemCommand;
import com.seckill.mall.cart.api.command.UpdateCartQuantityCommand;
import com.seckill.mall.cart.api.command.UpdateSelectedCommand;
import com.seckill.mall.cart.api.dto.CartItemDTO;
import com.seckill.mall.cart.application.facade.CartApiConverter;
import com.seckill.mall.cart.interfaces.vo.CartItemVO;
import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
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
 * 购物车控制器（Phase C.4-C 已切换到 {@link CartApi}）。
 *
 * <p>前缀 {@code /api/v1/cart}，需登录且角色为 BUYER、SELLER 或 ADMIN。
 * 当前用户 ID 通过 {@link SecurityUtils#getCurrentUserId()} 获取，
 * 用户仅能操作属于自己的购物车项。
 *
 * <p>Strangler Pattern：注入 {@link CartApi} 替代旧 {@code CartService}，
 * 通过 {@link CartApiConverter} 做 DTO → VO 转换，保持前端出参结构不变。
 *
 * <p>创建人：@author WNJ
 */
@Tag(name = "购物车管理", description = "购物车增删改查、选中状态与数量统计")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
public class CartController {

    private final CartApi cartApi;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        Long userId = securityUtils.getCurrentUserId();
        List<CartItemDTO> dtoList = cartApi.getCartList(userId);
        return Result.success(CartApiConverter.toVOList(dtoList));
    }

    @Operation(summary = "添加到购物车")
    @PostMapping("/add")
    // 安全修复（M3）：添加 @Validated 触发请求体字段校验
    public Result<Void> add(@Validated @RequestBody AddToCartRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        AddToCartCommand command = AddToCartCommand.builder()
                .userId(userId)
                .productId(req.getProductId())
                .skuId(req.getSkuId())
                .quantity(req.getQuantity())
                .build();
        cartApi.addToCart(command);
        return Result.success();
    }

    @Operation(summary = "修改购物车项数量")
    @PutMapping("/{cartId}/quantity")
    // 安全修复（M3）：添加 @Validated 触发请求体字段校验
    public Result<Void> updateQuantity(@PathVariable Long cartId,
                                       @Validated @RequestBody UpdateQuantityRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        UpdateCartQuantityCommand command = UpdateCartQuantityCommand.builder()
                .userId(userId)
                .cartId(cartId)
                .quantity(req.getQuantity())
                .build();
        cartApi.updateQuantity(command);
        return Result.success();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{cartId}")
    public Result<Void> remove(@PathVariable Long cartId) {
        Long userId = securityUtils.getCurrentUserId();
        RemoveCartItemCommand command = RemoveCartItemCommand.builder()
                .userId(userId)
                .cartId(cartId)
                .build();
        cartApi.removeFromCart(command);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        Long userId = securityUtils.getCurrentUserId();
        cartApi.clearCart(userId);
        return Result.success();
    }

    @Operation(summary = "更新单个购物车项选中状态")
    @PutMapping("/{cartId}/selected")
    public Result<Void> updateSelected(@PathVariable Long cartId,
                                       @RequestBody UpdateSelectedRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        UpdateSelectedCommand command = UpdateSelectedCommand.builder()
                .userId(userId)
                .cartId(cartId)
                .selected(req.getSelected())
                .build();
        cartApi.updateSelected(command);
        return Result.success();
    }

    @Operation(summary = "批量更新购物车项选中状态")
    @PutMapping("/batch-selected")
    public Result<Void> batchUpdateSelected(@RequestBody BatchUpdateSelectedRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        BatchUpdateSelectedCommand command = BatchUpdateSelectedCommand.builder()
                .userId(userId)
                .cartIds(req.getCartIds())
                .selected(req.getSelected())
                .build();
        cartApi.batchUpdateSelected(command);
        return Result.success();
    }

    @Operation(summary = "获取购物车数量")
    @GetMapping("/count")
    public Result<Integer> count() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(cartApi.getCartCount(userId));
    }

    /** 添加购物车请求体 */
    @Data
    public static class AddToCartRequest {
        @NotNull(message = "商品 ID 不能为空")
        private Long productId;

        /** SKU ID（可选，null 表示无规格商品） */
        private Long skuId;

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
