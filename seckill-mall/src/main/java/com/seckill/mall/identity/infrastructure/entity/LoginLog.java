package com.seckill.mall.identity.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.identity.domain.LoginResult;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体（t_login_log）。
 *
 * <p>从 {@code com.seckill.mall.entity.LoginLog} 迁移至 {@code identity.infrastructure.entity}。
 * 仅在 identity 模块 infrastructure 层内部使用，不对外暴露。
 *
 * @author WNJ
 * @since Phase I.3
 */
@Data
@TableName("t_login_log")
public class LoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String loginIp;

    private String loginLocation;

    private String userAgent;

    @TableField("login_result")
    private LoginResult loginResult;

    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}