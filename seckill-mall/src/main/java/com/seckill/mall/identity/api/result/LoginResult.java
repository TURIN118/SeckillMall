package com.seckill.mall.identity.api.result;

import com.seckill.mall.identity.api.dto.UserSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果，登录成功后的返回结果。
 *
 * <p>由 {@code AuthApi.login} 方法返回，包含访问令牌、刷新令牌与用户信息快照。
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 令牌类型（"Bearer"） */
    private String tokenType;

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户信息快照 */
    private UserSnapshot user;
}