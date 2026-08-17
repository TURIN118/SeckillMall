package com.seckill.mall.identity.api;

import com.seckill.mall.identity.api.command.AddFavoriteCommand;
import com.seckill.mall.identity.api.command.RemoveFavoriteCommand;
import com.seckill.mall.identity.api.dto.FavoriteItemDTO;

import java.util.List;

/**
 * Identity 模块用户收藏能力 API。
 *
 * <p>对外暴露收藏管理（列表/添加/移除/检查/数量）契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command 对象，禁止裸露多参数</li>
 *     <li>出参用 DTO，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * @author wnj
 * @since Phase I.2
 */
public interface FavoriteApi {

    /**
     * 获取指定用户的收藏列表（含商品展示信息）。
     *
     * @param userId 用户 ID
     * @return 收藏项列表
     */
    List<FavoriteItemDTO> listFavorites(Long userId);

    /**
     * 添加收藏。
     *
     * @param command 添加收藏命令
     * @throws com.seckill.mall.exception.BusinessException {@code PRODUCT_NOT_FOUND}、{@code PARAM_ERROR}
     */
    void addFavorite(AddFavoriteCommand command);

    /**
     * 取消收藏。
     *
     * @param command 移除收藏命令
     */
    void removeFavorite(RemoveFavoriteCommand command);

    /**
     * 检查指定用户是否已收藏某商品。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return {@code true} 已收藏
     */
    boolean checkFavorite(Long userId, Long productId);

    /**
     * 获取指定用户的收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    int getFavoriteCount(Long userId);
}