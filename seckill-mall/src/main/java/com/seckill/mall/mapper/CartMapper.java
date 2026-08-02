package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 购物车 Mapper
 * <p>
 * 基于 MyBatis-Plus {@link BaseMapper}，复杂查询使用 Lambda Wrapper。
 * 提供 {@link #selectByUserAndProductIncludeDeleted} 与
 * {@link #restoreAndSetQuantity} 用于绕过 {@code @TableLogic} 处理
 * 唯一约束冲突场景下逻辑删除记录的恢复。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CartMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 查询用户某商品的购物车记录（包含已逻辑删除的）。
     * <p>
     * 绕过 {@code @TableLogic}，用于添加购物车时检测唯一约束冲突并恢复。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 购物车实体（可能为 null）
     */
    @Select("SELECT * FROM t_cart WHERE user_id = #{userId} AND product_id = #{productId} LIMIT 1")
    Cart selectByUserAndProductIncludeDeleted(@Param("userId") Long userId,
                                              @Param("productId") Long productId);

    /**
     * 恢复逻辑删除的购物车记录并设置数量与选中状态。
     * <p>
     * 绕过 {@code @TableLogic}，直接更新 is_deleted=0。
     *
     * @param id       购物车项 ID
     * @param quantity 加购数量
     * @return 受影响行数
     */
    @Update("UPDATE t_cart SET is_deleted = 0, quantity = #{quantity}, selected = 1, update_time = NOW() WHERE id = #{id}")
    int restoreAndSetQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
}
