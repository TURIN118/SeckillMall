package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新个人资料命令。
 *
 * <p>业务语义：更新当前登录用户的个人信息（昵称/邮箱/手机号/头像 URL）。
 *
 * <p>原方法：{@code AuthService.updateProfile(ProfileUpdateRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileCommand {

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatarUrl;
}