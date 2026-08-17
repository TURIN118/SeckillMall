package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改邮箱命令。
 *
 * <p>业务语义：修改用户邮箱（调用方需先完成验证码校验）。
 *
 * <p>原方法：{@code UserService.updateEmail(Long, String)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 新邮箱（必填） */
    private String email;
}