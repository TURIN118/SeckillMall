package com.seckill.mall.service;

import com.seckill.mall.common.Result;
import com.seckill.mall.vo.FavoriteItemVO;

import java.util.List;

/**
 * 用户收藏夹服务接口
 * <p>
 * 提供收藏、取消收藏、列表查询、是否已收藏检查及数量统计能力。
 * 收藏/取消收藏时同步维护 {@code t_product.favorite_count} 冗余计数。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserFavoriteService.java
 * 邮箱：nj651217@163.com
 */
public interface UserFavoriteService {

    /**
     * 获取指定用户的收藏列表（含商品展示信息）。
     *
     * @param userId 用户 ID
     * @return 收藏项视图列表
     */
    Result<List<FavoriteItemVO>> getFavoriteList(Long userId);

    /**
     * 添加收藏。
     * <p>
     * 若已存在收藏记录（含逻辑删除的）则恢复 is_deleted=0，否则新建；
     * 同步递增 {@code t_product.favorite_count}。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     */
    Result<Void> addFavorite(Long userId, Long productId);

    /**
     * 取消收藏（逻辑删除）。
     * <p>
     * 同步递减 {@code t_product.favorite_count}。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     */
    Result<Void> removeFavorite(Long userId, Long productId);

    /**
     * 检查指定用户是否已收藏某商品。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return true=已收藏 / false=未收藏
     */
    Result<Boolean> isFavorited(Long userId, Long productId);

    /**
     * 获取指定用户的收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    Result<Integer> getFavoriteCount(Long userId);
}