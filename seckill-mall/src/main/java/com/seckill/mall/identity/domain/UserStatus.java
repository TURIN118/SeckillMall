package com.seckill.mall.identity.domain;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户状态枚举。
 *
 * <p>从 {@code com.seckill.mall.entity.enums.UserStatus} 迁移至 {@code identity.domain}。
 *
 * @author WNJ
 * @since Phase I.3
 */
public enum UserStatus implements IEnum<String> {

    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
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

    public static UserStatus fromCode(String code) {
        for (UserStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }
}