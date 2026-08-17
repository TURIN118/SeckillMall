package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌命令。
 *
 * <p>业务语义：校验 refresh token → 生成新 access token + refresh token。
 *
 * <p>原方法：{@code AuthService.refresh(RefreshTokenRequest)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenCommand {

    /** 刷新令牌（必填） */
    private String refreshToken;
}