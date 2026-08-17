package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户角色命令。
 *
 * <p>业务语义：修改用户角色（BUYER/SELLER/ADMIN）。
 *
 * <p>原方法：{@code AdminUserService.updateUserRole(Long, UserRole)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 用户角色（BUYER/SELLER/ADMIN，必填） */
    private String role;
}