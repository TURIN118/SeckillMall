package com.seckill.mall.identity.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.identity.infrastructure.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户收藏夹 Mapper。
 *
 * <p>基于 MyBatis-Plus {@link BaseMapper}，复杂查询使用 Lambda Wrapper。
 * 提供 {@link #selectByUserAndProductIncludeDeleted} 与 {@link #restore}
 * 用于绕过 {@code @TableLogic} 处理唯一约束冲突场景下逻辑删除记录的恢复。
 *
 * <p>从 {@code com.seckill.mall.mapper.UserFavoriteMapper} 迁移至 {@code identity.infrastructure.mapper}。
 * 仅在 identity 模块 infrastructure 层内部使用，不对外暴露。
 *
 * @author WNJ
 * @since Phase I.3
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 查询用户某商品的收藏记录（包含已逻辑删除的）。
     * <p>
     * 绕过 {@code @TableLogic}，用于添加收藏时检测唯一约束冲突并恢复。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 收藏实体（可能为 null）
     */
    @Select("SELECT id, user_id, product_id, is_deleted, create_time, update_time " +
            "FROM t_user_favorite WHERE user_id = #{userId} AND product_id = #{productId} LIMIT 1")
    UserFavorite selectByUserAndProductIncludeDeleted(@Param("userId") Long userId,
                                                      @Param("productId") Long productId);

    /**
     * 恢复逻辑删除的收藏记录。
     * <p>
     * 绕过 {@code @TableLogic}，直接更新 is_deleted=0。
     *
     * @param id 收藏记录 ID
     * @return 受影响行数
     */
    @Update("UPDATE t_user_favorite SET is_deleted = 0, update_time = NOW() WHERE id = #{id}")
    int restore(@Param("id") Long id);
}