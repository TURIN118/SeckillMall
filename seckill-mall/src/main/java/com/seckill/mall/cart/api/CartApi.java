package com.seckill.mall.cart.api;

import com.seckill.mall.cart.api.command.AddToCartCommand;
import com.seckill.mall.cart.api.command.BatchUpdateSelectedCommand;
import com.seckill.mall.cart.api.command.RemoveCartItemCommand;
import com.seckill.mall.cart.api.command.UpdateCartQuantityCommand;
import com.seckill.mall.cart.api.command.UpdateSelectedCommand;
import com.seckill.mall.cart.api.dto.CartItemDTO;

import java.util.List;

/**
 * Cart 模块购物车业务能力 API。
 *
 * <p>对外暴露购物车增删改查、选中状态管理、数量统计及跨模块只读访问等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * <p>原方法映射参见 CART-API-CONTRACT.md 第 7 节。
 *
 * @author wnj
 * @since Phase C.2
 */
public interface CartApi {

    /**
     * 获取指定用户的购物车列表（含商品展示信息）。
     *
     * @param userId 用户 ID
     * @return 购物车项 DTO 列表
     */
    List<CartItemDTO> getCartList(Long userId);

    /**
     * 添加商品到购物车。
     * <p>
     * 若已存在相同商品（同 productId + skuId）则数量累加，否则新建购物车项；
     * 同步递增 {@code t_product.cart_count}。
     *
     * @param command 加购命令
     */
    void addToCart(AddToCartCommand command);

    /**
     * 修改购物车项数量（校验归属当前用户）。
     *
     * @param command 修改数量命令
     */
    void updateQuantity(UpdateCartQuantityCommand command);

    /**
     * 删除购物车项（逻辑删除，校验归属）。
     * <p>
     * 同步递减 {@code t_product.cart_count}。
     *
     * @param command 删除命令
     */
    void removeFromCart(RemoveCartItemCommand command);

    /**
     * 清空指定用户的购物车（逻辑删除全部项）。
     * <p>
     * 同步递减对应商品的 {@code cart_count}。
     *
     * @param userId 用户 ID
     */
    void clearCart(Long userId);

    /**
     * 更新单个购物车项的选中状态（校验归属）。
     *
     * @param command 更新选中状态命令
     */
    void updateSelected(UpdateSelectedCommand command);

    /**
     * 批量更新购物车项的选中状态（校验归属）。
     *
     * @param command 批量更新选中状态命令
     */
    void batchUpdateSelected(BatchUpdateSelectedCommand command);

    /**
     * 获取指定用户购物车中的商品种类数量。
     *
     * @param userId 用户 ID
     * @return 购物车项数量
     */
    int getCartCount(Long userId);

    /**
     * 按 ID 批量查询购物车项（跨模块只读访问，返回 DTO 而非 Entity）。
     * <p>
     * 供 OrderServiceImpl#createOrderFromCart 跨模块内部调用，
     * 替代旧 {@code CartService.getCartsByIds}，消除调用方对 Cart Entity 的依赖。
     *
     * @param cartIds 购物车项 ID 列表
     * @return 购物车项 DTO 列表
     */
    List<CartItemDTO> getCartItemsByIds(List<Long> cartIds);

    /**
     * 批量逻辑删除购物车项（{@code @TableLogic} 自动 set is_deleted=1）。
     * <p>
     * 供 OrderServiceImpl#createOrderFromCart 跨模块内部调用，
     * 用于结算后删除已结算购物车项。
     *
     * @param cartIds 购物车项 ID 列表
     */
    void deleteCartItemsByIds(List<Long> cartIds);
}