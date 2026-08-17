package com.seckill.mall.identity.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏夹实体。
 *
 * <p>对应表 {@code t_user_favorite}，每用户每商品唯一（uk_user_product）。
 * 取消收藏采用逻辑删除，再次收藏时恢复 is_deleted=0。
 *
 * <p>从 {@code com.seckill.mall.entity.UserFavorite} 迁移至 {@code identity.infrastructure.entity}。
 * 仅在 identity 模块 infrastructure 层内部使用，不对外暴露。
 *
 * @author WNJ
 * @since Phase I.3
 */
@Data
@TableName("t_user_favorite")
public class UserFavorite {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long productId;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}