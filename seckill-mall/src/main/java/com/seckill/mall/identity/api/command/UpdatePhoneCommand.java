package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改手机号命令。
 *
 * <p>业务语义：修改用户手机号（调用方需先完成验证码校验）。
 *
 * <p>原方法：{@code UserService.updatePhone(Long, String)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePhoneCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 新手机号（11 位，必填） */
    private String phone;
}