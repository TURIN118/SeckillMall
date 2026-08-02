package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.UserFavorite;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.UserFavoriteMapper;
import com.seckill.mall.service.UserFavoriteService;
import com.seckill.mall.vo.FavoriteItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户收藏夹服务实现
 * <p>
 * 基于 {@link UserFavoriteMapper} 进行 CRUD，使用 {@link LambdaQueryWrapper} /
 * {@link LambdaUpdateWrapper} 构造条件。
 * <p>
 * 收藏语义说明：
 * <ul>
 *   <li>若已存在未删除的收藏记录，视为已收藏，幂等返回成功；</li>
 *   <li>若存在逻辑删除的记录（唯一约束），则恢复 is_deleted=0，{@code favorite_count + 1}；</li>
 *   <li>否则新建收藏记录，{@code favorite_count + 1}。</li>
 * </ul>
 * 取消收藏时逻辑删除并 {@code favorite_count - 1}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserFavoriteServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFavoriteServiceImpl implements UserFavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final ProductMapper productMapper;

    @Override
    public Result<List<FavoriteItemVO>> getFavoriteList(Long userId) {
        // 1. 查询用户所有收藏记录（按创建时间倒序）
        List<UserFavorite> favorites = userFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime));
        if (favorites.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        // 2. 批量查询商品信息（避免 N+1）
        List<Long> productIds = favorites.stream()
                .map(UserFavorite::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        // 3. 组装 VO
        List<FavoriteItemVO> voList = favorites.stream()
                .map(fav -> toVO(fav, productMap.get(fav.getProductId())))
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addFavorite(Long userId, Long productId) {
        // 校验商品存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 查询是否已收藏（is_deleted=0）
        UserFavorite existFav = userFavoriteMapper.selectOne(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getProductId, productId));
        if (existFav != null) {
            // 已收藏：幂等返回
            return Result.<Void>success("该商品已收藏", null);
        }
        // 检查是否存在逻辑删除的记录（唯一约束冲突处理）
        UserFavorite deletedFav = userFavoriteMapper.selectByUserAndProductIncludeDeleted(userId, productId);
        if (deletedFav != null) {
            // 恢复
            userFavoriteMapper.restore(deletedFav.getId());
            // favorite_count + 1
            updateProductFavoriteCount(productId, 1);
            log.info("恢复逻辑删除的收藏记录，favId={}, userId={}, productId={}",
                    deletedFav.getId(), userId, productId);
        } else {
            // 新建收藏记录
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            userFavoriteMapper.insert(favorite);
            // favorite_count + 1
            updateProductFavoriteCount(productId, 1);
            log.info("新建收藏记录，favId={}, userId={}, productId={}",
                    favorite.getId(), userId, productId);
        }
        return Result.<Void>success("收藏成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeFavorite(Long userId, Long productId) {
        UserFavorite favorite = userFavoriteMapper.selectOne(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getProductId, productId));
        if (favorite == null) {
            // 未收藏：幂等返回
            return Result.<Void>success("该商品未收藏", null);
        }
        // 逻辑删除
        userFavoriteMapper.deleteById(favorite.getId());
        // favorite_count - 1
        updateProductFavoriteCount(productId, -1);
        log.info("取消收藏成功，favId={}, userId={}, productId={}", favorite.getId(), userId, productId);
        return Result.<Void>success("取消收藏成功", null);
    }

    @Override
    public Result<Boolean> isFavorited(Long userId, Long productId) {
        Long count = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getProductId, productId));
        return Result.success(count != null && count > 0);
    }

    @Override
    public Result<Integer> getFavoriteCount(Long userId) {
        Long count = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, userId));
        return Result.success(count == null ? 0 : count.intValue());
    }

    /**
     * 递增/递减商品收藏计数（冗余计数维护）。
     * <p>
     * 使用 {@code setSql} 直接执行 {@code favorite_count = favorite_count + delta}，
     * 避免并发下的覆盖更新。{@code @TableLogic} 自动追加 {@code is_deleted=0} 条件。
     *
     * @param productId 商品 ID
     * @param delta     变化量（+1 或 -1）
     */
    private void updateProductFavoriteCount(Long productId, int delta) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("favorite_count = favorite_count + " + delta);
        productMapper.update(null, wrapper);
    }

    /**
     * 实体 + 商品 → 视图对象
     *
     * @param favorite 收藏项
     * @param product  商品（可能为 null，如商品被删除时）
     * @return 收藏项视图
     */
    private FavoriteItemVO toVO(UserFavorite favorite, Product product) {
        FavoriteItemVO vo = new FavoriteItemVO();
        vo.setId(favorite.getId());
        vo.setProductId(favorite.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setMainImage(product.getMainImage());
            vo.setOriginalPrice(product.getOriginalPrice());
            vo.setSalesCount(product.getSalesCount());
            vo.setFavoriteCount(product.getFavoriteCount());
            vo.setProductStatus(product.getStatus() != null ? product.getStatus().getCode() : null);
        }
        return vo;
    }
}