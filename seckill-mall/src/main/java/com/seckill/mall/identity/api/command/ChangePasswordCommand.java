package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码命令。
 *
 * <p>业务语义：修改当前登录用户的密码（校验旧密码，BCrypt 加密新密码）。
 *
 * <p>原方法：{@code AuthService.changePassword(ChangePasswordRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordCommand {

    /** 旧密码（明文，服务端 BCrypt 校验，必填） */
    private String oldPassword;

    /** 新密码（明文，服务端 BCrypt 加密存储，必填） */
    private String newPassword;
}