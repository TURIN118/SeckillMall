package com.seckill.mall.identity.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seckill.mall.identity.domain.UserRole;
import com.seckill.mall.identity.domain.UserStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体（t_user）。
 *
 * <p>从 {@code com.seckill.mall.entity.User} 迁移至 {@code identity.infrastructure.entity}。
 * 仅在 identity 模块 infrastructure 层内部使用，不对外暴露。
 *
 * @author WNJ
 * @since Phase I.3
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** H16 修复：序列化时忽略 password 字段，防止密码哈希通过 JSON 暴露 */
    @JsonIgnore
    private String password;

    private String phone;

    private String email;

    private String nickname;

    private String avatarUrl;

    /** 钱包余额 */
    private BigDecimal balance;

    @TableField("role")
    private UserRole role;

    @TableField("status")
    private UserStatus status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}