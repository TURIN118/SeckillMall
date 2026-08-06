package com.seckill.mall.service;

import com.seckill.mall.common.Result;
import com.seckill.mall.vo.CartItemVO;

import java.util.List;

/**
 * 购物车服务接口
 * <p>
 * 提供购物车的增删改查、选中状态管理及数量统计能力，
 * 所有写操作均需校验购物车项归属当前用户。
 * 加购/删除时同步维护 {@code t_product.cart_count} 冗余计数。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CartService.java
 * 邮箱：nj651217@163.com
 */
public interface CartService {

    /**
     * 获取指定用户的购物车列表（含商品展示信息）。
     *
     * @param userId 用户 ID
     * @return 购物车项视图列表
     */
    Result<List<CartItemVO>> getCartList(Long userId);

    /**
     * 添加商品到购物车。
     * <p>
     * 若已存在相同商品（同 productId + skuId）则数量累加，否则新建购物车项；
     * 同步递增 {@code t_product.cart_count}。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param skuId     SKU ID（可选，null 或 0 表示无规格商品）
     * @param quantity  加购数量（必须大于0）
     */
    Result<Void> addToCart(Long userId, Long productId, Long skuId, Integer quantity);

    /**
     * 修改购物车项数量（校验归属当前用户）。
     *
     * @param userId 用户 ID
     * @param cartId 购物车项 ID
     * @param quantity 新数量（必须大于0）
     */
    Result<Void> updateQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 删除购物车项（逻辑删除，校验归属）。
     * <p>
     * 同步递减 {@code t_product.cart_count}。
     *
     * @param userId 用户 ID
     * @param cartId 购物车项 ID
     */
    Result<Void> removeFromCart(Long userId, Long cartId);

    /**
     * 清空指定用户的购物车（逻辑删除全部项）。
     * <p>
     * 同步递减对应商品的 {@code cart_count}。
     *
     * @param userId 用户 ID
     */
    Result<Void> clearCart(Long userId);

    /**
     * 更新单个购物车项的选中状态（校验归属）。
     *
     * @param userId   用户 ID
     * @param cartId   购物车项 ID
     * @param selected 是否选中
     */
    Result<Void> updateSelected(Long userId, Long cartId, Boolean selected);

    /**
     * 批量更新购物车项的选中状态（校验归属）。
     *
     * @param userId   用户 ID
     * @param cartIds  购物车项 ID 列表
     * @param selected 是否选中
     */
    Result<Void> batchUpdateSelected(Long userId, List<Long> cartIds, Boolean selected);

    /**
     * 获取指定用户购物车中的商品种类数量。
     *
     * @param userId 用户 ID
     * @return 购物车项数量
     */
    Result<Integer> getCartCount(Long userId);
}