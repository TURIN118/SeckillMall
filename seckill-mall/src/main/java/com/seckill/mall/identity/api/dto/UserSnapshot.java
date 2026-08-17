package com.seckill.mall.identity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户快照 DTO，替代 User Entity 跨模块传递。
 *
 * <p>跨模块只读传递时使用，避免暴露 {@code User} Entity。
 * 裁剪掉 {@code password}、{@code isDeleted}、{@code createTime}、{@code updateTime} 等基础设施字段。
 *
 * <p>来源映射：User Entity → UserSnapshot
 * {@code role}/{@code status} 枚举转为 String 名称。
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshot {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** 钱包余额 */
    private BigDecimal balance;

    /** 角色（BUYER/SELLER/ADMIN） */
    private String role;

    /** 状态（ACTIVE/LOCKED/DISABLED） */
    private String status;
}