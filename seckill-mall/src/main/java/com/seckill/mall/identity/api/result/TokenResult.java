package com.seckill.mall.identity.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 令牌结果，刷新令牌后的返回结果。
 *
 * <p>由 {@code AuthApi.refreshToken} 方法返回，包含新的访问令牌与刷新令牌。
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResult {

    /** 新访问令牌 */
    private String accessToken;

    /** 新刷新令牌 */
    private String refreshToken;

    /** 令牌类型（"Bearer"） */
    private String tokenType;

    /** 过期时间（秒） */
    private Long expiresIn;
}