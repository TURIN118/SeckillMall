package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seckill.mall.entity.enums.UserRole;
import com.seckill.mall.entity.enums.UserStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：User.java
 * 邮箱：nj651217@163.com
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
