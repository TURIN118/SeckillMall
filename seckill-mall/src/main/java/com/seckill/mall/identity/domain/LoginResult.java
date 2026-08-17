package com.seckill.mall.identity.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 登录结果枚举。
 *
 * <p>从 {@code com.seckill.mall.entity.enums.LoginResult} 迁移至 {@code identity.domain}。
 *
 * @author WNJ
 * @since Phase I.3
 */
public enum LoginResult implements IEnum<String> {

    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    LoginResult(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getValue() {
        return code;
    }

    public static LoginResult fromCode(String code) {
        for (LoginResult e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LoginResult code: " + code);
    }
}