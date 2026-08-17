package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户状态命令。
 *
 * <p>业务语义：启用/禁用/锁定用户（同步更新 security 缓存中的用户状态）。
 *
 * <p>原方法：{@code AdminUserService.updateUserStatus(Long, UserStatus)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 用户状态（ACTIVE/LOCKED/DISABLED，必填） */
    private String status;
}